package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditActionType
import com.example.model.AuditLogEntry
import com.example.model.AuditModule
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class LogDateFilter(val label: String, val myanmarLabel: String) {
  ALL("All Time", "အားလုံး"),
  TODAY("Today", "ယနေ့"),
  THIS_WEEK("This Week", "ယခုအပတ်"),
  THIS_MONTH("This Month", "ယခုလ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
  viewModel: ClinicViewModel
) {
  val auditLogs by viewModel.auditLogs.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var selectedModuleFilter by remember { mutableStateOf("ALL") }
  var selectedDateFilter by remember { mutableStateOf(LogDateFilter.ALL) }

  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
  val calendar = remember { Calendar.getInstance() }

  // Date boundaries
  val now = System.currentTimeMillis()
  val startOfToday = remember(now) {
    calendar.timeInMillis = now
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.timeInMillis
  }
  val startOfWeek = remember(startOfToday) {
    calendar.timeInMillis = startOfToday
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    calendar.timeInMillis
  }
  val startOfMonth = remember(startOfToday) {
    calendar.timeInMillis = startOfToday
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.timeInMillis
  }

  // Filtered Logs
  val filteredLogs = remember(auditLogs, searchQuery, selectedModuleFilter, selectedDateFilter) {
    auditLogs.filter { log ->
      // Module filter
      val matchesModule = when (selectedModuleFilter) {
        "ALL" -> true
        "WASTAGE" -> log.actionType == AuditActionType.WASTAGE_WRITE_OFF.displayName
        "INVENTORY" -> log.module == AuditModule.INVENTORY.displayName
        "BILLING" -> log.module == AuditModule.BILLING.displayName
        "PATIENT" -> log.module == AuditModule.PATIENT.displayName
        else -> true
      }

      // Date filter
      val matchesDate = when (selectedDateFilter) {
        LogDateFilter.ALL -> true
        LogDateFilter.TODAY -> log.timestamp >= startOfToday
        LogDateFilter.THIS_WEEK -> log.timestamp >= startOfWeek
        LogDateFilter.THIS_MONTH -> log.timestamp >= startOfMonth
      }

      // Search query
      val matchesSearch = if (searchQuery.isBlank()) true else {
        log.targetName.contains(searchQuery, ignoreCase = true) ||
          log.operatorName.contains(searchQuery, ignoreCase = true) ||
          log.reason.contains(searchQuery, ignoreCase = true) ||
          log.details.contains(searchQuery, ignoreCase = true)
      }

      matchesModule && matchesDate && matchesSearch
    }
  }

  // Aggregate stats
  val totalWastageCount = auditLogs.count { it.actionType == AuditActionType.WASTAGE_WRITE_OFF.displayName }
  val totalWastageCost = auditLogs
    .filter { it.actionType == AuditActionType.WASTAGE_WRITE_OFF.displayName }
    .sumOf { it.costAmount }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundSurface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Top Header Banner
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MedicalGreenDark),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              "Audit & Activity Logs (လုပ်ငန်းမှတ်တမ်းချုပ်)",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = DarkGrayText
            )
            Text(
              "ဆေးခန်းအတွင်း အပြောင်းအလဲများနှင့် ဆေးဝါးအသုံး/အလေအလွင့် မှတ်တမ်းများ",
              fontSize = 11.sp,
              color = SlateMuted
            )
          }
        }

        // Summary Badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftGreenContainer,
            border = BorderStroke(0.8.dp, MedicalGreen.copy(alpha = 0.4f))
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
              Text("မှတ်တမ်းစုစုပေါင်း", fontSize = 9.sp, color = MedicalGreenDark)
              Text("${auditLogs.size} ခု", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MedicalGreenDark)
            }
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFFFFBEB),
            border = BorderStroke(0.8.dp, Color(0xFFFDE68A))
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
              Text("ဆေးဝါးလေလွင့်/အသုံး", fontSize = 9.sp, color = Color(0xFF92400E))
              Text(
                "${currencyFmt.format(totalWastageCost.toInt())} Ks ($totalWastageCount ကြိမ်)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF92400E)
              )
            }
          }
        }
      }
    }

    // 2. Search & Filter Bar
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("ဆေးအမည်၊ ပြုလုပ်သူ၊ အကြောင်းပြချက်မှတ်ချက် ရှာဖွေရန်...", fontSize = 11.5.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = SlateMedium) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          singleLine = true
        )

        // Module Filter Chips + Date Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("ကဏ္ဍ:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)

          val moduleOptions = listOf(
            "ALL" to "အားလုံး (All)",
            "WASTAGE" to "ဆေးဝါးအသုံး/အလေအလွင့် (Wastage)",
            "INVENTORY" to "စတော့ (Inventory)",
            "BILLING" to "ပြေစာ/ငွေစာရင်း (Billing)",
            "PATIENT" to "လူနာမှတ်တမ်း (Patient)"
          )

          moduleOptions.forEach { (key, label) ->
            val isSelected = selectedModuleFilter == key
            FilterChip(
              selected = isSelected,
              onClick = { selectedModuleFilter = key },
              label = { Text(label, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (key == "WASTAGE") Color(0xFFD97706) else MedicalGreenDark,
                selectedLabelColor = PureWhite
              )
            )
          }

          Spacer(modifier = Modifier.width(8.dp))
          Text("ရက်စွဲ:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)

          LogDateFilter.entries.forEach { dateFilter ->
            val isSelected = selectedDateFilter == dateFilter
            FilterChip(
              selected = isSelected,
              onClick = { selectedDateFilter = dateFilter },
              label = { Text(dateFilter.myanmarLabel, fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SkyBlueDark,
                selectedLabelColor = PureWhite
              )
            )
          }
        }
      }
    }

    // 3. Log Records List
    if (filteredLogs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(10.dp))
          .background(PureWhite),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(48.dp), tint = SlateLight)
          Text(
            if (auditLogs.isEmpty()) "မှတ်တမ်းတင်ထားသော စာရင်းအပြောင်းအလဲများ မရှိသေးပါ"
            else "ရှာဖွေမှုနှင့် ကိုက်ညီသော မှတ်တမ်း မတွေ့ရှိပါ",
            color = SlateMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredLogs, key = { it.id }) { log ->
          AuditLogItemCard(log = log, currencyFmt = currencyFmt)
        }
      }
    }
  }
}

@Composable
fun AuditLogItemCard(
  log: AuditLogEntry,
  currencyFmt: NumberFormat
) {
  val isWastage = log.actionType == AuditActionType.WASTAGE_WRITE_OFF.displayName
  val borderColor = if (isWastage) Color(0xFFFDE68A) else BorderGrey
  val badgeBg = if (isWastage) Color(0xFFFEF3C7) else SoftGreenContainer
  val badgeColor = if (isWastage) Color(0xFF92400E) else MedicalGreenDark

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(0.8.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // 1. Header Row (Module badge, Action badge, Date/Time, Operator)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeBg
          ) {
            Text(
              log.actionType,
              color = badgeColor,
              fontWeight = FontWeight.Bold,
              fontSize = 9.5.sp,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = BgLightGray
          ) {
            Text(
              log.module,
              color = SlateDark,
              fontSize = 9.sp,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // Timestamp & Operator
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = SlateMuted)
          Spacer(modifier = Modifier.width(3.dp))
          Text(log.formattedDate, fontSize = 9.5.sp, color = SlateMuted)
          Spacer(modifier = Modifier.width(8.dp))
          Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = SlateMuted)
          Spacer(modifier = Modifier.width(3.dp))
          Text("${log.operatorName} (${log.operatorRole})", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        }
      }

      // 2. Target Item & Details
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            log.targetName,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = DarkGrayText
          )
          if (log.details.isNotBlank()) {
            Text(
              log.details,
              fontSize = 10.5.sp,
              color = SlateMedium
            )
          }
        }

        if (log.costAmount > 0.0) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFFFBEB),
            border = BorderStroke(0.6.dp, Color(0xFFFDE68A))
          ) {
            Text(
              "ကျသင့်တန်ဖိုး: ${currencyFmt.format(log.costAmount.toInt())} Ks",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFB45309),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }
      }

      // 3. Highlighted Mandatory Reason Box
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            Icons.Default.Comment,
            contentDescription = null,
            tint = if (isWastage) Color(0xFFD97706) else SkyBlueDark,
            modifier = Modifier.size(14.dp).padding(top = 1.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              "အကြောင်းပြချက် / မှတ်ချက် (Remark):",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = SlateDark
            )
            Text(
              log.reason.ifBlank { "မှတ်ချက်မရှိ" },
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              color = if (log.reason.isNotBlank()) DarkGrayText else SlateMuted
            )
          }
        }
      }
    }
  }
}
