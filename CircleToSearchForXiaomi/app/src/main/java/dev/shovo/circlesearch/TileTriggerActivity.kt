package dev.shovo.circlesearch

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Transparent trampoline activity used by QuickSearchTileService to collapse
 * the system notification shade before triggering screen capture.
 */
class TileTriggerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = NavigationGestureService.instance
        if (service != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                service.triggerCircleToSearch()
            }, 100)
        }
        finish()
    }
}
