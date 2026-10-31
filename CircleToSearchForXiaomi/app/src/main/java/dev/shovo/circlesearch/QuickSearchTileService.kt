package dev.shovo.circlesearch

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService

class QuickSearchTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val service = NavigationGestureService.instance
        if (service != null) {
            // Collapse shade and trigger screenshot
            val collapseIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(collapseIntent)

            Handler(Looper.getMainLooper()).postDelayed({
                service.triggerCircleToSearch()
            }, 300)
        } else {
            val intent = Intent(this, CircleSearchActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
