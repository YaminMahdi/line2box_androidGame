package com.diu.yk_games.line2box.util

object Constants {
    const val PLAY_STORE = "com.android.vending"
    const val PLAY_SERVICES = "com.google.android.gms"
    const val PLAY_GAMES = "com.google.android.play.games"
    const val IP_INFO_URL = "http://ip-api.com/json/?fields=country,city,query"
    const val RESTART_YOUTUBE_URL = "https://youtu.be/G9r2YZTBlCE"
    const val DAY1_MILLIS = 86400000L

    val PLAY_STORE_APP_URL get() = getPlayStoreUrl(PLAY_STORE)
    val PLAY_SERVICES_APP_URL get() = getPlayStoreUrl(PLAY_SERVICES)
    val PLAY_GAMES_APP_URL get() = getPlayStoreUrl(PLAY_GAMES)

    fun getPlayStoreUrl(packageName: String) =
        "https://play.google.com/store/apps/details?id=$packageName"
}