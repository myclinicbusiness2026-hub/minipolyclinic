package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// ---------------- REAL A4 SHEET CONTAINER WRAPPER ----------------

/**
 * Standard A4 Paper Container with authentic styling.
 * Enforces responsive printing margins and optional visible printable bounds.
 */
@Composable
fun A4RealPageWrapper(
  marginDp: Dp,
  showMarginGuides: Boolean,
  content: @Composable ColumnScope.() -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(PureWhite)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = (marginDp * 0.75f).coerceIn(8.dp, 16.dp), vertical = (marginDp * 0.75f).coerceIn(8.dp, 16.dp))
    ) {
      // Printable Margin Boundary Guideline
      if (showMarginGuides) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .border(
              width = 1.dp,
              color = Color(0xFF3B82F6).copy(alpha = 0.55f),
              shape = RoundedCornerShape(1.dp)
            )
        )
      }

      // Inside Printable Margins
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        content()
      }
    }
  }
}

// ---------------- A4 PAGE RENDER DISPATCHER ----------------

@Composable
fun RenderA4PageByDescriptor(
  descriptor: A4PageDescriptor,
  pageNumber: Int,
  totalPages: Int,
  consultation: ConsultationRecord,
  patient: Patient?,
  marginDp: Dp,
  showMarginGuides: Boolean,
  isGrayscale: Boolean,
  incLetterhead: Boolean,
  incDemographics: Boolean,
  incCanvas: Boolean,
  incProcedures: Boolean,
  incInvestigations: Boolean = true,
  incAttachments: Boolean = true,
  incRx: Boolean,
  incStamp: Boolean,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  A4RealPageWrapper(marginDp = marginDp, showMarginGuides = showMarginGuides) {
    when (descriptor) {
      is A4PageDescriptor.UnifiedSinglePage -> {
        A4UnifiedSinglePageContent(
          consultation = consultation,
          patient = patient,
          pageNumber = pageNumber,
          totalPages = totalPages,
          isGrayscale = isGrayscale,
          incLetterhead = incLetterhead,
          incDemographics = incDemographics,
          incCanvas = incCanvas,
          incProcedures = incProcedures,
          incInvestigations = incInvestigations,
          incAttachments = incAttachments,
          incRx = incRx,
          incStamp = incStamp,
          printSettings = printSettings
        )
      }
      is A4PageDescriptor.ClinicalConsultationPage -> {
        A4ClinicalConsultationPageContent(
          consultation = consultation,
          patient = patient,
          pageNumber = pageNumber,
          totalPages = totalPages,
          isGrayscale = isGrayscale,
          incLetterhead = incLetterhead,
          incDemographics = incDemographics,
          incCanvas = incCanvas,
          incProcedures = incProcedures,
          incStamp = incStamp,
          pageIndex = descriptor.pageIndex,
          isFirstPage = descriptor.isFirstPage,
          printSettings = printSettings
        )
      }
      is A4PageDescriptor.OfficialPrescriptionPage -> {
        A4OfficialPrescriptionPageContent(
          consultation = consultation,
          patient = patient,
          pageNumber = pageNumber,
          totalPages = totalPages,
          isGrayscale = isGrayscale,
          incLetterhead = incLetterhead,
          incProcedures = incProcedures,
          incInvestigations = incInvestigations,
          incAttachments = incAttachments,
          incRx = incRx,
          incStamp = incStamp,
          printSettings = printSettings
        )
      }
      is A4PageDescriptor.MedicalLeaveCertificatePage -> {
        A4MedicalLeaveCertificatePageContent(
          consultation = consultation,
          patient = patient,
          pageNumber = pageNumber,
          totalPages = totalPages,
          isGrayscale = isGrayscale,
          printSettings = printSettings
        )
      }
      is A4PageDescriptor.ReferralLetterPage -> {
        A4ReferralLetterPageContent(
          consultation = consultation,
          patient = patient,
          pageNumber = pageNumber,
          totalPages = totalPages,
          isGrayscale = isGrayscale,
          printSettings = printSettings
        )
      }
      is A4PageDescriptor.CanvasDrawingPage -> {
        val page = consultation.drawingPages.getOrNull(descriptor.pageIndex)
          ?: DrawingPage(pageIndex = descriptor.pageIndex)
        A4FullCanvasPageContent(
          page = page,
          pageNumber = pageNumber,
          totalPages = totalPages,
          consultation = consultation,
          patient = patient,
          isGrayscale = isGrayscale,
          printSettings = printSettings
        )
      }
    }
  }
}

// ---------------- REUSABLE A4 COMPONENTS FOR DYNAMIC MULTIPAGE LAYOUT ----------------

/**
 * Renders handwriting notes with strict aspect-ratio preservation (no distortion or stretching)
 * using smooth quadratic Bézier curves.
 */
@Composable
fun A4HandwritingNotesCanvasView(
  page: DrawingPage,
  modifier: Modifier = Modifier,
  isGrayscale: Boolean = false
) {
  Box(
    modifier = modifier
      .background(PureWhite)
      .border(0.8.dp, BorderGray)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawA4TemplateGuides(page.template, size)

      // Strict Real Size Rendering with Full Height Preservation (Both upper and lower halves fully visible)
      val baseW = if (page.baseWidth > 0f) page.baseWidth else 800f
      val baseH = if (page.baseHeight > 0f) page.baseHeight else 1131f

      val scale = minOf(size.width / baseW, size.height / baseH)
      val drawnW = baseW * scale
      val drawnH = baseH * scale
      val offsetX = (size.width - drawnW) / 2f
      val offsetY = (size.height - drawnH) / 2f

      // Smooth Quadratic Bézier Curves for natural stylus writing
      page.strokes.forEach { stroke ->
        if (stroke.points.isNotEmpty()) {
          val path = Path()
          val startX = offsetX + stroke.points[0].x * scale
          val startY = offsetY + stroke.points[0].y * scale
          path.moveTo(startX, startY)

          if (stroke.points.size == 1) {
            path.lineTo(startX + 0.5f, startY + 0.5f)
          } else {
            for (i in 1 until stroke.points.size) {
              val prev = stroke.points[i - 1]
              val curr = stroke.points[i]
              val prevX = offsetX + prev.x * scale
              val prevY = offsetY + prev.y * scale
              val currX = offsetX + curr.x * scale
              val currY = offsetY + curr.y * scale
              val midX = (prevX + currX) / 2f
              val midY = (prevY + currY) / 2f
              path.quadraticBezierTo(prevX, prevY, midX, midY)
            }
            val last = stroke.points.last()
            path.lineTo(offsetX + last.x * scale, offsetY + last.y * scale)
          }

          drawPath(
            path = path,
            color = if (isGrayscale) Color.DarkGray else stroke.color,
            style = Stroke(
              width = (stroke.strokeWidth * scale).coerceAtLeast(1.0f),
              cap = StrokeCap.Round,
              join = StrokeJoin.Round
            )
          )
        }
      }
    }

    if (page.strokes.isEmpty()) {
      Text(
        "Clinical handwritten examination notes & findings",
        fontSize = 8.5.sp,
        color = SlateMuted,
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}

/**
 * Compact Patient & Clinic Header for continuation sheets and orders sheets.
 */
@Composable
fun A4CompactPatientHeader(
  consultation: ConsultationRecord,
  patient: Patient?,
  pageNumber: Int,
  totalPages: Int,
  primaryColor: Color,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ShweKhitLogo(
        size = 24.dp,
        customLogoUri = printSettings.clinicLogoUri.ifBlank { null }
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(printSettings.clinicNameMm, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = primaryColor)
        Text("Patient: ${consultation.patientName} • HN: ${patient?.hn ?: "HN-2026-0081"} • Date: ${consultation.timestamp.split(" ").take(2).joinToString(" ")}", fontSize = 8.5.sp, color = SlateMedium)
      }
    }
    Text("Page $pageNumber of $totalPages", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark)
  }
  HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = BorderGray)
}

/**
 * Procedures section: shown ONLY IF procedures are present.
 * Shows Date/Time, Operative Notes, Consumable Items from Pharmacy, and Fee Breakdown.
 */
@Composable
fun A4ProceduresSection(consultation: ConsultationRecord) {
  if (consultation.selectedProcedures.isEmpty()) return
  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)
  Column(modifier = Modifier.fillMaxWidth()) {
    Text("Clinical Procedures & Operative Notes:", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = DarkGrayText)
    Spacer(modifier = Modifier.height(3.dp))
    Column(modifier = Modifier.fillMaxWidth().border(0.8.dp, BorderGray)) {
      consultation.selectedProcedures.forEachIndexed { idx, proc ->
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (idx % 2 == 0) PureWhite else Color(0xFFF9FAFB))
            .padding(horizontal = 6.dp, vertical = 3.5.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "• ${proc.name} [${proc.category}]",
              fontSize = 9.sp,
              color = DarkGrayText,
              fontWeight = FontWeight.Bold
            )
            if (proc.dateTime.isNotBlank()) {
              Text(
                "Date/Time: ${proc.dateTime}",
                fontSize = 8.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (proc.consumableItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              "  ↳ Consumables Used (Pharmacy): " + proc.consumableItems.joinToString(", ") {
                "${it.name} x${it.quantity} ${it.unitType}"
              },
              fontSize = 8.sp,
              color = Color(0xFF1E293B),
              fontWeight = FontWeight.Normal
            )
          }

          if (proc.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
              color = Color(0xFFF1F5F9),
              shape = RoundedCornerShape(2.dp),
              modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) {
              Text(
                "• Operative Note: ${proc.notes}",
                fontSize = 8.sp,
                color = DarkGrayText,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                softWrap = true,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }
        if (idx < consultation.selectedProcedures.size - 1) {
          HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
        }
      }
    }
  }
}

/**
 * Suggested Investigations & Diagnostic Requisition (Doctor's modifiable list).
 * Clean, compact multi-column card layout to conserve paper.
 */
@Composable
fun A4SuggestedInvestigationsSection(
  consultation: ConsultationRecord,
  isGrayscale: Boolean,
  primaryColor: Color = if (isGrayscale) DarkGrayText else MedicalGreen
) {
  if (consultation.suggestedInvestigations.isEmpty()) return
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Default.Science,
        contentDescription = null,
        tint = if (isGrayscale) DarkGrayText else Color(0xFF2563EB),
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text("Suggested Diagnostic & Laboratory Investigations:", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = DarkGrayText)
    }
    Spacer(modifier = Modifier.height(3.dp))
    Column(modifier = Modifier.fillMaxWidth().border(0.8.dp, BorderGray)) {
      consultation.suggestedInvestigations.chunked(2).forEachIndexed { rowIdx, chunk ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (rowIdx % 2 == 0) PureWhite else Color(0xFFF8FAFC))
            .padding(horizontal = 6.dp, vertical = 3.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          chunk.forEach { testName ->
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("[✓] ", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = if (isGrayscale) DarkGrayText else Color(0xFF2563EB))
              Text(testName, fontSize = 8.5.sp, color = DarkGrayText, fontWeight = FontWeight.Medium)
            }
          }
          if (chunk.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

/**
 * Attached Diagnostic Documents & Image Files Section:
 * Displays attached Photo, Camera captures, and PDF documents along with their exact file path.
 */
@Composable
fun A4AttachedDocumentsSection(
  consultation: ConsultationRecord,
  isGrayscale: Boolean,
  primaryColor: Color = if (isGrayscale) DarkGrayText else MedicalGreen
) {
  if (consultation.attachments.isEmpty()) return
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Default.AttachFile,
        contentDescription = null,
        tint = if (isGrayscale) DarkGrayText else Color(0xFF0284C7),
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        "Attached Diagnostic Images & Documents (ဓါတ်မှန်၊ စစ်ဆေးချက် ဖိုင်များ):",
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        color = DarkGrayText
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .border(0.8.dp, BorderGray)
    ) {
      // Header Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (isGrayscale) Color(0xFFE2E8F0) else Color(0xFFE0F2FE))
          .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("No.", modifier = Modifier.width(22.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("Type & Document Title", modifier = Modifier.weight(1.1f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("Attached File Path (ဖိုင်လမ်းကြောင်း)", modifier = Modifier.weight(1.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("Date / Note", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, textAlign = TextAlign.End)
      }
      HorizontalDivider(thickness = 0.5.dp, color = BorderGray)

      consultation.attachments.forEachIndexed { idx, att ->
        val fileIconStr = when (att.fileType.uppercase()) {
          "PDF" -> "📄 PDF"
          "CAMERA" -> "📷 Camera"
          else -> "🖼️ Photo"
        }
        val displayPath = if (att.filePath.isNotBlank()) att.filePath else (if (att.imageUri.isNotBlank()) att.imageUri else "-")

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (idx % 2 == 0) PureWhite else Color(0xFFF8FAFC))
            .padding(horizontal = 6.dp, vertical = 3.5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("${idx + 1}.", modifier = Modifier.width(22.dp), fontSize = 8.sp, color = DarkGrayText, fontWeight = FontWeight.SemiBold)
          Column(modifier = Modifier.weight(1.1f)) {
            Text("$fileIconStr: ${att.title}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            if (att.fileName.isNotBlank() && att.fileName != att.title) {
              Text("File: ${att.fileName}", fontSize = 7.sp, color = SlateMedium)
            }
          }
          Text(
            displayPath,
            modifier = Modifier.weight(1.5f),
            fontSize = 7.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isGrayscale) DarkGrayText else Color(0xFF0369A1),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(att.date, fontSize = 7.5.sp, color = SlateMedium)
            if (att.notes.isNotBlank()) {
              Text(att.notes, fontSize = 7.sp, color = SlateDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
        if (idx < consultation.attachments.size - 1) {
          HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
        }
      }
    }
  }
}

/**
 * Helper to format Dosage and Direction for ordinary patients to easily understand:
 * Strictly avoids TDS, OD, BID, QID and instead uses:
 * (1-0-1), (1-1-1), (1-0-0), (1-1-1-1), (0-0-1)
 * Meal Timing: After meal -> စားပြီး, Before meal -> မစားမီ
 */
fun formatDosageAndDirection(drug: PrescribedDrug): String {
  val freqRaw = drug.frequency.trim()
  val cleanFreq = when {
    freqRaw.contains("1-1-1-1") || freqRaw.contains("qid", ignoreCase = true) || freqRaw.contains("qds", ignoreCase = true) || freqRaw.contains("Four times", ignoreCase = true) -> "1-1-1-1"
    freqRaw.contains("1-1-1") || freqRaw.contains("tds", ignoreCase = true) || freqRaw.contains("tid", ignoreCase = true) || freqRaw.contains("Thrice daily", ignoreCase = true) || freqRaw.contains("Three times", ignoreCase = true) -> "1-1-1"
    freqRaw.contains("1-0-1") || freqRaw.contains("bid", ignoreCase = true) || freqRaw.contains("bd", ignoreCase = true) || freqRaw.contains("Twice daily", ignoreCase = true) -> "1-0-1"
    freqRaw.contains("1-0-0") || freqRaw.contains("od", ignoreCase = true) || freqRaw.contains("Once daily", ignoreCase = true) || freqRaw.contains("Morning", ignoreCase = true) -> "1-0-0"
    freqRaw.contains("0-0-1") || freqRaw.contains("hs", ignoreCase = true) || freqRaw.contains("Bedtime", ignoreCase = true) || freqRaw.contains("Night", ignoreCase = true) -> "0-0-1"
    freqRaw.contains("prn", ignoreCase = true) || freqRaw.contains("As needed", ignoreCase = true) || freqRaw.contains("လိုအပ်") -> "လိုအပ်ပါက"
    freqRaw.startsWith("(") && freqRaw.contains("-") -> freqRaw.removeSurrounding("(", ")")
    else -> {
      val stripped = freqRaw.replace(Regex("(?i)\\b(tds|tid|bid|bd|od|qid|qds)\\b"), "")
        .replace(Regex("""\([^\)]*\)"""), { m -> if (m.value.contains("-")) m.value else "" })
        .replace("()", "")
        .trim()
      if (stripped.isNotBlank()) stripped else "1-0-1"
    }
  }

  val isInjectableOrExternal = drug.effectiveName.contains(Regex("(?i)\\b(inj|iv|im|sc|vial|amp|cream|ointment|drops|eye drops|ear drops)\\b")) ||
    drug.instructions.contains(Regex("(?i)\\b(inj|iv|im|sc|push|drip|infusion)\\b"))

  val instRaw = drug.instructions.trim()
  val cleanTiming = when {
    isInjectableOrExternal && (instRaw.contains("စားပြီး") || instRaw.contains("မစားမီ")) -> ""
    instRaw.contains("after", ignoreCase = true) || instRaw.contains("စားပြီး") -> "စားပြီး"
    instRaw.contains("before", ignoreCase = true) || instRaw.contains("မစားမီ") -> "မစားမီ"
    instRaw.contains("with", ignoreCase = true) || instRaw.contains("အစာနှင့်") -> "အစာနှင့်တွဲ၍"
    instRaw.isBlank() -> ""
    else -> instRaw
  }

  val parts = mutableListOf<String>()
  if (cleanFreq.isNotBlank()) parts.add("($cleanFreq)")
  if (cleanTiming.isNotBlank()) parts.add(cleanTiming)
  return parts.joinToString(" ")
}

/**
 * Comprehensive Prescriptions Table.
 * QTY is omitted per clinical workflow preference, with dosage & directions rendered
 * in (1-0-1) and စားပြီး/မစားမီ format in a compact font to minimize space.
 */
@Composable
fun A4PrescriptionTable(
  consultation: ConsultationRecord,
  isGrayscale: Boolean,
  primaryColor: Color = if (isGrayscale) DarkGrayText else MedicalGreen
) {
  if (consultation.prescriptions.isEmpty()) return
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("℞ ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
      Text("Prescription (Rx):", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = DarkGrayText)
    }
    Spacer(modifier = Modifier.height(3.dp))
    Column(modifier = Modifier.fillMaxWidth().border(0.8.dp, BorderGray)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF1F5F9))
          .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("#", modifier = Modifier.weight(0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("DRUG & FORMULATION", modifier = Modifier.weight(3.1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("DOSAGE & DIRECTION", modifier = Modifier.weight(1.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("DURATION", modifier = Modifier.weight(0.75f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, textAlign = TextAlign.End)
      }
      HorizontalDivider(thickness = 0.8.dp, color = BorderGray)
      consultation.prescriptions.forEachIndexed { idx, drug ->
        val isExt = drug.isExternal || drug.medicineId.startsWith("EXT-")
        val effectiveDrugName = drug.effectiveName.ifBlank { drug.name.ifBlank { drug.medicineName } }
        val baseTitle = if (drug.dosage.isNotBlank() && !effectiveDrugName.contains(drug.dosage, ignoreCase = true)) {
          "${effectiveDrugName} (${drug.dosage})"
        } else {
          effectiveDrugName
        }
        val drugTitle = if (isExt) "$baseTitle  [ပြင်ပဝယ်ရန်/External]" else baseTitle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(if (idx % 2 == 0) PureWhite else Color(0xFFFBFBFB))
            .padding(horizontal = 6.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("${idx + 1}", modifier = Modifier.weight(0.25f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Text(
            drugTitle,
            modifier = Modifier.weight(3.1f),
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            color = DarkGrayText,
            softWrap = true
          )
          Text(
            formatDosageAndDirection(drug),
            modifier = Modifier.weight(1.8f),
            fontSize = 10.sp,
            color = DarkGrayText,
            lineHeight = 16.sp
          )
          Text(
            drug.duration,
            modifier = Modifier.weight(0.75f),
            fontSize = 9.5.sp,
            color = DarkGrayText,
            textAlign = TextAlign.End
          )
        }
        if (idx < consultation.prescriptions.size - 1) {
          HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
        }
      }
    }
  }
}

/**
 * Follow-up Appointment notification block (Burmese with Auto Date/Day and Next Vaccine Schedule).
 */
@Composable
fun A4FollowUpSection(consultation: ConsultationRecord) {
  val formattedFollowUp = com.example.util.BurmeseDateUtils.formatFollowUpForPrint(
    consultation.followUpAppointment,
    consultation.followUpDueDate
  )
  val nextVaccine = consultation.nextVaccineSchedule

  if (formattedFollowUp.isBlank() && nextVaccine.isBlank()) return

  Surface(
    color = Color(0xFFF3E8FF),
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(0.8.dp, Color(0xFFD8B4FE)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      if (formattedFollowUp.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EventRepeat, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "ပြန်လည်ပြသရမည့်ရက် (Next Follow-Up): $formattedFollowUp",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7E22CE)
          )
        }
      }
      if (nextVaccine.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Vaccines, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "ထိုးနှံရမည့် ကာကွယ်ဆေး (Next Vaccine): $nextVaccine",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E40AF)
          )
        }
      }
    }
  }
}

// ---------------- SHEET 1: PATIENT INFORMATION & CLINICAL NOTES ----------------

@Composable
fun ColumnScope.A4ClinicalConsultationPageContent(
  consultation: ConsultationRecord,
  patient: Patient?,
  pageNumber: Int,
  totalPages: Int,
  isGrayscale: Boolean,
  incLetterhead: Boolean,
  incDemographics: Boolean,
  incCanvas: Boolean,
  incProcedures: Boolean,
  incStamp: Boolean,
  pageIndex: Int = 0,
  isFirstPage: Boolean = true,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  val primaryColor = if (isGrayscale) DarkGrayText else MedicalGreen
  val headerDividerColor = if (isGrayscale) BorderGray else AccentRedBrown

  if (isFirstPage) {
    // 1. Patient's Information:
    if (incLetterhead) {
      A4ClinicLetterhead(
        consultation = consultation,
        primaryColor = primaryColor,
        headerDividerColor = headerDividerColor,
        isGrayscale = isGrayscale,
        printSettings = printSettings
      )
    }

    if (incDemographics) {
      A4PatientDemographicsAndVitalsTable(patient = patient, consultation = consultation)
      Spacer(modifier = Modifier.height(6.dp))
    }

    // Chief Complaint
    if (patient?.chiefComplaint?.isNotBlank() == true) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Chief Complaint: ", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = DarkGrayText)
        Text(patient.chiefComplaint, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
      }
    }
  } else {
    // Continuation Sheet: Compact Patient Header
    A4CompactPatientHeader(
      consultation = consultation,
      patient = patient,
      pageNumber = pageNumber,
      totalPages = totalPages,
      primaryColor = primaryColor,
      printSettings = printSettings
    )
  }

  // 2. Clinical notes in handwriting as long as it takes in proper split view not in stretch mode:
  if (incCanvas) {
    val page = consultation.drawingPages.getOrNull(pageIndex)
      ?: DrawingPage(pageIndex = pageIndex)

    Spacer(modifier = Modifier.height(6.dp))
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .border(0.8.dp, BorderGray),
      color = PureWhite
    ) {
      Column(modifier = Modifier.padding(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            if (isFirstPage) "Clinical Notes (History • Examination • Findings):" else "Clinical Notes (Continuation Sheet ${pageIndex + 1}):",
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = DarkGrayText
          )
          Text(
            "Stylus Handwriting • Sheet ${pageIndex + 1}",
            fontSize = 8.sp,
            color = SlateMedium
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        A4HandwritingNotesCanvasView(
          page = page,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          isGrayscale = isGrayscale
        )
      }
    }

    if (page.notes.isNotBlank() || (isFirstPage && consultation.canvasNotes.isNotBlank())) {
      val typed = if (page.notes.isNotBlank()) page.notes else consultation.canvasNotes
      Spacer(modifier = Modifier.height(4.dp))
      Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.6.dp, BorderGray),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Typed Findings: $typed", fontSize = 8.5.sp, color = DarkGrayText, modifier = Modifier.padding(5.dp))
      }
    }

    // Clinical Diagnosis Box (Placed under handwriting medical record - only if recorded)
    if (isFirstPage && (consultation.diagnosisList.isNotEmpty() || consultation.assessment.isNotBlank())) {
      Spacer(modifier = Modifier.height(5.dp))
      Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.8.dp, if (isGrayscale) BorderGray else Color(0xFFA7F3D0)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Provisional Diagnosis: ", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = DarkGrayText)
          val diagText = when {
            consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
            else -> consultation.assessment
          }
          if (diagText.isNotBlank()) {
            Text(diagText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isGrayscale) DarkGrayText else MedicalGreenDark)
          }
        }
      }

      // Attached Diagnostic Documents & Image Files (with file path)
      if (consultation.attachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(5.dp))
        A4AttachedDocumentsSection(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
      }

      // Follow-up Appointment & Next Vaccine Schedule (Burmese with auto date/day calculation)
      if (consultation.followUpAppointment.isNotBlank() || consultation.nextVaccineSchedule.isNotBlank()) {
        Spacer(modifier = Modifier.height(5.dp))
        A4FollowUpSection(consultation = consultation)
      }
    }
  }

  Spacer(modifier = Modifier.height(4.dp))

  // A4 Footer
  A4SheetFooter(
    pageNumber = pageNumber,
    totalPages = totalPages,
    sheetType = if (isFirstPage) "Clinical Consultation Record" else "Clinical Notes Continuation",
    clinicNameMm = printSettings.clinicNameMm
  )
}

// ---------------- SHEET: PROCEDURES, PRESCRIPTIONS & DOCTOR'S ORDERS ----------------

@Composable
fun A4PageTwoContinuationHeader(
  pageNumber: Int,
  totalPages: Int,
  primaryColor: Color,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = printSettings.clinicNameEn.ifBlank { "Shwe Khit General Medicine Clinic" },
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp,
      color = primaryColor
    )
    Text(
      text = "Page $pageNumber of $totalPages",
      fontSize = 9.sp,
      fontWeight = FontWeight.Medium,
      color = SlateMedium
    )
  }
  HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp), thickness = 0.8.dp, color = BorderGray)
}

@Composable
fun ColumnScope.A4OfficialPrescriptionPageContent(
  consultation: ConsultationRecord,
  patient: Patient?,
  pageNumber: Int,
  totalPages: Int,
  isGrayscale: Boolean,
  incLetterhead: Boolean,
  incProcedures: Boolean,
  incInvestigations: Boolean = true,
  incAttachments: Boolean = true,
  incRx: Boolean,
  incStamp: Boolean,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  val primaryColor = if (isGrayscale) DarkGrayText else MedicalGreen

  // Clean Page 2 Header without duplicate demographic or diagnosis boxes
  A4PageTwoContinuationHeader(
    pageNumber = pageNumber,
    totalPages = totalPages,
    primaryColor = primaryColor,
    printSettings = printSettings
  )

  Spacer(modifier = Modifier.height(3.dp))

  // 3. Procedure that is shown IF PRESENT:
  if (incProcedures && consultation.selectedProcedures.isNotEmpty()) {
    A4ProceduresSection(consultation = consultation)
    Spacer(modifier = Modifier.height(5.dp))
  }

  // Suggested Investigations that are shown IF PRESENT:
  if (incInvestigations && consultation.suggestedInvestigations.isNotEmpty()) {
    A4SuggestedInvestigationsSection(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
    Spacer(modifier = Modifier.height(5.dp))
  }

  // Attached Diagnostic Documents & Image Files (with file path):
  if (incAttachments && consultation.attachments.isNotEmpty()) {
    A4AttachedDocumentsSection(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
    Spacer(modifier = Modifier.height(5.dp))
  }

  // 4. Prescriptions:
  if (incRx && consultation.prescriptions.isNotEmpty()) {
    A4PrescriptionTable(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
    Spacer(modifier = Modifier.height(5.dp))
  }

  // Patient Instructions & Precautions Box (Burmese, Configurable by Doctor)
  A4PatientInstructionsBox(instructions = consultation.patientInstructions)

  // 5. Follow up:
  if (consultation.followUpAppointment.isNotBlank() || consultation.nextVaccineSchedule.isNotBlank()) {
    Spacer(modifier = Modifier.height(5.dp))
    A4FollowUpSection(consultation = consultation)
  }

  Spacer(modifier = Modifier.weight(1f))

  // 6. Doctor's signature at the end:
  if (incStamp) {
    A4DoctorSignatureBlock(
      consultation = consultation,
      primaryColor = primaryColor,
      printSettings = printSettings
    )
  }

  // A4 Footer
  A4SheetFooter(
    pageNumber = pageNumber,
    totalPages = totalPages,
    sheetType = "Prescriptions, Procedures & Orders",
    clinicNameMm = printSettings.clinicNameMm
  )
}

// ---------------- DEDICATED FULL A4 CLINICAL CANVAS SHEET ----------------

@Composable
fun ColumnScope.A4FullCanvasPageContent(
  page: DrawingPage,
  pageNumber: Int,
  totalPages: Int,
  consultation: ConsultationRecord,
  patient: Patient?,
  isGrayscale: Boolean,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  val primaryColor = if (isGrayscale) DarkGrayText else MedicalGreen

  A4CompactPatientHeader(
    consultation = consultation,
    patient = patient,
    pageNumber = pageNumber,
    totalPages = totalPages,
    primaryColor = primaryColor,
    printSettings = printSettings
  )

  Spacer(modifier = Modifier.height(6.dp))

  // Full A4 Expansive Drawing Canvas in proper aspect ratio (no stretch!)
  A4HandwritingNotesCanvasView(
    page = page,
    modifier = Modifier
      .fillMaxWidth()
      .weight(1f),
    isGrayscale = isGrayscale
  )

  if (page.notes.isNotBlank() || consultation.canvasNotes.isNotBlank()) {
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
      color = Color(0xFFF8FAFC),
      shape = RoundedCornerShape(4.dp),
      border = BorderStroke(0.8.dp, BorderGray),
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(page.notes.ifBlank { consultation.canvasNotes }, fontSize = 9.sp, color = DarkGrayText, modifier = Modifier.padding(6.dp))
    }
  }

  Spacer(modifier = Modifier.height(4.dp))
  A4SheetFooter(
    pageNumber = pageNumber,
    totalPages = totalPages,
    sheetType = "Clinical Notes Sheet",
    clinicNameMm = printSettings.clinicNameMm
  )
}

// ---------------- UNIFIED SINGLE PAGE MODE (COMPACT & CLEAN) ----------------

@Composable
fun ColumnScope.A4UnifiedSinglePageContent(
  consultation: ConsultationRecord,
  patient: Patient?,
  pageNumber: Int,
  totalPages: Int,
  isGrayscale: Boolean,
  incLetterhead: Boolean,
  incDemographics: Boolean,
  incCanvas: Boolean,
  incProcedures: Boolean,
  incInvestigations: Boolean = true,
  incAttachments: Boolean = true,
  incRx: Boolean,
  incStamp: Boolean,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  val primaryColor = if (isGrayscale) DarkGrayText else MedicalGreen
  val headerDividerColor = if (isGrayscale) BorderGray else AccentRedBrown

  // 1. Patient's Information & Demographics
  if (incLetterhead) {
    A4ClinicLetterhead(
      consultation = consultation,
      primaryColor = primaryColor,
      headerDividerColor = headerDividerColor,
      isGrayscale = isGrayscale,
      printSettings = printSettings
    )
  }

  if (incDemographics) {
    A4PatientDemographicsAndVitalsTable(patient = patient, consultation = consultation)
    Spacer(modifier = Modifier.height(8.dp))
  }

  // Chief Complaint
  if (patient?.chiefComplaint?.isNotBlank() == true) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Chief Complaint: ", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = DarkGrayText)
      Text(patient.chiefComplaint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
    }
  }

  // 2. Clinical Notes in handwriting (shown if present, real size without stretching)
  val firstPage = consultation.drawingPages.firstOrNull() ?: DrawingPage()
  val hasStrokes = firstPage.strokes.isNotEmpty()
  val textNotes = buildString {
    val obj = consultation.objective.trim()
    if (obj.isNotBlank()) append(obj)
    val extra = (if (firstPage.notes.isNotBlank()) firstPage.notes else consultation.canvasNotes).trim()
    if (extra.isNotBlank() && extra != obj && !obj.contains(extra)) {
      if (isNotEmpty()) append("\n")
      append(extra)
    }
  }.trim()
  val hasTextNotes = textNotes.isNotBlank()
  if (incCanvas && (hasStrokes || hasTextNotes)) {
    Spacer(modifier = Modifier.height(8.dp))
    Text("Clinical Notes:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
    Spacer(modifier = Modifier.height(3.dp))
    if (hasStrokes) {
      A4HandwritingNotesCanvasView(
        page = firstPage,
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp),
        isGrayscale = isGrayscale
      )
    }
    if (hasTextNotes) {
      Text("Notes: $textNotes", fontSize = 9.5.sp, color = DarkGrayText, modifier = Modifier.padding(top = 2.dp))
    }
  }

  // Diagnosis Box (Placed under handwriting medical record - only if recorded)
  if (consultation.diagnosisList.isNotEmpty() || consultation.assessment.isNotBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      color = Color(0xFFF8FAFC),
      shape = RoundedCornerShape(4.dp),
      border = BorderStroke(0.8.dp, if (isGrayscale) BorderGray else Color(0xFFA7F3D0)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Provisional Diagnosis: ", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
        val diagText = when {
          consultation.diagnosisList.isNotEmpty() -> consultation.diagnosisList.joinToString(" • ")
          else -> consultation.assessment
        }
        if (diagText.isNotBlank()) {
          Text(diagText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isGrayscale) DarkGrayText else MedicalGreenDark)
        }
      }
    }
  }

  // 3. Procedure that is shown IF PRESENT
  if (incProcedures && consultation.selectedProcedures.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))
    A4ProceduresSection(consultation = consultation)
  }

  // Suggested Investigations that are shown IF PRESENT
  if (incInvestigations && consultation.suggestedInvestigations.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))
    A4SuggestedInvestigationsSection(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
  }

  // Attached Diagnostic Documents & Image Files (with file path)
  if (incAttachments && consultation.attachments.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))
    A4AttachedDocumentsSection(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
  }

  // 4. Prescriptions
  if (incRx && consultation.prescriptions.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))
    A4PrescriptionTable(consultation = consultation, isGrayscale = isGrayscale, primaryColor = primaryColor)
  }

  // Patient Instructions & Precautions Box (Burmese, Configurable by Doctor)
  Spacer(modifier = Modifier.height(6.dp))
  A4PatientInstructionsBox(instructions = consultation.patientInstructions)

  // 5. Follow up
  if (consultation.followUpAppointment.isNotBlank() || consultation.nextVaccineSchedule.isNotBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    A4FollowUpSection(consultation = consultation)
  }

  Spacer(modifier = Modifier.height(12.dp))

  // 6. Doctor's Signature at the end
  if (incStamp) {
    A4DoctorSignatureBlock(
      consultation = consultation,
      primaryColor = primaryColor,
      printSettings = printSettings
    )
  }

  A4SheetFooter(
    pageNumber = pageNumber,
    totalPages = totalPages,
    sheetType = "Complete Clinical Chart & Rx",
    clinicNameMm = printSettings.clinicNameMm
  )
}
