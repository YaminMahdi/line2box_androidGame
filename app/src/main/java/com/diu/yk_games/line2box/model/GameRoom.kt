package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class GameRoom(
    val key: String = "",
    val ver: Version = Version.V1,
    var pingAt: Long = -1L,
    val player1: PlayerInfo = PlayerInfo(),
    val player2: PlayerInfo = PlayerInfo(),
    val playerCount: String = "1",
    val friendlyChat: Map<String, MsgStore> = mapOf(),
    val matchInfo: MatchInfo = MatchInfo()
) : Parcelable {
    @Parcelize
    @IgnoreExtraProperties
    data class MatchInfo(
        val playerTurn: PlayerColor = PlayerColor.Red,
        val result: LiveResult = LiveResult(),
        val clicks: Map<String, Line> = emptyMap(),
        val plyr1: Map<String, String> = emptyMap(),
        val plyr2: Map<String, String> = emptyMap()
    ) : Parcelable

    enum class Version {
        V1, V2, V3, V4, V5;

        val isV1: Boolean get() = this == V1
    }

    @Parcelize
    @IgnoreExtraProperties
    data class LiveResult(
        val score1: Int = 0,
        val score2: Int = 0,
        val cup1: Int = 0,
        val cup2: Int = 0
    ) : Parcelable

    @Parcelize
    @IgnoreExtraProperties
    data class Line(
        val id: String = "",
        val color: PlayerColor = PlayerColor.Red
    ) : Parcelable
}
