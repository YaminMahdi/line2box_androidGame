package com.diu.yk_games.line2box.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.GameProfile


@Composable
fun ProfileDialog(
    profile: GameProfile,
    onDismiss: () -> Unit,
    isCompact: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(.9f),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = if (isCompact) Modifier
                .padding(end = 80.dp)
                .fillMaxWidth(.8f)
            else
                Modifier.fillMaxWidth()
        ) {
            ProfileContent(profile)
        }
    }
}

@Composable
private fun ProfileContent(profile: GameProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.profile),
            color = colorResource(R.color.greenY),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))

        // Name
        Row {
            Text(
                text = profile.nm,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "lvl.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = profile.lvlByCal().toString(),
                color = colorResource(R.color.greenY),
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
        }
        // country
        Text(
            text = "${profile.countryNm} ${profile.countryEmoji}",
            color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
            fontSize = 16.sp,
            lineHeight = 18.sp
        )

        // Coin pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(0.08f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_trophy),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = profile.coin.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onBackground.copy(0.06f)
            )
        ) {
            ProfileStat(
                icon = Icons.Rounded.SportsEsports,
                label = stringResource(R.string.match_played),
                value = profile.matchPlayed
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(0.08f))
            ProfileStat(
                icon = Icons.Rounded.EmojiEvents,
                label = stringResource(R.string.match_won),
                value = profile.matchWinMulti
            )
        }
    }
}

@Composable
private fun ProfileStat(icon: ImageVector, label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(R.color.greenY),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground.copy(0.85f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}