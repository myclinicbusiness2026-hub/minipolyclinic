package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Utility to play audio feedback for clinic synchronization events:
 * 1. Sync completion chime (Cloud & Local Wi-Fi)
 * 2. Incoming patient alert chime (when another device adds a patient to queue)
 */
object SoundHelper {

  /**
   * Pleasant subtle double-beep tone indicating successful sync
   */
  fun playSyncSuccessSound(context: Context) {
    try {
      val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
      tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
      Handler(Looper.getMainLooper()).postDelayed({
        try {
          tone.release()
        } catch (_: Exception) {}
      }, 250)
    } catch (_: Exception) {
      try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context.applicationContext, uri)?.play()
      } catch (_: Exception) {}
    }
  }

  /**
   * Prominent pleasant alert chime when a new patient is received from another peer device
   */
  fun playNewPatientIncomingSound(context: Context) {
    try {
      val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
      tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 280)
      Handler(Looper.getMainLooper()).postDelayed({
        try {
          tone.release()
        } catch (_: Exception) {}
      }, 350)
    } catch (_: Exception) {
      try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context.applicationContext, uri)?.play()
      } catch (_: Exception) {}
    }
  }
}
