package com.gunz.touch

import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import rikka.shizuku.Shizuku

/** Dua backend: Shizuku dan Accessibility. Salah satu aktif = aplikasi bisa dipakai. */
object Engine {
    fun shizukuRunning() = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
    fun shizukuReady() = shizukuRunning() && try {
        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }

    fun accessReady(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return s.contains("${c.packageName}/${GunzAccessibilityService::class.java.name}") ||
            s.contains("${c.packageName}/.GunzAccessibilityService")
    }

    fun ready(c: Context) = shizukuReady() || accessReady(c)
    fun statusText(c: Context) =
        "Shizuku: ${if (shizukuReady()) "ON" else if (shizukuRunning()) "belum diizinkan" else "OFF"}   |   " +
        "Accessibility: ${if (accessReady(c)) "ON" else "OFF"}"

    fun isGame(c: Context, pkg: String): Boolean = try {
        val i = c.packageManager.getApplicationInfo(pkg, 0)
        (i.flags and ApplicationInfo.FLAG_IS_GAME != 0) || i.category == ApplicationInfo.CATEGORY_GAME ||
            pkg.contains("freefire") || pkg.contains("pubg") || sp(c).getStringSet("games", emptySet())!!.contains(pkg)
    } catch (_: Exception) { false }

    private fun shell(cmd: String) = Thread {
        try {
            val m = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            m.isAccessible = true
            (m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process).waitFor()
        } catch (_: Throwable) {}
    }.start()

    /** Terapkan sensi. Shizuku: pointer_speed + refresh rate 120Hz + game mode performance. Tanpa Shizuku: pointer_speed via WRITE_SETTINGS. */
    fun apply(c: Context, gamePkg: String? = null) {
        val p = sp(c)
        val avg = (p.getInt("x", 50) + p.getInt("y", 50)) / 2f
        val v = ((avg / 100f) * 14 - 7).toInt()
        if (shizukuReady()) {
            val game = if (gamePkg != null) "; cmd game mode performance $gamePkg" else ""
            shell("settings put system pointer_speed $v; settings put system min_refresh_rate 120; settings put system peak_refresh_rate 120$game")
        } else try {
            if (Settings.System.canWrite(c)) Settings.System.putInt(c.contentResolver, "pointer_speed", v)
        } catch (_: Exception) {}
    }
}
