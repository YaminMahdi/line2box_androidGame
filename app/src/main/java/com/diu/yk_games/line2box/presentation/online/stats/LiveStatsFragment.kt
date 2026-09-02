package com.diu.yk_games.line2box.presentation.online.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.base.BaseFragmentCompose
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.presentation.component.ProfileDialog
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.presentation.online.ShareDialogFragment
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.*
import io.ak1.BubbleTabBar

class LiveStatsFragment : BaseFragmentCompose() {

    private val bubbleTabBar: BubbleTabBar by lazy {
        parentActivity.findViewById(R.id.bubbleTabBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        Line2BoxTheme {
            val matches by viewModel.matches.collectAsStateWithLifecycle()
            val actives by viewModel.actives.collectAsStateWithLifecycle()
            var profile by remember { mutableStateOf<GameProfile?>(null) }

            LiveStatsScreen(
                playerId = viewModel.playerId,
                matches = matches,
                actives = actives,
                onPlayerClick = { playerId ->
                    playerId.log(TAG)
                    if (viewModel.playerId.isNotEmpty()) {
                        viewModel.player.playButtonClickSound()
                        viewModel.gamerProfileRef.document(playerId).get()
                            .addOnSuccessListener { profile = it.toObjectOrNull<GameProfile>() }
                    }
                },
                onShareRoom = { room ->
                    viewModel.player.playButtonClickSound()
                    viewModel.matchRouteInfo = room.toRoutes(viewModel.playerId)
                    ShareDialogFragment().show(childFragmentManager, "share")
                },
                onJoinRoom = { room ->
                    viewModel.player.playButtonClickSound()
                    viewModel.getJoinRoute(room)
                        .onSuccess {
                            bubbleTabBar.setSelected(1, true)
                            navigateSafe(it)
                        }
                        .onFailure { DynamicIslandController.message(it.message.toString()) }
                },
                onWatchRoom = {
                    viewModel.player.playButtonClickSound()
                    bubbleTabBar.setSelected(1, true)
                    navigateSafe(viewModel.getWatchRoute(it))
                },
                onClickHome = {
                    viewModel.player.playButtonClickSound()
                    popBackSafe()
                },
                onClickIdea = {
                    viewModel.player.playButtonClickSound()

                },
                onClickSettings = {
                    viewModel.player.playButtonClickSound()
                    SettingsFragment.show(childFragmentManager)
                }
            )
            profile?.let {
                ProfileDialog(
                    profile = it,
                    isCompact = false,
                    onCopy = { context.setClipBoardData(it.toString(), "Copied!") },
                    onDismiss = { profile = null }
                )
            }
        }
    }

    companion object {
        private val TAG = getTag()
    }
}