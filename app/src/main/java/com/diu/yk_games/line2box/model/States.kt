package com.diu.yk_games.line2box.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScoreBoardState(
    val list: List<Score>,
    val lastBest: String
) : Parcelable

@Parcelize
data class LeaderBoardState(
    val list: List<GameProfile>,
    val count: Long
) : Parcelable