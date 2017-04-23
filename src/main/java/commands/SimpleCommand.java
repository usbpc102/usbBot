package commands;

import sx.blah.discord.handle.obj.IMessage;

public class SimpleCommand {
	final String name;
	final String response;
	SimpleCommand(String name, String response) {
		this.name = name;
		this.response = response;
	}

	public void execute(String[] args, IMessage msg) {
		msg.getChannel().sendMessage(response);
	}
}
