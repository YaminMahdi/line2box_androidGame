package com.diu.yk_games.line2box

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class Line2BoxApplication : Application() {
    val pref: SharedPreferences by lazy { applicationContext.getSharedPreferences(
        getString(R.string.preference_file_key), Context.MODE_PRIVATE
    ) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: Line2BoxApplication
            private set
    }
}

val pref: SharedPreferences by lazy {
    Line2BoxApplication.instance.pref
}
val prefEditor: SharedPreferences.Editor by lazy {
    Line2BoxApplication.instance.pref.edit()
}