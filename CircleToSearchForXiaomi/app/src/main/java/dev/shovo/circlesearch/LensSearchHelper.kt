package dev.shovo.circlesearch

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object LensSearchHelper {

    private const val TAG = "LensSearchHelper"
    private const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val LENS_SHARE_ACTIVITY = "com.google.android.apps.search.lens.LensShareEntryPointActivity"
    private const val LENS_SEARCH_ACTION = "com.google.android.apps.photos.SEARCH_IMAGE"

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

            val cropFile = File(cacheFolder, "search_crop_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cropFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            FileProvider.getUriForFile(context, "dev.shovo.circlesearch.fileprovider", cropFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cropped bitmap: ${e.message}", e)
            null
        }
    }

    private fun cleanOldCrops(folder: File) {
        try {
            val now = System.currentTimeMillis()
            folder.listFiles()?.forEach { file ->
                // Delete files older than 5 minutes to prevent storage bloat
                if (now - file.lastModified() > 5 * 60 * 1000) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old crops: ${e.message}")
        }
    }

    fun launchGoogleLensSearch(context: Context, imageUri: Uri): Boolean {
        try {
            context.grantUriPermission(GOOGLE_PACKAGE, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            Log.w(TAG, "grantUriPermission failed: ${e.message}")
        }

        val uriList = arrayListOf(imageUri)

        // Strategy 1: Direct Google Photos/Lens Search Image Action (Native on Google App)
        val lensActionIntent = Intent(LENS_SEARCH_ACTION).apply {
            setPackage(GOOGLE_PACKAGE)
            setDataAndType(imageUri, "image/jpeg")
            clipData = ClipData.newRawUri("image", imageUri)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (lensActionIntent.resolveActivity(context.packageManager) != null) {
            try {
                Log.i(TAG, "Launching Strategy 1: SEARCH_IMAGE action on Google App")
                context.startActivity(lensActionIntent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 1 failed: ${e.message}")
            }
        }

        // Strategy 2: Direct LensShareEntryPointActivity
        val lensShareIntent = Intent(Intent.ACTION_SEND).apply {
            setClassName(GOOGLE_PACKAGE, LENS_SHARE_ACTIVITY)
            type = "image/jpeg"
            clipData = ClipData.newRawUri("image", imageUri)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (lensShareIntent.resolveActivity(context.packageManager) != null) {
            try {
                Log.i(TAG, "Launching Strategy 2: LensShareEntryPointActivity")
                context.startActivity(lensShareIntent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Strategy 2 failed: ${e.message}")
            }
        }

        // Strategy 3: General ACTION_SEND targeted to Google App
        val googleSendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            clipData = ClipData.newRawUri("image", imageUri)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
            setPackage(GOOGLE_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            Log.i(TAG, "Launching Strategy 3: Google App share intent")
            context.startActivity(googleSendIntent)
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Strategy 3 failed: ${e.message}")
        }

        // Strategy 4: Universal Chooser Fallback
        return try {
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                clipData = ClipData.newRawUri("image", imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Search image with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "All visual search launch attempts failed: ${e.message}", e)
            false
        }
    }
}
