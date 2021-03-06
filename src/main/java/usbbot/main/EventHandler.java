package usbbot.main;

import usbbot.commands.CommandHandler;
import usbbot.commands.security.PermissionManager;
import usbbot.modules.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent;
import usbbot.modules.Moderation.BulkDeleteCommand;
import usbbot.modules.music.Music;

/**
 * @author usbpc
 * @since 2017-05-18
 */
public class EventHandler {
    private static long CHANNEL = 378509902009597965L;
    private CommandHandler cmdHandler = new CommandHandler();
    private Jail jail = new Jail();
    EventHandler() {
        //cmdHandler.registerCommands(new TestCommands());
        cmdHandler.registerCommands(new SimpleTextResponses(cmdHandler));
        cmdHandler.registerCommands(new HelpCommand());
        cmdHandler.registerCommands(new MiscCommands());
        cmdHandler.registerCommands(new MoreVoiceChannel());
        cmdHandler.registerCommands(new PermissionManager());
        cmdHandler.registerCommands(new ModerationHelp());
        cmdHandler.registerCommands(new SystemCommands());
        cmdHandler.registerCommands(new BulkDeleteCommand());
        cmdHandler.registerCommands(new Music());
        cmdHandler.registerCommands(jail);
    }
    private static Logger logger = LoggerFactory.getLogger(EventHandler.class);
    @EventSubscriber
    public void onUserVoiceChannelJoinEvent(UserVoiceChannelJoinEvent event) {
        logger.debug("Someone joined: {}", event.getVoiceChannel().getName());
        MoreVoiceChannelsKt.someoneJoined(event);
        jail.userMoved(event.getUser(), event.getVoiceChannel());
        /*if (event.getUser().getLongID() == 315264591867281408L && event.getVoiceChannel().getLongID() != CHANNEL) {
            RequestBuffer.request(() -> event.getUser().moveToVoiceChannel(event.getGuild().getVoiceChannelByID(CHANNEL)));
        }*/
    }

    @EventSubscriber
    public void onUserVoiceChannelMoveEvent(UserVoiceChannelMoveEvent event) {
        logger.debug("Someone moved to: {}", event.getOldChannel().getName());
        MoreVoiceChannelsKt.someoneMoved(event);
        jail.userMoved(event.getUser(), event.getNewChannel());
        /*if (event.getUser().getLongID() == 315264591867281408L && event.getNewChannel().getLongID() != CHANNEL) {
            RequestBuffer.request(() -> event.getUser().moveToVoiceChannel(event.getGuild().getVoiceChannelByID(CHANNEL)));
        }*/
    }

    @EventSubscriber
    public void onUserVoiceChannelLeaveEvent(UserVoiceChannelLeaveEvent event) {
        logger.debug("Someone left: {}", event.getVoiceChannel().getName());
        MoreVoiceChannelsKt.someoneLeft(event);
    }


    @EventSubscriber
    public void onGuildCreateEvent(GuildCreateEvent event) {
        logger.debug("I'm connected to {}", event.getGuild().getName());
        Long start = System.currentTimeMillis();
        cmdHandler.createMissingPermissions(event.getGuild().getLongID());
        logger.debug("It took {}ms to create permissions for {}", System.currentTimeMillis() - start, event.getGuild().getName());
                /*CommandModule commandModule = new CommandModule(event.getGuild().getLongID());
        commandModule.registerCommands(commandModule);
        //commandModule.registerCommands(new TestCommands());
        commandModule.registerCommands(new SimpleTextResponses(commandModule, event.getGuild().getLongID()));
        commandModule.registerCommands(new HelpCommand());
        commandModule.registerCommands(new MiscCommands());
        commandModule.registerCommands(new MoreVoiceChannel());
        commandModuleMap.put(event.getGuild().getLongID(), commandModule);*/
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event) {
        logger.debug("I got a message, content: {}", event.getMessage().getContent());
        cmdHandler.onMessageRecivedEvent(event);
    }

    @EventSubscriber
    public void onGuildLeaveEvent(GuildLeaveEvent event) {

    }
}
