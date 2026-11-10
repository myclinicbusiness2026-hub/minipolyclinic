package com.example.ui.screens.lab

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.LabInitialDataSeeder
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.AttachmentHelper
import com.example.util.BurmeseDateUtils
import com.example.util.LabPrintHelper
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabInvestigationScreen(
  viewModel: ClinicViewModel,
  onNavigateBack: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val labOrders by viewModel.labOrders.collectAsState()
  val labTemplates by viewModel.labTemplates.collectAsState()
  val labInvoices by viewModel.labInvoices.collectAsState()
  val clinicSettings by viewModel.clinicSettings.collectAsState()
  val allPatients by viewModel.patients.collectAsState()

  var selectedTab by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }

  // Modals state
  var showNewOrderDialog by remember { mutableStateOf(false) }
  var orderToEnterResults by remember { mutableStateOf<LabOrder?>(null) }
  var orderToInvoice by remember { mutableStateOf<LabOrder?>(null) }
  var templateToEdit by remember { mutableStateOf<LabTestTemplate?>(null) }
  var isCreatingTemplate by remember { mutableStateOf(false) }

  // Filtered orders
  val queueOrders = remember(labOrders, searchQuery) {
    labOrders.filter { it.status == LabOrderStatus.ORDERED || it.status == LabOrderStatus.SAMPLE_COLLECTED }
      .filter {
        searchQuery.isBlank() ||
          it.patientName.contains(searchQuery, ignoreCase = true) ||
          it.patientHn.contains(searchQuery, ignoreCase = true) ||
          it.orderNumber.contains(searchQuery, ignoreCase = true)
      }
  }

  val completedOrders = remember(labOrders, searchQuery) {
    labOrders.filter { it.status == LabOrderStatus.COMPLETED }
      .filter {
        searchQuery.isBlank() ||
          it.patientName.contains(searchQuery, ignoreCase = true) ||
          it.patientHn.contains(searchQuery, ignoreCase = true) ||
          it.orderNumber.contains(searchQuery, ignoreCase = true)
      }
  }

  val filteredInvoices = remember(labInvoices, searchQuery) {
    labInvoices.filter {
      searchQuery.isBlank() ||
        it.patientName.contains(searchQuery, ignoreCase = true) ||
        it.patientHn.contains(searchQuery, ignoreCase = true) ||
        it.invoiceNumber.contains(searchQuery, ignoreCase = true)
    }
  }

  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  Scaffold(
    topBar = {
      Surface(
        color = PureWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkGrayText)
                }
                Spacer(modifier = Modifier.width(6.dp))
              }
              Surface(
                shape = CircleShape,
                color = Color(0xFFE0F2FE),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(22.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  "ဓာတ်ခွဲစစ်ဆေးမှု ကဏ္ဍ (Laboratory Services)",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateDark
                )
                Text(
                  "B2B Independent Module • Canon A4 Letterhead & Thermal Ready",
                  fontSize = 11.sp,
                  color = SlateMuted
                )
              }
            }

            Button(
              onClick = { showNewOrderDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("စစ်ဆေးမှုအသစ် မှာယူရန်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Tab Navigation Bar
          TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = { HorizontalDivider(color = BorderGray) }
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("စစ်ဆေးရန် စောင့်ဆိုင်းစာရင်း (Queue)", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                  if (queueOrders.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(containerColor = Color(0xFF0284C7)) { Text("${queueOrders.size}") }
                  }
                }
              }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("ပြီးစီးပြီး အဖြေများ (Results)", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                  if (completedOrders.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(containerColor = Color(0xFF16A34A)) { Text("${completedOrders.size}") }
                  }
                }
              }
            )
            Tab(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("သီးသန့် ဘောင်ချာများ (Lab Invoices)", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                  if (filteredInvoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Badge(containerColor = Color(0xFF7C3AED)) { Text("${filteredInvoices.size}") }
                  }
                }
              }
            )
            Tab(
              selected = selectedTab == 3,
              onClick = { selectedTab = 3 },
              text = {
                Text("စစ်ဆေးမှုပုံစံများ (Templates)", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
              }
            )
          }

          // Search bar (for tabs 0, 1, 2)
          if (selectedTab in 0..2) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("လူနာအမည် / HN / Order No ရှာဖွေရန်...", fontSize = 11.5.sp, color = SlateMuted) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = SlateMuted) },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = SlateMuted)
                  }
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().height(44.dp),
              shape = RoundedCornerShape(8.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                unfocusedBorderColor = BorderGray
              )
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(Color(0xFFF1F5F9))
    ) {
      when (selectedTab) {
        0 -> {
          // Tab 1: Queue (Orders waiting for sample / testing)
          if (queueOrders.isEmpty()) {
            EmptyStateView(
              icon = Icons.Default.DoneAll,
              title = "လက်ရှိ စစ်ဆေးရန် စောင့်ဆိုင်းလူနာ မရှိပါ",
              description = "အပေါ်ရှိ '+ စစ်ဆေးမှုအသစ် မှာယူရန်' ကိုနှိပ်၍ သို့မဟုတ် Doctor Consultation screen မှ စစ်ဆေးမှုများကို စာရင်းသွင်းနိုင်ပါသည်။"
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(queueOrders, key = { it.id }) { order ->
                LabOrderQueueCard(
                  order = order,
                  clinicSettings = clinicSettings,
                  onSampleCollected = {
                    viewModel.updateLabOrderStatus(order.id, LabOrderStatus.SAMPLE_COLLECTED)
                    Toast.makeText(context, "နမူနာရယူပြီးအဖြစ် မှတ်သားပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                  },
                  onEnterResults = { orderToEnterResults = order },
                  onPrintBlankLetterhead = {
                    LabPrintHelper.printLabReportA4(
                      context = context,
                      order = order,
                      clinicSettings = clinicSettings,
                      isBlankLetterhead = true
                    )
                  },
                  onCreateInvoice = { orderToInvoice = order },
                  onCancelOrder = {
                    viewModel.updateLabOrderStatus(order.id, LabOrderStatus.CANCELLED)
                    Toast.makeText(context, "စစ်ဆေးမှု ပယ်ဖျက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                  }
                )
              }
            }
          }
        }

        1 -> {
          // Tab 2: Results (Completed orders with parameters & reports)
          if (completedOrders.isEmpty()) {
            EmptyStateView(
              icon = Icons.Default.Science,
              title = "ပြီးစီးပြီး အဖြေများ မရှိသေးပါ",
              description = "Queue ထဲရှိ စစ်ဆေးမှုများအား အဖြေထည့်သွင်းပြီးပါက ဤနေရာတွင် စာရင်းနှင့်တကွ Canon A4 Report ထုတ်နိုင်ပါမည်။"
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(completedOrders, key = { it.id }) { order ->
                LabCompletedResultCard(
                  order = order,
                  clinicSettings = clinicSettings,
                  onPrintFullReport = {
                    LabPrintHelper.printLabReportA4(
                      context = context,
                      order = order,
                      clinicSettings = clinicSettings,
                      isBlankLetterhead = false
                    )
                  },
                  onPrintBlankLetterhead = {
                    LabPrintHelper.printLabReportA4(
                      context = context,
                      order = order,
                      clinicSettings = clinicSettings,
                      isBlankLetterhead = true
                    )
                  },
                  onEditResults = { orderToEnterResults = order },
                  onAttachSlipImage = { imageUri ->
                    val updatedItems = order.items.map { it.copy(attachedImageUri = imageUri) }
                    viewModel.updateLabOrder(order.copy(items = updatedItems))
                    Toast.makeText(context, "စက်အဖြေပုံ တွဲစပ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                  }
                )
              }
            }
          }
        }

        2 -> {
          // Tab 3: Lab Invoices (Dedicated accounting)
          LabInvoicesTabContent(
            invoices = filteredInvoices,
            clinicSettings = clinicSettings,
            currencyFmt = currencyFmt
          )
        }

        3 -> {
          // Tab 4: Templates & Pricing
          LabTemplatesTabContent(
            templates = labTemplates,
            onAddNew = { isCreatingTemplate = true },
            onEdit = { templateToEdit = it },
            onDelete = {
              viewModel.deleteLabTemplate(it.id)
              Toast.makeText(context, "စစ်ဆေးမှု ပုံစံ ဖျက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
            },
            onResetPresets = {
              val seeds = LabInitialDataSeeder.getSeedTemplates()
              seeds.forEach { viewModel.saveLabTemplate(it) }
              Toast.makeText(context, "စံပြု ပုံစံများ ပြန်လည်သွင်းယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }

  // --- Modals ---

  // 1. New Lab Order Dialog
  if (showNewOrderDialog) {
    NewLabOrderDialog(
      patients = allPatients,
      templates = labTemplates,
      attendingDoctor = viewModel.currentUser.value?.displayName ?: clinicSettings.doctorName,
      onDismiss = { showNewOrderDialog = false },
      onSubmit = { newOrder ->
        viewModel.createLabOrder(newOrder)
        showNewOrderDialog = false
        Toast.makeText(context, "စစ်ဆေးမှု အသစ် စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // 2. Result Entry Dialog
  orderToEnterResults?.let { order ->
    LabResultEntryDialog(
      order = order,
      technicianName = viewModel.currentUser.value?.displayName ?: "Lab Technologist",
      onDismiss = { orderToEnterResults = null },
      onSave = { updatedItems, tech, notes ->
        viewModel.saveLabOrderResults(order.id, updatedItems, tech, notes)
        orderToEnterResults = null
        Toast.makeText(context, "ဓာတ်ခွဲအဖြေများ မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // 3. Create Lab Invoice Dialog
  orderToInvoice?.let { order ->
    CreateLabInvoiceDialog(
      order = order,
      cashierName = viewModel.currentUser.value?.displayName ?: "Cashier",
      onDismiss = { orderToInvoice = null },
      onSubmit = { invoice ->
        viewModel.createLabInvoice(invoice)
        orderToInvoice = null
        Toast.makeText(context, "ဓာတ်ခွဲခန်း ဘောင်ချာ ဖွင့်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        // Prompt to print thermal slip
        LabPrintHelper.printLabVoucherThermal(context, invoice, clinicSettings)
      }
    )
  }

  // 4. Template Editor Dialog
  if (isCreatingTemplate || templateToEdit != null) {
    LabTemplateEditorDialog(
      template = templateToEdit,
      onDismiss = {
        isCreatingTemplate = false
        templateToEdit = null
      },
      onSave = { savedTemplate ->
        viewModel.saveLabTemplate(savedTemplate)
        isCreatingTemplate = false
        templateToEdit = null
        Toast.makeText(context, "စစ်ဆေးမှုပုံစံ သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

// -------------------------------------------------------------
// Component: Lab Order Queue Card
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabOrderQueueCard(
  order: LabOrder,
  clinicSettings: ClinicSettings,
  onSampleCollected: () -> Unit,
  onEnterResults: () -> Unit,
  onPrintBlankLetterhead: () -> Unit,
  onCreateInvoice: () -> Unit,
  onCancelOrder: () -> Unit
) {
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGray),
    modifier = Modifier.fillMaxWidth().animateContentSize()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              order.patientName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFF1F5F9),
              border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
            ) {
              Text(
                order.patientHn,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            "Age: ${order.patientAge} • ${order.patientGender.name} • Dr: ${order.doctorName.ifBlank { "OPD" }} • ${order.orderDate} ${order.orderTime}",
            fontSize = 11.sp,
            color = SlateMuted
          )
        }

        // Status Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = when (order.status) {
            LabOrderStatus.ORDERED -> Color(0xFFFEF3C7)
            LabOrderStatus.SAMPLE_COLLECTED -> Color(0xFFE0F2FE)
            LabOrderStatus.COMPLETED -> Color(0xFFDCFCE7)
            LabOrderStatus.CANCELLED -> Color(0xFFFEE2E2)
          }
        ) {
          Text(
            when (order.status) {
              LabOrderStatus.ORDERED -> "စစ်ဆေးရန် စောင့်ဆိုင်း"
              LabOrderStatus.SAMPLE_COLLECTED -> "နမူနာရယူပြီး (Testing)"
              LabOrderStatus.COMPLETED -> "ပြီးစီး"
              LabOrderStatus.CANCELLED -> "ပယ်ဖျက်"
            },
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = when (order.status) {
              LabOrderStatus.ORDERED -> Color(0xFFD97706)
              LabOrderStatus.SAMPLE_COLLECTED -> Color(0xFF0284C7)
              LabOrderStatus.COMPLETED -> Color(0xFF16A34A)
              LabOrderStatus.CANCELLED -> Color(0xFFDC2626)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = Color(0xFFF1F5F9))
      Spacer(modifier = Modifier.height(8.dp))

      // Ordered Test Items Chips
      Text("စစ်ဆေးရမည့် အကြောင်းအရာများ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
      Spacer(modifier = Modifier.height(4.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        order.items.forEach { item ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(0.8.dp, Color(0xFFBFDBFE))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                item.testName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D4ED8)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "${currencyFmt.format(item.price.toInt())} Ks",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Payment & Action Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "ကျသင့်ငွေ: ",
            fontSize = 12.sp,
            color = SlateMuted
          )
          Text(
            "${currencyFmt.format(order.totalAmount.toInt())} Ks",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (order.isPaid) Color(0xFFDCFCE7) else Color(0xFFFEF2F2)
          ) {
            Text(
              if (order.isPaid) "ငွေရှင်းပြီး" else "မရှင်းရသေး",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (order.isPaid) Color(0xFF16A34A) else Color(0xFFDC2626),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Print Blank Letterhead (Mode A)
          OutlinedButton(
            onClick = onPrintBlankLetterhead,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("A4 Letterhead", fontSize = 10.5.sp)
          }

          if (!order.isPaid) {
            Button(
              onClick = onCreateInvoice,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ဘောင်ချာဖွင့်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (order.status == LabOrderStatus.ORDERED) {
            Button(
              onClick = onSampleCollected,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("နမူနာရယူပြီး", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          Button(
            onClick = onEnterResults,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("အဖြေထည့်ရန်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Component: Lab Completed Result Card
// -------------------------------------------------------------
@Composable
fun LabCompletedResultCard(
  order: LabOrder,
  clinicSettings: ClinicSettings,
  onPrintFullReport: () -> Unit,
  onPrintBlankLetterhead: () -> Unit,
  onEditResults: () -> Unit,
  onAttachSlipImage: (String) -> Unit
) {
  val context = LocalContext.current
  var isExpanded by remember { mutableStateOf(false) }

  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      val (_, localPath) = AttachmentHelper.copyUriToLocalStorage(context, it, "lab_slip")
      onAttachSlipImage(localPath)
    }
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGray),
    modifier = Modifier.fillMaxWidth().animateContentSize()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              order.patientName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFDCFCE7),
              border = BorderStroke(0.8.dp, Color(0xFF86EFAC))
            ) {
              Text(
                "Completed",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16A34A),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            "HN: ${order.patientHn} • Order: ${order.orderNumber} • Date: ${order.orderDate}",
            fontSize = 11.sp,
            color = SlateMuted
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Attach photo of analyzer slip
          OutlinedButton(
            onClick = { photoLauncher.launch("image/*") },
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("စက်အဖြေပုံတင်", fontSize = 10.5.sp)
          }

          // Print Blank Letterhead
          OutlinedButton(
            onClick = onPrintBlankLetterhead,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("A4 အလွတ်", fontSize = 10.5.sp)
          }

          // Canon A4 Full Print
          Button(
            onClick = onPrintFullReport,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("A4 Report ထုတ်ရန်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = Color(0xFFF1F5F9))
      Spacer(modifier = Modifier.height(8.dp))

      // Summary of tests in order
      Row(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "Tests: " + order.items.joinToString(", ") { it.testName },
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF1E293B),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Text(
          if (isExpanded) "အသေးစိတ် ပိတ်ရန် ▲" else "အဖြေ အသေးစိတ် ကြည့်ရန် ▼",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF0284C7)
        )
      }

      // Expanded Parameter Table
      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          for (item in order.items) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  "🔬 ${item.testName} (${item.specimenType})",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (item.parameters.isNotEmpty()) {
                  // Table header
                  Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp)).padding(6.dp)
                  ) {
                    Text("Parameter", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text("Result", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("Unit", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Reference", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("Flag", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                  }

                  item.parameters.forEach { p ->
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(p.name, fontSize = 11.sp, modifier = Modifier.weight(2f))
                      Text(p.result.ifBlank { "-" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1.5f))
                      Text(p.unit.ifBlank { "-" }, fontSize = 10.sp, color = SlateMuted, modifier = Modifier.weight(1f))
                      Text(p.referenceRange.ifBlank { "-" }, fontSize = 10.sp, color = SlateMuted, modifier = Modifier.weight(1.5f))
                      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (p.flag.isNotBlank()) {
                          Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = when (p.flag.uppercase()) {
                              "HIGH" -> Color(0xFFFEE2E2)
                              "LOW" -> Color(0xFFFEF3C7)
                              else -> Color(0xFFDCFCE7)
                            }
                          ) {
                            Text(
                              p.flag,
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = when (p.flag.uppercase()) {
                                "HIGH" -> Color(0xFFDC2626)
                                "LOW" -> Color(0xFFD97706)
                                else -> Color(0xFF16A34A)
                              },
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                          }
                        } else {
                          Text("-", fontSize = 10.sp, color = SlateMuted)
                        }
                      }
                    }
                  }
                }

                if (item.remarks.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Remarks: ${item.remarks}", fontSize = 10.5.sp, color = SlateDark)
                }

                // If attached image exists
                if (!item.attachedImageUri.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("Attached Slip Photo:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateMuted)
                  Spacer(modifier = Modifier.height(4.dp))
                  AsyncImage(
                    model = item.attachedImageUri,
                    contentDescription = "Analyzer Slip",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(6.dp))
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = onEditResults) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("အဖြေများ ပြန်လည်ပြင်ဆင်ရန်", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Component: Lab Invoices Tab Content
// -------------------------------------------------------------
@Composable
fun LabInvoicesTabContent(
  invoices: List<LabInvoice>,
  clinicSettings: ClinicSettings,
  currencyFmt: NumberFormat
) {
  val context = LocalContext.current
  val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val todayInvoices = remember(invoices) { invoices.filter { it.timestamp.startsWith(todayStr) } }
  val totalTodayRevenue = remember(todayInvoices) { todayInvoices.sumOf { it.paidAmount } }
  val totalAllRevenue = remember(invoices) { invoices.sumOf { it.paidAmount } }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    // KPI Cards
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("ယနေ့ ဓာတ်ခွဲခန်း ရငွေ", fontSize = 11.sp, color = Color(0xFF6D28D9))
          Spacer(modifier = Modifier.height(4.dp))
          Text("${currencyFmt.format(totalTodayRevenue.toInt())} Ks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4C1D95))
          Text("${todayInvoices.size} Vouchers", fontSize = 10.sp, color = Color(0xFF7C3AED))
        }
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("စုစုပေါင်း ဓာတ်ခွဲခန်း ရငွေ", fontSize = 11.sp, color = Color(0xFF15803D))
          Spacer(modifier = Modifier.height(4.dp))
          Text("${currencyFmt.format(totalAllRevenue.toInt())} Ks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
          Text("${invoices.size} Invoices", fontSize = 10.sp, color = Color(0xFF16A34A))
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (invoices.isEmpty()) {
      EmptyStateView(
        icon = Icons.Default.Receipt,
        title = "ဓာတ်ခွဲခန်း သီးသန့် ဘောင်ချာ မရှိသေးပါ",
        description = "Queue ရှိ စစ်ဆေးမှုများအား ငွေရှင်း၍ ဘောင်ချာထုတ်ယူသည့်အခါ ဤနေရာတွင် သီးသန့် စာရင်းပေါ်လာပါမည်။"
      )
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(invoices, key = { it.id }) { inv ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(inv.invoiceNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4C1D95))
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF3E8FF)) {
                    Text(inv.paymentMethod.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("${inv.patientName} (${inv.patientHn}) • ${inv.timestamp}", fontSize = 11.sp, color = SlateDark)
                Text("Cashier: ${inv.cashierName}", fontSize = 10.sp, color = SlateMuted)
              }

              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                  Text("${currencyFmt.format(inv.paidAmount.toInt())} Ks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                  Text("Total: ${currencyFmt.format(inv.totalAmount.toInt())} Ks", fontSize = 10.sp, color = SlateMuted)
                }

                Button(
                  onClick = {
                    LabPrintHelper.printLabVoucherThermal(context, inv, clinicSettings)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Thermal Print", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Component: Lab Templates Tab Content
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabTemplatesTabContent(
  templates: List<LabTestTemplate>,
  onAddNew: () -> Unit,
  onEdit: (LabTestTemplate) -> Unit,
  onDelete: (LabTestTemplate) -> Unit,
  onResetPresets: () -> Unit
) {
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "ဓာတ်ခွဲစစ်ဆေးမှု ပုံစံများနှင့် စျေးနှုန်းများ (${templates.size} မျိုး)",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = onResetPresets,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reset Presets", fontSize = 10.5.sp)
        }

        Button(
          onClick = onAddNew,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("စစ်ဆေးမှုပုံစံ အသစ်ထည့်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(templates, key = { it.id }) { template ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = PureWhite,
          border = BorderStroke(1.dp, BorderGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    template.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                      template.category,
                      fontSize = 10.sp,
                      color = SlateMuted,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  if (template.isAnalyzerSlipOnly) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEFF6FF)) {
                      Text(
                        "စက်အဖြေ Slip သီးသန့်",
                        fontSize = 9.5.sp,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  "Specimen: ${template.specimenType} • Parameters: ${template.parameters.size} ခု",
                  fontSize = 11.sp,
                  color = SlateMuted
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "${currencyFmt.format(template.defaultPrice.toInt())} Ks",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF16A34A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onEdit(template) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                }
                IconButton(onClick = { onDelete(template) }, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                }
              }
            }

            if (template.parameters.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                template.parameters.take(8).forEach { param ->
                  Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF8FAFC), border = BorderStroke(0.6.dp, Color(0xFFE2E8F0))) {
                    Text(
                      "${param.name} (${param.referenceRange})",
                      fontSize = 9.5.sp,
                      color = SlateDark,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                }
                if (template.parameters.size > 8) {
                  Text("+${template.parameters.size - 8} more", fontSize = 10.sp, color = SlateMuted, modifier = Modifier.align(Alignment.CenterVertically))
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Component: Empty State View
// -------------------------------------------------------------
@Composable
fun EmptyStateView(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(32.dp)
    ) {
      Surface(shape = CircleShape, color = Color(0xFFE2E8F0), modifier = Modifier.size(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(32.dp))
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SlateDark, textAlign = TextAlign.Center)
      Spacer(modifier = Modifier.height(6.dp))
      Text(description, fontSize = 12.sp, color = SlateMuted, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
  }
}

// -------------------------------------------------------------
// Dialog: New Lab Order
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewLabOrderDialog(
  patients: List<Patient>,
  templates: List<LabTestTemplate>,
  attendingDoctor: String,
  onDismiss: () -> Unit,
  onSubmit: (LabOrder) -> Unit
) {
  var patientSearch by remember { mutableStateOf("") }
  var selectedPatient by remember { mutableStateOf<Patient?>(null) }
  var walkInName by remember { mutableStateOf("") }
  var walkInAge by remember { mutableStateOf("30") }
  var walkInGender by remember { mutableStateOf(Gender.MALE) }
  var isWalkIn by remember { mutableStateOf(false) }

  var doctorName by remember { mutableStateOf(attendingDoctor) }
  var clinicalNotes by remember { mutableStateOf("") }
  val selectedTemplateIds = remember { mutableStateListOf<String>() }
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  val filteredPatients = remember(patientSearch, patients) {
    if (patientSearch.isBlank()) patients.take(10)
    else patients.filter { it.name.contains(patientSearch, ignoreCase = true) || it.hn.contains(patientSearch, ignoreCase = true) }.take(8)
  }

  val selectedTemplates = remember(selectedTemplateIds.toList(), templates) {
    templates.filter { selectedTemplateIds.contains(it.id) }
  }
  val totalAmount = remember(selectedTemplates) { selectedTemplates.sumOf { it.defaultPrice } }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text("စစ်ဆေးမှုအသစ် မှာယူခြင်း (New Lab Order)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          // Patient Selection
          Text("လူနာ ရွေးချယ်ရန် / အသစ်ထည့်ရန်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
          Spacer(modifier = Modifier.height(6.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
              selected = !isWalkIn,
              onClick = { isWalkIn = false },
              label = { Text("လက်ရှိဆေးခန်းလူနာစာရင်းမှ ရွေးရန်", fontSize = 11.sp) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
              selected = isWalkIn,
              onClick = { isWalkIn = true },
              label = { Text("ပြင်ပလူနာ (Walk-in)", fontSize = 11.sp) }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (!isWalkIn) {
            if (selectedPatient != null) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE0F2FE),
                border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp).fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(selectedPatient!!.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    Text("HN: ${selectedPatient!!.hn} • Age: ${selectedPatient!!.age} • ${selectedPatient!!.gender.name}", fontSize = 11.sp, color = Color(0xFF0284C7))
                  }
                  TextButton(onClick = { selectedPatient = null }) {
                    Text("ပြောင်းရန်", fontSize = 11.sp, color = Color(0xFFDC2626))
                  }
                }
              }
            } else {
              OutlinedTextField(
                value = patientSearch,
                onValueChange = { patientSearch = it },
                placeholder = { Text("လူနာအမည် သို့မဟုတ် HN ရိုက်ထည့်ရှာဖွေပါ...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(6.dp)
              )
              Spacer(modifier = Modifier.height(6.dp))
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filteredPatients.forEach { pat ->
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedPatient = pat }
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp).fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text("${pat.name} (${pat.hn})", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                      Text("Age ${pat.age} • ${pat.gender.name}", fontSize = 10.5.sp, color = SlateMuted)
                    }
                  }
                }
              }
            }
          } else {
            // Walk-in form
            OutlinedTextField(
              value = walkInName,
              onValueChange = { walkInName = it },
              label = { Text("လူနာအမည် *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().height(52.dp),
              shape = RoundedCornerShape(6.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = walkInAge,
                onValueChange = { walkInAge = it },
                label = { Text("အသက်") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(6.dp)
              )
              Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                  selected = walkInGender == Gender.MALE,
                  onClick = { walkInGender = Gender.MALE },
                  label = { Text("ကျား", fontSize = 11.sp) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                  selected = walkInGender == Gender.FEMALE,
                  onClick = { walkInGender = Gender.FEMALE },
                  label = { Text("မ", fontSize = 11.sp) }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = BorderGray)
          Spacer(modifier = Modifier.height(10.dp))

          // Test Selection
          Text("စစ်ဆေးမည့် စမ်းသပ်ချက်များ ရွေးချယ်ရန် (${selectedTemplateIds.size} ခု ရွေးချယ်ထား)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
          Spacer(modifier = Modifier.height(6.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            templates.forEach { tmpl ->
              val isSelected = selectedTemplateIds.contains(tmpl.id)
              FilterChip(
                selected = isSelected,
                onClick = {
                  if (isSelected) selectedTemplateIds.remove(tmpl.id)
                  else selectedTemplateIds.add(tmpl.id)
                },
                label = {
                  Text(
                    "${tmpl.name} (${currencyFmt.format(tmpl.defaultPrice.toInt())} Ks)",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF0284C7),
                  selectedLabelColor = PureWhite
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = BorderGray)
          Spacer(modifier = Modifier.height(10.dp))

          // Referring Doctor & Notes
          OutlinedTextField(
            value = doctorName,
            onValueChange = { doctorName = it },
            label = { Text("ညွှန်ကြားသည့် ဆရာဝန် (Referring Doctor)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(6.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = clinicalNotes,
            onValueChange = { clinicalNotes = it },
            label = { Text("ရောဂါအခြေအနေ / Clinical Notes (Optional)") },
            modifier = Modifier.fillMaxWidth().height(65.dp),
            shape = RoundedCornerShape(6.dp)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        // Total & Submit Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("စုစုပေါင်း စစ်ဆေးခ:", fontSize = 11.sp, color = SlateMuted)
            Text("${currencyFmt.format(totalAmount.toInt())} Ks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
              Text("ပယ်ဖျက်")
            }

            val canSubmit = (selectedPatient != null || (isWalkIn && walkInName.isNotBlank())) && selectedTemplateIds.isNotEmpty()

            Button(
              onClick = {
                val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val orderNumber = "LAB-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}-${(100..999).random()}"

                val pName = if (isWalkIn) walkInName.trim() else selectedPatient!!.name
                val pHn = if (isWalkIn) "WALK-IN" else selectedPatient!!.hn
                val pAge = if (isWalkIn) walkInAge.toIntOrNull() ?: 30 else selectedPatient!!.age
                val pGender = if (isWalkIn) walkInGender else selectedPatient!!.gender
                val pId = if (isWalkIn) UUID.randomUUID().toString() else selectedPatient!!.id

                val items = selectedTemplates.map { t ->
                  LabOrderItem(
                    testId = t.id,
                    testName = t.name,
                    category = t.category,
                    price = t.defaultPrice,
                    specimenType = t.specimenType,
                    remarks = "",
                    isCompleted = false,
                    parameters = t.parameters.map { p ->
                      LabTestParameter(
                        name = p.name,
                        result = "",
                        unit = p.unit,
                        referenceRange = p.referenceRange,
                        flag = ""
                      )
                    }
                  )
                }

                val order = LabOrder(
                  id = UUID.randomUUID().toString(),
                  orderNumber = orderNumber,
                  patientId = pId,
                  patientName = pName,
                  patientHn = pHn,
                  patientAge = pAge,
                  patientGender = pGender,
                  doctorName = doctorName,
                  orderDate = now,
                  orderTime = nowTime,
                  status = LabOrderStatus.ORDERED,
                  totalAmount = totalAmount,
                  paidAmount = 0.0,
                  paymentStatus = "Unpaid",
                  invoiceNumber = "",
                  technicianName = "",
                  notes = clinicalNotes,
                  items = items
                )

                onSubmit(order)
              },
              enabled = canSubmit,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Order အတည်ပြုမည်", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Dialog: Lab Result Entry
// -------------------------------------------------------------
@Composable
fun LabResultEntryDialog(
  order: LabOrder,
  technicianName: String,
  onDismiss: () -> Unit,
  onSave: (List<LabOrderItem>, String, String) -> Unit
) {
  var techName by remember { mutableStateOf(order.technicianName.ifBlank { technicianName }) }
  var generalNotes by remember { mutableStateOf(order.notes) }

  // Working state for items and parameters
  val itemsState = remember {
    order.items.map { item ->
      item.copy(parameters = item.parameters.map { it.copy() })
    }.toMutableStateList()
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("ဓာတ်ခွဲအဖြေ ထည့်သွင်းခြင်း", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Text("${order.patientName} (${order.patientHn}) • Order: ${order.orderNumber}", fontSize = 11.5.sp, color = SlateMuted)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          itemsState.forEachIndexed { itemIdx, item ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "🔬 ${item.testName} (${item.specimenType})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                      checked = item.isCompleted,
                      onCheckedChange = { checked ->
                        itemsState[itemIdx] = item.copy(isCompleted = checked)
                      }
                    )
                    Text("စစ်ဆေးပြီးစီး", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                  }
                }

                if (item.parameters.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  item.parameters.forEachIndexed { pIdx, p ->
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(2f)) {
                        Text(p.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("Ref: ${p.referenceRange.ifBlank { "-" }} ${p.unit}", fontSize = 9.5.sp, color = SlateMuted)
                      }

                      OutlinedTextField(
                        value = p.result,
                        onValueChange = { newVal ->
                          val updatedParams = item.parameters.toMutableList()
                          updatedParams[pIdx] = p.copy(result = newVal)
                          itemsState[itemIdx] = item.copy(parameters = updatedParams)
                        },
                        placeholder = { Text("Result", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(2f).height(44.dp),
                        shape = RoundedCornerShape(6.dp)
                      )

                      // Flag Selector (Normal / High / Low)
                      var flagExpanded by remember { mutableStateOf(false) }
                      Box(modifier = Modifier.weight(1.3f)) {
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = when (p.flag.uppercase()) {
                            "HIGH" -> Color(0xFFFEE2E2)
                            "LOW" -> Color(0xFFFEF3C7)
                            "NORMAL" -> Color(0xFFDCFCE7)
                            else -> Color(0xFFF1F5F9)
                          },
                          border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                          modifier = Modifier.fillMaxWidth().height(44.dp).clickable { flagExpanded = true }
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Text(
                              p.flag.ifBlank { "Flag ▼" },
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = when (p.flag.uppercase()) {
                                "HIGH" -> Color(0xFFDC2626)
                                "LOW" -> Color(0xFFD97706)
                                "NORMAL" -> Color(0xFF16A34A)
                                else -> SlateDark
                              }
                            )
                          }
                        }

                        DropdownMenu(
                          expanded = flagExpanded,
                          onDismissRequest = { flagExpanded = false }
                        ) {
                          listOf("NORMAL", "HIGH", "LOW", "-").forEach { opt ->
                            DropdownMenuItem(
                              text = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                              onClick = {
                                val updatedParams = item.parameters.toMutableList()
                                updatedParams[pIdx] = p.copy(flag = if (opt == "-") "" else opt)
                                itemsState[itemIdx] = item.copy(parameters = updatedParams)
                                flagExpanded = false
                              }
                            )
                          }
                        }
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = item.remarks,
                  onValueChange = { newRem ->
                    itemsState[itemIdx] = item.copy(remarks = newRem)
                  },
                  placeholder = { Text("အဖြေအပေါ် မှတ်ချက် (e.g. Lipemic, Hemolyzed, Repeat checked)...", fontSize = 10.sp) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().height(44.dp),
                  shape = RoundedCornerShape(6.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = techName,
            onValueChange = { techName = it },
            label = { Text("စစ်ဆေးသူ ဓာတ်ခွဲကျွမ်းကျင်ပညာရှင် (Lab Technologist)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(6.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = generalNotes,
            onValueChange = { generalNotes = it },
            label = { Text("အထွေထွေ မှတ်ချက် (General Notes)") },
            modifier = Modifier.fillMaxWidth().height(65.dp),
            shape = RoundedCornerShape(6.dp)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
            Text("မသိမ်းပါ")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              // Mark items as completed if results are filled
              val finalizedItems = itemsState.map { it.copy(isCompleted = true) }
              onSave(finalizedItems, techName, generalNotes)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(6.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("အဖြေများ အတည်ပြုသိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Dialog: Create Lab Invoice (Separate accounting)
// -------------------------------------------------------------
@Composable
fun CreateLabInvoiceDialog(
  order: LabOrder,
  cashierName: String,
  onDismiss: () -> Unit,
  onSubmit: (LabInvoice) -> Unit
) {
  var discountText by remember { mutableStateOf("0") }
  var paidAmountText by remember { mutableStateOf(order.totalAmount.toInt().toString()) }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var cashier by remember { mutableStateOf(cashierName) }

  val subtotal = order.totalAmount
  val discount = discountText.toDoubleOrNull() ?: 0.0
  val netTotal = (subtotal - discount).coerceAtLeast(0.0)
  val paidAmount = paidAmountText.toDoubleOrNull() ?: netTotal
  val changeAmount = (paidAmount - netTotal).coerceAtLeast(0.0)
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text("ဓာတ်ခွဲခန်း သီးသန့် ဘောင်ချာဖွင့်ခြင်း (Lab Invoice)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Text("ဆေးခန်း Inventory နှင့် ရောထွေးမှုမရှိဘဲ သီးသန့် စာရင်းသွင်းပါမည်", fontSize = 11.sp, color = Color(0xFF7C3AED))

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        // Order Summary
        Text("လူနာ: ${order.patientName} (${order.patientHn})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("စစ်ဆေးချက်: " + order.items.joinToString(", ") { it.testName }, fontSize = 11.sp, color = SlateMuted)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Subtotal:", fontSize = 12.sp)
          Text("${currencyFmt.format(subtotal.toInt())} Ks", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Discount
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Discount (Ks):", fontSize = 12.sp)
          OutlinedTextField(
            value = discountText,
            onValueChange = { discountText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(120.dp).height(44.dp),
            shape = RoundedCornerShape(6.dp)
          )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Net Total:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          Text("${currencyFmt.format(netTotal.toInt())} Ks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        // Payment Method
        Text("ငွေပေးချေမှုပုံစံ:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(PaymentMethod.CASH, PaymentMethod.KPAY, PaymentMethod.CBPAY, PaymentMethod.WAVEPAY).forEach { pm ->
            FilterChip(
              selected = selectedMethod == pm,
              onClick = { selectedMethod = pm },
              label = { Text(pm.name, fontSize = 10.5.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Paid Amount & Change
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("လက်ခံရရှိငွေ (Ks):", fontSize = 12.sp)
          OutlinedTextField(
            value = paidAmountText,
            onValueChange = { paidAmountText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(130.dp).height(44.dp),
            shape = RoundedCornerShape(6.dp)
          )
        }

        if (changeAmount > 0.0) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("ပြန်အမ်းငွေ:", fontSize = 12.sp, color = Color(0xFF16A34A))
            Text("${currencyFmt.format(changeAmount.toInt())} Ks", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
            Text("ပယ်ဖျက်")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val now = BurmeseDateUtils.getNowFormatted()
              val invNo = "LAB-INV-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}-${(100..999).random()}"
              val invoice = LabInvoice(
                id = UUID.randomUUID().toString(),
                invoiceNumber = invNo,
                orderId = order.id,
                patientId = order.patientId,
                patientName = order.patientName,
                patientHn = order.patientHn,
                totalAmount = subtotal,
                discount = discount,
                netTotal = netTotal,
                paidAmount = paidAmount,
                changeAmount = changeAmount,
                paymentMethod = selectedMethod,
                cashierName = cashier,
                timestamp = now,
                items = order.items
              )
              onSubmit(invoice)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            shape = RoundedCornerShape(6.dp)
          ) {
            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ဘောင်ချာဖွင့် & Thermal Print", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Dialog: Lab Test Template Editor
// -------------------------------------------------------------
@Composable
fun LabTemplateEditorDialog(
  template: LabTestTemplate?,
  onDismiss: () -> Unit,
  onSave: (LabTestTemplate) -> Unit
) {
  var name by remember { mutableStateOf(template?.name ?: "") }
  var category by remember { mutableStateOf(template?.category ?: "Hematology") }
  var specimenType by remember { mutableStateOf(template?.specimenType ?: "Blood") }
  var priceText by remember { mutableStateOf(template?.defaultPrice?.toInt()?.toString() ?: "5000") }
  var isAnalyzerSlipOnly by remember { mutableStateOf(template?.isAnalyzerSlipOnly ?: false) }

  val parameters = remember {
    (template?.parameters ?: emptyList()).map { it.copy() }.toMutableStateList()
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          if (template == null) "စစ်ဆေးမှုပုံစံ အသစ်ဖန်တီးခြင်း" else "စစ်ဆေးမှုပုံစံ ပြင်ဆင်ခြင်း",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = SlateDark
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("စစ်ဆေးမှု အမည် * (e.g. CBC, Serum Creatinine)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(6.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("အမျိုးအစား (Category)") },
              singleLine = true,
              modifier = Modifier.weight(1f).height(52.dp),
              shape = RoundedCornerShape(6.dp)
            )
            OutlinedTextField(
              value = specimenType,
              onValueChange = { specimenType = it },
              label = { Text("နမူနာ (Specimen)") },
              singleLine = true,
              modifier = Modifier.weight(1f).height(52.dp),
              shape = RoundedCornerShape(6.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("ပုံမှန် စျေးနှုန်း (Default Price in Ks) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(6.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Toggle for analyzer slip direct print
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = isAnalyzerSlipOnly,
              onCheckedChange = { isAnalyzerSlipOnly = it }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("စက်မှ တိုက်ရိုက်ထွက်သော အဖြေ Slip သီးသန့်ဖြစ်သည် (CBC Analyzer Direct)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              Text("ဤ Test အတွက် A4 Blank Letterhead ထုတ်ပေး၍ စက်ထဲထည့်သွင်းနိုင်ပါသည်", fontSize = 10.sp, color = SlateMuted)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = BorderGray)
          Spacer(modifier = Modifier.height(10.dp))

          // Parameter Builder
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("တိုင်းတာရမည့် Parameters (${parameters.size} ခု)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
            Button(
              onClick = {
                parameters.add(LabTestParameter(name = "", unit = "", referenceRange = "", flag = ""))
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Add Parameter", fontSize = 10.5.sp)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          parameters.forEachIndexed { pIdx, param ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = param.name,
                onValueChange = { parameters[pIdx] = param.copy(name = it) },
                placeholder = { Text("Name", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(2f).height(42.dp),
                shape = RoundedCornerShape(4.dp)
              )
              OutlinedTextField(
                value = param.unit,
                onValueChange = { parameters[pIdx] = param.copy(unit = it) },
                placeholder = { Text("Unit", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(4.dp)
              )
              OutlinedTextField(
                value = param.referenceRange,
                onValueChange = { parameters[pIdx] = param.copy(referenceRange = it) },
                placeholder = { Text("Normal Range", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1.5f).height(42.dp),
                shape = RoundedCornerShape(4.dp)
              )
              IconButton(
                onClick = { parameters.removeAt(pIdx) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
            Text("ပယ်ဖျက်")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val pVal = priceText.toDoubleOrNull() ?: 5000.0
              val item = LabTestTemplate(
                id = template?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                category = category.trim(),
                defaultPrice = pVal,
                specimenType = specimenType.trim(),
                isAnalyzerSlipOnly = isAnalyzerSlipOnly,
                parameters = parameters.toList()
              )
              onSave(item)
            },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
