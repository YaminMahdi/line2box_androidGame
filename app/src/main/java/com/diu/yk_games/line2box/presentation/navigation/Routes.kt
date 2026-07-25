package com.diu.yk_games.line2box.presentation.navigation

import com.diu.yk_games.line2box.util.isTrue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable data object Home: Routes()
    @Serializable data object ScoreBoard: Routes()
    @Serializable data object LeaderBoard: Routes()
    @Serializable data object ChangeName: Routes()

    @Serializable data object MultiPlayer: Routes()

    @Serializable data class GameDual(val nm1: String="", val nm2: String=""): Routes()
    @Serializable data object GameBot: Routes()
    @Serializable data class GameOnline(
        var gameKey: String = "",
        var isPlyr1: Boolean = false,
        var plr1Id: String = "",
        var plr2Id: String = "",
        var nm1: String = "",
        var nm2: String = "",
        var lvl1: Int = 0,
        var lvl2: Int = 0
    ): Routes()
}

val <T> KSerializer<T>.route
    get() = descriptor.serialName

val String?.asRoute: Routes?
    get() = when (this) {
        Routes.Home.serializer().route -> Routes.Home
        Routes.ScoreBoard.serializer().route -> Routes.ScoreBoard
        Routes.LeaderBoard.serializer().route -> Routes.LeaderBoard
        Routes.ChangeName.serializer().route -> Routes.ChangeName
        Routes.MultiPlayer.serializer().route -> Routes.MultiPlayer
        Routes.GameBot.serializer().route -> Routes.GameBot
        else -> if (this?.startsWith(Routes.GameDual.serializer().route).isTrue())
            Routes.GameDual()
        else if (this?.startsWith(Routes.GameOnline.serializer().route).isTrue())
            Routes.GameOnline()
        else
            null
    }
