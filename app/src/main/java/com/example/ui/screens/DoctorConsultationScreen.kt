package com.example.ui.screens
import com.example.ui.screens.consultation.*
import com.example.ui.screens.treatment.*

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.A4AnatomyCanvasDialog
import com.example.ui.components.A4HandwritingNotesCanvasView
import com.example.ui.components.AllergyWarningBadge
import com.example.ui.components.ChronicDiseaseTrendsDialog
import com.example.ui.components.ClinicalDiagnosisSection
import com.example.ui.components.MedicalAttachmentsDialog
import com.example.ui.components.PatientSelectorDialog
import com.example.ui.components.ReferralFormModal
import com.example.ui.components.VaccinationTrackerDialog
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.horizontalScroll
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.foundation.BorderStroke

private fun calculateFollowUpDueDate(followUpText: String): String? {
  if (followUpText.isBlank() || followUpText.contains("PRN", ignoreCase = true) || followUpText.contains("မသက်သာမှ")) return null

  // 1. Check for explicit ISO Date pattern: [2026-10-25] or 2026-10-25
  val isoRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
  val isoMatch = isoRegex.find(followUpText)
  if (isoMatch != null) {
    return isoMatch.value
  }

  // 2. Check for Burmese/English DD/MM/YYYY pattern: e.g. 25/10/2026 or ၂၅/၁၀/၂၀၂၆
  val normalized = followUpText
    .replace('၀', '0').replace('၁', '1').replace('၂', '2').replace('၃', '3').replace('၄', '4')
    .replace('၅', '5').replace('၆', '6').replace('၇', '7').replace('၈', '8').replace('၉', '9')
  val dmyRegex = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{4})\b""")
  val dmyMatch = dmyRegex.find(normalized)
  if (dmyMatch != null) {
    val d = dmyMatch.groupValues[1].toIntOrNull() ?: 1
    val m = dmyMatch.groupValues[2].toIntOrNull() ?: 1
    val y = dmyMatch.groupValues[3].toIntOrNull() ?: 2026
    return String.format("%04d-%02d-%02d", y, m, d)
  }

  // 3. Duration keywords fallback
  val daysToAdd = when {
    followUpText.contains("3 Days", ignoreCase = true) || followUpText.contains("၃ ရက်") -> 3
    followUpText.contains("5 Days", ignoreCase = true) || followUpText.contains("၅ ရက်") -> 5
    followUpText.contains("1 Week", ignoreCase = true) || followUpText.contains("၁ ပတ်") -> 7
    followUpText.contains("2 Weeks", ignoreCase = true) || followUpText.contains("၂ ပတ်") -> 14
    followUpText.contains("3 Weeks", ignoreCase = true) || followUpText.contains("၃ ပတ်") -> 21
    followUpText.contains("1 Month", ignoreCase = true) || followUpText.contains("၁ လ") -> 30
    followUpText.contains("2 Months", ignoreCase = true) || followUpText.contains("၂ လ") -> 60
    followUpText.contains("3 Months", ignoreCase = true) || followUpText.contains("၃ လ") -> 90
    followUpText.contains("6 Months", ignoreCase = true) || followUpText.contains("၆ လ") -> 180
    followUpText.contains("1 Year", ignoreCase = true) || followUpText.contains("၁ နှစ်") -> 365
    else -> 7
  }
  val cal = java.util.Calendar.getInstance()
  cal.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
  return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorConsultationScreen(
  viewModel: ClinicViewModel,
  onConsultationCompleted: () -> Unit
) {
  val context = LocalContext.current
  val patients by viewModel.patients.collectAsState()
  val medicines by viewModel.medicines.collectAsState()
  val procedures by viewModel.procedures.collectAsState()
  val vaccineRecords by viewModel.vaccineRecords.collectAsState()
  val selectedPatientId by viewModel.selectedPatientId.collectAsState()
  val clinicSettings by viewModel.clinicSettings.collectAsState()
  val users by viewModel.users.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val selectedAttendingDoctor by viewModel.selectedAttendingDoctor.collectAsState()
  val customAttendingDoctor by viewModel.customAttendingDoctor.collectAsState()
  var showAttendingDoctorDialog by remember { mutableStateOf(false) }

  val effectiveDoctor = remember(selectedAttendingDoctor, customAttendingDoctor, currentUser, clinicSettings) {
    viewModel.getEffectiveDoctor()
  }

  val consultationQueuePatients = remember(patients) {
    patients.filter { it.status == QueueStatus.IN_CONSULTATION || it.status == QueueStatus.WAITING }
  }

  val activePatient = remember(consultationQueuePatients, selectedPatientId) {
    consultationQueuePatients.find { it.id == selectedPatientId } ?: consultationQueuePatients.firstOrNull()
  }

  val activeComplaint = remember(activePatient?.id, activePatient?.isFollowUp, activePatient?.followUpReason, activePatient?.chiefComplaint) {
    if (activePatient != null && activePatient.isFollowUp && activePatient.followUpReason.isNotBlank()) {
      activePatient.followUpReason
    } else {
      activePatient?.chiefComplaint ?: ""
    }
  }

  // Clinical Notes & SOAP State
  var subjective by remember(activePatient?.id) {
    val draft = activePatient?.id?.let { viewModel.getDraftConsultation(it) }
    val initialComplaint = if (draft != null && draft.subjective.isNotBlank()) {
      draft.subjective
    } else if (activePatient != null && activePatient.isFollowUp && activePatient.followUpReason.isNotBlank()) {
      "Follow-up visit: ${activePatient.followUpReason}"
    } else {
      activePatient?.chiefComplaint ?: ""
    }
    mutableStateOf(initialComplaint)
  }
  var objective by remember(activePatient?.id) {
    val draft = activePatient?.id?.let { viewModel.getDraftConsultation(it) }
    mutableStateOf(draft?.objective?.ifBlank { draft.canvasNotes } ?: "")
  }
  var assessment by remember(activePatient?.id) {
    val draft = activePatient?.id?.let { viewModel.getDraftConsultation(it) }
    mutableStateOf(draft?.assessment ?: "")
  }
  var plan by remember(activePatient?.id) {
    val draft = activePatient?.id?.let { viewModel.getDraftConsultation(it) }
    mutableStateOf(draft?.plan ?: "")
  }

  // Editable Vitals State for Active Consultation
  val patientVitals = activePatient?.vitals
  var bpText by remember(activePatient?.id) { mutableStateOf(patientVitals?.bloodPressure ?: "120/80 mmHg") }
  var pulseText by remember(activePatient?.id) { mutableStateOf(patientVitals?.pulseRate?.toString() ?: "76") }
  var tempText by remember(activePatient?.id) { 
    val t = patientVitals?.temperatureFahrenheit ?: 98.4
    mutableStateOf(String.format(java.util.Locale.US, "%.1f", t)) 
  }
  var spo2Text by remember(activePatient?.id) { mutableStateOf(patientVitals?.oxygenSaturation?.toString() ?: "98") }
  var weightText by remember(activePatient?.id) { mutableStateOf(patientVitals?.weightKg?.toString() ?: "62.5") }
  var rbsText by remember(activePatient?.id) { 
    val rVal = patientVitals?.rbs
    val rStr = if (rVal == null) "" else if (rVal % 1.0 == 0.0) rVal.toInt().toString() else rVal.toString()
    mutableStateOf(rStr) 
  }
  var isEditingVitals by remember(activePatient?.id) { mutableStateOf(false) }

  val selectedDiagnoses = remember(activePatient?.id) { mutableStateListOf<String>() }
  val prescribedDrugs = remember(activePatient?.id) { mutableStateListOf<PrescribedDrug>() }
  val selectedProcedures = remember(activePatient?.id) { mutableStateListOf<ProcedureItem>() }
  val selectedInvestigations = remember(activePatient?.id) { mutableStateListOf<String>() }
  val orderedTreatmentCards = remember(activePatient?.id) { mutableStateListOf<TreatmentCard>() }
  var showAddTreatmentOrderModal by remember { mutableStateOf(false) }
  var consultationFeeText by remember { mutableStateOf("8000") }
  var serviceFeeText by remember { mutableStateOf("0") }
  var followUpAppointmentText by remember(activePatient?.id) { mutableStateOf("") }

  val defaultInstructions = """၁။ ဆရာဝန်ညွှန်ကြားသည့်အတိုင်း ဆေးများကို အချိန်မှန်၊ ရက်ပြည့်အောင် သောက်သုံးပါ။ ရေများများသောက်ပါ။
၂။ ပဋိဇီဝဆေး (Antibiotics) များကို ရောဂါသက်သာသော်လည်း ရက်ပြည့်အောင် ဆက်သောက်ပါ။
၃။ ဆေးသောက်ပြီး ဓါတ်မတည့်ခြင်း (သို့မဟုတ်) ထူးခြားသော လက္ခဏာများ ခံစားရပါက ဆေးခန်းသို့ ချက်ချင်း ပြန်လည်ပြသပါ။"""

  var patientInstructionsState by remember(activePatient?.id) { mutableStateOf(defaultInstructions) }
  var medicalLeaveState by remember(activePatient?.id) { mutableStateOf<MedicalLeaveRecord?>(null) }
  var referralState by remember(activePatient?.id) { mutableStateOf<ReferralRecord?>(null) }
  var showMedicalLeaveModal by remember { mutableStateOf(false) }
  var showReferralModal by remember { mutableStateOf(false) }
  var showInstructionsModal by remember { mutableStateOf(false) }

  // Modals & Anatomy Canvas state
  var showAddDrugModal by remember { mutableStateOf(false) }
  var showDiscardConfirmation by remember { mutableStateOf(false) }
  var showCustomProcedureModal by remember { mutableStateOf(false) }
  var showScribbleExpanded by remember { mutableStateOf(false) }
  var showPatientSelectorDialog by remember { mutableStateOf(false) }
  val attachmentsState = remember(activePatient?.id) { mutableStateListOf<MedicalAttachment>() }
  var showAttachmentsModal by remember { mutableStateOf(false) }
  var showTrendsModal by remember { mutableStateOf(false) }
  var showVaccinesModal by remember { mutableStateOf(false) }
  var showConfirmSaveDialog by remember { mutableStateOf(false) }
  val anatomyStrokes = remember(activePatient?.id) { mutableStateListOf<DrawStroke>() }
  val drawingPagesState = remember(activePatient?.id) { mutableStateListOf<DrawingPage>() }
  var canvasNotesState by remember(activePatient?.id) { mutableStateOf("") }
  var anatomyPageCount by remember(activePatient?.id) { mutableStateOf(1) }
  var editingConsultation by remember { mutableStateOf<ConsultationRecord?>(null) }

  // Restore in-progress active draft for active patient if currently being edited today
  val draftConsultations by viewModel.draftConsultations.collectAsState()
  val consultations by viewModel.consultations.collectAsState()
  LaunchedEffect(activePatient?.id) {
    if (activePatient != null) {
      val draft = viewModel.getDraftConsultation(activePatient.id)
      if (draft != null) {
        if (draft.subjective.isNotBlank()) subjective = draft.subjective
        val restoredNotes = draft.objective.ifBlank { draft.canvasNotes }
        if (restoredNotes.isNotBlank()) {
          objective = restoredNotes
          canvasNotesState = restoredNotes
        }
        if (draft.assessment.isNotBlank()) assessment = draft.assessment
        if (draft.plan.isNotBlank()) plan = draft.plan
        if (draft.patientInstructions.isNotBlank()) patientInstructionsState = draft.patientInstructions
        if (draft.medicalLeave != null) medicalLeaveState = draft.medicalLeave
        if (draft.referralRecord != null) referralState = draft.referralRecord
        if (draft.followUpAppointment.isNotBlank()) followUpAppointmentText = draft.followUpAppointment
        if (draft.drawingPages.isNotEmpty()) {
          drawingPagesState.clear()
          drawingPagesState.addAll(draft.drawingPages)
          if (draft.drawingPages.first().strokes.isNotEmpty()) {
            anatomyStrokes.clear()
            anatomyStrokes.addAll(draft.drawingPages.first().strokes)
          }
        }
        if (draft.diagnosisList.isNotEmpty() && selectedDiagnoses.isEmpty()) {
          selectedDiagnoses.addAll(draft.diagnosisList)
        }
        if (draft.prescriptions.isNotEmpty() && prescribedDrugs.isEmpty()) {
          prescribedDrugs.addAll(draft.prescriptions)
        }
        if (draft.selectedProcedures.isNotEmpty() && selectedProcedures.isEmpty()) {
          selectedProcedures.addAll(draft.selectedProcedures)
        }
        if (draft.suggestedInvestigations.isNotEmpty() && selectedInvestigations.isEmpty()) {
          selectedInvestigations.addAll(draft.suggestedInvestigations)
        }
        if (draft.attachments.isNotEmpty() && attachmentsState.isEmpty()) {
          attachmentsState.addAll(draft.attachments)
        }
      }
      val pendingCards = viewModel.treatmentCards.value.filter {
        it.patientId == activePatient.id && !it.isFullyCompleted && !it.isDiscontinued && it.consultationInvoiceId.isNullOrBlank()
      }
      if (pendingCards.isNotEmpty() && orderedTreatmentCards.isEmpty()) {
        orderedTreatmentCards.addAll(pendingCards)
      }
    }
  }

  // Auto-save clinical notes draft whenever doctor enters notes, strokes or orders
  LaunchedEffect(
    subjective,
    objective,
    assessment,
    plan,
    canvasNotesState,
    selectedDiagnoses.size,
    prescribedDrugs.size,
    selectedProcedures.size,
    selectedInvestigations.size,
    drawingPagesState.size,
    followUpAppointmentText,
    patientInstructionsState,
    medicalLeaveState,
    referralState
  ) {
    if (activePatient != null && (activePatient.status == QueueStatus.WAITING || activePatient.status == QueueStatus.IN_CONSULTATION)) {
      val resolvedPages = if (drawingPagesState.isNotEmpty()) {
        drawingPagesState.toList()
      } else if (anatomyStrokes.isNotEmpty()) {
        listOf(DrawingPage(pageIndex = 0, strokes = anatomyStrokes.toMutableList()))
      } else {
        emptyList()
      }
      val currentNotes = if (objective.isNotBlank()) objective else canvasNotesState
      viewModel.saveDraftClinicalNotes(
        patientId = activePatient.id,
        patientName = activePatient.name,
        subjective = subjective,
        objective = currentNotes,
        assessment = assessment,
        plan = plan,
        diagnosisList = selectedDiagnoses.toList(),
        prescriptions = prescribedDrugs.toList(),
        selectedProcedures = selectedProcedures.toList(),
        suggestedInvestigations = selectedInvestigations.toList(),
        drawingPages = resolvedPages,
        canvasNotes = currentNotes,
        followUpAppointment = followUpAppointmentText,
        patientInstructions = patientInstructionsState,
        medicalLeave = medicalLeaveState,
        referralRecord = referralState,
        doctorNameOverride = effectiveDoctor.first,
        doctorDegreeOverride = effectiveDoctor.second,
        doctorSamaOverride = effectiveDoctor.third,
        treatmentOrders = orderedTreatmentCards.toList()
      )
    }
  }

  // Past consultations for 2nd visit / follow-up review
  val pastConsultations = remember(activePatient?.id, activePatient?.hn, consultations) {
    if (activePatient != null) {
      viewModel.getPatientConsultationHistory(activePatient.id, activePatient.hn)
    } else {
      emptyList()
    }
  }

  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  val doCompleteConsultation = {
    if (activePatient != null) {
      val resolvedPages = if (drawingPagesState.isNotEmpty()) {
        drawingPagesState.toList()
      } else if (anatomyStrokes.isNotEmpty()) {
        listOf(DrawingPage(pageIndex = 0, strokes = anatomyStrokes.toMutableList()))
      } else {
        emptyList()
      }

      val dueDate = calculateFollowUpDueDate(followUpAppointmentText)
      if (!dueDate.isNullOrBlank()) {
        viewModel.setPatientFollowUpDueDate(activePatient.id, dueDate, followUpAppointmentText)
      }

      val patientVaccines = viewModel.getVaccinesForPatient(activePatient.id)
      val upcomingVaccine = patientVaccines.lastOrNull { !it.nextDueDate.isNullOrBlank() }
      val nextVaccineScheduleText = upcomingVaccine?.let {
        com.example.util.BurmeseDateUtils.formatNextVaccineSchedule(it.vaccineName, it.doseNumber, it.nextDueDate)
      } ?: ""

      val currentNotes = if (objective.isNotBlank()) objective else canvasNotesState
      val currentVitals = VitalSigns(
        bloodPressure = bpText,
        pulseRate = pulseText.toIntOrNull() ?: activePatient.vitals.pulseRate,
        temperatureCelsius = tempText.toDoubleOrNull() ?: activePatient.vitals.temperatureCelsius,
        oxygenSaturation = spo2Text.toIntOrNull() ?: activePatient.vitals.oxygenSaturation,
        weightKg = weightText.toDoubleOrNull() ?: activePatient.vitals.weightKg,
        rbs = rbsText.toDoubleOrNull() ?: activePatient.vitals.rbs
      )

      // Collect all treatment orders (explicit + injectable Rx)
      val finalTreatmentCards = orderedTreatmentCards.toMutableList()
      if (finalTreatmentCards.isEmpty()) {
        prescribedDrugs.forEach { drug ->
          val isInjection = drug.effectiveName.contains("Inj", ignoreCase = true) ||
              drug.dosage.contains("Inj", ignoreCase = true) ||
              drug.medicineName.contains("Inj", ignoreCase = true) ||
              drug.instructions.contains("IM", ignoreCase = true) ||
              drug.instructions.contains("IV", ignoreCase = true) ||
              drug.instructions.contains("Drip", ignoreCase = true) ||
              drug.instructions.contains("Infusion", ignoreCase = true) ||
              drug.instructions.contains("Neb", ignoreCase = true)
          if (isInjection) {
            val route = when {
              drug.instructions.contains("Drip", ignoreCase = true) || drug.instructions.contains("Infusion", ignoreCase = true) -> TreatmentRoute.IV_INFUSION
              drug.instructions.contains("IV", ignoreCase = true) -> TreatmentRoute.IV_PUSH
              drug.instructions.contains("Neb", ignoreCase = true) -> TreatmentRoute.NEBULIZATION
              else -> TreatmentRoute.IM
            }
            val days = drug.duration.filter { it.isDigit() }.toIntOrNull() ?: 3
            val sessions = TreatmentCardFactory.generateSessions(
              startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
              frequency = drug.frequency.ifBlank { "12 hrly" },
              days = days,
              medicationFee = drug.unitPrice,
              sessionFee = route.getDefaultFee(clinicSettings)
            )
            val card = TreatmentCard(
              patientId = activePatient.id,
              patientName = activePatient.name,
              patientHn = activePatient.hn,
              doctorName = effectiveDoctor.first,
              medicationId = drug.medicineId,
              medicationName = drug.effectiveName.ifBlank { drug.name.ifBlank { drug.medicineName } },
              dosage = drug.dosage,
              route = route,
              frequency = drug.frequency.ifBlank { "12 hrly" },
              totalDays = days,
              totalSessions = sessions.size,
              sessions = sessions,
              sessionFee = route.getDefaultFee(clinicSettings),
              medicationFee = drug.unitPrice,
              instructions = drug.instructions,
              isPayPerVisit = true,
              isPrepaidFullCourse = false
            )
            finalTreatmentCards.add(card)
          }
        }
      }

      // Complete consultation with combined injection fees
      viewModel.completeConsultation(
        patientId = activePatient.id,
        subjective = subjective,
        objective = currentNotes,
        assessment = assessment,
        plan = plan,
        diagnosisList = selectedDiagnoses.toList(),
        prescriptions = prescribedDrugs.toList(),
        selectedProcedures = selectedProcedures.toList(),
        consultationFee = consultationFeeText.toDoubleOrNull() ?: 8000.0,
        serviceFee = serviceFeeText.toDoubleOrNull() ?: 0.0,
        suggestedInvestigations = selectedInvestigations.toList(),
        drawingPages = resolvedPages,
        canvasNotes = currentNotes,
        followUpAppointment = followUpAppointmentText,
        followUpDueDate = dueDate,
        nextVaccineSchedule = nextVaccineScheduleText,
        patientInstructions = patientInstructionsState,
        medicalLeave = medicalLeaveState,
        referralRecord = referralState,
        attachments = attachmentsState.toList(),
        vitals = currentVitals,
        doctorNameOverride = effectiveDoctor.first,
        doctorDegreeOverride = effectiveDoctor.second,
        doctorSamaOverride = effectiveDoctor.third,
        treatmentOrders = finalTreatmentCards
      )

      onConsultationCompleted()
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTabletLayout = maxWidth >= 760.dp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(BackgroundSurface)
        .padding(10.dp)
    ) {
      // Top Bar: Clean, Full-Width Patient Selector with Dropdown Picker & Quick Nav
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (activePatient != null) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Queue Number badge
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (activePatient.isFollowUp) Color(0xFFF3E8FF) else SoftGreenContainer)
                  .padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Text(
                  if (activePatient.isFollowUp) "#${activePatient.queueNumber} Follow-up" else "#${activePatient.queueNumber}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = if (activePatient.isFollowUp) Color(0xFF7E22CE) else MedicalGreenDark
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    activePatient.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DarkGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    "• ${activePatient.hn} • ${activePatient.age}y/${activePatient.gender.name.first()}",
                    fontSize = 10.5.sp,
                    color = SlateMuted,
                    maxLines = 1
                  )
                  if (activePatient.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                          try {
                            val cleanPhone = activePatient.phone.replace(Regex("[^0-9+]"), "")
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                          } catch (e: Exception) {
                            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                          }
                        }
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                    ) {
                      Icon(
                        Icons.Default.Phone,
                        contentDescription = "Direct Call ${activePatient.phone}",
                        tint = MedicalGreenDark,
                        modifier = Modifier.size(11.dp)
                      )
                      Spacer(modifier = Modifier.width(2.dp))
                      Text(
                        activePatient.phone,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MedicalGreenDark,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1
                      )
                    }
                  }
                }
                Text(
                  if (activePatient.isFollowUp && activePatient.followUpReason.isNotBlank())
                    "Follow-up: ${activePatient.followUpReason}"
                  else
                    "Chief Complaint: ${activePatient.chiefComplaint}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (activePatient.isFollowUp) Color(0xFF7E22CE) else MedicalGreenDark,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          } else {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.PersonOff, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Select Patient", fontSize = 11.5.sp, color = SlateMuted)
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Compact Navigation & Select Dialog Controls
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val activePatId = activePatient?.id
            val currentIndex = consultationQueuePatients.indexOfFirst { it.id == activePatId }

            // Prev Patient Button
            IconButton(
              onClick = {
                if (consultationQueuePatients.isNotEmpty()) {
                  val prevIdx = if (currentIndex <= 0) consultationQueuePatients.size - 1 else currentIndex - 1
                  val target = consultationQueuePatients[prevIdx]
                  viewModel.selectPatient(target.id)
                  viewModel.updatePatientStatus(target.id, QueueStatus.IN_CONSULTATION)
                }
              },
              enabled = consultationQueuePatients.size > 1,
              modifier = Modifier.size(26.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(14.dp), tint = if (consultationQueuePatients.size > 1) DarkGrayText else SlateLight)
            }

            // Compact Select Button
            OutlinedButton(
              onClick = { showPatientSelectorDialog = true },
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MedicalGreen)
            ) {
              Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(12.dp), tint = MedicalGreen)
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                "Queue (${if (currentIndex >= 0) currentIndex + 1 else 0}/${consultationQueuePatients.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreen,
                maxLines = 1
              )
            }

            // Next Patient Button
            IconButton(
              onClick = {
                if (consultationQueuePatients.isNotEmpty()) {
                  val nextIdx = if (currentIndex < 0 || currentIndex >= consultationQueuePatients.size - 1) 0 else currentIndex + 1
                  val target = consultationQueuePatients[nextIdx]
                  viewModel.selectPatient(target.id)
                  viewModel.updatePatientStatus(target.id, QueueStatus.IN_CONSULTATION)
                }
              },
              enabled = consultationQueuePatients.size > 1,
              modifier = Modifier.size(26.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(14.dp), tint = if (consultationQueuePatients.size > 1) DarkGrayText else SlateLight)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Attending Doctor Selector Chip
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { showAttendingDoctorDialog = true },
              color = SoftIceBlue,
              border = androidx.compose.foundation.BorderStroke(0.8.dp, SkyBluePrimary.copy(alpha = 0.6f))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.MedicalInformation, contentDescription = null, modifier = Modifier.size(12.dp), tint = SkyBluePrimary)
                Spacer(modifier = Modifier.width(3.dp))
                Column {
                  Text("ဆရာဝန် (Doctor)", fontSize = 7.5.sp, color = SlateMedium, lineHeight = 9.sp)
                  Text(
                    effectiveDoctor.first.replace("Dr. ", "").split(" ").take(2).joinToString(" "),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark,
                    maxLines = 1
                  )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp), tint = SkyBluePrimary)
              }
            }
          }
        }
      }

      // Quick Horizontal Queue Strip for 1-Tap Switching (Active Queue only)
      if (consultationQueuePatients.isNotEmpty()) {
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Queue:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
          val activePatId = activePatient?.id
          consultationQueuePatients.forEach { p ->
            val isActive = activePatId != null && p.id == activePatId
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                  viewModel.selectPatient(p.id)
                  viewModel.updatePatientStatus(p.id, QueueStatus.IN_CONSULTATION)
                },
              shape = RoundedCornerShape(4.dp),
              color = if (isActive) MedicalGreen else PureWhite,
              border = androidx.compose.foundation.BorderStroke(0.6.dp, if (isActive) MedicalGreen else BorderGray)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "#${p.queueNumber} ${p.name}",
                  fontSize = 10.sp,
                  fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                  color = if (isActive) PureWhite else DarkGrayText,
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      val handlePreviewA4Doc = {
        if (activePatient != null) {
          val resolvedPages = if (drawingPagesState.isNotEmpty()) {
            drawingPagesState.toList()
          } else if (anatomyStrokes.isNotEmpty()) {
            listOf(DrawingPage(pageIndex = 0, strokes = anatomyStrokes.toMutableList()))
          } else {
            emptyList()
          }

          val dueDate = calculateFollowUpDueDate(followUpAppointmentText)
          if (!dueDate.isNullOrBlank()) {
            viewModel.setPatientFollowUpDueDate(activePatient.id, dueDate, followUpAppointmentText)
          }

          val patientVaccines = viewModel.getVaccinesForPatient(activePatient.id)
          val upcomingVaccine = patientVaccines.lastOrNull { !it.nextDueDate.isNullOrBlank() }
          val nextVaccineScheduleText = upcomingVaccine?.let {
            com.example.util.BurmeseDateUtils.formatNextVaccineSchedule(it.vaccineName, it.doseNumber, it.nextDueDate)
          } ?: ""

          val currentNotes = if (objective.isNotBlank()) objective else canvasNotesState
          val currentVitals = VitalSigns(
            bloodPressure = bpText,
            pulseRate = pulseText.toIntOrNull() ?: activePatient.vitals.pulseRate,
            temperatureCelsius = tempText.toDoubleOrNull() ?: activePatient.vitals.temperatureCelsius,
            oxygenSaturation = spo2Text.toIntOrNull() ?: activePatient.vitals.oxygenSaturation,
            weightKg = weightText.toDoubleOrNull() ?: activePatient.vitals.weightKg,
            rbs = rbsText.toDoubleOrNull() ?: activePatient.vitals.rbs
          )

          val docFullName = if (effectiveDoctor.second.isNotBlank() && !effectiveDoctor.first.contains(effectiveDoctor.second, ignoreCase = true)) {
            "${effectiveDoctor.first} ${effectiveDoctor.second}".trim()
          } else effectiveDoctor.first

          val previewRecord = ConsultationRecord(
            patientId = activePatient.id,
            patientName = activePatient.name,
            doctorName = docFullName,
            doctorDegree = effectiveDoctor.second,
            doctorSama = effectiveDoctor.third,
            subjective = subjective,
            objective = currentNotes,
            assessment = assessment.ifBlank { selectedDiagnoses.joinToString(", ") },
            plan = plan,
            diagnosisList = if (selectedDiagnoses.isNotEmpty()) selectedDiagnoses.toList() else if (assessment.isNotBlank()) listOf(assessment) else emptyList(),
            prescriptions = prescribedDrugs.toList(),
            selectedProcedures = selectedProcedures.toList(),
            suggestedInvestigations = selectedInvestigations.toList(),
            consultationFee = consultationFeeText.toDoubleOrNull() ?: 8000.0,
            drawingPages = resolvedPages,
            canvasNotes = currentNotes,
            followUpAppointment = followUpAppointmentText,
            followUpDueDate = dueDate,
            nextVaccineSchedule = nextVaccineScheduleText,
            patientInstructions = patientInstructionsState,
            medicalLeave = medicalLeaveState,
            referralRecord = referralState,
            attachments = attachmentsState.toList(),
            vitals = currentVitals
          )
          viewModel.showA4DocPreview(previewRecord)
        }
      }

      val handleSendToPharmacy = {
        showConfirmSaveDialog = true
      }

      if (activePatient != null) {
        val patient = activePatient
        val onSaveVitalsAction = {
          val newVitals = VitalSigns(
            bloodPressure = bpText,
            pulseRate = pulseText.toIntOrNull() ?: patient.vitals.pulseRate,
            temperatureCelsius = tempText.toDoubleOrNull() ?: patient.vitals.temperatureCelsius,
            oxygenSaturation = spo2Text.toIntOrNull() ?: patient.vitals.oxygenSaturation,
            weightKg = weightText.toDoubleOrNull() ?: patient.vitals.weightKg,
            rbs = rbsText.toDoubleOrNull() ?: patient.vitals.rbs
          )
          viewModel.updatePatientVitals(patient.id, newVitals)
          isEditingVitals = false
        }

        if (isTabletLayout) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ConsultationClinicalRecordPane(
                patient = patient,
                viewModel = viewModel,
                activeComplaint = activeComplaint,
                isEditingVitals = isEditingVitals,
                onToggleEditVitals = { isEditingVitals = !isEditingVitals },
                onSaveVitals = onSaveVitalsAction,
                bpText = bpText,
                onBpChange = { bpText = it },
                pulseText = pulseText,
                onPulseChange = { pulseText = it },
                tempText = tempText,
                onTempChange = { tempText = it },
                spo2Text = spo2Text,
                onSpo2Change = { spo2Text = it },
                weightText = weightText,
                onWeightChange = { weightText = it },
                rbsText = rbsText,
                onRbsChange = { rbsText = it },
                onOpenTrends = { showTrendsModal = true },
                onOpenVaccines = { showVaccinesModal = true },
                onOpenAttachments = { showAttachmentsModal = true },
                attachmentsState = attachmentsState,
                pastConsultations = pastConsultations,
                prescribedDrugs = prescribedDrugs,
                selectedDiagnoses = selectedDiagnoses,
                selectedProcedures = selectedProcedures,
                selectedInvestigations = selectedInvestigations,
                onUpdateAssessment = { assessment = it },
                onUpdatePlan = { plan = it },
                onUpdateObjective = {
                  objective = it
                  canvasNotesState = it
                },
                subjective = subjective,
                onSubjectiveChange = { subjective = it },
                anatomyStrokes = anatomyStrokes,
                drawingPagesState = drawingPagesState,
                anatomyPageCount = anatomyPageCount,
                objective = objective,
                canvasNotesState = canvasNotesState,
                onOpenFullscreenCanvas = { showScribbleExpanded = true },
                onEditRecord = { record -> editingConsultation = record }
              )
            }

            Column(
              modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ConsultationPrescriptionAndBillingPane(
                prescribedDrugs = prescribedDrugs,
                onOpenAddDrug = { showAddDrugModal = true },
                orderedTreatmentCards = orderedTreatmentCards,
                onOpenAddTreatmentOrder = { showAddTreatmentOrderModal = true },
                procedures = procedures,
                selectedProcedures = selectedProcedures,
                medicines = medicines,
                onOpenCustomProcedure = { showCustomProcedureModal = true },
                consultationFeeText = consultationFeeText,
                onConsultationFeeChange = { consultationFeeText = it },
                serviceFeeText = serviceFeeText,
                onServiceFeeChange = { serviceFeeText = it },
                followUpAppointmentText = followUpAppointmentText,
                onFollowUpAppointmentChange = { followUpAppointmentText = it },
                medicalLeaveState = medicalLeaveState,
                onOpenMedicalLeave = { showMedicalLeaveModal = true },
                referralState = referralState,
                onOpenReferral = { showReferralModal = true },
                patientInstructionsState = patientInstructionsState,
                onPatientInstructionsChange = { patientInstructionsState = it },
                onOpenInstructions = { showInstructionsModal = true },
                onPreviewA4Doc = handlePreviewA4Doc,
                onSendToPharmacy = handleSendToPharmacy,
                currencyFmt = currencyFmt
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item {
              ConsultationClinicalRecordPane(
                patient = patient,
                viewModel = viewModel,
                activeComplaint = activeComplaint,
                isEditingVitals = isEditingVitals,
                onToggleEditVitals = { isEditingVitals = !isEditingVitals },
                onSaveVitals = onSaveVitalsAction,
                bpText = bpText,
                onBpChange = { bpText = it },
                pulseText = pulseText,
                onPulseChange = { pulseText = it },
                tempText = tempText,
                onTempChange = { tempText = it },
                spo2Text = spo2Text,
                onSpo2Change = { spo2Text = it },
                weightText = weightText,
                onWeightChange = { weightText = it },
                rbsText = rbsText,
                onRbsChange = { rbsText = it },
                onOpenTrends = { showTrendsModal = true },
                onOpenVaccines = { showVaccinesModal = true },
                onOpenAttachments = { showAttachmentsModal = true },
                attachmentsState = attachmentsState,
                pastConsultations = pastConsultations,
                prescribedDrugs = prescribedDrugs,
                selectedDiagnoses = selectedDiagnoses,
                selectedProcedures = selectedProcedures,
                selectedInvestigations = selectedInvestigations,
                onUpdateAssessment = { assessment = it },
                onUpdatePlan = { plan = it },
                onUpdateObjective = {
                  objective = it
                  canvasNotesState = it
                },
                subjective = subjective,
                onSubjectiveChange = { subjective = it },
                anatomyStrokes = anatomyStrokes,
                drawingPagesState = drawingPagesState,
                anatomyPageCount = anatomyPageCount,
                objective = objective,
                canvasNotesState = canvasNotesState,
                onOpenFullscreenCanvas = { showScribbleExpanded = true },
                onEditRecord = { record -> editingConsultation = record }
              )
            }

            item {
              ConsultationPrescriptionAndBillingPane(
                prescribedDrugs = prescribedDrugs,
                onOpenAddDrug = { showAddDrugModal = true },
                orderedTreatmentCards = orderedTreatmentCards,
                onOpenAddTreatmentOrder = { showAddTreatmentOrderModal = true },
                procedures = procedures,
                selectedProcedures = selectedProcedures,
                medicines = medicines,
                onOpenCustomProcedure = { showCustomProcedureModal = true },
                consultationFeeText = consultationFeeText,
                onConsultationFeeChange = { consultationFeeText = it },
                serviceFeeText = serviceFeeText,
                onServiceFeeChange = { serviceFeeText = it },
                followUpAppointmentText = followUpAppointmentText,
                onFollowUpAppointmentChange = { followUpAppointmentText = it },
                medicalLeaveState = medicalLeaveState,
                onOpenMedicalLeave = { showMedicalLeaveModal = true },
                referralState = referralState,
                onOpenReferral = { showReferralModal = true },
                patientInstructionsState = patientInstructionsState,
                onPatientInstructionsChange = { patientInstructionsState = it },
                onOpenInstructions = { showInstructionsModal = true },
                onPreviewA4Doc = handlePreviewA4Doc,
                onSendToPharmacy = handleSendToPharmacy,
                currencyFmt = currencyFmt
              )
            }
          }
        }
      } else {
        // No patient currently active in consultation queue
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              "ဆရာဝန် ပြသရန် စောင့်ဆိုင်းနေသော လူနာမရှိပါ",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = DarkGrayText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "လူနာသစ် (New Patient) သို့မဟုတ် ရက်ချိန်းပြန်ပြ (Follow-up) စာရင်းသွင်းပြီးမှသာ ဆရာဝန်ဆေးမှတ်တမ်း စတင်နိုင်ပါမည်။",
              fontSize = 11.sp,
              color = SlateMuted,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = { viewModel.setTab(1) },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("လူနာစာရင်းသို့ သွားရန် (Queue)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { viewModel.setTab(8) },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, SkyBluePrimary)
              ) {
                Icon(Icons.Default.FolderShared, contentDescription = null, tint = SkyBlueDark, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("ဆေးမှတ်တမ်းဟောင်းများ (EMR Archive)", fontSize = 11.sp, color = SkyBlueDark, fontWeight = FontWeight.Bold)
              }
            }

            if (consultationQueuePatients.isNotEmpty()) {
              Spacer(modifier = Modifier.height(16.dp))
              HorizontalDivider(color = BorderGrey, thickness = 0.6.dp)
              Spacer(modifier = Modifier.height(10.dp))
              Text("စောင့်ဆိုင်းနေသော လူနာများ (${consultationQueuePatients.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Spacer(modifier = Modifier.height(6.dp))
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(consultationQueuePatients, key = { it.id }) { p ->
                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(6.dp))
                      .clickable {
                        viewModel.selectPatient(p.id)
                        viewModel.updatePatientStatus(p.id, QueueStatus.IN_CONSULTATION)
                      },
                    shape = RoundedCornerShape(6.dp),
                    color = BackgroundSurface,
                    border = BorderStroke(0.5.dp, BorderGray)
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                          modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (p.isFollowUp) Color(0xFFF3E8FF) else SoftGreenContainer),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(
                            "#${p.queueNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (p.isFollowUp) Color(0xFF7E22CE) else MedicalGreenDark
                          )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("• ${p.hn} • ${p.age}y/${p.gender.name.first()}", fontSize = 10.5.sp, color = SlateMuted)
                          }
                          Text(p.chiefComplaint, fontSize = 10.sp, color = SlateMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                      }
                      Button(
                        onClick = {
                          viewModel.selectPatient(p.id)
                          viewModel.updatePatientStatus(p.id, QueueStatus.IN_CONSULTATION)
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Text("Consult", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Modals
  if (showPatientSelectorDialog) {
    PatientSelectorDialog(
      patients = consultationQueuePatients,
      activePatientId = activePatient?.id,
      onSelectPatient = { patientId ->
        viewModel.selectPatient(patientId)
        viewModel.updatePatientStatus(patientId, QueueStatus.IN_CONSULTATION)
      },
      onDismiss = { showPatientSelectorDialog = false }
    )
  }

  if (showAttendingDoctorDialog) {
    val doctorUsers = users.filter { it.role == UserRole.DOCTOR }
    val defaultDoc = Triple(clinicSettings.doctorName, clinicSettings.doctorDegree, clinicSettings.doctorSama)
    AttendingDoctorSelectorDialog(
      currentDoctor = effectiveDoctor,
      doctorUsers = doctorUsers,
      defaultClinicDoctor = defaultDoc,
      onSelectDoctorAccount = { doc ->
        viewModel.setAttendingDoctor(doc)
      },
      onSetCustomDoctor = { name, degree, sama ->
        viewModel.setCustomAttendingDoctor(name, degree, sama)
      },
      onResetToDefault = {
        viewModel.setAttendingDoctor(null)
      },
      onDismiss = { showAttendingDoctorDialog = false }
    )
  }

  if (showDiscardConfirmation && activePatient != null) {
    AlertDialog(
      onDismissRequest = { showDiscardConfirmation = false },
      icon = {
        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
      },
      title = {
        Text("ဆေးစစ်ချက် ပယ်ဖျက်ရန် အတည်ပြုပါ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      },
      text = {
        Text(
          "လူနာ \"${activePatient.name}\" (${activePatient.hn}) ၏ လက်ရှိ စစ်ဆေးနေသော မှတ်စုများကို မသိမ်းဆည်းဘဲ စာရင်းမှ ပယ်ဖျက်၍ Waiting Queue သို့ ပြန်လည်ပို့ဆောင်ပါမည်လား?",
          fontSize = 11.sp,
          color = DarkGrayText,
          lineHeight = 16.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.discardCurrentConsultation(activePatient.id)
            showDiscardConfirmation = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("ပယ်ဖျက်မည် (Discard Draft)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showDiscardConfirmation = false },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("ဆက်လက်စစ်ဆေးမည် (Keep)", fontSize = 11.sp)
        }
      }
    )
  }

  if (showAddDrugModal && activePatient != null) {
    DrugPickerModal(
      patient = activePatient,
      medicines = medicines,
      onDismiss = { showAddDrugModal = false },
      onDrugAdded = { drug ->
        prescribedDrugs.add(drug)
        showAddDrugModal = false
      }
    )
  }

  if (showAddTreatmentOrderModal && activePatient != null) {
    AddTreatmentOrderModal(
      patient = activePatient,
      medicines = medicines,
      clinicSettings = clinicSettings,
      doctorName = effectiveDoctor.first,
      onDismiss = { showAddTreatmentOrderModal = false },
      onConfirm = { card ->
        orderedTreatmentCards.add(card)
        // Also add medication to prescribedDrugs for transparency on A4 print and Rx
        val rxName = if (card.isCocktail && card.cocktailItems.isNotEmpty()) {
          val itemsStr = card.cocktailItems.joinToString(" + ") { "${it.name} ${it.dosage}".trim() }
          if (card.medicationName.isNotBlank() && !card.medicationName.contains("Mix", ignoreCase = true) && !itemsStr.contains(card.medicationName)) {
            "${card.medicationName} ($itemsStr)"
          } else {
            itemsStr
          }
        } else {
          card.medicationName
        }
        val rxDosage = if (card.isCocktail) {
          if (card.cocktailItems.isNotEmpty()) "${card.cocktailItems.size} Drugs Mixed" else card.dosage
        } else card.dosage.ifBlank { "${card.route.label} (${card.frequency})" }
        val patientOwnTag = if (card.isPatientSupplied) " [လူနာပါဆေး]" else ""
        val finalRxName = rxName + patientOwnTag

        if (prescribedDrugs.none { it.name.equals(finalRxName, ignoreCase = true) }) {
          prescribedDrugs.add(
            PrescribedDrug(
              name = finalRxName,
              medicineName = finalRxName,
              dosage = rxDosage,
              frequency = card.frequency,
              duration = "${card.totalDays} Days",
              quantity = card.totalSessions,
              unitPrice = if (card.isPatientSupplied) 0.0 else card.medicationFee,
              instructions = "${card.route.label} - ${card.instructions}",
              isExternal = card.isPatientSupplied
            )
          )
        }
        showAddTreatmentOrderModal = false
      }
    )
  }

  if (showCustomProcedureModal) {
    CustomProcedureModal(
      medicines = medicines,
      onDismiss = { showCustomProcedureModal = false },
      onSaveProcedure = { name, myanmarName, category, fee, notes, dt, consumables, consItems ->
        val newProc = viewModel.addCustomProcedure(
          name = name,
          myanmarName = myanmarName,
          category = category,
          fee = fee,
          notes = notes,
          consumables = consumables,
          dateTime = dt,
          consumableItems = consItems
        )
        if (category.isNotBlank()) {
          viewModel.addProcedureCategory(category)
        }
        selectedProcedures.add(newProc)
        showCustomProcedureModal = false
      }
    )
  }

  if (showScribbleExpanded) {
    A4AnatomyCanvasDialog(
      patient = activePatient,
      initialStrokes = anatomyStrokes,
      initialPages = drawingPagesState,
      initialNotes = canvasNotesState,
      onDismiss = { showScribbleExpanded = false },
      onSave = { updatedStrokes, count ->
        anatomyStrokes.clear()
        anatomyStrokes.addAll(updatedStrokes)
        anatomyPageCount = count
      },
      onSavePages = { pages, notes ->
        drawingPagesState.clear()
        drawingPagesState.addAll(pages)
        canvasNotesState = notes
      }
    )
  }

  if (showMedicalLeaveModal) {
    MedicalLeaveModal(
      patient = activePatient,
      initialRecord = medicalLeaveState,
      diagnosisDefault = selectedDiagnoses.firstOrNull() ?: assessment,
      onDismiss = { showMedicalLeaveModal = false },
      onSave = { record ->
        medicalLeaveState = record
        showMedicalLeaveModal = false
      },
      onRemove = {
        medicalLeaveState = null
        showMedicalLeaveModal = false
      }
    )
  }

  if (showReferralModal) {
    ReferralFormModal(
      patient = activePatient,
      initialRecord = referralState,
      diagnosisDefault = selectedDiagnoses.firstOrNull() ?: assessment,
      clinicalSummaryDefault = buildString {
        if (subjective.isNotBlank()) append("History & Presentation: $subjective\n")
        if (objective.isNotBlank()) append("Clinical Examination: $objective\n")
        if (assessment.isNotBlank()) append("Assessment: $assessment")
      }.trim().ifBlank { assessment },
      currentMedicationsDefault = prescribedDrugs.joinToString("\n") { "• ${it.name} ${it.dosage} - ${it.frequency} (${it.duration})" },
      onDismiss = { showReferralModal = false },
      onSave = { record ->
        referralState = record
        showReferralModal = false
      },
      onRemove = {
        referralState = null
        showReferralModal = false
      }
    )
  }

  if (showInstructionsModal) {
    PatientInstructionsModal(
      instructions = patientInstructionsState,
      onDismiss = { showInstructionsModal = false },
      onSave = { updated ->
        patientInstructionsState = updated
        showInstructionsModal = false
      }
    )
  }

  if (showAttachmentsModal && activePatient != null) {
    MedicalAttachmentsDialog(
      patient = activePatient,
      attachments = attachmentsState.toList(),
      onDismiss = { showAttachmentsModal = false },
      onAddAttachment = { att -> 
        attachmentsState.add(att)
        viewModel.saveDraftClinicalNotes(
          patientId = activePatient.id,
          attachments = attachmentsState.toList()
        )
      },
      onDeleteAttachment = { id -> 
        attachmentsState.removeAll { it.id == id }
        viewModel.saveDraftClinicalNotes(
          patientId = activePatient.id,
          attachments = attachmentsState.toList()
        )
      }
    )
  }

  if (showTrendsModal && activePatient != null) {
    val vitalHistory = viewModel.getPatientVitalsHistory(activePatient.id)
    ChronicDiseaseTrendsDialog(
      patient = activePatient,
      vitalHistory = vitalHistory,
      onDismiss = { showTrendsModal = false },
      onAddVitalPoint = { point -> viewModel.addVitalHistoryPoint(activePatient.id, point) }
    )
  }

  if (showVaccinesModal && activePatient != null) {
    val patientVaccines = remember(vaccineRecords, activePatient.id) {
      vaccineRecords.filter { it.patientId == activePatient.id }
    }
    VaccinationTrackerDialog(
      patient = activePatient,
      vaccines = patientVaccines,
      onDismiss = { showVaccinesModal = false },
      onAddVaccine = { rec -> viewModel.addVaccineRecord(rec) },
      onUpdateVaccine = { rec -> viewModel.saveOrUpdateVaccineRecord(rec) },
      onDeleteVaccine = { id -> viewModel.deleteVaccineRecord(id) }
    )
  }

  if (showConfirmSaveDialog && activePatient != null) {
    AlertDialog(
      onDismissRequest = { showConfirmSaveDialog = false },
      icon = {
        Icon(Icons.Default.Save, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(28.dp))
      },
      title = {
        Text("Save & Finalize Consultation", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkGrayText)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("ဆေးကုသမှုမှတ်တမ်း သိမ်းဆည်းပြီး ငွေရှင်း/ဆေးပေးဌာနသို့ လွှဲပြောင်းရန် အတည်ပြုပါ:", fontSize = 11.sp, color = SlateDark)
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text("• Patient: ${activePatient.name} (${activePatient.hn})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              if (selectedDiagnoses.isNotEmpty()) {
                Text("• Diagnosis: ${selectedDiagnoses.joinToString(", ")}", fontSize = 10.5.sp, color = SlateDark)
              }
              Text("• Prescriptions: ${prescribedDrugs.size} items", fontSize = 10.5.sp, color = SlateDark)
              if (selectedProcedures.isNotEmpty()) {
                Text("• Procedures: ${selectedProcedures.size} items", fontSize = 10.5.sp, color = SlateDark)
              }
              if (followUpAppointmentText.isNotBlank()) {
                Text("• Follow-up: $followUpAppointmentText", fontSize = 10.5.sp, color = MedicalGreenDark)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showConfirmSaveDialog = false
            doCompleteConsultation()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Confirm & Save File", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showConfirmSaveDialog = false },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Cancel", fontSize = 11.5.sp)
        }
      }
    )
  }

  // Edit Past Consultation Dialog
  editingConsultation?.let { record ->
    EditConsultationDialog(
      record = record,
      onDismiss = { editingConsultation = null },
      onSave = { updated ->
        viewModel.updateConsultation(updated)
        editingConsultation = null
        Toast.makeText(context, "ဆေးမှတ်တမ်း အောင်မြင်စွာ ပြင်ဆင်သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

/**
 * Dialog to edit previously saved consultation record (SOAP notes, fee, diagnoses)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditConsultationDialog(
  record: ConsultationRecord,
  onDismiss: () -> Unit,
  onSave: (ConsultationRecord) -> Unit
) {
  var subjectiveText by remember { mutableStateOf(record.subjective) }
  var objectiveText by remember { mutableStateOf(record.objective.ifBlank { record.canvasNotes }) }
  var assessmentText by remember { mutableStateOf(record.assessment.ifBlank { record.diagnosisList.joinToString(", ") }) }
  var planText by remember { mutableStateOf(record.plan) }
  var patientInstructionsText by remember { mutableStateOf(record.patientInstructions) }
  var feeText by remember { mutableStateOf(record.consultationFee.toInt().toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF7E22CE))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text("ဆေးမှတ်တမ်း ပြင်ဆင်ခြင်း (Edit Consultation)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text("Patient: ${record.patientName} • Date: ${record.timestamp}", fontSize = 10.sp, color = SlateMedium)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = subjectiveText,
          onValueChange = { subjectiveText = it },
          label = { Text("ရောဂါရာဇဝင် / Chief Complaints (S)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          maxLines = 4
        )
        OutlinedTextField(
          value = objectiveText,
          onValueChange = { objectiveText = it },
          label = { Text("စမ်းသပ်တွေ့ရှိချက် / Examination & Findings (O)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          maxLines = 4
        )
        OutlinedTextField(
          value = assessmentText,
          onValueChange = { assessmentText = it },
          label = { Text("ရောဂါအမည် / Assessment & Diagnosis (A)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        OutlinedTextField(
          value = planText,
          onValueChange = { planText = it },
          label = { Text("ကုသမှုအစီအစဉ် / Treatment Plan (P)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          maxLines = 4
        )
        OutlinedTextField(
          value = patientInstructionsText,
          onValueChange = { patientInstructionsText = it },
          label = { Text("ဆရာဝန်၏ ညွှန်ကြားချက် / Doctor's Advice & Instructions", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          maxLines = 4
        )
        OutlinedTextField(
          value = feeText,
          onValueChange = { feeText = it },
          label = { Text("ဆရာဝန်ပြသခ / Consultation Fee (Ks)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val newFee = feeText.toDoubleOrNull() ?: record.consultationFee
          val updated = record.copy(
            subjective = subjectiveText,
            objective = objectiveText,
            assessment = assessmentText,
            plan = planText,
            patientInstructions = patientInstructionsText,
            consultationFee = newFee
          )
          onSave(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
        shape = RoundedCornerShape(6.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("သိမ်းဆည်းမည် (Save)", fontSize = 11.sp)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("မသိမ်းဘဲပိတ်မည်", fontSize = 11.sp)
      }
    }
  )
}

