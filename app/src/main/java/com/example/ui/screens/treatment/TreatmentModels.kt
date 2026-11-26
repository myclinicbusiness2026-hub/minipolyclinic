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
  DRESSING("Wound Dressing", "ဒဏ်ရာဆေးထည့်ခြင်း"),
  CUSTOM("Custom Route", "အခြား စိတ်ကြိုက်");

  fun getDefaultFee(settings: ClinicSettings): Double = when (this) {
    IM -> settings.defaultImInjectionFee
    IV_PUSH -> settings.defaultIvPushFee
    IV_INFUSION -> settings.defaultIvInfusionFee
    NEBULIZATION -> settings.defaultNebulizerFee
    SC -> settings.defaultDepoFee
    DRESSING -> 5000.0
    CUSTOM -> settings.defaultImInjectionFee
  }

  fun getEffectiveFee(settings: ClinicSettings, procedures: List<com.example.model.ProcedureItem> = emptyList()): Double {
    val matched = procedures.find { proc ->
      val n = proc.name.lowercase()
      val mn = proc.myanmarName.lowercase()
      when (this) {
        IM -> (n.contains("im") || mn.contains("အသားဆေး")) && !n.contains("infusion")
        IV_PUSH -> (n.contains("iv") || mn.contains("အကြောဆေး")) && !n.contains("infusion") && !n.contains("drip") && !mn.contains("ပုလင်း")
        IV_INFUSION -> n.contains("infusion") || n.contains("drip") || mn.contains("အကြောပုလင်း") || mn.contains("သွင်းဆေး")
        NEBULIZATION -> n.contains("neb") || mn.contains("အငွေ့")
        SC -> n.contains("sc") || n.contains("subcutaneous") || n.contains("depo")
        DRESSING -> n.contains("dressing") || mn.contains("ဆေးထည့်")
        CUSTOM -> false
      }
    }
    if (matched != null && matched.standardFee > 0.0) {
      return matched.standardFee
    }
    return getDefaultFee(settings)
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
 * Constituent medicine item in an IV Cocktail or multi-drug mixed injection
 */
data class CocktailDrugItem(
  val id: String = UUID.randomUUID().toString(),
  val medicineId: String = "",
  val name: String,
  val dosage: String = "",
  val quantity: Int = 1,
  val unitPrice: Double = 0.0
) {
  val totalPrice: Double get() = quantity * unitPrice
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
  val customRoute: String = "",
  val frequency: String = "Stat", // e.g. "Stat", "12 hrly", "Daily"
  val totalDays: Int = 1,
  val totalSessions: Int = 1,
  val sessions: List<TreatmentSession> = emptyList(),
  val instructions: String = "",
  val sessionFee: Double = 3000.0,
  val medicationFee: Double = 0.0,
  val isBilledPerSession: Boolean = true,
  val isPayPerVisit: Boolean = true,
  val isPrepaidFullCourse: Boolean = false,
  val isCocktail: Boolean = false,
  val cocktailItems: List<CocktailDrugItem> = emptyList(),
  val consumables: List<TreatmentConsumable> = emptyList(),
  val isPatientSupplied: Boolean = false,
  val consultationInvoiceId: String? = null,
  val cardStatus: TreatmentStatus = TreatmentStatus.IN_PROGRESS,
  val discontinueReason: String = "",
  val discontinuedBy: String = "",
  val discontinuedAt: String = "",
  val notes: String = ""
) {
  val routeDisplay: String get() = if (route == TreatmentRoute.CUSTOM && customRoute.isNotBlank()) customRoute else route.label

  val displayName: String get() {
    val baseName = if (medicationName.isNotBlank()) {
      medicationName.trim()
    } else if (isCocktail && cocktailItems.isNotEmpty()) {
      cocktailItems.joinToString(" + ") { it.name + if (it.dosage.isNotBlank()) " ${it.dosage}" else "" }
    } else {
      "Injection / Treatment"
    }
    return if (isPatientSupplied) "$baseName (Patient's Own Medicine / လူနာပါဆေး)" else baseName
  }
  val orderSummaryText: String get() {
    val routeShort = when (route) {
      TreatmentRoute.IM -> "IM"
      TreatmentRoute.IV_PUSH -> "IV"
      TreatmentRoute.IV_INFUSION -> "IV Infusion"
      TreatmentRoute.SC -> "SC"
      TreatmentRoute.NEBULIZATION -> "Nebulization"
      TreatmentRoute.DRESSING -> "Wound Dressing"
      TreatmentRoute.CUSTOM -> customRoute.ifBlank { "Custom" }
    }
    val cleanMed = if (isCocktail && cocktailItems.isNotEmpty()) {
      if (medicationName.isNotBlank() && !medicationName.contains("Mix", ignoreCase = true) && !medicationName.contains("Drugs", ignoreCase = true)) {
        medicationName.trim()
      } else {
        cocktailItems.joinToString(" + ") { it.name + if (it.dosage.isNotBlank()) " ${it.dosage}" else "" }
      }
    } else {
      medicationName.ifBlank { "Injection" }.trim()
    }
    val drugNameOnly = cleanMed
      .replace(Regex("(?i)^(im|iv\\s*push|iv\\s*infusion|iv|sc)\\s*:?\\s*"), "")
      .trim()
    val dosageStr = if (!isCocktail && dosage.isNotBlank()) {
      val d = dosage.trim()
      if (drugNameOnly.contains(d, ignoreCase = true)) "" else " $d"
    } else ""
    val freq = frequency.trim()
    val isStat = freq.contains("stat", ignoreCase = true)
    val freqStr = if (freq.isNotBlank()) " $freq" else ""
    val durStr = if (isStat || totalDays <= 0) "" else {
      val dayWord = if (totalDays == 1) "1 day" else "$totalDays days"
      " $dayWord"
    }
    val patientOwnStr = if (isPatientSupplied) " [လူနာပါဆေး]" else ""
    return "$routeShort $drugNameOnly$dosageStr$freqStr$durStr$patientOwnStr".trim()
  }
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
    splitVialMode: Boolean = false,
    consumables: List<TreatmentConsumable> = emptyList()
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
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "08:00 AM", medicationFee = medicationFee, sessionFee = sessionFee, consumables = consumables))
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "08:00 PM", medicationFee = if (splitVialMode) 0.0 else medicationFee, isClinicMed = !splitVialMode, sessionFee = sessionFee, consumables = consumables))
          cal.add(Calendar.DAY_OF_YEAR, 1)
        }
      }
      frequency.contains("8", ignoreCase = true) -> {
        var count = 1
        for (d in 0 until days) {
          val dateStr = dateFormat.format(cal.time)
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "06:00 AM", medicationFee = medicationFee, sessionFee = sessionFee, consumables = consumables))
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "02:00 PM", medicationFee = if (splitVialMode) 0.0 else medicationFee, isClinicMed = !splitVialMode, sessionFee = sessionFee, consumables = consumables))
          list.add(TreatmentSession(sessionNumber = count++, scheduledDate = dateStr, scheduledTime = "10:00 PM", medicationFee = if (splitVialMode) 0.0 else medicationFee, isClinicMed = !splitVialMode, sessionFee = sessionFee, consumables = consumables))
          cal.add(Calendar.DAY_OF_YEAR, 1)
        }
      }
      frequency.contains("Stat", ignoreCase = true) -> {
        val dateStr = dateFormat.format(cal.time)
        list.add(TreatmentSession(sessionNumber = 1, scheduledDate = dateStr, scheduledTime = "Immediate", medicationFee = medicationFee, sessionFee = sessionFee, consumables = consumables))
      }
      frequency.contains("Alternate", ignoreCase = true) || frequency.contains("QOD", ignoreCase = true) || frequency.contains("တစ်ရက်ခြား", ignoreCase = true) -> {
        // Alternate Day (every 2 days)
        for (d in 1..days) {
          val dateStr = dateFormat.format(cal.time)
          list.add(TreatmentSession(sessionNumber = d, scheduledDate = dateStr, scheduledTime = "09:00 AM", medicationFee = medicationFee, sessionFee = sessionFee, consumables = consumables))
          cal.add(Calendar.DAY_OF_YEAR, 2)
        }
      }
      else -> {
        // Daily
        for (d in 1..days) {
          val dateStr = dateFormat.format(cal.time)
          list.add(TreatmentSession(sessionNumber = d, scheduledDate = dateStr, scheduledTime = "09:00 AM", medicationFee = medicationFee, sessionFee = sessionFee, consumables = consumables))
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
        medicationName = "IV Todol 30 mg + NS 3cc",
        dosage = "30 mg",
        route = TreatmentRoute.IV_PUSH,
        frequency = "Stat",
        totalDays = 1,
        totalSessions = 1,
        sessionFee = 2000.0,
        medicationFee = 3500.0,
        isCocktail = true,
        cocktailItems = listOf(
          CocktailDrugItem(name = "Todol (Ketorolac)", dosage = "30mg", quantity = 1, unitPrice = 2500.0),
          CocktailDrugItem(name = "Normal Saline (0.9%)", dosage = "3cc flush", quantity = 1, unitPrice = 1000.0)
        ),
        instructions = "Slow IV push over 2-3 mins. Check for NSAID hypersensitivity."
      ),
      TreatmentCard(
        patientId = "",
        patientName = "",
        medicationName = "IV 0.9% NS + Burplex + Vit-C",
        dosage = "500ml Drip",
        route = TreatmentRoute.IV_INFUSION,
        frequency = "Daily",
        totalDays = 1,
        totalSessions = 1,
        sessionFee = 8000.0,
        medicationFee = 8500.0,
        isCocktail = true,
        cocktailItems = listOf(
          CocktailDrugItem(name = "0.9% Normal Saline", dosage = "500ml", quantity = 1, unitPrice = 3000.0),
          CocktailDrugItem(name = "Burplex (Vitamin B-Complex)", dosage = "1 amp", quantity = 1, unitPrice = 2500.0),
          CocktailDrugItem(name = "Vitamin C Inj", dosage = "500mg (1 amp)", quantity = 1, unitPrice = 3000.0)
        ),
        instructions = "IV infusion 30 drops/min with sterile infusion set. Monitor vitals."
      ),
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
        isCocktail = true,
        cocktailItems = listOf(
          CocktailDrugItem(name = "5% Dextrose Saline", dosage = "500ml", quantity = 1, unitPrice = 3500.0),
          CocktailDrugItem(name = "Inj. Neurobion", dosage = "1 amp", quantity = 1, unitPrice = 4500.0),
          CocktailDrugItem(name = "Inj. Vitamin C", dosage = "500mg", quantity = 1, unitPrice = 4000.0)
        ),
        instructions = "Drip at 30-40 drops/min with micro-filter set. Monitor vitals."
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

/**
 * Clean 4-mode selector for Treatment Orders across Doctor Consultation and Nurse Counter:
 * 1. Single drug injection (IV push, IM, SC)
 * 2. Cocktail / IV mix (IV Infusion / Drip)
 * 3. Wound dressing procedure (Minor, Major, Infected, Burn, Stitch off)
 * 4. Nebulization therapy (Ventolin, Combivent, Pulmicort, Saline)
 */
enum class TreatmentOrderMode(val label: String, val burmeseLabel: String) {
  SINGLE_DRUG("Single Drug", "ဆေး ၁ မျိုး"),
  COCKTAIL("Cocktail / Mix", "တွဲဖက်ဆေး"),
  WOUND_DRESSING("Wound Dressing", "ဒဏ်ရာဆေးထည့်"),
  NEBULIZATION("Nebulization", "အငွေ့ရှူကုသ")
}

data class DressingQuickPreset(
  val label: String,
  val fullName: String,
  val technique: String,
  val fee: Double,
  val frequency: String,
  val days: Int,
  val instructions: String
)

val DRESSING_PRESETS = listOf(
  DressingQuickPreset(
    label = "Clean Dressing (5,000 Ks)",
    fullName = "Regular Clean Dressing (ဒဏ်ရာဆေးထည့်)",
    technique = "Aseptic Clean Dressing",
    fee = 5000.0,
    frequency = "Daily",
    days = 3,
    instructions = "Clean with Normal Saline & Povidone Iodine. Apply sterile gauze & micropore."
  ),
  DressingQuickPreset(
    label = "Infected / Large Wound (7,000 Ks)",
    fullName = "Infected / Large Wound Dressing (ပြည်တည်/ဒဏ်ရာကြီး)",
    technique = "Antiseptic Pack & Dressing",
    fee = 7000.0,
    frequency = "Daily",
    days = 5,
    instructions = "Debridement if needed, clean with H2O2 / NS. Betadine pack & sterile bandage."
  ),
  DressingQuickPreset(
    label = "Burn Dressing (7,000 Ks)",
    fullName = "Burn Dressing (မီးလောင်ဒဏ်ရာ)",
    technique = "Silver Sulphadiazine Dressing",
    fee = 7000.0,
    frequency = "Alternate Day",
    days = 5,
    instructions = "Clean with normal saline. Apply SSD cream and sterile dressing."
  ),
  DressingQuickPreset(
    label = "Suture Removal / Stitch Off (5,000 Ks)",
    fullName = "Suture Removal / Stitch Off (ချုပ်ရိုးဖြည်)",
    technique = "Stitch Off & Dressing",
    fee = 5000.0,
    frequency = "Stat",
    days = 1,
    instructions = "Clean wound edges. Remove sutures aseptically and apply sterile strip/dressing."
  ),
  DressingQuickPreset(
    label = "Minor Dressing (3,000 Ks)",
    fullName = "Simple / Minor Dressing (ဒဏ်ရာအသေး)",
    technique = "Minor Clean Dressing",
    fee = 3000.0,
    frequency = "Daily",
    days = 2,
    instructions = "Clean with antiseptic and apply sterile adhesive dressing."
  )
)

data class NebulizationQuickPreset(
  val label: String,
  val fullName: String,
  val medicineName: String,
  val dosage: String,
  val fee: Double,
  val frequency: String,
  val days: Int,
  val instructions: String
)

val NEBULIZATION_PRESETS = listOf(
  NebulizationQuickPreset(
    label = "Salbutamol / Ventolin (3,000 Ks)",
    fullName = "Ventolin (Salbutamol) Nebulization",
    medicineName = "Ventolin Respirator Solution / Respules (2.5mg)",
    dosage = "2.5mg (1 neb)",
    fee = 3000.0,
    frequency = "Stat",
    days = 1,
    instructions = "Nebulize with 2.5ml Salbutamol + 2ml Normal Saline over 10-15 mins with oxygen/air."
  ),
  NebulizationQuickPreset(
    label = "Combivent / Duolin (3,000 Ks)",
    fullName = "Combivent (Ipratropium + Salbutamol) Nebulization",
    medicineName = "Combivent / Duolin Respules",
    dosage = "1 respule (2.5ml)",
    fee = 3000.0,
    frequency = "BD",
    days = 2,
    instructions = "Nebulize 1 respule over 10-15 mins. Monitor pulse and respiratory rate."
  ),
  NebulizationQuickPreset(
    label = "Pulmicort (Budesonide) (4,000 Ks)",
    fullName = "Pulmicort (Budesonide) Nebulization",
    medicineName = "Pulmicort (Budesonide) Respules 0.5mg/2ml",
    dosage = "0.5mg (1 respule)",
    fee = 4000.0,
    frequency = "BD",
    days = 3,
    instructions = "Nebulize 1 respule. Rinse mouth with water after nebulization."
  ),
  NebulizationQuickPreset(
    label = "Normal Saline Inhalation (2,500 Ks)",
    fullName = "Normal Saline 0.9% Moist Nebulization",
    medicineName = "Normal Saline 0.9% Inhalation",
    dosage = "4-5 ml",
    fee = 2500.0,
    frequency = "TDS",
    days = 2,
    instructions = "Moist inhalation with sterile normal saline to loosen phlegm."
  )
)

