package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.IgnoreExtraProperties as IgnoreExtraPropertiesFS

@Parcelize
@IgnoreExtraProperties
@IgnoreExtraPropertiesFS
data class PlayerInfo(
    val id: String = "",
    val nm: String = "",
    val lvl: Int = 0,
    val coin: Int = 0,
    val seenAt: Long = -1L  // -1L means never seen, -2L means left
) : Parcelable {
    fun shouldEnter(ver: GameRoom.Version) =
            (!ver.isV1 && seenAt < 0) ||
            (ver.isV1 && id.isEmpty())
}
