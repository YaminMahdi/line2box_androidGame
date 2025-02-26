package com.diu.yk_games.line2box.model

import androidx.core.content.edit
import com.diu.yk_games.line2box.pref
import kotlin.math.sqrt

class GameProfile {
    var nm = pref.getString("nm", "Noob" + (100..999).random())!!
    var cityNm = pref.getString("cityNm", "")!!
    var query = pref.getString("query", "")!!
    var matchPlayed = pref.getInt("matchPlayed", 0)
    var matchWinMulti = pref.getInt("matchWinMulti", 0)
    var coin = pref.getInt("coins", 100)
    var lvl = pref.getInt("lvl", lvlByCal)
    var playerId = ""
    var countryEmoji = pref.getString("countryEmoji", "")!!
    var countryNm = pref.getString("countryNm", "")!!

    fun apply() {
        pref.edit {
            putString("nm", nm)
            putString("cityNm", cityNm)
            putString("query", query)
            putInt("coins", coin)
            putInt("matchPlayed", matchPlayed)
            putInt("matchWinMulti", matchWinMulti)
            putString("countryEmoji", countryEmoji)
            putString("countryNm", countryNm)
        }
    }

    val lvlByCal: Int
        get() {
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