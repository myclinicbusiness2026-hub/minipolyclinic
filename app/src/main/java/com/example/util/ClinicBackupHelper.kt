package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicBackupHelper {

  fun createBackupFile(context: Context, jsonPayload: String): File? {
    return try {
      val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
      val fileName = "ShweKhit_Clinic_Backup_$timestamp.json"
      val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
      if (!backupDir.exists()) {
        backupDir.mkdirs()
      }
      val file = File(backupDir, fileName)
      FileOutputStream(file).use { out ->
        out.write(jsonPayload.toByteArray(Charsets.UTF_8))
        out.flush()
      }
      file
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun shareBackupFile(context: Context, file: File) {
    try {
      val authority = "${context.packageName}.fileprovider"
      val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "Shwe Khit Clinic Backup - ${file.name}")
        val dateDisplay = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
        putExtra(
          Intent.EXTRA_TEXT,
          "ရွှေခေတ်ဆေးခန်း ဒေတာဘေ့စ် အရန်ဖိုင် (${file.name})\nအချိန်: $dateDisplay\nအရွယ်အစား: ${file.length() / 1024} KB"
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, "အရန်ဖိုင်အား ပေးပို့သိမ်းဆည်းမည့်နေရာ ရွေးချယ်ပါ (Telegram / Viber / Drive)")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "ဖိုင်ပေးပို့ရာတွင် ချို့ယွင်းချက်ဖြစ်ပေါ်ပါသည်: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  fun shareBackupPayload(context: Context, jsonPayload: String): Boolean {
    val file = createBackupFile(context, jsonPayload) ?: return false
    shareBackupFile(context, file)
    return true
  }

  fun readJsonFromUri(context: Context, uri: Uri): String? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.bufferedReader(Charsets.UTF_8).use { reader ->
          reader.readText()
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun readBackupFileFromUri(context: Context, uri: Uri): String? = readJsonFromUri(context, uri)
}
