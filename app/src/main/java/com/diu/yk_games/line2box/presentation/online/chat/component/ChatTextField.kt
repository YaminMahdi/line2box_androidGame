package com.diu.yk_games.line2box.presentation.online.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.util.bounceOnClick


@Composable
fun ChatInputBar(
    state: TextFieldState,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(barBrush),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatTextField(
            state = state,
            onSend = onSend,
            modifier = modifier
                .weight(1f)
                .padding(start = 5.dp, top = 5.dp, bottom = 5.dp)
                .focusRequester(focusRequester)
        )
        Box(
            contentAlignment = Center,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .bounceOnClick()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFE34816), Color(0xFFFF6614))))
                .clickable {
                    onSend()
                }
                .border(2.dp, colorResource(R.color.orangeY), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send message",
                tint = Color(0xFFE0D89F)
            )
        }
    }
}


@Composable
fun ChatTextField(
    state: TextFieldState,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        state = state,
        lineLimits = SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = Send),
        onKeyboardAction = { onSend() },
        outputTransformation = CommandHighlighterTransformation(
            defaultTextColor = MaterialTheme.colorScheme.onBackground,
            validCommandColor = colorResource(R.color.color_match_action)
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Unspecified, // Required so SpanStyle colors work!
            fontSize = 16.sp,
            lineHeight = 18.sp
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.type_here),
                color = MaterialTheme.colorScheme.outline,
                fontSize = 16.sp,
                lineHeight = 18.sp
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier
    )
}