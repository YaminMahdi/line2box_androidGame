package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class PlayerInfo(
    val id: String = "",
    val nm: String = "",
    val lvl: Int = 0,
    val coin: Int = 0,
    val seenAt: Long = -1L
) : Parcelable

fun GameProfile.toPlayerInfo() = PlayerInfo(
    id = playerId,
    nm = nm,
    lvl = lvlByCal(),
    coin = coin,
    seenAt = System.currentTimeMillis()
)