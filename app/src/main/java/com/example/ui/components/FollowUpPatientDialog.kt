package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Gender
import com.example.model.Patient
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpPatientDialog(
  existingPatients: List<Patient>,
  onDismiss: () -> Unit,
  onQueueFollowUp: (
    hn: String,
    followUpReason: String,
    bp: String,
    pulse: Int,
    temp: Double,
    spo2: Int,
    weight: Double,
    rbs: Double?
  ) -> Unit
) {
  // Unique patients by HN
  val distinctPatients = remember(existingPatients) {
    existingPatients.distinctBy { it.hn }
  }

  var searchQuery by remember { mutableStateOf("") }
  var selectedPatient by remember { mutableStateOf<Patient?>(distinctPatients.firstOrNull()) }

  val filteredPatients = remember(searchQuery, distinctPatients) {
    if (searchQuery.isBlank()) distinctPatients
    else distinctPatients.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
          it.hn.contains(searchQuery, ignoreCase = true) ||
          it.phone.contains(searchQuery)
    }
  }

  // Reason presets
  val reasonPresets = listOf(
    "BP Recheck",
    "Dressing / Suture Removal",
    "Fever Recheck",
    "Rx Refill",
    "Lab Results Review",
    "Gastritis Recheck"
  )

  var followUpReason by remember(selectedPatient) {
    mutableStateOf(selectedPatient?.followUpReason?.ifBlank { "BP Recheck" } ?: "Follow-up Visit")
  }

  // Vitals for today
  var bp by remember(selectedPatient) { mutableStateOf(selectedPatient?.vitals?.bloodPressure ?: "120/80 mmHg") }
  var pulseText by remember(selectedPatient) { mutableStateOf(selectedPatient?.vitals?.pulseRate?.toString() ?: "76") }
  var tempText by remember(selectedPatient) { mutableStateOf(selectedPatient?.vitals?.let { String.format(java.util.Locale.US, "%.1f", it.temperatureFahrenheit) } ?: "98.4") }
  var spo2Text by remember(selectedPatient) { mutableStateOf(selectedPatient?.vitals?.oxygenSaturation?.toString() ?: "98") }
  var weightText by remember(selectedPatient) { mutableStateOf(selectedPatient?.vitals?.weightKg?.toString() ?: "60.0") }
  var rbsText by remember(selectedPatient) { mutableStateOf(selectedPatient?.vitals?.rbs?.toInt()?.toString() ?: "") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Dialog Header
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
                .background(Color(0xFFEDE7F6)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.EventRepeat, contentDescription = null, tint = Color(0xFF5E35B1), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "Follow-up / 2nd Visit Registration",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SlateDark
              )
              Text(
                "Search previous patient and queue for follow-up",
                fontSize = 11.sp,
                color = SlateMuted
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(18.dp))
          }
        }

        Divider(color = BorderGrey, thickness = 0.8.dp)

        // 1. Search Existing Patient
        Text("1. Search Existing Patient", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = SlateDark)
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search patient...", fontSize = 11.sp, color = SlateMuted) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          colors = clinicSearchBarColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        // Patient Selection list / chips
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
            .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (filteredPatients.isEmpty()) {
            Text(
              "မတွေ့ရှိပါ။ အမည် သို့မဟုတ် HN စစ်ဆေးပါ",
              fontSize = 11.sp,
              color = SlateMuted,
              modifier = Modifier.padding(8.dp)
            )
          } else {
            val selHn = selectedPatient?.hn
            filteredPatients.forEach { p ->
              val isSel = selHn != null && p.hn == selHn
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isSel) SoftGreenContainer else PureWhite)
                  .clickable { selectedPatient = p }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(if (isSel) MedicalGreen else SkyBlueLight),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      p.name.take(1),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSel) PureWhite else SkyBluePrimary
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      p.name,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = SlateDark
                    )
                    Text(
                      "${p.hn} • ${p.age}y/${if (p.gender == Gender.MALE) "M" else "F"} • ${p.phone}",
                      fontSize = 10.sp,
                      color = SlateMuted
                    )
                  }
                }

                if (isSel) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }

        // Selected Patient Snapshot Card
        selectedPatient?.let { patient ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = SoftGreenContainer.copy(alpha = 0.6f)),
            border = CardDefaults.outlinedCardBorder()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "Selected: ${patient.name} (${patient.hn})",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = MedicalGreenDark
                )
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFFEDE7F6)
                ) {
                  Text(
                    "Visit #${patient.visitNumber + 1}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5E35B1),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              if (patient.address.isNotBlank()) {
                Text(
                  "Address: ${patient.address}",
                  fontSize = 10.sp,
                  color = SlateMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
              }
              Text(
                "Previous complaint: ${patient.chiefComplaint}",
                fontSize = 10.5.sp,
                color = SlateDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (patient.allergies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  "Allergies: ${patient.allergies.joinToString(", ")}",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = StatusCritical
                )
              }
            }
          }
        }

        // 2. Reason for Follow-up / 2nd Visit
        Text("2. Reason for Follow-up / 2nd Visit", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateDark)
        
        // Quick Presets
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          reasonPresets.take(3).forEach { preset ->
            FilterChip(
              selected = followUpReason == preset,
              onClick = { followUpReason = preset },
              label = { Text(preset.substringBefore(" ("), fontSize = 10.sp) },
              modifier = Modifier.height(28.dp)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          reasonPresets.drop(3).forEach { preset ->
            FilterChip(
              selected = followUpReason == preset,
              onClick = { followUpReason = preset },
              label = { Text(preset.substringBefore(" ("), fontSize = 10.sp) },
              modifier = Modifier.height(28.dp)
            )
          }
        }

        OutlinedTextField(
          value = followUpReason,
          onValueChange = { followUpReason = it },
          label = { Text("Follow-up Complaint / Details") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors()
        )

        // 3. Today's Vital Signs
        Text("3. Vital Signs Today", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateDark)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = bp,
            onValueChange = { bp = it },
            label = { Text("BP (mmHg)") },
            modifier = Modifier.weight(1.2f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
          OutlinedTextField(
            value = pulseText,
            onValueChange = { pulseText = it },
            label = { Text("PR (bpm)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
          OutlinedTextField(
            value = tempText,
            onValueChange = { tempText = it },
            label = { Text("Temp (°F)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = spo2Text,
            onValueChange = { spo2Text = it },
            label = { Text("SpO₂ (%)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
          OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
        }

        OutlinedTextField(
          value = rbsText,
          onValueChange = { rbsText = it },
          label = { Text("RBS (Blood Sugar mg/dL) - Optional / လိုအပ်ပါက") },
          placeholder = { Text("e.g. 110 (Normal: 70-140 mg/dL)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          leadingIcon = {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
          }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Text("Cancel", fontSize = 11.sp, color = SlateDark)
          }

          Spacer(modifier = Modifier.width(6.dp))

          Button(
            onClick = {
              val patient = selectedPatient
              if (patient != null) {
                onQueueFollowUp(
                  patient.hn,
                  followUpReason,
                  bp,
                  pulseText.toIntOrNull() ?: 76,
                  tempText.toDoubleOrNull() ?: 36.8,
                  spo2Text.toIntOrNull() ?: 98,
                  weightText.toDoubleOrNull() ?: 60.0,
                  rbsText.toDoubleOrNull()
                )
                onDismiss()
              }
            },
            enabled = selectedPatient != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add to Queue", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }
      }
    }
  }
}
