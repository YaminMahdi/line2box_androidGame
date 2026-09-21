package com.diu.yk_games.line2box.model

enum class ChatFlag(val flag: String) {
    Silent("-silent"),
    Count("-count");

    override fun toString() = flag

    companion object {
        fun find(text: String) = ChatFlag.entries.find { text.contains(it.flag) }
        fun findAll(text: String): List<ChatFlag> {
            return ChatFlag.entries.filter { text.contains(it.flag) }
        }
    }
}