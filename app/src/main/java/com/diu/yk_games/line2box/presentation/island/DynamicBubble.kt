package com.diu.yk_games.line2box.presentation.island

import java.util.concurrent.atomic.AtomicLong

private val idCounter = AtomicLong()
private fun nextId(): Long = idCounter.incrementAndGet()

sealed interface DynamicBubble {
    val id: Long

    data class Loading(
        val label: String = "Working",
        override val id: Long = nextId(),
    ) : DynamicBubble

    data class Message(
        val text: String,
        val name: String = "Line2Box",
        val onClick: () -> Unit = {},
        override val id: Long = nextId(),
    ) : DynamicBubble
}