package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class HadithStore(
    var e: String = "",
    var en: String = "",
    var b: String = "",
    var bn: String = "",
    var ref: String = "",
    var src: String = "",
    var t: String = "q"
) : Parcelable