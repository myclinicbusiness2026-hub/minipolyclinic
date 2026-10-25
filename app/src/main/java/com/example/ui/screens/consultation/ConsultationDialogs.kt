package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.theme.*

@Composable
fun AddCustomComplaintDialog(
  onDismiss: () -> Unit,
  onAddComplaint: (name: String, duration: String, severity: String) -> Unit
) {
  var customName by remember { mutableStateOf("") }
  var customDuration by remember { mutableStateOf("2-3 days") }
  var customSeverity by remember { mutableStateOf("Moderate (++)") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AddCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Custom Complaint", fontSize = 14.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enter custom symptom or complaint:", fontSize = 10.5.sp, color = SlateDark)
        OutlinedTextField(
          value = customName,
          onValueChange = { customName = it },
          placeholder = { Text("e.g. Epistaxis, Knee pain, Hematuria, Toothache...", fontSize = 11.sp, color = SlateMuted) },
          modifier = Modifier.fillMaxWidth(),
          textStyle = TextStyle(fontSize = 11.5.sp, color = DarkGrayText),
          shape = RoundedCornerShape(8.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
          singleLine = true
        )

        Text("Initial Duration:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
          listOf("1 day", "2-3 days", "5 days", "1 week", "2 weeks", "> 1 month").forEach { dur ->
            val isSel = customDuration == dur
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isSel) MedicalGreen else Color(0xFFE2E8F0),
              modifier = Modifier.clickable { customDuration = dur }
            ) {
              Text(
                dur,
                fontSize = 9.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Text("Initial Severity:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf("Mild (+)", "Moderate (++)", "Severe (+++)").forEach { sev ->
            val isSel = customSeverity == sev
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isSel) Color(0xFF0288D1) else Color(0xFFE2E8F0),
              modifier = Modifier.clickable { customSeverity = sev }
            ) {
              Text(
                sev,
                fontSize = 9.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                color = if (isSel) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (customName.isNotBlank()) {
            onAddComplaint(customName.trim(), customDuration, customSeverity)
            onDismiss()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("Add to List", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", fontSize = 11.sp, color = SlateDark)
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomicalPinFindingDialog(
  pin: AnatomicalPin,
  currentFinding: String?,
  onDismiss: () -> Unit,
  onSaveFinding: (finding: String?) -> Unit
) {
  var selectedFindingText by remember { mutableStateOf(currentFinding ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("${pin.nameEn} Examination", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
    },
    text = {
      Column {
        Text("Quick Presets (1-Tap):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          pin.commonPresets.forEach { preset ->
            val isPicked = selectedFindingText == preset
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isPicked) SoftGreenContainer else Color(0xFFF1F5F9),
              border = BorderStroke(0.5.dp, if (isPicked) MedicalGreen else Color(0xFFCBD5E1)),
              modifier = Modifier.clickable {
                selectedFindingText = preset
              }
            ) {
              Text(
                preset,
                fontSize = 10.sp,
                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                color = if (isPicked) MedicalGreenDark else DarkGrayText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Custom Finding:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = selectedFindingText,
          onValueChange = { selectedFindingText = it },
          placeholder = { Text("e.g. Fine basal creps, Tender hepatomegaly...", fontSize = 11.sp, color = SlateMuted) },
          modifier = Modifier.fillMaxWidth(),
          textStyle = TextStyle(fontSize = 11.5.sp, color = DarkGrayText),
          shape = RoundedCornerShape(8.dp),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedFindingText.isNotBlank()) {
            onSaveFinding(selectedFindingText.trim())
          } else {
            onSaveFinding(null)
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("Save Finding", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!currentFinding.isNullOrBlank()) {
          TextButton(
            onClick = {
              onSaveFinding(null)
              onDismiss()
            }
          ) {
            Text("Clear", fontSize = 11.sp, color = Color(0xFFD32F2F))
          }
        }
        TextButton(onClick = onDismiss) {
          Text("Cancel", fontSize = 11.sp, color = SlateDark)
        }
      }
    }
  )
}

@Composable
fun FreeTextExamNoteDialog(
  currentNotes: String,
  onDismiss: () -> Unit,
  onApply: (notes: String) -> Unit
) {
  var customExamNote by remember { mutableStateOf(currentNotes) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Direct Examination Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Text("Edit or append free text physical examination notes:", fontSize = 11.sp, color = SlateMedium)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = customExamNote,
          onValueChange = { customExamNote = it },
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
          textStyle = TextStyle(fontSize = 11.sp, color = DarkGrayText),
          shape = RoundedCornerShape(8.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onApply(customExamNote)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
