package com.arlodev.onlinenow

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow

/** Package names that currently have a visible (non-ongoing) notification. */
object NotificationState {
    val packages = MutableStateFlow<Set<String>>(emptySet())
}

class NotificationService : NotificationListenerService() {

    override fun onListenerConnected() = refresh()

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onListenerDisconnected() {
        NotificationState.packages.value = emptySet()
    }

    private fun refresh() {
        NotificationState.packages.value = try {
            activeNotifications.orEmpty()
                .filter { !it.isOngoing && it.packageName != packageName }
                .map { it.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
