package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.update.AppUpdateInfo
import com.example.service.update.UpdateState

@Composable
fun AppUpdateDialog(
  updateState: UpdateState,
  currentVersionName: String,
  onStartDownload: (AppUpdateInfo) -> Unit,
  onInstall: () -> Unit,
  onRetry: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isDownloading = updateState is UpdateState.Downloading

  Dialog(
    onDismissRequest = {
      if (!isDownloading) onDismiss()
    },
    properties = DialogProperties(
      dismissOnBackPress = !isDownloading,
      dismissOnClickOutside = !isDownloading,
      usePlatformDefaultWidth = false
    )
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp)
        .wrapContentHeight()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Top Header Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color(0xFF0F766E), // Emerald Teal
                  Color(0xFF0D9488),
                  Color(0xFF14B8A6)
                )
              )
            )
            .padding(24.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = "Update",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ဆော့ဖ်ဝဲလ် ဗားရှင်းအသစ် ရရှိပါပြီ",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Shwe Khit General Clinic (EMR & POS)",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
              )
            }
            if (!isDownloading) {
              IconButton(onClick = onDismiss) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = Color.White.copy(alpha = 0.8f)
                )
              }
            }
          }
        }

        // Body Content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          when (updateState) {
            is UpdateState.UpdateAvailable -> {
              val info = updateState.info

              // Version Badges
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.padding(2.dp)
                ) {
                  Text(
                    text = "လက်ရှိ: v$currentVersionName",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }

                Icon(
                  imageVector = Icons.Default.ArrowForward,
                  contentDescription = null,
                  tint = Color(0xFF0F766E),
                  modifier = Modifier.size(20.dp)
                )

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFF0F766E).copy(alpha = 0.15f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F766E))
                ) {
                  Text(
                    text = "အသစ်: v${info.versionName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }

              // Release Highlights Card
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = "ဗားရှင်းသစ် ပြုပြင်ပြောင်းလဲချက်များ (What's New):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = info.releaseNotesMm.ifBlank { info.releaseNotes },
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Text(
                text = "✓ မူလဆေးခန်းဒေတာများ၊ လူနာမှတ်တမ်းများနှင့် အကောင့်များ မပျက်စီးစေဘဲ တိုက်ရိုက် အဆင့်မြှင့်တင်ပေးမည်ဖြစ်ပါသည်။",
                fontSize = 12.sp,
                color = Color(0xFF047857),
                lineHeight = 18.sp
              )

              // Action Buttons
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedButton(
                  onClick = onDismiss,
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("နောက်မှ (Later)")
                }
                Button(
                  onClick = { onStartDownload(info) },
                  modifier = Modifier.weight(1.5f),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F766E)
                  )
                ) {
                  Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("အခုပဲ ဒေါင်းလုဒ်ရယူမည်", fontWeight = FontWeight.Bold)
                }
              }
            }

            is UpdateState.Downloading -> {
              val animatedProgress by animateFloatAsState(
                targetValue = (updateState.percent / 100f).coerceIn(0f, 1f),
                label = "progress"
              )

              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Text(
                  text = "ဗားရှင်းသစ်ကို ဒေါင်းလုဒ် ရယူနေပါသည်...",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium
                )

                // Linear Progress Bar
                LinearProgressIndicator(
                  progress = animatedProgress,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                  color = Color(0xFF0F766E),
                  trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Stats Row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "${updateState.percent}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                  )

                  Text(
                    text = String.format("%.1f MB / %.1f MB", updateState.downloadedMb, updateState.totalMb),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  Text(
                    text = String.format("%.1f MB/s", updateState.speedMbPerSec),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                Text(
                  text = "ဒေါင်းလုဒ်ပြီးဆုံးပါက အလိုအလျောက် တပ်ဆင်ရန် ဖွင့်လှစ်ပေးပါမည်။ ကျေးဇူးပြု၍ ခေတ္တစောင့်ဆိုင်းပေးပါ။",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }

            is UpdateState.ReadyToInstall -> {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF047857).copy(alpha = 0.15f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF047857),
                    modifier = Modifier.size(36.dp)
                  )
                }

                Text(
                  text = "ဒေါင်းလုဒ် ရယူပြီးစီးပါပြီ!",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF047857)
                )

                Text(
                  text = "အောက်ပါ 'အခုပဲ ထည့်သွင်းမည်' ခလုတ်ကို နှိပ်၍ အဆင့်မြှင့်တင်မှုကို အပြီးသတ်လိုက်ပါ။ (ဆေးခန်းဒေတာများ ပျောက်ဆုံးမှုမရှိပါ)",
                  fontSize = 13.sp,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                  onClick = onInstall,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF047857)
                  )
                ) {
                  Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "အခုပဲ ထည့်သွင်းမည် (Install Now)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            is UpdateState.Error -> {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(40.dp)
                )

                Text(
                  text = updateState.message,
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.error,
                  textAlign = TextAlign.Center
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Text("ပိတ်မည်")
                  }
                  Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Text("ပြန်ကြိုးစားမည်")
                  }
                }
              }
            }

            is UpdateState.UpToDate -> {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF047857),
                  modifier = Modifier.size(40.dp)
                )

                Text(
                  text = "နောက်ဆုံးပေါ် ဗားရှင်းကို အသုံးပြုနေပါသည် (v$currentVersionName)",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  textAlign = TextAlign.Center
                )

                Button(
                  onClick = onDismiss,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("ကောင်းပြီ")
                }
              }
            }

            else -> {}
          }
        }
      }
    }
  }
}
