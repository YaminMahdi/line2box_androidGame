package com.diu.yk_games.line2box.presentation.online.chat

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.ChatMode
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.MainViewModel
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.presentation.component.ProfileDialog
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.ui.theme.Line2BoxChatTheme
import com.diu.yk_games.line2box.util.*
import io.ak1.BubbleTabBar
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class ChatFragment : Fragment() {
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var parentActivity: Activity

    private val drawerLayout: DrawerLayout by lazy {
        parentActivity.findViewById(R.id.drawer_layout)
    }
    private val bubbleTabBar: BubbleTabBar by lazy {
        parentActivity.findViewById(R.id.bubbleTabBar)
    }
    private val emojiPlay: ImageView by lazy {
        parentActivity.findViewById(R.id.emojiPlay)
    }

    private val mode: ChatMode by lazy {
        arguments?.getString(ARG_MODE)?.let { ChatMode.valueOf(it) } ?: ChatMode.GLOBAL
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        parentActivity = requireActivity()
        Line2BoxChatTheme {
            ChatRoute(
                mode = mode,
                toggleDrawerState = { isOpen ->
                    if (isOpen)
                        drawerLayout.openDrawer(GravityCompat.START)
                    else
                        drawerLayout.closeDrawer(GravityCompat.START)
                },
                onNavigate = ::navigateSafe,
                onPlayEmoji = { drawable, sound ->
                    viewModel.player.playSound(sound)
                    emojiPlay.loadDrawable(drawable)
                    emojiPlay.show()
                },
                onHideEmoji = { emojiPlay.gone() },
            )
        }
    }

    @Composable
    internal fun ChatRoute(
        mode: ChatMode,
        toggleDrawerState: (isOpen: Boolean) -> Unit,
        onNavigate: (Routes.GameOnline) -> Unit,
        onPlayEmoji: (drawableRes: Int, rawRes: Int) -> Unit,
        onHideEmoji: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        val messages by (if (mode == ChatMode.FRIENDLY) viewModel.friendlyChatList else viewModel.globalChatList)
            .collectAsStateWithLifecycle()
        val fieldState = rememberTextFieldState()
        var profile by remember { mutableStateOf<GameProfile?>(null) }
        var emojiEnabled by remember { mutableStateOf(true) }
        var lastKey by remember { mutableStateOf("") }

        fun send() {
            if (mode.isGlobal)
                viewModel.player.playPopSound()
            val sent = viewModel.sendMessage(
                text = fieldState.value,
                chatMode = mode
            )
            if (sent != null)
                fieldState.value = ""
            else
                DynamicIslandController.message("Write Something..")
        }

        LaunchedEffect(messages.firstOrNull()?.key, mode) {
            if (mode != ChatMode.FRIENDLY) return@LaunchedEffect
            val newest = messages.firstOrNull() ?: return@LaunchedEffect
            if (newest.key == lastKey) return@LaunchedEffect

            lastKey = newest.key

            val emoji = when (newest.msgData) {
                "🤣" -> R.drawable.emoji_haha to R.raw.haha
                "😭" -> R.drawable.emoji_cry to R.raw.cry
                "😱" -> R.drawable.emoji_scream to R.raw.scream
                "😘" -> R.drawable.emoji_kiss to R.raw.kiss
                "🥱" -> R.drawable.emoji_yawn to R.raw.yawn
                else -> null
            }

            if (emoji != null) {
                scope.launch {
                    emojiEnabled = false
                    viewModel.setNewMsgBoltVisible(false)
                    toggleDrawerState(false)
                    onPlayEmoji(emoji.first, emoji.second)

                    withContext(NonCancellable) { delay(2500.milliseconds) }
                    onHideEmoji()
                    emojiEnabled = true
                }
            } else if (newest.playerId != viewModel.playerId) {
                viewModel.player.playPopSound()
                viewModel.setNewMsgBoltVisible(true)
                DynamicIslandController.message(
                    name = newest.nmData,
                    text = newest.msgData,
                    onClick = { toggleDrawerState(true) }
                )
            }
        }

        ChatScreen(
            mode = mode,
            messages = messages,
            fieldState = fieldState,
            focusRequester = focusRequester,
            showEmoji = mode == ChatMode.FRIENDLY,
            emojiEnabled = emojiEnabled,
            onEmoji = {
                viewModel.sendMessage(
                    text = it,
                    chatMode = mode
                )
                viewModel.ignoreDrawerClosesSound = true
                toggleDrawerState(false)
            },
            onMessageClick = onClick@{ msg ->
                if (msg.playerId.isEmpty()) return@onClick
                viewModel.player.playButtonClickSound()
                viewModel.gamerProfileRef.document(msg.playerId).get()
                    .addOnSuccessListener { profile = it.toObjectOrNull<GameProfile>() }
            },
            onSend = ::send,
            onCommand = { selectedCommand ->
                fieldState.insertOrReplaceToken(
                    prefix = "/",
                    replacement = selectedCommand.command
                )
                focusRequester.requestFocus()
            },
            onFlag = { selectedFlag ->
                fieldState.insertOrReplaceToken(
                    prefix = "-",
                    replacement = selectedFlag.flag
                )
                focusRequester.requestFocus()
            },
            onCopy = { context.setClipBoardData(it, "Copied!") },
            onJoin = { msg ->
                if (msg.gameId.isEmpty())
                    DynamicIslandController.message("Invalid ID")
                else viewModel.getJoinRoute(msg)
                    .onSuccess {
                        bubbleTabBar.setSelected(1, true)
                        toggleDrawerState(false)
                        onNavigate(it)
                    }
                    .onFailure { DynamicIslandController.message(it.message.toString()) }
            },
            modifier = modifier
        )
        profile?.let {
            ProfileDialog(
                profile = it,
                onCopy = { context.setClipBoardData(it.toString(), "Copied!") },
                onDismiss = { profile = null }
            )
        }
    }

    companion object {
        private const val ARG_MODE = "chat_mode"

        fun newInstance(mode: ChatMode): ChatFragment = ChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MODE, mode.name)
            }
        }
    }
}
