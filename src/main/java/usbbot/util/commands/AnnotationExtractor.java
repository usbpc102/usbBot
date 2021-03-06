package usbbot.util.commands;

import usbbot.commands.core.Command;
import usbbot.util.MessageSending;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationExtractor {
	private static Logger logger = LoggerFactory.getLogger(AnnotationExtractor.class);

	public static List<Command> getCommandList(Object obj) {
		Class cl = obj.getClass();
		List<Command> commands = new ArrayList<>();

		Map<String, TmpCommandContainer> commandMap = new HashMap<>();

		MethodHandles.Lookup lookup = MethodHandles.lookup();

		//Gets all methods from the object that have the @DiscordCommand annotation and adds them to the commandMap
		Arrays.stream(cl.getDeclaredMethods()).filter(x -> x.isAnnotationPresent(DiscordCommand.class)).forEach(x -> {
			try {
				x.setAccessible(true);
				commandMap.put(x.getName(), new TmpCommandContainer(x.getAnnotation(DiscordCommand.class).value(), null, lookup.unreflect(x).bindTo(obj)));
			} catch (IllegalAccessException e) {
				logger.error("Something went wrong trying to register {}", x.getAnnotation(DiscordCommand.class).value(), e);
			}
		});

		//Gets all methods from the object that have the @DiscordSubCommand annotation and adds them to the commandMap
		Arrays.stream(cl.getDeclaredMethods()).filter(x -> x.isAnnotationPresent(DiscordSubCommand.class)).forEach(x -> {
			try {
				x.setAccessible(true);
				commandMap.put(x.getName(), new TmpCommandContainer(x.getAnnotation(DiscordSubCommand.class).name(), x.getAnnotation(DiscordSubCommand.class).parent() ,lookup.unreflect(x).bindTo(obj)));
			} catch (IllegalAccessException e) {
				logger.error("Something went wrong trying to register {}", x.getAnnotation(DiscordSubCommand.class).name(), e);
			}
		});

		//This sets the hasChildren of the Commands in the commandMap to true that have sub usbbot.commands
		commandMap.entrySet().stream()
				//filters all the Top Level Commands out
				.filter(x -> x.getValue().parentCommand != null)
				.forEach(x -> {
			if (commandMap.containsKey(x.getValue().parentCommand)) {
				commandMap.get(x.getValue().parentCommand).hasChildren = true;
			} else {
				throw new IllegalStateException("Can't find parent " + x.getValue().parentCommand + " of method " + x.getKey());
			}
		});
		//TODO make this code smarter and less sucky
		logger.debug("This is the map of all command I currently have before creating any DBCommand Objects {}", commandMap.toString());
		//System.out.printf("This is the map of all command I currently have before creating any DBCommand Objects %s\n", commandMap.toString());
		//commandMap.forEach((name, cmdContainer) -> System.out.printf("%s: children %b, parent is %s\r\n", name, cmdContainer.hasChildren, cmdContainer.parentCommand));

		//This puts all usbbot.commands that don't have sub usbbot.commands into the usbbot.commands list and removes them from the commandMap Map
		Set<String> noSubCommands = new HashSet<>();
		commandMap.entrySet().stream()
				.filter(x -> !x.getValue().hasChildren && x.getValue().parentCommand == null)
				.forEach(x -> {
					noSubCommands.add(x.getKey());
					commands.add(new AnnotationCommand(x.getValue().name, "", x.getValue().command));
				});
		noSubCommands.forEach(commandMap::remove);

		while (true) {
			TmpCommandContainer currDeepest = null;
			int currMaxDepth = 0;

			//This gets the current deepest sub command
			for (TmpCommandContainer x : commandMap.values().stream()
					//this filters everything but the bottom most usbbot.commands out that have a parent command
					.filter(x -> x.parentCommand != null && !x.hasChildren)
					.collect(Collectors.toCollection(HashSet::new)))
			{
				TmpCommandContainer tmp = x;
				int depthCount = 0;
				//This count how deep the bottom most command is
				while (tmp.parentCommand != null) {
					tmp = commandMap.get(tmp.parentCommand);
					depthCount++;
				}
				if (depthCount > currMaxDepth) {
					currMaxDepth = depthCount;
					currDeepest = x;
				}
			}

			//this breaks out of the loop when no leave command ist found anymore
			if (currDeepest == null) break;


			String curSearchParentName = currDeepest.parentCommand;
			TmpCommandContainer currParent = commandMap.get(currDeepest.parentCommand);
			currParent.subCommands = new HashMap<>();

			logger.debug("Currently searching for subcommand of {}", curSearchParentName);
			//System.out.printf("Currently searching for subcommand of %s \r\n", curSearchParentName);

			//This goes through every entry that is not a top level command and has the same parent command that our deepest command has
			for (Map.Entry<String, TmpCommandContainer> entry : commandMap.entrySet().stream()
					.filter(x -> x.getValue().parentCommand != null)
					.filter(x -> x.getValue().parentCommand.equals(curSearchParentName))
					.collect(Collectors.toCollection(HashSet::new)))
			{
				//Create a SubCommand and add it into the subCommands map of the parent
				TmpCommandContainer sameParent = entry.getValue();
				logger.debug("I found: {}", sameParent.name);
				//System.out.printf("I found: %s\r\n", sameParent.name);
				currParent.subCommands.put(sameParent.name, new SubCommand(sameParent.command, sameParent.subCommands));
				commandMap.remove(entry.getKey());
			}

			currParent.hasChildren = false;
		}

		//adds all the top level command to the returned usbbot.commands list
		commandMap.values().forEach(x -> commands.add(new SubCommandParent(x.name, "", new SubCommand(x.command, x.subCommands))));


		return commands;
	}
	private static class TmpCommandContainer {
		Map<String, SubCommand> subCommands = null;
		boolean hasChildren = false;
		String name;
		String parentCommand;
		MethodHandle command;

		TmpCommandContainer(String name, String parentCommand, MethodHandle command) {
			this.name = name;
			this.command = command;
			this.parentCommand = parentCommand;
		}

		@Override
		public String toString() {
			return "TmpCommandContainer: hasChildren:" + hasChildren + " name:" + name + " parentCommand:" + parentCommand;
		}
	}
	private static class SubCommand {
		private MethodHandle command;
		Map<String, SubCommand> subCommandMap;

		SubCommand(MethodHandle command, Map<String, SubCommand> subCommandMap) {
			this.command = command;
			this.subCommandMap = subCommandMap;
		}

		void execute(IMessage msg, String[] args, int depth) {
			//TODO: Make error reponses more helpful
			try {
				int offset = (int) command.invoke(msg, args);
				if (subCommandMap != null && offset != -1) {
					depth += offset;
					if (args.length > depth + 1) {
						if (subCommandMap.containsKey(args[depth + 1])) {
							subCommandMap.get(args[depth + 1]).execute(msg, args, depth + 1);
						} else {
							StringBuilder builder = new StringBuilder();
							builder.append("The Subcommand `").append(args[depth + 1]).append("` of command `");
							for (int i = 0; i <= depth; i++) {
								builder.append(args[i]).append(' ');
							}
							builder.append("` dosen't exist");

							MessageSending.sendMessage(msg.getChannel(), builder.toString());
						}

					} else {
						MessageSending.sendMessage(msg.getChannel(), "You need to give me more, please!");
					}

				}
			} catch (Throwable throwable) {
				if (msg.getChannel().getModifiedPermissions(msg.getClient().getOurUser()).contains(Permissions.SEND_MESSAGES)) {
					MessageSending.sendMessage(msg.getChannel(), "Well that sure got me an Error [1] ... ```" + Arrays.toString(throwable.getStackTrace()) + "```");
					logger.error("I got an throwable [1]", throwable);
				} else {
					logger.error("Well I got an Error AND don't have permission to write in the channel I wanna write to... {}", throwable.getMessage(), throwable);
					//System.out.println("Well I got an Error AND don't have permission to write in the channel I wanna write to... " + throwable.getMessage());
				}
			}
		}
	}
	private static class SubCommandParent extends Command {
		SubCommand command;

		SubCommandParent(String name, String description, SubCommand command) {
			this.name = name;
			this.description = description;
			this.command = command;
		}

		@Override
		public void execute(IMessage msg, String... args) {
			command.execute(msg, args, 0);
		}
	}
	private static class AnnotationCommand extends Command {
		private MethodHandle command;
		AnnotationCommand(String name, String description, MethodHandle command) {
			this.name = name;
			this.description = description;
			this.command = command;
		}

		@Override
		public void execute(IMessage msg, String...args) {

			try {
				command.invoke(msg, args);
			} catch (Throwable throwable) {
				if (msg.getChannel().getModifiedPermissions(msg.getClient().getOurUser()).contains(Permissions.SEND_MESSAGES)) {
					MessageSending.sendMessage(msg.getChannel(), "Well that sure got me an Error... ```" + Arrays.toString(throwable.getStackTrace()) + "```");
					logger.error("I got an throwable [2]", throwable);
				} else {
					logger.error("Well I got an Error AND don't have permission to write in the channel I wanna write to... {}", throwable.getMessage(), throwable);
					//System.out.println("Well I got an Error AND don't have permission to write in the channel I wanna write to... " + throwable.getMessage());
				}
			}

		}
	}
}
