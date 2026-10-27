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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhysicalExamSection(
  patientId: String?,
  typedNotes: String,
  onTypedNotesChange: (String) -> Unit,
  onPinFindingsCountChange: (Int) -> Unit = {}
) {
  var peSubTab by remember(patientId) { mutableIntStateOf(0) } // 0: General Condition, 1: Systemic & Body Map, 2: Neurological, 3: Trauma & Wound

  val pinFindings = remember(patientId) { mutableStateMapOf<String, String>() }
  var editingPin by remember { mutableStateOf<AnatomicalPin?>(null) }
  var showFreeTextExamDialog by remember { mutableStateOf(false) }
  var examSaveFeedback by remember { mutableStateOf<String?>(null) }

  // General Condition Assessment State
  var genAppearance by remember(patientId) { mutableStateOf<String?>("Alert, conscious & oriented") }
  var genBuildNutrition by remember(patientId) { mutableStateOf<String?>("Well-nourished") }
  var genPallor by remember(patientId) { mutableStateOf("No pallor (-)") }
  var genJaundice by remember(patientId) { mutableStateOf("No jaundice (-)") }
  var genCyanosis by remember(patientId) { mutableStateOf("No cyanosis (-)") }
  var genClubbing by remember(patientId) { mutableStateOf("No clubbing (-)") }
  var genEdema by remember(patientId) { mutableStateOf("No pedal edema (-)") }
  var genLAP by remember(patientId) { mutableStateOf("No palpable LAP (-)") }
  var genHydration by remember(patientId) { mutableStateOf("Moist mucous membranes, good skin turgor") }
  var genPerfusion by remember(patientId) { mutableStateOf("Capillary refill < 2s (Normal)") }
  var genCustomNotes by remember(patientId) { mutableStateOf("") }

  // Neurological Assessment State
  var hasNeuroExam by remember(patientId) { mutableStateOf(false) }
  var neuroGcs by remember(patientId) { mutableStateOf("") }
  var neuroOrientation by remember(patientId) { mutableStateOf("") }
  var neuroPupils by remember(patientId) { mutableStateOf("") }
  var neuroCranialNerves by remember(patientId) { mutableStateOf("") }
  var neuroMotorPower by remember(patientId) { mutableStateOf("") }
  var neuroReflexes by remember(patientId) { mutableStateOf("") }
  var neuroPlantar by remember(patientId) { mutableStateOf("") }
  var neuroMeningeal by remember(patientId) { mutableStateOf("") }
  var neuroGait by remember(patientId) { mutableStateOf("") }
  var neuroCustomNotes by remember(patientId) { mutableStateOf("") }

  // Trauma & Wound Assessment State
  var hasTraumaPresent by remember(patientId) { mutableStateOf(false) }
  val traumaWoundTypes = remember(patientId) { mutableStateListOf<String>() }
  var traumaSite by remember(patientId) { mutableStateOf("") }
  var traumaDimensions by remember(patientId) { mutableStateOf("") }
  var traumaBleeding by remember(patientId) { mutableStateOf("") }
  var traumaContamination by remember(patientId) { mutableStateOf("") }
  val traumaInfectionSigns = remember(patientId) { mutableStateListOf<String>() }
  var traumaNeurovascular by remember(patientId) { mutableStateOf("") }
  var traumaCustomNotes by remember(patientId) { mutableStateOf("") }

  // Notify parent of pin findings count
  LaunchedEffect(pinFindings.size) {
    onPinFindingsCountChange(pinFindings.size)
  }

  fun syncExamFindings(showSavedBadge: Boolean = false) {
    val sections = mutableListOf<String>()

    // 1. General Condition Assessment
    val genLines = mutableListOf<String>()
    if (!genAppearance.isNullOrBlank()) genLines.add("• Appearance: $genAppearance")
    if (!genBuildNutrition.isNullOrBlank()) genLines.add("• Build & Nutrition: $genBuildNutrition")
    val cardinalSigns = listOfNotNull(
      genPallor.ifBlank { null },
      genJaundice.ifBlank { null },
      genCyanosis.ifBlank { null },
      genClubbing.ifBlank { null },
      genEdema.ifBlank { null },
      genLAP.ifBlank { null }
    )
    if (cardinalSigns.isNotEmpty()) {
      genLines.add("• 6 Cardinal Signs: ${cardinalSigns.joinToString(", ")}")
    }
    if (genHydration.isNotBlank() || genPerfusion.isNotBlank()) {
      genLines.add("• Perfusion & Hydration: ${listOfNotNull(genHydration.ifBlank { null }, genPerfusion.ifBlank { null }).joinToString(", ")}")
    }
    if (genCustomNotes.isNotBlank()) {
      genLines.add("• Remarks: ${genCustomNotes.trim()}")
    }
    if (genLines.isNotEmpty()) {
      sections.add("[GENERAL CONDITION ASSESSMENT]\n" + genLines.joinToString("\n"))
    }

    // 2. Systemic & Anatomical Pins
    val pinLines = mutableListOf<String>()
    CLINIC_BODY_PINS.forEach { pin ->
      val finding = pinFindings[pin.id]
      if (!finding.isNullOrBlank()) {
        pinLines.add("• ${pin.nameEn}: $finding")
      }
    }
    if (pinLines.isNotEmpty()) {
      sections.add("[SYSTEMIC & ANATOMICAL EXAM]\n" + pinLines.joinToString("\n"))
    }

    // 3. Neurological Assessment
    val neuroLines = mutableListOf<String>()
    if (neuroGcs.isNotBlank()) {
      val oriPart = if (neuroOrientation.isNotBlank()) " ($neuroOrientation)" else ""
      neuroLines.add("• Consciousness: $neuroGcs$oriPart")
    } else if (neuroOrientation.isNotBlank()) {
      neuroLines.add("• Orientation: $neuroOrientation")
    }
    if (neuroPupils.isNotBlank() || neuroCranialNerves.isNotBlank()) {
      neuroLines.add("• Pupils & Cranial Nerves: ${listOfNotNull(neuroPupils.ifBlank { null }, neuroCranialNerves.ifBlank { null }).joinToString(", ")}")
    }
    if (neuroMotorPower.isNotBlank()) neuroLines.add("• Motor System & Tone: $neuroMotorPower")
    if (neuroReflexes.isNotBlank() || neuroPlantar.isNotBlank()) {
      neuroLines.add("• Reflexes & Plantar: ${listOfNotNull(neuroReflexes.ifBlank { null }, neuroPlantar.ifBlank { null }).joinToString(", ")}")
    }
    if (neuroMeningeal.isNotBlank()) neuroLines.add("• Meningeal Signs: $neuroMeningeal")
    if (neuroGait.isNotBlank()) neuroLines.add("• Gait & Coordination: $neuroGait")
    if (neuroCustomNotes.isNotBlank()) neuroLines.add("• Neuro Remarks: ${neuroCustomNotes.trim()}")
    if (neuroLines.isNotEmpty() && (hasNeuroExam || neuroGcs.isNotBlank() || neuroCustomNotes.isNotBlank() || neuroPupils.isNotBlank())) {
      sections.add("[NEUROLOGICAL ASSESSMENT]\n" + neuroLines.joinToString("\n"))
    }

    // 4. Trauma & Wound Assessment
    if (hasTraumaPresent || traumaWoundTypes.isNotEmpty() || traumaSite.isNotBlank() || traumaCustomNotes.isNotBlank()) {
      val trLines = mutableListOf<String>()
      if (traumaWoundTypes.isNotEmpty()) trLines.add("• Wound Classification: ${traumaWoundTypes.joinToString(", ")}")
      if (traumaSite.isNotBlank() || traumaDimensions.isNotBlank()) {
        trLines.add("• Location & Dimensions: ${listOfNotNull(traumaSite.ifBlank { null }, traumaDimensions.ifBlank { null }).joinToString(" - ")}")
      }
      if (traumaBleeding.isNotBlank() || traumaContamination.isNotBlank()) {
        trLines.add("• Hemostasis & Condition: ${listOfNotNull(traumaBleeding.ifBlank { null }, traumaContamination.ifBlank { null }).joinToString(", ")}")
      }
      if (traumaNeurovascular.isNotBlank()) trLines.add("• Neurovascular & Tendons: $traumaNeurovascular")
      if (traumaInfectionSigns.isNotEmpty()) trLines.add("• Infection Signs: ${traumaInfectionSigns.joinToString(", ")}")
      if (traumaCustomNotes.isNotBlank()) trLines.add("• Wound Remarks: ${traumaCustomNotes.trim()}")
      if (trLines.isNotEmpty()) {
        sections.add("[TRAUMA & WOUND ASSESSMENT]\n" + trLines.joinToString("\n"))
      }
    }

    val compiled = sections.joinToString("\n\n")
    onTypedNotesChange(compiled)

    if (showSavedBadge) {
      examSaveFeedback = "Physical Exam Findings Saved to Record ✓"
    }
  }

  Column(modifier = Modifier.padding(top = 8.dp)) {
    // Action row: 1-Tap Normal All, Clear, and Free Text Notes
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          "Structured Physical Examination (စနစ်တကျ စမ်းသပ်စစ်ဆေးချက်):",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = SlateDark
        )
        Text("General Condition • Systems • Neurological • Trauma & Wound", fontSize = 9.sp, color = SlateMuted)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(
          onClick = {
            // 1. Normal General Condition
            genAppearance = "Alert, conscious & oriented, comfortable"
            genBuildNutrition = "Well-nourished, adequate build"
            genPallor = "No pallor (-)"
            genJaundice = "No jaundice (-)"
            genCyanosis = "No cyanosis (-)"
            genClubbing = "No clubbing (-)"
            genEdema = "No pedal edema (-)"
            genLAP = "No palpable LAP (-)"
            genHydration = "Moist mucous membranes, good skin turgor"
            genPerfusion = "Capillary refill < 2s (Normal)"

            // 2. Normal Body Map
            pinFindings["chest"] = "Bilateral breath sounds clear, vesicular"
            pinFindings["heart"] = "S1 S2 normal, no murmurs"
            pinFindings["abdomen"] = "Soft, non-tender, no guarding"
            pinFindings["limbs"] = "No pedal edema, peripheral pulses intact"
            pinFindings["head"] = "Throat clear, no congestion"
            pinFindings["neck"] = "Supple, no lymphadenopathy"

            // 3. Normal Neurological
            neuroGcs = "GCS 15/15 (E4V5M6)"
            neuroOrientation = "Oriented to Time, Place & Person"
            neuroPupils = "PERRL 3mm bilateral (Normal)"
            neuroCranialNerves = "CN II-XII Grossly Intact"
            neuroMotorPower = "Tone & Power 5/5 in all 4 limbs"
            neuroReflexes = "Normal DTRs (2+)"
            neuroPlantar = "Plantar flexor bilateral (Normal -)"
            neuroMeningeal = "No neck stiffness / Kernig (-)"
            neuroGait = "Normal steady gait, Romberg (-)"

            // 4. No Trauma
            hasTraumaPresent = false
            traumaWoundTypes.clear()
            traumaSite = ""
            traumaDimensions = ""

            syncExamFindings()
          },
          contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = SoftGreenContainer)
        ) {
          Icon(Icons.Default.DoneAll, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Normal All", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
        }

        OutlinedButton(
          onClick = {
            pinFindings.clear()
            genAppearance = null
            genBuildNutrition = null
            genPallor = ""
            genJaundice = ""
            genCyanosis = ""
            genClubbing = ""
            genEdema = ""
            genLAP = ""
            genHydration = ""
            genPerfusion = ""
            neuroGcs = ""
            neuroOrientation = ""
            neuroPupils = ""
            neuroCranialNerves = ""
            neuroMotorPower = ""
            neuroReflexes = ""
            neuroPlantar = ""
            neuroMeningeal = ""
            neuroGait = ""
            hasTraumaPresent = false
            traumaWoundTypes.clear()
            traumaSite = ""
            traumaDimensions = ""
            syncExamFindings()
          },
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          border = BorderStroke(0.5.dp, Color(0xFFEF9A9A))
        ) {
          Text("Clear", fontSize = 9.5.sp, color = Color(0xFFD32F2F))
        }

        IconButton(
          onClick = { showFreeTextExamDialog = true },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.EditNote, contentDescription = "Free Text", tint = SlateMedium, modifier = Modifier.size(18.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Sub-Navigation Selector Tabs
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = Color(0xFFF1F5F9)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        // Sub-tab 0: General Condition
        Surface(
          modifier = Modifier
            .weight(1.15f)
            .clickable { peSubTab = 0 },
          shape = RoundedCornerShape(6.dp),
          color = if (peSubTab == 0) PureWhite else Color.Transparent,
          shadowElevation = if (peSubTab == 0) 1.dp else 0.dp
        ) {
          Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = if (peSubTab == 0) MedicalGreen else SlateMedium, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("General Condition", fontSize = 9.5.sp, fontWeight = if (peSubTab == 0) FontWeight.Bold else FontWeight.Medium, color = if (peSubTab == 0) MedicalGreenDark else SlateDark)
          }
        }

        // Sub-tab 1: Systemic & Body Map
        Surface(
          modifier = Modifier
            .weight(1f)
            .clickable { peSubTab = 1 },
          shape = RoundedCornerShape(6.dp),
          color = if (peSubTab == 1) PureWhite else Color.Transparent,
          shadowElevation = if (peSubTab == 1) 1.dp else 0.dp
        ) {
          Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = if (peSubTab == 1) Color(0xFF0288D1) else SlateMedium, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Body Map", fontSize = 9.5.sp, fontWeight = if (peSubTab == 1) FontWeight.Bold else FontWeight.Medium, color = if (peSubTab == 1) Color(0xFF0288D1) else SlateDark)
            if (pinFindings.isNotEmpty()) {
              Spacer(modifier = Modifier.width(3.dp))
              Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE1F5FE)) {
                Text("${pinFindings.size}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
              }
            }
          }
        }

        // Sub-tab 2: Neurological
        Surface(
          modifier = Modifier
            .weight(1.05f)
            .clickable { peSubTab = 2 },
          shape = RoundedCornerShape(6.dp),
          color = if (peSubTab == 2) PureWhite else Color.Transparent,
          shadowElevation = if (peSubTab == 2) 1.dp else 0.dp
        ) {
          Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Biotech, contentDescription = null, tint = if (peSubTab == 2) Color(0xFF7B1FA2) else SlateMedium, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Neurological", fontSize = 9.5.sp, fontWeight = if (peSubTab == 2) FontWeight.Bold else FontWeight.Medium, color = if (peSubTab == 2) Color(0xFF7B1FA2) else SlateDark)
          }
        }

        // Sub-tab 3: Trauma & Wound
        Surface(
          modifier = Modifier
            .weight(1.05f)
            .clickable { peSubTab = 3 },
          shape = RoundedCornerShape(6.dp),
          color = if (peSubTab == 3) PureWhite else Color.Transparent,
          shadowElevation = if (peSubTab == 3) 1.dp else 0.dp
        ) {
          Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Healing, contentDescription = null, tint = if (peSubTab == 3) Color(0xFFE65100) else SlateMedium, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Trauma & Wound", fontSize = 9.5.sp, fontWeight = if (peSubTab == 3) FontWeight.Bold else FontWeight.Medium, color = if (peSubTab == 3) Color(0xFFE65100) else SlateDark)
            if (hasTraumaPresent || traumaWoundTypes.isNotEmpty()) {
              Spacer(modifier = Modifier.width(3.dp))
              Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFE082)) {
                Text("!", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // SUB-TAB 1: SYSTEMIC ANATOMICAL BODY MAP
    if (peSubTab == 1) {
      Column {
        Text("Click anatomical pins to record findings. 1-tap presets replace typing.", fontSize = 9.5.sp, color = SlateMuted)
        Spacer(modifier = Modifier.height(6.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
        ) {
          // Silhouette Canvas
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val gridPaintColor = Color(0xFFEDF2F7)
            for (i in 1..8) {
              drawLine(gridPaintColor, Offset(0f, h * i / 9f), Offset(w, h * i / 9f), strokeWidth = 1f)
              drawLine(gridPaintColor, Offset(w * i / 9f, 0f), Offset(w * i / 9f, h), strokeWidth = 1f)
            }

            val bodyStrokeColor = Color(0xFF94A3B8)
            val bodyFillColor = Color(0xFFE2E8F0)

            // Head
            val headCenter = Offset(w * 0.5f, h * 0.08f)
            val headRadius = h * 0.055f
            drawCircle(bodyFillColor, radius = headRadius, center = headCenter)
            drawCircle(bodyStrokeColor, radius = headRadius, center = headCenter, style = Stroke(width = 2f))

            // Neck
            val neckRectTop = h * 0.135f
            val neckRectHeight = h * 0.04f
            drawRect(
              bodyFillColor,
              topLeft = Offset(w * 0.47f, neckRectTop),
              size = androidx.compose.ui.geometry.Size(w * 0.06f, neckRectHeight)
            )

            // Torso
            val torsoPath = Path().apply {
              moveTo(w * 0.35f, h * 0.18f)
              lineTo(w * 0.65f, h * 0.18f)
              lineTo(w * 0.62f, h * 0.38f)
              lineTo(w * 0.60f, h * 0.52f)
              lineTo(w * 0.50f, h * 0.56f)
              lineTo(w * 0.40f, h * 0.52f)
              lineTo(w * 0.38f, h * 0.38f)
              close()
            }
            drawPath(torsoPath, bodyFillColor)
            drawPath(torsoPath, bodyStrokeColor, style = Stroke(width = 2f, join = StrokeJoin.Round))

            // Arms
            val leftArmPath = Path().apply {
              moveTo(w * 0.35f, h * 0.18f)
              lineTo(w * 0.22f, h * 0.32f)
              lineTo(w * 0.17f, h * 0.45f)
            }
            val rightArmPath = Path().apply {
              moveTo(w * 0.65f, h * 0.18f)
              lineTo(w * 0.78f, h * 0.32f)
              lineTo(w * 0.83f, h * 0.45f)
            }
            drawPath(leftArmPath, bodyStrokeColor, style = Stroke(width = 8f, cap = StrokeCap.Round))
            drawPath(rightArmPath, bodyStrokeColor, style = Stroke(width = 8f, cap = StrokeCap.Round))

            // Legs
            val leftLegPath = Path().apply {
              moveTo(w * 0.44f, h * 0.54f)
              lineTo(w * 0.42f, h * 0.72f)
              lineTo(w * 0.42f, h * 0.90f)
            }
            val rightLegPath = Path().apply {
              moveTo(w * 0.56f, h * 0.54f)
              lineTo(w * 0.58f, h * 0.72f)
              lineTo(w * 0.58f, h * 0.90f)
            }
            drawPath(leftLegPath, bodyStrokeColor, style = Stroke(width = 10f, cap = StrokeCap.Round))
            drawPath(rightLegPath, bodyStrokeColor, style = Stroke(width = 10f, cap = StrokeCap.Round))
          }

          // Clickable Anatomical Pins
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val mapW = maxWidth
            val mapH = maxHeight

            CLINIC_BODY_PINS.forEach { pin ->
              val finding = pinFindings[pin.id]
              val hasFinding = !finding.isNullOrBlank()

              val pinX = (mapW.value * pin.xFactor).dp
              val pinY = (mapH.value * pin.yFactor).dp

              Box(
                modifier = Modifier
                  .offset(x = pinX - if (hasFinding) 45.dp else 24.dp, y = pinY - 14.dp)
                  .clickable { editingPin = pin }
              ) {
                if (hasFinding) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MedicalGreen,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, PureWhite)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(11.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        finding ?: pin.nameEn,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                } else {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFFFF),
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFF0288D1))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(10.dp))
                      Spacer(modifier = Modifier.width(2.dp))
                      Text(pin.nameEn, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // SUB-TAB 0: GENERAL CONDITION ASSESSMENT
    if (peSubTab == 0) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "General Condition Assessment (အထွေထွေ အခြေအနေ စမ်းသပ်ချက်):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreenDark
          )
          TextButton(
            onClick = {
              genAppearance = "Alert, conscious & oriented, comfortable"
              genBuildNutrition = "Well-nourished, adequate build"
              genPallor = "No pallor (-)"
              genJaundice = "No jaundice (-)"
              genCyanosis = "No cyanosis (-)"
              genClubbing = "No clubbing (-)"
              genEdema = "No pedal edema (-)"
              genLAP = "No palpable LAP (-)"
              genHydration = "Moist mucous membranes, good skin turgor"
              genPerfusion = "Capillary refill < 2s (Normal)"
              syncExamFindings()
            },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.DoneAll, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Preset Normal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
          }
        }

        // 1. General Appearance
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF9FAFB),
          border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("General Appearance (လူနာ၏ အသွင်အပြင်):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("Alert, conscious & oriented", "Comfortable, no acute distress", "Ill-looking / Toxic", "Acute Distress / Pain", "Lethargic / Drowsy", "Restless / Agitated", "Unconscious").forEach { app ->
                val isSel = genAppearance == app
                FilterChip(
                  selected = isSel,
                  onClick = {
                    genAppearance = if (isSel) null else app
                    syncExamFindings()
                  },
                  label = { Text(app, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
                )
              }
            }
          }
        }

        // 2. Build & Nutrition
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF9FAFB),
          border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Build & Nutrition (ခန္ဓာကိုယ် အချိုးအစား & အာဟာရ):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("Well-nourished", "Moderate build", "Overweight / Obese", "Underweight / Cachectic", "Adequate hydration", "Dehydrated / Dry tongue").forEach { bld ->
                val isSel = genBuildNutrition == bld
                FilterChip(
                  selected = isSel,
                  onClick = {
                    genBuildNutrition = if (isSel) null else bld
                    syncExamFindings()
                  },
                  label = { Text(bld, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0288D1), selectedLabelColor = PureWhite)
                )
              }
            }
          }
        }

        // 3. The 6 Cardinal General Signs
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF0FDF4),
          border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("The 6 Cardinal Clinical Signs (အဓိက စမ်းသပ်ချက် ၆ ချက်):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
            Spacer(modifier = Modifier.height(4.dp))

            Text("• Pallor (သွေးအားနည်း / ဖြူဖျော့ခြင်း):", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No pallor (-)", "Mild pallor (+)", "Moderate pallor (++)", "Severe pallor (+++)").forEach { pal ->
                FilterChip(
                  selected = genPallor == pal,
                  onClick = { genPallor = pal; syncExamFindings() },
                  label = { Text(pal, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (pal.contains("No")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (pal.contains("No")) MedicalGreenDark else Color(0xFFC62828))
                )
              }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text("• Jaundice / Icterus (အသားဝါခြင်း):", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No jaundice (-)", "Scleral icterus (+)", "Deep jaundice (++)").forEach { jnd ->
                FilterChip(
                  selected = genJaundice == jnd,
                  onClick = { genJaundice = jnd; syncExamFindings() },
                  label = { Text(jnd, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (jnd.contains("No")) SoftGreenContainer else Color(0xFFFFF9C4), selectedLabelColor = if (jnd.contains("No")) MedicalGreenDark else Color(0xFFF57F17))
                )
              }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text("• Cyanosis (နှုတ်ခမ်း / လက်ဖျား ပြာနှမ်းခြင်း):", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No cyanosis (-)", "Peripheral cyanosis (+)", "Central cyanosis (+)").forEach { cya ->
                FilterChip(
                  selected = genCyanosis == cya,
                  onClick = { genCyanosis = cya; syncExamFindings() },
                  label = { Text(cya, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (cya.contains("No")) SoftGreenContainer else Color(0xFFE0F7FA), selectedLabelColor = if (cya.contains("No")) MedicalGreenDark else Color(0xFF006064))
                )
              }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text("• Clubbing (လက်သည်းခွံ ကွေးညွှတ်ထူထဲခြင်း):", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No clubbing (-)", "Finger clubbing (+)").forEach { clb ->
                FilterChip(
                  selected = genClubbing == clb,
                  onClick = { genClubbing = clb; syncExamFindings() },
                  label = { Text(clb, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
                )
              }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text("• Edema (ခြေထောက် / ခန္ဓာကိုယ် ဖောရောင်ခြင်း):", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No pedal edema (-)", "Pitting edema (+) ankle", "Pitting edema (++) tibia", "Generalized edema (Anasarca)").forEach { edm ->
                FilterChip(
                  selected = genEdema == edm,
                  onClick = { genEdema = edm; syncExamFindings() },
                  label = { Text(edm, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (edm.contains("No")) SoftGreenContainer else Color(0xFFEDE7F6), selectedLabelColor = if (edm.contains("No")) MedicalGreenDark else Color(0xFF512DA8))
                )
              }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text("• Lymphadenopathy (ပြန်ရည်ကျိတ်များ):", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No palpable LAP (-)", "Cervical LAP (+)", "Axillary LAP (+)", "Inguinal LAP (+)", "Generalized LAP (+)").forEach { lap ->
                FilterChip(
                  selected = genLAP == lap,
                  onClick = { genLAP = lap; syncExamFindings() },
                  label = { Text(lap, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (lap.contains("No")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (lap.contains("No")) MedicalGreenDark else Color(0xFFC62828))
                )
              }
            }
          }
        }

        // 4. Perfusion & Capillary Refill
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF9FAFB),
          border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Perfusion & Hydration (သွေးလည်ပတ်မှု & ရေဓါတ် အခြေအနေ):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("Capillary refill < 2s (Normal)", "Capillary refill > 2s (Prolonged)", "Moist oral mucosa", "Dry tongue & mucous membranes", "Warm peripheral extremities", "Cold clammy extremities").forEach { perf ->
                val isSel = genHydration.contains(perf) || genPerfusion == perf
                FilterChip(
                  selected = isSel,
                  onClick = {
                    if (perf.contains("Capillary")) genPerfusion = perf
                    else genHydration = perf
                    syncExamFindings()
                  },
                  label = { Text(perf, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
                )
              }
            }
          }
        }
      }
    }

    // SUB-TAB 2: NEUROLOGICAL ASSESSMENT
    if (peSubTab == 2) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Neurological System Assessment (အာရုံကြော စနစ် စနစ်တကျ စမ်းသပ်ချက်):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6A1B9A)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
              onClick = {
                hasNeuroExam = false
                neuroGcs = ""
                neuroOrientation = ""
                neuroPupils = ""
                neuroCranialNerves = ""
                neuroMotorPower = ""
                neuroReflexes = ""
                neuroPlantar = ""
                neuroMeningeal = ""
                neuroGait = ""
                neuroCustomNotes = ""
                syncExamFindings()
              },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Clear, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Clear", fontSize = 10.sp, color = SlateDark)
            }

            TextButton(
              onClick = {
                hasNeuroExam = true
                neuroGcs = "GCS 15/15 (E4V5M6)"
                neuroOrientation = "Oriented to Time, Place & Person"
                neuroPupils = "PERRL 3mm bilateral (Normal)"
                neuroCranialNerves = "CN II-XII Grossly Intact"
                neuroMotorPower = "Tone & Power 5/5 in all 4 limbs"
                neuroReflexes = "Normal DTRs (2+)"
                neuroPlantar = "Plantar flexor bilateral (Normal -)"
                neuroMeningeal = "No neck stiffness / Kernig (-)"
                neuroGait = "Normal steady gait, Romberg (-)"
                syncExamFindings()
              },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Preset Normal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
            }
          }
        }

        // 1. Consciousness & GCS
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFFAF5FF),
          border = BorderStroke(1.dp, Color(0xFFE1BEE7))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Consciousness Level & GCS (သတိရှိမှု & GCS အဆင့်):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("GCS 15/15 (E4V5M6) - Normal", "GCS 13-14 (Mild Impairment)", "GCS 9-12 (Moderate)", "GCS ≤ 8 (Severe / Coma)").forEach { gcs ->
                FilterChip(
                  selected = neuroGcs.startsWith(gcs.take(6)),
                  onClick = { neuroGcs = gcs; syncExamFindings() },
                  label = { Text(gcs, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF7B1FA2), selectedLabelColor = PureWhite)
                )
              }
            }
            Spacer(modifier = Modifier.height(3.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("Oriented to Time, Place & Person", "Confused / Disoriented", "Stuporous / Drowsy", "Unresponsive").forEach { ori ->
                FilterChip(
                  selected = neuroOrientation == ori,
                  onClick = { neuroOrientation = ori; syncExamFindings() },
                  label = { Text(ori, fontSize = 8.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
                )
              }
            }
          }
        }

        // 2. Pupils & Cranial Nerves
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF9FAFB),
          border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Pupils & Cranial Nerves (သူငယ်အိမ် & ဦးနှောက်အာရုံကြောများ):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("PERRL 3mm bilateral (Normal)", "Right sluggish", "Left sluggish", "Dilated bilateral", "Pinpoint / Constricted").forEach { pup ->
                FilterChip(
                  selected = neuroPupils == pup,
                  onClick = { neuroPupils = pup; syncExamFindings() },
                  label = { Text(pup, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0288D1), selectedLabelColor = PureWhite)
                )
              }
            }
            Spacer(modifier = Modifier.height(3.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("CN II-XII Grossly Intact", "Facial droop / Asymmetry (+)", "Dysarthria / Slurred speech", "Extraocular palsy (+)", "Tongue deviation (+)").forEach { cn ->
                FilterChip(
                  selected = neuroCranialNerves == cn,
                  onClick = { neuroCranialNerves = cn; syncExamFindings() },
                  label = { Text(cn, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (cn.contains("Intact")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (cn.contains("Intact")) MedicalGreenDark else Color(0xFFC62828))
                )
              }
            }
          }
        }

        // 3. Motor System & Tone
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF9FAFB),
          border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Motor System, Tone & Power (ကြွက်သားအား & လှုပ်ရှားမှု):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("Tone & Power 5/5 in all 4 limbs", "Right Hemiparesis (Power 3-4/5)", "Left Hemiparesis (Power 3-4/5)", "Paraparesis (Lower limbs weak)", "Flaccid tone / Hypotonia", "Spastic tone / Hypertonia").forEach { mot ->
                FilterChip(
                  selected = neuroMotorPower == mot,
                  onClick = { neuroMotorPower = mot; syncExamFindings() },
                  label = { Text(mot, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (mot.contains("5/5")) SoftGreenContainer else Color(0xFFFFF3E0), selectedLabelColor = if (mot.contains("5/5")) MedicalGreenDark else Color(0xFFE65100))
                )
              }
            }
          }
        }

        // 4. Reflexes, Babinski & Meningeal Signs
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF9FAFB),
          border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Reflexes, Plantar & Meningeal Signs (တုံ့ပြန်မှု & ဦးနှောက်မြှေး):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              listOf("Normal DTRs (2+)", "Hyperreflexia (3-4+)", "Absent reflexes (0)", "Plantar flexor bilateral (Normal -)", "Babinski extensor (+) Right", "Babinski extensor (+) Left", "Babinski extensor (+) Bilateral").forEach { ref ->
                val isSel = neuroReflexes == ref || neuroPlantar == ref
                FilterChip(
                  selected = isSel,
                  onClick = {
                    if (ref.contains("DTR") || ref.contains("reflex")) neuroReflexes = ref
                    else neuroPlantar = ref
                    syncExamFindings()
                  },
                  label = { Text(ref, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
                )
              }
            }
            Spacer(modifier = Modifier.height(3.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("No neck stiffness / Kernig (-)", "Neck stiffness present (+)", "Kernig & Brudzinski (+)").forEach { men ->
                FilterChip(
                  selected = neuroMeningeal == men,
                  onClick = { neuroMeningeal = men; syncExamFindings() },
                  label = { Text(men, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (men.contains("No")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (men.contains("No")) MedicalGreenDark else Color(0xFFC62828))
                )
              }
            }
            Spacer(modifier = Modifier.height(3.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("Normal steady gait, Romberg (-)", "Ataxic / Unsteady gait", "Romberg positive (+)", "Bedbound / Unable to walk").forEach { gt ->
                FilterChip(
                  selected = neuroGait == gt,
                  onClick = { neuroGait = gt; syncExamFindings() },
                  label = { Text(gt, fontSize = 8.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
                )
              }
            }
          }
        }
      }
    }

    // SUB-TAB 3: TRAUMA & WOUND ASSESSMENT
    if (peSubTab == 3) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Trauma & Wound Assessment (ထိခိုက်ဒဏ်ရာ & အနာ စမ်းသပ်စစ်ဆေးချက်):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBF360C)
          )

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
              selected = !hasTraumaPresent && traumaWoundTypes.isEmpty(),
              onClick = {
                hasTraumaPresent = false
                traumaWoundTypes.clear()
                traumaSite = ""
                traumaDimensions = ""
                traumaInfectionSigns.clear()
                syncExamFindings()
              },
              label = { Text("No Acute Trauma", fontSize = 9.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
            )

            FilterChip(
              selected = hasTraumaPresent || traumaWoundTypes.isNotEmpty(),
              onClick = {
                hasTraumaPresent = true
                if (traumaWoundTypes.isEmpty()) traumaWoundTypes.add("Laceration (စုတ်ပြဲဒဏ်ရာ)")
                syncExamFindings()
              },
              label = { Text("Trauma Present", fontSize = 9.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFE082), selectedLabelColor = Color(0xFFBF360C))
            )
          }
        }

        if (hasTraumaPresent || traumaWoundTypes.isNotEmpty()) {
          // 1. Wound Classification
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF8E1),
            border = BorderStroke(1.dp, Color(0xFFFFE082))
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("Wound Classification (ဒဏ်ရာအမျိုးအစား):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
              Spacer(modifier = Modifier.height(4.dp))
              FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                  "Laceration (စုတ်ပြဲဒဏ်ရာ)",
                  "Abrasion (ပွန်းပဲ့ဒဏ်ရာ)",
                  "Contusion / Hematoma (ညိုမဲစွဲဒဏ်ရာ)",
                  "Incised wound (ဓားရှ/ချွန်ထက်ဒဏ်ရာ)",
                  "Puncture wound (ထိုးဖောက်ဒဏ်ရာ)",
                  "Burn / Scald (မီးလောင်ဒဏ်ရာ)",
                  "Surgical wound / Suture",
                  "Chronic ulcer (နာတာရှည်အနာ)"
                ).forEach { wType ->
                  val isPicked = traumaWoundTypes.contains(wType)
                  FilterChip(
                    selected = isPicked,
                    onClick = {
                      if (isPicked) traumaWoundTypes.remove(wType) else traumaWoundTypes.add(wType)
                      syncExamFindings()
                    },
                    label = { Text(wType, fontSize = 8.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE65100), selectedLabelColor = PureWhite)
                  )
                }
              }
            }
          }

          // 2. Anatomical Site & Dimensions
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF9FAFB),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("Anatomical Site & Dimensions (ဒဏ်ရာတည်နေရာနှင့် အတိုင်းအတာ):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Spacer(modifier = Modifier.height(4.dp))
              FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("Head & Scalp", "Forehead & Face", "Upper Extremity / Hand", "Thorax / Chest", "Abdomen", "Lower Extremity / Knee", "Foot / Ankle", "Back / Spine").forEach { st ->
                  val isSel = traumaSite.contains(st)
                  FilterChip(
                    selected = isSel,
                    onClick = {
                      traumaSite = if (isSel) "" else st
                      syncExamFindings()
                    },
                    label = { Text(st, fontSize = 8.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0288D1), selectedLabelColor = PureWhite)
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                  value = traumaSite,
                  onValueChange = { traumaSite = it; syncExamFindings() },
                  placeholder = { Text("Specific anatomical site (e.g. Left forearm, scalp, right knee)...", fontSize = 9.sp, color = SlateMuted) },
                  modifier = Modifier.weight(1.2f),
                  textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                  shape = RoundedCornerShape(4.dp),
                  singleLine = false, minLines = 1, maxLines = 3
                )
                OutlinedTextField(
                  value = traumaDimensions,
                  onValueChange = { traumaDimensions = it; syncExamFindings() },
                  placeholder = { Text("Dimensions & depth (e.g. 3x0.5 cm, superficial)...", fontSize = 9.sp, color = SlateMuted) },
                  modifier = Modifier.weight(1f),
                  textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                  shape = RoundedCornerShape(4.dp),
                  singleLine = false, minLines = 1, maxLines = 3
                )
              }
            }
          }

          // 3. Bleeding, Contamination & Infection Signs
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF9FAFB),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("Hemostasis, Contamination & Infection Signs (သွေးထွက်မှုနှင့် ပိုးဝင်လက္ခဏာ):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Spacer(modifier = Modifier.height(4.dp))

              Text("• Bleeding / Hemostasis:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Bleeding controlled / stopped", "Active minor oozing", "Pulsatile bleeding (Arterial)", "Expanding hematoma").forEach { bld ->
                  FilterChip(
                    selected = traumaBleeding == bld,
                    onClick = { traumaBleeding = bld; syncExamFindings() },
                    label = { Text(bld, fontSize = 8.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (bld.contains("stopped")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (bld.contains("stopped")) MedicalGreenDark else Color(0xFFC62828))
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))
              Text("• Contamination / Cleanliness:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Clean wound", "Contaminated (dirt / road gravel)", "Foreign body present", "Purulent exudate").forEach { cnt ->
                  FilterChip(
                    selected = traumaContamination == cnt,
                    onClick = { traumaContamination = cnt; syncExamFindings() },
                    label = { Text(cnt, fontSize = 8.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (cnt.contains("Clean")) SoftGreenContainer else Color(0xFFFFF9C4), selectedLabelColor = if (cnt.contains("Clean")) MedicalGreenDark else Color(0xFFF57F17))
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))
              Text("• Infection Signs & Inflammation:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("No signs of infection", "Erythema / Redness (+)", "Local warmth & edema (+)", "Severe tenderness (+)", "Foul odor (+)").forEach { inf ->
                  val isInf = traumaInfectionSigns.contains(inf)
                  FilterChip(
                    selected = isInf,
                    onClick = {
                      if (isInf) traumaInfectionSigns.remove(inf)
                      else {
                        if (inf.startsWith("No")) traumaInfectionSigns.clear()
                        else traumaInfectionSigns.removeAll { it.startsWith("No") }
                        traumaInfectionSigns.add(inf)
                      }
                      syncExamFindings()
                    },
                    label = { Text(inf, fontSize = 8.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (inf.contains("No")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (inf.contains("No")) MedicalGreenDark else Color(0xFFC62828))
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))
              Text("• Distal Neurovascular & Tendons:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Distal sensation & pulses intact", "Distal pulse weak / diminished", "Distal numbness / tingling (+)", "Tendon / motor function impaired").forEach { nv ->
                  FilterChip(
                    selected = traumaNeurovascular == nv,
                    onClick = { traumaNeurovascular = nv; syncExamFindings() },
                    label = { Text(nv, fontSize = 8.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (nv.contains("intact")) SoftGreenContainer else Color(0xFFFFEBEE), selectedLabelColor = if (nv.contains("intact")) MedicalGreenDark else Color(0xFFC62828))
                  )
                }
              }
            }
          }

          // 4. Detailed Trauma / Wound Remarks
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF9FAFB),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("Detailed Trauma / Wound Remarks (ဒဏ်ရာအသေးစိတ် မှတ်ချက်များ):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = traumaCustomNotes,
                onValueChange = { traumaCustomNotes = it; syncExamFindings() },
                placeholder = { Text("Describe wound depth, mechanism of injury, debridement/suturing needed, tetanus prophylaxis...", fontSize = 9.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                minLines = 2,
                maxLines = 5
              )
            }
          }
        } else {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Check, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("No acute trauma or wound identified. Tap 'Trauma Present' above if patient has injury.", fontSize = 10.sp, color = MedicalGreenDark)
            }
          }
        }
      }
    }

    // Documented Findings Summary Card
    if (pinFindings.isNotEmpty() || typedNotes.isNotBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F8E9),
        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Documented Physical Examination Summary:",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = MedicalGreenDark
            )
            TextButton(
              onClick = { syncExamFindings() },
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
              modifier = Modifier.height(22.dp)
            ) {
              Text("Sync Record ✓", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            typedNotes.ifBlank { "Exam documented and synchronized." },
            fontSize = 9.5.sp,
            color = DarkGrayText,
            lineHeight = 13.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Optional Free Text Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End
    ) {
      TextButton(
        onClick = { showFreeTextExamDialog = true },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Icon(Icons.Default.EditNote, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Free Text Exam Notes", fontSize = 10.sp, color = SlateDark)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Save Physical Exam Action Bar with Feedback
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (examSaveFeedback != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(examSaveFeedback ?: "", fontSize = 10.sp, color = Color(0xFF0277BD), fontWeight = FontWeight.Bold)
        }
      } else {
        Text("Syncs to Objective findings in EMR", fontSize = 9.5.sp, color = SlateMuted)
      }

      Button(
        onClick = { syncExamFindings(showSavedBadge = true) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(30.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Save Physical Exam", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
      }
    }
  }

  // MODULARIZED DIALOGS
  if (editingPin != null) {
    val pin = editingPin!!
    AnatomicalPinFindingDialog(
      pin = pin,
      currentFinding = pinFindings[pin.id],
      onDismiss = { editingPin = null },
      onSaveFinding = { finding ->
        if (finding != null) {
          pinFindings[pin.id] = finding
        } else {
          pinFindings.remove(pin.id)
        }
        syncExamFindings()
      }
    )
  }

  if (showFreeTextExamDialog) {
    FreeTextExamNoteDialog(
      currentNotes = typedNotes,
      onDismiss = { showFreeTextExamDialog = false },
      onApply = { customExamNote ->
        onTypedNotesChange(customExamNote)
      }
    )
  }
}
