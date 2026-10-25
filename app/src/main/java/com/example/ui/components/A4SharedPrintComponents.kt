package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

// ---------------- SHARED REUSABLE A4 COMPONENTS ----------------

@Composable
fun A4ClinicLetterhead(
  consultation: ConsultationRecord,
  primaryColor: Color = MedicalGreen,
  headerDividerColor: Color = BorderGray,
  isGrayscale: Boolean = false,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(3.dp),
    color = Color(0xFFF8F9FA),
    border = BorderStroke(0.8.dp, Color(0xFFE5E7EB))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 3.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Official Clinic Logo + Shwe Khit Clinic Name + Address + Phone
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1.3f)
      ) {
        ShweKhitLogo(
          size = 30.dp,
          customLogoUri = printSettings.clinicLogoUri.ifBlank { null }
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column {
          Text(
            printSettings.clinicNameMm,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.5.sp,
            color = if (isGrayscale) DarkGrayText else MedicalGreenDark,
            maxLines = 1
          )
          Text(
            printSettings.clinicNameEn,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = if (isGrayscale) DarkGrayText else Color(0xFF1B5E20),
            maxLines = 1
          )
          Text(
            "${printSettings.clinicAddress} • Ph: ${printSettings.clinicPhone}",
            fontSize = 8.5.sp,
            color = SlateMedium,
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Right: Clinic Operating Hours & Registration
      Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.weight(0.8f)
      ) {
        Text(
          "OUTPATIENT EMR & Rx",
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp,
          color = if (isGrayscale) SlateDark else MedicalGreenDark,
          maxLines = 1
        )
        if (printSettings.clinicHours.isNotBlank()) {
          Text(
            printSettings.clinicHours,
            fontSize = 9.sp,
            color = SlateMedium,
            maxLines = 1
          )
        }
        if (printSettings.clinicRegNo.isNotBlank()) {
          Text(
            "Reg No: ${printSettings.clinicRegNo}",
            fontSize = 8.5.sp,
            color = SlateLight,
            fontWeight = FontWeight.Medium,
            maxLines = 1
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(3.dp))
}

@Composable
fun A4PatientDemographicsAndVitalsTable(
  patient: Patient?,
  consultation: ConsultationRecord
) {
  val vitals = patient?.vitals
  val rawBp = vitals?.bloodPressure ?: "120/80"
  val cleanBp = rawBp.replace("(?i)\\s*mmhg".toRegex(), "").trim() + " mmHg"

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderGray)
  ) {
    // Row 1: Patient Particulars (Compact single strip)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF8FAFC)),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TableCell(title = "HN", value = patient?.hn ?: "HN-2026-0081", weight = 0.9f)
      TableCell(title = "Name", value = consultation.patientName, weight = 1.3f, isBold = true)
      TableCell(title = "Age/Sex", value = "${patient?.age ?: 34}y/${if (patient?.gender?.name == "FEMALE") "F" else "M"}", weight = 0.8f)
      TableCell(title = "Date", value = consultation.timestamp.split(" ").take(2).joinToString(" "), weight = 0.9f)
      TableCell(title = "Phone", value = patient?.phone ?: "09-421001122", weight = 1.0f)
      if (patient?.allergies.isNullOrEmpty()) {
        TableCell(title = "Allergies", value = "NKDA", weight = 0.8f)
      }
    }

    // Dedicated Full-Width Drug Allergy Bar - fully viewable with softWrap, never truncated
    if (!patient?.allergies.isNullOrEmpty()) {
      HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFFCA5A5))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFEF2F2))
          .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "⚠️ DRUG ALLERGIES: ",
          fontWeight = FontWeight.Bold,
          fontSize = 9.5.sp,
          color = Color(0xFFDC2626)
        )
        Text(
          patient!!.allergies.joinToString(", "),
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp,
          color = Color(0xFFB91C1C),
          softWrap = true
        )
      }
    }

    HorizontalDivider(thickness = 0.8.dp, color = BorderGray)

    // Row 2: Compact Vital Signs Strip (single line ribbon to save vertical space)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF1F5F9))
        .padding(horizontal = 8.dp, vertical = 3.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("BP: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Text(cleanBp, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
      Text("•", fontSize = 8.sp, color = BorderGray)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Pulse: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Text("${vitals?.pulseRate ?: 76} bpm", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
      Text("•", fontSize = 8.sp, color = BorderGray)
      Row(verticalAlignment = Alignment.CenterVertically) {
        val rawTemp = vitals?.temperatureCelsius ?: 36.8
        val tempF = if (rawTemp < 50.0) (rawTemp * 9.0 / 5.0 + 32.0) else rawTemp
        val tempStr = String.format(java.util.Locale.US, "%.1f °F", tempF)
        Text("Temp: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Text(tempStr, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
      Text("•", fontSize = 8.sp, color = BorderGray)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("SpO2: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Text("${vitals?.oxygenSaturation ?: 98}%", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
      Text("•", fontSize = 8.sp, color = BorderGray)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Weight: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Text("${vitals?.weightKg ?: 62.5} kg", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
      if (vitals?.rbs != null) {
        Text("•", fontSize = 8.sp, color = BorderGray)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("RBS: ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
          Text("${vitals.rbs.toInt()} mg/dL", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        }
      }
    }
  }
}

/**
 * Patient Instructions & Precautions Box in Burmese (doctor-configurable)
 */
@Composable
fun A4PatientInstructionsBox(
  instructions: String,
  modifier: Modifier = Modifier
) {
  val defaultBurmese = """၁။ ဆရာဝန်ညွှန်ကြားသည့်အတိုင်း ဆေးများကို အချိန်မှန်၊ ရက်ပြည့်အောင် သောက်သုံးပါ။ ရေများများသောက်ပါ။
၂။ ပဋိဇီဝဆေး (Antibiotics) များကို ရောဂါသက်သာသော်လည်း ရက်ပြည့်အောင် ဆက်သောက်ပါ။
၃။ ဆေးသောက်ပြီး ဓါတ်မတည့်ခြင်း (သို့မဟုတ်) ထူးခြားသော လက္ခဏာများ ခံစားရပါက ဆေးခန်းသို့ ချက်ချင်း ပြန်လည်ပြသပါ။"""

  val displayText = if (instructions.isNotBlank()) instructions else defaultBurmese

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderGray),
    color = Color(0xFFF8FAFC),
    shape = RoundedCornerShape(2.dp)
  ) {
    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.Info,
          contentDescription = null,
          tint = MedicalGreenDark,
          modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          "လူနာညွှန်ကြားချက်နှင့် သတိပြုရန်များ (Patient Instructions & Precautions):",
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp,
          color = DarkGrayText
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = displayText,
        fontSize = 9.5.sp,
        color = DarkGrayText,
        lineHeight = 14.5.sp
      )
    }
  }
}

/**
 * Dedicated Full A4 Medical Leave Form (ဆေးခွင့်လက်မှတ်)
 * Printed ONLY when manually added by the doctor.
 */
@Composable
fun ColumnScope.A4MedicalLeaveCertificatePageContent(
  consultation: ConsultationRecord,
  patient: Patient?,
  pageNumber: Int,
  totalPages: Int,
  isGrayscale: Boolean,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  val primaryColor = if (isGrayscale) DarkGrayText else MedicalGreen
  val headerDividerColor = if (isGrayscale) BorderGray else AccentRedBrown
  val ml = consultation.medicalLeave

  // 1. Clinic Letterhead
  A4ClinicLetterhead(
    consultation = consultation,
    primaryColor = primaryColor,
    headerDividerColor = headerDividerColor,
    isGrayscale = isGrayscale,
    printSettings = printSettings
  )

  Spacer(modifier = Modifier.height(10.dp))

  // Certificate Header Banner
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = if (isGrayscale) Color(0xFFF1F5F9) else SoftGreenContainer,
    border = BorderStroke(1.dp, if (isGrayscale) BorderGray else MedicalGreen.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        "MEDICAL LEAVE CERTIFICATE",
        fontSize = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (isGrayscale) DarkGrayText else MedicalGreenDark,
        letterSpacing = 1.sp
      )
      Text(
        "Official Medical Certificate of Physical Unfitness & Rest Recommendation",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (isGrayscale) SlateDark else Color(0xFF1B5E20)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text("Certificate Ref No.: ${ml?.certificateNo ?: "MC-${consultation.id.take(8).uppercase()}"}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = SlateDark)
        Text("Date of Issue: ${ml?.issueDate ?: consultation.timestamp.split(" ").firstOrNull() ?: ""}", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      }
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  // Patient Particulars Card (Compact)
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderGray),
    color = PureWhite
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text("PATIENT PARTICULARS & IDENTIFICATION:", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = DarkGrayText)
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Patient Name: ${ml?.patientName ?: consultation.patientName}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("Age / Sex: ${ml?.patientAge ?: patient?.age ?: 30} Yrs / ${ml?.patientGender ?: patient?.gender?.name ?: "Male"}", fontSize = 9.sp, color = DarkGrayText)
        Text("Hospital / Clinic HN: ${ml?.patientHn ?: patient?.hn ?: "HN-2026-0081"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = DarkGrayText)
      }
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  // Diagnosis Section
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderGray),
    color = Color(0xFFF8FAFC)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text("CLINICAL DIAGNOSIS & REASON FOR LEAVE:", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = DarkGrayText)
      Spacer(modifier = Modifier.height(4.dp))
      val diag = ml?.diagnosis?.ifBlank {
        consultation.diagnosisList.joinToString(" • ").ifBlank { consultation.assessment.ifBlank { "Acute Medical Condition" } }
      } ?: "Acute Medical Condition"
      Text(diag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isGrayscale) DarkGrayText else MedicalGreenDark)
    }
  }

  Spacer(modifier = Modifier.height(10.dp))

  // Recommendation & Leave Duration
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, if (isGrayscale) BorderGray else MedicalGreen),
    color = PureWhite,
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text("MEDICAL EVALUATION & FITNESS STATUS:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (isGrayscale) DarkGrayText else MedicalGreenDark)
      Spacer(modifier = Modifier.height(6.dp))

      Text(
        "To Whom It May Concern:\nThis is to certify that the patient named above was clinically examined and treated at this facility on ${ml?.issueDate ?: consultation.timestamp.split(" ").firstOrNull() ?: ""}. In my professional medical evaluation, the patient is unfit for official duty, workplace attendance, or academic activities and is advised complete physical rest.",
        fontSize = 8.5.sp,
        color = DarkGrayText,
        lineHeight = 13.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(4.dp)
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Text(
            "Fitness Status: ${ml?.fitnessStatus ?: "Unfit for Duty / Complete Rest Required"}",
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = Color(0xFFB91C1C)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            "Recommended Leave Period: ${ml?.leaveDays ?: 3} Day(s), from ${ml?.startDate ?: ""} to ${ml?.endDate ?: ""}",
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = DarkGrayText
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        "The patient is strongly advised to adhere strictly to prescribed pharmacotherapy and rest during the aforementioned period.",
        fontSize = 8.sp,
        color = SlateDark,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        lineHeight = 12.sp
      )

      if (!ml?.remarks.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text("Physician's Remarks & Advice: ${ml!!.remarks}", fontSize = 8.sp, color = SlateDark)
      }
    }
  }

  Spacer(modifier = Modifier.weight(1f))

  // Doctor Signature & Stamp
  A4DoctorSignatureBlock(
    consultation = consultation,
    primaryColor = primaryColor,
    printSettings = printSettings
  )

  Spacer(modifier = Modifier.height(4.dp))

  A4SheetFooter(
    pageNumber = pageNumber,
    totalPages = totalPages,
    sheetType = "Medical Leave Certificate",
    clinicNameMm = printSettings.clinicNameMm
  )
}

/**
 * Dedicated Full A4 Clinical Referral Letter (လွှဲစာ)
 * Printed when manually requested/added by the doctor.
 */
@Composable
fun ColumnScope.A4ReferralLetterPageContent(
  consultation: ConsultationRecord,
  patient: Patient?,
  pageNumber: Int,
  totalPages: Int,
  isGrayscale: Boolean,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  val primaryColor = if (isGrayscale) DarkGrayText else Color(0xFF1D4ED8)
  val headerDividerColor = if (isGrayscale) BorderGray else AccentRedBrown
  val ref = consultation.referralRecord

  // 1. Clinic Letterhead
  A4ClinicLetterhead(
    consultation = consultation,
    primaryColor = primaryColor,
    headerDividerColor = headerDividerColor,
    isGrayscale = isGrayscale,
    printSettings = printSettings
  )

  Spacer(modifier = Modifier.height(8.dp))

  // Referral Letter Header Banner
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = if (isGrayscale) Color(0xFFF1F5F9) else Color(0xFFEFF6FF),
    border = BorderStroke(1.dp, if (isGrayscale) BorderGray else Color(0xFF3B82F6).copy(alpha = 0.5f)),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 7.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        "OFFICIAL CLINICAL REFERRAL LETTER",
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (isGrayscale) DarkGrayText else Color(0xFF1E3A8A),
        letterSpacing = 1.sp
      )
      Text(
        "Inter-Hospital & Specialist Consultation Requisition",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (isGrayscale) SlateDark else Color(0xFF1D4ED8)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text("Referral Ref No.: ${ref?.referralNo ?: "REF-${consultation.id.take(8).uppercase()}"}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateDark)
        Text("Date: ${ref?.referralDate ?: consultation.timestamp.split(" ").firstOrNull() ?: ""}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        val urgency = ref?.urgencyLevel ?: "Routine"
        val urgencyColor = when (urgency) {
          "Emergency" -> Color(0xFFDC2626)
          "Urgent" -> Color(0xFFD97706)
          else -> Color(0xFF2563EB)
        }
        Text("Urgency: $urgency", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isGrayscale) DarkGrayText else urgencyColor)
      }
    }
  }

  Spacer(modifier = Modifier.height(6.dp))

  // Recipient Box (To: Doctor / Hospital)
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, if (isGrayscale) BorderGray else Color(0xFF93C5FD)),
    color = PureWhite,
    shape = RoundedCornerShape(3.dp)
  ) {
    Column(modifier = Modifier.padding(7.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("TO: ", fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = if (isGrayscale) DarkGrayText else Color(0xFF1D4ED8))
        Text(
          ref?.referralTo?.ifBlank { "Attending Specialist / Medical Officer" } ?: "Attending Specialist / Medical Officer",
          fontWeight = FontWeight.Bold,
          fontSize = 9.5.sp,
          color = DarkGrayText
        )
      }
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        "Dear Colleague / Attending Specialist,\nMay I respectfully refer my patient for your expert clinical evaluation, specialized investigations, and inpatient/tertiary management as deemed appropriate.",
        fontSize = 8.sp,
        color = SlateDark,
        lineHeight = 11.5.sp
      )
    }
  }

  Spacer(modifier = Modifier.height(6.dp))

  // Patient Particulars Strip
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderGray),
    color = Color(0xFFF8FAFC)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Patient: ${ref?.patientName ?: consultation.patientName}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      Text("Age/Sex: ${ref?.patientAge ?: patient?.age ?: 30} Yrs / ${ref?.patientGender ?: patient?.gender?.name ?: "Male"}", fontSize = 8.5.sp, color = DarkGrayText)
      Text("HN: ${ref?.patientHn ?: patient?.hn ?: "HN-2026-0081"}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = SlateDark)
      if (patient?.phone?.isNotBlank() == true) {
        Text("Phone: ${patient.phone}", fontSize = 8.sp, color = SlateDark)
      }
    }
  }

  Spacer(modifier = Modifier.height(6.dp))

  // Working Diagnosis Box
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, if (isGrayscale) BorderGray else Color(0xFFA7F3D0)),
    color = Color(0xFFF0FDF4),
    shape = RoundedCornerShape(3.dp)
  ) {
    Column(modifier = Modifier.padding(7.dp)) {
      Text("PROVISIONAL / WORKING DIAGNOSIS:", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = if (isGrayscale) DarkGrayText else Color(0xFF166534))
      Spacer(modifier = Modifier.height(2.dp))
      val diag = ref?.provisionalDiagnosis?.ifBlank {
        consultation.diagnosisList.joinToString(" • ").ifBlank { consultation.assessment.ifBlank { "Clinical Diagnosis under Investigation" } }
      } ?: "Clinical Diagnosis under Investigation"
      Text(diag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isGrayscale) DarkGrayText else Color(0xFF15803D))
    }
  }

  Spacer(modifier = Modifier.height(6.dp))

  // Reason for Referral Box
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, BorderGray),
    color = PureWhite
  ) {
    Column(modifier = Modifier.padding(7.dp)) {
      Text("REASON FOR REFERRAL & CLINICAL INQUIRIES:", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = DarkGrayText)
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        ref?.reasonForReferral?.ifBlank { "For specialist evaluation, advanced diagnostic workup, and further inpatient management." } ?: "For specialist evaluation and further inpatient management.",
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Medium,
        color = DarkGrayText,
        lineHeight = 12.sp
      )
    }
  }

  Spacer(modifier = Modifier.height(6.dp))

  // Clinical Summary & Examination Findings
  val clinicalSumm = ref?.clinicalSummary?.ifBlank {
    listOfNotNull(
      if (consultation.subjective.isNotBlank()) "History & Presentation: ${consultation.subjective}" else null,
      if (consultation.objective.isNotBlank()) "Physical Examination Findings: ${consultation.objective}" else null,
      if (consultation.plan.isNotBlank()) "Management Plan: ${consultation.plan}" else null
    ).joinToString("\n")
  } ?: ""

  if (clinicalSumm.isNotBlank()) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .border(0.8.dp, BorderGray),
      color = Color(0xFFFAFAFA)
    ) {
      Column(modifier = Modifier.padding(7.dp)) {
        Text("CLINICAL SUMMARY & EXAMINATION FINDINGS:", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = DarkGrayText)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          clinicalSumm,
          fontSize = 8.sp,
          color = DarkGrayText,
          lineHeight = 11.5.sp
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
  }

  // Treatment Given & Current Medications
  val medText = ref?.currentMedicationsGiven?.ifBlank {
    consultation.prescriptions.joinToString("\n") { "• ${it.name} ${it.dosage} - ${it.frequency} (${it.duration})" }
  } ?: ""

  if (medText.isNotBlank()) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .border(0.8.dp, BorderGray),
      color = PureWhite
    ) {
      Column(modifier = Modifier.padding(7.dp)) {
        Text("MEDICATIONS ADMINISTERED & CURRENT TREATMENT REGIMEN:", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = DarkGrayText)
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          medText,
          fontSize = 8.sp,
          color = DarkGrayText,
          lineHeight = 11.5.sp
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
  }

  // Attached Investigations Notice
  if (consultation.suggestedInvestigations.isNotEmpty()) {
    val invText = consultation.suggestedInvestigations.joinToString(", ")
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .border(0.8.dp, BorderGray),
      color = Color(0xFFF8FAFC)
    ) {
      Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Diagnostic Reports & Investigations Attached: ", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = DarkGrayText)
        Text(invText, fontSize = 8.sp, color = SlateDark)
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
  }

  if (!ref?.specialInstructions.isNullOrBlank()) {
    Text("Special Instructions & Transport Notes: ${ref!!.specialInstructions}", fontSize = 7.5.sp, color = SlateDark)
    Spacer(modifier = Modifier.height(4.dp))
  }

  Text(
    "Thank you very much for taking over the care of this patient.\nSincerely,",
    fontSize = 8.sp,
    color = SlateDark,
    lineHeight = 11.sp
  )

  Spacer(modifier = Modifier.weight(1f))

  // Doctor Signature & Stamp
  A4DoctorSignatureBlock(
    consultation = consultation,
    primaryColor = primaryColor,
    printSettings = printSettings
  )

  Spacer(modifier = Modifier.height(4.dp))

  A4SheetFooter(
    pageNumber = pageNumber,
    totalPages = totalPages,
    sheetType = "Official Clinical Referral Letter",
    clinicNameMm = printSettings.clinicNameMm
  )
}

@Composable
fun A4DoctorSignatureBlock(
  consultation: ConsultationRecord,
  primaryColor: Color,
  printSettings: A4PrintSettings = A4PrintSettings()
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom
  ) {
    Column {
      Text("Official Clinical Consultation Record", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = SlateLight)
      Text("System Verification Code: SK-${consultation.id.take(8).uppercase()}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateLight)
      Text("Official Document • Valid with Physician SAMA Registration", fontSize = 8.sp, color = SlateLight)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("________________________________", color = SlateMuted, fontSize = 9.sp)
      val docName = consultation.doctorName.ifBlank { printSettings.doctorName }
      val cleanDocName = docName.replace("(?i)(,\\s*)?M\\.?B\\.?\\,?\\s*B\\.?S\\.?(.*)".toRegex(), "").trim()
      val docDegree = consultation.doctorDegree.ifBlank { printSettings.doctorDegree }
      val docSama = consultation.doctorSama.ifBlank { printSettings.doctorSama }
      val cleanSama = docSama.replace("(?i)^(ဆမ|SAMA)[:\\s-]*".toRegex(), "").trim()
      Text(cleanDocName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      Text(docDegree, fontSize = 9.5.sp, color = SlateMedium)
      Text("SAMA: $cleanSama", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = primaryColor)
      Text("Attending Physician Signature & Seal", fontSize = 8.5.sp, color = SlateMuted)
    }
  }
}

@Composable
fun A4SheetFooter(
  pageNumber: Int,
  totalPages: Int,
  sheetType: String = "",
  clinicNameMm: String = ""
) {
  Spacer(modifier = Modifier.height(6.dp))
  HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
  Spacer(modifier = Modifier.height(4.dp))
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("CONFIDENTIAL MEDICAL RECORD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SlateLight)
    Text("Page $pageNumber of $totalPages", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
  }
}

// ---------------- SUB-COMPONENTS & HELPERS ----------------

@Composable
fun RowScope.TableCell(
  title: String,
  value: String,
  weight: Float,
  isBold: Boolean = false,
  highlightRed: Boolean = false
) {
  Column(
    modifier = Modifier
      .weight(weight)
      .padding(horizontal = 6.dp, vertical = 4.dp)
  ) {
    Text(title, fontSize = 8.5.sp, color = SlateMedium, fontWeight = FontWeight.SemiBold)
    Text(
      value,
      fontSize = 10.5.sp,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
      color = if (highlightRed) Color(0xFFC62828) else DarkGrayText,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun RowScope.VitalCell(
  label: String,
  value: String,
  weight: Float
) {
  Column(
    modifier = Modifier
      .weight(weight)
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(label, fontSize = 8.5.sp, color = SlateMedium, fontWeight = FontWeight.Bold)
    Text(value, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
  }
}

fun DrawScope.drawA4TemplateGuides(template: AnatomyGuideTemplate, size: Size) {
  when (template) {
    AnatomyGuideTemplate.RULED_NOTES -> {
      val lineSpacing = 26.dp.toPx()
      val marginX = 28.dp.toPx()
      // Red clinical left margin rule
      drawLine(
        color = Color(0xFFFFCDD2),
        start = Offset(marginX, 12.dp.toPx()),
        end = Offset(marginX, size.height - 12.dp.toPx()),
        strokeWidth = 1f
      )
      var y = 30.dp.toPx()
      while (y < size.height - 20.dp.toPx()) {
        drawLine(
          color = Color(0xFFE2E8F0),
          start = Offset(16.dp.toPx(), y),
          end = Offset(size.width - 16.dp.toPx(), y),
          strokeWidth = 0.8f
        )
        y += lineSpacing
      }
    }
    AnatomyGuideTemplate.GRID_NOTES -> {
      val gridSpacing = 20.dp.toPx()
      var x = gridSpacing
      while (x < size.width) {
        drawLine(color = Color(0xFFF1F5F9), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 0.5f)
        x += gridSpacing
      }
      var y = gridSpacing
      while (y < size.height) {
        drawLine(color = Color(0xFFF1F5F9), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 0.5f)
        y += gridSpacing
      }
    }
    AnatomyGuideTemplate.BLANK -> {}
  }
}
