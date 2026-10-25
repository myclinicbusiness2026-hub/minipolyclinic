package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object AttachmentHelper {

  /**
   * Retrieves the original display file name from a content URI.
   */
  fun queryFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
      try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
              result = cursor.getString(nameIndex)
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    if (result == null) {
      result = uri.path
      val cut = result?.lastIndexOf('/') ?: -1
      if (cut != -1) {
        result = result?.substring(cut + 1)
      }
    }
    return result ?: "document_${System.currentTimeMillis()}"
  }

  /**
   * Copies a picked Uri (photo or document) to the clinic's local internal storage
   * to guarantee an absolute, persistent file path.
   * Returns Pair(fileName, absoluteFilePath).
   */
  fun copyUriToLocalStorage(context: Context, uri: Uri, defaultPrefix: String = "doc"): Pair<String, String> {
    try {
      val originalName = queryFileName(context, uri)
      val attachmentsDir = File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
      val cleanName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val destFile = File(attachmentsDir, "${timeStamp}_$cleanName")

      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output ->
          input.copyTo(output)
        }
      }
      return Pair(originalName, destFile.absolutePath)
    } catch (e: Exception) {
      e.printStackTrace()
      // Fallback to the URI string itself as path
      return Pair(queryFileName(context, uri), uri.toString())
    }
  }

  /**
   * Creates a temporary file in the attachments directory and returns its File and FileProvider Uri.
   * Ensures high-resolution photo captures via TakePicture() without FileUriExposedException.
   */
  fun createCameraTempUri(context: Context): Pair<File, Uri> {
    val attachmentsDir = File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val file = File(attachmentsDir, "camera_${timeStamp}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Pair(file, uri)
  }

  /**
   * Saves a camera-captured Bitmap into internal storage and returns Pair(fileName, absoluteFilePath).
   */
  fun saveCameraBitmap(context: Context, bitmap: Bitmap): Pair<String, String> {
    try {
      val attachmentsDir = File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val fileName = "camera_$timeStamp.jpg"
      val file = File(attachmentsDir, fileName)

      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
      }
      return Pair(fileName, file.absolutePath)
    } catch (e: Exception) {
      e.printStackTrace()
      val fallbackName = "camera_${System.currentTimeMillis()}.jpg"
      return Pair(fallbackName, "/data/user/0/${context.packageName}/files/attachments/$fallbackName")
    }
  }
}
