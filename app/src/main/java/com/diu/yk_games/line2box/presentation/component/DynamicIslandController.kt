package com.diu.yk_games.line2box.presentation.component

import com.diu.yk_games.line2box.model.DynamicBubble
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Process-wide island state, so any fragment can drive it without knowing the host.
 *
 *   DynamicIslandController.loading()
 *   DynamicIslandController.message("Saved to your box")
 */
object DynamicIslandController {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var dismissJob: Job? = null

    val state: StateFlow<DynamicBubble>
        field = MutableStateFlow<DynamicBubble>(DynamicBubble.Idle)

    val isLoading
        get() = state.value == DynamicBubble.Loading

    fun idle() = set(Idle)

    fun loading() = set(Loading)

    /** @param autoDismissMillis 0 keeps the message up until something replaces it. */
    fun message(name: String, text: String?, autoDismissMillis: Long = 3500L) {
        if (text.isNullOrBlank()) return
        set(DynamicBubble.Message(name.ifBlank { "Line2Box" }, text))
        if (autoDismissMillis > 0) {
            dismissJob = scope.launch {
                delay(autoDismissMillis.milliseconds)
                state.value = Idle
            }
        }
    }

    private fun set(bubble: DynamicBubble) {
        dismissJob?.cancel()
        dismissJob = null
        state.value = bubble
    }
}
