package dev.shovo.circlesearch

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Transparent trampoline activity used by QuickSearchTileService and system
 * Assistant intents (ASSIST / VOICE_ASSIST) to collapse the shade and trigger
 * screen capture.
 */
class TileTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        val service = NavigationGestureService.instance
        if (service != null) {
            // Allow 200ms for HyperOS notification shade collapse animation to complete cleanly
            Handler(Looper.getMainLooper()).postDelayed({
                service.triggerCircleToSearch()
            }, 200)
        } else {
            Toast.makeText(this, "Please enable Circle to Search accessibility service", Toast.LENGTH_SHORT).show()
            val dashboardIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(dashboardIntent)
        }
        finish()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
