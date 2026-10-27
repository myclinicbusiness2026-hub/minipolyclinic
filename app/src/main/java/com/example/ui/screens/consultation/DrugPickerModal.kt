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
  var searchQuery by remember { mutableStateOf("") }
  var selectedMedicine by remember { mutableStateOf<Medicine?>(medicines.firstOrNull()) }
  var dosage by remember { mutableStateOf(medicines.firstOrNull()?.standardDosage ?: "500 mg") }
  var frequency by remember { mutableStateOf("1-0-1") }
  var duration by remember { mutableStateOf("5 Days") }
  var quantityText by remember { mutableStateOf("10") }
  var instructions by remember { mutableStateOf("") }

  // State for Non-Store External Rx Tab
  var extDrugName by remember { mutableStateOf("") }
  var extDosage by remember { mutableStateOf("1 Tab") }
  var extFrequency by remember { mutableStateOf("1-0-1") }
  var extDuration by remember { mutableStateOf("5 Days") }
  var extQuantityText by remember { mutableStateOf("10") }
  var extInstructions by remember { mutableStateOf("") }
  var extNotes by remember { mutableStateOf("ပြင်ပဆေးဆိုင်တွင် ဝယ်ယူရန်") }

  val filteredMedicines = medicines.filter {
    searchQuery.isBlank() ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.genericName.contains(searchQuery, ignoreCase = true)
  }

  val freqPresets = listOf(
    "1-0-1",
    "1-1-1",
    "1-0-0",
    "1-1-1-1",
    "0-0-1",
    "လိုအပ်ပါက"
  )

  val instructionPresets = listOf(
    "စားပြီး",
    "မစားမီ",
    "အစာနှင့်တွဲ၍",
    "အိပ်ရာဝင်",
    "ကိုက်ခဲ/ဖျားပါက"
  )

  val commonExternalPresets = listOf(
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
                        "Stock: ${med.totalStock}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (med.totalStock < med.minStockThreshold) StatusCritical else MedicalGreenDark
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

            Text("Frequency (ကြိမ်ရေ):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              freqPresets.forEach { f ->
                FilterChip(
                  selected = frequency == f,
                  onClick = { frequency = f },
                  label = { Text(f, fontSize = 9.5.sp, fontWeight = if (frequency == f) FontWeight.Bold else FontWeight.Normal) },
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.height(26.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Meal Direction (သောက်သုံးရန်):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              instructionPresets.forEach { inst ->
                FilterChip(
                  selected = instructions == inst,
                  onClick = { instructions = if (instructions == inst) "" else inst },
                  label = { Text(inst, fontSize = 9.5.sp, fontWeight = if (instructions == inst) FontWeight.Bold else FontWeight.Normal) },
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier.height(26.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                label = { Text("Duration", fontSize = 10.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = clinicTextFieldColors(),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
              )
              OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Total Qty", fontSize = 10.sp) },
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
                  val qty = quantityText.toIntOrNull() ?: 10
                  onDrugAdded(
                    PrescribedDrug(
                      medicineId = med.id,
                      name = med.name,
                      dosage = dosage,
                      frequency = frequency,
                      duration = duration,
                      quantity = qty,
                      unitPrice = med.calculatedSellingPrice,
                      instructions = instructions,
                      isExternal = false
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
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = PureWhite,
                border = BorderStroke(0.8.dp, BorderGray),
                modifier = Modifier.clickable {
                  extDrugName = p
                  val pattern = Regex("\\b\\d+(\\.\\d+)?\\s*(mg|g|mcg|ml|iu|IU|%)\\b", RegexOption.IGNORE_CASE)
                  val match = pattern.find(p)?.value
                  if (match != null) extDosage = match
                }
              ) {
                Text(
                  "+ $p",
                  fontSize = 8.5.sp,
                  color = SlateDark,
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

          Text("Frequency (ကြိမ်ရေ):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            freqPresets.forEach { f ->
              FilterChip(
                selected = extFrequency == f,
                onClick = { extFrequency = f },
                label = { Text(f, fontSize = 9.5.sp, fontWeight = if (extFrequency == f) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(26.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text("Meal Direction (သောက်သုံးရန်):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            instructionPresets.forEach { inst ->
              FilterChip(
                selected = extInstructions == inst,
                onClick = { extInstructions = if (extInstructions == inst) "" else inst },
                label = { Text(inst, fontSize = 9.5.sp, fontWeight = if (extInstructions == inst) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(26.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

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
              label = { Text("Duration", fontSize = 10.sp) },
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
              label = { Text("Total Qty", fontSize = 10.sp) },
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
                  onDrugAdded(
                    PrescribedDrug(
                      medicineId = "EXT-" + UUID.randomUUID().toString(),
                      name = extDrugName.trim(),
                      dosage = extDosage.trim(),
                      frequency = extFrequency,
                      duration = extDuration.trim(),
                      quantity = qty,
                      unitPrice = 0.0,
                      instructions = fullInstructions,
                      isExternal = true
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
