package com.diu.yk_games.line2box.presentation.online.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.*
import com.diu.yk_games.line2box.presentation.online.chat.component.ChatInputBar
import com.diu.yk_games.line2box.presentation.online.chat.component.EmojiBar
import com.diu.yk_games.line2box.presentation.online.chat.component.MessageRow
import com.diu.yk_games.line2box.presentation.online.chat.component.getCommandSuggestions
import com.diu.yk_games.line2box.ui.theme.Line2BoxChatTheme
import com.diu.yk_games.line2box.util.value

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
        fieldState.value.getCommandSuggestions()

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
                        contentAlignment = Center,
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
                    .align(BottomCenter)
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
        ChatInputBar(
            state = fieldState,
            focusRequester = focusRequester,
            onSend = onSend,
            modifier = Modifier.focusRequester(focus)
        )
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

@Preview(showBackground = true, backgroundColor = 0xFF444540)
@Composable
private fun ChatPreview() = Line2BoxChatTheme {
    val fieldState = TextFieldState()
    ChatScreen(
        mode = GLOBAL,
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
        focusRequester = remember { FocusRequester() },
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
