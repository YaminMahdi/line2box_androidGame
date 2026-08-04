package com.diu.yk_games.line2box.presentation.online

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.typeEnum
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.toDateTime

@get:Composable
private val cocX get() = colorResource(R.color.cocX)

@get:Composable
private val cocXx get() = colorResource(R.color.cocXx)

@get:Composable
private val outline get() = colorResource(R.color.cocZ)

@get:Composable
private val fieldBg get() = cocXx.copy(.6f)
private val barBrush =
    Brush.verticalGradient(listOf(Color(0xFF7B775C), Color(0xFF9A9465), Color(0xFF706C4F)))

/**
 * Stateless chat UI. All screen state and business actions are hoisted to the caller.
 */
@Composable
fun ChatScreen(
    messages: List<MsgStore>,
    playerId: String,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    showEmoji: Boolean,
    emojiEnabled: Boolean,
    onEmoji: (String) -> Unit,
    onMessageClick: (MsgStore) -> Unit,
    onMessageLongClick: (MsgStore) -> Unit,
    onCopyGameId: (String) -> Unit,
    onJoin: (MsgStore) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(messages.firstOrNull()?.key) {
        if (messages.isNotEmpty()) state.scrollToItem(0)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cocX)
            .imePadding()
    ) {
        LazyColumn(
            state = state,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom),
            modifier = Modifier.weight(1f)
        ) {
            items(messages) { msg ->
                MessageRow(
                    msg = msg,
                    own = msg.playerId == playerId,
                    click = { onMessageClick(msg) },
                    longClick = { onMessageLongClick(msg) },
                    copy = { onCopyGameId(msg.gameId) },
                    join = { onJoin(msg) }
                )
            }
        }
        if (showEmoji)
            EmojiBar(enabled = emojiEnabled, onEmoji = onEmoji)
        InputBar(
            value = text,
            changed = onTextChange,
            send = onSend,
            modifier = Modifier.focusRequester(focus)
        )
    }
}

@Composable
private fun MessageRow(
    msg: MsgStore,
    own: Boolean,
    click: () -> Unit,
    longClick: () -> Unit,
    copy: () -> Unit,
    join: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = click, onLongClick = longClick)
            .background(cocXx)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Row {
            Text(
                text = msg.nmData,
                color = colorResource(R.color.cocZz),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "lvl.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
            Text(
                text = msg.lvlData,
                color = colorResource(R.color.greenY),
                fontSize = 9.sp,
                lineHeight = 10.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = msg.time.toDateTime(),
                color = colorResource(R.color.cocZ),
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
        Text(
            text = if (msg.type.typeEnum == MsgStore.Type.Invitation) msg.msgData.substringBefore("Match ID")
                .trim() else msg.msgData,
            color = when (msg.type.typeEnum) {
                MsgStore.Type.EnterText -> colorResource(R.color.color_match_action)
                MsgStore.Type.ExitText -> colorResource(R.color.color_left_match)
                else -> colorResource(R.color.whiteY)
            },
            fontSize = 17.sp,
            lineHeight = 22.sp
        )
        if (msg.type.typeEnum == MsgStore.Type.Invitation) {
            InvitationActions(
                id = msg.gameId,
                enabled = !own,
                copy = copy,
                join = join
            )
        }
    }
}

@Composable
private fun InvitationActions(id: String, enabled: Boolean, copy: () -> Unit, join: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp), verticalAlignment = Alignment.Bottom
    ) {
        Surface(color = colorResource(R.color.whiteX), shape = RoundedCornerShape(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    id.take(4),
                    color = colorResource(R.color.whiteY),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
                IconButton(onClick = copy) {
                    Icon(
                        painterResource(R.drawable.icon_copy),
                        "Copy game ID",
                        tint = colorResource(R.color.cocZz),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = join,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.greenY)),
            modifier = Modifier.height(36.dp)
        ) {
            Text(stringResource(R.string.join), fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmojiBar(enabled: Boolean, onEmoji: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(75.dp)
            .background(barBrush),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("🤣", "😭", "😱", "😘", "🥱").forEach { emoji ->
            Button(
                onClick = { onEmoji(emoji) }, enabled = enabled, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
            ) { Text(emoji, fontSize = 35.sp) }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    changed: (String) -> Unit,
    send: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(barBrush),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = changed,
            singleLine = true,
            placeholder = {
                Text(
                    stringResource(R.string.type_here),
                    color = colorResource(R.color.cocZ),
                    fontSize = 16.sp,
                    lineHeight = 18.sp
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colorResource(R.color.cocZz),
                unfocusedTextColor = colorResource(R.color.cocZz),
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg,
                focusedBorderColor = outline,
                unfocusedBorderColor = outline,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 18.sp
            ),
            shape = RoundedCornerShape(15.dp),
            modifier = modifier
                .weight(1f)
                .padding(start = 5.dp, top = 5.dp, bottom = 5.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFE34816), Color(0xFFFF6614))))
                .clickable {
                    send()
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
fun ProfileDialog(profile: GameProfile, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = cocXx.copy(.9f),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(2.dp, outline),
            modifier = Modifier
                .padding(end = 80.dp)
                .fillMaxWidth(.8f)
        ) {
            ProfileContent(profile)
        }
    }
}

@Composable
private fun ProfileContent(profile: GameProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.profile),
            color = colorResource(R.color.greenY),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))

        // Name
        Row {
            Text(
                text = profile.nm,
                color = colorResource(R.color.cocZz),
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "lvl.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = profile.lvlByCal().toString(),
                color = colorResource(R.color.greenY),
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
        }
        // country
        Text(
            text = "${profile.countryNm} ${profile.countryEmoji}",
            color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
            fontSize = 16.sp,
            lineHeight = 18.sp
        )

        // Coin pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(0.08f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_trophy),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = profile.coin.toString(),
                color = colorResource(R.color.cocZz),
                fontSize = 22.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onBackground.copy(0.06f)
            )
        ) {
            ProfileStat(
                icon = Icons.Rounded.SportsEsports,
                label = stringResource(R.string.match_played),
                value = profile.matchPlayed
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(0.08f))
            ProfileStat(
                icon = Icons.Rounded.EmojiEvents,
                label = stringResource(R.string.match_won),
                value = profile.matchWinMulti
            )
        }
    }
}

@Composable
private fun ProfileStat(icon: ImageVector, label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(R.color.greenY),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground.copy(0.85f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF444540)
@Composable
private fun ChatPreview() = Line2BoxTheme {
    ChatScreen(
        messages = listOf(
            MsgStore(
                key = "2",
                nmData = "Player 2",
                msgData = "Join Match ID",
                type = MsgStore.Type.Invitation.name,
                gameId = "ABCD"
            ),
            MsgStore(key = "1", nmData = "Player 1", msgData = "Hello!"),
        ),
        playerId = "me",
        text = "",
        onTextChange = {},
        onSend = {},
        showEmoji = true,
        emojiEnabled = true,
        onEmoji = {},
        onMessageClick = {},
        onMessageLongClick = {},
        onCopyGameId = {},
        onJoin = {},
    )
}
