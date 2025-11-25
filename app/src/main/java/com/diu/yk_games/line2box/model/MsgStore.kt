package com.diu.yk_games.line2box.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MsgStore(
    @Transient
    val key: String = "",
    var playerId: String = "",
    var time: Long = 0L,
    var nmData: String = "",
    var msgData: String = "Blue",
    var lvlData: String = "1",
    val type: String = Type.Normal.name,
    val gameId: String = ""
) : Parcelable {
    enum class Type {
        Normal,
        EnterText,
        ExitText,
        Invitation
    }
}

val String?.typeEnum get() = MsgStore.Type.entries.find { it.name == this } ?: MsgStore.Type.Normal

fun GameProfile.toMessage(
    playerId: String,
    msg: String,
    type: MsgStore.Type = MsgStore.Type.Normal
) =
    MsgStore(
        playerId = playerId,
        time = System.currentTimeMillis(),
        nmData = nm,
        lvlData = lvlByCal().toString(),
        msgData = msg,
        type = type.name
    )