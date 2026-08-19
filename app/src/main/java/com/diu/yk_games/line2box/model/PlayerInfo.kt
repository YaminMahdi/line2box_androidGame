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
    val score: Int = 0,
    val cup: Int = 0,
    val seenAt: Long = System.currentTimeMillis()
) : Parcelable

fun GameProfile.toPlayerInfo(score: Int = 0, cup: Int = 0) = PlayerInfo(
    id = playerId,
    nm = nm,
    lvl = lvlByCal(),
    coin = coin,
    score = score,
    cup = cup,
    seenAt = System.currentTimeMillis()
)