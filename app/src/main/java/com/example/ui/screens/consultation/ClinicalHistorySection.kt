package com.example.ui.screens.consultation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClinicalHistorySection(
  patientId: String?,
  patientChiefComplaint: String,
  subjectiveNotes: String,
  onSubjectiveNotesChange: (String) -> Unit,
  onActiveComplaintsCountChange: (Int) -> Unit = {}
) {
  var historySaveFeedback by remember { mutableStateOf<String?>(null) }

  // 1. History State & Cascading Complaints
  val activeComplaints = remember(patientId) { mutableStateListOf<ComplaintItem>() }
  var historyRemarks by remember(patientId) { mutableStateOf("") }
  var showAddCustomComplaintDialog by remember { mutableStateOf(false) }

  // Background Histories State
  var showBackgroundSection by remember { mutableStateOf(true) }
  val pastMedicalHistory = remember(patientId) { mutableStateListOf<String>() }
  var customPmhx by remember(patientId) { mutableStateOf("") }

  val pastSurgicalHistory = remember(patientId) { mutableStateListOf<String>() }
  var customPshx by remember(patientId) { mutableStateOf("") }

  val drugAllergies = remember(patientId) { mutableStateListOf<String>() }
  var customDrugAllergy by remember(patientId) { mutableStateOf("") }

  val regularMedicationsList = remember(patientId) { mutableStateListOf<String>() }
  var customRegularMedInput by remember(patientId) { mutableStateOf("") }
  var menstrualHistory by remember(patientId) { mutableStateOf("") }

  val socialHabits = remember(patientId) { mutableStateListOf<String>() }
  var customSocial by remember(patientId) { mutableStateOf("") }

  val traumaHistory = remember(patientId) { mutableStateListOf<String>() }
  var customTrauma by remember(patientId) { mutableStateOf("") }

  var saveFeedbackMessage by remember { mutableStateOf<String?>(null) }
  var initializedFromExisting by remember(patientId) { mutableStateOf(false) }

  // Notify parent of count
  LaunchedEffect(activeComplaints.size) {
    onActiveComplaintsCountChange(activeComplaints.size)
  }

  // Auto-init remarks from reception chief complaint or existing subjective notes
  LaunchedEffect(patientId, patientChiefComplaint) {
    if (initializedFromExisting) return@LaunchedEffect
    if (subjectiveNotes.isNotBlank()) {
      if (subjectiveNotes.contains("[CLINICAL REMARKS]")) {
        val extracted = subjectiveNotes.substringAfter("[CLINICAL REMARKS]").trim()
        if (extracted.isNotBlank()) historyRemarks = extracted
      } else if (!subjectiveNotes.contains("[CHIEF COMPLAINTS & HPI]") && !subjectiveNotes.contains("[BACKGROUND MEDICAL HISTORY]")) {
        historyRemarks = subjectiveNotes
      }
    } else if (patientChiefComplaint.isNotBlank()) {
      if (historyRemarks.isBlank() || historyRemarks == "General clinical consultation") {
        historyRemarks = patientChiefComplaint
      }
    }
    initializedFromExisting = true
  }

  // Synchronize history into subjective notes
  fun syncHistory(showSavedBadge: Boolean = false) {
    val sections = mutableListOf<String>()

    // 1. Chief Complaints & Cascading HPI
    if (activeComplaints.isNotEmpty()) {
      val compLines = mutableListOf<String>()
      compLines.add("[CHIEF COMPLAINTS & HPI]")
      activeComplaints.forEachIndexed { idx, c ->
        val dur = c.customDuration.trim().ifBlank { c.duration }
        val charStr = c.customCharacter.trim().ifBlank { c.character }
        val charPart = if (charStr.isNotBlank()) ", $charStr" else ""
        val sevPart = "[${c.severity}$charPart, ${c.onset}]"

        compLines.add("#${idx + 1}. ${c.name} - $dur $sevPart")

        val factors = mutableListOf<String>()
        val relList = c.relievingFactors.toMutableList()
        if (c.customRelieving.isNotBlank()) relList.add(c.customRelieving.trim())
        if (relList.isNotEmpty()) factors.add("Relieved by: ${relList.joinToString(", ")}")

        val aggList = c.aggravatingFactors.toMutableList()
        if (c.customAggravating.isNotBlank()) aggList.add(c.customAggravating.trim())
        if (aggList.isNotEmpty()) factors.add("Aggravated by: ${aggList.joinToString(", ")}")

        val assocList = c.associatedSymptoms.toMutableList()
        if (c.customAssociated.isNotBlank()) assocList.add(c.customAssociated.trim())
        if (assocList.isNotEmpty()) factors.add("Associated: ${assocList.joinToString(", ")}")

        if (factors.isNotEmpty()) {
          compLines.add("   → ${factors.joinToString(" | ")}")
        }

        if (c.notes.isNotBlank()) {
          compLines.add("   → Notes: ${c.notes.trim()}")
        }
      }
      sections.add(compLines.joinToString("\n"))
    }

    // 2. Background Medical Histories
    val bgLines = mutableListOf<String>()

    val pmhxList = pastMedicalHistory.toMutableList()
    if (customPmhx.isNotBlank()) pmhxList.add(customPmhx.trim())
    if (pmhxList.isNotEmpty()) bgLines.add("• PMHx: ${pmhxList.joinToString(", ")}")

    val pshxList = pastSurgicalHistory.toMutableList()
    if (customPshx.isNotBlank()) pshxList.add(customPshx.trim())
    if (pshxList.isNotEmpty()) bgLines.add("• PSHx: ${pshxList.joinToString(", ")}")

    val allergyList = drugAllergies.toMutableList()
    if (customDrugAllergy.isNotBlank()) allergyList.add(customDrugAllergy.trim())
    if (allergyList.isNotEmpty()) bgLines.add("• Drug Allergies: ${allergyList.joinToString(", ")}")

    val allRegMeds = regularMedicationsList.toMutableList()
    if (customRegularMedInput.isNotBlank()) allRegMeds.add(customRegularMedInput.trim())
    if (allRegMeds.isNotEmpty()) bgLines.add("• Regular Daily Meds (${allRegMeds.size}): ${allRegMeds.joinToString(", ")}")

    if (menstrualHistory.isNotBlank()) bgLines.add("• Menstrual/OBGYN: ${menstrualHistory.trim()}")

    val socialList = socialHabits.toMutableList()
    if (customSocial.isNotBlank()) socialList.add(customSocial.trim())
    if (socialList.isNotEmpty()) bgLines.add("• Social History: ${socialList.joinToString(", ")}")

    val traumaList = traumaHistory.toMutableList()
    if (customTrauma.isNotBlank()) traumaList.add(customTrauma.trim())
    if (traumaList.isNotEmpty()) bgLines.add("• Trauma/Injury: ${traumaList.joinToString(", ")}")

    if (bgLines.isNotEmpty()) {
      sections.add("[BACKGROUND MEDICAL HISTORY]\n" + bgLines.joinToString("\n"))
    }

    // 3. Additional Remarks
    if (historyRemarks.isNotBlank()) {
      sections.add("[CLINICAL REMARKS]\n$historyRemarks")
    }

    val compiled = sections.joinToString("\n\n")
    if (compiled.isNotBlank()) {
      onSubjectiveNotesChange(compiled)
    } else if (subjectiveNotes.isBlank()) {
      onSubjectiveNotesChange(compiled)
    }

    if (showSavedBadge) {
      saveFeedbackMessage = "History Saved to Record ✓"
      historySaveFeedback = "History Saved to Record ✓"
    }
  }

  // Debounced background sync
  LaunchedEffect(
    historyRemarks,
    customPmhx,
    customPshx,
    customDrugAllergy,
    menstrualHistory,
    customSocial,
    customTrauma
  ) {
    if (initializedFromExisting) {
      val hasDataToSync = activeComplaints.isNotEmpty() ||
          pastMedicalHistory.isNotEmpty() ||
          pastSurgicalHistory.isNotEmpty() ||
          drugAllergies.isNotEmpty() ||
          regularMedicationsList.isNotEmpty() ||
          socialHabits.isNotEmpty() ||
          traumaHistory.isNotEmpty() ||
          customPmhx.isNotBlank() ||
          customPshx.isNotBlank() ||
          customDrugAllergy.isNotBlank() ||
          menstrualHistory.isNotBlank() ||
          customSocial.isNotBlank() ||
          customTrauma.isNotBlank() ||
          (historyRemarks.isNotBlank() && historyRemarks != patientChiefComplaint)

      if (hasDataToSync) {
        kotlinx.coroutines.delay(800)
        syncHistory(showSavedBadge = false)
      }
    }
  }

  Column(modifier = Modifier.padding(top = 6.dp)) {
    // Chief Complaint registered from Reception / Triage
    if (patientChiefComplaint.isNotBlank()) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, Color(0xFF93C5FD))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("Triage / Reception Chief Complaint (လူနာစာရင်းသွင်းစဉ်က လက္ခဏာ):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateMuted)
            Text(patientChiefComplaint, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
          }
          TextButton(
            onClick = {
              if (!historyRemarks.contains(patientChiefComplaint)) {
                historyRemarks = if (historyRemarks.isBlank()) patientChiefComplaint else "$historyRemarks\n$patientChiefComplaint"
                syncHistory()
              }
            },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(24.dp)
          ) {
            Text("Add to Notes", fontSize = 9.5.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Top action row for Complaints: Title, + Add Custom Complaint, Clear All
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "Chief Complaints (Select to open drill-down):",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(
          onClick = { showAddCustomComplaintDialog = true },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(26.dp),
          colors = ButtonDefaults.filledTonalButtonColors(containerColor = SoftGreenContainer)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("+ Custom", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
        }

        if (activeComplaints.isNotEmpty()) {
          TextButton(
            onClick = {
              activeComplaints.clear()
              syncHistory()
            },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Text("Clear All", fontSize = 9.5.sp, color = Color(0xFFD32F2F))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Common Symptom Selection Chips (Pure English)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      CLINIC_COMMON_SYMPTOMS.forEach { symName ->
        val isSelected = activeComplaints.any { it.name.equals(symName, ignoreCase = true) }
        FilterChip(
          selected = isSelected,
          onClick = {
            if (isSelected) {
              activeComplaints.removeAll { it.name.equals(symName, ignoreCase = true) }
            } else {
              activeComplaints.add(
                ComplaintItem(
                  id = "sym_${System.currentTimeMillis()}_${symName.hashCode()}",
                  name = symName
                )
              )
            }
            syncHistory()
          },
          label = {
            Text(
              symName,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SoftGreenContainer,
            selectedLabelColor = MedicalGreenDark
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) MedicalGreen else BorderGray
          )
        )
      }
    }

    // CASCADING DRILL-DOWN BOXES
    if (activeComplaints.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "Active Complaints & Detailed HPI (${activeComplaints.size}):",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = MedicalGreenDark
        )
        Text("Drill-down forms stacked sequentially", fontSize = 9.sp, color = SlateMuted)
      }
      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        activeComplaints.forEachIndexed { index, complaint ->
          ComplaintDetailCard(
            index = index,
            complaint = complaint,
            onSync = { syncHistory() },
            onRemove = {
              activeComplaints.removeAt(index)
              syncHistory()
            }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // SECTION: BACKGROUND MEDICAL HISTORIES
    val bgCount = pastMedicalHistory.size + (if (customPmhx.isNotBlank()) 1 else 0) +
      pastSurgicalHistory.size + (if (customPshx.isNotBlank()) 1 else 0) +
      drugAllergies.size + (if (customDrugAllergy.isNotBlank()) 1 else 0) +
      regularMedicationsList.size + (if (customRegularMedInput.isNotBlank()) 1 else 0) +
      (if (menstrualHistory.isNotBlank()) 1 else 0) +
      socialHabits.size + (if (customSocial.isNotBlank()) 1 else 0) +
      traumaHistory.size + (if (customTrauma.isNotBlank()) 1 else 0)

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp),
      color = PureWhite,
      border = BorderStroke(1.2.dp, if (bgCount > 0) Color(0xFF90CAF9) else Color(0xFFCFD8DC))
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        // Main Accordion Header
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { showBackgroundSection = !showBackgroundSection },
          color = if (showBackgroundSection) Color(0xFFF0F7FF) else Color(0xFFF8FAFC)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF1976D2)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  if (showBackgroundSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = null,
                  tint = PureWhite,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "Background Medical History (အထွေထွေ ကျန်းမာရေး ရာဇဝင်)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0D47A1)
                )
                Text(
                  "PMHx • PSHx • Drug Allergies • Regular Meds • OBGYN • Social • Trauma",
                  fontSize = 9.sp,
                  color = SlateMedium
                )
              }
            }

            if (bgCount > 0) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFE3F2FD),
                border = BorderStroke(0.5.dp, Color(0xFF90CAF9))
              ) {
                Text(
                  "✓ $bgCount documented",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF1565C0),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }

        if (showBackgroundSection) {
          Spacer(modifier = Modifier.height(10.dp))

          // 1. PAST MEDICAL HISTORY (PMHx)
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF4F9FF),
            border = BorderStroke(1.dp, Color(0xFFBBDEFB))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("1. Past Medical History (PMHx)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    Text("နာတာရှည်နှင့် ယခင်ခံစားခဲ့ရသော ရောဂါများ", fontSize = 9.sp, color = SlateMedium)
                  }
                }
                if (pastMedicalHistory.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFBBDEFB)) {
                    Text("${pastMedicalHistory.size} selected", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("None", "Hypertension (HTN)", "Diabetes Mellitus (T2DM)", "Asthma / COPD", "IHD / CAD", "CKD", "Dyslipidemia", "Stroke / CVA", "GERD / Gastritis", "Thyroid disorder", "Gout", "Epilepsy").forEach { dis ->
                  val isPicked = pastMedicalHistory.contains(dis)
                  FilterChip(
                    selected = isPicked,
                    onClick = {
                      if (isPicked) {
                        pastMedicalHistory.remove(dis)
                      } else {
                        if (dis == "None") pastMedicalHistory.clear()
                        else pastMedicalHistory.remove("None")
                        pastMedicalHistory.add(dis)
                      }
                      syncHistory()
                    },
                    label = { Text(dis, fontSize = 9.sp, fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFF1976D2),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = customPmhx,
                onValueChange = { customPmhx = it },
                placeholder = { Text("Custom PMHx (e.g. Hepatitis B, Chronic Kidney Disease Stage 3...)", fontSize = 9.5.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                singleLine = false, minLines = 1, maxLines = 3
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 2. PAST SURGICAL HISTORY (PSHx)
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFAF5FF),
            border = BorderStroke(1.dp, Color(0xFFE1BEE7))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFF7B1FA2)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Healing, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("2. Past Surgical History (PSHx)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                    Text("ယခင်ခွဲစိတ်ကုသမှု ခံယူခဲ့ဖူးသော ရာဇဝင်", fontSize = 9.sp, color = SlateMedium)
                  }
                }
                if (pastSurgicalHistory.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE1BEE7)) {
                    Text("${pastSurgicalHistory.size} selected", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("None", "Appendectomy", "Cholecystectomy", "C-Section (LSCS)", "Herniorrhaphy", "Orthopedic Fixation", "Cataract Surgery", "Tonsillectomy").forEach { surg ->
                  val isPicked = pastSurgicalHistory.contains(surg)
                  FilterChip(
                    selected = isPicked,
                    onClick = {
                      if (isPicked) {
                        pastSurgicalHistory.remove(surg)
                      } else {
                        if (surg == "None") pastSurgicalHistory.clear()
                        else pastSurgicalHistory.remove("None")
                        pastSurgicalHistory.add(surg)
                      }
                      syncHistory()
                    },
                    label = { Text(surg, fontSize = 9.sp, fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFF7B1FA2),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = customPshx,
                onValueChange = { customPshx = it },
                placeholder = { Text("Custom surgical details (e.g. Laparoscopic Appendectomy 2021, Knee replacement...)", fontSize = 9.5.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                singleLine = false, minLines = 1, maxLines = 3
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 3. DRUG ALLERGIES
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF5F5),
            border = BorderStroke(1.2.dp, Color(0xFFEF9A9A))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFFC62828)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("3. Drug Allergies (ဆေးမတည့်မှု သတိပေးချက်)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                    Text("လူနာနှင့် ဓါတ်မတည့်သော ဆေးဝါးများ (အရေးကြီး)", fontSize = 9.sp, color = Color(0xFFC62828))
                  }
                }
                if (drugAllergies.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFCDD2)) {
                    Text("${drugAllergies.size} recorded", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("NKDA (No Known Allergies)", "Penicillin", "Cephalosporins", "NSAIDs / Ibuprofen", "Sulfa drugs", "Aspirin", "Paracetamol", "Ciprofloxacin").forEach { alg ->
                  val isPicked = drugAllergies.contains(alg)
                  FilterChip(
                    selected = isPicked,
                    onClick = {
                      if (isPicked) {
                        drugAllergies.remove(alg)
                      } else {
                        if (alg.startsWith("NKDA")) drugAllergies.clear()
                        else drugAllergies.removeAll { it.startsWith("NKDA") }
                        drugAllergies.add(alg)
                      }
                      syncHistory()
                    },
                    label = { Text(alg, fontSize = 9.sp, fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFC62828),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = customDrugAllergy,
                onValueChange = { customDrugAllergy = it },
                placeholder = { Text("Custom allergy (e.g. Paracetamol severe urticaria, Augmentin rash...)", fontSize = 9.5.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                singleLine = false, minLines = 1, maxLines = 3
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 4. REGULAR DAILY MEDICATIONS
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.2.dp, Color(0xFFA7F3D0))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFF00796B)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("4. Regular Daily Medications (ပုံမှန်သောက်ဆေးများ)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                    Text("လက်ရှိနေ့စဉ် သောက်သုံးနေသော ဆေးဝါးများ (တမျိုးမက ထည့်သွင်းနိုင်ပါသည်)", fontSize = 9.sp, color = SlateMedium)
                  }
                }
                if (regularMedicationsList.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFA7F3D0)) {
                    Text("${regularMedicationsList.size} meds", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text("အသုံးများသော ပုံမှန်သောက်ဆေးများ (နှိပ်၍ ထည့်နိုင်ပါသည်):", fontSize = 9.5.sp, color = SlateDark)
              Spacer(modifier = Modifier.height(3.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf(
                  "Amlodipine 5mg OD",
                  "Metformin 500mg BD",
                  "Losartan 50mg OD",
                  "Telmisartan 40mg OD",
                  "Atorvastatin 20mg ON",
                  "Aspirin 75mg OD",
                  "Omeprazole 20mg AC",
                  "Glimepiride 2mg OD",
                  "Salbutamol Inhaler PRN",
                  "Levothyroxine 50mcg OD"
                ).forEach { medPreset ->
                  val isAdded = regularMedicationsList.contains(medPreset)
                  FilterChip(
                    selected = isAdded,
                    onClick = {
                      if (isAdded) {
                        regularMedicationsList.remove(medPreset)
                      } else {
                        regularMedicationsList.add(medPreset)
                      }
                      syncHistory()
                    },
                    label = { Text(medPreset, fontSize = 8.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFF00796B),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              if (regularMedicationsList.isNotEmpty()) {
                Text("လက်ရှိ သောက်နေသော ဆေးစာရင်း:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004D40))
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  regularMedicationsList.forEachIndexed { idx, med ->
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = PureWhite,
                      border = BorderStroke(1.dp, Color(0xFF00796B))
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(med, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                          onClick = {
                            regularMedicationsList.removeAt(idx)
                            syncHistory()
                          },
                          modifier = Modifier.size(14.dp)
                        ) {
                          Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFD32F2F), modifier = Modifier.size(10.dp))
                        }
                      }
                    }
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = customRegularMedInput,
                  onValueChange = { customRegularMedInput = it },
                  placeholder = { Text("ဆေးအမည်၊ ပမာဏနှင့် သောက်ရမည့်အကြိမ် ရိုက်ထည့်ပါ (ဥပမာ- Enalapril 5mg 1 tab OD)...", fontSize = 9.5.sp, color = SlateMuted) },
                  modifier = Modifier.weight(1f),
                  textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                  shape = RoundedCornerShape(4.dp),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                  singleLine = false, minLines = 1, maxLines = 3
                )

                Button(
                  onClick = {
                    if (customRegularMedInput.isNotBlank()) {
                      regularMedicationsList.add(customRegularMedInput.trim())
                      customRegularMedInput = ""
                      syncHistory()
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                  shape = RoundedCornerShape(4.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(34.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(2.dp))
                  Text("Add Med", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 5. MENSTRUAL & OBGYN HISTORY
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFDF2F8),
            border = BorderStroke(1.dp, Color(0xFFFBCFE8))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFFC2185B)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("5. Menstrual & OBGYN History (မီးယပ် & သားဖွားရာဇဝင်)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF880E4F))
                    Text("ရာသီလာမှုရက်စွဲ (LMP)၊ စက်ဝန်းမှန်/မမှန် နှင့် သားဖွားမှတ်တမ်း", fontSize = 9.sp, color = SlateMedium)
                  }
                }
                if (menstrualHistory.isNotBlank()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFBCFE8)) {
                    Text("Noted", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF880E4F), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              OutlinedTextField(
                value = menstrualHistory,
                onValueChange = { menstrualHistory = it },
                placeholder = { Text("e.g. LMP: 05/10/2026, Regular 28-day cycle, G2P1, No dysmenorrhea...", fontSize = 9.5.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                singleLine = false, minLines = 1, maxLines = 3
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 6. PERSONAL & SOCIAL HABITS
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFCFD8DC))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFF455A64)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("6. Personal & Social Habits (လူမှုရေး & စွဲလမ်းမှုအလေ့အထ)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    Text("ဆေးလိပ်၊ အရက်၊ ကွမ်းစားခြင်းနှင့် နေထိုင်မှုပုံစံ", fontSize = 9.sp, color = SlateMedium)
                  }
                }
                if (socialHabits.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFCFD8DC)) {
                    Text("${socialHabits.size} selected", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("Non-smoker", "Current Smoker", "Ex-smoker", "Non-drinker", "Social Alcohol", "Regular Alcohol", "Betel Chewer", "Sedentary", "Active Exercise").forEach { habit ->
                  val isPicked = socialHabits.contains(habit)
                  FilterChip(
                    selected = isPicked,
                    onClick = {
                      if (isPicked) socialHabits.remove(habit) else socialHabits.add(habit)
                      syncHistory()
                    },
                    label = { Text(habit, fontSize = 9.sp, fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFF455A64),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = customSocial,
                onValueChange = { customSocial = it },
                placeholder = { Text("Custom social history (e.g. 10 pack-years, occupation: office worker, high stress...)", fontSize = 9.5.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                singleLine = false, minLines = 1, maxLines = 3
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 7. TRAUMA & INJURY HISTORY
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF8E1),
            border = BorderStroke(1.dp, Color(0xFFFFE082))
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(RoundedCornerShape(5.dp))
                      .background(Color(0xFFE65100)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("7. Trauma & Injury History (မတော်တဆ & ထိခိုက်ဒဏ်ရာ)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
                    Text("ယခင်နှင့် လတ်တလော မတော်တဆ ထိခိုက်မှုရာဇဝင်", fontSize = 9.sp, color = SlateMedium)
                  }
                }
                if (traumaHistory.isNotEmpty()) {
                  Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFE082)) {
                    Text("${traumaHistory.size} recorded", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("No trauma", "RTA (Road Traffic Accident)", "Slip & Fall", "Blunt Trauma", "Laceration / Cut", "Burn / Scald", "Head Injury (No LOC)", "Fracture history").forEach { tr ->
                  val isPicked = traumaHistory.contains(tr)
                  FilterChip(
                    selected = isPicked,
                    onClick = {
                      if (isPicked) traumaHistory.remove(tr) else traumaHistory.add(tr)
                      syncHistory()
                    },
                    label = { Text(tr, fontSize = 9.sp, fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFE65100),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = customTrauma,
                onValueChange = { customTrauma = it },
                placeholder = { Text("Injury details (e.g. yesterday motorcycle slip, right knee abrasion, tetanus up-to-date...)", fontSize = 9.5.sp, color = SlateMuted) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
                shape = RoundedCornerShape(4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
                singleLine = false, minLines = 1, maxLines = 3
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Single Text Field for Additional Remarks
    Text("Additional Remarks / Free Text:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
      value = historyRemarks,
      onValueChange = { historyRemarks = it },
      singleLine = false, minLines = 2, maxLines = 5,
      placeholder = { Text("ရောဂါရာဇဝင် အသေးစိတ်၊ လူနာပြောပြချက်များနှင့် အထွေထွေမှတ်ချက်များကို လွတ်လပ်စွာ ရိုက်ထည့်နိုင်ပါသည်...", fontSize = 11.sp, color = SlateMuted) },
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 70.dp, max = 140.dp),
      textStyle = TextStyle(fontSize = 11.sp, color = DarkGrayText),
      shape = RoundedCornerShape(8.dp),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Default
      ),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MedicalGreen,
        unfocusedBorderColor = BorderGray,
        focusedContainerColor = PureWhite,
        unfocusedContainerColor = Color(0xFFFAFAFA)
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Save / Sync Action Bar with Feedback
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (saveFeedbackMessage != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(saveFeedbackMessage ?: "", fontSize = 10.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
        }
      } else {
        Text("Auto-synced to consultation record", fontSize = 9.5.sp, color = SlateMuted)
      }

      Button(
        onClick = { syncHistory(showSavedBadge = true) },
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(30.dp)
      ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Save History", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
      }
    }
  }

  // MODULARIZED DIALOGS
  if (showAddCustomComplaintDialog) {
    AddCustomComplaintDialog(
      onDismiss = { showAddCustomComplaintDialog = false },
      onAddComplaint = { name, dur, sev ->
        activeComplaints.add(
          ComplaintItem(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            duration = dur,
            severity = sev,
            isCustom = true
          )
        )
        syncHistory()
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComplaintDetailCard(
  index: Int,
  complaint: ComplaintItem,
  onSync: () -> Unit,
  onRemove: () -> Unit
) {
  var selectedDuration by remember(complaint.id, complaint.duration) { mutableStateOf(complaint.duration) }
  var selectedSeverity by remember(complaint.id, complaint.severity) { mutableStateOf(complaint.severity) }
  var selectedOnset by remember(complaint.id, complaint.onset) { mutableStateOf(complaint.onset) }
  var selectedCharacter by remember(complaint.id, complaint.character) { mutableStateOf(complaint.character) }

  val relievingList = remember(complaint.id) { mutableStateListOf<String>().apply { addAll(complaint.relievingFactors) } }
  val aggravatingList = remember(complaint.id) { mutableStateListOf<String>().apply { addAll(complaint.aggravatingFactors) } }
  val associatedList = remember(complaint.id) { mutableStateListOf<String>().apply { addAll(complaint.associatedSymptoms) } }

  var localDuration by remember(complaint.id) { mutableStateOf(complaint.customDuration) }
  var localCharacter by remember(complaint.id) { mutableStateOf(complaint.customCharacter) }
  var localRelieving by remember(complaint.id) { mutableStateOf(complaint.customRelieving) }
  var localAggravating by remember(complaint.id) { mutableStateOf(complaint.customAggravating) }
  var localAssociated by remember(complaint.id) { mutableStateOf(complaint.customAssociated) }
  var localNotes by remember(complaint.id) { mutableStateOf(complaint.notes) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFDFB)),
    border = BorderStroke(1.dp, Color(0xFFC8E6C9))
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Header for Complaint #N
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MedicalGreen
          ) {
            Text(
              "#${index + 1}",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            complaint.name,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = DarkGrayText
          )
          if (complaint.isCustom) {
            Spacer(modifier = Modifier.width(4.dp))
            Text("[Custom]", fontSize = 9.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
          }
        }

        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(22.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Remove", tint = SlateMedium, modifier = Modifier.size(15.dp))
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 1. Duration
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Duration:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.width(68.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          listOf("1 day", "2-3 days", "5 days", "1 week", "2 weeks", "> 1 month").forEach { dur ->
            val isDurSelected = selectedDuration == dur && localDuration.isBlank()
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isDurSelected) MedicalGreen else Color(0xFFE2E8F0),
              modifier = Modifier.clickable {
                selectedDuration = if (isDurSelected) "" else dur
                complaint.duration = selectedDuration
                complaint.customDuration = ""
                localDuration = ""
                onSync()
              }
            ) {
              Text(
                dur,
                fontSize = 9.sp,
                fontWeight = if (isDurSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isDurSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      OutlinedTextField(
        value = localDuration,
        onValueChange = {
          localDuration = it
          complaint.customDuration = it
          onSync()
        },
        placeholder = { Text("Custom duration (e.g. 4 hours, 3 weeks, since morning...)", fontSize = 9.5.sp, color = SlateMuted) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 68.dp),
        textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
        shape = RoundedCornerShape(4.dp),
        singleLine = false, minLines = 1, maxLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Severity & Onset
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Severity:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.width(68.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          listOf("Mild (+)", "Moderate (++)", "Severe (+++)").forEach { sev ->
            val isSevSelected = selectedSeverity == sev
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isSevSelected) Color(0xFF0288D1) else Color(0xFFE2E8F0),
              modifier = Modifier.clickable {
                selectedSeverity = if (isSevSelected) "" else sev
                complaint.severity = selectedSeverity
                onSync()
              }
            ) {
              Text(
                sev,
                fontSize = 9.sp,
                fontWeight = if (isSevSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSevSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Onset:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.width(68.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          listOf("Sudden / Acute", "Gradual", "Intermittent", "Chronic").forEach { ons ->
            val isOnsetSelected = selectedOnset == ons
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isOnsetSelected) Color(0xFF7E22CE) else Color(0xFFE2E8F0),
              modifier = Modifier.clickable {
                selectedOnset = if (isOnsetSelected) "" else ons
                complaint.onset = selectedOnset
                onSync()
              }
            ) {
              Text(
                ons,
                fontSize = 9.sp,
                fontWeight = if (isOnsetSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isOnsetSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 3. Pain / Symptom Character
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Character:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.width(68.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          listOf("Sharp / Stabbing", "Dull aching", "Burning", "Throbbing", "Colicky", "Tightness / Pressure", "Cramping", "Dry", "Productive").forEach { char ->
            val isCharSelected = selectedCharacter == char
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isCharSelected) Color(0xFFE65100) else Color(0xFFE2E8F0),
              modifier = Modifier.clickable {
                selectedCharacter = if (isCharSelected) "" else char
                complaint.character = selectedCharacter
                onSync()
              }
            ) {
              Text(
                char,
                fontSize = 9.sp,
                fontWeight = if (isCharSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isCharSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      OutlinedTextField(
        value = localCharacter,
        onValueChange = {
          localCharacter = it
          complaint.customCharacter = it
          onSync()
        },
        placeholder = { Text("Custom character (e.g. nocturnal, barking, crushing, radiating...)", fontSize = 9.5.sp, color = SlateMuted) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 68.dp),
        textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
        shape = RoundedCornerShape(4.dp),
        singleLine = false, minLines = 1, maxLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 4. Relieving Factors
      Text("Relieving Factors:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
      Spacer(modifier = Modifier.height(3.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        listOf("Rest", "Medication / Analgesic", "Sitting forward", "Food / Eating", "Fasting", "Warm compress", "Lying flat").forEach { factor ->
          val isPicked = relievingList.contains(factor)
          FilterChip(
            selected = isPicked,
            onClick = {
              if (isPicked) {
                relievingList.remove(factor)
                complaint.relievingFactors.remove(factor)
              } else {
                relievingList.add(factor)
                if (!complaint.relievingFactors.contains(factor)) complaint.relievingFactors.add(factor)
              }
              onSync()
            },
            label = { Text(factor, fontSize = 9.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE8F5E9), selectedLabelColor = MedicalGreenDark)
          )
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      OutlinedTextField(
        value = localRelieving,
        onValueChange = {
          localRelieving = it
          complaint.customRelieving = it
          onSync()
        },
        placeholder = { Text("Custom relieving factor (e.g. Antacids, cool air, cold drink...)", fontSize = 9.5.sp, color = SlateMuted) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
        shape = RoundedCornerShape(4.dp),
        singleLine = false, minLines = 1, maxLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 5. Aggravating Factors
      Text("Aggravating Factors:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
      Spacer(modifier = Modifier.height(3.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        listOf("Movement / Exertion", "Deep breathing", "After meals", "Night / Cold air", "Stress", "Palpation / Pressure", "Walking").forEach { factor ->
          val isPicked = aggravatingList.contains(factor)
          FilterChip(
            selected = isPicked,
            onClick = {
              if (isPicked) {
                aggravatingList.remove(factor)
                complaint.aggravatingFactors.remove(factor)
              } else {
                aggravatingList.add(factor)
                if (!complaint.aggravatingFactors.contains(factor)) complaint.aggravatingFactors.add(factor)
              }
              onSync()
            },
            label = { Text(factor, fontSize = 9.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE), selectedLabelColor = Color(0xFFC62828))
          )
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      OutlinedTextField(
        value = localAggravating,
        onValueChange = {
          localAggravating = it
          complaint.customAggravating = it
          onSync()
        },
        placeholder = { Text("Custom aggravating factor (e.g. oily food, bending forward...)", fontSize = 9.5.sp, color = SlateMuted) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
        shape = RoundedCornerShape(4.dp),
        singleLine = false, minLines = 1, maxLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 6. Associated Symptoms
      Text("Associated Symptoms:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))
      Spacer(modifier = Modifier.height(3.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        listOf("Nausea / Vomiting", "Fever", "Chills & Rigors", "Sweating (+)", "Radiation", "Dizziness", "Diarrhea", "Anorexia", "Palpitations").forEach { factor ->
          val isPicked = associatedList.contains(factor)
          FilterChip(
            selected = isPicked,
            onClick = {
              if (isPicked) {
                associatedList.remove(factor)
                complaint.associatedSymptoms.remove(factor)
              } else {
                associatedList.add(factor)
                if (!complaint.associatedSymptoms.contains(factor)) complaint.associatedSymptoms.add(factor)
              }
              onSync()
            },
            label = { Text(factor, fontSize = 9.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE1F5FE), selectedLabelColor = Color(0xFF0277BD))
          )
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      OutlinedTextField(
        value = localAssociated,
        onValueChange = {
          localAssociated = it
          complaint.customAssociated = it
          onSync()
        },
        placeholder = { Text("Custom associated symptom (e.g. radiates to back, yellowish sputum...)", fontSize = 9.5.sp, color = SlateMuted) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
        shape = RoundedCornerShape(4.dp),
        singleLine = false, minLines = 1, maxLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 7. Complaint Specific Notes
      Text("Complaint Specific Notes (မှတ်ချက်):", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      Spacer(modifier = Modifier.height(3.dp))
      OutlinedTextField(
        value = localNotes,
        onValueChange = {
          localNotes = it
          complaint.notes = it
          onSync()
        },
        placeholder = { Text("Specific notes for this complaint (Space & Enter supported)...", fontSize = 9.5.sp, color = SlateMuted) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 10.sp, color = DarkGrayText),
        shape = RoundedCornerShape(4.dp),
        singleLine = false, minLines = 2, maxLines = 4,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
      )
    }
  }
}
