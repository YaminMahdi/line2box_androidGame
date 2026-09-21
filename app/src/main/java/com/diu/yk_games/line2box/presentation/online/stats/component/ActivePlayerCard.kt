package com.diu.yk_games.line2box.presentation.online.stats.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.PlayerInfo
import com.diu.yk_games.line2box.presentation.online.stats.LiveDot
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import com.diu.yk_games.line2box.ui.theme.cocZ
import com.diu.yk_games.line2box.util.bounceClick
import com.diu.yk_games.line2box.util.toInitial

@Composable
fun ActivePlayerCard(
    player: PlayerInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(
                    radius = 5.dp,
                    spread = 2.dp,
                    color = MaterialTheme.colorScheme.surface.copy(.5f),
                    offset = DpOffset(2.dp, 2.dp)
                )
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .5f), RoundedCornerShape(20.dp))
            .border(.5.dp, MaterialTheme.colorScheme.outline.copy(.2f), RoundedCornerShape(20.dp))
            .padding(10.dp)
            .padding(vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Center
        ) {
            Text(
                text = player.nm.toInitial(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    text = player.nm,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiveDot(color = colorResource(R.color.greenY))
                TimePassed(millis = player.seenAt, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.icon_trophy),
                contentDescription = "Coins",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${player.coin}",
                color = cocZ,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivePlayerCardPreview() {
    Line2BoxTheme {
        ActivePlayerCard(
            player = PlayerInfo(
                id = "1",
                nm = "Player",
                lvl = 12,
                coin = 450,
                seenAt = System.currentTimeMillis()
            ),
            modifier = Modifier.padding(16.dp),
            onClick = {}
        )
    }
}