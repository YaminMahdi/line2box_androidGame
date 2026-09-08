package com.diu.yk_games.line2box.notification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import com.diu.yk_games.line2box.model.NotificationItem
import com.google.gson.Gson
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "NotificationUtils"

/**
 * Converts a Map<String, String> (such as RemoteMessage.data) to an Android Bundle.
 */
fun Map<String, String>.toBundle(): Bundle {
    val bundle = Bundle()
    for ((key, value) in this) {
        bundle.putString(key, value)
    }
    return bundle
}

/**
 * Downloads a Bitmap from a URL string synchronously (or on IO context).
 */
fun String.getBitmap(): Bitmap? {
    if (this.isBlank()) return null
    return try {
        runBlocking(Dispatchers.IO) {
            val url = URL(this@getBitmap)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.connect()
            connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to download image bitmap from $this", e)
        null
    }
}

/**
 * Utility to log any object as formatted JSON.
 */
fun Any.logJson(tag: String = "NotificationJson") {
    try {
        val json = Gson().toJson(this)
        Log.d(tag, json)
    } catch (_: Exception) {
        Log.d(tag, this.toString())
    }
}

/**
 * Compresses a Bitmap into a ByteArray.
 */
fun Bitmap.toByteArray(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): ByteArray {
    val stream = java.io.ByteArrayOutputStream()
    compress(format, quality, stream)
    return stream.toByteArray()
}

/**
 * Decodes a ByteArray back into a Bitmap.
 */
fun ByteArray.toBitmap(): Bitmap? {
    return BitmapFactory.decodeByteArray(this, 0, size)
}

val dummyItems = persistentListOf(
    // 1. Regular notification with image
    NotificationItem(
        id = "1",
        title = "New Message from John",
        body = "Hey! Are you free for lunch today? I was thinking about that new Italian place downtown.",
        type = "message",
        imageLink = "https://example.com/images/avatar_john.jpg",
        phone = "+1234567890",
        redirectUrl = "app://chat/john",
        topic = "personal",
        isRead = false
    ),

    // 2. Promotional notification
    NotificationItem(
        id = "2",
        title = "🔥 50% OFF Flash Sale!",
        body = "Don't miss out! Premium subscription at half price. Offer ends in 24 hours!",
        type = "promotion",
        imageLink = "https://example.com/images/sale_banner.jpg",
        redirectUrl = "app://promo/flash-sale",
        topic = "promotions",
        isRead = false
    ),

    // 3. System/Update notification
    NotificationItem(
        id = "3",
        title = "App Update Available",
        body = "Version 3.2.1 is ready to install. New features: Dark mode, improved performance, and bug fixes.",
        type = "system",
        imageLink = null,
        redirectUrl = "app://settings/updates",
        topic = "system",
        isRead = true
    ),

    // 4. Social media notification
    NotificationItem(
        id = "4",
        title = "Sarah liked your photo",
        body = "Sarah Johnson liked your recent post: 'Sunset at the beach 🌅'",
        type = "social",
        imageLink = "https://example.com/images/sarah_avatar.jpg",
        phone = null,
        redirectUrl = "app://social/post/123",
        topic = "social",
        isRead = false
    ),

    // 5. Reminder notification
    NotificationItem(
        id = "5",
        title = "⏰ Meeting Reminder",
        body = "Team standup meeting in 15 minutes. Join the video call using the link below.",
        type = "reminder",
        imageLink = null,
        redirectUrl = "app://calendar/event/456",
        topic = "work",
        isRead = false
    ),

    // 6. News notification
    NotificationItem(
        id = "6",
        title = "Breaking News: Tech Innovation",
        body = "Groundbreaking AI model released today, promising to revolutionize the industry.",
        type = "news",
        imageLink = "https://example.com/images/news_ai.jpg",
        redirectUrl = "app://news/article/789",
        topic = "technology",
        isRead = true
    ),

    // 7. Order status update
    NotificationItem(
        id = "7",
        title = "📦 Order Delivered",
        body = "Your order #ORD-2024-001 has been delivered successfully. Thank you for shopping with us!",
        type = "order",
        imageLink = "https://example.com/images/package.jpg",
        phone = "+9876543210",
        redirectUrl = "app://orders/2024-001",
        topic = "shopping",
        isRead = true
    ),

    // 8. Friend request
    NotificationItem(
        id = "8",
        title = "New Friend Request",
        body = "Alex Chen wants to connect with you. Accept or decline?",
        type = "social",
        imageLink = "https://example.com/images/alex_avatar.jpg",
        phone = null,
        redirectUrl = "app://social/friend-request/567",
        topic = "social",
        isRead = false
    ),

    // 9. Payment notification
    NotificationItem(
        id = "9",
        title = "💰 Payment Received",
        body = "You've received $250.00 from Mark Wilson for the freelance project.",
        type = "payment",
        imageLink = null,
        phone = "+1122334455",
        redirectUrl = "app://wallet/transaction/890",
        topic = "finance",
        isRead = false
    ),

    // 10. Weather alert
    NotificationItem(
        id = "10",
        title = "🌧️ Weather Alert",
        body = "Heavy rain expected in your area. Please stay indoors and avoid unnecessary travel.",
        type = "alert",
        imageLink = "https://example.com/images/weather_rain.jpg",
        redirectUrl = "app://weather/alert",
        topic = "weather",
        isRead = false
    ),

    // 11. App recommendation
    NotificationItem(
        id = "11",
        title = "🎮 New Game Available",
        body = "Try out 'Space Adventure' - the latest hit game with stunning graphics and immersive gameplay!",
        type = "recommendation",
        imageLink = "https://example.com/images/game_thumb.jpg",
        redirectUrl = "app://store/game/space-adventure",
        topic = "gaming",
        isRead = true
    ),

    // 12. Security notification
    NotificationItem(
        id = "12",
        title = "🔐 Security Alert",
        body = "New login detected from an unrecognized device. Was this you? Check your account security.",
        type = "security",
        imageLink = null,
        phone = null,
        redirectUrl = "app://settings/security",
        topic = "security",
        isRead = false
    )
)
