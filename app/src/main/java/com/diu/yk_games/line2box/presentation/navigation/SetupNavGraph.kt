package com.diu.yk_games.line2box.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.fragment
import com.diu.yk_games.line2box.presentation.main.DisplayFragment
import com.diu.yk_games.line2box.presentation.main.StartFragment
import com.diu.yk_games.line2box.presentation.offline.GameDualFragment
import com.diu.yk_games.line2box.presentation.offline.NameInfoFragment
import com.diu.yk_games.line2box.presentation.online.LeaderBoardFragment

fun NavController.setupNavGraph() {
    graph = createGraph(
        startDestination = Routes.Home
    ) {
        fragment<StartFragment, Routes.Home>()
        fragment<DisplayFragment, Routes.ScoreBoard>()
        fragment<LeaderBoardFragment, Routes.LeaderBoard>()
        fragment<NameInfoFragment, Routes.ChangeName>()
        fragment<StartFragment, Routes.MultiPlayer>()
        fragment<GameDualFragment, Routes.GameDual>()
    }
}