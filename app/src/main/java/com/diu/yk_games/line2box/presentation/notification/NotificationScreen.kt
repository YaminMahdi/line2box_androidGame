package com.diu.yk_games.line2box.presentation.notification

import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.diu.yk_games.line2box.databinding.HomeRowBinding
import com.diu.yk_games.line2box.model.NotificationItem
import com.diu.yk_games.line2box.presentation.notification.component.ClearAllConfirmDialog
import com.diu.yk_games.line2box.presentation.notification.component.DismissibleNotificationItem
import com.diu.yk_games.line2box.presentation.notification.component.EmptyNotificationsState
import com.diu.yk_games.line2box.presentation.notification.component.NotificationHeaderBar
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.setBounceClickListener
import com.diu.yk_games.line2box.util.show
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NotificationScreen(
    notifications: PersistentList<NotificationItem>,
    unreadCount: Int,
    onNotificationClick: (NotificationItem) -> Unit,
    onDeleteNotification: (NotificationItem) -> Unit,
    onMarkAsRead: (NotificationItem) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDialPhone: (String) -> Unit,
    onClickHome: (View) -> Unit,
    onClickIdea: (View) -> Unit,
    onClickSettings: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        AndroidViewBinding(
            factory = HomeRowBinding::inflate,
            modifier = Modifier.fillMaxWidth()
        ) {
            btnHome.setBounceClickListener(onClickHome)
            btnIdea.apply {
                show()
                setBounceClickListener(onClickIdea)
            }
            btnSetting.setBounceClickListener(onClickSettings)
        }

        NotificationHeaderBar(
            totalCount = notifications.size,
            unreadCount = unreadCount,
            onMarkAllAsRead = onMarkAllAsRead,
            onClearAllClick = { showClearAllConfirmDialog = true }
        )

        if (notifications.isEmpty()) {
            EmptyNotificationsState(
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = notifications,
                    key = { it.id }
                ) { item ->
                    DismissibleNotificationItem(
                        item = item,
                        onClick = { onNotificationClick(item) },
                        onDelete = { onDeleteNotification(item) },
                        onMarkAsRead = { onMarkAsRead(item) },
                        onOpenUrl = onOpenUrl,
                        onDialPhone = onDialPhone
                    )
                }
            }
        }
    }

    if (showClearAllConfirmDialog) {
        ClearAllConfirmDialog(
            onConfirm = {
                showClearAllConfirmDialog = false
                onClearAll()
            },
            onDismiss = { showClearAllConfirmDialog = false }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101010)
@Composable
private fun NotificationScreenPreview() {
    Line2BoxTheme {
        NotificationScreen(
            notifications = persistentListOf(
                NotificationItem(
                    id = "1",
                    title = "Match Challenge Available!",
                    body = "A new opponent has joined the arena. Tap here to join and test your skills.",
                    type = "Game",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 12,
                    isRead = false,
                    redirectUrl = "https://example.com"
                ),
                NotificationItem(
                    id = "2",
                    title = "Welcome to Line2Box",
                    body = "Thanks for playing Line2Box! Customize your themes in settings and enjoy.",
                    type = "Info",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
                    isRead = true
                )
            ),
            unreadCount = 1,
            onNotificationClick = {},
            onDeleteNotification = {},
            onMarkAsRead = {},
            onMarkAllAsRead = {},
            onClearAll = {},
            onOpenUrl = {},
            onDialPhone = {},
            onClickHome = {},
            onClickIdea = {},
            onClickSettings = {}
        )
    }
}