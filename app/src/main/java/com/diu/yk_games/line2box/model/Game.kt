package com.diu.yk_games.line2box.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerInfo(
    val id: String = "",
    val nm: String = "",
    val lvl: Int = 0,
    val coin: Int = 0,
) : Parcelable

@Parcelize
data class PlayerInfoOld(
    val nm1: String = "",
    val lvl1: Int = 0,
    val plr1Id: String = "",

    val nm2: String = "",
    val lvl2: Int = 0,
    val plr2Id: String = ""
) : Parcelable

@Parcelize
data class GameRoom(
    val player1: PlayerInfo = PlayerInfo(),
    val player2: PlayerInfo = PlayerInfo(),
    val playerInfo: PlayerInfoOld = PlayerInfoOld(), //remove
    val playerCount: String = "1",
    val plr2Cup: String = "0",
    val friendlyChat: Map<String, MsgStore> = mapOf(),
    val key: String = ""
) : Parcelable

fun GameProfile.toPlayerInfo() = PlayerInfo(
    id = playerId,
    nm = nm,
    lvl = lvlByCal(),
    coin = coin
)
