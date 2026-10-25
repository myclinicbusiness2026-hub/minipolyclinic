package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Medicine
import com.example.model.StockBatch
import com.example.model.Supplier
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class InventoryMasterTab(val label: String, val myanmarLabel: String) {
  VALUATION("Valuation & Ledger", "စတော့အချုပ် & စာရင်း"),
  CATEGORY_ANALYSIS("Category Breakdown", "အမျိုးအစားအလိုက် ခွဲခြမ်းမှု"),
  REORDER_CHECKLIST("Reorder & PO", "ဆေးမှာယူရန် အကြံပြုစာရင်း")
}

enum class MasterStockFilter(val label: String) {
  ALL("All Items (အားလုံး)"),
  LOW_STOCK("Low Stock (<= Threshold)"),
  CRITICAL_EXPIRY("Critical (<3 Months)"),
  EXPIRING_SOON("Expiring Soon (<6 Months)"),
  ZERO_STOCK("Out of Stock (ပစ္စည်းပြတ်)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryMasterScreen(
  viewModel: ClinicViewModel,
  onNavigateToTab: (Int) -> Unit = {},
  onOpenUserAccounts: () -> Unit = {}
) {
  val context = LocalContext.current
  val medicines by viewModel.medicines.collectAsState()
  val suppliers by viewModel.suppliers.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  // RBAC Permission check
  val user = currentUser
  val canAccess = user != null && user.effectivePermissions.canAccessInventoryMaster

  if (!canAccess) {
    // Restricted Access View
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(BackgroundSurface)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(0.92f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BorderGrey),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(RoundedCornerShape(27.dp))
              .background(StatusCriticalContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Lock,
              contentDescription = null,
              tint = StatusCritical,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            "Access Restricted (ဝင်ရောက်ခွင့် ကန့်သတ်ထားပါသည်)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DarkGrayText,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            "ဆေးဝါးစတော့ ဝယ်ရင်းတန်ဖိုး၊ ရောင်းဈေးနှင့် စတော့အချုပ် ဘဏ္ဍာရေးအချက်အလက်များကို ဆေးခန်း စီမံခန့်ခွဲသူ (Admin)၊ ဆရာဝန် (Doctor) နှင့် ဆေးဝါးကျွမ်းကျင် (Pharmacist) များသာ ကြည့်ရှုခွင့်ရှိပါသည်။",
            fontSize = 11.5.sp,
            color = SlateMedium,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onOpenUserAccounts,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
          ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Switch User / အကောင့်ပြောင်းရန်", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
    return
  }

  var currentTab by remember { mutableStateOf(InventoryMasterTab.VALUATION) }
  var searchQuery by remember { mutableStateOf("") }
  var activeFilter by remember { mutableStateOf(MasterStockFilter.ALL) }
  var sortByValueDesc by remember { mutableStateOf(false) }
  var showPoSummaryDialog by remember { mutableStateOf(false) }
  var isKpiExpanded by remember { mutableStateOf(false) }

  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
  val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val now = remember { Date() }

  fun getDaysUntilExpiry(expiryDateStr: String): Long {
    return try {
      val expDate = sdf.parse(expiryDateStr) ?: return 999
      val diff = expDate.time - now.time
      diff / (1000 * 60 * 60 * 24)
    } catch (e: Exception) {
      999
    }
  }

  // Aggregate Financial & Stock Metrics
  val totalStockCost = remember(medicines) {
    medicines.sumOf { med ->
      med.batches.sumOf { b -> b.quantity * b.costPricePerUnit }
    }
  }

  val totalRetailValue = remember(medicines) {
    medicines.sumOf { med ->
      med.totalStock * med.currentSellingPrice
    }
  }

  val totalProjectedMargin = totalRetailValue - totalStockCost
  val overallMarginPercent = if (totalRetailValue > 0) {
    (totalProjectedMargin / totalRetailValue) * 100.0
  } else 0.0

  val totalSkus = medicines.size
  val totalUnitsCount = remember(medicines) { medicines.sumOf { it.totalStock } }

  // Stock Risk Counters
  val lowStockList = remember(medicines) {
    medicines.filter { it.totalStock <= it.minStockThreshold }
  }

  val zeroStockList = remember(medicines) {
    medicines.filter { it.totalStock == 0 }
  }

  val criticalExpiryList = remember(medicines) {
    medicines.filter { med ->
      val earliestDays = med.sortedBatches.firstOrNull()?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999
      earliestDays in 0..90
    }
  }

  val expiringSoonList = remember(medicines) {
    medicines.filter { med ->
      val earliestDays = med.sortedBatches.firstOrNull()?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999
      earliestDays in 91..180
    }
  }

  // Filtered and Sorted Medicines for Tab 0
  val filteredMedicines = remember(medicines, searchQuery, activeFilter, sortByValueDesc) {
    val list = medicines.filter { med ->
      val matchesSearch = searchQuery.isBlank() ||
          med.name.contains(searchQuery, ignoreCase = true) ||
          med.genericName.contains(searchQuery, ignoreCase = true) ||
          med.category.contains(searchQuery, ignoreCase = true) ||
          med.primarySupplier.contains(searchQuery, ignoreCase = true) ||
          med.batches.any { it.batchNumber.contains(searchQuery, ignoreCase = true) }

      val earliestDays = med.sortedBatches.firstOrNull()?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999

      val matchesFilter = when (activeFilter) {
        MasterStockFilter.ALL -> true
        MasterStockFilter.LOW_STOCK -> med.totalStock <= med.minStockThreshold
        MasterStockFilter.CRITICAL_EXPIRY -> earliestDays in 0..90
        MasterStockFilter.EXPIRING_SOON -> earliestDays in 91..180
        MasterStockFilter.ZERO_STOCK -> med.totalStock == 0
      }
      matchesSearch && matchesFilter
    }

    if (sortByValueDesc) {
      list.sortedByDescending { it.totalStock * it.currentSellingPrice }
    } else {
      list.sortedBy { it.name.lowercase(Locale.ROOT) }
    }
  }

  // Categories aggregation for Tab 1
  val categoriesList = remember(medicines) {
    val grouped = medicines.groupBy { it.category.ifBlank { "Uncategorized" } }
    grouped.map { (catName, meds) ->
      val itemCount = meds.size
      val totalStockUnits = meds.sumOf { it.totalStock }
      val catCost = meds.sumOf { m -> m.batches.sumOf { b -> b.quantity * b.costPricePerUnit } }
      val catRetail = meds.sumOf { m -> m.totalStock * m.currentSellingPrice }
      val catMargin = catRetail - catCost
      val catMarginPercent = if (catRetail > 0) (catMargin / catRetail) * 100.0 else 0.0
      CategoryStat(
        name = catName,
        itemCount = itemCount,
        totalUnits = totalStockUnits,
        totalCost = catCost,
        totalRetail = catRetail,
        marginAmount = catMargin,
        marginPercent = catMarginPercent
      )
    }.sortedByDescending { it.totalRetail }
  }

  // Reorder list for Tab 2
  val reorderItems = remember(medicines) {
    medicines.filter { it.totalStock <= it.minStockThreshold }.map { med ->
      val suggestedOrderQty = maxOf(med.minStockThreshold * 2 - med.totalStock, med.minStockThreshold)
      val estimatedCost = suggestedOrderQty * med.latestCostPrice
      ReorderRecommendation(
        medicine = med,
        currentStock = med.totalStock,
        minThreshold = med.minStockThreshold,
        suggestedQty = suggestedOrderQty,
        unitCost = med.latestCostPrice,
        totalCost = estimatedCost,
        supplierName = med.primarySupplier.ifBlank { "AA Medical Products" }
      )
    }.sortedBy { it.supplierName }
  }

  val totalReorderEstimatedCost = remember(reorderItems) { reorderItems.sumOf { it.totalCost } }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundSurface)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    // Top Bar: Header & Tab Switcher
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            tint = MedicalGreen,
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              "Inventory Master (စတော့အချုပ်)",
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              color = DarkGrayText
            )
            Text(
              "Financial Valuation, Health Alerts & Reorder",
              fontSize = 8.5.sp,
              color = SlateMuted
            )
          }
        }

        // Sub-Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          InventoryMasterTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { currentTab = tab },
              shape = RoundedCornerShape(4.dp),
              color = if (isSelected) MedicalGreen else BackgroundSurface,
              border = BorderStroke(0.8.dp, if (isSelected) MedicalGreenDark else BorderGrey)
            ) {
              Text(
                tab.label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Financial KPI Summary: Ultra-Compact Collapsible Card to Save Vertical Space
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
        // Compact Summary Bar with Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isKpiExpanded = !isKpiExpanded },
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "စတော့တန်ဖိုး: ${currencyFmt.format(totalStockCost.toInt())} Ks (ဝယ်) / ${currencyFmt.format(totalRetailValue.toInt())} Ks (ရောင်း)",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = DarkGrayText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(3.dp),
              color = SoftGreenContainer
            ) {
              Text(
                "+${currencyFmt.format(totalProjectedMargin.toInt())} Ks (${String.format(Locale.US, "%.1f", overallMarginPercent)}%)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreenDark,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              if (isKpiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Toggle KPI details",
              tint = SlateMedium,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        // Expanded 2x2 Detailed Grid
        if (isKpiExpanded) {
          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)
          Spacer(modifier = Modifier.height(6.dp))

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              KpiValuationCard(
                title = "ဝယ်ရင်းတန်ဖိုး စုစုပေါင်း",
                subtitle = "Cost Valuation",
                amountStr = "${currencyFmt.format(totalStockCost.toInt())} Ks",
                icon = Icons.Default.AccountBalance,
                indicatorColor = SlateMedium,
                modifier = Modifier.weight(1f)
              )
              KpiValuationCard(
                title = "ရောင်းဈေးတန်ဖိုး စုစုပေါင်း",
                subtitle = "Retail Valuation",
                amountStr = "${currencyFmt.format(totalRetailValue.toInt())} Ks",
                icon = Icons.Default.PointOfSale,
                indicatorColor = MedicalGreen,
                modifier = Modifier.weight(1f)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              KpiValuationCard(
                title = "ခန့်မှန်းအမြတ်ငွေ (Margin)",
                subtitle = String.format(Locale.US, "Avg Margin: %.1f%%", overallMarginPercent),
                amountStr = "+${currencyFmt.format(totalProjectedMargin.toInt())} Ks",
                icon = Icons.Default.TrendingUp,
                indicatorColor = StatusPurple,
                modifier = Modifier.weight(1f)
              )
              KpiValuationCard(
                title = "ဆေးဝါး အမျိုးအစား & လက်ကျန်",
                subtitle = "${currencyFmt.format(totalUnitsCount)} Units In Stock",
                amountStr = "$totalSkus SKUs",
                icon = Icons.Default.Medication,
                indicatorColor = StatusInfo,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Main Content based on Selected Tab
    when (currentTab) {
      InventoryMasterTab.VALUATION -> {
        ValuationAndLedgerView(
          medicines = filteredMedicines,
          searchQuery = searchQuery,
          onSearchChange = { searchQuery = it },
          activeFilter = activeFilter,
          onFilterChange = { activeFilter = it },
          sortByValueDesc = sortByValueDesc,
          onToggleSort = { sortByValueDesc = !sortByValueDesc },
          currencyFmt = currencyFmt,
          lowStockCount = lowStockList.size,
          criticalExpiryCount = criticalExpiryList.size,
          expiringSoonCount = expiringSoonList.size,
          zeroStockCount = zeroStockList.size,
          onNavigateToStockIn = { onNavigateToTab(3) }
        )
      }
      InventoryMasterTab.CATEGORY_ANALYSIS -> {
        CategoryBreakdownView(
          categories = categoriesList,
          currencyFmt = currencyFmt,
          totalRetailAll = totalRetailValue
        )
      }
      InventoryMasterTab.REORDER_CHECKLIST -> {
        ReorderChecklistView(
          reorderItems = reorderItems,
          totalEstimatedCost = totalReorderEstimatedCost,
          currencyFmt = currencyFmt,
          onCopyChecklist = {
            val sb = StringBuilder()
            sb.append("📋 ရွှေခေတ်ဆေးခန်း - ဆေးဝါးမှာယူရန် အကြံပြုစာရင်း (Reorder Purchase Order)\n")
            sb.append("ရက်စွဲ: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())}\n")
            sb.append("ခန့်မှန်းကုန်ကျငွေ စုစုပေါင်း: ${currencyFmt.format(totalReorderEstimatedCost.toInt())} Ks\n\n")

            val groupedBySupplier = reorderItems.groupBy { it.supplierName }
            groupedBySupplier.forEach { (supplier, items) ->
              sb.append("🏢 Supplier: $supplier\n")
              items.forEachIndexed { idx, itm ->
                sb.append("  ${idx + 1}. ${itm.medicine.name} (${itm.medicine.dosageForm}) - လက်ကျန်: ${itm.currentStock} ${itm.medicine.unitType} | မှာယူရန်: ${itm.suggestedQty} ${itm.medicine.unitType} (~${currencyFmt.format(itm.totalCost.toInt())} Ks)\n")
              }
              sb.append("\n")
            }

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Reorder Checklist", sb.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "ဆေးမှာယူရန် စာရင်းကို Clipboard သို့ ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
          },
          onOpenSummary = { showPoSummaryDialog = true },
          onNavigateToStockIn = { onNavigateToTab(3) }
        )
      }
    }
  }

  // PO Summary Dialog
  if (showPoSummaryDialog) {
    AlertDialog(
      onDismissRequest = { showPoSummaryDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MedicalGreen)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Purchase Order Summary (အမှာစာ အကျဉ်းချုပ်)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            "ပြန်လည်မှာယူရမည့် ဆေးဝါး စုစုပေါင်း: ${reorderItems.size} မျိုး",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkGrayText
          )
          Text(
            "ခန့်မှန်းဝယ်ယူစရိတ် စုစုပေါင်း: ${currencyFmt.format(totalReorderEstimatedCost.toInt())} Ks",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreenDark
          )

          Spacer(modifier = Modifier.height(8.dp))
          Divider(color = BorderGrey)
          Spacer(modifier = Modifier.height(8.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(reorderItems) { item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(BackgroundSurface, RoundedCornerShape(4.dp))
                  .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.medicine.name, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                  Text(
                    "Supplier: ${item.supplierName} • လက်ကျန်: ${item.currentStock}",
                    fontSize = 8.5.sp,
                    color = SlateMuted
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    "မှာရန်: ${item.suggestedQty} ${item.medicine.unitType}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusCritical
                  )
                  Text(
                    "${currencyFmt.format(item.totalCost.toInt())} Ks",
                    fontSize = 8.5.sp,
                    color = SlateDark
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showPoSummaryDialog = false
            val sb = StringBuilder()
            sb.append("📋 ရွှေခေတ်ဆေးခန်း - ဆေးဝါးမှာယူရန် အကြံပြုစာရင်း (Reorder Purchase Order)\n")
            sb.append("ရက်စွဲ: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())}\n")
            sb.append("ခန့်မှန်းကုန်ကျငွေ စုစုပေါင်း: ${currencyFmt.format(totalReorderEstimatedCost.toInt())} Ks\n\n")

            val groupedBySupplier = reorderItems.groupBy { it.supplierName }
            groupedBySupplier.forEach { (supplier, items) ->
              sb.append("🏢 Supplier: $supplier\n")
              items.forEachIndexed { idx, itm ->
                sb.append("  ${idx + 1}. ${itm.medicine.name} (${itm.medicine.dosageForm}) - လက်ကျန်: ${itm.currentStock} ${itm.medicine.unitType} | မှာယူရန်: ${itm.suggestedQty} ${itm.medicine.unitType} (~${currencyFmt.format(itm.totalCost.toInt())} Ks)\n")
              }
              sb.append("\n")
            }

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Reorder Checklist", sb.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "အမှာစာ စာရင်းကို Clipboard သို့ ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
          },
          shape = RoundedCornerShape(4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Copy Checklist", fontSize = 10.sp)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPoSummaryDialog = false }) {
          Text("ပိတ်မည်", fontSize = 10.sp, color = SlateMedium)
        }
      }
    )
  }
}

// -------------------------------------------------------------
// SUB-VIEWS & COMPONENTS
// -------------------------------------------------------------

@Composable
private fun ValuationAndLedgerView(
  medicines: List<Medicine>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  activeFilter: MasterStockFilter,
  onFilterChange: (MasterStockFilter) -> Unit,
  sortByValueDesc: Boolean,
  onToggleSort: () -> Unit,
  currencyFmt: NumberFormat,
  lowStockCount: Int,
  criticalExpiryCount: Int,
  expiringSoonCount: Int,
  zeroStockCount: Int,
  onNavigateToStockIn: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Search, Filter & Sort Controls
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Column(modifier = Modifier.padding(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Search Box
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search trade, generic, supplier, batch...", fontSize = 10.5.sp, color = SlateMuted) },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp))
            },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                }
              }
            },
            modifier = Modifier
              .weight(1f)
              .height(38.dp),
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MedicalGreen,
              unfocusedBorderColor = BorderGrey,
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite
            ),
            singleLine = true
          )

          // Sort Button
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { onToggleSort() },
            shape = RoundedCornerShape(6.dp),
            color = if (sortByValueDesc) SoftGreenContainer else BackgroundSurface,
            border = BorderStroke(1.dp, if (sortByValueDesc) MedicalGreen else BorderGrey)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                if (sortByValueDesc) Icons.Default.ArrowDownward else Icons.Default.SortByAlpha,
                contentDescription = null,
                tint = if (sortByValueDesc) MedicalGreenDark else SlateDark,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                if (sortByValueDesc) "Sort: Value (High)" else "Sort: Name (A-Z)",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (sortByValueDesc) MedicalGreenDark else SlateDark
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Fast Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          MasterStockFilter.values().forEach { filter ->
            val isSelected = activeFilter == filter
            val countStr = when (filter) {
              MasterStockFilter.ALL -> medicines.size
              MasterStockFilter.LOW_STOCK -> lowStockCount
              MasterStockFilter.CRITICAL_EXPIRY -> criticalExpiryCount
              MasterStockFilter.EXPIRING_SOON -> expiringSoonCount
              MasterStockFilter.ZERO_STOCK -> zeroStockCount
            }

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onFilterChange(filter) },
              shape = RoundedCornerShape(4.dp),
              color = if (isSelected) MedicalGreen else BackgroundSurface,
              border = BorderStroke(0.8.dp, if (isSelected) MedicalGreenDark else BorderGrey)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  filter.label,
                  fontSize = 8.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) PureWhite else DarkGrayText
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  shape = RoundedCornerShape(3.dp),
                  color = if (isSelected) PureWhite.copy(alpha = 0.25f) else BorderGrey
                ) {
                  Text(
                    "$countStr",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PureWhite else SlateDark,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // List of Medicines & Valuation Cards
    if (medicines.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(PureWhite, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Inventory2, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text("ရှာဖွေမှုနှင့် ကိုက်ညီသော ဆေးဝါးမရှိပါ", fontSize = 11.5.sp, color = SlateMuted)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(medicines, key = { it.id }) { med ->
          MedicineValuationCard(
            medicine = med,
            currencyFmt = currencyFmt,
            onStockIn = onNavigateToStockIn
          )
        }
      }
    }
  }
}

@Composable
private fun MedicineValuationCard(
  medicine: Medicine,
  currencyFmt: NumberFormat,
  onStockIn: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }

  val totalCostValue = medicine.batches.sumOf { it.quantity * it.costPricePerUnit }
  val totalRetailValue = medicine.totalStock * medicine.currentSellingPrice
  val grossMargin = totalRetailValue - totalCostValue

  val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val now = remember { Date() }

  val earliestExp = medicine.sortedBatches.firstOrNull()?.expiryDate ?: "N/A"
  val daysToExpiry = try {
    if (earliestExp != "N/A") {
      val exp = sdf.parse(earliestExp)
      if (exp != null) (exp.time - now.time) / (1000 * 60 * 60 * 24) else 999
    } else 999
  } catch (_: Exception) { 999 }

  val isLowStock = medicine.totalStock <= medicine.minStockThreshold
  val isCriticalExpiry = daysToExpiry in 0..90
  val isExpiringSoon = daysToExpiry in 91..180

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(
      1.dp,
      when {
        medicine.totalStock == 0 -> StatusCritical.copy(alpha = 0.6f)
        isCriticalExpiry -> StatusCritical.copy(alpha = 0.6f)
        isLowStock -> StatusWarning.copy(alpha = 0.7f)
        else -> BorderGrey
      }
    )
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      // Top Row: Name, Generic, Category, Stock Count
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              medicine.name,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = DarkGrayText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(3.dp),
              color = SoftGreenContainer
            ) {
              Text(
                medicine.category,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MedicalGreenDark,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
              )
            }
          }
          Text(
            "${medicine.genericName} • ${medicine.dosageForm} (${medicine.unitType})",
            fontSize = 9.sp,
            color = SlateMuted
          )
        }

        // Stock Badge
        Column(horizontalAlignment = Alignment.End) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = when {
              medicine.totalStock == 0 -> StatusCriticalContainer
              isLowStock -> StatusWarningContainer
              else -> SoftGreenContainer
            },
            border = BorderStroke(
              0.8.dp,
              when {
                medicine.totalStock == 0 -> StatusCritical
                isLowStock -> StatusWarning
                else -> MedicalGreen
              }
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                when {
                  medicine.totalStock == 0 -> Icons.Default.Cancel
                  isLowStock -> Icons.Default.WarningAmber
                  else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when {
                  medicine.totalStock == 0 -> StatusCritical
                  isLowStock -> StatusWarning
                  else -> MedicalGreen
                },
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                "${medicine.totalStock} ${medicine.unitType}",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                  medicine.totalStock == 0 -> StatusCritical
                  isLowStock -> Color(0xFFB45309)
                  else -> MedicalGreenDark
                }
              )
            }
          }
          Text(
            "Min: ${medicine.minStockThreshold} ${medicine.unitType}",
            fontSize = 7.5.sp,
            color = SlateLight
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Divider(color = BorderGrey, thickness = 0.5.dp)
      Spacer(modifier = Modifier.height(5.dp))

      // Financial Matrix Row (Cost vs Retail vs Margin)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(BackgroundSurface, RoundedCornerShape(4.dp))
          .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Unit Cost & Retail
        Column {
          Text("Unit Pricing", fontSize = 7.5.sp, color = SlateMuted)
          Text(
            "Cost: ${currencyFmt.format(medicine.latestCostPrice.toInt())} Ks",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateDark
          )
          Text(
            "Retail: ${currencyFmt.format(medicine.currentSellingPrice.toInt())} Ks (+${medicine.profitMarginPercent.toInt()}%)",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreenDark
          )
        }

        // Total Valuation
        Column(horizontalAlignment = Alignment.End) {
          Text("Total Valuation (In Stock)", fontSize = 7.5.sp, color = SlateMuted)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "Cost: ${currencyFmt.format(totalCostValue.toInt())} Ks",
              fontSize = 8.5.sp,
              color = SlateMedium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Retail: ${currencyFmt.format(totalRetailValue.toInt())} Ks",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = DarkGrayText
            )
          }
          Text(
            "Gross Margin: +${currencyFmt.format(grossMargin.toInt())} Ks",
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = StatusPurple
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Bottom Row: Expiry Status & Batch Expander
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = when {
              isCriticalExpiry -> StatusCritical
              isExpiringSoon -> StatusWarning
              else -> MedicalGreen
            },
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            "Earliest Expiry: $earliestExp",
            fontSize = 8.sp,
            fontWeight = if (isCriticalExpiry || isExpiringSoon) FontWeight.Bold else FontWeight.Normal,
            color = when {
              isCriticalExpiry -> StatusCritical
              isExpiringSoon -> Color(0xFFB45309)
              else -> SlateMuted
            }
          )
          if (isCriticalExpiry) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(shape = RoundedCornerShape(2.dp), color = StatusCriticalContainer) {
              Text("<3 Mo Critical", fontSize = 7.sp, color = StatusCritical, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "${medicine.batches.size} Batches",
            fontSize = 8.sp,
            color = SlateMuted
          )
          Spacer(modifier = Modifier.width(4.dp))
          TextButton(
            onClick = { isExpanded = !isExpanded },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            modifier = Modifier.height(24.dp)
          ) {
            Text(
              if (isExpanded) "Hide Batches ▲" else "View Batches ▼",
              fontSize = 8.sp,
              color = MedicalGreen,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Expandable Batches Table
      if (isExpanded) {
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = BorderGrey, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(4.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundSurface, RoundedCornerShape(4.dp))
            .padding(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            "Batch Details (FEFO Stock Breakdown):",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGrayText
          )

          medicine.sortedBatches.forEach { batch ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  "Lot: ${batch.batchNumber}",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = DarkGrayText
                )
                Text(
                  "Supplier: ${batch.supplierName ?: medicine.primarySupplier}",
                  fontSize = 7.5.sp,
                  color = SlateMuted
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "Exp: ${batch.expiryDate}",
                  fontSize = 8.sp,
                  color = SlateMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  "${batch.quantity} ${medicine.unitType}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = MedicalGreenDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  "@ ${currencyFmt.format(batch.costPricePerUnit.toInt())} Ks",
                  fontSize = 8.sp,
                  color = SlateDark
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryBreakdownView(
  categories: List<CategoryStat>,
  currencyFmt: NumberFormat,
  totalRetailAll: Double
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 4.dp)
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            "အမျိုးအစားအလိုက် ဆေးဝါးစတော့ ခွဲခြမ်းစိတ်ဖြာမှု (Category Analytics)",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGrayText
          )
          Text(
            "ဆေးဝါးအုပ်စုတစ်ခုချင်းစီ၏ ဝယ်ရင်းတန်ဖိုး၊ ရောင်းဈေးနှင့် စတော့ပါဝင်မှု အချိုးအစား",
            fontSize = 8.5.sp,
            color = SlateMuted
          )
        }
        Surface(shape = RoundedCornerShape(4.dp), color = SoftGreenContainer) {
          Text(
            "${categories.size} Categories",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreenDark,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(categories) { cat ->
        val sharePercent = if (totalRetailAll > 0) (cat.totalRetail / totalRetailAll) * 100.0 else 0.0

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          border = BorderStroke(1.dp, BorderGrey)
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SoftGreenContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Category, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(13.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                  Text("${cat.itemCount} SKUs • ${currencyFmt.format(cat.totalUnits)} Units", fontSize = 8.5.sp, color = SlateMuted)
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  "${currencyFmt.format(cat.totalRetail.toInt())} Ks",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MedicalGreenDark
                )
                Text(
                  String.format(Locale.US, "Share: %.1f%% of Store", sharePercent),
                  fontSize = 8.sp,
                  color = SlateMedium
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar representing share of inventory
            LinearProgressIndicator(
              progress = { (sharePercent / 100f).toFloat().coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = MedicalGreen,
              trackColor = BackgroundSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-metrics row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundSurface, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.5.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                "Cost: ${currencyFmt.format(cat.totalCost.toInt())} Ks",
                fontSize = 8.sp,
                color = SlateDark
              )
              Text(
                "Est. Profit: +${currencyFmt.format(cat.marginAmount.toInt())} Ks",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = StatusPurple
              )
              Text(
                String.format(Locale.US, "Margin: %.1f%%", cat.marginPercent),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreenDark
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReorderChecklistView(
  reorderItems: List<ReorderRecommendation>,
  totalEstimatedCost: Double,
  currencyFmt: NumberFormat,
  onCopyChecklist: () -> Unit,
  onOpenSummary: () -> Unit,
  onNavigateToStockIn: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 4.dp)
  ) {
    // Actions Header
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            "ပြန်လည်မှာယူရန် အကြံပြုစာရင်း (Reorder Checklist)",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGrayText
          )
          Text(
            "လက်ကျန်နည်းနေသော ဆေးဝါးများ: ${reorderItems.size} မျိုး | ခန့်မှန်းကုန်ကျငွေ: ${currencyFmt.format(totalEstimatedCost.toInt())} Ks",
            fontSize = 8.5.sp,
            color = SlateMuted
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Button(
            onClick = onCopyChecklist,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Copy List", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onOpenSummary,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MedicalGreen),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Summarize, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("PO Summary", fontSize = 8.5.sp, color = MedicalGreen, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (reorderItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(PureWhite, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text("လက်ကျန်ပြတ်လပ်/နည်းနေသော ဆေးဝါးများ မရှိပါ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Text("ဆေးဝါးစတော့များအားလုံး လုံလောက်စွာ ရှိနေပါသည်", fontSize = 9.sp, color = SlateMuted)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(reorderItems) { item ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, if (item.currentStock == 0) StatusCriticalContainer else BorderGrey)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    item.medicine.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  if (item.currentStock == 0) {
                    Surface(shape = RoundedCornerShape(2.dp), color = StatusCriticalContainer) {
                      Text("ပြတ်လပ် (0)", fontSize = 7.sp, color = StatusCritical, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
                    }
                  } else {
                    Surface(shape = RoundedCornerShape(2.dp), color = StatusWarningContainer) {
                      Text("နည်း (${item.currentStock})", fontSize = 7.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
                    }
                  }
                }
                Text(
                  "${item.medicine.genericName} • ${item.medicine.dosageForm}",
                  fontSize = 8.5.sp,
                  color = SlateMuted
                )
                Text(
                  "Supplier: ${item.supplierName} • Min: ${item.minThreshold} ${item.medicine.unitType}",
                  fontSize = 8.sp,
                  color = SlateDark
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SoftGreenContainer,
                  border = BorderStroke(0.8.dp, MedicalGreen)
                ) {
                  Text(
                    "မှာရန်: +${item.suggestedQty} ${item.medicine.unitType}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalGreenDark,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  "Est: ${currencyFmt.format(item.totalCost.toInt())} Ks",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = SlateDark
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun KpiValuationCard(
  title: String,
  subtitle: String,
  amountStr: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  indicatorColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = PureWhite,
    border = BorderStroke(1.dp, BorderGrey)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.Center
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          title,
          fontSize = 8.sp,
          color = SlateMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Icon(icon, contentDescription = null, tint = indicatorColor, modifier = Modifier.size(13.dp))
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        amountStr,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGrayText,
        maxLines = 1
      )
      Text(
        subtitle,
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Medium,
        color = indicatorColor,
        maxLines = 1
      )
    }
  }
}

// Data models for category & reorder analytics
data class CategoryStat(
  val name: String,
  val itemCount: Int,
  val totalUnits: Int,
  val totalCost: Double,
  val totalRetail: Double,
  val marginAmount: Double,
  val marginPercent: Double
)

data class ReorderRecommendation(
  val medicine: Medicine,
  val currentStock: Int,
  val minThreshold: Int,
  val suggestedQty: Int,
  val unitCost: Double,
  val totalCost: Double,
  val supplierName: String
)
