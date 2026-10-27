package com.example.ui.screens.consultation

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
  onEditRecord: ((ConsultationRecord) -> Unit)? = null
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // 1. Patient Banner Card with Allergies Alert
    PatientBannerCard(
      patient = patient,
      onSaveVitals = onSaveVitals,
      isEditingVitals = isEditingVitals,
      onToggleEditVitals = onToggleEditVitals,
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
    SuggestedInvestigationsCard(
      selectedInvestigations = selectedInvestigations
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
  currencyFmt: NumberFormat
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    // 1. Prescription Builder & FEFO Search
    PrescriptionBuilderCard(
      prescribedDrugs = prescribedDrugs,
      onOpenAddDrug = onOpenAddDrug,
      onRemoveDrug = { index -> prescribedDrugs.removeAt(index) },
      currencyFmt = currencyFmt
    )

    // 1.5. Day-Care Injections & Treatment Order Card
    TreatmentOrderCard(
      treatmentOrders = orderedTreatmentCards,
      onOpenAddOrder = onOpenAddTreatmentOrder,
      onRemoveOrder = { index -> orderedTreatmentCards.removeAt(index) },
      currencyFmt = currencyFmt
    )

    // 2. 1-Tap Procedure Presets & Custom Procedure Builder
    ProcedureManagementCard(
      procedures = procedures,
      selectedProcedures = selectedProcedures,
      medicines = medicines,
      onOpenCustomProcedure = onOpenCustomProcedure,
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
      onSendToPharmacy = onSendToPharmacy
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestedInvestigationsCard(
  selectedInvestigations: SnapshotStateList<String>
) {
  var customInvestigationText by remember { mutableStateOf("") }
  val commonInvestigations = remember {
    listOf(
      "CBC", "RBS", "FBS", "Urine RE", "Stool RE",
      "Lipid Profile", "LFT", "RFT (Urea / Cr)", "Serum Electrolytes", "Serum Uric Acid",
      "Chest X-Ray", "ECG", "USG Whole Abdomen", "USG KUB",
      "Dengue NS1 / IgM", "Malaria RDT", "H. Pylori Ag", "Widal / Typhidot", "Thyroid Profile (TSH)"
    )
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGray),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
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

      // Quick Chips
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        commonInvestigations.forEach { testName ->
          val isSelected = selectedInvestigations.any { it.equals(testName, ignoreCase = true) }
          FilterChip(
            selected = isSelected,
            onClick = {
              if (isSelected) {
                selectedInvestigations.removeAll { it.equals(testName, ignoreCase = true) }
              } else {
                selectedInvestigations.add(testName)
              }
            },
            label = { Text(testName, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF0284C7),
              selectedLabelColor = PureWhite
            ),
            modifier = Modifier.height(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Custom Input Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = customInvestigationText,
          onValueChange = { customInvestigationText = it },
          placeholder = { Text("အခြားစစ်ဆေးလိုသည့် Lab/X-Ray (e.g. HbA1c, Serum Creatinine)...", fontSize = 10.5.sp, color = SlateMuted) },
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

      // Selected list preview
      if (selectedInvestigations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          selectedInvestigations.forEach { inv ->
            InputChip(
              selected = true,
              onClick = { selectedInvestigations.remove(inv) },
              label = { Text(inv, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
              trailingIcon = {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
              },
              colors = InputChipDefaults.inputChipColors(
                selectedContainerColor = Color(0xFFE0F2FE),
                selectedLabelColor = Color(0xFF0369A1)
              ),
              modifier = Modifier.height(26.dp)
            )
          }
        }
      }
    }
  }
}
