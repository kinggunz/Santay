package com.gunz.touch

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : android.app.Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var status: TextView? = null
    private val poll = object : Runnable { override fun run() { status?.text = Engine.statusText(this@MainActivity); h.postDelayed(this, 1500) } }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(Color.parseColor("#111827"))
            addView(ImageView(context).apply { setImageResource(R.drawable.ic_launcher) }, LinearLayout.LayoutParams(300, 300))
            addView(TextView(context).apply { text = "GUNZ TOUCH"; textSize = 26f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        }
        setContentView(splash)
        splash.setOnClickListener { showMain() }
        h.postDelayed({ showMain() }, 1500)
    }

    override fun onDestroy() { h.removeCallbacks(poll); super.onDestroy() }

    private fun gate(action: () -> Unit) {
        if (Engine.ready(this)) action()
        else Toast.makeText(this, "Aktifkan salah satu dulu: Shizuku atau Accessibility", Toast.LENGTH_LONG).show()
    }

    private fun showMain() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 30, 40, 30); setBackgroundColor(Color.parseColor("#111827")) }
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }
        status = TextView(this).apply { setTextColor(Color.parseColor("#FF5A1F")); textSize = 14f }
        root.addView(status)
        root.addView(btn("OPSI 1: AKTIFKAN ACCESSIBILITY") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        root.addView(btn("OPSI 2: HUBUNGKAN SHIZUKU") {
            try {
                if (!Engine.shizukuRunning()) {
                    Toast.makeText(this, "Jalankan Shizuku dulu (Wireless debugging / root)", Toast.LENGTH_LONG).show()
                    packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let { startActivity(it) }
                } else Shizuku.requestPermission(100)
            } catch (_: Throwable) {}
        })
        root.addView(btn("IZIN OVERLAY & WRITE_SETTINGS") {
            if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            else if (!Settings.System.canWrite(this)) startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            else Toast.makeText(this, "Semua izin sudah aktif", Toast.LENGTH_SHORT).show()
        })
        root.addView(btn("SENSI MENU (X & Y)") { gate { AlertDialog.Builder(this).setView(buildSensiView(this) { }).show() } })
        root.addView(btn("FLOATING + CROSSHAIR: ON") { gate {
            if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            else startForegroundService(Intent(this, OverlayService::class.java)) } })
        root.addView(btn("FLOATING: OFF") { startService(Intent(this, OverlayService::class.java).setAction("STOP")) })
        root.addView(TextView(this).apply { text = "Game Launcher"; setTextColor(Color.WHITE); textSize = 16f; setPadding(0, 20, 0, 5) })
        val pm = packageManager
        val apps = pm.getInstalledApplications(0).filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != packageName && Engine.isGame(this, it.packageName) }
        val lv = ListView(this)
        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, apps.map { pm.getApplicationLabel(it).toString() })
        lv.setOnItemClickListener { _, _, i, _ -> gate { Engine.apply(this, apps[i].packageName); startActivity(pm.getLaunchIntentForPackage(apps[i].packageName)) } }
        root.addView(lv)
        setContentView(ScrollView(this).apply { addView(root) })
        h.removeCallbacks(poll); h.post(poll)
    }
}
