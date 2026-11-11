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
import androidx.compose.ui.window.DialogProperties
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

/**
 * Mobile-First LabInvestigationScreen
 *
 * Fully responsive, thumb-friendly UI/UX built specifically for mobile screens:
 * - Clean vertical stack cards with primary action buttons & more options menu.
 * - Mobile card-based result entry form (replaces cramped desktop tables).
 * - Walk-in direct lab patient support (no OPD consultation queue required).
 * - Canon A4 letterhead printing (blank for analyzer feed / full report) & thermal cash vouchers.
 */
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
        shadowElevation = 1.5.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
          // Mobile Top Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(32.dp)) {
                  Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkGrayText, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
              }
              Surface(
                shape = CircleShape,
                color = Color(0xFFE0F2FE),
                modifier = Modifier.size(32.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "ဓာတ်ခွဲခန်း (Laboratory)",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateDark,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  "B2B Module • A4 & Thermal",
                  fontSize = 9.5.sp,
                  color = SlateMuted,
                  maxLines = 1
                )
              }
            }

            // Mobile New Order Button
            Button(
              onClick = { showNewOrderDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("စစ်ဆေးမှုအသစ်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Mobile-friendly Tab Row
          ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
          ) {
            Tab(
              selected = selectedTab == 0,
              onClick = { selectedTab = 0 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("စောင့်ဆိုင်း (Queue)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                  if (queueOrders.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(containerColor = Color(0xFF0284C7)) { Text("${queueOrders.size}", fontSize = 9.sp) }
                  }
                }
              }
            )
            Tab(
              selected = selectedTab == 1,
              onClick = { selectedTab = 1 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("အဖြေများ (Results)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                  if (completedOrders.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(containerColor = Color(0xFF16A34A)) { Text("${completedOrders.size}", fontSize = 9.sp) }
                  }
                }
              }
            )
            Tab(
              selected = selectedTab == 2,
              onClick = { selectedTab = 2 },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("ဘောင်ချာ (Vouchers)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                  if (filteredInvoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(containerColor = Color(0xFF7C3AED)) { Text("${filteredInvoices.size}", fontSize = 9.sp) }
                  }
                }
              }
            )
            Tab(
              selected = selectedTab == 3,
              onClick = { selectedTab = 3 },
              text = {
                Text("စျေးနှုန်း (Prices)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
              }
            )
          }

          // Compact Mobile Search Bar (Only for tabs 0, 1, 2)
          if (selectedTab in 0..2) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("ရှာဖွေရန် (လူနာအမည် / HN)...", fontSize = 12.sp, color = SlateMuted) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = SlateMuted) },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp), tint = SlateMuted)
                  }
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = Color(0xFF0284C7)
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
        .background(Color(0xFFF8FAFC))
    ) {
      when (selectedTab) {
        0 -> {
          // Tab 1: Queue (Orders waiting for testing)
          if (queueOrders.isEmpty()) {
            MobileEmptyState(
              icon = Icons.Default.CheckCircleOutline,
              title = "စစ်ဆေးရန် စောင့်ဆိုင်းလူနာ မရှိပါ",
              description = "အပေါ်ရှိ '+ စစ်ဆေးမှုအသစ်' သို့မဟုတ် ဆရာဝန်စမ်းသပ်ခန်းမှ တိုက်ရိုက် မှာယူနိုင်ပါသည်။"
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(queueOrders, key = { it.id }) { order ->
                MobileLabOrderQueueCard(
                  order = order,
                  clinicSettings = clinicSettings,
                  onSampleCollected = {
                    viewModel.updateLabOrderStatus(order.id, LabOrderStatus.SAMPLE_COLLECTED)
                    Toast.makeText(context, "နမူနာရယူပြီးအဖြစ် သတ်မှတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
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
          // Tab 2: Completed Results
          if (completedOrders.isEmpty()) {
            MobileEmptyState(
              icon = Icons.Default.Science,
              title = "ပြီးစီးပြီး အဖြေများ မရှိသေးပါ",
              description = "Queue ရှိ စစ်ဆေးမှုများအား အဖြေထည့်သွင်းပြီးပါက ဤနေရာတွင် ကြည့်ရှု၍ Canon A4 Report ထုတ်နိုင်ပါမည်။"
            )
          } else {
            LazyColumn(
              contentPadding = PaddingValues(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(completedOrders, key = { it.id }) { order ->
                MobileLabCompletedResultCard(
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
          MobileLabInvoicesTabContent(
            invoices = filteredInvoices,
            clinicSettings = clinicSettings,
            currencyFmt = currencyFmt
          )
        }

        3 -> {
          // Tab 4: Templates & Pricing
          MobileLabTemplatesTabContent(
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

  // 1. New Lab Order Dialog (Mobile Optimized with Walk-in toggle)
  if (showNewOrderDialog) {
    MobileNewLabOrderDialog(
      patients = allPatients,
      templates = labTemplates,
      attendingDoctor = viewModel.currentUser.value?.displayName ?: clinicSettings.doctorName,
      onDismiss = { showNewOrderDialog = false },
      onSubmit = { newOrder ->
        viewModel.createLabOrder(newOrder)
        showNewOrderDialog = false
        Toast.makeText(context, "သွေးစစ်မှု အသစ် စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // 2. Result Entry Form (Mobile Card List Form)
  orderToEnterResults?.let { order ->
    MobileLabResultEntryDialog(
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

  // 3. Create Lab Invoice Dialog (Mobile Friendly)
  orderToInvoice?.let { order ->
    MobileCreateLabInvoiceDialog(
      order = order,
      cashierName = viewModel.currentUser.value?.displayName ?: "Cashier",
      onDismiss = { orderToInvoice = null },
      onSubmit = { invoice ->
        viewModel.createLabInvoice(invoice)
        orderToInvoice = null
        Toast.makeText(context, "ဓာတ်ခွဲခန်း ဘောင်ချာ ဖွင့်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
        LabPrintHelper.printLabVoucherThermal(context, invoice, clinicSettings)
      }
    )
  }

  // 4. Template Editor Dialog
  if (isCreatingTemplate || templateToEdit != null) {
    MobileLabTemplateEditorDialog(
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
// Component: Mobile Lab Order Queue Card
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobileLabOrderQueueCard(
  order: LabOrder,
  clinicSettings: ClinicSettings,
  onSampleCollected: () -> Unit,
  onEnterResults: () -> Unit,
  onPrintBlankLetterhead: () -> Unit,
  onCreateInvoice: () -> Unit,
  onCancelOrder: () -> Unit
) {
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
  var showMenu by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    modifier = Modifier.fillMaxWidth().animateContentSize()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Top Row: Patient Name + HN + Status Pill + Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            order.patientName,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(3.dp),
            color = if (order.patientHn == "WALK-IN") Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
            border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
          ) {
            Text(
              order.patientHn,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (order.patientHn == "WALK-IN") Color(0xFF1D4ED8) else SlateDark,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        // Status Badge
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = when (order.status) {
            LabOrderStatus.ORDERED -> Color(0xFFFEF3C7)
            LabOrderStatus.SAMPLE_COLLECTED -> Color(0xFFE0F2FE)
            LabOrderStatus.COMPLETED -> Color(0xFFDCFCE7)
            LabOrderStatus.CANCELLED -> Color(0xFFFEE2E2)
          }
        ) {
          Text(
            when (order.status) {
              LabOrderStatus.ORDERED -> "စစ်ဆေးရန်"
              LabOrderStatus.SAMPLE_COLLECTED -> "နမူနာရပြီး"
              LabOrderStatus.COMPLETED -> "ပြီးစီး"
              LabOrderStatus.CANCELLED -> "ပယ်ဖျက်"
            },
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = when (order.status) {
              LabOrderStatus.ORDERED -> Color(0xFFD97706)
              LabOrderStatus.SAMPLE_COLLECTED -> Color(0xFF0284C7)
              LabOrderStatus.COMPLETED -> Color(0xFF16A34A)
              LabOrderStatus.CANCELLED -> Color(0xFFDC2626)
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Row 2: Demographics & Time
      Text(
        "Age: ${order.patientAge} • ${order.patientGender.name} • ${order.orderDate} ${order.orderTime}",
        fontSize = 10.sp,
        color = SlateMuted
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Row 3: Tests Ordered (Compact Chips)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        order.items.forEach { item ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(0.6.dp, Color(0xFFBBF7D0))
          ) {
            Text(
              "${item.testName} (${currencyFmt.format(item.price.toInt())} Ks)",
              fontSize = 10.sp,
              color = Color(0xFF166534),
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = Color(0xFFF1F5F9))
      Spacer(modifier = Modifier.height(6.dp))

      // Bottom Row: Price + Primary Action Button + More Actions Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Price & Paid Tag
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "${currencyFmt.format(order.totalAmount.toInt())} Ks",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(3.dp),
            color = if (order.isPaid) Color(0xFFDCFCE7) else Color(0xFFFEF2F2)
          ) {
            Text(
              if (order.isPaid) "Paid" else "Unpaid",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (order.isPaid) Color(0xFF16A34A) else Color(0xFFDC2626),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        // Action Buttons Row (Mobile optimized)
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Primary Action
          if (order.status == LabOrderStatus.ORDERED) {
            Button(
              onClick = onSampleCollected,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("နမူနာရယူပြီး", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          Button(
            onClick = onEnterResults,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("အဖြေထည့်ရန်", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          // More Options Dropdown Menu (Contains A4 letterhead, voucher, cancel)
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "More", tint = SlateDark, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("📄 A4 Blank Letterhead ထုတ်ရန်", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  onPrintBlankLetterhead()
                }
              )
              if (!order.isPaid) {
                DropdownMenuItem(
                  text = { Text("🧾 ငွေရှင်းဘောင်ချာ ဖွင့်ရန်", fontSize = 11.5.sp) },
                  onClick = {
                    showMenu = false
                    onCreateInvoice()
                  }
                )
              }
              DropdownMenuItem(
                text = { Text("❌ စစ်ဆေးမှု ပယ်ဖျက်ရန်", fontSize = 11.5.sp, color = Color(0xFFDC2626)) },
                onClick = {
                  showMenu = false
                  onCancelOrder()
                }
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Component: Mobile Lab Completed Result Card
// -------------------------------------------------------------
@Composable
fun MobileLabCompletedResultCard(
  order: LabOrder,
  clinicSettings: ClinicSettings,
  onPrintFullReport: () -> Unit,
  onPrintBlankLetterhead: () -> Unit,
  onEditResults: () -> Unit,
  onAttachSlipImage: (String) -> Unit
) {
  val context = LocalContext.current
  var isExpanded by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }

  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      val (_, localPath) = AttachmentHelper.copyUriToLocalStorage(context, it, "lab_slip")
      onAttachSlipImage(localPath)
    }
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    modifier = Modifier.fillMaxWidth().animateContentSize()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            order.patientName,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(3.dp),
            color = Color(0xFFDCFCE7),
            border = BorderStroke(0.6.dp, Color(0xFF86EFAC))
          ) {
            Text(
              "ပြီးစီး",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF16A34A),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
          Button(
            onClick = onPrintFullReport,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("A4 Report", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "More", tint = SlateDark, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("📄 A4 Blank Letterhead ထုတ်ရန်", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  onPrintBlankLetterhead()
                }
              )
              DropdownMenuItem(
                text = { Text("📷 စက်အဖြေ Slip ပုံတင်ရန်", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  photoLauncher.launch("image/*")
                }
              )
              DropdownMenuItem(
                text = { Text("✏️ အဖြေများ ပြန်လည်ပြင်ဆင်ရန်", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  onEditResults()
                }
              )
            }
          }
        }
      }

      Text(
        "HN: ${order.patientHn} • ${order.orderDate} • Dr: ${order.doctorName.ifBlank { "Direct" }}",
        fontSize = 10.sp,
        color = SlateMuted
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Expand / Collapse Result Details Toggle
      Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFFF1F5F9)).clickable { isExpanded = !isExpanded }.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "စစ်ဆေးချက်: " + order.items.joinToString(", ") { it.testName },
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF1E293B),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Text(
          if (isExpanded) "ပိတ်ရန် ▲" else "အဖြေကြည့်ရန် ▼",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF0284C7)
        )
      }

      // Mobile Expanded Result Parameter Cards
      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          for (item in order.items) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  "🔬 ${item.testName} (${item.specimenType})",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0F172A)
                )

                if (item.parameters.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    item.parameters.forEach { p ->
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                          Text(p.name, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                          Text("Ref: ${p.referenceRange} ${p.unit}", fontSize = 9.sp, color = SlateMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            "${p.result.ifBlank { "-" }} ${p.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                          )
                          if (p.flag.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
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
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (p.flag.uppercase()) {
                                  "HIGH" -> Color(0xFFDC2626)
                                  "LOW" -> Color(0xFFD97706)
                                  else -> Color(0xFF16A34A)
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }

                if (item.remarks.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Remarks: ${item.remarks}", fontSize = 9.5.sp, color = SlateDark)
                }

                // If attached slip photo exists
                if (!item.attachedImageUri.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  AsyncImage(
                    model = item.attachedImageUri,
                    contentDescription = "Slip Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(4.dp))
                  )
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
// Component: Mobile Lab Invoices Tab Content
// -------------------------------------------------------------
@Composable
fun MobileLabInvoicesTabContent(
  invoices: List<LabInvoice>,
  clinicSettings: ClinicSettings,
  currencyFmt: NumberFormat
) {
  val context = LocalContext.current
  val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val todayInvoices = remember(invoices) { invoices.filter { it.timestamp.startsWith(todayStr) } }
  val totalTodayRevenue = remember(todayInvoices) { todayInvoices.sumOf { it.paidAmount } }
  val totalAllRevenue = remember(invoices) { invoices.sumOf { it.paidAmount } }

  Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
    // Compact KPI Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Text("ယနေ့ ဓာတ်ခွဲရငွေ", fontSize = 10.sp, color = Color(0xFF6D28D9))
          Text("${currencyFmt.format(totalTodayRevenue.toInt())} Ks", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4C1D95))
          Text("${todayInvoices.size} Vouchers", fontSize = 9.sp, color = Color(0xFF7C3AED))
        }
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          Text("စုစုပေါင်း ရငွေ", fontSize = 10.sp, color = Color(0xFF15803D))
          Text("${currencyFmt.format(totalAllRevenue.toInt())} Ks", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
          Text("${invoices.size} Invoices", fontSize = 9.sp, color = Color(0xFF16A34A))
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (invoices.isEmpty()) {
      MobileEmptyState(
        icon = Icons.Default.Receipt,
        title = "ဓာတ်ခွဲခန်း သီးသန့် ဘောင်ချာ မရှိသေးပါ",
        description = "Queue ရှိ စစ်ဆေးမှုများအား ငွေရှင်းသည့်အခါ ဤနေရာတွင် စာရင်းပေါ်လာပါမည်။"
      )
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(invoices, key = { it.id }) { inv ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(inv.invoiceNumber, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4C1D95))
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFF3E8FF)) {
                    Text(inv.paymentMethod.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                  }
                }
                Text("${inv.patientName} (${inv.patientHn})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(inv.timestamp, fontSize = 9.5.sp, color = SlateMuted)
              }

              Column(horizontalAlignment = Alignment.End) {
                Text("${currencyFmt.format(inv.paidAmount.toInt())} Ks", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                  onClick = {
                    LabPrintHelper.printLabVoucherThermal(context, inv, clinicSettings)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                  shape = RoundedCornerShape(4.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp)
                ) {
                  Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(11.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Thermal", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
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
// Component: Mobile Lab Templates Tab Content
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobileLabTemplatesTabContent(
  templates: List<LabTestTemplate>,
  onAddNew: () -> Unit,
  onEdit: (LabTestTemplate) -> Unit,
  onDelete: (LabTestTemplate) -> Unit,
  onResetPresets: () -> Unit
) {
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "စစ်ဆေးမှုပုံစံများ (${templates.size} မျိုး)",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = onResetPresets,
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("Reset", fontSize = 9.5.sp)
        }

        Button(
          onClick = onAddNew,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("အသစ်ထည့်", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(templates, key = { it.id }) { template ->
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = PureWhite,
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(template.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(
                  "${template.category} • ${template.specimenType} • ${template.parameters.size} Params",
                  fontSize = 9.5.sp,
                  color = SlateMuted
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "${currencyFmt.format(template.defaultPrice.toInt())} Ks",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF16A34A)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onEdit(template) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = Color(0xFF0284C7))
                }
                IconButton(onClick = { onDelete(template) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
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
// Component: Mobile Empty State
// -------------------------------------------------------------
@Composable
fun MobileEmptyState(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Surface(shape = CircleShape, color = Color(0xFFE2E8F0), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(24.dp))
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateDark, textAlign = TextAlign.Center)
      Spacer(modifier = Modifier.height(4.dp))
      Text(description, fontSize = 11.sp, color = SlateMuted, textAlign = TextAlign.Center, lineHeight = 16.sp)
    }
  }
}

// -------------------------------------------------------------
// Dialog: Mobile New Lab Order Dialog (Walk-in vs Clinic Patient)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MobileNewLabOrderDialog(
  patients: List<Patient>,
  templates: List<LabTestTemplate>,
  attendingDoctor: String,
  onDismiss: () -> Unit,
  onSubmit: (LabOrder) -> Unit
) {
  var isWalkIn by remember { mutableStateOf(false) }
  var patientSearch by remember { mutableStateOf("") }
  var selectedPatient by remember { mutableStateOf<Patient?>(null) }

  var walkInName by remember { mutableStateOf("") }
  var walkInAge by remember { mutableStateOf("30") }
  var walkInGender by remember { mutableStateOf(Gender.MALE) }
  var walkInPhone by remember { mutableStateOf("") }

  var doctorName by remember { mutableStateOf(attendingDoctor) }
  var clinicalNotes by remember { mutableStateOf("") }
  val selectedTemplateIds = remember { mutableStateListOf<String>() }
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  val filteredPatients = remember(patientSearch, patients) {
    if (patientSearch.isBlank()) patients.take(6)
    else patients.filter { it.name.contains(patientSearch, ignoreCase = true) || it.hn.contains(patientSearch, ignoreCase = true) }.take(6)
  }

  val selectedTemplates = remember(selectedTemplateIds.toList(), templates) {
    templates.filter { selectedTemplateIds.contains(it.id) }
  }
  val totalAmount = remember(selectedTemplates) { selectedTemplates.sumOf { it.defaultPrice } }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Dialog Title
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("သွေးစစ်မှု အသစ်မှာယူခြင်း (New Lab Order)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mobile Segmented Toggle: [ဆေးခန်းလူနာ] vs [ပြင်ပသွေးစစ် (Walk-in)]
        Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFF1F5F9)).padding(3.dp)
        ) {
          Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (!isWalkIn) PureWhite else Color.Transparent).clickable { isWalkIn = false }.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("ဆေးခန်းလူနာ", fontSize = 11.sp, fontWeight = if (!isWalkIn) FontWeight.Bold else FontWeight.Normal, color = if (!isWalkIn) Color(0xFF0284C7) else SlateMuted)
          }
          Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (isWalkIn) PureWhite else Color.Transparent).clickable { isWalkIn = true }.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("ပြင်ပလူနာ (Walk-in သီးသန့်)", fontSize = 11.sp, fontWeight = if (isWalkIn) FontWeight.Bold else FontWeight.Normal, color = if (isWalkIn) Color(0xFF0284C7) else SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          if (!isWalkIn) {
            // Existing Clinic Patient Search
            if (selectedPatient != null) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE0F2FE),
                border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp).fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(selectedPatient!!.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    Text("HN: ${selectedPatient!!.hn} • Age: ${selectedPatient!!.age} • ${selectedPatient!!.gender.name}", fontSize = 10.sp, color = Color(0xFF0284C7))
                  }
                  TextButton(
                    onClick = { selectedPatient = null },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("ပြောင်းရန်", fontSize = 10.5.sp, color = Color(0xFFDC2626))
                  }
                }
              }
            } else {
              OutlinedTextField(
                value = patientSearch,
                onValueChange = { patientSearch = it },
                placeholder = { Text("လူနာအမည် သို့မဟုတ် HN ရိုက်ထည့်ရှာဖွေပါ...", fontSize = 11.sp, color = SlateMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color(0xFF0F172A),
                  unfocusedTextColor = Color(0xFF0F172A),
                  focusedContainerColor = PureWhite,
                  unfocusedContainerColor = PureWhite,
                  unfocusedBorderColor = BorderGray,
                  focusedBorderColor = Color(0xFF0284C7)
                )
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
                      modifier = Modifier.padding(6.dp).fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text("${pat.name} (${pat.hn})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                      Text("Age ${pat.age} • ${pat.gender.name}", fontSize = 10.sp, color = SlateMuted)
                    }
                  }
                }
              }
            }
          } else {
            // Walk-in Direct Patient Form
            OutlinedTextField(
              value = walkInName,
              onValueChange = { walkInName = it },
              label = { Text("လူနာအမည် *", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = Color(0xFF0284C7)
              )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                value = walkInAge,
                onValueChange = { walkInAge = it },
                label = { Text("အသက်", fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color(0xFF0F172A),
                  unfocusedTextColor = Color(0xFF0F172A),
                  focusedContainerColor = PureWhite,
                  unfocusedContainerColor = PureWhite,
                  unfocusedBorderColor = BorderGray,
                  focusedBorderColor = Color(0xFF0284C7)
                )
              )
              Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                  selected = walkInGender == Gender.MALE,
                  onClick = { walkInGender = Gender.MALE },
                  label = { Text("ကျား", fontSize = 11.sp) },
                  modifier = Modifier.height(36.dp)
                )
                FilterChip(
                  selected = walkInGender == Gender.FEMALE,
                  onClick = { walkInGender = Gender.FEMALE },
                  label = { Text("မ", fontSize = 11.sp) },
                  modifier = Modifier.height(36.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFF1F5F9))
          Spacer(modifier = Modifier.height(8.dp))

          // Test Selection
          Text("စစ်ဆေးမည့် Tests ရွေးရန် (${selectedTemplateIds.size} ခု ရွေးထား)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
          Spacer(modifier = Modifier.height(6.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Color(0xFF0284C7),
                  selectedLabelColor = PureWhite
                ),
                modifier = Modifier.height(28.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFF1F5F9))
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = doctorName,
            onValueChange = { doctorName = it },
            label = { Text("ညွှန်ကြားသူ (Doctor / Self)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = clinicalNotes,
            onValueChange = { clinicalNotes = it },
            label = { Text("ရောဂါမှတ်ချက် / Clinical Notes (Optional)", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        // Mobile Bottom Row: Total & Submit Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("ကျသင့်ငွေ:", fontSize = 10.sp, color = SlateMuted)
            Text("${currencyFmt.format(totalAmount.toInt())} Ks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(34.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text("ပယ်ဖျက်", fontSize = 11.sp)
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
                  doctorName = doctorName.ifBlank { if (isWalkIn) "Self-request" else "Clinic Doctor" },
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
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(34.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
              Text("Order တင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Dialog: Mobile Lab Result Entry Form (Card-based parameter inputs)
// -------------------------------------------------------------
@Composable
fun MobileLabResultEntryDialog(
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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Sticky Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("ဓာတ်ခွဲအဖြေ ထည့်သွင်းခြင်း", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            Text("${order.patientName} (${order.patientHn}) • ${order.orderNumber}", fontSize = 10.5.sp, color = SlateMuted)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable List of Tests & Parameter Cards (Mobile Friendly Form)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          itemsState.forEachIndexed { itemIdx, item ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                // Test Header Row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "🔬 ${item.testName} (${item.specimenType})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                      checked = item.isCompleted,
                      onCheckedChange = { checked ->
                        itemsState[itemIdx] = item.copy(isCompleted = checked)
                      },
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ပြီးစီး", fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                  }
                }

                if (item.parameters.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))

                  // Each Parameter is an individual mobile input card
                  item.parameters.forEachIndexed { pIdx, p ->
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = PureWhite,
                      border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                      modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                      Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                          Text(p.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                          Text("Ref: ${p.referenceRange} ${p.unit}", fontSize = 9.5.sp, color = SlateMuted)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Result Input Field
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          OutlinedTextField(
                            value = p.result,
                            onValueChange = { newVal ->
                              val updatedParams = item.parameters.toMutableList()
                              updatedParams[pIdx] = p.copy(result = newVal)
                              itemsState[itemIdx] = item.copy(parameters = updatedParams)
                            },
                            placeholder = { Text("အဖြေ Result (${p.unit})", fontSize = 11.sp, color = SlateMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1.8f),
                            shape = RoundedCornerShape(6.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                            colors = OutlinedTextFieldDefaults.colors(
                              focusedTextColor = Color(0xFF0F172A),
                              unfocusedTextColor = Color(0xFF0F172A),
                              focusedContainerColor = PureWhite,
                              unfocusedContainerColor = PureWhite,
                              unfocusedBorderColor = BorderGray,
                              focusedBorderColor = Color(0xFF0284C7)
                            )
                          )

                          // Quick Thumb Chips: Normal / High / Low
                          Row(modifier = Modifier.weight(1.8f).height(52.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf("NORMAL", "HIGH", "LOW").forEach { flagOpt ->
                              val isFlagSelected = p.flag.equals(flagOpt, ignoreCase = true)
                              Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isFlagSelected) {
                                  when (flagOpt) {
                                    "HIGH" -> Color(0xFFDC2626)
                                    "LOW" -> Color(0xFFD97706)
                                    else -> Color(0xFF16A34A)
                                  }
                                } else Color(0xFFF1F5F9),
                                border = BorderStroke(0.6.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                                  val updatedParams = item.parameters.toMutableList()
                                  updatedParams[pIdx] = p.copy(flag = if (isFlagSelected) "" else flagOpt)
                                  itemsState[itemIdx] = item.copy(parameters = updatedParams)
                                }
                              ) {
                                Box(contentAlignment = Alignment.Center) {
                                  Text(
                                    flagOpt.take(3),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFlagSelected) PureWhite else SlateDark
                                  )
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = item.remarks,
                  onValueChange = { newRem ->
                    itemsState[itemIdx] = item.copy(remarks = newRem)
                  },
                  placeholder = { Text("စစ်ဆေးမှု မှတ်ချက် (e.g. Lipemic, Hemolyzed)...", fontSize = 11.sp, color = SlateMuted) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = Color(0xFF0F172A)),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = Color(0xFF0284C7)
                  )
                )
              }
            }
          }

          OutlinedTextField(
            value = techName,
            onValueChange = { techName = it },
            label = { Text("စစ်ဆေးသူ ဓာတ်ခွဲပညာရှင် (Technologist)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = generalNotes,
            onValueChange = { generalNotes = it },
            label = { Text("အထွေထွေ မှတ်ချက် (General Notes)", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        // Sticky Bottom Save Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text("မသိမ်းပါ", fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              val finalizedItems = itemsState.map { it.copy(isCompleted = true) }
              onSave(finalizedItems, techName, generalNotes)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("အဖြေ အတည်ပြုသိမ်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Dialog: Mobile Create Lab Invoice Dialog
// -------------------------------------------------------------
@Composable
fun MobileCreateLabInvoiceDialog(
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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth(0.92f)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text("ဓာတ်ခွဲခန်း သီးသန့် ဘောင်ချာ (Lab Invoice)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Text("ဆေးခန်း OPD ငွေစာရင်းနှင့် မရောဘဲ သီးသန့်သွင်းပါမည်", fontSize = 10.sp, color = Color(0xFF7C3AED))

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        Text("${order.patientName} (${order.patientHn})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Tests: " + order.items.joinToString(", ") { it.testName }, fontSize = 10.sp, color = SlateMuted)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Subtotal:", fontSize = 11.sp)
          Text("${currencyFmt.format(subtotal.toInt())} Ks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Discount (Ks):", fontSize = 11.sp)
          OutlinedTextField(
            value = discountText,
            onValueChange = { discountText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(120.dp),
            shape = RoundedCornerShape(6.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Net Total:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("${currencyFmt.format(netTotal.toInt())} Ks", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(6.dp))

        Text("ငွေပေးချေမှုပုံစံ:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf(PaymentMethod.CASH, PaymentMethod.KBZPAY, PaymentMethod.WAVEPAY, PaymentMethod.OTHER).forEach { pm ->
            FilterChip(
              selected = selectedMethod == pm,
              onClick = { selectedMethod = pm },
              label = { Text(pm.label, fontSize = 9.5.sp) },
              modifier = Modifier.height(26.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("လက်ခံရရှိငွေ:", fontSize = 11.sp)
          OutlinedTextField(
            value = paidAmountText,
            onValueChange = { paidAmountText = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(130.dp),
            shape = RoundedCornerShape(6.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
        }

        if (changeAmount > 0.0) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ပြန်အမ်းငွေ:", fontSize = 11.sp, color = Color(0xFF16A34A))
            Text("${currencyFmt.format(changeAmount.toInt())} Ks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text("ပယ်ဖျက်", fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.width(6.dp))
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
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ဘောင်ချာဖွင့် & Print", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// Dialog: Mobile Lab Template Editor Dialog
// -------------------------------------------------------------
@Composable
fun MobileLabTemplateEditorDialog(
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

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          if (template == null) "စစ်ဆေးမှုပုံစံ အသစ်ဖန်တီးခြင်း" else "စစ်ဆေးမှုပုံစံ ပြင်ဆင်ခြင်း",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = SlateDark
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("စစ်ဆေးမှု အမည် * (e.g. CBC, Lipid)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )
          Spacer(modifier = Modifier.height(6.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Category", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = Color(0xFF0284C7)
              )
            )
            OutlinedTextField(
              value = specimenType,
              onValueChange = { specimenType = it },
              label = { Text("Specimen (e.g. Blood)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp),
              textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = Color(0xFF0284C7)
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("ပုံမှန် စျေးနှုန်း (Price in Ks) *", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A),
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              unfocusedBorderColor = BorderGray,
              focusedBorderColor = Color(0xFF0284C7)
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = isAnalyzerSlipOnly,
              onCheckedChange = { isAnalyzerSlipOnly = it },
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("စက်အဖြေ Slip သီးသန့် Test ဖြစ်သည် (CBC)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("A4 Blank Letterhead ထုတ်၍ စက်ထဲထည့်သွင်းနိုင်ပါသည်", fontSize = 9.sp, color = SlateMuted)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = Color(0xFFF1F5F9))
          Spacer(modifier = Modifier.height(8.dp))

          // Parameters Builder
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Parameters (${parameters.size} ခု)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
            Button(
              onClick = {
                parameters.add(LabTestParameter(name = "", unit = "", referenceRange = "", flag = ""))
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(11.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Add Param", fontSize = 9.5.sp)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          parameters.forEachIndexed { pIdx, param ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedTextField(
                    value = param.name,
                    onValueChange = { parameters[pIdx] = param.copy(name = it) },
                    placeholder = { Text("Parameter အမည် (e.g. Hb)", fontSize = 11.sp, color = SlateMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = Color(0xFF0F172A),
                      unfocusedTextColor = Color(0xFF0F172A),
                      focusedContainerColor = PureWhite,
                      unfocusedContainerColor = PureWhite,
                      unfocusedBorderColor = BorderGray,
                      focusedBorderColor = Color(0xFF0284C7)
                    )
                  )
                  OutlinedTextField(
                    value = param.unit,
                    onValueChange = { parameters[pIdx] = param.copy(unit = it) },
                    placeholder = { Text("Unit (g/dL)", fontSize = 11.sp, color = SlateMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(6.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedTextColor = Color(0xFF0F172A),
                      unfocusedTextColor = Color(0xFF0F172A),
                      focusedContainerColor = PureWhite,
                      unfocusedContainerColor = PureWhite,
                      unfocusedBorderColor = BorderGray,
                      focusedBorderColor = Color(0xFF0284C7)
                    )
                  )
                  IconButton(
                    onClick = { parameters.removeAt(pIdx) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = param.referenceRange,
                  onValueChange = { parameters[pIdx] = param.copy(referenceRange = it) },
                  placeholder = { Text("ပုံမှန်ပမာဏ / Normal Range (e.g. 13.0 - 17.5)", fontSize = 11.sp, color = SlateMuted) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = Color(0xFF0284C7)
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text("ပယ်ဖျက်", fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.width(6.dp))
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
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
          ) {
            Text("သိမ်းဆည်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
