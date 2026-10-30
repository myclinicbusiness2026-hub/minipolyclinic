package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddSupplierDialog
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInScreen(
  viewModel: ClinicViewModel,
  onStockInSuccess: () -> Unit = {}
) {
  val suppliers by viewModel.suppliers.collectAsState()
  val medicines by viewModel.medicines.collectAsState()
  val context = LocalContext.current

  // 1. Product Info State
  var tradeName by remember { mutableStateOf("") }
  var genericName by remember { mutableStateOf("") }
  var selectedMedicineType by remember { mutableStateOf("Oral") }
  val medicineTypeOptions = listOf(
    "Oral" to "Oral (သောက်ဆေး)",
    "Injection" to "Injection (ထိုးဆေး/သွင်းဆေး)",
    "Topical" to "Topical (လိမ်းဆေး/အပြင်သုံး)",
    "Consumables" to "Consumables (ဆေးခန်းသုံးပစ္စည်းများ)"
  )
  var selectedCategory by remember { mutableStateOf("Antibiotics") }
  var selectedUnitType by remember { mutableStateOf("Strip") }
  var minStockThresholdText by remember { mutableStateOf("20") }
  var isExistingMedicineSelected by remember { mutableStateOf(false) }

  // Custom Unit Type Dialog State
  var showAddCustomUnitDialog by remember { mutableStateOf(false) }
  var newCustomUnitInput by remember { mutableStateOf("") }
  var customUnitList by remember { mutableStateOf(listOf("Strip", "Bottle", "Box", "Piece", "Vial", "Ampoule", "Tube", "Respule", "Sachet", "Cartridge", "Set")) }

  // 2. Batch Details State
  var batchNumber by remember { mutableStateOf("BAT-2026-") }
  var manufacturer by remember { mutableStateOf("AA Pharmaceuticals") }
  var manufactureDate by remember { mutableStateOf("2025-06-01") }
  var expiryDate by remember { mutableStateOf("2027-12-31") }
  var quantityText by remember { mutableStateOf("100") }

  // 3. Bi-Directional Pricing Calculation Engine
  var costPriceText by remember { mutableStateOf("1000") }
  var marginPercentText by remember { mutableStateOf("25") }
  var sellingPriceText by remember { mutableStateOf("1250") }
  var isUpdatingProgrammatically by remember { mutableStateOf(false) }

  // 4. Supplier Selection
  var selectedSupplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "AA Medical Products Ltd.") }
  var showAddSupplierDialog by remember { mutableStateOf(false) }
  var supplierDropdownExpanded by remember { mutableStateOf(false) }
  var recordAsExpense by remember { mutableStateOf(true) }
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

  // Success Feedback Snackbar State
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val dynamicCategories by viewModel.medicineCategories.collectAsState()
  var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
  var newCategoryInputText by remember { mutableStateOf("") }

  val unitTypeOptions = listOf("Strip", "Bottle", "Box", "Piece", "Vial", "Ampoule", "Tube", "Respule")

  // Function to calculate selling price from cost & margin
  fun updateSellingFromMargin(cost: Double, margin: Double) {
    if (isUpdatingProgrammatically) return
    isUpdatingProgrammatically = true
    val calculatedSelling = cost + (cost * (margin / 100.0))
    sellingPriceText = if (calculatedSelling % 1.0 == 0.0) {
      calculatedSelling.toInt().toString()
    } else {
      String.format(Locale.US, "%.1f", calculatedSelling)
    }
    isUpdatingProgrammatically = false
  }

  // Function to calculate margin from cost & selling
  fun updateMarginFromSelling(cost: Double, selling: Double) {
    if (isUpdatingProgrammatically) return
    if (cost <= 0.0) return
    isUpdatingProgrammatically = true
    val calculatedMargin = ((selling - cost) / cost) * 100.0
    marginPercentText = if (calculatedMargin % 1.0 == 0.0) {
      calculatedMargin.toInt().toString()
    } else {
      String.format(Locale.US, "%.1f", calculatedMargin)
    }
    isUpdatingProgrammatically = false
  }

  // Date Pickers
  val calendar = Calendar.getInstance()
  val mfgDatePickerDialog = DatePickerDialog(
    context,
    { _, year, month, dayOfMonth ->
      manufactureDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
    },
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH),
    calendar.get(Calendar.DAY_OF_MONTH)
  )

  val expDatePickerDialog = DatePickerDialog(
    context,
    { _, year, month, dayOfMonth ->
      expiryDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
    },
    calendar.get(Calendar.YEAR) + 2,
    calendar.get(Calendar.MONTH),
    calendar.get(Calendar.DAY_OF_MONTH)
  )

  val currencyFormatter = NumberFormat.getNumberInstance(Locale.US)

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(10.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header Banner
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SoftGreenContainer),
        border = BorderStroke(1.dp, BorderSubtleRedBrown)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MedicalGreen),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Inventory2, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                "Pharmacy Stock-In Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkGrayText
              )
              Text(
                "ဆေးဝါးနှင့် ပစ္စည်းအသစ် တင်သွင်းခြင်း / FEFO စနစ်",
                fontSize = 10.5.sp,
                color = SlateMuted
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, BorderGray)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("FEFO Compliant", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
            }
          }
        }
      }

      // SECTION 1: Product Basic Information
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BorderSubtleRedBrown)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Medication, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("1. Product Basic Information", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkGrayText)
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Medicine Classification Type Selector (Oral / Injection / Topical / Consumables)
          Text("ဆေးဝါးအမျိုးအစား ခွဲခြားမှု (Classification):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            medicineTypeOptions.forEach { (typeKey, typeLabel) ->
              val isSel = selectedMedicineType == typeKey
              FilterChip(
                selected = isSel,
                onClick = {
                  selectedMedicineType = typeKey
                  when (typeKey) {
                    "Oral" -> if (selectedUnitType in listOf("Ampoule", "Vial", "Piece", "Set", "Tube")) selectedUnitType = "Strip"
                    "Injection" -> selectedUnitType = "Ampoule"
                    "Topical" -> selectedUnitType = "Tube"
                    "Consumables" -> selectedUnitType = "Piece"
                  }
                },
                label = { Text(typeLabel, fontSize = 10.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = when (typeKey) {
                    "Injection" -> Color(0xFF0F766E)
                    "Consumables" -> Color(0xFFD97706)
                    "Topical" -> Color(0xFF7C3AED)
                    else -> MedicalGreen
                  },
                  selectedLabelColor = PureWhite
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          var tradeNameDropdownExpanded by remember { mutableStateOf(false) }
          val matchingMedicines = remember(tradeName, medicines) {
            if (tradeName.isNotBlank()) {
              medicines.filter { it.name.contains(tradeName, ignoreCase = true) || it.genericName.contains(tradeName, ignoreCase = true) }.take(5)
            } else emptyList()
          }

          ExposedDropdownMenuBox(
            expanded = tradeNameDropdownExpanded && matchingMedicines.isNotEmpty(),
            onExpandedChange = { tradeNameDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedTextField(
              value = tradeName,
              onValueChange = {
                tradeName = it
                tradeNameDropdownExpanded = true
                isExistingMedicineSelected = false
              },
              label = { Text("Trade / Brand Name *", fontSize = 12.sp) },
              placeholder = { Text("e.g. Augmentin 625mg / Biogesic 500mg", fontSize = 12.sp) },
              leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp)) },
              trailingIcon = {
                if (matchingMedicines.isNotEmpty()) {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = tradeNameDropdownExpanded)
                }
              },
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )

            ExposedDropdownMenu(
              expanded = tradeNameDropdownExpanded && matchingMedicines.isNotEmpty(),
              onDismissRequest = { tradeNameDropdownExpanded = false }
            ) {
              matchingMedicines.forEach { existing ->
                DropdownMenuItem(
                  leadingIcon = {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(15.dp))
                  },
                  text = {
                    Column {
                      Text(
                        "${existing.name} (ရှိပြီးသားဆေးဝါး - Batch ထပ်ဖြည့်မည်)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = DarkGrayText
                      )
                      Text(
                        "${existing.genericName} • လက်ကျန်: ${existing.totalStock} ${existing.unitType} (အသုတ်: ${existing.batches.size})",
                        fontSize = 9.sp,
                        color = SlateMuted
                      )
                    }
                  },
                  onClick = {
                    tradeName = existing.name
                    genericName = existing.genericName
                    selectedMedicineType = existing.effectiveMedicineType
                    selectedCategory = existing.category
                    selectedUnitType = existing.unitType
                    minStockThresholdText = existing.minStockThreshold.toString()
                    costPriceText = existing.latestCostPrice.toInt().toString()
                    marginPercentText = existing.profitMarginPercent.toInt().toString()
                    updateSellingFromMargin(existing.latestCostPrice, existing.profitMarginPercent)
                    selectedSupplierName = existing.primarySupplier.ifBlank { selectedSupplierName }
                    batchNumber = "BAT-${SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())}-${existing.batches.size + 1}"
                    isExistingMedicineSelected = true
                    tradeNameDropdownExpanded = false
                  }
                )
              }
            }
          }

          if (isExistingMedicineSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(4.dp),
              color = SoftGreenContainer,
              border = BorderStroke(0.8.dp, MedicalGreen.copy(alpha = 0.5f))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "ရှိပြီးသားဆေးဝါးဖြစ်ပါသည်။ Batch အသစ် (သို့) Expire Date အသစ်ဖြင့် ထပ်မံဖြည့်သွင်းပါမည် (FEFO နီးရာမှ အရင်နှုတ်ပါမည်)။",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = MedicalGreenDark
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = genericName,
            onValueChange = { genericName = it },
            label = { Text("Generic Formula Name *", fontSize = 12.sp) },
            placeholder = { Text("e.g. Co-Amoxiclav / Paracetamol", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Biotech, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Category & Unit Type Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Category Selector
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = catExpanded,
              onExpandedChange = { catExpanded = it },
              modifier = Modifier.weight(1.2f)
            ) {
              OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
              ExposedDropdownMenu(
                expanded = catExpanded,
                onDismissRequest = { catExpanded = false }
              ) {
                dynamicCategories.forEach { cat ->
                  DropdownMenuItem(
                    text = { Text(cat, fontSize = 12.sp) },
                    onClick = {
                      selectedCategory = cat
                      catExpanded = false
                    }
                  )
                }
                HorizontalDivider()
                DropdownMenuItem(
                  leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp)) },
                  text = { Text("+ Add Custom Category...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark) },
                  onClick = {
                    catExpanded = false
                    newCategoryInputText = ""
                    showAddCustomCategoryDialog = true
                  }
                )
              }
            }

            // Unit Type Selector
            var unitExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = unitExpanded,
              onExpandedChange = { unitExpanded = it },
              modifier = Modifier.weight(1f)
            ) {
              OutlinedTextField(
                value = selectedUnitType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit Type", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
              ExposedDropdownMenu(
                expanded = unitExpanded,
                onDismissRequest = { unitExpanded = false }
              ) {
                customUnitList.forEach { unit ->
                  DropdownMenuItem(
                    text = { Text(unit, fontSize = 12.sp) },
                    onClick = {
                      selectedUnitType = unit
                      unitExpanded = false
                    }
                  )
                }
                HorizontalDivider()
                DropdownMenuItem(
                  leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp)) },
                  text = { Text("+ Custom Unit (အသစ်)...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark) },
                  onClick = {
                    unitExpanded = false
                    newCustomUnitInput = ""
                    showAddCustomUnitDialog = true
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = minStockThresholdText,
            onValueChange = { minStockThresholdText = it },
            label = { Text("Min Stock Alert Threshold", fontSize = 12.sp) },
            placeholder = { Text("20", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true
          )
        }
      }

      // SECTION 2: Batch Details & Expiry (FEFO Core)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BorderSubtleRedBrown)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("2. Batch & FEFO Expiry Tracking", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkGrayText)
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = batchNumber,
              onValueChange = { batchNumber = it },
              label = { Text("Batch Number *", fontSize = 12.sp) },
              placeholder = { Text("BAT-2026-08", fontSize = 12.sp) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )

            OutlinedTextField(
              value = quantityText,
              onValueChange = { quantityText = it },
              label = { Text("Stock-In Qty ($selectedUnitType) *", fontSize = 12.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = manufacturer,
            onValueChange = { manufacturer = it },
            label = { Text("Manufacturer / Supplier", fontSize = 12.sp) },
            placeholder = { Text("e.g. GlaxoSmithKline / Mega We Care", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Factory, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Manufacture Date Picker Trigger
            Box(
              modifier = Modifier
                .weight(1f)
                .clickable { mfgDatePickerDialog.show() }
            ) {
              OutlinedTextField(
                value = manufactureDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Mfg Date", fontSize = 12.sp) },
                trailingIcon = {
                  IconButton(onClick = { mfgDatePickerDialog.show() }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
            }

            // Expiry Date Picker Trigger
            Box(
              modifier = Modifier
                .weight(1f)
                .clickable { expDatePickerDialog.show() }
            ) {
              OutlinedTextField(
                value = expiryDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Expiry Date (FEFO) *", fontSize = 12.sp) },
                trailingIcon = {
                  IconButton(onClick = { expDatePickerDialog.show() }) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(16.dp))
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
            }
          }
        }
      }

      // SECTION 3: Dynamic Price Calculator Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BorderSubtleRedBrown)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Calculate, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("3. Dynamic Bi-Directional Pricing", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkGrayText)
            }

            Text(
              "Selling = Cost × (1 + Margin%)",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = MedicalGreenDark
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Cost Price
            OutlinedTextField(
              value = costPriceText,
              onValueChange = {
                costPriceText = it
                val cost = it.toDoubleOrNull() ?: 0.0
                val margin = marginPercentText.toDoubleOrNull() ?: 0.0
                updateSellingFromMargin(cost, margin)
              },
              label = { Text("Cost (Ks/unit) *", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )

            // Margin %
            OutlinedTextField(
              value = marginPercentText,
              onValueChange = {
                marginPercentText = it
                val cost = costPriceText.toDoubleOrNull() ?: 0.0
                val margin = it.toDoubleOrNull() ?: 0.0
                updateSellingFromMargin(cost, margin)
              },
              label = { Text("Margin % *", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Selling Price (Bi-directional)
          OutlinedTextField(
            value = sellingPriceText,
            onValueChange = {
              sellingPriceText = it
              val cost = costPriceText.toDoubleOrNull() ?: 0.0
              val selling = it.toDoubleOrNull() ?: 0.0
              updateMarginFromSelling(cost, selling)
            },
            label = { Text("Selling Price per Unit (Ks) *", fontSize = 11.5.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Financial Summary Strip
          val costVal = costPriceText.toDoubleOrNull() ?: 0.0
          val qtyVal = quantityText.toIntOrNull() ?: 0
          val sellVal = sellingPriceText.toDoubleOrNull() ?: 0.0
          val totalCost = costVal * qtyVal
          val projectedRevenue = sellVal * qtyVal
          val projectedProfit = projectedRevenue - totalCost

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(BgLightGray)
              .border(BorderStroke(1.dp, BorderGray), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Total Investment", fontSize = 9.5.sp, color = SlateMuted)
              Text("${currencyFormatter.format(totalCost.toInt())} Ks", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Projected Revenue", fontSize = 9.5.sp, color = SlateMuted)
              Text("${currencyFormatter.format(projectedRevenue.toInt())} Ks", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text("Estimated Profit", fontSize = 9.5.sp, color = SlateMuted)
              Text("+${currencyFormatter.format(projectedProfit.toInt())} Ks", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreen)
            }
          }
        }
      }

      // SECTION 4: Supplier Selector & Management
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, BorderSubtleRedBrown)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("4. Pharmaceutical Supplier", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkGrayText)
            }

            TextButton(
              onClick = { showAddSupplierDialog = true },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp), tint = MedicalGreen)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add New Supplier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreen)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          ExposedDropdownMenuBox(
            expanded = supplierDropdownExpanded,
            onExpandedChange = { supplierDropdownExpanded = it }
          ) {
            OutlinedTextField(
              value = selectedSupplierName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Select Registered Supplier *", fontSize = 12.sp) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )

            ExposedDropdownMenu(
              expanded = supplierDropdownExpanded,
              onDismissRequest = { supplierDropdownExpanded = false }
            ) {
              suppliers.forEach { sup ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
                      Text("${sup.phone} • ${sup.contactPerson}", fontSize = 10.sp, color = SlateMuted)
                    }
                  },
                  onClick = {
                    selectedSupplierName = sup.name
                    supplierDropdownExpanded = false
                  }
                )
              }
            }
          }

          val currentSupplier = suppliers.find { it.name == selectedSupplierName }
          if (currentSupplier != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              color = BgLightGray,
              border = BorderStroke(1.dp, BorderGray)
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(currentSupplier.phone, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = DarkGrayText)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(currentSupplier.address, fontSize = 10.5.sp, color = SlateMuted, maxLines = 1)
              }
            }
          }
        }
      }

      // 5. Clinic Expense Link Option
      val currentTotalCost = (costPriceText.toDoubleOrNull() ?: 0.0) * (quantityText.toIntOrNull() ?: 0)
      Surface(
        color = if (recordAsExpense) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (recordAsExpense) MedicalGreen.copy(alpha = 0.5f) else BorderGray),
        modifier = Modifier.fillMaxWidth().clickable { recordAsExpense = !recordAsExpense }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(
            checked = recordAsExpense,
            onCheckedChange = { recordAsExpense = it },
            colors = CheckboxDefaults.colors(checkedColor = MedicalGreen)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              "ဆေးဝါးဝယ်ယူစရိတ်အဖြစ် Clinic Expense တွင် တပါတည်း မှတ်တမ်းတင်မည်",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = DarkGrayText
            )
            Text(
              "စုစုပေါင်း ကုန်ကျငွေ: ${currencyFmt.format(currentTotalCost)} Ks • ကုမ္ပဏီ: $selectedSupplierName",
              fontSize = 9.5.sp,
              color = SlateMedium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Action Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            tradeName = ""
            genericName = ""
            batchNumber = "BAT-2026-"
            quantityText = "100"
          },
          modifier = Modifier
            .weight(1f)
            .height(38.dp),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, BorderGray),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = SlateMedium)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reset Form", color = SlateMedium, fontSize = 11.5.sp)
        }

        Button(
          onClick = {
            val cost = costPriceText.toDoubleOrNull() ?: 0.0
            val margin = marginPercentText.toDoubleOrNull() ?: 25.0
            val qty = quantityText.toIntOrNull() ?: 50
            val threshold = minStockThresholdText.toIntOrNull() ?: 20

            if (tradeName.isNotBlank() && batchNumber.isNotBlank() && cost > 0) {
              viewModel.stockInMedicine(
                tradeName = tradeName.trim(),
                genericName = genericName.trim().ifBlank { tradeName.trim() },
                dosageForm = selectedUnitType,
                category = selectedCategory,
                unitType = selectedUnitType,
                batchNumber = batchNumber.trim(),
                manufacturer = manufacturer.trim(),
                manufactureDate = manufactureDate,
                expiryDate = expiryDate,
                quantity = qty,
                costPrice = cost,
                profitMarginPercent = margin,
                minThreshold = threshold,
                supplierName = selectedSupplierName,
                medicineType = selectedMedicineType
              )

              if (recordAsExpense && (cost * qty) > 0) {
                viewModel.addExpense(
                  title = "ဆေးဝါးဝယ်ယူခြင်း - ${tradeName.trim()} ($qty $selectedUnitType)",
                  category = "Pharmacy Stock",
                  amount = cost * qty,
                  payee = selectedSupplierName,
                  notes = "Batch: ${batchNumber.trim()} • Exp: $expiryDate"
                )
              }

              // Clear or reset fields
              tradeName = ""
              genericName = ""
              batchNumber = "BAT-2026-"
              quantityText = "100"

              onStockInSuccess()
            }
          },
          enabled = tradeName.isNotBlank() && batchNumber.isNotBlank() && (costPriceText.toDoubleOrNull() ?: 0.0) > 0,
          modifier = Modifier
            .weight(1.6f)
            .height(38.dp),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
        ) {
          Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Confirm Stock-In (FEFO)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }

  // Add Supplier Dialog
  if (showAddSupplierDialog) {
    AddSupplierDialog(
      onDismiss = { showAddSupplierDialog = false },
      onAdd = { name, contact, phone, email, addr ->
        viewModel.addSupplier(name, contact, phone, email, addr)
        selectedSupplierName = name
      }
    )
  }

  // Add Custom Category Dialog
  if (showAddCustomCategoryDialog) {
    AlertDialog(
      onDismissRequest = { showAddCustomCategoryDialog = false },
      icon = { Icon(Icons.Default.Category, contentDescription = null, tint = MedicalGreenDark) },
      title = { Text("Add Custom Category", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("ဆေးဝါးအမျိုးအစား အသစ် သတ်မှတ်ထည့်သွင်းပါ", fontSize = 11.5.sp, color = SlateMedium)
          OutlinedTextField(
            value = newCategoryInputText,
            onValueChange = { newCategoryInputText = it },
            placeholder = { Text("e.g. Eye Drops / Dermatological", fontSize = 11.5.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newCategoryInputText.trim()
            if (clean.isNotBlank()) {
              viewModel.addMedicineCategory(clean)
              selectedCategory = clean
              showAddCustomCategoryDialog = false
            }
          },
          enabled = newCategoryInputText.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Add Category", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showAddCustomCategoryDialog = false },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Cancel", fontSize = 11.5.sp)
        }
      }
    )
  }

  // Add Custom Unit Dialog
  if (showAddCustomUnitDialog) {
    AlertDialog(
      onDismissRequest = { showAddCustomUnitDialog = false },
      icon = { Icon(Icons.Default.Scale, contentDescription = null, tint = MedicalGreenDark) },
      title = { Text("Add Custom Unit Type", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("ဆေးဝါးထည့်သွင်းမည့် ယူနစ် (Unit) အသစ် ရိုက်ထည့်ပါ", fontSize = 11.5.sp, color = SlateMedium)
          OutlinedTextField(
            value = newCustomUnitInput,
            onValueChange = { newCustomUnitInput = it },
            placeholder = { Text("e.g. Sachet / Cartridge / Roll / Inhaler", fontSize = 11.5.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newCustomUnitInput.trim()
            if (clean.isNotBlank()) {
              if (!customUnitList.contains(clean)) {
                customUnitList = customUnitList + clean
              }
              selectedUnitType = clean
              showAddCustomUnitDialog = false
            }
          },
          enabled = newCustomUnitInput.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Add Unit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showAddCustomUnitDialog = false },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Cancel", fontSize = 11.5.sp)
        }
      }
    )
  }
}
