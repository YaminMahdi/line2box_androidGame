package com.diu.yk_games.line2box.model

import com.diu.yk_games.line2box.model.MsgStore.MessageType

enum class ChatCommand(val command: String, val isVisible: Boolean = true) {
    DeleteLast("/delete_last"),
    ClearAll("/clear_all"),
    LastUser("/last_user");

    override fun toString() = command

    companion object {
        val visibleEntries
            get() = entries.filter { it.isVisible }

        val bot = MsgStore(
            playerId = "bot",
            time = System.currentTimeMillis(),
            type = MessageType.Bot.name,
            nmData = "Bot",
            lvlData = "67",
        )

        fun find(text: String) = entries.find { text.contains(it.command) }

        fun hasCommand(text: String) = find(text) != null
    }
}