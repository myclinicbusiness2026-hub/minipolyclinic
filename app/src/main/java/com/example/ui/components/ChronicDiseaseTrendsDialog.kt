package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Patient
import com.example.model.VitalHistoryPoint
import com.example.ui.theme.*

/**
 * Chronic Disease Trends & Vitals Progression Dialog (ဆီးချို/သွေးတိုး စောင့်ကြည့်ဂရပ်).
 * Displays interactive chronological graphs of Blood Pressure (Systolic/Diastolic),
 * Random Blood Sugar (RBS), and Weight/Pulse to monitor treatment efficacy.
 */
@Composable
fun ChronicDiseaseTrendsDialog(
  patient: Patient,
  vitalHistory: List<VitalHistoryPoint>,
  onDismiss: () -> Unit,
  onAddVitalPoint: (VitalHistoryPoint) -> Unit = {}
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: BP, 1: RBS (Blood Sugar), 2: Weight & Pulse
  var showAddVitalDialog by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f),
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // --- DIALOG HEADER ---
        Surface(
          color = Color(0xFF1E293B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = MedicalGreen,
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.ShowChart, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    "Chronic Disease Vitals & Trends",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF334155)
                  ) {
                    Text(
                      patient.hn,
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
                Text(
                  "${patient.name} (${patient.age}y / ${patient.gender.name}) • ${patient.chiefComplaint}",
                  color = Color(0xFFCBD5E1),
                  fontSize = 10.sp
                )
              }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
            }
          }
        }

        // --- TAB SELECTOR ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val tabs = listOf(
            Triple(0, "Blood Pressure (BP)", Icons.Default.Favorite),
            Triple(1, "Blood Sugar (RBS)", Icons.Default.Bloodtype),
            Triple(2, "Weight & Pulse", Icons.Default.MonitorWeight)
          )

          tabs.forEach { (index, title, icon) ->
            val isSelected = selectedTab == index
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) OceanGreenPrimary else PureWhite,
              border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFCBD5E1)),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedTab = index }
            ) {
              Row(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  icon,
                  contentDescription = null,
                  tint = if (isSelected) PureWhite else SlateMedium,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  title,
                  fontSize = 10.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) PureWhite else DarkGrayText,
                  maxLines = 1
                )
              }
            }
          }
        }

        // --- SCROLLABLE CONTENT BODY ---
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
        ) {
          when (selectedTab) {
            0 -> BloodPressureTrendView(vitalHistory)
            1 -> BloodSugarTrendView(vitalHistory)
            2 -> WeightAndPulseTrendView(vitalHistory)
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Chronological Data Table
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "Chronological Vitals Log (ရက်စွဲအလိုက် စစ်ဆေးမှတ်တမ်း)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp,
                  color = DarkGrayText
                )
                Button(
                  onClick = { showAddVitalDialog = true },
                  shape = RoundedCornerShape(4.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
                  modifier = Modifier.height(26.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("+ Log Reading", fontSize = 9.5.sp)
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Table Headers
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFFF8FAFC))
                  .padding(vertical = 4.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Date", modifier = Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
                Text("BP (mmHg)", modifier = Modifier.weight(1.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
                Text("Pulse", modifier = Modifier.weight(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
                Text("RBS (mg/dL)", modifier = Modifier.weight(1.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
                Text("Weight", modifier = Modifier.weight(0.9f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
                Text("Notes", modifier = Modifier.weight(1.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
              }
              HorizontalDivider(thickness = 0.8.dp, color = BorderGray)

              vitalHistory.forEachIndexed { index, point ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) PureWhite else Color(0xFFFAFAFA))
                    .padding(vertical = 5.dp, horizontal = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(point.date, modifier = Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.Medium, color = DarkGrayText)
                  Text(
                    "${point.bpSystolic}/${point.bpDiastolic}",
                    modifier = Modifier.weight(1.2f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (point.bpSystolic >= 140 || point.bpDiastolic >= 90) StatusCritical else StatusSuccess
                  )
                  Text("${point.pulse} bpm", modifier = Modifier.weight(0.8f), fontSize = 9.sp, color = DarkGrayText)
                  Text(
                    if (point.rbs != null) "${point.rbs.toInt()} mg/dL" else "-",
                    modifier = Modifier.weight(1.2f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if ((point.rbs ?: 0.0) >= 180) StatusCritical else SlateDark
                  )
                  Text(if ((point.weightKg ?: 0.0) > 0) "${point.weightKg} kg" else "-", modifier = Modifier.weight(0.9f), fontSize = 9.sp, color = DarkGrayText)
                  Text(point.notes.ifBlank { "-" }, modifier = Modifier.weight(1.8f), fontSize = 8.5.sp, color = SlateMedium, maxLines = 1)
                }
                if (index < vitalHistory.size - 1) {
                  HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                }
              }
            }
          }
        }

        // --- FOOTER BUTTON ---
        Surface(
          color = PureWhite,
          tonalElevation = 3.dp,
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
              modifier = Modifier.height(34.dp)
            ) {
              Text("Close (ပိတ်မည်)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  if (showAddVitalDialog) {
    AddVitalPointModal(
      onDismiss = { showAddVitalDialog = false },
      onSave = { point ->
        onAddVitalPoint(point)
        showAddVitalDialog = false
      }
    )
  }
}

// --- BLOOD PRESSURE GRAPH COMPOSABLE ---
@Composable
private fun BloodPressureTrendView(vitalHistory: List<VitalHistoryPoint>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Systolic & Diastolic Blood Pressure Curve (mmHg)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
          Text("Goal target: < 130/80 mmHg (AHA/ESC Guideline)", fontSize = 9.sp, color = SlateMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFDC2626), CircleShape))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Systolic", fontSize = 8.5.sp, color = DarkGrayText)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF0284C7), CircleShape))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Diastolic", fontSize = 8.5.sp, color = DarkGrayText)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Canvas Trend Graph
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        if (vitalHistory.size >= 2) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingX = 30f
            val paddingY = 20f

            val minVal = 50f
            val maxVal = 190f
            val range = maxVal - minVal

            val stepX = (width - paddingX * 2) / (vitalHistory.size - 1)

            // Target baseline (130 mmHg)
            val targetY = height - paddingY - ((130f - minVal) / range) * (height - paddingY * 2)
            drawLine(
              color = Color(0xFF10B981).copy(alpha = 0.4f),
              start = Offset(paddingX, targetY),
              end = Offset(width - paddingX, targetY),
              strokeWidth = 2f,
              pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw Systolic line
            val sysPath = Path()
            vitalHistory.forEachIndexed { i, pt ->
              val x = paddingX + i * stepX
              val y = height - paddingY - ((pt.bpSystolic.toFloat() - minVal) / range) * (height - paddingY * 2)
              if (i == 0) sysPath.moveTo(x, y) else sysPath.lineTo(x, y)
              drawCircle(Color(0xFFDC2626), radius = 4.5f, center = Offset(x, y))
            }
            drawPath(sysPath, Color(0xFFDC2626), style = Stroke(width = 3.5f))

            // Draw Diastolic line
            val diaPath = Path()
            vitalHistory.forEachIndexed { i, pt ->
              val x = paddingX + i * stepX
              val y = height - paddingY - ((pt.bpDiastolic.toFloat() - minVal) / range) * (height - paddingY * 2)
              if (i == 0) diaPath.moveTo(x, y) else diaPath.lineTo(x, y)
              drawCircle(Color(0xFF0284C7), radius = 4.5f, center = Offset(x, y))
            }
            drawPath(diaPath, Color(0xFF0284C7), style = Stroke(width = 3.5f))
          }
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Single reading available: ${vitalHistory.firstOrNull()?.bpSystolic}/${vitalHistory.firstOrNull()?.bpDiastolic} mmHg", fontSize = 11.sp, color = SlateMedium)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Status Summary Pills
      val firstPt = vitalHistory.firstOrNull()
      val lastPt = vitalHistory.lastOrNull()
      if (firstPt != null && lastPt != null && vitalHistory.size > 1) {
        val diffSys = lastPt.bpSystolic - firstPt.bpSystolic
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (diffSys <= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
          border = BorderStroke(0.8.dp, if (diffSys <= 0) Color(0xFFA7F3D0) else Color(0xFFFECACA)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              if (diffSys <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
              contentDescription = null,
              tint = if (diffSys <= 0) StatusSuccess else StatusCritical,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (diffSys <= 0) "Clinical Progression: Systolic BP reduced by ${Math.abs(diffSys)} mmHg from baseline. Treatment regimen effective."
              else "Clinical Alert: BP elevated by $diffSys mmHg compared to baseline. Check medication compliance & sodium restriction.",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Medium,
              color = DarkGrayText
            )
          }
        }
      }
    }
  }
}

// --- BLOOD SUGAR (RBS) GRAPH COMPOSABLE ---
@Composable
private fun BloodSugarTrendView(vitalHistory: List<VitalHistoryPoint>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Blood Glucose / RBS Progression (mg/dL)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
          Text("Target: Normal < 140 mg/dL | Pre-diabetes 140-199 mg/dL | Diabetes >= 200 mg/dL", fontSize = 9.sp, color = SlateMedium)
        }
        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFFEF3C7)) {
          Text("Target: <140", fontSize = 8.5.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        val rbsPoints = vitalHistory.filter { it.rbs != null && it.rbs > 0 }
        if (rbsPoints.size >= 2) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingX = 30f
            val paddingY = 20f

            val minVal = 70f
            val maxVal = 300f
            val range = maxVal - minVal

            val stepX = (width - paddingX * 2) / (rbsPoints.size - 1)

            // Target line 140 mg/dL
            val targetY = height - paddingY - ((140f - minVal) / range) * (height - paddingY * 2)
            drawLine(
              color = Color(0xFF10B981),
              start = Offset(paddingX, targetY),
              end = Offset(width - paddingX, targetY),
              strokeWidth = 2f,
              pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            val path = Path()
            rbsPoints.forEachIndexed { i, pt ->
              val rbsVal = pt.rbs!!.toFloat()
              val x = paddingX + i * stepX
              val y = height - paddingY - ((rbsVal - minVal) / range) * (height - paddingY * 2)
              if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
              val ptColor = if (rbsVal >= 200) Color(0xFFDC2626) else if (rbsVal >= 140) Color(0xFFEA580C) else Color(0xFF10B981)
              drawCircle(ptColor, radius = 5f, center = Offset(x, y))
            }
            drawPath(path, Color(0xFFEA580C), style = Stroke(width = 3.5f))
          }
        } else {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No multiple glucose readings recorded yet.", fontSize = 11.sp, color = SlateMedium)
          }
        }
      }
    }
  }
}

// --- WEIGHT & PULSE VIEW ---
@Composable
private fun WeightAndPulseTrendView(vitalHistory: List<VitalHistoryPoint>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text("Body Weight (kg) & Resting Pulse Rate (bpm)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
      Spacer(modifier = Modifier.height(6.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vitalHistory.takeLast(4).forEach { pt ->
          Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(pt.date, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
              Spacer(modifier = Modifier.height(2.dp))
              Text("${pt.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Text("${pt.pulse} bpm", fontSize = 9.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

// --- MODAL TO ADD A VITAL READING ---
@Composable
private fun AddVitalPointModal(
  onDismiss: () -> Unit,
  onSave: (VitalHistoryPoint) -> Unit
) {
  var systolicStr by remember { mutableStateOf("") }
  var diastolicStr by remember { mutableStateOf("") }
  var pulseStr by remember { mutableStateOf("") }
  var rbsStr by remember { mutableStateOf("") }
  var weightStr by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth().padding(10.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("Log New Inter-Visit Vital Check", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = systolicStr,
            onValueChange = { systolicStr = it },
            label = { Text("Systolic BP", fontSize = 10.sp) },
            placeholder = { Text("120", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
          OutlinedTextField(
            value = diastolicStr,
            onValueChange = { diastolicStr = it },
            label = { Text("Diastolic BP", fontSize = 10.sp) },
            placeholder = { Text("80", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = pulseStr,
            onValueChange = { pulseStr = it },
            label = { Text("Pulse (bpm)", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
          OutlinedTextField(
            value = rbsStr,
            onValueChange = { rbsStr = it },
            label = { Text("RBS (mg/dL)", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
          OutlinedTextField(
            value = weightStr,
            onValueChange = { weightStr = it },
            label = { Text("Weight (kg)", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Checkup Note / Setting (e.g. Home BP, Pharmacy Check)", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel", color = SlateMedium, fontSize = 11.sp) }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              val sys = systolicStr.toIntOrNull() ?: 120
              val dia = diastolicStr.toIntOrNull() ?: 80
              val pr = pulseStr.toIntOrNull() ?: 76
              val rbs = rbsStr.toDoubleOrNull()
              val wt = weightStr.toDoubleOrNull() ?: 0.0
              val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
              onSave(VitalHistoryPoint(date = todayStr, bpSystolic = sys, bpDiastolic = dia, pulse = pr, rbs = rbs, weightKg = wt, notes = notes))
            },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
          ) {
            Text("Save Vital Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
