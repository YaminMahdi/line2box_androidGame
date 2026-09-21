package com.diu.yk_games.line2box.model

enum class PlayerColor(val ref: String) {
    Red("plyr1"),
    Blue("plyr2");

    val isRed get() = this == Red
}