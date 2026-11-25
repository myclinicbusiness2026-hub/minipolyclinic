package com.example.service.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object ClinicNotificationHelper {
  const val CHANNEL_FOLLOWUP = "clinic_followup_channel"
  const val CHANNEL_QUEUE = "clinic_queue_channel"

  const val NOTIF_ID_FOLLOWUP = 1001
  const val NOTIF_ID_QUEUE_BASE = 2000

  fun initChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val followUpChannel = NotificationChannel(
        CHANNEL_FOLLOWUP,
        "ရက်ချိန်း သတိပေးချက်များ (Follow-Up Reminders)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "နံနက် ၈:၃၀ တွင် ယနေ့ ပြန်ပြရန် ရက်ချိန်းရှိသော လူနာများအား အသိပေးခြင်း"
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
      }

      val queueChannel = NotificationChannel(
        CHANNEL_QUEUE,
        "လူနာအသစ် ရောက်ရှိမှု (Patient Queue Alerts)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "လူနာအသစ် စာရင်းသွင်းပြီး စောင့်ဆိုင်းခန်းသို့ ရောက်ရှိလာပါက ချက်ချင်း အသိပေးခြင်း"
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
      }

      notificationManager.createNotificationChannel(followUpChannel)
      notificationManager.createNotificationChannel(queueChannel)
    }
  }

  fun scheduleDailyFollowUpAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, FollowUpAlarmReceiver::class.java)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getBroadcast(context, 7701, intent, flags)

    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 8)
      set(Calendar.MINUTE, 30)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (before(Calendar.getInstance())) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      } else {
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
      }
    } catch (e: Exception) {
      alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
  }

  fun showFollowUpNotification(context: Context, dueCount: Int, previewText: String) {
    if (dueCount <= 0) return

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("open_tab", 1) // Open Patient Queue
      putExtra("open_followup", true)
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 8801, intent, flags)

    val builder = NotificationCompat.Builder(context, CHANNEL_FOLLOWUP)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("ရွှေခေတ်ဆေးခန်း - ယနေ့ ရက်ချိန်းပြန်ပြမည့် လူနာများ")
      .setContentText("ယနေ့ ရက်ချိန်းပြန်ပြရန် လူနာ (${dueCount}) ဦး ရှိပါသည် (စစ်ဆေးရန် နှိပ်ပါ)")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("ယနေ့ ရက်ချိန်းပြန်ပြရန် လူနာ (${dueCount}) ဦး ရှိပါသည်:\n$previewText\nဖုန်းဆက်သတိပေးရန် သို့မဟုတ် Queue သို့ သွင်းရန် နှိပ်ပါ။")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setNumber(dueCount)

    try {
      NotificationManagerCompat.from(context).notify(NOTIF_ID_FOLLOWUP, builder.build())
    } catch (e: SecurityException) {
      // Permission not granted
    }
  }

  fun showNewPatientNotification(context: Context, patientName: String, tokenNumber: Int, queueCount: Int) {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("open_tab", 1) // Open Patient Queue
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 8802, intent, flags)

    val notifId = NOTIF_ID_QUEUE_BASE + (tokenNumber % 100)
    val builder = NotificationCompat.Builder(context, CHANNEL_QUEUE)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("လူနာအသစ် ရောက်ရှိပါသည် (Token #$tokenNumber)")
      .setContentText("$patientName စောင့်ဆိုင်းခန်းသို့ ရောက်ရှိလာပါပြီ")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setNumber(queueCount.coerceAtLeast(1))

    try {
      NotificationManagerCompat.from(context).notify(notifId, builder.build())
    } catch (e: SecurityException) {
      // Permission not granted
    }
  }
}
