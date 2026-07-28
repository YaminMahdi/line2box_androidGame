package com.diu.yk_games.line2box.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.fragment
import com.diu.yk_games.line2box.presentation.bot.GameBotFragment
import com.diu.yk_games.line2box.presentation.main.ScoreBoardFragment
import com.diu.yk_games.line2box.presentation.main.StartFragment
import com.diu.yk_games.line2box.presentation.offline.GameDualFragment
import com.diu.yk_games.line2box.presentation.offline.NameInfoFragment
import com.diu.yk_games.line2box.presentation.online.GameOnlineFragment
import com.diu.yk_games.line2box.presentation.online.LeaderBoardFragment
import com.diu.yk_games.line2box.presentation.online.MultiplayerFragment

fun NavController.setupNavGraph() {
    graph = createGraph(
        startDestination = Routes.Home
    ) {
        fragment<StartFragment, Routes.Home>()
        fragment<ScoreBoardFragment, Routes.ScoreBoard>()
        fragment<LeaderBoardFragment, Routes.LeaderBoard>()
        fragment<NameInfoFragment, Routes.ChangeName>()
        fragment<MultiplayerFragment, Routes.MultiPlayer>()

        fragment<GameDualFragment, Routes.GameDual>()
        fragment<GameBotFragment, Routes.GameBot>()
        fragment<GameOnlineFragment, Routes.GameOnline>()
    }
}