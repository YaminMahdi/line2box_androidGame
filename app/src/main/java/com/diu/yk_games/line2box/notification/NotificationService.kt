package com.diu.yk_games.line2box.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.NotificationItem
import com.diu.yk_games.line2box.presentation.MainActivity
import com.diu.yk_games.line2box.presentation.island.DynamicIslandController
import com.diu.yk_games.line2box.util.pref
import com.diu.yk_games.line2box.util.tryGet
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class NotificationService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Channel.entries.forEach { channel ->
            Log.d(TAG, "onCreate NotificationService: $channel")
            val notificationChannel = NotificationChannel(
                channel.id,
                channel.nm,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                enableLights(true)
                lightColor = Color.WHITE
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        pref.save("fcm_token", token)
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // Can be expanded to upload token to Firestore/RTDB for targeted push
        Log.d(TAG, "sendRegistrationToServer: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: getString(R.string.app_name)
        val body = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: ""
        val type = remoteMessage.data["type"] ?: ""
        val imageLink = remoteMessage.data["image_link"]
            ?: remoteMessage.notification?.imageUrl?.toString()
            ?: ""
        val imageBitmap = tryGet { imageLink.getBitmap() }
        val imageBytes = imageBitmap?.toByteArray()
        val phone = remoteMessage.data["phone"] ?: ""
        val channel = Channel.entries.find { it.id == type } ?: Channel.Others

        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Data: ${remoteMessage.data}")

        val notificationItem = try {
            if (remoteMessage.data.isNotEmpty()) {
                Gson().fromJson(Gson().toJson(remoteMessage.data), NotificationItem::class.java)
                    .copy(imageBitmap = imageBytes)
            } else {
                NotificationItem(
                    title = title,
                    body = body,
                    type = channel.id,
                    imageLink = imageLink.ifBlank { null },
                    phone = phone.ifBlank { null },
                    imageBitmap = imageBytes
                )
            }
        } catch (_: Exception) {
            NotificationItem(
                title = title,
                body = body,
                type = channel.id,
                imageLink = imageLink.ifBlank { null },
                phone = phone.ifBlank { null },
                imageBitmap = imageBytes
            )
        }.copy(
            id = remoteMessage.messageId ?: System.currentTimeMillis().toString(),
            topic = remoteMessage.from?.removePrefix("/topics/")
        )

        // Intent to launch MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtras(
                remoteMessage.data.toMutableMap()
                .also { it["type"] = channel.id }
                .toBundle()
            )
            putExtra(JSON, Gson().toJson(notificationItem))
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "Message Notification: ${remoteMessage.messageId}")

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, channel.id)
                .setContentTitle(title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setColor(ContextCompat.getColor(this, R.color.blueY))
                .setContentIntent(pendingIntent)
                .apply {
                    setContentText(body)
                    if (imageBitmap != null) {
                        setLargeIcon(imageBitmap)
                        setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(imageBitmap)
                                .bigLargeIcon(null as Bitmap?)
                        )
                    }
                }

        // Show in-app banner if active
        MainScope().launch {
            DynamicIslandController.message(
                text = body.ifBlank { title },
                name = title
            )
        }

        // Post system status bar notification
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (lastTitle != title || System.currentTimeMillis() - lastTime > 2000) {
                NotificationManagerCompat.from(this).notify(notifyId++, notificationBuilder.build())
            }
        } else {
            Log.d(TAG, "onMessageReceived: POST_NOTIFICATIONS permission denied")
        }

        lastTime = System.currentTimeMillis()
        lastTitle = title

        // Persist notification data
        try {
            notificationItem.logJson()
            NotificationStore.addOrUpdateNotification(notificationItem)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist notification", e)
        }
    }

    enum class Channel(val id: String) {
        GameInvite("invite"),
        Chats("chat"),
        Updates("update"),
        Promotions("promotion"),
        ProductCategoryPromotion("category"),
        ProductPromotion("product"),
        Others("other")
    }

    companion object {
        const val JSON = "notification_json"
        private const val TAG = "FirebaseNotificationService"
        private var notifyId = 0
        private var lastTime = 0L
        private var lastTitle = ""

        val Channel.nm: String
            get() {
                val nm = this.name
                if (nm.isNotEmpty()) {
                    val nmLst = nm.drop(1).toCharArray().toMutableList()
                    nmLst.filter { it.isUpperCase() }.onEach {
                        nmLst.add(nmLst.indexOf(it), ' ')
                    }
                    return nm[0] + nmLst.joinToString("")
                }
                return ""
            }
    }
}
