package com.diu.yk_games.line2box.presentation.notification.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.NotificationItem
import com.diu.yk_games.line2box.notification.toBitmap
import com.diu.yk_games.line2box.presentation.online.stats.LiveDot
import com.diu.yk_games.line2box.util.setClipBoardData
import com.diu.yk_games.line2box.util.toTimeDynamic

@Composable
fun DismissibleNotificationItem(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDialPhone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = { }
    ) {
        NotificationCard(
            item = item,
            onClick = onClick,
            onDelete = onDelete,
            onMarkAsRead = onMarkAsRead,
            onOpenUrl = onOpenUrl,
            onDialPhone = onDialPhone
        )
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDialPhone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val isUnread = !item.isRead

    val bitmap = remember(item.imageBitmap) {
        item.imageBitmap?.toBitmap()?.asImageBitmap()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(
                    radius = 4.dp,
                    spread = 1.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    offset = DpOffset(1.dp, 2.dp)
                )
            )
            .background(
                color = if (isUnread) MaterialTheme.colorScheme.primary.copy(.15f) else MaterialTheme.colorScheme.surface.copy(
                    .5f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isUnread) 1.dp else .5.dp,
                color = if (isUnread) MaterialTheme.colorScheme.primary.copy(.5f) else MaterialTheme.colorScheme.outline.copy(
                    .2f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = {
                    isExpanded = !isExpanded
                    onClick()
                },
                onLongClick = {
                    val textToCopy = listOfNotNull(
                        item.title.takeIf { it.isNotBlank() },
                        item.body.takeIf { it.isNotBlank() }
                    ).joinToString("\n")
                    context.setClipBoardData(textToCopy, "Copied to clipboard")
                }
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUnread) {
                LiveDot(color = colorResource(R.color.greenY))
                Spacer(Modifier.width(6.dp))
            }

            if (item.type.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = item.type.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = item.timestamp.toTimeDynamic(),
                color = MaterialTheme.colorScheme.outline,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete notification",
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        )
                )
                Spacer(Modifier.width(10.dp))
            } else {
                val icon = when {
                    item.type.contains("chat", ignoreCase = true) -> Icons.AutoMirrored.Rounded.Chat
                    item.type.contains("game", ignoreCase = true) -> Icons.Rounded.SportsEsports
                    item.type.contains("promo", ignoreCase = true) || item.type.contains("update", ignoreCase = true) -> Icons.Rounded.Campaign
                    else -> Icons.Rounded.Notifications
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            1.dp,
                            if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (item.title.isNotBlank()) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                }

                if (item.body.isNotBlank()) {
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded && bitmap != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (bitmap != null) {
                Spacer(Modifier.height(10.dp))
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                )
            }
        }

        val hasActions = !item.redirectUrl.isNullOrBlank() || !item.phone.isNullOrBlank() || isUnread
        if (hasActions) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!item.redirectUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenUrl(item.redirectUrl) },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Open Link",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!item.phone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onDialPhone(item.phone) },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, colorResource(R.color.greenY).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colorResource(R.color.greenY).copy(alpha = 0.15f),
                            contentColor = colorResource(R.color.greenY)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Call",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (isUnread) {
                    TextButton(
                        onClick = onMarkAsRead,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Mark read",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
