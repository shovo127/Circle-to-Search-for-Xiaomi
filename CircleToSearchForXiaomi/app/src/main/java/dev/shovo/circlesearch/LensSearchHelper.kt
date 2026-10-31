package dev.shovo.circlesearch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object LensSearchHelper {

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
            val cropFile = File(cacheFolder, "search_crop_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cropFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            FileProvider.getUriForFile(context, "dev.shovo.circlesearch.fileprovider", cropFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun launchGoogleLensSearch(context: Context, imageUri: Uri): Boolean {
        // Method 1: Google Lens direct intent
        val lensIntent = Intent("android.intent.action.VIEW").apply {
            setPackage("com.google.android.googlequicksearchbox")
            setDataAndType(imageUri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (lensIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(lensIntent)
            return true
        }

        // Method 2: Google App Lens action
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            setPackage("com.google.android.googlequicksearchbox")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(shareIntent)
            true
        } catch (e: Exception) {
            // Method 3: Generic visual search chooser
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Search image with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }
    }
}
