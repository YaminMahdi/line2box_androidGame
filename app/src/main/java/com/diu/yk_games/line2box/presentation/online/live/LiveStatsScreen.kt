package com.diu.yk_games.line2box.presentation.online.live

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.databinding.HomeRowBinding
import com.diu.yk_games.line2box.model.GameRoom
import com.diu.yk_games.line2box.model.LiveResult
import com.diu.yk_games.line2box.model.PlayerInfo
import com.diu.yk_games.line2box.presentation.online.live.component.ActivePlayerCard
import com.diu.yk_games.line2box.presentation.online.live.component.MatchCard
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.util.isLessThanAgo
import com.diu.yk_games.line2box.util.setBounceClickListener
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

private enum class LiveTab(val label: String) {
    MATCHES("Matches"),
    ACTIVE("Active")
}

@Composable
fun LiveStatsScreen(
    playerId: String,
    actives: List<PlayerInfo>,
    matches: List<GameRoom>,
    modifier: Modifier = Modifier,
    onPlayerClick: (id: String) -> Unit = {},
    onJoinRoom: (GameRoom) -> Unit = {},
    onShareRoom: (GameRoom) -> Unit = {},
    onWatchRoom: (GameRoom) -> Unit = {},
    onClickHome: (View) -> Unit = {},
    onClickIdea: (View) -> Unit = {},
    onClickSettings: (View) -> Unit = {}
) {
    val tabs = remember { LiveTab.entries }
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
                ACTIVE -> ActivePlayersList(
                    actives = actives,
                    onPlayerClick = onPlayerClick
                )

                MATCHES -> MatchesList(
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
    actives: List<PlayerInfo>,
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
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 12.dp),
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
    matches: List<GameRoom>,
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
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 12.dp),
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

// ---------- Shared ----------
@Composable
private fun LiveTabRow(
    tabs: List<LiveTab>,
    selectedIndex: Int,
    matchCount: Int,
    activeCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .padding(horizontal = 30.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            LiveTabItem(
                selected = index == selectedIndex,
                onTabSelected = onTabSelected,
                index = index,
                tab = tab,
                count = if (tab == ACTIVE) activeCount else matchCount,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun LiveTabItem(
    selected: Boolean,
    onTabSelected: (Int) -> Unit,
    index: Int,
    tab: LiveTab,
    count: Int,
    modifier: Modifier
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        label = "tabBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "tabBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent"
    )
    BadgedBox(
        badge = {
            AnimatedVisibility(selected && count > 0) {
                Badge(
                    containerColor = colorResource(R.color.greenY),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(count.toString())
                }
            }
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { onTabSelected(index) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tab == ACTIVE) {
                LiveDot(color = contentColor)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = tab.label,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp)
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
            actives = listOf(
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
            matches = listOf(
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