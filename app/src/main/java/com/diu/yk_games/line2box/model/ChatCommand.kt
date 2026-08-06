package com.diu.yk_games.line2box.model

enum class ChatCommand(val command: String,val isVisible: Boolean = true) {
    ClearLastMsg("/delete_last"),
    ClearAllMsg("/clear_all", false),
    LastUser("/last_user");

    companion object {
        val visibleEntries
            get() = entries.filter { it.isVisible }

        fun find(command: String?) = entries.find { it.command == command }

        fun isCommand(command: String?) = entries.any { it.command == command }
    }
}