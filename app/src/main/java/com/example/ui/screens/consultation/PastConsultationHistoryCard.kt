package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConsultationRecord
import com.example.model.DrawStroke
import com.example.model.PrescribedDrug
import com.example.model.ProcedureItem
import com.example.ui.components.A4HandwritingNotesCanvasView
import com.example.ui.theme.*
import com.example.ui.components.FormattedClinicalSectionView

@Composable
fun PastConsultationHistoryCard(
  pastRecords: List<ConsultationRecord>,
  onRepeatPrescriptions: (List<PrescribedDrug>) -> Unit,
  onAddPrescription: ((PrescribedDrug) -> Unit)? = null,
  onCopySoap: ((ConsultationRecord) -> Unit)? = null,
  onCopyComplaint: ((String) -> Unit)? = null,
  onCopyClinicalNotes: ((String) -> Unit)? = null,
  onCopyDrawingStrokes: ((List<DrawStroke>) -> Unit)? = null,
  onAddDiagnosis: ((String) -> Unit)? = null,
  onAddAllDiagnoses: ((List<String>) -> Unit)? = null,
  onAddProcedure: ((ProcedureItem) -> Unit)? = null,
  onCopyPlan: ((String) -> Unit)? = null,
  onCopyInvestigations: ((List<String>) -> Unit)? = null,
  onViewA4Record: ((ConsultationRecord) -> Unit)? = null,
  onDeleteRecord: ((String) -> Unit)? = null,
  onEditRecord: ((ConsultationRecord) -> Unit)? = null
) {
  var isExpanded by remember(pastRecords) { mutableStateOf(false) }
  var recordToDelete by remember { mutableStateOf<ConsultationRecord?>(null) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.2.dp, Color(0xFF7E22CE).copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFFF3E8FF)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                "📋 ယခင် ဆေးမှတ်တမ်းဟောင်း (${pastRecords.size} ကြိမ်) ရှိပါသည်",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = Color(0xFF6B21A8)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFEDE9FE)
              ) {
                Text(
                  "Follow-up / Next Visit",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF6D28D9),
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
            }
            val lastRecord = pastRecords.firstOrNull()
            val lastDate = lastRecord?.timestamp ?: "ယခင်ရက်စွဲ"
            val lastDiag = lastRecord?.diagnosisList?.joinToString(", ")?.ifBlank { lastRecord.assessment.take(28) } ?: "အထွေထွေကုသမှု"
            Text(
              "နောက်ဆုံးပြသမှု: $lastDate • $lastDiag",
              fontSize = 9.sp,
              color = SlateMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dedicated Unhide / Hide Action Button
        OutlinedButton(
          onClick = { isExpanded = !isExpanded },
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, Color(0xFF7E22CE)),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isExpanded) Color(0xFFF3E8FF) else PureWhite
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Icon(
            if (isExpanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = null,
            tint = Color(0xFF7E22CE),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            if (isExpanded) "ဝှက်ထားမည် (Hide)" else "ကြည့်မည် (Unhide)",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7E22CE)
          )
        }
      }

      if (isExpanded) {
        Spacer(modifier = Modifier.height(8.dp))
        if (pastRecords.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFFFAF5FF), RoundedCornerShape(8.dp))
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("ယခင် ဆေးစစ်ချက်မှတ်တမ်း မရှိသေးပါ (No prior consultation records found)", fontSize = 11.sp, color = SlateMuted)
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pastRecords.forEachIndexed { index, record ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF5FF),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE))
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  // Visit & Doctor Identity Header
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        "Visit #${pastRecords.size - index} • ${record.timestamp}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF5B21B6)
                      )
                      Text(
                        "တာဝန်ခံဆရာဝန်: ${record.doctorName} (ဆမ-${record.doctorSama})",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MedicalGreenDark
                      )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      if (onDeleteRecord != null) {
                        IconButton(
                          onClick = { recordToDelete = record },
                          modifier = Modifier.size(28.dp)
                        ) {
                          Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Record", tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                        }
                      }
                      if (onEditRecord != null) {
                        OutlinedButton(
                          onClick = { onEditRecord(record) },
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                          modifier = Modifier.height(28.dp),
                          shape = RoundedCornerShape(6.dp),
                          border = BorderStroke(1.dp, Color(0xFF7E22CE))
                        ) {
                          Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(13.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text("ပြင်ဆင်မည်", fontSize = 9.5.sp, color = Color(0xFF7E22CE), fontWeight = FontWeight.Bold)
                        }
                      }
                      if (onViewA4Record != null) {
                        OutlinedButton(
                          onClick = { onViewA4Record(record) },
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                          modifier = Modifier.height(28.dp),
                          shape = RoundedCornerShape(6.dp),
                          border = BorderStroke(1.dp, MedicalGreen)
                        ) {
                          Icon(Icons.Default.Print, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(13.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text("A4 စာရွက်", fontSize = 9.5.sp, color = MedicalGreen, fontWeight = FontWeight.Bold)
                        }
                      }
                      if (record.prescriptions.isNotEmpty()) {
                        Button(
                          onClick = { onRepeatPrescriptions(record.prescriptions) },
                          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                          modifier = Modifier.height(28.dp),
                          shape = RoundedCornerShape(6.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                        ) {
                          Icon(Icons.Default.Repeat, contentDescription = null, tint = PureWhite, modifier = Modifier.size(12.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text("Repeat All Rx", fontSize = 9.5.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                  }

                  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFDDD6FE), thickness = 0.8.dp)

                  // 0. Vitals at that Visit (if recorded)
                  if (record.vitals != null) {
                    val v = record.vitals
                    val vitalParts = listOfNotNull(
                      if (v.bloodPressure.isNotBlank()) "BP: ${v.bloodPressure}" else null,
                      if (v.pulseRate > 0) "PR: ${v.pulseRate} bpm" else null,
                      if (v.temperatureCelsius > 0) "T: ${v.formattedTemperatureF}" else null,
                      if (v.oxygenSaturation > 0) "SpO2: ${v.oxygenSaturation}%" else null,
                      if (v.weightKg > 0) "Wt: ${v.weightKg} kg" else null,
                      if (v.rbs != null && v.rbs > 0) "RBS: ${v.rbs}" else null
                    )
                    if (vitalParts.isNotEmpty()) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text("Vitals at Visit:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                          Text(vitalParts.joinToString(" • "), fontSize = 9.sp, color = DarkGrayText)
                        }
                      }
                    }
                  }

                  // 1. Chief Complaint
                  if (record.subjective.isNotBlank()) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("Chief Complaint: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                        FormattedClinicalSectionView(rawText = record.subjective)
                      }
                      if (onCopyComplaint != null) {
                        TextButton(
                          onClick = { onCopyComplaint(record.subjective) },
                          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                          modifier = Modifier.height(22.dp)
                        ) {
                          Text("+ Copy", fontSize = 8.5.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                  }

                  // 2. Clinical Notes & Stylus Drawings
                  val typedNotes = record.objective.ifBlank { record.canvasNotes }
                  val hasStrokes = record.drawingPages.any { it.strokes.isNotEmpty() }
                  if (typedNotes.isNotBlank() || hasStrokes) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                      if (typedNotes.isNotBlank()) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.Top
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text("Clinical Notes:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                            Text(typedNotes, fontSize = 9.5.sp, color = DarkGrayText, modifier = Modifier.padding(top = 1.dp))
                          }
                          if (onCopyClinicalNotes != null) {
                            TextButton(
                              onClick = { onCopyClinicalNotes(typedNotes) },
                              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                              modifier = Modifier.height(22.dp)
                            ) {
                              Text("+ Copy Notes", fontSize = 8.5.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                      if (hasStrokes) {
                        val firstPage = record.drawingPages.first { it.strokes.isNotEmpty() }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text("✍️ Stylus Sketch & Anatomy Notes (${firstPage.strokes.size} strokes):", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))
                          if (onCopyDrawingStrokes != null) {
                            TextButton(
                              onClick = { onCopyDrawingStrokes(firstPage.strokes) },
                              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                              modifier = Modifier.height(22.dp)
                            ) {
                              Text("+ Copy Sketch to Canvas", fontSize = 8.5.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                        Box(
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PureWhite)
                            .border(0.8.dp, Color(0xFFBAE6FD), RoundedCornerShape(6.dp))
                        ) {
                          A4HandwritingNotesCanvasView(
                            page = firstPage,
                            modifier = Modifier.fillMaxSize()
                          )
                        }
                      }
                    }
                  }

                  // 3. Clinical Diagnosis with Individual "+ Use" & "+ Use All" buttons
                  if (record.diagnosisList.isNotEmpty() || record.assessment.isNotBlank()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                      if (record.diagnosisList.isNotEmpty()) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text("Clinical Diagnosis:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                          if (onAddAllDiagnoses != null && record.diagnosisList.size > 1) {
                            TextButton(
                              onClick = { onAddAllDiagnoses(record.diagnosisList) },
                              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                              modifier = Modifier.height(22.dp)
                            ) {
                              Text("+ Use All Diagnoses", fontSize = 8.5.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          record.diagnosisList.forEach { diag ->
                            Surface(
                              shape = RoundedCornerShape(4.dp),
                              color = Color(0xFFEDE9FE),
                              border = BorderStroke(0.6.dp, Color(0xFFC4B5FD))
                            ) {
                              Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Text(diag, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B21B6))
                                if (onAddDiagnosis != null) {
                                  Spacer(modifier = Modifier.width(4.dp))
                                  Text(
                                    "+ Use",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalGreen,
                                    modifier = Modifier.clickable { onAddDiagnosis(diag) }
                                  )
                                }
                              }
                            }
                          }
                        }
                      }
                      if (record.assessment.isNotBlank()) {
                        Text("Diagnosis Notes: ${record.assessment}", fontSize = 9.5.sp, color = DarkGrayText, modifier = Modifier.padding(top = 2.dp))
                      }
                    }
                  }

                  // 4. Procedures & Operative Notes
                  if (record.selectedProcedures.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ဆောင်ရွက်ခဲ့သော ကုသမှုများနှင့် မှတ်စုများ (Procedures & Operative Notes):", fontSize = 10.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                      verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                      record.selectedProcedures.forEach { proc ->
                        Surface(
                          color = PureWhite,
                          shape = RoundedCornerShape(4.dp),
                          border = BorderStroke(0.8.dp, Color(0xFFBBDEFB)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Row(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("• ${proc.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(3.dp), color = SkyBlueLight) {
                                  Text(proc.category, fontSize = 8.sp, color = SkyBlueDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                              }
                              if (onAddProcedure != null) {
                                OutlinedButton(
                                  onClick = { onAddProcedure(proc) },
                                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                  modifier = Modifier.height(24.dp),
                                  shape = RoundedCornerShape(4.dp)
                                ) {
                                  Text("+ ထပ်ထည့်မည်", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                }
                              }
                            }
                            if (proc.notes.isNotBlank()) {
                              Spacer(modifier = Modifier.height(2.dp))
                              Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(3.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Operative Note: ${proc.notes}", fontSize = 9.sp, color = SlateDark, modifier = Modifier.padding(4.dp))
                              }
                            }
                            if (proc.consumableItems.isNotEmpty()) {
                              Spacer(modifier = Modifier.height(2.dp))
                              Text("Consumables: ${proc.consumableItems.joinToString(", ") { "${it.name} (${it.quantity} ${it.unitType})" }}", fontSize = 8.5.sp, color = SlateMedium)
                            }
                          }
                        }
                      }
                    }
                  }

                  // 5. Diagnostic Investigations & Attached Documents
                  if (record.suggestedInvestigations.isNotEmpty() || record.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text("ဓါတ်ခွဲ/ဓါတ်မှန် စစ်ဆေးချက်များနှင့် ဖိုင်များ (Investigations & Files):", fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                      if (onCopyInvestigations != null && record.suggestedInvestigations.isNotEmpty()) {
                        TextButton(
                          onClick = { onCopyInvestigations(record.suggestedInvestigations) },
                          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                          modifier = Modifier.height(22.dp)
                        ) {
                          Text("+ Copy Orders", fontSize = 8.5.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                    if (record.suggestedInvestigations.isNotEmpty()) {
                      Text("စစ်ဆေးရန် ညွှန်းခဲ့သည်: ${record.suggestedInvestigations.joinToString(", ")}", fontSize = 9.5.sp, color = DarkGrayText, modifier = Modifier.padding(top = 1.dp))
                    }
                    if (record.attachments.isNotEmpty()) {
                      Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                      ) {
                        record.attachments.forEach { att ->
                          Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.8.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Row(
                              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Column(modifier = Modifier.weight(1f)) {
                                val icon = if (att.fileType.equals("PDF", true)) "📄" else "🖼️"
                                Text("$icon ${att.title}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                                val subText = listOfNotNull(
                                  if (att.date.isNotBlank()) att.date else null,
                                  if (att.notes.isNotBlank()) att.notes else null,
                                  if (att.fileName.isNotBlank()) "File: ${att.fileName}" else null
                                ).joinToString(" • ")
                                Text(subText, fontSize = 8.5.sp, color = SlateMedium)
                              }
                            }
                          }
                        }
                      }
                    }
                  }

                  // Day-Care Treatment & Injections
                  if (record.treatmentOrderDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ဆေးထိုး/အကြောဆေး အော်ဒါများ (Treatment & Injections):", fontSize = 10.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                      verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                      record.treatmentOrderDetails.forEach { orderText ->
                        Surface(
                          color = PureWhite,
                          shape = RoundedCornerShape(4.dp),
                          border = BorderStroke(0.8.dp, Color(0xFFBBF7D0)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text("💉 $orderText", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                          }
                        }
                      }
                    }
                  }

                  // 6. Prescriptions with Individual Add Option
                  if (record.prescriptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ပေးခဲ့သော ဆေးဝါးများ (Previous Medications):", fontSize = 10.sp, color = Color(0xFF5B21B6), fontWeight = FontWeight.Bold)
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                      verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                      record.prescriptions.forEach { rx ->
                        Surface(
                          color = PureWhite,
                          shape = RoundedCornerShape(4.dp),
                          border = BorderStroke(0.8.dp, Color(0xFFE9D5FF)),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Column(modifier = Modifier.weight(1f)) {
                              Text(
                                "• ${rx.name} (${rx.dosage})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGrayText
                              )
                              Text(
                                "${rx.frequency} • ${rx.duration} • Qty: ${rx.quantity} • ${rx.instructions}",
                                fontSize = 9.sp,
                                color = SlateMedium
                              )
                            }
                            if (onAddPrescription != null) {
                              OutlinedButton(
                                onClick = { onAddPrescription(rx) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                modifier = Modifier.height(24.dp),
                                shape = RoundedCornerShape(4.dp)
                              ) {
                                Text("+ ထည့်မည်", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreen)
                              }
                            }
                          }
                        }
                      }
                    }
                  }

                  // 7. Treatment Plan & Follow-up
                  if (record.plan.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text("Treatment Plan: ${record.plan}", fontSize = 9.5.sp, color = DarkGrayText, modifier = Modifier.weight(1f))
                      if (onCopyPlan != null) {
                        TextButton(
                          onClick = { onCopyPlan(record.plan) },
                          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                          modifier = Modifier.height(22.dp)
                        ) {
                          Text("+ Copy Plan", fontSize = 8.5.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                  }

                  if (record.followUpAppointment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      "ချိန်းဆိုချက်: ${record.followUpAppointment}",
                      fontSize = 9.5.sp,
                      color = Color(0xFF7E22CE),
                      fontWeight = FontWeight.Bold
                    )
                  }

                  // Bottom Action: Copy All to Today's EMR
                  if (onCopySoap != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.End
                    ) {
                      TextButton(
                        onClick = { onCopySoap(record) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                      ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ရောဂါရှာဖွေချက်နှင့် ကုသမှု ယနေ့မှတ်တမ်းသို့ အားလုံးကူးယူမည်", fontSize = 9.5.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Delete Consultation Record Confirmation Dialog
  if (recordToDelete != null) {
    AlertDialog(
      onDismissRequest = { recordToDelete = null },
      icon = {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp))
      },
      title = {
        Text("ဆေးစစ်ချက် မှတ်တမ်း ဖျက်ပစ်ရန် အတည်ပြုပါ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      },
      text = {
        Text(
          "လူနာ \"${recordToDelete?.patientName}\" ၏ ရက်စွဲ ${recordToDelete?.timestamp} ပါ ဆေးစစ်ချက်မှတ်တမ်းအား အမှန်တကယ် ဖျက်ပစ်ပါမည်လား?\n\n(ဖျက်ပြီးပါက ဤမှတ်တမ်းအား ပြန်လည်ရယူ၍ မရနိုင်ပါ)",
          fontSize = 11.sp,
          color = DarkGrayText,
          lineHeight = 16.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            recordToDelete?.let { r ->
              onDeleteRecord?.invoke(r.id)
            }
            recordToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("ဖျက်မည် (Delete)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { recordToDelete = null },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("မဖျက်တော့ပါ (Cancel)", fontSize = 11.sp)
        }
      }
    )
  }
}
