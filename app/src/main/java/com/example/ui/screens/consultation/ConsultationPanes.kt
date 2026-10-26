package com.example.ui.screens.consultation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.ClinicalDiagnosisSection
import com.example.ui.screens.treatment.TreatmentCard
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
  onOpenFullscreenCanvas: () -> Unit
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
        }
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

    // 4. Diagnostic Scans & Attachments
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
