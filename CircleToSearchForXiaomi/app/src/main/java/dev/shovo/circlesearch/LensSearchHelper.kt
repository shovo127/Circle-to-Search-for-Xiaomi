package dev.shovo.circlesearch

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Visual search dispatcher and image cropping engine for Shovo Circle Search.
 *
 * Implements truthful multi-engine visual search:
 * - Google Lens: Direct image dispatch via LensShareEntryPointActivity or Google App.
 * - Microsoft Bing: Dispatches image to Microsoft Bing application if installed.
 * - Yandex Search: Dispatches image to Yandex application if installed.
 * - System Chooser: Dispatches image to any installed visual search / OCR application.
 *
 * Never fakes visual search by opening blank browser landing pages without the image.
 */
object LensSearchHelper {

    private const val TAG = "ShovoLensHelper"

    const val PKG_GOOGLE_APP = "com.google.android.googlequicksearchbox"
    const val PKG_MICROSOFT_BING = "com.microsoft.bing"
    const val PKG_YANDEX = "com.yandex.searchapp"

    private const val LENS_SHARE_ACTIVITY = "com.google.android.apps.search.lens.LensShareEntryPointActivity"
    private const val LENS_SEARCH_ACTION = "com.google.android.apps.photos.SEARCH_IMAGE"

    sealed class DispatchResult {
        data class Success(val providerName: String) : DispatchResult()
        data class EngineNotInstalled(val engineName: String, val targetPackage: String) : DispatchResult()
        data class Failed(val reason: String) : DispatchResult()
    }

    enum class SearchEngine(
        val key: String,
        val displayName: String,
        val targetPackage: String?
    ) {
        GOOGLE_LENS("GOOGLE_LENS", "Google Lens", PKG_GOOGLE_APP),
        BING_VISUAL("BING_VISUAL", "Microsoft Bing", PKG_MICROSOFT_BING),
        YANDEX_IMAGES("YANDEX_IMAGES", "Yandex Search", PKG_YANDEX),
        SYSTEM_CHOOSER("SYSTEM_CHOOSER", "System App Chooser", null)
    }

    fun cropBitmap(source: Bitmap, rect: RectF): Bitmap {
        val left = max(0, rect.left.toInt())
        val top = max(0, rect.top.toInt())
        val right = min(source.width, rect.right.toInt())
        val bottom = min(source.height, rect.bottom.toInt())
        val width = max(1, right - left)
        val height = max(1, bottom - top)

        return Bitmap.createBitmap(source, left, top, width, height)
    }

    fun saveCroppedBitmap(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cacheFolder = File(context.cacheDir, "crops").apply { mkdirs() }
            cleanOldCrops(cacheFolder)

            val cropFile = File(cacheFolder, "shovo_crop_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cropFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            FileProvider.getUriForFile(context, "dev.shovo.circlesearch.fileprovider", cropFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cropped bitmap: ${e.message}", e)
            null
        }
    }

    fun clearAllCrops(context: Context) {
        try {
            val cacheFolder = File(context.cacheDir, "crops")
            cacheFolder.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "All cached crop files purged")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear all crops: ${e.message}")
        }
    }

    private fun cleanOldCrops(folder: File) {
        try {
            val now = System.currentTimeMillis()
            folder.listFiles()?.forEach { file ->
                // Purge crops older than 5 minutes to prevent storage accumulation
                if (now - file.lastModified() > 5 * 60 * 1000) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old crops: ${e.message}")
        }
    }

    fun dispatchVisualSearch(context: Context, imageUri: Uri): DispatchResult {
        val prefs = context.getSharedPreferences("cts_prefs", Context.MODE_PRIVATE)
        val engineKey = prefs.getString("pref_search_engine", SearchEngine.GOOGLE_LENS.key)

        return when (engineKey) {
            SearchEngine.BING_VISUAL.key -> dispatchBing(context, imageUri)
            SearchEngine.YANDEX_IMAGES.key -> dispatchYandex(context, imageUri)
            SearchEngine.SYSTEM_CHOOSER.key -> dispatchSystemChooser(context, imageUri)
            else -> dispatchGoogleLens(context, imageUri)
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun dispatchGoogleLens(context: Context, imageUri: Uri): DispatchResult {
        if (!isPackageInstalled(context, PKG_GOOGLE_APP)) {
            Log.w(TAG, "Google App ($PKG_GOOGLE_APP) not installed on device")
            // Attempt system chooser fallback
            return dispatchSystemChooser(context, imageUri)
        }

        try {
            context.grantUriPermission(PKG_GOOGLE_APP, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w(TAG, "grantUriPermission failed: ${e.message}")
        }

        // Strategy 1: Google Lens Direct Share Activity
        val lensShareIntent = Intent(Intent.ACTION_SEND).apply {
            setClassName(PKG_GOOGLE_APP, LENS_SHARE_ACTIVITY)
            type = "image/jpeg"
            clipData = ClipData.newRawUri("image", imageUri)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(imageUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (lensShareIntent.resolveActivity(context.packageManager) != null) {
            try {
                Log.i(TAG, "Dispatching via Strategy 1: LensShareEntryPointActivity")
                context.startActivity(lensShareIntent)
                return DispatchResult.Success("Google Lens (Direct Entry)")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 1 failed: ${e.message}")
            }
        }

        // Strategy 2: Google Photos SEARCH_IMAGE Action
        val lensActionIntent = Intent(LENS_SEARCH_ACTION).apply {
            setPackage(PKG_GOOGLE_APP)
            setDataAndType(imageUri, "image/jpeg")
            clipData = ClipData.newRawUri("image", imageUri)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (lensActionIntent.resolveActivity(context.packageManager) != null) {
            try {
                Log.i(TAG, "Dispatching via Strategy 2: SEARCH_IMAGE action on Google App")
                context.startActivity(lensActionIntent)
                return DispatchResult.Success("Google Lens (Search Image Action)")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 2 failed: ${e.message}")
            }
        }

        // Strategy 3: Standard ACTION_SEND targeted to Google App
        val googleSendIntent = Intent(Intent.ACTION_SEND).apply {
            setPackage(PKG_GOOGLE_APP)
            type = "image/jpeg"
            clipData = ClipData.newRawUri("image", imageUri)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (googleSendIntent.resolveActivity(context.packageManager) != null) {
            try {
                Log.i(TAG, "Dispatching via Strategy 3: Google App SEND share")
                context.startActivity(googleSendIntent)
                return DispatchResult.Success("Google App (Image Share)")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 3 failed: ${e.message}")
            }
        }

        // Fallback to Chooser
        return dispatchSystemChooser(context, imageUri)
    }

    private fun dispatchBing(context: Context, imageUri: Uri): DispatchResult {
        if (!isPackageInstalled(context, PKG_MICROSOFT_BING)) {
            Log.w(TAG, "Microsoft Bing app not installed on device")
            return DispatchResult.EngineNotInstalled("Microsoft Bing", PKG_MICROSOFT_BING)
        }

        return try {
            context.grantUriPermission(PKG_MICROSOFT_BING, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage(PKG_MICROSOFT_BING)
                type = "image/jpeg"
                clipData = ClipData.newRawUri("image", imageUri)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DispatchResult.Success("Microsoft Bing App")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Bing image search: ${e.message}")
            DispatchResult.Failed(e.message ?: "Failed to launch Bing app")
        }
    }

    private fun dispatchYandex(context: Context, imageUri: Uri): DispatchResult {
        if (!isPackageInstalled(context, PKG_YANDEX)) {
            Log.w(TAG, "Yandex Search app not installed on device")
            return DispatchResult.EngineNotInstalled("Yandex Search", PKG_YANDEX)
        }

        return try {
            context.grantUriPermission(PKG_YANDEX, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage(PKG_YANDEX)
                type = "image/jpeg"
                clipData = ClipData.newRawUri("image", imageUri)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            DispatchResult.Success("Yandex Search App")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Yandex image search: ${e.message}")
            DispatchResult.Failed(e.message ?: "Failed to launch Yandex app")
        }
    }

    private fun dispatchSystemChooser(context: Context, imageUri: Uri): DispatchResult {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                clipData = ClipData.newRawUri("image", imageUri)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Search image using...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            DispatchResult.Success("System App Chooser")
        } catch (e: Exception) {
            Log.e(TAG, "System chooser launch failed: ${e.message}", e)
            DispatchResult.Failed(e.message ?: "System chooser failed")
        }
    }
}
