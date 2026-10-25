package com.example.util

import java.text.SimpleDateFormat
import java.util.*

data class FollowUpSchedule(
  val days: Int,
  val dateFormatted: String, // DD/MM/YYYY
  val dayOfWeekBurmese: String, // တနင်္လာ၊ အင်္ဂါ...
  val reasonBurmese: String,
  val fullDisplayBurmese: String
)

object BurmeseDateUtils {
  private val burmeseDays = mapOf(
    Calendar.SUNDAY to "တနင်္ဂနွေနေ့",
    Calendar.MONDAY to "တနင်္လာနေ့",
    Calendar.TUESDAY to "အင်္ဂါနေ့",
    Calendar.WEDNESDAY to "ဗုဒ္ဓဟူးနေ့",
    Calendar.THURSDAY to "ကြာသပတေးနေ့",
    Calendar.FRIDAY to "သောကြာနေ့",
    Calendar.SATURDAY to "စနေနေ့"
  )

  private val burmeseMonths = listOf(
    "ဇန်နဝါရီ", "ဖေဖော်ဝါရီ", "မတ်", "ဧပြီ", "မေ", "ဇွန်",
    "ဇူလိုင်", "သြဂုတ်", "စက်တင်ဘာ", "အောက်တိုဘာ", "နိုဝင်ဘာ", "ဒီဇင်ဘာ"
  )

  fun calculateFollowUp(days: Int, reason: String = "ပြန်လည်စစ်ဆေးရန်"): FollowUpSchedule {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, days)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    val dayOfWeek = burmeseDays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    val dateStr = String.format("%02d/%02d/%04d", day, month, year)

    val periodLabel = when (days) {
      3 -> "၃ ရက်အကြာ"
      5 -> "၅ ရက်အကြာ"
      7 -> "၁ ပတ်အကြာ"
      14 -> "၂ ပတ်အကြာ"
      21 -> "၃ ပတ်အကြာ"
      30 -> "၁ လအကြာ"
      60 -> "၂ လအကြာ"
      else -> "$days ရက်အကြာ"
    }

    val fullDisplay = "$periodLabel • $dateStr ($dayOfWeek) - $reason"
    return FollowUpSchedule(days, dateStr, dayOfWeek, reason, fullDisplay)
  }

  fun formatNextVaccineSchedule(vaccineName: String, dose: Any, dueDate: String? = null): String {
    val d = dueDate ?: ""
    return "နောက်တစ်ကြိမ် $vaccineName (Dose $dose) ထိုးရန်ရက်: $d"
  }

  fun calculateDateFromBase(baseDateStr: String, daysToAdd: Int, reason: String = "ကာကွယ်ဆေးထိုးရန်"): FollowUpSchedule {
    val cal = Calendar.getInstance()
    try {
      val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss")
      for (fmt in formats) {
        try {
          val date = SimpleDateFormat(fmt, Locale.getDefault()).parse(baseDateStr.trim())
          if (date != null) {
            cal.time = date
            break
          }
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
    cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    val dayOfWeek = burmeseDays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    val dateStr = String.format("%02d/%02d/%04d", day, month, year)
    val fullDisplay = "$dateStr ($dayOfWeek)"
    return FollowUpSchedule(daysToAdd, dateStr, dayOfWeek, reason, fullDisplay)
  }

  fun getTodayFormatted(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
  }

  fun getNowFormatted(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    return sdf.format(Date())
  }

  fun formatToBurmeseDateAndDay(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    try {
      val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd hh:mm a", "yyyy-MM-dd", "dd/MM/yyyy")
      var date: Date? = null
      for (fmt in formats) {
        try {
          val sdf = SimpleDateFormat(fmt, Locale.getDefault())
          date = sdf.parse(dateStr.trim())
          if (date != null) break
        } catch (_: Exception) {}
      }
      if (date != null) {
        val cal = Calendar.getInstance()
        cal.time = date
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        val dayOfWeek = burmeseDays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        return String.format("%02d/%02d/%04d (%s)", day, month, year, dayOfWeek)
      }
    } catch (_: Exception) {}
    return dateStr
  }

  fun formatFollowUpForPrint(appointment: String, dueDate: String? = null): String {
    val dateToUse = dueDate?.ifBlank { null } ?: appointment
    if (dateToUse.isBlank()) return "ရက်ချိန်းမရှိပါ"
    val formatted = formatToBurmeseDateAndDay(dateToUse)
    return if (appointment.isNotBlank() && appointment != dateToUse) "$appointment ($formatted)" else formatted
  }
}

