package com.diu.yk_games.line2box.model

data class MsgStore (
    var playerId : String = "",
    var time : Long = 0L,
    var timeData : String = "",
    var nmData : String = "",
    var msgData : String = "Blue",
    var lvlData : String = "1"
)

fun GameProfile.toMessage(text: String) =
    MsgStore(
        playerId = playerId,
        time = System.currentTimeMillis(),
        nmData = nm,
        msgData = text,
        lvlData = lvlByCal.toString()
    )