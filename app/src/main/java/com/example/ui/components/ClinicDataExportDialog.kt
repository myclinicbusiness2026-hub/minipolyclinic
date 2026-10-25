package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ClinicDataPersistence
import com.example.model.*
import com.example.ui.theme.*

/**
 * Excel (CSV) & Comprehensive Data Backup Dialog (ဆေးခန်းစာရင်းများ Excel/CSV ဖြင့် ထုတ်ယူခြင်းနှင့် Backup စနစ်).
 */
@Composable
fun ClinicDataExportDialog(
  patients: List<Patient>,
  medicines: List<Medicine>,
  consultations: List<ConsultationRecord>,
  invoices: List<Invoice>,
  vaccines: List<VaccineRecord>,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var selectedExportType by remember { mutableStateOf(0) } // 0: Patients, 1: Pharmacy, 2: Consultations, 3: Invoices, 4: Full JSON Backup
  var copiedNotice by remember { mutableStateOf(false) }

  val csvContent: String = remember(selectedExportType, patients, medicines, consultations, invoices, vaccines) {
    when (selectedExportType) {
      0 -> ClinicDataPersistence.generatePatientsCsv(patients)
      1 -> ClinicDataPersistence.generateInventoryCsv(medicines)
      2 -> ClinicDataPersistence.generateConsultationsCsv(consultations)
      3 -> ClinicDataPersistence.generateInvoicesCsv(invoices)
      else -> ClinicDataPersistence.generateFullBackupJson(context, patients, medicines, consultations, invoices, vaccines)
    }
  }

  val exportFileName = when (selectedExportType) {
    0 -> "ShweKhit_Patients_${patients.size}.csv"
    1 -> "ShweKhit_Pharmacy_Stock_${medicines.size}.csv"
    2 -> "ShweKhit_Consultations_${consultations.size}.csv"
    3 -> "ShweKhit_Billing_Invoices_${invoices.size}.csv"
    else -> "ShweKhit_Clinic_FullBackup.json"
  }

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
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // --- DIALOG HEADER ---
        Surface(
          color = Color(0xFF1E293B),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = MedicalGreen,
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.FileDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "Export Data to Excel (CSV) & Cloud Backup",
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Text(
                  "ရွှေခေတ်ဆေးခန်း စာရင်းများ Excel ထုတ်ယူခြင်းနှင့် Backup",
                  color = Color(0xFFCBD5E1),
                  fontSize = 10.sp
                )
              }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite, modifier = Modifier.size(18.dp))
            }
          }
        }

        // --- EXPORT SELECTOR CHIPS ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val types = listOf(
            Triple(0, "Patients (${patients.size})", Icons.Default.People),
            Triple(1, "Pharmacy Stock (${medicines.size})", Icons.Default.Medication),
            Triple(2, "Consultations (${consultations.size})", Icons.Default.MedicalServices),
            Triple(3, "Billing Invoices (${invoices.size})", Icons.Default.ReceiptLong),
            Triple(4, "Full Backup (JSON)", Icons.Default.Backup)
          )

          types.forEach { (typeId, label, icon) ->
            val isSelected = selectedExportType == typeId
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) OceanGreenPrimary else PureWhite,
              border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFCBD5E1)),
              modifier = Modifier.clickable {
                selectedExportType = typeId
                copiedNotice = false
              }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  icon,
                  contentDescription = null,
                  tint = if (isSelected) PureWhite else SlateMedium,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  label,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) PureWhite else DarkGrayText
                )
              }
            }
          }
        }

        // --- PREVIEW BOX ---
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TableChart, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Export Preview ($exportFileName)",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = DarkGrayText
              )
            }

            if (copiedNotice) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFECFDF5)
              ) {
                Text("✓ Copied to clipboard!", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
          ) {
            val scrollHorizontal = rememberScrollState()
            val scrollVertical = rememberScrollState()

            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
            ) {
              Text(
                text = csvContent,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF334155),
                lineHeight = 13.sp,
                modifier = Modifier
                  .verticalScroll(scrollVertical)
                  .horizontalScroll(scrollHorizontal)
              )
            }
          }
        }

        // --- ACTION BUTTONS FOOTER ---
        Surface(
          color = PureWhite,
          tonalElevation = 3.dp,
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(csvContent))
                copiedNotice = true
              },
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(34.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copy CSV Text", fontSize = 10.5.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                  shareTextFile(context, csvContent, exportFileName)
                },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share / Open in Excel", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text("Close", fontSize = 10.5.sp)
              }
            }
          }
        }
      }
    }
  }
}

private fun shareTextFile(context: Context, content: String, fileName: String) {
  try {
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, content)
      putExtra(Intent.EXTRA_SUBJECT, fileName)
      type = if (fileName.endsWith(".json")) "application/json" else "text/csv"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export $fileName to:")
    context.startActivity(shareIntent)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}
