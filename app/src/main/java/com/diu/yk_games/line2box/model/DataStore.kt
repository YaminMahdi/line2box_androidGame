package com.diu.yk_games.line2box.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DataStore (
    @JvmField var time : Long = 0L,
    @JvmField var timeData : String = "",
    @JvmField var redData : String = "Red",
    @JvmField var blueData : String = "Blue",
    @JvmField var starData : String = "",
    @JvmField var plr1Id : String = "",
    @JvmField var plr2Id : String = "",
    @JvmField var plr1Cup : String = "",
    @JvmField var plr2Cup : String = ""
): Parcelable