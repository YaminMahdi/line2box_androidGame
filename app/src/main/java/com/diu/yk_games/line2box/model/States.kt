package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.diu.yk_games.line2box.base.PersistentListParceler
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
data class ScoreBoardState(
    @TypeParceler<PersistentList<Score>, PersistentListParceler<Score>>
    val friendlyMatches: PersistentList<Score> = persistentListOf(),
    @TypeParceler<PersistentList<Score>, PersistentListParceler<Score>>
    val globalMatches: PersistentList<Score> = persistentListOf(),
    val lastBest: String = "",
    val error: Throwable? = null
) : Parcelable

@Parcelize
data class LeaderBoardState(
    @TypeParceler<PersistentList<GameProfile>, PersistentListParceler<GameProfile>>
    val list: PersistentList<GameProfile> = persistentListOf(),
    val count: Long = 0,
    val error: Throwable? = null
) : Parcelable