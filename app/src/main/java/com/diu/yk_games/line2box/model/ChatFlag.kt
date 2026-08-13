package com.diu.yk_games.line2box.model

enum class ChatFlag(val flag: String) {
    Silent("--silent"),
    Count("--count");

    override fun toString() = flag
}