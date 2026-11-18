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
import androidx.compose.foundation.horizontalScroll
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
import com.example.ui.components.PosThermalSlipDialog
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
  val allExpenses by viewModel.expenses.collectAsState()
  val procedures by viewModel.procedures.collectAsState()

  val labExpenses = remember(allExpenses) {
    allExpenses.filter { it.category.contains("Lab", ignoreCase = true) || it.category.contains("Reagent", ignoreCase = true) }
  }

  var selectedTab by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedDateFilter by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "YESTERDAY", "MONTH", "LAST_MONTH"

  val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val yesterdayStr = remember {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }
  val thisMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
  val lastMonthStr = remember {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -1)
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
  }

  // Modals state
  var showNewOrderDialog by remember { mutableStateOf(false) }
  var orderToEnterResults by remember { mutableStateOf<LabOrder?>(null) }
  var orderToInvoice by remember { mutableStateOf<LabOrder?>(null) }
  var templateToEdit by remember { mutableStateOf<LabTestTemplate?>(null) }
  var isCreatingTemplate by remember { mutableStateOf(false) }
  var orderToDelete by remember { mutableStateOf<LabOrder?>(null) }
  var previewLabThermalSlip by remember { mutableStateOf<LabInvoice?>(null) }
  var showLabExpenseDialog by remember { mutableStateOf(false) }

  // Filtered orders
  val queueOrders = remember(labOrders, searchQuery, selectedDateFilter) {
    labOrders.filter { it.status == LabOrderStatus.ORDERED || it.status == LabOrderStatus.SAMPLE_COLLECTED }
      .filter { ord ->
        val matchesDate = when (selectedDateFilter) {
          "TODAY" -> ord.orderDate.startsWith(todayStr)
          "YESTERDAY" -> ord.orderDate.startsWith(yesterdayStr)
          "MONTH" -> ord.orderDate.startsWith(thisMonthStr)
          "LAST_MONTH" -> ord.orderDate.startsWith(lastMonthStr)
          else -> true
        }
        val matchesQuery = searchQuery.isBlank() ||
          ord.patientName.contains(searchQuery, ignoreCase = true) ||
          ord.patientHn.contains(searchQuery, ignoreCase = true) ||
          ord.orderNumber.contains(searchQuery, ignoreCase = true) ||
          ord.orderDate.contains(searchQuery, ignoreCase = true)
        matchesDate && matchesQuery
      }
  }

  val completedOrders = remember(labOrders, searchQuery, selectedDateFilter) {
    labOrders.filter { it.status == LabOrderStatus.COMPLETED }
      .filter { ord ->
        val matchesDate = when (selectedDateFilter) {
          "TODAY" -> ord.orderDate.startsWith(todayStr)
          "YESTERDAY" -> ord.orderDate.startsWith(yesterdayStr)
          "MONTH" -> ord.orderDate.startsWith(thisMonthStr)
          "LAST_MONTH" -> ord.orderDate.startsWith(lastMonthStr)
          else -> true
        }
        val matchesQuery = searchQuery.isBlank() ||
          ord.patientName.contains(searchQuery, ignoreCase = true) ||
          ord.patientHn.contains(searchQuery, ignoreCase = true) ||
          ord.orderNumber.contains(searchQuery, ignoreCase = true) ||
          ord.orderDate.contains(searchQuery, ignoreCase = true)
        matchesDate && matchesQuery
      }
  }

  val filteredInvoices = remember(labInvoices, searchQuery, selectedDateFilter) {
    labInvoices.filter { inv ->
      val matchesDate = when (selectedDateFilter) {
        "TODAY" -> inv.timestamp.startsWith(todayStr)
        "YESTERDAY" -> inv.timestamp.startsWith(yesterdayStr)
        "MONTH" -> inv.timestamp.startsWith(thisMonthStr)
        "LAST_MONTH" -> inv.timestamp.startsWith(lastMonthStr)
        else -> true
      }
      val matchesQuery = searchQuery.isBlank() ||
        inv.patientName.contains(searchQuery, ignoreCase = true) ||
        inv.patientHn.contains(searchQuery, ignoreCase = true) ||
        inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
        inv.timestamp.contains(searchQuery, ignoreCase = true)
      matchesDate && matchesQuery
    }
  }

  val filteredExpenses = remember(labExpenses, searchQuery, selectedDateFilter) {
    labExpenses.filter { exp ->
      val expDate = exp.date.ifBlank { exp.timestamp }
      val matchesDate = when (selectedDateFilter) {
        "TODAY" -> expDate.startsWith(todayStr)
        "YESTERDAY" -> expDate.startsWith(yesterdayStr)
        "MONTH" -> expDate.startsWith(thisMonthStr)
        "LAST_MONTH" -> expDate.startsWith(lastMonthStr)
        else -> true
      }
      val matchesQuery = searchQuery.isBlank() ||
        exp.title.contains(searchQuery, ignoreCase = true) ||
        exp.description.contains(searchQuery, ignoreCase = true) ||
        exp.payee.contains(searchQuery, ignoreCase = true) ||
        exp.voucherNo.contains(searchQuery, ignoreCase = true)
      matchesDate && matchesQuery
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

            // Mobile Action Buttons: Blank Letterhead, Lab Expense & New Order
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedButton(
                onClick = {
                  LabPrintHelper.printBlankLetterheadA4(context, clinicSettings)
                },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp),
                border = BorderStroke(1.dp, Color(0xFF0D9488))
              ) {
                Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("📄 Blank Letterhead", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
              }
              OutlinedButton(
                onClick = { showLabExpenseDialog = true },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp),
                border = BorderStroke(1.dp, Color(0xFFEA580C))
              ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Lab စရိတ်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
              }
              Button(
                onClick = { showNewOrderDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("စစ်ဆေးမှုအသစ်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
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
                  Text("ငွေစာရင်း (Ledger)", fontSize = 11.5.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                  val totalCount = filteredInvoices.size + filteredExpenses.size
                  if (totalCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(containerColor = Color(0xFF7C3AED)) { Text("$totalCount", fontSize = 9.sp) }
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

          // Compact Mobile Search Bar & Date Filter Chips (Only for tabs 0, 1, 2)
          if (selectedTab in 0..2) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("လူနာအမည်၊ HN၊ ဘောင်ချာ/Order No.၊ ရက်စွဲ ဖြင့်ရှာရန်...", fontSize = 11.5.sp, color = SlateMuted) },
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
              textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = Color(0xFF0284C7)
              )
            )

            // Date Quick Filter Chips
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              listOf(
                Pair("ALL", "အားလုံး (All)"),
                Pair("TODAY", "ယနေ့ (Today)"),
                Pair("YESTERDAY", "ယမန်နေ့"),
                Pair("MONTH", "ဒီလ (This Month)"),
                Pair("LAST_MONTH", "ပြီးခဲ့သည့်လ")
              ).forEach { (code, label) ->
                val isSel = selectedDateFilter == code
                FilterChip(
                  selected = isSel,
                  onClick = { selectedDateFilter = code },
                  label = { Text(label, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = PureWhite
                  ),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.height(26.dp)
                )
              }
            }
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
                  },
                  onAttachSlipImage = { imageUri ->
                    val updatedItems = order.items.map { it.copy(attachedImageUri = imageUri) }
                    viewModel.updateLabOrder(order.copy(items = updatedItems))
                    Toast.makeText(context, "စက်အဖြေပုံ တွဲစပ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                  },
                  onDeleteOrder = { orderToDelete = order }
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
                  onPrintVoucher = {
                    val linkedInv = labInvoices.find { it.orderId == order.id || it.orderId == order.orderNumber || (order.invoiceNumber.isNotBlank() && it.invoiceNumber == order.invoiceNumber) }
                    if (linkedInv != null) {
                      val fullInv = if (linkedInv.items.isEmpty() && order.items.isNotEmpty()) linkedInv.copy(items = order.items) else linkedInv
                      previewLabThermalSlip = fullInv
                    } else {
                      orderToInvoice = order
                    }
                  },
                  onEditResults = { orderToEnterResults = order },
                  onAttachSlipImage = { imageUri ->
                    val updatedItems = order.items.map { it.copy(attachedImageUri = imageUri) }
                    viewModel.updateLabOrder(order.copy(items = updatedItems))
                    Toast.makeText(context, "စက်အဖြေပုံ တွဲစပ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                  },
                  onDeleteOrder = { orderToDelete = order }
                )
              }
            }
          }
        }

        2 -> {
          // Tab 3: Lab Invoices & Expenses (Dedicated Ledger)
          MobileLabInvoicesTabContent(
            invoices = filteredInvoices,
            expenses = filteredExpenses,
            clinicSettings = clinicSettings,
            currencyFmt = currencyFmt,
            onPrintVoucher = { inv ->
              val fullInv = if (inv.items.isEmpty()) {
                val matchingOrder = labOrders.find { it.id == inv.orderId || it.orderNumber == inv.orderId || (inv.invoiceNumber.isNotBlank() && it.invoiceNumber == inv.invoiceNumber) }
                if (matchingOrder != null && matchingOrder.items.isNotEmpty()) inv.copy(items = matchingOrder.items) else inv
              } else inv
              previewLabThermalSlip = fullInv
            },
            onAddExpense = { showLabExpenseDialog = true },
            onDeleteInvoice = { inv ->
              viewModel.deleteLabInvoice(inv.id)
              Toast.makeText(context, "ဘောင်ချာ ${inv.invoiceNumber} အား ဖျက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
            }
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
            onRestoreFromOrders = {
              val count = viewModel.restoreLabTemplatesFromOrders()
              if (count > 0) {
                Toast.makeText(context, "ယခင်မှတ်တမ်းများမှ စစ်ဆေးမှု $count မျိုး၏ ဈေးနှုန်းနှင့် Parameter များကို အောင်မြင်စွာ ပြန်လည်ဆယ်ယူပြီးပါပြီ", Toast.LENGTH_LONG).show()
              } else {
                Toast.makeText(context, "ဆယ်ယူရန် ယခင်မှတ်တမ်း မတွေ့ရှိပါ သို့မဟုတ် မူလအတိုင်း ရှိနှင့်ပြီးဖြစ်ပါသည်", Toast.LENGTH_SHORT).show()
              }
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
      procedures = procedures,
      attendingDoctor = viewModel.currentUser.value?.displayName ?: clinicSettings.doctorName,
      recentDoctorNames = labOrders.mapNotNull { it.doctorName.trim().takeIf { d -> d.isNotBlank() && !d.equals("Self-request", ignoreCase = true) } }.distinct().take(8),
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
      technicianName = viewModel.currentUser.value?.displayName ?: "",
      defaultPathologistName = clinicSettings.defaultPathologistName,
      defaultPathologistDegree = clinicSettings.defaultPathologistDegree,
      defaultPathologistSama = clinicSettings.defaultPathologistSama,
      defaultPathologistTitle = clinicSettings.defaultPathologistTitle,
      onDismiss = { orderToEnterResults = null },
      onSave = { updatedItems, tech, notes, pName, pDegree, pSama, pTitle, showDocSeal, showTechSign, refDoc ->
        viewModel.saveLabOrderResults(order.id, updatedItems, tech, notes, pName, pDegree, pSama, pTitle, showDocSeal, showTechSign, refDoc)
        orderToEnterResults = null
        Toast.makeText(context, "ဓာတ်ခွဲအဖြေများ မှတ်တမ်းတင်ပြီးပါပြီ (ပြီးစီးပြီး စာရင်း Tab သို့ ရောက်ရှိသွားပါပြီ)", Toast.LENGTH_SHORT).show()
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
        previewLabThermalSlip = invoice
      }
    )
  }

  // 3.2 Bluetooth Thermal POS Slip Studio Modal
  previewLabThermalSlip?.let { rawLabInv ->
    val labInv = if (rawLabInv.items.isEmpty()) {
      val matchingOrder = labOrders.find { it.id == rawLabInv.orderId || it.orderNumber == rawLabInv.orderId || (rawLabInv.invoiceNumber.isNotBlank() && it.invoiceNumber == rawLabInv.invoiceNumber) }
      if (matchingOrder != null && matchingOrder.items.isNotEmpty()) rawLabInv.copy(items = matchingOrder.items) else rawLabInv
    } else rawLabInv
    PosThermalSlipDialog(
      invoice = labInv.toClinicInvoice(),
      patientHn = labInv.patientHn,
      cashierName = labInv.cashierName,
      clinicSettings = clinicSettings,
      onDismiss = { previewLabThermalSlip = null },
      onPrintSuccess = { previewLabThermalSlip = null }
    )
  }

  // 3.3 Lab Expense Management Modal
  if (showLabExpenseDialog) {
    MobileLabExpenseDialog(
      onDismiss = { showLabExpenseDialog = false },
      onSaveExpense = { title, amt, payee, notes ->
        viewModel.addLabExpense(title = title, amount = amt, payee = payee, notes = notes)
        showLabExpenseDialog = false
        Toast.makeText(context, "Lab ကုန်ကျစရိတ် မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // 3.5 Delete Order Confirmation Dialog
  orderToDelete?.let { ord ->
    AlertDialog(
      onDismissRequest = { orderToDelete = null },
      title = { Text("စစ်ဆေးမှုအော်ဒါ အပြီးဖျက်မည်", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
      text = { Text("စစ်ဆေးမှုအော်ဒါ (${ord.orderNumber} - ${ord.patientName}) အား စာရင်းမှ အပြီးတိုင် ဖျက်ထုတ်ရန် သေချာပါသလား?\n\nဤလုပ်ဆောင်ချက်ကို ပြန်ပြင်၍မရနိုင်ပါ။", fontSize = 12.sp) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteLabOrder(ord.id)
            orderToDelete = null
            Toast.makeText(context, "စစ်ဆေးမှုအော်ဒါအား ဖျက်ထုတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("အပြီးဖျက်မည်", color = PureWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { orderToDelete = null }) {
          Text("မဖျက်တော့ပါ")
        }
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
  onCancelOrder: () -> Unit,
  onAttachSlipImage: (String) -> Unit = {},
  onDeleteOrder: () -> Unit = {}
) {
  val context = LocalContext.current
  val photoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      val (_, localPath) = AttachmentHelper.copyUriToLocalStorage(context, it, "lab_slip")
      onAttachSlipImage(localPath)
    }
  }
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

      // Row 2: Demographics & Time & Lab No
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "Age: ${order.patientAge} • ${order.patientGender.name} • ${order.orderDate} ${order.orderTime}",
          fontSize = 10.sp,
          color = SlateMuted
        )
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFFF1F5F9),
          border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
        ) {
          Text(
            "🔬 Lab No: ${order.orderNumber}",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0369A1),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
          )
        }
      }

      val queueContact = listOfNotNull(
        order.patientPhone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
        order.patientAddress.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
      ).joinToString("  •  ")
      if (queueContact.isNotBlank()) {
        Text(
          queueContact,
          fontSize = 9.5.sp,
          color = Color(0xFF0369A1),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      val refDocQueue = order.referringDoctorName.ifBlank { "Direct / Self-request" }
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF0284C7))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          "Ref. Dr: $refDocQueue",
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          color = if (refDocQueue.equals("Direct / Self-request", ignoreCase = true) || refDocQueue.equals("Self-request", ignoreCase = true)) Color(0xFF64748B) else Color(0xFF0369A1)
        )
      }

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

        // Action Buttons Row (Mobile & Tablet optimized: Slip & Payment First, then Results Follow)
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (!order.isPaid) {
            // STEP 1: Must print slip and purchase payment first!
            Button(
              onClick = onCreateInvoice,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("ငွေရှင်း / Slip ထုတ်မည်", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            // STEP 2: Payment purchased -> Sample collection & Test results follow!
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
          }

          // More Options Dropdown Menu
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
                text = { Text("📄 CBC စက်ထည့်ရန် စာရွက်ကြိုထုတ် (Pre-printed Sheet)", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  onPrintBlankLetterhead()
                }
              )
              if (!order.isPaid) {
                DropdownMenuItem(
                  text = { Text("🔬 စစ်ဆေးချက် အဖြေထည့်ရန်", fontSize = 11.5.sp) },
                  onClick = {
                    showMenu = false
                    onEnterResults()
                  }
                )
              } else {
                DropdownMenuItem(
                  text = { Text("🧾 ငွေရှင်းဘောင်ချာ ပြန်လည်ထုတ်ယူမည်", fontSize = 11.5.sp) },
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
              DropdownMenuItem(
                text = { Text("🗑️ စစ်ဆေးမှု စာရင်းမှ ဖျက်မည်", fontSize = 11.5.sp, color = Color(0xFFDC2626)) },
                onClick = {
                  showMenu = false
                  onDeleteOrder()
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
  onPrintVoucher: () -> Unit,
  onEditResults: () -> Unit,
  onAttachSlipImage: (String) -> Unit,
  onDeleteOrder: () -> Unit = {}
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Button(
            onClick = onPrintFullReport,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("📄 A4 / PDF Print", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onPrintVoucher,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("ဘောင်ချာ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                text = { Text("🧾 Thermal ဘောင်ချာ ထုတ်ရန်", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  onPrintVoucher()
                }
              )
              DropdownMenuItem(
                text = { Text("✏️ အဖြေများ ပြန်လည်ပြင်ဆင်ရန်", fontSize = 11.5.sp) },
                onClick = {
                  showMenu = false
                  onEditResults()
                }
              )
              DropdownMenuItem(
                text = { Text("🗑️ စစ်ဆေးမှု စာရင်းမှ ဖျက်မည်", fontSize = 11.5.sp, color = Color(0xFFDC2626)) },
                onClick = {
                  showMenu = false
                  onDeleteOrder()
                }
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "HN: ${order.patientHn} • ${order.orderDate} • Ref. Dr: ${order.referringDoctorName.ifBlank { "Direct / Self-request" }}",
          fontSize = 10.sp,
          color = SlateMuted
        )
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFFF1F5F9),
          border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
        ) {
          Text(
            "🔬 Lab No: ${order.orderNumber}",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0369A1),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
          )
        }
      }

      val completedContact = listOfNotNull(
        order.patientPhone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
        order.patientAddress.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
      ).joinToString("  •  ")
      if (completedContact.isNotBlank()) {
        Text(
          completedContact,
          fontSize = 9.5.sp,
          color = Color(0xFF0369A1),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

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
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "🔬 ${item.testName} (${item.specimenType})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                  )
                  OutlinedButton(
                    onClick = {
                      LabPrintHelper.printLabReportA4(
                        context = context,
                        order = order,
                        item = item,
                        clinicSettings = clinicSettings,
                        isBlankLetterhead = false
                      )
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(26.dp)
                  ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("သီးသန့် Report", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                  }
                }

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
                          if (p.referenceRange.isNotBlank()) {
                            val cleanRef = p.referenceRange.trim()
                            val unitSuffix = if (p.unit.isNotBlank()) " ${p.unit}" else ""
                            val refDisplay = if (cleanRef.contains("\n")) {
                              cleanRef.split("\n").joinToString("\n") { "Ref: ${it.trim()}$unitSuffix" }
                            } else {
                              "Ref: $cleanRef$unitSuffix"
                            }
                            Text(refDisplay, fontSize = 9.sp, lineHeight = 12.sp, color = SlateMuted)
                          }
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
                  val slipUri = item.attachedImageUri
                  Spacer(modifier = Modifier.height(6.dp))
                  AsyncImage(
                    model = slipUri,
                    contentDescription = "Slip Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(4.dp))
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Button(
                      onClick = {
                        LabPrintHelper.printLabReportA4(
                          context = context,
                          order = order,
                          clinicSettings = clinicSettings
                        )
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                      shape = RoundedCornerShape(4.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                      modifier = Modifier.weight(1f).height(30.dp)
                    ) {
                      Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("စလစ်ပါ A4 Report ထုတ်ရန်", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                      onClick = {
                        LabPrintHelper.printAttachedImage(
                          context = context,
                          imageUri = slipUri,
                          jobTitle = "Lab_Slip_${order.orderNumber}"
                        )
                      },
                      shape = RoundedCornerShape(4.dp),
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Text("စလစ်တည်း", fontSize = 9.5.sp)
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

// -------------------------------------------------------------
// Component: Mobile Lab Invoices & Expenses Ledger Tab Content
// -------------------------------------------------------------
private data class LabLedgerEntry(
  val id: String,
  val isIncome: Boolean,
  val timestamp: String,
  val title: String,
  val subtitle: String,
  val tag: String,
  val amount: Double,
  val invoice: LabInvoice? = null,
  val expense: ExpenseRecord? = null
)

@Composable
fun MobileLabInvoicesTabContent(
  invoices: List<LabInvoice>,
  expenses: List<ExpenseRecord> = emptyList(),
  clinicSettings: ClinicSettings,
  currencyFmt: NumberFormat,
  onPrintVoucher: (LabInvoice) -> Unit = {},
  onAddExpense: () -> Unit = {},
  onDeleteInvoice: (LabInvoice) -> Unit = {}
) {
  var transactionType by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
  var invoiceToDelete by remember { mutableStateOf<LabInvoice?>(null) }

  val totalIncome = remember(invoices) { invoices.sumOf { it.paidAmount } }
  val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }
  val netProfit = totalIncome - totalExpense

  val ledgerEntries = remember(invoices, expenses, transactionType) {
    val list = mutableListOf<LabLedgerEntry>()
    if (transactionType == "ALL" || transactionType == "INCOME") {
      invoices.forEach { inv ->
        list.add(
          LabLedgerEntry(
            id = "INV_${inv.id}",
            isIncome = true,
            timestamp = inv.timestamp,
            title = inv.invoiceNumber,
            subtitle = buildString {
              append("${inv.patientName} (${inv.patientHn})")
              val extra = listOfNotNull(
                inv.patientPhone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                inv.patientAddress.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
              ).joinToString(" • ")
              if (extra.isNotBlank()) append(" • $extra")
            },
            tag = inv.paymentMethod.label,
            amount = inv.paidAmount,
            invoice = inv
          )
        )
      }
    }
    if (transactionType == "ALL" || transactionType == "EXPENSE") {
      expenses.forEach { exp ->
        list.add(
          LabLedgerEntry(
            id = "EXP_${exp.id}",
            isIncome = false,
            timestamp = exp.date.ifBlank { exp.timestamp },
            title = exp.voucherNo.ifBlank { exp.title },
            subtitle = if (exp.payee.isNotBlank()) "${exp.title} (${exp.payee})" else exp.title,
            tag = exp.category,
            amount = exp.amount,
            expense = exp
          )
        )
      }
    }
    list.sortedByDescending { it.timestamp }
  }

  Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
    // Header & Quick Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        "ဓာတ်ခွဲခန်း ငွေစာရင်း (Lab Ledger)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )
      OutlinedButton(
        onClick = onAddExpense,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp),
        border = BorderStroke(1.dp, Color(0xFFEA580C))
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("Lab စရိတ်ထည့်ရန်", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 3 KPI Cards Row (Income, Expense, Net)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // 1. Income
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(6.dp)) {
          Text("ရငွေ (Income)", fontSize = 9.sp, color = Color(0xFF6D28D9))
          Text(
            "${currencyFmt.format(totalIncome.toInt())} Ks",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4C1D95),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text("${invoices.size} Invoices", fontSize = 8.5.sp, color = Color(0xFF7C3AED))
        }
      }

      // 2. Expense
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(6.dp)) {
          Text("စရိတ် (Expense)", fontSize = 9.sp, color = Color(0xFFEA580C))
          Text(
            "${currencyFmt.format(totalExpense.toInt())} Ks",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC2410C),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text("${expenses.size} Items", fontSize = 8.5.sp, color = Color(0xFFEA580C))
        }
      }

      // 3. Net
      val isProfit = netProfit >= 0
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isProfit) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, if (isProfit) Color(0xFFBBF7D0) else Color(0xFFFECACA)),
        modifier = Modifier.weight(1f)
      ) {
        Column(modifier = Modifier.padding(6.dp)) {
          Text("အသားတင် (Net)", fontSize = 9.sp, color = if (isProfit) Color(0xFF15803D) else Color(0xFFDC2626))
          Text(
            "${currencyFmt.format(netProfit.toInt())} Ks",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isProfit) Color(0xFF14532D) else Color(0xFF991B1B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            if (isProfit) "အမြတ် (Profit)" else "အရှုံး (Deficit)",
            fontSize = 8.5.sp,
            color = if (isProfit) Color(0xFF16A34A) else Color(0xFFDC2626)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Transaction Type Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      listOf(
        Pair("ALL", "အားလုံး (${invoices.size + expenses.size})"),
        Pair("INCOME", "ရငွေ Invoices (${invoices.size})"),
        Pair("EXPENSE", "စရိတ် Expenses (${expenses.size})")
      ).forEach { (typeCode, label) ->
        val isSel = transactionType == typeCode
        FilterChip(
          selected = isSel,
          onClick = { transactionType = typeCode },
          label = { Text(label, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (typeCode == "EXPENSE") Color(0xFFEA580C) else Color(0xFF7C3AED),
            selectedLabelColor = PureWhite
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(24.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (ledgerEntries.isEmpty()) {
      MobileEmptyState(
        icon = Icons.Default.Receipt,
        title = "ဓာတ်ခွဲခန်း ငွေစာရင်း မှတ်တမ်း မရှိသေးပါ",
        description = "စစ်ဆေးမှုများအား ငွေရှင်းခြင်း သို့မဟုတ် စရိတ်မှတ်တမ်းတင်ခြင်း ပြုလုပ်ပါက ဤနေရာတွင် ပေါ်လာပါမည်။"
      )
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(ledgerEntries, key = { it.id }) { entry ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, if (entry.isIncome) Color(0xFFE2E8F0) else Color(0xFFFFEDD5)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (entry.isIncome) Color(0xFFF3E8FF) else Color(0xFFFFEDD5)
                  ) {
                    Text(
                      if (entry.isIncome) "+ ရငွေ" else "- စရိတ်",
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (entry.isIncome) Color(0xFF7E22CE) else Color(0xFFC2410C),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    entry.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isIncome) Color(0xFF4C1D95) else Color(0xFFC2410C)
                  )
                  if (entry.tag.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                      shape = RoundedCornerShape(3.dp),
                      color = Color(0xFFF1F5F9)
                    ) {
                      Text(
                        entry.tag,
                        fontSize = 8.5.sp,
                        color = SlateMuted,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  entry.subtitle,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFF1E293B)
                )
                Text(entry.timestamp, fontSize = 9.sp, color = SlateMuted)
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  "${if (entry.isIncome) "+" else "-"}${currencyFmt.format(entry.amount.toInt())} Ks",
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (entry.isIncome) Color(0xFF15803D) else Color(0xFFDC2626)
                )
                if (entry.isIncome && entry.invoice != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                      onClick = { onPrintVoucher(entry.invoice) },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                      shape = RoundedCornerShape(4.dp),
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                      modifier = Modifier.height(26.dp)
                    ) {
                      Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(10.dp))
                      Spacer(modifier = Modifier.width(2.dp))
                      Text("Thermal", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                      onClick = { invoiceToDelete = entry.invoice },
                      modifier = Modifier.size(26.dp)
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
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

  // Confirm Delete Invoice Dialog
  invoiceToDelete?.let { inv ->
    AlertDialog(
      onDismissRequest = { invoiceToDelete = null },
      title = { Text("ဘောင်ချာဖျက်ရန် အတည်ပြုပါ", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
      text = {
        Text(
          "ဘောင်ချာနံပါတ် ${inv.invoiceNumber} (${inv.patientName}) အား စာရင်းမှ ဖျက်ထုတ်ရန် သေချာပါသလား?\n\nဤဘောင်ချာအား ဖျက်လိုက်ပါက သက်ဆိုင်ရာ စစ်ဆေးမှုအော်ဒါ၏ ငွေပေးချေမှုအခြေအနေသည် 'မရှင်းရသေး (Unpaid)' သို့ ပြန်လည်ပြောင်းလဲသွားပါမည်။",
          fontSize = 12.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteInvoice(inv)
            invoiceToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
          Text("ဖျက်မည်", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { invoiceToDelete = null }) {
          Text("မဖျက်တော့ပါ", color = DarkGrayText, fontSize = 11.sp)
        }
      }
    )
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
  onRestoreFromOrders: () -> Unit = {},
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
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
          onClick = onRestoreFromOrders,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("🔄 ဆယ်ယူမည်", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onAddNew,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(11.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("အသစ်ထည့်", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
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
  procedures: List<ProcedureItem> = emptyList(),
  attendingDoctor: String,
  recentDoctorNames: List<String> = emptyList(),
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
  var walkInAddress by remember { mutableStateOf("") }

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

  fun getPriceForTemplate(tmpl: LabTestTemplate): Double {
    val matchedProc = procedures.firstOrNull {
      it.name.trim().equals(tmpl.name.trim(), ignoreCase = true) ||
      it.code.trim().equals(tmpl.id.trim(), ignoreCase = true)
    }
    return matchedProc?.standardFee ?: tmpl.defaultPrice
  }

  val totalAmount = remember(selectedTemplates, procedures) {
    selectedTemplates.sumOf { getPriceForTemplate(it) }
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
                  Column(modifier = Modifier.weight(1f)) {
                    Text(selectedPatient!!.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    Text("HN: ${selectedPatient!!.hn} • Age: ${selectedPatient!!.age} • ${selectedPatient!!.gender.name}", fontSize = 10.sp, color = Color(0xFF0284C7))
                    val patContact = listOfNotNull(
                      selectedPatient!!.phone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                      selectedPatient!!.address.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
                    ).joinToString("  •  ")
                    if (patContact.isNotBlank()) {
                      Text(patContact, fontSize = 9.5.sp, color = Color(0xFF0369A1))
                    }
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
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text("${pat.name} (${pat.hn})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        val patContact = listOfNotNull(
                          pat.phone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                          pat.address.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
                        ).joinToString(" • ")
                        if (patContact.isNotBlank()) {
                          Text(patContact, fontSize = 9.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                      }
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
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = walkInPhone,
              onValueChange = { walkInPhone = it },
              label = { Text("ဖုန်းနံပါတ် (Phone Number)", fontSize = 11.sp) },
              placeholder = { Text("09xxxxxxxxx", fontSize = 11.sp, color = SlateMuted) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
              value = walkInAddress,
              onValueChange = { walkInAddress = it },
              label = { Text("နေရပ်လိပ်စာ (Address)", fontSize = 11.sp) },
              placeholder = { Text("မြို့နယ် / လိပ်စာအသေးစိတ်", fontSize = 11.sp, color = SlateMuted) },
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
                    "${tmpl.name} (${currencyFmt.format(getPriceForTemplate(tmpl).toInt())} Ks)",
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

          // ညွှန်းပို့သည့် ဆရာဝန် (Referred Dr. Name)
          Text(
            "ညွှန်းပို့သည့် ဆရာဝန် (Referred Dr. Name)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
          )
          Spacer(modifier = Modifier.height(4.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
          ) {
            val clinicDocLabel = attendingDoctor.ifBlank { "ဆေးခန်းဆရာဝန်" }
            val isClinicDoc = doctorName == clinicDocLabel
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isClinicDoc) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
              border = BorderStroke(0.6.dp, if (isClinicDoc) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
              modifier = Modifier.clickable { doctorName = clinicDocLabel }
            ) {
              Text(
                "🏥 ဆေးခန်း ($clinicDocLabel)",
                fontSize = 10.sp,
                fontWeight = if (isClinicDoc) FontWeight.Bold else FontWeight.Normal,
                color = if (isClinicDoc) Color(0xFF0369A1) else Color(0xFF334155),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }

            val isSelf = doctorName.equals("Self-request", ignoreCase = true) || doctorName.contains("ကိုယ်တိုင်")
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelf) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
              border = BorderStroke(0.6.dp, if (isSelf) Color(0xFFD97706) else Color(0xFFCBD5E1)),
              modifier = Modifier.clickable { doctorName = "Self-request" }
            ) {
              Text(
                "👤 လူနာကိုယ်တိုင် (Self)",
                fontSize = 10.sp,
                fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelf) Color(0xFFB45309) else Color(0xFF334155),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }

            val isOutside = doctorName.startsWith("Dr.") && doctorName != clinicDocLabel
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isOutside) Color(0xFFF3E8FF) else Color(0xFFF1F5F9),
              border = BorderStroke(0.6.dp, if (isOutside) Color(0xFF9333EA) else Color(0xFFCBD5E1)),
              modifier = Modifier.clickable {
                if (!doctorName.startsWith("Dr.")) {
                  doctorName = "Dr. "
                }
              }
            ) {
              Text(
                "🩺 Dr. (အပြင်ဆရာဝန်)",
                fontSize = 10.sp,
                fontWeight = if (isOutside) FontWeight.Bold else FontWeight.Normal,
                color = if (isOutside) Color(0xFF7E22CE) else Color(0xFF334155),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }

            recentDoctorNames.filter { it != clinicDocLabel && !it.equals("Self-request", ignoreCase = true) }.take(3).forEach { doc ->
              val isSelected = doctorName == doc
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                border = BorderStroke(0.6.dp, if (isSelected) Color(0xFF16A34A) else Color(0xFFE2E8F0)),
                modifier = Modifier.clickable { doctorName = doc }
              ) {
                Text(
                  doc,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) Color(0xFF15803D) else Color(0xFF475569),
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = doctorName,
            onValueChange = { doctorName = it },
            label = { Text("ညွှန်းပို့သည့် ဆရာဝန် (Referred Dr. Name)", fontSize = 11.sp) },
            placeholder = { Text("e.g. Dr. Zaw Min / ဆေးခန်းဆရာဝန် / Self-request", fontSize = 10.5.sp, color = SlateMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A)),
            leadingIcon = {
              Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
            },
            trailingIcon = {
              if (doctorName.isNotBlank()) {
                IconButton(onClick = { doctorName = "" }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = SlateMuted)
                }
              }
            },
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
                val pPhone = if (isWalkIn) walkInPhone.trim() else (selectedPatient?.phone ?: "")
                val pAddress = if (isWalkIn) walkInAddress.trim().ifBlank { "ပြင်ပလူနာ (Walk-in)" } else (selectedPatient?.address?.ifBlank { "မန္တလေးမြို့" } ?: "မန္တလေးမြို့")

                val items = selectedTemplates.map { t ->
                  LabOrderItem(
                    testId = t.id,
                    testName = t.name,
                    category = t.category,
                    price = getPriceForTemplate(t),
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
                  patientPhone = pPhone,
                  patientAddress = pAddress,
                  patientAge = pAge,
                  patientGender = pGender,
                  doctorName = doctorName.trim().ifBlank { if (isWalkIn) "Self-request" else attendingDoctor.ifBlank { "Clinic Doctor" } },
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
// Quick Qualitative Presets Helper for Urine RE/ME, Stool RE/ME & Serology
// -------------------------------------------------------------
private fun getQuickPresetsForParam(paramName: String, specimenType: String = "", testName: String = ""): List<String> {
  val n = paramName.lowercase()
  val s = specimenType.lowercase()
  val t = testName.lowercase()

  // CBC and Blood count tests must NOT have urine/stool microscopy presets
  if (t.contains("cbc") || t.contains("complete blood") || s.contains("edta") || (s.contains("blood") && !t.contains("widal") && !t.contains("malaria") && !t.contains("dengue") && !t.contains("serology"))) {
    return emptyList()
  }

  return when {
    n.contains("protein") || n.contains("albumin") || n.contains("glucose") || n.contains("sugar") || n.contains("ketone") -> {
      listOf("Nil", "Trace", "+", "++", "+++")
    }
    n.contains("pus") || (n.contains("wbc") && (s.contains("urine") || s.contains("stool") || n.contains("/hpf") || n.contains("pus"))) -> {
      listOf("Nil", "0 - 2", "2 - 4", "5 - 10", "10 - 15", "Numerous")
    }
    (n.contains("rbc") || n.contains("red blood")) && (s.contains("urine") || s.contains("stool") || n.contains("/hpf") || n.contains("urine") || n.contains("stool")) -> {
      listOf("Nil", "0 - 1", "1 - 2", "3 - 5", "Numerous")
    }
    n.contains("epithelial") || n.contains("cast") || n.contains("crystal") || n.contains("bacteria") || n.contains("cyst") || n.contains("trophozoite") || n.contains("ova") || n.contains("macrophage") || n.contains("fat") -> {
      listOf("Nil seen", "Few", "Occasional", "Moderate", "Many", "Seen")
    }
    n.contains("color") || n.contains("colour") -> {
      if (s.contains("stool") || n.contains("stool")) listOf("Brown", "Yellowish", "Dark Brown", "Clay")
      else listOf("Pale Yellow", "Straw", "Amber", "Reddish")
    }
    n.contains("appearance") -> {
      listOf("Clear", "Slightly Turbid", "Turbid")
    }
    n.contains("consistency") -> {
      listOf("Formed", "Semi-formed", "Soft", "Loose", "Watery")
    }
    n.contains("mucus") || (n.contains("blood") && (s.contains("stool") || s.contains("urine"))) -> {
      listOf("Absent", "Present", "Negative", "Positive")
    }
    n.contains("bilirubin") || n.contains("urobilinogen") || n.contains("widal") || n.contains("dengue") || n.contains("malaria") || n.contains("hbsag") || n.contains("hcv") || n.contains("hiv") -> {
      listOf("Negative", "Positive", "Normal")
    }
    else -> emptyList()
  }
}

// -------------------------------------------------------------
// Dialog: Mobile Lab Result Entry Form (Card-based parameter inputs)
// -------------------------------------------------------------
@Composable
fun MobileLabResultEntryDialog(
  order: LabOrder,
  technicianName: String,
  defaultPathologistName: String = "",
  defaultPathologistDegree: String = "",
  defaultPathologistSama: String = "",
  defaultPathologistTitle: String = "",
  onDismiss: () -> Unit,
  onSave: (List<LabOrderItem>, String, String, String, String, String, String, Boolean, Boolean, String) -> Unit
) {
  val context = LocalContext.current
  var referringDoctor by remember { mutableStateOf(order.referringDoctorName) }
  var techName by remember { mutableStateOf(order.technicianName.ifBlank { technicianName }) }
  var pathologistName by remember { mutableStateOf(order.pathologistName.ifBlank { defaultPathologistName }) }
  var pathologistDegree by remember { mutableStateOf(order.pathologistDegree.ifBlank { defaultPathologistDegree }) }
  var pathologistSama by remember { mutableStateOf(order.pathologistSama.ifBlank { defaultPathologistSama }) }
  var pathologistTitle by remember { mutableStateOf(order.pathologistTitle.ifBlank { defaultPathologistTitle }) }
  var showDoctorSeal by remember { mutableStateOf(order.showDoctorSeal) }
  var showTechnicianSign by remember { mutableStateOf(order.showTechnicianSign) }
  var generalNotes by remember { mutableStateOf(order.notes) }

  // Working state for items and parameters
  val itemsState = remember(order.id) {
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
            val resultContact = listOfNotNull(
              order.patientPhone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
              order.patientAddress.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
            ).joinToString("  •  ")
            if (resultContact.isNotBlank()) {
              Text(resultContact, fontSize = 9.5.sp, color = Color(0xFF0369A1))
            }
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
          // Pathologist Verification Signature Input Card (Top of dialog)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = showDoctorSeal,
                  onCheckedChange = { showDoctorSeal = it },
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("A4 Report တွင် ဆရာဝန်/Pathologist Seal ထည့်သွင်းမည်", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
              }

              if (showDoctorSeal) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedTextField(
                    value = pathologistName,
                    onValueChange = { pathologistName = it },
                    label = { Text("Pathologist အမည်", fontSize = 10.sp) },
                    placeholder = { Text("Dr. ...") },
                    modifier = Modifier.weight(1.3f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
                  )
                  OutlinedTextField(
                    value = pathologistSama,
                    onValueChange = { pathologistSama = it },
                    label = { Text("ဆမ (S.A.M.A)", fontSize = 10.sp) },
                    modifier = Modifier.weight(0.7f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = pathologistDegree,
                  onValueChange = { pathologistDegree = it },
                  label = { Text("ဘွဲ့ (Medical Degree)", fontSize = 10.sp) },
                  placeholder = { Text("M.B.,B.S, M.Med.Sc (Pathology)") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  shape = RoundedCornerShape(6.dp),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = pathologistTitle,
                  onValueChange = { pathologistTitle = it },
                  label = { Text("ရာထူး (Title / Position - e.g. Consultant Pathologist)", fontSize = 10.sp) },
                  placeholder = { Text("မလိုပါက ကွက်လပ်ထားနိုင်ပါသည်") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  shape = RoundedCornerShape(6.dp),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
                )
              }
            }
          }

          // Medical Technologist Card
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = showTechnicianSign,
                  onCheckedChange = { showTechnicianSign = it },
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("A4 Report တွင် Medical Technologist လက်မှတ် ထည့်သွင်းမည်", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
              }
              if (showTechnicianSign) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = techName,
                  onValueChange = { techName = it },
                  label = { Text("စစ်ဆေးသူ ဓာတ်ခွဲပညာရှင် (Technologist အမည်)", fontSize = 11.sp) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = Color(0xFF0F172A))
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Referring Doctor Card
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("ညွှန်းပို့သည့် ဆရာဝန် (Referred Dr. Name)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = referringDoctor,
                onValueChange = { referringDoctor = it },
                label = { Text("Referred Doctor / Self", fontSize = 11.sp) },
                placeholder = { Text("e.g. Dr. Zaw Min / ဆေးခန်းဆရာဝန် / Self-request", fontSize = 10.5.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = Color(0xFF0F172A)),
                leadingIcon = {
                  Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0284C7))
                },
                trailingIcon = {
                  if (referringDoctor.isNotBlank()) {
                    IconButton(onClick = { referringDoctor = "" }, modifier = Modifier.size(24.dp)) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = SlateMuted)
                    }
                  }
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(6.dp))

                if (item.parameters.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))

                  // Each Parameter is an individual mobile input card
                  item.parameters.forEachIndexed { pIdx, p ->
                    val showSectionHeader = p.section.isNotBlank() && (pIdx == 0 || item.parameters[pIdx - 1].section != p.section)
                    if (showSectionHeader) {
                      var isEditingSection by remember { mutableStateOf(false) }
                      var sectionTitleText by remember(p.section) { mutableStateOf(p.section) }

                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          if (isEditingSection) {
                            OutlinedTextField(
                              value = sectionTitleText,
                              onValueChange = { sectionTitleText = it },
                              modifier = Modifier.weight(1f),
                              singleLine = true,
                              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                            )
                            IconButton(
                              onClick = {
                                isEditingSection = false
                                val oldSec = p.section
                                val updatedParams = item.parameters.map { param ->
                                  if (param.section == oldSec) param.copy(section = sectionTitleText) else param
                                }
                                itemsState[itemIdx] = item.copy(parameters = updatedParams)
                              },
                              modifier = Modifier.size(28.dp)
                            ) {
                              Icon(Icons.Default.Check, contentDescription = "Save Section Title", modifier = Modifier.size(16.dp), tint = Color(0xFF16A34A))
                            }
                          } else {
                            Text(
                              "▶ ${p.section}",
                              fontSize = 11.5.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color(0xFF1D4ED8),
                              modifier = Modifier.weight(1f)
                            )
                            IconButton(
                              onClick = { isEditingSection = true },
                              modifier = Modifier.size(28.dp)
                            ) {
                              Icon(Icons.Default.Edit, contentDescription = "Edit Subtitle", modifier = Modifier.size(14.dp), tint = Color(0xFF1D4ED8))
                            }
                          }
                        }
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = PureWhite,
                      border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                      modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                      Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.Top
                        ) {
                          Text(p.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
                          if (p.referenceRange.isNotBlank()) {
                            val cleanRef = p.referenceRange.trim()
                            val unitSuffix = if (p.unit.isNotBlank()) " ${p.unit}" else ""
                            val refDisplay = if (cleanRef.contains("\n")) {
                              cleanRef.split("\n").joinToString("\n") { "Ref: ${it.trim()}$unitSuffix" }
                            } else {
                              "Ref: $cleanRef$unitSuffix"
                            }
                            Text(
                              refDisplay,
                              fontSize = 9.sp,
                              lineHeight = 12.sp,
                              color = SlateMuted,
                              textAlign = TextAlign.End,
                              modifier = Modifier.padding(start = 6.dp)
                            )
                          }
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

                              // Auto-calculate eGFR (CKD-EPI 2021) when Creatinine is entered
                              val pNameClean = p.name.trim().lowercase()
                              if (pNameClean.contains("creatinine") || pNameClean == "cr") {
                                val crVal = newVal.toDoubleOrNull()
                                val ageYears = if (order.patientAge > 0) order.patientAge else 35
                                val isFemale = order.patientGender.name.contains("FEMALE", ignoreCase = true)
                                if (crVal != null && crVal > 0.0) {
                                  val k = if (isFemale) 0.7 else 0.9
                                  val alpha = if (isFemale) -0.241 else -0.302
                                  val femaleFactor = if (isFemale) 1.012 else 1.0
                                  val crRatio = crVal / k
                                  val term1 = Math.pow(Math.min(crRatio, 1.0), alpha)
                                  val term2 = Math.pow(Math.max(crRatio, 1.0), -1.200)
                                  val term3 = Math.pow(0.9938, ageYears.toDouble())
                                  val egfr = Math.round(142.0 * term1 * term2 * term3 * femaleFactor * 10.0) / 10.0

                                  val egfrIdx = updatedParams.indexOfFirst { it.name.contains("egfr", ignoreCase = true) }
                                  if (egfrIdx >= 0) {
                                    updatedParams[egfrIdx] = updatedParams[egfrIdx].copy(
                                      result = "$egfr",
                                      flag = if (egfr < 60.0) "LOW" else "NORMAL"
                                    )
                                  } else {
                                    updatedParams.add(
                                      LabTestParameter(
                                        name = "eGFR (CKD-EPI 2021)",
                                        result = "$egfr",
                                        unit = "mL/min/1.73m²",
                                        referenceRange = "≥ 90",
                                        flag = if (egfr < 60.0) "LOW" else "NORMAL"
                                      )
                                    )
                                  }
                                }
                              }
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

                        // Quick Qualitative Presets Chips (Urine / Stool / Serology only)
                        val quickPresets = remember(p.name, item.specimenType, item.testName) {
                          getQuickPresetsForParam(p.name, item.specimenType, item.testName)
                        }
                        if (quickPresets.isNotEmpty()) {
                          Spacer(modifier = Modifier.height(4.dp))
                          Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                          ) {
                            quickPresets.forEach { preset ->
                              val isSelected = p.result.equals(preset, ignoreCase = true)
                              Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                border = BorderStroke(0.6.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable {
                                  val updatedParams = item.parameters.toMutableList()
                                  updatedParams[pIdx] = p.copy(result = preset)
                                  itemsState[itemIdx] = item.copy(parameters = updatedParams)
                                }
                              ) {
                                Text(
                                  preset,
                                  fontSize = 10.sp,
                                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                  color = if (isSelected) PureWhite else Color(0xFF334155),
                                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
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

          Spacer(modifier = Modifier.height(4.dp))
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
              onSave(
                finalizedItems,
                techName,
                generalNotes,
                pathologistName,
                pathologistDegree,
                pathologistSama,
                pathologistTitle,
                showDoctorSeal,
                showTechnicianSign,
                referringDoctor
              )
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
        val invoiceContact = listOfNotNull(
          order.patientPhone.takeIf { it.isNotBlank() }?.let { "📞 $it" },
          order.patientAddress.takeIf { it.isNotBlank() }?.let { "🏠 $it" }
        ).joinToString("  •  ")
        if (invoiceContact.isNotBlank()) {
          Text(invoiceContact, fontSize = 9.5.sp, color = SlateMuted)
        }
        Text("ညွှန်းပို့ဆရာဝန်: ${order.referringDoctorName.ifBlank { "လူနာကိုယ်တိုင် / Direct" }}", fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Medium)
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
          Text("Discount (လျှော့ငွေ Ks):", fontSize = 11.sp, fontWeight = FontWeight.Medium)
          OutlinedTextField(
            value = discountText,
            onValueChange = {
              discountText = it
              val d = it.toDoubleOrNull() ?: 0.0
              val newNet = (subtotal - d).coerceAtLeast(0.0)
              paidAmountText = newNet.toInt().toString()
            },
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
        Spacer(modifier = Modifier.height(4.dp))

        // Quick Discount Chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          listOf(
            "Clear" to 0.0,
            "5%" to subtotal * 0.05,
            "10%" to subtotal * 0.10,
            "15%" to subtotal * 0.15,
            "20%" to subtotal * 0.20,
            "3,000 Ks" to 3000.0,
            "5,000 Ks" to 5000.0,
            "FOC (100%)" to subtotal
          ).forEach { (label, discVal) ->
            val isCurrent = (discountText.toDoubleOrNull() ?: 0.0).toInt() == discVal.toInt() && (discVal > 0 || discountText == "0")
            FilterChip(
              selected = isCurrent,
              onClick = {
                discountText = discVal.toInt().toString()
                val newNet = (subtotal - discVal).coerceAtLeast(0.0)
                paidAmountText = newNet.toInt().toString()
              },
              label = { Text(label, fontSize = 9.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal) },
              modifier = Modifier.height(24.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Net Total (ကျသင့်ငွေ):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                patientPhone = order.patientPhone,
                patientAddress = order.patientAddress,
                referringDoctorName = order.referringDoctorName,
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
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("ပုံမှန်ပမာဏ (Reference Range / Interval):", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0xFFEFF6FF),
                      border = BorderStroke(0.6.dp, Color(0xFF93C5FD)),
                      modifier = Modifier.clickable {
                        val current = param.referenceRange.trim()
                        val template = "M: \nF: \nChild: "
                        parameters[pIdx] = param.copy(referenceRange = if (current.isBlank()) template else "$current\n$template")
                      }
                    ) {
                      Text("+ M/F/Child", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = Color(0xFFF0FDF4),
                      border = BorderStroke(0.6.dp, Color(0xFF86EFAC)),
                      modifier = Modifier.clickable {
                        val current = param.referenceRange.trim()
                        val template = "M: \nF: "
                        parameters[pIdx] = param.copy(referenceRange = if (current.isBlank()) template else "$current\n$template")
                      }
                    ) {
                      Text("+ M/F", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = param.referenceRange,
                  onValueChange = { parameters[pIdx] = param.copy(referenceRange = it) },
                  placeholder = { Text("ဥပမာ -\nM: 13.0 - 17.5\nF: 12.0 - 15.5\nChild: 11.5 - 14.5", fontSize = 11.sp, color = SlateMuted, lineHeight = 14.sp) },
                  singleLine = false,
                  minLines = 2,
                  maxLines = 5,
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color(0xFF0F172A), lineHeight = 16.sp),
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
                isAnalyzerSlipOnly = false,
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

// -------------------------------------------------------------
// Dialog: Mobile Lab Expense Recording Dialog
// -------------------------------------------------------------
@Composable
fun MobileLabExpenseDialog(
  onDismiss: () -> Unit,
  onSaveExpense: (String, Double, String, String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var payee by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier.fillMaxWidth(0.92f).padding(8.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("🧪 Lab ကုန်ကျစရိတ် မှတ်တမ်းတင်ခြင်း", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
          }
        }
        Text("Reagents, Test Kits, Tubes, စက်ပစ္စည်း နှင့် စစ်ဆေးခ ကုန်ကျစရိတ်များ", fontSize = 10.sp, color = SlateMuted)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(8.dp))

        // Quick Category / Title chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf("CBC Reagent Pack", "Electrolyte Reagents", "Biochemistry Test Kits", "Blood Collection Tubes", "Syringes & Needles", "External Reference Lab Fee").forEach { t ->
            AssistChip(
              onClick = { title = t },
              label = { Text(t, fontSize = 9.5.sp) }
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("အကြောင်းအရာ (Expense Title)", fontSize = 11.sp) },
          placeholder = { Text("ဥပမာ - CBC Diluent Reagents") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("ကုန်ကျငွေ (Amount Ks)", fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1.2f)
          )
          OutlinedTextField(
            value = payee,
            onValueChange = { payee = it },
            label = { Text("ပေးချေသူ/ဆိုင် (Payee)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("အသေးစိတ် မှတ်ချက် (Notes)", fontSize = 11.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.height(36.dp)) {
            Text("မသိမ်းပါ", fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val amt = amountText.toDoubleOrNull() ?: 0.0
              if (title.isNotBlank() && amt > 0) {
                onSaveExpense(title.trim(), amt, payee.trim(), notes.trim())
              }
            },
            enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
            modifier = Modifier.height(36.dp)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("စရိတ် သိမ်းဆည်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
