package com.diu.yk_games.line2box.model

data class PlayerInfo(
    val id: String = "",
    val nm: String = "",
    val lvl: Int = 0,
    val coin: Int = 0,
)

data class PlayerInfoOld(
    val nm1: String = "",
    val lvl1: Int = 0,
    val plr1Id : String = "",

    val nm2: String = "",
    val lvl2: Int = 0,
    val plr2Id : String = ""
)

data class GameRoom(
    val player1: PlayerInfo = PlayerInfo(),
    val player2: PlayerInfo = PlayerInfo(),
    val playerInfo: PlayerInfoOld = PlayerInfoOld(), //remove
    val playerCount: String = "1",
    val plr2Cup: String = "0",
    val key: String = ""
)

fun GameProfile.toPlayerInfo()= PlayerInfo(
    id = playerId,
    nm = nm,
    lvl = lvlByCal,
    coin = coin
)
