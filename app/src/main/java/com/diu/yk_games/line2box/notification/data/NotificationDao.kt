package com.diu.yk_games.line2box.notification.data

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT 50")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE image_bitmap IS NOT NULL ORDER BY timestamp DESC LIMIT 50")
    fun getBannerFlow(): Flow<List<NotificationEntity>>

    @Query("""
        SELECT 
            (SELECT COUNT(*) FROM notifications WHERE is_read = 0) AS unreadCount,
            (SELECT COUNT(*) FROM notifications) AS totalCount
    """)
    fun getNotificationCountsFlow(): Flow<NotificationCounts>

    @Upsert
    suspend fun upsert(notification: NotificationEntity)

    @Query("UPDATE notifications SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications WHERE id NOT IN (SELECT id FROM notifications ORDER BY timestamp DESC LIMIT 50)")
    suspend fun trimOldNotifications()
}
