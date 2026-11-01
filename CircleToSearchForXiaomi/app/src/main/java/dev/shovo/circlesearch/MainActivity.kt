package dev.shovo.circlesearch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast

/**
 * Transparent launcher trampoline. HyperOS Back Tap launches this activity;
 * it invokes Google's native Circle to Search and exits without showing a dashboard.
 */
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var triggerQueued = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideTransition()
        queueTrigger()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        queueTrigger()
    }

    private fun queueTrigger() {
        if (triggerQueued) return
        triggerQueued = true
        handler.postDelayed({
            when (val result = OmniTrigger.invoke(this)) {
                OmniTrigger.Result.Triggered -> {
                    Log.i(TAG, "Google Circle to Search invocation accepted.")
                }
                OmniTrigger.Result.GoogleNotDefault -> {
                    Toast.makeText(
                        this,
                        "Set Google as the default Digital Assistant, then use Double Back Tap again.",
                        Toast.LENGTH_LONG
                    ).show()
                    openAssistantSettings()
                }
                OmniTrigger.Result.Rejected -> {
                    Toast.makeText(
                        this,
                        "Google rejected Circle to Search for this device. Check Google app support and eligibility.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.w(TAG, "VoiceInteractionManagerService rejected the invocation.")
                }
                is OmniTrigger.Result.Failed -> {
                    Toast.makeText(
                        this,
                        "Circle to Search could not start: " + result.reason,
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "Native invocation failed: " + result.reason)
                }
            }
            finish()
        }, TRIGGER_DELAY_MS)
    }

    private fun openAssistantSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (e: Exception) {
                Log.e(TAG, "Could not open default assistant settings.", e)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hideTransition() {
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CircleToSearchTrigger"
        private const val TRIGGER_DELAY_MS = 100L
    }
}
