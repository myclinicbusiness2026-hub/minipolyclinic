package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ClinicDataPersistence
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class LedgerDateFilterMode(val label: String, val myanmarLabel: String) {
  DAILY("Daily", "ရက်အလိုက်"),
  MONTHLY("Monthly", "လအလိုက်"),
  CUSTOM_RANGE("Custom Date", "ရက်စွဲအပိုင်းအခြား")
}

enum class LedgerTypeFilter(val label: String, val myanmarLabel: String) {
  ALL("All", "အားလုံး"),
  INCOME("Income", "ဝင်ငွေ"),
  EXPENSE("Expense", "ထွက်ငွေ")
}

sealed class LedgerEntry(
  val date: String,
  val timestamp: String,
  val voucherNo: String,
  val title: String,
  val category: String,
  val amount: Double,
  val paymentMethod: PaymentMethod,
  val isIncome: Boolean,
  val referenceId: String
) {
  class IncomeEntry(val invoice: Invoice) : LedgerEntry(
    date = invoice.dateString,
    timestamp = invoice.createdAt,
    voucherNo = invoice.invoiceNo,
    title = "${invoice.patientName} (${invoice.items.size} မျိုး)",
    category = "Patient Bill",
    amount = invoice.grandTotal,
    paymentMethod = invoice.paymentMethod,
    isIncome = true,
    referenceId = invoice.id
  )

  class ExpenseEntry(val expense: ClinicExpense) : LedgerEntry(
    date = expense.date,
    timestamp = expense.timestamp,
    voucherNo = expense.voucherNo,
    title = expense.title + if (expense.payee.isNotBlank()) " [${expense.payee}]" else "",
    category = expense.category,
    amount = expense.amount,
    paymentMethod = expense.paymentMethod,
    isIncome = false,
    referenceId = expense.id
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicLedgerScreen(
  viewModel: ClinicViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val invoices by viewModel.invoices.collectAsState()
  val expenses by viewModel.expenses.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
  val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
  val displayDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  var filterMode by remember { mutableStateOf(LedgerDateFilterMode.DAILY) }
  var typeFilter by remember { mutableStateOf(LedgerTypeFilter.ALL) }

  // Current selected daily date
  var selectedDateStr by remember { mutableStateOf(dateFormat.format(Date())) }
  // Current selected month
  var selectedMonthStr by remember { mutableStateOf(monthFormat.format(Date())) }
  // Custom range
  var customStartDateStr by remember {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -7)
    mutableStateOf(dateFormat.format(cal.time))
  }
  var customEndDateStr by remember { mutableStateOf(dateFormat.format(Date())) }

  var showAddExpenseDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }

  // Combine Paid Invoices + Expenses into unified ledger entries
  val allEntries = remember(invoices, expenses) {
    val list = mutableListOf<LedgerEntry>()
    invoices.filter { it.isPaid }.forEach { inv ->
      list.add(LedgerEntry.IncomeEntry(inv))
    }
    expenses.forEach { exp ->
      list.add(LedgerEntry.ExpenseEntry(exp))
    }
    list.sortedByDescending { it.timestamp }
  }

  // Filter by Date Mode
  val dateFilteredEntries = remember(allEntries, filterMode, selectedDateStr, selectedMonthStr, customStartDateStr, customEndDateStr) {
    when (filterMode) {
      LedgerDateFilterMode.DAILY -> {
        allEntries.filter { it.date == selectedDateStr }
      }
      LedgerDateFilterMode.MONTHLY -> {
        allEntries.filter { it.date.startsWith(selectedMonthStr) }
      }
      LedgerDateFilterMode.CUSTOM_RANGE -> {
        allEntries.filter { it.date in customStartDateStr..customEndDateStr }
      }
    }
  }

  // Filter by Type (All / Income / Expense)
  val finalFilteredEntries = remember(dateFilteredEntries, typeFilter) {
    when (typeFilter) {
      LedgerTypeFilter.ALL -> dateFilteredEntries
      LedgerTypeFilter.INCOME -> dateFilteredEntries.filter { it.isIncome }
      LedgerTypeFilter.EXPENSE -> dateFilteredEntries.filter { !it.isIncome }
    }
  }

  // Financial aggregates for current view
  val totalIncome = remember(dateFilteredEntries) {
    dateFilteredEntries.filter { it.isIncome }.sumOf { it.amount }
  }
  val totalExpense = remember(dateFilteredEntries) {
    dateFilteredEntries.filter { !it.isIncome }.sumOf { it.amount }
  }
  val netBalance = totalIncome - totalExpense

  val cashIncome = dateFilteredEntries.filter { it.isIncome && it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
  val digitalIncome = totalIncome - cashIncome

  // Daily Aggregated breakdown for Monthly view
  val dailyBreakdown = remember(dateFilteredEntries, filterMode) {
    if (filterMode == LedgerDateFilterMode.MONTHLY) {
      dateFilteredEntries.groupBy { it.date }
        .map { (date, entries) ->
          val inc = entries.filter { it.isIncome }.sumOf { it.amount }
          val exp = entries.filter { !it.isIncome }.sumOf { it.amount }
          Triple(date, inc, exp)
        }
        .sortedByDescending { it.first }
    } else {
      emptyList()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundSurface)
  ) {
    // 1. Compact Header Bar: Date Filter Tabs & Quick Action Buttons
    Surface(
      color = PureWhite,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Filter Mode Segmented Buttons (Compact)
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(SoftGreenContainer)
              .padding(2.dp)
          ) {
            LedgerDateFilterMode.values().forEach { mode ->
              val isSelected = filterMode == mode
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isSelected) MedicalGreen else Color.Transparent)
                  .clickable { filterMode = mode }
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  mode.myanmarLabel,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) PureWhite else DarkGrayText
                )
              }
            }
          }

          // Action Buttons: Add Expense & Export CSV
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
              onClick = { showAddExpenseDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.height(34.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("ထွက်ငွေထည့်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                val csv = ClinicDataPersistence.generateExpensesCsv(expenses)
                Toast.makeText(context, "Ledger CSV exported (${expenses.size} vouchers)", Toast.LENGTH_SHORT).show()
                showExportDialog = true
              },
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, MedicalGreen),
              modifier = Modifier.height(34.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("CSV", fontSize = 10.5.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub-controller depending on Filter Mode
        when (filterMode) {
          LedgerDateFilterMode.DAILY -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    try {
                      val cal = Calendar.getInstance()
                      cal.time = dateFormat.parse(selectedDateStr) ?: Date()
                      cal.add(Calendar.DAY_OF_YEAR, -1)
                      selectedDateStr = dateFormat.format(cal.time)
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                  },
                  modifier = Modifier.size(30.dp)
                ) {
                  Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = MedicalGreenDark)
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = SoftGreenContainer,
                  border = BorderStroke(0.8.dp, MedicalGreen.copy(alpha = 0.3f)),
                  modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val isToday = selectedDateStr == dateFormat.format(Date())
                    Text(
                      text = if (isToday) "$selectedDateStr (ယနေ့)" else selectedDateStr,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = MedicalGreenDark
                    )
                  }
                }

                IconButton(
                  onClick = {
                    try {
                      val cal = Calendar.getInstance()
                      cal.time = dateFormat.parse(selectedDateStr) ?: Date()
                      cal.add(Calendar.DAY_OF_YEAR, 1)
                      selectedDateStr = dateFormat.format(cal.time)
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                  },
                  modifier = Modifier.size(30.dp)
                ) {
                  Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = MedicalGreenDark)
                }
              }

              // Jump to Today button
              if (selectedDateStr != dateFormat.format(Date())) {
                TextButton(
                  onClick = { selectedDateStr = dateFormat.format(Date()) },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                  modifier = Modifier.height(28.dp)
                ) {
                  Text("ယနေ့သို့ပြန်သွားရန်", fontSize = 10.5.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          LedgerDateFilterMode.MONTHLY -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    try {
                      val cal = Calendar.getInstance()
                      cal.time = monthFormat.parse(selectedMonthStr) ?: Date()
                      cal.add(Calendar.MONTH, -1)
                      selectedMonthStr = monthFormat.format(cal.time)
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                  },
                  modifier = Modifier.size(30.dp)
                ) {
                  Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = MedicalGreenDark)
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = SoftGreenContainer,
                  border = BorderStroke(0.8.dp, MedicalGreen.copy(alpha = 0.3f)),
                  modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                  ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "$selectedMonthStr (လစဉ်အချုပ်)",
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp,
                      color = MedicalGreenDark
                    )
                  }
                }

                IconButton(
                  onClick = {
                    try {
                      val cal = Calendar.getInstance()
                      cal.time = monthFormat.parse(selectedMonthStr) ?: Date()
                      cal.add(Calendar.MONTH, 1)
                      selectedMonthStr = monthFormat.format(cal.time)
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                  },
                  modifier = Modifier.size(30.dp)
                ) {
                  Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MedicalGreenDark)
                }
              }

              Text(
                "ရက်ပေါင်း ${dailyBreakdown.size} ရက် လှုပ်ရှားမှုရှိ",
                fontSize = 10.sp,
                color = SlateMedium,
                fontWeight = FontWeight.Medium
              )
            }
          }

          LedgerDateFilterMode.CUSTOM_RANGE -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = customStartDateStr,
                onValueChange = { customStartDateStr = it },
                label = { Text("From (YYYY-MM-DD)", fontSize = 9.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
              )
              Text("—", fontSize = 12.sp, color = SlateMedium)
              OutlinedTextField(
                value = customEndDateStr,
                onValueChange = { customEndDateStr = it },
                label = { Text("To (YYYY-MM-DD)", fontSize = 9.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
              )

              // Preset Quick Chips
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SoftGreenContainer,
                  modifier = Modifier.clickable {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    customStartDateStr = dateFormat.format(cal.time)
                    customEndDateStr = dateFormat.format(Date())
                  }
                ) {
                  Text("7D", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SoftGreenContainer,
                  modifier = Modifier.clickable {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -30)
                    customStartDateStr = dateFormat.format(cal.time)
                    customEndDateStr = dateFormat.format(Date())
                  }
                ) {
                  Text("30D", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
              }
            }
          }
        }
      }
    }

    // 2. Compact Financial Overview Cards (Mobile Consistent 8dp Padding)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Income Card
      CompactLedgerSummaryCard(
        title = "ဝင်ငွေ စုစုပေါင်း",
        subtitle = "${dateFilteredEntries.count { it.isIncome }} စောင် (Paid)",
        amount = totalIncome,
        color = MedicalGreen,
        containerColor = SoftGreenContainer,
        icon = Icons.Default.TrendingUp,
        modifier = Modifier.weight(1f)
      )

      // Expense Card
      CompactLedgerSummaryCard(
        title = "ထွက်ငွေ စုစုပေါင်း",
        subtitle = "${dateFilteredEntries.count { !it.isIncome }} ဘောင်ချာ",
        amount = totalExpense,
        color = MahoganyAccent,
        containerColor = Color(0xFFFFEBEE),
        icon = Icons.Default.TrendingDown,
        modifier = Modifier.weight(1f)
      )

      // Net Balance Card
      CompactLedgerSummaryCard(
        title = "အသားတင် လက်ကျန်",
        subtitle = if (netBalance >= 0) "ငွေပို (Surplus)" else "လိုငွေ (Deficit)",
        amount = netBalance,
        color = if (netBalance >= 0) Color(0xFF1565C0) else StatusCritical,
        containerColor = if (netBalance >= 0) Color(0xFFE3F2FD) else Color(0xFFFFEBEE),
        icon = Icons.Default.AccountBalanceWallet,
        modifier = Modifier.weight(1f)
      )
    }

    // Cash vs Digital Breakdown Mini Strip
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 0.dp),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(0.6.dp, BorderGrey)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Payments, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ငွေသား (Cash In-Hand): ", fontSize = 10.sp, color = DarkGrayText)
          Text("${cashIncome.toInt()} Ks", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ဒီဂျစ်တယ် (KPay / Wave): ", fontSize = 10.sp, color = DarkGrayText)
          Text("${digitalIncome.toInt()} Ks", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 3. Filter Chips (All | Income | Expense)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LedgerTypeFilter.values().forEach { type ->
          val isSelected = typeFilter == type
          FilterChip(
            selected = isSelected,
            onClick = { typeFilter = type },
            label = {
              Text(
                "${type.myanmarLabel} (${when (type) {
                  LedgerTypeFilter.ALL -> dateFilteredEntries.size
                  LedgerTypeFilter.INCOME -> dateFilteredEntries.count { it.isIncome }
                  LedgerTypeFilter.EXPENSE -> dateFilteredEntries.count { !it.isIncome }
                }})",
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            shape = RoundedCornerShape(6.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = when (type) {
                LedgerTypeFilter.ALL -> SoftGreenContainer
                LedgerTypeFilter.INCOME -> SoftGreenContainer
                LedgerTypeFilter.EXPENSE -> Color(0xFFFFEBEE)
              },
              selectedLabelColor = when (type) {
                LedgerTypeFilter.ALL -> MedicalGreenDark
                LedgerTypeFilter.INCOME -> MedicalGreenDark
                LedgerTypeFilter.EXPENSE -> MahoganyAccent
              }
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = BorderGrey,
              selectedBorderColor = MedicalGreen
            ),
            modifier = Modifier.height(30.dp)
          )
        }
      }

      Text(
        "စုစုပေါင်း ${finalFilteredEntries.size} စာရင်း",
        fontSize = 10.sp,
        color = SlateMedium,
        fontWeight = FontWeight.Medium
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 4. Monthly View Day-by-Day Summary Table OR Transactions Ledger List
    if (filterMode == LedgerDateFilterMode.MONTHLY && dailyBreakdown.isNotEmpty()) {
      Text(
        "နေ့အလိုက် ဝင်/ထွက် စာရင်းအချုပ်ဇယား (Daily Monthly Summary Table)",
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        color = MedicalGreenDark,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(0.6.dp, BorderGrey)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Table Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(SoftGreenContainer)
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("ရက်စွဲ (Date)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark, modifier = Modifier.weight(1.2f))
            Text("ဝင်ငွေ (Income)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("ထွက်ငွေ (Expense)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MahoganyAccent, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("လက်ကျန် (Net)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
          }

          Divider(color = BorderGrey, thickness = 0.5.dp)

          // Table Rows
          dailyBreakdown.take(7).forEach { (date, inc, exp) ->
            val net = inc - exp
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable {
                  selectedDateStr = date
                  filterMode = LedgerDateFilterMode.DAILY
                },
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(date, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText, modifier = Modifier.weight(1.2f))
              Text("${inc.toInt()} Ks", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreen, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
              Text("${exp.toInt()} Ks", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MahoganyAccent, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
              Text(
                "${net.toInt()} Ks",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0) MedicalGreenDark else StatusCritical,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
              )
            }
            Divider(color = BorderGrey.copy(alpha = 0.4f), thickness = 0.5.dp)
          }
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
    }

    // 5. Individual Transaction Cards List
    Text(
      "အသေးစိတ် ပြေစာနှင့် ဘောင်ချာများ (Detailed Transaction Items)",
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Bold,
      color = SlateMedium,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
    )

    if (finalFilteredEntries.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text("ဤရက်စွဲအပိုင်းအခြားအတွက် ဝင်ငွေ/ထွက်ငွေ စာရင်းမရှိသေးပါ", fontSize = 11.sp, color = SlateMedium)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        items(finalFilteredEntries, key = { "${it.voucherNo}_${it.referenceId}" }) { entry ->
          LedgerEntryItemCard(
            entry = entry,
            onDelete = {
              if (!entry.isIncome) {
                viewModel.deleteExpense(entry.referenceId)
                Toast.makeText(context, "ထွက်ငွေဘောင်ချာ ပယ်ဖျက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
              }
            }
          )
        }
      }
    }
  }

  // Dialog: Add New Clinic Outflow / Expense Voucher
  if (showAddExpenseDialog) {
    AddClinicExpenseDialog(
      onDismiss = { showAddExpenseDialog = false },
      onConfirm = { title, category, amount, method, payee, notes, date ->
        viewModel.addExpense(
          title = title,
          category = category,
          amount = amount,
          paymentMethod = method,
          payee = payee,
          notes = notes,
          date = date
        )
        showAddExpenseDialog = false
        Toast.makeText(context, "ထွက်ငွေဘောင်ချာ အောင်မြင်စွာ မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Dialog: Export CSV Summary
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = { Text("CSV စာရင်းဖိုင် သိမ်းဆည်းပြီး", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "ဆေးခန်းဝင်ငွေ ထွက်ငွေအချက်အလက်များကို shwe_khit_expenses.json နှင့် CSV format အဖြစ် local storage တွင် offline-first သိမ်းဆည်းပြီးဖြစ်ပါသည်။ Excel / Spreadsheet ဖြင့်ဖွင့်ကြည့်နိုင်ပါသည်။",
          fontSize = 11.5.sp,
          color = DarkGrayText
        )
      },
      confirmButton = {
        Button(
          onClick = { showExportDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
        ) {
          Text("နားလည်ပါပြီ", fontSize = 11.sp)
        }
      }
    )
  }
}

/**
 * Compact Ledger Summary Card with 8dp tight padding for standard mobile layout
 */
@Composable
fun CompactLedgerSummaryCard(
  title: String,
  subtitle: String,
  amount: Double,
  color: Color,
  containerColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          title,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = SlateMedium,
          maxLines = 1
        )
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "${amount.toInt()} Ks",
        fontSize = 13.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        subtitle,
        fontSize = 8.5.sp,
        color = SlateMuted,
        fontWeight = FontWeight.Medium,
        maxLines = 1
      )
    }
  }
}

/**
 * Single Transaction Item Card (Income / Expense)
 */
@Composable
fun LedgerEntryItemCard(
  entry: LedgerEntry,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(0.6.dp, BorderGrey)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Icon badge
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (entry.isIncome) SoftGreenContainer else Color(0xFFFFEBEE)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            if (entry.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = if (entry.isIncome) MedicalGreen else MahoganyAccent,
            modifier = Modifier.size(15.dp)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              entry.voucherNo,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = SlateMedium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(3.dp),
              color = if (entry.isIncome) SoftGreenContainer else Color(0xFFF3E5F5)
            ) {
              Text(
                entry.category,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isIncome) MedicalGreenDark else Color(0xFF7B1FA2),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }

          Text(
            entry.title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkGrayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              entry.timestamp,
              fontSize = 8.5.sp,
              color = SlateMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "• ${entry.paymentMethod.label}",
              fontSize = 8.5.sp,
              color = MedicalGreenDark,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Amount & Delete Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = (if (entry.isIncome) "+" else "-") + "${entry.amount.toInt()} Ks",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (entry.isIncome) MedicalGreen else MahoganyAccent
          )
          Text(
            if (entry.isIncome) "Paid" else "Outflow",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (entry.isIncome) MedicalGreenDark else SlateMedium
          )
        }

        if (!entry.isIncome) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              Icons.Default.DeleteOutline,
              contentDescription = "Delete Expense Voucher",
              tint = SlateMuted,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Add Clinic Outflow / Expense Voucher Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClinicExpenseDialog(
  onDismiss: () -> Unit,
  onConfirm: (
    title: String,
    category: String,
    amount: Double,
    method: PaymentMethod,
    payee: String,
    notes: String,
    date: String
  ) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Medical Consumables") }
  var amountStr by remember { mutableStateOf("") }
  var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var payee by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

  val categories = listOf(
    "Medical Consumables",
    "Stock Purchase",
    "Utilities & Power",
    "Staff Allowance",
    "Oxygen & Gas Refill",
    "Equipment Maintenance",
    "Clinic Rent",
    "Waste Disposal",
    "Other"
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(10.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFFEBEE)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MahoganyAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("ထွက်ငွေဘောင်ချာ အသစ်ထည့်ရန်", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              Text("Clinic Outflow / Expense Voucher", fontSize = 9.sp, color = SlateMedium)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        Divider(color = BorderGrey, thickness = 0.5.dp)

        // Title
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("အသုံးစရိတ် ခေါင်းစဉ် (Title / Reason)", fontSize = 10.sp) },
          placeholder = { Text("ဥပမာ - မီးစက်ဒီဇယ်၊ အောက်ဆီဂျင်ဖြည့်ခြင်း", fontSize = 10.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Amount & Date
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = amountStr,
            onValueChange = { if (it.all { ch -> ch.isDigit() }) amountStr = it },
            label = { Text("ကျသင့်ငွေ ကျပ် (Amount Ks)", fontSize = 10.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.2f)
          )
          OutlinedTextField(
            value = dateStr,
            onValueChange = { dateStr = it },
            label = { Text("ရက်စွဲ (YYYY-MM-DD)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        // Category Selection
        Text("အသုံးစရိတ် အမျိုးအစား (Category):", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val mainCategories = listOf("Medical Consumables", "Stock Purchase", "Utilities & Power", "Staff Allowance")
          mainCategories.forEach { cat ->
            val isSelected = category == cat
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isSelected) SoftGreenContainer else BackgroundSurface,
              border = BorderStroke(0.6.dp, if (isSelected) MedicalGreen else BorderGrey),
              modifier = Modifier
                .weight(1f)
                .clickable { category = cat }
            ) {
              Text(
                cat.take(10) + "..",
                fontSize = 8.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MedicalGreenDark else DarkGrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
              )
            }
          }
        }

        // Payee (ဆိုင် / လက်ခံသူ)
        OutlinedTextField(
          value = payee,
          onValueChange = { payee = it },
          label = { Text("လက်ခံသူ / ဆိုင်အမည် (Payee / Supplier)", fontSize = 10.sp) },
          placeholder = { Text("ဥပမာ - AA Medical / Denko Fuel", fontSize = 10.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Payment Method
        Text("ပေးချေသည့် နည်းလမ်း (Payment Method):", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PaymentMethod.values().forEach { method ->
            val isSelected = selectedMethod == method
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) SoftGreenContainer else BackgroundSurface,
              border = BorderStroke(0.8.dp, if (isSelected) MedicalGreen else BorderGrey),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedMethod = method }
            ) {
              Text(
                method.label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MedicalGreenDark else DarkGrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Confirm & Cancel Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(38.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("မလုပ်တော့ပါ", fontSize = 11.sp)
          }

          Button(
            onClick = {
              val amt = amountStr.toDoubleOrNull() ?: 0.0
              if (title.isNotBlank() && amt > 0) {
                onConfirm(title, category, amt, selectedMethod, payee, notes, dateStr)
              }
            },
            enabled = title.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            modifier = Modifier.weight(1f).height(38.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("မှတ်တမ်းတင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
