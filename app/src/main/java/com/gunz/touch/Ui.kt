package com.gunz.touch

import android.content.Context
import android.graphics.Color
import android.provider.Settings
import android.view.Gravity
import android.widget.*

fun sp(c: Context) = c.getSharedPreferences("gunz", 0)


fun buildSensiView(ctx: Context, onApply: () -> Unit): LinearLayout {
    val p = sp(ctx)
    val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 30, 40, 30); setBackgroundColor(Color.parseColor("#E6111827")) }
    fun label(t: String) = TextView(ctx).apply { text = t; setTextColor(Color.WHITE); textSize = 14f }
    fun slider(key: String, name: String) {
        val lb = label("$name: ${p.getInt(key, 50)}%")
        val sb = SeekBar(ctx).apply { max = 100; progress = p.getInt(key, 50)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { lb.text = "$name: $v%"; p.edit().putInt(key, v).apply() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            }) }
        root.addView(lb); root.addView(sb)
    }
    root.addView(label("GUNZ TOUCH - SENSI").apply { textSize = 18f; gravity = Gravity.CENTER })
    slider("x", "Sumbu X (Horizontal)")
    slider("y", "Sumbu Y (Vertikal)")
    root.addView(Switch(ctx).apply { text = "Crosshair (+)"; setTextColor(Color.WHITE); isChecked = p.getBoolean("cross", true)
        setOnCheckedChangeListener { _, b -> p.edit().putBoolean("cross", b).apply() } })
    root.addView(Button(ctx).apply { text = "KONFIRMASI / APPLY"; setOnClickListener { Engine.apply(ctx); onApply() } })
    return root
}
