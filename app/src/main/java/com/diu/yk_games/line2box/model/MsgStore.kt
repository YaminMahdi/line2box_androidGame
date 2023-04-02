package com.diu.yk_games.line2box.model

data class MsgStore (
    @JvmField var playerId : String = "",
    @JvmField var timeData : String = "",
    @JvmField var nmData : String = "",
    @JvmField var msgData : String = "Blue",
    @JvmField var lvlData : String = "1"
)