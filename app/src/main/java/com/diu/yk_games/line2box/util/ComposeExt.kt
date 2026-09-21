package com.diu.yk_games.line2box.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


enum class ButtonState { Pressed, Idle }

fun Modifier.bounceClick(
    keepRipple: Boolean = true,
    onClick: () -> Unit = {}
) = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = if (keepRipple) LocalIndication.current else null, // Conditional ripple
        onClick = singleClick(onClick)
    )
}.bounceOnClick()

fun Modifier.bounceOnClick(pass: PointerEventPass = PointerEventPass.Main) = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(
        if (buttonState == ButtonState.Pressed) 0.95f else 1f,
        label = ""
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(buttonState) {
        awaitPointerEventScope {
            buttonState = if (buttonState == ButtonState.Pressed) {
                waitForUpOrCancellation(pass)
                ButtonState.Idle
            } else {
                awaitFirstDown(false, pass)
                ButtonState.Pressed
            }
        }
    }
}

fun Modifier.forceClickable(
    showRipple: Boolean = true,
    shape: Shape = RectangleShape,
    requireUnconsumed: Boolean = false,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val singleClick = singleClick(onClick)
    minimumInteractiveComponentSize()
        .clip(shape)
        .indication(
            interactionSource = interactionSource,
            indication = if (showRipple) ripple() else null
        )
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(
                        requireUnconsumed = requireUnconsumed,
                        pass = PointerEventPass.Final
                    )
                    down.consume()

                    // Emit press for ripple
                    scope.launch {
                        interactionSource.emit(
                            PressInteraction.Press(down.position)
                        )
                    }

                    val up = waitForUpOrCancellation(
                        pass = PointerEventPass.Initial
                    )

                    // Emit release for ripple
                    scope.launch {
                        interactionSource.emit(
                            PressInteraction.Release(PressInteraction.Press(down.position))
                        )
                    }

                    if (up != null && up.id == down.id) {
                        singleClick()
                    }
                }
            }
        }
}

@OptIn(ExperimentalTime::class)
@Composable
fun singleClick(
    onClick: () -> Unit,
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return {
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastClickTime >= 500L) {
            lastClickTime = now
            onClick()
        }
    }
}

fun Modifier.singleClickable(
    onClick: () -> Unit,
): Modifier = composed {
    this.clickable(onClick = singleClick(onClick))
}

var TextFieldState.value: String
    get() = text.toString()
    set(value) = edit {
        replace(0, length, value)
    }

/**
 * Replaces the partial word currently being typed at the cursor position
 * matching the given [prefix] (e.g. "/" or "--") with the [replacement].
 */
fun TextFieldState.insertOrReplaceToken(prefix: String, replacement: String) {
    edit {
        val currentText = asCharSequence().toString()
        val cursorIndex = selection.end

        // Take text up to current cursor position
        val textBeforeCursor = currentText.take(cursorIndex)
        val tokenStartIndex = textBeforeCursor.lastIndexOf(prefix)

        val textToInsert = "$replacement "

        if (tokenStartIndex != -1) {
            // Replace from prefix index to current cursor position
            replace(tokenStartIndex, cursorIndex, textToInsert)
        } else {
            // Fallback: Insert directly at cursor position
            insert(cursorIndex, textToInsert)
        }

        // Move cursor cleanly to the end of the field
        placeCursorAtEnd()
    }
}
