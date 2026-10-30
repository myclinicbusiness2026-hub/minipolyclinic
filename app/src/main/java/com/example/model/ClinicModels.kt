package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class QueueStatus(val displayName: String, val myanmarName: String = displayName) {
  WAITING("Waiting for Doctor", "စောင့်ဆိုင်းဆဲ"),
  IN_CONSULTATION("In Consultation", "ဆရာဝန်ပြသဆဲ"),
  AT_PHARMACY("Pharmacy Queue", "ဆေးထုတ်တန်းစီ"),
  AT_BILLING("Cashier Billing", "ငွေရှင်းကောင်တာ"),
  COMPLETED("Completed", "ပြီးစီး")
}

enum class Gender(val label: String) {
  MALE("ကျား (Male)"),
  FEMALE("မ (Female)"),
  OTHER("အခြား (Other)")
}

data class VitalSigns(
  val bloodPressure: String = "120/80 mmHg",
  val pulseRate: Int = 76,
  val temperatureCelsius: Double = 98.4,
  val oxygenSaturation: Int = 98,
  val weightKg: Double = 60.0,
  val rbs: Double? = null,
  val heightCm: Double = 165.0
) {
  val bmi: Double get() {
    val hM = heightCm / 100.0
    return if (hM > 0) Math.round((weightKg / (hM * hM)) * 10.0) / 10.0 else 0.0
  }
  val formattedTemperatureF: String get() {
    val f = if (temperatureCelsius > 50.0) temperatureCelsius else (temperatureCelsius * 9.0 / 5.0) + 32.0
    return String.format(Locale.getDefault(), "%.1f°F", f)
  }
  val temperatureFahrenheit: Double get() = if (temperatureCelsius > 50.0) temperatureCelsius else (temperatureCelsius * 9.0 / 5.0) + 32.0
}

data class Patient(
  val id: String = UUID.randomUUID().toString(),
  val tokenNumber: Int = 1,
  val hn: String = "HN-${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}-${(1000..9999).random()}",
  val queueNumber: Int = tokenNumber,
  val name: String,
  val age: Int,
  val gender: Gender = Gender.MALE,
  val phone: String = "",
  val address: String = "မန္တလေးမြို့",
  val status: QueueStatus = QueueStatus.WAITING,
  val registrationTime: String = "",
  val vitals: VitalSigns = VitalSigns(),
  val knownAllergies: List<String> = emptyList(),
  val allergies: List<String> = knownAllergies,
  val chronicDiseases: List<String> = emptyList(),
  val notes: String = "",
  val followUpDueDate: String? = null,
  val isReminderSent: Boolean = false,
  val isFollowUp: Boolean = false,
  val followUpReason: String = "",
  val chiefComplaint: String = "",
  val bloodType: String = "O+"
) {
  val effectiveAllergies: List<String> get() = if (allergies.isNotEmpty()) allergies else knownAllergies
  val registrationDate: String get() {
    val trimmed = registrationTime.trim()
    if (trimmed.isEmpty()) return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
      return trimmed.take(10)
    }
    if (trimmed.length >= 10 && trimmed[2] == '/' && trimmed[5] == '/') {
      val d = trimmed.substring(0, 2)
      val m = trimmed.substring(3, 5)
      val y = trimmed.substring(6, 10)
      return "$y-$m-$d"
    }
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  }
  val registeredAt: String get() = registrationTime
  val visitNumber: Int get() = tokenNumber
  val formattedRegistrationDisplay: String get() = if (registrationTime.length >= 16) registrationTime.take(16) else registrationTime
}

data class StockBatch(
  val batchNumber: String,
  val expiryDate: String, // YYYY-MM-DD
  val quantity: Int,
  val costPricePerUnit: Double,
  val supplierName: String? = null,
  val lastUpdated: Long = System.currentTimeMillis()
)

data class Medicine(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val genericName: String,
  val dosageForm: String, // Tablet, Capsule, Syrup, Injection, Cream, Inhaler, Consumable
  val category: String, // Antibiotics, Analgesic, Antacid, Cardiovascular, Antidiabetic, Respiratory, Consumable
  val unitType: String = "Strip",
  val batches: List<StockBatch> = emptyList(),
  val minStockThreshold: Int = 20,
  val profitMarginPercent: Double = 25.0,
  val isAllergyWarning: Boolean = false,
  val primarySupplier: String = "AA Medical Products",
  val standardDosage: String = ""
) {
  val supplierName: String get() = primarySupplier
  val totalStock: Int get() = batches.sumOf { it.quantity }
  val sortedBatches: List<StockBatch> get() = batches.filter { it.quantity > 0 }.sortedBy { it.expiryDate }
  val allBatches: List<StockBatch> get() = batches.sortedBy { it.expiryDate }
  val earliestExpiry: String get() = sortedBatches.firstOrNull()?.expiryDate ?: (batches.firstOrNull()?.expiryDate ?: "N/A")
  val latestCostPrice: Double get() = sortedBatches.firstOrNull()?.costPricePerUnit ?: (batches.firstOrNull()?.costPricePerUnit ?: 0.0)
  val calculatedSellingPrice: Double get() {
    val cost = latestCostPrice
    return cost + (cost * (profitMarginPercent / 100.0))
  }
  val currentSellingPrice: Double get() = calculatedSellingPrice
  val effectiveCostPrice: Double get() = latestCostPrice
  val sellingPrice: Double get() = calculatedSellingPrice
}

data class PrescribedDrug(
  val medicineId: String = "",
  val medicineName: String = "",
  val name: String = medicineName,
  val dosage: String = "",
  val frequency: String = "",
  val duration: String = "",
  val quantity: Int = 1,
  val unitPrice: Double = 0.0,
  val instructions: String = "",
  val isExternal: Boolean = false
) {
  val effectiveName: String get() = if (name.isNotBlank()) name else medicineName
  val totalPrice: Double get() = quantity * unitPrice

  constructor(
    medicineId: String,
    medicineName: String,
    dosage: String,
    frequency: String,
    duration: String,
    quantity: Int,
    unitPrice: Double,
    instructions: String,
    isExternal: Boolean = false
  ) : this(
    medicineId = medicineId,
    medicineName = medicineName,
    name = medicineName,
    dosage = dosage,
    frequency = frequency,
    duration = duration,
    quantity = quantity,
    unitPrice = unitPrice,
    instructions = instructions,
    isExternal = isExternal
  )
}

data class ProcedureConsumable(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val quantity: Int = 1,
  val unitType: String = "Item",
  val unitPrice: Double = 0.0,
  val medicineId: String? = null
) {
  val totalPrice: Double get() = quantity * unitPrice
}

data class ProcedureItem(
  val id: String = UUID.randomUUID().toString(),
  val code: String = id,
  val name: String,
  val myanmarName: String = name,
  val category: String = "Minor Surgery",
  val price: Double = 0.0,
  val standardFee: Double = price,
  val consumableCost: Double = 0.0,
  val notes: String = "",
  val dateTime: String = "",
  val isCustom: Boolean = false,
  val consumableItems: List<ProcedureConsumable> = emptyList(),
  val description: String = ""
) {
  val totalProcedureFee: Double get() = standardFee + consumableItems.sumOf { it.totalPrice } + consumableCost
  val consumables: List<String> get() = consumableItems.map { it.name }
}

data class DrawPoint(val x: Float, val y: Float) {
  val asOffset: Offset get() = Offset(x, y)
}

data class DrawStroke(
  val points: List<DrawPoint> = emptyList(),
  val colorHex: Long = 0xFF000000,
  val strokeWidth: Float = 4.0f
) {
  val color: Color get() = Color(colorHex)
  val offsets: List<Offset> get() = points.map { Offset(it.x, it.y) }
}

data class DrawingPage(
  val pageIndex: Int = 0,
  val strokes: MutableList<DrawStroke> = mutableListOf(),
  val template: AnatomyGuideTemplate = AnatomyGuideTemplate.BLANK,
  val notes: String = "",
  var baseWidth: Float = 800f,
  var baseHeight: Float = 1131f
)

data class MedicalLeaveCertificate(
  val id: String = UUID.randomUUID().toString(),
  val patientId: String = "",
  val patientName: String,
  val leaveDays: Int = 3,
  val startDate: String,
  val endDate: String,
  val diagnosis: String,
  val doctorName: String = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
  val samaNumber: String = "ဆမ-၂၄၅၈၉",
  val dutyStatus: String = "Unfit for duty",
  val patientAge: Int = 30,
  val patientGender: String = "Male",
  val patientHn: String = "",
  val remarks: String = "",
  val issueDate: String = startDate,
  val certificateNo: String = "MC-${id.take(6).uppercase()}",
  val fitnessStatus: String = dutyStatus
)

typealias MedicalLeaveRecord = MedicalLeaveCertificate

data class ReferralRecord(
  val id: String = UUID.randomUUID().toString(),
  val referralNo: String = "REF-${id.take(6).uppercase()}",
  val patientId: String = "",
  val patientName: String,
  val referralTo: String,
  val specialty: String = "General Medicine / Specialist",
  val hospitalOrClinic: String = "Mandalay General Hospital (MGH)",
  val clinicalSummary: String,
  val reasonForReferral: String,
  val doctorName: String,
  val doctorSama: String = "ဆမ-၂၄၅၈၉",
  val patientAge: Int = 30,
  val patientGender: String = "Male",
  val patientHn: String = "",
  val referralDate: String = "",
  val urgencyLevel: String = "Routine",
  val provisionalDiagnosis: String = "",
  val currentMedicationsGiven: String = "",
  val specialInstructions: String = ""
)

data class ConsultationRecord(
  val id: String = UUID.randomUUID().toString(),
  val patientId: String,
  val patientName: String,
  val clinicName: String = "Shwe Khit Clinic",
  val doctorName: String = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
  val doctorDegree: String = "M.B.,B.S(MDY)",
  val doctorSama: String = "ဆမ-၂၄၅၈၉",
  val subjective: String = "",
  val objective: String = "",
  val assessment: String = "",
  val plan: String = "",
  val diagnosisList: List<String> = emptyList(),
  val prescriptions: List<PrescribedDrug> = emptyList(),
  val selectedProcedures: List<ProcedureItem> = emptyList(),
  val suggestedInvestigations: List<String> = emptyList(),
  val consultationFee: Double = 8000.0,
  val drawingPages: List<DrawingPage> = emptyList(),
  val canvasNotes: String = "",
  val followUpAppointment: String = "",
  val followUpDueDate: String? = null,
  val nextVaccineSchedule: String = "",
  val patientInstructions: String = "",
  val medicalLeave: MedicalLeaveCertificate? = null,
  val referralRecord: ReferralRecord? = null,
  val attachments: List<MedicalAttachment> = emptyList(),
  val vitals: VitalSigns = VitalSigns(),
  val timestamp: String = ""
)

data class InvoiceItem(
  val name: String,
  val quantity: Int,
  val unitPrice: Double,
  val totalPrice: Double,
  val isProcedure: Boolean = false,
  val category: String = if (isProcedure) "Procedure" else "Pharmacy"
) {
  val description: String get() = name
  val total: Double get() = totalPrice
}

enum class PaymentMethod(val label: String, val myanmarLabel: String = label) {
  CASH("Cash", "ငွေသား"),
  KBZPAY("KPay", "KBZPay"),
  WAVEPAY("Wave", "WavePay"),
  OTHER("Other", "အခြား");

  companion object {
    val KBZ_PAY get() = KBZPAY
    val WAVE_PAY get() = WAVEPAY
    val BANK_TRANSFER get() = OTHER
    fun fromString(value: String): PaymentMethod {
      return entries.firstOrNull {
        it.name.equals(value, ignoreCase = true) ||
        it.label.equals(value, ignoreCase = true)
      } ?: CASH
    }
  }
}

data class Invoice(
  val id: String = UUID.randomUUID().toString(),
  val invoiceNumber: String = "INV-${System.currentTimeMillis()}",
  val consultationId: String = "",
  val patientId: String = "",
  val patientName: String = "",
  val items: List<InvoiceItem> = emptyList(),
  val consultationFee: Double = 8000.0,
  val serviceFee: Double = 0.0,
  val discount: Double = 0.0,
  val tax: Double = 0.0,
  val totalAmount: Double = 0.0,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val paymentStatus: String = "Unpaid",
  val paidAmount: Double = 0.0,
  val changeAmount: Double = 0.0,
  val cashierName: String = "Counter Staff",
  val timestamp: String = "",
  val isPaid: Boolean = paymentStatus.equals("Paid", ignoreCase = true),
  val discountPercent: Double = 0.0,
  val doctorFeeDiscount: Double = 0.0,
  val pharmacyDiscount: Double = 0.0,
  val procedureDiscount: Double = 0.0,
  val isInventoryDeducted: Boolean = false
) {
  val invoiceNo: String get() = invoiceNumber
  val createdAt: String get() = timestamp
  val dateString: String get() = if (timestamp.length >= 10) timestamp.take(10) else timestamp
  val grandTotal: Double get() = totalAmount
  val subTotal: Double get() = items.sumOf { it.totalPrice } + consultationFee + serviceFee
  val totalGranularDiscount: Double get() = doctorFeeDiscount + pharmacyDiscount + procedureDiscount
  val discountAmount: Double get() = if (discount > 0.0) discount else if (totalGranularDiscount > 0.0) totalGranularDiscount else (subTotal * (discountPercent / 100.0))
  val taxAmount: Double get() = tax
  val taxPercent: Double get() = if (subTotal > 0 && tax > 0) (tax / subTotal) * 100.0 else 0.0
}

enum class UserRole(val displayName: String, val myanmarName: String = displayName) {
  ADMIN("Administrator", "မန်နေဂျာ"),
  DOCTOR("Doctor", "ဆရာဝန်"),
  PHARMACIST("Pharmacist", "ဆေးဝါးကျွမ်းကျင်"),
  CASHIER("Cashier / Pharmacy", "ငွေကိုင် / ဆေးပေး"),
  NURSE("Nurse", "သူနာပြု"),
  RECEPTIONIST("Receptionist", "ဧည့်ကြို / စာရင်းသွင်း");

  val description: String get() = "$displayName ($myanmarName)"
}

data class UserRolePermissions(
  val canAccessDoctorEmr: Boolean = false,
  val canAccessPharmacy: Boolean = false,
  val canAccessBilling: Boolean = false,
  val canAccessPatientQueue: Boolean = false,
  val canAccessProcedures: Boolean = false,
  val canAccessVaccineTracker: Boolean = false,
  val canAccessInventoryMaster: Boolean = false,
  val canAccessFinancialLedger: Boolean = false,
  val canManageUsers: Boolean = false,
  val canExportData: Boolean = false
) {
  companion object {
    fun defaultFor(role: UserRole): UserRolePermissions {
      return when (role) {
        UserRole.ADMIN -> UserRolePermissions(
          canAccessDoctorEmr = true,
          canAccessPharmacy = true,
          canAccessBilling = true,
          canAccessPatientQueue = true,
          canAccessProcedures = true,
          canAccessVaccineTracker = true,
          canAccessInventoryMaster = true,
          canAccessFinancialLedger = true,
          canManageUsers = true,
          canExportData = true
        )
        UserRole.DOCTOR -> UserRolePermissions(
          canAccessDoctorEmr = true,
          canAccessPharmacy = true,
          canAccessBilling = true,
          canAccessPatientQueue = true,
          canAccessProcedures = true,
          canAccessVaccineTracker = true,
          canAccessInventoryMaster = true,
          canAccessFinancialLedger = true,
          canManageUsers = false,
          canExportData = true
        )
        UserRole.PHARMACIST -> UserRolePermissions(
          canAccessDoctorEmr = false,
          canAccessPharmacy = true,
          canAccessBilling = true,
          canAccessPatientQueue = true,
          canAccessProcedures = false,
          canAccessVaccineTracker = false,
          canAccessInventoryMaster = true,
          canAccessFinancialLedger = false,
          canManageUsers = false,
          canExportData = false
        )
        UserRole.CASHIER -> UserRolePermissions(
          canAccessDoctorEmr = false,
          canAccessPharmacy = false,
          canAccessBilling = true,
          canAccessPatientQueue = true,
          canAccessProcedures = false,
          canAccessVaccineTracker = false,
          canAccessInventoryMaster = false,
          canAccessFinancialLedger = true,
          canManageUsers = false,
          canExportData = false
        )
        UserRole.NURSE -> UserRolePermissions(
          canAccessDoctorEmr = false,
          canAccessPharmacy = false,
          canAccessBilling = false,
          canAccessPatientQueue = true,
          canAccessProcedures = true,
          canAccessVaccineTracker = true,
          canAccessInventoryMaster = false,
          canAccessFinancialLedger = false,
          canManageUsers = false,
          canExportData = false
        )
        UserRole.RECEPTIONIST -> UserRolePermissions(
          canAccessDoctorEmr = false,
          canAccessPharmacy = false,
          canAccessBilling = false,
          canAccessPatientQueue = true,
          canAccessProcedures = false,
          canAccessVaccineTracker = false,
          canAccessInventoryMaster = false,
          canAccessFinancialLedger = false,
          canManageUsers = false,
          canExportData = false
        )
      }
    }
  }
}

data class UserAccount(
  val id: String = UUID.randomUUID().toString(),
  val username: String,
  val fullName: String,
  val role: UserRole,
  val samaNumber: String = "",
  val degree: String = "",
  val pinCode: String = "1234",
  val passwordHash: String = "123456",
  val isActive: Boolean = true,
  val phone: String = "",
  val email: String = "",
  val department: String = "",
  val lastLoginAt: String = "",
  val customPermissions: UserRolePermissions = UserRolePermissions.defaultFor(role)
) {
  val effectivePermissions: UserRolePermissions
    get() = customPermissions
}

data class Supplier(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val contactPerson: String = "",
  val phone: String = "",
  val address: String = "Mandalay",
  val email: String = ""
)

data class SyncLog(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: String,
  val action: String,
  val status: String,
  val details: String = ""
)

data class VaccineRecord(
  val id: String = UUID.randomUUID().toString(),
  val patientId: String,
  val patientName: String,
  val vaccineName: String,
  val doseNumber: String = "Dose 1",
  val givenDate: String = "",
  val dateAdministered: String = givenDate,
  val nextDueDate: String? = null,
  val batchNumber: String = "",
  val batchLotNumber: String = batchNumber,
  val givenBy: String = "Nurse",
  val remarks: String = "",
  val regimeName: String = vaccineName
) {
  constructor(
    id: String = UUID.randomUUID().toString(),
    patientId: String,
    patientName: String,
    vaccineName: String,
    doseNumber: Int,
    givenDate: String = "",
    nextDueDate: String? = null,
    batchNumber: String = "",
    givenBy: String = "Nurse"
  ) : this(
    id = id,
    patientId = patientId,
    patientName = patientName,
    vaccineName = vaccineName,
    doseNumber = "Dose $doseNumber",
    givenDate = givenDate,
    dateAdministered = givenDate,
    nextDueDate = nextDueDate,
    batchNumber = batchNumber,
    batchLotNumber = batchNumber,
    givenBy = givenBy,
    remarks = "",
    regimeName = vaccineName
  )
}

data class ExpenseRecord(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "",
  val category: String = "Supplies",
  val amount: Double = 0.0,
  val description: String = title,
  val date: String = "",
  val recordedBy: String = "Staff",
  val timestamp: String = date,
  val voucherNo: String = "EXP-${id.take(6).uppercase()}",
  val payee: String = "",
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val notes: String = ""
) {
  constructor(
    id: String = UUID.randomUUID().toString(),
    category: String,
    amount: Double,
    description: String,
    date: String,
    recordedBy: String = "Staff"
  ) : this(
    id = id,
    title = description,
    category = category,
    amount = amount,
    description = description,
    date = date,
    recordedBy = recordedBy
  )
}

typealias ClinicExpense = ExpenseRecord

data class VitalHistoryPoint(
  val id: String = UUID.randomUUID().toString(),
  val date: String = "",
  val bpSystolic: Int = 120,
  val bpDiastolic: Int = 80,
  val pulse: Int = 76,
  val rbs: Double? = null,
  val weightKg: Double? = null,
  val notes: String = ""
)

data class MedicalAttachment(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "",
  val category: String = "Lab Report",
  val date: String = "",
  val imageUri: String = "",
  val filePath: String = imageUri,
  val fileType: String = "Image",
  val fileName: String = title,
  val notes: String = ""
)

data class InstructionPreset(
  val code: String,
  val title: String,
  val text: String
)

val DEFAULT_INSTRUCTION_PRESETS = listOf(
  InstructionPreset("1", "ဖျားနာ", "• ရေများများသောက်ပါ\n• အနားယူပါ\n• ဆေးကို အစာစားပြီးမှ သောက်ပါ\n• အဖျားမကျပါက ၃ ရက်အကြာ ဆေးခန်းပြန်ပြပါ"),
  InstructionPreset("2", "သွေးတိုး", "• ဆားနှင့် ငံပြာရည် လျှော့စားပါ\n• နေ့စဉ် သွေးပေါင်ချိန် မှတ်တမ်းတင်ပါ\n• သွေးတိုးကျဆေးကို နေ့စဉ်ပုံမှန် သောက်ပါ"),
  InstructionPreset("3", "ဆီးချို", "• အချိုနှင့် ကစီဓာတ် လျှော့စားပါ\n• လေ့ကျင့်ခန်း ပုံမှန်ပြုလုပ်ပါ\n• ခြေထောက်ဒဏ်ရာမဖြစ်အောင် ဂရုစိုက်ပါ"),
  InstructionPreset("4", "အစာအိမ်", "• အစပ်၊ အချဉ်၊ အဆီကြော် ရှောင်ပါ\n• အစာကို အချိန်မှန် စားပါ\n• ဆေးကို အစာမစားမီ ကြိုသောက်ပါ"),
  InstructionPreset("5", "ချောင်းဆိုး", "• ရေနွေးနွေး သောက်ပါ\n• အအေးနှင့် ဖုန်မှုန့် ရှောင်ပါ\n• ရင်ကျပ်လာပါက ချက်ချင်း ဆေးရုံ/ဆေးခန်း ပြသပါ"),
  InstructionPreset("6", "ဒဏ်ရာ", "• ဒဏ်ရာကို ရေမစိုစေရန် သတိပြုပါ\n• နေ့စဉ် ဆေးထည့်ပတ်တီးလဲပါ\n• ပိုးသတ်ဆေးကို ရက်ပြည့်အောင် သောက်ပါ")
)

typealias SyncLogEntry = com.example.sync.SyncAuditEntry

