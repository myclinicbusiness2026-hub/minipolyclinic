package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.ChronicDiseaseTrendsDialog
import com.example.ui.components.EditPatientDialog
import com.example.ui.components.FollowUpPatientDialog
import com.example.ui.components.MedicalAttachmentsDialog
import com.example.ui.components.VaccinationTrackerDialog
import com.example.ui.components.StructuredClinicalEditDialog
import com.example.ui.components.FormattedClinicalSectionView
import com.example.ui.theme.*
import com.example.util.BurmeseDateUtils
import com.example.viewmodel.ClinicViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * Dedicated Medical Records (EMR Archive & History) Module
 * "လူနာ ဆေးမှတ်တမ်းများ (Archive & EMR Lookup)"
 *
 * Provides a dedicated, safe space for doctors and clinic staff to search,
 * browse, and review past medical records without triggering an auto-consultation
 * or accidentally creating a follow-up visit.
 */
@Composable
fun MedicalRecordsScreen(
  viewModel: ClinicViewModel,
  onNavigateToQueue: () -> Unit = {}
) {
  val patients by viewModel.patients.collectAsState()
  val consultations by viewModel.consultations.collectAsState()
  val invoices by viewModel.invoices.collectAsState()
  val vaccineRecords by viewModel.vaccineRecords.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedPatientId by remember { mutableStateOf<String?>(null) }
  var selectedDateFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, YESTERDAY, PAST_7_DAYS, THIS_MONTH, CUSTOM
  var customDateFilter by remember { mutableStateOf<String?>(null) }
  var showCustomDatePicker by remember { mutableStateOf(false) }
  var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, COMPLETED, FOLLOW_UP_DUE, IN_QUEUE
  var mobileViewingDetail by remember { mutableStateOf(false) }

  // Modals for attached actions
  var trendsPatient by remember { mutableStateOf<Patient?>(null) }
  var vaccinesPatient by remember { mutableStateOf<Patient?>(null) }
  var attachmentsConsultation by remember { mutableStateOf<ConsultationRecord?>(null) }
  var showFollowUpConfirmDialog by remember { mutableStateOf<Patient?>(null) }
  var editingConsultation by remember { mutableStateOf<ConsultationRecord?>(null) }
  var editingPatient by remember { mutableStateOf<Patient?>(null) }
  val context = androidx.compose.ui.platform.LocalContext.current

  val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val yesterdayIso = remember {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }
  val sevenDaysAgoIso = remember {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }
  val thisMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }

  // Filtered Patients List
  val filteredPatients = remember(patients, consultations, searchQuery, selectedDateFilter, customDateFilter, selectedStatusFilter) {
    patients.filter { patient ->
      // Query match: Name, HN, Phone, Address, Chief complaint
      val matchesQuery = searchQuery.isBlank() ||
          patient.name.contains(searchQuery, ignoreCase = true) ||
          patient.hn.contains(searchQuery, ignoreCase = true) ||
          patient.phone.contains(searchQuery, ignoreCase = true) ||
          patient.chiefComplaint.contains(searchQuery, ignoreCase = true) ||
          patient.address.contains(searchQuery, ignoreCase = true)

      // Date match: based on registrationDate or past consultation dates
      val patientConsultations = consultations.filter { it.patientId == patient.id }
      val latestConsultDate = patientConsultations.maxByOrNull { it.timestamp }?.timestamp?.take(10) ?: patient.registrationDate

      val matchesDate = when (selectedDateFilter) {
        "ALL" -> true
        "TODAY" -> patient.registrationDate == todayIso || latestConsultDate == todayIso
        "YESTERDAY" -> patient.registrationDate == yesterdayIso || latestConsultDate == yesterdayIso
        "PAST_7_DAYS" -> patient.registrationDate >= sevenDaysAgoIso || latestConsultDate >= sevenDaysAgoIso
        "THIS_MONTH" -> patient.registrationDate.startsWith(thisMonthPrefix) || latestConsultDate.startsWith(thisMonthPrefix)
        "CUSTOM" -> customDateFilter == null || patient.registrationDate == customDateFilter || latestConsultDate == customDateFilter
        else -> true
      }

      // Status match
      val matchesStatus = when (selectedStatusFilter) {
        "ALL" -> true
        "COMPLETED" -> patient.status == QueueStatus.COMPLETED
        "FOLLOW_UP_DUE" -> !patient.followUpDueDate.isNullOrBlank()
        "IN_QUEUE" -> patient.status == QueueStatus.WAITING || patient.status == QueueStatus.IN_CONSULTATION || patient.status == QueueStatus.AT_PHARMACY || patient.status == QueueStatus.AT_BILLING
        else -> true
      }

      matchesQuery && matchesDate && matchesStatus
    }
  }

  // Active Selected Patient
  val activePatient = remember(filteredPatients, selectedPatientId, patients) {
    if (selectedPatientId != null) {
      patients.find { it.id == selectedPatientId } ?: filteredPatients.firstOrNull()
    } else {
      filteredPatients.firstOrNull()
    }
  }

  val patientConsultations = remember(activePatient?.id, consultations) {
    if (activePatient != null) {
      consultations.filter { it.patientId == activePatient.id }.sortedByDescending { it.timestamp }
    } else {
      emptyList()
    }
  }

  val patientInvoices = remember(activePatient?.id, invoices) {
    if (activePatient != null) {
      invoices.filter { it.patientId == activePatient.id }.sortedByDescending { it.createdAt }
    } else {
      emptyList()
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(BgLightGray)
  ) {
    val isTablet = maxWidth >= 760.dp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      // 1. Header Strip
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderGrey)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE0F2FE)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.FolderShared,
                contentDescription = null,
                tint = SkyBlueDark,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "Medical Records (EMR Archive)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = DarkGrayText
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = SoftGreenContainer
                ) {
                  Text(
                    "${filteredPatients.size} Records",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MedicalGreenDark,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                "လူနာ ဆေးမှတ်တမ်းဟောင်းများ ရှာဖွေခြင်းနှင့် A4 Print ပြန်ထုတ်ခြင်း (Read-Only)",
                fontSize = 10.sp,
                color = SlateMuted
              )
            }
          }

          // Back to Queue Shortcut
          OutlinedButton(
            onClick = onNavigateToQueue,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp),
            border = BorderStroke(1.dp, BorderGrey)
          ) {
            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(13.dp), tint = SlateDark)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Patient Queue", fontSize = 10.5.sp, color = SlateDark, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Search & Filter Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by Name, HN (e.g. HN-2026-0081), Phone...", fontSize = 11.5.sp, color = SlateMuted) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SkyBlueDark, modifier = Modifier.size(16.dp)) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMuted, modifier = Modifier.size(14.dp))
              }
            }
          },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          colors = clinicSearchBarColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 3. Date & Status Filter Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Date filters
        listOf(
          "ALL" to "ရက်စွဲအားလုံး",
          "TODAY" to "ယနေ့",
          "YESTERDAY" to "မနေ့က",
          "PAST_7_DAYS" to "လွန်ခဲ့သော ၇ ရက်",
          "THIS_MONTH" to "ယခုလ"
        ).forEach { (key, label) ->
          val isSel = selectedDateFilter == key
          FilterChip(
            selected = isSel,
            onClick = { selectedDateFilter = key },
            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.height(28.dp),
            shape = RoundedCornerShape(4.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFE0F2FE),
              selectedLabelColor = SkyBlueDark
            )
          )
        }

        // Custom Date Filter Chip
        val isCustomSel = selectedDateFilter == "CUSTOM"
        FilterChip(
          selected = isCustomSel,
          onClick = {
            selectedDateFilter = "CUSTOM"
            showCustomDatePicker = true
          },
          leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp)) },
          label = { Text(if (isCustomSel && customDateFilter != null) customDateFilter!! else "ရက်စွဲရွေးရန်...", fontSize = 10.sp) },
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(4.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFE0F2FE),
            selectedLabelColor = SkyBlueDark
          )
        )

        Spacer(modifier = Modifier.width(4.dp))
        VerticalDivider(modifier = Modifier.height(18.dp), color = BorderGrey)
        Spacer(modifier = Modifier.width(4.dp))

        // Status filters
        listOf(
          "ALL" to "အားလုံး",
          "COMPLETED" to "ပြသပြီး (Completed)",
          "FOLLOW_UP_DUE" to "ရက်ချိန်းစောင့် (Follow-up Due)",
          "IN_QUEUE" to "Queue စာရင်းဝင်"
        ).forEach { (key, label) ->
          val isSel = selectedStatusFilter == key
          FilterChip(
            selected = isSel,
            onClick = { selectedStatusFilter = key },
            label = { Text(label, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.height(28.dp),
            shape = RoundedCornerShape(4.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SoftGreenContainer,
              selectedLabelColor = MedicalGreenDark
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 4. Main Body: Split Pane on Tablet or List / Detail on Mobile
      if (isTablet) {
        // TABLET 2-COLUMN LAYOUT
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Left Pane: Patient List (35% width)
          Surface(
            modifier = Modifier
              .weight(4f)
              .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderGrey)
          ) {
            PatientRecordsListPane(
              patients = filteredPatients,
              selectedPatientId = activePatient?.id,
              onSelectPatient = { selectedPatientId = it },
              consultations = consultations
            )
          }

          // Right Pane: Patient Complete EMR Chart & Visits Timeline (65% width)
          Surface(
            modifier = Modifier
              .weight(6f)
              .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderGrey)
          ) {
            if (activePatient != null) {
              PatientDetailedEmrPane(
                patient = activePatient,
                consultations = patientConsultations,
                invoices = patientInvoices,
                onPrintA4 = { consult -> viewModel.showA4DocPreview(consult) },
                onPrintThermal = { inv -> viewModel.showThermalPreview(inv) },
                onOpenTrends = { trendsPatient = activePatient },
                onOpenVaccines = { vaccinesPatient = activePatient },
                onOpenAttachments = { consult -> attachmentsConsultation = consult },
                onRequestFollowUpQueue = { showFollowUpConfirmDialog = activePatient },
                onEditRecord = { consult -> editingConsultation = consult },
                onEditPatient = { p -> editingPatient = p }
              )
            } else {
              EmptyRecordsSelection()
            }
          }
        }
      } else {
        // MOBILE SINGLE COLUMN LAYOUT
        if (mobileViewingDetail && activePatient != null) {
          // Mobile Detail View with Back Button
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderGrey)
          ) {
            Column(modifier = Modifier.fillMaxSize()) {
              // Top Back Bar
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(SoftIceBlue)
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { mobileViewingDetail = false },
                  modifier = Modifier.size(30.dp)
                ) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkGrayText, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "လူနာ ဆေးမှတ်တမ်း - ${activePatient.name}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = DarkGrayText
                )
              }

              PatientDetailedEmrPane(
                patient = activePatient,
                consultations = patientConsultations,
                invoices = patientInvoices,
                onPrintA4 = { consult -> viewModel.showA4DocPreview(consult) },
                onPrintThermal = { inv -> viewModel.showThermalPreview(inv) },
                onOpenTrends = { trendsPatient = activePatient },
                onOpenVaccines = { vaccinesPatient = activePatient },
                onOpenAttachments = { consult -> attachmentsConsultation = consult },
                onRequestFollowUpQueue = { showFollowUpConfirmDialog = activePatient },
                onEditRecord = { consult -> editingConsultation = consult },
                onEditPatient = { p -> editingPatient = p }
              )
            }
          }
        } else {
          // Mobile Patients List
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderGrey)
          ) {
            PatientRecordsListPane(
              patients = filteredPatients,
              selectedPatientId = activePatient?.id,
              onSelectPatient = {
                selectedPatientId = it
                mobileViewingDetail = true
              },
              consultations = consultations
            )
          }
        }
      }
    }
  }

  // --- Modals ---

  // Custom Date Picker Dialog
  if (showCustomDatePicker) {
    val availableDates = remember(patients, consultations) {
      val regDates = patients.map { it.registrationDate }
      val consultDates = consultations.map { it.timestamp.take(10) }
      (regDates + consultDates).filter { it.isNotBlank() }.distinct().sortedDescending()
    }

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
            Text("ဆေးမှတ်တမ်း ရက်စွဲရွေးချယ်ပါ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
            IconButton(onClick = { showCustomDatePicker = false }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text("စနစ်ထဲရှိ မှတ်တမ်းတင်ထားသော ရက်စွဲများ:", fontSize = 10.sp, color = SlateMuted)
          Spacer(modifier = Modifier.height(6.dp))
          LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(availableDates) { dt ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    customDateFilter = dt
                    showCustomDatePicker = false
                  },
                shape = RoundedCornerShape(6.dp),
                color = if (customDateFilter == dt) SoftIceBlue else BgLightGray,
                border = BorderStroke(0.6.dp, if (customDateFilter == dt) SkyBluePrimary else BorderGrey)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(dt, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
                  val burmeseDate = BurmeseDateUtils.formatToBurmeseDateAndDay(dt)
                  Text(burmeseDate, fontSize = 10.sp, color = SlateMedium)
                }
              }
            }
          }
        }
      }
    }
  }

  // Follow-Up Registration Confirmation Dialog
  // User must explicitly confirm before patient is entered into today's queue
  showFollowUpConfirmDialog?.let { p ->
    var followUpReasonInput by remember { mutableStateOf(if (p.followUpReason.isNotBlank()) p.followUpReason else "ရက်ချိန်း ပြန်လည်ပြသခြင်း") }

    AlertDialog(
      onDismissRequest = { showFollowUpConfirmDialog = null },
      icon = {
        Icon(Icons.Default.EventRepeat, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(28.dp))
      },
      title = {
        Text("ယနေ့အတွက် ရက်ချိန်း (Follow-up) သွင်းမည်လား?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "လူနာ: ${p.name} (${p.hn})\nအသက်: ${p.age} နှစ် / ${p.gender.label}\n\nအတည်ပြုပါက လူနာအား ယနေ့ Patient Queue (လူနာစာရင်း) သို့ Follow-up အဖြစ် သွင်းပေးမည်ဖြစ်ပြီး ဆရာဝန် consultation စတင်နိုင်မည်ဖြစ်သည်။",
            fontSize = 11.sp,
            color = SlateDark,
            lineHeight = 16.sp
          )
          OutlinedTextField(
            value = followUpReasonInput,
            onValueChange = { followUpReasonInput = it },
            label = { Text("ရက်ချိန်းပြသသည့် အကြောင်းရင်း (Follow-up Reason)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.registerFollowUpPatient(
              existingHn = p.hn,
              followUpReason = followUpReasonInput,
              bp = p.vitals.bloodPressure,
              pulse = p.vitals.pulseRate,
              temp = p.vitals.temperatureFahrenheit,
              spo2 = p.vitals.oxygenSaturation,
              weight = p.vitals.weightKg,
              rbs = p.vitals.rbs
            )
            showFollowUpConfirmDialog = null
            onNavigateToQueue()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Queue ထဲသို့ သွင်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showFollowUpConfirmDialog = null },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("မသွင်းသေးပါ", fontSize = 11.sp)
        }
      }
    )
  }

  // Attached Modals
  trendsPatient?.let { p ->
    val vitalHistory = viewModel.getPatientVitalsHistory(p.id)
    ChronicDiseaseTrendsDialog(
      patient = p,
      vitalHistory = vitalHistory,
      onDismiss = { trendsPatient = null },
      onAddVitalPoint = { pt -> viewModel.addVitalHistoryPoint(p.id, pt) }
    )
  }

  vaccinesPatient?.let { p ->
    val patientVaccines = remember(vaccineRecords, p.id) {
      vaccineRecords.filter { it.patientId == p.id }
    }
    VaccinationTrackerDialog(
      patient = p,
      vaccines = patientVaccines,
      onDismiss = { vaccinesPatient = null },
      onAddVaccine = { rec -> viewModel.addVaccineRecord(rec) },
      onUpdateVaccine = { rec -> viewModel.saveOrUpdateVaccineRecord(rec) },
      onDeleteVaccine = { id -> viewModel.deleteVaccineRecord(id) }
    )
  }

  attachmentsConsultation?.let { consult ->
    val liveConsult = consultations.find { it.id == consult.id } ?: consult
    val patient = patients.find { it.id == consult.patientId } ?: Patient(
      id = consult.patientId,
      hn = "HN-RECORD",
      name = consult.patientName,
      age = 30,
      gender = Gender.OTHER,
      phone = "",
      queueNumber = 0
    )
    MedicalAttachmentsDialog(
      patient = patient,
      attachments = liveConsult.attachments,
      onDismiss = { attachmentsConsultation = null },
      onAddAttachment = { att -> viewModel.addAttachmentToConsultation(consult.id, att) },
      onDeleteAttachment = { attId -> viewModel.deleteAttachmentFromConsultation(consult.id, attId) }
    )
  }

  editingConsultation?.let { consult: ConsultationRecord ->
    StructuredClinicalEditDialog(
      record = consult,
      onDismiss = { editingConsultation = null },
      onSave = { updated: ConsultationRecord ->
        viewModel.updateConsultation(updated)
        editingConsultation = null
        android.widget.Toast.makeText(context, "ဆေးမှတ်တမ်း အောင်မြင်စွာ ပြင်ဆင်သိမ်းဆည်းပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
      }
    )
  }

  editingPatient?.let { p ->
    EditPatientDialog(
      patient = p,
      onDismiss = { editingPatient = null },
      onSave = { updated ->
        viewModel.updatePatient(updated)
        editingPatient = null
        android.widget.Toast.makeText(context, "Patient information updated successfully", android.widget.Toast.LENGTH_SHORT).show()
      }
    )
  }
}

/**
 * Left Pane: Patient List Card View with Visits count
 */
@Composable
private fun PatientRecordsListPane(
  patients: List<Patient>,
  selectedPatientId: String?,
  onSelectPatient: (String) -> Unit,
  consultations: List<ConsultationRecord>
) {
  if (patients.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = SlateLight, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text("ရှာဖွေမှုနှင့် ကိုက်ညီသော လူနာမှတ်တမ်း မရှိပါ", fontSize = 11.5.sp, color = SlateMuted)
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(patients, key = { it.id }) { patient ->
        val isSelected = patient.id == selectedPatientId
        val visitCount = consultations.count { it.patientId == patient.id }

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectPatient(patient.id) },
          shape = RoundedCornerShape(6.dp),
          color = if (isSelected) Color(0xFFF0FDF4) else PureWhite,
          border = BorderStroke(
            if (isSelected) 1.5.dp else 0.8.dp,
            if (isSelected) MedicalGreen else BorderGrey
          )
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MedicalGreen else SoftGreenContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    patient.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isSelected) PureWhite else MedicalGreenDark
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
                    "${patient.hn} • ${patient.age}y / ${patient.gender.label.first()}",
                    fontSize = 10.sp,
                    color = SlateMuted
                  )
                }
              }

              // Visits Count Badge
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (visitCount > 0) Color(0xFFEDE7F6) else BgLightGray
              ) {
                Text(
                  "$visitCount Visits",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (visitCount > 0) Color(0xFF6B21A8) else SlateMedium,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Chief Complaint / Reason
            Text(
              "Complaint: ${patient.chiefComplaint}",
              fontSize = 10.sp,
              color = SlateDark,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            // Date of registration
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "Reg: ${patient.registrationDate}",
                fontSize = 9.sp,
                color = SlateMuted
              )
              if (!patient.followUpDueDate.isNullOrBlank()) {
                Text(
                  "🗓️ Next: ${patient.followUpDueDate}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFC026D3)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Right Pane: Comprehensive Patient EMR Chart & Chronological Timeline of Visits
 */
@Composable
private fun PatientDetailedEmrPane(
  patient: Patient,
  consultations: List<ConsultationRecord>,
  invoices: List<Invoice>,
  onPrintA4: (ConsultationRecord) -> Unit,
  onPrintThermal: (Invoice) -> Unit,
  onOpenTrends: () -> Unit,
  onOpenVaccines: () -> Unit,
  onOpenAttachments: (ConsultationRecord) -> Unit,
  onRequestFollowUpQueue: () -> Unit,
  onEditRecord: (ConsultationRecord) -> Unit,
  onEditPatient: (Patient) -> Unit = {}
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. Patient Profile Header Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = Color(0xFFF8FAFC),
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val isMobilePortrait = screenWidthDp < 600

        if (isMobilePortrait) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MedicalGreen),
              contentAlignment = Alignment.Center
            ) {
              Text(
                patient.name.take(1).uppercase(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  patient.name,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = DarkGrayText
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SoftGreenContainer
                ) {
                  Text(
                    patient.hn,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalGreenDark,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                "${patient.age} Yrs • ${patient.gender.label} • ${patient.bloodType} • 📞 ${patient.phone}",
                fontSize = 11.sp,
                color = SlateMedium
              )
              if (patient.address.isNotBlank()) {
                Text("🏠 ${patient.address}", fontSize = 10.sp, color = SlateMuted)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = { onEditPatient(patient) },
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp).weight(1f)
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = SlateDark)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit Info", fontSize = 11.sp, color = SlateDark, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = onRequestFollowUpQueue,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp).weight(1.3f)
            ) {
              Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Register Follow-Up", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(MedicalGreen),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  patient.name.take(1).uppercase(),
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = PureWhite
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    patient.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SoftGreenContainer
                  ) {
                    Text(
                      patient.hn,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = MedicalGreenDark,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  "${patient.age} Years • ${patient.gender.label} • Blood: ${patient.bloodType} • 📞 ${patient.phone}",
                  fontSize = 11.sp,
                  color = SlateMedium
                )
                if (patient.address.isNotBlank()) {
                  Text("🏠 ${patient.address}", fontSize = 10.sp, color = SlateMuted)
                }
              }
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { onEditPatient(patient) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = SlateDark)
                Spacer(modifier = Modifier.width(3.dp))
                Text("Edit Info", fontSize = 10.sp, color = SlateDark, fontWeight = FontWeight.Bold)
              }

              // Explicit Action: Register Follow-up
              Button(
                onClick = onRequestFollowUpQueue,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
              ) {
                Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Follow-Up", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Allergies Badge if any
        if (patient.allergies.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFEE2E2),
            border = BorderStroke(0.6.dp, Color(0xFFF87171))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Allergies (ဆေးမတည့်မှုများ): ${patient.allergies.joinToString(", ")}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB91C1C)
              )
            }
          }
        }

        // Quick Tools Row
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = onOpenTrends,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(12.dp), tint = OceanGreenPrimary)
            Spacer(modifier = Modifier.width(3.dp))
            Text("Vitals Trends", fontSize = 9.5.sp, color = DarkGrayText)
          }

          OutlinedButton(
            onClick = onOpenVaccines,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Icon(Icons.Default.Vaccines, contentDescription = null, modifier = Modifier.size(12.dp), tint = SkyBluePrimary)
            Spacer(modifier = Modifier.width(3.dp))
            Text("Vaccines Track", fontSize = 9.5.sp, color = DarkGrayText)
          }
        }
      }
    }

    // 2. Baseline Vitals
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        Text("Latest Recorded Vitals", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          VitalBox("BP", patient.vitals.bloodPressure)
          VitalBox("Pulse", "${patient.vitals.pulseRate} bpm")
          VitalBox("Temp", "${patient.vitals.formattedTemperatureF}")
          VitalBox("SpO2", "${patient.vitals.oxygenSaturation}%")
          VitalBox("Weight", "${patient.vitals.weightKg} kg")
          patient.vitals.rbs?.let {
            VitalBox("RBS", "${it.toInt()} mg/dL")
          }
        }
      }
    }

    // 3. Chronological Visit History (Timeline of Consultations)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "Consultation Visit History (${consultations.size} Visits)",
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        color = DarkGrayText
      )
    }

    if (consultations.isEmpty()) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(0.8.dp, BorderGrey)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.MedicalServices, contentDescription = null, tint = SlateLight, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text("ယခုလူနာအတွက် ဆရာဝန် ဆေးမှတ်တမ်း မရှိသေးပါ", fontSize = 11.5.sp, color = SlateMuted)
          Spacer(modifier = Modifier.height(2.dp))
          Text("လူနာအား Queue စာရင်းသွင်း၍ ဆရာဝန်ထံ ပြသနိုင်ပါသည်", fontSize = 10.sp, color = SlateLight)
        }
      }
    } else {
      consultations.forEachIndexed { idx, consult ->
        val visitNum = consultations.size - idx
        val correspondingInvoice = invoices.find { it.patientId == patient.id && it.createdAt.take(10) == consult.timestamp.take(10) }

        ConsultationVisitTimelineCard(
          visitNumber = visitNum,
          consultation = consult,
          invoice = correspondingInvoice,
          onPrintA4 = { onPrintA4(consult) },
          onPrintThermal = { inv -> onPrintThermal(inv) },
          onOpenAttachments = { onOpenAttachments(consult) },
          onEditRecord = { onEditRecord(consult) }
        )
      }
    }
  }
}

/**
 * Individual Consultation Card in Timeline View
 */
@Composable
private fun ConsultationVisitTimelineCard(
  visitNumber: Int,
  consultation: ConsultationRecord,
  invoice: Invoice?,
  onPrintA4: () -> Unit,
  onPrintThermal: (Invoice) -> Unit,
  onOpenAttachments: () -> Unit,
  onEditRecord: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(visitNumber == 1) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGrey)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      val visitScreenWidthDp = LocalConfiguration.current.screenWidthDp
      val isVisitMobile = visitScreenWidthDp < 600

      if (isVisitMobile) {
        // Mobile Layout: Info Row with Expand Toggle on right
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "V$visitNumber",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MedicalGreenDark
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "Visit Date: ${consultation.timestamp}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = DarkGrayText
              )
              Text(
                "Attending: ${consultation.doctorName} (SAMA: ${consultation.doctorSama})",
                fontSize = 9.5.sp,
                color = SlateMuted
              )
            }
          }

          IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Expand",
              tint = SlateMedium,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Action Buttons Row: A4 Print & Edit Record (clearly visible and easy to tap)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = onPrintA4,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp).weight(1f)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("A4 Print", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onEditRecord,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp).weight(1f)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = SkyBlueDark, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit Record", fontSize = 10.sp, color = SkyBlueDark, fontWeight = FontWeight.Bold)
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "V$visitNumber",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MedicalGreenDark
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "Visit Date: ${consultation.timestamp}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = DarkGrayText
              )
              Text(
                "Attending: ${consultation.doctorName} (SAMA: ${consultation.doctorSama})",
                fontSize = 9.5.sp,
                color = SlateMuted
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
              onClick = onPrintA4,
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("A4 Print", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = onEditRecord,
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, tint = SkyBlueDark, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Edit", fontSize = 9.5.sp, color = SkyBlueDark, fontWeight = FontWeight.Bold)
            }

            IconButton(
              onClick = { isExpanded = !isExpanded },
              modifier = Modifier.size(26.dp)
            ) {
              Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = SlateMedium,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Summary diagnoses preview if collapsed
      if (!isExpanded && consultation.diagnosisList.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          "Diagnoses: ${consultation.diagnosisList.joinToString(" • ")}",
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          color = MedicalGreenDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Expanded Content
      if (isExpanded) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = BorderGrey, thickness = 0.6.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // 0. Vital Signs Strip
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = BgLightGray,
          border = BorderStroke(0.6.dp, BorderGrey),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            VitalBox("BP", consultation.vitals.bloodPressure.ifBlank { "120/80" })
            VitalBox("Pulse", "${consultation.vitals.pulseRate} bpm")
            VitalBox("Temp", consultation.vitals.formattedTemperatureF)
            VitalBox("SpO2", "${consultation.vitals.oxygenSaturation}%")
            if (consultation.vitals.weightKg > 0) {
              VitalBox("Weight", "${consultation.vitals.weightKg} kg")
            }
            if ((consultation.vitals.rbs ?: 0.0) > 0) {
              VitalBox("RBS", "${consultation.vitals.rbs!!.toInt()} mg/dL")
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Chief Complaint & History
        if (consultation.subjective.isNotBlank()) {
          Text("Chief Complaint & Clinical History:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          FormattedClinicalSectionView(rawText = consultation.subjective)
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 2. Physical Examination Findings
        val findings = consultation.objective.ifBlank { consultation.canvasNotes }
        if (findings.isNotBlank()) {
          Text("Physical Examination & Findings:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          FormattedClinicalSectionView(rawText = findings)
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 3. Clinical Diagnoses
        if (consultation.diagnosisList.isNotEmpty() || consultation.assessment.isNotBlank()) {
          Text("Clinical Diagnosis & Assessment:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          val diagStr = if (consultation.diagnosisList.isNotEmpty()) {
            consultation.diagnosisList.joinToString(" • ")
          } else {
            consultation.assessment
          }
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = SoftGreenContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              diagStr,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = MedicalGreenDark,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 4. Prescriptions
        if (consultation.prescriptions.isNotEmpty()) {
          Text("Prescriptions (Rx - ${consultation.prescriptions.size} Items):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(2.dp))
          consultation.prescriptions.forEachIndexed { i, drug ->
            val medName = drug.effectiveName.ifBlank { drug.name.ifBlank { drug.medicineName } }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    "${i + 1}. $medName ${if (drug.dosage.isNotBlank()) "(${drug.dosage})" else ""}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText
                  )
                  if (drug.isExternal) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      shape = RoundedCornerShape(3.dp),
                      color = Color(0xFFFEF3C7)
                    ) {
                      Text(
                        "ပြင်ပဝယ်ယူရန် (External Pharmacy)",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
                Text("Qty: ${drug.quantity}", fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = SlateDark)
              }
              val details = buildString {
                if (drug.frequency.isNotBlank()) append(drug.frequency)
                if (drug.duration.isNotBlank()) append(" x ${drug.duration}")
                if (drug.instructions.isNotBlank()) append(" • [ညွှန်ကြားချက်/မှတ်ချက်: ${drug.instructions}]")
              }
              if (details.isNotBlank()) {
                Text(
                  details,
                  fontSize = 9.sp,
                  color = if (drug.isExternal) Color(0xFFB45309) else SlateMedium,
                  modifier = Modifier.padding(start = 12.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // Suggested Lab & Diagnostic Investigations
        if (consultation.suggestedInvestigations.isNotEmpty()) {
          Text("Suggested Lab & Diagnostic Investigations (ဓာတ်ခွဲ/စစ်ဆေးချက်များ):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            consultation.suggestedInvestigations.forEach { inv ->
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(0.6.dp, Color(0xFF93C5FD))
              ) {
                Text(
                  "🔬 $inv",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = SkyBlueDark,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // Day-Care Treatment & Injections
        val injectionDrugs = consultation.prescriptions.filter { drug ->
          drug.effectiveName.contains("Inj", ignoreCase = true) ||
          drug.dosage.contains("Inj", ignoreCase = true) ||
          drug.instructions.contains("IM", ignoreCase = true) ||
          drug.instructions.contains("IV", ignoreCase = true) ||
          drug.instructions.contains("Neb", ignoreCase = true) ||
          drug.instructions.contains("Drip", ignoreCase = true)
        }
        if (injectionDrugs.isNotEmpty()) {
          Text("Treatment & Injections (ဆေးထိုး / ဆေးသွင်းကုသမှု):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = SoftGreenContainer.copy(alpha = 0.4f),
            border = BorderStroke(0.6.dp, MedicalGreen.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              injectionDrugs.forEachIndexed { idx, drug ->
                val route = when {
                  drug.instructions.contains("IV", ignoreCase = true) -> "IV Route"
                  drug.instructions.contains("IM", ignoreCase = true) -> "IM Route"
                  drug.instructions.contains("Neb", ignoreCase = true) -> "Nebulization"
                  else -> "Injection"
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "💉 ${drug.effectiveName} [${drug.dosage}] (${route})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalGreenDark
                  )
                  Text("${drug.quantity} • ${drug.instructions}", fontSize = 9.5.sp, color = SlateMedium)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 5. Clinical Procedures Performed with Notes and Date/Time
        if (consultation.selectedProcedures.isNotEmpty()) {
          Text("Clinical Procedures Performed (ကုသမှုလုပ်ဆောင်ချက်များ):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(2.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            consultation.selectedProcedures.forEach { proc ->
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = BgLightGray,
                border = BorderStroke(0.6.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("• ${proc.name} [${proc.category}]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                    Text(
                      "${proc.price.toInt()} MMK",
                      fontSize = 9.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MedicalGreenDark
                    )
                  }
                  if (proc.dateTime.isNotBlank()) {
                    Text("Time: ${proc.dateTime}", fontSize = 8.5.sp, color = SlateMedium)
                  }
                  if (proc.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Operative Notes: ${proc.notes}", fontSize = 9.5.sp, color = SlateDark)
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // Doctor's Advice & Patient Instructions
        if (consultation.patientInstructions.isNotBlank()) {
          Text("Doctor's Advice & Patient Instructions (ဆရာဝန်၏ ညွှန်ကြားချက်များ):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(2.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
            border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              consultation.patientInstructions,
              fontSize = 10.sp,
              color = Color(0xFF92400E),
              modifier = Modifier.padding(6.dp)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
        }
        // 6. Follow-up
        if (consultation.followUpAppointment.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFDF4FF),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFC026D3), modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              val followUpFormatted = BurmeseDateUtils.formatFollowUpForPrint(consultation.followUpAppointment, consultation.followUpDueDate)
              Text("ရက်ချိန်း (Next Follow-Up): $followUpFormatted", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86198F))
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // 7. Attached X-Rays, Scans & Diagnostic Files
        if (consultation.attachments.isNotEmpty()) {
          Text("Attached Scans, X-Rays & Files (ဓါတ်မှန်နှင့် စစ်ဆေးချက်များ):", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            consultation.attachments.forEach { att ->
              val file = File(att.filePath)
              val isImg = att.fileType.equals("IMAGE", ignoreCase = true) || att.fileType.equals("PHOTO", ignoreCase = true) || att.fileType.equals("CAMERA", ignoreCase = true) || att.fileName.endsWith(".jpg", ignoreCase = true) || att.fileName.endsWith(".png", ignoreCase = true)
              Surface(
                modifier = Modifier
                  .width(110.dp)
                  .clickable { onOpenAttachments() },
                shape = RoundedCornerShape(6.dp),
                color = BgLightGray,
                border = BorderStroke(0.8.dp, BorderGrey)
              ) {
                Column(modifier = Modifier.padding(4.dp)) {
                  if (isImg && file.exists()) {
                    AsyncImage(
                      model = file,
                      contentDescription = att.title,
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .clip(RoundedCornerShape(4.dp)),
                      contentScale = ContentScale.Crop
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        if (att.fileType.equals("PDF", ignoreCase = true) || att.fileName.endsWith(".pdf", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (att.fileType.equals("PDF", ignoreCase = true)) StatusCritical else SkyBluePrimary,
                        modifier = Modifier.size(24.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    att.title.ifBlank { att.fileName },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DarkGrayText
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        // Action Buttons Row: Open Attachments & Thermal Invoice
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onOpenAttachments,
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.height(24.dp)
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(11.dp), tint = SkyBluePrimary)
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              if (consultation.attachments.isNotEmpty()) "${consultation.attachments.size} Files (ဓါတ်မှန်ကြည့်မည်/ထည့်မည်)" else "+ ဓါတ်မှန်/ဖိုင်ထည့်မည်",
              fontSize = 9.sp,
              color = SkyBluePrimary
            )
          }

          if (invoice != null) {
            OutlinedButton(
              onClick = { onPrintThermal(invoice) },
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(24.dp)
            ) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(11.dp), tint = OceanGreenPrimary)
              Spacer(modifier = Modifier.width(2.dp))
              Text("POS Invoice (${invoice.grandTotal.toInt()} MMK)", fontSize = 9.sp, color = OceanGreenPrimary)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VitalBox(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, fontSize = 9.sp, color = SlateMuted)
    Text(value, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
  }
}

@Composable
private fun EmptyRecordsSelection() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(30.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(Icons.Default.ContactPage, contentDescription = null, tint = SlateLight, modifier = Modifier.size(48.dp))
      Spacer(modifier = Modifier.height(8.dp))
      Text("လူနာတစ်ဦးအား ရွေးချယ်ပါ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateDark)
      Spacer(modifier = Modifier.height(2.dp))
      Text("လူနာ၏ ယခင်ဆေးမှတ်တမ်းများနှင့် timeline အား ဤနေရာတွင် ဖော်ပြပေးပါမည်", fontSize = 10.5.sp, color = SlateMuted)
    }
  }
}
