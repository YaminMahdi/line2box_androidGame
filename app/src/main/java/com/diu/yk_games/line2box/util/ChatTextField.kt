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
import com.diu.yk_games.line2box.ui.theme.cocXx
import com.diu.yk_games.line2box.ui.theme.cocZ
import com.diu.yk_games.line2box.ui.theme.cocZz
import com.diu.yk_games.line2box.ui.theme.outline

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
            defaultTextColor = cocZz,
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
                color = cocZ,
                fontSize = 16.sp,
                lineHeight = 18.sp
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Unspecified,   // Set to Unspecified
            unfocusedTextColor = Color.Unspecified, // Set to Unspecified
            focusedContainerColor = cocXx.copy(.6f),
            unfocusedContainerColor = cocXx.copy(.6f),
            focusedBorderColor = outline,
            unfocusedBorderColor = outline,
        ),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier
    )
}