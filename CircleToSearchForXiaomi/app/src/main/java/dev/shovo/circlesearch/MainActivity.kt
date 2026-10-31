package dev.shovo.circlesearch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvDevice = findViewById<TextView>(R.id.tvDeviceStatus)
        val tvHyperOs = findViewById<TextView>(R.id.tvHyperOsStatus)
        val btnEnable = findViewById<Button>(R.id.btnEnableOverlay)
        val btnTest = findViewById<Button>(R.id.btnTestSearch)

        val model = Build.MODEL
        val incremental = Build.VERSION.INCREMENTAL

        tvDevice.text = "Device: Xiaomi 13 ($model)"
        tvHyperOs.text = "Firmware: HyperOS 3 ($incremental)"

        btnEnable.setOnClickListener {
            // Open Accessibility Settings to enable the Navigation Handle Service
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        btnTest.setOnClickListener {
            val intent = Intent(this, CircleSearchActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }
}

