package com.example.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.ClinicDataPersistence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FollowUpAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    // 1. Reschedule for next day 8:30 AM
    ClinicNotificationHelper.scheduleDailyFollowUpAlarm(context)

    // 2. Check for patients due today who haven't been reminded yet
    try {
      val patients = ClinicDataPersistence.loadPatients(context)
      val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

      val duePatients = patients.filter {
        it.followUpDueDate == todayStr && !it.isReminderSent
      }

      if (duePatients.isNotEmpty()) {
        val preview = duePatients.take(4).joinToString("\n") { "• ${it.name} (${it.hn}) - ${it.phone}" }
        val more = if (duePatients.size > 4) "\n... နှင့် အခြား ${duePatients.size - 4} ဦး" else ""
        ClinicNotificationHelper.showFollowUpNotification(context, duePatients.size, "$preview$more")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
