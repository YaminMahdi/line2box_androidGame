package com.diu.yk_games.line2box.presentation.online.stats

import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.diu.yk_games.line2box.databinding.HomeRowBinding
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.LiveResult
import com.diu.yk_games.line2box.model.PlayerInfo
import com.diu.yk_games.line2box.presentation.online.stats.component.ActivePlayerCard
import com.diu.yk_games.line2box.presentation.online.stats.component.LiveTab
import com.diu.yk_games.line2box.presentation.online.stats.component.LiveTabRow
import com.diu.yk_games.line2box.presentation.online.stats.component.MatchCard
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.isLessThanAgo
import com.diu.yk_games.line2box.util.setBounceClickListener
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

@Composable
fun LiveStatsScreen(
    playerId: String,
    actives: PersistentList<PlayerInfo>,
    matches: PersistentList<GameRoom>,
    modifier: Modifier = Modifier,
    onPlayerClick: (id: String) -> Unit = {},
    onJoinRoom: (GameRoom) -> Unit = {},
    onShareRoom: (GameRoom) -> Unit = {},
    onWatchRoom: (GameRoom) -> Unit = {},
    onClickHome: (View) -> Unit = {},
    onClickIdea: (View) -> Unit = {},
    onClickSettings: (View) -> Unit = {}
) {
    val tabs = remember { LiveTab.entries.toPersistentList() }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        AndroidViewBinding(
            factory = HomeRowBinding::inflate,
            modifier = modifier.fillMaxWidth()
        ) {
            btnHome.setBounceClickListener(onClickHome)
            btnIdea.setBounceClickListener(onClickIdea)
            btnSetting.setBounceClickListener(onClickSettings)
        }
        LiveTabRow(
            tabs = tabs,
            selectedIndex = pagerState.currentPage,
            matchCount = matches.size,
            activeCount = actives.count { it.seenAt.isLessThanAgo(1.days) },
            onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )

        HorizontalPager(
            state = pagerState,
            key = { tabs[it].name },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (tabs[page]) {
                Active -> ActivePlayersList(
                    actives = actives,
                    onPlayerClick = onPlayerClick
                )

                Matches -> MatchesList(
                    playerId = playerId,
                    matches = matches,
                    onPlayerClick = onPlayerClick,
                    onJoinRoom = onJoinRoom,
                    onShareRoom = onShareRoom,
                    onWatchRoom = onWatchRoom
                )
            }
        }
    }
}

// ---------- Active players ----------

@Composable
private fun ActivePlayersList(
    actives: PersistentList<PlayerInfo>,
    onPlayerClick: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (actives.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Group,
            title = "No players online",
            subtitle = "Check back soon"
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 35.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actives, key = { it.id }) { player ->
            ActivePlayerCard(
                player = player,
                onClick = { onPlayerClick(player.id) }
            )
        }
    }
}

// ---------- Matches ----------

@Composable
private fun MatchesList(
    playerId: String,
    matches: PersistentList<GameRoom>,
    onPlayerClick: (id: String) -> Unit,
    onJoinRoom: (GameRoom) -> Unit,
    onShareRoom: (GameRoom) -> Unit,
    onWatchRoom: (GameRoom) -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.PlayArrow,
            title = "No live matches",
            subtitle = "Start one from the lobby"
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 35.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(matches, key = { it.key }) { room ->
            val isMe = room.player1.id == playerId || room.player2.id == playerId
            MatchCard(
                isMe = isMe,
                room = room,
                onPlayerClick = onPlayerClick,
                onJoin = { onJoinRoom(room) },
                onShare = { onShareRoom(room) },
                onWatch = { onWatchRoom(room) }
            )
        }
    }
}

@Composable
fun LiveDot(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Preview
@Composable
private fun LiveStatsScreenPrev() {
    Line2BoxTheme {
        LiveStatsScreen(
            playerId = "a",
            actives = persistentListOf(
                PlayerInfo(
                    id = "a",
                    nm = "Player 1",
                    lvl = 10,
                    coin = 100,
                    seenAt = System.currentTimeMillis()
                ),
                PlayerInfo(
                    id = "b",
                    nm = "Player 2",
                    lvl = 20,
                    coin = 200,
                    seenAt = System.currentTimeMillis()
                )
            ),
            matches = persistentListOf(
                GameRoom(
                    ver = V2,
                    key = "aa",
                    player1 = PlayerInfo(
                        id = "a",
                        nm = "Player 1",
                        lvl = 10,
                        coin = 100,
                        seenAt = System.currentTimeMillis()
                    ),
                    player2 = PlayerInfo(
                        id = "b",
                        nm = "Player 2",
                        lvl = 20,
                        coin = 200,
                        seenAt = System.currentTimeMillis()
                    ),
                    matchInfo = GameRoom.MatchInfo(
                        result = LiveResult(
                            score1 = 10,
                            score2 = 20
                        )
                    )
                ),
                GameRoom(
                    ver = V2,
                    key = "bb",
                    pingAt = 11500000,
                    player1 = PlayerInfo(
                        id = "a",
                        nm = "Solo Player",
                        lvl = 5,
                        coin = 40,
                        seenAt = System.currentTimeMillis()
                    ),
                    player2 = PlayerInfo(
                        id = "",
                        nm = "",
                        lvl = 0,
                        coin = 0,
                        seenAt = 0
                    ),
                    matchInfo = GameRoom.MatchInfo(
                        result = LiveResult(
                            score1 = 10,
                            score2 = 20
                        )
                    )
                )
            ),
        )
    }
}