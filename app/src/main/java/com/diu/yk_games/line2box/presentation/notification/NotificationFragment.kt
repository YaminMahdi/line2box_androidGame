package com.diu.yk_games.line2box.presentation.notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.notification.NotificationStore
import com.diu.yk_games.line2box.presentation.base.BaseFragmentCompose
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.presentation.main.SettingsFragment
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.getTag
import com.diu.yk_games.line2box.util.popBackSafe
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.showCustomTab

class NotificationFragment : BaseFragmentCompose() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        Line2BoxTheme {
            val notifications by NotificationStore.notifications.collectAsStateWithLifecycle()
            val counts by NotificationStore.notificationCounts.collectAsStateWithLifecycle()

            NotificationScreen(
                notifications = notifications,
                unreadCount = counts.unreadCount,
                onNotificationClick = { item ->
                    viewModel.player.playButtonClickSound()
                    NotificationStore.markAsRead(item.id)
                    if (!item.redirectUrl.isNullOrBlank()) {
                        parentActivity.showCustomTab(item.redirectUrl)
                    }
                },
                onDeleteNotification = { item ->
                    viewModel.player.playButtonClickSound()
                    NotificationStore.removeNotification(item.id)
                },
                onMarkAsRead = { item ->
                    viewModel.player.playButtonClickSound()
                    NotificationStore.markAsRead(item.id)
                },
                onClearAll = {
                    viewModel.player.playButtonClickSound()
                    NotificationStore.clearAll()
                    DynamicIslandController.message("Notifications cleared")
                },
                onOpenUrl = { url ->
                    viewModel.player.playButtonClickSound()
                    parentActivity.showCustomTab(url)
                },
                onDialPhone = { phone ->
                    viewModel.player.playButtonClickSound()
                    runCatching {
                        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                        startActivity(intent)
                    }.onFailure {
                        parentActivity.setClipBoardData(phone, "Phone number copied")
                    }
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
        }
    }

    companion object {
        private val TAG = getTag()
    }
}