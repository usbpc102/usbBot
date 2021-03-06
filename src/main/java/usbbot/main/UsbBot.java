package usbbot.main;

import usbbot.commands.*;
import usbbot.commands.core.Command;
import usbbot.config.DatabaseConnection;
import usbbot.util.commands.AnnotationExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

import java.io.*;
import java.util.Collection;
import java.util.Properties;

public class UsbBot implements DiscordCommands {
	private static Logger logger = LoggerFactory.getLogger(UsbBot.class);
	private static String keysFile = "keys.properties";
	private IDiscordClient client;
	private UsbBot(String discordAPIKey) {

		client = createClient(discordAPIKey, false);
		client.getDispatcher().registerListener(new EventHandler());
		Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection.INSTANCE::close));
		client.login();

	}

	public static void main(String...args) {
		if (args.length == 1) {
			keysFile = args[0];
		}
		new UsbBot(getProperty("discord"));
	}


	public static String getProperty(String name) {
		//TODO the get property throws an exeption when it dosen't finde the file... fix that and stuff
		Properties keys = new Properties();
		String discordApiKey = "404";
		try {
			FileInputStream in = new FileInputStream(keysFile);
			keys.load(in);
			discordApiKey = keys.getProperty(name, "404");
			if (discordApiKey.equals("404")) {
				FileOutputStream out = new FileOutputStream(keysFile);

				logger.error("No discord API key found, please enter one in the keys.properties file");
				keys.setProperty(name, "404");
				keys.store(out, "");
				System.exit(-1);
			}
		} catch (IOException e) {
			logger.error("Something went wrong while opening the keys.properties", e);
		}
		return discordApiKey;
	}

	private static IDiscordClient createClient(String token, boolean login) {
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(token);
		try {
			if (login) {
				return clientBuilder.login();
			} else {
				return clientBuilder.build();
			}
		} catch (DiscordException e) {
			logger.error("Building the client failed ", e);
			return null;
		}
	}

	@Override
	public Collection<Command> getDiscordCommands() {
		return AnnotationExtractor.getCommandList(this);
	}
}
