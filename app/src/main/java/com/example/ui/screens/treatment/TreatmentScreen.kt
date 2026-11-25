package com.example.ui.screens.treatment

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ClinicSettings
import com.example.model.Medicine
import com.example.model.Patient
import com.example.ui.theme.*
import com.example.ui.components.PosThermalTreatmentSlipDialog
import com.example.ui.components.VaccinationTrackerDialog
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentScreen(
  viewModel: ClinicViewModel,
  onNavigateToInvoice: (String) -> Unit = {},
  isReadOnly: Boolean = false
) {
  val context = LocalContext.current
  val treatmentCards by viewModel.treatmentCards.collectAsState()
  val patients by viewModel.patients.collectAsState()
  val medicines by viewModel.medicines.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val clinicSettings by viewModel.clinicSettings.collectAsState()

  var selectedTab by remember { mutableStateOf(0) } // 0: Today's Due, 1: Active Cards, 2: All / History
  var searchQuery by remember { mutableStateOf("") }
  var showCreateCardDialog by remember { mutableStateOf(false) }
  var cardToDiscontinue by remember { mutableStateOf<TreatmentCard?>(null) }
  var sessionToAdminister by remember { mutableStateOf<Pair<TreatmentCard, TreatmentSession>?>(null) }
  var cardToPrint by remember { mutableStateOf<TreatmentCard?>(null) }
  var cardToEdit by remember { mutableStateOf<TreatmentCard?>(null) }
  val vaccineRecords by viewModel.vaccineRecords.collectAsState()
  var showVaccinePatientPicker by remember { mutableStateOf(false) }
  var selectedVaccinePatient by remember { mutableStateOf<Patient?>(null) }

  val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  // Find all due sessions for today across active cards
  val todayDueSessions = remember(treatmentCards, todayIso, patients) {
    val patientIdSet = patients.map { it.id }.toSet()
    val list = mutableListOf<Pair<TreatmentCard, TreatmentSession>>()
    treatmentCards.filter { !it.isDiscontinued && !it.isFullyCompleted && patientIdSet.contains(it.patientId) }.forEach { card ->
      card.sessions.filter { s ->
        s.status == TreatmentStatus.PENDING && (s.scheduledDate <= todayIso || s.scheduledTime.contains("Immediate", ignoreCase = true))
      }.forEach { session ->
        list.add(Pair(card, session))
      }
    }
    list.sortedBy { it.second.scheduledDate + it.second.scheduledTime }
  }

  val filteredTodayDueSessions = remember(todayDueSessions, searchQuery) {
    if (searchQuery.isBlank()) todayDueSessions
    else todayDueSessions.filter { (card, _) ->
      card.patientName.contains(searchQuery, ignoreCase = true) ||
      card.patientHn.contains(searchQuery, ignoreCase = true) ||
      card.medicationName.contains(searchQuery, ignoreCase = true)
    }
  }

  val patientIdSet = remember(patients) { patients.map { it.id }.toSet() }
  val validTreatmentCards = remember(treatmentCards, patientIdSet) {
    treatmentCards.filter { patientIdSet.contains(it.patientId) }
  }

  val activeCardsCount = validTreatmentCards.count { !it.isDiscontinued && !it.isFullyCompleted }
  val completedCardsCount = validTreatmentCards.count { it.isFullyCompleted }

  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val isMobilePortrait = screenWidthDp < 600

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgLightGray)
      .padding(8.dp)
  ) {
    if (isReadOnly) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFEF3C7),
        border = BorderStroke(1.dp, Color(0xFFD97706)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "VIEW ONLY (ကြည့်ရှုခွင့်သာ) - ဆေးထိုး/ကုသမှုစာရင်း ကြည့်ရှုခွင့်သာရှိပြီး ဆေးထိုးခြင်းနှင့် ကတ်အသစ်ဖွင့်ခွင့် မရှိပါ",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF92400E)
          )
        }
      }
    }
    // 1. Top Header & Metrics Bar
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
      modifier = Modifier.fillMaxWidth()
    ) {
      if (isMobilePortrait) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Icon(Icons.Default.Healing, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "Day-Care Treatment Cards",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateDark
                )
                Text(
                  "ဆေးထိုး/အကြောပုလင်း ကုသမှုကတ်များ",
                  fontSize = 10.sp,
                  color = SlateMuted
                )
              }
            }
          }
          if (!isReadOnly) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Button(
                onClick = { showCreateCardDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(34.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ကုသမှုကတ် (New)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
              OutlinedButton(
                onClick = { showVaccinePatientPicker = true },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(34.dp)
              ) {
                Icon(Icons.Default.Vaccines, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ကာကွယ်ဆေး (Vaccines)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreen)
              }
            }
          }
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Healing, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "Day-Care Treatment Cards (ဆေးထိုး/အကြောပုလင်း ကုသမှုကတ်များ)",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
              )
              Text(
                "IM / IV Push / IV Infusion Drip / Nebulizer • Routine Nursing & Day-Care",
                fontSize = 10.5.sp,
                color = SlateMuted
              )
            }
          }

          if (!isReadOnly) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { showVaccinePatientPicker = true },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Icon(Icons.Default.Vaccines, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ကာကွယ်ဆေးထိုး မှတ်တမ်း (Vaccines)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreen)
              }
              Button(
                onClick = { showCreateCardDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ကုသမှုကတ်အသစ် (New Card)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 2. Metrics & Status Row (Scrollable on small screens)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .then(if (isMobilePortrait) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Metric 1: Today Due
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (todayDueSessions.isNotEmpty()) StatusWarningContainer else PureWhite,
        border = BorderStroke(1.dp, if (todayDueSessions.isNotEmpty()) StatusWarning.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        modifier = (if (isMobilePortrait) Modifier.width(150.dp) else Modifier.weight(1f))
          .height(52.dp)
          .clickable { selectedTab = 0 }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = if (todayDueSessions.isNotEmpty()) StatusWarning else SlateMuted, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("ယနေ့ထိုးရန် (Due)", fontSize = 9.5.sp, color = SlateMedium)
            Text("${todayDueSessions.size} Sessions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (todayDueSessions.isNotEmpty()) StatusWarning else SlateDark)
          }
        }
      }

      // Metric 2: Active Courses
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = (if (isMobilePortrait) Modifier.width(150.dp) else Modifier.weight(1f))
          .height(52.dp)
          .clickable { selectedTab = 1 }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Sync, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("ကုသဆဲ (Active)", fontSize = 9.5.sp, color = SlateMedium)
            Text("$activeCardsCount Patients", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          }
        }
      }

      // Metric 3: Completed Courses
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = (if (isMobilePortrait) Modifier.width(150.dp) else Modifier.weight(1f))
          .height(52.dp)
          .clickable { selectedTab = 2 }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("ပြီးစီးမှု (Completed)", fontSize = 9.5.sp, color = SlateMedium)
            Text("$completedCardsCount Cards", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 3. Tab Bar (Always horizontally scrollable to prevent tab squeeze)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilterChip(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        label = { Text("ယနေ့ထိုးရန် စာရင်း (${todayDueSessions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MedicalGreen,
          selectedLabelColor = PureWhite,
          selectedLeadingIconColor = PureWhite
        )
      )
      FilterChip(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        label = { Text("ကုသဆဲ ကတ်များ ($activeCardsCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MedicalGreen,
          selectedLabelColor = PureWhite,
          selectedLeadingIconColor = PureWhite
        )
      )
      FilterChip(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        label = { Text("ကတ်မှတ်တမ်း အားလုံး (${validTreatmentCards.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MedicalGreen,
          selectedLabelColor = PureWhite,
          selectedLeadingIconColor = PureWhite
        )
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Search Bar for Treatment Cards & Patients
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("လူနာအမည်၊ HN သို့မဟုတ် ဆေးအမည်ဖြင့် ရှာဖွေပါ...", fontSize = 11.sp, color = SlateMuted) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(17.dp)) },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMuted, modifier = Modifier.size(14.dp))
          }
        }
      },
      shape = RoundedCornerShape(8.dp),
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = PureWhite,
        unfocusedContainerColor = PureWhite,
        focusedBorderColor = MedicalGreen,
        unfocusedBorderColor = Color(0xFFE2E8F0)
      ),
      textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = SlateDark)
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 4. Tab Contents
    when (selectedTab) {
      0 -> {
        // TAB 0: TODAY DUE INJECTIONS
        if (filteredTodayDueSessions.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(42.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                if (searchQuery.isNotBlank()) "ရှာဖွေမှုနှင့် ကိုက်ညီသော ယနေ့ထိုးရန် ဆေးမရှိပါ" else "ယနေ့အတွက် ဆေးထိုးရန် ကျန်ရှိသူ မရှိပါ",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
              )
              Text("All scheduled injection sessions for today are completed.", fontSize = 11.sp, color = SlateMuted)
            }
          }
        } else {
          val groupedDueSessions = remember(filteredTodayDueSessions) {
            filteredTodayDueSessions.groupBy { it.first.patientId }
          }
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(groupedDueSessions.keys.toList(), key = { it }) { patientId ->
              val sessionPairs = groupedDueSessions[patientId] ?: emptyList()
              val firstCard = sessionPairs.firstOrNull()?.first
              val patient = patients.find { it.id == patientId }
              val patientName = firstCard?.patientName ?: patient?.name ?: "Patient"
              val patientHn = firstCard?.patientHn ?: patient?.hn ?: ""

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(patientName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                        if (patient != null && patient.phone.isNotBlank()) {
                          Spacer(modifier = Modifier.width(6.dp))
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                              .clip(RoundedCornerShape(4.dp))
                              .clickable {
                                try {
                                  val clean = patient.phone.replace(Regex("[^0-9+]"), "")
                                  context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
                                } catch (e: Exception) {
                                  Toast.makeText(context, "Cannot dial: " + e.message, Toast.LENGTH_SHORT).show()
                                }
                              }
                          ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(patient.phone, fontSize = 10.5.sp, color = MedicalGreenDark, textDecoration = TextDecoration.Underline)
                          }
                        }
                      }
                      Text(
                        patientHn + " • ယနေ့ထိုးရန် (" + sessionPairs.size + ") မျိုး",
                        fontSize = 10.5.sp,
                        color = SlateMuted
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0xFFEDE7F6)
                    ) {
                      Text(
                        sessionPairs.size.toString() + " Injections Due",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5E35B1),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  sessionPairs.forEachIndexed { sIdx, (card, session) ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF1F5F9))
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
                            .background(Color(0xFFEDE7F6)),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(
                            "#" + session.sessionNumber,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5E35B1),
                            fontSize = 11.sp
                          )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(
                            card.medicationName + " (" + card.dosage + ") • " + card.routeDisplay,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                          )
                          Text(
                            "Dose " + session.sessionNumber + "/" + card.totalSessions + " • Time: " + session.scheduledTime + if (card.instructions.isNotBlank()) " • " + card.instructions else "",
                            fontSize = 9.5.sp,
                            color = SlateMedium
                          )
                        }
                      }
                      if (!isReadOnly) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                          onClick = { sessionToAdminister = Pair(card, session) },
                          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                          shape = RoundedCornerShape(4.dp),
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                          modifier = Modifier.height(28.dp)
                        ) {
                          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text("ဆေးထိုးပြီး", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

      1, 2 -> {
        // TAB 1 & 2: TREATMENT CARDS LIST
        val displayCards = remember(validTreatmentCards, selectedTab, searchQuery, patients) {
          val patientIdSet = patients.map { it.id }.toSet()
          validTreatmentCards.filter { card ->
            if (!patientIdSet.contains(card.patientId)) return@filter false
            val matchesTab = when (selectedTab) {
              1 -> !card.isDiscontinued && !card.isFullyCompleted
              else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                card.patientName.contains(searchQuery, ignoreCase = true) ||
                card.patientHn.contains(searchQuery, ignoreCase = true) ||
                card.medicationName.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
          }
        }

        if (displayCards.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Text("ကုသမှုကတ်မှတ်တမ်း မရှိပါ", fontSize = 12.sp, color = SlateLight)
          }
        } else {
          val groupedDisplayCards = remember(displayCards) {
            displayCards.groupBy { it.patientId }
          }
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(groupedDisplayCards.keys.toList(), key = { it }) { patientId ->
              val patientCards = groupedDisplayCards[patientId] ?: emptyList()
              val firstCard = patientCards.firstOrNull()
              val patient = patients.find { it.id == patientId }
              val patientName = firstCard?.patientName ?: patient?.name ?: "Patient"
              val patientHn = firstCard?.patientHn ?: patient?.hn ?: ""

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(patientName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                      Text(
                        patientHn + " • ဆေးကတ် (" + patientCards.size + ") ကတ် • ဒေါက်တာ: " + (firstCard?.doctorName ?: ""),
                        fontSize = 10.5.sp,
                        color = SlateMuted
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = SoftGreenContainer
                    ) {
                      Text(
                        patientCards.size.toString() + " Cards",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalGreenDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  patientCards.forEachIndexed { cIdx, card ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF1F5F9))
                    Column {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (card.cardStatus) {
                              TreatmentStatus.COMPLETED -> Color(0xFFE8F5E9)
                              TreatmentStatus.DISCONTINUED -> Color(0xFFFFEBEE)
                              else -> Color(0xFFEDE7F6)
                            }
                          ) {
                            Text(
                              when (card.cardStatus) {
                                TreatmentStatus.COMPLETED -> "ပြီးစီး"
                                TreatmentStatus.DISCONTINUED -> "ရပ်ဆိုင်း"
                                else -> "ကုသဆဲ (" + card.completedSessionsCount + "/" + card.totalSessions + ")"
                              },
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = when (card.cardStatus) {
                                TreatmentStatus.COMPLETED -> StatusSuccess
                                TreatmentStatus.DISCONTINUED -> StatusCritical
                                else -> Color(0xFF5E35B1)
                              },
                              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                          }
                          Spacer(modifier = Modifier.width(6.dp))
                          Column {
                            Text(
                              "Rx: " + card.medicationName + " (" + card.dosage + ")",
                              fontSize = 11.5.sp,
                              fontWeight = FontWeight.Bold,
                              color = MedicalGreenDark
                            )
                            Text(
                              "Route: " + card.routeDisplay + " • " + card.frequency + " (" + card.totalDays + " ရက်)",
                              fontSize = 9.5.sp,
                              color = SlateMedium
                            )
                            if (card.instructions.isNotBlank()) {
                              Text(
                                "အသုံး: " + card.instructions,
                                fontSize = 9.5.sp,
                                color = Color(0xFF0D9488),
                                fontWeight = FontWeight.SemiBold
                              )
                            }
                          }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                          IconButton(onClick = { cardToPrint = card }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Print, contentDescription = "Print Slip", tint = SlateMedium, modifier = Modifier.size(15.dp))
                          }
                          if (!isReadOnly && !card.isDiscontinued) {
                            IconButton(onClick = { cardToEdit = card }, modifier = Modifier.size(26.dp)) {
                              Icon(Icons.Default.Edit, contentDescription = "Edit Card", tint = MedicalGreen, modifier = Modifier.size(15.dp))
                            }
                          }
                          if (!isReadOnly) {
                            IconButton(onClick = { cardToDiscontinue = card }, modifier = Modifier.size(26.dp)) {
                              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(15.dp))
                            }
                          }
                        }
                      }

                      Spacer(modifier = Modifier.height(4.dp))
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        card.sessions.forEach { s ->
                          val isDone = s.status == TreatmentStatus.COMPLETED
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDone) MedicalGreen else if (card.isDiscontinued) Color(0xFFE2E8F0) else Color(0xFFF3E5F5),
                            modifier = Modifier
                              .widthIn(min = 32.dp)
                              .height(24.dp),
                            border = BorderStroke(1.dp, if (isDone) MedicalGreenDark else SlateLight)
                          ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                              Text(
                                "D" + s.sessionNumber,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone) PureWhite else if (card.isDiscontinued) SlateMedium else Color(0xFF5E35B1)
                              )
                            }
                          }
                        }
                      }

                      if (card.isDiscontinued && card.discontinueReason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          "ရပ်ဆိုင်းရသည့် အကြောင်းရင်း: " + card.discontinueReason + " (" + card.discontinuedBy + ")",
                          fontSize = 9.5.sp,
                          color = StatusCritical
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
  }

  // --- MODALS ---

  // 1. Administer Session Dialog (Dose confirm with Consumables Picker)
  sessionToAdminister?.let { (card, session) ->
    AdministerSessionModal(
      card = card,
      session = session,
      medicines = medicines,
      defaultNurseName = currentUser?.fullName ?: "Nurse Station",
      onDismiss = { sessionToAdminister = null },
      onConfirm = { isClinicMed, medPrice, sessionFee, consumables, isConsumablesFoc, notes ->
        val generatedInv = viewModel.administerTreatmentSession(
          cardId = card.id,
          sessionId = session.id,
          nurseName = currentUser?.fullName ?: "Nurse Staff",
          isClinicMed = isClinicMed,
          medPrice = medPrice,
          sessionFee = sessionFee,
          consumables = consumables,
          isConsumablesFoc = isConsumablesFoc,
          notes = notes
        )
        if (isConsumablesFoc && consumables.isNotEmpty()) {
          Toast.makeText(context, "Dose ${session.sessionNumber} ဆေးထိုးပြီးစီးပါပြီ (အပိုပစ္စည်းများကို ဆေးခန်းအသုံး/FOC အဖြစ် Stock နှင့် အသုံးစရိတ်တွင် စာရင်းသွင်းပြီး)", Toast.LENGTH_LONG).show()
        } else if (generatedInv == null) {
          Toast.makeText(context, "Dose ${session.sessionNumber} ဆေးထိုးပြီးစီးပါပြီ (Consultation ဘောင်ချာတွင် ပေါင်းထည့်ပြီးဖြစ်၍ ဘောင်ချာအသစ် မထုတ်တော့ပါ)", Toast.LENGTH_LONG).show()
        } else if (generatedInv.id.startsWith("INV-TC-SUPP-")) {
          Toast.makeText(context, "Dose ${session.sessionNumber} ဆေးထိုးပြီးစီးပါပြီ (အပိုသုံးပစ္စည်း ထပ်ဆောင်းဘောင်ချာ #${generatedInv.invoiceNumber} ထုတ်ပေးပြီး)", Toast.LENGTH_LONG).show()
        } else {
          Toast.makeText(context, "Dose ${session.sessionNumber} ဆေးထိုးပြီးစီးပါပြီ (ဘောင်ချာ #${generatedInv.invoiceNumber} ထုတ်ပေးပြီး)", Toast.LENGTH_SHORT).show()
        }
        sessionToAdminister = null
      }
    )
  }

  // 2. Discontinue / Void Card Dialog
  cardToDiscontinue?.let { card ->
    DiscontinueCardModal(
      card = card,
      onDismiss = { cardToDiscontinue = null },
      onConfirm = { reason, returnStock ->
        if (card.completedSessionsCount > 0) {
          viewModel.discontinueTreatmentCard(
            cardId = card.id,
            reason = reason,
            staffName = currentUser?.fullName ?: "Staff"
          )
          Toast.makeText(context, "ကုသမှု ရပ်ဆိုင်းပြီး မှတ်တမ်းတွင် ထိန်းသိမ်းထားပါသည်", Toast.LENGTH_LONG).show()
        } else {
          val (success, restoredList) = viewModel.voidTreatmentCardAndReturnStock(
            cardId = card.id,
            returnStock = returnStock,
            reason = reason
          )
          if (success) {
            val msg = if (restoredList.isNotEmpty()) {
              "ကုသမှု ပယ်ဖျက်ပြီး စတော့ခ်ထဲ ပြန်သွင်းပြီးပါပြီ: ${restoredList.joinToString(", ")}"
            } else {
              "ကုသမှုကတ်အား အောင်မြင်စွာ ပယ်ဖျက်ပြီးပါပြီ"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
          }
        }
        cardToDiscontinue = null
      }
    )
  }

  // 3. Create Card Dialog
  if (showCreateCardDialog) {
    CreateTreatmentCardModal(
      patients = patients,
      medicines = medicines,
      clinicSettings = clinicSettings,
      defaultDoctor = currentUser?.fullName ?: "Dr. Nyan Kaung Sett",
      onDismiss = { showCreateCardDialog = false },
      onCreate = { newCard ->
        viewModel.addTreatmentCard(newCard)
        Toast.makeText(context, "ကုသမှုကတ် အသစ်ဖွင့်လှစ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        showCreateCardDialog = false
      }
    )
  }

  // 4. Print Treatment Card Slip Modal (Dual Bluetooth POS & Native System Print)
  cardToPrint?.let { card ->
    PosThermalTreatmentSlipDialog(
      card = card,
      clinicSettings = clinicSettings,
      onDismiss = { cardToPrint = null }
    )
  }

  // 4.1 Edit Treatment Card Modal
  cardToEdit?.let { card ->
    EditTreatmentCardModal(
      card = card,
      clinicSettings = clinicSettings,
      onDismiss = { cardToEdit = null },
      onSave = { updatedCard ->
        viewModel.updateTreatmentCard(updatedCard)
        Toast.makeText(context, "ကုသမှုကတ်နှင့် EMR မှတ်တမ်းအား အောင်မြင်စွာ ပြင်ဆင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        cardToEdit = null
      }
    )
  }

  // 5. Vaccine Patient Picker & Direct Nurse Administration Dialog
  if (showVaccinePatientPicker) {
    var vaccineSearchQuery by remember { mutableStateOf("") }
    val filteredVaccinePatients = remember(patients, vaccineSearchQuery) {
      if (vaccineSearchQuery.isBlank()) patients.take(30)
      else patients.filter {
        it.name.contains(vaccineSearchQuery, ignoreCase = true) ||
        it.phone.contains(vaccineSearchQuery, ignoreCase = true) ||
        it.hn.contains(vaccineSearchQuery, ignoreCase = true)
      }
    }

    AlertDialog(
      onDismissRequest = { showVaccinePatientPicker = false },
      icon = { Icon(Icons.Default.Vaccines, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(28.dp)) },
      title = { Text("ကာကွယ်ဆေးထိုးမည့် လူနာ ရွေးချယ်ပါ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkGrayText) },
      text = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("ရက်ချိန်းအတိုင်း ကာကွယ်ဆေးထိုးရန် လာရောက်သော လူနာများအား ဆရာဝန်စောင့်စရာမလိုဘဲ တိုက်ရိုက်ထိုးနှံပြီး EMR တွင် မှတ်တမ်းတင်နိုင်ပါသည်:", fontSize = 11.sp, color = SlateDark)
          OutlinedTextField(
            value = vaccineSearchQuery,
            onValueChange = { vaccineSearchQuery = it },
            placeholder = { Text("လူနာအမည်၊ ဖုန်း သို့မဟုတ် HN ဖြင့်ရှာပါ...", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(filteredVaccinePatients) { p ->
              val pVaccines = vaccineRecords.filter { it.patientId == p.id }
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = BackgroundSurface,
                border = BorderStroke(0.6.dp, BorderGray),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedVaccinePatient = p
                    showVaccinePatientPicker = false
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
                    Text("${p.hn} • ${p.gender.name.take(1)}/${p.age}y • ${p.phone.ifBlank { "ဖုန်းမရှိ" }}", fontSize = 9.5.sp, color = SlateMedium)
                  }
                  if (pVaccines.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDBEAFE)) {
                      Text("${pVaccines.size} doses", fontSize = 9.sp, color = Color(0xFF1D4ED8), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showVaccinePatientPicker = false }) {
          Text("Cancel", fontSize = 11.sp)
        }
      }
    )
  }

  selectedVaccinePatient?.let { p ->
    val patientVaccines = remember(vaccineRecords, p.id) {
      vaccineRecords.filter { it.patientId == p.id }
    }
    VaccinationTrackerDialog(
      patient = p,
      vaccines = patientVaccines,
      onDismiss = { selectedVaccinePatient = null },
      onAddVaccine = { rec ->
        viewModel.addVaccineRecord(rec)
        Toast.makeText(context, "${p.name} ၏ ${rec.vaccineName} (Dose ${rec.doseNumber}) EMR ထဲသို့ မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      },
      onUpdateVaccine = { rec ->
        viewModel.saveOrUpdateVaccineRecord(rec)
        Toast.makeText(context, "${p.name} ၏ ကာကွယ်ဆေးမှတ်တမ်း ပြင်ဆင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      },
      onDeleteVaccine = { id -> viewModel.deleteVaccineRecord(id) }
    )
  }
}

/**
 * Modal to administer a dose:
 * Allows selecting Clinic Med vs Patient's Own Med,
 * Adding Extra Consumables (Cannula, Scalp vein, Syringes) from Pharmacy inventory,
 * and creating the invoice itemized per visit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministerSessionModal(
  card: TreatmentCard,
  session: TreatmentSession,
  medicines: List<Medicine>,
  defaultNurseName: String,
  onDismiss: () -> Unit,
  onConfirm: (isClinicMed: Boolean, medPrice: Double, sessionFee: Double, consumables: List<TreatmentConsumable>, isConsumablesFoc: Boolean, notes: String) -> Unit
) {
  val isPrepaid = session.isPrepaid || card.isPrepaidFullCourse
  var isClinicMed by remember { mutableStateOf(!isPrepaid && session.isClinicMed && session.medicationFee > 0) }
  var medPriceText by remember { mutableStateOf((if (isPrepaid) 0.0 else (if (session.medicationFee > 0) session.medicationFee else card.medicationFee)).toInt().toString()) }
  var sessionFeeText by remember { mutableStateOf((if (isPrepaid) 0.0 else card.sessionFee).toInt().toString()) }
  var notes by remember { mutableStateOf(session.notes) }
  val initialConsumables = remember(session, card) {
    if (session.consumables.isNotEmpty()) session.consumables
    else card.consumables
  }
  val selectedConsumables = remember(initialConsumables) {
    mutableStateListOf<TreatmentConsumable>().apply { addAll(initialConsumables) }
  }
  var isConsumablesFoc by remember { mutableStateOf(false) }
  var storeSearchQuery by remember { mutableStateOf("") }

  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)
  val context = LocalContext.current

  // Preset consumable items mapped to store inventory
  val presetQueries = listOf(
    Pair("Cannula", "IV Cannula"),
    Pair("Scalp Vein", "Scalp Vein Needle"),
    Pair("Syringe", "Syringe & WFI"),
    Pair("Infusion", "IV Infusion Set")
  )

  val filteredStoreItems = remember(medicines, storeSearchQuery) {
    if (storeSearchQuery.isBlank()) emptyList()
    else medicines.filter {
      it.name.contains(storeSearchQuery, ignoreCase = true) ||
      it.genericName.contains(storeSearchQuery, ignoreCase = true) ||
      it.category.contains(storeSearchQuery, ignoreCase = true)
    }.take(6)
  }

  val medPrice = if (isClinicMed) (medPriceText.toDoubleOrNull() ?: 0.0) else 0.0
  val sessionFee = sessionFeeText.toDoubleOrNull() ?: 3000.0
  val consumablesTotal = if (isConsumablesFoc) 0.0 else selectedConsumables.sumOf { it.totalPrice }
  val visitTotal = medPrice + sessionFee + consumablesTotal

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.88f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "ဆေးထိုးပြီးစီးကြောင်း အတည်ပြုခြင်း (Dose ${session.sessionNumber}/${card.totalSessions})",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "လူနာ: ${card.patientName} (${card.patientHn}) • Rx: ${card.medicationName}",
              fontSize = 11.sp,
              color = MedicalGreenDark
            )
            if (card.instructions.isNotBlank()) {
              Text(
                "အသုံး/ထိုးနည်း: ${card.instructions}",
                fontSize = 10.sp,
                color = Color(0xFF0D9488),
                fontWeight = FontWeight.SemiBold
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
        ) {
          if (isPrepaid) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFEDE9FE),
              border = BorderStroke(1.dp, Color(0xFF7C3AED)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("✨ အကြေရှင်းပြီးသား ဆေးထိုးကတ် (Prepaid Full Course)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B21B6))
                  Text("ဆေးဖိုးနှင့် ဝန်ဆောင်ခကို ရက်စွဲအစတွင် အကြေရှင်းပြီးဖြစ်သဖြင့် ယခုကြိမ်အတွက် ထပ်မံကောက်ခံရန် မလိုပါ (0 Ks)", fontSize = 9.5.sp, color = Color(0xFF6D28D9))
                }
              }
            }
          }
          // 1. Medicine Source Toggle
          Text("၁။ ဆေးအရင်းအမြစ် (Medication Source):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = isClinicMed,
              onClick = { isClinicMed = true },
              label = { Text("ဆေးခန်းဆေး (Clinic Stock)", fontSize = 10.5.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
            )
            FilterChip(
              selected = !isClinicMed,
              onClick = {
                isClinicMed = false
                medPriceText = "0"
              },
              label = { Text("လူနာပါဆေး / ကြိုရှင်းပြီး (0 Ks)", fontSize = 10.5.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF5E35B1), selectedLabelColor = PureWhite)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 2. Fee breakdown (Medication fee + Service fee)
          Text("၂။ တစ်ကြိမ်စာ ကျသင့်ငွေ (Visit Charges):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = if (isClinicMed) medPriceText else "0 (လူနာပါဆေး)",
              onValueChange = { if (isClinicMed) medPriceText = it },
              label = { Text("ဆေးဖိုး (Med Price Ks)", fontSize = 10.sp) },
              enabled = isClinicMed,
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sessionFeeText,
              onValueChange = { sessionFeeText = it },
              label = { Text("ဆေးထိုးဝန်ဆောင်ခ (Service Ks)", fontSize = 10.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 3. Extra Consumables (Cannula, Scalp Vein, Syringe, etc.)
          Text("၃။ အပိုသုံးပစ္စည်းများ (Extra Consumables from Pharmacy Store):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Text("Store/Pharmacy စာရင်းမှ အသုံးပစ္စည်းများ ရှာဖွေထည့်သွင်းနိုင်ပါသည်", fontSize = 9.5.sp, color = SlateMuted)

          Spacer(modifier = Modifier.height(6.dp))

          // Option C (Bill to patient) vs Option B (FOC / Clinic Expense)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !isConsumablesFoc,
              onClick = { isConsumablesFoc = false },
              label = { Text("လူနာထံမှ ကောက်မည် (Bill to Patient)", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
            )
            FilterChip(
              selected = isConsumablesFoc,
              onClick = { isConsumablesFoc = true },
              label = { Text("ဆေးခန်းအသုံး / FOC (0 Ks)", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC2410C), selectedLabelColor = PureWhite)
            )
          }

          if (isConsumablesFoc && selectedConsumables.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFFFEDD5),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                "✨ ဤအသုံးပစ္စည်းများကို လူနာထံမှ မကောက်ခံဘဲ (0 Ks)၊ ဆေးခန်း Stock မှ နှုတ်ယူကာ ကုန်ကျစရိတ် (Clinic Expense) အဖြစ် ဝယ်ရင်းဈေးဖြင့် စာရင်းသွင်းပါမည်",
                fontSize = 9.5.sp,
                color = Color(0xFF9A3412),
                modifier = Modifier.padding(6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Preset Chips strictly matched with Store Inventory
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            presetQueries.forEach { (keyword, defaultLabel) ->
              val storeMatch = medicines.find {
                it.name.contains(keyword, ignoreCase = true) ||
                it.genericName.contains(keyword, ignoreCase = true)
              }
              ElevatedAssistChip(
                onClick = {
                  if (storeMatch != null) {
                    val existingIdx = selectedConsumables.indexOfFirst { it.medicineId == storeMatch.id }
                    if (existingIdx >= 0) {
                      val curr = selectedConsumables[existingIdx]
                      selectedConsumables[existingIdx] = curr.copy(quantity = curr.quantity + 1)
                    } else {
                      selectedConsumables.add(
                        TreatmentConsumable(
                          medicineId = storeMatch.id,
                          name = storeMatch.name,
                          quantity = 1,
                          unitPrice = storeMatch.sellingPrice
                        )
                      )
                    }
                  } else {
                    Toast.makeText(
                      context,
                      "$defaultLabel အား Pharmacy Store ထဲတွင် ဦးစွာ စာရင်းသွင်းပေးပါ",
                      Toast.LENGTH_SHORT
                    ).show()
                  }
                },
                label = {
                  if (storeMatch != null) {
                    Text("${storeMatch.name} (${storeMatch.totalStock})", fontSize = 9.5.sp, color = MedicalGreenDark)
                  } else {
                    Text("$defaultLabel (Store တွင်မရှိ)", fontSize = 9.sp, color = SlateMuted)
                  }
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Store Inventory Search Box
          OutlinedTextField(
            value = storeSearchQuery,
            onValueChange = { storeSearchQuery = it },
            placeholder = { Text("Store မှ အသုံးပစ္စည်း/ဆေး ရှာဖွေထည့်ရန် (Search Store Items)...", fontSize = 10.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
              if (storeSearchQuery.isNotBlank()) {
                IconButton(onClick = { storeSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                  Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Store Search Results Dropdown/List
          if (filteredStoreItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, BorderGrey),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(4.dp)) {
                filteredStoreItems.forEach { med ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        val existingIdx = selectedConsumables.indexOfFirst { it.medicineId == med.id }
                        if (existingIdx >= 0) {
                          val curr = selectedConsumables[existingIdx]
                          selectedConsumables[existingIdx] = curr.copy(quantity = curr.quantity + 1)
                        } else {
                          selectedConsumables.add(
                            TreatmentConsumable(
                              medicineId = med.id,
                              name = med.name,
                              quantity = 1,
                              unitPrice = med.sellingPrice
                            )
                          )
                        }
                        storeSearchQuery = ""
                      }
                      .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(med.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                      Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${currencyFmt.format(med.sellingPrice)} Ks", fontSize = 10.sp, color = SlateMuted)
                    }
                    Button(
                      onClick = {
                        val existingIdx = selectedConsumables.indexOfFirst { it.medicineId == med.id }
                        if (existingIdx >= 0) {
                          val curr = selectedConsumables[existingIdx]
                          selectedConsumables[existingIdx] = curr.copy(quantity = curr.quantity + 1)
                        } else {
                          selectedConsumables.add(
                            TreatmentConsumable(
                              medicineId = med.id,
                              name = med.name,
                              quantity = 1,
                              unitPrice = med.sellingPrice
                            )
                          )
                        }
                        storeSearchQuery = ""
                      },
                      shape = RoundedCornerShape(4.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      modifier = Modifier.height(24.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
                    ) {
                      Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                  HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                }
              }
            }
          }

          if (selectedConsumables.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            selectedConsumables.forEachIndexed { idx, c ->
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = BgLightGray,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(c.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                    Text("${currencyFmt.format(c.unitPrice)} Ks x ${c.quantity} = ${currencyFmt.format(c.totalPrice)} Ks", fontSize = 9.5.sp, color = MedicalGreenDark)
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = {
                        if (c.quantity > 1) {
                          selectedConsumables[idx] = c.copy(quantity = c.quantity - 1)
                        } else {
                          selectedConsumables.removeAt(idx)
                        }
                      },
                      modifier = Modifier.size(22.dp)
                    ) {
                      Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Minus", tint = StatusCritical, modifier = Modifier.size(16.dp))
                    }
                    Text("${c.quantity}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(
                      onClick = {
                        selectedConsumables[idx] = c.copy(quantity = c.quantity + 1)
                      },
                      modifier = Modifier.size(22.dp)
                    ) {
                      Icon(Icons.Default.AddCircleOutline, contentDescription = "Plus", tint = MedicalGreen, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                      onClick = { selectedConsumables.removeAt(idx) },
                      modifier = Modifier.size(22.dp)
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = StatusCritical, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 4. Clinical Notes
          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ဆေးထိုးမှတ်ချက် (Notes e.g. Skin test neg, Left arm)", fontSize = 10.5.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total Summary Card
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SoftGreenContainer,
          border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("ယနေ့ စုစုပေါင်း ကျသင့်ငွေ (Total Net):", fontSize = 11.sp, color = SlateMedium)
              Text(
                "ဆေးဖိုး (${currencyFmt.format(medPrice)}) + ဆေးထိုးခ (${currencyFmt.format(sessionFee)}) + အသုံးစရိတ် (${currencyFmt.format(consumablesTotal)})",
                fontSize = 9.5.sp,
                color = SlateDark
              )
            }
            Text(
              "${currencyFmt.format(visitTotal)} Ks",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MedicalGreenDark
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Confirm Button
        Button(
          onClick = {
            onConfirm(isClinicMed, medPrice, sessionFee, selectedConsumables.toList(), isConsumablesFoc, notes)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("အတည်ပြုပြီး ဘောင်ချာထုတ်မည် (Confirm & Bill)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Modal to discontinue/void a treatment card
 */
@Composable
fun DiscontinueCardModal(
  card: TreatmentCard,
  onDismiss: () -> Unit,
  onConfirm: (reason: String, returnStock: Boolean) -> Unit
) {
  var selectedReason by remember { mutableStateOf("လူနာမှ မထိုးဖြစ်တော့သဖြင့် ပယ်ဖျက်ခြင်း (Patient cancelled / Did not take)") }
  var customReason by remember { mutableStateOf("") }
  var returnStockToStore by remember { mutableStateOf(true) }

  val reasons = listOf(
    "လူနာမှ မထိုးဖြစ်တော့သဖြင့် ပယ်ဖျက်ခြင်း (Patient cancelled / Did not take)",
    "ဆရာဝန်မှ ဆေးပြောင်းလဲကုသခြင်း (Treatment changed by Doctor)",
    "ဆေးမတည့်သည့် လက္ခဏာပြသဖြင့် ရပ်ဆိုင်းခြင်း (Allergic / Adverse Reaction)",
    "လူနာ ရက်ချိန်း ဆက်မလာတော့ခြင်း (Patient defaulted / Did not show up)",
    "ဆေးညွှန်း အမှားပါသဖြင့် ပယ်ဖျက်ခြင်း (Order entered by mistake)"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(28.dp)) },
    title = {
      val isDiscontinueMode = card.completedSessionsCount > 0
      Text(
        if (isDiscontinueMode) "ကုသမှု ရပ်ဆိုင်းမည် (Discontinue Course)" else "ကုသမှု ပယ်ဖျက်မည် (Void / Delete Card)",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column {
        Text("လူနာ: ${card.patientName} (${card.orderSummaryText})", fontSize = 11.5.sp, color = SlateDark)
        Text("ပြီးစီးခဲ့သော အကြိမ်ရေ: ${card.completedSessionsCount} of ${card.totalSessions}", fontSize = 10.5.sp, color = SlateMuted)
        Spacer(modifier = Modifier.height(10.dp))

        if (!card.isPatientSupplied) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftGreenContainer,
            border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().clickable { returnStockToStore = !returnStockToStore }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Checkbox(
                checked = returnStockToStore,
                onCheckedChange = { returnStockToStore = it },
                colors = CheckboxDefaults.colors(checkedColor = MedicalGreen)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  "ဆေးဝါးနှင့် အသုံးအဆောင်များ စတော့ခ်ထဲ ပြန်သွင်းမည်",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = MedicalGreenDark
                )
                Text(
                  "Return medicines & consumables to pharmacy inventory",
                  fontSize = 8.5.sp,
                  color = SlateMedium
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Text("ပယ်ဖျက်ရသည့် အကြောင်းရင်း ရွေးချယ်ပါ:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        reasons.forEach { r ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = r }
              .padding(vertical = 2.dp)
          ) {
            RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
            Spacer(modifier = Modifier.width(4.dp))
            Text(r, fontSize = 10.sp, color = SlateDark)
          }
        }
      }
    },
    confirmButton = {
      val isDiscontinueMode = card.completedSessionsCount > 0
      Button(
        onClick = { onConfirm(if (customReason.isNotBlank()) customReason else selectedReason, returnStockToStore) },
        colors = ButtonDefaults.buttonColors(containerColor = if (isDiscontinueMode) StatusWarning else StatusCritical)
      ) {
        Text(
          if (isDiscontinueMode) "ကုသမှု ရပ်ဆိုင်းမည် (Discontinue)"
          else if (returnStockToStore) "ပယ်ဖျက်ပြီး စတော့ခ်ပြန်သွင်းမည်"
          else "ကတ်ဖျက်မည် (Delete)",
          fontSize = 11.sp
        )
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("မလုပ်တော့ပါ", fontSize = 11.sp)
      }
    }
  )
}

/**
 * Modal to create a new treatment card manually from presets or custom medication
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTreatmentCardModal(
  patients: List<Patient>,
  medicines: List<Medicine>,
  clinicSettings: ClinicSettings = ClinicSettings(),
  defaultDoctor: String,
  onDismiss: () -> Unit,
  onCreate: (TreatmentCard) -> Unit
) {
  var selectedPatient by remember { mutableStateOf<Patient?>(patients.firstOrNull()) }
  var isCocktail by remember { mutableStateOf(false) }
  var isPatientSupplied by remember { mutableStateOf(false) }

  var medicationName by remember { mutableStateOf("") }
  var cocktailTitle by remember { mutableStateOf("") }
  val cocktailItems = remember { mutableStateListOf<CocktailDrugItem>() }

  var dosage by remember { mutableStateOf("") }
  var singleDrugQuantity by remember { mutableStateOf(1) }
  var selectedSingleMedicine by remember { mutableStateOf<Medicine?>(null) }
  var selectedRoute by remember { mutableStateOf(TreatmentRoute.IV_PUSH) }
  var customRouteText by remember { mutableStateOf("") }
  var frequency by remember { mutableStateOf("Stat") }
  var totalDaysText by remember { mutableStateOf("1") }
  var medFeeText by remember { mutableStateOf("0") }
  var sessionFeeText by remember(selectedRoute) { mutableStateOf(selectedRoute.getDefaultFee(clinicSettings).toInt().toString()) }
  var instructions by remember { mutableStateOf("") }
  var storeSearchQuery by remember { mutableStateOf("") }
  var splitVialMode by remember { mutableStateOf(false) }
  var isPayPerVisit by remember { mutableStateOf(true) }

  var addWoundDressing by remember { mutableStateOf(false) }
  var dressingFeeText by remember { mutableStateOf("5000") }

  val consumablesList = remember { mutableStateListOf<TreatmentConsumable>() }
  var consumableSearchQuery by remember { mutableStateOf("") }

  val filteredConsumables = remember(medicines, consumableSearchQuery) {
    if (consumableSearchQuery.isBlank()) emptyList()
    else medicines.filter {
      (it.effectiveMedicineType == "Consumables" ||
       it.dosageForm.contains("Consumable", ignoreCase = true) ||
       it.category.contains("Consumable", ignoreCase = true) ||
       it.name.contains("Syringe", ignoreCase = true) ||
       it.name.contains("Needle", ignoreCase = true) ||
       it.name.contains("Scalp", ignoreCase = true) ||
       it.name.contains("Cannula", ignoreCase = true) ||
       it.name.contains("Set", ignoreCase = true) ||
       it.name.contains("Swab", ignoreCase = true) ||
       it.name.contains("Plaster", ignoreCase = true) ||
       it.name.contains("Water", ignoreCase = true) ||
       it.name.contains(consumableSearchQuery, ignoreCase = true)) &&
      (it.name.contains(consumableSearchQuery, ignoreCase = true) ||
       it.genericName.contains(consumableSearchQuery, ignoreCase = true))
    }.take(8)
  }

  val filteredStoreMeds = remember(medicines, storeSearchQuery) {
    if (storeSearchQuery.isBlank()) emptyList()
    else medicines.filter {
      (it.effectiveMedicineType in listOf("Injection", "Consumables")) &&
      (it.name.contains(storeSearchQuery, ignoreCase = true) ||
       it.genericName.contains(storeSearchQuery, ignoreCase = true))
    }.take(8)
  }

  val days = totalDaysText.toIntOrNull() ?: 1
  val medFee = if (isPatientSupplied) 0.0 else (medFeeText.toDoubleOrNull() ?: 0.0)
  val sessionFee = sessionFeeText.toDoubleOrNull() ?: selectedRoute.getDefaultFee(clinicSettings)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.92f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("ကုသမှုကတ် အသစ်ဖွင့်လှစ်ခြင်း (New Day-Care Card)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Searchable Patient Selector
          var patientSearchQuery by remember { mutableStateOf("") }
          val filteredPatients = remember(patients, patientSearchQuery) {
            if (patientSearchQuery.isBlank()) patients.take(12)
            else patients.filter {
              it.name.contains(patientSearchQuery, ignoreCase = true) ||
              it.hn.contains(patientSearchQuery, ignoreCase = true) ||
              it.phone.contains(patientSearchQuery, ignoreCase = true)
            }.take(12)
          }

          Text("လူနာ ရွေးချယ်ပါ (Select Patient):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)

          // Currently Selected Patient Banner
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (selectedPatient != null) SoftGreenContainer else PureWhite,
            border = BorderStroke(1.dp, if (selectedPatient != null) MedicalGreen else BorderGrey),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (selectedPatient != null) {
                Column {
                  Text("ရွေးချယ်ထားသော လူနာ: ${selectedPatient?.name}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                  Text("HN: ${selectedPatient?.hn} • ${selectedPatient?.age} Yrs • 📞 ${selectedPatient?.phone}", fontSize = 9.5.sp, color = SlateMedium)
                }
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
              } else {
                Text("လူနာ မရွေးချယ်ရသေးပါ (ရှာဖွေပြီး ရွေးချယ်ပါ)", fontSize = 10.5.sp, color = StatusWarning)
              }
            }
          }

          // Search Field for Patient
          OutlinedTextField(
            value = patientSearchQuery,
            onValueChange = { patientSearchQuery = it },
            placeholder = { Text("လူနာအမည်၊ HN သို့မဟုတ် ဖုန်းဖြင့် ရှာရန်...", fontSize = 10.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp)) },
            trailingIcon = {
              if (patientSearchQuery.isNotBlank()) {
                IconButton(onClick = { patientSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                  Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          // Filtered Patient Quick Select Chips
          if (filteredPatients.isNotEmpty()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              filteredPatients.forEach { p ->
                val isSel = selectedPatient?.id == p.id
                FilterChip(
                  selected = isSel,
                  onClick = { selectedPatient = p },
                  label = { Text("${p.name} (${p.hn})", fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MedicalGreen,
                    selectedLabelColor = PureWhite
                  )
                )
              }
            }
          }

          // 1. Order Type Mode Selector: Single Drug vs Cocktail Mix
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(3.dp)) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (!isCocktail) MedicalGreen else Color.Transparent,
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    isCocktail = false
                    if (!isPatientSupplied) medFeeText = "4500"
                  }
                  .padding(vertical = 6.dp),
                contentColor = if (!isCocktail) PureWhite else SlateMedium
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Single Drug (ဆေး ၁ မျိုး)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isCocktail) MedicalGreen else Color.Transparent,
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    isCocktail = true
                    if (selectedRoute == TreatmentRoute.IM || selectedRoute == TreatmentRoute.DRESSING) {
                      selectedRoute = TreatmentRoute.IV_INFUSION
                    }
                    if (!isPatientSupplied) {
                      medFeeText = cocktailItems.sumOf { it.totalPrice }.toInt().toString()
                    }
                  }
                  .padding(vertical = 6.dp),
                contentColor = if (isCocktail) PureWhite else SlateMedium
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Cocktail / IV Mix (တွဲဖက်ဆေး)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          // 2. Patient-Supplied / External Medicine Toggle Banner
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isPatientSupplied) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isPatientSupplied) Color(0xFFF59E0B) else BorderGrey),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                isPatientSupplied = !isPatientSupplied
                if (isPatientSupplied) {
                  medFeeText = "0"
                } else {
                  medFeeText = if (isCocktail) cocktailItems.sumOf { it.totalPrice }.toInt().toString() else "4500"
                }
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = isPatientSupplied,
                onCheckedChange = {
                  isPatientSupplied = it
                  if (it) {
                    medFeeText = "0"
                  } else {
                    medFeeText = if (isCocktail) cocktailItems.sumOf { c -> c.totalPrice }.toInt().toString() else "4500"
                  }
                },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text(
                  "လူနာပါ ဆေးဝါး (Patient-Supplied / External Medicine)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isPatientSupplied) Color(0xFF92400E) else SlateDark
                )
                Text(
                  if (isPatientSupplied) "လူနာမှ ကိုယ်တိုင်ယူဆောင်လာသောဆေး (ဆေးဖိုး 0 Ks • Store မနှုတ်ပါ • ဝန်ဆောင်ခသာ ယူမည်)"
                  else "ဆေးခန်း Store ဆေးသုံးစွဲမည် (Stock နှုတ်ပြီး ဆေးဖိုးတွက်ချက်မည်)",
                  fontSize = 9.sp,
                  color = if (isPatientSupplied) Color(0xFFB45309) else SlateMuted
                )
              }
            }
          }

          if (!isCocktail) {
            // === Single Drug Mode ===
            // Search Store
            OutlinedTextField(
              value = storeSearchQuery,
              onValueChange = { storeSearchQuery = it },
              placeholder = { Text("Store ဆေးစာရင်းမှ ရှာဖွေရွေးချယ်ရန် (Optional)...", fontSize = 11.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
              trailingIcon = {
                if (storeSearchQuery.isNotBlank()) {
                  IconButton(onClick = { storeSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                  }
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            if (filteredStoreMeds.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(4.dp)) {
                  filteredStoreMeds.forEach { med ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          medicationName = med.name
                          selectedSingleMedicine = med
                          if (!isPatientSupplied) {
                            medFeeText = (med.sellingPrice * singleDrugQuantity).toInt().toString()
                          }
                          if (med.standardDosage.isNotBlank()) {
                            dosage = med.standardDosage
                          }
                          val lower = med.name.lowercase()
                          if (instructions.isBlank()) {
                            when {
                              lower.contains("rabies") || lower.contains("tetanus") || lower.contains("toxoid") ||
                              lower.contains("hep") || lower.contains("vaccine") || lower.contains("flu") -> {
                                instructions = "Deltoid IM Stat (လက်မောင်း)"
                                selectedRoute = TreatmentRoute.IM
                              }
                              lower.contains("depo") -> {
                                instructions = "Deep IM Stat (Gluteal)"
                                selectedRoute = TreatmentRoute.IM
                              }
                              lower.contains("ceftri") || lower.contains("roceph") -> {
                                instructions = "Slow IV Push (over 3-5 mins)"
                                selectedRoute = TreatmentRoute.IV_PUSH
                              }
                              lower.contains("todol") || lower.contains("tramadol") || lower.contains("ketorolac") -> {
                                instructions = "Slow IV Push / IM"
                                selectedRoute = TreatmentRoute.IV_PUSH
                              }
                            }
                          }
                          storeSearchQuery = ""
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(med.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                        Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 9.5.sp, color = SlateMuted)
                      }
                      Text("Select", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                  }
                }
              }
            }

            // Medication Name, Dosage & Qty/Amps Stepper
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = medicationName,
                onValueChange = { medicationName = it },
                label = { Text("ဆေးအမည် (Medication Name)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1.3f)
              )
              OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("ပမာဏ (Dosage)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.7f)
              )
              // Quantity / Amps Stepper
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.weight(0.8f).height(56.dp)
              ) {
                Column(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Text("အလုံးရေ (Qty/Amps)", fontSize = 8.5.sp, color = SlateMuted)
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    IconButton(
                      onClick = {
                        if (singleDrugQuantity > 1) {
                          singleDrugQuantity--
                          selectedSingleMedicine?.let { med ->
                            if (!isPatientSupplied) {
                              medFeeText = (med.sellingPrice * singleDrugQuantity).toInt().toString()
                            }
                          }
                        }
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(12.dp), tint = SlateDark)
                    }
                    Text(
                      "$singleDrugQuantity",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.5.sp,
                      color = MedicalGreenDark,
                      modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                      onClick = {
                        singleDrugQuantity++
                        selectedSingleMedicine?.let { med ->
                          if (!isPatientSupplied) {
                            medFeeText = (med.sellingPrice * singleDrugQuantity).toInt().toString()
                          }
                        }
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(12.dp), tint = MedicalGreenDark)
                    }
                  }
                }
              }
            }

            // Dosage Quick Chips
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              listOf("30 mg", "75 mg", "300 mg", "500 mg", "1 g", "1.2 g", "1 Amp", "1 Vial", "1 Bottle").forEach { d ->
                AssistChip(
                  onClick = { dosage = d },
                  label = { Text(d, fontSize = 9.5.sp) }
                )
              }
            }
          } else {
            // === Cocktail / IV Mix Mode ===
            OutlinedTextField(
              value = cocktailTitle,
              onValueChange = { cocktailTitle = it },
              label = { Text("Cocktail အမည် (Cocktail Title / Order Name)", fontSize = 11.sp) },
              placeholder = { Text("ဥပမာ - IV NS + Burplex + Vit-C သို့မဟုတ် IV Todol + NS 3cc", fontSize = 10.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            // Search to Add Drugs to Cocktail
            OutlinedTextField(
              value = storeSearchQuery,
              onValueChange = { storeSearchQuery = it },
              placeholder = { Text("+ Store ဆေးစာရင်းမှ ဆေးထည့်သွင်းရန် ရှာဖွေပါ (Add Drug)...", fontSize = 11.sp) },
              leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
              trailingIcon = {
                if (storeSearchQuery.isNotBlank()) {
                  IconButton(onClick = { storeSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                  }
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            if (filteredStoreMeds.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(4.dp)) {
                  filteredStoreMeds.forEach { med ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          cocktailItems.add(
                            CocktailDrugItem(
                              medicineId = med.id,
                              name = med.name,
                              dosage = med.standardDosage.ifBlank { "1 amp" },
                              quantity = 1,
                              unitPrice = med.sellingPrice
                            )
                          )
                          if (!isPatientSupplied) {
                            medFeeText = cocktailItems.sumOf { it.totalPrice }.toInt().toString()
                          }
                          if (cocktailTitle.isBlank()) {
                            cocktailTitle = cocktailItems.joinToString(" + ") { it.name.replace(Regex("(?i)^inj\\.?\\s*"), "") }
                          }
                          storeSearchQuery = ""
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(med.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                        Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 9.5.sp, color = SlateMuted)
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add to Mix", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                      }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                  }
                }
              }
            }

            // Constituent Drugs List
            Text(
              "တွဲစပ်ထားသော ဆေးများ (Constituent Drugs - ${cocktailItems.size} items):",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )

            if (cocktailItems.isEmpty()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
              ) {
                Text(
                  "Cocktail တွင် ပါဝင်မည့် ဆေး ၂ မျိုး သို့မဟုတ် ၃ မျိုးကို အထက်ပါ Store ရှာဖွေမှုမှ ရွေးချယ်ထည့်သွင်းပါ",
                  fontSize = 10.sp,
                  color = SlateMuted,
                  modifier = Modifier.padding(10.dp)
                )
              }
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cocktailItems.forEachIndexed { idx, item ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                        Text(
                          "Dose: ${item.dosage.ifBlank { "-" }} • Qty: ${item.quantity} • ${item.unitPrice.toInt()} Ks/unit",
                          fontSize = 9.5.sp,
                          color = SlateMedium
                        )
                      }

                      IconButton(
                        onClick = {
                          cocktailItems.removeAt(idx)
                          if (!isPatientSupplied) {
                            medFeeText = cocktailItems.sumOf { it.totalPrice }.toInt().toString()
                          }
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusCritical, modifier = Modifier.size(15.dp))
                      }
                    }
                  }
                }
              }
            }
          }

          // 2.5 Manual Consumables / Supplies Selection (Syringe, Needle, Scalp Vein, etc.)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, BorderGrey),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "အသုံးစာရင်း / ဆေးထိုးပစ္စည်းများ (Consumables - ${consumablesList.size} items):",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateDark
                )
                if (consumablesList.isNotEmpty()) {
                  val cFmt = NumberFormat.getNumberInstance(Locale.US)
                  Text(
                    "Total: ${cFmt.format(consumablesList.sumOf { it.totalPrice }.toInt())} Ks",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = consumableSearchQuery,
                onValueChange = { consumableSearchQuery = it },
                placeholder = { Text("+ Store စာရင်းမှ ဆေးထိုးအပ်/အသုံးပစ္စည်း ရှာဖွေထည့်ရန် (e.g. 3cc Syringe, Scalp Vein, Needle)...", fontSize = 10.5.sp) },
                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                  if (consumableSearchQuery.isNotBlank()) {
                    IconButton(onClick = { consumableSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                      Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              if (filteredConsumables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = PureWhite,
                  border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(4.dp)) {
                    filteredConsumables.forEach { med ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            val existingIdx = consumablesList.indexOfFirst { it.medicineId == med.id || it.name.equals(med.name, ignoreCase = true) }
                            if (existingIdx >= 0) {
                              val cur = consumablesList[existingIdx]
                              consumablesList[existingIdx] = cur.copy(quantity = cur.quantity + 1)
                            } else {
                              consumablesList.add(
                                TreatmentConsumable(
                                  medicineId = med.id,
                                  name = med.name,
                                  quantity = 1,
                                  unitPrice = med.sellingPrice
                                )
                              )
                            }
                            consumableSearchQuery = ""
                          }
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(med.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                          Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 9.sp, color = SlateMuted)
                        }
                        Text("+ Add", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                      }
                      HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }
                  }
                }
              }

              if (consumablesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  consumablesList.forEachIndexed { idx, cons ->
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = PureWhite,
                      border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(cons.name, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                          Text("${cons.unitPrice.toInt()} Ks x ${cons.quantity} = ${cons.totalPrice.toInt()} Ks", fontSize = 9.sp, color = SlateMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          IconButton(
                            onClick = {
                              if (cons.quantity > 1) {
                                consumablesList[idx] = cons.copy(quantity = cons.quantity - 1)
                              } else {
                                consumablesList.removeAt(idx)
                              }
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                          }
                          Text("${cons.quantity}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                          IconButton(
                            onClick = {
                              consumablesList[idx] = cons.copy(quantity = cons.quantity + 1)
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                          }
                          IconButton(
                            onClick = { consumablesList.removeAt(idx) },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                          }
                        }
                      }
                    }
                  }
                }
              } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text("(ဆေးထိုးအပ်/ပစ္စည်း ထည့်သွင်းလိုပါက အထက်တွင် ရှာဖွေရွေးချယ်ပါ - မလိုပါက ကျော်နိုင်ပါသည်)", fontSize = 9.sp, color = SlateMuted)
              }
            }
          }

          // 2.6 Option A: Add Extra Nursing Service / Wound Dressing
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (addWoundDressing) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (addWoundDressing) Color(0xFF3B82F6) else BorderGrey),
            modifier = Modifier.fillMaxWidth().clickable { addWoundDressing = !addWoundDressing }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = addWoundDressing,
                onCheckedChange = { addWoundDressing = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  "+ အနာဆေးထည့်ခြင်း ထပ်တိုးပေါင်းထည့်မည် (Add Wound Dressing Service)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (addWoundDressing) Color(0xFF1E40AF) else SlateDark
                )
                Text(
                  if (addWoundDressing) "ဆေးထိုးခြင်းနှင့်အတူ အနာဆေးထည့်ခြင်း ဝန်ဆောင်မှုကို ဘောင်ချာတစ်ခုတည်း၌ ပေါင်းစည်းကောက်ခံပါမည်"
                  else "ဆေးထိုးခြင်းအပြင် အနာဆေးထည့်ပေးခြင်းပါ တစ်ပါတည်း ဆောင်ရွက်လိုပါက အမှန်ခြစ်ပါ (ဆရာဝန် Consultation မလိုပါ)",
                  fontSize = 9.5.sp,
                  color = if (addWoundDressing) Color(0xFF1D4ED8) else SlateMuted
                )
              }
              if (addWoundDressing) {
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                  value = dressingFeeText,
                  onValueChange = { dressingFeeText = it },
                  label = { Text("ဆေးထည့်ခ Ks", fontSize = 8.5.sp) },
                  singleLine = true,
                  modifier = Modifier.width(95.dp)
                )
              }
            }
          }

          // 3. Route Selector
          Text("ထိုးမည့်နည်းလမ်း (Route of Administration):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TreatmentRoute.entries.forEach { r ->
              val isSel = selectedRoute == r
              FilterChip(
                selected = isSel,
                onClick = {
                  selectedRoute = r
                  sessionFeeText = r.getDefaultFee(clinicSettings).toInt().toString()
                },
                label = { Text(r.label, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MedicalGreen,
                  selectedLabelColor = PureWhite
                )
              )
            }
          }

          if (selectedRoute == TreatmentRoute.CUSTOM) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = customRouteText,
              onValueChange = { customRouteText = it },
              label = { Text("စိတ်ကြိုက် နည်းလမ်းအမည် (Custom Route)", fontSize = 11.sp) },
              placeholder = { Text("e.g. Intradermal (ID), Joint Injection, Eye Drop, Enema...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          // 4. Frequency & Duration
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = frequency,
              onValueChange = { frequency = it },
              label = { Text("ကြိမ်နှုန်း (Frequency)", fontSize = 11.sp) },
              placeholder = { Text("12 hrly, BD, Stat") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = totalDaysText,
              onValueChange = { totalDaysText = it },
              label = { Text("ရက်ပေါင်း (Days)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(0.8f)
            )
          }

          // Quick Frequency Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("Stat (1 time)", "12 hrly", "BD (2 times/day)", "TDS (3 times/day)", "Daily (OD)").forEach { f ->
              ElevatedAssistChip(
                onClick = {
                  frequency = f.split(" ").first()
                  if (f.startsWith("Stat")) totalDaysText = "1"
                },
                label = { Text(f, fontSize = 9.5.sp) }
              )
            }
          }

          // 5. Billing Calculation Mode
          Text("ဆေးပုလင်း တွက်ချက်မှုပုံစံ (Billing Calculation):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (!splitVialMode) SoftGreenContainer else PureWhite,
              border = BorderStroke(1.dp, if (!splitVialMode) MedicalGreen else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { splitVialMode = false }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = !splitVialMode,
                  onClick = { splitVialMode = false },
                  colors = RadioButtonDefaults.colors(selectedColor = MedicalGreen),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("Dose တိုင်း ဆေးဖိုးတွက်မည် (Standard Default)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("ထိုးသည့်အကြိမ်တိုင်းအတွက် ဆေး ၁ လုံးစီ သီးခြားတွက်ချက်မည်", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (splitVialMode) Color(0xFFF0F9FF) else PureWhite,
              border = BorderStroke(1.dp, if (splitVialMode) Color(0xFF0284C7) else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { splitVialMode = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = splitVialMode,
                  onClick = { splitVialMode = true },
                  colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7)),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("ကလေး/ခွဲထိုး ဆေးတွက်ချက်မှု (Split-Vial Mode)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("၁ ရက်လျှင် ဆေး ၁ ပုလင်းသာ အားလုံးခွဲဝေထိုးမည် (ဆေးပုလင်း အပိုမဖြုန်းတီးပါ)", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }
          }

          // 6. Payment Option
          Text("ငွေပေးချေမှုပုံစံ (Payment Option):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isPayPerVisit) SoftGreenContainer else PureWhite,
              border = BorderStroke(1.dp, if (isPayPerVisit) MedicalGreen else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { isPayPerVisit = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isPayPerVisit,
                  onClick = { isPayPerVisit = true },
                  colors = RadioButtonDefaults.colors(selectedColor = MedicalGreen),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("တစ်ကြိမ်ချင်းရှင်းမည် (Pay per Visit - Default)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("လာထိုးသည့် နေ့စဉ်အကြိမ်တိုင်းအတွက်သာ အလှည့်ကျငွေရှင်းမည် (လူနာကြိုက်)", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (!isPayPerVisit) Color(0xFFFAF5FF) else PureWhite,
              border = BorderStroke(1.dp, if (!isPayPerVisit) Color(0xFF7C3AED) else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { isPayPerVisit = false }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = !isPayPerVisit,
                  onClick = { isPayPerVisit = false },
                  colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C3AED)),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("တစ်ခေါက်တည်း အကြေရှင်းမည် (Full Course Upfront)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("ရက်ပြည့် course စာရင်းတစ်ခုလုံးကို ပထမဆုံး visit တွင် အကြေရှင်းမည်", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }
          }

          // 7. Fee breakdown with clear Auto-Sum of Medication + Consumables
          val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
          val totalConsumablesFee = consumablesList.sumOf { it.totalPrice }
          val effectiveBaseMedFee = if (isPatientSupplied) 0.0 else (medFeeText.toDoubleOrNull() ?: 0.0)
          val dressingFee = if (addWoundDressing) (dressingFeeText.toDoubleOrNull() ?: 5000.0) else 0.0
          val baseSessionFee = sessionFeeText.toDoubleOrNull() ?: selectedRoute.getDefaultFee(clinicSettings)
          val totalSessionFee = baseSessionFee + dressingFee
          val combinedDrugAndConsumables = effectiveBaseMedFee + totalConsumablesFee
          val grandTotalPerSession = combinedDrugAndConsumables + totalSessionFee

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("၁ ကြိမ် စုစုပေါင်းကျသင့်ငွေ (Per Session Total):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                Text("${currencyFmt.format(grandTotalPerSession.toInt())} Ks", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                buildString {
                  append("ဆေးဖိုး: ${currencyFmt.format(effectiveBaseMedFee.toInt())} Ks")
                  if (totalConsumablesFee > 0) append(" + အသုံးပစ္စည်း: ${currencyFmt.format(totalConsumablesFee.toInt())} Ks")
                  append(" + ထိုးခ: ${currencyFmt.format(baseSessionFee.toInt())} Ks")
                  if (addWoundDressing) append(" + အနာဆေးထည့်ခ: ${currencyFmt.format(dressingFee.toInt())} Ks")
                },
                fontSize = 9.5.sp,
                color = SlateMedium
              )
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = if (isPatientSupplied) "0 (လူနာပါဆေး)" else medFeeText,
              onValueChange = { if (!isPatientSupplied) medFeeText = it },
              enabled = !isPatientSupplied,
              label = { Text("ဆေးဖိုး (Med Fee Ks)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sessionFeeText,
              onValueChange = { sessionFeeText = it },
              label = { Text("ဆေးထိုးဝန်ဆောင်ခ (Service Ks)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          // 8. Instructions
          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text(if (!isCocktail) "အပိုဆောင်း ညွှန်ကြားချက် / မှတ်ချက် (Additional Remarks / Instructions)" else "ညွှန်ကြားချက် (Instructions / Drip Rate)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val canGenerate = (selectedPatient != null) && (
          if (isCocktail) cocktailTitle.isNotBlank() || cocktailItems.isNotEmpty()
          else medicationName.isNotBlank()
        )

        Button(
          onClick = {
            val itemsSummary = if (cocktailItems.isNotEmpty()) {
              cocktailItems.joinToString(" + ") { "${it.name} ${it.dosage}".trim() }
            } else ""
            val finalMedName = if (isCocktail) {
              cocktailTitle.ifBlank {
                if (itemsSummary.isNotBlank()) itemsSummary
                else "IV Cocktail Mix"
              }
            } else {
              medicationName
            }
            val finalDosage = if (isCocktail) {
              if (itemsSummary.isNotBlank()) "${cocktailItems.size} Drugs ($itemsSummary)"
              else "${cocktailItems.size} Drugs Mixed"
            } else {
              if (singleDrugQuantity > 1) {
                if (dosage.isNotBlank()) "$singleDrugQuantity Amps ($dosage)" else "$singleDrugQuantity Amps"
              } else {
                dosage
              }
            }

            if (!isCocktail && selectedSingleMedicine != null && cocktailItems.isEmpty()) {
              cocktailItems.add(
                CocktailDrugItem(
                  medicineId = selectedSingleMedicine!!.id,
                  name = selectedSingleMedicine!!.name,
                  dosage = dosage,
                  quantity = singleDrugQuantity,
                  unitPrice = selectedSingleMedicine!!.sellingPrice
                )
              )
            }

            val dressingFee = if (addWoundDressing) (dressingFeeText.toDoubleOrNull() ?: 5000.0) else 0.0
            val baseSessionFee = sessionFeeText.toDoubleOrNull() ?: selectedRoute.getDefaultFee(clinicSettings)
            val effectiveSessionFee = baseSessionFee + dressingFee

            val sessions = TreatmentCardFactory.generateSessions(
              startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
              frequency = frequency.ifBlank { "Stat" },
              days = days,
              medicationFee = medFee,
              sessionFee = effectiveSessionFee,
              splitVialMode = splitVialMode,
              consumables = consumablesList.toList()
            )
            val newCard = TreatmentCard(
              patientId = selectedPatient?.id ?: "",
              patientName = selectedPatient?.name ?: "Unknown Patient",
              patientHn = selectedPatient?.hn ?: "HN-1000",
              doctorName = defaultDoctor,
              medicationName = if (addWoundDressing) "$finalMedName + အနာဆေးထည့်ခြင်း" else finalMedName,
              dosage = finalDosage,
              route = selectedRoute,
              customRoute = if (selectedRoute == TreatmentRoute.CUSTOM) customRouteText.trim() else "",
              frequency = frequency.ifBlank { "Stat" },
              totalDays = days,
              totalSessions = sessions.size,
              sessions = sessions.mapIndexed { idx, s ->
                if (!isPayPerVisit) s.copy(isPrepaid = true) else s
              },
              sessionFee = effectiveSessionFee,
              medicationFee = medFee,
              instructions = instructions,
              isPayPerVisit = isPayPerVisit,
              isPrepaidFullCourse = !isPayPerVisit,
              isCocktail = isCocktail,
              cocktailItems = if (isCocktail) cocktailItems.toList() else emptyList(),
              consumables = consumablesList.toList(),
              isPatientSupplied = isPatientSupplied,
              notes = if (addWoundDressing) "Includes Wound Dressing (${dressingFee.toInt()} Ks)" else ""
            )
            onCreate(newCard)
          },
          enabled = canGenerate,
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("ကုသမှုကတ် အသစ်ဖွင့်မည် (Generate Card)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Modal to edit an existing Treatment Card (Total days, dosage, frequency, fees, remarks)
 * Preserves completed doses while recalculating remaining pending doses.
 */
@Composable
fun EditTreatmentCardModal(
  card: TreatmentCard,
  clinicSettings: ClinicSettings,
  onDismiss: () -> Unit,
  onSave: (TreatmentCard) -> Unit
) {
  var medicationName by remember { mutableStateOf(card.medicationName) }
  var dosage by remember { mutableStateOf(card.dosage) }
  var totalDaysText by remember { mutableStateOf(card.totalDays.toString()) }
  var frequency by remember { mutableStateOf(card.frequency) }
  var selectedRoute by remember { mutableStateOf(card.route) }
  var customRouteText by remember { mutableStateOf(card.customRoute) }
  var sessionFeeText by remember { mutableStateOf(card.sessionFee.toInt().toString()) }
  var medFeeText by remember { mutableStateOf(card.medicationFee.toInt().toString()) }
  var instructions by remember { mutableStateOf(card.instructions) }

  val days = totalDaysText.toIntOrNull() ?: card.totalDays
  val sessionFee = sessionFeeText.toDoubleOrNull() ?: card.sessionFee
  val medFee = medFeeText.toDoubleOrNull() ?: card.medicationFee
  val completedCount = card.completedSessionsCount

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.92f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("ကုသမှုကတ် ပြင်ဆင်ရန် (Edit Treatment Card)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Text("လူနာ: ${card.patientName} (${card.patientHn}) • ကတ်ဖွင့်ရက်: ${card.prescribedDate}", fontSize = 10.sp, color = SlateMuted)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGrey)

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Status & Completed Sessions Notice
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "ထိုးပြီးစီးပြီး Dose: $completedCount ကြိမ် (ယခင်မှတ်တမ်း မပျက်ပြယ်ဘဲ ထိန်းသိမ်းထားပါမည်။ ရက်နှင့်အကြိမ် ပြင်ဆင်ပါက ကျန်ရှိသော Pending Doses များကိုသာ အသစ်ညှိနှိုင်းပေးပါမည်။)",
                fontSize = 9.5.sp,
                color = MedicalGreenDark,
                lineHeight = 13.sp
              )
            }
          }

          // Medicine Name & Dosage
          OutlinedTextField(
            value = medicationName,
            onValueChange = { medicationName = it },
            label = { Text("ဆေးအမည် (Medication Name)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = dosage,
              onValueChange = { dosage = it },
              label = { Text("ဆေးပမာဏ (Dosage)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = totalDaysText,
              onValueChange = { totalDaysText = it },
              label = { Text("ရက်ပေါင်း (Total Days)", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          // Frequency Chips
          Text("အကြိမ်အရေအတွက် (Frequency):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("Stat", "Daily", "BD (2 times/day)", "TDS (3 times/day)", "12 hrly").forEach { f ->
              val fCode = f.split(" ").first()
              FilterChip(
                selected = frequency.startsWith(fCode, ignoreCase = true),
                onClick = {
                  frequency = fCode
                  if (fCode.equals("Stat", ignoreCase = true)) totalDaysText = "1"
                },
                label = { Text(f, fontSize = 9.5.sp) }
              )
            }
          }

          // Route Chips
          Text("ထိုးမည့်နည်းလမ်း (Route):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            TreatmentRoute.entries.forEach { r ->
              FilterChip(
                selected = selectedRoute == r,
                onClick = { selectedRoute = r },
                label = { Text(r.label.split(" ").first(), fontSize = 9.5.sp) }
              )
            }
          }
          if (selectedRoute == TreatmentRoute.CUSTOM) {
            OutlinedTextField(
              value = customRouteText,
              onValueChange = { customRouteText = it },
              label = { Text("စိတ်ကြိုက် နည်းလမ်း (Custom Route)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          // Fees
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = medFeeText,
              onValueChange = { medFeeText = it },
              label = { Text("ဆေးဖိုး (Med Fee Ks)", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sessionFeeText,
              onValueChange = { sessionFeeText = it },
              label = { Text("ဝန်ဆောင်ခ (Service Fee Ks)", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          // Instructions
          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("မှတ်ချက် / ညွှန်ကြားချက် (Instructions)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(42.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("မပြင်တော့ပါ (Cancel)", fontSize = 11.5.sp)
          }
          Button(
            onClick = {
              val completedSessions = card.sessions.filter { it.status == TreatmentStatus.COMPLETED }
              val newBaseSessions = TreatmentCardFactory.generateSessions(
                startDateStr = card.sessions.firstOrNull()?.scheduledDate ?: card.prescribedDate,
                frequency = frequency.ifBlank { "Stat" },
                days = days.coerceAtLeast(1),
                medicationFee = medFee,
                sessionFee = sessionFee,
                splitVialMode = false,
                consumables = card.consumables
              )

              val reconciledSessions = mutableListOf<TreatmentSession>()
              val targetTotal = maxOf(completedSessions.size, newBaseSessions.size)
              for (i in 0 until targetTotal) {
                if (i < completedSessions.size) {
                  reconciledSessions.add(completedSessions[i].copy(sessionNumber = i + 1))
                } else if (i < newBaseSessions.size) {
                  val tmpl = newBaseSessions[i]
                  reconciledSessions.add(
                    tmpl.copy(
                      sessionNumber = i + 1,
                      isPrepaid = card.isPrepaidFullCourse
                    )
                  )
                }
              }

              val updatedCard = card.copy(
                medicationName = medicationName.trim(),
                dosage = dosage.trim(),
                route = selectedRoute,
                customRoute = if (selectedRoute == TreatmentRoute.CUSTOM) customRouteText.trim() else "",
                frequency = frequency.ifBlank { "Stat" },
                totalDays = days.coerceAtLeast(1),
                totalSessions = reconciledSessions.size,
                sessions = reconciledSessions,
                sessionFee = sessionFee,
                medicationFee = medFee,
                instructions = instructions.trim(),
                cardStatus = if (reconciledSessions.isNotEmpty() && reconciledSessions.all { it.status == TreatmentStatus.COMPLETED }) TreatmentStatus.COMPLETED else TreatmentStatus.IN_PROGRESS
              )
              onSave(updatedCard)
            },
            modifier = Modifier.weight(1f).height(42.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ပြင်ဆင်မှု သိမ်းမည် (Save)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}


/**
 * Printable slip dialog for patient treatment card
 */
@Composable
fun TreatmentCardSlipDialog(
  card: TreatmentCard,
  clinicSettings: ClinicSettings = ClinicSettings(),
  onDismiss: () -> Unit
) {
  PosThermalTreatmentSlipDialog(
    card = card,
    clinicSettings = clinicSettings,
    onDismiss = onDismiss
  )
}
