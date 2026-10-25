package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Patient
import com.example.model.QueueStatus
import com.example.ui.components.AllergyWarningBadge
import com.example.ui.components.ChronicDiseaseTrendsDialog
import com.example.ui.components.EditPatientDialog
import com.example.ui.components.FollowUpAlertsDialog
import com.example.ui.components.FollowUpPatientDialog
import com.example.ui.components.QueueStatusChip
import com.example.ui.components.RegisterPatientDialog
import com.example.ui.components.VaccinationTrackerDialog
import com.example.ui.components.VitalSignsDisplay
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientQueueScreen(
  viewModel: ClinicViewModel,
  onNavigateToConsultation: (String) -> Unit
) {
  val patients by viewModel.patients.collectAsState()
  val vaccineRecords by viewModel.vaccineRecords.collectAsState()
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedStatusFilter by remember { mutableStateOf<QueueStatus?>(null) }
  var filterFollowUpOnly by remember { mutableStateOf(false) }
  var showRegisterDialog by remember { mutableStateOf(false) }
  var showFollowUpDialog by remember { mutableStateOf(false) }
  var showFollowUpAlertsDialog by remember { mutableStateOf(false) }
  var selectedTrendsPatient by remember { mutableStateOf<Patient?>(null) }
  var selectedVaccinesPatient by remember { mutableStateOf<Patient?>(null) }

  val dueFollowUpCount = remember(patients) {
    patients.count { !it.followUpDueDate.isNullOrBlank() && !it.isReminderSent }
  }

  // Date Filter State
  var selectedDateFilter by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "YESTERDAY", "PAST_7_DAYS", "CUSTOM"
  var customDateFilter by remember { mutableStateOf<String?>(null) }
  var showCustomDatePicker by remember { mutableStateOf(false) }
  var patientToDelete by remember { mutableStateOf<Patient?>(null) }
  var patientToEdit by remember { mutableStateOf<Patient?>(null) }

  val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val yesterdayDateStr = remember {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }
  val past7DaysStartDate = remember {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -6)
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }

  val filteredPatients = patients.filter { p ->
    val matchesSearch = searchQuery.isBlank() ||
        p.name.contains(searchQuery, ignoreCase = true) ||
        p.hn.contains(searchQuery, ignoreCase = true) ||
        p.phone.contains(searchQuery)
    val matchesStatus = selectedStatusFilter == null || p.status == selectedStatusFilter
    val matchesFollowUp = !filterFollowUpOnly || p.isFollowUp

    val matchesDate = when (selectedDateFilter) {
      "ALL" -> true
      "TODAY" -> p.registrationDate == todayDateStr || p.registeredAt.startsWith(todayDateStr)
      "YESTERDAY" -> p.registrationDate == yesterdayDateStr || p.registeredAt.startsWith(yesterdayDateStr)
      "PAST_7_DAYS" -> p.registrationDate >= past7DaysStartDate && p.registrationDate <= todayDateStr
      "CUSTOM" -> customDateFilter == null || p.registrationDate == customDateFilter || p.registeredAt.startsWith(customDateFilter ?: "")
      else -> true
    }

    matchesSearch && matchesStatus && matchesFollowUp && matchesDate
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(BackgroundSurface)
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      // Clean Header Strip: Clinic Queue Title, New Patient & Follow-up Actions
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Row 1: Title and Follow-up Alerts
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
                  .background(SoftGreenContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.PeopleAlt,
                  contentDescription = null,
                  tint = MedicalGreen,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    "Patient Queue & EMR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = DarkGrayText
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftGreenContainer
                  ) {
                    Text(
                      "${patients.size} Total",
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.5.sp,
                      color = MedicalGreenDark,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  "Reception & Clinical Workflow",
                  fontSize = 10.sp,
                  color = SlateMuted
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              // Day-Care Treatment Cards Shortcut
              Button(
                onClick = { viewModel.setTab(9) },
                colors = ButtonDefaults.buttonColors(containerColor = OceanGreenLight),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                modifier = Modifier.height(30.dp)
              ) {
                Icon(
                  Icons.Default.Healing,
                  contentDescription = null,
                  tint = OceanGreenPrimary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "ဆေးထိုးကတ်",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = OceanGreenPrimary
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              // Follow-up Alerts (Phone & SMS Reminders)
              Button(
                onClick = { showFollowUpAlertsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = if (dueFollowUpCount > 0) Color(0xFFE65100) else BgLightGray),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                modifier = Modifier.height(30.dp)
              ) {
              Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (dueFollowUpCount > 0) PureWhite else SlateDark,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Alerts${if (dueFollowUpCount > 0) " ($dueFollowUpCount)" else ""}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (dueFollowUpCount > 0) PureWhite else SlateDark
              )
            }
          }
        }

          // Row 2: Action Buttons (Prominent + New Patient & + Follow-up)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // New Patient Primary Action
            Button(
              onClick = { showRegisterDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
            ) {
              Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("+ New Patient", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            // Follow-up / 2nd Visit Action
            Button(
              onClick = { showFollowUpDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
            ) {
              Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(5.dp))
              Text("+ Follow-up", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Compact Search Field with Grey Background
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search patient...", fontSize = 11.5.sp, color = SlateMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMuted, modifier = Modifier.size(14.dp))
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        singleLine = true,
        colors = clinicSearchBarColors(),
        textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
      )

      Spacer(modifier = Modifier.height(5.dp))

      // Scrollable Filter Chips Row (Ensures In Consultation and all statuses are fully readable)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        FilterChip(
          selected = selectedStatusFilter == null && !filterFollowUpOnly,
          onClick = {
            selectedStatusFilter = null
            filterFollowUpOnly = false
          },
          label = { Text("All (${patients.size})", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
          modifier = Modifier.height(30.dp),
          shape = RoundedCornerShape(6.dp)
        )
        // Follow-up Filter Chip
        val followUpCount = patients.count { it.isFollowUp }
        FilterChip(
          selected = filterFollowUpOnly,
          onClick = { filterFollowUpOnly = !filterFollowUpOnly },
          leadingIcon = {
            Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (filterFollowUpOnly) PureWhite else Color(0xFF5E35B1))
          },
          label = { Text("Follow-up ($followUpCount)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) },
          modifier = Modifier.height(30.dp),
          shape = RoundedCornerShape(6.dp)
        )
        QueueStatus.entries.forEach { status ->
          val count = patients.count { it.status == status }
          FilterChip(
            selected = selectedStatusFilter == status,
            onClick = {
              selectedStatusFilter = if (selectedStatusFilter == status) null else status
              if (selectedStatusFilter != null) filterFollowUpOnly = false
            },
            label = {
              Text(
                "${status.displayName} ($count)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
              )
            },
            modifier = Modifier.height(30.dp),
            shape = RoundedCornerShape(6.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Date Filter Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = PureWhite,
          border = androidx.compose.foundation.BorderStroke(0.8.dp, BorderGrey)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("ရက်စွဲ:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          }
        }

        FilterChip(
          selected = selectedDateFilter == "ALL",
          onClick = { selectedDateFilter = "ALL" },
          label = { Text("ရက်စွဲအားလုံး", fontSize = 9.5.sp) },
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(4.dp)
        )

        val todayCount = patients.count { it.registrationDate == todayDateStr || it.registeredAt.startsWith(todayDateStr) }
        FilterChip(
          selected = selectedDateFilter == "TODAY",
          onClick = { selectedDateFilter = "TODAY" },
          label = { Text("ယနေ့ ($todayCount)", fontSize = 9.5.sp, fontWeight = if (selectedDateFilter == "TODAY") FontWeight.Bold else FontWeight.Normal) },
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(4.dp)
        )

        val yesterdayCount = patients.count { it.registrationDate == yesterdayDateStr || it.registeredAt.startsWith(yesterdayDateStr) }
        FilterChip(
          selected = selectedDateFilter == "YESTERDAY",
          onClick = { selectedDateFilter = "YESTERDAY" },
          label = { Text("မနေ့က ($yesterdayCount)", fontSize = 9.5.sp) },
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(4.dp)
        )

        val past7DaysCount = patients.count { it.registrationDate >= past7DaysStartDate && it.registrationDate <= todayDateStr }
        FilterChip(
          selected = selectedDateFilter == "PAST_7_DAYS",
          onClick = { selectedDateFilter = "PAST_7_DAYS" },
          label = { Text("လွန်ခဲ့သော ၇ ရက် ($past7DaysCount)", fontSize = 9.5.sp) },
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(4.dp)
        )

        FilterChip(
          selected = selectedDateFilter == "CUSTOM",
          onClick = {
            selectedDateFilter = "CUSTOM"
            showCustomDatePicker = true
          },
          leadingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(11.dp))
          },
          label = {
            Text(
              if (selectedDateFilter == "CUSTOM" && customDateFilter != null) customDateFilter!! else "ရက်စွဲရွေးရန်...",
              fontSize = 9.5.sp
            )
          },
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(4.dp)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Patients List
      if (filteredPatients.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = SlateLight, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No patients matching criteria.", color = SlateMuted, fontSize = 12.sp)
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredPatients, key = { it.id }) { patient ->
            PatientQueueItemCard(
              patient = patient,
              onSelectForConsultation = {
                viewModel.selectPatient(patient.id)
                viewModel.updatePatientStatus(patient.id, QueueStatus.IN_CONSULTATION)
                onNavigateToConsultation(patient.id)
              },
              onViewMedicalRecord = { p ->
                viewModel.selectPatient(p.id)
                viewModel.setTab(8) // Navigate directly to dedicated Medical Records Archive
              },
              onRegisterFollowUp = { p ->
                showFollowUpDialog = true
              },
              onOpenTrends = { selectedTrendsPatient = patient },
              onOpenVaccines = { selectedVaccinesPatient = patient },
              onDeletePatient = { patientToDelete = it },
              onEditPatient = { patientToEdit = it }
            )
          }
        }
      }
    }
  }

  if (showRegisterDialog) {
    RegisterPatientDialog(
      onDismiss = { showRegisterDialog = false },
      onRegister = { name, age, gender, phone, address, bloodType, allergies, chiefComplaint, bp, pulse, temp, spo2, weight, rbs ->
        viewModel.registerPatient(
          name, age, gender, phone, address, bloodType, allergies, chiefComplaint, bp, pulse, temp, spo2, weight, rbs
        )
      }
    )
  }

  if (showFollowUpDialog) {
    FollowUpPatientDialog(
      existingPatients = patients,
      onDismiss = { showFollowUpDialog = false },
      onQueueFollowUp = { hn, reason, bp, pulse, temp, spo2, weight, rbs ->
        viewModel.registerFollowUpPatient(hn, reason, bp, pulse, temp, spo2, weight, rbs)
      }
    )
  }

  if (showFollowUpAlertsDialog) {
    FollowUpAlertsDialog(
      patients = patients,
      onDismiss = { showFollowUpAlertsDialog = false },
      onMarkReminded = { patientId ->
        viewModel.markReminderSent(patientId)
      },
      onQueuePatient = { patient ->
        viewModel.registerFollowUpPatient(
          patient.hn,
          patient.followUpReason.ifBlank { "Scheduled Follow-up" },
          patient.vitals.bloodPressure,
          patient.vitals.pulseRate,
          patient.vitals.temperatureFahrenheit,
          patient.vitals.oxygenSaturation,
          patient.vitals.weightKg,
          patient.vitals.rbs
        )
      }
    )
  }

  selectedTrendsPatient?.let { p ->
    val vitalHistory = viewModel.getPatientVitalsHistory(p.id)
    ChronicDiseaseTrendsDialog(
      patient = p,
      vitalHistory = vitalHistory,
      onDismiss = { selectedTrendsPatient = null },
      onAddVitalPoint = { point ->
        viewModel.addVitalHistoryPoint(p.id, point)
      }
    )
  }

  selectedVaccinesPatient?.let { p ->
    val patientVaccines = remember(vaccineRecords, p.id) {
      vaccineRecords.filter { it.patientId == p.id }
    }
    VaccinationTrackerDialog(
      patient = p,
      vaccines = patientVaccines,
      onDismiss = { selectedVaccinesPatient = null },
      onAddVaccine = { rec -> viewModel.addVaccineRecord(rec) },
      onUpdateVaccine = { rec -> viewModel.saveOrUpdateVaccineRecord(rec) },
      onDeleteVaccine = { id -> viewModel.deleteVaccineRecord(id) }
    )
  }

  // Delete Patient Confirmation Dialog
  if (patientToDelete != null) {
    AlertDialog(
      onDismissRequest = { patientToDelete = null },
      icon = {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
      },
      title = {
        Text("လူနာမှတ်တမ်း ဖျက်ပစ်ရန် အတည်ပြုပါ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      },
      text = {
        Text(
          "လူနာ \"${patientToDelete?.name}\" (${patientToDelete?.hn}) အား စာရင်းမှ အပြီးတိုင် ဖျက်ပစ်ပါမည်လား?\n\nမှားယွင်းစာရင်းသွင်းမိခြင်း သို့မဟုတ် စမ်းသပ်စာရင်းများအတွက်သာ ဖျက်ပေးပါရန်။ (ဖျက်ပြီးပါက အချက်အလက်များ ပြန်လည်ရယူ၍ မရနိုင်ပါ)",
          fontSize = 11.sp,
          color = DarkGrayText,
          lineHeight = 16.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            patientToDelete?.let { p ->
              viewModel.deletePatient(p.id)
            }
            patientToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("ဖျက်မည် (Delete Patient)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { patientToDelete = null },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("မဖျက်တော့ပါ (Cancel)", fontSize = 11.sp)
        }
      }
    )
  }

  // Edit Patient Information & Vitals Dialog
  if (patientToEdit != null) {
    EditPatientDialog(
      patient = patientToEdit!!,
      onDismiss = { patientToEdit = null },
      onSave = { updated: Patient ->
        viewModel.updatePatient(updated)
        patientToEdit = null
      }
    )
  }

  // Custom Date Picker Dialog
  if (showCustomDatePicker) {
    val availableDates = remember(patients) {
      patients.map { it.registrationDate }
        .filter { it.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) }
        .distinct()
        .sortedDescending()
    }
    var inputDate by remember { mutableStateOf(customDateFilter ?: todayDateStr) }

    Dialog(onDismissRequest = { showCustomDatePicker = false }) {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).padding(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("လူနာကြည့်ရှုလိုသည့် ရက်စွဲရွေးချယ်ပါ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkGrayText)
            IconButton(onClick = { showCustomDatePicker = false }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          if (availableDates.isNotEmpty()) {
            Text("စနစ်ထဲရှိ မှတ်တမ်းတင်ထားသော ရက်စွဲများ:", fontSize = 10.sp, color = SlateMuted)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              availableDates.forEach { d ->
                val count = patients.count { it.registrationDate == d }
                FilterChip(
                  selected = inputDate == d,
                  onClick = { inputDate = d },
                  label = { Text("$d ($count ဦး)", fontSize = 9.5.sp) }
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
          }
          OutlinedTextField(
            value = inputDate,
            onValueChange = { inputDate = it },
            label = { Text("ရက်စွဲ (YYYY-MM-DD)", fontSize = 10.sp) },
            trailingIcon = {
              IconButton(onClick = {
                val cal = Calendar.getInstance()
                try {
                  val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(inputDate)
                  if (parsed != null) cal.time = parsed
                } catch (_: Exception) {}
                android.app.DatePickerDialog(
                  context,
                  { _, y, m, d ->
                    inputDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                  },
                  cal.get(Calendar.YEAR),
                  cal.get(Calendar.MONTH),
                  cal.get(Calendar.DAY_OF_MONTH)
                ).show()
              }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MedicalGreen, modifier = Modifier.size(18.dp))
              }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = { showCustomDatePicker = false }) {
              Text("Cancel", fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                customDateFilter = inputDate
                selectedDateFilter = "CUSTOM"
                showCustomDatePicker = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
            ) {
              Text("ရွေးချယ်မည် (Filter)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun PatientQueueItemCard(
  patient: Patient,
  onSelectForConsultation: () -> Unit,
  onViewMedicalRecord: (Patient) -> Unit = {},
  onRegisterFollowUp: (Patient) -> Unit = {},
  onOpenTrends: () -> Unit = {},
  onOpenVaccines: () -> Unit = {},
  onDeletePatient: (Patient) -> Unit = {},
  onEditPatient: (Patient) -> Unit = {}
) {
  var showMenu by remember { mutableStateOf(false) }
  val context = LocalContext.current

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable {
        if (patient.status == QueueStatus.WAITING || patient.status == QueueStatus.IN_CONSULTATION) {
          onSelectForConsultation()
        } else {
          onViewMedicalRecord(patient)
        }
      },
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Row 1: Queue # + Patient Name + Age/Gender + Status Badge + 3-dot Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          // Token / Queue #
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (patient.isFollowUp) Color(0xFFEDE7F6) else OceanGreenLight),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "#${patient.queueNumber}",
              fontWeight = FontWeight.Bold,
              color = if (patient.isFollowUp) Color(0xFF5E35B1) else OceanGreenPrimary,
              fontSize = 13.sp
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                patient.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SlateDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (patient.isFollowUp) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFFEDE7F6)
                ) {
                  Text(
                    "Follow-up (#${patient.visitNumber})",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5E35B1),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                  )
                }
              }
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                "${patient.hn} • ${patient.age}y • ${patient.gender.name}",
                fontSize = 11.sp,
                color = SlateMuted
              )
              if (patient.phone.isNotBlank()) {
                Text(" • ", fontSize = 11.sp, color = SlateMuted)
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                      try {
                        val cleanPhone = patient.phone.replace(Regex("[^0-9+]"), "")
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                      } catch (e: Exception) {
                        Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                      }
                    }
                    .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                  Icon(
                    Icons.Default.Phone,
                    contentDescription = "Direct Call ${patient.phone}",
                    tint = MedicalGreenDark,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    patient.phone,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MedicalGreenDark,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Status Badge + 3-dot Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
          QueueStatusChip(patient.status)

          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.MoreVert,
                contentDescription = "More actions",
                tint = SlateMedium,
                modifier = Modifier.size(18.dp)
              )
            }

            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("ပြင်ဆင်မည် (Edit)", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
                onClick = {
                  showMenu = false
                  onEditPatient(patient)
                }
              )
              DropdownMenuItem(
                text = { Text("Trends / ဂရပ် (Vitals)", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(16.dp)) },
                onClick = {
                  showMenu = false
                  onOpenTrends()
                }
              )
              DropdownMenuItem(
                text = { Text("Vaccines မှတ်တမ်း", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Vaccines, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(16.dp)) },
                onClick = {
                  showMenu = false
                  onOpenVaccines()
                }
              )
              if (patient.phone.isNotBlank()) {
                DropdownMenuItem(
                  text = { Text("ဖုန်းခေါ်ဆိုမည် (${patient.phone})", fontSize = 11.sp, color = MedicalGreenDark) },
                  leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(16.dp)) },
                  onClick = {
                    showMenu = false
                    try {
                      val cleanPhone = patient.phone.replace(Regex("[^0-9+]"), "")
                      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                    } catch (e: Exception) {
                      Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                  }
                )
              }
              HorizontalDivider()
              DropdownMenuItem(
                text = { Text("ဖျက်မည် (Delete)", fontSize = 11.sp, color = Color(0xFFDC2626)) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) },
                onClick = {
                  showMenu = false
                  onDeletePatient(patient)
                }
              )
            }
          }
        }
      }

      // Row 2: Follow-up reason (if applicable)
      if (patient.isFollowUp && patient.followUpReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFFF3E5F5),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Follow-up: ${patient.followUpReason}", fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, color = Color(0xFF4A148C))
          }
        }
      }

      // Row 3: Chief Complaint & Registration Date
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "Complaint: ${patient.chiefComplaint}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = SlateMedium,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          patient.formattedRegistrationDisplay,
          fontSize = 9.5.sp,
          color = SlateLight
        )
      }

      // Row 4: Allergy Alert (only if any)
      if (patient.allergies.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        AllergyWarningBadge(patient.allergies)
      }

      // Row 5: Compact Vital Signs Bar
      Spacer(modifier = Modifier.height(5.dp))
      VitalSignsDisplay(patient.vitals)

      // Row 6: Bottom Primary Action Row
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Quick glance buttons: Trends and Vaccines
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          OutlinedButton(
            onClick = onOpenTrends,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(11.dp), tint = OceanGreenPrimary)
            Spacer(modifier = Modifier.width(2.dp))
            Text("Trends", fontSize = 9.sp, color = DarkGrayText, fontWeight = FontWeight.SemiBold)
          }

          OutlinedButton(
            onClick = onOpenVaccines,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Icon(Icons.Default.Vaccines, contentDescription = null, modifier = Modifier.size(11.dp), tint = SkyBluePrimary)
            Spacer(modifier = Modifier.width(2.dp))
            Text("Vaccines", fontSize = 9.sp, color = DarkGrayText, fontWeight = FontWeight.SemiBold)
          }
        }

        // Primary Action Button (Prominent Consult / Review)
        if (patient.status == QueueStatus.WAITING || patient.status == QueueStatus.IN_CONSULTATION) {
          Button(
            onClick = onSelectForConsultation,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (patient.status == QueueStatus.IN_CONSULTATION) MedicalGreen else OceanGreenPrimary
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(
              if (patient.status == QueueStatus.IN_CONSULTATION) Icons.Default.PlayArrow else Icons.Default.MedicalServices,
              contentDescription = null,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              if (patient.status == QueueStatus.IN_CONSULTATION) "ဆက်ကြည့်မည်" else "Consult စမည်",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
              onClick = { onViewMedicalRecord(patient) },
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(12.dp), tint = SkyBlueDark)
              Spacer(modifier = Modifier.width(3.dp))
              Text("ဆေးမှတ်တမ်း", fontSize = 10.sp, color = DarkGrayText, fontWeight = FontWeight.SemiBold)
            }

            if (patient.status == QueueStatus.COMPLETED) {
              Button(
                onClick = { onRegisterFollowUp(patient) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("+ Follow-up", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

