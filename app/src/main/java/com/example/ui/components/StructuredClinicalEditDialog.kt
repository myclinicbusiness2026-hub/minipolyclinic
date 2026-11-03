package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.model.ProcedureItem
import com.example.ui.theme.*
import java.util.UUID

/**
 * StructuredClinicalEditDialog
 *
 * Comprehensive, structured consultation record editor matching the doctor's consultation workflow:
 * Tab 0: History, Physical Examination & Provisional Diagnosis
 * Tab 1: Day-Care Injections & Prescriptions
 * Tab 2: Minor Procedures, Instructions, Follow-up & Consultation Fee
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StructuredClinicalEditDialog(
  record: ConsultationRecord,
  onDismiss: () -> Unit,
  onSave: (ConsultationRecord) -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) }

  // Tab 0 states
  var subjectiveText by remember { mutableStateOf(record.subjective) }
  var objectiveText by remember { mutableStateOf(record.objective.ifBlank { record.canvasNotes }) }
  var assessmentText by remember { mutableStateOf(record.assessment) }

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

  // Tab 1 states: Injections & Prescriptions
  val editableTreatmentOrders = remember {
    mutableStateListOf<String>().apply { addAll(record.treatmentOrderDetails) }
  }
  var showAddInjectionSheet by remember { mutableStateOf(false) }
  var newInjectionOrderInput by remember { mutableStateOf("") }

  val editablePrescriptions = remember {
    mutableStateListOf<PrescribedDrug>().apply { addAll(record.prescriptions) }
  }
  var showAddDrugSheet by remember { mutableStateOf(false) }
  var drugNameInput by remember { mutableStateOf("") }
  var drugDosageInput by remember { mutableStateOf("") }
  var drugFreqInput by remember { mutableStateOf("1 tab tds pc") }
  var drugDurationInput by remember { mutableStateOf("5 days") }
  var drugQtyInput by remember { mutableStateOf("10") }
  var drugIsExternal by remember { mutableStateOf(false) }
  var drugRemarksInput by remember { mutableStateOf("") }

  // Tab 2 states: Procedures, Advice, Follow-up, Fee
  val editableProcedures = remember {
    mutableStateListOf<ProcedureItem>().apply { addAll(record.selectedProcedures) }
  }
  var showAddProcedureSheet by remember { mutableStateOf(false) }
  var procedureNameInput by remember { mutableStateOf("") }
  var procedureFeeInput by remember { mutableStateOf("5000") }
  var procedureNotesInput by remember { mutableStateOf("") }

  var patientInstructionsText by remember { mutableStateOf(record.patientInstructions) }
  var followUpText by remember { mutableStateOf(record.followUpAppointment) }
  var planText by remember { mutableStateOf(record.plan) }
  var feeText by remember { mutableStateOf(record.consultationFee.toInt().toString()) }

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
                  "လူနာ: ${record.patientName} (${record.patientHn}) • ရက်စွဲ: ${record.timestamp}",
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
            text = { Text("၁။ ရာဇဝင်၊ စမ်းသပ်ချက် & ရောဂါအမည်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("၂။ ထိုးဆေး & သောက်ဆေးစာရင်း", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("၃။ ခွဲစိတ်ကုသမှု & ညွှန်ကြားချက်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
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
            // TAB 0: History, Physical Examination & Provisional Diagnosis
            0 -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text("ရောဂါရာဇဝင် & အဓိကခံစားရသော လက္ခဏာများ (Subjective / Chief Complaints):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = subjectiveText,
                  onValueChange = { subjectiveText = it },
                  placeholder = { Text("Chief complaints, HPI, Past medical history, Clinical remarks...", fontSize = 11.sp) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 180.dp),
                  singleLine = false,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("စမ်းသပ်တွေ့ရှိချက်များ (Objective Examination & Physical Findings):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = objectiveText,
                  onValueChange = { objectiveText = it },
                  placeholder = { Text("General condition, Systemic examination, Physical findings...", fontSize = 11.sp) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 180.dp),
                  singleLine = false,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
                )

                Spacer(modifier = Modifier.height(4.dp))
                // Diagnosis Section
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                  border = BorderStroke(0.8.dp, Color(0xFFA7F3D0)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text("ရောဂါအမည် သတ်မှတ်ချက်များ (Provisional Diagnoses - ${editableDiagnoses.size} ခု):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
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
                        placeholder = { Text("ရောဂါအမည် အသစ်ရိုက်ထည့်ပါ (e.g. Hypertension, GERD, Viral Fever)...", fontSize = 10.5.sp) },
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
              }
            }

            // TAB 1: Day-Care Injections & Prescriptions
            1 -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Section 1: Injections & Day-Care Treatment Orders
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                  border = BorderStroke(0.8.dp, Color(0xFFDDD6FE)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vaccines, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          "ဆေးထိုး & နေ့ခင်းကုသမှုအမိန့်များ (Treatment Orders - ${editableTreatmentOrders.size} ခု):",
                          fontSize = 11.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF6D28D9)
                        )
                      }
                      Button(
                        onClick = { showAddInjectionSheet = !showAddInjectionSheet },
                        colors = ButtonDefaults.buttonColors(containerColor = if (showAddInjectionSheet) SlateMedium else Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Icon(if (showAddInjectionSheet) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddInjectionSheet) "ပိတ်မည်" else "+ ဆေးထိုးအမိန့်ထည့်မည်", fontSize = 10.sp)
                      }
                    }

                    if (showAddInjectionSheet) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PureWhite,
                        border = BorderStroke(0.8.dp, Color(0xFFDDD6FE)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text("ဆေးထိုးကုသမှုအမိန့်အသစ်:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                          OutlinedTextField(
                            value = newInjectionOrderInput,
                            onValueChange = { newInjectionOrderInput = it },
                            placeholder = { Text("e.g. Inj. Ceftriaxone 1g IV stat, Inj. Tramadol 50mg IM...", fontSize = 10.5.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                          )
                          Button(
                            onClick = {
                              val clean = newInjectionOrderInput.trim()
                              if (clean.isNotBlank()) {
                                editableTreatmentOrders.add(clean)
                                newInjectionOrderInput = ""
                                showAddInjectionSheet = false
                              }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Text("ဆေးထိုးအမိန့်စာရင်းထဲသို့ ထည့်မည်", fontSize = 11.sp)
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (editableTreatmentOrders.isEmpty()) {
                      Text("ဆေးထိုး/ကုသမှုအမိန့် မရှိသေးပါ", fontSize = 10.5.sp, color = SlateMuted)
                    } else {
                      editableTreatmentOrders.forEachIndexed { idx, orderText ->
                        Surface(
                          color = PureWhite,
                          shape = RoundedCornerShape(4.dp),
                          border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
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
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                              Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(15.dp))
                              Spacer(modifier = Modifier.width(6.dp))
                              Text(
                                "${idx + 1}. $orderText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkGrayText
                              )
                            }
                            IconButton(
                              onClick = { editableTreatmentOrders.removeAt(idx) },
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

                // Section 2: Prescriptions
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
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ဆေးစာရင်း (Prescribed Medicines - ${editablePrescriptions.size} မျိုး):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                      }
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

            // TAB 2: Procedures, Advice, Follow-up & Consultation Fee
            2 -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Section 1: Minor Procedures
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                  border = BorderStroke(0.8.dp, Color(0xFFA7F3D0)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          "ခွဲစိတ်ကုသမှုများ (Minor Procedures - ${editableProcedures.size} ခု):",
                          fontSize = 11.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = MedicalGreenDark
                        )
                      }
                      Button(
                        onClick = { showAddProcedureSheet = !showAddProcedureSheet },
                        colors = ButtonDefaults.buttonColors(containerColor = if (showAddProcedureSheet) SlateMedium else MedicalGreenDark),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Icon(if (showAddProcedureSheet) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddProcedureSheet) "ပိတ်မည်" else "+ Procedure ထည့်မည်", fontSize = 10.sp)
                      }
                    }

                    if (showAddProcedureSheet) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PureWhite,
                        border = BorderStroke(0.8.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text("ခွဲစိတ်ကုသမှုအသစ် ထည့်သွင်းခြင်း:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                          OutlinedTextField(
                            value = procedureNameInput,
                            onValueChange = { procedureNameInput = it },
                            label = { Text("Procedure Name (e.g. Wound Dressing, Suturing, Abscess I&D)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                          )
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                              value = procedureFeeInput,
                              onValueChange = { procedureFeeInput = it },
                              label = { Text("ဝန်ဆောင်ခ / Fee (Ks)", fontSize = 10.sp) },
                              singleLine = true,
                              modifier = Modifier.weight(1f),
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                              value = procedureNotesInput,
                              onValueChange = { procedureNotesInput = it },
                              label = { Text("မှတ်ချက် (Optional)", fontSize = 10.sp) },
                              singleLine = true,
                              modifier = Modifier.weight(1f)
                            )
                          }
                          Button(
                            onClick = {
                              val cleanName = procedureNameInput.trim()
                              if (cleanName.isNotBlank()) {
                                val fee = procedureFeeInput.toDoubleOrNull() ?: 0.0
                                editableProcedures.add(
                                  ProcedureItem(
                                    id = UUID.randomUUID().toString(),
                                    name = cleanName,
                                    price = fee,
                                    standardFee = fee,
                                    notes = procedureNotesInput.trim()
                                  )
                                )
                                procedureNameInput = ""
                                procedureFeeInput = "5000"
                                procedureNotesInput = ""
                                showAddProcedureSheet = false
                              }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreenDark),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Text("Procedure စာရင်းထဲသို့ ထည့်မည်", fontSize = 11.sp)
                          }
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (editableProcedures.isEmpty()) {
                      Text("ခွဲစိတ်ကုသမှုမှတ်တမ်း မရှိသေးပါ", fontSize = 10.5.sp, color = SlateMuted)
                    } else {
                      editableProcedures.forEachIndexed { idx, proc ->
                        Surface(
                          color = PureWhite,
                          shape = RoundedCornerShape(4.dp),
                          border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
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
                              Text(
                                "${idx + 1}. ${proc.name} (${proc.standardFee.toInt()} Ks)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGrayText
                              )
                              if (proc.notes.isNotBlank()) {
                                Text("မှတ်ချက်: ${proc.notes}", fontSize = 9.5.sp, color = SlateMedium)
                              }
                              if (proc.consumableItems.isNotEmpty()) {
                                val consSummary = proc.consumableItems.joinToString(", ") { "${it.name} x${it.quantity}" }
                                Text("Consumables: $consSummary", fontSize = 9.sp, color = MedicalGreenDark)
                              }
                            }
                            IconButton(
                              onClick = { editableProcedures.removeAt(idx) },
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

                // Section 2: Patient Advice & Instructions
                Text("ဆရာဝန်၏ ညွှန်ကြားချက် / Patient Advice & Instructions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                OutlinedTextField(
                  value = patientInstructionsText,
                  onValueChange = { patientInstructionsText = it },
                  placeholder = { Text("အစားအသောက်၊ နေထိုင်မှုပုံစံနှင့် သတိပြုရန် ညွှန်ကြားချက်များ...", fontSize = 11.sp) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 150.dp),
                  singleLine = false,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default)
                )

                // Section 3: Follow-up Appointment & Plan
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

                // Section 4: Consultation Fee
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
                  treatmentOrderDetails = editableTreatmentOrders.toList(),
                  prescriptions = editablePrescriptions.toList(),
                  selectedProcedures = editableProcedures.toList(),
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
