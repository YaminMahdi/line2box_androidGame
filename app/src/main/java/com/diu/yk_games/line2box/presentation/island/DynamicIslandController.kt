package com.diu.yk_games.line2box.presentation.island

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Process-wide island stack. Bubbles coexist; each is dismissed by its own id.
 *
 *   val job = DynamicIslandController.loading()
 *   DynamicIslandController.message("Saved to your box")
 *   DynamicIslandController.dismiss(job)
 */
object DynamicIslandController {

    /** Oldest bubbles fall off the bottom once the stack is this deep. */
    private const val MAX_VISIBLE = 4

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val dismissJobs = mutableMapOf<Long, Job>()

    val state: StateFlow<List<DynamicBubble>>
        field = MutableStateFlow<List<DynamicBubble>>(emptyList())

    val isLoading: Boolean
        get() = state.value.any { it is DynamicBubble.Loading }

    /** @return the bubble id, so you can [dismiss] this exact spinner later. */
    fun loading(label: String = "Working"): Long {
        state.value.firstOrNull { it is DynamicBubble.Loading && it.label == label }
            ?.let { return it.id }                       // never stack duplicate spinners
        val bubble = DynamicBubble.Loading(label = label)
        push(bubble)
        return bubble.id
    }

    /** @param autoDismissMillis 0 keeps the message up until it's clicked or dismissed. */
    fun message(
        text: String?,
        name: String = "",
        autoDismissMillis: Long = 3500L,
        onClick: () -> Unit = {},
    ): Long? {
        if (text.isNullOrBlank()) return null
        val bubble = DynamicBubble.Message(
            text = text,
            name = name.ifBlank { "Line2Box" },
            onClick = onClick,
        )
        push(bubble)
        if (autoDismissMillis > 0) {
            dismissJobs[bubble.id] = scope.launch {
                delay(autoDismissMillis.milliseconds)
                dismiss(bubble.id)
            }
        }
        return bubble.id
    }

    /** Removes one bubble and leaves the rest of the stack alone. */
    fun dismiss(id: Long) = scope.launch {
        dismissJobs.remove(id)?.cancel()
        state.value = state.value.filterNot { it.id == id }
    }.let { }

    fun stopLoading() = scope.launch {
        state.value.filterIsInstance<DynamicBubble.Loading>().forEach { dismiss(it.id) }
    }.let { }

    /** Clears everything. */
    fun idle() = scope.launch {
        dismissJobs.values.forEach(Job::cancel)
        dismissJobs.clear()
        state.value = emptyList()
    }.let { }

    private fun push(bubble: DynamicBubble) {
        val next = state.value + bubble
        val overflow = next.size - MAX_VISIBLE
        state.value = if (overflow <= 0) next else {
            next.take(overflow).forEach { dismissJobs.remove(it.id)?.cancel() }
            next.drop(overflow)
        }
    }
}