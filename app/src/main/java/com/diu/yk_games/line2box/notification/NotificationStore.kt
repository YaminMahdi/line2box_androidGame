package com.diu.yk_games.line2box.notification

import com.diu.yk_games.line2box.Line2BoxApplication
import com.diu.yk_games.line2box.model.Banner
import com.diu.yk_games.line2box.model.NotificationItem
import com.diu.yk_games.line2box.notification.data.NotificationCounts
import com.diu.yk_games.line2box.notification.data.NotificationDatabase
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object NotificationStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dao by lazy {
        NotificationDatabase.getInstance(Line2BoxApplication.instance).notificationDao()
    }

    val notifications: StateFlow<PersistentList<NotificationItem>> by lazy {
        dao.getAllFlow().toItemStateFlow { it.toItem() }
    }

    val banners: StateFlow<PersistentList<Banner>> by lazy {
        dao.getBannerFlow().toItemStateFlow { it.toBanner() }
    }

    val notificationCounts: StateFlow<NotificationCounts> by lazy {
        dao.getNotificationCountsFlow()
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

    /**
     * Shared conversion for entity flows -> eagerly-collected PersistentList StateFlow.
     * Builds the persistent list in a single pass via a mutate builder instead of
     * map(transform).toPersistentList(), which allocates an intermediate ArrayList.
     */
    private fun <T, R> Flow<List<T>>.toItemStateFlow(
        transform: (T) -> R?
    ): StateFlow<PersistentList<R>> =
        map { list ->
            persistentListOf<R>().mutate { builder ->
                for (entity in list) transform(entity)?.let { builder.add(it) }
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = persistentListOf()
        )
}