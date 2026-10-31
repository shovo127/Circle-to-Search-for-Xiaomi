package dev.shovo.circlesearch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvDevice: TextView
    private lateinit var tvHyperOs: TextView
    private lateinit var btnEnable: Button
    private lateinit var btnTest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDevice = findViewById(R.id.tvDeviceStatus)
        tvHyperOs = findViewById(R.id.tvHyperOsStatus)
        btnEnable = findViewById(R.id.btnEnableOverlay)
        btnTest = findViewById(R.id.btnTestSearch)

        val model = Build.MODEL
        val incremental = Build.VERSION.INCREMENTAL

        tvDevice.text = "Device: Xiaomi 13 ($model)"
        tvHyperOs.text = "Firmware: HyperOS 3 ($incremental) | Android ${Build.VERSION.RELEASE}"

        updateStatus()

        btnEnable.setOnClickListener {
            val isEnabled = isAccessibilityServiceEnabled(this, NavigationGestureService::class.java)
            if (isEnabled) {
                Toast.makeText(this, "Accessibility Service is active & monitoring bottom hold", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        btnTest.setOnClickListener {
            val service = NavigationGestureService.instance
            if (service != null) {
                // Minimize MainActivity and trigger screenshot via service
                moveTaskToBack(true)
                window.decorView.postDelayed({
                    service.triggerCircleToSearch()
                }, 300)
            } else {
                val intent = Intent(this, CircleSearchActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, NavigationGestureService::class.java)
        if (isEnabled) {
            btnEnable.text = "Accessibility Active (Monitoring Bottom Hold)"
            btnEnable.isEnabled = true
        } else {
            btnEnable.text = "Enable Accessibility Service"
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expected = "${context.packageName}/${serviceClass.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
