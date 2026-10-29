package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ConsultationRecord
import com.example.model.PrescribedDrug
import com.example.ui.theme.*

/**
 * StructuredClinicalEditDialog
 *
 * Rich, structured consultation record editor matching the doctor's consultation workflow.
 * Allows editing diagnoses, prescriptions, clinical history, exam findings, and advice.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StructuredClinicalEditDialog(
  record: ConsultationRecord,
  onDismiss: () -> Unit,
  onSave: (ConsultationRecord) -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) } // 0 = Diagnoses & Rx, 1 = History & Exam, 2 = Advice & Fee

  var subjectiveText by remember { mutableStateOf(record.subjective) }
  var objectiveText by remember { mutableStateOf(record.objective.ifBlank { record.canvasNotes }) }
  var assessmentText by remember { mutableStateOf(record.assessment) }
  var planText by remember { mutableStateOf(record.plan) }
  var patientInstructionsText by remember { mutableStateOf(record.patientInstructions) }
  var followUpText by remember { mutableStateOf(record.followUpAppointment) }
  var feeText by remember { mutableStateOf(record.consultationFee.toInt().toString()) }

  val editableDiagnoses = remember {
    val initial = if (record.diagnosisList.isNotEmpty()) {
      record.diagnosisList.toMutableList()
    } else if (record.assessment.isNotBlank()) {
      record.assessment.split("•", ",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
    } else {
      mutableListOf<String>()
    }
    mutableStateListOf<String>().apply { addAll(initial) }
  }
  var newDiagnosisInput by remember { mutableStateOf("") }

  val editablePrescriptions = remember {
    mutableStateListOf<PrescribedDrug>().apply { addAll(record.prescriptions) }
  }

  // Quick Drug Add form state
  var showAddDrugSheet by remember { mutableStateOf(false) }
  var drugNameInput by remember { mutableStateOf("") }
  var drugDosageInput by remember { mutableStateOf("") }
  var drugFreqInput by remember { mutableStateOf("1 tab tds pc") }
  var drugDurationInput by remember { mutableStateOf("5 days") }
  var drugQtyInput by remember { mutableStateOf("10") }
  var drugIsExternal by remember { mutableStateOf(false) }
  var drugRemarksInput by remember { mutableStateOf("") }

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
      border = BorderStroke(1.dp, BorderGray)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Surface(
          color = Color(0xFFF8FAFC),
          border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF7E22CE)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.EditNote, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  "ဆေးမှတ်တမ်း ပြင်ဆင်ခြင်း (Structured Record Edit)",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = DarkGrayText
                )
                Text(
                  "လူနာ: ${record.patientName} • ရက်စွဲ: ${record.timestamp}",
                  fontSize = 10.sp,
                  color = SlateMedium
                )
              }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
            }
          }
        }

        // Section Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = PureWhite,
          contentColor = MedicalGreenDark
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("၁။ ရောဂါအမည် & ဆေးစာရင်း (Rx)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("၂။ ရာဇဝင် & စမ်းသပ်ချက် (H & E)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("၃။ ညွှန်ကြားချက် & ဆရာဝန်ခ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
          )
        }

        // Content Area
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(14.dp)
        ) {
          when (selectedTab) {
            0 -> {
              // TAB 0: Diagnoses & Prescriptions
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Diagnosis Section
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                  border = BorderStroke(0.8.dp, Color(0xFFA7F3D0)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text("ရောဂါအမည် သတ်မှတ်ချက်များ (Clinical Diagnoses):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (editableDiagnoses.isNotEmpty()) {
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        editableDiagnoses.forEach { diag ->
                          Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PureWhite,
                            border = BorderStroke(0.8.dp, MedicalGreenDark)
                          ) {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                              Text(diag, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                              Spacer(modifier = Modifier.width(4.dp))
                              Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                  .size(14.dp)
                                  .clickable { editableDiagnoses.remove(diag) },
                                tint = StatusCritical
                              )
                            }
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      OutlinedTextField(
                        value = newDiagnosisInput,
                        onValueChange = { newDiagnosisInput = it },
                        placeholder = { Text("ရောဂါအမည် အသစ်ရိုက်ထည့်ပါ (e.g. Hypertension, GERD)...", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                      )
                      Button(
                        onClick = {
                          val clean = newDiagnosisInput.trim()
                          if (clean.isNotBlank() && !editableDiagnoses.contains(clean)) {
                            editableDiagnoses.add(clean)
                            newDiagnosisInput = ""
                          }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp)
                      }
                    }
                  }
                }

                // Prescriptions Section
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = PureWhite),
                  border = BorderStroke(0.8.dp, BorderGray),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text("ဆေးစာရင်း (Prescribed Medicines - ${editablePrescriptions.size} မျိုး):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                      Button(
                        onClick = { showAddDrugSheet = !showAddDrugSheet },
                        colors = ButtonDefaults.buttonColors(containerColor = if (showAddDrugSheet) SlateMedium else SkyBluePrimary),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Icon(if (showAddDrugSheet) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddDrugSheet) "ပိတ်မည်" else "+ ဆေးထည့်မည်", fontSize = 10.sp)
                      }
                    }

                    if (showAddDrugSheet) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text("ဆေးသစ်ထည့်သွင်းခြင်း:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                          OutlinedTextField(
                            value = drugNameInput,
                            onValueChange = { drugNameInput = it },
                            label = { Text("ဆေးအမည် (Medicine Name)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                          )
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                              value = drugDosageInput,
                              onValueChange = { drugDosageInput = it },
                              label = { Text("Dosage (e.g. 500mg)", fontSize = 10.sp) },
                              singleLine = true,
                              modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                              value = drugFreqInput,
                              onValueChange = { drugFreqInput = it },
                              label = { Text("သောက်ပုံ (e.g. 1 tab tds)", fontSize = 10.sp) },
                              singleLine = true,
                              modifier = Modifier.weight(1f)
                            )
                          }
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                              value = drugDurationInput,
                              onValueChange = { drugDurationInput = it },
                              label = { Text("Duration (e.g. 5 days)", fontSize = 10.sp) },
                              singleLine = true,
                              modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                              value = drugQtyInput,
                              onValueChange = { drugQtyInput = it },
                              label = { Text("အရေအတွက် (Qty)", fontSize = 10.sp) },
                              singleLine = true,
                              modifier = Modifier.weight(1f)
                            )
                          }
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                              checked = drugIsExternal,
                              onCheckedChange = { drugIsExternal = it }
                            )
                            Text("ပြင်ပဝယ်ယူရန် (External Pharmacy)", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = AccentRedBrown)
                          }
                          OutlinedTextField(
                            value = drugRemarksInput,
                            onValueChange = { drugRemarksInput = it },
                            label = { Text("ညွှန်ကြားချက် / သောက်သုံးရန် မှတ်ချက်", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                          )
                          Button(
                            onClick = {
                              val cleanName = drugNameInput.trim()
                              if (cleanName.isNotBlank()) {
                                editablePrescriptions.add(
                                  PrescribedDrug(
                                    medicineId = "EXT-${System.currentTimeMillis()}",
                                    medicineName = cleanName,
                                    name = cleanName,
                                    dosage = drugDosageInput.trim(),
                                    frequency = drugFreqInput.trim(),
                                    duration = drugDurationInput.trim(),
                                    quantity = drugQtyInput.toIntOrNull() ?: 1,
                                    instructions = drugRemarksInput.trim(),
                                    isExternal = drugIsExternal
                                  )
                                )
                                drugNameInput = ""
                                drugDosageInput = ""
                                drugRemarksInput = ""
                                showAddDrugSheet = false
                              }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Text("ဆေးစာရင်းထဲသို့ ထည့်မည်", fontSize = 11.sp)
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (editablePrescriptions.isEmpty()) {
                      Text("ဆေးစာရင်း မရှိသေးပါ", fontSize = 10.5.sp, color = SlateMuted)
                    } else {
                      editablePrescriptions.forEachIndexed { idx, drug ->
                        Surface(
                          color = if (drug.isExternal) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                          shape = RoundedCornerShape(4.dp),
                          border = BorderStroke(0.6.dp, if (drug.isExternal) Color(0xFFFCD34D) else BorderGray),
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                        ) {
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Column(modifier = Modifier.weight(1f)) {
                              val medName = drug.effectiveName.ifBlank { drug.name.ifBlank { drug.medicineName } }
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                  "${idx + 1}. $medName ${if (drug.dosage.isNotBlank()) "(${drug.dosage})" else ""}",
                                  fontSize = 11.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = DarkGrayText
                                )
                                if (drug.isExternal) {
                                  Spacer(modifier = Modifier.width(6.dp))
                                  Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFEF3C7)
                                  ) {
                                    Text("ပြင်ပဝယ်ယူရန်", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                  }
                                }
                              }
                              Text(
                                "${drug.frequency} x ${drug.duration} • Qty: ${drug.quantity}${if (drug.instructions.isNotBlank()) " • [${drug.instructions}]" else ""}",
                                fontSize = 9.5.sp,
                                color = SlateMedium
                              )
                            }
                            IconButton(
                              onClick = { editablePrescriptions.removeAt(idx) },
                              modifier = Modifier.size(22.dp)
                            ) {
                              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(15.dp))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            1 -> {
              // TAB 1: Clinical History & Examination
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text("ရောဂါရာဇဝင် & မှတ်ချက်များ (Subjective Notes):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = subjectiveText,
                  onValueChange = { subjectiveText = it },
                  placeholder = { Text("Chief complaints, HPI, Past history, Clinical remarks...", fontSize = 11.sp) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp),
                  singleLine = false,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("စမ်းသပ်တွေ့ရှိချက်များ (Objective Examination):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = objectiveText,
                  onValueChange = { objectiveText = it },
                  placeholder = { Text("General condition, Systemic examination, Physical findings...", fontSize = 11.sp) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp),
                  singleLine = false,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
                )
              }
            }

            2 -> {
              // TAB 2: Advice, Follow-up & Consultation Fee
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text("ဆရာဝန်၏ ညွှန်ကြားချက် / Patient Advice & Instructions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = patientInstructionsText,
                  onValueChange = { patientInstructionsText = it },
                  placeholder = { Text("အစားအသောက်၊ နေထိုင်မှုပုံစံနှင့် သတိပြုရန် ညွှန်ကြားချက်များ...", fontSize = 11.sp) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 160.dp),
                  singleLine = false,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
                )

                Text("နောက်တစ်ကြိမ် ပြသရန်ရက်ချိန်း (Follow-up Appointment):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = followUpText,
                  onValueChange = { followUpText = it },
                  placeholder = { Text("e.g. 1 week later, PRN (မသက်သာမှ ပြန်လာရန်)...", fontSize = 11.sp) },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )

                Text("ကုသမှုအစီအစဉ် (Treatment Plan - Optional):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = planText,
                  onValueChange = { planText = it },
                  placeholder = { Text("Treatment plan details...", fontSize = 11.sp) },
                  modifier = Modifier.fillMaxWidth(),
                  minLines = 2,
                  maxLines = 3
                )

                Text("ဆရာဝန်ပြသခ / Consultation Fee (Ks):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = feeText,
                  onValueChange = { feeText = it },
                  placeholder = { Text("8000", fontSize = 11.sp) },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
              }
            }
          }
        }

        // Bottom Action Bar
        Surface(
          color = Color(0xFFF8FAFC),
          border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("မသိမ်းဘဲပိတ်မည်", fontSize = 11.sp)
            }

            Button(
              onClick = {
                val resolvedDiagList = editableDiagnoses.toList()
                val resolvedAssessment = if (resolvedDiagList.isNotEmpty()) {
                  resolvedDiagList.joinToString(" • ")
                } else {
                  assessmentText
                }
                val newFee = feeText.toDoubleOrNull() ?: record.consultationFee
                val updated = record.copy(
                  subjective = subjectiveText,
                  objective = objectiveText,
                  assessment = resolvedAssessment,
                  diagnosisList = resolvedDiagList,
                  prescriptions = editablePrescriptions.toList(),
                  plan = planText,
                  patientInstructions = patientInstructionsText,
                  followUpAppointment = followUpText,
                  consultationFee = newFee
                )
                onSave(updated)
              },
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("အပြောင်းအလဲများ အားလုံးသိမ်းဆည်းမည် (Save Changes)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
