package com.diu.yk_games.line2box.model

import com.diu.yk_games.line2box.pref
import com.diu.yk_games.line2box.prefEditor
import kotlin.math.floor
import kotlin.math.sqrt

class GameProfile {
    var nm = pref.getString("nm", "Noob" + floor(Math.random() * 900 + 100).toInt())!!
    var cityNm = pref.getString("cityNm", "")!!
    var query = pref.getString("query", "")!!
    var matchPlayed = pref.getInt("matchPlayed", 0)
    var matchWinMulti = pref.getInt("matchWinMulti", 0)
    var coin = pref.getInt("coins", 100)
    var lvl = pref.getInt("lvl", lvlByCal)
    var playerId = ""
    var countryEmoji = ""
    var countryNm = ""

    fun apply() {
        prefEditor.putString("nm", nm).apply()
        prefEditor.putString("cityNm", cityNm).apply()
        prefEditor.putString("query", query).apply()
        prefEditor.putInt("coins", coin).apply()
        prefEditor.putInt("matchPlayed", matchPlayed).apply()
        prefEditor.putInt("matchWinMulti", matchWinMulti).apply()
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