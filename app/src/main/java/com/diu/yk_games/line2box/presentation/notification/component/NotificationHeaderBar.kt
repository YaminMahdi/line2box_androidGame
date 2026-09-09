package com.diu.yk_games.line2box.presentation.notification.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.presentation.online.stats.LiveDot

@Composable
fun NotificationHeaderBar(
    totalCount: Int,
    unreadCount: Int,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (unreadCount > 0) {
                    LiveDot(color = colorResource(R.color.greenY))
                    Text(
                        text = "$unreadCount unread",
                        color = colorResource(R.color.greenY),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "$totalCount total",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (totalCount > 0) {
            IconButton(
                onClick = onClearAllClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear all notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
