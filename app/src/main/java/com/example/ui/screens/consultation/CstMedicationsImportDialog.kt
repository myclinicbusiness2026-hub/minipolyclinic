package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Patient
import com.example.model.PrescribedDrug
import com.example.ui.theme.*
import java.util.UUID

data class CstCandidateItem(
  val id: String = UUID.randomUUID().toString(),
  var name: String,
  var dosage: String = "",
  var frequency: String = "1-0-0",
  var instructions: String = "မနက်စာစားပြီး",
  var isSelected: Boolean = true,
  val source: String = "Regular Meds" // "Regular Meds", "Past Visit", or "Manual"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CstMedicationsImportDialog(
  patient: Patient,
  subjectiveNotes: String,
  pastPrescriptions: List<PrescribedDrug>,
  onDismiss: () -> Unit,
  onImportCstDrugs: (List<PrescribedDrug>) -> Unit
) {
  // Extract regular medications from subjective notes
  val candidates = remember {
    val list = mutableStateListOf<CstCandidateItem>()
    val seenNames = mutableSetOf<String>()

    // 1. Parse from subjectiveNotes (Background Medical History: • Regular Daily Meds)
    val regLine = subjectiveNotes.lines().firstOrNull { it.contains("Regular Daily Meds") }
    if (regLine != null) {
      val items = regLine.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotBlank() }
      for (item in items) {
        val clean = item.replace(Regex("""^\d+\.\s*"""), "").trim()
        if (clean.isNotBlank() && seenNames.add(clean.lowercase())) {
          list.add(
            CstCandidateItem(
              name = clean,
              frequency = "1-0-0",
              instructions = "မနက်စာစားပြီး",
              isSelected = true,
              source = "ပုံမှန်သောက်ဆေး (EMR)"
            )
          )
        }
      }
    }

    // 2. Parse from past prescriptions (deduplicated)
    for (rx in pastPrescriptions) {
      val clean = rx.effectiveName.trim()
      if (clean.isNotBlank() && !clean.contains("CST") && !clean.contains("သောက်နေကြဆေး") && seenNames.add(clean.lowercase())) {
        list.add(
          CstCandidateItem(
            name = clean,
            dosage = rx.dosage,
            frequency = rx.frequency.ifBlank { "1-0-1" },
            instructions = rx.instructions.ifBlank { "စားပြီး" },
            isSelected = list.isEmpty(), // Select if no regular meds were found, else keep unselected
            source = "ယခင်ရက် ဆေးညွှန်း (Past Rx)"
          )
        )
      }
    }

    list
  }

  var newCustomMedName by remember { mutableStateOf("") }
  var newCustomFreq by remember { mutableStateOf("1-0-0") }
  var newCustomTiming by remember { mutableStateOf("မနက်စာစားပြီး") }
  var showAddManualRow by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.85f),
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      tonalElevation = 6.dp
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
            Box(
              modifier = Modifier
                .size(34.dp)
                .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "CST ဆေးများ ရွေးချယ် ထည့်သွင်းခြင်း",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = DarkGrayText
              )
              Text(
                "Continue Same Treatment - ပုံမှန်သောက်နေကြဆေးများ (Stock မနှုတ် / 0 Ks)",
                fontSize = 10.sp,
                color = SlateMuted
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info Banner
        Surface(
          color = Color(0xFFEFF6FF),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "လူနာ ${patient.name} ၏ မှတ်တမ်းတင်ထားသော ပုံမှန်သောက်ဆေးများ သို့မဟုတ် ယခင်ဆေးညွှန်းဟောင်းများမှ ရွေးချယ်နိုင်ပါသည်။ ဆေးသောက်ရမည့် အကြိမ်ရေကိုလည်း လိုအပ်သလို ပြင်ဆင်နိုင်ပါသည်။",
              fontSize = 10.sp,
              color = Color(0xFF1E40AF)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Candidate List
        if (candidates.isEmpty() && !showAddManualRow) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
              .border(0.8.dp, BorderGray, RoundedCornerShape(8.dp))
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = SlateLight, modifier = Modifier.size(36.dp))
              Text("ဤလူနာအတွက် မှတ်တမ်းတင်ထားသော ပုံမှန်သောက်ဆေး မရှိသေးပါ", fontSize = 11.5.sp, color = SlateMedium, fontWeight = FontWeight.SemiBold)
              Text("အောက်ပါခလုတ်မှတစ်ဆင့် ဆေးအမည် ရိုက်ထည့်နိုင်ပါသည် သို့မဟုတ် 'သောက်လက်စအတိုင်း ဆက်သောက်ရန်' သာ ထည့်သွင်းနိုင်ပါသည်", fontSize = 10.sp, color = SlateMuted)
              
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { showAddManualRow = true },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("ပုံမှန်သောက်ဆေး အသစ်ရိုက်ထည့်မည်", fontSize = 10.5.sp)
                }

                OutlinedButton(
                  onClick = {
                    onImportCstDrugs(
                      listOf(
                        PrescribedDrug(
                          medicineId = "EXT-CST",
                          medicineName = "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)",
                          name = "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)",
                          genericName = "",
                          dosage = "",
                          frequency = "ပုံမှန်အတိုင်း",
                          duration = "ပုံမှန်သောက်ရန်",
                          quantity = 0,
                          unitPrice = 0.0,
                          instructions = "သောက်လက်စသောက်ဆေးများ ပုံမှန်အတိုင်းဆက်သောက်ရန်",
                          isExternal = true,
                          isCst = true
                        )
                      )
                    )
                  },
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text("သောက်လက်စအတိုင်း ဆက်သောက်ရန် (General)", fontSize = 10.5.sp)
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            itemsIndexed(candidates) { idx, item ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (item.isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                border = BorderStroke(0.8.dp, if (item.isSelected) Color(0xFF86EFAC) else BorderGray),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Checkbox(
                      checked = item.isSelected,
                      onCheckedChange = { candidates[idx] = item.copy(isSelected = it) },
                      colors = CheckboxDefaults.colors(checkedColor = MedicalGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          item.name,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = DarkGrayText,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (item.source.contains("ပုံမှန်")) Color(0xFFDBEAFE) else Color(0xFFFEF3C7)
                        ) {
                          Text(
                            item.source,
                            fontSize = 8.5.sp,
                            color = if (item.source.contains("ပုံမှန်")) Color(0xFF1D4ED8) else Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                          )
                        }
                      }
                      if (item.dosage.isNotBlank()) {
                        Text("Strength: ${item.dosage}", fontSize = 9.5.sp, color = SlateMedium)
                      }
                    }
                  }

                  if (item.isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedTextField(
                        value = item.frequency,
                        onValueChange = { candidates[idx] = item.copy(frequency = it) },
                        label = { Text("ကြိမ်နှုန်း (Freq)", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
                      )
                      OutlinedTextField(
                        value = item.instructions,
                        onValueChange = { candidates[idx] = item.copy(instructions = it) },
                        label = { Text("သောက်နည်း (Meal Direction)", fontSize = 9.sp) },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
                      )
                    }

                    // Quick Frequency Chips
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, top = 4.dp),
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      listOf("1-0-0", "1-0-1", "0-0-1", "1-1-1", "လိုအပ်ပါက").forEach { f ->
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (item.frequency == f) MedicalGreen else BackgroundSurface,
                          border = BorderStroke(0.6.dp, BorderGrey),
                          modifier = Modifier.clickable { candidates[idx] = item.copy(frequency = f) }
                        ) {
                          Text(
                            f,
                            fontSize = 9.sp,
                            fontWeight = if (item.frequency == f) FontWeight.Bold else FontWeight.Normal,
                            color = if (item.frequency == f) PureWhite else DarkGrayText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            // Optional Manual Entry Row
            if (showAddManualRow) {
              item {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFF1F5F9),
                  border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                  modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ပုံမှန်သောက်ဆေး အသစ်ထည့်ရန်:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                    OutlinedTextField(
                      value = newCustomMedName,
                      onValueChange = { newCustomMedName = it },
                      label = { Text("ဆေးအမည် (e.g. Tab. Metformin 500mg)", fontSize = 10.sp) },
                      modifier = Modifier.fillMaxWidth(),
                      singleLine = true,
                      shape = RoundedCornerShape(4.dp),
                      textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      OutlinedTextField(
                        value = newCustomFreq,
                        onValueChange = { newCustomFreq = it },
                        label = { Text("ကြိမ်နှုန်း", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                      )
                      OutlinedTextField(
                        value = newCustomTiming,
                        onValueChange = { newCustomTiming = it },
                        label = { Text("သောက်နည်း", fontSize = 9.sp) },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                      )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                      TextButton(onClick = { showAddManualRow = false }) {
                        Text("Cancel", fontSize = 10.sp)
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Button(
                        onClick = {
                          if (newCustomMedName.isNotBlank()) {
                            candidates.add(
                              CstCandidateItem(
                                name = newCustomMedName.trim(),
                                frequency = newCustomFreq.trim().ifBlank { "1-0-0" },
                                instructions = newCustomTiming.trim().ifBlank { "မနက်စာစားပြီး" },
                                isSelected = true,
                                source = "စိတ်ကြိုက်ထည့် (Manual)"
                              )
                            )
                            newCustomMedName = ""
                            showAddManualRow = false
                          }
                        },
                        enabled = newCustomMedName.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
                      ) {
                        Text("Add to List", fontSize = 10.5.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Actions Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (!showAddManualRow && candidates.isNotEmpty()) {
            OutlinedButton(
              onClick = { showAddManualRow = true },
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(36.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ဆေးထပ်ထည့်မည်", fontSize = 10.5.sp)
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(36.dp)
            ) {
              Text("Cancel", fontSize = 11.sp)
            }

            val selectedItems = candidates.filter { it.isSelected }
            Button(
              onClick = {
                if (selectedItems.isNotEmpty()) {
                  val resultList = selectedItems.map { c ->
                    PrescribedDrug(
                      medicineId = "CST-${UUID.randomUUID()}",
                      medicineName = c.name,
                      name = c.name,
                      genericName = "",
                      dosage = c.dosage,
                      frequency = c.frequency.ifBlank { "ပုံမှန်အတိုင်း" },
                      duration = "ပုံမှန်သောက်ရန် (Continuous)",
                      quantity = 0,
                      unitPrice = 0.0,
                      instructions = c.instructions,
                      isExternal = true,
                      isCst = true
                    )
                  }
                  onImportCstDrugs(resultList)
                } else {
                  // Fallback to general CST
                  onImportCstDrugs(
                    listOf(
                      PrescribedDrug(
                        medicineId = "EXT-CST",
                        medicineName = "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)",
                        name = "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)",
                        genericName = "",
                        dosage = "",
                        frequency = "ပုံမှန်အတိုင်း",
                        duration = "ပုံမှန်သောက်ရန်",
                        quantity = 0,
                        unitPrice = 0.0,
                        instructions = "သောက်လက်စသောက်ဆေးများ ပုံမှန်အတိုင်းဆက်သောက်ရန်",
                        isExternal = true,
                        isCst = true
                      )
                    )
                  )
                }
              },
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
              modifier = Modifier.height(36.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (selectedItems.isNotEmpty()) "ဆေးညွှန်းထဲ ထည့်မည် (${selectedItems.size} မျိုး)" else "သောက်လက်စအတိုင်း ဆက်သောက်ရန် ထည့်မည်",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
