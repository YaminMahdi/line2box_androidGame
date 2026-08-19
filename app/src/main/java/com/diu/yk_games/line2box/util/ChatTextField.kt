package com.diu.yk_games.line2box.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R

@Composable
fun ChatTextField(
    state: TextFieldState,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        onKeyboardAction = { onSend() },
        outputTransformation = CommandHighlighterTransformation(
            defaultTextColor = MaterialTheme.colorScheme.onBackground,
            validCommandColor = colorResource(R.color.color_match_action),
            validFlagColor = colorResource(R.color.orangeY),
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Color.Unspecified, // Required so SpanStyle colors work!
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