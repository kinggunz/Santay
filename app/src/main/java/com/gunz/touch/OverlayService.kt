package com.gunz.touch

import android.app.*
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var cross: TextView? = null
    private var orient: View? = null
    private var btn: TextView? = null
    private var panel: View? = null

    private fun lp(w: Int, h: Int, touchable: Boolean) = WindowManager.LayoutParams(w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE), PixelFormat.TRANSLUCENT)

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("gunz", "Gunz Touch", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "gunz").setContentTitle("Gunz Touch aktif")
            .setSmallIcon(android.R.drawable.ic_menu_compass).build())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (orient == null) { // paksa landscape lewat overlay 1x1
            orient = View(this)
            wm.addView(orient, lp(1, 1, false).apply { screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE })
        }
        if (btn == null) addButton()
        refreshCross()
        return START_STICKY
    }

    private fun refreshCross() {
        val on = sp(this).getBoolean("cross", true)
        if (on && cross == null) {
            cross = TextView(this).apply { text = "+"; textSize = 30f; setTextColor(Color.RED); gravity = Gravity.CENTER }
            wm.addView(cross, lp(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, false).apply { gravity = Gravity.CENTER })
        } else if (!on && cross != null) { wm.removeView(cross); cross = null }
    }

    private fun addButton() {
        val p = lp(120, 120, true).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 200 }
        btn = TextView(this).apply {
            text = "GT"; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(Color.parseColor("#CCFF5A1F")) }
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
            setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false }
                    MotionEvent.ACTION_MOVE -> { p.x = ox + (e.rawX - sx).toInt(); p.y = oy + (e.rawY - sy).toInt()
                        if (Math.abs(e.rawX - sx) > 10) moved = true; wm.updateViewLayout(v, p) }
                    MotionEvent.ACTION_UP -> if (!moved) togglePanel()
                }; true }
        }
        wm.addView(btn, p)
    }

    private fun togglePanel() {
        if (panel != null) { wm.removeView(panel); panel = null; return }
        panel = buildSensiView(this) { refreshCross(); togglePanel() }
        wm.addView(panel, lp(700, WindowManager.LayoutParams.WRAP_CONTENT, true).apply { gravity = Gravity.CENTER; flags = flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv() })
    }

    override fun onDestroy() {
        listOf(cross, orient, btn, panel).forEach { v -> try { if (v != null) wm.removeView(v) } catch (_: Exception) {} }
        super.onDestroy()
    }
    override fun onBind(i: Intent?): IBinder? = null
}
