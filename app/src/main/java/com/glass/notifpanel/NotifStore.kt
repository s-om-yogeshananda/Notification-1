package com.glass.notifpanel

import android.app.PendingIntent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper

data class NotifItem(
    val key: String,
    val pkg: String,
    val app: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: Drawable?,
    val intent: PendingIntent?,
    val clearable: Boolean
)

object NotifStore {
    var items: List<NotifItem> = emptyList()
    var listener: GlassNotifListener? = null
    var onChange: (() -> Unit)? = null
    private val main = Handler(Looper.getMainLooper())

    fun update(list: List<NotifItem>) {
        items = list
        main.post { onChange?.invoke() }
    }

    fun dismiss(key: String) {
        try { listener?.cancelNotification(key) } catch (e: Exception) { }
    }

    fun clearAll() {
        try { listener?.cancelAllNotifications() } catch (e: Exception) { }
    }
}
