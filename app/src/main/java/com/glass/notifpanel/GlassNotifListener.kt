package com.glass.notifpanel

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class GlassNotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NotifStore.listener = this
        refresh()
    }

    override fun onListenerDisconnected() {
        NotifStore.listener = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val list = try {
            activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        NotifStore.update(list.mapNotNull { toItem(it) }.sortedByDescending { it.time })
    }

    private fun toItem(sbn: StatusBarNotification): NotifItem? {
        if (sbn.packageName == packageName) return null
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return null
        val pm = packageManager
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }
        val icon = try { pm.getApplicationIcon(sbn.packageName) } catch (e: Exception) { null }
        return NotifItem(
            key = sbn.key,
            pkg = sbn.packageName,
            app = appName,
            title = title,
            text = text,
            time = sbn.postTime,
            icon = icon,
            intent = n.contentIntent,
            clearable = sbn.isClearable
        )
    }
}
