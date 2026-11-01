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
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Visual search dispatcher and image cropping engine for Circle to Search for Xiaomi.
 *
 * Implements truthful multi-engine visual search:
 * - Google Lens: Direct image dispatch via standalone Lens, LensShareEntryPointActivity, or Google App.
 * - Microsoft Bing: Dispatches image to Microsoft Bing application if verified installed.
 * - Yandex Search: Dispatches image to Yandex application if verified installed.
 * - System Chooser: Dispatches image to any installed visual search / OCR application.
 *
 * Adheres to Honest Provider Protocol: Never fakes visual search by opening blank browser URLs.
 */
object LensSearchHelper {

    private const val TAG = "LensSearchHelper"

    const val PKG_GOOGLE_APP = "com.google.android.googlequicksearchbox"
    const val PKG_STANDALONE_LENS = "com.google.ar.lens"
    const val PKG_MICROSOFT_BING = "com.microsoft.bing"
    const val PKG_YANDEX = "com.yandex.searchapp"

    private const val LENS_SHARE_ACTIVITY = "com.google.android.apps.search.lens.LensShareEntryPointActivity"
    private const val LENS_SEARCH_ACTION = "com.google.android.apps.photos.SEARCH_IMAGE"

    sealed class DispatchResult {
        data class HandedOff(val providerName: String) : DispatchResult()
        object ChooserOpened : DispatchResult()
        data class ProviderUnavailable(val reason: String) : DispatchResult()
        data class DispatchFailure(val error: String) : DispatchResult()
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

    /**
     * Crops [source] to the pixel boundaries defined by [rect].
     * Note: If [rect] spans the entire source dimensions, [Bitmap.createBitmap] may return
     * the [source] instance directly. Callers must check `cropped !== source` before recycling.
     */
    fun cropBitmap(source: Bitmap, rect: RectF): Bitmap? {
        if (source.isRecycled) {
            Log.e(TAG, "Source bitmap is recycled, cropping rejected")
            return null
        }
        if (rect.isEmpty || !rect.left.isFinite() || !rect.top.isFinite() ||
            !rect.right.isFinite() || !rect.bottom.isFinite()) {
            Log.e(TAG, "Crop rect is invalid or non-finite: $rect")
            return null
        }

        val left = max(0, min(floor(rect.left).toInt(), source.width - 1))
        val top = max(0, min(floor(rect.top).toInt(), source.height - 1))
        val right = max(left + 1, min(source.width, ceil(rect.right).toInt()))
        val bottom = max(top + 1, min(source.height, ceil(rect.bottom).toInt()))
        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Crop dimensions invalid: ${width}x${height}")
            return null
        }

        return try {
            Bitmap.createBitmap(source, left, top, width, height)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create cropped bitmap: ${e.message}", e)
            null
        }
    }

    fun saveCroppedBitmap(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cacheFolder = File(context.cacheDir, "crops").apply { mkdirs() }
            cleanOldCrops(cacheFolder)

            val cropFile = File(cacheFolder, "crop_${UUID.randomUUID()}.jpg")
            val compressed = FileOutputStream(cropFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (!compressed) {
                Log.e(TAG, "Bitmap compression failed")
                if (cropFile.exists()) cropFile.delete()
                return null
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
                // Purge crops older than 10 minutes so receiving apps have ample time to read
                if (now - file.lastModified() > 10 * 60 * 1000) {
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
        // Strategy 1: Check standalone Google Lens app first if installed
        if (isPackageInstalled(context, PKG_STANDALONE_LENS)) {
            try {
                context.grantUriPermission(PKG_STANDALONE_LENS, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val standaloneIntent = Intent(Intent.ACTION_SEND).apply {
                    setPackage(PKG_STANDALONE_LENS)
                    type = "image/jpeg"
                    clipData = ClipData.newRawUri("image", imageUri)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (standaloneIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(standaloneIntent)
                    return DispatchResult.HandedOff("Google Lens (Standalone App)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Standalone Lens dispatch failed: ${e.message}")
            }
        }

        if (!isPackageInstalled(context, PKG_GOOGLE_APP)) {
            Log.w(TAG, "Neither Google App nor Standalone Lens is installed on device")
            return dispatchSystemChooser(context, imageUri)
        }

        try {
            context.grantUriPermission(PKG_GOOGLE_APP, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w(TAG, "grantUriPermission failed: ${e.message}")
        }

        // Strategy 2: Google Lens Direct Share Activity with ArrayList extra
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
                Log.i(TAG, "Dispatching via Strategy 2: LensShareEntryPointActivity")
                context.startActivity(lensShareIntent)
                return DispatchResult.HandedOff("Google Lens (Direct Entry)")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 2 failed: ${e.message}")
            }
        }

        // Strategy 3: Google Photos SEARCH_IMAGE Action
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
                Log.i(TAG, "Dispatching via Strategy 3: SEARCH_IMAGE action on Google App")
                context.startActivity(lensActionIntent)
                return DispatchResult.HandedOff("Google Lens (Search Image Action)")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 3 failed: ${e.message}")
            }
        }

        // Strategy 4: Standard ACTION_SEND targeted to Google App
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
                Log.i(TAG, "Dispatching via Strategy 4: Google App SEND share")
                context.startActivity(googleSendIntent)
                return DispatchResult.HandedOff("Google App (Image Share)")
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 4 failed: ${e.message}")
            }
        }

        // Fallback to Universal Chooser
        return dispatchSystemChooser(context, imageUri)
    }

    private fun dispatchBing(context: Context, imageUri: Uri): DispatchResult {
        if (!isPackageInstalled(context, PKG_MICROSOFT_BING)) {
            Log.w(TAG, "Microsoft Bing app not installed on device")
            return DispatchResult.ProviderUnavailable("Microsoft Bing app is not installed. Please install it or select Google Lens.")
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
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                DispatchResult.HandedOff("Microsoft Bing App")
            } else {
                DispatchResult.ProviderUnavailable("Microsoft Bing app cannot receive image shares.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Bing image search: ${e.message}")
            DispatchResult.DispatchFailure(e.message ?: "Failed to launch Bing app")
        }
    }

    private fun dispatchYandex(context: Context, imageUri: Uri): DispatchResult {
        if (!isPackageInstalled(context, PKG_YANDEX)) {
            Log.w(TAG, "Yandex Search app not installed on device")
            return DispatchResult.ProviderUnavailable("Yandex Search app is not installed. Please install it or select Google Lens.")
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
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                DispatchResult.HandedOff("Yandex Search App")
            } else {
                DispatchResult.ProviderUnavailable("Yandex Search app cannot receive image shares.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Yandex image search: ${e.message}")
            DispatchResult.DispatchFailure(e.message ?: "Failed to launch Yandex app")
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
            DispatchResult.ChooserOpened
        } catch (e: Exception) {
            Log.e(TAG, "System chooser launch failed: ${e.message}", e)
            DispatchResult.DispatchFailure(e.message ?: "System chooser failed")
        }
    }
}
