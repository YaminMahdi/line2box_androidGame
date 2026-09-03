package com.diu.yk_games.line2box.presentation.online.stats.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun <T> AnimatedTabRow(
    tabs: PersistentList<T>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() }
) {
    val density = LocalDensity.current
    val spacing = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .padding(horizontal = 25.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            tabs.forEachIndexed { index, tab ->
                AnimatedTabItem(
                    selected = index == selectedIndex,
                    onClick = { onTabSelected(index) },
                    text = label(tab),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AnimatedTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabContent"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimatedTabRowPreview() {
    Line2BoxTheme {
        val (selectedIndex, setSelectedIndex) = remember { mutableIntStateOf(0) }
        AnimatedTabRow(
            tabs = persistentListOf("Live Matches", "Active Players"),
            selectedIndex = selectedIndex,
            onTabSelected = setSelectedIndex,
            modifier = Modifier.fillMaxWidth()
                .padding(top = 20.dp)
        )
    }
}