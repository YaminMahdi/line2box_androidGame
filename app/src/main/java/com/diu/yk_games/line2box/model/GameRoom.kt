package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.diu.yk_games.line2box.presentation.navigation.Routes
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

    fun toRoutes(
        playerId: String,
        watchOnly: Boolean = false
    ) = Routes.GameOnline(
        gameKey = key,
        plr1Id = player1.id,
        nm1 = player1.nm,
        lvl1 = player1.lvl,
        plr2Id = player2.id,
        nm2 = player2.nm,
        lvl2 = player2.lvl,
        watchOnly = watchOnly,
        isPlyr1 = playerId == player1.id
    )

    fun toRoutes(gameProfile: GameProfile): Routes.GameOnline {
        val isPlyr1 =
            player1.id == gameProfile.playerId || player1.id.isEmpty() || player1.seenAt < 0
        return Routes.GameOnline(
            gameKey = key,
            plr1Id = if (isPlyr1) gameProfile.playerId else player1.id,
            nm1 = if (isPlyr1) gameProfile.nm else player1.nm,
            lvl1 = if (isPlyr1) gameProfile.lvlByCal() else player1.lvl,
            plr2Id = if (isPlyr1) player2.id else gameProfile.playerId,
            nm2 = if (isPlyr1) player2.nm else gameProfile.nm,
            lvl2 = if (isPlyr1) player2.lvl else gameProfile.lvlByCal(),
            isPlyr1 = isPlyr1
        )
    }
}
