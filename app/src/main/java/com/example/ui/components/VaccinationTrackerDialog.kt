package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Patient
import com.example.model.VaccineRecord
import com.example.model.VaccineRegimesData
import com.example.model.VaccineRegime
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Vaccination & Immunization Tracker Dialog (ကာကွယ်ဆေးထိုးနှံမှု မှတ်တမ်း).
 * Manages routine, pediatric, and travel vaccinations, batch numbers, and due date tracking.
 */
@Composable
fun VaccinationTrackerDialog(
  patient: Patient,
  vaccines: List<VaccineRecord>,
  onDismiss: () -> Unit,
  onAddVaccine: (VaccineRecord) -> Unit,
  onUpdateVaccine: (VaccineRecord) -> Unit = {},
  onDeleteVaccine: (String) -> Unit
) {
  var showAddModal by remember { mutableStateOf(false) }
  var editingRecord by remember { mutableStateOf<VaccineRecord?>(null) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f),
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // --- HEADER ---
        Surface(
          color = Color(0xFF1E293B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = SkyBluePrimary,
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Vaccines, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    "Immunization & Vaccine Record",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF334155)
                  ) {
                    Text(
                      patient.hn,
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
                Text(
                  "${patient.name} (${patient.age}y / ${patient.gender.name}) • ကာကွယ်ဆေးထိုး မှတ်တမ်း",
                  color = Color(0xFFCBD5E1),
                  fontSize = 10.sp
                )
              }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
            }
          }
        }

        // --- SUBHEADER ACTION BAR ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "Doses Administered: ",
              fontSize = 11.sp,
              color = SlateDark,
              fontWeight = FontWeight.Medium
            )
            Text(
              "${vaccines.size}",
              fontSize = 12.sp,
              color = OceanGreenPrimary,
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            onClick = { showAddModal = true },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Record Vaccination", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
          }
        }

        // --- VACCINES LIST ---
        if (vaccines.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = SlateMuted,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "No immunization records for this patient yet.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMedium
              )
              Text(
                "Tap '+ Record Vaccination' to log Hepatitis B, Flu, Tetanus, or other doses.",
                fontSize = 10.5.sp,
                color = SlateMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            itemsIndexed(vaccines) { index, v ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { editingRecord = v },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Surface(
                        shape = CircleShape,
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.size(24.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SkyBlueDark)
                        }
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(v.vaccineName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
                          Spacer(modifier = Modifier.width(6.dp))
                          Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFEDE9FE)
                          ) {
                            Text(
                              v.doseNumber,
                              fontSize = 8.5.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color(0xFF6D28D9),
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                          }
                          if (!v.regimeName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                              shape = RoundedCornerShape(3.dp),
                              color = Color(0xFFDCFCE7),
                              border = BorderStroke(0.5.dp, Color(0xFF86EFAC))
                            ) {
                              Text(
                                v.regimeName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                              )
                            }
                          }
                        }
                        if (v.batchLotNumber.isNotBlank()) {
                          Text("Lot/Batch: ${v.batchLotNumber}", fontSize = 9.sp, color = SlateMedium)
                        }
                      }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = { editingRecord = v },
                        modifier = Modifier.size(26.dp)
                      ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SkyBluePrimary, modifier = Modifier.size(15.dp))
                      }
                      IconButton(
                        onClick = { onDeleteVaccine(v.id) },
                        modifier = Modifier.size(26.dp)
                      ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(15.dp))
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))
                  HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                  Spacer(modifier = Modifier.height(4.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Event, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Given: ${v.dateAdministered}", fontSize = 9.5.sp, color = DarkGrayText, fontWeight = FontWeight.Medium)
                    }

                    if (!v.nextDueDate.isNullOrBlank()) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(11.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text("Next Due: ${v.nextDueDate}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                      }
                    } else {
                      Text("Course Completed", fontSize = 9.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                    }
                  }

                  if (v.remarks.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Note: ${v.remarks}", fontSize = 9.sp, color = SlateMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                  }
                }
              }
            }
          }
        }

        // --- FOOTER BUTTON ---
        Surface(
          color = PureWhite,
          tonalElevation = 3.dp,
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
              modifier = Modifier.height(34.dp)
            ) {
              Text("Done (ပြီးပါပြီ)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  editingRecord?.let { rec ->
    AddOrEditVaccineRecordModal(
      patient = patient,
      recordToEdit = rec,
      onDismiss = { editingRecord = null },
      onSave = { updatedRec ->
        onUpdateVaccine(updatedRec)
        editingRecord = null
      }
    )
  }

  if (showAddModal) {
    AddOrEditVaccineRecordModal(
      patient = patient,
      recordToEdit = null,
      onDismiss = { showAddModal = false },
      onSave = { newRec ->
        onAddVaccine(newRec)
        showAddModal = false
      }
    )
  }
}

// --- MODAL TO ADD OR EDIT A VACCINE RECORD ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddOrEditVaccineRecordModal(
  patient: Patient,
  recordToEdit: VaccineRecord? = null,
  onDismiss: () -> Unit,
  onSave: (VaccineRecord) -> Unit
) {
  val isEditing = recordToEdit != null
  val today = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

  var vaccineName by remember { mutableStateOf(recordToEdit?.vaccineName ?: "Hepatitis B Vaccine (အသည်းရောင် အသားဝါ ဘီ)") }
  var regimeName by remember { mutableStateOf(recordToEdit?.regimeName ?: "") }
  var doseNumber by remember { mutableStateOf(recordToEdit?.doseNumber ?: "Dose 1 (Day 0)") }
  var dateAdministered by remember { mutableStateOf(recordToEdit?.dateAdministered ?: today) }
  var batchNumber by remember { mutableStateOf(recordToEdit?.batchLotNumber ?: "HB-2026-09A") }
  var nextDueDate by remember { mutableStateOf(recordToEdit?.nextDueDate ?: "") }
  var remarks by remember { mutableStateOf(recordToEdit?.remarks ?: "Deltoid IM injection 0.5ml. Well tolerated.") }

  // Check matching regime
  val activeRegime = remember(regimeName) {
    VaccineRegimesData.ALL_REGIMES.find { it.regimeName == regimeName }
  }

  val doseOptions = remember(activeRegime) {
    if (activeRegime != null) {
      activeRegime.doses.map { it.doseLabel } + listOf("Custom")
    } else {
      listOf("Dose 1", "Dose 2", "Dose 3", "Booster", "Annual Dose", "0.5ml IM", "0.25ml (Pediatric)", "Custom")
    }
  }

  val nextDuePresets = listOf(
    "Day 3 (၃ ရက်)" to 3,
    "Day 7 (၇ ရက်)" to 7,
    "Day 21 (၂၁ ရက်)" to 21,
    "Day 28 (၂၈ ရက်)" to 28,
    "1 Month (၁ လ)" to 30,
    "2 Months (၂ လ)" to 60,
    "6 Months (၆ လ)" to 180,
    "1 Year (၁ နှစ်)" to 365,
    "5 Years (၅ နှစ်)" to 1825
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.92f)
        .padding(6.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          if (isEditing) "Edit Immunization / Vaccination Record (ကာကွယ်ဆေး ပြင်ဆင်ရန်)"
          else "Record New Immunization / Vaccination (ကာကွယ်ဆေး အသစ်ထည့်ရန်)",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = DarkGrayText
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- REGIME SELECTION HEADER ---
        Text(
          "Vaccination Regime (ကာကွယ်ဆေး ထိုးနည်း စနစ်ရွေးချယ်ရန်)",
          fontSize = 10.sp,
          color = OceanGreenPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          "Select clinical protocol to auto-configure doses and follow-up dates (e.g. Rabies Day 0,3,7,21 or Hepatitis B)",
          fontSize = 8.5.sp,
          color = SlateMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          VaccineRegimesData.ALL_REGIMES.forEach { regime ->
            val isRegimeSelected = regimeName == regime.regimeName
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isRegimeSelected) OceanGreenPrimary else Color(0xFFF1F5F9),
              border = BorderStroke(0.6.dp, if (isRegimeSelected) OceanGreenPrimary else Color(0xFFCBD5E1)),
              modifier = Modifier.clickable {
                if (isRegimeSelected) {
                  regimeName = ""
                } else {
                  regimeName = regime.regimeName
                  vaccineName = regime.vaccineName
                  // Auto-select Dose 1 and calculate follow up for Dose 2
                  val firstDose = regime.doses.firstOrNull()
                  if (firstDose != null) {
                    doseNumber = firstDose.doseLabel
                    remarks = firstDose.defaultDosageRoute
                  }
                  if (regime.doses.size > 1) {
                    val nextDose = regime.doses[1]
                    val daysDiff = nextDose.dayOffsetFromStart - (firstDose?.dayOffsetFromStart ?: 0)
                    val calc = com.example.util.BurmeseDateUtils.calculateDateFromBase(dateAdministered, daysDiff)
                    nextDueDate = "${nextDose.doseLabel} • ${calc.fullDisplayBurmese}"
                  }
                }
              }
            ) {
              Text(
                regime.regimeName,
                fontSize = 8.5.sp,
                fontWeight = if (isRegimeSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isRegimeSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = vaccineName,
          onValueChange = { vaccineName = it },
          label = { Text("Vaccine Name (ကာကွယ်ဆေး အမည်) *", fontSize = 10.sp) },
          placeholder = { Text("e.g. Hepatitis B, Influenza, TT, Rabies", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dose Selector
        Text("Dose Number / Stage (ထိုးမည့် အကြိမ်အရေအတွက်)", fontSize = 10.sp, color = SlateMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          doseOptions.forEach { opt ->
            val isSelected = doseNumber == opt
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isSelected) OceanGreenPrimary else Color(0xFFF1F5F9),
              modifier = Modifier.clickable {
                doseNumber = if (opt == "Custom") "" else opt

                // Auto-calculate next due date if regime active
                if (activeRegime != null && opt != "Custom") {
                  val currentIndex = activeRegime.doses.indexOfFirst { it.doseLabel == opt }
                  if (currentIndex != -1 && currentIndex < activeRegime.doses.size - 1) {
                    val currentDose = activeRegime.doses[currentIndex]
                    val nextDose = activeRegime.doses[currentIndex + 1]
                    val daysDiff = nextDose.dayOffsetFromStart - currentDose.dayOffsetFromStart
                    val calc = com.example.util.BurmeseDateUtils.calculateDateFromBase(dateAdministered, daysDiff)
                    nextDueDate = "${nextDose.doseLabel} • ${calc.fullDisplayBurmese}"
                    remarks = currentDose.defaultDosageRoute
                  } else if (currentIndex == activeRegime.doses.size - 1) {
                    nextDueDate = "Full Course Completed (ကာကွယ်ဆေး အကြိမ်ပြည့်ပြီး)"
                  }
                }
              }
            ) {
              Text(
                opt,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = doseNumber,
            onValueChange = { doseNumber = it },
            label = { Text("Dose / ပမာဏ *", fontSize = 10.sp) },
            placeholder = { Text("e.g. Dose 1 (Day 0), 0.5ml IM", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
          OutlinedTextField(
            value = dateAdministered,
            onValueChange = { newDate ->
              dateAdministered = newDate
              // Recalculate next due date if active regime
              if (activeRegime != null) {
                val currentIndex = activeRegime.doses.indexOfFirst { it.doseLabel == doseNumber }
                if (currentIndex != -1 && currentIndex < activeRegime.doses.size - 1) {
                  val currentDose = activeRegime.doses[currentIndex]
                  val nextDose = activeRegime.doses[currentIndex + 1]
                  val daysDiff = nextDose.dayOffsetFromStart - currentDose.dayOffsetFromStart
                  val calc = com.example.util.BurmeseDateUtils.calculateDateFromBase(newDate, daysDiff)
                  nextDueDate = "${nextDose.doseLabel} • ${calc.fullDisplayBurmese}"
                }
              }
            },
            label = { Text("Date Given (ရက်စွဲ)", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Next Due Schedule Presets
        Text("Quick Next Due Presets (ရက်ချိန်း အမြန်တွက်ချက်ရန်)", fontSize = 10.sp, color = SlateMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          nextDuePresets.forEach { (label, days) ->
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFF1F5F9),
              border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
              modifier = Modifier.clickable {
                val calc = com.example.util.BurmeseDateUtils.calculateDateFromBase(dateAdministered, days)
                nextDueDate = calc.fullDisplayBurmese
              }
            ) {
              Text(
                label,
                fontSize = 8.5.sp,
                color = OceanGreenPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = batchNumber,
            onValueChange = { batchNumber = it },
            label = { Text("Batch / Lot #", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          )
          OutlinedTextField(
            value = nextDueDate,
            onValueChange = { nextDueDate = it },
            label = { Text("Next Due Schedule (မြန်မာလို)", fontSize = 10.sp) },
            placeholder = { Text("e.g. Dose 2 • ၃ ရက်အကြာ (သောကြာနေ့)", fontSize = 9.sp) },
            modifier = Modifier.weight(1.4f),
            shape = RoundedCornerShape(6.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Remarks & Route (e.g. Deltoid IM 0.5ml)", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel", color = SlateMedium, fontSize = 11.sp) }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              if (recordToEdit != null) {
                onSave(
                  recordToEdit.copy(
                    vaccineName = vaccineName.ifBlank { "Vaccine" },
                    doseNumber = doseNumber.ifBlank { "Dose 1" },
                    dateAdministered = dateAdministered.ifBlank { today },
                    nextDueDate = nextDueDate.ifBlank { null },
                    batchLotNumber = batchNumber,
                    remarks = remarks,
                    regimeName = regimeName
                  )
                )
              } else {
                onSave(
                  VaccineRecord(
                    patientId = patient.id,
                    patientName = patient.name,
                    vaccineName = vaccineName.ifBlank { "Vaccine" },
                    doseNumber = doseNumber.ifBlank { "Dose 1" },
                    dateAdministered = dateAdministered.ifBlank { today },
                    nextDueDate = nextDueDate.ifBlank { null },
                    batchLotNumber = batchNumber,
                    remarks = remarks,
                    regimeName = regimeName
                  )
                )
              }
            },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
          ) {
            Text(
              if (isEditing) "Save Changes (ပြင်ဆင်မှု သိမ်းရန်)" else "Save Vaccination (သိမ်းဆည်းရန်)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
