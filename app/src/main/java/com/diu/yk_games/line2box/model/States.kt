package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.diu.yk_games.line2box.base.GameProfileListParceler
import com.diu.yk_games.line2box.base.ScoreListParceler
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
data class ScoreBoardState(
    @TypeParceler<PersistentList<Score>, ScoreListParceler>
    val friendlyMatches: PersistentList<Score> = persistentListOf(),
    @TypeParceler<PersistentList<Score>, ScoreListParceler>
    val globalMatches: PersistentList<Score> = persistentListOf(),
    val isLoading: Boolean = false,
    val lastBest: String = ""
) : Parcelable

@Parcelize
data class LeaderBoardState(
    @TypeParceler<PersistentList<GameProfile>, GameProfileListParceler>
    val list: PersistentList<GameProfile> = persistentListOf(),
    val count: Long = 0,
    val error: Throwable? = null
) : Parcelable