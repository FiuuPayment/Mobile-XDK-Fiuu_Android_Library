package com.fiuu.xdkandroid.platform

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import com.fiuu.xdkandroid.utils.FiuuLogger

// minSdk is 29, so MediaStore's scoped-storage insert (RELATIVE_PATH is API 29) works with no
// runtime storage permission — no legacy WRITE_EXTERNAL_STORAGE handling needed.
class AndroidImageSaver(private val context: Context) : ImageSaver {
    override fun saveImage(bytes: ByteArray, fileName: String, onResult: (Boolean, String?) -> Unit) {
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                fail("MediaStore.insert returned null (gallery provider unavailable)", onResult)
                return
            }
            val stream = resolver.openOutputStream(uri)
            if (stream == null) {
                // Without this check the row stayed in MediaStore as a 0-byte "success" — the
                // exact silent-failure shape reported: no error, no exception, no actual file.
                resolver.delete(uri, null, null)
                fail("openOutputStream returned null for $uri", onResult)
                return
            }
            stream.use { it.write(bytes) }
            openImage(uri)
            onResult(true, null)
        } catch (e: Exception) {
            fail(e.message ?: e.toString(), onResult)
        }
    }

    // Best-effort — a missing/failing viewer app shouldn't turn a successful save into a
    // reported failure, so this is swallowed rather than routed through onResult.
    private fun openImage(uri: android.net.Uri) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/png")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            FiuuLogger.log("AndroidImageSaver: no app available to view $uri: $e")
        }
    }

    private fun fail(message: String, onResult: (Boolean, String?) -> Unit) {
        FiuuLogger.log("AndroidImageSaver: save failed: $message")
        onResult(false, message)
    }
}
