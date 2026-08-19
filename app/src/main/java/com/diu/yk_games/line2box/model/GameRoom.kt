package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class GameRoom(
    val key: String = "",
    val pingAt: Long = System.currentTimeMillis(),
    val player1: PlayerInfo = PlayerInfo(),
    val player2: PlayerInfo = PlayerInfo(),
    val playerCount: String = "1",
    val friendlyChat: Map<String, MsgStore> = mapOf(),
    val matchInfo: MatchInfo = MatchInfo()
) : Parcelable {
    @Parcelize
    @IgnoreExtraProperties
    data class MatchInfo(
        val plyr1: Map<String, String> = emptyMap(),
        val plyr2: Map<String, String> = emptyMap()
    ) : Parcelable
}
