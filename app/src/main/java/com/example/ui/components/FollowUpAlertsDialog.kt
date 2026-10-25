package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Patient
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Follow-up Phone Reminder & Patient Alert Dialog.
 * Allows clinic receptionist and doctors to review patients with upcoming/overdue
 * follow-up dates, initiate direct phone calls, send automated SMS reminders,
 * and track notification status.
 */
@Composable
fun FollowUpAlertsDialog(
  patients: List<Patient>,
  onDismiss: () -> Unit,
  onMarkReminded: (patientId: String) -> Unit,
  onQueuePatient: (patient: Patient) -> Unit
) {
  val context = LocalContext.current
  val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

  // Patients with follow-up due dates
  val followUpPatients = remember(patients) {
    patients.filter { !it.followUpDueDate.isNullOrBlank() }
      .sortedBy { it.followUpDueDate }
  }

  var selectedFilter by remember { mutableStateOf("All") } // "All", "Due Today / Overdue", "Upcoming", "Reminded"
  var searchQuery by remember { mutableStateOf("") }

  val filteredList = remember(followUpPatients, selectedFilter, searchQuery) {
    followUpPatients.filter { patient ->
      val matchesSearch = searchQuery.isBlank() ||
          patient.name.contains(searchQuery, ignoreCase = true) ||
          patient.hn.contains(searchQuery, ignoreCase = true) ||
          patient.phone.contains(searchQuery)

      val dueDate = patient.followUpDueDate ?: ""
      val matchesCategory = when (selectedFilter) {
        "Due Today / Overdue" -> dueDate <= todayStr && !patient.isReminderSent
        "Upcoming" -> dueDate > todayStr && !patient.isReminderSent
        "Reminded" -> patient.isReminderSent
        else -> true
      }
      matchesSearch && matchesCategory
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 620.dp)
        .fillMaxHeight(0.88f)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
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
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFF3E0)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "Follow-up Patient Alerts",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.5.sp,
                  color = SlateDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFFFFE0B2)
                ) {
                  Text(
                    "${followUpPatients.size} Due",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFFBF360C),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                "Phone call & SMS reminders before due date (ရက်ချိန်းဖုန်းဆက်ရန်)",
                fontSize = 11.sp,
                color = SlateMuted
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search patient name, phone, HN...", fontSize = 11.5.sp, color = SlateMuted) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
          },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Clear, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(14.dp))
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          colors = clinicSearchBarColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val filterOptions = listOf("All", "Due Today / Overdue", "Upcoming", "Reminded")
          filterOptions.forEach { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { selectedFilter = filter },
              label = {
                Text(filter, fontSize = 10.5.sp)
              },
              modifier = Modifier.height(28.dp),
              shape = RoundedCornerShape(6.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Patient List
        if (filteredList.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(42.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No pending follow-up alerts in this filter.", fontSize = 12.sp, color = SlateMedium, fontWeight = FontWeight.Medium)
              Text("All scheduled patients are up to date.", fontSize = 10.5.sp, color = SlateLight)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredList, key = { it.id }) { patient ->
              FollowUpReminderCard(
                patient = patient,
                todayStr = todayStr,
                onCall = {
                  launchPhoneCall(context, patient.phone)
                },
                onSms = {
                  launchSmsReminder(context, patient)
                },
                onMarkReminded = {
                  onMarkReminded(patient.id)
                  Toast.makeText(context, "Marked as contacted: ${patient.name}", Toast.LENGTH_SHORT).show()
                },
                onQueue = {
                  onQueuePatient(patient)
                  onDismiss()
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer note
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = BgLightGray,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Shwe Khit Clinic Reminders • Call or SMS to confirm attendance before due date.",
              fontSize = 9.5.sp,
              color = SlateMedium
            )
          }
        }
      }
    }
  }
}

@Composable
fun FollowUpReminderCard(
  patient: Patient,
  todayStr: String,
  onCall: () -> Unit,
  onSms: () -> Unit,
  onMarkReminded: () -> Unit,
  onQueue: () -> Unit
) {
  val dueDate = patient.followUpDueDate ?: ""
  val isOverdue = dueDate < todayStr
  val isDueToday = dueDate == todayStr

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, if (isDueToday) Color(0xFFFF9800) else if (isOverdue) Color(0xFFEF5350) else BorderGrey)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Row 1: Patient Name, HN & Due Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(if (patient.isReminderSent) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              if (patient.isReminderSent) Icons.Default.DoneAll else Icons.Default.PhoneCallback,
              contentDescription = null,
              tint = if (patient.isReminderSent) MedicalGreen else Color(0xFFE65100),
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              patient.name,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              color = DarkGrayText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              "${patient.hn} • ${patient.age}y • ${patient.gender.name} • ${patient.phone}",
              fontSize = 10.sp,
              color = SlateMuted
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Due badge
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = when {
            patient.isReminderSent -> Color(0xFFE8F5E9)
            isDueToday -> Color(0xFFFFF3E0)
            isOverdue -> Color(0xFFFFEBEE)
            else -> Color(0xFFEDE7F6)
          }
        ) {
          Text(
            text = when {
              patient.isReminderSent -> "Contacted / Reminded"
              isDueToday -> "Due Today ($dueDate)"
              isOverdue -> "Overdue ($dueDate)"
              else -> "Due: $dueDate"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = when {
              patient.isReminderSent -> MedicalGreenDark
              isDueToday -> Color(0xFFE65100)
              isOverdue -> Color(0xFFC62828)
              else -> Color(0xFF5E35B1)
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Patient Address & Reason
      if (patient.address.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Home, contentDescription = null, tint = SlateLight, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            patient.address,
            fontSize = 10.sp,
            color = SlateMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
      }

      if (patient.followUpReason.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "Reason: ${patient.followUpReason}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4A148C),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Action Buttons: Call, SMS, Mark Contacted, Queue
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          // Direct Phone Call
          Button(
            onClick = onCall,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Call", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          // SMS Reminder
          OutlinedButton(
            onClick = onSms,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, tint = SlateDark, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("SMS", fontSize = 10.sp, color = SlateDark)
          }

          // Mark Reminded Check
          if (!patient.isReminderSent) {
            OutlinedButton(
              onClick = onMarkReminded,
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Reminded", fontSize = 9.5.sp, color = MedicalGreenDark)
            }
          }
        }

        // Add to Queue
        Button(
          onClick = onQueue,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Queue Today", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

private fun launchPhoneCall(context: Context, phone: String) {
  try {
    val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
    val intent = Intent(Intent.ACTION_DIAL).apply {
      data = Uri.parse("tel:$cleanPhone")
    }
    context.startActivity(intent)
  } catch (e: Exception) {
    Toast.makeText(context, "Could not open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}

private fun launchSmsReminder(context: Context, patient: Patient) {
  try {
    val cleanPhone = patient.phone.replace(Regex("[^0-9+]"), "")
    val reason = if (patient.followUpReason.isNotBlank()) patient.followUpReason else "ရက်ချိန်း ပြန်လည်ပြသခြင်း"
    val dueDate = patient.followUpDueDate ?: "မကြာမီ"
    val message = "မင်္ဂလာပါ ${patient.name}၊ “ရွှေခေတ်” (Shwe Khit Clinic) မှ ရက်ချိန်းပြန်ပြသရန် အသိပေးအပ်ပါသည်။ ရက်စွဲ: $dueDate ($reason)။ အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့။ ဖုန်း: 09-421001122"

    val intent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("smsto:$cleanPhone")
      putExtra("sms_body", message)
    }
    context.startActivity(intent)
  } catch (e: Exception) {
    Toast.makeText(context, "Could not open SMS: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}
