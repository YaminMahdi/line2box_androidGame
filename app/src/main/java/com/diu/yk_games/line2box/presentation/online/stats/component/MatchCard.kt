package com.diu.yk_games.line2box.presentation.online.stats.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.LiveResult
import com.diu.yk_games.line2box.model.PlayerInfo
import com.diu.yk_games.line2box.presentation.online.stats.LiveDot
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.ui.theme.cocZ
import com.diu.yk_games.line2box.util.bounceClick
import com.diu.yk_games.line2box.util.isLessThanAgo
import com.diu.yk_games.line2box.util.isMoreThanAgo
import com.diu.yk_games.line2box.util.toTimePassed
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Composable
fun MatchCard(
    isMe: Boolean,
    room: GameRoom,
    onPlayerClick: (id: String) -> Unit,
    onJoin: () -> Unit,
    onShare: () -> Unit,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMatchOver = room.matchInfo.result.score1 + room.matchInfo.result.score2 == 36
    val playerCount = remember(room) {
        listOf(room.player1, room.player2).count { it.id.isNotEmpty() && it.seenAt > 0 }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(
                    radius = 5.dp,
                    spread = 2.dp,
                    color = MaterialTheme.colorScheme.surface.copy(.5f),
                    offset = DpOffset(2.dp, 2.dp)
                )
            )
            .background(
                color = if (isMe) MaterialTheme.colorScheme.primary.copy(.15f) else MaterialTheme.colorScheme.surface.copy(
                    .5f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isMe) 1.dp else .5.dp,
                color = if (isMe) MaterialTheme.colorScheme.primary.copy(.5f) else MaterialTheme.colorScheme.outline.copy(
                    .2f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            LiveBadge(
                status = if (isMatchOver)
                    LiveStatus.Ended
                else if (room.player1.seenAt < 0 || room.player2.seenAt < 0)
                    LiveStatus.Waiting
                else if (room.pingAt.isMoreThanAgo(1.days))
                    LiveStatus.Dead
                else
                    LiveStatus.Live
            )
            TimePassed(room.pingAt)
            PlayerCountBadge(count = playerCount)
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            MatchPlayerSlot(
                player = room.player1,
                cup = room.matchInfo.result.cup1,
                isMatchOver = isMatchOver,
                alignEnd = false,
                modifier = Modifier
                    .weight(1f)
                    .bounceClick {
                        if (room.player1.id.isNotEmpty())
                            onPlayerClick(room.player1.id)
                    }
            )
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary.copy(.5f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(room.ver.name.lowercase())
                    }
                },
                modifier = Modifier
                    .graphicsLayer {
                        translationY = -13.dp.toPx()
                    }
            ) {
                VersusBadge(
                    result = room.matchInfo.result,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 100.dp)
                )
            }
            MatchPlayerSlot(
                player = room.player2,
                cup = room.matchInfo.result.cup2,
                alignEnd = true,
                isMatchOver = isMatchOver,
                modifier = Modifier
                    .weight(1f)
                    .bounceClick {
                        if (room.player2.id.isNotEmpty())
                            onPlayerClick(room.player2.id)
                    }
            )
        }

        val canJoin = playerCount < 2 || isMe
        val canWatch = playerCount == 2 && room.pingAt.isLessThanAgo(5.minutes)


        AnimatedVisibility(!isMatchOver && (canJoin || canWatch)) {
            Column {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MatchActionButton(
                        isMe = isMe,
                        canJoin = canJoin,
                        onJoin = onJoin,
                        onWatch = onWatch,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(isMe) {
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimePassed(
    millis: Long,
    modifier: Modifier = Modifier
) {
    if (millis != -1L) {
        Text(
            text = if (millis == -2L)
                "Player left.."
            else if (millis.isLessThanAgo(5.minutes))
                "Active"
            else
                millis.toTimePassed(),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            modifier = modifier
        )
    }
}

@Composable
private fun MatchPlayerSlot(
    player: PlayerInfo,
    cup: String,
    alignEnd: Boolean,
    isMatchOver: Boolean,
    modifier: Modifier = Modifier
) {
    val isEmpty = player.nm.isEmpty()
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                text = if (isEmpty) "Waiting…" else player.nm,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!isEmpty) {
                Text(
                    text = "lvl.",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = player.lvl.toString(),
                    color = colorResource(R.color.greenY),
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                )
            }
        }
        if (isMatchOver) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val composableList: List<@Composable () -> Unit> = listOf(
                    {
                        Image(
                            painter = painterResource(R.drawable.icon_trophy),
                            contentDescription = "Coins",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    { Spacer(Modifier.width(4.dp)) },
                    {
                        Text(
                            text = cup,
                            color = cocZ,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                )
                if (alignEnd)
                    composableList.reversed().forEach { it() }
                else
                    composableList.forEach { it() }
            }
        } else TimePassed(player.seenAt)
    }
}

@Composable
private fun VersusBadge(modifier: Modifier = Modifier, result: LiveResult) {
    Box(
        contentAlignment = Center,
        modifier = modifier
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${result.score1} - ${result.score2}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

enum class LiveStatus {
    Live,
    Ended,
    Waiting,
    Dead
}

/** Container + border + content colors, optional leading icon, and label for one [LiveStatus]. */
private data class LiveBadgeStyle(
    val container: Color,
    val border: Color,
    val content: Color,
    val icon: ImageVector?,
    val label: String
)

@Composable
private fun liveBadgeStyle(status: LiveStatus): LiveBadgeStyle {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        LiveStatus.Live -> LiveBadgeStyle(
            container = colors.errorContainer.copy(alpha = 0.35f),
            border = colors.error,
            content = colors.error,
            icon = null,
            label = "LIVE"
        )

        LiveStatus.Ended -> LiveBadgeStyle(
            container = colors.errorContainer.copy(alpha = 0.1f),
            border = colors.error.copy(alpha = .3f),
            content = colors.error.copy(alpha = .5f),
            icon = null,
            label = "ENDED"
        )

        LiveStatus.Waiting -> LiveBadgeStyle(
            container = colors.tertiaryContainer.copy(alpha = 0.35f),
            border = colors.tertiary,
            content = colors.tertiary,
            icon = Icons.Rounded.Schedule,
            label = "WAITING"
        )

        LiveStatus.Dead -> LiveBadgeStyle(
            container = colors.surfaceVariant.copy(alpha = 0.35f),
            border = colors.onSurfaceVariant.copy(alpha = .3f),
            content = colors.onSurfaceVariant.copy(alpha = .5f),
            icon = Icons.Rounded.LinkOff,
            label = "DEAD"
        )
    }
}

@Composable
private fun LiveBadge(
    status: LiveStatus,
    modifier: Modifier = Modifier
) {
    val style = liveBadgeStyle(status)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(style.container)
            .border(width = 1.dp, color = style.border, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            status == LiveStatus.Live -> {
                LiveDot(color = style.content)
                Spacer(Modifier.width(5.dp))
            }

            style.icon != null -> {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.content,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
        }
        Text(
            text = style.label,
            color = style.content,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun PlayerCountBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "$count/2",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MatchActionButton(
    isMe: Boolean,
    canJoin: Boolean,
    onJoin: () -> Unit,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = if (canJoin) onJoin else onWatch,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (canJoin)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (canJoin) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
            contentColor = if (canJoin) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    ) {
        Icon(
            imageVector = if (canJoin) Icons.Rounded.PlayArrow else Icons.Rounded.RemoveRedEye,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (isMe)
                "Rejoin Match"
            else if (canJoin)
                "Join Match"
            else
                "Watch",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MatchCardPreview() {
    Line2BoxTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MatchCard(
                isMe = true,
                room = GameRoom(
                    ver = GameRoom.Version.V2,
                    pingAt = System.currentTimeMillis() - 123243443,
                    player1 = PlayerInfo(
                        id = "1",
                        nm = "Player One",
                        lvl = 5,
                        seenAt = System.currentTimeMillis() - 23243443
                    ),
                    player2 = PlayerInfo(id = "2", nm = "Player Two", lvl = 3, seenAt = -2L),
                    matchInfo = GameRoom.MatchInfo(
                        result = LiveResult(score1 = 12, score2 = 24)
                    )
                ),
                onPlayerClick = {},
                onJoin = {},
                onShare = {},
                onWatch = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Waiting Player")
@Composable
fun MatchCardWaitingPreview() {
    Line2BoxTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MatchCard(
                isMe = false,
                room = GameRoom(
                    player1 = PlayerInfo(id = "1", nm = "Player One", lvl = 5),
                    player2 = PlayerInfo(),
                    matchInfo = GameRoom.MatchInfo(
                        result = LiveResult(score1 = 0, score2 = 0)
                    )
                ),
                onPlayerClick = {},
                onJoin = {},
                onShare = {},
                onWatch = {},
            )
        }
    }
}
