package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Patient
import com.example.ui.theme.*

@Composable
fun PatientBannerCard(
  patient: Patient,
  modifier: Modifier = Modifier,
  onSaveVitals: () -> Unit = {},
  isEditingVitals: Boolean = false,
  onToggleEditVitals: () -> Unit = {},
  bpText: String = patient.vitals.bloodPressure,
  onBpChange: (String) -> Unit = {},
  pulseText: String = patient.vitals.pulseRate.toString(),
  onPulseChange: (String) -> Unit = {},
  tempText: String = patient.vitals.temperatureCelsius.toString(),
  onTempChange: (String) -> Unit = {},
  spo2Text: String = patient.vitals.oxygenSaturation.toString(),
  onSpo2Change: (String) -> Unit = {},
  weightText: String = patient.vitals.weightKg.toString(),
  onWeightChange: (String) -> Unit = {},
  rbsText: String = (patient.vitals.rbs ?: "").toString(),
  onRbsChange: (String) -> Unit = {},
  onOpenTrends: () -> Unit = {},
  onOpenVaccines: () -> Unit = {},
  onOpenAttachments: () -> Unit = {},
  attachmentsCount: Int = 0
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, BorderGray)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Header: Token #, Name, Demographics, Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MedicalGreen),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "#${patient.tokenNumber}",
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(patient.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkGrayText)
            Text("${patient.gender.label} • ${patient.age} နှစ် • ဖုန်း: ${patient.phone}", fontSize = 10.sp, color = SlateMedium)
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = SoftGreenContainer
        ) {
          Text(
            patient.status.myanmarName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreenDark,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      // Allergies Alert
      if (patient.knownAllergies.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = RedLight,
          border = BorderStroke(0.5.dp, RedAlert),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "မတည့်သောဆေးများ (Allergies): ${patient.knownAllergies.joinToString(", ")}",
              color = RedAlert,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Vitals Section (Read / Edit Mode)
      if (isEditingVitals) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          color = SoftLightGray
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("အရေးကြီးလက္ခဏာများ ပြင်ဆင်ရန် (Edit Vitals)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = bpText,
                onValueChange = onBpChange,
                label = { Text("BP", fontSize = 9.sp) },
                textStyle = TextStyle(fontSize = 10.sp),
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = pulseText,
                onValueChange = onPulseChange,
                label = { Text("PR", fontSize = 9.sp) },
                textStyle = TextStyle(fontSize = 10.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = tempText,
                onValueChange = onTempChange,
                label = { Text("Temp", fontSize = 9.sp) },
                textStyle = TextStyle(fontSize = 10.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = spo2Text,
                onValueChange = onSpo2Change,
                label = { Text("SpO2", fontSize = 9.sp) },
                textStyle = TextStyle(fontSize = 10.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = weightText,
                onValueChange = onWeightChange,
                label = { Text("Weight", fontSize = 9.sp) },
                textStyle = TextStyle(fontSize = 10.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = rbsText,
                onValueChange = onRbsChange,
                label = { Text("RBS", fontSize = 9.sp) },
                textStyle = TextStyle(fontSize = 10.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              OutlinedButton(
                onClick = onToggleEditVitals,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text("Cancel", fontSize = 10.sp)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = onSaveVitals,
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text("Save Vitals", fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        // Read-only Vitals Bar & Shortcut Action Chips
        val v = patient.vitals
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          color = SoftLightGray
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("BP: ${v.bloodPressure}", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              Text("PR: ${v.pulseRate}", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              Text("Temp: ${v.temperatureCelsius}°", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              Text("SpO2: ${v.oxygenSaturation}%", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              Text("Wt: ${v.weightKg}kg", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              if ((v.rbs ?: 0.0) > 0) {
                Text("RBS: ${v.rbs?.toInt()} mg/dL", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              }
            }

            IconButton(
              onClick = onToggleEditVitals,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Vitals", tint = SlateMedium, modifier = Modifier.size(14.dp))
            }
          }
        }
      }

      // Quick Shortcut Action Buttons: Trends, Vaccines, Attachments
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenTrends() },
          color = PureWhite,
          border = BorderStroke(0.8.dp, BorderGrey)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.ShowChart, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Trends", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
          }
        }

        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenVaccines() },
          color = PureWhite,
          border = BorderStroke(0.8.dp, BorderGrey)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Vaccines, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Vaccines", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
          }
        }

        Surface(
          modifier = Modifier
            .weight(1.1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onOpenAttachments() },
          color = PureWhite,
          border = BorderStroke(0.8.dp, BorderGrey)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFFC026D3), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              if (attachmentsCount > 0) "Scans ($attachmentsCount)" else "Attachments",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = SlateDark
            )
          }
        }
      }
    }
  }
}
