package com.diu.yk_games.line2box.model

enum class ChatMode {
    FRIENDLY,
    GLOBAL;

    val isGlobal get() = this == GLOBAL
}