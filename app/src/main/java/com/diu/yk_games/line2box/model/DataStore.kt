package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class DataStore(
    var time: Long = 0L,
    var redData: String = "Red",
    var blueData: String = "Blue",
    var starData: String = "",
    var plr1Id: String = "",
    var plr2Id: String = "",
    var plr1Cup: String = "0",
    var plr2Cup: String = "0"
) : Parcelable {
    fun toScore(): Score? {
        val red = redData.split(": ")
        val blue = blueData.split(": ")
        if (red.size != 2 || blue.size != 2) return null
        return Score(
            time = time,
            type = if (starData == "friendly") Score.Type.Friendly else Score.Type.Globe,
            player1 = PlayerInfo(id = plr1Id, nm = red.first()),
            player2 = PlayerInfo(id = plr2Id, nm = blue.first()),
            result = LiveResult(
                score1 = red.last().toInt(),
                score2 = blue.last().toInt(),
                cup1 = plr1Cup,
                cup2 = plr2Cup
            )
        )
    }
}