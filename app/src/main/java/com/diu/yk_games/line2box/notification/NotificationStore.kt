package com.diu.yk_games.line2box.notification

import com.diu.yk_games.line2box.Line2BoxApplication
import com.diu.yk_games.line2box.model.NotificationItem
import com.diu.yk_games.line2box.notification.data.NotificationCounts
import com.diu.yk_games.line2box.notification.data.NotificationDatabase
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object NotificationStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dao by lazy {
        NotificationDatabase.getInstance(Line2BoxApplication.instance).notificationDao()
    }

    val notifications: StateFlow<PersistentList<NotificationItem>> by lazy {
        dao.getAllFlow()
            .map { list -> list.map { it.toItem() }.toPersistentList() }
//            .map { dummyItems  }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = persistentListOf()
            )
    }

    val notificationCounts: StateFlow<NotificationCounts> by lazy {
        dao.getNotificationCountsFlow()
//            .map { NotificationCounts(totalCount = 46, unreadCount = 14) }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = NotificationCounts()
            )
    }

    fun addOrUpdateNotification(notification: NotificationItem) {
        scope.launch {
            dao.upsert(notification.toEntity())
            dao.trimOldNotifications()
        }
    }

    fun removeNotification(id: String) {
        scope.launch {
            dao.deleteById(id)
        }
    }

    fun clearAll() {
        scope.launch {
            dao.deleteAll()
        }
    }

    fun markAllAsRead() {
        scope.launch {
            dao.markAllAsRead()
        }
    }

    fun markAsRead(id: String) {
        scope.launch {
            dao.markAsRead(id)
        }
    }
}
