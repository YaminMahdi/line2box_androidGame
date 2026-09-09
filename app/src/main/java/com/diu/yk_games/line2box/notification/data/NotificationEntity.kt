package com.diu.yk_games.line2box.notification.data

import androidx.compose.ui.graphics.asImageBitmap
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.diu.yk_games.line2box.model.Banner
import com.diu.yk_games.line2box.model.NotificationItem
import com.diu.yk_games.line2box.notification.toBitmap

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String = "",
    val body: String = "",
    val type: String = "",
    @ColumnInfo(name = "image_link")
    val imageLink: String? = null,
    val phone: String? = null,
    @ColumnInfo(name = "redirect_url")
    val redirectUrl: String? = null,
    val topic: String? = null,
    val timestamp: Long = -1L,
    @ColumnInfo(name = "image_bitmap")
    val imageBitmap: ByteArray? = null,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false
) {
    fun toItem(): NotificationItem = NotificationItem(
        id = id,
        title = title,
        body = body,
        type = type,
        imageLink = imageLink,
        phone = phone,
        redirectUrl = redirectUrl,
        topic = topic,
        timestamp = timestamp,
        imageBitmap = imageBitmap,
        isRead = isRead
    )

    fun toBanner(): Banner? = Banner(
        redirectUrl = redirectUrl,
        imageBitmap = imageBitmap?.toBitmap()?.asImageBitmap() ?: return null
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationEntity

        if (timestamp != other.timestamp) return false
        if (isRead != other.isRead) return false
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
}
