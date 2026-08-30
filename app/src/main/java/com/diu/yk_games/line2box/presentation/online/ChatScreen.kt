package com.diu.yk_games.line2box.presentation.online

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.ui.theme.Line2BoxChatTheme
import com.diu.yk_games.line2box.util.*

private val barBrush =
    Brush.verticalGradient(listOf(Color(0xFF7B775C), Color(0xFF9A9465), Color(0xFF706C4F)))

/**
 * Stateless chat UI. All screen state and business actions are hoisted to the caller.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(
    mode: ChatMode,
    messages: List<MsgStore>,
    fieldState: TextFieldState,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    onCommand: (ChatCommand) -> Unit,
    onFlag: (ChatFlag) -> Unit,
    showEmoji: Boolean,
    emojiEnabled: Boolean,
    onEmoji: (String) -> Unit,
    onMessageClick: (MsgStore) -> Unit,
    onCopy: (String) -> Unit,
    onJoin: (MsgStore) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val (commands, flags) =
        getSuggestions(text = fieldState.value)

    LaunchedEffect(messages.firstOrNull()?.key) {
        if (messages.isNotEmpty()) state.scrollToItem(0)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
//            .imePadding()
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                state = state,
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom),
                modifier = Modifier.fillMaxSize()
            ) {
                if (messages.isEmpty()) item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillParentMaxSize()
                    ) {
                        if (mode.isGlobal) ContainedLoadingIndicator(
                            modifier = Modifier.padding(top = 100.dp)
                        ) else Text(
                            text = stringResource(R.string.not_in_match),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                items(
                    items = messages,
                    key = { it.key },
                    contentType = { it.type }
                ) { msg ->
                    MessageRow(
                        msg = msg,
                        onClick = { onMessageClick(msg) },
                        onCopy = onCopy,
                        onJoin = { onJoin(msg) }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                AnimatedVisibility(commands.isNotEmpty()) {
                    CommandCard(
                        commands = commands,
                        onClick = onCommand
                    )
                }
                AnimatedVisibility(flags.isNotEmpty()) {
                    CommandCard(
                        commands = flags,
                        onClick = onFlag
                    )
                }
            }
        }
        if (showEmoji)
            EmojiBar(enabled = emojiEnabled, onEmoji = onEmoji)
        InputBar(
            fieldState = fieldState,
            focusRequester = focusRequester,
            onSend = onSend,
            modifier = Modifier.focusRequester(focus)
        )
    }
}

@Composable
private fun MessageRow(
    msg: MsgStore,
    onClick: () -> Unit,
    onCopy: (String) -> Unit,
    onJoin: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onCopy(msg.msgData ?: msg.playerId) })
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Row {
            Text(
                text = msg.nmData,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.outline,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
        msg.msgData?.let {
            if (msg.type.typeEnum == MsgStore.MessageType.Command || ChatCommand.hasCommand(it)) Text(
                text = it.toHighlightedCommandText(
                    defaultTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    validCommandColor = colorResource(R.color.color_match_action),
                    validFlagColor = colorResource(R.color.orangeY),
                ),
                fontSize = 17.sp,
                lineHeight = 22.sp
            ) else Text(
                text = if (msg.type.typeEnum == MsgStore.MessageType.Invitation)
                    msg.msgData.substringBefore("Match ID").trim()
                else
                    msg.msgData,
                color = when (msg.type.typeEnum) {
                    MsgStore.MessageType.EnterText -> colorResource(R.color.color_match_action)
                    MsgStore.MessageType.ExitText -> colorResource(R.color.color_left_match)
                    else -> colorResource(R.color.whiteY)
                },
                fontSize = 17.sp,
                lineHeight = 22.sp
            )
        }
        when (msg.type.typeEnum) {
            MsgStore.MessageType.Invitation -> {
                InvitationActions(
                    id = msg.gameId,
                    onCopy = {
                        onCopy(msg.gameId)
                    },
                    onJoin = onJoin
                )
            }

            MsgStore.MessageType.Bot if msg.user != null -> UserInfoCard(msg.user)
            else -> Unit
        }
    }
}

@Composable
fun <T> CommandCard(
    commands: List<T>,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(10.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(.99f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            commands.forEach {
                Text(
                    text = it.toString(),
                    color = colorResource(R.color.color_match_action),
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onClick(it) })
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun UserInfoCard(user: GameProfile, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(0.08f))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // First row: Country, Name, Level, Coins
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            // Player Name
            Text(
                text = user.nm.ifEmpty { "Unknown Player" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Coins
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_trophy),
                    contentDescription = "Coins",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${user.coin}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        // Second row: City, Country, Matches
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // City, Country
            if (user.cityNm.isNotEmpty() && user.countryNm.isNotEmpty()) {
                Text(
                    text = "${user.cityNm}, ${user.countryNm} ${user.countryEmoji}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Match Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "⚔️",
                    fontSize = 11.sp
                )
                Text(
                    text = "${user.matchPlayed}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = "🥇",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = "${user.matchWinMulti}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        // Third row: Player ID and IP/Query
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Player ID
            if (user.playerId.isNotEmpty()) {
                Text(
                    text = "ID: ${user.playerId}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Query/IP display
            if (user.query.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    // Show IP indicator
                    Text(
                        text = "🌐",
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = user.query,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationActions(
    id: String,
    onCopy: () -> Unit,
    onJoin: () -> Unit
) {
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
                IconButton(onClick = onCopy) {
                    Icon(
                        painterResource(R.drawable.icon_copy),
                        "Copy game ID",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onJoin,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.greenY)),
            modifier = Modifier
                .height(36.dp)
                .bounceOnClick()
        ) {
            Text(
                text = stringResource(R.string.join),
                fontSize = 13.sp
            )
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
                onClick = { onEmoji(emoji) },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .bounceOnClick()
            ) { Text(emoji, fontSize = 35.sp) }
        }
    }
}

@Composable
private fun InputBar(
    fieldState: TextFieldState,
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
            state = fieldState,
            onSend = onSend,
            modifier = modifier
                .weight(1f)
                .padding(start = 5.dp, top = 5.dp, bottom = 5.dp)
                .focusRequester(focusRequester)
        )
        Box(
            contentAlignment = Alignment.Center,
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

fun getSuggestions(text: String): Pair<List<ChatCommand>, List<ChatFlag>> {
    // Split text into words and get the last word
    val words = text.split(' ')
    val lastWord = words.lastOrNull() ?: ""

    // Check if we're currently typing a command or flag
    val isTypingCommand = lastWord.startsWith("/")
    val isTypingFlag = lastWord.startsWith("--")

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

@Preview(showBackground = true, backgroundColor = 0xFF444540)
@Composable
private fun ChatPreview() = Line2BoxChatTheme {
    val fieldState = TextFieldState()
    ChatScreen(
        mode = ChatMode.GLOBAL,
        messages = listOf(
            MsgStore(
                key = "2",
                nmData = "Player 2",
                msgData = "Join Match ID",
                type = MsgStore.MessageType.Invitation.name,
                gameId = "ABCD"
            ),
            MsgStore(key = "3", nmData = "Player 1", msgData = "Hello!"),
            MsgStore(
                key = "4",
                nmData = "Player 2",
                msgData = ChatCommand.DeleteLast.command,
                type = MsgStore.MessageType.Command.name
            ),
            MsgStore(
                key = "5",
                nmData = "Bot",
                type = MsgStore.MessageType.Bot.name,
                user = GameProfile(
                    nm = "John Doe",
                    cityNm = "New York",
                    query = "192.168.1.100",
                    matchPlayed = 42,
                    matchWinMulti = 37,
                    coin = 1250,
                    lvl = 15,
                    playerId = "a_1070246872382803456",
                    countryEmoji = "🇺🇸",
                    countryNm = "United States"
                )
            ),
        ),
        fieldState = fieldState,
        focusRequester = FocusRequester(),
        onSend = {},
        onCommand = {},
        onFlag = {},
        showEmoji = true,
        emojiEnabled = true,
        onEmoji = {},
        onMessageClick = {},
        onCopy = {},
        onJoin = {}
    )
}
