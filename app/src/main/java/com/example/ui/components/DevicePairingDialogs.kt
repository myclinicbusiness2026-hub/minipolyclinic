package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.CompoundBarcodeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun generateQrCodeBitmap(text: String, size: Int = 512): Bitmap? {
  return try {
    val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
      for (y in 0 until height) {
        bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
      }
    }
    bitmap
  } catch (_: Exception) {
    null
  }
}

/**
 * Dialog shown on Device A (Admin) to export clinic settings and staff accounts
 * to Device B/C via QR Code or Pairing Token.
 */
@Composable
fun ExportDevicePairingDialog(
  viewModel: ClinicViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val pairingJson = remember { viewModel.generatePairingBundleJson() }
  var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isGenerating by remember { mutableStateOf(true) }

  LaunchedEffect(pairingJson) {
    withContext(Dispatchers.Default) {
      qrBitmap = generateQrCodeBitmap(pairingJson, 512)
      isGenerating = false
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      border = BorderStroke(1.dp, BorderGray)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("အခြားစက်သို့ အကောင့်များကူးယူရန်", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium, modifier = Modifier.size(16.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SoftGreenContainer,
          border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            "Device B သို့မဟုတ် C (Reception/Cashier) ၏ Login Screen တွင် '📷 QR ဖြင့် စက်ချိတ်မည်' ကို နှိပ်ပြီး ဤ QR ကို ပြပေးပါ သို့မဟုတ် Code ကို Copy ကူး၍ ထည့်သွင်းပါ",
            fontSize = 10.5.sp,
            color = MedicalGreenDark,
            modifier = Modifier.padding(8.dp),
            lineHeight = 14.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // QR Code Box
        Box(
          modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, BorderGray, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MedicalGreen)
          } else {
            qrBitmap?.let { bmp ->
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Pairing QR Code",
                modifier = Modifier
                  .size(210.dp)
                  .padding(4.dp)
              )
            } ?: Text("QR ကုဒ် မထုတ်နိုင်ပါ", fontSize = 11.sp, color = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Copy Pairing Token Button
        OutlinedButton(
          onClick = {
            clipboardManager.setText(AnnotatedString(pairingJson))
            Toast.makeText(context, "Pairing Code ကို Clipboard သို့ ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pairing Code ကို Copy ကူးယူမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("ပြီးပါပြီ (Done)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Dialog shown on Device B/C (Login Screen) to import clinic pairing data
 * and immediately activate staff accounts.
 */
@Composable
fun ImportDevicePairingDialog(
  viewModel: ClinicViewModel,
  onDismiss: () -> Unit,
  onImportSuccess: (String) -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()
  var pairingInput by remember { mutableStateOf("") }
  var isImporting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Camera Scan, 1 = Manual Paste

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
    if (!isGranted) {
      errorMessage = "Camera ခွင့်ပြုချက်မရရှိပါ။ အောက်ပါ Paste Code နည်းလမ်းဖြင့် ချိတ်ဆက်နိုင်ပါသည်။"
    }
  }

  var barcodeViewInstance by remember { mutableStateOf<CompoundBarcodeView?>(null) }
  var isTorchOn by remember { mutableStateOf(false) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE) {
        barcodeViewInstance?.pause()
      } else if (event == Lifecycle.Event.ON_RESUME) {
        if (hasCameraPermission && selectedTab == 0 && !isImporting) {
          barcodeViewInstance?.resume()
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      barcodeViewInstance?.pause()
    }
  }

  fun processPairingCode(code: String) {
    val cleanCode = code.trim()
    if (cleanCode.isBlank()) {
      errorMessage = "ကျေးဇူးပြု၍ Pairing Code ထည့်သွင်းပါ"
      return
    }
    isImporting = true
    errorMessage = null
    coroutineScope.launch {
      val (ok, msg) = viewModel.importPairingBundleJson(cleanCode)
      isImporting = false
      if (ok) {
        barcodeViewInstance?.pause()
        onImportSuccess(msg)
        onDismiss()
      } else {
        errorMessage = msg
        barcodeViewInstance?.resume()
      }
    }
  }

  Dialog(onDismissRequest = {
    barcodeViewInstance?.pause()
    onDismiss()
  }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      border = BorderStroke(1.dp, BorderGray)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Devices, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("စက်အချင်းချင်း ချိတ်ဆက်မည် (Pairing)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
          }
          IconButton(
            onClick = {
              barcodeViewInstance?.pause()
              onDismiss()
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(16.dp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: 0 = Camera Scan, 1 = Paste Code
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F5F9))
            .padding(3.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (selectedTab == 0) MedicalGreen else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clickable {
                selectedTab = 0
                errorMessage = null
                if (hasCameraPermission) barcodeViewInstance?.resume()
              }
              .padding(vertical = 6.dp),
            contentColor = if (selectedTab == 0) PureWhite else SlateMedium
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("📷 QR Scan ဖတ်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (selectedTab == 1) MedicalGreen else Color.Transparent,
            modifier = Modifier
              .weight(1f)
              .clickable {
                selectedTab = 1
                barcodeViewInstance?.pause()
                errorMessage = null
              }
              .padding(vertical = 6.dp),
            contentColor = if (selectedTab == 1) PureWhite else SlateMedium
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("📋 Code ကူးထည့်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        errorMessage?.let { err ->
          Surface(
            color = Color(0xFFFEE2E2),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFFF87171)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
          ) {
            Text(err, color = Color(0xFFDC2626), fontSize = 10.5.sp, modifier = Modifier.padding(8.dp))
          }
        }

        if (selectedTab == 0) {
          // ==================== MODE 0: EMBEDDED CAMERA QR SCANNER ====================
          if (!hasCameraPermission) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, BorderGray)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  "Admin စက်ရှိ QR Code ကို တိုက်ရိုက် Scan ဖတ်ရန် Camera ခွင့်ပြုချက် လိုအပ်ပါသည်",
                  fontSize = 11.sp,
                  color = SlateDark,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                  onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                  colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.height(36.dp)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Camera ဖွင့်ခွင့်ပြုမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          } else {
            // Live Viewfinder inside Dialog Card
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, MedicalGreen, RoundedCornerShape(8.dp))
                .background(Color.Black)
            ) {
              AndroidView(
                factory = { ctx ->
                  CompoundBarcodeView(ctx).apply {
                    barcodeViewInstance = this
                    setStatusText("")
                    decodeContinuous { result ->
                      val text = result.text
                      if (!text.isNullOrBlank() && !isImporting) {
                        pause()
                        processPairingCode(text)
                      }
                    }
                    resume()
                  }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view ->
                  view.pause()
                }
              )

              // Torch toggle button in top right
              IconButton(
                onClick = {
                  isTorchOn = !isTorchOn
                  if (isTorchOn) barcodeViewInstance?.setTorchOn() else barcodeViewInstance?.setTorchOff()
                },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .size(32.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(Color.Black.copy(alpha = 0.5f))
              ) {
                Icon(
                  if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                  contentDescription = "Torch",
                  tint = if (isTorchOn) Color(0xFFFACC15) else PureWhite,
                  modifier = Modifier.size(16.dp)
                )
              }

              if (isImporting) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PureWhite, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("စက်နှင့် ချိတ်ဆက်နေပါသည်...", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              "Admin စက်ရှိ QR Code ကို ကင်မရာရှေ့တည့်တည့် ထိုးပြပါ",
              fontSize = 10.5.sp,
              color = SlateMuted
            )
          }
        } else {
          // ==================== MODE 1: MANUAL PASTE CODE FALLBACK ====================
          Text(
            "Admin စက် (Device A) မှ ထုတ်ပေးသော Pairing Code ကို Paste လုပ်ပြီး ဆေးခန်းစနစ်နှင့် ချိတ်ဆက်နိုင်ပါသည်",
            fontSize = 10.5.sp,
            color = SlateMuted,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = pairingInput,
            onValueChange = {
              pairingInput = it
              errorMessage = null
            },
            label = { Text("Pairing Token / Code", fontSize = 11.sp) },
            placeholder = { Text("Paste code here...", fontSize = 11.sp) },
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            OutlinedButton(
              onClick = {
                val clip = clipboardManager.getText()?.text
                if (!clip.isNullOrBlank()) {
                  pairingInput = clip.trim()
                  errorMessage = null
                } else {
                  Toast.makeText(context, "Clipboard တွင် ကုဒ်မရှိပါ", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier
                .weight(1f)
                .height(36.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Paste Code", fontSize = 10.5.sp)
            }

            Button(
              onClick = { processPairingCode(pairingInput) },
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              modifier = Modifier
                .weight(1.2f)
                .height(36.dp),
              shape = RoundedCornerShape(6.dp),
              enabled = !isImporting
            ) {
              if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PureWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ချိတ်ဆက်နေ...", fontSize = 10.5.sp, color = PureWhite)
              } else {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("စက်ချိတ်မည် (Pair)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
