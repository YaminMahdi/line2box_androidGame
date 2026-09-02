package com.diu.yk_games.line2box.presentation.online.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.ChatCommand
import com.diu.yk_games.line2box.model.MsgStore
import com.diu.yk_games.line2box.model.MsgStore.MessageType.*
import com.diu.yk_games.line2box.model.typeEnum
import com.diu.yk_games.line2box.util.bounceOnClick
import com.diu.yk_games.line2box.util.toTimeDynamic

@Composable
fun MessageRow(
    msg: MsgStore,
    onClick: () -> Unit,
    onCopy: (String) -> Unit,
    onJoin: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (msg.type.typeEnum == Bot && msg.user != null)
                        onCopy(msg.user.toString())
                    else
                        onCopy(msg.msgData ?: msg.playerId)
                }
            ).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Row {
            Text(
                text = msg.nmData,
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
                text = msg.lvlData,
                color = colorResource(R.color.greenY),
                fontSize = 9.sp,
                lineHeight = 10.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = msg.time.toTimeDynamic(),
                color = MaterialTheme.colorScheme.outline,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
        msg.msgData?.let {
            if (msg.type.typeEnum == Command || ChatCommand.hasCommand(it)) Text(
                text = it.toHighlightedCommandText(
                    defaultTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    validCommandColor = colorResource(R.color.color_match_action)
                ),
                fontSize = 17.sp,
                lineHeight = 22.sp
            ) else Text(
                text = if (msg.type.typeEnum == Invitation)
                    msg.msgData.substringBefore("Match ID").trim()
                else
                    msg.msgData,
                color = when (msg.type.typeEnum) {
                    EnterText -> colorResource(R.color.color_match_action)
                    ExitText -> colorResource(R.color.color_left_match)
                    else -> colorResource(R.color.whiteY)
                },
                fontSize = 17.sp,
                lineHeight = 22.sp
            )
        }
        when (msg.type.typeEnum) {
            Invitation -> {
                InvitationActions(
                    id = msg.gameId,
                    onCopy = {
                        onCopy(msg.gameId)
                    },
                    onJoin = onJoin
                )
            }

            Bot if msg.user != null -> UserInfoCard(msg.user)
            else -> Unit
        }
    }
}

@Composable
fun InvitationActions(
    id: String,
    onCopy: () -> Unit,
    onJoin: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp), verticalAlignment = Alignment.Bottom
    ) {
        Surface(color = colorResource(R.color.whiteX), shape = RoundedCornerShape(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    id.take(4),
                    color = colorResource(R.color.whiteY),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
                IconButton(onClick = onCopy) {
                    Icon(
                        painterResource(R.drawable.icon_copy),
                        "Copy game ID",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onJoin,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.greenY)),
            modifier = Modifier
                .height(36.dp)
                .bounceOnClick()
        ) {
            Text(
                text = stringResource(R.string.join),
                fontSize = 13.sp
            )
        }
    }
}