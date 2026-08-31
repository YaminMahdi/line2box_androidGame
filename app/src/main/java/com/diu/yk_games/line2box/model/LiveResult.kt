package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.IgnoreExtraProperties as IgnoreExtraPropertiesFS

@Parcelize
@IgnoreExtraProperties
@IgnoreExtraPropertiesFS
data class LiveResult(
    val score1: Int = 0,
    val score2: Int = 0,
    val cup1: String = "0",
    val cup2: String = "0"
) : Parcelable