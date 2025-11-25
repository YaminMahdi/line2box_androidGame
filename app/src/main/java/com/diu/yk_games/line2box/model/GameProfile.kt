package com.diu.yk_games.line2box.model

import androidx.core.content.edit
import com.diu.yk_games.line2box.util.pref
import kotlin.math.sqrt

class GameProfile {
    var nm = pref.read("nm", "Noob" + (100..999).random())
    var cityNm = pref.read("cityNm", "")
    var query = pref.read("query", "")
    var matchPlayed = pref.read("matchPlayed", 0)
    var matchWinMulti = pref.read("matchWinMulti", 0)
    var coin = pref.read("coins", 100)
    var lvl = pref.read("lvl", lvlByCal())
    var playerId = pref.read("playerId", "")
    var countryEmoji = pref.read("countryEmoji", "")
    var countryNm = pref.read("countryNm", "")

    fun apply() {
        pref.preferences.edit {
            putString("nm", nm)
            putString("cityNm", cityNm)
            putString("query", query)
            putInt("coins", coin)
            putInt("matchPlayed", matchPlayed)
            putInt("matchWinMulti", matchWinMulti)
            putString("playerId", playerId)
            putString("countryEmoji", countryEmoji)
            putString("countryNm", countryNm)
        }
    }

    fun lvlByCal(): Int {
        val mul = matchWinMulti + 1
        val pld = matchPlayed + 1
        return sqrt(mul * (mul / 7.0) + pld * 2).toInt()
    }

    fun setMatchPlayed() {
        matchPlayed++
    }

    fun setMatchWinMulti() {
        matchWinMulti++
    }

}