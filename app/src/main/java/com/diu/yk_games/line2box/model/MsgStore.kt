package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class MsgStore(
    @Transient
    val key: String = "",
    val playerId: String = "",
    val time: Long = 0L,
    val nmData: String = "",
    val msgData: String? = null,
    val lvlData: String = "1",
    val type: String = MessageType.Normal.name,
    val gameId: String = "",
    val user: GameProfile? = null
) : Parcelable {
    enum class MessageType {
        Normal,
        EnterText,
        ExitText,
        Invitation,
        Command,
        Bot
    }
}

val String?.typeEnum get() = MsgStore.MessageType.entries.find { it.name == this } ?: MsgStore.MessageType.Normal

fun GameProfile.toMessage(
    playerId: String,
    msg: String,
    type: MsgStore.MessageType = MsgStore.MessageType.Normal,
    user: GameProfile? = null
) =
    MsgStore(
        playerId = playerId,
        time = System.currentTimeMillis(),
        nmData = nm,
        lvlData = lvlByCal().toString(),
        msgData = msg,
        type = type.name,
        user = user
    )