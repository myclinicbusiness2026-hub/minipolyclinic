package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Invoice
import com.example.model.PaymentMethod
import com.example.ui.components.WalkInSaleDialog
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * CashierBillingScreen
 * 
 * Compact, Clinical POS Station for "ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း
 * Doctor: Dr. Nyan Kaung Sett M.B.,B.S(MDY) (ဆမ-၄၇၄၁၄)
 * 
 * Adaptive layout:
 * - Mobile (< 760dp): Segmented toggle [Pending Queue] <-> [Checkout POS]
 * - Tablet (>= 760dp): Balanced 2-Column POS layout
 */
@Composable
fun CashierBillingScreen(
  viewModel: ClinicViewModel
) {
  val invoices by viewModel.invoices.collectAsState()
  val patients by viewModel.patients.collectAsState()
  val consultations by viewModel.consultations.collectAsState()
  val medicines by viewModel.medicines.collectAsState()

  var selectedInvoiceId by remember { mutableStateOf<String?>(null) }
  var showWalkInSaleDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedTabFilter by remember { mutableStateOf(0) } // 0: Pending, 1: Settled, 2: All
  var selectedDateFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, YESTERDAY, PAST_7_DAYS, THIS_MONTH, CUSTOM
  var customDateFilter by remember { mutableStateOf<String?>(null) }
  var showCustomDatePicker by remember { mutableStateOf(false) }
  var mobileActiveTab by remember { mutableStateOf(0) } // 0: Queue, 1: Checkout

  val todayIso = remember { java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()) }
  val yesterdayIso = remember {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }
  val sevenDaysAgoIso = remember {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
  }
  val thisMonthPrefix = remember { java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(java.util.Date()) }

  val filteredInvoices = remember(invoices, searchQuery, selectedTabFilter, selectedDateFilter, customDateFilter, patients) {
    invoices.filter { inv ->
      val matchesTab = when (selectedTabFilter) {
        0 -> !inv.isPaid
        1 -> inv.isPaid
        else -> true
      }
      val matchesDate = when (selectedDateFilter) {
        "ALL" -> true
        "TODAY" -> inv.dateString == todayIso
        "YESTERDAY" -> inv.dateString == yesterdayIso
        "PAST_7_DAYS" -> inv.dateString >= sevenDaysAgoIso
        "THIS_MONTH" -> inv.dateString.startsWith(thisMonthPrefix)
        "CUSTOM" -> customDateFilter == null || inv.dateString == customDateFilter
        else -> true
      }
      val patient = patients.find { it.id == inv.patientId }
      val matchesQuery = searchQuery.isBlank() ||
          inv.invoiceNo.contains(searchQuery, ignoreCase = true) ||
          inv.patientName.contains(searchQuery, ignoreCase = true) ||
          (patient != null && (patient.hn.contains(searchQuery, ignoreCase = true) || patient.phone.contains(searchQuery, ignoreCase = true)))
      matchesTab && matchesDate && matchesQuery
    }
  }

  val activeInvoice = remember(filteredInvoices, selectedInvoiceId, invoices) {
    invoices.find { it.id == selectedInvoiceId }
      ?: filteredInvoices.firstOrNull { !it.isPaid }
      ?: filteredInvoices.firstOrNull()
      ?: invoices.firstOrNull { !it.isPaid }
      ?: invoices.firstOrNull()
  }

  val activePatient = remember(activeInvoice, patients) {
    activeInvoice?.let { inv -> patients.find { it.id == inv.patientId } }
  }
  val activeConsultation = remember(activeInvoice, consultations) {
    activeInvoice?.let { inv -> consultations.find { it.patientId == inv.patientId } }
  }

  var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var discountPercentText by remember { mutableStateOf("0") }

  LaunchedEffect(activeInvoice?.id) {
    activeInvoice?.let { inv ->
      selectedPaymentMethod = inv.paymentMethod
      discountPercentText = if (inv.discountPercent > 0) inv.discountPercent.toInt().toString() else "0"
    }
  }

  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)
  val unpaidCount = invoices.count { !it.isPaid }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(BgLightGray)
  ) {
    val isWideScreen = maxWidth >= 760.dp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      // 1. Responsive Header Bar
      if (isWideScreen) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.PointOfSale,
                contentDescription = null,
                tint = MedicalGreen,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "Cashier Billing POS",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = DarkGrayText
              )
              Text(
                "Shwe Khit Clinic • Dr. Nyan Kaung Sett (SAMA: 47414)",
                fontSize = 10.sp,
                color = MedicalGreenDark
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
              onClick = { showWalkInSaleDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ဆေးသီးသန့်အရောင်း (Walk-in OTC)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (unpaidCount > 0) StatusWarningContainer else SoftGreenContainer,
              border = BorderStroke(1.dp, if (unpaidCount > 0) StatusWarning.copy(alpha = 0.5f) else MedicalGreen.copy(alpha = 0.3f)),
              modifier = Modifier.height(24.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (unpaidCount > 0) StatusWarning else MedicalGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  if (unpaidCount > 0) "$unpaidCount Pending" else "All Paid",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (unpaidCount > 0) StatusWarning else MedicalGreen
                )
              }
            }
          }
        }
      } else {
        // Mobile Header (Two Rows so button never gets clipped or hidden)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(SoftGreenContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.PointOfSale,
                  contentDescription = null,
                  tint = MedicalGreen,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  "Cashier Billing POS",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.5.sp,
                  color = DarkGrayText
                )
                Text(
                  "Shwe Khit Clinic",
                  fontSize = 9.5.sp,
                  color = MedicalGreenDark
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (unpaidCount > 0) StatusWarningContainer else SoftGreenContainer,
              border = BorderStroke(1.dp, if (unpaidCount > 0) StatusWarning.copy(alpha = 0.5f) else MedicalGreen.copy(alpha = 0.3f)),
              modifier = Modifier.height(22.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (unpaidCount > 0) StatusWarning else MedicalGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  if (unpaidCount > 0) "$unpaidCount Pending" else "All Paid",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (unpaidCount > 0) StatusWarning else MedicalGreen
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Button(
            onClick = { showWalkInSaleDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(30.dp)
          ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ဆေးသီးသန့်အရောင်း (Direct Walk-in OTC POS)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      if (showWalkInSaleDialog) {
        WalkInSaleDialog(
          medicines = medicines,
          onDismiss = { showWalkInSaleDialog = false },
          onCompleteSale = { custName, items, pm, disc ->
            val inv = viewModel.createWalkInSaleInvoice(
              customerName = custName,
              items = items,
              paymentMethod = pm,
              discountPercent = disc
            )
            selectedInvoiceId = inv.id
          }
        )
      }

      // Responsive Body
      if (isWideScreen) {
        // ==================== TABLET 2-COLUMN LAYOUT ====================
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Left Column: Queue List
          Card(
            modifier = Modifier
              .weight(5f)
              .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, BorderSubtleRedBrown)
          ) {
            BillingQueuePane(
              invoices = invoices,
              filteredInvoices = filteredInvoices,
              activeInvoice = activeInvoice,
              patients = patients,
              searchQuery = searchQuery,
              onSearchQueryChange = { searchQuery = it },
              selectedTabFilter = selectedTabFilter,
              onTabFilterChange = { selectedTabFilter = it },
              selectedDateFilter = selectedDateFilter,
              onDateFilterChange = { selectedDateFilter = it },
              customDateFilter = customDateFilter,
              onOpenDatePicker = { showCustomDatePicker = true },
              onSelectInvoice = { selectedInvoiceId = it },
              currencyFmt = currencyFmt
            )
          }

          // Right Column: Checkout Inspector
          Card(
            modifier = Modifier
              .weight(7f)
              .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, BorderSubtleRedBrown)
          ) {
            BillingCheckoutPane(
              activeInvoice = activeInvoice,
              activePatient = activePatient,
              activeConsultation = activeConsultation,
              selectedPaymentMethod = selectedPaymentMethod,
              onPaymentMethodChange = { selectedPaymentMethod = it },
              discountPercentText = discountPercentText,
              onDiscountChange = { discountPercentText = it },
              currencyFmt = currencyFmt,
              onConfirmPayment = { inv, method, pct ->
                viewModel.processPayment(inv.id, method, pct)
                val updatedInvoice = inv.copy(isPaid = true, paymentMethod = method, discountPercent = pct)
                viewModel.showThermalPreview(updatedInvoice)
              },
              onConfirmPaymentWithGranular = { inv, method, docD, pharmD, procD, pct ->
                viewModel.processPaymentWithGranularDiscount(inv.id, method, docD, pharmD, procD, pct)
                val totalGranular = docD + pharmD + procD
                val pctDisc = (inv.subTotal - totalGranular).coerceAtLeast(0.0) * (pct / 100.0)
                val finalDisc = totalGranular + pctDisc
                val finalTot = (inv.subTotal - finalDisc).coerceAtLeast(0.0)
                val updatedInvoice = inv.copy(
                  isPaid = true,
                  paymentMethod = method,
                  doctorFeeDiscount = docD,
                  pharmacyDiscount = pharmD,
                  procedureDiscount = procD,
                  discountPercent = pct,
                  discount = finalDisc,
                  totalAmount = finalTot
                )
                viewModel.showThermalPreview(updatedInvoice)
              },
              onApplyDoctorFeeFoc = { inv -> viewModel.applyDoctorFeeFoc(inv.id) },
              onApplyAllFoc = { inv -> viewModel.applyAllFoc(inv.id) },
              onResetDiscounts = { inv -> viewModel.resetInvoiceDiscounts(inv.id) },
              onPrintThermal = { inv ->
                viewModel.showThermalPreview(inv)
              },
              onPrintA4 = { consultation ->
                viewModel.showA4DocPreview(consultation)
              },
              onToggleDocFee = { inv, includeFee, feeAmount ->
                viewModel.updateInvoiceDocumentationFee(inv.id, includeFee, feeAmount)
              },
              onBackToQueue = null,
              onDeleteInvoiceAndReturnStock = { inv, returnedNames, voidAll ->
                val (success, restoredList) = viewModel.returnSelectedInvoiceItemsAndAdjust(inv.id, returnedNames, voidAll)
                if (success && voidAll) {
                  selectedInvoiceId = null
                }
              },
              onToggleInvoicePaid = { invId, isPaid ->
                viewModel.toggleInvoicePaidStatus(invId, isPaid)
              }
            )
          }
        }
      } else {
        // ==================== MOBILE COMPACT LAYOUT ====================
        // Compact Segmented Tabs
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE5E7EB))
            .padding(2.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(32.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(if (mobileActiveTab == 0) PureWhite else Color.Transparent)
              .clickable { mobileActiveTab = 0 },
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (mobileActiveTab == 0) MedicalGreen else SlateMedium
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Patient List (${filteredInvoices.size})",
                fontSize = 11.sp,
                fontWeight = if (mobileActiveTab == 0) FontWeight.Bold else FontWeight.Medium,
                color = if (mobileActiveTab == 0) MedicalGreen else SlateDark
              )
            }
          }

          Box(
            modifier = Modifier
              .weight(1.2f)
              .height(32.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(if (mobileActiveTab == 1) PureWhite else Color.Transparent)
              .clickable { mobileActiveTab = 1 },
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (mobileActiveTab == 1) MedicalGreen else SlateMedium
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                activeInvoice?.let { "Invoice (${it.patientName.take(8)})" } ?: "Invoice",
                fontSize = 11.sp,
                fontWeight = if (mobileActiveTab == 1) FontWeight.Bold else FontWeight.Medium,
                color = if (mobileActiveTab == 1) MedicalGreen else SlateDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Active View Container
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          border = BorderStroke(1.dp, BorderSubtleRedBrown)
        ) {
          if (mobileActiveTab == 0) {
            BillingQueuePane(
              invoices = invoices,
              filteredInvoices = filteredInvoices,
              activeInvoice = activeInvoice,
              patients = patients,
              searchQuery = searchQuery,
              onSearchQueryChange = { searchQuery = it },
              selectedTabFilter = selectedTabFilter,
              onTabFilterChange = { selectedTabFilter = it },
              selectedDateFilter = selectedDateFilter,
              onDateFilterChange = { selectedDateFilter = it },
              customDateFilter = customDateFilter,
              onOpenDatePicker = { showCustomDatePicker = true },
              onSelectInvoice = { id ->
                selectedInvoiceId = id
                mobileActiveTab = 1 // Automatically open checkout inspector on mobile
              },
              currencyFmt = currencyFmt
            )
          } else {
            BillingCheckoutPane(
              activeInvoice = activeInvoice,
              activePatient = activePatient,
              activeConsultation = activeConsultation,
              selectedPaymentMethod = selectedPaymentMethod,
              onPaymentMethodChange = { selectedPaymentMethod = it },
              discountPercentText = discountPercentText,
              onDiscountChange = { discountPercentText = it },
              currencyFmt = currencyFmt,
              onConfirmPayment = { inv, method, pct ->
                viewModel.processPayment(inv.id, method, pct)
                val updatedInvoice = inv.copy(isPaid = true, paymentMethod = method, discountPercent = pct)
                viewModel.showThermalPreview(updatedInvoice)
              },
              onConfirmPaymentWithGranular = { inv, method, docD, pharmD, procD, pct ->
                viewModel.processPaymentWithGranularDiscount(inv.id, method, docD, pharmD, procD, pct)
                val totalGranular = docD + pharmD + procD
                val pctDisc = (inv.subTotal - totalGranular).coerceAtLeast(0.0) * (pct / 100.0)
                val finalDisc = totalGranular + pctDisc
                val finalTot = (inv.subTotal - finalDisc).coerceAtLeast(0.0)
                val updatedInvoice = inv.copy(
                  isPaid = true,
                  paymentMethod = method,
                  doctorFeeDiscount = docD,
                  pharmacyDiscount = pharmD,
                  procedureDiscount = procD,
                  discountPercent = pct,
                  discount = finalDisc,
                  totalAmount = finalTot
                )
                viewModel.showThermalPreview(updatedInvoice)
              },
              onApplyDoctorFeeFoc = { inv -> viewModel.applyDoctorFeeFoc(inv.id) },
              onApplyAllFoc = { inv -> viewModel.applyAllFoc(inv.id) },
              onResetDiscounts = { inv -> viewModel.resetInvoiceDiscounts(inv.id) },
              onPrintThermal = { inv ->
                viewModel.showThermalPreview(inv)
              },
              onPrintA4 = { consultation ->
                viewModel.showA4DocPreview(consultation)
              },
              onToggleDocFee = { inv, includeFee, feeAmount ->
                viewModel.updateInvoiceDocumentationFee(inv.id, includeFee, feeAmount)
              },
              onBackToQueue = {
                mobileActiveTab = 0
              },
              onDeleteInvoiceAndReturnStock = { inv, returnedNames, voidAll ->
                val (success, restoredList) = viewModel.returnSelectedInvoiceItemsAndAdjust(inv.id, returnedNames, voidAll)
                if (success && voidAll) {
                  selectedInvoiceId = null
                  mobileActiveTab = 0
                }
              },
              onToggleInvoicePaid = { invId, isPaid ->
                viewModel.toggleInvoicePaidStatus(invId, isPaid)
              }
            )
          }
        }
      }
    }
  }
}

/**
 * Compact Billing Queue Pane (Left on Tablet, Tab 0 on Mobile)
 */
@Composable
private fun BillingQueuePane(
  invoices: List<Invoice>,
  filteredInvoices: List<Invoice>,
  activeInvoice: Invoice?,
  patients: List<com.example.model.Patient>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedTabFilter: Int,
  onTabFilterChange: (Int) -> Unit,
  selectedDateFilter: String = "ALL",
  onDateFilterChange: (String) -> Unit = {},
  customDateFilter: String? = null,
  onOpenDatePicker: () -> Unit = {},
  onSelectInvoice: (String) -> Unit,
  currencyFmt: NumberFormat
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp)
  ) {
    // Compact Search Box with Grey Background
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Search invoice / patient...", fontSize = 11.5.sp, color = SlateMuted) },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(17.dp), tint = MedicalGreen)
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp), tint = SlateMuted)
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(6.dp),
      colors = clinicSearchBarColors(),
      textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Status Filter Chips Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFFF1F5F9))
        .padding(2.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      listOf(
        "Pending (${invoices.count { !it.isPaid }})",
        "Settled (${invoices.count { it.isPaid }})",
        "All (${invoices.size})"
      ).forEachIndexed { index, label ->
        val isSelected = selectedTabFilter == index
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) PureWhite else Color.Transparent)
            .clickable { onTabFilterChange(index) }
            .padding(vertical = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MedicalGreen else SlateMedium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Date Filter Chips Row for easy date-based lookup of old records
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      listOf(
        "ALL" to "အားလုံး (All Dates)",
        "TODAY" to "ယနေ့ (Today)",
        "YESTERDAY" to "မနေ့က",
        "PAST_7_DAYS" to "လွန်ခဲ့သော ၇ ရက်",
        "THIS_MONTH" to "ယခုလ (This Month)",
        "CUSTOM" to if (selectedDateFilter == "CUSTOM" && customDateFilter != null) customDateFilter else "ရက်စွဲရွေးရန်..."
      ).forEach { (filterKey, filterLabel) ->
        val isSelected = selectedDateFilter == filterKey
        FilterChip(
          selected = isSelected,
          onClick = {
            if (filterKey == "CUSTOM") {
              onOpenDatePicker()
            } else {
              onDateFilterChange(filterKey)
            }
          },
          label = { Text(filterLabel, fontSize = 9.sp) },
          leadingIcon = if (filterKey == "CUSTOM") {
            { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp)) }
          } else null,
          modifier = Modifier.height(26.dp),
          shape = RoundedCornerShape(4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Queue Items List
    if (filteredInvoices.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(32.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            if (selectedTabFilter == 0) "No pending bills" else "No matching invoices",
            color = SlateMedium,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    } else {
      val activeInvId = activeInvoice?.id
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(filteredInvoices, key = { it.id }) { inv ->
          val isSelected = activeInvId != null && inv.id == activeInvId
          val patient = patients.find { it.id == inv.patientId }

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .clickable { onSelectInvoice(inv.id) },
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) SoftGreenContainer else PureWhite,
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) MedicalGreen else BorderGray
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    inv.patientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = DarkGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  patient?.let { p ->
                    Surface(
                      color = SoftGreenContainer,
                      shape = RoundedCornerShape(3.dp),
                      border = BorderStroke(0.5.dp, MedicalGreen.copy(alpha = 0.5f))
                    ) {
                      Text(
                        "#${p.queueNumber}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedicalGreenDark,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  "${inv.invoiceNo} • ${inv.items.size} items",
                  fontSize = 9.sp,
                  color = SlateMedium
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  "${currencyFmt.format(inv.grandTotal.toInt())} Ks",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (inv.isPaid) SlateDark else MedicalGreen
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                  shape = RoundedCornerShape(3.dp),
                  color = if (inv.isPaid) StatusSuccessContainer else StatusWarningContainer
                ) {
                  Text(
                    if (inv.isPaid) "PAID" else "UNPAID",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (inv.isPaid) StatusSuccess else StatusWarning,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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

/**
 * Manual Percentage Input Field with visible text and clear % suffix
 */
@Composable
private fun DiscountPercentInputField(
  value: String,
  onValueChange: (String) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier
) {
  BasicTextField(
    value = value,
    onValueChange = { newVal ->
      val digits = newVal.filter { it.isDigit() }
      if (digits.isEmpty() || (digits.toDoubleOrNull() ?: 0.0) <= 100.0) {
        onValueChange(digits)
      }
    },
    enabled = enabled,
    singleLine = true,
    textStyle = TextStyle(
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = DarkGrayText,
      textAlign = TextAlign.End
    ),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = modifier,
    decorationBox = { innerTextField ->
      Row(
        modifier = Modifier
          .border(
            1.dp,
            if (value.isNotEmpty() && value != "0") MedicalGreen else BorderGray,
            RoundedCornerShape(4.dp)
          )
          .background(if (enabled) PureWhite else Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
          .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
          if (value.isEmpty()) {
            Text("0", fontSize = 11.sp, color = SlateMuted, textAlign = TextAlign.End)
          }
          innerTextField()
        }
        Spacer(modifier = Modifier.width(3.dp))
        Text("%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
      }
    }
  )
}

/**
 * Compact Billing Checkout Inspector (Right on Tablet, Tab 1 on Mobile)
 */
@Composable
private fun BillingCheckoutPane(
  activeInvoice: Invoice?,
  activePatient: com.example.model.Patient?,
  activeConsultation: com.example.model.ConsultationRecord?,
  selectedPaymentMethod: PaymentMethod,
  onPaymentMethodChange: (PaymentMethod) -> Unit,
  discountPercentText: String,
  onDiscountChange: (String) -> Unit,
  currencyFmt: NumberFormat,
  onConfirmPayment: (Invoice, PaymentMethod, Double) -> Unit,
  onPrintThermal: (Invoice) -> Unit,
  onPrintA4: (com.example.model.ConsultationRecord) -> Unit,
  onToggleDocFee: (Invoice, Boolean, Double) -> Unit = { _, _, _ -> },
  onBackToQueue: (() -> Unit)?,
  onDeleteInvoiceAndReturnStock: ((Invoice, Set<String>, Boolean) -> Unit)? = null,
  onApplyDoctorFeeFoc: (Invoice) -> Unit = {},
  onApplyAllFoc: (Invoice) -> Unit = {},
  onResetDiscounts: (Invoice) -> Unit = {},
  onConfirmPaymentWithGranular: ((Invoice, PaymentMethod, Double, Double, Double, Double) -> Unit)? = null,
  onToggleInvoicePaid: ((String, Boolean) -> Unit)? = null
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  if (activeInvoice == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SlateLight, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Please select a patient from the invoice list", color = SlateMedium, fontSize = 12.sp)
        if (onBackToQueue != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = onBackToQueue,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            modifier = Modifier.height(36.dp)
          ) {
            Text("Back to Queue", fontSize = 11.sp)
          }
        }
      }
    }
    return
  }

  // Check if active invoice already has documentation fee line item
  val hasDocFeeItem = activeInvoice.items.any {
    it.description.contains("Documentation Fee", ignoreCase = true) || it.description.contains("Document Fee", ignoreCase = true)
  }
  val existingDocFeeAmount = activeInvoice.items.find {
    it.description.contains("Documentation Fee", ignoreCase = true) || it.description.contains("Document Fee", ignoreCase = true)
  }?.unitPrice ?: 1000.0

  var wantsDocument by remember(activeInvoice.id, hasDocFeeItem) { mutableStateOf(hasDocFeeItem) }
  var docFeeAmountText by remember(activeInvoice.id) { mutableStateOf(existingDocFeeAmount.toInt().toString()) }

  val pharmacySubtotal = remember(activeInvoice.items) {
    activeInvoice.items.filter { !it.isProcedure }.sumOf { it.totalPrice }
  }
  val procedureSubtotal = remember(activeInvoice.items) {
    activeInvoice.items.filter { it.isProcedure }.sumOf { it.totalPrice }
  }

  var docDiscountPctText by remember(activeInvoice.id) {
    mutableStateOf(
      if (activeInvoice.consultationFee > 0 && activeInvoice.doctorFeeDiscount > 0) {
        val pct = (activeInvoice.doctorFeeDiscount / activeInvoice.consultationFee) * 100.0
        pct.toInt().toString()
      } else "0"
    )
  }
  var pharmacyDiscountPctText by remember(activeInvoice.id) {
    mutableStateOf(
      if (pharmacySubtotal > 0 && activeInvoice.pharmacyDiscount > 0) {
        val pct = (activeInvoice.pharmacyDiscount / pharmacySubtotal) * 100.0
        pct.toInt().toString()
      } else "0"
    )
  }
  var procedureDiscountPctText by remember(activeInvoice.id) {
    mutableStateOf(
      if (procedureSubtotal > 0 && activeInvoice.procedureDiscount > 0) {
        val pct = (activeInvoice.procedureDiscount / procedureSubtotal) * 100.0
        pct.toInt().toString()
      } else "0"
    )
  }

  val docDiscountPct = (docDiscountPctText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
  val pharmacyDiscountPct = (pharmacyDiscountPctText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
  val procedureDiscountPct = (procedureDiscountPctText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
  val overallDiscountPct = (discountPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

  val docDiscountAmount = activeInvoice.consultationFee * (docDiscountPct / 100.0)
  val pharmacyDiscountAmount = pharmacySubtotal * (pharmacyDiscountPct / 100.0)
  val procedureDiscountAmount = procedureSubtotal * (procedureDiscountPct / 100.0)
  val totalGranularDiscount = docDiscountAmount + pharmacyDiscountAmount + procedureDiscountAmount

  val remainingForOverall = (activeInvoice.subTotal - totalGranularDiscount).coerceAtLeast(0.0)
  val overallDiscountAmount = remainingForOverall * (overallDiscountPct / 100.0)
  val calculatedDiscountAmount = totalGranularDiscount + overallDiscountAmount
  val finalGrandTotal = (activeInvoice.subTotal - calculatedDiscountAmount).coerceAtLeast(0.0)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // 1. Header with Back Button (Mobile) and Patient Banner
    BillingPatientHeaderCard(
      activeInvoice = activeInvoice,
      activePatient = activePatient,
      onBackToQueue = onBackToQueue,
      onToggleInvoicePaid = onToggleInvoicePaid
    )

    Spacer(modifier = Modifier.height(5.dp))

    // Documentation Fees & Print Option Banner
    BillingDocumentationBanner(
      activeInvoice = activeInvoice,
      wantsDocument = wantsDocument,
      docFeeAmountText = docFeeAmountText,
      onDocFeeAmountChange = { docFeeAmountText = it },
      onWantsDocumentChange = { wantsDocument = it },
      onToggleDocFee = onToggleDocFee
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Itemized Table Section
    BillingItemizedTable(
      activeInvoice = activeInvoice,
      currencyFmt = currencyFmt
    )

    HorizontalDivider(color = BorderSubtleRedBrown, thickness = 1.dp)
    Spacer(modifier = Modifier.height(6.dp))

    // 3. Costs & Deductions
    BillingCostsAndDiscountsCard(
      activeInvoice = activeInvoice,
      pharmacySubtotal = pharmacySubtotal,
      procedureSubtotal = procedureSubtotal,
      currencyFmt = currencyFmt,
      docDiscountPctText = docDiscountPctText,
      onDocDiscountPctChange = { docDiscountPctText = it },
      pharmacyDiscountPctText = pharmacyDiscountPctText,
      onPharmacyDiscountPctChange = { pharmacyDiscountPctText = it },
      procedureDiscountPctText = procedureDiscountPctText,
      onProcedureDiscountPctChange = { procedureDiscountPctText = it },
      discountPercentText = discountPercentText,
      onDiscountChange = onDiscountChange,
      docDiscountAmount = docDiscountAmount,
      pharmacyDiscountAmount = pharmacyDiscountAmount,
      procedureDiscountAmount = procedureDiscountAmount,
      overallDiscountAmount = overallDiscountAmount,
      remainingForOverall = remainingForOverall,
      calculatedDiscountAmount = calculatedDiscountAmount,
      docDiscountPct = docDiscountPct,
      overallDiscountPct = overallDiscountPct,
      onApplyDoctorFeeFoc = onApplyDoctorFeeFoc,
      onApplyAllFoc = onApplyAllFoc,
      onResetDiscounts = onResetDiscounts
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Final Grand Total Banner
    BillingNetTotalBanner(
      finalGrandTotal = finalGrandTotal,
      currencyFmt = currencyFmt
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 5. Payment Method Selection
    BillingPaymentMethodSelector(
      selectedPaymentMethod = selectedPaymentMethod,
      onPaymentMethodChange = onPaymentMethodChange
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 6. Action Buttons
    BillingActionButtonsRow(
      activeInvoice = activeInvoice,
      activeConsultation = activeConsultation,
      selectedPaymentMethod = selectedPaymentMethod,
      overallDiscountPct = overallDiscountPct,
      docDiscountAmount = docDiscountAmount,
      pharmacyDiscountAmount = pharmacyDiscountAmount,
      procedureDiscountAmount = procedureDiscountAmount,
      onPrintThermal = onPrintThermal,
      onPrintA4 = onPrintA4,
      onConfirmPayment = onConfirmPayment,
      onConfirmPaymentWithGranular = onConfirmPaymentWithGranular,
      onRequestDelete = { showDeleteDialog = true },
      canDelete = onDeleteInvoiceAndReturnStock != null
    )

    // 7. Delete / Selective Return Modal
    if (showDeleteDialog && onDeleteInvoiceAndReturnStock != null) {
      BillingDeleteConfirmDialog(
        activeInvoice = activeInvoice,
        currencyFmt = currencyFmt,
        onDismiss = { showDeleteDialog = false },
        onConfirmReturn = { invoice, returnedNames, voidAll ->
          onDeleteInvoiceAndReturnStock(invoice, returnedNames, voidAll)
          showDeleteDialog = false
        }
      )
    }
  }
}

@Composable
private fun BillingPatientHeaderCard(
  activeInvoice: Invoice,
  activePatient: com.example.model.Patient?,
  onBackToQueue: (() -> Unit)?,
  onToggleInvoicePaid: ((String, Boolean) -> Unit)? = null
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = SoftGreenContainer,
    border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.25f))
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onBackToQueue != null) {
            IconButton(
              onClick = onBackToQueue,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MedicalGreenDark,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(
            activeInvoice.patientName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DarkGrayText
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(3.dp),
            color = MedicalGreen
          ) {
            Text(
              activePatient?.hn ?: "HN-2026-0081",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
        }

        // Status Badge with Manual Checkbox Toggle
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = if (activeInvoice.isPaid) StatusSuccessContainer else StatusWarningContainer,
          border = BorderStroke(1.dp, if (activeInvoice.isPaid) StatusSuccess.copy(alpha = 0.5f) else StatusWarning.copy(alpha = 0.5f)),
          modifier = Modifier.clickable {
            onToggleInvoicePaid?.invoke(activeInvoice.id, !activeInvoice.isPaid)
          }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Checkbox(
              checked = activeInvoice.isPaid,
              onCheckedChange = { isChecked ->
                onToggleInvoicePaid?.invoke(activeInvoice.id, isChecked)
              },
              modifier = Modifier.size(16.dp),
              colors = CheckboxDefaults.colors(
                checkedColor = MedicalGreen,
                uncheckedColor = StatusWarning
              )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              if (activeInvoice.isPaid) "PAID (ငွေရှင်းပြီး)" else "UNPAID (ငွေမရှင်းရသေး)",
              fontWeight = FontWeight.Bold,
              fontSize = 9.5.sp,
              color = if (activeInvoice.isPaid) StatusSuccess else StatusWarning
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Doctor & Clinic Credentials
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          "ဆရာဝန်: Dr. Nyan Kaung Sett M.B.,B.S(MDY) (ဆမ-၄၇၄၁၄)",
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          color = MedicalGreenDark
        )
        Text(
          "Bill: ${activeInvoice.invoiceNo}",
          fontSize = 9.sp,
          color = SlateMedium
        )
      }
    }
  }
}

@Composable
private fun BillingDocumentationBanner(
  activeInvoice: Invoice,
  wantsDocument: Boolean,
  docFeeAmountText: String,
  onDocFeeAmountChange: (String) -> Unit,
  onWantsDocumentChange: (Boolean) -> Unit,
  onToggleDocFee: (Invoice, Boolean, Double) -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = if (wantsDocument) SoftGreenContainer.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
    border = BorderStroke(1.dp, if (wantsDocument) MedicalGreen else BorderGray)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .weight(1f)
          .clickable(enabled = !activeInvoice.isPaid) {
            val next = !wantsDocument
            onWantsDocumentChange(next)
            val amount = docFeeAmountText.toDoubleOrNull() ?: 1000.0
            onToggleDocFee(activeInvoice, next, amount)
          }
      ) {
        Checkbox(
          checked = wantsDocument,
          onCheckedChange = { checked ->
            if (!activeInvoice.isPaid) {
              onWantsDocumentChange(checked)
              val amount = docFeeAmountText.toDoubleOrNull() ?: 1000.0
              onToggleDocFee(activeInvoice, checked, amount)
            }
          },
          enabled = !activeInvoice.isPaid,
          colors = CheckboxDefaults.colors(checkedColor = MedicalGreen),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            "Document (ဆေးလက်မှတ်/ဆေးမှတ်တမ်း) ထုတ်ပေးမည်",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGrayText
          )
          Text(
            if (wantsDocument) "Document လိုချင်သူများအတွက် သီးသန့် Documentation fees ထည့်သွင်းထားပါသည်" else "Document မထုတ်ယူပါ (Documentation fees ကင်းလွတ်)",
            fontSize = 8.5.sp,
            color = if (wantsDocument) MedicalGreenDark else SlateMedium
          )
        }
      }

      if (wantsDocument) {
        Spacer(modifier = Modifier.width(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Fees:", fontSize = 9.5.sp, color = SlateDark, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.width(4.dp))
          OutlinedTextField(
            value = docFeeAmountText,
            onValueChange = {
              onDocFeeAmountChange(it)
              val amt = it.toDoubleOrNull() ?: 0.0
              if (!activeInvoice.isPaid) {
                onToggleDocFee(activeInvoice, true, amt)
              }
            },
            enabled = !activeInvoice.isPaid,
            modifier = Modifier.width(72.dp),
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = clinicTextFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = DarkGrayText)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text("Ks", fontSize = 9.sp, color = SlateMedium)
        }
      }
    }
  }
}

@Composable
private fun BillingItemizedTable(
  activeInvoice: Invoice,
  currencyFmt: NumberFormat
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(4.dp))
      .background(Color(0xFFF1F5F9))
      .padding(horizontal = 6.dp, vertical = 4.dp)
  ) {
    Text("ITEM DESCRIPTION", modifier = Modifier.weight(3.2f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
    Text("QTY", modifier = Modifier.weight(0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, textAlign = TextAlign.Center)
    Text("PRICE", modifier = Modifier.weight(1.3f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, textAlign = TextAlign.End)
    Text("TOTAL (Ks)", modifier = Modifier.weight(1.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, textAlign = TextAlign.End)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp)
  ) {
    // 1. Doctor Consultation Fee Row
    if (activeInvoice.consultationFee > 0.0) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(3.2f)) {
          Text(
            "Doctor Consultation Fee (ဆရာဝန်ပြသခ)",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkGrayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            "Clinical Consultation",
            fontSize = 8.sp,
            color = MedicalGreenDark
          )
        }
        Text(
          "1",
          modifier = Modifier.weight(0.7f),
          fontSize = 10.sp,
          textAlign = TextAlign.Center,
          color = DarkGrayText
        )
        Text(
          currencyFmt.format(activeInvoice.consultationFee.toInt()),
          modifier = Modifier.weight(1.3f),
          fontSize = 10.sp,
          textAlign = TextAlign.End,
          color = SlateMedium
        )
        Text(
          currencyFmt.format(activeInvoice.consultationFee.toInt()),
          modifier = Modifier.weight(1.4f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.End,
          color = DarkGrayText
        )
      }
      HorizontalDivider(color = BorderGray.copy(alpha = 0.4f), thickness = 0.5.dp)
    }

    // 2. Documentation / Certificate Fee Row
    if (activeInvoice.serviceFee > 0.0) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(3.2f)) {
          Text(
            "Medical Certificate / Documentation Fee (ဆေးမှတ်တမ်းခ)",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkGrayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            "Documentation",
            fontSize = 8.sp,
            color = SlateMuted
          )
        }
        Text(
          "1",
          modifier = Modifier.weight(0.7f),
          fontSize = 10.sp,
          textAlign = TextAlign.Center,
          color = DarkGrayText
        )
        Text(
          currencyFmt.format(activeInvoice.serviceFee.toInt()),
          modifier = Modifier.weight(1.3f),
          fontSize = 10.sp,
          textAlign = TextAlign.End,
          color = SlateMedium
        )
        Text(
          currencyFmt.format(activeInvoice.serviceFee.toInt()),
          modifier = Modifier.weight(1.4f),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.End,
          color = DarkGrayText
        )
      }
      HorizontalDivider(color = BorderGray.copy(alpha = 0.4f), thickness = 0.5.dp)
    }

    // 3. Prescribed Medicines & Clinical Procedures
    if (activeInvoice.items.isEmpty() && activeInvoice.consultationFee <= 0.0 && activeInvoice.serviceFee <= 0.0) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No billable items on this invoice", fontSize = 10.sp, color = SlateMuted)
      }
    } else {
      activeInvoice.items.forEach { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(3.2f)) {
            Text(
              item.description,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              color = DarkGrayText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              item.category,
              fontSize = 8.sp,
              color = if (item.isProcedure) Color(0xFF0288D1) else MedicalGreenDark
            )
          }
          Text(
            "${item.quantity}",
            modifier = Modifier.weight(0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = DarkGrayText
          )
          Text(
            currencyFmt.format(item.unitPrice.toInt()),
            modifier = Modifier.weight(1.3f),
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            color = SlateMedium
          )
          Text(
            currencyFmt.format(item.total.toInt()),
            modifier = Modifier.weight(1.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = DarkGrayText
          )
        }
        HorizontalDivider(color = BorderGray.copy(alpha = 0.4f), thickness = 0.5.dp)
      }
    }
  }
}

@Composable
private fun BillingCostsAndDiscountsCard(
  activeInvoice: Invoice,
  pharmacySubtotal: Double,
  procedureSubtotal: Double,
  currencyFmt: NumberFormat,
  docDiscountPctText: String,
  onDocDiscountPctChange: (String) -> Unit,
  pharmacyDiscountPctText: String,
  onPharmacyDiscountPctChange: (String) -> Unit,
  procedureDiscountPctText: String,
  onProcedureDiscountPctChange: (String) -> Unit,
  discountPercentText: String,
  onDiscountChange: (String) -> Unit,
  docDiscountAmount: Double,
  pharmacyDiscountAmount: Double,
  procedureDiscountAmount: Double,
  overallDiscountAmount: Double,
  remainingForOverall: Double,
  calculatedDiscountAmount: Double,
  docDiscountPct: Double,
  overallDiscountPct: Double,
  onApplyDoctorFeeFoc: (Invoice) -> Unit,
  onApplyAllFoc: (Invoice) -> Unit,
  onResetDiscounts: (Invoice) -> Unit
) {
  // 1. Actual Costs Breakdown
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = Color(0xFFF8FAFC),
    border = BorderStroke(1.dp, BorderGray)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        "မူလ ကုန်ကျစရိတ်များ (Actual Costs):",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )
      Spacer(modifier = Modifier.height(6.dp))

      if (activeInvoice.consultationFee > 0.0) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("• ဆရာဝန်ပြသခ (Doctor Consultation Fee):", fontSize = 10.sp, color = DarkGrayText)
          Text("${currencyFmt.format(activeInvoice.consultationFee.toInt())} Ks", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
        }
      }

      if (pharmacySubtotal > 0.0) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("• ဆေးဝါးဖိုး စုစုပေါင်း (Prescribed Medicines):", fontSize = 10.sp, color = DarkGrayText)
          Text("${currencyFmt.format(pharmacySubtotal.toInt())} Ks", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
        }
      }

      if (procedureSubtotal > 0.0) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("• ကုသမှု/ခွဲစိတ်စရိတ် (Clinical Procedures):", fontSize = 10.sp, color = DarkGrayText)
          Text("${currencyFmt.format(procedureSubtotal.toInt())} Ks", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
        }
      }

      if (activeInvoice.serviceFee > 0.0) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("• ဆေးမှတ်တမ်းကြေး (Documentation Fee):", fontSize = 10.sp, color = DarkGrayText)
          Text("${currencyFmt.format(activeInvoice.serviceFee.toInt())} Ks", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderGray.copy(alpha = 0.6f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("ကုန်ကျစရိတ် စုစုပေါင်း (Subtotal):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Text("${currencyFmt.format(activeInvoice.subTotal.toInt())} Ks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
    }
  }

  Spacer(modifier = Modifier.height(8.dp))

  // 2. Discounts & Deductions
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = Color(0xFFF8FAFC),
    border = BorderStroke(1.dp, BorderGray)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        "လျှော့စျေးနှင့် ကင်းလွတ်ခွင့်များ (Discounts & Deductions):",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = SlateDark
      )
      Spacer(modifier = Modifier.height(6.dp))

      // Doctor Fee Discount Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("ဆရာဝန်ပြသခ လျှော့စျေး (Dr Fee Disc):", fontSize = 10.sp, color = DarkGrayText)
          Text("Max: ${currencyFmt.format(activeInvoice.consultationFee.toInt())} Ks", fontSize = 8.5.sp, color = SlateMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          DiscountPercentInputField(
            value = docDiscountPctText,
            onValueChange = onDocDiscountPctChange,
            enabled = !activeInvoice.isPaid,
            modifier = Modifier.width(72.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (docDiscountAmount > 0) "-${currencyFmt.format(docDiscountAmount.toInt())} Ks" else "0 Ks",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (docDiscountAmount > 0) MedicalGreenDark else SlateMedium,
            modifier = Modifier.width(78.dp),
            textAlign = TextAlign.End
          )
        }
      }

      // Pharmacy Discount Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("ဆေးဖိုး လျှော့စျေး (Pharmacy Disc):", fontSize = 10.sp, color = DarkGrayText)
          Text("Max: ${currencyFmt.format(pharmacySubtotal.toInt())} Ks", fontSize = 8.5.sp, color = SlateMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          DiscountPercentInputField(
            value = pharmacyDiscountPctText,
            onValueChange = onPharmacyDiscountPctChange,
            enabled = !activeInvoice.isPaid,
            modifier = Modifier.width(72.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (pharmacyDiscountAmount > 0) "-${currencyFmt.format(pharmacyDiscountAmount.toInt())} Ks" else "0 Ks",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (pharmacyDiscountAmount > 0) MedicalGreenDark else SlateMedium,
            modifier = Modifier.width(78.dp),
            textAlign = TextAlign.End
          )
        }
      }

      // Procedure Discount Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("ကုသမှုစရိတ် လျှော့စျေး (Procedure Disc):", fontSize = 10.sp, color = DarkGrayText)
          Text("Max: ${currencyFmt.format(procedureSubtotal.toInt())} Ks", fontSize = 8.5.sp, color = SlateMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          DiscountPercentInputField(
            value = procedureDiscountPctText,
            onValueChange = onProcedureDiscountPctChange,
            enabled = !activeInvoice.isPaid,
            modifier = Modifier.width(72.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (procedureDiscountAmount > 0) "-${currencyFmt.format(procedureDiscountAmount.toInt())} Ks" else "0 Ks",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (procedureDiscountAmount > 0) MedicalGreenDark else SlateMedium,
            modifier = Modifier.width(78.dp),
            textAlign = TextAlign.End
          )
        }
      }

      // Overall Discount Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("ဘေလ်တစ်ခုလုံး အထူးလျှော့စျေး (Overall Disc):", fontSize = 10.sp, color = DarkGrayText)
          Text("On Remaining: ${currencyFmt.format(remainingForOverall.toInt())} Ks", fontSize = 8.5.sp, color = SlateMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          DiscountPercentInputField(
            value = discountPercentText,
            onValueChange = onDiscountChange,
            enabled = !activeInvoice.isPaid,
            modifier = Modifier.width(72.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (overallDiscountAmount > 0) "-${currencyFmt.format(overallDiscountAmount.toInt())} Ks" else "0 Ks",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (overallDiscountAmount > 0) MedicalGreenDark else SlateMedium,
            modifier = Modifier.width(78.dp),
            textAlign = TextAlign.End
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Quick FOC / Reset Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            onDocDiscountPctChange("100")
            onApplyDoctorFeeFoc(activeInvoice)
          },
          enabled = !activeInvoice.isPaid,
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          border = BorderStroke(1.dp, if (docDiscountPct >= 100.0) MedicalGreen else BorderGray),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (docDiscountPct >= 100.0) SoftGreenContainer else PureWhite,
            contentColor = if (docDiscountPct >= 100.0) MedicalGreenDark else DarkGrayText
          )
        ) {
          Text("Dr Fee FOC", fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = {
            onDiscountChange("100")
            onApplyAllFoc(activeInvoice)
          },
          enabled = !activeInvoice.isPaid,
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          border = BorderStroke(1.dp, if (overallDiscountPct >= 100.0) AccentRedBrown else BorderGray),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (overallDiscountPct >= 100.0) Color(0xFFFEE2E2) else PureWhite,
            contentColor = if (overallDiscountPct >= 100.0) AccentRedBrown else DarkGrayText
          )
        ) {
          Text("All FOC", fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = {
            onDocDiscountPctChange("0")
            onPharmacyDiscountPctChange("0")
            onProcedureDiscountPctChange("0")
            onDiscountChange("0")
            onResetDiscounts(activeInvoice)
          },
          enabled = !activeInvoice.isPaid,
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          border = BorderStroke(1.dp, BorderGray)
        ) {
          Text("Reset", fontSize = 9.sp)
        }
      }

      if (calculatedDiscountAmount > 0) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = BorderGray.copy(alpha = 0.6f))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("စုစုပေါင်း လျှော့စျေး (Total Deductions):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
          Text("-${currencyFmt.format(calculatedDiscountAmount.toInt())} Ks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
        }
      }
    }
  }
}

@Composable
private fun BillingNetTotalBanner(
  finalGrandTotal: Double,
  currencyFmt: NumberFormat
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = SoftGreenContainer,
    border = BorderStroke(1.2.dp, MedicalGreen)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("ကျသင့်ငွေ စုစုပေါင်း", fontSize = 10.sp, color = MedicalGreenDark)
        Text("NET TOTAL PAYABLE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
      }
      Text(
        "${currencyFmt.format(finalGrandTotal.toInt())} Ks",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        color = MedicalGreenDark
      )
    }
  }
}

@Composable
private fun BillingPaymentMethodSelector(
  selectedPaymentMethod: PaymentMethod,
  onPaymentMethodChange: (PaymentMethod) -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGray)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        "ငွေပေးချေမှုပုံစံ ရွေးချယ်ပါ (Payment Method):",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGrayText
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
      ) {
        PaymentMethod.entries.forEach { method ->
          val isSelected = selectedPaymentMethod == method
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .clickable { onPaymentMethodChange(method) },
            shape = RoundedCornerShape(4.dp),
            color = if (isSelected) SoftGreenContainer else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isSelected) MedicalGreen else BorderGray)
          ) {
            Text(
              method.label,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) MedicalGreenDark else DarkGrayText,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BillingActionButtonsRow(
  activeInvoice: Invoice,
  activeConsultation: com.example.model.ConsultationRecord?,
  selectedPaymentMethod: PaymentMethod,
  overallDiscountPct: Double,
  docDiscountAmount: Double,
  pharmacyDiscountAmount: Double,
  procedureDiscountAmount: Double,
  onPrintThermal: (Invoice) -> Unit,
  onPrintA4: (com.example.model.ConsultationRecord) -> Unit,
  onConfirmPayment: (Invoice, PaymentMethod, Double) -> Unit,
  onConfirmPaymentWithGranular: ((Invoice, PaymentMethod, Double, Double, Double, Double) -> Unit)?,
  onRequestDelete: () -> Unit,
  canDelete: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // A4 Medical Record & Rx
    OutlinedButton(
      onClick = {
        val consultationToPreview = activeConsultation ?: com.example.model.ConsultationRecord(
          patientId = activeInvoice.patientId,
          patientName = activeInvoice.patientName,
          clinicName = "Shwe Khit Clinic",
          doctorName = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
          doctorSama = "47414",
          subjective = "General medical checkup and follow-up consultation",
          assessment = "Routine follow-up / general consultation",
          plan = "Follow prescribed medications and dietary advice.",
          diagnosisList = listOf("General Clinical Follow-up"),
          prescriptions = activeInvoice.items.filter { it.category == "Pharmacy" }.map { item ->
            com.example.model.PrescribedDrug(
              medicineId = "med-01",
              name = item.description,
              dosage = "Standard",
              frequency = "1 Tab BD",
              duration = "5 Days",
              quantity = item.quantity,
              unitPrice = item.unitPrice,
              instructions = "After meals"
            )
          },
          selectedProcedures = activeInvoice.items.filter { it.category == "Procedure" }.map { item ->
            com.example.model.ProcedureItem(
              code = "PROC-01",
              name = item.description,
              myanmarName = item.description,
              category = "Treatment",
              standardFee = item.unitPrice
            )
          }
        )
        onPrintA4(consultationToPreview)
      },
      modifier = Modifier
        .weight(1f)
        .height(40.dp),
      shape = RoundedCornerShape(6.dp),
      border = BorderStroke(1.2.dp, MedicalGreen),
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Icon(Icons.Default.Description, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        "Print A4",
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = MedicalGreen,
        maxLines = 1
      )
    }

    // Thermal Receipt Button
    if (!activeInvoice.isPaid) {
      Button(
        onClick = {
          if (onConfirmPaymentWithGranular != null) {
            onConfirmPaymentWithGranular(
              activeInvoice,
              selectedPaymentMethod,
              docDiscountAmount,
              pharmacyDiscountAmount,
              procedureDiscountAmount,
              overallDiscountPct
            )
          } else {
            onConfirmPayment(activeInvoice, selectedPaymentMethod, overallDiscountPct)
          }
        },
        modifier = Modifier
          .weight(1.2f)
          .height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Icon(Icons.Default.Print, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          "Pay & Print Slip",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite,
          maxLines = 1
        )
      }
    } else {
      Button(
        onClick = {
          onPrintThermal(activeInvoice)
        },
        modifier = Modifier
          .weight(1.2f)
          .height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Icon(Icons.Default.Print, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          "Slip ထုတ်မည် (Reprint)",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite,
          maxLines = 1
        )
      }
    }

    // Void & Auto-Return Stock to Pharmacy Button
    if (canDelete) {
      OutlinedButton(
        onClick = onRequestDelete,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.2.dp, Color(0xFFDC2626)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          "Void & Return",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFDC2626),
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun BillingDeleteConfirmDialog(
  activeInvoice: Invoice,
  currencyFmt: NumberFormat,
  onDismiss: () -> Unit,
  onConfirmReturn: (Invoice, Set<String>, Boolean) -> Unit
) {
  val pharmacyItems = remember(activeInvoice.items) {
    activeInvoice.items.filter {
      !it.isProcedure && (it.category.equals("Pharmacy", ignoreCase = true) || it.category.equals("Medicine", ignoreCase = true))
    }
  }

  // Selected items to return (default: empty, user picks which medicines to return)
  val selectedToReturn = remember { mutableStateListOf<String>() }

  val returnedMedCost = pharmacyItems.filter { selectedToReturn.contains(it.name) }.sumOf { it.totalPrice }
  val adjustedGrandTotal = (activeInvoice.totalAmount - returnedMedCost).coerceAtLeast(activeInvoice.consultationFee)

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.Default.AssignmentReturn,
        contentDescription = null,
        tint = Color(0xFFDC2626),
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        "ဆေးဝါး ရွေးချယ်ပြန်အပ်ခြင်းနှင့် ဘေလ်ပြင်ဆင်ခြင်း",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = DarkGrayText
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          "လူနာ #${activeInvoice.invoiceNo} (${activeInvoice.patientName}) အတွက် ငွေမလောက်၍ မယူဖြစ်သော ဆေးဝါးများကို ရွေးချယ်ပေးပါရန်။ (ဆရာဝန် ကြည့်ရှုပြီးဖြစ်၍ ဆရာဝန်ပြသခ မပျက်မပြယ် ကျန်ရှိပါမည်)",
          fontSize = 11.5.sp,
          color = DarkGrayText
        )

        // Doctor Consultation Fee (Locked)
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFF1F5F9),
          border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("ဆရာဝန်ပြသခ (Consultation Fee):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            }
            Text("${currencyFmt.format(activeInvoice.consultationFee.toInt())} Ks (ကျန်ရှိမည်)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
          }
        }

        if (pharmacyItems.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFFFFBEB),
            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "ပြန်အပ်မည့် ဆေးဝါးများအား အမှန်ခြစ်ပါ (${selectedToReturn.size}/${pharmacyItems.size}):",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = Color(0xFF92400E)
                )
                TextButton(
                  onClick = {
                    if (selectedToReturn.size == pharmacyItems.size) {
                      selectedToReturn.clear()
                    } else {
                      selectedToReturn.clear()
                      selectedToReturn.addAll(pharmacyItems.map { it.name })
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    if (selectedToReturn.size == pharmacyItems.size) "အားလုံးဖြုတ်" else "ဆေးအားလုံးရွေး",
                    fontSize = 10.sp,
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              pharmacyItems.forEach { item ->
                val isChecked = selectedToReturn.contains(item.name)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      if (isChecked) selectedToReturn.remove(item.name) else selectedToReturn.add(item.name)
                    }
                    .padding(vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = isChecked,
                    onCheckedChange = { chk ->
                      if (chk) selectedToReturn.add(item.name) else selectedToReturn.remove(item.name)
                    },
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      item.name,
                      fontSize = 11.sp,
                      fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                      color = if (isChecked) Color(0xFF991B1B) else DarkGrayText
                    )
                    Text(
                      "${item.quantity} ခု x ${currencyFmt.format(item.unitPrice.toInt())} Ks = ${currencyFmt.format(item.totalPrice.toInt())} Ks",
                      fontSize = 10.sp,
                      color = if (isChecked) Color(0xFFDC2626) else SlateMuted
                    )
                  }
                }
              }
            }
          }
        }

        // Live Total Calculation Card
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFF8FAFC),
          border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("မူလ စုစုပေါင်း ကျသင့်ငွေ:", fontSize = 11.sp, color = SlateMedium)
              Text("${currencyFmt.format(activeInvoice.totalAmount.toInt())} Ks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            }
            if (returnedMedCost > 0) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ပြန်အပ်မည့် ဆေးဖိုး (စတော့ထဲ ပြန်ဝင်မည်):", fontSize = 11.sp, color = Color(0xFFDC2626))
                Text("-${currencyFmt.format(returnedMedCost.toInt())} Ks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
              }
              HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 0.8.dp)
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ပြင်ဆင်ပြီး အသစ်ကျသင့်ငွေ (ပေးချေရန်):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                Text("${currencyFmt.format(adjustedGrandTotal.toInt())} Ks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (selectedToReturn.isNotEmpty()) {
          Button(
            onClick = { onConfirmReturn(activeInvoice, selectedToReturn.toSet(), false) },
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ရွေးထားသောဆေး ပြန်အပ်ပြီး ဘေလ်ပြင်မည်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
          }
        }

        OutlinedButton(
          onClick = { onConfirmReturn(activeInvoice, emptySet(), true) },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
          border = BorderStroke(1.dp, Color(0xFFDC2626)),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("ဘေလ်တစ်ခုလုံး ပယ်ဖျက်မည် (Void)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text("မလုပ်တော့ပါ", fontSize = 11.sp)
      }
    }
  )
}
