package commands;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CommandHandler {
	private Map<String, Command> commands = new HashMap<>();
	static private final String PREFIX = "!";

	public CommandHandler() {
		registerCommands(this);
	}

	public void registerCommands(Object obj) {
		CommandRegisterHelper.getCommands(obj).forEach(this::registerCommand);
	}

	public void registerCommand(Command cmd) {
		commands.put(cmd.name, cmd);
	}

	@EventSubscriber
	public void runCommand(MessageReceivedEvent event) {
		IMessage message = event.getMessage();
		if (isCommand(message.getContent())) {
			String msg = message.getContent();
			String[] digestedString = msg.substring(msg.indexOf(PREFIX) + 1).split(" ");
			if (commands.containsKey(digestedString[0])) {
				commands.get(digestedString[0]).execute(message, digestedString);
			} else {
				message.getChannel().sendMessage("Command `" + PREFIX + digestedString[0] + "` not found.");
			}

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
		msg.getChannel().sendMessage("Commands are: " + commands);

	}

	private static boolean isCommand(String str) {
		return str.matches(" *" + PREFIX + ".*");
	}
}
