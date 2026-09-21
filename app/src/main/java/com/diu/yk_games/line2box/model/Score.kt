package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Score (
    var time : Long = -1L,
    var type : Type = Type.Friendly,
    val player1: PlayerInfo = PlayerInfo(),
    val player2: PlayerInfo = PlayerInfo(),
    val result: LiveResult = LiveResult(),
): Parcelable {
    enum class Type {
        Friendly,
        Globe;
        val isFriendly get() = this == Friendly
    }
}