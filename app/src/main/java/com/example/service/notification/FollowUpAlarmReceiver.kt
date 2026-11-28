package com.example.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.ClinicDataPersistence
import com.example.model.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FollowUpAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    // 1. Reschedule for next day 8:30 AM
    ClinicNotificationHelper.scheduleDailyFollowUpAlarm(context)

    // 2. Check for patients due today who haven't been reminded yet
    try {
      val patients = ClinicDataPersistence.loadPatients(context) ?: emptyList()
      val vaccines = ClinicDataPersistence.loadVaccines(context) ?: emptyList()
      val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

      val duePatients = patients.filter {
        it.followUpDueDate == todayStr && !it.isReminderSent
      }
      val dueVaccines = vaccines.filter { v ->
        v.nextDueDate == todayStr && duePatients.none { p -> p.id == v.patientId }
      }

      val totalDue = duePatients.size + dueVaccines.size
      if (totalDue > 0) {
        val patLines = duePatients.map { "• ${it.name} (${it.hn}) - ${it.phone}" }
        val vacLines = dueVaccines.map { "• [ကာကွယ်ဆေး] ${it.patientName} (${it.vaccineName} ${it.doseNumber})" }
        val combined = patLines + vacLines
        val preview = combined.take(4).joinToString("\n")
        val more = if (combined.size > 4) "\n... နှင့် အခြား ${combined.size - 4} ဦး" else ""
        ClinicNotificationHelper.showFollowUpNotification(context, totalDue, "$preview$more")
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
