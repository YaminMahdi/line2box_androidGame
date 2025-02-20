package com.diu.yk_games.line2box.model

data class PlayerInfo(
    val nm1: String = "",
    val lvl1: Int = 0,
    val nm2: String = "",
    val lvl2: Int = 0
)

data class GameRoom(
    val playerInfo: PlayerInfo = PlayerInfo(),
    val playerCount: String = "1",
)
