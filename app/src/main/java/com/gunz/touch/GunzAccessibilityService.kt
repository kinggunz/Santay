package com.gunz.touch

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/** Mendeteksi game yang sedang di depan: overlay otomatis muncul saat game dibuka, hilang saat keluar. */
class GunzAccessibilityService : AccessibilityService() {
    private var overlayOn = false
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg == packageName || pkg == "com.android.systemui" || pkg.contains("inputmethod") || pkg.contains("keyboard")) return
        if (Engine.isGame(this, pkg)) {
            if (!overlayOn && Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, OverlayService::class.java)); overlayOn = true
            }
            Engine.apply(this, pkg)
        } else if (overlayOn) {
            startService(Intent(this, OverlayService::class.java).setAction("STOP")); overlayOn = false
        }
    }
    override fun onInterrupt() {}
}
