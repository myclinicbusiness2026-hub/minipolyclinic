package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.service.printing.A4PrinterSettingsStore
import com.example.ui.theme.*

/**
 * A4PrintPreviewDialog
 * 
 * Interactive A4 Print Preview Dialog providing a 100% WYSIWYG
 * Drafting Desk viewport displaying the authentic paper document layout
 * as it will physically emerge from the printer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun A4PrintPreviewDialog(
  consultation: ConsultationRecord,
  patient: Patient? = null,
  clinicSettings: ClinicSettings = ClinicSettings(),
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val a4SettingsStore = remember { A4PrinterSettingsStore(context) }

  val isGrayscaleSetting = a4SettingsStore.isGrayscale()
  var isGrayscale by remember { mutableStateOf(isGrayscaleSetting) }
  val marginMm = a4SettingsStore.getMarginMm()

  // Granular Section Toggles
  var showVitals by remember { mutableStateOf(true) }
  var showClinicalNotes by remember { mutableStateOf(true) }
  var showDiagnosis by remember { mutableStateOf(true) }
  var showProcedures by remember { mutableStateOf(consultation.selectedProcedures.isNotEmpty()) }
  var showPrescriptions by remember { mutableStateOf(consultation.prescriptions.isNotEmpty()) }
  var showInstructions by remember { mutableStateOf(consultation.patientInstructions.isNotBlank()) }
  var showDoctorSignature by remember { mutableStateOf(true) }

  var currentPageIndex by remember { mutableIntStateOf(0) }
  var showMarginGuides by remember { mutableStateOf(false) }
  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var fitMode by remember { mutableStateOf("fit_width") }

  val printSettings = remember(consultation.doctorName, consultation.doctorDegree, consultation.doctorSama, clinicSettings, showVitals, showClinicalNotes, showDiagnosis, showProcedures, showPrescriptions, showInstructions, showDoctorSignature) {
    A4PrintSettings.fromClinicSettings(
      clinicSettings.copy(
        doctorName = if (consultation.doctorName.isNotBlank()) consultation.doctorName else clinicSettings.doctorName,
        doctorDegree = if (consultation.doctorDegree.isNotBlank()) consultation.doctorDegree else clinicSettings.doctorDegree,
        doctorSama = if (consultation.doctorSama.isNotBlank()) consultation.doctorSama else clinicSettings.doctorSama
      ),
      options = A4PrintOptions(
        showVitals = showVitals,
        showClinicalNotes = showClinicalNotes,
        showDiagnosis = showDiagnosis,
        showProcedures = showProcedures,
        showInvestigations = true,
        showPrescriptions = showPrescriptions,
        showAdvice = showInstructions,
        showDoctorSignature = showDoctorSignature
      )
    )
  }

  // Dynamic Page Descriptors List: Auto-flows full-page Stylus drawing proportionately
  val pageList = remember(consultation, showPrescriptions, showProcedures, showClinicalNotes) {
    val list = mutableListOf<A4PageDescriptor>()
    val validDrawingPages = consultation.drawingPages.filter { it.strokes.isNotEmpty() }

    if (validDrawingPages.isNotEmpty() && showClinicalNotes) {
      // 1. Each Stylus drawing sheet automatically gets a full A4 portrait page in proper aspect ratio
      validDrawingPages.forEachIndexed { idx, _ ->
        val title = if (validDrawingPages.size > 1) {
          "Clinical Examination & Notes (Sheet ${idx + 1})"
        } else {
          "Clinical Examination & Notes"
        }
        list.add(A4PageDescriptor.CanvasDrawingPage(pageIndex = idx, title = title))
      }

      // 2. If prescriptions, procedures, or treatment plan are present, dynamically flow to the next sheet (Rx Sheet)
      val hasRxContent = (showPrescriptions && consultation.prescriptions.isNotEmpty()) ||
        (showProcedures && consultation.selectedProcedures.isNotEmpty()) ||
        consultation.suggestedInvestigations.isNotEmpty() ||
        consultation.patientInstructions.isNotBlank() ||
        consultation.assessment.isNotBlank() ||
        consultation.diagnosisList.isNotEmpty()

      if (hasRxContent) {
        list.add(A4PageDescriptor.OfficialPrescriptionPage(title = "Official Prescription & Treatment Orders"))
      }
    } else {
      // Dynamic EMR page calculation: check if it fits on 1 unified page or flows to 2 pages
      val emrPages = com.example.service.printing.A4PdfNativeCanvasRenderer.computeEmrPages(consultation, patient, printSettings)
      if (emrPages > 1) {
        list.add(A4PageDescriptor.ClinicalConsultationPage(pageIndex = 0, isFirstPage = true, title = "Clinical Consultation (Page 1)"))
        list.add(A4PageDescriptor.OfficialPrescriptionPage(title = "Treatment Orders & Orders (Page 2)"))
      } else {
        list.add(A4PageDescriptor.UnifiedSinglePage())
      }
    }

    // Medical leave certificate sheet if issued
    if (consultation.medicalLeave != null) {
      list.add(A4PageDescriptor.MedicalLeaveCertificatePage("Sheet ${list.size + 1}: Medical Leave Certificate"))
    }

    // Referral letter sheet if issued
    if (consultation.referralRecord != null) {
      list.add(A4PageDescriptor.ReferralLetterPage("Sheet ${list.size + 1}: Official Referral Letter"))
    }

    if (list.isEmpty()) {
      list.add(A4PageDescriptor.UnifiedSinglePage())
    }
    list
  }

  LaunchedEffect(pageList.size) {
    if (currentPageIndex >= pageList.size) {
      currentPageIndex = (pageList.size - 1).coerceAtLeast(0)
    }
  }
  val totalPages = pageList.size
  val currentPageDescriptor = pageList.getOrNull(currentPageIndex) ?: A4PageDescriptor.UnifiedSinglePage()

  val executePrint = {
    try {
      triggerRealAndroidPrint(
        context = context,
        consultation = consultation,
        patient = patient,
        printSettings = printSettings,
        pageDescriptors = pageList,
        marginMm = marginMm,
        isGrayscale = isGrayscale
      )
      Toast.makeText(context, "Android Print Spooler ဖွင့်နေပါပြီ...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Print Error: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
      onDismiss()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.96f),
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      tonalElevation = 6.dp,
      shadowElevation = 8.dp
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // 1. Header (Title, Patient Info, Primary Print button, Close button)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Print, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "A4 ဆေးမှတ်တမ်း အစမ်းကြည့်ရှုခြင်း (A4 Print Preview)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = DarkGrayText,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                "လူနာ: ${consultation.patientName} (${patient?.hn ?: "HN-0000"})",
                fontSize = 11.sp,
                color = SlateMedium,
                maxLines = 1
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Prominent Primary Print Button right in the Top Header Bar
          Button(
            onClick = executePrint,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("🖨️ Print ထုတ်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        HorizontalDivider(thickness = 0.8.dp, color = BorderGray)

        // 2. Section toggles (Scrollable row)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = showVitals,
            onClick = { showVitals = !showVitals },
            label = { Text("Vitals", fontSize = 10.5.sp) },
            leadingIcon = {
              Icon(
                if (showVitals) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftGreenContainer,
              selectedLabelColor = MedicalGreenDark
            )
          )

          FilterChip(
            selected = showClinicalNotes,
            onClick = { showClinicalNotes = !showClinicalNotes },
            label = { Text("History & Exam", fontSize = 10.5.sp) },
            leadingIcon = {
              Icon(
                if (showClinicalNotes) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftGreenContainer,
              selectedLabelColor = MedicalGreenDark
            )
          )

          FilterChip(
            selected = showDiagnosis,
            onClick = { showDiagnosis = !showDiagnosis },
            label = { Text("Diagnosis", fontSize = 10.5.sp) },
            leadingIcon = {
              Icon(
                if (showDiagnosis) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftGreenContainer,
              selectedLabelColor = MedicalGreenDark
            )
          )

          if (consultation.selectedProcedures.isNotEmpty()) {
            FilterChip(
              selected = showProcedures,
              onClick = { showProcedures = !showProcedures },
              label = { Text("Procedures (${consultation.selectedProcedures.size})", fontSize = 10.5.sp) },
              leadingIcon = {
                Icon(
                  if (showProcedures) Icons.Default.Check else Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SoftGreenContainer,
                selectedLabelColor = MedicalGreenDark
              )
            )
          }

          if (consultation.prescriptions.isNotEmpty()) {
            FilterChip(
              selected = showPrescriptions,
              onClick = { showPrescriptions = !showPrescriptions },
              label = { Text("℞ Prescriptions (${consultation.prescriptions.size})", fontSize = 10.5.sp) },
              leadingIcon = {
                Icon(
                  if (showPrescriptions) Icons.Default.Check else Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SoftGreenContainer,
                selectedLabelColor = MedicalGreenDark
              )
            )
          }

          if (consultation.patientInstructions.isNotBlank()) {
            FilterChip(
              selected = showInstructions,
              onClick = { showInstructions = !showInstructions },
              label = { Text("Instructions", fontSize = 10.5.sp) },
              leadingIcon = {
                Icon(
                  if (showInstructions) Icons.Default.Check else Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SoftGreenContainer,
                selectedLabelColor = MedicalGreenDark
              )
            )
          }

          FilterChip(
            selected = showDoctorSignature,
            onClick = { showDoctorSignature = !showDoctorSignature },
            label = { Text("Doctor Seal", fontSize = 10.5.sp) },
            leadingIcon = {
              Icon(
                if (showDoctorSignature) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftGreenContainer,
              selectedLabelColor = MedicalGreenDark
            )
          )

          FilterChip(
            selected = isGrayscale,
            onClick = { isGrayscale = !isGrayscale },
            label = { Text(if (isGrayscale) "B&W (မည်း/ဖြူ)" else "Color", fontSize = 10.5.sp) },
            leadingIcon = {
              Icon(
                if (isGrayscale) Icons.Default.Check else Icons.Default.Palette,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftGreenContainer,
              selectedLabelColor = MedicalGreenDark
            )
          )
        }

        // 3. Toolbar for Page Navigation & Zoom & Margin guides
        A4PreviewToolbar(
          currentPage = currentPageIndex + 1,
          totalPages = totalPages,
          currentDescriptor = currentPageDescriptor,
          zoomScale = zoomScale,
          fitMode = fitMode,
          onFitModeChange = { fitMode = it },
          showMarginGuides = showMarginGuides,
          onToggleMarginGuides = { showMarginGuides = !showMarginGuides },
          onPrevPage = { if (currentPageIndex > 0) currentPageIndex-- },
          onNextPage = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
          onZoomIn = { zoomScale = (zoomScale + 0.1f).coerceAtMost(2.0f) },
          onZoomOut = { zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.5f) },
          onResetZoom = { zoomScale = 1.0f }
        )

        // 4. WYSIWYG A4 Drafting Desk Viewport
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          A4DraftingDeskViewport(
            modifier = Modifier.fillMaxSize(),
            pageDescriptor = currentPageDescriptor,
            currentPage = currentPageIndex + 1,
            totalPages = totalPages,
            consultation = consultation,
            patient = patient,
            marginDp = marginMm.dp,
            showMarginGuides = showMarginGuides,
            isGrayscale = isGrayscale,
            incLetterhead = true,
            incDemographics = true,
            incCanvas = showClinicalNotes,
            incProcedures = showProcedures,
            incInvestigations = true,
            incAttachments = true,
            incRx = showPrescriptions,
            incStamp = showDoctorSignature,
            printSettings = printSettings,
            zoomScale = zoomScale,
            fitMode = fitMode
          )
        }

        // 5. Bottom Action Bar
        Surface(
          color = PureWhite,
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                "A4 စာရွက်ပေါ်တွင် အမှန်တကယ်ထွက်မည့်အတိုင်း ပြသထားပါသည် (WYSIWYG)",
                fontSize = 10.5.sp,
                color = SlateMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
              )
              Text(
                "Page ${currentPageIndex + 1} of $totalPages",
                fontSize = 10.sp,
                color = SlateLight
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text("Cancel", fontSize = 11.5.sp)
              }

              Button(
                onClick = executePrint,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
              ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("🖨️ Print (ပရင့်ထုတ်မည်)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
