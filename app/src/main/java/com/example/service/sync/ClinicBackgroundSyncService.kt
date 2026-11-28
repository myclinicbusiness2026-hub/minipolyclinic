package com.example.service.sync

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.service.notification.ClinicNotificationHelper
import com.example.sync.LocalWifiSyncManager
import kotlinx.coroutines.*

/**
 * Persistent ForegroundService that keeps Local Wi-Fi Sync alive
 * even when the app is minimized or swiped away on the same Wi-Fi router.
 */
class ClinicBackgroundSyncService : Service() {

  private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var bgSyncManager: LocalWifiSyncManager? = null
  private var periodicJob: Job? = null

  companion object {
    @Volatile
    var isServiceRunning = false
      private set

    fun start(context: Context) {
      val intent = Intent(context, ClinicBackgroundSyncService::class.java)
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (_: Exception) {
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    isServiceRunning = true
    ClinicNotificationHelper.initChannels(this)
    startAsForeground()
    ensureSyncEngineRunning()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ensureSyncEngineRunning()
    return START_STICKY
  }

  private fun startAsForeground() {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pi = PendingIntent.getActivity(
      this,
      9001,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification: Notification = NotificationCompat.Builder(this, ClinicNotificationHelper.CHANNEL_SYNC)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("ရွှေခေတ်ဆေးခန်း Wi-Fi Sync")
      .setContentText("Same Wi-Fi ပေါ်ရှိ စက်များနှင့် အလိုအလျောက် ချိတ်ဆက်နေပါသည်")
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setContentIntent(pi)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        ClinicNotificationHelper.NOTIF_ID_BG_SYNC,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else {
      startForeground(ClinicNotificationHelper.NOTIF_ID_BG_SYNC, notification)
    }
  }

  private fun ensureSyncEngineRunning() {
    if (bgSyncManager == null) {
      val manager = LocalWifiSyncManager(applicationContext) {
        // Data saved to ClinicDataPersistence; active UI reloads via its own manager/resume
      }
      bgSyncManager = manager
      if (LocalWifiSyncService.currentDataSupplier == null) {
        LocalWifiSyncService.currentDataSupplier = { manager.getLocalDataPayloadJson() }
      }
      if (LocalWifiSyncService.onDataReceivedToMerge == null) {
        LocalWifiSyncService.onDataReceivedToMerge = { json -> manager.processIncomingSyncPayload(json) }
      }
      manager.start()
      if (!LocalWifiSyncService.isHubRunning()) {
        LocalWifiSyncService.startHub(8080)
      }
    }

    if (periodicJob?.isActive != true) {
      periodicJob = serviceScope.launch {
        while (isActive) {
          delay(25_000L)
          try {
            bgSyncManager?.syncWithAllPeers()
          } catch (_: Exception) {
          }
        }
      }
    }
  }

  override fun onDestroy() {
    isServiceRunning = false
    periodicJob?.cancel()
    bgSyncManager?.stop()
    bgSyncManager = null
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
