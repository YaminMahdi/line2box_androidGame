package com.diu.yk_games.line2box.model

sealed class DynamicBubble {
    data object Idle : DynamicBubble()
    data object Loading : DynamicBubble()
    data class Message(val name: String, val text: String) : DynamicBubble()
}