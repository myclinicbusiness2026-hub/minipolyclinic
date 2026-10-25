package com.example.ui.screens.consultation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.DrawStroke
import com.example.ui.theme.*

@Composable
fun ClinicalNotesCard(
  patientId: String? = null,
  patientChiefComplaint: String = "",
  subjectiveNotes: String,
  onSubjectiveNotesChange: (String) -> Unit,
  strokes: MutableList<DrawStroke>,
  typedNotes: String,
  onTypedNotesChange: (String) -> Unit,
  pageCount: Int,
  onOpenFullscreenCanvas: () -> Unit,
  onStrokesUpdated: () -> Unit
) {
  var isHistoryExpanded by remember { mutableStateOf(true) }
  var isPhysicalExamExpanded by remember { mutableStateOf(true) }
  var isStylusExpanded by remember { mutableStateOf(strokes.isNotEmpty()) }
  var historyCount by remember(patientId) { mutableIntStateOf(0) }
  var examPinCount by remember(patientId) { mutableIntStateOf(0) }

  val hasHistory = historyCount > 0 || subjectiveNotes.isNotBlank()
  val hasExam = examPinCount > 0 || typedNotes.isNotBlank()
  val hasStrokes = strokes.isNotEmpty()
  val hasAnyContent = hasHistory || hasExam || hasStrokes

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, if (hasAnyContent) Color(0xFFA5D6A7) else BorderGray)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Header with Title & Active Status Badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (hasAnyContent) SoftGreenContainer else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.MedicalServices,
              contentDescription = null,
              tint = if (hasAnyContent) MedicalGreen else SlateMedium,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              "Clinical Documentation (Smart EMR)",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = DarkGrayText
            )
            Text(
              "Cascading History Drill-Down • Interactive Body Map • Stylus Canvas",
              fontSize = 10.sp,
              color = SlateMuted
            )
          }
        }

        // Status Badges
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
          if (hasHistory) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFE8F5E9),
              border = BorderStroke(0.5.dp, Color(0xFF81C784))
            ) {
              Text(
                if (historyCount > 0) "📋 History ($historyCount)" else "📋 History",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreenDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (hasExam) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFE1F5FE),
              border = BorderStroke(0.5.dp, Color(0xFF81D4FA))
            ) {
              Text(
                if (examPinCount > 0) "🩺 Exam ($examPinCount)" else "🩺 Exam",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0277BD),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (hasStrokes) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFFF3E0),
              border = BorderStroke(0.5.dp, Color(0xFFFFB74D))
            ) {
              Text(
                "✍️ Stylus (${strokes.size})",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Sequential Flow Navigation Indicator / Collapse-Expand Pills
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Pill 1: History
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .clickable { isHistoryExpanded = !isHistoryExpanded },
            color = if (isHistoryExpanded) PureWhite else Color.Transparent,
            shadowElevation = if (isHistoryExpanded) 1.dp else 0.dp
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = if (isHistoryExpanded) MedicalGreenDark else SlateMuted, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (historyCount > 0) "1. History ($historyCount)" else "1. History",
                fontSize = 10.5.sp,
                fontWeight = if (isHistoryExpanded) FontWeight.Bold else FontWeight.Medium,
                color = if (isHistoryExpanded) MedicalGreenDark else SlateMedium
              )
              Spacer(modifier = Modifier.width(2.dp))
              Icon(
                if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (isHistoryExpanded) MedicalGreenDark else SlateMuted,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          // Pill 2: Physical Exam
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .clickable { isPhysicalExamExpanded = !isPhysicalExamExpanded },
            color = if (isPhysicalExamExpanded) PureWhite else Color.Transparent,
            shadowElevation = if (isPhysicalExamExpanded) 1.dp else 0.dp
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = if (isPhysicalExamExpanded) Color(0xFF0288D1) else SlateMuted, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (examPinCount > 0) "2. Physical Exam ($examPinCount)" else "2. Physical Exam",
                fontSize = 10.5.sp,
                fontWeight = if (isPhysicalExamExpanded) FontWeight.Bold else FontWeight.Medium,
                color = if (isPhysicalExamExpanded) Color(0xFF0288D1) else SlateMedium
              )
              Spacer(modifier = Modifier.width(2.dp))
              Icon(
                if (isPhysicalExamExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (isPhysicalExamExpanded) Color(0xFF0288D1) else SlateMuted,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          // Pill 3: Stylus
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .clickable { isStylusExpanded = !isStylusExpanded },
            color = if (isStylusExpanded) PureWhite else Color.Transparent,
            shadowElevation = if (isStylusExpanded) 1.dp else 0.dp
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Draw, contentDescription = null, tint = if (isStylusExpanded) Color(0xFFE65100) else SlateMuted, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "3. Stylus Canvas",
                fontSize = 10.5.sp,
                fontWeight = if (isStylusExpanded) FontWeight.Bold else FontWeight.Medium,
                color = if (isStylusExpanded) Color(0xFFE65100) else SlateMedium
              )
              Spacer(modifier = Modifier.width(2.dp))
              Icon(
                if (isStylusExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (isStylusExpanded) Color(0xFFE65100) else SlateMuted,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      // ==================== SECTION 1: CLINICAL HISTORY ====================
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { isHistoryExpanded = !isHistoryExpanded }
            .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("၁။ ရောဂါရာဇဝင်နှင့် မေးမြန်းစစ်ဆေးချက် (Chief Complaints & Clinical History)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasHistory) {
              Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFDCFCE7)) {
                Text(if (historyCount > 0) "✓ $historyCount Complaints" else "✓ Documented", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(18.dp))
          }
        }
      }

      if (isHistoryExpanded) {
        ClinicalHistorySection(
          patientId = patientId,
          patientChiefComplaint = patientChiefComplaint,
          subjectiveNotes = subjectiveNotes,
          onSubjectiveNotesChange = onSubjectiveNotesChange,
          onActiveComplaintsCountChange = { historyCount = it }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
      Spacer(modifier = Modifier.height(10.dp))

      // ==================== SECTION 2: PHYSICAL EXAMINATION ====================
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF0F9FF),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { isPhysicalExamExpanded = !isPhysicalExamExpanded }
            .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("၂။ စနစ်တကျ ခန္ဓာကိုယ် စမ်းသပ်စစ်ဆေးချက် (Physical Examination)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasExam) {
              Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE0F2FE)) {
                Text(if (examPinCount > 0) "✓ $examPinCount Pins" else "✓ Documented", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(if (isPhysicalExamExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
          }
        }
      }

      if (isPhysicalExamExpanded) {
        PhysicalExamSection(
          patientId = patientId,
          typedNotes = typedNotes,
          onTypedNotesChange = onTypedNotesChange,
          onPinFindingsCountChange = { examPinCount = it }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
      Spacer(modifier = Modifier.height(10.dp))

      // ==================== SECTION 3: STYLUS CANVAS ====================
      StylusCanvasSection(
        isStylusExpanded = isStylusExpanded,
        onToggleExpand = { isStylusExpanded = !isStylusExpanded },
        strokes = strokes,
        onStrokesUpdated = onStrokesUpdated,
        onOpenFullscreenCanvas = onOpenFullscreenCanvas
      )
    }
  }
}
