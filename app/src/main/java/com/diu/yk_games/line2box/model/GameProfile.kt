package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.diu.yk_games.line2box.util.asMap
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlin.math.sqrt

@Parcelize
@IgnoreExtraProperties
data class GameProfile(
    var nm: String = "Noob" + (100..999).random(),
    var cityNm: String = "",
    var query: String = "",  // IP Address
    var matchPlayed: Int = 0,
    var matchWinMulti: Int = 0,
    var coin: Int = 100,
    var lvl: Int = 0,
    var playerId: String = "",
    var countryEmoji: String = "",
    var countryNm: String = ""
) : Parcelable {
    fun lvlByCal(): Int {
        val mul = matchWinMulti + 1
        val pld = matchPlayed + 1
        return sqrt(mul * (mul / 7.0) + pld * 2).toInt()
    }


    fun toPlayerInfo() = PlayerInfo(
        id = playerId,
        nm = nm,
        lvl = lvlByCal(),
        coin = coin,
        seenAt = System.currentTimeMillis()
    )

    fun toPlayerInfoDB() = toPlayerInfo().asMap().plus("seenAt" to ServerValue.TIMESTAMP)

    override fun toString(): String {
        return buildString {
            appendLine("=============================")
            appendLine("Line2Box User Profile")
            appendLine("=============================")
            appendLine("Player Name  : $nm")
            appendLine("City                 : ${cityNm.ifEmpty { "Not set" }}")
            appendLine("Country          : ${countryNm.ifEmpty { "Not set" }}")
            appendLine("Player ID        : ${playerId.ifEmpty { "Not assigned" }}")
            appendLine("----------------------------------------")
            appendLine("Statistics")
            appendLine("----------------------------------------")
            appendLine("Matches Played : $matchPlayed")
            appendLine("Multiplayer Win  : $matchWinMulti")
            appendLine("Coins                   : $coin")
            appendLine("Level                    : $lvl")
            appendLine("----------------------------------------")
            appendLine("IP Address          : 118.179.0.201")
            appendLine("=============================")
        }
    }
}
