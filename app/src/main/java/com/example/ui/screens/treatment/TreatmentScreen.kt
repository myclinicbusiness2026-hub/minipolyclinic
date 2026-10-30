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
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentScreen(
  viewModel: ClinicViewModel
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

  val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  // Find all due sessions for today across active cards
  val todayDueSessions = remember(treatmentCards, todayIso) {
    val list = mutableListOf<Pair<TreatmentCard, TreatmentSession>>()
    treatmentCards.filter { !it.isDiscontinued && !it.isFullyCompleted }.forEach { card ->
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

  val activeCardsCount = treatmentCards.count { !it.isDiscontinued && !it.isFullyCompleted }
  val completedCardsCount = treatmentCards.count { it.isFullyCompleted }

  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val isMobilePortrait = screenWidthDp < 600

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgLightGray)
      .padding(8.dp)
  ) {
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
          Button(
            onClick = { showCreateCardDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(34.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ကုသမှုကတ်အသစ် (New Card)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

          Row(verticalAlignment = Alignment.CenterVertically) {
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
        label = { Text("ကတ်မှတ်တမ်း အားလုံး (${treatmentCards.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
      modifier = Modifier.fillMaxWidth()
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("လူနာအမည်၊ HN သို့မဟုတ် ဆေးအမည်ဖြင့် ရှာဖွေပါ...", fontSize = 11.sp, color = SlateMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent
        )
      )
    }

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
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredTodayDueSessions, key = { it.second.id }) { (card, session) ->
              val patient = patients.find { it.id == card.patientId }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(34.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .background(Color(0xFFEDE7F6)),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          "#${session.sessionNumber}",
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF5E35B1),
                          fontSize = 13.sp
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(card.patientName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateDark)
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
                                    Toast.makeText(context, "Cannot dial: ${e.message}", Toast.LENGTH_SHORT).show()
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
                          "${card.patientHn} • Dose ${session.sessionNumber} of ${card.totalSessions} • Time: ${session.scheduledTime}",
                          fontSize = 10.5.sp,
                          color = SlateMuted
                        )
                      }
                    }

                    // Administer Button (Landscape / Tablet)
                    if (!isMobilePortrait) {
                      Button(
                        onClick = { sessionToAdminister = Pair(card, session) },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ဆေးထိုးပြီး (Administer)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }

                  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        "${card.medicationName} (${card.dosage}) • ${card.route.label}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                      )
                      if (card.instructions.isNotBlank()) {
                        Text("Instructions: ${card.instructions}", fontSize = 10.sp, color = SlateMedium)
                      }
                    }
                    Text(
                      "ခန့်မှန်း: ${currencyFmt.format(card.medicationFee + card.sessionFee)} Ks",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = SlateDark
                    )
                  }

                  // Administer Button (Mobile Portrait - Full Width touch target)
                  if (isMobilePortrait) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                      onClick = { sessionToAdminister = Pair(card, session) },
                      colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                    ) {
                      Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("ဆေးထိုးမည် (Administer)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
        val displayCards = remember(treatmentCards, selectedTab, searchQuery) {
          treatmentCards.filter { card ->
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
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(displayCards, key = { it.id }) { card ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  // Top Line: Patient Name & Card Status
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(card.patientName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                        Spacer(modifier = Modifier.width(6.dp))
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
                              TreatmentStatus.COMPLETED -> "ပြီးစီး (Completed)"
                              TreatmentStatus.DISCONTINUED -> "ရပ်ဆိုင်း (Discontinued)"
                              else -> "ကုသဆဲ (${card.completedSessionsCount}/${card.totalSessions})"
                            },
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (card.cardStatus) {
                              TreatmentStatus.COMPLETED -> StatusSuccess
                              TreatmentStatus.DISCONTINUED -> StatusCritical
                              else -> Color(0xFF5E35B1)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                      Text(
                        "${card.patientHn} • စတင်ရက်: ${card.prescribedDate} • ဒေါက်တာ: ${card.doctorName}",
                        fontSize = 10.5.sp,
                        color = SlateMuted
                      )
                    }

                    // Card Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = { cardToPrint = card },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(Icons.Default.Print, contentDescription = "Print Slip", tint = SlateMedium, modifier = Modifier.size(16.dp))
                      }
                      if (!card.isDiscontinued && !card.isFullyCompleted) {
                        IconButton(
                          onClick = { cardToDiscontinue = card },
                          modifier = Modifier.size(28.dp)
                        ) {
                          Icon(Icons.Default.Cancel, contentDescription = "Discontinue", tint = StatusCritical, modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  // Medication info & Route
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoftGreenContainer,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(
                          "Rx: ${card.medicationName} (${card.dosage})",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          color = MedicalGreenDark
                        )
                        Text(
                          "Route: ${card.route.label} • ကြိမ်နှုန်း: ${card.frequency} (${card.totalDays} ရက်) • ဆေးထိုးခ: ${currencyFmt.format(card.sessionFee)} Ks/dose",
                          fontSize = 10.sp,
                          color = SlateMedium
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Visual Progress Grid / Session Badges (Scrollable to prevent squishing)
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
                          .widthIn(min = 36.dp)
                          .height(26.dp),
                        border = BorderStroke(1.dp, if (isDone) MedicalGreenDark else SlateLight)
                      ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                          Text(
                            "D${s.sessionNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) PureWhite else if (card.isDiscontinued) SlateMedium else Color(0xFF5E35B1)
                          )
                        }
                      }
                    }
                  }

                  if (card.isDiscontinued && card.discontinueReason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      "ရပ်ဆိုင်းရသည့် အကြောင်းရင်း: ${card.discontinueReason} (${card.discontinuedBy})",
                      fontSize = 10.sp,
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

  // --- MODALS ---

  // 1. Administer Session Dialog (Dose confirm with Consumables Picker)
  sessionToAdminister?.let { (card, session) ->
    AdministerSessionModal(
      card = card,
      session = session,
      medicines = medicines,
      defaultNurseName = currentUser?.fullName ?: "Nurse Station",
      onDismiss = { sessionToAdminister = null },
      onConfirm = { isClinicMed, medPrice, sessionFee, consumables, notes ->
        val generatedInv = viewModel.administerTreatmentSession(
          cardId = card.id,
          sessionId = session.id,
          nurseName = currentUser?.fullName ?: "Nurse Staff",
          isClinicMed = isClinicMed,
          medPrice = medPrice,
          sessionFee = sessionFee,
          consumables = consumables,
          notes = notes
        )
        if (generatedInv == null) {
          Toast.makeText(context, "Dose ${session.sessionNumber} ဆေးထိုးပြီးစီးပါပြီ (Consultation ဘောင်ချာတွင် ပေါင်းထည့်ပြီးဖြစ်၍ ဘောင်ချာအသစ် မထုတ်တော့ပါ)", Toast.LENGTH_LONG).show()
        } else {
          Toast.makeText(context, "Dose ${session.sessionNumber} ဆေးထိုးပြီးစီးပါပြီ (ဘောင်ချာ #${generatedInv.invoiceNo} ထုတ်ပေးပြီး)", Toast.LENGTH_SHORT).show()
        }
        sessionToAdminister = null
      }
    )
  }

  // 2. Discontinue Card Dialog
  cardToDiscontinue?.let { card ->
    DiscontinueCardModal(
      card = card,
      onDismiss = { cardToDiscontinue = null },
      onConfirm = { reason ->
        viewModel.discontinueTreatmentCard(
          cardId = card.id,
          reason = reason,
          staffName = currentUser?.fullName ?: "Staff"
        )
        Toast.makeText(context, "ကုသမှုကတ် ရပ်ဆိုင်းလိုက်ပါပြီ", Toast.LENGTH_SHORT).show()
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

  // 4. Print Treatment Card Slip Modal
  cardToPrint?.let { card ->
    TreatmentCardSlipDialog(
      card = card,
      clinicSettings = clinicSettings,
      onDismiss = { cardToPrint = null }
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
  onConfirm: (isClinicMed: Boolean, medPrice: Double, sessionFee: Double, consumables: List<TreatmentConsumable>, notes: String) -> Unit
) {
  val isPrepaid = session.isPrepaid || card.isPrepaidFullCourse
  var isClinicMed by remember { mutableStateOf(!isPrepaid && session.isClinicMed && session.medicationFee > 0) }
  var medPriceText by remember { mutableStateOf((if (isPrepaid) 0.0 else (if (session.medicationFee > 0) session.medicationFee else card.medicationFee)).toInt().toString()) }
  var sessionFeeText by remember { mutableStateOf((if (isPrepaid) 0.0 else card.sessionFee).toInt().toString()) }
  var notes by remember { mutableStateOf(session.notes) }
  val selectedConsumables = remember { mutableStateListOf<TreatmentConsumable>() }
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
  val consumablesTotal = selectedConsumables.sumOf { it.totalPrice }
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
            onConfirm(isClinicMed, medPrice, sessionFee, selectedConsumables.toList(), notes)
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
  onConfirm: (reason: String) -> Unit
) {
  var selectedReason by remember { mutableStateOf("ဆရာဝန်မှ ဆေးပြောင်းလဲကုသခြင်း (Treatment changed by Doctor)") }
  var customReason by remember { mutableStateOf("") }

  val reasons = listOf(
    "ဆရာဝန်မှ ဆေးပြောင်းလဲကုသခြင်း (Treatment changed by Doctor)",
    "ဆေးမတည့်သည့် လက္ခဏာပြသဖြင့် ရပ်ဆိုင်းခြင်း (Allergic / Adverse Reaction)",
    "လူနာ ရက်ချိန်း ဆက်မလာတော့ခြင်း (Patient defaulted / Did not show up)",
    "ရောဂါသက်သာသွားသဖြင့် စောစီးစွာ ရပ်ဆိုင်းခြင်း (Condition improved early)",
    "ဆေးညွှန်း အမှားပါသဖြင့် ပယ်ဖျက်ခြင်း (Order entered by mistake)"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(28.dp)) },
    title = { Text("ကုသမှု ရပ်ဆိုင်းမည် (Void / Discontinue)", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Text("လူနာ: ${card.patientName} (${card.medicationName})", fontSize = 11.5.sp, color = SlateDark)
        Text("ပြီးစီးခဲ့သော အကြိမ်ရေ: ${card.completedSessionsCount} of ${card.totalSessions}", fontSize = 10.5.sp, color = SlateMuted)
        Spacer(modifier = Modifier.height(8.dp))
        Text("ရပ်ဆိုင်းရသည့် အကြောင်းရင်း ရွေးချယ်ပါ:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
      Button(
        onClick = { onConfirm(if (customReason.isNotBlank()) customReason else selectedReason) },
        colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
      ) {
        Text("ရပ်ဆိုင်းမည် (Discontinue)", fontSize = 11.sp)
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
  var medicationName by remember { mutableStateOf("Inj. Ceftriaxone 1g") }
  var dosage by remember { mutableStateOf("1 g") }
  var selectedRoute by remember { mutableStateOf(TreatmentRoute.IV_PUSH) }
  var frequency by remember { mutableStateOf("12 hrly") }
  var totalDaysText by remember { mutableStateOf("3") }
  var medFeeText by remember { mutableStateOf("4500") }
  var sessionFeeText by remember(selectedRoute) { mutableStateOf(selectedRoute.getDefaultFee(clinicSettings).toInt().toString()) }
  var instructions by remember { mutableStateOf("Slow IV push over 3-5 mins.") }
  var storeSearchQuery by remember { mutableStateOf("") }
  var splitVialMode by remember { mutableStateOf(false) }
  var isPayPerVisit by remember { mutableStateOf(true) }

  val filteredStoreMeds = remember(medicines, storeSearchQuery) {
    if (storeSearchQuery.isBlank()) emptyList()
    else medicines.filter {
      it.name.contains(storeSearchQuery, ignoreCase = true) ||
      it.genericName.contains(storeSearchQuery, ignoreCase = true)
    }.take(5)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.9f)
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
          verticalArrangement = Arrangement.spacedBy(8.dp)
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
          } else {
            Text("ရှာဖွေမှုနှင့် ကိုက်ညီသော လူနာမတွေ့ပါ", fontSize = 9.5.sp, color = SlateLight)
          }

          // Store Inventory Search Box
          OutlinedTextField(
            value = storeSearchQuery,
            onValueChange = { storeSearchQuery = it },
            placeholder = { Text("Store ဆေးစာရင်းမှ ရှာဖွေရွေးချယ်ရန် (Search Pharmacy Store)...", fontSize = 11.sp) },
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
                        medFeeText = med.sellingPrice.toInt().toString()
                        if (med.standardDosage.isNotBlank()) {
                          dosage = med.standardDosage
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

          // Quick Presets
          Text("အသုံးများသော Preset များ (Routine & Injections):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ElevatedAssistChip(
              onClick = {
                medicationName = "Inj. Ceftriaxone 1g"
                dosage = "1 g"
                selectedRoute = TreatmentRoute.IV_PUSH
                frequency = "12 hrly"
                totalDaysText = "3"
                medFeeText = "4500"
                sessionFeeText = "3000"
                instructions = "Slow IV push over 3-5 mins."
                isPayPerVisit = true
              },
              label = { Text("Inj. Ceftriaxone 12h", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Daily Dressing (အနာဆေးထည့်ခြင်း)"
                dosage = "1 time"
                selectedRoute = TreatmentRoute.DRESSING
                frequency = "Daily"
                totalDaysText = "3"
                medFeeText = "0"
                sessionFeeText = "3000"
                instructions = "Aseptic normal saline & betadine dressing."
                isPayPerVisit = true
              },
              label = { Text("🩹 Wound Dressing", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Nebulizer (Salbutamol 2.5mg + NS)"
                dosage = "1 nebule"
                selectedRoute = TreatmentRoute.NEBULIZATION
                frequency = "BD"
                totalDaysText = "2"
                medFeeText = "1500"
                sessionFeeText = "2000"
                instructions = "Nebulize with Oxygen 6L/min."
                isPayPerVisit = true
              },
              label = { Text("🫁 Nebulizer", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "5% Dextrose Saline 500ml + Vitamins"
                dosage = "1 Bottle"
                selectedRoute = TreatmentRoute.IV_INFUSION
                frequency = "Daily"
                totalDaysText = "1"
                medFeeText = "12000"
                sessionFeeText = "8000"
                instructions = "IV Drip 30-40 drops/min."
                isPayPerVisit = true
              },
              label = { Text("💧 IV Drip 500ml", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Depo-Provera (Medroxyprogesterone)"
                dosage = "150mg/1ml"
                selectedRoute = TreatmentRoute.IM
                frequency = "Stat"
                totalDaysText = "1"
                medFeeText = "7000"
                sessionFeeText = "2500"
                instructions = "Deep IM injection."
                isPayPerVisit = true
              },
              label = { Text("💉 Depo-Provera", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Suture Removal (ချုပ်ရိုးဖြည်ခြင်း)"
                dosage = "1 procedure"
                selectedRoute = TreatmentRoute.DRESSING
                frequency = "Stat"
                totalDaysText = "1"
                medFeeText = "0"
                sessionFeeText = "3000"
                instructions = "Stitch off and wound cleansing."
                isPayPerVisit = true
              },
              label = { Text("✂️ Stitch Off", fontSize = 10.sp) }
            )
          }
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
              value = frequency,
              onValueChange = { frequency = it },
              label = { Text("ကြိမ်နှုန်း (Frequency)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = totalDaysText,
              onValueChange = { totalDaysText = it },
              label = { Text("ရက်ပေါင်း (Days)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(0.7f)
            )
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

          // Split-Vial / Pediatric option
          Text("ဆေးပုလင်း တွက်ချက်မှု (Billing Mode):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = !splitVialMode,
              onClick = { splitVialMode = false },
              label = { Text("Dose တိုင်း ဆေးဖိုးတွက်မည် (Default)", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
            )
            FilterChip(
              selected = splitVialMode,
              onClick = { splitVialMode = true },
              label = { Text("ကလေး/ခွဲထိုး (၁ ရက် ၁ လုံးသာ)", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0284C7), selectedLabelColor = PureWhite)
            )
          }

          // Payment Option (Pay per Visit vs Full Course Upfront)
          Text("ငွေပေးချေမှုပုံစံ (Payment Option):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = isPayPerVisit,
              onClick = { isPayPerVisit = true },
              label = { Text("တစ်ကြိမ်ချင်းရှင်းမည် (Pay per Visit - Default)", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MedicalGreen, selectedLabelColor = PureWhite)
            )
            FilterChip(
              selected = !isPayPerVisit,
              onClick = { isPayPerVisit = false },
              label = { Text("တစ်ခေါက်တည်း အကြေရှင်းမည် (Full Course Upfront)", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF7C3AED), selectedLabelColor = PureWhite)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = medFeeText,
              onValueChange = { medFeeText = it },
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

          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("ညွှန်ကြားချက် (Instructions)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = {
            val days = totalDaysText.toIntOrNull() ?: 3
            val medFee = medFeeText.toDoubleOrNull() ?: 4500.0
            val sessionFee = sessionFeeText.toDoubleOrNull() ?: 3000.0
            val sessions = TreatmentCardFactory.generateSessions(
              startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
              frequency = frequency,
              days = days,
              medicationFee = medFee,
              sessionFee = sessionFee,
              splitVialMode = splitVialMode
            )
            val newCard = TreatmentCard(
              patientId = selectedPatient?.id ?: "",
              patientName = selectedPatient?.name ?: "Unknown Patient",
              patientHn = selectedPatient?.hn ?: "HN-1000",
              doctorName = defaultDoctor,
              medicationName = medicationName,
              dosage = dosage,
              route = selectedRoute,
              frequency = frequency,
              totalDays = days,
              totalSessions = sessions.size,
              sessions = sessions.mapIndexed { idx, s ->
                if (!isPayPerVisit) s.copy(isPrepaid = true) else s
              },
              sessionFee = sessionFee,
              medicationFee = medFee,
              instructions = instructions,
              isPayPerVisit = isPayPerVisit,
              isPrepaidFullCourse = !isPayPerVisit
            )
            onCreate(newCard)
          },
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
 * Printable slip dialog for patient treatment card
 */
@Composable
fun TreatmentCardSlipDialog(
  card: TreatmentCard,
  clinicSettings: ClinicSettings = ClinicSettings(),
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Receipt, contentDescription = null, tint = MedicalGreen)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Treatment Card Slip", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFAFAFA), shape = RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        Text("SHWE KHIT GENERAL CLINIC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Text("DAY-CARE TREATMENT CARD", fontSize = 9.sp, color = MedicalGreenDark)
        Text("-----------------------------------------", fontSize = 9.sp, color = SlateMuted)
        Text("Patient: ${card.patientName} (${card.patientHn})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Doctor : ${card.doctorName}", fontSize = 9.5.sp, color = SlateMedium)
        Text("Rx     : ${card.medicationName} (${card.dosage})", fontSize = 9.5.sp, color = SlateDark, fontWeight = FontWeight.Bold)
        Text("Route  : ${card.route.label} • ${card.frequency}", fontSize = 9.sp, color = SlateMedium)
        Text("-----------------------------------------", fontSize = 9.sp, color = SlateMuted)
        card.sessions.forEach { s ->
          val statusText = if (s.status == TreatmentStatus.COMPLETED) "[✓] ${s.administeredAt ?: "Done"} (${s.administeredBy})" else "[ ] ${s.scheduledDate} ${s.scheduledTime}"
          Text("Dose ${s.sessionNumber}: $statusText", fontSize = 9.sp, color = if (s.status == TreatmentStatus.COMPLETED) StatusSuccess else SlateDark)
        }
        Text("-----------------------------------------", fontSize = 9.sp, color = SlateMuted)
        Text("* ဆေးထိုးရန်လာတိုင်း ဤကတ်ပြား ယူဆောင်လာပါ *", fontSize = 8.5.sp, color = SlateMuted)
      }
    },
    confirmButton = {
      Button(
        onClick = {
          com.example.service.printing.triggerSystemPrintTreatmentSlip(
            context = context,
            card = card,
            clinicSettings = clinicSettings,
            is80mm = clinicSettings.thermalPaperWidthMm == 80
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
      ) {
        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Print Slip", fontSize = 11.sp)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("ပိတ်မည်", fontSize = 11.sp)
      }
    }
  )
}
