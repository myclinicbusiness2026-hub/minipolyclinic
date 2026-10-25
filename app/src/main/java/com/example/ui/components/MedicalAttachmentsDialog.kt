package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.model.MedicalAttachment
import com.example.model.Patient
import com.example.ui.theme.*
import com.example.util.AttachmentHelper
import com.example.util.PdfRenderHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Medical Images & Diagnostic Document Attachments Dialog.
 * Clean, category-free, direct Photo / Camera / PDF attachment with visible file path.
 */
@Composable
fun MedicalAttachmentsDialog(
  patient: Patient,
  attachments: List<MedicalAttachment>,
  onDismiss: () -> Unit,
  onAddAttachment: (MedicalAttachment) -> Unit,
  onDeleteAttachment: (String) -> Unit
) {
  var showAddModal by remember { mutableStateOf(false) }
  var selectedPreviewAttachment by remember { mutableStateOf<MedicalAttachment?>(null) }

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
        // --- HEADER ---
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
                color = SkyBluePrimary,
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.AttachFile, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    "Medical Attachments (ဓါတ်မှန်၊ ရုပ်ပုံနှင့် စာရွက်စာတမ်းများ)",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF334155)
                  ) {
                    Text(
                      patient.hn,
                      color = Color(0xFF94A3B8),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
                Text(
                  "${patient.name} • Attached Diagnostic Files & Images",
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

        // --- SUBHEADER ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "Total Attached Files: ",
              fontSize = 11.sp,
              color = SlateDark,
              fontWeight = FontWeight.Medium
            )
            Text(
              "${attachments.size}",
              fontSize = 12.sp,
              color = OceanGreenPrimary,
              fontWeight = FontWeight.Bold
            )
          }

          Button(
            onClick = { showAddModal = true },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Attach Image / PDF", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
          }
        }

        // --- ATTACHMENTS LIST ---
        if (attachments.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.ImageNotSupported,
                contentDescription = null,
                tint = SlateMuted,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "No diagnostic reports or images attached yet.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMedium
              )
              Text(
                "Tap '+ Attach Image / PDF' to attach photo, camera capture, or PDF report.",
                fontSize = 10.5.sp,
                color = SlateMuted
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(attachments) { att ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedPreviewAttachment = att },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  // Top Row: Badge & Actions
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (att.fileType.uppercase()) {
                          "PDF" -> Color(0xFFDC2626)
                          "CAMERA" -> Color(0xFF2563EB)
                          else -> Color(0xFF059669)
                        }
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(
                            when (att.fileType.uppercase()) {
                              "PDF" -> Icons.Default.PictureAsPdf
                              "CAMERA" -> Icons.Default.PhotoCamera
                              else -> Icons.Default.Image
                            },
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(11.dp)
                          )
                          Spacer(modifier = Modifier.width(3.dp))
                          Text(
                            att.fileType.uppercase(),
                            color = PureWhite,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(8.dp))

                      Text(
                        att.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DarkGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    IconButton(
                      onClick = { onDeleteAttachment(att.id) },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(15.dp))
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  // Prominent File Path Display Box
                  Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          "Attached File Path (ဖိုင်လမ်းကြောင်း):",
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = SlateDark
                        )
                      }
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        if (att.filePath.isNotBlank()) att.filePath else (if (att.imageUri.isNotBlank()) att.imageUri else "No file path available"),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                      if (att.fileName.isNotBlank()) {
                        Text(
                          "File: ${att.fileName}",
                          fontSize = 8.5.sp,
                          color = SlateMedium
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      "Date: ${att.date}",
                      fontSize = 9.sp,
                      color = SlateMedium
                    )
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0xFFF1F5F9),
                      border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
                      modifier = Modifier.clickable { selectedPreviewAttachment = att }
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = OceanGreenPrimary, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("View / Preview (ကြည့်မည်)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = OceanGreenPrimary)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // --- FOOTER BUTTON ---
        Surface(
          color = PureWhite,
          tonalElevation = 3.dp,
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
              modifier = Modifier.height(34.dp)
            ) {
              Text("Done (ပြီးပါပြီ)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  // --- ADD MODAL ---
  if (showAddModal) {
    AddMedicalAttachmentModal(
      onDismiss = { showAddModal = false },
      onSave = { newAtt ->
        onAddAttachment(newAtt)
        showAddModal = false
      }
    )
  }

  // --- DETAIL PREVIEW MODAL ---
  selectedPreviewAttachment?.let { att ->
    val context = LocalContext.current
    var renderedPdfBitmap by remember(att) { mutableStateOf<Bitmap?>(null) }
    var isPdfLoading by remember(att) { mutableStateOf(false) }

    val isPdf = att.fileType.uppercase() == "PDF" ||
        att.fileName.endsWith(".pdf", ignoreCase = true) ||
        att.filePath.endsWith(".pdf", ignoreCase = true)

    LaunchedEffect(att) {
      if (isPdf && att.filePath.isNotBlank()) {
        isPdfLoading = true
        renderedPdfBitmap = PdfRenderHelper.renderPdfFirstPage(context, att.filePath)
        isPdfLoading = false
      } else {
        renderedPdfBitmap = null
        isPdfLoading = false
      }
    }

    Dialog(onDismissRequest = { selectedPreviewAttachment = null }) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.88f)
          .padding(8.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(att.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
              Text("Type: ${att.fileType} • Date: ${att.date}", fontSize = 9.5.sp, color = SlateMedium)
            }
            IconButton(onClick = { selectedPreviewAttachment = null }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Visual Document / Photo Preview Frame
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 220.dp, max = 340.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
              contentAlignment = Alignment.Center
            ) {
              if (isPdf) {
                if (isPdfLoading) {
                  CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(32.dp))
                } else if (renderedPdfBitmap != null) {
                  Image(
                    bitmap = renderedPdfBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                  )
                } else {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                  ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PDF Document Attached", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(att.fileName.ifBlank { "medical_report.pdf" }, color = Color(0xFF94A3B8), fontSize = 9.sp)
                  }
                }
              } else {
                // Photo / Camera / Image Attachment Preview using Coil
                val imageModel = when {
                  att.filePath.isNotBlank() && File(att.filePath).exists() -> File(att.filePath)
                  att.imageUri.isNotBlank() -> Uri.parse(att.imageUri)
                  else -> null
                }

                if (imageModel != null) {
                  AsyncImage(
                    model = imageModel,
                    contentDescription = att.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                  )
                } else {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                  ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Image Attachment", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(att.fileName.ifBlank { "Photo.jpg" }, color = Color(0xFF94A3B8), fontSize = 9.sp)
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Attached File Path High-Visibility Display
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  when (att.fileType.uppercase()) {
                    "PDF" -> Icons.Default.PictureAsPdf
                    "CAMERA" -> Icons.Default.PhotoCamera
                    else -> Icons.Default.Image
                  },
                  contentDescription = null,
                  tint = OceanGreenPrimary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Attached File Path (ဖိုင်တည်နေရာ လမ်းကြောင်း):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                if (att.filePath.isNotBlank()) att.filePath else (if (att.imageUri.isNotBlank()) att.imageUri else "No file path available"),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
              )
              if (att.fileName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("File Name: ${att.fileName}", fontSize = 9.sp, color = SlateMedium)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text("Clinical Interpretation & Notes:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Text(att.notes.ifBlank { "No special remarks noted." }, fontSize = 10.sp, color = SlateMedium)

          Spacer(modifier = Modifier.height(14.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
              onClick = { selectedPreviewAttachment = null },
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
            ) {
              Text("Close Preview", fontSize = 10.5.sp)
            }
          }
        }
      }
    }
  }
}

// --- MODAL TO ATTACH AN IMAGE OR DOCUMENT (PHOTO, CAMERA, PDF) ---
@Composable
private fun AddMedicalAttachmentModal(
  onDismiss: () -> Unit,
  onSave: (MedicalAttachment) -> Unit
) {
  val context = LocalContext.current
  val today = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

  var title by remember { mutableStateOf("") }
  var filePath by remember { mutableStateOf("") }
  var fileName by remember { mutableStateOf("") }
  var fileType by remember { mutableStateOf("PHOTO") } // "PHOTO", "CAMERA", "PDF"
  var notes by remember { mutableStateOf("") }
  var statusMessage by remember { mutableStateOf<String?>(null) }

  var tempCameraFile by remember { mutableStateOf<File?>(null) }
  var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

  // 1. High-Resolution Camera Launcher via FileProvider
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    if (success && tempCameraFile != null) {
      fileName = tempCameraFile!!.name
      filePath = tempCameraFile!!.absolutePath
      fileType = "CAMERA"
      if (title.isBlank()) {
        title = "Camera Capture (${tempCameraFile!!.name})"
      }
      statusMessage = "Photo captured successfully from camera"
    }
  }

  // Camera Runtime Permission Launcher
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      try {
        val (file, uri) = AttachmentHelper.createCameraTempUri(context)
        tempCameraFile = file
        tempCameraUri = uri
        cameraLauncher.launch(uri)
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Camera launch error: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
    }
  }

  val launchCameraSafely = {
    try {
      val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
      if (hasPermission) {
        val (file, uri) = AttachmentHelper.createCameraTempUri(context)
        tempCameraFile = file
        tempCameraUri = uri
        cameraLauncher.launch(uri)
      } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  // 2. Photo / Gallery Launcher
  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      val (origName, localPath) = AttachmentHelper.copyUriToLocalStorage(context, uri, "photo")
      fileName = origName
      filePath = localPath
      fileType = "PHOTO"
      if (title.isBlank()) {
        title = origName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
      }
      statusMessage = "Photo selected from gallery"
    }
  }

  // 3. PDF / Document Launcher
  val pdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      val (origName, localPath) = AttachmentHelper.copyUriToLocalStorage(context, uri, "pdf")
      fileName = origName
      filePath = localPath
      fileType = "PDF"
      if (title.isBlank()) {
        title = origName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
      }
      statusMessage = "PDF document attached"
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth().padding(10.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AttachFile, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Attach Medical Image / PDF Document", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
        }
        Text("ဓါတ်ပုံ၊ ကင်မရာ သို့မဟုတ် PDF စာရွက်စာတမ်း ပူးတွဲပါ", fontSize = 9.5.sp, color = SlateMedium)

        Spacer(modifier = Modifier.height(12.dp))

        // 3 Direct Buttons: Photo, Camera, PDF
        Text("Select Attachment Source (ဖိုင်ရွေးချယ်ရန်)", fontSize = 10.sp, color = SlateMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Camera
          Button(
            onClick = { launchCameraSafely() },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            modifier = Modifier.weight(1f).height(36.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          // Photo (Gallery)
          Button(
            onClick = { photoLauncher.launch("image/*") },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            modifier = Modifier.weight(1f).height(36.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          // PDF Document
          Button(
            onClick = { pdfLauncher.launch("application/pdf") },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            modifier = Modifier.weight(1f).height(36.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("PDF File", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominently Display Attached File Path
        if (filePath.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "File Selected (${fileType.uppercase()}): ${fileName.ifBlank { "Attached File" }}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  color = Color(0xFF166534)
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                "Path: $filePath",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Editable File Path field so user can also type/copy/verify path
        OutlinedTextField(
          value = filePath,
          onValueChange = { filePath = it },
          label = { Text("Attached File Path (ဖိုင်လမ်းကြောင်း) *", fontSize = 10.sp) },
          placeholder = { Text("e.g. /storage/emulated/0/... or /data/user/0/...", fontSize = 9.5.sp) },
          leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = SkyBluePrimary) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Document / Report Title (အမည်) *", fontSize = 10.sp) },
          placeholder = { Text("e.g. Chest X-Ray PA View, CBC Blood Test, Ultrasound", fontSize = 9.5.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Clinical Findings / Interpretation Note (တွေ့ရှိချက် မှတ်ချက်)", fontSize = 10.sp) },
          placeholder = { Text("e.g. Normal cardiothoracic ratio. No focal lesion.", fontSize = 9.5.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel", color = SlateMedium, fontSize = 11.sp) }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              val finalTitle = title.ifBlank {
                if (fileName.isNotBlank()) fileName else "Medical Attachment ($fileType)"
              }
              val finalPath = filePath.ifBlank { "/attachments/${System.currentTimeMillis()}_${fileType.lowercase()}" }
              onSave(
                MedicalAttachment(
                  title = finalTitle,
                  category = fileType,
                  date = today,
                  imageUri = finalPath,
                  filePath = finalPath,
                  fileType = fileType,
                  fileName = fileName.ifBlank { finalTitle },
                  notes = notes
                )
              )
            },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary)
          ) {
            Text("Save Attachment (ပူးတွဲချက် သိမ်းရန်)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
