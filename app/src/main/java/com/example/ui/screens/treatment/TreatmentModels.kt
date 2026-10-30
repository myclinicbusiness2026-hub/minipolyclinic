package com.example.ui.screens.treatment

import com.example.model.ClinicSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Route of administration for routine clinic Day-Care & Treatment Card procedures
 */
enum class TreatmentRoute(val label: String, val burmeseLabel: String) {
  IM("IM (Intramuscular)", "အသားဆေး"),
  IV_PUSH("IV Push (Intravenous)", "အကြောဆေး အမြန်ထိုး"),
  IV_INFUSION("IV Infusion / Drip", "အကြောပုလင်း သွင်းဆေး"),
  SC("SC (Subcutaneous)", "အရေပြားအောက်ထိုးဆေး"),
  NEBULIZATION("Nebulization", "အငွေ့ရှူကုသခြင်း"),
  DRESSING("Wound Dressing", "ဒဏ်ရာဆေးထည့်ခြင်း");

  fun getDefaultFee(settings: ClinicSettings): Double = when (this) {
    IM -> settings.defaultImInjectionFee
    IV_PUSH -> settings.defaultIvPushFee
    IV_INFUSION -> settings.defaultIvInfusionFee
    NEBULIZATION -> settings.defaultNebulizerFee
    SC -> settings.defaultDepoFee
    DRESSING -> 5000.0
  }
}

enum class TreatmentStatus(val label: String, val burmeseLabel: String) {
  PENDING("Pending", "စောင့်ဆိုင်းဆဲ"),
  IN_PROGRESS("In Progress", "ဆေးသွင်း/ထိုးနေဆဲ"),
  COMPLETED("Completed", "ပြီးစီး"),
  DISCONTINUED("Discontinued", "ရပ်ဆိုင်း"),
  CANCELLED("Cancelled", "ပယ်ဖျက်")
}

/**
 * Consumable or disposable material used during injection / infusion
 * (e.g. IV Cannula, Scalp Vein needle, Syringe, WFI, Infusion Set)
 * Directly linked with clinic pharmacy inventory.
 */
data class TreatmentConsumable(
  val id: String = UUID.randomUUID().toString(),
  val medicineId: String = "",
  val name: String,
  val quantity: Int = 1,
  val unitPrice: Double = 0.0
) {
  val totalPrice: Double get() = quantity * unitPrice
}

/**
 * An individual administration dose/session within a Treatment Card course
 * (e.g., Dose 2 of 6 for a 12-hourly 3-day course)
 */
data class TreatmentSession(
  val id: String = UUID.randomUUID().toString(),
  val sessionNumber: Int,
  val scheduledDate: String,
  val scheduledTime: String = "08:00 AM",
  val administeredAt: String? = null,
  val administeredBy: String = "",
  val status: TreatmentStatus = TreatmentStatus.PENDING,
  val isClinicMed: Boolean = true,
  val isPrepaid: Boolean = false,
  val medicationFee: Double = 0.0,
  val sessionFee: Double = 3000.0,
  val consumables: List<TreatmentConsumable> = emptyList(),
  val invoiceId: String? = null,
  val notes: String = ""
) {
  val totalSessionCost: Double get() = (if (isClinicMed && !isPrepaid) medicationFee else 0.0) + (if (isPrepaid) 0.0 else sessionFee) + consumables.sumOf { it.totalPrice }
}

/**
 * Modular Day-Care Treatment Card Model
 * Handles routine injections, IV drip courses, and standalone nursing visits without requiring
 * repeated doctor consultation records or clumping under surgical minor procedures.
 */
data class TreatmentCard(
  val id: String = "TC-${UUID.randomUUID().toString().take(8)}",
  val patientId: String,
  val patientName: String,
  val patientHn: String = "",
  val doctorName: String = "",
  val prescribedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
  val medicationId: String = "",
  val medicationName: String,
  val dosage: String = "",
  val route: TreatmentRoute = TreatmentRoute.IM,
  val frequency: String = "12 hrly", // e.g. "12 hrly", "Daily", "Stat"
  val totalDays: Int = 3,
  val totalSessions: Int = 6,
  val sessions: List<TreatmentSession> = emptyList(),
  val instructions: String = "",
  val sessionFee: Double = 3000.0,
  val medicationFee: Double = 0.0,
  val isBilledPerSession: Boolean = true,
  val isPayPerVisit: Boolean = true,
  val isPrepaidFullCourse: Boolean = false,
  val consultationInvoiceId: String? = null,
  val cardStatus: TreatmentStatus = TreatmentStatus.IN_PROGRESS,
  val discontinueReason: String = "",
  val discontinuedBy: String = "",
  val discontinuedAt: String = "",
  val notes: String = ""
) {
  val completedSessionsCount: Int get() = sessions.count { it.status == TreatmentStatus.COMPLETED }
  val isFullyCompleted: Boolean get() = cardStatus == TreatmentStatus.COMPLETED || (sessions.isNotEmpty() && sessions.all { it.status == TreatmentStatus.COMPLETED })
  val isDiscontinued: Boolean get() = cardStatus == TreatmentStatus.DISCONTINUED || cardStatus == TreatmentStatus.CANCELLED
  val nextDueSession: TreatmentSession? get() = if (isDiscontinued) null else sessions.firstOrNull { it.status == TreatmentStatus.PENDING }
}

/**
 * Factory utilities to generate treatment schedules and routine day-care templates
 */
object TreatmentCardFactory {

  fun generateSessions(
    startDateStr: String,
    frequency: String,
    days: Int,
    medicationFee: Double = 0.0,
    sessionFee: Double = 3000.0,
    splitVialMode: Boolean = false
  ): List<TreatmentSession> {
    val list = mutableListOf<TreatmentSession>()
    val cal = Calendar.getInstance()
    try {
      val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDateStr)
      if (parsed != null) cal.time = parsed
    } catch (_: Exception) {}

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    when {
      frequency.contains("12", ignoreCase = true) -> {
        var count = 1
        for (d in 0 until days) {
          val dateStr = dateFormat.format(cal.time)
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "08:00 AM", medicationFee = medicationFee, sessionFee = sessionFee))
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "08:00 PM", medicationFee = if (splitVialMode) 0.0 else medicationFee, isClinicMed = !splitVialMode, sessionFee = sessionFee))
          cal.add(Calendar.DAY_OF_YEAR, 1)
        }
      }
      frequency.contains("8", ignoreCase = true) -> {
        var count = 1
        for (d in 0 until days) {
          val dateStr = dateFormat.format(cal.time)
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "06:00 AM", medicationFee = medicationFee, sessionFee = sessionFee))
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "02:00 PM", medicationFee = if (splitVialMode) 0.0 else medicationFee, isClinicMed = !splitVialMode, sessionFee = sessionFee))
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "10:00 PM", medicationFee = if (splitVialMode) 0.0 else medicationFee, isClinicMed = !splitVialMode, sessionFee = sessionFee))
          cal.add(Calendar.DAY_OF_YEAR, 1)
        }
      }
      frequency.contains("Stat", ignoreCase = true) -> {
        val dateStr = dateFormat.format(cal.time)
        list.add(TreatmentSession(sessionNumber = 1, scheduledDate = dateStr, scheduledTime = "Immediate", medicationFee = medicationFee, sessionFee = sessionFee))
      }
      else -> {
        // Daily
        for (d in 1..days) {
          val dateStr = dateFormat.format(cal.time)
          list.add(TreatmentSession(sessionNumber = d, scheduledDate = dateStr, scheduledTime = "09:00 AM", medicationFee = medicationFee, sessionFee = sessionFee))
          cal.add(Calendar.DAY_OF_YEAR, 1)
        }
      }
    }
    return list
  }

  /**
   * Quick presets for routine Day-Care treatments
   */
  fun getRoutineDayCarePresets(): List<TreatmentCard> {
    return listOf(
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "5% Dextrose Saline 500ml + Neurobion & Vit C",
        dosage = "1 Bottle (500ml)",
        route = TreatmentRoute.IV_INFUSION,
        frequency = "Daily",
        totalDays = 1,
        totalSessions = 1,
        sessionFee = 8000.0,
        medicationFee = 12000.0,
        instructions = "Drip at 30 drops/min with micro-filter set. Monitor vitals."
      ),
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "Ceftriaxone 1g IV + Water for Inj",
        dosage = "1g IV",
        route = TreatmentRoute.IV_PUSH,
        frequency = "12 hrly",
        totalDays = 3,
        totalSessions = 6,
        sessionFee = 3000.0,
        medicationFee = 4500.0,
        instructions = "Skin test negative before 1st dose. Slow IV push over 3-5 mins."
      ),
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "Depo-Provera (Medroxyprogesterone)",
        dosage = "150mg / 1ml",
        route = TreatmentRoute.IM,
        frequency = "Stat (Every 3 months)",
        totalDays = 1,
        totalSessions = 1,
        sessionFee = 2500.0,
        medicationFee = 7000.0,
        instructions = "Deep IM injection in gluteal muscle. Do not massage site."
      ),
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "Salbutamol + Ipratropium Respules",
        dosage = "1 nebule with O2",
        route = TreatmentRoute.NEBULIZATION,
        frequency = "Stat",
        totalDays = 1,
        totalSessions = 1,
        sessionFee = 5000.0,
        medicationFee = 3500.0,
        instructions = "Nebulize with 6L/min O2 for 15 mins. Check SPO2 and pulse."
      ),
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "Wound Dressing (Clean & Aseptic)",
        dosage = "Regular Dressing",
        route = TreatmentRoute.DRESSING,
        frequency = "Daily",
        totalDays = 3,
        totalSessions = 3,
        sessionFee = 5000.0,
        medicationFee = 2000.0,
        instructions = "Clean with Normal Saline & Povidone Iodine. Apply sterile gauze & micropore."
      ),
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "Suture Removal / Stitch Off",
        dosage = "Complete",
        route = TreatmentRoute.DRESSING,
        frequency = "Stat",
        totalDays = 1,
        totalSessions = 1,
        sessionFee = 5000.0,
        medicationFee = 1000.0,
        instructions = "Clean wound edges. Remove alternating or all sutures aseptically."
      )
    )
  }
}
