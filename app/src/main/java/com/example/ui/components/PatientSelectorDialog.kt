package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Patient
import com.example.model.QueueStatus
import com.example.ui.theme.*

enum class PatientQueueFilter(val label: String) {
  ALL("All"),
  WAITING("Waiting"),
  IN_CONSULTATION("In Progress"),
  FOLLOW_UP("Follow-up")
}

@Composable
fun PatientSelectorDialog(
  patients: List<Patient>,
  activePatientId: String?,
  onSelectPatient: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var activeFilter by remember { mutableStateOf(PatientQueueFilter.ALL) }

  val filteredPatients = patients.filter { p ->
    val matchesSearch = searchQuery.isBlank() ||
        p.name.contains(searchQuery, ignoreCase = true) ||
        p.hn.contains(searchQuery, ignoreCase = true) ||
        p.phone.contains(searchQuery, ignoreCase = true) ||
        p.chiefComplaint.contains(searchQuery, ignoreCase = true) ||
        p.followUpReason.contains(searchQuery, ignoreCase = true)

    val matchesFilter = when (activeFilter) {
      PatientQueueFilter.ALL -> true
      PatientQueueFilter.WAITING -> p.status == QueueStatus.WAITING
      PatientQueueFilter.IN_CONSULTATION -> p.status == QueueStatus.IN_CONSULTATION
      PatientQueueFilter.FOLLOW_UP -> p.isFollowUp
    }

    matchesSearch && matchesFilter
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .wrapContentHeight()
        .clip(RoundedCornerShape(10.dp)),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGray)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.People, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Select Patient",
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
              color = DarkGrayText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("(${filteredPatients.size})", fontSize = 11.sp, color = SlateMuted)
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(15.dp))
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Input with Grey Background & Short placeholder
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search patient name, HN, phone...", fontSize = 11.sp, color = SlateMuted) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp)) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMuted, modifier = Modifier.size(13.dp))
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          colors = clinicSearchBarColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Filter Chips Row (Compact)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          PatientQueueFilter.entries.forEach { filter ->
            val count = when (filter) {
              PatientQueueFilter.ALL -> patients.size
              PatientQueueFilter.WAITING -> patients.count { it.status == QueueStatus.WAITING }
              PatientQueueFilter.IN_CONSULTATION -> patients.count { it.status == QueueStatus.IN_CONSULTATION }
              PatientQueueFilter.FOLLOW_UP -> patients.count { it.isFollowUp }
            }
            val isSel = activeFilter == filter
            FilterChip(
              selected = isSel,
              onClick = { activeFilter = filter },
              label = { Text("${filter.label} ($count)", fontSize = 10.sp) },
              modifier = Modifier.height(26.dp),
              shape = RoundedCornerShape(4.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SoftGreenContainer,
                selectedLabelColor = MedicalGreenDark,
                containerColor = PureWhite,
                labelColor = SlateMedium
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSel,
                borderColor = BorderGray,
                selectedBorderColor = MedicalGreen
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Patient List
        if (filteredPatients.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.SearchOff, contentDescription = null, tint = SlateLight, modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(4.dp))
              Text("No matching patients found", fontSize = 11.5.sp, color = SlateMuted)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            items(filteredPatients, key = { it.id }) { patient ->
              val isSelected = patient.id == activePatientId

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onSelectPatient(patient.id)
                    onDismiss()
                  },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) SoftGreenContainer.copy(alpha = 0.7f) else PureWhite,
                border = BorderStroke(
                  if (isSelected) 1.dp else 0.5.dp,
                  if (isSelected) MedicalGreen else BorderGray
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // Queue Token Badge
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(if (patient.isFollowUp) Color(0xFFF3E8FF) else if (isSelected) MedicalGreen else SoftGreenContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      "#${patient.queueNumber}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      color = if (patient.isFollowUp) Color(0xFF7E22CE) else if (isSelected) PureWhite else MedicalGreenDark
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  // Patient Details (Name clearly displayed and prominent)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      patient.name,
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = DarkGrayText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                      "${patient.hn} • ${patient.age}y / ${patient.gender.name.first()} • ${patient.phone}",
                      fontSize = 10.5.sp,
                      color = SlateMuted,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      if (patient.isFollowUp && patient.followUpReason.isNotBlank())
                        "Follow-up: ${patient.followUpReason}"
                      else
                        "Complaint: ${patient.chiefComplaint}",
                      fontSize = 10.sp,
                      color = SlateDark,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        "BP ${patient.vitals.bloodPressure} • HR ${patient.vitals.pulseRate} • SpO2 ${patient.vitals.oxygenSaturation}%",
                        fontSize = 9.sp,
                        color = SlateMuted,
                        maxLines = 1
                      )
                      if (patient.vitals.rbs != null) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Surface(
                          shape = RoundedCornerShape(3.dp),
                          color = Color(0xFFFFEDD5),
                          border = BorderStroke(0.5.dp, Color(0xFFFDBA74))
                        ) {
                          Text(
                            "RBS ${patient.vitals.rbs.toInt()}",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C),
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                          )
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.width(6.dp))

                  // Compact Select / Status Button
                  Button(
                    onClick = {
                      onSelectPatient(patient.id)
                      onDismiss()
                    },
                    modifier = Modifier.height(26.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isSelected) MedicalGreen else SoftGreenContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text(
                      if (isSelected) "Active" else "Select",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) PureWhite else MedicalGreenDark
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
