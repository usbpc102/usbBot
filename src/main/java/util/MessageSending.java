package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

import java.io.InputStream;

public class MessageSending {
    private static Logger logger = LoggerFactory.getLogger(MessageSending.class);

    public static void sendMessage(IChannel channel, String message) {
        //INFO This just buffers the message sending for now, it could be made smarter with bundling messages to the same channel together if a RLE happens
        RequestBuffer.request(() -> {
            try {
                channel.sendMessage(message);
            } catch (DiscordException e) {
                logger.debug("I got an error trying to send a message: {}", e.getErrorMessage(), e);
                throw e;
            }
        });
    }

    public static void sendFile(IChannel channel, String message, InputStream inputStream, String fileName) {
        RequestBuffer.request(() -> {
            try {
                channel.sendFile(message, inputStream, fileName);
            } catch (DiscordException e) {
                logger.debug("I got an error trying to send a message: {}", e.getErrorMessage(), e);
                throw e;
            }
        });
    }
}
