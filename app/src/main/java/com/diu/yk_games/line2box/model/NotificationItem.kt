package com.diu.yk_games.line2box.model

import android.os.Parcelable
import com.diu.yk_games.line2box.notification.data.NotificationEntity
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    @SerializedName("image_link")
    val imageLink: String? = null,
    val phone: String? = null,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null,
    val topic: String? = null,
    val timestamp: Long = -1L,
    val imageBitmap: ByteArray? = null,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationItem

        if (timestamp != other.timestamp) return false
        if (isRead != other.isRead) return false
        if (isDeleted != other.isDeleted) return false
        if (id != other.id) return false
        if (title != other.title) return false
        if (body != other.body) return false
        if (type != other.type) return false
        if (imageLink != other.imageLink) return false
        if (phone != other.phone) return false
        if (redirectUrl != other.redirectUrl) return false
        if (topic != other.topic) return false
        if (!imageBitmap.contentEquals(other.imageBitmap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + isRead.hashCode()
        result = 31 * result + isDeleted.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + imageLink.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + redirectUrl.hashCode()
        result = 31 * result + topic.hashCode()
        result = 31 * result + (imageBitmap?.contentHashCode() ?: 0)
        return result
    }

    fun toEntity(): NotificationEntity = NotificationEntity(
        id = id.ifBlank { System.currentTimeMillis().toString() },
        title = title,
        body = body,
        type = type,
        imageLink = imageLink,
        phone = phone,
        redirectUrl = redirectUrl,
        topic = topic,
        timestamp = timestamp,
        imageBitmap = imageBitmap,
        isRead = isRead,
        isDeleted = isDeleted
    )

}
