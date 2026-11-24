package com.glass.notifpanel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.btnListener).setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
        findViewById<TextView>(R.id.btnOverlay).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        findViewById<TextView>(R.id.btnStart).setOnClickListener { startPanel(null) }
        findViewById<TextView>(R.id.btnPreview).setOnClickListener { startPanel(PanelService.ACTION_OPEN) }
        findViewById<TextView>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, PanelService::class.java))
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        val listenerOn = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val overlayOn = Settings.canDrawOverlays(this)
        findViewById<TextView>(R.id.tvListener).text =
            (if (listenerOn) "✅ " else "❌ ") + "নোটিফিকেশন অ্যাক্সেস"
        findViewById<TextView>(R.id.tvOverlay).text =
            (if (overlayOn) "✅ " else "❌ ") + "ওভারলে অনুমতি"
    }

    private fun startPanel(action: String?) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "আগে ওভারলে অনুমতি দিন", Toast.LENGTH_SHORT).show()
            return
        }
        val i = Intent(this, PanelService::class.java)
        if (action != null) i.action = action
        ContextCompat.startForegroundService(this, i)
    }
}
