package com.diu.yk_games.line2box.model

import android.content.SharedPreferences
import kotlin.math.floor
import kotlin.math.sqrt

class GameProfile {
    @JvmField var nm = sharedPreferences.getString("nm", "Noob" + floor(Math.random() * 900 + 100).toInt())!!
    @JvmField var cityNm = sharedPreferences.getString("cityNm", "")!!
    @JvmField var query = sharedPreferences.getString("query", "")!!
    @JvmField var matchPlayed = sharedPreferences.getInt("matchPlayed", 0)
    @JvmField var matchWinMulti = sharedPreferences.getInt("matchWinMulti", 0)
    @JvmField var coin = sharedPreferences.getInt("coins", 100)
    @JvmField var lvl = sharedPreferences.getInt("lvl", lvlByCal)
    @JvmField var playerId = ""
    @JvmField var countryEmoji = ""
    @JvmField var countryNm = ""

    fun apply() {
        preferencesEditor.putString("nm", nm).apply()
        preferencesEditor.putString("cityNm", cityNm).apply()
        preferencesEditor.putString("query", query).apply()
        preferencesEditor.putInt("coins", coin).apply()
        preferencesEditor.putInt("matchPlayed", matchPlayed).apply()
        preferencesEditor.putInt("matchWinMulti", matchWinMulti).apply()
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

    companion object {
        lateinit var sharedPreferences: SharedPreferences
        lateinit var preferencesEditor: SharedPreferences.Editor

        @JvmStatic fun setPreferences(x: SharedPreferences) {
            sharedPreferences = x
            preferencesEditor = sharedPreferences.edit()
        }
    }
}