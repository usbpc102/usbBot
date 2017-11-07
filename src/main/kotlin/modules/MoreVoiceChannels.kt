package modules

import commands.DiscordCommands
import commands.core.Command
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent
import sx.blah.discord.handle.obj.ICategory
import sx.blah.discord.handle.obj.IMessage
import util.MessageSending
import util.commands.AnnotationExtractor
import util.commands.DiscordCommand
import util.commands.DiscordSubCommand

class MoreVoiceChannel : DiscordCommands {
    override fun getDiscordCommands(): MutableCollection<Command> {
        return AnnotationExtractor.getCommandList(this)
    }

    @DiscordCommand("voice")
    fun voice(msg: IMessage, args: Array<String>) : Int {
        return 0
    }

    @DiscordSubCommand(name = "add", parent = "voice")
    fun voiceAdd(msg: IMessage, args: Array<String>) {
        val categoryID = args[2].toLong()
        msg.guild.getCategoryByID(categoryID)?.let {
            val blah = config.addWatched(it)
            MessageSending.sendMessage(msg.channel, "That got me SQL error: " + blah)
            return
        }
        MessageSending.sendMessage(msg.channel, "That is not a Categorie!")
    }

    @DiscordSubCommand(name = "remove", parent = "voice")
    fun voiceRemove(msg: IMessage, args: Array<String>) {

    }
}

fun someoneJoined(event: UserVoiceChannelJoinEvent) {
    if (config.isWatched(event.voiceChannel.category)) {
        checkCategorieForRoom(event.voiceChannel.category)
    }
}

fun someoneMoved(event: UserVoiceChannelMoveEvent) {
    if (config.isWatched(event.oldChannel.category)) {
        checkCategorieForEmptyRooms(event.oldChannel.category)
    } else if (config.isWatched(event.newChannel.category)) {
        checkCategorieForRoom(event.newChannel.category)
    }
}

fun someoneLeft(event: UserVoiceChannelLeaveEvent) {
    if (config.isWatched(event.voiceChannel.category)) {
        checkCategorieForEmptyRooms(event.voiceChannel.category)
    }
}

fun checkCategorieForRoom(category: ICategory) {
    //If there isn't an empty voice room anymore create one
    if (category.voiceChannels.none { it.connectedUsers.isEmpty() }) {
        category.createVoiceChannel(category.name)
    }
}

fun checkCategorieForEmptyRooms(category: ICategory) {
    category.voiceChannels.filter { it.connectedUsers.isEmpty() }.dropLast(1).forEach { it.delete() }
}