package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Patient
import com.example.model.ReferralRecord
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ReferralFormModal
 *
 * Official Doctor's Referral Letter / Form for transferring patients to
 * General Hospitals, Tertiary Centers, or Specialist Clinics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralFormModal(
  patient: Patient?,
  initialRecord: ReferralRecord?,
  diagnosisDefault: String = "",
  clinicalSummaryDefault: String = "",
  currentMedicationsDefault: String = "",
  onDismiss: () -> Unit,
  onSave: (ReferralRecord) -> Unit,
  onRemove: () -> Unit
) {
  val defaultDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

  var referralDate by remember { mutableStateOf(initialRecord?.referralDate ?: defaultDateStr) }
  var referralTo by remember {
    mutableStateOf(initialRecord?.referralTo ?: "Mandalay General Hospital (Medical Ward)")
  }
  var urgencyLevel by remember { mutableStateOf(initialRecord?.urgencyLevel ?: "Routine") }
  var provisionalDiagnosis by remember {
    mutableStateOf(initialRecord?.provisionalDiagnosis ?: diagnosisDefault.ifBlank { "Acute Condition / For Investigation" })
  }
  var clinicalSummary by remember {
    mutableStateOf(initialRecord?.clinicalSummary ?: clinicalSummaryDefault.ifBlank {
      "May I respectfully refer my patient for your expert evaluation, specialized investigations, and further management."
    })
  }
  var reasonForReferral by remember {
    mutableStateOf(initialRecord?.reasonForReferral ?: "Specialist clinical assessment, advanced diagnostic workup, and appropriate inpatient care.")
  }
  var currentMedications by remember {
    mutableStateOf(initialRecord?.currentMedicationsGiven ?: currentMedicationsDefault)
  }
  var specialInstructions by remember {
    mutableStateOf(initialRecord?.specialInstructions ?: "Please kindly evaluate and provide feedback.")
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 580.dp)
        .padding(10.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEDE7F6)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.ReplyAll,
                contentDescription = null,
                tint = Color(0xFF673AB7),
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "Clinical Referral Letter",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = DarkGrayText
              )
              Text(
                "Official Medical Referral to Hospital / Specialist Consultant",
                fontSize = 9.sp,
                color = SlateMedium
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Patient Demographics Summary Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = SoftLightGray),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                "Patient: ${patient?.name ?: "Patient Name"}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DarkGrayText
              )
              Text(
                "HN: ${patient?.hn ?: "HN-NEW"} • ${patient?.age ?: 0} yrs / ${patient?.gender?.label ?: "Unknown"}",
                fontSize = 10.sp,
                color = SlateMedium
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = PureWhite,
              border = androidx.compose.foundation.BorderStroke(0.7.dp, BorderGray)
            ) {
              Text(
                "BP: ${patient?.vitals?.bloodPressure ?: "120/80"} • HR: ${patient?.vitals?.pulseRate ?: 75}",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MedicalGreenDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Referral Urgency
        Text("Urgency Level:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            "Routine" to Color(0xFF2E7D32),
            "Urgent" to Color(0xFFE65100),
            "Emergency" to Color(0xFFC62828)
          ).forEach { (level, color) ->
            val isSelected = urgencyLevel == level
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable { urgencyLevel = level },
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) color.copy(alpha = 0.12f) else PureWhite,
              border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isSelected) color else BorderGray)
            ) {
              Row(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                  level,
                  fontSize = 10.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) color else DarkGrayText
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Referred To Destination
        Text("Referred To (Attending Specialist / Medical Officer):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = referralTo,
          onValueChange = { referralTo = it },
          placeholder = { Text("e.g. Mandalay General Hospital (Medical Ward), Cardiologist Clinic", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        // Quick Preset Hospitals / Departments
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf(
            "MGH (Med Ward)",
            "MGH (Surg)",
            "300-Bed Children",
            "Specialist OPD"
          ).forEach { preset ->
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = PureWhite,
              border = androidx.compose.foundation.BorderStroke(0.8.dp, BorderGray),
              modifier = Modifier.clickable {
                referralTo = when (preset) {
                  "MGH (Med Ward)" -> "Mandalay General Hospital (Medical Ward)"
                  "MGH (Surg)" -> "Mandalay General Hospital (Surgical Ward / ER)"
                  "300-Bed Children" -> "Mandalay 300-Bedded Children's Hospital"
                  "Specialist OPD" -> "Consultant Specialist OPD Clinic"
                  else -> preset
                }
              }
            ) {
              Text(
                preset,
                fontSize = 8.5.sp,
                color = SlateDark,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Provisional Diagnosis
        Text("Provisional Clinical Diagnosis:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = provisionalDiagnosis,
          onValueChange = { provisionalDiagnosis = it },
          placeholder = { Text("e.g. Acute Coronary Syndrome (ACS), Uncontrolled Type 2 DM with Nephropathy", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Clinical Summary & Findings
        Text("Clinical Summary & Presenting History:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = clinicalSummary,
          onValueChange = { clinicalSummary = it },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp),
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Reason for Referral
        Text("Reason for Referral & Clinical Inquiries:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = reasonForReferral,
          onValueChange = { reasonForReferral = it },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Medications Given / Current Management
        Text("Medications Administered & Emergency Measures:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = currentMedications,
          onValueChange = { currentMedications = it },
          placeholder = { Text("e.g. Aspirin 300mg stat, IV Normal Saline 500ml running, sublingual nitrates", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Special Instructions & Date
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = referralDate,
            onValueChange = { referralDate = it },
            label = { Text("Referral Date", fontSize = 9.5.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 10.5.sp, color = DarkGrayText)
          )

          OutlinedTextField(
            value = specialInstructions,
            onValueChange = { specialInstructions = it },
            label = { Text("Instructions for Transport / Escort", fontSize = 9.5.sp) },
            modifier = Modifier.weight(1.8f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 10.5.sp, color = DarkGrayText)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (initialRecord != null) {
            OutlinedButton(
              onClick = onRemove,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(38.dp)
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Remove", fontSize = 11.sp)
            }
          }

          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
          ) {
            Text("Cancel", fontSize = 11.sp)
          }

          Button(
            onClick = {
              val record = ReferralRecord(
                id = initialRecord?.id ?: java.util.UUID.randomUUID().toString(),
                referralNo = initialRecord?.referralNo ?: "REF-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(1000..9999).random()}",
                patientName = patient?.name ?: "Patient",
                patientAge = patient?.age ?: 0,
                patientGender = patient?.gender?.label ?: "Unknown",
                patientHn = patient?.hn ?: "HN-NEW",
                referralTo = referralTo.trim().ifBlank { "Specialist / General Hospital" },
                urgencyLevel = urgencyLevel,
                provisionalDiagnosis = provisionalDiagnosis.trim(),
                clinicalSummary = clinicalSummary.trim(),
                reasonForReferral = reasonForReferral.trim(),
                currentMedicationsGiven = currentMedications.trim(),
                doctorName = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
                doctorSama = "47414",
                referralDate = referralDate.trim(),
                specialInstructions = specialInstructions.trim()
              )
              onSave(record)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .weight(1.4f)
              .height(38.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (initialRecord != null) "Update Referral" else "Save Referral", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
