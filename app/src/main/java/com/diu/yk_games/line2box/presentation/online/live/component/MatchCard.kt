package com.diu.yk_games.line2box.presentation.online.live.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.PlayerInfo
import com.diu.yk_games.line2box.presentation.online.live.LiveDot
import com.diu.yk_games.line2box.util.bounceClick

@Composable
fun MatchCard(
    room: GameRoom,
    onPlayerClick: (id: String) -> Unit = {},
    onJoin: () -> Unit,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerCount = remember(room) {
        listOf(room.player1, room.player2).count { it.id.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveBadge()
            PlayerCountBadge(count = playerCount)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            MatchPlayerSlot(
                player = room.player1,
                score = room.matchInfo.result.score1,
                alignEnd = false,
                modifier = Modifier
                    .weight(1f)
                    .bounceClick {
                        if (room.player1.id.isNotEmpty())
                            onPlayerClick(room.player1.id)
                    }
            )
            VersusBadge()
            MatchPlayerSlot(
                player = room.player2,
                score = room.matchInfo.result.score2,
                alignEnd = true,
                modifier = Modifier
                    .weight(1f)
                    .bounceClick {
                        if (room.player2.id.isNotEmpty())
                            onPlayerClick(room.player2.id)
                    }
            )
        }

        Spacer(Modifier.height(8.dp))

        MatchActionButton(
            playerCount = playerCount,
            onJoin = onJoin,
            onWatch = onWatch
        )
    }
}

@Composable
private fun MatchPlayerSlot(
    player: PlayerInfo,
    score: Int,
    modifier: Modifier = Modifier,
    alignEnd: Boolean
) {
    val isEmpty = player.id.isEmpty()
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                .border(
                    1.dp,
                    if (isEmpty) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Center
        ) {
            if (isEmpty) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Text(
                    text = "${player.lvl}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isEmpty) "Waiting…" else player.nm,
            color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!isEmpty) {
            Text(
                text = "$score",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
private fun VersusBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Center
    ) {
        Text(
            text = "VS",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveDot(color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(5.dp))
        Text(
            text = "LIVE",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
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
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Group,
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
    playerCount: Int,
    onJoin: () -> Unit,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canJoin = playerCount < 2
    OutlinedButton(
        onClick = if (canJoin) onJoin else onWatch,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (canJoin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (canJoin) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
            contentColor = if (canJoin) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = if (canJoin) Icons.Filled.PlayArrow else Icons.Filled.RemoveRedEye,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (canJoin) "Join Match" else "Watch",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}