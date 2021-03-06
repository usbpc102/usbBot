package usbbot.modules

import org.slf4j.LoggerFactory
import usbbot.commands.DiscordCommands
import usbbot.commands.core.Command
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent
import sx.blah.discord.handle.obj.ICategory
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.Permissions
import usbbot.config.addWatchedForGuild
import usbbot.config.delWatchedForGuild
import usbbot.config.isWached
import usbbot.util.MessageSending
import usbbot.util.commands.AnnotationExtractor
import usbbot.util.commands.DiscordCommand
import usbbot.util.commands.DiscordSubCommand
import util.*
import java.security.Permission

//TODO: Add help lines for this
//TODO: Make responses more clear
//TODO: Allow more than just categories growing, maybe multiple channels in categorie or something
//TODO: Add master slave channel system (Streaming -> Warteraum Stream)
class MoreVoiceChannel : DiscordCommands {
    companion object {
        val logger = this.getLogger()
    }
    override fun getDiscordCommands(): MutableCollection<Command> {
        return AnnotationExtractor.getCommandList(this)
    }

    @DiscordCommand("voice")
    fun voice(msg: IMessage, args: Array<String>) : Int {
        return 0
    }

    @DiscordSubCommand(name = "add", parent = "voice")
    fun voiceAdd(msg: IMessage, args: Array<String>) {
        if (!msg.guild.checkOurPermissionOrSendError(msg.channel, Permissions.MANAGE_CHANNELS)) return
        val categoryID = args[2].toLong()
        msg.guild.getCategoryByID(categoryID)?.let {
            if (addWatchedForGuild(it.guild.longID, it.longID) >= 1) {
                msg.channel.sendSuccess("Okay, am now watching ${it.name}")
            } else {
                msg.channel.sendError("Am already watching ${it.name}")
            }
            return
        }
        msg.channel.sendError("That is not a Categorie")
    }

    @DiscordSubCommand(name = "remove", parent = "voice")
    fun voiceRemove(msg: IMessage, args: Array<String>) {
        val categoryID = args[2].toLong()
        msg.guild.getCategoryByID(categoryID)?.let {
            if (delWatchedForGuild(it.guild.longID, it.longID) >= 1) {
                msg.channel.sendSuccess("Okay, am no longer watching ${it.name}")
            } else {
                msg.channel.sendError("Was never watching ${it.name}")
            }
            return
        }
        msg.channel.sendError("That is not a Categorie!")
    }
}

fun someoneJoined(event: UserVoiceChannelJoinEvent) {
    MoreVoiceChannel.logger.trace("someoneJoined was called!")
    if (event.voiceChannel.category == null) return
    if (isWached(event.guild.longID, event.voiceChannel.category.longID) >= 1) {
        checkCategorieForRoom(event.voiceChannel.category)
    }
}

fun someoneMoved(event: UserVoiceChannelMoveEvent) {
    MoreVoiceChannel.logger.trace("someoneMoved was called!")
    if (event.oldChannel.category != null && isWached(event.guild.longID, event.oldChannel.category.longID) >= 1) {
        checkCategorieForEmptyRooms(event.oldChannel.category)
    }

    if (event.newChannel.category == null) return
    if (isWached(event.guild.longID, event.newChannel.category.longID) >= 1) {
        checkCategorieForRoom(event.newChannel.category)
    }
}

fun someoneLeft(event: UserVoiceChannelLeaveEvent) {
    MoreVoiceChannel.logger.trace("someoneLeft was called!")
    if (event.voiceChannel.category == null) return
    if (isWached(event.guild.longID, event.voiceChannel.category.longID) >= 1) {
        checkCategorieForEmptyRooms(event.voiceChannel.category)
    }
}

fun checkCategorieForRoom(category: ICategory) {
    MoreVoiceChannel.logger.trace("checkCategoriForRoom was called!")
    if (!category.guild.checkOurPermissions(Permissions.MANAGE_CHANNELS)) {
        return
    }
    //If there isn't an empty voice room anymore create one
    if (category.voiceChannels.none { it.connectedUsers.isEmpty() }) {
        category.createVoiceChannel(category.name)
    }
}

fun checkCategorieForEmptyRooms(category: ICategory) {
    category.guild.checkOurPermissions(Permissions.MANAGE_CHANNELS)
    category.voiceChannels.filter { it.connectedUsers.isEmpty() }.dropLast(1).forEach { it.delete() }
}