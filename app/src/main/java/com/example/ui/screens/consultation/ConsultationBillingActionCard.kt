package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MedicalLeaveCertificate
import com.example.model.ReferralRecord
import com.example.model.DEFAULT_INSTRUCTION_PRESETS
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationBillingActionCard(
  consultationFeeText: String,
  onConsultationFeeChange: (String) -> Unit,
  serviceFeeText: String,
  onServiceFeeChange: (String) -> Unit,
  followUpAppointmentText: String,
  onFollowUpAppointmentChange: (String) -> Unit,
  medicalLeaveState: MedicalLeaveCertificate?,
  onOpenMedicalLeave: () -> Unit,
  referralState: ReferralRecord?,
  onOpenReferral: () -> Unit,
  patientInstructionsState: String,
  onPatientInstructionsChange: (String) -> Unit,
  onOpenInstructions: () -> Unit,
  onPreviewA4Pdf: () -> Unit,
  onSendToPharmacy: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = SkyBlueLight),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Doctor Consultation Fee:", fontSize = 12.sp, color = SlateDark, fontWeight = FontWeight.Bold)
        OutlinedTextField(
          value = consultationFeeText,
          onValueChange = onConsultationFeeChange,
          modifier = Modifier.width(120.dp),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = DarkGrayText),
          suffix = { Text("Ks", fontSize = 10.sp, color = SlateDark) }
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Clinic Service Fee (ဝန်ဆောင်ခ):", fontSize = 11.5.sp, color = SlateDark, fontWeight = FontWeight.Medium)
        OutlinedTextField(
          value = serviceFeeText,
          onValueChange = onServiceFeeChange,
          modifier = Modifier.width(120.dp),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = DarkGrayText),
          suffix = { Text("Ks", fontSize = 10.sp, color = SlateDark) }
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Next Follow-up Appointment Selector
      val context = LocalContext.current
      val openDatePicker = {
        val cal = Calendar.getInstance()
        try {
          val isoMatch = Regex("""\b(\d{4}-\d{2}-\d{2})\b""").find(followUpAppointmentText)
          if (isoMatch != null) {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(isoMatch.value)
            if (parsed != null) cal.time = parsed
          } else {
            val dmyMatch = Regex("""\b(\d{1,2}/\d{1,2}/\d{4})\b""").find(followUpAppointmentText)
            if (dmyMatch != null) {
              val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dmyMatch.value)
              if (parsed != null) cal.time = parsed
            }
          }
        } catch (_: Exception) {}

        android.app.DatePickerDialog(
          context,
          { _, year, month, dayOfMonth ->
            val targetCal = Calendar.getInstance().apply {
              set(Calendar.YEAR, year)
              set(Calendar.MONTH, month)
              set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            val todayCal = Calendar.getInstance()
            val diffDays = ((targetCal.timeInMillis - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            val burmeseDays = mapOf(
              Calendar.SUNDAY to "တနင်္ဂနွေနေ့",
              Calendar.MONDAY to "တနင်္လာနေ့",
              Calendar.TUESDAY to "အင်္ဂါနေ့",
              Calendar.WEDNESDAY to "ဗုဒ္ဓဟူးနေ့",
              Calendar.THURSDAY to "ကြာသပတေးနေ့",
              Calendar.FRIDAY to "သောကြာနေ့",
              Calendar.SATURDAY to "စနေနေ့"
            )
            val dayOfWeek = burmeseDays[targetCal.get(Calendar.DAY_OF_WEEK)] ?: ""
            val dateFormatted = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            val isoFormatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val customDisplay = if (diffDays > 0) {
              "$diffDays ရက်အကြာ • $dateFormatted ($dayOfWeek) - ပြန်လည်စစ်ဆေးရန် [$isoFormatted]"
            } else {
              "$dateFormatted ($dayOfWeek) - ပြန်လည်စစ်ဆေးရန် [$isoFormatted]"
            }
            onFollowUpAppointmentChange(customDisplay)
          },
          cal.get(Calendar.YEAR),
          cal.get(Calendar.MONTH),
          cal.get(Calendar.DAY_OF_MONTH)
        ).show()
      }

      val isPresetSelected = (followUpAppointmentText.contains("၃ ရက်") || followUpAppointmentText.contains("3 Days")) ||
          (followUpAppointmentText.contains("၅ ရက်") || followUpAppointmentText.contains("5 Days")) ||
          (followUpAppointmentText.contains("၁ ပတ်") || followUpAppointmentText.contains("1 Week")) ||
          (followUpAppointmentText.contains("၂ ပတ်") || followUpAppointmentText.contains("2 Weeks")) ||
          (followUpAppointmentText.contains("၃ ပတ်") || followUpAppointmentText.contains("3 Weeks")) ||
          (followUpAppointmentText.contains("၁ လ") || followUpAppointmentText.contains("1 Month")) ||
          (followUpAppointmentText.contains("၂ လ") || followUpAppointmentText.contains("2 Months"))
      val isCustomSelected = followUpAppointmentText.isNotEmpty() && !isPresetSelected

      Text("ရက်ချိန်း ရက်စွဲနှင့် နေ့ရက် (Follow-up Appointment):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      Spacer(modifier = Modifier.height(3.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf(
          "PRN (မသက်သာမှ ပြန်ပြရန်)" to 0,
          "၃ ရက် (3 Days)" to 3,
          "၅ ရက် (5 Days)" to 5,
          "၁ ပတ် (1 Week)" to 7,
          "၂ ပတ် (2 Weeks)" to 14,
          "၃ ပတ် (3 Weeks)" to 21,
          "၁ လ (1 Month)" to 30,
          "၂ လ (2 Months)" to 60
        ).forEach { (label, days) ->
          val isSelected = (days == 0 && (followUpAppointmentText.contains("PRN", ignoreCase = true) || followUpAppointmentText.contains("မသက်သာမှ"))) ||
              (days == 3 && (followUpAppointmentText.contains("၃ ရက်") || followUpAppointmentText.contains("3 Days"))) ||
              (days == 5 && (followUpAppointmentText.contains("၅ ရက်") || followUpAppointmentText.contains("5 Days"))) ||
              (days == 7 && (followUpAppointmentText.contains("၁ ပတ်") || followUpAppointmentText.contains("1 Week"))) ||
              (days == 14 && (followUpAppointmentText.contains("၂ ပတ်") || followUpAppointmentText.contains("2 Weeks"))) ||
              (days == 21 && (followUpAppointmentText.contains("၃ ပတ်") || followUpAppointmentText.contains("3 Weeks"))) ||
              (days == 30 && (followUpAppointmentText.contains("၁ လ") || followUpAppointmentText.contains("1 Month"))) ||
              (days == 60 && (followUpAppointmentText.contains("၂ လ") || followUpAppointmentText.contains("2 Months")))

          FilterChip(
            selected = isSelected,
            onClick = {
              if (days == 0) {
                onFollowUpAppointmentChange("PRN (မသက်သာမှသာပြန်ပြရန်)")
              } else {
                val schedule = com.example.util.BurmeseDateUtils.calculateFollowUp(days, "ပြန်လည်စစ်ဆေးရန်")
                onFollowUpAppointmentChange(schedule.fullDisplayBurmese)
              }
            },
            label = { Text(label, fontSize = 9.5.sp) },
            modifier = Modifier.height(26.dp),
            shape = RoundedCornerShape(4.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MedicalGreen,
              selectedLabelColor = PureWhite
            )
          )
        }

        // Custom Date Picker FilterChip
        FilterChip(
          selected = isCustomSelected,
          onClick = { openDatePicker() },
          leadingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isCustomSelected) PureWhite else MedicalGreen)
          },
          label = { Text("+ ရက်စွဲရွေးမည် (Custom)", fontSize = 9.5.sp, fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal) },
          modifier = Modifier.height(26.dp),
          shape = RoundedCornerShape(4.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MedicalGreen,
            selectedLabelColor = PureWhite,
            containerColor = SoftGreenContainer.copy(alpha = 0.6f),
            labelColor = MedicalGreenDark
          )
        )
      }

      if (followUpAppointmentText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = followUpAppointmentText,
          onValueChange = onFollowUpAppointmentChange,
          label = { Text("ရက်ချိန်း ရက်စွဲနှင့် နေ့ရက် (Doctor Configurable)", fontSize = 9.sp) },
          placeholder = { Text("ဥပမာ - ၁ ပတ်အကြာ • ၁၆/၀၉/၂၀၂၆ (ဗုဒ္ဓဟူးနေ့) - သွေးပေါင်ချိန် ပြန်စစ်ရန်", fontSize = 9.sp) },
          trailingIcon = {
            IconButton(onClick = { openDatePicker() }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Custom Date", tint = MedicalGreen, modifier = Modifier.size(16.dp))
            }
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(4.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = DarkGrayText)
        )
      }

      // Medical Leave & Patient Instructions Quick Controls
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Medical Leave Certificate button/badge
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenMedicalLeave() },
          shape = RoundedCornerShape(6.dp),
          color = if (medicalLeaveState != null) SoftGreenContainer else PureWhite,
          border = BorderStroke(1.dp, if (medicalLeaveState != null) MedicalGreen else BorderGray)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Badge,
              contentDescription = null,
              tint = if (medicalLeaveState != null) MedicalGreen else SlateMedium,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              if (medicalLeaveState != null) "Medical Leave: ${medicalLeaveState.leaveDays}d" else "+ Medical Leave",
              fontSize = 10.sp,
              fontWeight = if (medicalLeaveState != null) FontWeight.Bold else FontWeight.Medium,
              color = if (medicalLeaveState != null) MedicalGreenDark else DarkGrayText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Referral Letter button/badge
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenReferral() },
          shape = RoundedCornerShape(6.dp),
          color = if (referralState != null) Color(0xFFEFF6FF) else PureWhite,
          border = BorderStroke(1.dp, if (referralState != null) Color(0xFF2563EB) else BorderGray)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ForwardToInbox,
              contentDescription = null,
              tint = if (referralState != null) Color(0xFF2563EB) else SlateMedium,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              if (referralState != null) "Referral: ${referralState.referralTo.take(16)}..." else "+ Referral Letter",
              fontSize = 10.sp,
              fontWeight = if (referralState != null) FontWeight.Bold else FontWeight.Medium,
              color = if (referralState != null) Color(0xFF1D4ED8) else DarkGrayText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Patient Instructions button/badge
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenInstructions() },
          shape = RoundedCornerShape(6.dp),
          color = if (patientInstructionsState.isNotBlank()) Color(0xFFE8F5E9) else PureWhite,
          border = BorderStroke(1.dp, if (patientInstructionsState.isNotBlank()) Color(0xFF81C784) else BorderGray)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = if (patientInstructionsState.isNotBlank()) MedicalGreen else SkyBluePrimary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (patientInstructionsState.isNotBlank()) "Instructions (✓ Saved)" else "Instructions (ညွှန်ကြားချက်)",
                fontSize = 10.sp,
                fontWeight = if (patientInstructionsState.isNotBlank()) FontWeight.Bold else FontWeight.Medium,
                color = if (patientInstructionsState.isNotBlank()) MedicalGreenDark else DarkGrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Icon(Icons.Default.Edit, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(12.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Quick Patient Instructions 1-Tap Presets
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Quick ညွှန်ကြားချက်:", fontSize = 8.5.sp, color = SlateMuted, fontWeight = FontWeight.SemiBold)
        listOf(
          "1" to "ဖျားနာ",
          "2" to "သွေးတိုး",
          "3" to "ဆီးချို",
          "4" to "အစာအိမ်",
          "5" to "ချောင်းဆိုး",
          "6" to "ဒဏ်ရာ"
        ).forEach { (code, lbl) ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.clickable {
              val preset = DEFAULT_INSTRUCTION_PRESETS.find { it.code == code }
              if (preset != null) {
                onPatientInstructionsChange(preset.text)
              }
            }
          ) {
            Text("$code.$lbl", fontSize = 8.5.sp, color = DarkGrayText, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onPreviewA4Pdf,
          modifier = Modifier
            .weight(1f)
            .height(38.dp),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Preview A4 PDF", fontSize = 11.sp, maxLines = 1)
        }

        Button(
          onClick = onSendToPharmacy,
          modifier = Modifier
            .weight(1.3f)
            .height(38.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Send to Pharmacy", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}
