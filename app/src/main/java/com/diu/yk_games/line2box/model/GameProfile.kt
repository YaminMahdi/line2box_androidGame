package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlin.math.sqrt

@Parcelize
@IgnoreExtraProperties
data class GameProfile(
    var nm: String = "Noob" + (100..999).random(),
    var cityNm: String = "",
    var query: String = "",
    var matchPlayed: Int = 0,
    var matchWinMulti: Int = 0,
    var coin: Int = 100,
    var lvl: Int = 0,
    var playerId: String = "",
    var countryEmoji: String = "",
    var countryNm: String = ""
): Parcelable {
    fun lvlByCal(): Int {
        val mul = matchWinMulti + 1
        val pld = matchPlayed + 1
        return sqrt(mul * (mul / 7.0) + pld * 2).toInt()
    }
}