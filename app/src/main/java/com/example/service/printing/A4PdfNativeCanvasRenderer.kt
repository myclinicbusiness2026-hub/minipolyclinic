package com.example.service.printing

import android.graphics.*
import java.io.File
import com.example.model.ConsultationRecord
import com.example.model.Patient
import com.example.model.PrescribedDrug
import com.example.ui.components.A4PageDescriptor
import com.example.ui.components.A4PrintSettings
import com.example.ui.components.formatDosageAndDirection

/**
 * A4PdfNativeCanvasRenderer
 * 
 * High-fidelity native vector canvas renderer for A4 clinical documents.
 * Produces crisp, infinitely sharp vector PDFs directly on Android Canvas.
 * Supports dynamic pagination (auto multi-page waterfall flow) with comfortable line-height,
 * professional international English clinical headings,
 * and dedicated full-sheet rendering for Medical Leave Certificates and Clinical Referral Letters.
 */
object A4PdfNativeCanvasRenderer {
  private const val PAGE_WIDTH = 595 // A4 standard pt at 72dpi
  private const val PAGE_HEIGHT = 842

  /**
   * Helper function to calculate how many prescriptions can fit on Page 1
   * when flowing dynamically across multiple pages in a waterfall layout.
   */
  fun calculateRxFitOnPageOne(
    consultation: ConsultationRecord,
    patient: Patient? = null,
    height: Float = 842f
  ): Int {
    val headerBottom = 16f + 62f // 78f
    val demoBottom = headerBottom + 6f + 22f + 5f // 111f
    val allergies = patient?.knownAllergies?.takeIf { it.isNotEmpty() } ?: patient?.allergies?.takeIf { it.isNotEmpty() }
    val allergiesH = 18f + 5f // 23f
    val vitalsH = 34f + 6f // 40f
    val p1Top = demoBottom + allergiesH + vitalsH // ~174f

    val dummyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8.5f }
    val colW = (PAGE_WIDTH - 40f - 12f) / 2f
    val subLines = getSubjectiveLines(consultation, patient)
    val objLines = getObjectiveLines(consultation)
    val subWrapped = subLines.sumOf { wrapTextIntoLines(it, colW, dummyPaint).size.coerceAtLeast(1) }
    val objWrapped = objLines.sumOf { wrapTextIntoLines(it, colW, dummyPaint).size.coerceAtLeast(1) }
    val clinicalLineCount = maxOf(subWrapped, objWrapped)
    val clinicalH = 15f + (clinicalLineCount * 14.5f).coerceAtLeast(30f) + 6f

    val diagText = when {
      consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
      consultation.assessment.isNotBlank() -> consultation.assessment
      else -> ""
    }
    val diagH = if (diagText.isNotBlank()) 26f else 0f

    val contentStartY = p1Top + clinicalH + diagH
    val maxPageOneY = height - 58f
    val spaceLeft = maxPageOneY - contentStartY - 30f // 30f for Prescription (Rx) table header

    val rxList = consultation.prescriptions
    if (rxList.isEmpty() || spaceLeft < 18f) return 0
    val maxCanFit = ((spaceLeft) / 18.5f).toInt().coerceIn(0, rxList.size)

    val validProcs = consultation.selectedProcedures.filter { it.name.isNotBlank() }
    val validInvs = consultation.suggestedInvestigations.filter { it.isNotBlank() }

    // If there are no procedures and no investigations going to Page 2,
    // and all Rx would fit on Page 1, leave at least 3-4 Rx for Page 2 so Page 2 has Rx content!
    if (validProcs.isEmpty() && validInvs.isEmpty() && maxCanFit == rxList.size && rxList.size > 4) {
      return (rxList.size / 2).coerceAtLeast(3)
    }

    return maxCanFit
  }

  /**
   * Dynamically calculate how many A4 pages are needed strictly for the base EMR consultation.
   * If all content fits comfortably on 1 page before the signature, returns 1.
   * Otherwise returns 2, triggering the dynamic continuous waterfall flow.
   */
  fun computeEmrPages(
    consultation: ConsultationRecord,
    patient: Patient? = null,
    printSettings: A4PrintSettings = A4PrintSettings()
  ): Int {
    val hasDrawing = consultation.drawingPages.any { it.strokes.isNotEmpty() }
    if (hasDrawing) {
      val validSheets = consultation.drawingPages.count { it.strokes.isNotEmpty() }
      return 1 + validSheets
    }

    var estY = 175f // Full Header (~78) + Demographics (~28) + Allergies (~23) + Vitals (~40)
    val dummyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8.5f }
    val colW = (PAGE_WIDTH - 40f - 12f) / 2f
    val subLines = getSubjectiveLines(consultation, patient)
    val objLines = getObjectiveLines(consultation)
    val subWrapped = subLines.sumOf { wrapTextIntoLines(it, colW, dummyPaint).size.coerceAtLeast(1) }
    val objWrapped = objLines.sumOf { wrapTextIntoLines(it, colW, dummyPaint).size.coerceAtLeast(1) }
    val clinicalLineCount = maxOf(subWrapped, objWrapped)
    estY += 15f + (clinicalLineCount * 14.5f).coerceAtLeast(30f) + 6f

    val diagText = when {
      consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
      consultation.assessment.isNotBlank() -> consultation.assessment
      else -> ""
    }
    if (diagText.isNotBlank()) {
      estY += 26f
    }

    val rxCount = consultation.prescriptions.size
    if (rxCount > 0) {
      estY += 30f + rxCount * 18.5f
    }

    val validProcs = consultation.selectedProcedures.filter { it.name.isNotBlank() }
    if (validProcs.isNotEmpty()) {
      estY += 16f
      validProcs.forEach { p ->
        val noteLines = if (p.notes.isNotBlank()) {
          wrapTextIntoLines("Notes: ${p.notes}", PAGE_WIDTH - 68f, dummyPaint).size.coerceAtLeast(1)
        } else 0
        estY += 18f + (noteLines * 11.5f) + 8f
      }
    }

    val validInvs = consultation.suggestedInvestigations.filter { it.isNotBlank() }
    if (validInvs.isNotEmpty()) {
      estY += 16f + (Math.ceil(validInvs.size / 2.0).toFloat() * 14f)
    }

    val contentBeforeInstructions = estY

    // Patient Instructions & Follow-up
    val hasInstructions = consultation.patientInstructions.isNotBlank()
    val rawFollowUp = consultation.followUpAppointment.trim()
    val isPrn = rawFollowUp.contains("PRN", ignoreCase = true) || rawFollowUp.contains("မသက်သာမှ")
    val isNone = rawFollowUp.isBlank() || rawFollowUp.equals("None", ignoreCase = true) || rawFollowUp.contains("None", ignoreCase = true) || rawFollowUp.contains("မလို")
    val hasFollowUp = isPrn || (!isNone && !consultation.followUpDueDate.isNullOrBlank()) || (!isNone && rawFollowUp.isNotBlank())

    if (hasInstructions || hasFollowUp) {
      val instCount = if (hasInstructions) consultation.patientInstructions.lines().filter { it.isNotBlank() }.size else 3
      estY += 24f + (instCount * 22f) // each instruction takes ~2 wrapped lines
    }

    // FIXED SAFE CONTENT CEILING (ကိန်းသေ အကွာအဝေး သတ်မှတ်ချက်):
    // Standard A4 height is 842pt. Doctor Signature & Seal and official footer begin at 755pt.
    // If patient instructions & follow-up exist (~95-110pt):
    // - Content before instructions must NOT exceed 605pt.
    // - Overall total estY must NOT exceed 670pt.
    // Otherwise it will crash into the signature zone, so trigger 2-page flow immediately!
    val needsMultiPage = if (hasInstructions || hasFollowUp) {
      contentBeforeInstructions > 605f || estY > 670f
    } else {
      estY > 710f
    }

    return if (needsMultiPage) 2 else 1
  }

  /**
   * Total page count including optional Medical Leave and Referral Letter.
   */
  fun computePageCount(
    consultation: ConsultationRecord,
    patient: Patient? = null,
    printSettings: A4PrintSettings = A4PrintSettings()
  ): Int {
    var count = computeEmrPages(consultation, patient, printSettings)
    if (consultation.medicalLeave != null) count += 1
    if (consultation.referralRecord != null) count += 1
    return count
  }

  fun renderA4RecordBitmap(
    record: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings = A4PrintSettings(),
    pageNumber: Int = 1,
    totalPages: Int = 1
  ): Bitmap {
    val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawPage(
      canvas = canvas,
      descriptor = A4PageDescriptor.UnifiedSinglePage(),
      pageNumber = pageNumber,
      totalPages = totalPages,
      consultation = record,
      patient = patient,
      printSettings = printSettings,
      width = PAGE_WIDTH.toFloat(),
      height = PAGE_HEIGHT.toFloat()
    )
    return bitmap
  }

  fun drawPage(
    canvas: Canvas,
    descriptor: A4PageDescriptor,
    pageNumber: Int,
    totalPages: Int,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings = A4PrintSettings(),
    width: Float = 595f,
    height: Float = 842f,
    marginMm: Float = 10f
  ) {
    canvas.drawColor(Color.WHITE)

    when (descriptor) {
      is A4PageDescriptor.MedicalLeaveCertificatePage -> {
        drawMedicalLeaveCertificate(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
      }
      is A4PageDescriptor.ReferralLetterPage -> {
        drawReferralLetter(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
      }
      is A4PageDescriptor.OfficialPrescriptionPage -> {
        drawPageTwoContent(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
      }
      is A4PageDescriptor.ClinicalConsultationPage -> {
        drawPageOneMultiPageContent(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
      }
      is A4PageDescriptor.UnifiedSinglePage -> {
        drawSinglePageUnified(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
      }
      else -> {
        if (totalPages > 1 && pageNumber > 1) {
          drawPageTwoContent(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
        } else if (totalPages > 1 && pageNumber == 1) {
          drawPageOneMultiPageContent(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
        } else {
          drawSinglePageUnified(canvas, width, height, consultation, patient, printSettings, pageNumber, totalPages)
        }
      }
    }
  }

  // =========================================================================
  // 1. SINGLE-PAGE UNIFIED MODE (Continuous Dynamic Flow)
  // =========================================================================
  private fun drawSinglePageUnified(
    canvas: Canvas,
    width: Float,
    height: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings,
    pageNumber: Int = 1,
    totalPages: Int = 1
  ) {
    // 1. Clinic Letterhead Header
    val headerBottom = drawFullHeader(canvas, width, printSettings)

    // 2. Patient Demographics Bar
    var currentY = headerBottom + 6f
    currentY = drawPatientDemographics(canvas, width, currentY, patient, consultation)

    // 3. Drug Allergies Bar
    currentY = drawAllergiesBanner(canvas, width, currentY, patient)

    // 4. Vitals Row (6 Stat Boxes)
    currentY = drawVitalsRow(canvas, width, currentY, consultation, patient)

    // 5. Clinical Examination & History Section
    val hasHistory = !patient?.chiefComplaint.isNullOrBlank() || consultation.subjective.isNotBlank()
    val hasExam = consultation.objective.isNotBlank()

    if (hasHistory && hasExam) {
      currentY = drawTwoColumnClinicalSection(canvas, width, currentY, consultation, patient, maxLines = 25)
    } else if (hasHistory) {
      val lines = getSubjectiveLines(consultation, patient)
      currentY = drawSingleColumnClinicalSection(canvas, width, currentY, "Chief Complaint & Clinical History:", lines, maxLines = 25)
    } else if (hasExam) {
      val lines = getObjectiveLines(consultation)
      currentY = drawSingleColumnClinicalSection(canvas, width, currentY, "Physical Examination & Clinical Findings:", lines, maxLines = 25)
    }

    // 6. Provisional Diagnosis Box (Placed under History & Physical Examination)
    val diagText = when {
      consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
      consultation.assessment.isNotBlank() -> consultation.assessment
      else -> ""
    }
    if (diagText.isNotBlank()) {
      currentY = drawDiagnosisBox(canvas, width, currentY, diagText)
    }

    // 7. Prescriptions (Rx) Section (Only if present)
    if (consultation.prescriptions.isNotEmpty()) {
      currentY = drawPrescriptionsTable(canvas, width, currentY, consultation, isExpanded = false)
    }

    // 8. Procedures & Investigations (Only if present)
    if (consultation.selectedProcedures.isNotEmpty() || consultation.suggestedInvestigations.isNotEmpty()) {
      currentY = drawProceduresAndInvestigations(canvas, width, currentY, consultation)
    }

    // 9. Bottom Split: Patient Instructions & Follow-up Appointment
    val hasInstructions = consultation.patientInstructions.isNotBlank()
    val followUpStr = consultation.followUpAppointment.trim()
    val isPrnFollowUp = followUpStr.contains("PRN", ignoreCase = true) || followUpStr.contains("မသက်သာမှ")
    val isNoneFollowUp = followUpStr.isBlank() || followUpStr.equals("None", ignoreCase = true) || followUpStr.contains("မလို")
    val hasFollowUp = isPrnFollowUp || (!isNoneFollowUp && (!consultation.followUpDueDate.isNullOrBlank() || consultation.nextVaccineSchedule.isNotBlank() || followUpStr.isNotBlank()))
    val maxAvailableY = height - 84f
    if (currentY < maxAvailableY - 25f && (hasInstructions || hasFollowUp)) {
      currentY = drawBottomSplitSection(canvas, width, currentY, consultation, maxAvailableY)
    }

    // 10. Official Footer & Physician Signature Block
    drawFooterAndSignature(canvas, width, height, consultation, printSettings, pageNumber = pageNumber, totalPages = totalPages)
  }

    // =========================================================================
  // 2. MULTI-PAGE: PAGE 1 (Continuous Dynamic Waterfall Flow)
  // =========================================================================
  private fun drawPageOneMultiPageContent(
    canvas: Canvas,
    width: Float,
    height: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings,
    pageNumber: Int,
    totalPages: Int
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Full Clinic Header
    val headerBottom = drawFullHeader(canvas, width, printSettings)

    // 2. Patient Demographics Bar
    var currentY = headerBottom + 6f
    currentY = drawPatientDemographics(canvas, width, currentY, patient, consultation)

    // 3. Drug Allergies Bar
    currentY = drawAllergiesBanner(canvas, width, currentY, patient)

    // 4. Vitals Row (6 Stat Boxes)
    currentY = drawVitalsRow(canvas, width, currentY, consultation, patient)

    // 5. Clinical Examination & History Section (maxLines = 40 to avoid artificial cutoff)
    val hasHistory = !patient?.chiefComplaint.isNullOrBlank() || consultation.subjective.isNotBlank()
    val hasExam = consultation.objective.isNotBlank()

    if (hasHistory && hasExam) {
      currentY = drawTwoColumnClinicalSection(canvas, width, currentY, consultation, patient, maxLines = 40)
    } else if (hasHistory) {
      val lines = getSubjectiveLines(consultation, patient)
      currentY = drawSingleColumnClinicalSection(canvas, width, currentY, "Chief Complaint & Clinical History:", lines, maxLines = 40)
    } else if (hasExam) {
      val lines = getObjectiveLines(consultation)
      currentY = drawSingleColumnClinicalSection(canvas, width, currentY, "Physical Examination & Clinical Findings:", lines, maxLines = 40)
    }

    // 6. Provisional Diagnosis Box (Placed under History & Physical Examination)
    val diagTextMulti = when {
      consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
      consultation.assessment.isNotBlank() -> consultation.assessment
      else -> ""
    }
    if (diagTextMulti.isNotBlank()) {
      currentY = drawDiagnosisBox(canvas, width, currentY, diagTextMulti)
    }

    // 7. Dynamic Waterfall Flow: Fill remaining vertical space on Page 1 with Prescriptions
    val rxList = consultation.prescriptions
    val rxRowsCanFit = calculateRxFitOnPageOne(consultation, patient, height)

    if (rxRowsCanFit > 0 && rxList.isNotEmpty()) {
      val p1Rx = rxList.take(rxRowsCanFit)
      currentY = drawPrescriptionsTableSlice(canvas, width, currentY, p1Rx, startIndex = 0)
    }

    // Continuation notice at bottom of Page 1 (Clear, professional English)
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 8.5f
    paint.textAlign = Paint.Align.CENTER
    val continueNotice = if (rxRowsCanFit in 1 until rxList.size) {
      "▼ Continued on Page 2 (Remaining Prescriptions & Treatment Orders) ▼"
    } else if (rxRowsCanFit == 0) {
      "▼ Continued on Page 2 (Prescriptions, Treatment Orders & Attending Physician Seal) ▼"
    } else {
      "▼ Continued on Page 2 (Clinical Procedures, Investigations & Attending Physician Seal) ▼"
    }
    canvas.drawText(continueNotice, width / 2f, height - 38f, paint)
    paint.textAlign = Paint.Align.LEFT

    // Footer
    drawFooterOnly(canvas, width, height, printSettings, pageNumber, totalPages)
  }

  // =========================================================================
  // 3. MULTI-PAGE: PAGE 2 (Continuation - Orders, Advice & Signature)
  // =========================================================================
  private fun drawPageTwoContent(
    canvas: Canvas,
    width: Float,
    height: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings,
    pageNumber: Int,
    totalPages: Int
  ) {
    // 1. Clean Mini Continuation Header
    var currentY = drawMiniContinuationHeader(canvas, width, consultation, patient, printSettings, pageNumber)

    // Compact patient reminder sub-bar on Page 2
    val hn = patient?.hn ?: "HN-2026-0101"
    val name = patient?.name ?: consultation.patientName
    val age = patient?.age ?: 45
    val pGender = patient?.gender?.name
    val gender = if (pGender == "FEMALE") "F" else "M"
    val date = consultation.timestamp.ifBlank { "2026-10-19" }.take(16)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(0x47, 0x55, 0x69)
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Patient: $name  |  HN: $hn  |  Age/Sex: " + age + "y/" + gender + "  |  Date: " + date, 22f, currentY + 11f, paint)
    paint.color = Color.rgb(0xE2, 0xE8, 0xF0)
    paint.strokeWidth = 0.6f
    canvas.drawLine(22f, currentY + 16f, width - 22f, currentY + 16f, paint)
    currentY += 22f

    // 2. Remaining Prescriptions (if any)
    val rxList = consultation.prescriptions
    val rxRowsOnP1 = calculateRxFitOnPageOne(consultation, patient, height)
    val remainingRx = if (rxRowsOnP1 in 0 until rxList.size) rxList.drop(rxRowsOnP1) else emptyList()

    if (remainingRx.isNotEmpty()) {
      currentY = drawPrescriptionsTableSlice(
        canvas = canvas,
        width = width,
        topY = currentY,
        prescriptions = remainingRx,
        startIndex = rxRowsOnP1
      )
    }

    // 3. Procedures & Investigations (if any)
    val validProcs = consultation.selectedProcedures.filter { it.name.isNotBlank() }
    val validInvs = consultation.suggestedInvestigations.filter { it.isNotBlank() }
    if (validProcs.isNotEmpty() || validInvs.isNotEmpty()) {
      currentY = drawProceduresAndInvestigations(canvas, width, currentY, consultation)
    }

    // 4. Patient Instructions & Follow-up
    val hasInstructions = consultation.patientInstructions.isNotBlank()
    val followUpStr = consultation.followUpAppointment.trim()
    val isPrnFollowUp = followUpStr.contains("PRN", ignoreCase = true) || followUpStr.contains("မသက်သာမှ")
    val isNoneFollowUp = followUpStr.isBlank() || followUpStr.equals("None", ignoreCase = true) || followUpStr.contains("မလို")
    val hasFollowUp = isPrnFollowUp || (!isNoneFollowUp && (!consultation.followUpDueDate.isNullOrBlank() || consultation.nextVaccineSchedule.isNotBlank() || followUpStr.isNotBlank()))
    val maxAvailableY = height - 84f
    if (currentY < maxAvailableY - 25f && (hasInstructions || hasFollowUp)) {
      currentY = drawBottomSplitSection(canvas, width, currentY, consultation, maxAvailableY)
    }

    // 5. Full Doctor Signature Block & Official Footer in English
    drawFooterAndSignature(canvas, width, height, consultation, printSettings, pageNumber = pageNumber, totalPages = totalPages)
  }

  // =========================================================================
  // 4. DEDICATED FULL A4 MEDICAL LEAVE CERTIFICATE
  // =========================================================================
  fun drawMedicalLeaveCertificate(
    canvas: Canvas,
    width: Float,
    height: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings,
    pageNumber: Int,
    totalPages: Int
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val ml = consultation.medicalLeave

    // 1. Clinic Letterhead
    val headerBottom = drawFullHeader(canvas, width, printSettings)
    var currentY = headerBottom + 10f

    // 2. Certificate Header Banner
    paint.color = Color.rgb(0xFA, 0xFD, 0xF8)
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 44f, 4f, 4f, paint)
    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.2f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 44f, 4f, 4f, paint)
    paint.style = Paint.Style.FILL

    // Banner Text
    paint.textAlign = Paint.Align.CENTER
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 13.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("MEDICAL LEAVE CERTIFICATE", width / 2f, currentY + 16f, paint)

    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Official Medical Certificate of Physical Unfitness & Rest Recommendation", width / 2f, currentY + 28f, paint)

    paint.color = Color.rgb(0x61, 0x61, 0x61)
    paint.textSize = 7.5f
    val refNo = ml?.certificateNo ?: "MC-${consultation.id.take(8).uppercase()}"
    val issueDate = ml?.issueDate ?: consultation.timestamp.take(10)
    canvas.drawText("Certificate Ref No: $refNo   •   Date of Issue: $issueDate", width / 2f, currentY + 39f, paint)
    paint.textAlign = Paint.Align.LEFT
    currentY += 54f

    // 3. Patient Particulars Card
    currentY = drawPatientDemographics(canvas, width, currentY, patient, consultation) + 4f

    // 4. Clinical Diagnosis & Reason for Leave
    val diagText = ml?.diagnosis?.ifBlank {
      consultation.diagnosisList.joinToString(" • ").ifBlank { consultation.assessment.ifBlank { "Acute Medical Condition" } }
    } ?: "Acute Medical Condition"

    paint.color = Color.rgb(0xF8, 0xFA, 0xFC)
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 36f, 3f, 3f, paint)
    paint.color = Color.rgb(0xCB, 0xD5, 0xE1)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.8f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 36f, 3f, 3f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0x47, 0x55, 0x69)
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("CLINICAL DIAGNOSIS & REASON FOR LEAVE:", 28f, currentY + 13f, paint)

    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(diagText, 28f, currentY + 28f, paint)
    currentY += 46f

    // 5. Medical Evaluation & Fitness Status Box
    val evalBoxStartY = currentY
    paint.color = Color.WHITE
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 180f, 4f, 4f, paint)
    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 180f, 4f, 4f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("MEDICAL EVALUATION & FITNESS STATUS:", 28f, currentY + 16f, paint)
    currentY += 28f

    // Certification statement
    paint.color = Color.rgb(0x21, 0x21, 0x21)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("To Whom It May Concern:", 28f, currentY, paint)
    currentY += 14f

    val certStatement = "This is to certify that the patient named above was clinically examined and treated at this facility on $issueDate. In my professional medical evaluation, the patient is unfit for official duty, workplace attendance, or academic activities and is advised complete physical rest."
    currentY = drawWrappedText(canvas, certStatement, 28f, currentY, width - 56f, 13f, paint, maxLines = 4)
    currentY += 8f

    // Highlighted Fitness & Leave Duration Box
    paint.color = Color.rgb(0xF1, 0xF5, 0xF9)
    canvas.drawRoundRect(28f, currentY, width - 28f, currentY + 38f, 3f, 3f, paint)
    paint.color = Color.rgb(0xE2, 0xE8, 0xF0)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.6f
    canvas.drawRoundRect(28f, currentY, width - 28f, currentY + 38f, 3f, 3f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0xB9, 0x1C, 0x1C) // Red status
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val status = ml?.fitnessStatus ?: "Unfit for Duty / Complete Rest Required"
    canvas.drawText("Fitness Status: $status", 36f, currentY + 15f, paint)

    paint.color = Color.rgb(0x1E, 0x29, 0x3B)
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val leaveDays = ml?.leaveDays ?: 3
    val periodStr = "Recommended Leave Period: $leaveDays Day(s), from ${ml?.startDate ?: issueDate} to ${ml?.endDate ?: issueDate}"
    canvas.drawText(periodStr, 36f, currentY + 29f, paint)
    currentY += 48f

    // Advice note
    paint.color = Color.rgb(0x64, 0x74, 0x8B)
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    canvas.drawText("The patient is strongly advised to adhere strictly to prescribed pharmacotherapy and rest during the aforementioned period.", 28f, currentY, paint)
    currentY += 14f

    if (!ml?.remarks.isNullOrBlank()) {
      paint.typeface = Typeface.DEFAULT
      paint.color = Color.rgb(0x33, 0x41, 0x55)
      canvas.drawText("Physician's Remarks & Advice: ${ml!!.remarks}", 28f, currentY, paint)
    }

    // 6. Doctor Signature & Seal Block
    drawFooterAndSignature(
      canvas = canvas,
      width = width,
      height = height,
      consultation = consultation,
      printSettings = printSettings,
      pageNumber = pageNumber,
      totalPages = totalPages,
      docLabel = "Attending Physician Signature & Registration Seal"
    )
  }

  // =========================================================================
  // 5. DEDICATED FULL A4 CLINICAL REFERRAL LETTER
  // =========================================================================
  fun drawReferralLetter(
    canvas: Canvas,
    width: Float,
    height: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings,
    pageNumber: Int,
    totalPages: Int
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val ref = consultation.referralRecord

    // 1. Clinic Letterhead
    val headerBottom = drawFullHeader(canvas, width, printSettings)
    var currentY = headerBottom + 10f

    // 2. Banner Box (Soft Blue Accent)
    paint.color = Color.rgb(0xFA, 0xFB, 0xFF)
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 44f, 4f, 4f, paint)
    paint.color = Color.rgb(0x1D, 0x4E, 0xD8)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.2f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 44f, 4f, 4f, paint)
    paint.style = Paint.Style.FILL

    paint.textAlign = Paint.Align.CENTER
    paint.color = Color.rgb(0x1E, 0x3A, 0x8A)
    paint.textSize = 13.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("OFFICIAL CLINICAL REFERRAL LETTER", width / 2f, currentY + 16f, paint)

    paint.color = Color.rgb(0x1D, 0x4E, 0xD8)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Inter-Hospital & Specialist Consultation Requisition", width / 2f, currentY + 28f, paint)

    paint.color = Color.rgb(0x61, 0x61, 0x61)
    paint.textSize = 7.5f
    val refNo = ref?.referralNo ?: "REF-${consultation.id.take(8).uppercase()}"
    val refDate = ref?.referralDate?.ifBlank { consultation.timestamp.take(10) } ?: consultation.timestamp.take(10)
    val urgency = ref?.urgencyLevel ?: "Routine"
    canvas.drawText("Referral Ref No: $refNo   •   Date: $refDate   •   Urgency: $urgency", width / 2f, currentY + 39f, paint)
    paint.textAlign = Paint.Align.LEFT
    currentY += 52f

    // 3. Recipient Box (TO: ...)
    paint.color = Color.WHITE
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 46f, 3f, 3f, paint)
    paint.color = Color.rgb(0x93, 0xC5, 0xFD)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.8f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 46f, 3f, 3f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0x1D, 0x4E, 0xD8)
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("TO: ", 28f, currentY + 14f, paint)
    paint.color = Color.rgb(0x1E, 0x29, 0x3B)
    val toTarget = ref?.referralTo?.ifBlank { "Attending Specialist / Medical Officer" } ?: "Attending Specialist / Medical Officer"
    canvas.drawText(toTarget, 52f, currentY + 14f, paint)

    paint.color = Color.rgb(0x64, 0x74, 0x8B)
    paint.textSize = 7.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Dear Colleague / Attending Specialist,", 28f, currentY + 27f, paint)
    canvas.drawText("May I respectfully refer my patient for your expert clinical evaluation, specialized investigations, and management.", 28f, currentY + 38f, paint)
    currentY += 52f

    // 4. Patient Particulars Strip
    currentY = drawPatientDemographics(canvas, width, currentY, patient, consultation) + 4f

    // 5. Provisional / Working Diagnosis
    val diag = ref?.provisionalDiagnosis?.ifBlank {
      consultation.diagnosisList.joinToString(" • ").ifBlank { consultation.assessment.ifBlank { "Clinical Diagnosis under Investigation" } }
    } ?: "Clinical Diagnosis under Investigation"

    paint.color = Color.rgb(0xF0, 0xFD, 0xF4)
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 30f, 3f, 3f, paint)
    paint.color = Color.rgb(0xA7, 0xF3, 0xD0)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.8f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 30f, 3f, 3f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0x16, 0x65, 0x34)
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("PROVISIONAL / WORKING DIAGNOSIS:", 28f, currentY + 11f, paint)

    paint.color = Color.rgb(0x15, 0x80, 0x3D)
    paint.textSize = 9.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(diag, 28f, currentY + 23f, paint)
    currentY += 36f

    // 6. Reason for Referral Box
    val reason = ref?.reasonForReferral?.ifBlank {
      "For specialist evaluation, advanced diagnostic workup, and further inpatient management."
    } ?: "For specialist evaluation and further inpatient management."

    paint.color = Color.rgb(0xFA, 0xFA, 0xFA)
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 34f, 3f, 3f, paint)
    paint.color = Color.rgb(0xE2, 0xE8, 0xF0)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.6f
    canvas.drawRoundRect(20f, currentY, width - 20f, currentY + 34f, 3f, 3f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0x47, 0x55, 0x69)
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("REASON FOR REFERRAL & CLINICAL INQUIRIES:", 28f, currentY + 11f, paint)

    paint.color = Color.rgb(0x1E, 0x29, 0x3B)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(reason.take(85), 28f, currentY + 24f, paint)
    currentY += 40f

    // 7. Clinical Summary & Findings
    val summaryLines = mutableListOf<String>()
    if (consultation.subjective.isNotBlank()) summaryLines.add("History: ${consultation.subjective.replace("\n", " ").take(90)}")
    if (consultation.objective.isNotBlank()) summaryLines.add("Findings: ${consultation.objective.replace("\n", " ").take(90)}")
    if (summaryLines.isNotEmpty()) {
      paint.color = Color.rgb(0xFA, 0xFA, 0xFA)
      val boxH = 14f + (summaryLines.size * 13f)
      canvas.drawRoundRect(20f, currentY, width - 20f, currentY + boxH, 3f, 3f, paint)
      paint.color = Color.rgb(0xE2, 0xE8, 0xF0)
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 0.6f
      canvas.drawRoundRect(20f, currentY, width - 20f, currentY + boxH, 3f, 3f, paint)
      paint.style = Paint.Style.FILL

      paint.color = Color.rgb(0x47, 0x55, 0x69)
      paint.textSize = 7.5f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("CLINICAL SUMMARY & EXAMINATION FINDINGS:", 28f, currentY + 11f, paint)

      paint.color = Color.rgb(0x21, 0x21, 0x21)
      paint.textSize = 8f
      paint.typeface = Typeface.DEFAULT
      var sY = currentY + 23f
      summaryLines.forEach { line ->
        canvas.drawText(line, 28f, sY, paint)
        sY += 13f
      }
      currentY += boxH + 6f
    }

    // 8. Medications Given / Current Regimen
    if (consultation.prescriptions.isNotEmpty()) {
      currentY = drawPrescriptionsTable(canvas, width, currentY, consultation, isExpanded = false)
    }

    // 9. Closing appreciation
    paint.color = Color.rgb(0x64, 0x74, 0x8B)
    paint.textSize = 8f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Thank you very much for taking over the care of this patient.", 28f, currentY + 12f, paint)
    canvas.drawText("Sincerely,", 28f, currentY + 24f, paint)

    // 10. Doctor Signature & Seal Block
    drawFooterAndSignature(
      canvas = canvas,
      width = width,
      height = height,
      consultation = consultation,
      printSettings = printSettings,
      pageNumber = pageNumber,
      totalPages = totalPages,
      docLabel = "Referring Physician Signature & Registration Seal"
    )
  }

  // =========================================================================
  // HEADER DRAWING FUNCTIONS
  // =========================================================================
  private fun drawFullHeader(canvas: Canvas, width: Float, printSettings: A4PrintSettings): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val headerHeight = 62f

    // Header Background
    paint.color = Color.rgb(0xFA, 0xFA, 0xFA)
    canvas.drawRect(20f, 16f, width - 20f, 16f + headerHeight, paint)

    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.strokeWidth = 2.5f
    canvas.drawLine(20f, 16f + headerHeight, width - 20f, 16f + headerHeight, paint)

    // Left: Circular Logo Emblem (Custom or White Cross)
    val logoCx = 46f
    val logoCy = 16f + headerHeight / 2f
    val logoR = 18f
    val customLogo = loadLogoBitmap(printSettings.clinicLogoUri)
    if (customLogo != null) {
      val maxW = 50f
      val maxH = 48f
      val scale = minOf(maxW / customLogo.width.toFloat(), maxH / customLogo.height.toFloat())
      val targetW = customLogo.width * scale
      val targetH = customLogo.height * scale
      val dstRect = RectF(logoCx - targetW / 2f, logoCy - targetH / 2f, logoCx + targetW / 2f, logoCy + targetH / 2f)
      canvas.drawBitmap(customLogo, null, dstRect, null)
    } else {
      paint.color = Color.rgb(0x2E, 0x7D, 0x32)
      canvas.drawCircle(logoCx, logoCy, logoR, paint)
      paint.color = Color.WHITE
      val crossW = 4f
      val crossL = 10f
      canvas.drawRect(logoCx - crossW / 2, logoCy - crossL, logoCx + crossW / 2, logoCy + crossL, paint)
      canvas.drawRect(logoCx - crossL, logoCy - crossW / 2, logoCx + crossL, logoCy + crossW / 2, paint)
    }

    // Clinic Titles
    val textStartX = 72f
    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 14f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val cleanNameMm = printSettings.clinicNameMm.replace("“", "").replace("”", "").replace("\"", "").replace("'", "").trim().ifBlank { "ရွှေခေတ်" }
    val cleanSubMm = printSettings.clinicSubtitleMm.ifBlank { "အထွေထွေရောဂါကုဆေးခန်း" }
    val clinicTitleMm = "“$cleanNameMm” $cleanSubMm"
    canvas.drawText(clinicTitleMm, textStartX, 36f, paint)

    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val clinicTitleEn = printSettings.clinicNameEn.ifBlank { "Shwe Khit General Clinic" }
    canvas.drawText(clinicTitleEn, textStartX, 51f, paint)

    paint.color = Color.rgb(0x61, 0x61, 0x61)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    val addr = printSettings.clinicAddress.ifBlank { "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့" }
    val phone = printSettings.clinicPhone.ifBlank { "09-421001122, 09-790112233" }
    canvas.drawText("$addr • Ph: $phone", textStartX, 66f, paint)

    // Right Side: Outpatient Info
    paint.textAlign = Paint.Align.RIGHT
    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("OUTPATIENT EMR & Rx", width - 26f, 34f, paint)

    paint.color = Color.rgb(0x42, 0x42, 0x42)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    val hours = printSettings.clinicHours.ifBlank { "8:00 AM - 8:00 PM" }
    canvas.drawText(hours, width - 26f, 48f, paint)

    val regNo = printSettings.clinicRegNo.ifBlank { "MOHS-MDY-2024" }
    canvas.drawText("Reg No: $regNo", width - 26f, 62f, paint)

    paint.textAlign = Paint.Align.LEFT
    return 16f + headerHeight
  }

  private fun drawMiniContinuationHeader(
    canvas: Canvas,
    width: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    printSettings: A4PrintSettings,
    pageNumber: Int = 2
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val clinicTitleEn = printSettings.clinicNameEn.ifBlank { "Shwe Khit General Medicine Clinic" }
    canvas.drawText(clinicTitleEn, 22f, 26f, paint)

    paint.textAlign = Paint.Align.RIGHT
    paint.color = Color.rgb(0x75, 0x75, 0x75)
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Page $pageNumber", width - 22f, 26f, paint)
    paint.textAlign = Paint.Align.LEFT

    paint.color = Color.rgb(0xCB, 0xD5, 0xE1)
    paint.strokeWidth = 0.8f
    canvas.drawLine(22f, 32f, width - 22f, 32f, paint)

    return 40f
  }

  // =========================================================================
  // DEMOGRAPHICS & VITALS DRAWING
  // =========================================================================
  private fun drawPatientDemographics(
    canvas: Canvas,
    width: Float,
    topY: Float,
    patient: Patient?,
    consultation: ConsultationRecord
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val barHeight = 22f

    paint.color = Color.rgb(0xF8, 0xF9, 0xFA)
    canvas.drawRect(20f, topY, width - 20f, topY + barHeight, paint)
    paint.color = Color.rgb(0xE0, 0xE0, 0xE0)
    paint.strokeWidth = 0.8f
    paint.style = Paint.Style.STROKE
    canvas.drawRect(20f, topY, width - 20f, topY + barHeight, paint)
    paint.style = Paint.Style.FILL

    val hn = patient?.hn ?: "HN-2026-0101"
    val name = patient?.name ?: consultation.patientName
    val age = patient?.age ?: 45
    val pGender = patient?.gender?.name
    val gender = if (pGender == "FEMALE") "F" else "M"
    val phone = patient?.phone ?: "09-123456789"
    val date = consultation.timestamp.ifBlank { "2026-10-19" }

    paint.textSize = 9.5f
    val textY = topY + 15f

    // HN
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.rgb(0x61, 0x61, 0x61)
    canvas.drawText("HN: ", 26f, textY, paint)
    paint.typeface = Typeface.DEFAULT
    paint.color = Color.BLACK
    canvas.drawText(hn, 48f, textY, paint)

    // Name
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.rgb(0x61, 0x61, 0x61)
    canvas.drawText("Name: ", 120f, textY, paint)
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    canvas.drawText(name, 155f, textY, paint)

    // Age / Sex
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.rgb(0x61, 0x61, 0x61)
    canvas.drawText("Age/Sex: ", 250f, textY, paint)
    paint.typeface = Typeface.DEFAULT
    paint.color = Color.BLACK
    canvas.drawText("${age}y/$gender", 298f, textY, paint)

    // Date
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.rgb(0x61, 0x61, 0x61)
    canvas.drawText("Date: ", 355f, textY, paint)
    paint.typeface = Typeface.DEFAULT
    paint.color = Color.BLACK
    canvas.drawText(date.take(16), 382f, textY, paint)

    // Phone
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.rgb(0x61, 0x61, 0x61)
    canvas.drawText("Phone: ", 470f, textY, paint)
    paint.typeface = Typeface.DEFAULT
    paint.color = Color.BLACK
    canvas.drawText(phone, 508f, textY, paint)

    return topY + barHeight + 5f
  }

  private fun drawAllergiesBanner(
    canvas: Canvas,
    width: Float,
    topY: Float,
    patient: Patient?
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bannerH = 18f

    val allergies = patient?.knownAllergies?.takeIf { it.isNotEmpty() }
      ?: patient?.allergies?.takeIf { it.isNotEmpty() }

    val isAlert = !allergies.isNullOrEmpty()
    val bgColor = if (isAlert) Color.rgb(0xFE, 0xEB, 0xEE) else Color.rgb(0xFA, 0xFA, 0xFA)
    val borderColor = if (isAlert) Color.rgb(0xEF, 0x9A, 0x9A) else Color.rgb(0xE0, 0xE0, 0xE0)
    val textColor = if (isAlert) Color.rgb(0xC6, 0x28, 0x28) else Color.rgb(0x61, 0x61, 0x61)

    paint.color = bgColor
    canvas.drawRect(20f, topY, width - 20f, topY + bannerH, paint)
    paint.color = borderColor
    paint.strokeWidth = 0.8f
    paint.style = Paint.Style.STROKE
    canvas.drawRect(20f, topY, width - 20f, topY + bannerH, paint)
    paint.style = Paint.Style.FILL

    paint.color = textColor
    paint.textSize = 9.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val text = if (isAlert) "⚠️ DRUG ALLERGIES: ${allergies!!.joinToString(", ")}" else "⚠️ DRUG ALLERGIES: NKDA (No Known Drug Allergies)"
    canvas.drawText(text, 26f, topY + 13f, paint)

    return topY + bannerH + 5f
  }

  private fun drawVitalsRow(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord,
    patient: Patient?
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val vitals = consultation.vitals
    val boxH = 28f
    val spacing = 5f
    val totalW = width - 40f
    val numBoxes = 6
    val boxW = (totalW - (spacing * (numBoxes - 1))) / numBoxes

    val vitalsList = listOf(
      "BLOOD PRESSURE" to vitals.bloodPressure.ifBlank { "120/80" },
      "PULSE RATE" to "${vitals.pulseRate} bpm",
      "TEMPERATURE" to "${String.format(java.util.Locale.US, "%.1f", vitals.temperatureCelsius)} °F",
      "SPO2" to "${vitals.oxygenSaturation}%",
      "WEIGHT" to "${String.format(java.util.Locale.US, "%.1f", vitals.weightKg)} kg",
      "RBS" to (vitals.rbs?.let { if (it > 0) "${it.toInt()} mg/dL" else "-" } ?: "-")
    )

    vitalsList.forEachIndexed { idx, pair ->
      val x = 20f + idx * (boxW + spacing)
      paint.color = Color.rgb(0xF4, 0xF6, 0xF8)
      canvas.drawRoundRect(x, topY, x + boxW, topY + boxH, 4f, 4f, paint)
      paint.color = Color.rgb(0xE2, 0xE8, 0xF0)
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 0.6f
      canvas.drawRoundRect(x, topY, x + boxW, topY + boxH, 4f, 4f, paint)
      paint.style = Paint.Style.FILL

      paint.textAlign = Paint.Align.CENTER
      paint.textSize = 7f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.color = Color.rgb(0x78, 0x90, 0x9C)
      canvas.drawText(pair.first, x + boxW / 2f, topY + 10f, paint)

      paint.textSize = 9.5f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.color = Color.rgb(0x1B, 0x5E, 0x20)
      canvas.drawText(pair.second, x + boxW / 2f, topY + 23f, paint)
    }
    paint.textAlign = Paint.Align.LEFT

    return topY + boxH + 8f
  }

  private fun drawDiagnosisBox(
    canvas: Canvas,
    width: Float,
    topY: Float,
    diagText: String
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(0xF0, 0xFD, 0xF4)
    canvas.drawRoundRect(20f, topY, width - 20f, topY + 26f, 3f, 3f, paint)
    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 0.8f
    canvas.drawRoundRect(20f, topY, width - 20f, topY + 26f, 3f, 3f, paint)
    paint.style = Paint.Style.FILL

    paint.textSize = 9.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    canvas.drawText("Provisional Diagnosis: $diagText", 28f, topY + 17f, paint)

    return topY + 32f
  }

  // =========================================================================
  // CLINICAL FINDINGS (PROFESSIONAL INTERNATIONAL HEADINGS)
  // =========================================================================
  private fun getSubjectiveLines(consultation: ConsultationRecord, patient: Patient?): List<String> {
    val lines = mutableListOf<String>()
    val subText = consultation.subjective.trim()
    val ccText = patient?.chiefComplaint?.trim().orEmpty()

    if (subText.isNotBlank()) {
      subText.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
        lines.add(line)
      }
      // Only include ccText if subjective notes did not already contain it
      if (ccText.isNotBlank() && !subText.contains(ccText, ignoreCase = true)) {
        lines.add(0, ccText)
      }
    } else if (ccText.isNotBlank()) {
      lines.add(ccText)
    }
    return lines
  }

  private fun getObjectiveLines(consultation: ConsultationRecord): List<String> {
    val lines = mutableListOf<String>()
    if (consultation.objective.isNotBlank()) {
      consultation.objective.lines().forEach { line ->
        if (line.isNotBlank()) lines.add(line.trim())
      }
    }
    return lines
  }

  private fun drawTwoColumnClinicalSection(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    maxLines: Int = 12
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val usableW = width - 40f - 12f // 20f margins, 12f centered gap
    val colW = usableW / 2f
    val leftW = colW
    val rightX = 20f + colW + 12f
    val rightW = colW

    var leftY = topY + 13f
    var rightY = topY + 13f

    // Left Column Header
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Chief Complaint & Clinical History:", 22f, leftY, paint)
    leftY += 15f

    val subjectiveLines = getSubjectiveLines(consultation, patient)
    subjectiveLines.take(maxLines).forEach { line ->
      leftY = drawFormattedClinicalLine(canvas, line, 22f, leftY, leftW, paint)
    }

    // Right Column Header
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Physical Examination & Clinical Findings:", rightX, rightY, paint)
    rightY += 15f

    val objectiveLines = getObjectiveLines(consultation)
    objectiveLines.take(maxLines).forEach { line ->
      rightY = drawFormattedClinicalLine(canvas, line, rightX, rightY, rightW, paint)
    }

    // Vertical Divider Line between columns (Exactly 50/50 centered)
    val sectionEndY = maxOf(leftY, rightY) + 4f
    paint.color = Color.rgb(0xE2, 0xE8, 0xF0)
    paint.strokeWidth = 0.8f
    canvas.drawLine(20f + colW + 6f, topY, 20f + colW + 6f, sectionEndY, paint)

    return sectionEndY
  }

  private fun drawSingleColumnClinicalSection(
    canvas: Canvas,
    width: Float,
    topY: Float,
    title: String,
    lines: List<String>,
    maxLines: Int = 12
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var currentY = topY + 13f

    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(title, 22f, currentY, paint)
    currentY += 15f

    val colW = width - 44f
    lines.take(maxLines).forEach { line ->
      currentY = drawFormattedClinicalLine(canvas, line, 22f, currentY, colW, paint)
    }

    return currentY + 4f
  }

  private fun drawExtendedClinicalSection(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord,
    patient: Patient?,
    maxY: Float
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var currentY = topY + 14f

    // Section 1: Chief Complaint & History (International English)
    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 11f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("1. Chief Complaint & Clinical History:", 22f, currentY, paint)
    currentY += 16f

    val subLines = getSubjectiveLines(consultation, patient)
    subLines.forEach { line ->
      if (currentY < maxY - 140f) {
        currentY = drawFormattedClinicalLine(canvas, line, 24f, currentY, width - 44f, paint)
      }
    }

    currentY += 10f

    // Section 2: Physical Examination & Clinical Findings
    val objLines = getObjectiveLines(consultation)
    if (objLines.isNotEmpty() && currentY < maxY - 100f) {
      paint.color = Color.rgb(0x1B, 0x5E, 0x20)
      paint.textSize = 11f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("2. Physical Examination & Clinical Findings:", 22f, currentY, paint)
      currentY += 16f

      objLines.forEach { line ->
        if (currentY < maxY - 60f) {
          currentY = drawFormattedClinicalLine(canvas, line, 24f, currentY, width - 44f, paint)
        }
      }
    }

    // Clinical Diagnosis Box at bottom of Page 1
    val diagText = when {
      consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
      consultation.assessment.isNotBlank() -> consultation.assessment
      else -> ""
    }

    if (diagText.isNotBlank()) {
      currentY += 10f
      currentY = drawDiagnosisBox(canvas, width, currentY, diagText)
    }

    return currentY + 4f
  }

  private fun loadLogoBitmap(logoUri: String): Bitmap? {
    if (logoUri.isBlank()) return null
    return try {
      val file = File(logoUri.removePrefix("file://"))
      if (file.exists() && file.length() > 0) {
        BitmapFactory.decodeFile(file.absolutePath)
      } else if (logoUri.startsWith("data:image") || (!logoUri.startsWith("/") && !logoUri.startsWith("file://"))) {
        val clean = if (logoUri.contains(",")) logoUri.substringAfter(",") else logoUri
        val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun wrapTextIntoLines(text: String, maxWidth: Float, paint: Paint): List<String> {
    if (text.isBlank()) return emptyList()
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()

    for (word in words) {
      if (currentLine.isEmpty()) {
        currentLine.append(word)
      } else {
        val testLine = "${currentLine} $word"
        if (paint.measureText(testLine) <= maxWidth) {
          currentLine.append(" ").append(word)
        } else {
          lines.add(currentLine.toString())
          currentLine = StringBuilder(word)
        }
      }
    }
    if (currentLine.isNotEmpty()) {
      lines.add(currentLine.toString())
    }
    return lines
  }

  private fun drawFormattedClinicalLine(
    canvas: Canvas,
    rawLine: String,
    x: Float,
    y: Float,
    maxWidth: Float,
    paint: Paint
  ): Float {
    val trimmed = rawLine.trim()
    if (trimmed.isBlank()) return y

    // Subheading: catches markdown headings # and bracketed clinical headings like [CHIEF COMPLAINTS & HPI], [BACKGROUND MEDICAL HISTORY], [CLINICAL REMARKS], [GENERAL CONDITION ASSESSMENT], etc.
    val isBracketedSubheading = trimmed.startsWith("[") && trimmed.endsWith("]")
    val isExplicitSubheading = (trimmed.startsWith("###") || trimmed.startsWith("##") || trimmed.startsWith("# ")) ||
      isBracketedSubheading || ((trimmed.startsWith("[HEADING:") || trimmed.startsWith("[SECTION:")) && trimmed.endsWith("]"))

    if (isExplicitSubheading) {
      val title = trimmed
        .removePrefix("[HEADING:")
        .removePrefix("[SECTION:")
        .removePrefix("[")
        .removeSuffix("]")
        .removePrefix("###")
        .removePrefix("##")
        .removePrefix("#")
        .trim()

      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 9.5f
      paint.color = Color.rgb(0x1E, 0x3A, 0x8A) // Bold Navy Blue

      paint.color = Color.rgb(0x25, 0x63, 0xEB) // Accent Blue bullet
      canvas.drawCircle(x + 3f, y - 3f, 2.5f, paint)

      paint.color = Color.rgb(0x1E, 0x3A, 0x8A)
      canvas.drawText(title, x + 10f, y, paint)
      return y + 14f
    } else {
      paint.typeface = Typeface.DEFAULT
      paint.textSize = 8.5f
      paint.color = Color.rgb(0x1E, 0x29, 0x3B)

      val isListItem = trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("→") || trimmed.startsWith("#")
      val cleanContent = if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
        trimmed.substring(1).trim()
      } else {
        trimmed
      }

      val wrappedLines = wrapTextIntoLines(cleanContent, maxWidth - 16f, paint)
      var currentY = y
      wrappedLines.forEachIndexed { index, lineText ->
        val prefix = if (index == 0) {
          if (trimmed.startsWith("→") || trimmed.startsWith("#")) "" else "• "
        } else "  "
        canvas.drawText(prefix + lineText, x + 4f, currentY, paint)
        currentY += 12f
      }
      return currentY + 2f
    }
  }

  // =========================================================================
  // PRESCRIPTIONS (Rx) TABLE DRAWING (CONTINUOUS SLICE SUPPORT)
  // =========================================================================
  private fun drawPrescriptionsTable(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord,
    isExpanded: Boolean = false
  ): Float {
    return drawPrescriptionsTableSlice(
      canvas = canvas,
      width = width,
      topY = topY,
      prescriptions = consultation.prescriptions,
      startIndex = 0
    )
  }

  private fun drawPrescriptionsTableSlice(
    canvas: Canvas,
    width: Float,
    topY: Float,
    prescriptions: List<PrescribedDrug>,
    startIndex: Int = 0
  ): Float {
    if (prescriptions.isEmpty()) return topY
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var currentY = topY + 12f

    // Table Title: Prescription (Rx):
    paint.color = Color.rgb(0x2E, 0x7D, 0x32)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val headerTitle = if (startIndex > 0) "Prescription (Rx) - Continued:" else "Prescription (Rx):"
    canvas.drawText(headerTitle, 22f, currentY, paint)
    currentY += 12f

    // Table Header Bar
    paint.color = Color.rgb(0xF1, 0xF5, 0xF9)
    canvas.drawRect(20f, currentY, width - 20f, currentY + 18f, paint)
    paint.color = Color.rgb(0xCB, 0xD5, 0xE1)
    paint.strokeWidth = 0.8f
    paint.style = Paint.Style.STROKE
    canvas.drawRect(20f, currentY, width - 20f, currentY + 18f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.rgb(0x47, 0x55, 0x69)
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val colY = currentY + 12f
    canvas.drawText("#", 28f, colY, paint)
    canvas.drawText("DRUG & FORMULATION", 52f, colY, paint)
    canvas.drawText("DOSAGE & DIRECTION", 280f, colY, paint)
    canvas.drawText("DURATION", width - 75f, colY, paint)
    currentY += 18f

    // Rows
    prescriptions.forEachIndexed { i, rx ->
      val idx = startIndex + i
      val rowH = 18f
      paint.color = if (i % 2 == 0) Color.WHITE else Color.rgb(0xF8, 0xFA, 0xFC)
      canvas.drawRect(20f, currentY, width - 20f, currentY + rowH, paint)

      paint.color = Color.rgb(0x21, 0x21, 0x21)
      paint.textSize = 8.5f
      val textRowY = currentY + 12f

      // #
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("${idx + 1}", 28f, textRowY, paint)

      // Drug Name & Strength (With [External] tag if external)
      val medName = rx.effectiveName.ifBlank { rx.name.ifBlank { rx.medicineName } }
      val externalPrefix = if (rx.isExternal) "[External] " else ""
      val baseDrug = if (rx.dosage.isNotBlank() && !medName.contains(rx.dosage)) "$medName (${rx.dosage})" else medName
      val fullDrug = "$externalPrefix$baseDrug"
      canvas.drawText(fullDrug.take(45), 52f, textRowY, paint)

      // Dosage & Direction (Include doctor's notes if external)
      paint.typeface = Typeface.DEFAULT
      val directionText = formatDosageAndDirection(rx)
      canvas.drawText(directionText.take(38), 280f, textRowY, paint)

      // Duration
      val durationText = rx.duration.ifBlank { "5 Days" }
      canvas.drawText(durationText, width - 75f, textRowY, paint)

      currentY += rowH
    }

    // Bottom Border of table
    paint.color = Color.rgb(0xCB, 0xD5, 0xE1)
    paint.strokeWidth = 0.8f
    canvas.drawLine(20f, currentY, width - 20f, currentY, paint)

    return currentY + 8f
  }

  private fun drawTreatmentAndInjectionsSection(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord
  ): Float {
    val injectionDrugs = consultation.prescriptions.filter { drug ->
      drug.effectiveName.contains("Inj", ignoreCase = true) ||
      drug.dosage.contains("Inj", ignoreCase = true) ||
      drug.medicineName.contains("Inj", ignoreCase = true) ||
      drug.instructions.contains("IM", ignoreCase = true) ||
      drug.instructions.contains("IV", ignoreCase = true) ||
      drug.instructions.contains("Neb", ignoreCase = true) ||
      drug.instructions.contains("Drip", ignoreCase = true)
    }
    if (injectionDrugs.isEmpty()) return topY

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var currentY = topY

    paint.color = Color.rgb(0x0F, 0x76, 0x6E)
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Treatment & Injections (ဆေးထိုး / ဆေးသွင်း ကုသမှု):", 22f, currentY + 10f, paint)
    currentY += 15f

    injectionDrugs.forEachIndexed { idx, drug ->
      val name = drug.effectiveName.ifBlank { drug.name.ifBlank { drug.medicineName } }
      val route = when {
        drug.instructions.contains("IV", ignoreCase = true) -> "IV Route"
        drug.instructions.contains("IM", ignoreCase = true) -> "IM Route"
        drug.instructions.contains("Neb", ignoreCase = true) -> "Nebulization"
        else -> "Injection"
      }
      paint.color = Color.rgb(0xF8, 0xFA, 0xFC)
      val cardRect = RectF(22f, currentY, width - 22f, currentY + 17f)
      canvas.drawRoundRect(cardRect, 3f, 3f, paint)
      paint.style = Paint.Style.STROKE
      paint.color = Color.rgb(0xCC, 0xF0, 0xEB)
      paint.strokeWidth = 0.6f
      canvas.drawRoundRect(cardRect, 3f, 3f, paint)
      paint.style = Paint.Style.FILL

      paint.color = Color.rgb(0x0F, 0x17, 0x2A)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 8.5f
      canvas.drawText(".  () - ", 28f, currentY + 11.5f, paint)

      paint.color = Color.rgb(0x64, 0x74, 0x8B)
      paint.typeface = Typeface.DEFAULT
      paint.textSize = 8f
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText(" x  â€¢ ", width - 28f, currentY + 11.5f, paint)
      paint.textAlign = Paint.Align.LEFT

      currentY += 20f
    }
    return currentY + 4f
  }

  private fun drawProceduresAndInvestigations(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord
  ): Float {
    if (consultation.selectedProcedures.isEmpty() && consultation.suggestedInvestigations.isEmpty()) {
      return topY
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var currentY = topY

    val validProcs = consultation.selectedProcedures.filter { it.name.isNotBlank() }
    if (validProcs.isNotEmpty()) {
      paint.color = Color.rgb(0x0F, 0x76, 0x6E)
      paint.textSize = 10f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Clinical Procedures & Minor Surgery:", 22f, currentY + 10f, paint)
      currentY += 15f

      validProcs.forEach { proc ->
        val dt = proc.dateTime.ifBlank { consultation.timestamp }
        val hasNotes = proc.notes.isNotBlank()
        val noteText = if (hasNotes) "Notes: ${proc.notes}" else ""

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = Typeface.DEFAULT
          textSize = 8.5f
          color = Color.rgb(0x33, 0x41, 0x55)
        }
        val noteLines = if (hasNotes) {
          wrapTextIntoLines(noteText, width - 68f, notePaint)
        } else emptyList()

        val boxH = if (hasNotes) {
          18f + (noteLines.size * 11.5f) + 6f
        } else {
          17f
        }

        paint.color = Color.rgb(0xF0, 0xFD, 0xFA)
        val boxRect = RectF(22f, currentY, width - 22f, currentY + boxH)
        canvas.drawRoundRect(boxRect, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(0x99, 0xF6, 0xE4)
        paint.strokeWidth = 0.6f
        canvas.drawRoundRect(boxRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Procedure Title & Category
        paint.color = Color.rgb(0x11, 0x5E, 0x59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        val catText = if (proc.category.isNotBlank()) " [" + proc.category + "]" else ""
        canvas.drawText("• " + proc.name + catText, 28f, currentY + 11.5f, paint)

        // Date / Time
        if (dt.isNotBlank()) {
          paint.typeface = Typeface.DEFAULT
          paint.color = Color.rgb(0x64, 0x74, 0x8B)
          paint.textSize = 8f
          paint.textAlign = Paint.Align.RIGHT
          canvas.drawText("Done: " + dt, width - 28f, currentY + 11.5f, paint)
          paint.textAlign = Paint.Align.LEFT
        }

        // Operative / Procedure Notes - drawn cleanly inside the dynamically expanded box
        if (hasNotes && noteLines.isNotEmpty()) {
          var lineY = currentY + 22.5f
          noteLines.forEach { line ->
            canvas.drawText(line, 34f, lineY, notePaint)
            lineY += 11.5f
          }
        }
        currentY += boxH + 4f
      }
      }
    }

    if (consultation.suggestedInvestigations.isNotEmpty()) {
      paint.color = Color.rgb(0x0F, 0x76, 0x6E)
      paint.textSize = 9.5f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Suggested Diagnostic Investigations:", 22f, currentY + 10f, paint)
      currentY += 15f

      paint.typeface = Typeface.DEFAULT
      paint.color = Color.rgb(0x1E, 0x29, 0x3B)
      paint.textSize = 8.5f
      val labs = consultation.suggestedInvestigations
      labs.chunked(2).forEach { rowItems ->
        val textL = "• ${rowItems[0]}"
        canvas.drawText(textL, 28f, currentY + 9f, paint)
        if (rowItems.size > 1) {
          val textR = "• ${rowItems[1]}"
          canvas.drawText(textR, width / 2f + 10f, currentY + 9f, paint)
        }
        currentY += 14f
      }
      currentY += 2f
    }

    return currentY + 4f
  }

  // =========================================================================
  // BOTTOM SECTION: INSTRUCTIONS & FOLLOW-UP
  // =========================================================================
  private fun drawBottomSplitSection(
    canvas: Canvas,
    width: Float,
    topY: Float,
    consultation: ConsultationRecord,
    maxBottomY: Float = 766f
  ): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rawFollowUp = consultation.followUpAppointment.trim()
    val isPrn = rawFollowUp.contains("PRN", ignoreCase = true) || rawFollowUp.contains("မသက်သာမှ")
    val isNone = rawFollowUp.isBlank() || rawFollowUp.equals("None", ignoreCase = true) || rawFollowUp.contains("None", ignoreCase = true) || rawFollowUp.contains("မလို")
    val hasFollowUp = isPrn || (!isNone && !consultation.followUpDueDate.isNullOrBlank()) || (!isNone && rawFollowUp.isNotBlank())

    val instructions = if (consultation.patientInstructions.isNotBlank()) {
      consultation.patientInstructions.lines().filter { it.isNotBlank() }
    } else {
      listOf(
        "၁။ ဆရာဝန်ညွှန်ကြားသည့်အတိုင်း ဆေးများကို အချိန်မှန် ရက်ပြည့်အောင် သောက်သုံးပါ။ ရေများများသောက်ပါ။",
        "၂။ ပဋိဇီဝဆေး (Antibiotics) များကို ရောဂါသက်သာသော်လည်း ရက်ပြည့်အောင် ဆက်သောက်ပါ။",
        "၃။ ဆေးသောက်ပြီး ဓါတ်မတည့်ခြင်း (သို့မဟုတ်) ထူးခြားသော လက္ခဏာများ ခံစားရပါက ဆေးခန်းသို့ ချက်ချင်း ပြန်လည်ပြသပါ။"
      )
    }

    var currentY = topY

    if (!hasFollowUp) {
      // Full Width Patient Instructions & Advice (No fake follow-up box)
      paint.color = Color.rgb(0x2E, 0x7D, 0x32)
      paint.textSize = 9.5f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Patient Instructions & Advice:", 22f, currentY + 10f, paint)

      paint.typeface = Typeface.DEFAULT
      paint.textSize = 8.5f
      paint.color = Color.rgb(0x37, 0x41, 0x51)

      var instY = currentY + 23f
      instructions.forEach { line ->
        if (instY + 11.5f <= maxBottomY) {
          instY = drawWrappedText(canvas, line, 22f, instY, leftW, 11.5f, paint, maxLines = 3)
          instY += 1.5f
        }
      }

      // Right Column: Follow-up & Next Appointment Box
      paint.color = Color.rgb(0x2E, 0x7D, 0x32)
      paint.textSize = 9.5f
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Follow-up & Next Appointment:", rightX, currentY + 10f, paint)

      val followUpStr = if (isPrn) {
        "PRN (မသက်သာမှသာ ပြန်ပြရန်)"
      } else if (rawFollowUp.isNotBlank() && !isNone) {
        rawFollowUp
      } else {
        (consultation.followUpDueDate ?: "")
      }
      val subNote = if (isPrn) {
        "ရောဂါလက္ခဏာ မသက်သာပါက သို့မဟုတ် ပိုဆိုးလာပါက ဆေးခန်းသို့ ပြန်လည်ပြသရန်"
      } else {
        "အဖျား၊ သွေးပေါင်နှင့် ကျန်းမာရေးအခြေအနေ ပြန်လည်စစ်ဆေးရန်"
      }

      val boxTop = currentY + 16f
      val boxBottom = (currentY + 58f).coerceAtMost(maxBottomY)
      if (boxBottom > boxTop + 18f) {
        paint.color = Color.rgb(0xF0, 0xFD, 0xF4)
        canvas.drawRoundRect(rightX, boxTop, rightX + rightW, boxBottom, 4f, 4f, paint)
        paint.color = Color.rgb(0xBB, 0xF7, 0xD0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rightX, boxTop, rightX + rightW, boxBottom, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Safe multi-line wrapping so text never exceeds the right margin
        paint.color = Color.rgb(0x1B, 0x5E, 0x20)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var fuY = drawWrappedText(canvas, "🗓️ ပြန်လည်ပြသရက်: $followUpStr", rightX + 6f, currentY + 28f, rightW - 12f, 11f, paint, maxLines = 2)

        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(0x4B, 0x55, 0x63)
        paint.textSize = 7.5f
        if (fuY + 10f <= maxBottomY) {
          fuY = drawWrappedText(canvas, subNote, rightX + 6f, fuY + 2f, rightW - 12f, 9.5f, paint, maxLines = 2)
        }
      }

      return maxOf(instY + 4f, boxBottom + 6f)
    }
  }

  // =========================================================================
  // FOOTER & DOCTOR SIGNATURE SEAL BLOCK
  // =========================================================================
  private fun drawFooterAndSignature(
    canvas: Canvas,
    width: Float,
    height: Float,
    consultation: ConsultationRecord,
    printSettings: A4PrintSettings,
    pageNumber: Int,
    totalPages: Int,
    docLabel: String = "Attending Physician Signature & Seal"
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val sigY = height - 76f

    // Left Footer: Official Verification Info
    paint.color = Color.rgb(0x75, 0x75, 0x75)
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Official Clinical Consultation Record", 22f, sigY + 12f, paint)

    paint.typeface = Typeface.DEFAULT
    paint.textSize = 7.5f
    val verCode = "SK-${consultation.id.takeLast(8).uppercase()}"
    canvas.drawText("System Verification Code: $verCode", 22f, sigY + 24f, paint)
    canvas.drawText("Official Document • Valid with Physician SAMA Registration", 22f, sigY + 36f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 7f
    paint.color = Color.rgb(0x9E, 0x9E, 0x9E)
    canvas.drawText("CONFIDENTIAL MEDICAL RECORD", 22f, height - 20f, paint)

    // Right Footer: Doctor Credentials & Attending Physician Seal (Pure English as requested)
    val sigRightX = width - 26f
    paint.textAlign = Paint.Align.RIGHT

    val rawDocName = consultation.doctorName.ifBlank { printSettings.doctorName.ifBlank { "Dr. Nyan Kaung Sett" } }
    val cleanDocName = rawDocName.replace("(?i)(,\\s*)?M\\.?B\\.?\\,?\\s*B\\.?S\\.?(.*)".toRegex(), "").trim()
    val docDegree = consultation.doctorDegree.ifBlank { printSettings.doctorDegree.ifBlank { "M.B.,B.S (MDY)" } }
    val rawDocSama = consultation.doctorSama.ifBlank { printSettings.doctorSama.ifBlank { "47414" } }
    val cleanSama = rawDocSama.replace("(?i)^(ဆမ|SAMA)[:\\s-]*".toRegex(), "").trim()

    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(cleanDocName, sigRightX, sigY + 10f, paint)

    paint.color = Color.rgb(0x42, 0x42, 0x42)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(docDegree, sigRightX, sigY + 23f, paint)

    paint.color = Color.rgb(0x1B, 0x5E, 0x20)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("SAMA: $cleanSama", sigRightX, sigY + 36f, paint)

    paint.color = Color.rgb(0x9E, 0x9E, 0x9E)
    paint.textSize = 7.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(docLabel, sigRightX, sigY + 48f, paint)

    // Page Number
    paint.textSize = 8f
    canvas.drawText("Page $pageNumber of $totalPages", sigRightX, height - 20f, paint)

    paint.textAlign = Paint.Align.LEFT
  }

  private fun drawFooterOnly(
    canvas: Canvas,
    width: Float,
    height: Float,
    printSettings: A4PrintSettings,
    pageNumber: Int,
    totalPages: Int
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(0x9E, 0x9E, 0x9E)
    paint.textSize = 7.5f
    val clinicTitleEn = printSettings.clinicNameEn.ifBlank { "Shwe Khit Clinic" }
    canvas.drawText("CONFIDENTIAL MEDICAL RECORD • $clinicTitleEn", 22f, height - 20f, paint)

    paint.textAlign = Paint.Align.RIGHT
    paint.textSize = 8f
    canvas.drawText("Page $pageNumber of $totalPages", width - 26f, height - 20f, paint)
    paint.textAlign = Paint.Align.LEFT
  }

  private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    lineHeight: Float,
    paint: Paint,
    maxLines: Int = 10
  ): Float {
    if (text.isBlank()) return startY
    var y = startY
    val words = text.split(" ")
    var currentLine = StringBuilder()
    var linesDrawn = 0

    for (word in words) {
      val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
      val measure = paint.measureText(testLine)
      if (measure > maxWidth && currentLine.isNotEmpty()) {
        canvas.drawText(currentLine.toString(), x, y, paint)
        y += lineHeight
        linesDrawn++
        if (linesDrawn >= maxLines) break
        currentLine = StringBuilder(word)
      } else {
        currentLine = StringBuilder(testLine)
      }
    }
    if (currentLine.isNotEmpty() && linesDrawn < maxLines) {
      canvas.drawText(currentLine.toString(), x, y, paint)
      y += lineHeight
    }
    return y
  }
}
