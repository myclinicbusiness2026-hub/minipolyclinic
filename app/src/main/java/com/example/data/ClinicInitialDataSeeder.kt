package com.example.data

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ClinicInitialDataSeeder {

  fun getSeedMedicines(): List<Medicine> = listOf(
    Medicine(
      id = "MED-001",
      name = "Amoxicillin + Clavulanate (Augmentin)",
      genericName = "Co-Amoxiclav 625mg",
      dosageForm = "Tablet",
      category = "Antibiotics",
      profitMarginPercent = 30.0,
      isAllergyWarning = true,
      batches = listOf(
        StockBatch(batchNumber = "AUG-2401", expiryDate = "2026-11-30", quantity = 35, costPricePerUnit = 850.0),
        StockBatch(batchNumber = "AUG-2409", expiryDate = "2027-08-15", quantity = 120, costPricePerUnit = 820.0)
      )
    ),
    Medicine(
      id = "MED-002",
      name = "Paracetamol (Biogesic)",
      genericName = "Acetaminophen 500mg",
      dosageForm = "Tablet",
      category = "Analgesic / Antipyretic",
      profitMarginPercent = 25.0,
      batches = listOf(
        StockBatch(batchNumber = "PCM-239", expiryDate = "2026-10-15", quantity = 40, costPricePerUnit = 80.0),
        StockBatch(batchNumber = "PCM-245", expiryDate = "2027-05-20", quantity = 250, costPricePerUnit = 75.0)
      )
    ),
    Medicine(
      id = "MED-003",
      name = "Omeprazole (Omez)",
      genericName = "Omeprazole 20mg",
      dosageForm = "Capsule",
      category = "Antacid / PPI",
      profitMarginPercent = 35.0,
      batches = listOf(
        StockBatch(batchNumber = "OMZ-88", expiryDate = "2026-12-31", quantity = 60, costPricePerUnit = 180.0)
      )
    ),
    Medicine(
      id = "MED-004",
      name = "Ibuprofen (Brufen)",
      genericName = "Ibuprofen 400mg",
      dosageForm = "Tablet",
      category = "NSAID / Pain Relief",
      profitMarginPercent = 25.0,
      batches = listOf(
        StockBatch(batchNumber = "IBU-101", expiryDate = "2027-04-10", quantity = 80, costPricePerUnit = 150.0)
      )
    ),
    Medicine(
      id = "MED-005",
      name = "Metformin HCl (Glucophage)",
      genericName = "Metformin 500mg",
      dosageForm = "Tablet",
      category = "Antidiabetic",
      profitMarginPercent = 20.0,
      batches = listOf(
        StockBatch(batchNumber = "MET-102", expiryDate = "2027-02-28", quantity = 90, costPricePerUnit = 120.0)
      )
    ),
    Medicine(
      id = "MED-006",
      name = "Amlodipine Besylate (Norvasc)",
      genericName = "Amlodipine 5mg",
      dosageForm = "Tablet",
      category = "Cardiovascular / Anti-HTN",
      profitMarginPercent = 25.0,
      batches = listOf(
        StockBatch(batchNumber = "AML-401", expiryDate = "2026-09-30", quantity = 15, costPricePerUnit = 220.0)
      )
    ),
    Medicine(
      id = "MED-007",
      name = "Azithromycin (Zithromax)",
      genericName = "Azithromycin 500mg",
      dosageForm = "Tablet",
      category = "Antibiotics",
      profitMarginPercent = 30.0,
      isAllergyWarning = true,
      batches = listOf(
        StockBatch(batchNumber = "AZM-303", expiryDate = "2027-01-15", quantity = 45, costPricePerUnit = 900.0)
      )
    ),
    Medicine(
      id = "MED-008",
      name = "Cough Syrup (Ascoril)",
      genericName = "Bromhexine + Guaiphenesin",
      dosageForm = "Syrup",
      category = "Respiratory",
      unitType = "Bottle",
      profitMarginPercent = 28.0,
      batches = listOf(
        StockBatch(batchNumber = "ASC-55", expiryDate = "2026-11-20", quantity = 30, costPricePerUnit = 2500.0)
      )
    ),
    Medicine(
      id = "MED-009",
      name = "Ciprofloxacin 500mg",
      genericName = "Ciprofloxacin 500mg",
      dosageForm = "Tablet",
      category = "Antibiotics",
      profitMarginPercent = 25.0,
      batches = listOf(
        StockBatch(batchNumber = "CIP-99", expiryDate = "2027-06-30", quantity = 70, costPricePerUnit = 320.0)
      )
    ),
    Medicine(
      id = "MED-010",
      name = "Salbutamol Inhaler (Ventolin)",
      genericName = "Salbutamol 100mcg",
      dosageForm = "Inhaler",
      category = "Respiratory",
      unitType = "Piece",
      profitMarginPercent = 20.0,
      batches = listOf(
        StockBatch(batchNumber = "VEN-12", expiryDate = "2027-09-10", quantity = 25, costPricePerUnit = 6500.0)
      )
    ),
    Medicine(
      id = "MED-011",
      name = "Cetirizine (Zyrtec)",
      genericName = "Cetirizine 10mg",
      dosageForm = "Tablet",
      category = "Antihistamine / Allergy",
      profitMarginPercent = 30.0,
      batches = listOf(
        StockBatch(batchNumber = "CET-801", expiryDate = "2027-12-15", quantity = 110, costPricePerUnit = 140.0)
      )
    ),
    Medicine(
      id = "MED-012",
      name = "Betamethasone 0.1% Cream",
      genericName = "Betamethasone Valerate",
      dosageForm = "Cream",
      category = "Dermatology",
      unitType = "Tube",
      profitMarginPercent = 30.0,
      batches = listOf(
        StockBatch(batchNumber = "BTM-44", expiryDate = "2027-03-20", quantity = 35, costPricePerUnit = 1350.0)
      )
    )
  )

  fun getSeedProcedures(): List<ProcedureItem> = listOf(
    ProcedureItem(id = "PROC-01", code = "PROC-01", name = "Wound Dressing (Normal)", category = "Nursing", price = 3000.0, description = "Antiseptic cleaning & sterile gauze dressing"),
    ProcedureItem(id = "PROC-02", code = "PROC-02", name = "Minor Suturing (1-3 stitches)", category = "Minor Surgery", price = 12000.0, description = "Local anesthesia & non-absorbable sutures"),
    ProcedureItem(id = "PROC-03", code = "PROC-03", name = "Incision & Drainage (I&D)", category = "Minor Surgery", price = 15000.0, description = "Abscess evacuation & gauze ribbon packing"),
    ProcedureItem(id = "PROC-04", code = "PROC-04", name = "Nebulization Therapy (1 session)", category = "Respiratory", price = 4000.0, description = "Salbutamol + Ipratropium respule nebulization"),
    ProcedureItem(id = "PROC-05", code = "PROC-05", name = "IV Infusion & Cannulation", category = "Nursing", price = 7000.0, description = "IV cannula insertion & Dextrose/Saline drip"),
    ProcedureItem(id = "PROC-06", code = "PROC-06", name = "IM / IV Injection Only", category = "Nursing", price = 2000.0, description = "Administration fee with sterile disposable syringe"),
    ProcedureItem(id = "PROC-07", code = "PROC-07", name = "ECG (12 Lead)", category = "Investigation", price = 8000.0, description = "Standard 12-lead diagnostic electrocardiogram"),
    ProcedureItem(id = "PROC-08", code = "PROC-08", name = "RBS (Finger Prick Glucose)", category = "Investigation", price = 2500.0, description = "Rapid glucometer capillary blood glucose")
  )

  fun getSeedPatients(): List<Patient> {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    return listOf(
      Patient(
        id = "PAT-001",
        tokenNumber = 1,
        name = "ဦးအောင်နိုင်",
        age = 52,
        gender = Gender.MALE,
        phone = "09450012345",
        address = "မဟာအောင်မြေမြို့နယ်၊ မန္တလေး",
        status = QueueStatus.WAITING,
        registrationTime = "$today 09:15 AM",
        vitals = VitalSigns(bloodPressure = "140/90 mmHg", pulseRate = 82, temperatureCelsius = 98.4, oxygenSaturation = 98, weightKg = 68.0, rbs = 145.0),
        chronicDiseases = listOf("Hypertension (သွေးတိုး)"),
        notes = "ဆေးခန်းပြသရန် လာရောက်စောင့်ဆိုင်းနေပါသည်"
      ),
      Patient(
        id = "PAT-002",
        tokenNumber = 2,
        name = "ဒေါ်ခင်စိုးမြတ်",
        age = 45,
        gender = Gender.FEMALE,
        phone = "09250123456",
        address = "ချမ်းအေးသာစံမြို့နယ်၊ မန္တလေး",
        status = QueueStatus.IN_CONSULTATION,
        registrationTime = "$today 09:30 AM",
        vitals = VitalSigns(bloodPressure = "125/80 mmHg", pulseRate = 78, temperatureCelsius = 100.2, oxygenSaturation = 97, weightKg = 56.0, rbs = 108.0),
        knownAllergies = listOf("Penicillin"),
        notes = "ဖျားနာခြင်းနှင့် လည်ချောင်းနာခြင်း ၂ ရက်"
      ),
      Patient(
        id = "PAT-003",
        tokenNumber = 3,
        name = "မောင်မင်းသန့်",
        age = 24,
        gender = Gender.MALE,
        phone = "09970112233",
        address = "ပြည်ကြီးတံခွန်မြို့နယ်၊ မန္တလေး",
        status = QueueStatus.AT_PHARMACY,
        registrationTime = "$today 09:45 AM",
        vitals = VitalSigns(bloodPressure = "110/70 mmHg", pulseRate = 74, temperatureCelsius = 98.2, oxygenSaturation = 99, weightKg = 62.0, rbs = 95.0),
        notes = "ဆိုင်ကယ်ချော်လဲ၍ ဒူးပွန်းဒဏ်ရာ"
      ),
      Patient(
        id = "PAT-004",
        tokenNumber = 4,
        name = "ဒေါ်မာလာဦး",
        age = 63,
        gender = Gender.FEMALE,
        phone = "09790445566",
        address = "အောင်မြေသာစံမြို့နယ်၊ မန္တလေး",
        status = QueueStatus.AT_BILLING,
        registrationTime = "$today 10:00 AM",
        vitals = VitalSigns(bloodPressure = "130/85 mmHg", pulseRate = 72, temperatureCelsius = 98.0, oxygenSaturation = 98, weightKg = 54.0, rbs = 160.0),
        chronicDiseases = listOf("Type 2 Diabetes (ဆီးချို)"),
        notes = "ဆီးချိုဆေး ပုံမှန်လာသောက်ခြင်း"
      ),
      Patient(
        id = "PAT-005",
        tokenNumber = 5,
        name = "ဦးတင်ဝင်း",
        age = 48,
        gender = Gender.MALE,
        phone = "09440889900",
        address = "ချမ်းမြသာစည်မြို့နယ်၊ မန္တလေး",
        status = QueueStatus.COMPLETED,
        registrationTime = "$today 08:45 AM",
        vitals = VitalSigns(bloodPressure = "120/80 mmHg", pulseRate = 76, temperatureCelsius = 98.6, oxygenSaturation = 98, weightKg = 70.0, rbs = 112.0),
        notes = "အစာအိမ်အောင့်၍ ပြသပြီး ဆေးထုတ်ယူပြီးစီး"
      )
    )
  }

  fun getSeedUsers(): List<UserAccount> = listOf(
    UserAccount(
      id = "USR-001",
      username = "nks",
      fullName = "Dr. Nyan Kaung Sett (Admin)",
      role = UserRole.ADMIN,
      passwordHash = "2121994",
      pinCode = "2121994",
      samaNumber = "ဆမ-၂၄၅၈၉",
      degree = "M.B.,B.S (Mandalay)"
    ),
    UserAccount(
      id = "USR-002",
      username = "shwekhit",
      fullName = "Cashier Staff (Shwe Khit)",
      role = UserRole.CASHIER,
      passwordHash = "123456",
      pinCode = "123456"
    )
  )

  fun getSeedSuppliers(): List<Supplier> = listOf(
    Supplier("SUP-01", "AA Medical Products Ltd", "Ko Zaw Min", "09420011223", "73rd Street, Mandalay"),
    Supplier("SUP-02", "Mega Lifesciences Myanmar", "Daw Thida", "095123456", "35th Street, Mandalay"),
    Supplier("SUP-03", "DKSH Myanmar Healthcare", "U Myo Win", "09790123456", "Chanayethazan, Mandalay")
  )

  fun getSeedConsultations(): List<ConsultationRecord> = listOf(
    ConsultationRecord(
      id = "CONS-20261017-001",
      patientId = "PAT-005",
      patientName = "ဦးတင်ဝင်း",
      doctorName = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
      subjective = "[CHIEF COMPLAINTS & HPI]\n#1. Epigastric burning pain - 3 days [Moderate, Gradual]\n   → Relieved by: Milk, Antacid\n   → Aggravated by: Spicy food, Fasting",
      objective = "• Abdomen: Soft, mild epigastric tenderness (+), no guarding, bowel sounds normal\n• Vitals: BP 120/80 mmHg, PR 76, Temp 98.6F",
      assessment = "GERD / Gastritis (အစာအိမ်ရောင်ခြင်း)",
      diagnosisList = listOf("GERD", "Acute Gastritis"),
      plan = "1. Avoid spicy and deep-fried oily foods\n2. Have meals at regular scheduled times\n3. Take PPI regularly before morning meals",
      prescriptions = listOf(
        PrescribedDrug("MED-003", "Omeprazole 20mg", "20mg", "1-0-0 (Before meal)", "14 Days", 14, 250.0, "မနက် အစာမစားမီ မိနစ် ၃၀ ကြိုသောက်ရန်")
      ),
      consultationFee = 8000.0,
      timestamp = "2026-10-17 09:10 AM"
    )
  )

  fun getSeedInvoices(): List<Invoice> = listOf(
    Invoice(
      id = "INV-20261017-001",
      invoiceNumber = "INV-1001",
      consultationId = "CONS-20261017-001",
      patientId = "PAT-005",
      patientName = "ဦးတင်ဝင်း",
      items = listOf(
        InvoiceItem("Omeprazole 20mg (14 Days)", 14, 250.0, 3500.0)
      ),
      consultationFee = 8000.0,
      totalAmount = 11500.0,
      paymentMethod = PaymentMethod.CASH,
      paymentStatus = "Unpaid",
      paidAmount = 0.0,
      cashierName = "Ma Hnin Wai",
      timestamp = "2026-10-17 09:15 AM"
    )
  )

  fun getSeedExpenses(): List<ExpenseRecord> = listOf(
    ExpenseRecord(UUID.randomUUID().toString(), "Clinic Supplies", 15000.0, "Alcohol, Cotton, Gauze refills", "2026-10-17", "Admin"),
    ExpenseRecord(UUID.randomUUID().toString(), "Electricity / Utility", 35000.0, "Clinic monthly utility bill", "2026-10-15", "Admin")
  )

  fun getSeedVaccines(): List<VaccineRecord> = listOf(
    VaccineRecord(UUID.randomUUID().toString(), "PAT-001", "ဦးအောင်နိုင်", "Hepatitis B", 2, "2026-09-15", "2026-10-15", "HEP-091", "Nurse Win")
  )

  fun getSeedSyncLogs(): List<SyncLog> = listOf(
    SyncLog(UUID.randomUUID().toString(), "2026-10-17 09:00 AM", "App Initialized", "SUCCESS", "Local database ready")
  )
}
