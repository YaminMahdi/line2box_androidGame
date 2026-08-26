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
import com.diu.yk_games.line2box.util.getTag
import com.diu.yk_games.line2box.util.log
import com.diu.yk_games.line2box.util.popBackSafe
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

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
                matches = matches,
                actives = actives,
                onPlayerClick = { playerId ->
                    playerId.log(TAG)
                    viewModel.player.playButtonClickSound()
                    Firebase.firestore
                        .collection("gamerProfile").document(playerId).get()
                        .addOnSuccessListener { profile = it.toObject<GameProfile>() }
                },
                onJoinRoom = {
                    viewModel.player.playButtonClickSound()

                },
                onWatchRoom = {
                    viewModel.player.playButtonClickSound()

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