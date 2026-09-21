package com.diu.yk_games.line2box.presentation.online.chat.component

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.diu.yk_games.line2box.model.ChatCommand
import com.diu.yk_games.line2box.model.ChatFlag

private val COMMAND_REGEX = Regex("""(/\S+|-\S+)""")
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
            word.startsWith("-") -> {
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
    private val defaultTextColor: Color = White,
    private val validCommandColor: Color = Color(0xFF7C4DFF),
    private val validFlagColor: Color = validCommandColor.copy(.5f),
    private val invalidColor: Color = Color(0xFFFF5252),
) : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        if (length == 0) return

        addStyle(SpanStyle(color = defaultTextColor, fontWeight = Normal), 0, length)

        highlightSpans(asCharSequence(), validCommandColor, validFlagColor, invalidColor)
            .forEach { (start, end, style) -> addStyle(style, start, end) }
    }
}

fun String.toHighlightedCommandText(
    defaultTextColor: Color = White,
    validCommandColor: Color = Color(0xFF7C4DFF),
    validFlagColor: Color = validCommandColor.copy(.5f),
    invalidColor: Color = Color(0xFFFF5252),
): AnnotatedString = buildAnnotatedString {
    if (this@toHighlightedCommandText.isEmpty()) return@buildAnnotatedString

    append(this@toHighlightedCommandText)
    addStyle(SpanStyle(color = defaultTextColor, fontWeight = Normal), 0, length)

    highlightSpans(this@toHighlightedCommandText, validCommandColor, validFlagColor, invalidColor)
        .forEach { (start, end, style) -> addStyle(style, start, end) }
}



fun String.getCommandSuggestions(): Pair<List<ChatCommand>, List<ChatFlag>> {
    // Split text into words and get the last word
    val words = split(' ')
    val lastWord = words.lastOrNull() ?: ""

    // Check if we're currently typing a command or flag
    val isTypingCommand = lastWord.startsWith("/")
    val isTypingFlag = lastWord.startsWith("-")

    val commands = if (isTypingCommand) {
        // Get all visible commands, but exclude those that are already fully written
        ChatCommand.visibleEntries
            .filter { it.command.startsWith(lastWord) }
            .filterNot { it.command == lastWord } // Exclude if full command is already typed
    } else {
        listOf()
    }

    val flags = if (isTypingFlag) {
        // Get all flags, but exclude those that are already fully written
        ChatFlag.entries
            .filter { it.flag.startsWith(lastWord) }
            .filterNot { it.flag == lastWord } // Exclude if full flag is already typed
    } else {
        listOf()
    }

    return commands to flags
}