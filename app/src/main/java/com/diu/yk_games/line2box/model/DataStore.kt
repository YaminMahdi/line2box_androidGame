package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class DataStore (
    var time : Long = 0L,
    var redData : String = "Red",
    var blueData : String = "Blue",
    var starData : String = "",
    var plr1Id : String = "",
    var plr2Id : String = "",
    var plr1Cup : String = "0",
    var plr2Cup : String = "0"
): Parcelable