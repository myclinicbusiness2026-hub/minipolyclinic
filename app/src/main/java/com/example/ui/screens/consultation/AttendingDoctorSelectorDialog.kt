package com.example.ui.screens.consultation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.UserAccount
import com.example.ui.theme.*

/**
 * AttendingDoctorSelectorDialog
 * Enables quick switching between registered doctor accounts or entering a visiting doctor's credentials
 * so that A4 medical records and prescriptions correctly reflect the attending doctor.
 */
@Composable
fun AttendingDoctorSelectorDialog(
  currentDoctor: Triple<String, String, String>,
  doctorUsers: List<UserAccount>,
  defaultClinicDoctor: Triple<String, String, String>,
  onSelectDoctorAccount: (UserAccount) -> Unit,
  onSetCustomDoctor: (name: String, degree: String, sama: String) -> Unit,
  onResetToDefault: () -> Unit,
  onDismiss: () -> Unit
) {
  var customName by remember { mutableStateOf("") }
  var customDegree by remember { mutableStateOf("") }
  var customSama by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 500.dp)
        .padding(12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "တာဝန်ခံဆရာဝန် ရွေးချယ်/ပြောင်းလဲခြင်း",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              "A4 ဆေးမှတ်တမ်းတွင် ဖော်ပြမည့် ဆရာဝန်အချက်အလက်",
              fontSize = 11.sp,
              color = SlateMuted
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Currently Active Doctor Banner
        Surface(
          color = SoftIceBlue,
          shape = RoundedCornerShape(6.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, SkyBluePrimary.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("လက်ရှိ တာဝန်ကျဆရာဝန် (Current Active Doctor):", fontSize = 9.5.sp, color = SlateMedium)
              Text(
                "${currentDoctor.first} ${if (currentDoctor.second.isNotBlank()) currentDoctor.second else ""}".trim(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
              )
              if (currentDoctor.third.isNotBlank()) {
                Text("ဆမ: ${currentDoctor.third}", fontSize = 10.sp, color = MedicalGreen, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("စနစ်အတွင်း စာရင်းသွင်းထားသော ဆရာဝန်များ (Registered Doctors):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(6.dp))

        if (doctorUsers.isEmpty()) {
          Text("အခြားဆရာဝန် အကောင့်များ မရှိသေးပါ (Default Clinic Doctor အား အသုံးပြုနေပါသည်)", fontSize = 10.sp, color = SlateMuted)
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            doctorUsers.forEach { doc ->
              val isSelected = currentDoctor.first.equals(doc.fullName, ignoreCase = true)
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .clickable {
                    onSelectDoctorAccount(doc)
                    onDismiss()
                  },
                color = if (isSelected) OceanGreenLight else SoftLightGray,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, if (isSelected) MedicalGreen else BorderGray)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(doc.fullName, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = SlateDark)
                    Text("User: @${doc.username} • ဆမ-${doc.samaNumber ?: "မရှိသေးပါ"}", fontSize = 9.5.sp, color = SlateMuted)
                  }
                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
                  } else {
                    Text("ရွေးမည်", fontSize = 10.sp, color = MedicalGreen, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visiting / Substitute Doctor Section
        Text("ရံဖန်ရံခါ အစားထိုးဆရာဝန် အမည်ထည့်သွင်းရန် (Visiting / Cover Doctor):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = customName,
          onValueChange = { customName = it },
          label = { Text("ဆရာဝန်အမည် (Doctor Name)", fontSize = 10.sp) },
          placeholder = { Text("e.g. Dr. Kyaw Min Htun", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = customDegree,
            onValueChange = { customDegree = it },
            label = { Text("ဘွဲ့ (Degree)", fontSize = 10.sp) },
            placeholder = { Text("M.B.,B.S (YGN)", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = customSama,
            onValueChange = { customSama = it },
            label = { Text("ဆမနံပါတ် (SAMA No.)", fontSize = 10.sp) },
            placeholder = { Text("e.g. 51234", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = {
              onResetToDefault()
              onDismiss()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("မူလဆရာဝန်သို့ ပြန်ပြောင်းမည်", fontSize = 9.sp)
          }

          Button(
            onClick = {
              if (customName.isNotBlank()) {
                onSetCustomDoctor(customName, customDegree, customSama)
                onDismiss()
              }
            },
            enabled = customName.isNotBlank(),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
          ) {
            Text("အစားထိုး ဆရာဝန် သတ်မှတ်မည်", fontSize = 9.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
