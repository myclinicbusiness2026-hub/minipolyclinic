package com.example.service.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val releaseNotes: String,
  val releaseNotesMm: String,
  val downloadUrl: String,
  val apkSizeBytes: Long = 0L,
  val publishedAt: String = ""
)

sealed class UpdateState {
  object Idle : UpdateState()
  object Checking : UpdateState()
  data class UpdateAvailable(val info: AppUpdateInfo) : UpdateState()
  object UpToDate : UpdateState()
  data class Downloading(
    val percent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedKbPerSec: Double
  ) : UpdateState() {
    val downloadedMb: Double get() = downloadedBytes / (1024.0 * 1024.0)
    val totalMb: Double get() = if (totalBytes > 0) totalBytes / (1024.0 * 1024.0) else 0.0
    val speedMbPerSec: Double get() = speedKbPerSec / 1024.0
  }
  data class ReadyToInstall(val apkFile: File, val info: AppUpdateInfo) : UpdateState()
  data class Error(val message: String) : UpdateState()
}

object AppUpdateManager {
  private const val TAG = "AppUpdateManager"
  private const val VERSION_JSON_PRIMARY = "https://raw.githubusercontent.com/myclinicbusiness2026-hub/minipolyclinic/main/version.json"
  private const val GITHUB_RELEASE_API = "https://api.github.com/repos/myclinicbusiness2026-hub/minipolyclinic/releases/latest"

  /**
   * Check whether a new version of the app is available on GitHub.
   */
  suspend fun checkForUpdates(
    currentVersionCode: Int,
    currentVersionName: String
  ): UpdateState = withContext(Dispatchers.IO) {
    try {
      // 1. Try raw version.json endpoint
      var jsonStr = fetchString(VERSION_JSON_PRIMARY)
      
      // 2. Fallback to GitHub releases API if raw json is empty
      if (jsonStr.isNullOrBlank()) {
        val ghJson = fetchString(GITHUB_RELEASE_API)
        if (!ghJson.isNullOrBlank()) {
          val ghObj = JSONObject(ghJson)
          val tagName = ghObj.optString("tag_name", "").removePrefix("v")
          val body = ghObj.optString("body", "Bug fixes and improvements")
          val assets = ghObj.optJSONArray("assets")
          var dlUrl = ""
          var size = 0L
          if (assets != null && assets.length() > 0) {
            val a = assets.getJSONObject(0)
            dlUrl = a.optString("browser_download_url", "")
            size = a.optLong("size", 0L)
          }
          if (dlUrl.isNotBlank()) {
            val info = AppUpdateInfo(
              versionCode = currentVersionCode + 1,
              versionName = tagName.ifBlank { "Latest" },
              releaseNotes = body,
              releaseNotesMm = body,
              downloadUrl = dlUrl,
              apkSizeBytes = size,
              publishedAt = ghObj.optString("published_at", "")
            )
            return@withContext UpdateState.UpdateAvailable(info)
          }
        }
        return@withContext UpdateState.Error("ဗားရှင်းအသစ် စစ်ဆေး၍မရပါ (Network unavailable)")
      }

      val obj = JSONObject(jsonStr)
      val remoteCode = obj.optInt("versionCode", 1)
      val remoteName = obj.optString("versionName", "1.0.0")
      val notes = obj.optString("releaseNotes", "")
      val notesMm = obj.optString("releaseNotesMm", notes)
      val dlUrl = obj.optString("downloadUrl", "")
      val apkSize = obj.optLong("apkSizeBytes", 0L)
      val publishedAt = obj.optString("publishedAt", "")

      if (remoteCode > currentVersionCode || isSemanticNewer(remoteName, currentVersionName)) {
        val info = AppUpdateInfo(
          versionCode = remoteCode,
          versionName = remoteName,
          releaseNotes = notes,
          releaseNotesMm = notesMm,
          downloadUrl = dlUrl,
          apkSizeBytes = apkSize,
          publishedAt = publishedAt
        )
        UpdateState.UpdateAvailable(info)
      } else {
        UpdateState.UpToDate
      }
    } catch (e: Exception) {
      Log.e(TAG, "Update check failed: ${e.message}")
      UpdateState.Error("စစ်ဆေးမှု မအောင်မြင်ပါ: ${e.localizedMessage ?: "Network error"}")
    }
  }

  /**
   * Downloads the APK streamed with real-time byte count and download speed callbacks.
   */
  suspend fun downloadApk(
    context: Context,
    info: AppUpdateInfo,
    onProgress: (UpdateState.Downloading) -> Unit
  ): Pair<Boolean, File?> = withContext(Dispatchers.IO) {
    try {
      val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
      val targetFile = File(updateDir, "ShweKhitClinic-v${info.versionName}.apk")
      if (targetFile.exists()) {
        targetFile.delete()
      }

      var currentUrl = info.downloadUrl
      var redirectCount = 0
      var conn: HttpURLConnection? = null

      // Follow redirects (e.g. GitHub raw -> cdn / releases -> S3)
      while (redirectCount < 5) {
        val url = URL(currentUrl)
        conn = (url.openConnection() as HttpURLConnection).apply {
          connectTimeout = 15000
          readTimeout = 30000
          requestMethod = "GET"
          instanceFollowRedirects = false
          setRequestProperty("User-Agent", "ShweKhitClinic-AppUpdateManager")
        }

        val code = conn.responseCode
        if (code in 301..308) {
          val newLoc = conn.getHeaderField("Location")
          if (!newLoc.isNullOrBlank()) {
            currentUrl = newLoc
            redirectCount++
            conn.disconnect()
            continue
          }
        }
        break
      }

      val activeConn = conn ?: return@withContext Pair(false, null)
      val responseCode = activeConn.responseCode
      if (responseCode !in 200..299) {
        return@withContext Pair(false, null)
      }

      val totalBytes = activeConn.contentLengthLong.let { if (it > 0) it else info.apkSizeBytes }
      val inputStream = activeConn.inputStream
      val outputStream = FileOutputStream(targetFile)

      val buffer = ByteArray(8192)
      var bytesRead: Int
      var totalRead = 0L
      var startTime = System.currentTimeMillis()
      var lastReportTime = startTime
      var bytesSinceLastReport = 0L

      while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
        totalRead += bytesRead
        bytesSinceLastReport += bytesRead

        val now = System.currentTimeMillis()
        if (now - lastReportTime >= 250) {
          val timeDiffSec = (now - lastReportTime) / 1000.0
          val speedKbPerSec = if (timeDiffSec > 0) (bytesSinceLastReport / 1024.0) / timeDiffSec else 0.0
          val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

          onProgress(
            UpdateState.Downloading(
              percent = percent,
              downloadedBytes = totalRead,
              totalBytes = totalBytes,
              speedKbPerSec = speedKbPerSec
            )
          )

          lastReportTime = now
          bytesSinceLastReport = 0L
        }
      }

      outputStream.flush()
      outputStream.close()
      inputStream.close()
      activeConn.disconnect()

      // Final 100% callback
      onProgress(
        UpdateState.Downloading(
          percent = 100,
          downloadedBytes = totalRead,
          totalBytes = totalRead,
          speedKbPerSec = 0.0
        )
      )

      Pair(true, targetFile)
    } catch (e: Exception) {
      Log.e(TAG, "Download APK failed: ${e.message}")
      Pair(false, null)
    }
  }

  /**
   * Launches Android Package Installer via FileProvider.
   * Performs in-place app update without deleting user data.
   */
  fun launchInstaller(context: Context, apkFile: File): Pair<Boolean, String> {
    if (!apkFile.exists() || apkFile.length() == 0L) {
      return Pair(false, "APK ဖိုင် ရှာမတွေ့ပါ သို့မဟုတ် ဒေါင်းလုဒ်မပြီးပြတ်သေးပါ")
    }

    try {
      val authority = "${context.packageName}.fileprovider"
      val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      context.startActivity(installIntent)
      return Pair(true, "Package Installer ဖွင့်လှစ်ပြီးပါပြီ")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch installer: ${e.message}")
      return Pair(false, "Install လုပ်၍မရပါ: ${e.localizedMessage}")
    }
  }

  /**
   * Check if app has permission to install unknown apps (Android 8.0+).
   */
  fun canRequestPackageInstalls(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  /**
   * Open system settings for allowing this app to install packages.
   */
  fun openInstallPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  private fun fetchString(targetUrl: String): String? {
    return try {
      val url = URL(targetUrl)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 10000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "ShweKhitClinic-AppUpdate")
      }
      if (conn.responseCode in 200..299) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun isSemanticNewer(remoteVer: String, localVer: String): Boolean {
    try {
      val rParts = remoteVer.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
      val lParts = localVer.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
      val maxLen = maxOf(rParts.size, lParts.size)
      for (i in 0 until maxLen) {
        val r = rParts.getOrElse(i) { 0 }
        val l = lParts.getOrElse(i) { 0 }
        if (r > l) return true
        if (r < l) return false
      }
      return false
    } catch (_: Exception) {
      return false
    }
  }
}
