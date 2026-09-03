package com.diu.yk_games.line2box.presentation.online.stats.component

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.presentation.online.stats.LiveDot
import kotlinx.collections.immutable.PersistentList

enum class LiveTab {
    Matches,
    Active
}

@Composable
fun LiveTabRow(
    tabs: PersistentList<LiveTab>,
    selectedIndex: Int,
    matchCount: Int,
    activeCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val spacing = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .padding(horizontal = 30.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                RoundedCornerShape(15.dp)
            )
            .padding(3.dp)
    ) {
        val spacingPx = with(density) { spacing.roundToPx() }
        val tabWidthPx = (constraints.maxWidth - spacingPx * (tabs.size - 1)) / tabs.size

        val indicatorOffset by animateIntAsState(
            targetValue = selectedIndex * (tabWidthPx + spacingPx),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "tabIndicatorOffset"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .wrapContentWidth(Alignment.Start)
                .width(with(density) { tabWidthPx.toDp() })
                .offset { IntOffset(indicatorOffset, 0) }
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)
                )
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            tabs.forEachIndexed { index, tab ->
                LiveTabItem(
                    selected = index == selectedIndex,
                    onTabSelected = onTabSelected,
                    index = index,
                    tab = tab,
                    count = if (tab == LiveTab.Active) activeCount else matchCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LiveTabItem(
    selected: Boolean,
    onTabSelected: (Int) -> Unit,
    index: Int,
    tab: LiveTab,
    count: Int,
    modifier: Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent"
    )
    BadgedBox(
        badge = {
            AnimatedVisibility(
                visible = selected && count > 0,
                enter = fadeIn() + expandIn(clip = false),
                exit = fadeOut() + shrinkOut(clip = false)
            ) {
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
                .clickable { onTabSelected(index) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tab == LiveTab.Active) {
                LiveDot(color = contentColor)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = tab.toString(),
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}