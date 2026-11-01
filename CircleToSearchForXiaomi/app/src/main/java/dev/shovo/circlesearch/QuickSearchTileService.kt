package dev.shovo.circlesearch

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings Tile for one-tap invocation of Circle to Search for Xiaomi.
 *
 * Uses official TileService.startActivityAndCollapse APIs without deprecated/restricted
 * ACTION_CLOSE_SYSTEM_DIALOGS broadcast, properly collapsing the shade before capture.
 */
class QuickSearchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            val isServiceActive = NavigationGestureService.instance != null
            tile.state = if (isServiceActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isServiceActive) "Ready" else "Tap to Enable"
            }
            tile.updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()

        val service = NavigationGestureService.instance
        if (service != null) {
            val intent = Intent(this, TileTriggerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } else {
            Toast.makeText(
                this,
                "Please enable Circle to Search Accessibility Service",
                Toast.LENGTH_LONG
            ).show()

            val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    settingsIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(settingsIntent)
            }
        }
    }
}
