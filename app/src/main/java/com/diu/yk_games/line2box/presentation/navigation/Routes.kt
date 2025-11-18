package com.diu.yk_games.line2box.presentation.navigation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable data object Home: Routes()
    @Serializable data object ScoreBoard: Routes()
    @Serializable data object LeaderBoard: Routes()
    @Serializable data object ChangeName: Routes()

    @Serializable data object MultiPlayer: Routes()

    @Serializable data object GameDual: Routes()
    @Serializable data object GameBot: Routes()
    @Serializable data object GameOnline: Routes()
}

val <T> KSerializer<T>.route
    get() = descriptor.serialName

val String?.asRoute : Routes?
    get() = when(this) {
        Routes.Home.serializer().route -> Routes.Home
        Routes.ScoreBoard.serializer().route -> Routes.ScoreBoard
        Routes.LeaderBoard.serializer().route -> Routes.LeaderBoard
        Routes.ChangeName.serializer().route -> Routes.ChangeName
        Routes.MultiPlayer.serializer().route -> Routes.MultiPlayer
        Routes.GameDual.serializer().route -> Routes.GameDual
        Routes.GameBot.serializer().route -> Routes.GameBot
        Routes.GameOnline.serializer().route -> Routes.GameOnline
        else -> null
    }
