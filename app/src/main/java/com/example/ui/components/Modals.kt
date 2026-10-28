package com.example.ui.components
import com.example.viewmodel.ClinicViewModel
import com.example.service.sync.LocalWifiSyncService
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RegisterPatientDialog(
  onDismiss: () -> Unit,
  onRegister: (
    name: String,
    age: Int,
    gender: Gender,
    phone: String,
    address: String,
    bloodType: String,
    allergies: List<String>,
    chiefComplaint: String,
    bp: String,
    pulse: Int,
    temp: Double,
    spo2: Int,
    weight: Double,
    rbs: Double?,
    isDirectInjection: Boolean
  ) -> Unit
) {
  var isDirectInjection by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf("") }
  var ageText by remember { mutableStateOf("35") }
  var selectedGender by remember { mutableStateOf(Gender.MALE) }
  var phone by remember { mutableStateOf("09-") }
  var address by remember { mutableStateOf("") }
  var bloodType by remember { mutableStateOf("O+") }
  var allergyInput by remember { mutableStateOf("") }
  var chiefComplaint by remember { mutableStateOf("") }

  // Vitals
  var bp by remember { mutableStateOf("120/80") }
  var pulseText by remember { mutableStateOf("76") }
  var tempText by remember { mutableStateOf("98.4") }
  var spo2Text by remember { mutableStateOf("98") }
  var weightText by remember { mutableStateOf("60.0") }
  var rbsText by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .padding(vertical = 16.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Register New Patient",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "လူနာအသစ်မှတ်ပုံတင်ခြင်းနှင့် အရေးကြီးလက္ခဏာများ",
              fontSize = 12.sp,
              color = SlateMuted
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visit Type Selection: Doctor Consultation vs Direct Injection / Day-Care
        Text("လာရောက်သည့် ရည်ရွယ်ချက် (Visit Type):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .clickable { isDirectInjection = false },
            shape = RoundedCornerShape(8.dp),
            color = if (!isDirectInjection) SoftGreenContainer else PureWhite,
            border = BorderStroke(1.2.dp, if (!isDirectInjection) MedicalGreen else BorderGrey)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = !isDirectInjection,
                onClick = { isDirectInjection = false },
                colors = RadioButtonDefaults.colors(selectedColor = MedicalGreen)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text("ဆရာဝန်ပြသရန်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                Text("Doctor Consultation", fontSize = 9.sp, color = SlateMedium)
              }
            }
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .clickable {
                isDirectInjection = true
                if (chiefComplaint.isBlank()) {
                  chiefComplaint = "တိုက်ရိုက်ဆေးထိုး / Day-Care (Direct Injection)"
                }
              },
            shape = RoundedCornerShape(8.dp),
            color = if (isDirectInjection) Color(0xFFEFF6FF) else PureWhite,
            border = BorderStroke(1.2.dp, if (isDirectInjection) SkyBluePrimary else BorderGrey)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = isDirectInjection,
                onClick = {
                  isDirectInjection = true
                  if (chiefComplaint.isBlank()) {
                  chiefComplaint = "တိုက်ရိုက်ဆေးထိုး / Day-Care (Direct Injection)"
                  }
                },
                colors = RadioButtonDefaults.colors(selectedColor = SkyBluePrimary)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text("တိုက်ရိုက်ဆေးထိုး", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                Text("Direct Injection (No Doc)", fontSize = 9.sp, color = SkyBlueDark)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Basic Info
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Patient Full Name (လူနာအမည်)") },
          placeholder = { Text("e.g. U Kyaw Than / Daw Hla Myint") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it },
            label = { Text("Age (အသက်)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone No (ဖုန်း)") },
            modifier = Modifier.weight(1.5f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Address Field
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Address / Township (နေရပ်လိပ်စာ)") },
          placeholder = { Text("e.g. No. 42, 26th Street, Chanayethazan, Mandalay") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          leadingIcon = {
            Icon(Icons.Default.Home, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(18.dp))
          }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Gender Selector
        Text("Gender (ကျား/မ):", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SlateDark)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Gender.entries.forEach { g ->
            FilterChip(
              selected = selectedGender == g,
              onClick = { selectedGender = g },
              label = { Text(g.label, fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chief Complaint
        OutlinedTextField(
          value = chiefComplaint,
          onValueChange = { chiefComplaint = it },
          label = { Text("Chief Complaint (အဓိကခံစားရသောဝေဒနာ)") },
          placeholder = { Text("e.g. Severe headache, High fever for 2 days") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Allergies
        OutlinedTextField(
          value = allergyInput,
          onValueChange = { allergyInput = it },
          label = { Text("Known Allergies (မတည့်သောဆေးများ)") },
          placeholder = { Text("e.g. Penicillin, Aspirin, Sulfa") },
          modifier = Modifier.fillMaxWidth(),
          trailingIcon = {
            if (allergyInput.isNotBlank()) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical)
            }
          }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Vitals Section
        Text(
          "Triage Vitals (အရေးကြီးလက္ခဏာများ တိုင်းတာချက်)",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = SkyBlueDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = bp,
            onValueChange = { bp = it },
            label = { Text("BP (mmHg)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = pulseText,
            onValueChange = { pulseText = it },
            label = { Text("Pulse (bpm)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tempText,
            onValueChange = { tempText = it },
            label = { Text("Temp (°F)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = spo2Text,
            onValueChange = { spo2Text = it },
            label = { Text("SpO2 (%)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // RBS (Random Blood Sugar) - Whenever necessary
        OutlinedTextField(
          value = rbsText,
          onValueChange = { rbsText = it },
          label = { Text("RBS (Blood Sugar mg/dL) - Optional / လိုအပ်ပါက") },
          placeholder = { Text("e.g. 110 (Normal: 70-140 mg/dL)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          leadingIcon = {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
          }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Submit Button
        Button(
          onClick = {
            if (name.isNotBlank()) {
              val allergyList = allergyInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
              onRegister(
                name,
                ageText.toIntOrNull() ?: 30,
                selectedGender,
                phone,
                address.trim(),
                bloodType,
                allergyList,
                chiefComplaint.ifBlank { if (isDirectInjection) "တိုက်ရိုက်ဆေးထိုး / Day-Care (Direct Injection)" else "General clinical consultation" },
                bp,
                pulseText.toIntOrNull() ?: 75,
                tempText.toDoubleOrNull() ?: 36.8,
                spo2Text.toIntOrNull() ?: 98,
                weightText.toDoubleOrNull() ?: 60.0,
                rbsText.toDoubleOrNull(),
                isDirectInjection
              )
              onDismiss()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
          enabled = name.isNotBlank(),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Register Patient", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}

/**
 * Edit Patient Information & Vitals Dialog
 * Allows correcting any patient registration error (name, age, phone, address, vitals, complaint, etc.)
 */
@Composable
fun EditPatientDialog(
  patient: com.example.model.Patient,
  onDismiss: () -> Unit,
  onSave: (com.example.model.Patient) -> Unit
) {
  var name by remember(patient.id) { mutableStateOf(patient.name) }
  var ageText by remember(patient.id) { mutableStateOf(patient.age.toString()) }
  var selectedGender by remember(patient.id) { mutableStateOf(patient.gender) }
  var phone by remember(patient.id) { mutableStateOf(patient.phone) }
  var address by remember(patient.id) { mutableStateOf(patient.address) }
  var bloodType by remember(patient.id) { mutableStateOf(if (patient.bloodType.isNotBlank()) patient.bloodType else "O+") }
  var allergyInput by remember(patient.id) { mutableStateOf(patient.allergies.joinToString(", ")) }
  var chiefComplaint by remember(patient.id) { mutableStateOf(patient.chiefComplaint) }
  var followUpReason by remember(patient.id) { mutableStateOf(patient.followUpReason) }

  // Vitals
  var bp by remember(patient.id) { mutableStateOf(patient.vitals.bloodPressure) }
  var pulseText by remember(patient.id) { mutableStateOf(patient.vitals.pulseRate.toString()) }
  var tempText by remember(patient.id) { mutableStateOf(String.format(Locale.US, "%.1f", patient.vitals.temperatureFahrenheit)) }
  var spo2Text by remember(patient.id) { mutableStateOf(patient.vitals.oxygenSaturation.toString()) }
  var weightText by remember(patient.id) { mutableStateOf(patient.vitals.weightKg.toString()) }
  var rbsText by remember(patient.id) { mutableStateOf(if (patient.vitals.rbs != null) String.format(Locale.US, "%.0f", patient.vitals.rbs) else "") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "လူနာအချက်အလက် ပြင်ဆင်ရန် (Edit Patient)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "${patient.hn} • စာရင်းစဉ် #${patient.queueNumber}",
              style = MaterialTheme.typography.bodySmall,
              color = MedicalGreen,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Basic Info
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("လူနာအမည် (Full Name)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it },
            label = { Text("အသက် (Age)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          // Gender Selector
          Row(
            modifier = Modifier
              .weight(1.3f)
              .align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Gender.values().forEach { g ->
              FilterChip(
                selected = selectedGender == g,
                onClick = { selectedGender = g },
                label = { Text(g.name, fontSize = 11.sp) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("ဖုန်းနံပါတ် (Phone)") },
            modifier = Modifier.weight(1.3f),
            singleLine = true
          )
          OutlinedTextField(
            value = bloodType,
            onValueChange = { bloodType = it },
            label = { Text("သွေးအမျိုးအစား") },
            placeholder = { Text("O+, B+, ...") },
            modifier = Modifier.weight(0.9f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("နေရပ်လိပ်စာ (Address)") },
          placeholder = { Text("e.g. မဟာအောင်မြေ၊ မန္တလေး") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = chiefComplaint,
          onValueChange = { chiefComplaint = it },
          label = { Text("ရောဂါလက္ခဏာ / ခံစားချက် (Chief Complaint)") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          maxLines = 3
        )

        if (patient.isFollowUp) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = followUpReason,
            onValueChange = { followUpReason = it },
            label = { Text("Follow-up အကြောင်းအရင်း") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = allergyInput,
          onValueChange = { allergyInput = it },
          label = { Text("မတည့်သော ဆေးဝါးများ (Allergies - Comma separated)") },
          placeholder = { Text("e.g. Penicillin, Aspirin") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vitals Section
        Text(
          "အခြေခံ သွေးပေါင်နှင့် ကျန်းမာရေးစစ်ဆေးချက် (Vital Signs)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MedicalGreenDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = bp,
            onValueChange = { bp = it },
            label = { Text("BP (mmHg)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = pulseText,
            onValueChange = { pulseText = it },
            label = { Text("Pulse (bpm)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tempText,
            onValueChange = { tempText = it },
            label = { Text("Temp (°F)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = spo2Text,
            onValueChange = { spo2Text = it },
            label = { Text("SpO2 (%)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = rbsText,
          onValueChange = { rbsText = it },
          label = { Text("RBS (Blood Sugar mg/dL) - လိုအပ်ပါက") },
          placeholder = { Text("e.g. 110") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          leadingIcon = {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
          }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Cancel and Save
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(38.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("မလုပ်တော့ပါ (Cancel)", fontSize = 11.5.sp)
          }

          Button(
            onClick = {
              if (name.isNotBlank()) {
                val allergyList = allergyInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val parsedTemp = tempText.toDoubleOrNull() ?: 98.4
                // Convert Fahrenheit input to Celsius for internal storage
                val tempCelsius = if (parsedTemp > 50.0) ((parsedTemp - 32.0) * 5.0 / 9.0) else parsedTemp
                val updatedVitals = patient.vitals.copy(
                  bloodPressure = bp.trim().ifBlank { "120/80" },
                  pulseRate = pulseText.toIntOrNull() ?: 75,
                  temperatureCelsius = tempCelsius,
                  oxygenSaturation = spo2Text.toIntOrNull() ?: 98,
                  weightKg = weightText.toDoubleOrNull() ?: 60.0,
                  rbs = rbsText.toDoubleOrNull() ?: patient.vitals.rbs
                )
                val updatedPatient = patient.copy(
                  name = name.trim(),
                  age = ageText.toIntOrNull() ?: patient.age,
                  gender = selectedGender,
                  phone = phone.trim(),
                  address = address.trim(),
                  bloodType = bloodType.trim(),
                  allergies = allergyList,
                  chiefComplaint = chiefComplaint.trim().ifBlank { patient.chiefComplaint },
                  followUpReason = followUpReason.trim(),
                  vitals = updatedVitals
                )
                onSave(updatedPatient)
                onDismiss()
              }
            },
            modifier = Modifier
              .weight(1.2f)
              .height(38.dp),
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("အချက်အလက် သိမ်းမည် (Save)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun AddMedicineDialog(
  onDismiss: () -> Unit,
  onAdd: (
    name: String,
    genericName: String,
    dosageForm: String,
    category: String,
    batchNo: String,
    expiryDate: String,
    quantity: Int,
    costPrice: Double,
    profitMarginPercent: Double,
    minThreshold: Int
  ) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var genericName by remember { mutableStateOf("") }
  var dosageForm by remember { mutableStateOf("Tablet") }
  var category by remember { mutableStateOf("Antibiotics") }
  var batchNo by remember { mutableStateOf("BAT-2026-01") }
  var expiryDate by remember { mutableStateOf("2027-12-31") }
  var quantityText by remember { mutableStateOf("100") }
  var costPriceText by remember { mutableStateOf("500") }
  var marginPercentText by remember { mutableStateOf("25") }

  // Auto-calculated selling price
  val cost = costPriceText.toDoubleOrNull() ?: 0.0
  val margin = marginPercentText.toDoubleOrNull() ?: 0.0
  val calculatedSellingPrice = cost + (cost * (margin / 100.0))

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .padding(vertical = 16.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Add Medicine & Stock Batch",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "FEFO စနစ်ဖြင့် ဆေးဝါးအသစ်ထည့်သွင်းခြင်း",
              fontSize = 12.sp,
              color = SlateMuted
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Trade / Brand Name (ကုန်အမှတ်တံဆိပ်)") },
          placeholder = { Text("e.g. Augmentin 625mg / Biogesic 500mg") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = genericName,
          onValueChange = { genericName = it },
          label = { Text("Generic Formula Name (ဆေးအမည်ရင်း)") },
          placeholder = { Text("e.g. Co-Amoxiclav / Paracetamol") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = dosageForm,
            onValueChange = { dosageForm = it },
            label = { Text("Dosage Form") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FEFO Batch Details Box
        Text(
          "FEFO Initial Batch & Expiry (သက်တမ်းကုန်ဆုံးရက်)",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = SkyBlueDark
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = batchNo,
            onValueChange = { batchNo = it },
            label = { Text("Batch #") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            label = { Text("Expiry (YYYY-MM-DD)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = quantityText,
          onValueChange = { quantityText = it },
          label = { Text("Initial Stock Quantity (ယူနစ်အရေအတွက်)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pricing Formula Box: Cost Price + Profit Margin % = Selling Price
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(OceanGreenLight)
            .border(1.dp, OceanGreenContainer, RoundedCornerShape(6.dp))
            .padding(10.dp)
        ) {
          Column {
            Text(
              "Pricing Engine: Cost + Margin % = Selling Price",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = OceanGreenPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = costPriceText,
                onValueChange = { costPriceText = it },
                label = { Text("Cost (ဝယ်ဈေး)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
              )
              OutlinedTextField(
                value = marginPercentText,
                onValueChange = { marginPercentText = it },
                label = { Text("Margin % (အမြတ်)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Selling Price (ရောင်းဈေး):", fontSize = 11.sp, color = SlateDark)
              Text(
                "${NumberFormat.getNumberInstance(Locale.US).format(calculatedSellingPrice.toInt())} Ks",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = OceanGreenPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            if (name.isNotBlank()) {
              onAdd(
                name,
                genericName,
                dosageForm,
                category,
                batchNo,
                expiryDate,
                quantityText.toIntOrNull() ?: 50,
                cost,
                margin,
                20
              )
              onDismiss()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
          enabled = name.isNotBlank(),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
        ) {
          Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Medicine", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProcedureDialog(
  onDismiss: () -> Unit,
  categoryOptions: List<String> = emptyList(),
  onAdd: (
    name: String,
    myanmarName: String,
    category: String,
    fee: Double,
    notes: String,
    consumables: List<String>
  ) -> Unit
) {
  val defaultOptions = listOf("Minor Surgery", "Dressing", "Injection", "Diagnostics", "Wound Care", "Cannulation")
  val effectiveCategories = (categoryOptions + defaultOptions).distinct()

  var name by remember { mutableStateOf("") }
  var myanmarName by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(effectiveCategories.firstOrNull() ?: "Minor Surgery") }
  var feeText by remember { mutableStateOf("15000") }
  var notes by remember { mutableStateOf("") }
  var consumablesText by remember { mutableStateOf("Sterile pack, Local Anaesthesia, Gauze") }
  var catDropdownExpanded by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 480.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Add Custom Procedure",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "သီးသန့်ကုသမှု အသစ်ထည့်သွင်းခြင်း",
              fontSize = 11.sp,
              color = SlateMuted
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Procedure Name (English)", fontSize = 11.sp) },
          placeholder = { Text("e.g. Foreign Body Removal", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = myanmarName,
          onValueChange = { myanmarName = it },
          label = { Text("Procedure Name (Myanmar)", fontSize = 11.sp) },
          placeholder = { Text("e.g. ပြင်ပပစ္စည်းထုတ်ယူခြင်း", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ExposedDropdownMenuBox(
            expanded = catDropdownExpanded,
            onExpandedChange = { catDropdownExpanded = it },
            modifier = Modifier.weight(1.1f)
          ) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Category / Custom", fontSize = 11.sp) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              singleLine = true
            )
            ExposedDropdownMenu(
              expanded = catDropdownExpanded,
              onDismissRequest = { catDropdownExpanded = false }
            ) {
              effectiveCategories.forEach { catOption ->
                DropdownMenuItem(
                  text = { Text(catOption, fontSize = 11.5.sp) },
                  onClick = {
                    category = catOption
                    catDropdownExpanded = false
                  }
                )
              }
            }
          }
          OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = it },
            label = { Text("Fee (Ks)", fontSize = 11.sp) },
            modifier = Modifier.weight(0.9f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = consumablesText,
          onValueChange = { consumablesText = it },
          label = { Text("Consumables", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Clinical Notes", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            if (name.isNotBlank()) {
              val items = consumablesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
              onAdd(
                name,
                myanmarName,
                category,
                feeText.toDoubleOrNull() ?: 10000.0,
                notes,
                items
              )
              onDismiss()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
          enabled = name.isNotBlank(),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
        ) {
          Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Procedure", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProcedureDialog(
  procedure: com.example.model.ProcedureItem,
  categoryOptions: List<String> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (com.example.model.ProcedureItem) -> Unit
) {
  val defaultOptions = listOf("Minor Surgery", "Dressing", "Injection", "Diagnostics", "Wound Care", "Cannulation")
  val effectiveCategories = (categoryOptions + defaultOptions).distinct()

  var name by remember { mutableStateOf(procedure.name) }
  var myanmarName by remember { mutableStateOf(procedure.myanmarName) }
  var category by remember { mutableStateOf(procedure.category) }
  var feeText by remember { mutableStateOf(procedure.standardFee.toInt().toString()) }
  var notes by remember { mutableStateOf(procedure.notes) }
  val initialConsumables = if (procedure.consumableItems.isNotEmpty()) {
    procedure.consumableItems.joinToString(", ") { "${it.name} x ${it.quantity}" }
  } else procedure.notes
  var consumablesText by remember { mutableStateOf(initialConsumables) }
  var catDropdownExpanded by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 480.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Edit Procedure (ကုသမှု ပြင်ဆင်ရန်)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "ID: ${procedure.id.take(8)} • ${procedure.category}",
              fontSize = 11.sp,
              color = SlateMuted
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Procedure Name (English)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = myanmarName,
          onValueChange = { myanmarName = it },
          label = { Text("Procedure Name (Myanmar)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ExposedDropdownMenuBox(
            expanded = catDropdownExpanded,
            onExpandedChange = { catDropdownExpanded = it },
            modifier = Modifier.weight(1.1f)
          ) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Category / Custom", fontSize = 11.sp) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              singleLine = true
            )
            ExposedDropdownMenu(
              expanded = catDropdownExpanded,
              onDismissRequest = { catDropdownExpanded = false }
            ) {
              effectiveCategories.forEach { catOption ->
                DropdownMenuItem(
                  text = { Text(catOption, fontSize = 11.5.sp) },
                  onClick = {
                    category = catOption
                    catDropdownExpanded = false
                  }
                )
              }
            }
          }
          OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = it },
            label = { Text("Fee (Ks)", fontSize = 11.sp) },
            modifier = Modifier.weight(0.9f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = consumablesText,
          onValueChange = { consumablesText = it },
          label = { Text("Consumables", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Clinical Notes", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = {
            if (name.isNotBlank()) {
              val newFee = feeText.toDoubleOrNull() ?: procedure.standardFee
              val updated = procedure.copy(
                name = name.trim(),
                myanmarName = myanmarName.trim().ifBlank { name.trim() },
                category = category.trim(),
                price = newFee,
                standardFee = newFee,
                notes = notes.trim()
              )
              onSave(updated)
              onDismiss()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
          enabled = name.isNotBlank(),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Update Procedure (သိမ်းဆည်းမည်)", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusDialog(
  viewModel: ClinicViewModel,
  initialTab: Int = 0,
  onDismiss: () -> Unit
) {
  val syncLogs by viewModel.syncLogs.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val isWifiHubRunning by viewModel.isWifiHubRunning.collectAsState()
  val localWifiIp by viewModel.localWifiIp.collectAsState()
  val clinicSettings by viewModel.clinicSettings.collectAsState()
  val context = LocalContext.current

  var selectedTab by remember { mutableStateOf(initialTab.coerceIn(0, 2)) } // 0 = Wi-Fi Peer Sync, 1 = Supabase Cloud, 2 = Logs
  var targetHubIp by remember { mutableStateOf(clinicSettings.wifiHubHostIp.ifBlank { "192.168.1." }) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isErrorStatus by remember { mutableStateOf(false) }
  var isTesting by remember { mutableStateOf(false) }
  var isScanningPeers by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  fun triggerScan() {
    coroutineScope.launch {
      isScanningPeers = true
      try {
        LocalWifiSyncService.startHub()
        LocalWifiSyncService.startDiscovery(context)
        withContext(Dispatchers.IO) {
          LocalWifiSyncService.scanSubnetForPeers()
        }
      } catch (_: Exception) {
      } finally {
        isScanningPeers = false
      }
    }
  }

  LaunchedEffect(Unit) {
    triggerScan()
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp)
        .padding(vertical = 10.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
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
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MedicalGreen.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.CloudSync, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "ဒေတာ ထပ်တူပြုခြင်း (Clinic Sync Center)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = SlateDark
              )
              Text(
                "Station: ${clinicSettings.activeStationName.ifBlank { "Consultation Room" }}",
                fontSize = 10.sp,
                color = SlateMuted
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = Color(0xFFF8FAFC),
          contentColor = MedicalGreen
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0; statusMessage = null },
            text = { Text("၁။ Wi-Fi Peer Sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1; statusMessage = null },
            text = { Text("၂။ Supabase Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2; statusMessage = null },
            text = { Text("၃။ Sync မှတ်တမ်း", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
        }

        // Status banner
        if (statusMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = if (isErrorStatus) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(
                if (isErrorStatus) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isErrorStatus) Color(0xFFDC2626) else Color(0xFF16A34A),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                statusMessage ?: "",
                fontSize = 10.5.sp,
                color = if (isErrorStatus) Color(0xFFDC2626) else Color(0xFF166534),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content
        when (selectedTab) {
          0 -> {
            // Wi-Fi Peer Sync Tab
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Info banner
              Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Wifi, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    "အင်တာနက်မလိုဘဲ တူညီသော Wi-Fi router / Mobile Hotspot ဖြင့် Doctor, Reception, Cashier စက်များ တိုက်ရိုက် Sync လုပ်နိုင်ပါသည် (Data ကုန်ကျစရိတ်လုံးဝမရှိပါ)",
                    fontSize = 9.5.sp,
                    color = Color(0xFF166534)
                  )
                }
              }

              // Local IP Display Card
              Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text("ဤစက်၏ Local Wi-Fi IP လိပ်စာ:", fontSize = 10.sp, color = SlateMuted)
                    Text(
                      "$localWifiIp:${clinicSettings.wifiSyncPort}",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = SlateDark,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                  OutlinedButton(
                    onClick = {
                      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      cm.setPrimaryClip(ClipData.newPlainText("IP", localWifiIp))
                      Toast.makeText(context, "IP copied: $localWifiIp", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy IP", fontSize = 10.sp)
                  }
                }
              }

              // Mode 1: Host / Hub (Doctor or Main Desk)
              Surface(
                color = PureWhite,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isWifiHubRunning) MedicalGreen else BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(RoundedCornerShape(4.dp))
                          .background(if (isWifiHubRunning) Color(0xFF16A34A) else Color(0xFF94A3B8))
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "က။ ပင်မစက် (Hub / Host Mode)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                      )
                    }
                    Surface(
                      color = if (isWifiHubRunning) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        if (isWifiHubRunning) "ACTIVE (ဖွင့်ထားသည်)" else "STANDBY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWifiHubRunning) Color(0xFF166534) else SlateMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Text(
                    if (isWifiHubRunning)
                      "အခြား Reception သို့မဟုတ် Cashier စက်များမှ ဤစက်သို့ ချိတ်ဆက်၍ လူနာစာရင်းနှင့် ဘောက်ချာများ Sync လုပ်နိုင်ပါပြီ။"
                    else
                      "Doctor စက် သို့မဟုတ် ပင်မကောင်တာစက်တွင် Hub ဖွင့်ထားပါက အခြားစက်များ အလိုအလျောက်ချိတ်ဆက်နိုင်ပါသည်။",
                    fontSize = 9.5.sp,
                    color = DarkGrayText.copy(alpha = 0.8f)
                  )

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isWifiHubRunning) {
                      OutlinedButton(
                        onClick = {
                          viewModel.stopWifiSyncHub()
                          statusMessage = "Wi-Fi Hub ကို ပိတ်လိုက်ပါပြီ"
                          isErrorStatus = false
                        },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text("Hub ပိတ်မည် (Stop)", fontSize = 10.5.sp, color = MahoganyAccent)
                      }
                    } else {
                      Button(
                        onClick = {
                          viewModel.startWifiSyncHub(clinicSettings.wifiSyncPort) { ok, msg ->
                            statusMessage = msg
                            isErrorStatus = !ok
                          }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ပင်မစက်အဖြစ် ဖွင့်မည် (Start Hub)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              // Auto-Discovered Peers Section
              val discoveredPeers by LocalWifiSyncService.discoveredPeers.collectAsState()
              Surface(
                color = PureWhite,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (discoveredPeers.isNotEmpty()) MedicalGreen else BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Devices, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "ခ။ အလိုအလျောက် တွေ့ရှိသော စက်များ (Auto-Discovered Peers)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                      )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (discoveredPeers.isNotEmpty()) {
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                          Text(
                            "${discoveredPeers.size} စက် ချိတ်ဆက်မိ",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                      IconButton(
                        onClick = { triggerScan() },
                        enabled = !isScanningPeers,
                        modifier = Modifier.size(26.dp)
                      ) {
                        if (isScanningPeers) {
                          CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = MedicalGreen)
                        } else {
                          Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = MedicalGreen, modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }

                  if (discoveredPeers.isNotEmpty()) {
                    Button(
                      onClick = {
                        viewModel.syncWithAllDiscoveredPeers { ok, msg ->
                          statusMessage = msg
                          isErrorStatus = !ok
                        }
                      },
                      enabled = !isSyncing,
                      colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "တွေ့ရှိသော စက်အားလုံးနှင့် ချိတ်ဆက် Sync လုပ်မည် (Sync All Peers)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }

                  if (discoveredPeers.isEmpty()) {
                    if (isScanningPeers) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                      ) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = MedicalGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          "Wi-Fi ကွန်ရက်ပေါ်ရှိ အခြားစက်များကို ရှာဖွေနေပါသည် (Scanning Wi-Fi subnet...)",
                          fontSize = 9.sp,
                          color = SlateMuted
                        )
                      }
                    } else {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        Text(
                          "လက်ရှိ Wi-Fi တွင် အခြားစက် မတွေ့ရှိသေးပါ (တူညီသော Wi-Fi တွင် အခြားဖုန်းမှ App ဖွင့်ထားပါ)",
                          fontSize = 9.sp,
                          color = SlateMuted
                        )
                        OutlinedButton(
                          onClick = { triggerScan() },
                          modifier = Modifier.height(28.dp),
                          shape = RoundedCornerShape(4.dp),
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = MedicalGreen)
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("ကွန်ရက် ပြန်လည်ရှာဖွေမည် (Scan LAN)", fontSize = 9.5.sp, color = MedicalGreen)
                        }
                      }
                    }
                  } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      discoveredPeers.forEach { peer ->
                        Surface(
                          color = Color(0xFFF8FAFC),
                          shape = RoundedCornerShape(6.dp),
                          border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Column {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                  modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF16A34A))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(peer.deviceName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(${peer.role})", fontSize = 8.5.sp, color = MedicalGreenDark)
                              }
                              Text("${peer.ip}:${peer.port}", fontSize = 9.sp, color = SlateMuted, fontFamily = FontFamily.Monospace)
                            }
                            Button(
                              onClick = {
                                viewModel.syncWithRemoteWifiHub(peer.ip, peer.port) { ok, msg ->
                                  statusMessage = msg
                                  isErrorStatus = !ok
                                }
                              },
                              enabled = !isSyncing,
                              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                              modifier = Modifier.height(26.dp),
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text("Sync ပြုလုပ်မည်", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }

              // Mode 3: Manual IP Connect (Fallback)
              Surface(
                color = PureWhite,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    "ဂ။ IP လိပ်စာဖြင့် တိုက်ရိုက်ချိတ်ဆက်မည် (Manual Fallback)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark
                  )

                  OutlinedTextField(
                    value = targetHubIp,
                    onValueChange = { targetHubIp = it },
                    label = { Text("Hub IP Address", fontSize = 10.sp) },
                    placeholder = { Text("e.g. 192.168.1.50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                  )

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = {
                        isTesting = true
                        viewModel.testWifiHubConnection(targetHubIp, clinicSettings.wifiSyncPort) { ok, msg ->
                          isTesting = false
                          statusMessage = msg
                          isErrorStatus = !ok
                        }
                      },
                      enabled = !isTesting && !isSyncing,
                      modifier = Modifier.height(34.dp),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(if (isTesting) "စစ်ဆေးနေသည်..." else "စမ်းသပ်မည်", fontSize = 10.sp)
                    }

                    Button(
                      onClick = {
                        viewModel.syncWithRemoteWifiHub(targetHubIp, clinicSettings.wifiSyncPort) { ok, msg ->
                          statusMessage = msg
                          isErrorStatus = !ok
                        }
                      },
                      enabled = !isSyncing,
                      colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
                      modifier = Modifier.height(34.dp),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PureWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Syncing...", fontSize = 10.sp)
                      } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manual Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }

          1 -> {
            // Supabase Cloud Tab
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Free tier safeguard banner
              Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      "Supabase Free Tier Quota Safeguard ဖွင့်ထားပါသည်",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF1E40AF)
                    )
                  }
                  Text(
                    "• Bandwidth ချွေတာခြင်း: သာမန်စာသားဒေတာများကိုသာ အကြိမ်ကြိမ်မဆွဲဘဲ Delta Sync ပြုလုပ်ပါသည် (ပုံကြမ်း drawing vector များကို အော့ဖ်လိုင်းတွင်သာထားရှိသည်)\n• 7-Day Inactivity Safe: Free database များ အိပ်မပျော်စေရန် Auto Heartbeat ပါဝင်ပါသည်\n• အော့ဖ်လိုင်းဖြစ်နေချိန်တွင် စက်တွင်း SQLite သို့ အပြည့်အဝ အလိုအလျောက်သိမ်းဆည်းပါသည်",
                    fontSize = 9.5.sp,
                    color = Color(0xFF1E3A8A),
                    lineHeight = 14.sp
                  )
                }
              }

              // Configuration summary card
              Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Supabase Project URL:", fontSize = 10.sp, color = SlateMuted)
                    Text(
                      if (clinicSettings.supabaseUrl.isNotBlank()) "Configured" else "Not Configured",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (clinicSettings.supabaseUrl.isNotBlank()) Color(0xFF16A34A) else MahoganyAccent
                    )
                  }
                  Text(
                    clinicSettings.supabaseUrl.ifBlank { "ဆေးခန်း ဆက်တင် (Settings) တွင် Supabase URL ထည့်သွင်းပေးပါ" },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SlateDark,
                    maxLines = 1
                  )

                  HorizontalDivider(thickness = 0.5.dp, color = BorderGrey)

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("နောက်ဆုံး Cloud Sync ချိန်:", fontSize = 10.sp, color = SlateMuted)
                    Text(
                      clinicSettings.lastSupabaseSyncTimestamp.ifBlank { "မရှိသေးပါ" },
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = SlateDark
                    )
                  }
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = {
                    if (clinicSettings.supabaseUrl.isBlank() || clinicSettings.supabaseAnonKey.isBlank()) {
                      statusMessage = "Settings တွင် Supabase URL နှင့် Anon Key ကို ဦးစွာ ထည့်သွင်းပေးပါ"
                      isErrorStatus = true
                      return@OutlinedButton
                    }
                    isTesting = true
                    viewModel.testSupabaseConnection(clinicSettings.supabaseUrl, clinicSettings.supabaseAnonKey) { ok, msg ->
                      isTesting = false
                      statusMessage = msg
                      isErrorStatus = !ok
                    }
                  },
                  enabled = !isTesting && !isSyncing,
                  modifier = Modifier.height(34.dp).weight(1f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(if (isTesting) "စစ်ဆေးနေသည်..." else "စစ်ဆေးမည်", fontSize = 10.sp)
                }

                Button(
                  onClick = {
                    viewModel.syncWithSupabaseCloud { ok, msg ->
                      statusMessage = msg
                      isErrorStatus = !ok
                    }
                  },
                  enabled = !isSyncing,
                  colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                  modifier = Modifier.height(34.dp).weight(1.2f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Cloud သို့ Upload", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    viewModel.pullFromSupabaseCloud { ok, msg ->
                      statusMessage = msg
                      isErrorStatus = !ok
                    }
                  },
                  enabled = !isSyncing,
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                  modifier = Modifier.height(34.dp).weight(1.2f),
                  contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Cloud မှ Pull", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          2 -> {
            // Logs Tab
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("Transaction & Peer Sync Activity:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SlateDark)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(200.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFF8FAFC))
                  .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
                  .padding(6.dp)
              ) {
                if (syncLogs.isEmpty()) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("မှတ်တမ်းများ မရှိသေးပါ", color = SlateMuted, fontSize = 11.sp)
                  }
                } else {
                  LazyColumn {
                    items(syncLogs) { log ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text("[${log.timestamp}]", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = SlateLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          log.action,
                          fontFamily = FontFamily.Monospace,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = when {
                            log.action.contains("SUCCESS") -> Color(0xFF16A34A)
                            log.action.contains("ERROR") || log.action.contains("FAIL") -> Color(0xFFDC2626)
                            log.action.contains("WIFI") -> OceanGreenPrimary
                            else -> MedicalGreen
                          }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          "${log.entityType}: ${log.recordId}",
                          fontFamily = FontFamily.Monospace,
                          fontSize = 9.sp,
                          color = SlateDark,
                          maxLines = 1
                        )
                      }
                    }
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "Local Storage: Ready",
                  fontSize = 10.5.sp,
                  color = Color(0xFF166534),
                  fontWeight = FontWeight.SemiBold
                )
                Button(
                  onClick = { viewModel.triggerManualSync() },
                  enabled = !isSyncing,
                  modifier = Modifier.height(34.dp),
                  shape = RoundedCornerShape(6.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = SlateDark)
                ) {
                  Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Force Disk Cache Flush", fontSize = 10.5.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SyncStatusDialog(
  syncLogs: List<SyncLogEntry>,
  isSyncing: Boolean,
  onForceSync: () -> Unit,
  onDismiss: () -> Unit
) {
  // Legacy overload kept for backward compatibility
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Sync Status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onForceSync, enabled = !isSyncing, modifier = Modifier.fillMaxWidth()) {
          Text("Force Sync")
        }
      }
    }
  }
}

@Composable
fun ChangePasswordDialog(
  currentUser: UserAccount?,
  onDismiss: () -> Unit,
  onChangePassword: (oldPass: String, newPass: String, callback: (Boolean, String) -> Unit) -> Unit
) {
  var oldPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var oldPassVisible by remember { mutableStateOf(false) }
  var newPassVisible by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }
  var isSubmitting by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Password, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("စကားဝှက် ပြောင်းလဲခြင်း", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = SlateDark)
              Text("အကောင့်: ${currentUser?.username ?: "User"}", fontSize = 10.sp, color = SlateMuted)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(16.dp))
          }
        }

        if (statusMessage != null) {
          Surface(
            color = if (isError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              statusMessage ?: "",
              fontSize = 11.sp,
              color = if (isError) Color(0xFFDC2626) else Color(0xFF166534),
              modifier = Modifier.padding(8.dp)
            )
          }
        }

        OutlinedTextField(
          value = oldPassword,
          onValueChange = { oldPassword = it; statusMessage = null },
          label = { Text("လက်ရှိ စကားဝှက် (Current Password)", fontSize = 11.sp) },
          singleLine = true,
          visualTransformation = if (oldPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { oldPassVisible = !oldPassVisible }) {
              Icon(if (oldPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        OutlinedTextField(
          value = newPassword,
          onValueChange = { newPassword = it; statusMessage = null },
          label = { Text("စကားဝှက် အသစ် (New Password)", fontSize = 11.sp) },
          singleLine = true,
          visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { newPassVisible = !newPassVisible }) {
              Icon(if (newPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it; statusMessage = null },
          label = { Text("စကားဝှက် အသစ်ကို ထပ်ရိုက်ပါ (Confirm)", fontSize = 11.sp) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(36.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("မလုပ်တော့ပါ", fontSize = 11.sp)
          }

          Button(
            onClick = {
              if (oldPassword.isBlank()) {
                statusMessage = "လက်ရှိစကားဝှက်ကို ထည့်ပါ"
                isError = true
                return@Button
              }
              if (newPassword.length < 4) {
                statusMessage = "စကားဝှက်အသစ်သည် အနည်းဆုံး ၄ လုံးရှိရပါမည်"
                isError = true
                return@Button
              }
              if (newPassword != confirmPassword) {
                statusMessage = "စကားဝှက်အသစ် ၂ ကြိမ် ရိုက်ထည့်မှု မတူညီပါ"
                isError = true
                return@Button
              }
              isSubmitting = true
              onChangePassword(oldPassword, newPassword) { ok, msg ->
                isSubmitting = false
                isError = !ok
                statusMessage = msg
                if (ok) {
                  onDismiss()
                }
              }
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            modifier = Modifier.weight(1.2f).height(36.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(if (isSubmitting) "သိမ်းဆည်းနေပါသည်..." else "ပြောင်းလဲမည် (Save)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

