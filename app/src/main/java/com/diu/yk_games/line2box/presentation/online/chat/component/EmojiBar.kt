package com.diu.yk_games.line2box.presentation.online.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.util.bounceOnClick

val barBrush =
    Brush.verticalGradient(listOf(Color(0xFF7B775C), Color(0xFF9A9465), Color(0xFF706C4F)))

@Composable
fun EmojiBar(enabled: Boolean, onEmoji: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(75.dp)
            .background(barBrush),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("🤣", "😭", "😱", "😘", "🥱").forEach { emoji ->
            Button(
                onClick = { onEmoji(emoji) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Transparent,
                    disabledContainerColor = Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .bounceOnClick()
            ) { Text(emoji, fontSize = 35.sp) }
        }
    }
}