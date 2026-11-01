package dev.shovo.circlesearch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Main Dashboard and Configuration Center for Shovo Circle Search.
 *
 * Provides real-time Accessibility Service monitoring, activation method selection,
 * hold duration calibration (300-750ms), multi-engine visual search provider selection,
 * haptic controls, cache maintenance, and about information.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvDevice: TextView
    private lateinit var tvHyperOs: TextView
    private lateinit var tvServiceStatus: TextView
    private lateinit var btnEnable: Button
    private lateinit var btnBatteryOptimization: Button

    private lateinit var rgActivation: RadioGroup
    private lateinit var rbBottomHold: RadioButton
    private lateinit var rbQsTile: RadioButton
    private lateinit var rbAssistant: RadioButton

    private lateinit var layoutHoldSettings: LinearLayout
    private lateinit var tvHoldThreshold: TextView
    private lateinit var btnHoldDec: Button
    private lateinit var btnHoldInc: Button

    private lateinit var btnOpenGestureSettings: Button
    private lateinit var btnOpenAssistantSettings: Button

    private lateinit var rgSearchEngine: RadioGroup
    private lateinit var rbEngineGoogle: RadioButton
    private lateinit var rbEngineBing: RadioButton
    private lateinit var rbEngineYandex: RadioButton
    private lateinit var rbEngineChooser: RadioButton

    private lateinit var switchHaptics: SwitchMaterial
    private lateinit var btnTest: Button
    private lateinit var btnClearCache: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupDeviceInfo()
        loadPreferences()
        setupListeners()
    }

    private fun bindViews() {
        tvDevice = findViewById(R.id.tvDeviceStatus)
        tvHyperOs = findViewById(R.id.tvHyperOsStatus)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnEnable = findViewById(R.id.btnEnableOverlay)
        btnBatteryOptimization = findViewById(R.id.btnBatteryOptimization)

        rgActivation = findViewById(R.id.rgActivationMethod)
        rbBottomHold = findViewById(R.id.rbBottomHold)
        rbQsTile = findViewById(R.id.rbQsTile)
        rbAssistant = findViewById(R.id.rbAssistant)

        layoutHoldSettings = findViewById(R.id.layoutHoldSettings)
        tvHoldThreshold = findViewById(R.id.tvHoldThreshold)
        btnHoldDec = findViewById(R.id.btnHoldDec)
        btnHoldInc = findViewById(R.id.btnHoldInc)

        btnOpenGestureSettings = findViewById(R.id.btnOpenGestureSettings)
        btnOpenAssistantSettings = findViewById(R.id.btnOpenAssistantSettings)

        rgSearchEngine = findViewById(R.id.rgSearchEngine)
        rbEngineGoogle = findViewById(R.id.rbEngineGoogle)
        rbEngineBing = findViewById(R.id.rbEngineBing)
        rbEngineYandex = findViewById(R.id.rbEngineYandex)
        rbEngineChooser = findViewById(R.id.rbEngineChooser)

        switchHaptics = findViewById(R.id.switchHaptics)
        btnTest = findViewById(R.id.btnTestSearch)
        btnClearCache = findViewById(R.id.btnClearCache)
    }

    private fun setupDeviceInfo() {
        val model = Build.MODEL
        val incremental = Build.VERSION.INCREMENTAL
        tvDevice.text = "Device: Xiaomi 13 ($model)"
        tvHyperOs.text = "Firmware: HyperOS 3 ($incremental) • Android ${Build.VERSION.RELEASE}"
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)

        // Activation Method
        when (prefs.getString("pref_activation_method", "BOTTOM_HOLD")) {
            "QS_TILE" -> rbQsTile.isChecked = true
            "ASSISTANT" -> rbAssistant.isChecked = true
            else -> rbBottomHold.isChecked = true
        }
        updateHoldVisibility()

        // Hold threshold
        val threshold = prefs.getLong("pref_hold_threshold_ms", 450L)
        updateThresholdDisplay(threshold)

        // Search engine
        when (prefs.getString("pref_search_engine", LensSearchHelper.SearchEngine.GOOGLE_LENS.key)) {
            LensSearchHelper.SearchEngine.BING_VISUAL.key -> rbEngineBing.isChecked = true
            LensSearchHelper.SearchEngine.YANDEX_IMAGES.key -> rbEngineYandex.isChecked = true
            LensSearchHelper.SearchEngine.SYSTEM_CHOOSER.key -> rbEngineChooser.isChecked = true
            else -> rbEngineGoogle.isChecked = true
        }

        // Haptics
        switchHaptics.isChecked = prefs.getBoolean("pref_haptic_feedback", true)
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)

        btnEnable.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        btnBatteryOptimization.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open App Info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rgActivation.setOnCheckedChangeListener { _, checkedId ->
            val selectedMethod = when (checkedId) {
                R.id.rbQsTile -> "QS_TILE"
                R.id.rbAssistant -> "ASSISTANT"
                else -> "BOTTOM_HOLD"
            }
            prefs.edit().putString("pref_activation_method", selectedMethod).apply()
            updateHoldVisibility()
            NavigationGestureService.instance?.updateGestureZoneConfiguration()
        }

        btnHoldDec.setOnClickListener {
            var current = prefs.getLong("pref_hold_threshold_ms", 450L)
            if (current > 300) {
                current -= 50
                prefs.edit().putLong("pref_hold_threshold_ms", current).apply()
                updateThresholdDisplay(current)
            }
        }

        btnHoldInc.setOnClickListener {
            var current = prefs.getLong("pref_hold_threshold_ms", 450L)
            if (current < 750) {
                current += 50
                prefs.edit().putLong("pref_hold_threshold_ms", current).apply()
                updateThresholdDisplay(current)
            }
        }

        btnOpenGestureSettings.setOnClickListener {
            try {
                startActivity(Intent("android.settings.QUICK_LAUNCH_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }

        btnOpenAssistantSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }

        rgSearchEngine.setOnCheckedChangeListener { _, checkedId ->
            val engine = when (checkedId) {
                R.id.rbEngineBing -> LensSearchHelper.SearchEngine.BING_VISUAL.key
                R.id.rbEngineYandex -> LensSearchHelper.SearchEngine.YANDEX_IMAGES.key
                R.id.rbEngineChooser -> LensSearchHelper.SearchEngine.SYSTEM_CHOOSER.key
                else -> LensSearchHelper.SearchEngine.GOOGLE_LENS.key
            }
            prefs.edit().putString("pref_search_engine", engine).apply()
        }

        switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_haptic_feedback", isChecked).apply()
        }

        btnTest.setOnClickListener {
            val service = NavigationGestureService.instance
            if (service != null) {
                moveTaskToBack(true)
                window.decorView.postDelayed({
                    service.triggerCircleToSearch()
                }, 300)
            } else {
                Toast.makeText(
                    this,
                    "Accessibility Service is not enabled. Please enable it first.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        btnClearCache.setOnClickListener {
            LensSearchHelper.clearAllCrops(this)
            Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHoldVisibility() {
        layoutHoldSettings.visibility = if (rbBottomHold.isChecked) View.VISIBLE else View.GONE
    }

    private fun updateThresholdDisplay(thresholdMs: Long) {
        tvHoldThreshold.text = getString(R.string.hold_threshold_label, thresholdMs.toInt())
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, NavigationGestureService::class.java)
        if (isEnabled) {
            tvServiceStatus.text = "Accessibility Service: Active (Connected)"
            tvServiceStatus.setTextColor(getColor(R.color.cyan_glow))
            btnEnable.text = "Accessibility Active"
        } else {
            tvServiceStatus.text = "Accessibility Service: Inactive"
            tvServiceStatus.setTextColor(getColor(R.color.google_red))
            btnEnable.text = getString(R.string.start_service)
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
