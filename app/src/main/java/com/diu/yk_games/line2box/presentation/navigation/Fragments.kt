package com.diu.yk_games.line2box.presentation.navigation

import kotlinx.serialization.Serializable

data object Fragments {
    @Serializable data object Home
    @Serializable data object ScoreBoard
    @Serializable data object LeaderBoard
    @Serializable data object ChangeName

    @Serializable data object MultiPlayer

    @Serializable data object GameDual
    @Serializable data object GameBot
    @Serializable data object GameOnline
}