package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ClinicSettings
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicSettingsDialog(
  currentSettings: ClinicSettings,
  onDismiss: () -> Unit,
  onSave: (ClinicSettings) -> Unit
) {
  var clinicNameMm by remember { mutableStateOf(currentSettings.clinicNameMm) }
  var clinicNameEn by remember { mutableStateOf(currentSettings.clinicNameEn) }
  var clinicSubtitleMm by remember { mutableStateOf(currentSettings.clinicSubtitleMm) }
  var clinicLogoUri by remember { mutableStateOf(currentSettings.clinicLogoUri) }
  var doctorName by remember { mutableStateOf(currentSettings.doctorName) }
  var doctorDegree by remember { mutableStateOf(currentSettings.doctorDegree) }
  var doctorSama by remember { mutableStateOf(currentSettings.doctorSama) }
  var phonePrimary by remember { mutableStateOf(currentSettings.phonePrimary) }
  var phoneSecondary by remember { mutableStateOf(currentSettings.phoneSecondary) }
  var addressMm by remember { mutableStateOf(currentSettings.addressMm) }
  var clinicHours by remember { mutableStateOf(currentSettings.clinicHours) }
  var clinicRegNo by remember { mutableStateOf(currentSettings.clinicRegNo) }
  var defaultConsultationFee by remember { mutableStateOf(currentSettings.defaultConsultationFee.toInt().toString()) }
  var defaultImInjectionFee by remember { mutableStateOf(currentSettings.defaultImInjectionFee.toInt().toString()) }
  var defaultIvPushFee by remember { mutableStateOf(currentSettings.defaultIvPushFee.toInt().toString()) }
  var defaultIvInfusionFee by remember { mutableStateOf(currentSettings.defaultIvInfusionFee.toInt().toString()) }
  var defaultNebulizerFee by remember { mutableStateOf(currentSettings.defaultNebulizerFee.toInt().toString()) }
  var defaultDepoFee by remember { mutableStateOf(currentSettings.defaultDepoFee.toInt().toString()) }
  var thermalPaperWidthMm by remember { mutableStateOf(currentSettings.thermalPaperWidthMm) }
  var receiptGreetingMm by remember { mutableStateOf(currentSettings.receiptGreetingMm) }
  var activeStationName by remember { mutableStateOf(currentSettings.activeStationName) }
  var wifiHubHostIp by remember { mutableStateOf(currentSettings.wifiHubHostIp) }
  var wifiSyncPort by remember { mutableStateOf(currentSettings.wifiSyncPort.toString()) }
  var supabaseUrl by remember { mutableStateOf(currentSettings.supabaseUrl) }
  var supabaseAnonKey by remember { mutableStateOf(currentSettings.supabaseAnonKey) }
  var enableSupabaseSync by remember { mutableStateOf(currentSettings.enableSupabaseSync) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      try {
        val file = java.io.File(context.filesDir, "clinic_logo.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
          file.outputStream().use { output ->
            input.copyTo(output)
          }
        }
        clinicLogoUri = file.absolutePath
      } catch (e: Exception) {
        clinicLogoUri = uri.toString()
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
        .padding(horizontal = 8.dp, vertical = 12.dp),
      shape = RoundedCornerShape(14.dp),
      color = PureWhite,
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MedicalGreenDark)
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
                  .background(PureWhite),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Store, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  "ဆေးခန်း အချက်အလက်နှင့် သတ်မှတ်ချက်များ",
                  color = PureWhite,
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "Clinic Profile, Doctor SAMA & POS Printer Settings",
                  color = PureWhite.copy(alpha = 0.85f),
                  fontSize = 10.sp
                )
              }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
            }
          }
        }

        // Form Fields
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Section 1: Clinic Branding
          Text("၁။ ဆေးခန်း အမည်နှင့် Logo (Clinic Identity & Logo)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

          // Clinic Logo Upload & Preview Card
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
              Row(verticalAlignment = Alignment.CenterVertically) {
                ShweKhitLogo(
                  size = 52.dp,
                  customLogoUri = clinicLogoUri.ifBlank { null }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    "ဆေးခန်း Logo တံဆိပ်",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText
                  )
                  Text(
                    if (clinicLogoUri.isNotBlank()) "စိတ်ကြိုက် Logo အသုံးပြုထားပါသည်" else "မူလ “ရွှေခေတ်” Logo ကို အသုံးပြုထားပါသည်",
                    fontSize = 9.5.sp,
                    color = SlateMedium
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(34.dp),
                  shape = RoundedCornerShape(6.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
                ) {
                  Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Logo အသစ်ထည့်မည်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                if (clinicLogoUri.isNotBlank()) {
                  OutlinedButton(
                    onClick = { clinicLogoUri = "" },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("မူလ Logo", fontSize = 10.sp)
                  }
                }
              }
            }
          }

          OutlinedTextField(
            value = clinicNameMm,
            onValueChange = { clinicNameMm = it },
            label = { Text("ဆေးခန်း အမည် (မြန်မာ)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = clinicNameEn,
              onValueChange = { clinicNameEn = it },
              label = { Text("Clinic Name (English)") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
              value = clinicSubtitleMm,
              onValueChange = { clinicSubtitleMm = it },
              label = { Text("အမျိုးအစား / ဆေးခန်းခွဲ") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
          }

          OutlinedTextField(
            value = addressMm,
            onValueChange = { addressMm = it },
            label = { Text("ဆေးခန်း တည်နေရာ လိပ်စာ (Address)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = phonePrimary,
              onValueChange = { phonePrimary = it },
              label = { Text("ဖုန်းနံပါတ် ၁ (Primary)") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
              value = phoneSecondary,
              onValueChange = { phoneSecondary = it },
              label = { Text("ဖုန်းနံပါတ် ၂ (Secondary)") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = clinicHours,
              onValueChange = { clinicHours = it },
              label = { Text("ဆေးခန်းဖွင့်ချိန် (Operating Hours)") },
              placeholder = { Text("e.g. 8:00 AM – 8:00 PM") },
              modifier = Modifier.weight(1.1f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
              value = clinicRegNo,
              onValueChange = { clinicRegNo = it },
              label = { Text("မှတ်ပုံတင်အမှတ် (Reg No.)") },
              placeholder = { Text("e.g. MOHS-MDY-2024") },
              modifier = Modifier.weight(0.9f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
          }

          Divider(color = BorderGrey, thickness = 0.8.dp)

          // Section 2: In-charge Doctor Info
          Text("၂။ တာဝန်ခံဆရာဝန် အချက်အလက် (Doctor & SAMA)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = doctorName,
              onValueChange = { doctorName = it },
              label = { Text("ဆရာဝန် အမည်") },
              modifier = Modifier.weight(1.2f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
              value = doctorSama,
              onValueChange = { doctorSama = it },
              label = { Text("ဆမ အမှတ် (SAMA)") },
              modifier = Modifier.weight(0.8f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
          }

          OutlinedTextField(
            value = doctorDegree,
            onValueChange = { doctorDegree = it },
            label = { Text("ဘွဲ့နှင့် အထူးပြု (Degrees e.g. M.B.,B.S (MDY))") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
          )

          OutlinedTextField(
            value = defaultConsultationFee,
            onValueChange = { defaultConsultationFee = it.filter { ch -> ch.isDigit() } },
            label = { Text("အထွေထွေ ဆေးကုသခ အခြေခံနှုန်း (Ks)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
          )

          Spacer(modifier = Modifier.height(4.dp))
          Text("ဆေးထိုးခနှင့် ဝန်ဆောင်ခ ပုံသေနှုန်းထားများ (Default Injection Service Fees)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = defaultImInjectionFee,
              onValueChange = { defaultImInjectionFee = it.filter { ch -> ch.isDigit() } },
              label = { Text("အသားဆေးထိုးခ (IM Ks)", fontSize = 9.5.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
            )
            OutlinedTextField(
              value = defaultIvPushFee,
              onValueChange = { defaultIvPushFee = it.filter { ch -> ch.isDigit() } },
              label = { Text("အကြောဆေးထိုးခ (IV Push Ks)", fontSize = 9.5.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = defaultIvInfusionFee,
              onValueChange = { defaultIvInfusionFee = it.filter { ch -> ch.isDigit() } },
              label = { Text("ပုလင်းချိတ်ခ (IV Drip Ks)", fontSize = 9.5.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
            )
            OutlinedTextField(
              value = defaultNebulizerFee,
              onValueChange = { defaultNebulizerFee = it.filter { ch -> ch.isDigit() } },
              label = { Text("အငွေ့ရှူခ (Nebulizer Ks)", fontSize = 9.5.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
            )
          }

          OutlinedTextField(
            value = defaultDepoFee,
            onValueChange = { defaultDepoFee = it.filter { ch -> ch.isDigit() } },
            label = { Text("သားဆက်ခြား/အရေပြားအောက်ထိုးခ (Depo/SC Ks)", fontSize = 9.5.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
          )

          Divider(color = BorderGrey, thickness = 0.8.dp)

          // Section 3: Hardware & Printing Preferences
          Text("၃။ ဘောက်ချာ & အပူပေး ပရင်တာ သတ်မှတ်ချက် (POS Printing)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("အပူပေးစက္ကူ အရွယ်အစား (Paper Size):", fontSize = 11.sp, color = DarkGrayText)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = thermalPaperWidthMm == 58,
                onClick = { thermalPaperWidthMm = 58 },
                label = { Text("58 mm (Standard)", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SoftGreenContainer,
                  selectedLabelColor = MedicalGreenDark
                )
              )
              FilterChip(
                selected = thermalPaperWidthMm == 80,
                onClick = { thermalPaperWidthMm = 80 },
                label = { Text("80 mm (Wide)", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SoftGreenContainer,
                  selectedLabelColor = MedicalGreenDark
                )
              )
            }
          }

          OutlinedTextField(
            value = receiptGreetingMm,
            onValueChange = { receiptGreetingMm = it },
            label = { Text("ဘောက်ချာအောက်ခြေ နှုတ်ခွန်းဆက်စာသား") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
          )

          Surface(
            color = SoftGreenContainer,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Print, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "ဘလူးတု Thermal Printer ချိတ်ဆက်မှုများကို သီးသန့် Menu ဖြစ်သော 'XP-T361 Printer Setup' တွင် အလွယ်တကူ စီမံခန့်ခွဲနိုင်ပါသည်။",
                fontSize = 10.5.sp,
                color = MedicalGreenDark
              )
            }
          }

          Divider(color = BorderGrey, thickness = 0.8.dp)

          // Section 4: Multi-Device Wi-Fi & Supabase Online Database
          Text("၄။ တူညီသော Wi-Fi Sync & Supabase Cloud (Multi-Device Setup)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

          OutlinedTextField(
            value = activeStationName,
            onValueChange = { activeStationName = it },
            label = { Text("လက်ရှိ စက်အမျိုးအစား (Station Name)") },
            placeholder = { Text("e.g. Doctor Consultation Room, Reception, Cashier") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = wifiHubHostIp,
              onValueChange = { wifiHubHostIp = it },
              label = { Text("Wi-Fi Hub Host IP (ပင်မစက် IP)") },
              placeholder = { Text("e.g. 192.168.1.50") },
              modifier = Modifier.weight(1.3f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
              value = wifiSyncPort,
              onValueChange = { wifiSyncPort = it.filter { ch -> ch.isDigit() } },
              label = { Text("Port") },
              placeholder = { Text("8080") },
              modifier = Modifier.weight(0.7f),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Supabase Cloud Sync စနစ်", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              Text("Free-tier safe delta upload mode", fontSize = 9.5.sp, color = SlateMedium)
            }
            Switch(
              checked = enableSupabaseSync,
              onCheckedChange = { enableSupabaseSync = it }
            )
          }

          if (enableSupabaseSync) {
            OutlinedTextField(
              value = supabaseUrl,
              onValueChange = { supabaseUrl = it },
              label = { Text("Supabase Project URL") },
              placeholder = { Text("https://your-project.supabase.co") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
              value = supabaseAnonKey,
              onValueChange = { supabaseAnonKey = it },
              label = { Text("Supabase Anon / Public API Key") },
              placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            Surface(
              color = Color(0xFFEFF6FF),
              shape = RoundedCornerShape(6.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  "Supabase Free Tier သုံးစွဲသူများအတွက် Egress နှင့် Storage အကန့်အသတ်မကျော်လွန်စေရန် Delta Sync (ပြောင်းလဲမှုသီးသန့်) စနစ်ဖြင့် လုံခြုံစွာ ကာကွယ်ပေးထားပါသည်။",
                  fontSize = 10.sp,
                  color = Color(0xFF1E40AF)
                )
              }
            }
          }
        }

        // Bottom Actions
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = PureWhite,
          shadowElevation = 4.dp
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier
                .weight(1f)
                .height(38.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("မလုပ်တော့ပါ", fontSize = 11.sp)
            }

            Button(
              onClick = {
                val updated = currentSettings.copy(
                  clinicNameMm = clinicNameMm.trim().ifBlank { "ရွှေခေတ်" },
                  clinicNameEn = clinicNameEn.trim().ifBlank { "Shwe Khit Clinic" },
                  clinicSubtitleMm = clinicSubtitleMm.trim(),
                  doctorName = doctorName.trim().ifBlank { "Dr. Nyan Kaung Sett" },
                  doctorDegree = doctorDegree.trim(),
                  doctorSama = doctorSama.trim().ifBlank { "47414" },
                  phonePrimary = phonePrimary.trim(),
                  phoneSecondary = phoneSecondary.trim(),
                  clinicPhone = listOf(phonePrimary.trim(), phoneSecondary.trim()).filter { it.isNotBlank() }.joinToString(", "),
                  addressMm = addressMm.trim(),
                  clinicAddress = addressMm.trim(),
                  clinicHours = clinicHours.trim(),
                  clinicRegNo = clinicRegNo.trim(),
                  defaultConsultationFee = defaultConsultationFee.toDoubleOrNull() ?: 8000.0,
                  defaultImInjectionFee = defaultImInjectionFee.toDoubleOrNull() ?: 2000.0,
                  defaultIvPushFee = defaultIvPushFee.toDoubleOrNull() ?: 3000.0,
                  defaultIvInfusionFee = defaultIvInfusionFee.toDoubleOrNull() ?: 6000.0,
                  defaultNebulizerFee = defaultNebulizerFee.toDoubleOrNull() ?: 3000.0,
                  defaultDepoFee = defaultDepoFee.toDoubleOrNull() ?: 2500.0,
                  thermalPaperWidthMm = thermalPaperWidthMm,
                  bluetoothPrinterMac = currentSettings.bluetoothPrinterMac,
                  receiptGreetingMm = receiptGreetingMm.trim(),
                  clinicLogoUri = clinicLogoUri.trim(),
                  activeStationName = activeStationName.trim().ifBlank { "Doctor Consultation Station" },
                  wifiHubHostIp = wifiHubHostIp.trim(),
                  wifiSyncPort = wifiSyncPort.toIntOrNull() ?: 8080,
                  supabaseUrl = supabaseUrl.trim(),
                  supabaseAnonKey = supabaseAnonKey.trim(),
                  enableSupabaseSync = enableSupabaseSync
                )
                onSave(updated)
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              modifier = Modifier
                .weight(1.3f)
                .height(38.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("သိမ်းဆည်းမည် (Save Settings)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
