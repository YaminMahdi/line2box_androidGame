package com.diu.yk_games.line2box.util

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.diu.yk_games.line2box.model.ChatCommand
import com.diu.yk_games.line2box.model.ChatFlag

private val COMMAND_REGEX = Regex("""(/\S+|--\S+)""")
private val VALID_COMMANDS = ChatCommand.entries.map { it.command }.toSet()
private val VALID_FLAGS = ChatFlag.entries.map { it.flag }.toSet()

/**
 * Computes the (start, end, style) spans for command/flag highlighting
 * over [text]. Shared by both the live-input transformation and the
 * static AnnotatedString builder below.
 */
private fun highlightSpans(
    text: CharSequence,
    validCommandColor: Color,
    validFlagColor: Color,
    invalidColor: Color,
): List<Triple<Int, Int, SpanStyle>> =
    COMMAND_REGEX.findAll(text).mapNotNull { match ->
        val word = match.value
        val (isValid, color) = when {
            word.startsWith("/") -> {
                val valid = VALID_COMMANDS.any { word.startsWith(it.take(word.length)) }
                valid to (if (valid) validCommandColor else invalidColor)
            }
            word.startsWith("--") -> {
                val valid = VALID_FLAGS.contains(word)
                valid to (if (valid) validFlagColor else invalidColor)
            }
            else -> return@mapNotNull null
        }
        Triple(
            match.range.first,
            match.range.last + 1,
            SpanStyle(color = color, fontWeight = if (isValid) FontWeight.Bold else FontWeight.Normal)
        )
    }.toList()

class CommandHighlighterTransformation(
    private val defaultTextColor: Color = Color.White,
    private val validCommandColor: Color = Color(0xFF7C4DFF),
    private val validFlagColor: Color = Color(0xFF4CAF50),
    private val invalidColor: Color = Color(0xFFFF5252),
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        if (length == 0) return

        addStyle(SpanStyle(color = defaultTextColor, fontWeight = FontWeight.Normal), 0, length)

        highlightSpans(asCharSequence(), validCommandColor, validFlagColor, invalidColor)
            .forEach { (start, end, style) -> addStyle(style, start, end) }
    }
}

fun String.toHighlightedCommandText(
    defaultTextColor: Color = Color.White,
    validCommandColor: Color = Color(0xFF7C4DFF),
    validFlagColor: Color = Color(0xFF4CAF50),
    invalidColor: Color = Color(0xFFFF5252),
): AnnotatedString = buildAnnotatedString {
    if (this@toHighlightedCommandText.isEmpty()) return@buildAnnotatedString

    append(this@toHighlightedCommandText)
    addStyle(SpanStyle(color = defaultTextColor, fontWeight = FontWeight.Normal), 0, length)

    highlightSpans(this@toHighlightedCommandText, validCommandColor, validFlagColor, invalidColor)
        .forEach { (start, end, style) -> addStyle(style, start, end) }
}