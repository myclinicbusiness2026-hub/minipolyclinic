package com.example.service.notification

import android.annotation.SuppressLint
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
  const val CHANNEL_INVOICE = "clinic_invoice_channel"
  const val CHANNEL_PHARMACY = "clinic_pharmacy_channel"
  const val CHANNEL_TREATMENT = "clinic_treatment_channel"
  const val CHANNEL_LAB = "clinic_lab_channel"
  const val CHANNEL_SYNC = "clinic_bg_sync_channel"

  const val NOTIF_ID_FOLLOWUP = 1001
  const val NOTIF_ID_QUEUE_BASE = 2000
  const val NOTIF_ID_INVOICE_BASE = 3000
  const val NOTIF_ID_PHARMACY_BASE = 4000
  const val NOTIF_ID_TREATMENT_BASE = 5000
  const val NOTIF_ID_LAB_BASE = 6000
  const val NOTIF_ID_BG_SYNC = 9001

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

      val invoiceChannel = NotificationChannel(
        CHANNEL_INVOICE,
        "ငွေရှင်းပြေစာ အသိပေးချက်များ (Cashier & Invoices)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "ပြေစာအသစ် ထွက်ရှိချိန်နှင့် ငွေရှင်းပြီးချိန် အသိပေးခြင်း"
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
      }

      val pharmacyChannel = NotificationChannel(
        CHANNEL_PHARMACY,
        "ဆေးဝါးထုတ်ပေးခြင်း (Pharmacy Dispense Alerts)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "ဆေးညွှန်းအသစ် ရောက်ရှိလာချိန်တွင် အသိပေးခြင်း"
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
      }

      val treatmentChannel = NotificationChannel(
        CHANNEL_TREATMENT,
        "ဆေးထိုး/ကုသမှု (Treatment & Injection Alerts)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "ဆေးထိုးနှင့် ကုသမှု Order အသစ်များ ရောက်ရှိချိန်တွင် အသိပေးခြင်း"
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
      }

      val labChannel = NotificationChannel(
        CHANNEL_LAB,
        "ဓာတ်ခွဲခန်း (Lab Investigation Alerts)",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "ဓာတ်ခွဲအော်ဒါအသစ်နှင့် အဖြေထွက်ချိန်တွင် အသိပေးခြင်း"
        enableLights(true)
        enableVibration(true)
        setShowBadge(true)
      }

      val syncChannel = NotificationChannel(
        CHANNEL_SYNC,
        "Local Wi-Fi Sync Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Same Wi-Fi ပေါ်ရှိ စက်များနှင့် Background Sync ချိတ်ဆက်ပေးခြင်း"
        setShowBadge(false)
      }

      notificationManager.createNotificationChannel(followUpChannel)
      notificationManager.createNotificationChannel(queueChannel)
      notificationManager.createNotificationChannel(invoiceChannel)
      notificationManager.createNotificationChannel(pharmacyChannel)
      notificationManager.createNotificationChannel(treatmentChannel)
      notificationManager.createNotificationChannel(labChannel)
      notificationManager.createNotificationChannel(syncChannel)
    }
  }

  fun scheduleDailyFollowUpAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, FollowUpAlarmReceiver::class.java)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

  @SuppressLint("MissingPermission")
  fun showFollowUpNotification(context: Context, dueCount: Int, previewText: String) {
    if (dueCount <= 0) return

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("open_tab", 1) // Open Patient Queue
      putExtra("open_followup", true)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

  @SuppressLint("MissingPermission")
  fun showNewPatientNotification(context: Context, patientName: String, tokenNumber: Int, queueCount: Int) {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("open_tab", 1) // Open Patient Queue
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

  @SuppressLint("MissingPermission")
  fun showGenericClinicNotification(
    context: Context,
    channelId: String,
    notifId: Int,
    title: String,
    message: String,
    openTab: Int
  ) {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("open_tab", openTab)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val pendingIntent = PendingIntent.getActivity(context, 8810 + openTab, intent, flags)

    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    try {
      NotificationManagerCompat.from(context).notify(notifId, builder.build())
    } catch (_: SecurityException) {
      // Permission not granted
    }
  }

  fun showInvoiceNotification(
    context: Context,
    title: String,
    message: String,
    notifId: Int = NOTIF_ID_INVOICE_BASE + ((System.currentTimeMillis() % 900).toInt())
  ) = showGenericClinicNotification(context, CHANNEL_INVOICE, notifId, title, message, openTab = 4)

  fun showPharmacyNotification(
    context: Context,
    title: String,
    message: String,
    notifId: Int = NOTIF_ID_PHARMACY_BASE + ((System.currentTimeMillis() % 900).toInt())
  ) = showGenericClinicNotification(context, CHANNEL_PHARMACY, notifId, title, message, openTab = 3)

  fun showTreatmentNotification(
    context: Context,
    title: String,
    message: String,
    notifId: Int = NOTIF_ID_TREATMENT_BASE + ((System.currentTimeMillis() % 900).toInt())
  ) = showGenericClinicNotification(context, CHANNEL_TREATMENT, notifId, title, message, openTab = 9)

  fun showLabNotification(
    context: Context,
    title: String,
    message: String,
    notifId: Int = NOTIF_ID_LAB_BASE + ((System.currentTimeMillis() % 900).toInt())
  ) = showGenericClinicNotification(context, CHANNEL_LAB, notifId, title, message, openTab = 10)
}
