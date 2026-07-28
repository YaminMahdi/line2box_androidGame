package com.diu.yk_games.line2box

import android.app.Application
import com.chesire.lifecyklelog.LifecykleLog
import com.diu.yk_games.line2box.util.ConnectivityObserver
import com.diu.yk_games.line2box.util.SharedPreferenceUtils
import com.google.android.gms.games.PlayGamesSdk

class Line2BoxApplication : Application() {
    val preference by lazy {
        SharedPreferenceUtils(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ConnectivityObserver.initialize(this)
        PlayGamesSdk.initialize(this)
        LifecykleLog.run {
            initialize(instance)
            requireAnnotation = false
        }
    }

    companion object {
        lateinit var instance: Line2BoxApplication
            private set
    }
}