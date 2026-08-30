package com.diu.yk_games.line2box.presentation.online.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.presentation.base.BaseFragmentCompose
import com.diu.yk_games.line2box.presentation.component.ProfileDialog
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.*

class LiveStatsFragment : BaseFragmentCompose() {

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
                onJoinRoom = { room ->
                    viewModel.player.playButtonClickSound()
                    viewModel.getJoinRoute(room)
                        .onSuccess { navigateSafe(it) }
                        .onFailure { context.toast(it.message.toString()) }
                },
                onWatchRoom = {
                    viewModel.player.playButtonClickSound()
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
                    onDismiss = { profile = null }
                )
            }
        }
    }

    companion object {
        private val TAG = getTag()
    }
}