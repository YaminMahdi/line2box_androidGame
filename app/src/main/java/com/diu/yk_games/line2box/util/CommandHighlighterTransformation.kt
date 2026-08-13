package com.diu.yk_games.line2box.util

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.diu.yk_games.line2box.model.ChatCommand
import com.diu.yk_games.line2box.model.ChatFlag

class CommandHighlighterTransformation(
    private val defaultTextColor: Color = Color.Unspecified,
    private val validCommandColor: Color = Color(0xFF7C4DFF),
    private val validFlagColor: Color = Color(0xFF4CAF50),
    private val invalidColor: Color = Color(0xFFFF5252),
) : OutputTransformation {

    private val commandRegex = Regex("""(/\S+|--\S+)""")
    private val validCommands = ChatCommand.entries.map { it.command }.toSet()
    private val validFlags = ChatFlag.entries.map { it.flag }.toSet()

    override fun TextFieldBuffer.transformOutput() {
        if (length == 0) return

        // 1. Set default color & weight across the entire text buffer first
        addStyle(
            spanStyle = SpanStyle(
                color = defaultTextColor,
                fontWeight = FontWeight.Normal
            ),
            start = 0,
            end = length
        )

        // 2. Iterate through matches to override colors for slash commands and flags
        val currentText = asCharSequence().toString()

        commandRegex.findAll(currentText).forEach { match ->
            val word = match.value

            when {
                word.startsWith("/") -> {
                    val isValid = validCommands.any { word.startsWith(it.take(word.length)) }
                    addStyle(
                        spanStyle = SpanStyle(
                            color = if (isValid) validCommandColor else invalidColor,
                            fontWeight = if (isValid) FontWeight.Bold else FontWeight.Normal
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
                word.startsWith("--") -> {
                    val isValid = validFlags.contains(word)
                    addStyle(
                        spanStyle = SpanStyle(
                            color = if (isValid) validFlagColor else invalidColor,
                            fontWeight = if (isValid) FontWeight.Bold else FontWeight.Normal
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
            }
        }
    }
}