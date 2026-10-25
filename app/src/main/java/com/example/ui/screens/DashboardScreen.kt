package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.ClinicDataExportDialog
import com.example.ui.components.ShweKhitLogo
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * High-Utility Clinical Analytics Dashboard
 * 
 * Replaces generic shortcuts with actionable, real-world clinic intelligence:
 * 1. Daily & Monthly Patient Attendance Graph (Canvas Bar Charts & Metrics)
 * 2. Top Prescribed Medicines (Dispensed Volume & Inventory Status)
 * 3. Least Used Medicines (Slow-Moving Stock & Expiry Risk Audit)
 * 4. Most Frequent Clinical Cases & Diagnoses (Disease Distribution & Case Counts)
 * 5. Live Queue Counters & Financial Ticker
 */
@Composable
fun DashboardScreen(
  viewModel: ClinicViewModel,
  onNavigateToTab: (Int) -> Unit,
  onOpenUserAccounts: () -> Unit = {}
) {
  val patients by viewModel.patients.collectAsState()
  val medicines by viewModel.medicines.collectAsState()
  val invoices by viewModel.invoices.collectAsState()
  val expenses by viewModel.expenses.collectAsState()
  val consultations by viewModel.consultations.collectAsState()
  val clinicSettings by viewModel.clinicSettings.collectAsState()

  var showExportDialog by remember { mutableStateOf(false) }
  var trendViewMode by remember { mutableStateOf(0) } // 0: နေ့စဉ် (Daily Trend), 1: လစဉ် (Monthly Trend)

  val waitingCount = patients.count { it.status == QueueStatus.WAITING }
  val inConsultCount = patients.count { it.status == QueueStatus.IN_CONSULTATION }
  val pharmacyCount = patients.count { it.status == QueueStatus.AT_PHARMACY }
  val lowStockCount = medicines.count { it.totalStock <= it.minStockThreshold }
  val todayRevenue = invoices.filter { it.isPaid }.sumOf { it.grandTotal }
  val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
  val todayExpenses = expenses.filter { it.date == todayDateStr }.sumOf { it.amount }
  val todayNetBalance = todayRevenue - todayExpenses

  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  // ---------------- 1. DATA PREPARATION FOR DAILY & MONTHLY TRENDS ----------------
  val dailyStats = remember(consultations, invoices, patients) {
    val cal = Calendar.getInstance()
    val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("EEE", Locale.US)
    val dayNamesMm = mapOf(
      "Mon" to "တနင်္လာ",
      "Tue" to "အင်္ဂါ",
      "Wed" to "ဗုဒ္ဓဟူး",
      "Thu" to "ကြာသပတေး",
      "Fri" to "သောကြာ",
      "Sat" to "စနေ",
      "Sun" to "တနင်္ဂနွေ"
    )

    val list = mutableListOf<DailyPatientStat>()
    // Past 7 days (from 6 days ago up to today)
    for (i in 6 downTo 0) {
      val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
      val dateKey = sdfKey.format(dayCal.time)
      val dayEng = sdfDisplay.format(dayCal.time)
      val dayMm = dayNamesMm[dayEng] ?: dayEng

      // Count actual patient encounters on this date
      val consCount = consultations.count { it.timestamp.startsWith(dateKey) }
      val invCount = invoices.count { it.dateString == dateKey }
      val patCount = patients.count { it.registrationDate == dateKey }

      // Total distinct visits or encounters recorded on this date
      val totalCount = maxOf(consCount, invCount, patCount)

      list.add(
        DailyPatientStat(
          dateStr = dateKey,
          label = if (i == 0) "ယနေ့" else dayMm,
          subLabel = dayEng,
          count = totalCount,
          isToday = (i == 0)
        )
      )
    }
    list
  }

  val monthlyStats = remember(consultations, invoices, patients) {
    val sdfMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val monthNamesMm = listOf(
      "မေ" to "May",
      "ဇွန်" to "Jun",
      "ဇူလိုင်" to "Jul",
      "သြဂုတ်" to "Aug",
      "စက်တင်ဘာ" to "Sep",
      "အောက်တိုဘာ" to "Oct"
    )
    val cal = Calendar.getInstance()
    val list = mutableListOf<MonthlyPatientStat>()

    for (i in 5 downTo 0) {
      val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
      val mKey = sdfMonthKey.format(mCal.time)
      val (mmName, enName) = monthNamesMm.getOrElse(5 - i) { "လ" to "M" }

      val consCount = consultations.count { it.timestamp.startsWith(mKey) }
      val invCount = invoices.count { it.dateString.startsWith(mKey) }
      val patCount = patients.count { it.registrationDate.startsWith(mKey) }
      val total = maxOf(consCount, invCount, patCount)

      list.add(
        MonthlyPatientStat(
          monthKey = mKey,
          labelMm = mmName,
          labelEn = enName,
          count = total,
          isCurrent = (i == 0)
        )
      )
    }
    list
  }

  // ---------------- 2. MEDICINE USAGE (TOP DISPENSED & LEAST DISPENSED) ----------------
  val (topMedicines, leastMedicines) = remember(medicines, consultations, invoices) {
    val usageMap = mutableMapOf<String, Int>()

    // Sum from consultation prescriptions
    consultations.forEach { cons ->
      cons.prescriptions.forEach { rx ->
        val cleanName = rx.name.trim()
        usageMap[cleanName] = (usageMap[cleanName] ?: 0) + rx.quantity
      }
    }

    // Sum from invoice pharmacy items
    invoices.forEach { inv ->
      inv.items.filter { it.category.equals("Pharmacy", ignoreCase = true) }.forEach { item ->
        val cleanName = item.description.replace(Regex("""\s*\(.*\)"""), "").trim()
        usageMap[cleanName] = (usageMap[cleanName] ?: 0) + item.quantity
      }
    }

    // Top Medicines sorted descending (strictly from recorded usage)
    val topList = usageMap.entries
      .sortedByDescending { it.value }
      .take(6)
      .map { entry ->
        val med = medicines.firstOrNull { it.name.contains(entry.key.take(10), ignoreCase = true) }
        MedicineUsageItem(
          name = entry.key,
          genericOrCategory = med?.category ?: "ဆေးဝါး",
          dispensedCount = entry.value,
          unitType = med?.unitType ?: "Tabs",
          stockRemaining = med?.totalStock ?: 0
        )
      }

    // Least Medicines: Medicines in inventory sorted by lowest recorded dispensed count
    val leastList = medicines
      .sortedWith(compareBy<Medicine> { med ->
        usageMap.entries.firstOrNull { it.key.contains(med.name.take(10), ignoreCase = true) }?.value ?: 0
      }.thenBy { it.earliestExpiry })
      .take(5)
      .map { med ->
        val count = usageMap.entries.firstOrNull { it.key.contains(med.name.take(10), ignoreCase = true) }?.value ?: 0
        LeastUsedMedicineItem(
          name = med.name,
          category = med.category,
          dispensedCount = count,
          currentStock = med.totalStock,
          unitType = med.unitType,
          earliestExpiry = med.earliestExpiry
        )
      }

    Pair(topList, leastList)
  }

  // ---------------- 3. FREQUENT CLINICAL CASES & DIAGNOSES ----------------
  val frequentCases = remember(consultations, patients) {
    val caseMap = mutableMapOf<String, Int>()

    consultations.forEach { cons ->
      val diag = cons.assessment.ifBlank { cons.diagnosisList.firstOrNull() ?: "" }
      if (diag.isNotBlank()) {
        val categoryKey = when {
          diag.contains("Respiratory", true) || diag.contains("URI", true) || diag.contains("Cough", true) || diag.contains("Pharyngitis", true) ->
            "Upper Respiratory Tract Infection (URI) • ဖျားနာ/ချောင်းဆိုး"
          diag.contains("Hypertension", true) || diag.contains("HTN", true) || diag.contains("Blood Pressure", true) ->
            "Essential Hypertension (HTN) • သွေးတိုးရောဂါ"
          diag.contains("Gastritis", true) || diag.contains("GERD", true) || diag.contains("Epigastric", true) ->
            "Acute Gastritis & Acid Reflux • အစာအိမ်ရောဂါ"
          diag.contains("Diabetes", true) || diag.contains("T2DM", true) || diag.contains("Sugar", true) ->
            "Type 2 Diabetes Mellitus • ဆီးချိုရောဂါ"
          diag.contains("Abrasion", true) || diag.contains("Wound", true) || diag.contains("Accident", true) || diag.contains("Trauma", true) ->
            "Minor Trauma & Abrasion Wound • ထိခိုက်ဒဏ်ရာ"
          diag.contains("Abscess", true) || diag.contains("Cellulitis", true) || diag.contains("Infection", true) ->
            "Skin Infection & Subcutaneous Abscess • ပြည်တည်နာ"
          diag.contains("Asthma", true) || diag.contains("Bronchospasm", true) || diag.contains("Wheezing", true) ->
            "Bronchial Asthma & Bronchospasm • ရင်ကျပ်မော"
          diag.contains("Dermatitis", true) || diag.contains("Eczema", true) || diag.contains("Allergy", true) ->
            "Allergic Dermatitis & Skin Rash • အရေပြားယားယံ"
          else -> "General Medical Consultation • အထွေထွေကျန်းမာရေး"
        }
        caseMap[categoryKey] = (caseMap[categoryKey] ?: 0) + 1
      }
    }

    val totalCases = caseMap.values.sum().coerceAtLeast(1)
    caseMap.entries
      .sortedByDescending { it.value }
      .map { entry ->
        val pct = (entry.value * 100f / totalCases)
        ClinicalCaseItem(
          title = entry.key,
          count = entry.value,
          percentage = pct
        )
      }
  }

  // ---------------- UI RENDER ----------------
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. Clinic Branding Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = OceanGreenPrimary)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "\"${clinicSettings.clinicNameMm.ifBlank { "ရွှေခေတ်" }}\" ${clinicSettings.clinicSubtitleMm.ifBlank { "အထွေထွေရောဂါကုဆေးခန်း" }}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = PureWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              "${clinicSettings.doctorName} ${clinicSettings.doctorDegree} • ဆမ-${clinicSettings.doctorSama}",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium,
              color = PureWhite.copy(alpha = 0.9f)
            )
            Text(
              "Clinical Intelligence & Analytics Dashboard • EMR System Active",
              fontSize = 10.sp,
              color = SoftGreenContainer
            )
          }
          ShweKhitLogo(
            size = 40.dp,
            customLogoUri = clinicSettings.clinicLogoUri.ifBlank { null }
          )
        }
      }
    }

    // 2. Real-time Live Queue & Financial Status Ticker
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        DashboardMetricBadge(
          title = "Waiting",
          labelMm = "စောင့်ဆိုင်း",
          value = "$waitingCount",
          color = StatusWarning,
          icon = Icons.Default.HourglassEmpty,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateToTab(1) }
        )
        DashboardMetricBadge(
          title = "Consulting",
          labelMm = "စမ်းသပ်ဆဲ",
          value = "$inConsultCount",
          color = OceanGreenPrimary,
          icon = Icons.Default.MedicalServices,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateToTab(2) }
        )
        DashboardMetricBadge(
          title = "Pharmacy",
          labelMm = "ဆေးထုတ်",
          value = "$pharmacyCount",
          color = Color(0xFF0F766E),
          icon = Icons.Default.Medication,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateToTab(3) }
        )
        DashboardMetricBadge(
          title = "Low Stock",
          labelMm = "စတော့နည်း",
          value = "$lowStockCount",
          color = if (lowStockCount > 0) StatusCritical else StatusSuccess,
          icon = Icons.Default.WarningAmber,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateToTab(6) }
        )
        DashboardMetricBadge(
          title = "Today Net",
          labelMm = "ဝင်ငွေ",
          value = "${currencyFmt.format(todayNetBalance.toInt())} K",
          color = MedicalGreenDark,
          icon = Icons.Default.AccountBalanceWallet,
          modifier = Modifier.weight(1.3f),
          onClick = { onNavigateToTab(7) }
        )
      }
    }

    // 3. SECTION: လူနာအရေအတွက် နေစဉ်၊ လစဉ် (Patient Volume Analytics with Interactive Graph)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          // Header with View Toggle (Daily vs Monthly)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(SoftGreenContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "လူနာအရေအတွက် ဆန်းစစ်ချက် (Patient Volume Trends)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = DarkGrayText
                )
                Text(
                  if (trendViewMode == 0) "လွန်ခဲ့သော ၇ ရက်အတွင်း နေ့စဉ် လာရောက်မှု" else "လွန်ခဲ့သော ၆ လအတွင်း လစဉ် လာရောက်မှု",
                  fontSize = 10.sp,
                  color = SlateMedium
                )
              }
            }

            // Segmented Pills
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F5F9))
                .padding(2.dp)
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(if (trendViewMode == 0) OceanGreenPrimary else Color.Transparent)
                  .clickable { trendViewMode = 0 }
                  .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  "နေ့စဉ် (Daily)",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (trendViewMode == 0) PureWhite else DarkGrayText
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(18.dp))
                  .background(if (trendViewMode == 1) OceanGreenPrimary else Color.Transparent)
                  .clickable { trendViewMode = 1 }
                  .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  "လစဉ် (Monthly)",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (trendViewMode == 1) PureWhite else DarkGrayText
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Native Compose Canvas Chart
          if (trendViewMode == 0) {
            DailyPatientBarChart(dailyStats = dailyStats)
            Spacer(modifier = Modifier.height(10.dp))
            // KPI Summary Row
            val total7Days = dailyStats.sumOf { it.count }
            val avgPerDay = String.format(Locale.US, "%.1f", total7Days / 7.0)
            val peakDay = dailyStats.maxByOrNull { it.count }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              KpiStatItem(label = "၇ ရက် စုစုပေါင်း", value = "$total7Days ဦး", color = OceanGreenPrimary)
              KpiStatItem(label = "ပျမ်းမျှ (တစ်ရက်)", value = "$avgPerDay ဦး", color = Color(0xFF0F766E))
              KpiStatItem(label = "အများဆုံးနေ့", value = "${peakDay?.label ?: "-"} (${peakDay?.count ?: 0} ဦး)", color = Color(0xFFB45309))
            }
          } else {
            MonthlyPatientColumnChart(monthlyStats = monthlyStats)
            Spacer(modifier = Modifier.height(10.dp))
            // Monthly KPI Summary Row
            val total6Months = monthlyStats.sumOf { it.count }
            val currentMonth = monthlyStats.lastOrNull()?.count ?: 0
            val peakMonth = monthlyStats.maxByOrNull { it.count }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              KpiStatItem(label = "၆ လ စုစုပေါင်း", value = "$total6Months ဦး", color = OceanGreenPrimary)
              KpiStatItem(label = "ယခုလ (လက်ရှိ)", value = "$currentMonth ဦး", color = Color(0xFF0F766E))
              KpiStatItem(label = "အများဆုံးလ", value = "${peakMonth?.labelMm ?: "-"} (${peakMonth?.count ?: 0} ဦး)", color = Color(0xFFB45309))
            }
          }
        }
      }
    }

    // 4. SECTION: အသုံးများသောဆေး (Top Dispensed / Prescribed Medicines)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "အသုံးများသော ဆေးဝါးများ (Top Prescribed Medicines)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = DarkGrayText
                )
                Text(
                  "လူနာများအတွက် အများဆုံး ညွှန်ကြားထုတ်ပေးခဲ့သော ဆေးဝါးများ",
                  fontSize = 10.sp,
                  color = SlateMedium
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFF0FDF4),
              border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
              Text(
                "High Volume",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreenDark,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (topMedicines.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("ညွှန်ကြားထုတ်ပေးထားသော ဆေးဝါးမှတ်တမ်း မရှိသေးပါ (No dispensed medicines recorded yet)", fontSize = 10.sp, color = SlateMuted)
            }
          } else {
            val maxDispensed = topMedicines.maxOfOrNull { it.dispensedCount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
            topMedicines.forEachIndexed { index, item ->
              TopMedicineRow(
                rank = index + 1,
                item = item,
                maxCount = maxDispensed
              )
              if (index < topMedicines.size - 1) {
                Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 6.dp))
              }
            }
          }
        }
      }
    }

    // 5. SECTION: အသုံးနည်းသောဆေး (Slow-Moving Medicines & Expiry Risk Audit)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "အသုံးနည်းသော ဆေးဝါးများ (Slow-Moving Stocks & Expiry Risk)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = DarkGrayText
                )
                Text(
                  "ဆေးသေ/စတော့ငြိမ်နေပြီး သက်တမ်းကုန်လွန်နိုင်ခြေ စောင့်ကြည့်ရန်",
                  fontSize = 10.sp,
                  color = SlateMedium
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFFFBEB),
              border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
              Text(
                "Stock Audit",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB45309),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          leastMedicines.forEachIndexed { index, item ->
            LeastUsedMedicineRow(item = item)
            if (index < leastMedicines.size - 1) {
              Divider(color = Color(0xFFF1F5F9), thickness = 0.8.dp, modifier = Modifier.padding(vertical = 6.dp))
            }
          }
        }
      }
    }

    // 6. SECTION: အလာများသော Case များ (Most Frequent Clinical Cases & Diagnoses)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFF3E8FF)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFF7E22CE), modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "အလာများသော ရောဂါနှင့် Case များ (Frequent Diagnoses)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = DarkGrayText
                )
                Text(
                  "ဆေးခန်းသို့ အများဆုံး လာရောက်ပြသသော ရောဂါလက္ခဏာ ဖြန့်ကြက်မှု",
                  fontSize = 10.sp,
                  color = SlateMedium
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFAF5FF),
              border = BorderStroke(1.dp, Color(0xFFE9D5FF))
            ) {
              Text(
                "Epi Profile",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B21A8),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (frequentCases.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("ရောဂါအမည် သုံးသပ်ချက် မှတ်တမ်း မရှိသေးပါ (No diagnosis records yet)", fontSize = 10.sp, color = SlateMuted)
            }
          } else {
            frequentCases.forEachIndexed { index, item ->
              FrequentCaseRow(item = item)
              if (index < frequentCases.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
              }
            }
          }
        }
      }
    }

    // 7. Full Data Backup & Excel CSV Export
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showExportDialog = true },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFECFDF5),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(18.dp))
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Export Data to Excel (CSV) & Full Backup", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
              Text("လူနာ၊ ဆေးဝါးလက်ကျန်၊ ရောဂါမှတ်တမ်းနှင့် ငွေစာရင်းများ Excel ထုတ်ယူရန်", fontSize = 9.5.sp, color = SlateMedium)
            }
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(18.dp))
        }
      }
    }
  }

  if (showExportDialog) {
    ClinicDataExportDialog(
      patients = patients,
      medicines = medicines,
      consultations = consultations,
      invoices = invoices,
      vaccines = viewModel.vaccineRecords.collectAsState().value,
      onDismiss = { showExportDialog = false }
    )
  }
}

// ---------------- SUB-COMPONENTS & GRAPH CANVASES ----------------

data class DailyPatientStat(
  val dateStr: String,
  val label: String,
  val subLabel: String,
  val count: Int,
  val isToday: Boolean
)

data class MonthlyPatientStat(
  val monthKey: String,
  val labelMm: String,
  val labelEn: String,
  val count: Int,
  val isCurrent: Boolean
)

data class MedicineUsageItem(
  val name: String,
  val genericOrCategory: String,
  val dispensedCount: Int,
  val unitType: String,
  val stockRemaining: Int
)

data class LeastUsedMedicineItem(
  val name: String,
  val category: String,
  val dispensedCount: Int,
  val currentStock: Int,
  val unitType: String,
  val earliestExpiry: String
)

data class ClinicalCaseItem(
  val title: String,
  val count: Int,
  val percentage: Float
)

@Composable
fun DashboardMetricBadge(
  title: String,
  labelMm: String,
  value: String,
  color: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier.clickable { onClick() },
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
  ) {
    Column(
      modifier = Modifier.padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          value,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = DarkGrayText,
          maxLines = 1
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        labelMm,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        color = SlateMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun DailyPatientBarChart(dailyStats: List<DailyPatientStat>) {
  val maxCount = dailyStats.maxOfOrNull { it.count }?.toFloat()?.coerceAtLeast(1f) ?: 1f

  Column(modifier = Modifier.fillMaxWidth()) {
    // Custom Bar Chart Canvas
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .padding(horizontal = 4.dp)
    ) {
      val w = size.width
      val h = size.height
      val barCount = dailyStats.size
      val slotWidth = w / barCount
      val barWidth = slotWidth * 0.52f
      val maxBarH = h - 28f // reserve top for labels

      dailyStats.forEachIndexed { index, stat ->
        val centerX = slotWidth * index + slotWidth / 2f
        val barLeft = centerX - barWidth / 2f
        val ratio = (stat.count / maxCount).coerceIn(0.12f, 1.0f)
        val barHeight = maxBarH * ratio
        val barTop = h - 2f - barHeight

        // Background subtle guide slot
        drawRoundRect(
          color = Color(0xFFF1F5F9),
          topLeft = Offset(barLeft, h - 2f - maxBarH),
          size = Size(barWidth, maxBarH),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Bar gradient
        val brush = if (stat.isToday) {
          Brush.verticalGradient(
            colors = listOf(OceanGreenPrimary, StatusSuccess),
            startY = barTop,
            endY = h - 2f
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
            startY = barTop,
            endY = h - 2f
          )
        }

        drawRoundRect(
          brush = brush,
          topLeft = Offset(barLeft, barTop),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Highlight ring if today
        if (stat.isToday) {
          drawRoundRect(
            color = OceanGreenPrimary,
            topLeft = Offset(barLeft - 1.5f, barTop - 1.5f),
            size = Size(barWidth + 3f, barHeight + 3f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
          )
        }
      }
    }

    // Value Labels on Top & Day Names Below
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      dailyStats.forEach { stat ->
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            "${stat.count}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (stat.isToday) OceanGreenPrimary else DarkGrayText
          )
          Text(
            stat.label,
            fontSize = 9.sp,
            fontWeight = if (stat.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (stat.isToday) OceanGreenPrimary else SlateMedium,
            maxLines = 1
          )
          Text(
            stat.subLabel,
            fontSize = 7.5.sp,
            color = Color(0xFF94A3B8),
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
fun MonthlyPatientColumnChart(monthlyStats: List<MonthlyPatientStat>) {
  val maxCount = monthlyStats.maxOfOrNull { it.count }?.toFloat()?.coerceAtLeast(1f) ?: 1f

  Column(modifier = Modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .padding(horizontal = 4.dp)
    ) {
      val w = size.width
      val h = size.height
      val colCount = monthlyStats.size
      val slotWidth = w / colCount
      val colWidth = slotWidth * 0.55f
      val maxColH = h - 28f

      monthlyStats.forEachIndexed { index, stat ->
        val centerX = slotWidth * index + slotWidth / 2f
        val colLeft = centerX - colWidth / 2f
        val ratio = (stat.count / maxCount).coerceIn(0.15f, 1.0f)
        val colHeight = maxColH * ratio
        val colTop = h - 2f - colHeight

        // Background guide
        drawRoundRect(
          color = Color(0xFFF1F5F9),
          topLeft = Offset(colLeft, h - 2f - maxColH),
          size = Size(colWidth, maxColH),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        val brush = if (stat.isCurrent) {
          Brush.verticalGradient(
            colors = listOf(Color(0xFF10B981), Color(0xFF047857)),
            startY = colTop,
            endY = h - 2f
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(Color(0xFF6366F1), Color(0xFF4338CA)),
            startY = colTop,
            endY = h - 2f
          )
        }

        drawRoundRect(
          brush = brush,
          topLeft = Offset(colLeft, colTop),
          size = Size(colWidth, colHeight),
          cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      monthlyStats.forEach { stat ->
        Column(
          modifier = Modifier.weight(1f),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            "${stat.count}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (stat.isCurrent) Color(0xFF047857) else DarkGrayText
          )
          Text(
            stat.labelMm,
            fontSize = 9.sp,
            fontWeight = if (stat.isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (stat.isCurrent) Color(0xFF047857) else SlateMedium,
            maxLines = 1
          )
          Text(
            stat.labelEn,
            fontSize = 7.5.sp,
            color = Color(0xFF94A3B8),
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
fun KpiStatItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, fontSize = 9.sp, color = SlateMedium)
    Spacer(modifier = Modifier.height(1.dp))
    Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color)
  }
}

@Composable
fun TopMedicineRow(
  rank: Int,
  item: MedicineUsageItem,
  maxCount: Float
) {
  val ratio = (item.dispensedCount / maxCount).coerceIn(0f, 1f)

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        // Rank pill
        Surface(
          shape = CircleShape,
          color = when (rank) {
            1 -> Color(0xFFFEF3C7)
            2 -> Color(0xFFF1F5F9)
            3 -> Color(0xFFFFEDD5)
            else -> Color(0xFFF8FAFC)
          },
          border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
          modifier = Modifier.size(20.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              "#$rank",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = when (rank) {
                1 -> Color(0xFFB45309)
                2 -> DarkGrayText
                3 -> Color(0xFFC2410C)
                else -> SlateMedium
              }
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            item.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = DarkGrayText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            "${item.genericOrCategory} • လက်ကျန်: ${item.stockRemaining} ${item.unitType}",
            fontSize = 9.5.sp,
            color = SlateMedium
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFECFDF5),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
      ) {
        Text(
          "${item.dispensedCount} ${item.unitType}",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = MedicalGreenDark,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Horizontal proportional progress bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(Color(0xFFE2E8F0))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(ratio)
          .fillMaxHeight()
          .clip(RoundedCornerShape(3.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(OceanGreenPrimary, StatusSuccess)
            )
          )
      )
    }
  }
}

@Composable
fun LeastUsedMedicineRow(item: LeastUsedMedicineItem) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(Color(0xFFFEE2E2)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(13.dp))
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          item.name,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.5.sp,
          color = DarkGrayText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          "${item.category} • စတော့: ${item.currentStock} ${item.unitType} (ထုတ်ပေးမှု: ${item.dispensedCount})",
          fontSize = 9.5.sp,
          color = SlateMedium
        )
      }
    }

    Column(horizontalAlignment = Alignment.End) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFFECACA))
      ) {
        Text(
          "သက်တမ်း: ${item.earliestExpiry}",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.SemiBold,
          color = StatusCritical,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
      Text(
        "Slow-Moving Stock",
        fontSize = 8.sp,
        color = Color(0xFF94A3B8)
      )
    }
  }
}

@Composable
fun FrequentCaseRow(item: ClinicalCaseItem) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(OceanGreenPrimary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          item.title,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          color = DarkGrayText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        "${item.count} Cases (${String.format(Locale.US, "%.0f", item.percentage)}%)",
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        color = Color(0xFF0F766E)
      )
    }

    Spacer(modifier = Modifier.height(3.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(2.5.dp))
        .background(Color(0xFFE2E8F0))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth((item.percentage / 100f).coerceIn(0.04f, 1f))
          .fillMaxHeight()
          .clip(RoundedCornerShape(2.5.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(Color(0xFF0284C7), OceanGreenPrimary)
            )
          )
      )
    }
  }
}
