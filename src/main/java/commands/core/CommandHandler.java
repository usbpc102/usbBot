package commands.core;

import commands.annotations.AnnotationRegister;
import commands.annotations.DiscordCommand;
import util.MessageSending;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

import java.util.*;

public class CommandHandler {
	private Map<String, Command> commands = new HashMap<>();
	//TODO make this a config option
	private String PREFIX = "!";
	private Logger logger = LoggerFactory.getLogger(CommandHandler.class);
	public CommandHandler() {
	}

	public void registerCommand(Command cmd) {
		commands.put(cmd.getName(), cmd);
	}

	public void unregisterCommand(String name) {
		commands.remove(name);
	}

	public void runCommand(MessageReceivedEvent event) {
		IMessage message = event.getMessage();
		if (!message.getChannel().isPrivate() && isCommand(message.getContent())) {
			String msg = message.getContent();
			String[] digestedString = msg.substring(msg.indexOf(PREFIX) + 1).split(" ");
			if (commands.containsKey(digestedString[0])) {

				logger.debug("Executing command '{}'", digestedString[0]);
				//System.out.printf("[CommandHandler] Executing command '%s'\r\n", digestedString[0]);
				commands.get(digestedString[0]).execute(message, digestedString);
			} /*else {
				message.getChannel().sendMessage("Command `" + PREFIX + digestedString[0] + "` not found.");
			}*/

		}
	}

	@DiscordCommand("list")
	public void list(IMessage msg, String...args) {
		Iterator<String> iterator = commands.keySet().iterator();
		String commands = "";
		StringBuilder builder = new StringBuilder();
		while (iterator.hasNext()) {
			builder.append('!').append(iterator.next());
			if (iterator.hasNext()) {
				builder.append(", ");
			} else {
				commands = builder.toString();
			}
		}
		MessageSending.sendMessage(msg.getChannel(), "Commands are: " + commands);

	}

	public Command getCommandByName(String name) {
		return commands.get(name);
	}

	public Collection<Command> getAllCommands() {
		return commands.values();
	}

	private boolean isCommand(String str) {
		return str.matches(" *" + PREFIX + ".*");
	}
}
