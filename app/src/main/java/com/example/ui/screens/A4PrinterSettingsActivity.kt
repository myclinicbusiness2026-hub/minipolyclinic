package com.example.ui.screens

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.R
import com.example.model.ConsultationRecord
import com.example.model.Gender
import com.example.model.Patient
import com.example.model.PrescribedDrug
import com.example.model.ProcedureItem
import com.example.service.printing.A4PrinterSettingsStore
import com.example.ui.components.A4PageDescriptor
import com.example.ui.components.A4PrintSettings
import com.example.ui.components.triggerRealAndroidPrint

/**
 * A4PrinterSettingsActivity
 *
 * Dedicated configuration UI to manage, configure, and test A4 Medical Record & Prescription
 * print preferences (Margins, Paper size, Clinic Letterhead, Doctor SAMA seal, Grayscale ink-saver mode).
 */
class A4PrinterSettingsActivity : ComponentActivity() {

  private lateinit var settingsStore: A4PrinterSettingsStore

  private lateinit var spnPaperSize: Spinner
  private lateinit var spnMargins: Spinner
  private lateinit var spnOrientation: Spinner
  private lateinit var cbLetterhead: CheckBox
  private lateinit var cbStamp: CheckBox
  private lateinit var cbDemographics: CheckBox
  private lateinit var cbAnatomy: CheckBox
  private lateinit var cbGrayscale: CheckBox
  private lateinit var btnOpenSystemPrinters: Button
  private lateinit var btnSaveA4Settings: Button
  private lateinit var btnTestA4Print: Button
  private lateinit var tvA4Status: TextView

  private val paperSizes = listOf("A4 (210 x 297 mm)", "Letter (8.5 x 11 in)", "Legal (8.5 x 14 in)")
  private val marginOptions = listOf(
    "Narrow 10mm (စာပိုဆန့် စာမျက်နှာကျဉ်း)",
    "Normal 15mm (စံသတ်မှတ်ချက် - အကြံပြုပါသည်)",
    "Wide 20mm (စာအုပ်ချုပ်/ဖိုင်တွဲရန် ဘေးဘောင်ကျယ်)"
  )
  private val orientations = listOf("Portrait (ဒေါင်လိုက်)", "Landscape (အလျားလိုက်)")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_a4_printer_settings)

    settingsStore = A4PrinterSettingsStore(this)

    initViews()
    setupSpinners()
    loadSavedSettings()
    setupListeners()
  }

  private fun initViews() {
    spnPaperSize = findViewById(R.id.spnPaperSize)
    spnMargins = findViewById(R.id.spnMargins)
    spnOrientation = findViewById(R.id.spnOrientation)
    cbLetterhead = findViewById(R.id.cbLetterhead)
    cbStamp = findViewById(R.id.cbStamp)
    cbDemographics = findViewById(R.id.cbDemographics)
    cbAnatomy = findViewById(R.id.cbAnatomy)
    cbGrayscale = findViewById(R.id.cbGrayscale)
    btnOpenSystemPrinters = findViewById(R.id.btnOpenSystemPrinters)
    btnSaveA4Settings = findViewById(R.id.btnSaveA4Settings)
    btnTestA4Print = findViewById(R.id.btnTestA4Print)
    tvA4Status = findViewById(R.id.tvA4Status)
  }

  private fun setupSpinners() {
    val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paperSizes)
    spnPaperSize.adapter = sizeAdapter

    val marginAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, marginOptions)
    spnMargins.adapter = marginAdapter

    val orientAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, orientations)
    spnOrientation.adapter = orientAdapter
  }

  private fun loadSavedSettings() {
    // Paper Size
    val savedSize = settingsStore.getPaperSize()
    val sizeIdx = when (savedSize.uppercase()) {
      "LETTER" -> 1
      "LEGAL" -> 2
      else -> 0
    }
    spnPaperSize.setSelection(sizeIdx)

    // Margin Type
    val marginType = settingsStore.getMarginType().coerceIn(0, 2)
    spnMargins.setSelection(marginType)

    // Orientation
    val orientation = settingsStore.getOrientation()
    val orientIdx = if (orientation.equals("Landscape", ignoreCase = true)) 1 else 0
    spnOrientation.setSelection(orientIdx)

    // Checkboxes
    cbLetterhead.isChecked = settingsStore.isIncludeLetterhead()
    cbStamp.isChecked = settingsStore.isIncludeStamp()
    cbDemographics.isChecked = settingsStore.isIncludeDemographics()
    cbAnatomy.isChecked = settingsStore.isIncludeAnatomy()
    cbGrayscale.isChecked = settingsStore.isGrayscale()

    tvA4Status.text = "Current: $savedSize • Margin: ${settingsStore.getMarginMm().toInt()}mm • SAMA Stamp: ${if (settingsStore.isIncludeStamp()) "Active" else "Disabled"}"
  }

  private fun setupListeners() {
    btnOpenSystemPrinters.setOnClickListener {
      try {
        val intent = Intent(Settings.ACTION_PRINT_SETTINGS)
        startActivity(intent)
      } catch (e: Exception) {
        Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show()
        val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
        startActivity(fallbackIntent)
      }
    }

    btnSaveA4Settings.setOnClickListener {
      saveSettings()
    }

    btnTestA4Print.setOnClickListener {
      saveSettings()
      executeTestPrint()
    }
  }

  private fun saveSettings() {
    val selectedSize = when (spnPaperSize.selectedItemPosition) {
      1 -> "Letter"
      2 -> "Legal"
      else -> "A4"
    }
    val selectedMarginType = spnMargins.selectedItemPosition.coerceIn(0, 2)
    val selectedOrientation = if (spnOrientation.selectedItemPosition == 1) "Landscape" else "Portrait"

    settingsStore.setPaperSize(selectedSize)
    settingsStore.setMarginType(selectedMarginType)
    settingsStore.setOrientation(selectedOrientation)
    settingsStore.setIncludeLetterhead(cbLetterhead.isChecked)
    settingsStore.setIncludeStamp(cbStamp.isChecked)
    settingsStore.setIncludeDemographics(cbDemographics.isChecked)
    settingsStore.setIncludeAnatomy(cbAnatomy.isChecked)
    settingsStore.setGrayscale(cbGrayscale.isChecked)

    val marginMm = settingsStore.getMarginMm().toInt()
    tvA4Status.text = "Saved: $selectedSize • $selectedOrientation • ${marginMm}mm Margin"

    Toast.makeText(
      this,
      "A4 Print Preferences Saved Successfully!",
      Toast.LENGTH_SHORT
    ).show()
  }

  private fun executeTestPrint() {
    val samplePatient = Patient(
      id = "DEMO-001",
      hn = "HN-2026-0081",
      name = "ဦးအောင်မင်း (Aung Min)",
      gender = Gender.MALE,
      age = 45,
      phone = "09-450123456",
      address = "မန္တလေးမြို့",
      bloodType = "O+",
      allergies = listOf("None known"),
      queueNumber = 1
    )

    val samplePrescriptions = listOf(
      PrescribedDrug(
        medicineId = "MED-001",
        name = "Amoxicillin + Clavulanate (Augmentin) 625mg",
        dosage = "625 mg",
        frequency = "1-0-1 (မနက် ၁ လုံး၊ ည ၁ လုံး)",
        duration = "7 Days",
        quantity = 14,
        unitPrice = 1200.0,
        instructions = "အစာစားပြီး သောက်ရန်။ ရေများများသောက်ပါ။"
      ),
      PrescribedDrug(
        medicineId = "MED-002",
        name = "Paracetamol 500mg",
        dosage = "500 mg",
        frequency = "1-1-1 (မနက်/နေ့/ည ကိုက်ခဲလျှင်)",
        duration = "5 Days",
        quantity = 15,
        unitPrice = 150.0,
        instructions = "အဖျား သို့မဟုတ် နာကျင်ကိုက်ခဲချိန်တွင် သောက်ရန်။"
      )
    )

    val sampleConsultation = ConsultationRecord(
      id = "TEST-EMR-001",
      patientId = samplePatient.id,
      patientName = samplePatient.name,
      clinicName = "Shwe Khit Clinic",
      doctorName = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
      doctorSama = "47414",
      subjective = "Fever and productive cough for 3 days with mild sore throat (စမ်းသပ်ပုံနှိပ်ခြင်း A4 Verification)",
      objective = "Chest: Bilateral vesicular breath sounds, mild rhonchi right base. Throat: Congested posterior pharyngeal wall.",
      assessment = "Acute Bronchitis (J20.9)",
      plan = "Adequate oral hydration, steam inhalation, antibiotic course and symptomatic analgesics.",
      diagnosisList = listOf("Acute Bronchitis (J20.9)"),
      prescriptions = samplePrescriptions,
      drawingPages = emptyList(),
      selectedProcedures = listOf(
        ProcedureItem(
          code = "PR-01",
          name = "Nebulizer Therapy (Salbutamol)",
          category = "Respiratory",
          standardFee = 5000.0
        )
      ),
      suggestedInvestigations = listOf("Complete Blood Count (CBC) with ESR")
    )

    val printSettings = A4PrintSettings(
      clinicNameEn = "Shwe Khit General Clinic",
      clinicNameMm = "“ရွှေခေတ်” အထွေထွေရောဂါကုဆေးခန်း",
      clinicAddress = "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့",
      clinicPhone = "09-421001122, 09-790112233",
      doctorName = "Dr. Nyan Kaung Sett",
      doctorDegree = "M.B.,B.S (MDY)",
      doctorSama = "47414"
    )

    val pageDescriptors = listOf(A4PageDescriptor.UnifiedSinglePage())

    tvA4Status.text = "Launching Android Print Spooler for A4 Test Page..."

    triggerRealAndroidPrint(
      context = this,
      consultation = sampleConsultation,
      patient = samplePatient,
      printSettings = printSettings,
      pageDescriptors = pageDescriptors,
      marginMm = settingsStore.getMarginMm(),
      isGrayscale = settingsStore.isGrayscale()
    )
  }
}
