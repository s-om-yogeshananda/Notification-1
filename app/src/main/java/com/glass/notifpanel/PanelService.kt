package com.glass.notifpanel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class PanelService : Service() {

    companion object {
        const val ACTION_OPEN = "com.glass.notifpanel.OPEN"
        const val ACTION_STOP = "com.glass.notifpanel.STOP"
        private const val CHANNEL = "panel_service"
    }

    private lateinit var wm: WindowManager
    private var trigger: View? = null
    private var panel: View? = null
    private var card: View? = null
    private var emptyView: View? = null
    private var adapter: NotifAdapter? = null
    private var closing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        NotifStore.onChange = { refreshList() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (trigger == null) addTrigger()
        if (intent?.action == ACTION_OPEN) showPanel()
        return START_STICKY
    }

    override fun onDestroy() {
        NotifStore.onChange = null
        removePanelNow()
        trigger?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        trigger = null
        super.onDestroy()
    }

    // ---------- Foreground notification ----------

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Notification panel", NotificationManager.IMPORTANCE_MIN)
        )
        val fl = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getForegroundService(
            this, 1, Intent(this, PanelService::class.java).setAction(ACTION_OPEN), fl
        )
        val stop = PendingIntent.getForegroundService(
            this, 2, Intent(this, PanelService::class.java).setAction(ACTION_STOP), fl
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Glass Notification চালু আছে")
            .setContentText("প্যানেল খুলতে এখানে ট্যাপ করুন")
            .setContentIntent(open)
            .addAction(0, "বন্ধ করুন", stop)
            .setOngoing(true)
            .build()
    }

    // ---------- Trigger handle (just below the status bar) ----------

    private fun addTrigger() {
        val v = FrameLayout(this)
        val pill = View(this)
        pill.background = GradientDrawable().apply {
            cornerRadius = 100f
            setColor(Color.argb(150, 255, 255, 255))
        }
        v.addView(pill, FrameLayout.LayoutParams(dp(44), dp(4), Gravity.CENTER))

        var startY = 0f
        var fired = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { startY = e.rawY; fired = false }
                MotionEvent.ACTION_MOVE -> if (!fired && e.rawY - startY > dp(30)) {
                    fired = true
                    showPanel()
                }
                MotionEvent.ACTION_UP -> if (!fired && abs(e.rawY - startY) < dp(8)) showPanel()
            }
            true
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(26),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP
        lp.y = statusBarHeight()
        try { wm.addView(v, lp); trigger = v } catch (e: Exception) { }
    }

    // ---------- Glass panel ----------

    private fun showPanel() {
        if (panel != null) return
        closing = false
        val ctx = ContextThemeWrapper(this, R.style.Theme_Glass)
        val root = LayoutInflater.from(ctx).inflate(R.layout.panel, null) as PanelRoot
        val c = root.findViewById<View>(R.id.card)
        val header = root.findViewById<View>(R.id.header)
        val rv = root.findViewById<RecyclerView>(R.id.list)
        val empty = root.findViewById<View>(R.id.empty)

        (c.layoutParams as FrameLayout.LayoutParams).topMargin = statusBarHeight() + dp(8)

        root.onBack = { hidePanel() }
        root.setOnClickListener { hidePanel() }
        root.isFocusable = true
        root.isFocusableInTouchMode = true
        root.findViewById<View>(R.id.clear).setOnClickListener { NotifStore.clearAll() }

        var sy = 0f
        header.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> sy = e.rawY
                MotionEvent.ACTION_MOVE -> if (sy - e.rawY > dp(40)) hidePanel()
            }
            true
        }

        val ad = NotifAdapter { n ->
            try { n.intent?.send() } catch (e: Exception) { }
            hidePanel()
        }
        adapter = ad
        rv.layoutManager = LinearLayoutManager(ctx)
        rv.adapter = ad

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                r: RecyclerView,
                v: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ) = false

            override fun getSwipeDirs(r: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                val item = ad.itemAt(vh.bindingAdapterPosition)
                return if (item?.clearable == true) super.getSwipeDirs(r, vh) else 0
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                ad.itemAt(vh.bindingAdapterPosition)?.let { NotifStore.dismiss(it.key) }
            }
        }).attachToRecyclerView(rv)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        lp.dimAmount = 0.35f
        if (Build.VERSION.SDK_INT >= 31) {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            lp.blurBehindRadius = 80
        }

        try {
            wm.addView(root, lp)
        } catch (e: Exception) {
            return
        }
        panel = root
        card = c
        emptyView = empty
        refreshList()
        root.requestFocus()

        c.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                c.viewTreeObserver.removeOnPreDrawListener(this)
                c.translationY = -c.height.toFloat() - dp(40)
                c.alpha = 0f
                c.animate().translationY(0f).alpha(1f)
                    .setDuration(320).setInterpolator(DecelerateInterpolator()).start()
                return true
            }
        })
    }

    private fun hidePanel() {
        val c = card
        if (panel == null || c == null || closing) return
        closing = true
        c.animate().translationY(-c.height.toFloat() - dp(40)).alpha(0f)
            .setDuration(220)
            .withEndAction { removePanelNow() }
            .start()
    }

    private fun removePanelNow() {
        panel?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        panel = null
        card = null
        emptyView = null
        adapter = null
        closing = false
    }

    private fun refreshList() {
        adapter?.submit(NotifStore.items)
        emptyView?.visibility = if (NotifStore.items.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---------- Helpers ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }
}
