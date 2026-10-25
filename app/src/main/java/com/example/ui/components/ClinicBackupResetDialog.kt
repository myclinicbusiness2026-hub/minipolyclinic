package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.ClinicBackupHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicBackupResetDialog(
  onDismiss: () -> Unit,
  onExportBackup: () -> String,
  onRestoreBackup: (String) -> Boolean,
  onFactoryReset: (Boolean) -> Unit
) {
  val context = LocalContext.current
  var activeTab by remember { mutableStateOf(0) } // 0 = Backup, 1 = Restore, 2 = Reset
  var exportedJson by remember { mutableStateOf("") }
  var restoreInputJson by remember { mutableStateOf("") }
  var showResetConfirmDialog by remember { mutableStateOf(false) }
  var resetMedicinesAlso by remember { mutableStateOf(false) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }

  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      val content = ClinicBackupHelper.readBackupFileFromUri(context, uri)
      if (content != null) {
        restoreInputJson = content
        statusMessage = "Backup ဖိုင်ကို အောင်မြင်စွာ ဖတ်ရှုပြီးပါပြီ (${content.length} characters)"
        isError = false
      } else {
        statusMessage = "Backup ဖိုင်ဖတ်မရပါ"
        isError = true
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
        .fillMaxHeight(0.90f)
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
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  "ဒေတာ သိမ်းဆည်းခြင်း & ပြန်တင်ခြင်း",
                  color = PureWhite,
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "Offline Database Backup, Restore & Launch Reset",
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

        // Tabs
        TabRow(
          selectedTabIndex = activeTab,
          containerColor = PureWhite,
          contentColor = MedicalGreenDark
        ) {
          Tab(
            selected = activeTab == 0,
            onClick = { activeTab = 0 },
            text = { Text("၁။ ဒေတာ သိမ်းမည် (Export)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = activeTab == 1,
            onClick = { activeTab = 1 },
            text = { Text("၂။ ပြန်လည်သွင်းမည် (Restore)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = activeTab == 2,
            onClick = { activeTab = 2 },
            text = { Text("၃။ ဆေးခန်းဖွင့် စာရင်းရှင်း (Reset)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MahoganyAccent) }
          )
        }

        if (statusMessage != null) {
          Surface(
            color = if (isError) StatusCriticalContainer else SoftGreenContainer,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) StatusCritical else MedicalGreenDark,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(statusMessage!!, fontSize = 11.sp, color = if (isError) StatusCritical else MedicalGreenDark)
            }
          }
        }

        // Tab Content
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(14.dp)
        ) {
          when (activeTab) {
            0 -> {
              // Backup Tab
              Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text(
                  "ဆေးခန်းရှိ လူနာစာရင်းများ၊ ဆေးမှတ်တမ်းများ၊ ဆေးဝါးစတော့နှင့် ငွေစာရင်းအားလုံးကို JSON Backup ဖိုင်အဖြစ် လုံခြုံစွာ ထုတ်ယူသိမ်းဆည်းပါမည်။",
                  fontSize = 11.sp,
                  color = DarkGrayText
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      val json = onExportBackup()
                      exportedJson = json
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      val clip = ClipData.newPlainText("Clinic_Backup", json)
                      clipboard.setPrimaryClip(clip)
                      statusMessage = "Backup JSON Generated & Copied to Clipboard (${json.length} characters)!"
                      isError = false
                      Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clipboard သို့ကူးမည်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = {
                      val json = if (exportedJson.isNotBlank()) exportedJson else onExportBackup()
                      exportedJson = json
                      val ok = ClinicBackupHelper.shareBackupPayload(context, json)
                      if (ok) {
                        statusMessage = "Backup ဖိုင်အား သိမ်းဆည်းပြီး မျှဝေရန် အဆင်သင့်ဖြစ်ပါသည်"
                        isError = false
                      } else {
                        statusMessage = "Backup ဖိုင် ဖန်တီး၍ မရပါ"
                        isError = true
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ဖိုင်အဖြစ်ပေးပို့မည် (Share)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                  }
                }

                if (exportedJson.isNotBlank()) {
                  OutlinedTextField(
                    value = exportedJson.take(1500) + if (exportedJson.length > 1500) "\n... [${exportedJson.length} bytes total]" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Backup Data Preview (JSON)") },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                  )
                }
              }
            }

            1 -> {
              // Restore Tab
              Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text(
                  "ယခင်ထုတ်ယူထားသော JSON Backup စာသား သို့မဟုတ် ဖုန်းထဲရှိ Backup ဖိုင် (.json) ကို ရွေးချယ်၍ ဆေးခန်းဒေတာများ အားလုံးကို မူလအတိုင်း ပြန်လည်ရယူနိုင်ပါသည်။",
                  fontSize = 11.sp,
                  color = DarkGrayText
                )

                OutlinedTextField(
                  value = restoreInputJson,
                  onValueChange = { restoreInputJson = it },
                  label = { Text("Paste Backup JSON String Here") },
                  placeholder = { Text("{\"version\":1,\"timestamp\":\"...\",\"data\":{...}}") },
                  modifier = Modifier.fillMaxWidth().height(180.dp),
                  textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = { filePickerLauncher.launch("application/json") },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF0288D1))
                  ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ဖိုင်ရွေးမည် (Pick File)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                  }

                  Button(
                    onClick = {
                      if (restoreInputJson.isBlank()) {
                        statusMessage = "ကျေးဇူးပြု၍ Backup JSON ထည့်သွင်းပါ သို့မဟုတ် ဖိုင်ရွေးပါ"
                        isError = true
                        return@Button
                      }
                      val success = onRestoreBackup(restoreInputJson.trim())
                      if (success) {
                        statusMessage = "ဒေတာများအားလုံး အောင်မြင်စွာ ပြန်လည်ရယူပြီးပါပြီ (Database Restored Successfully)!"
                        isError = false
                        Toast.makeText(context, "Database restored!", Toast.LENGTH_SHORT).show()
                      } else {
                        statusMessage = "Backup JSON ဖတ်မရပါ၊ ဖော်မတ်မှားယွင်းနေပါသည်"
                        isError = true
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ပြန်လည်သွင်းမည် (Restore)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            2 -> {
              // Factory Reset Tab
              Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  color = StatusCriticalContainer,
                  shape = RoundedCornerShape(8.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, StatusCritical.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("သတိပေးချက် - ဆေးခန်းဖွင့် စာရင်းရှင်းလင်းခြင်း", fontWeight = FontWeight.Bold, color = StatusCritical, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      "ဆေးခန်းကို တကယ်စတင်ဖွင့်လှစ် အသုံးပြုတော့မည်ဆိုပါက စမ်းသပ်ထည့်သွင်းထားသော စမ်းသပ်လူနာစာရင်းများ၊ စမ်းသပ်ဘောက်ချာများ၊ ဆေးစစ်ချက်များနှင့် Dashboard စာရင်းဇယားအားလုံးကို အစအဆုံး သန့်ရှင်း သွားစေရန် ရှင်းလင်းပေးနိုင်ပါသည်။\n\nရှင်းလင်းပြီးပါက လူနာအသစ်၊ Dashboard အသစ်မှ သန့်ရှင်းစွာ စတင်နိုင်မည် ဖြစ်သည်။",
                      fontSize = 11.sp,
                      color = DarkGrayText,
                      lineHeight = 16.sp
                    )
                  }
                }

                // Pharmacy Reset Checkbox Option
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFF8FAFC),
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { resetMedicinesAlso = !resetMedicinesAlso }
                      .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Checkbox(
                      checked = resetMedicinesAlso,
                      onCheckedChange = { resetMedicinesAlso = it },
                      colors = CheckboxDefaults.colors(checkedColor = MahoganyAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text("ဆေးဝါး စတော့စာရင်း (Pharmacy Inventory) ပါ အကုန်ရှင်းလင်းမည်", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkGrayText)
                      Text(
                        if (resetMedicinesAlso) "ဆေးဝါးနှင့် စတော့စာရင်းများအားလုံးပါ ရှင်းလင်းပြီး သုညမှ အသစ်စတင်ပါမည်"
                        else "အမှန်ခြစ်မထားပါက ဆေးဝါးစာရင်းကို ထိန်းသိမ်းထားပြီး လူနာ၊ ဘောက်ချာနှင့် Dashboard စာရင်းများကိုသာ ရှင်းပါမည်",
                        fontSize = 9.5.sp,
                        color = SlateMedium
                      )
                    }
                  }
                }

                Button(
                  onClick = { showResetConfirmDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = MahoganyAccent),
                  modifier = Modifier.fillMaxWidth().height(42.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(if (resetMedicinesAlso) "အကုန်လုံးရှင်းလင်းမည် (Reset All Data & Pharmacy)" else "စမ်းသပ်လူနာနှင့် စာရင်းဇယားများ ရှင်းလင်းမည် (Reset Demo Data)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }

  // Confirmation Alert Dialog
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(36.dp)) },
      title = { Text("ဒေတာများ ရှင်းလင်းရန် သေချာပါသလား?", fontWeight = FontWeight.Bold, fontSize = 13.5.sp) },
      text = {
        Text(
          if (resetMedicinesAlso)
            "လူနာမှတ်ပုံတင်စာရင်း၊ ဆေးစစ်ချက်၊ ဘောက်ချာ၊ Dashboard နှင့် ဆေးဝါးစတော့စာရင်း (Pharmacy) အားလုံး ရှင်းလင်းသွားမည် ဖြစ်သည်။"
          else
            "လူနာမှတ်ပုံတင်စာရင်း၊ ဆေးစစ်ချက်မှတ်တမ်း၊ ဘောက်ချာများနှင့် Dashboard စာရင်းအားလုံး သန့်ရှင်းသွားမည်ဖြစ်ပြီး ဆေးဝါးစတော့စာရင်းကို ထိန်းသိမ်းထားပါမည်။",
          fontSize = 11.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showResetConfirmDialog = false
            onFactoryReset(resetMedicinesAlso)
            statusMessage = if (resetMedicinesAlso)
              "ဒေတာများနှင့် ဆေးဝါးစတော့များအားလုံး အောင်မြင်စွာ ရှင်းလင်းပြီးပါပြီ။"
            else
              "စမ်းသပ်ဒေတာများနှင့် Dashboard စာရင်းများ အားလုံးရှင်းလင်းပြီးပါပြီ။"
            isError = false
            Toast.makeText(context, "Clinic data reset for launch", Toast.LENGTH_LONG).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
        ) {
          Text("ရှင်းလင်းမည် (Confirm Reset)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showResetConfirmDialog = false }) {
          Text("မလုပ်တော့ပါ", fontSize = 11.sp)
        }
      }
    )
  }
}
