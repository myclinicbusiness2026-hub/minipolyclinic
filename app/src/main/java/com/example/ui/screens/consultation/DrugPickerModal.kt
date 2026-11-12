package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Medicine
import com.example.model.Patient
import com.example.model.PrescribedDrug
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrugPickerModal(
  patient: Patient,
  medicines: List<Medicine>,
  onDismiss: () -> Unit,
  onDrugAdded: (PrescribedDrug) -> Unit
) {
  var activeTab by remember { mutableIntStateOf(0) } // 0 = Clinic Stock, 1 = Non-Store External Rx

  // State for Clinic Stock Tab
  val currencyFmt = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.US) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedMedicine by remember { mutableStateOf<Medicine?>(medicines.firstOrNull()) }
  var dosage by remember { mutableStateOf(medicines.firstOrNull()?.standardDosage ?: "500 mg") }
  var dosePerTime by remember { mutableStateOf("1") } // "1/2", "1", "1.5", "2", or custom
  var frequency by remember { mutableStateOf("1-0-1") }
  var customFrequencyText by remember { mutableStateOf("") }
  var isCustomFreq by remember { mutableStateOf(false) }
  var durationNum by remember { mutableIntStateOf(5) }
  var durationUnit by remember { mutableStateOf("Days") }
  var duration by remember { mutableStateOf("5 Days") }
  var quantityText by remember { mutableStateOf("10") }
  var instructions by remember { mutableStateOf("") }

  // State for Non-Store External Rx Tab
  var extDrugName by remember { mutableStateOf("") }
  var extDosage by remember { mutableStateOf("1 Tab") }
  var extDosePerTime by remember { mutableStateOf("1") }
  var extFrequency by remember { mutableStateOf("1-0-1") }
  var extCustomFrequencyText by remember { mutableStateOf("") }
  var isExtCustomFreq by remember { mutableStateOf(false) }
  var extDurationNum by remember { mutableIntStateOf(5) }
  var extDurationUnit by remember { mutableStateOf("Days") }
  var extDuration by remember { mutableStateOf("5 Days") }
  var extQuantityText by remember { mutableStateOf("10") }
  var extInstructions by remember { mutableStateOf("") }
  var extNotes by remember { mutableStateOf("ပြင်ပဆေးဆိုင်တွင် ဝယ်ယူရန်") }

  // Calculation helper to auto-estimate recommended quantity based on frequency and duration
  fun calculateAutoQty(freq: String, durNum: Int, unit: String): Int {
    if (unit == "ပုံမှန်သောက်ရန်" || unit == "Regular") {
      return (durNum * 30).coerceAtLeast(30)
    }
    val days = when (unit) {
      "Weeks" -> durNum * 7
      "Months" -> durNum * 30
      "Stat" -> 1
      else -> durNum
    }
    val dosesPerDay: Double = when {
      freq.contains("1-1-1-1") || freq.contains("2-2-2-2") -> {
        val mult = if (freq.startsWith("2")) 2.0 else if (freq.startsWith("½") || freq.startsWith("1/2")) 0.5 else 1.0
        4.0 * mult
      }
      freq.contains("1-1-1") || freq.contains("2-2-2") -> {
        val mult = if (freq.startsWith("2")) 2.0 else if (freq.startsWith("½") || freq.startsWith("1/2")) 0.5 else 1.0
        3.0 * mult
      }
      freq.contains("1-0-1") || freq.contains("2-0-2") -> {
        val mult = if (freq.startsWith("2")) 2.0 else if (freq.startsWith("½") || freq.startsWith("1/2")) 0.5 else 1.0
        2.0 * mult
      }
      freq.contains("1-0-0") || freq.contains("0-0-1") || freq.contains("0-1-0") || freq.contains("2-0-0") || freq.contains("0-0-2") -> {
        val mult = if (freq.startsWith("2")) 2.0 else if (freq.startsWith("½") || freq.startsWith("1/2")) 0.5 else 1.0
        1.0 * mult
      }
      freq.contains("½-0-½") -> 1.0
      freq.contains("½-0-0") || freq.contains("0-0-½") -> 0.5
      else -> 2.0
    }
    val total = kotlin.math.ceil(dosesPerDay * days).toInt()
    return total.coerceAtLeast(1)
  }

  fun applyDoseToFreq(dose: String, basePattern: String): String {
    val prefix = when (dose) {
      "1/2", "½" -> "½"
      "2" -> "2"
      "1.5", "1½" -> "1½"
      else -> "1"
    }
    return when (basePattern) {
      "1-0-1" -> "$prefix-0-$prefix"
      "1-1-1" -> "$prefix-$prefix-$prefix"
      "1-0-0" -> "$prefix-0-0"
      "0-0-1" -> "0-0-$prefix"
      "1-1-1-1" -> "$prefix-$prefix-$prefix-$prefix"
      else -> basePattern
    }
  }

  val filteredMedicines = medicines.filter {
    (it.effectiveMedicineType in listOf("Oral", "Topical")) &&
    (searchQuery.isBlank() ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.genericName.contains(searchQuery, ignoreCase = true))
  }

  val baseFreqPatterns = listOf(
    "1-0-1",
    "1-1-1",
    "1-0-0",
    "1-1-1-1",
    "0-0-1",
    "လိုအပ်ပါက"
  )

  val dosePresets = listOf(
    Pair("½", "½ Tab (တဝက်)"),
    Pair("1", "1 Tab (၁ လုံး)"),
    Pair("2", "2 Tabs (၂ လုံး)")
  )

  val durationQuickNums = listOf(3, 5, 7, 10, 14, 30)
  val durationUnits = listOf(
    Pair("Days", "ရက် (Days)"),
    Pair("Weeks", "ပတ် (Weeks)"),
    Pair("Months", "လ (Months)"),
    Pair("ပုံမှန်သောက်ရန်", "ပုံမှန်သောက်ရန် (Regular)")
  )
  val quantityQuickNums = listOf(5, 10, 14, 20, 30, 60)

  val instructionPresetPairs = listOf(
    Pair("စားပြီး", "စားပြီး"),
    Pair("မစားမီ", "မစားမီ"),
    Pair("အစာနှင့်တွဲ၍", "အစာနှင့်တွဲ၍"),
    Pair("အိပ်ရာဝင်", "အိပ်ရာဝင်"),
    Pair("ကိုက်ခဲ/ဖျားပါက", "ကိုက်ခဲ/ဖျားပါက"),
    Pair("CST", "သောက်လက်စသောက်ဆေးများ ပုံမှန်အတိုင်းဆက်သောက်ရန်")
  )

  val commonExternalPresets = listOf(
    "CST (သောက်လက်စသောက်ဆေးများ ပုံမှန်အတိုင်းဆက်သောက်ရန်)",
    "Tab. Telmisartan 40mg",
    "Tab. Rosuvastatin 10mg",
    "Inj. Ceftriaxone 1g",
    "Tab. Metformin 500mg",
    "Syr. Ambroxol",
    "Eye Drops Ciprofloxacin",
    "Inhaler Salbutamol",
    "Cap. Omeprazole 20mg"
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp)
        .padding(vertical = 10.dp),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Modal Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Medication, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("ဆေးညွှန်း ထည့်သွင်းခြင်း (Add Medication)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
              Text("In-Clinic Stock & Non-Store External Rx", fontSize = 9.5.sp, color = SlateMuted)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs
        TabRow(
          selectedTabIndex = activeTab,
          containerColor = Color(0xFFF8FAFC),
          contentColor = MedicalGreen
        ) {
          Tab(
            selected = activeTab == 0,
            onClick = { activeTab = 0 },
            text = { Text("၁။ ဆေးခန်းစတိုး (FEFO Stock)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = activeTab == 1,
            onClick = { activeTab = 1 },
            text = { Text("၂။ စတိုးပြင်ပဆေး (External Rx)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTab == 0) {
          // TAB 0: IN-CLINIC STOCK MEDICINES
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search brand name or generic in store...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                }
              }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            colors = clinicSearchBarColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = DarkGrayText)
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Stock Medicine Cards List
          if (filteredMedicines.isEmpty()) {
            Surface(
              color = Color(0xFFFEF3C7),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
              Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  "\"$searchQuery\" သည် ဆေးခန်းစတိုးတွင် မရှိပါ",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF92400E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                  onClick = {
                    extDrugName = searchQuery
                    activeTab = 1
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                  modifier = Modifier.height(32.dp),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("စတိုးပြင်ပဆေးအဖြစ် ရေးပေးမည် (External Rx)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              val selMedId = selectedMedicine?.id
              filteredMedicines.forEach { med ->
                val isSelected = selMedId != null && selMedId == med.id
                val isAllergyConflict = patient.allergies.any {
                  med.name.contains(it, ignoreCase = true) || med.genericName.contains(it, ignoreCase = true)
                }
                val earliestBatch = med.sortedBatches.firstOrNull()

                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      selectedMedicine = med
                      dosage = med.standardDosage
                    },
                  shape = RoundedCornerShape(6.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SoftGreenContainer else if (isAllergyConflict) StatusCriticalContainer.copy(alpha = 0.5f) else BackgroundSurface
                  ),
                  border = if (isSelected) BorderStroke(1.dp, MedicalGreen) else null
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(med.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkGrayText)
                        if (isAllergyConflict) {
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("⚠️ ALLERGY", fontSize = 8.sp, color = StatusCritical, fontWeight = FontWeight.Bold)
                        }
                      }
                      Text("${med.genericName} • ${med.dosageForm} • ${med.standardDosage}", fontSize = 9.sp, color = SlateMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        "${currencyFmt.format(med.calculatedSellingPrice.toInt())} Ks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyBluePrimary
                      )
                      Text(
                        if (med.totalStock <= 0) "Stock: 0 (ပြတ်လပ်)" else "Stock: ${med.totalStock}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (med.totalStock <= 0) StatusCritical else if (med.totalStock < med.minStockThreshold) Color(0xFFD97706) else MedicalGreenDark
                      )
                      Text("Exp: ${earliestBatch?.expiryDate ?: "N/A"}", fontSize = 8.sp, color = SlateLight)
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          selectedMedicine?.let { med ->
            val isAllergyConflict = patient.allergies.any {
              med.name.contains(it, ignoreCase = true) || med.genericName.contains(it, ignoreCase = true)
            }
            if (isAllergyConflict) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(StatusCriticalContainer)
                  .border(1.dp, StatusCritical.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                  .padding(8.dp)
              ) {
                Text(
                  "⚠️ ALLERGY WARNING: Patient is allergic to: ${patient.allergies.joinToString(", ")}",
                  color = StatusCritical,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  softWrap = true
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
            }

            if (med.totalStock <= 0) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFFEF2F2))
                  .border(1.dp, StatusCritical.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                  .padding(8.dp)
              ) {
                Text(
                  "⚠️ စတော့ပြတ်လပ်နေပါသည် (Out of Stock): လက်ရှိ ဆေးခန်းတွင် ဤဆေးဝါးလက်ကျန် (၀) ဖြစ်နေပါသည်။ အပြင်ဆေးဆိုင်မှ ဝယ်ယူရန် ဆေးညွှန်းထုတ်ပေးလိုပါက အပေါ်ရှိ 'Non-Store External Rx' tab ကို အသုံးပြုပါ သို့မဟုတ် စတော့အသစ် ထည့်သွင်းပါ။",
                  color = StatusCritical,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  softWrap = true
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
            }

            // Price & Total Estimation Banner
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF0FDF4),
              border = BorderStroke(0.8.dp, MedicalGreen.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Sell, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    "ဈေးနှုန်း: ${currencyFmt.format(med.calculatedSellingPrice.toInt())} Ks / unit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalGreenDark
                  )
                }
                val curQty = quantityText.toIntOrNull() ?: 1
                Text(
                  "ကျသင့်ငွေ: ${currencyFmt.format((med.calculatedSellingPrice * curQty).toInt())} Ks",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = SkyBluePrimary
                )
              }
            }

            // Dose per time (½, 1, 2)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("တစ်ကြိမ်သောက် (Dose):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              Spacer(modifier = Modifier.width(6.dp))
              dosePresets.forEach { (dVal, dLabel) ->
                val isSel = dosePerTime == dVal
                FilterChip(
                  selected = isSel,
                  onClick = {
                    dosePerTime = dVal
                    isCustomFreq = false
                    val base = when {
                      frequency.contains("-0-") && frequency.endsWith("-0") -> "1-0-0"
                      frequency.startsWith("0-0-") -> "0-0-1"
                      frequency.split("-").size == 4 -> "1-1-1-1"
                      frequency.split("-").size == 3 && !frequency.contains("0") -> "1-1-1"
                      else -> "1-0-1"
                    }
                    frequency = applyDoseToFreq(dVal, base)
                    quantityText = calculateAutoQty(frequency, durationNum, durationUnit).toString()
                  },
                  label = { Text(dLabel, fontSize = 9.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.height(24.dp).padding(end = 4.dp),
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (dVal == "½") Color(0xFFFEF3C7) else SoftGreenContainer,
                    selectedLabelColor = if (dVal == "½") Color(0xFFB45309) else MedicalGreenDark
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Frequency (ကြိမ်ရေ):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              baseFreqPatterns.forEach { basePattern ->
                val calculatedFreq = if (basePattern == "လိုအပ်ပါက") "လိုအပ်ပါက" else applyDoseToFreq(dosePerTime, basePattern)
                val isSel = !isCustomFreq && frequency == calculatedFreq
                FilterChip(
                  selected = isSel,
                  onClick = {
                    isCustomFreq = false
                    frequency = calculatedFreq
                    quantityText = calculateAutoQty(frequency, durationNum, durationUnit).toString()
                  },
                  label = { Text(calculatedFreq, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.height(26.dp)
                )
              }

              // Custom Frequency Chip
              FilterChip(
                selected = isCustomFreq,
                onClick = {
                  isCustomFreq = !isCustomFreq
                  if (isCustomFreq && customFrequencyText.isNotBlank()) {
                    frequency = customFrequencyText
                  }
                },
                label = { Text("+ စိတ်ကြိုက် (Custom)", fontSize = 9.sp, fontWeight = if (isCustomFreq) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(26.dp)
              )
            }

            if (isCustomFreq) {
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = customFrequencyText,
                onValueChange = {
                  customFrequencyText = it
                  frequency = it
                },
                label = { Text("စိတ်ကြိုက် ကြိမ်နှုန်းရိုက်ထည့်ပါ (e.g. 1/2 tab bd, 2 tabs stat)", fontSize = 9.5.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = clinicTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Meal Direction (သောက်သုံးရန်):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              instructionPresetPairs.forEach { (chipLabel, instText) ->
                val isSelected = if (chipLabel == "CST") {
                  instructions.contains("သောက်လက်စသောက်ဆေးများ") || instructions.contains("CST", ignoreCase = true)
                } else {
                  instructions.contains(instText)
                }
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    instructions = if (isSelected) {
                      instructions.replace(instText, "").replace("•", "").trim().trim(',', ' ')
                    } else {
                      if (instructions.isBlank()) instText else "$instructions • $instText"
                    }
                  },
                  label = {
                    Text(
                      if (chipLabel == "CST") "CST (ဆက်သောက်ရန်)" else chipLabel,
                      fontSize = 9.5.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.height(26.dp),
                  colors = if (chipLabel == "CST") FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDBEAFE),
                    selectedLabelColor = Color(0xFF1E40AF),
                    containerColor = Color(0xFFEFF6FF),
                    labelColor = Color(0xFF2563EB)
                  ) else FilterChipDefaults.filterChipColors()
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration Quick Picker Section (Chips for Numbers & Units)
            Text("သောက်သုံးမည့်ကာလ (Duration):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              // Quick Number Chips
              durationQuickNums.forEach { n ->
                val isSel = durationNum == n && durationUnit != "ပုံမှန်သောက်ရန်"
                Surface(
                  modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                      durationNum = n
                      duration = "$n $durationUnit"
                      quantityText = calculateAutoQty(frequency, n, durationUnit).toString()
                    },
                  color = if (isSel) MedicalGreen else BackgroundSurface,
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(0.6.dp, if (isSel) MedicalGreenDark else BorderGrey)
                ) {
                  Text(
                    "$n",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) PureWhite else DarkGrayText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(4.dp))

              // Increment/Decrement controls
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    if (durationNum > 1) {
                      durationNum -= 1
                      duration = "$durationNum $durationUnit"
                      quantityText = calculateAutoQty(frequency, durationNum, durationUnit).toString()
                    }
                  },
                color = BackgroundSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, BorderGrey)
              ) {
                Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }

              Spacer(modifier = Modifier.width(3.dp))

              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    durationNum += 1
                    duration = "$durationNum $durationUnit"
                    quantityText = calculateAutoQty(frequency, durationNum, durationUnit).toString()
                  },
                color = BackgroundSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, BorderGrey)
              ) {
                Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration Units
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              durationUnits.forEach { (uKey, uLabel) ->
                val isSel = durationUnit == uKey
                FilterChip(
                  selected = isSel,
                  onClick = {
                    durationUnit = uKey
                    duration = if (uKey == "ပုံမှန်သောက်ရန်") "ပုံမှန်သောက်ရန်" else "$durationNum $uKey"
                    quantityText = calculateAutoQty(frequency, durationNum, uKey).toString()
                  },
                  label = { Text(uLabel, fontSize = 9.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.height(24.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity Quick Picker Section (Chips for Qty)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("ထုတ်ပေးမည့် အရေအတွက် (Qty):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              Spacer(modifier = Modifier.width(6.dp))
              quantityQuickNums.forEach { q ->
                val isSel = quantityText == q.toString()
                Surface(
                  modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { quantityText = q.toString() },
                  color = if (isSel) Color(0xFF1565C0) else BackgroundSurface,
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(0.6.dp, if (isSel) Color(0xFF0D47A1) else BorderGrey)
                ) {
                  Text(
                    "$q",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) PureWhite else DarkGrayText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                  )
                }
              }

              // Qty +/- buttons
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    val cur = quantityText.toIntOrNull() ?: 10
                    if (cur > 1) quantityText = (cur - 1).toString()
                  },
                color = BackgroundSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, BorderGrey)
              ) {
                Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }

              Spacer(modifier = Modifier.width(3.dp))

              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    val cur = quantityText.toIntOrNull() ?: 10
                    quantityText = (cur + 1).toString()
                  },
                color = BackgroundSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, BorderGrey)
              ) {
                Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Strength / Dose", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = clinicTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
              )
              OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (ရက်)", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = clinicTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
              )
              OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Total Qty (လုံးရေ)", fontSize = 10.sp) },
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = clinicTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
              value = instructions,
              onValueChange = { instructions = it },
              label = { Text("Clinical Instructions (e.g. စားပြီး)", fontSize = 10.sp) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              singleLine = true,
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier
                .weight(1f)
                .height(36.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Cancel", fontSize = 11.sp, maxLines = 1)
            }
            Button(
              onClick = {
                selectedMedicine?.let { med ->
                  val isCst = instructions.contains("သောက်လက်စ") || instructions.contains("CST", ignoreCase = true)
                  val qty = if (isCst) 0 else (quantityText.toIntOrNull() ?: 10)
                  val finalUnitPrice = if (isCst) 0.0 else med.calculatedSellingPrice
                  onDrugAdded(
                    PrescribedDrug(
                      medicineId = if (isCst) "CST-${med.id}" else med.id,
                      medicineName = med.name,
                      name = med.name,
                      genericName = med.genericName,
                      dosage = dosage,
                      frequency = frequency,
                      duration = duration,
                      quantity = qty,
                      unitPrice = finalUnitPrice,
                      instructions = instructions,
                      isExternal = isCst,
                      isCst = isCst
                    )
                  )
                }
              },
              modifier = Modifier
                .weight(1.3f)
                .height(36.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Add to Prescription", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }
        } else {
          // TAB 1: NON-STORE EXTERNAL RX
          Surface(
            color = Color(0xFFFEF3C7),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "ဤဆေးသည် ဆေးခန်းစတိုးတွင် မရှိသောကြောင့် လူနာအား ပြင်ပဆေးဆိုင်တွင် ဝယ်ယူရန် A4 ဆေးမှတ်တမ်းတွင် သီးသန့်ဖော်ပြပေးမည်ဖြစ်ပြီး ဆေးခန်းဘောက်ချာတွင် စတော့မနှုတ်/ငွေမကောက်ပါခင်ဗျာ။",
                fontSize = 9.5.sp,
                color = Color(0xFF78350F)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text("အသုံးများသော ပြင်ပဆေး အကြံပြုချက်များ (Quick Presets):", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            commonExternalPresets.forEach { p ->
              val isCstPreset = p.startsWith("CST")
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isCstPreset) Color(0xFFEFF6FF) else PureWhite,
                border = BorderStroke(0.8.dp, if (isCstPreset) Color(0xFF93C5FD) else BorderGray),
                modifier = Modifier.clickable {
                  if (isCstPreset) {
                    extDrugName = "CST (Continue Same Treatment)"
                    extDosage = "-"
                    extFrequency = "ပုံမှန်အတိုင်း"
                    extDuration = "ပုံမှန်သောက်ရန်"
                    extInstructions = "သောက်လက်စသောက်ဆေးများ ပုံမှန်အတိုင်းဆက်သောက်ရန်"
                    extQuantityText = "1"
                  } else {
                    extDrugName = p
                    val pattern = Regex("\\b\\d+(\\.\\d+)?\\s*(mg|g|mcg|ml|iu|IU|%)\\b", RegexOption.IGNORE_CASE)
                    val match = pattern.find(p)?.value
                    if (match != null) extDosage = match
                  }
                }
              ) {
                Text(
                  if (isCstPreset) "✓ $p" else "+ $p",
                  fontSize = 8.5.sp,
                  fontWeight = if (isCstPreset) FontWeight.Bold else FontWeight.Normal,
                  color = if (isCstPreset) Color(0xFF1D4ED8) else SlateDark,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = extDrugName,
            onValueChange = { extDrugName = it },
            label = { Text("ပြင်ပဆေး အမည်ရင်း (Drug Name & Brand) *", fontSize = 10.sp) },
            placeholder = { Text("e.g. Tab. Telmisartan, Inj. Ceftriaxone 1g, Syr. Augmentin", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText, fontWeight = FontWeight.Bold)
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Dose per time for External Rx
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("တစ်ကြိမ်သောက် (Dose):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.width(6.dp))
            dosePresets.forEach { (dVal, dLabel) ->
              val isSel = extDosePerTime == dVal
              FilterChip(
                selected = isSel,
                onClick = {
                  extDosePerTime = dVal
                  isExtCustomFreq = false
                  val base = when {
                    extFrequency.contains("-0-") && extFrequency.endsWith("-0") -> "1-0-0"
                    extFrequency.startsWith("0-0-") -> "0-0-1"
                    extFrequency.split("-").size == 4 -> "1-1-1-1"
                    extFrequency.split("-").size == 3 && !extFrequency.contains("0") -> "1-1-1"
                    else -> "1-0-1"
                  }
                  extFrequency = applyDoseToFreq(dVal, base)
                  extQuantityText = calculateAutoQty(extFrequency, extDurationNum, extDurationUnit).toString()
                },
                label = { Text(dLabel, fontSize = 9.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(24.dp).padding(end = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = if (dVal == "½") Color(0xFFFEF3C7) else SoftGreenContainer,
                  selectedLabelColor = if (dVal == "½") Color(0xFFB45309) else MedicalGreenDark
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text("Frequency (ကြိမ်ရေ):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            baseFreqPatterns.forEach { basePattern ->
              val calculatedFreq = if (basePattern == "လိုအပ်ပါက") "လိုအပ်ပါက" else applyDoseToFreq(extDosePerTime, basePattern)
              val isSel = !isExtCustomFreq && extFrequency == calculatedFreq
              FilterChip(
                selected = isSel,
                onClick = {
                  isExtCustomFreq = false
                  extFrequency = calculatedFreq
                  extQuantityText = calculateAutoQty(extFrequency, extDurationNum, extDurationUnit).toString()
                },
                label = { Text(calculatedFreq, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(26.dp)
              )
            }

            // Custom Frequency Chip
            FilterChip(
              selected = isExtCustomFreq,
              onClick = {
                isExtCustomFreq = !isExtCustomFreq
                if (isExtCustomFreq && extCustomFrequencyText.isNotBlank()) {
                  extFrequency = extCustomFrequencyText
                }
              },
              label = { Text("+ စိတ်ကြိုက် (Custom)", fontSize = 9.sp, fontWeight = if (isExtCustomFreq) FontWeight.Bold else FontWeight.Normal) },
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier.height(26.dp)
            )
          }

          if (isExtCustomFreq) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = extCustomFrequencyText,
              onValueChange = {
                extCustomFrequencyText = it
                extFrequency = it
              },
              label = { Text("စိတ်ကြိုက် ကြိမ်နှုန်းရိုက်ထည့်ပါ (e.g. 1/2 tab bd, 2 tabs stat)", fontSize = 9.5.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text("Meal Direction (သောက်သုံးရန်):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            instructionPresetPairs.forEach { (chipLabel, instText) ->
              val isSelected = if (chipLabel == "CST") {
                extInstructions.contains("သောက်လက်စသောက်ဆေးများ") || extInstructions.contains("CST", ignoreCase = true)
              } else {
                extInstructions.contains(instText)
              }
              FilterChip(
                selected = isSelected,
                onClick = {
                  extInstructions = if (isSelected) {
                    extInstructions.replace(instText, "").replace("•", "").trim().trim(',', ' ')
                  } else {
                    if (extInstructions.isBlank()) instText else "$extInstructions • $instText"
                  }
                },
                label = {
                  Text(
                    if (chipLabel == "CST") "CST (ဆက်သောက်ရန်)" else chipLabel,
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(26.dp),
                colors = if (chipLabel == "CST") FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFFDBEAFE),
                  selectedLabelColor = Color(0xFF1E40AF),
                  containerColor = Color(0xFFEFF6FF),
                  labelColor = Color(0xFF2563EB)
                ) else FilterChipDefaults.filterChipColors()
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Duration Quick Picker Section (Chips for Numbers & Units) for External Rx
          Text("သောက်သုံးမည့်ကာလ (Duration):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            durationQuickNums.forEach { n ->
              val isSel = extDurationNum == n && extDurationUnit != "ပုံမှန်သောက်ရန်"
              Surface(
                modifier = Modifier
                  .padding(end = 4.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    extDurationNum = n
                    extDuration = "$n $extDurationUnit"
                    extQuantityText = calculateAutoQty(extFrequency, n, extDurationUnit).toString()
                  },
                color = if (isSel) MedicalGreen else BackgroundSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, if (isSel) MedicalGreenDark else BorderGrey)
              ) {
                Text(
                  "$n",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSel) PureWhite else DarkGrayText,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                  if (extDurationNum > 1) {
                    extDurationNum -= 1
                    extDuration = "$extDurationNum $extDurationUnit"
                    extQuantityText = calculateAutoQty(extFrequency, extDurationNum, extDurationUnit).toString()
                  }
                },
              color = BackgroundSurface,
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.6.dp, BorderGrey)
            ) {
              Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }

            Spacer(modifier = Modifier.width(3.dp))

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                  extDurationNum += 1
                  extDuration = "$extDurationNum $extDurationUnit"
                  extQuantityText = calculateAutoQty(extFrequency, extDurationNum, extDurationUnit).toString()
                },
              color = BackgroundSurface,
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.6.dp, BorderGrey)
            ) {
              Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            durationUnits.forEach { (uKey, uLabel) ->
              val isSel = extDurationUnit == uKey
              FilterChip(
                selected = isSel,
                onClick = {
                  extDurationUnit = uKey
                  extDuration = if (uKey == "ပုံမှန်သောက်ရန်") "ပုံမှန်သောက်ရန်" else "$extDurationNum $uKey"
                  extQuantityText = calculateAutoQty(extFrequency, extDurationNum, uKey).toString()
                },
                label = { Text(uLabel, fontSize = 9.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Quantity Quick Picker Section (Chips for Qty) for External Rx
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ထုတ်ပေးမည့် အရေအတွက် (Qty):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.width(6.dp))
            quantityQuickNums.forEach { q ->
              val isSel = extQuantityText == q.toString()
              Surface(
                modifier = Modifier
                  .padding(end = 4.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .clickable { extQuantityText = q.toString() },
                color = if (isSel) Color(0xFF1565C0) else BackgroundSurface,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.6.dp, if (isSel) Color(0xFF0D47A1) else BorderGrey)
              ) {
                Text(
                  "$q",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSel) PureWhite else DarkGrayText,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
              }
            }

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                  val cur = extQuantityText.toIntOrNull() ?: 10
                  if (cur > 1) extQuantityText = (cur - 1).toString()
                },
              color = BackgroundSurface,
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.6.dp, BorderGrey)
            ) {
              Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }

            Spacer(modifier = Modifier.width(3.dp))

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                  val cur = extQuantityText.toIntOrNull() ?: 10
                  extQuantityText = (cur + 1).toString()
                },
              color = BackgroundSurface,
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(0.6.dp, BorderGrey)
            ) {
              Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
              value = extDosage,
              onValueChange = { extDosage = it },
              label = { Text("Strength / Dose", fontSize = 10.sp) },
              placeholder = { Text("e.g. 40 mg, 1 Tab", fontSize = 10.sp) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true,
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )
            OutlinedTextField(
              value = extDuration,
              onValueChange = { extDuration = it },
              label = { Text("Duration (ရက်)", fontSize = 10.sp) },
              placeholder = { Text("e.g. 7 Days, 1 Month", fontSize = 10.sp) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true,
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )
            OutlinedTextField(
              value = extQuantityText,
              onValueChange = { extQuantityText = it },
              label = { Text("Total Qty (လုံးရေ)", fontSize = 10.sp) },
              placeholder = { Text("14", fontSize = 10.sp) },
              modifier = Modifier.weight(0.9f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true,
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = extInstructions,
            onValueChange = { extInstructions = it },
            label = { Text("ညွှန်ကြားချက် (Directions e.g. စားပြီး၊ ရေနွေးဖြင့်သောက်ရန်)", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
          )

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = extNotes,
            onValueChange = { extNotes = it },
            label = { Text("ဆေးညွှန်းမှတ်ချက် (Special Prescription Remarks)", fontSize = 10.sp) },
            placeholder = { Text("e.g. ပြင်ပဆေးဆိုင်တွင် ဝယ်ယူရန်", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier
                .weight(1f)
                .height(36.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Cancel", fontSize = 11.sp, maxLines = 1)
            }
            Button(
              onClick = {
                if (extDrugName.isNotBlank()) {
                  val qty = extQuantityText.toIntOrNull() ?: 10
                  val fullInstructions = if (extNotes.isNotBlank() && !extInstructions.contains(extNotes)) {
                    "$extInstructions ($extNotes)"
                  } else {
                    extInstructions
                  }
                  val isCst = extDrugName.contains("သောက်လက်စ") || extDrugName.contains("CST", ignoreCase = true) || fullInstructions.contains("သောက်လက်စ") || fullInstructions.contains("CST", ignoreCase = true)
                  onDrugAdded(
                    PrescribedDrug(
                      medicineId = if (isCst) "CST-" + UUID.randomUUID().toString() else "EXT-" + UUID.randomUUID().toString(),
                      name = extDrugName.trim(),
                      dosage = extDosage.trim(),
                      frequency = extFrequency,
                      duration = extDuration.trim(),
                      quantity = if (isCst) 0 else qty,
                      unitPrice = 0.0,
                      instructions = fullInstructions,
                      isExternal = true,
                      isCst = isCst
                    )
                  )
                }
              },
              enabled = extDrugName.isNotBlank(),
              modifier = Modifier
                .weight(1.3f)
                .height(36.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ပြင်ပဆေးညွှန်း ထည့်မည် (Add External Rx)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }
        }
      }
    }
  }
}
