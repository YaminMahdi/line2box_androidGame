package com.diu.yk_games.line2box

import android.app.Application
import com.chesire.lifecyklelog.LifecykleLog
import com.diu.yk_games.line2box.notification.NotificationService
import com.diu.yk_games.line2box.util.ConnectivityObserver
import com.diu.yk_games.line2box.util.SharedPreferenceUtils
import com.google.android.gms.games.PlayGamesSdk
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging

class Line2BoxApplication : Application() {
    val preference by lazy {
        SharedPreferenceUtils(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ConnectivityObserver.initialize(this)
        PlayGamesSdk.initialize(this)
        NotificationService.Channel.entries.forEach {
            Firebase.messaging.subscribeToTopic(it.id)
        }
        LifecykleLog.run {
            initialize(instance)
            requireAnnotation = false
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ConnectivityObserver.teardown()
    }

    companion object {
        lateinit var instance: Line2BoxApplication
            private set
    }
}