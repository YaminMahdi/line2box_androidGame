package com.diu.yk_games.line2box.presentation.online.chat.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameProfile
import com.diu.yk_games.line2box.ui.theme.Line2BoxChatTheme

@Composable
fun UserInfoCard(user: GameProfile, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(0.08f))
            .padding(vertical = 6.dp, horizontal = 10.dp)
    ) {
        // First row: Country, Name, Level, Coins
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            // Player Name
            Text(
                text = user.nm.ifEmpty { "Unknown Player" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Coins
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_trophy),
                    contentDescription = "Coins",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${user.coin}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        // Second row: City, Country, Matches
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {
            // City, Country
            if (user.cityNm.isNotEmpty() && user.countryNm.isNotEmpty()) {
                Text(
                    text = "${user.cityNm}, ${user.countryNm} ${user.countryEmoji}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Match Stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "⚔️",
                    fontSize = 12.sp
                )
                Text(
                    text = "${user.matchPlayed}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = "🥇",
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = "${user.matchWinMulti}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        // Third row: Player ID and IP/Query
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Player ID
            if (user.playerId.isNotEmpty()) {
                Text(
                    text = "ID: ${user.playerId}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Query/IP display
            if (user.query.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                ) {
                    // Show IP indicator
                    Text(
                        text = "🌐",
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = user.query,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserInfoCardPreview() {
    Line2BoxChatTheme {
        UserInfoCard(
            user = GameProfile(
                nm = "John Doe",
                cityNm = "New York",
                countryNm = "United States",
                countryEmoji = "🇺🇸",
                query = "192.168.1.100",
                matchPlayed = 42,
                matchWinMulti = 37,
                coin = 1250,
                lvl = 15,
                playerId = "a_1070246872382803456"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}