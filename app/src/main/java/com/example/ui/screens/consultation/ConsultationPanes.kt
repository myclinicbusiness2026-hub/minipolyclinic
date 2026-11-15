package com.example.ui.screens.consultation

import androidx.compose.animation.animateContentSize

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.ClinicalDiagnosisSection
import com.example.ui.screens.treatment.TreatmentCard
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat

@Composable
fun ConsultationClinicalRecordPane(
  patient: Patient,
  viewModel: ClinicViewModel,
  activeComplaint: String,
  // Vitals
  isEditingVitals: Boolean,
  onToggleEditVitals: () -> Unit,
  onSaveVitals: () -> Unit,
  bpText: String,
  onBpChange: (String) -> Unit,
  pulseText: String,
  onPulseChange: (String) -> Unit,
  tempText: String,
  onTempChange: (String) -> Unit,
  spo2Text: String,
  onSpo2Change: (String) -> Unit,
  weightText: String,
  onWeightChange: (String) -> Unit,
  rbsText: String,
  onRbsChange: (String) -> Unit,
  onOpenTrends: () -> Unit,
  onOpenVaccines: () -> Unit,
  onOpenAttachments: () -> Unit,
  attachmentsState: SnapshotStateList<MedicalAttachment>,
  // Past records
  pastConsultations: List<ConsultationRecord>,
  prescribedDrugs: SnapshotStateList<PrescribedDrug>,
  selectedDiagnoses: SnapshotStateList<String>,
  selectedProcedures: SnapshotStateList<ProcedureItem>,
  selectedInvestigations: SnapshotStateList<String>,
  onUpdateAssessment: (String) -> Unit,
  onUpdatePlan: (String) -> Unit,
  onUpdateObjective: (String) -> Unit,
  // Clinical Notes & Canvas
  subjective: String,
  onSubjectiveChange: (String) -> Unit,
  anatomyStrokes: SnapshotStateList<DrawStroke>,
  drawingPagesState: SnapshotStateList<DrawingPage>,
  anatomyPageCount: Int,
  objective: String,
  canvasNotesState: String,
  onOpenFullscreenCanvas: () -> Unit,
  onEditRecord: ((ConsultationRecord) -> Unit)? = null,
  isReadOnly: Boolean = false
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // 1. Patient Banner Card with Allergies Alert
    PatientBannerCard(
      patient = patient,
      onSaveVitals = { if (!isReadOnly) onSaveVitals() },
      isEditingVitals = isEditingVitals && !isReadOnly,
      onToggleEditVitals = { if (!isReadOnly) onToggleEditVitals() },
      bpText = bpText,
      onBpChange = onBpChange,
      pulseText = pulseText,
      onPulseChange = onPulseChange,
      tempText = tempText,
      onTempChange = onTempChange,
      spo2Text = spo2Text,
      onSpo2Change = onSpo2Change,
      weightText = weightText,
      onWeightChange = onWeightChange,
      rbsText = rbsText,
      onRbsChange = onRbsChange,
      onOpenTrends = onOpenTrends,
      onOpenVaccines = onOpenVaccines,
      onOpenAttachments = onOpenAttachments,
      attachmentsCount = attachmentsState.size
    )

    // 1.5. Past Consultation History (EMR) for Follow-up & 2nd Visit Patients
    if (pastConsultations.isNotEmpty() || patient.isFollowUp) {
      PastConsultationHistoryCard(
        pastRecords = pastConsultations,
        onRepeatPrescriptions = { pastDrugs ->
          prescribedDrugs.clear()
          prescribedDrugs.addAll(pastDrugs)
        },
        onAddPrescription = { drug ->
          if (prescribedDrugs.none { it.name.equals(drug.name, ignoreCase = true) }) {
            prescribedDrugs.add(drug)
          }
        },
        onCopySoap = { record ->
          if (record.diagnosisList.isNotEmpty()) {
            selectedDiagnoses.clear()
            selectedDiagnoses.addAll(record.diagnosisList)
          }
          if (record.assessment.isNotBlank()) onUpdateAssessment(record.assessment)
          if (record.plan.isNotBlank()) onUpdatePlan(record.plan)
          val pastNotes = record.objective.ifBlank { record.canvasNotes }
          if (pastNotes.isNotBlank()) {
            onUpdateObjective(pastNotes)
          }
        },
        onCopyComplaint = {
          onSubjectiveChange(it)
        },
        onCopyClinicalNotes = {
          if (objective.isBlank()) {
            onUpdateObjective(it)
          } else {
            onUpdateObjective("$objective\n$it")
          }
        },
        onCopyDrawingStrokes = { strokes ->
          anatomyStrokes.clear()
          anatomyStrokes.addAll(strokes)
          if (drawingPagesState.isNotEmpty()) {
            drawingPagesState[0] = drawingPagesState[0].copy(strokes = anatomyStrokes.toMutableList())
          } else {
            drawingPagesState.add(DrawingPage(pageIndex = 0, strokes = anatomyStrokes.toMutableList()))
          }
        },
        onAddDiagnosis = { diag ->
          if (selectedDiagnoses.none { it.equals(diag, ignoreCase = true) }) {
            selectedDiagnoses.add(diag)
          }
        },
        onAddAllDiagnoses = { diags ->
          diags.forEach { diag ->
            if (selectedDiagnoses.none { it.equals(diag, ignoreCase = true) }) {
              selectedDiagnoses.add(diag)
            }
          }
        },
        onAddProcedure = { proc ->
          if (selectedProcedures.none { it.id == proc.id || (it.name.equals(proc.name, ignoreCase = true) && it.dateTime == proc.dateTime) }) {
            selectedProcedures.add(proc)
          }
        },
        onCopyPlan = {
          onUpdatePlan(it)
        },
        onCopyInvestigations = { invs ->
          invs.forEach { inv ->
            if (selectedInvestigations.none { it.equals(inv, ignoreCase = true) }) {
              selectedInvestigations.add(inv)
            }
          }
        },
        onViewA4Record = { record ->
          viewModel.showA4DocPreview(record)
        },
        onDeleteRecord = { recId ->
          viewModel.deleteConsultation(recId)
        },
        onEditRecord = onEditRecord
      )
    }

    // 2. Clinical Notes (Smart History Chips, Interactive Body Map & Stylus Canvas)
    ClinicalNotesCard(
      patientId = patient.id,
      patientChiefComplaint = activeComplaint,
      subjectiveNotes = subjective,
      onSubjectiveNotesChange = onSubjectiveChange,
      strokes = anatomyStrokes,
      typedNotes = if (objective.isNotBlank()) objective else canvasNotesState,
      onTypedNotesChange = onUpdateObjective,
      pageCount = if (drawingPagesState.isNotEmpty()) drawingPagesState.size else anatomyPageCount,
      onOpenFullscreenCanvas = onOpenFullscreenCanvas,
      onStrokesUpdated = {
        if (drawingPagesState.isNotEmpty()) {
          drawingPagesState[0] = drawingPagesState[0].copy(strokes = anatomyStrokes.toMutableList())
        } else {
          drawingPagesState.add(DrawingPage(pageIndex = 0, strokes = anatomyStrokes.toMutableList()))
        }
      }
    )

    // 3. Clinical Diagnosis
    ClinicalDiagnosisSection(
      selectedDiagnoses = selectedDiagnoses
    )

    // 4. Suggested Diagnostic Investigations (Labs, Imaging & Tests)
    val labTemplates by viewModel.labTemplates.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    SuggestedInvestigationsCard(
      selectedInvestigations = selectedInvestigations,
      labTemplates = labTemplates,
      onOrderToLab = { templates ->
        viewModel.orderLabTestsForPatient(
          patient = patient,
          selectedTemplates = templates,
          doctorName = viewModel.currentUser.value?.displayName ?: "Attending Doctor",
          clinicalNotes = "OPD Doctor Suggested Investigation"
        )
        android.widget.Toast.makeText(context, "ဓာတ်ခွဲခန်းသို့ တိုက်ရိုက်ပေးပို့ပြီးပါပြီ (Sent to Lab Queue)", android.widget.Toast.LENGTH_SHORT).show()
      }
    )

    // 5. Diagnostic Scans & Attachments
    MedicalAttachmentsCard(
      attachments = attachmentsState.toList(),
      onOpenAttachments = onOpenAttachments
    )
  }
}

@Composable
fun ConsultationPrescriptionAndBillingPane(
  prescribedDrugs: SnapshotStateList<PrescribedDrug>,
  onOpenAddDrug: () -> Unit,
  orderedTreatmentCards: SnapshotStateList<TreatmentCard>,
  onOpenAddTreatmentOrder: () -> Unit,
  onEditTreatmentOrder: ((Int) -> Unit)? = null,
  procedures: List<ProcedureItem>,
  selectedProcedures: SnapshotStateList<ProcedureItem>,
  medicines: List<Medicine>,
  onOpenCustomProcedure: () -> Unit,
  consultationFeeText: String,
  onConsultationFeeChange: (String) -> Unit,
  serviceFeeText: String,
  onServiceFeeChange: (String) -> Unit,
  followUpAppointmentText: String,
  onFollowUpAppointmentChange: (String) -> Unit,
  medicalLeaveState: MedicalLeaveRecord?,
  onOpenMedicalLeave: () -> Unit,
  referralState: ReferralRecord?,
  onOpenReferral: () -> Unit,
  patientInstructionsState: String,
  onPatientInstructionsChange: (String) -> Unit,
  onOpenInstructions: () -> Unit,
  onPreviewA4Doc: () -> Unit,
  onSendToPharmacy: () -> Unit,
  currencyFmt: NumberFormat,
  isReadOnly: Boolean = false
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // 1. Day-Care Injections & Treatment Order Card (Clinical Treatment & Injections)
    TreatmentOrderCard(
      treatmentOrders = orderedTreatmentCards,
      onOpenAddOrder = { if (!isReadOnly) onOpenAddTreatmentOrder() },
      onRemoveOrder = { index -> if (!isReadOnly) orderedTreatmentCards.removeAt(index) },
      onEditOrder = { index -> if (!isReadOnly) onEditTreatmentOrder?.invoke(index) },
      onTogglePaymentMode = { index ->
        if (!isReadOnly) {
          val c = orderedTreatmentCards[index]
          val newPayPerVisit = !c.isPayPerVisit
          orderedTreatmentCards[index] = c.copy(
            isPayPerVisit = newPayPerVisit,
            isPrepaidFullCourse = !newPayPerVisit,
            sessions = c.sessions.map { s -> if (!newPayPerVisit) s.copy(isPrepaid = true) else s.copy(isPrepaid = false) }
          )
        }
      },
      currencyFmt = currencyFmt
    )

    // 2. Prescription Builder & FEFO Search (Oral Take-Home Prescriptions)
    PrescriptionBuilderCard(
      prescribedDrugs = prescribedDrugs,
      onOpenAddDrug = { if (!isReadOnly) onOpenAddDrug() },
      onRemoveDrug = { index -> if (!isReadOnly) prescribedDrugs.removeAt(index) },
      currencyFmt = currencyFmt,
      onAddCstDrug = if (!isReadOnly) {
        {
          val existingIdx = prescribedDrugs.indexOfFirst {
            it.isCst || it.medicineId == "EXT-CST" || it.name.startsWith("CST") || it.name.contains("သောက်နေကြဆေး") || it.instructions.contains("သောက်လက်စ")
          }
          if (existingIdx >= 0) {
            prescribedDrugs.removeAt(existingIdx)
          } else {
            prescribedDrugs.add(
              com.example.model.PrescribedDrug(
                medicineId = "EXT-CST",
                medicineName = "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)",
                name = "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)",
                genericName = "",
                dosage = "",
                frequency = "",
                duration = "",
                quantity = 0,
                unitPrice = 0.0,
                instructions = "",
                isExternal = true,
                isCst = true
              )
            )
          }
        }
      } else null
    )

    // 3. 1-Tap Procedure Presets & Custom Procedure Builder
    ProcedureManagementCard(
      procedures = procedures,
      selectedProcedures = selectedProcedures,
      medicines = medicines,
      onOpenCustomProcedure = { if (!isReadOnly) onOpenCustomProcedure() },
      currencyFmt = currencyFmt
    )

    // 3. Consultation Fee & Complete Action Card
    ConsultationBillingActionCard(
      consultationFeeText = consultationFeeText,
      onConsultationFeeChange = onConsultationFeeChange,
      serviceFeeText = serviceFeeText,
      onServiceFeeChange = onServiceFeeChange,
      followUpAppointmentText = followUpAppointmentText,
      onFollowUpAppointmentChange = onFollowUpAppointmentChange,
      medicalLeaveState = medicalLeaveState,
      onOpenMedicalLeave = onOpenMedicalLeave,
      referralState = referralState,
      onOpenReferral = onOpenReferral,
      patientInstructionsState = patientInstructionsState,
      onPatientInstructionsChange = onPatientInstructionsChange,
      onOpenInstructions = onOpenInstructions,
      onPreviewA4Pdf = onPreviewA4Doc,
      onSendToPharmacy = onSendToPharmacy,
      isReadOnly = isReadOnly
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestedInvestigationsCard(
  selectedInvestigations: SnapshotStateList<String>,
  labTemplates: List<LabTestTemplate> = emptyList(),
  onOrderToLab: ((List<LabTestTemplate>) -> Unit)? = null
) {
  var customInvestigationText by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("ALL") }
  val currencyFmt = remember { NumberFormat.getNumberInstance(java.util.Locale.US) }

  // Non-lab Imaging & Outside Scans
  val imagingTests = remember {
    listOf(
      "Chest X-Ray", "ECG", "USG Whole Abdomen", "USG KUB",
      "Echocardiogram", "X-Ray Spine", "CT Brain", "MRI Brain"
    )
  }

  // Pre-configured Categories
  val categories = remember(labTemplates) {
    val cats = mutableListOf(
      "ALL" to "အားလုံး (All)",
      "Hematology" to "သွေးစစ် (Hematology)",
      "Biochemistry" to "ဇီဝဓာတု (Biochemistry)",
      "Urine/Stool" to "ဆီးနှင့်ဝမ်း (Urine/Stool)",
      "Serology" to "ပိုးစစ် (Serology / Rapid)",
      "Imaging" to "ဓာတ်မှန် / Scan (Imaging)",
      "Others" to "အခြား (Others)"
    )
    cats
  }

  // Filtered Test Items for current category
  val displayedItems: List<Pair<String, Double>> = remember(selectedCategory, labTemplates) {
    when (selectedCategory) {
      "Imaging" -> imagingTests.map { it to 0.0 }
      "ALL" -> {
        val labItems = labTemplates.map { it.name to it.defaultPrice }
        val nonLabItems = imagingTests
          .filterNot { img -> labTemplates.any { it.name.equals(img, ignoreCase = true) } }
          .map { it to 0.0 }
        labItems + nonLabItems
      }
      "Urine/Stool" -> {
        labTemplates
          .filter { it.category.contains("Urine", ignoreCase = true) || it.category.contains("Stool", ignoreCase = true) || it.name.contains("Urine", ignoreCase = true) || it.name.contains("Stool", ignoreCase = true) }
          .map { it.name to it.defaultPrice }
      }
      else -> {
        labTemplates
          .filter { it.category.equals(selectedCategory, ignoreCase = true) }
          .map { it.name to it.defaultPrice }
      }
    }
  }

  // Search suggestions
  val searchSuggestions = remember(customInvestigationText, labTemplates) {
    val q = customInvestigationText.trim()
    if (q.length < 2) emptyList()
    else {
      val matches = mutableListOf<Pair<String, Double>>()
      labTemplates.forEach { t ->
        if (t.name.contains(q, ignoreCase = true)) {
          matches.add(t.name to t.defaultPrice)
        }
      }
      imagingTests.forEach { img ->
        if (img.contains(q, ignoreCase = true) && matches.none { it.first.equals(img, ignoreCase = true) }) {
          matches.add(img to 0.0)
        }
      }
      matches.take(6)
    }
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGray),
    modifier = Modifier.fillMaxWidth().animateContentSize()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "စစ်ဆေးစေလိုသော စမ်းသပ်ချက်များ (Suggested Investigations / Labs)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SlateDark
          )
        }
        if (selectedInvestigations.isNotEmpty()) {
          TextButton(
            onClick = { selectedInvestigations.clear() },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text("Clear All", fontSize = 10.5.sp, color = Color(0xFFDC2626))
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Category Tabs / Selector
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        categories.forEach { (catKey, catLabel) ->
          val isCatSelected = selectedCategory == catKey
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isCatSelected) Color(0xFFE0F2FE) else PureWhite,
            border = BorderStroke(1.dp, if (isCatSelected) Color(0xFF0284C7) else BorderGray),
            modifier = Modifier.clickable { selectedCategory = catKey }
          ) {
            Text(
              catLabel,
              fontSize = 9.sp,
              fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isCatSelected) Color(0xFF0369A1) else DarkGrayText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Test Chips from Clinic Lab Catalog & Imaging
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        displayedItems.forEach { (testName, price) ->
          val isSelected = selectedInvestigations.any { it.equals(testName, ignoreCase = true) }
          val priceBadge = if (price > 0.0) " (${currencyFmt.format(price.toInt())} Ks)" else ""
          FilterChip(
            selected = isSelected,
            onClick = {
              if (isSelected) {
                selectedInvestigations.removeAll { it.equals(testName, ignoreCase = true) }
              } else {
                // Store clean test name only (prices never saved into text or printed on A4)
                selectedInvestigations.add(testName)
              }
            },
            label = {
              Text(
                "$testName$priceBadge",
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF0284C7),
              selectedLabelColor = PureWhite,
              containerColor = Color(0xFFF8FAFC),
              labelColor = DarkGrayText
            ),
            modifier = Modifier.height(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Custom Input Row & Search
      Row(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = customInvestigationText,
          onValueChange = { customInvestigationText = it },
          placeholder = { Text("အခြားစစ်ဆေးလိုသည့် Lab/X-Ray (e.g. HbA1c, USG, CT)...", fontSize = 10.5.sp, color = SlateMuted) },
          modifier = Modifier.weight(1f),
          textStyle = TextStyle(fontSize = 11.sp, color = DarkGrayText),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = {
            val clean = customInvestigationText.trim()
            if (clean.isNotBlank() && selectedInvestigations.none { it.equals(clean, ignoreCase = true) }) {
              selectedInvestigations.add(clean)
              customInvestigationText = ""
            }
          })
        )

        Button(
          onClick = {
            val clean = customInvestigationText.trim()
            if (clean.isNotBlank() && selectedInvestigations.none { it.equals(clean, ignoreCase = true) }) {
              selectedInvestigations.add(clean)
              customInvestigationText = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.height(42.dp),
          enabled = customInvestigationText.isNotBlank()
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Quick Search Suggestions
      if (searchSuggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          searchSuggestions.forEach { (sugName, price) ->
            val pStr = if (price > 0.0) " (${currencyFmt.format(price.toInt())} Ks)" else ""
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFF0FDF4),
              border = BorderStroke(1.dp, Color(0xFF86EFAC)),
              modifier = Modifier.clickable {
                if (selectedInvestigations.none { it.equals(sugName, ignoreCase = true) }) {
                  selectedInvestigations.add(sugName)
                }
                customInvestigationText = ""
              }
            ) {
              Text(
                "+ $sugName$pStr",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF15803D),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }
      }

      // Selected list preview (Doctor Screen shows price badge for in-house tests)
      if (selectedInvestigations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("ရွေးချယ်ထားသော စစ်ဆေးချက်များ (Selected):", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = SlateMedium)
        Spacer(modifier = Modifier.height(3.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          selectedInvestigations.forEach { inv ->
            val matchedTmpl = labTemplates.find { it.name.equals(inv, ignoreCase = true) }
            val priceBadge = if (matchedTmpl != null && matchedTmpl.defaultPrice > 0.0) {
              " • ${currencyFmt.format(matchedTmpl.defaultPrice.toInt())} Ks"
            } else ""

            InputChip(
              selected = true,
              onClick = { selectedInvestigations.remove(inv) },
              label = {
                Text(
                  "$inv$priceBadge",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              },
              trailingIcon = {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
              },
              colors = InputChipDefaults.inputChipColors(
                selectedContainerColor = if (matchedTmpl != null) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                selectedLabelColor = if (matchedTmpl != null) Color(0xFF0369A1) else Color(0xFF334155)
              ),
              modifier = Modifier.height(26.dp)
            )
          }
        }

        // Direct Order to Lab Queue (Only filters actual in-house lab templates)
        val matchedTemplates = remember(selectedInvestigations.toList(), labTemplates) {
          labTemplates.filter { t ->
            selectedInvestigations.any { inv ->
              t.name.equals(inv, ignoreCase = true) ||
              inv.contains(t.name, ignoreCase = true) ||
              t.name.contains(inv, ignoreCase = true) ||
              (inv.equals("CBC", ignoreCase = true) && t.name.contains("CBC", ignoreCase = true)) ||
              (inv.equals("Urine RE", ignoreCase = true) && t.name.contains("Urine", ignoreCase = true)) ||
              (inv.equals("LFT", ignoreCase = true) && t.name.contains("Liver", ignoreCase = true)) ||
              (inv.equals("RBS", ignoreCase = true) && (t.name.contains("Glucose", ignoreCase = true) || t.name.contains("RBS", ignoreCase = true))) ||
              (inv.contains("Lipid", ignoreCase = true) && t.name.contains("Lipid", ignoreCase = true)) ||
              (inv.contains("Electrolyte", ignoreCase = true) && t.name.contains("Electrolyte", ignoreCase = true))
            }
          }
        }

        val unlistedTests = remember(selectedInvestigations.toList(), matchedTemplates) {
          selectedInvestigations.filterNot { inv ->
            matchedTemplates.any { it.name.equals(inv, ignoreCase = true) || inv.contains(it.name, ignoreCase = true) || it.name.contains(inv, ignoreCase = true) }
          }
        }

        if (matchedTemplates.isNotEmpty() && onOrderToLab != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth()
            ) {
              val labTotal = matchedTemplates.sumOf { it.defaultPrice }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "🧪 ဆေးခန်း Lab တိုက်ရိုက်စစ်ဆေးမည့်စာရင်း (${matchedTemplates.size} ခု)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF166534)
                )
                if (labTotal > 0.0) {
                  Text(
                    "စုစုပေါင်း: ${currencyFmt.format(labTotal.toInt())} Ks",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                matchedTemplates.joinToString(", ") { "${it.name} (${currencyFmt.format(it.defaultPrice.toInt())} Ks)" },
                fontSize = 9.5.sp,
                color = Color(0xFF15803D)
              )

              if (unlistedTests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  "📷 ပြင်ပ/ဓာတ်မှန် စစ်ဆေးချက်များ: ${unlistedTests.joinToString(", ")} (ဆေးညွှန်း A4 တွင်သာ ဖော်ပြမည်၊ Lab Invoice တွင် မပါပါ)",
                  fontSize = 9.sp,
                  color = Color(0xFF64748B)
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              Button(
                onClick = { onOrderToLab(matchedTemplates) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth().height(32.dp)
              ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ဓာတ်ခွဲခန်းသို့ တိုက်ရိုက် Order ပို့မည် (Send to Lab Queue)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        } else if (unlistedTests.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "ℹ️ ရွေးချယ်ထားသော စစ်ဆေးချက်များ (${unlistedTests.joinToString(", ")}) သည် ပြင်ပ/ဓာတ်မှန် စစ်ဆေးချက်များဖြစ်သဖြင့် ဆေးညွှန်း A4 ပေါ်တွင်သာ ဖော်ပြပါမည်။",
            fontSize = 9.sp,
            color = Color(0xFF64748B)
          )
        }
      }
    }
  }
}
