package dev.shovo.circlesearch

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings Tile allowing one-tap invocation of Shovo Circle Search.
 *
 * Automatically collapses notification shade and triggers zero-prompt screen capture
 * through the active NavigationGestureService.
 */
class QuickSearchTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val service = NavigationGestureService.instance
        if (service != null) {
            // Collapse notification shade cleanly
            @Suppress("DEPRECATION")
            val collapseIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(collapseIntent)

            Handler(Looper.getMainLooper()).postDelayed({
                service.triggerCircleToSearch()
            }, 300)
        } else {
            Toast.makeText(
                this,
                "Please enable Shovo Circle Search Accessibility Service",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(intent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
