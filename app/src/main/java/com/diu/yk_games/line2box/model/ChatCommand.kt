package com.diu.yk_games.line2box.model

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
            nmData = "Bot",
            lvlData = "67",
        )

        fun find(command: String?) = entries.find { it.command == command }

        fun isCommand(command: String?) = entries.any { it.command == command }
    }
}