package com.diu.yk_games.line2box.presentation.online

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.diu.yk_games.line2box.presentation.navigation.Routes
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class ChatFragment : Fragment() {
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var parentActivity: Activity

    private val drawerLayout: DrawerLayout by lazy {
        parentActivity.findViewById(R.id.drawer_layout)
    }

    private val mode: ChatMode by lazy {
        arguments?.getString(ARG_MODE)?.let { ChatMode.valueOf(it) } ?: ChatMode.GLOBAL
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        parentActivity = requireActivity()
        Line2BoxTheme {
            ChatRoute(
                mode = mode,
                viewModel = viewModel,
                onCloseDrawer = { drawerLayout.closeDrawer(GravityCompat.START) },
                onNavigate = ::navigateSafe,
                onPlayEmoji = { drawable, sound ->
                    viewModel.player.playSound(sound)
                    parentActivity.findViewById<ImageView>(R.id.emojiPlay)
                        .apply { loadDrawable(drawable); show() }
                },
                onHideEmoji = { parentActivity.findViewById<ImageView>(R.id.emojiPlay).gone() },
            )
        }
    }

    @Composable
    internal fun ChatRoute(
        mode: ChatMode,
        viewModel: MainViewModel,
        onCloseDrawer: () -> Unit,
        onNavigate: (Routes.GameOnline) -> Unit,
        onPlayEmoji: (drawableRes: Int, rawRes: Int) -> Unit,
        onHideEmoji: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val messages by (if (mode == ChatMode.FRIENDLY) viewModel.friendsChatList else viewModel.globalChatList)
            .collectAsStateWithLifecycle()
        val fieldState = rememberTextFieldState()
        var profile by remember { mutableStateOf<GameProfile?>(null) }
        var emojiEnabled by remember { mutableStateOf(true) }
        var lastKey by remember { mutableStateOf("") }

        fun send() {
            val sent =
                if (mode == ChatMode.FRIENDLY) viewModel.sendMessage2FriendlyChat(
                    fieldState.value
                ) else viewModel.sendMessage2GlobalChat(fieldState.value)
            if (sent != null) fieldState.value = "" else context.toast("Write Something..")
        }
        LaunchedEffect(messages.firstOrNull()?.key, mode) {
            if (mode != ChatMode.FRIENDLY) return@LaunchedEffect
            val newest = messages.firstOrNull() ?: return@LaunchedEffect
            if (newest.key == lastKey) return@LaunchedEffect
            val emoji = when (newest.msgData) {
                "🤣" -> R.drawable.emoji_haha to R.raw.haha
                "😭" -> R.drawable.emoji_cry to R.raw.cry
                "😱" -> R.drawable.emoji_scream to R.raw.scream
                "😘" -> R.drawable.emoji_kiss to R.raw.kiss
                "🥱" -> R.drawable.emoji_yawn to R.raw.yawn
                else -> null
            }
            if (emoji == null) {
                viewModel.player.playPopSound()
                viewModel.setNewMsgBoltVisible(true)
            } else {
                emojiEnabled = false; viewModel.setNewMsgBoltVisible(false); onPlayEmoji(
                    emoji.first,
                    emoji.second
                )
                delay(2500.milliseconds)
                onHideEmoji()
                emojiEnabled = true
            }
            lastKey = newest.key
        }
        ChatScreen(
            messages = messages,
            playerId = viewModel.playerId,
            fieldState = fieldState,
            onSend = { if (mode == ChatMode.GLOBAL) viewModel.player.playPopSound(); send() },
            showEmoji = mode == ChatMode.FRIENDLY,
            emojiEnabled = emojiEnabled,
            onEmoji = {
                viewModel.sendMessage2FriendlyChat(it); viewModel.ignoreDrawerClosesSound =
                true; onCloseDrawer()
            },
            onMessageClick = { msg ->
                viewModel.player.playButtonClickSound()
                Firebase.firestore
                    .collection("gamerProfile").document(msg.playerId).get()
                    .addOnSuccessListener { profile = it.toObject<GameProfile>() }
            },
            onCommand = {
                fieldState.value = ""
                viewModel.player.playButtonClickSound()
                // TODO: Handle commands
            },
            onCopy = { context.setClipBoardData(it, "ID copied") },
            onJoin = { msg ->
                if (msg.gameId.isEmpty())
                    context.toast("Invalid ID")
                else viewModel.getJoinRoute(msg)
                    .onSuccess { onCloseDrawer(); onNavigate(it) }
                    .onFailure { context.toast(it.message.toString()) }
            },
            modifier = modifier
        )
        profile?.let {
            ProfileDialog(
                profile = it,
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
