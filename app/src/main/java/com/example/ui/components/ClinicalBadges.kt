package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QueueStatus
import com.example.model.VitalSigns
import com.example.ui.theme.*

@Composable
fun QueueStatusChip(status: QueueStatus) {
  val (bgColor, borderColor, textColor, icon) = when (status) {
    QueueStatus.WAITING -> Quadruple(
      Color(0xFFFFFBEB),
      Color(0xFFFDE68A),
      Color(0xFFB45309),
      Icons.Default.HourglassEmpty
    )
    QueueStatus.IN_CONSULTATION -> Quadruple(
      Color(0xFFE0F2FE),
      Color(0xFF38BDF8),
      Color(0xFF0369A1), // High-contrast crisp blue so text is 100% clear
      Icons.Default.MedicalServices
    )
    QueueStatus.AT_PHARMACY -> Quadruple(
      Color(0xFFCCFBF1),
      Color(0xFF5EEAD4),
      Color(0xFF0F766E),
      Icons.Default.Medication
    )
    QueueStatus.AT_BILLING -> Quadruple(
      Color(0xFFEDE9FE),
      Color(0xFFC4B5FD),
      Color(0xFF6D28D9),
      Icons.Default.PointOfSale
    )
    QueueStatus.COMPLETED -> Quadruple(
      Color(0xFFDCFCE7),
      Color(0xFF86EFAC),
      Color(0xFF15803D),
      Icons.Default.CheckCircle
    )
  }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(13.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = status.displayName,
        color = textColor,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun AllergyWarningBadge(allergies: List<String>) {
  if (allergies.isEmpty()) return

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(StatusCriticalContainer)
      .border(1.dp, StatusCritical.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Icon(
      Icons.Default.Warning,
      contentDescription = "Allergy Warning",
      tint = StatusCritical,
      modifier = Modifier.size(14.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = "Allergies: ${allergies.joinToString(", ")}",
      color = StatusCritical,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun VitalSignsDisplay(vitals: VitalSigns, modifier: Modifier = Modifier) {
  // Ultra-compact single horizontal strip that never steals space from the card
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = Color(0xFFF8FAFC),
    border = BorderStroke(0.8.dp, BorderGray)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 6.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // BP
      CompactVitalChip(
        label = "BP",
        value = vitals.bloodPressure,
        accentColor = if (vitals.bloodPressure.startsWith("16") || vitals.bloodPressure.startsWith("15")) StatusCritical else SlateDark
      )

      // Pulse
      CompactVitalChip(
        label = "HR",
        value = "${vitals.pulseRate} bpm",
        accentColor = SlateDark
      )

      // Temp
      CompactVitalChip(
        label = "Temp",
        value = vitals.formattedTemperatureF,
        accentColor = if (vitals.temperatureFahrenheit >= 99.5) StatusCritical else SlateDark
      )

      // SpO2
      CompactVitalChip(
        label = "SpO2",
        value = "${vitals.oxygenSaturation}%",
        accentColor = if (vitals.oxygenSaturation < 95) StatusCritical else SlateDark
      )

      // Weight
      CompactVitalChip(
        label = "Wt",
        value = "${vitals.weightKg} kg",
        accentColor = SlateDark
      )

      // RBS (Random Blood Sugar) - Highlighted but ultra-compact
      if (vitals.rbs != null) {
        val isCritical = vitals.rbs >= 200.0 || vitals.rbs < 70.0
        val isElevated = vitals.rbs >= 140.0
        val rbsColor = if (isCritical) StatusCritical else if (isElevated) Color(0xFFC2410C) else Color(0xFF15803D)
        val rbsBg = if (isCritical) Color(0xFFFEE2E2) else if (isElevated) Color(0xFFFFEDD5) else Color(0xFFDCFCE7)
        val rbsBorder = if (isCritical) Color(0xFFFCA5A5) else if (isElevated) Color(0xFFFDBA74) else Color(0xFF86EFAC)

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = rbsBg,
          border = BorderStroke(0.8.dp, rbsBorder)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("RBS", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = rbsColor)
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              "${vitals.rbs.toInt()} mg/dL",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              color = rbsColor,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompactVitalChip(
  label: String,
  value: String,
  accentColor: Color
) {
  Surface(
    shape = RoundedCornerShape(4.dp),
    color = PureWhite,
    border = BorderStroke(0.5.dp, BorderGray)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(label, fontSize = 8.5.sp, color = SlateMedium, fontWeight = FontWeight.Medium)
      Spacer(modifier = Modifier.width(2.5.dp))
      Text(value, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1)
    }
  }
}
