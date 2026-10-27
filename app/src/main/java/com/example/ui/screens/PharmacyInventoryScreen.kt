package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Medicine
import com.example.model.QueueStatus
import com.example.model.StockBatch
import com.example.model.Supplier
import com.example.ui.components.AddSupplierDialog
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class PharmacyViewTab(val label: String) {
  INVENTORY("Inventory"),
  STOCK_IN("Stock-In"),
  SUPPLIERS("Suppliers")
}

enum class InventoryFilter(val label: String) {
  ALL("All Items"),
  LOW_STOCK("Low Stock Alert"),
  EXPIRING_SOON("Expiring Soon (<6 Mo)"),
  CRITICAL_EXPIRY("Critical (<3 Mo)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyInventoryScreen(
  viewModel: ClinicViewModel
) {
  val medicines by viewModel.medicines.collectAsState()
  val medicineCategories by viewModel.medicineCategories.collectAsState()
  val suppliers by viewModel.suppliers.collectAsState()
  val patients by viewModel.patients.collectAsState()
  val consultations by viewModel.consultations.collectAsState()

  var currentTab by remember { mutableStateOf(PharmacyViewTab.INVENTORY) }
  var searchQuery by remember { mutableStateOf("") }
  var activeFilter by remember { mutableStateOf(InventoryFilter.ALL) }
  var selectedCategoryFilter by remember { mutableStateOf<String?>("All") }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }
  var showAddSupplierDialog by remember { mutableStateOf(false) }
  var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
  var deletingSupplier by remember { mutableStateOf<Supplier?>(null) }

  // Patients waiting at pharmacy for dispensing
  val pharmacyQueuePatients = patients.filter { it.status == QueueStatus.AT_PHARMACY }

  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  // Date parsing for expiry badges
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  val now = Date()

  fun getDaysUntilExpiry(expiryDateStr: String): Long {
    return try {
      val expDate = sdf.parse(expiryDateStr) ?: return 999
      val diff = expDate.time - now.time
      diff / (1000 * 60 * 60 * 24)
    } catch (e: Exception) {
      999
    }
  }

  // Filtered Medicines List
  val filteredMedicines = medicines.filter { med ->
    val matchesSearch = searchQuery.isBlank() ||
        med.name.contains(searchQuery, ignoreCase = true) ||
        med.genericName.contains(searchQuery, ignoreCase = true) ||
        med.category.contains(searchQuery, ignoreCase = true) ||
        med.batches.any { it.batchNumber.contains(searchQuery, ignoreCase = true) }

    val matchesCategory = selectedCategoryFilter == null ||
        selectedCategoryFilter == "All" ||
        med.category.equals(selectedCategoryFilter, ignoreCase = true)

    val earliestExpDays = med.sortedBatches.firstOrNull()?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999

    val matchesFilter = when (activeFilter) {
      InventoryFilter.ALL -> true
      InventoryFilter.LOW_STOCK -> med.totalStock <= med.minStockThreshold
      InventoryFilter.EXPIRING_SOON -> earliestExpDays <= 180 // < 6 months
      InventoryFilter.CRITICAL_EXPIRY -> earliestExpDays <= 90 // < 3 months
    }

    matchesSearch && matchesCategory && matchesFilter
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundSurface)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    // Top Bar: Ultra-Compact Header & Tab Switcher
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(6.dp),
      color = PureWhite,
      border = BorderStroke(1.dp, BorderGrey)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Pharmacy",
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = DarkGrayText
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftGreenContainer
          ) {
            Text(
              "${medicines.size} Items",
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              color = MedicalGreenDark,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
          }
        }

        // Sub-Navigation Tabs & Quick Stock-In Button
        Row(
          horizontalArrangement = Arrangement.spacedBy(3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PharmacyViewTab.entries.forEach { tab ->
            val isSel = currentTab == tab
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (isSel) MedicalGreen else Color(0xFFF4F6F8),
              border = if (isSel) null else BorderStroke(1.dp, BorderGrey),
              modifier = Modifier.clickable { currentTab = tab }
            ) {
              Text(
                tab.label,
                fontSize = 10.5.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                color = if (isSel) PureWhite else DarkGrayText,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }

          if (currentTab == PharmacyViewTab.INVENTORY) {
            Spacer(modifier = Modifier.width(2.dp))
            Button(
              onClick = { currentTab = PharmacyViewTab.STOCK_IN },
              colors = ButtonDefaults.buttonColors(containerColor = OceanGreenPrimary),
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Stock", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    when (currentTab) {
      PharmacyViewTab.INVENTORY -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Dispensing Queue Section from Doctor Consultations
          if (pharmacyQueuePatients.isNotEmpty()) {
              item {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(containerColor = SoftGreenContainer),
                  border = BorderStroke(1.dp, BorderSubtleRedBrown)
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          "Prescription Dispense Queue (${pharmacyQueuePatients.size})",
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = MedicalGreenDark
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    pharmacyQueuePatients.forEach { patient ->
                      val consultation = consultations.find { it.patientId == patient.id }
                      Card(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, BorderGray)
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              patient.name,
                              fontWeight = FontWeight.Bold,
                              fontSize = 13.sp,
                              color = SlateDark,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                            Text(
                              "${patient.hn} • ${consultation?.prescriptions?.size ?: 0} Rx items",
                              fontSize = 11.sp,
                              color = SlateMuted,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                          }

                          Spacer(modifier = Modifier.width(8.dp))

                          Button(
                            onClick = {
                              val items = consultation?.prescriptions ?: emptyList()
                              viewModel.dispensePrescription(patient.id, items)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                          ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Dispense", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // Search Bar with Grey Background & Horizontally Scrollable Filter Chips
            item {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  placeholder = { Text("Search medicine...", fontSize = 11.5.sp, color = SlateMuted) },
                  leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(17.dp)) },
                  trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                      IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMuted, modifier = Modifier.size(15.dp))
                      }
                    }
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  singleLine = true,
                  colors = clinicSearchBarColors(),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
                )

                // Horizontally Scrollable Filter Chips (Status & Expiry)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  InventoryFilter.entries.forEach { filter ->
                    FilterChip(
                      selected = activeFilter == filter,
                      onClick = { activeFilter = filter },
                      label = { Text(filter.label, fontSize = 11.sp, fontWeight = if (activeFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                      shape = RoundedCornerShape(6.dp),
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoftGreenContainer,
                        selectedLabelColor = MedicalGreenDark,
                        containerColor = PureWhite,
                        labelColor = SlateMedium
                      ),
                      border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeFilter == filter,
                        borderColor = BorderGray,
                        selectedBorderColor = MedicalGreen,
                        borderWidth = 1.dp
                      )
                    )
                  }
                }

                // Category Filter Row & + Add Category Button
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // "All Categories" chip
                  FilterChip(
                    selected = selectedCategoryFilter == "All" || selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = "All" },
                    label = { Text("All Categories", fontSize = 10.5.sp, fontWeight = if (selectedCategoryFilter == "All") FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(5.dp),
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFE0F2FE),
                      selectedLabelColor = Color(0xFF0369A1),
                      containerColor = PureWhite,
                      labelColor = SlateMedium
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                      enabled = true,
                      selected = selectedCategoryFilter == "All",
                      borderColor = BorderGray,
                      selectedBorderColor = Color(0xFF0284C7),
                      borderWidth = 1.dp
                    )
                  )

                  medicineCategories.forEach { cat ->
                    val isCatSelected = selectedCategoryFilter.equals(cat, ignoreCase = true)
                    FilterChip(
                      selected = isCatSelected,
                      onClick = { selectedCategoryFilter = cat },
                      label = { Text(cat, fontSize = 10.5.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal) },
                      shape = RoundedCornerShape(5.dp),
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE0F2FE),
                        selectedLabelColor = Color(0xFF0369A1),
                        containerColor = PureWhite,
                        labelColor = SlateMedium
                      ),
                      border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isCatSelected,
                        borderColor = BorderGray,
                        selectedBorderColor = Color(0xFF0284C7),
                        borderWidth = 1.dp
                      )
                    )
                  }

                  // "+ Category" Action Chip
                  SuggestionChip(
                    onClick = { showAddCategoryDialog = true },
                    label = { Text("+ Category", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp)) },
                    shape = RoundedCornerShape(5.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SoftGreenContainer),
                    border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.5f))
                  )
                }
              }
            }

            // Empty State
            if (filteredMedicines.isEmpty()) {
              item {
                Card(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = BackgroundSurface)
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = SlateLight, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No medicines match your filter", fontWeight = FontWeight.Bold, color = SlateDark)
                    Text("Try resetting filters or perform a new Stock-In entry.", fontSize = 12.sp, color = SlateMuted)
                  }
                }
              }
            }

            // FEFO Medicine Inventory Cards
            items(filteredMedicines, key = { it.id }) { med ->
              EnhancedMedicineItemCard(
                medicine = med,
                currencyFmt = currencyFmt,
                suppliers = suppliers,
                getDaysUntilExpiry = ::getDaysUntilExpiry,
                onDispenseQuick = { qty -> viewModel.dispenseQuick(med.id, qty) },
                onDeleteMedicine = { medicineToDelete = med }
              )
            }
          }
        }

        PharmacyViewTab.STOCK_IN -> {
          StockInScreen(
            viewModel = viewModel,
            onStockInSuccess = {
              currentTab = PharmacyViewTab.INVENTORY
            }
          )
        }

        PharmacyViewTab.SUPPLIERS -> {
          SupplierManagementView(
            suppliers = suppliers,
            onAddSupplierClick = { showAddSupplierDialog = true },
            onEditSupplier = { editingSupplier = it },
            onDeleteSupplier = { deletingSupplier = it }
          )
        }
      }
    }

    if (showAddSupplierDialog) {
      AddSupplierDialog(
        onDismiss = { showAddSupplierDialog = false },
        onAdd = { name, contact, phone, email, addr ->
          viewModel.addSupplier(name, contact, phone, email, addr)
        }
      )
    }

    editingSupplier?.let { sup ->
      EditSupplierDialog(
        supplier = sup,
        onDismiss = { editingSupplier = null },
        onSave = { updated ->
          viewModel.updateSupplier(updated)
          editingSupplier = null
        }
      )
    }

    deletingSupplier?.let { sup ->
      AlertDialog(
        onDismissRequest = { deletingSupplier = null },
        icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp)) },
        title = { Text("Delete Supplier?", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        text = { Text("Are you sure you want to remove '${sup.name}'? This action cannot be undone.", fontSize = 11.5.sp) },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteSupplier(sup.id)
              deletingSupplier = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Confirm Delete", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { deletingSupplier = null }, shape = RoundedCornerShape(6.dp)) {
            Text("Cancel", fontSize = 11.sp)
          }
        }
      )
    }

    // Add New Medicine Category Dialog
    if (showAddCategoryDialog) {
      var newCategoryName by remember { mutableStateOf("") }
      AlertDialog(
        onDismissRequest = { showAddCategoryDialog = false },
        icon = { Icon(Icons.Default.Category, contentDescription = null, tint = MedicalGreenDark) },
        title = {
          Text("Add Medicine Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ဆေးဝါးအမျိုးအစား အသစ် သတ်မှတ်ထည့်သွင်းပါ", fontSize = 11.5.sp, color = SlateMedium)
            OutlinedTextField(
              value = newCategoryName,
              onValueChange = { newCategoryName = it },
              placeholder = { Text("e.g. Eye Drops / Dermatological / Antiviral", fontSize = 11.5.sp) },
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
              if (newCategoryName.isNotBlank()) {
                viewModel.addMedicineCategory(newCategoryName)
                selectedCategoryFilter = newCategoryName.trim()
                showAddCategoryDialog = false
              }
            },
            enabled = newCategoryName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Add Category", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = { showAddCategoryDialog = false },
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Cancel", fontSize = 11.5.sp)
          }
        }
      )
    }

    // Delete Medicine Confirmation Dialog
    medicineToDelete?.let { med ->
      AlertDialog(
        onDismissRequest = { medicineToDelete = null },
        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(26.dp)) },
        title = {
          Text("Delete Medicine Record?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              "${med.name} (${med.genericName}) အား ဆေးဝါးစာရင်းမှ ဖျက်သိမ်းပါမည်လား။",
              fontSize = 12.sp,
              color = DarkGrayText,
              fontWeight = FontWeight.Medium
            )
            Text(
              "လက်ကျန်စတော့: ${med.totalStock} ${med.unitType} (အသုတ်ပေါင်း ${med.batches.size} ခု ပါဝင်ပါသည်)",
              fontSize = 11.sp,
              color = Color(0xFFDC2626)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "သတိပေးချက်: အတည်ပြုဖျက်သိမ်းပါက ဤဆေးဝါးနှင့် ပတ်သက်သော စတော့အသုတ်များအားလုံး စနစ်ထဲမှ ပယ်ဖျက်သွားမည် ဖြစ်ပါသည်။",
              fontSize = 10.5.sp,
              color = SlateMedium
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteMedicine(med.id)
              medicineToDelete = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Delete Medicine", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = { medicineToDelete = null },
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Cancel", fontSize = 11.5.sp)
          }
        }
      )
    }
}
fun getMedicineCategoryColor(category: String): Color {
  return when {
    category.contains("Antibiotic", true) || category.contains("Anti-infective", true) -> Color(0xFF1D4ED8) // Deep Blue
    category.contains("Analgesic", true) || category.contains("Pain", true) || category.contains("Antipyretic", true) -> Color(0xFF047857) // Deep Emerald
    category.contains("Antacid", true) || category.contains("Gastro", true) || category.contains("PPI", true) -> Color(0xFFB45309) // Amber/Brown
    category.contains("Cardio", true) || category.contains("Hypertens", true) -> Color(0xFFBE123C) // Crimson Rose
    category.contains("Diabet", true) || category.contains("Endocrin", true) -> Color(0xFF6D28D9) // Deep Purple
    category.contains("Respirat", true) || category.contains("Cough", true) || category.contains("Cold", true) -> Color(0xFF0F766E) // Dark Teal
    category.contains("Allergy", true) || category.contains("Antihistamin", true) -> Color(0xFFC2410C) // Rust Orange
    category.contains("Vitamin", true) || category.contains("Mineral", true) || category.contains("Supplement", true) -> Color(0xFF15803D) // Green
    else -> Color(0xFF475569) // Slate
  }
}

@Composable
fun EnhancedMedicineItemCard(
  medicine: Medicine,
  currencyFmt: NumberFormat,
  suppliers: List<Supplier>,
  getDaysUntilExpiry: (String) -> Long,
  onDispenseQuick: (Int) -> Unit = {},
  onDeleteMedicine: (() -> Unit)? = null
) {
  var isExpanded by remember { mutableStateOf(false) }
  val isLowStock = medicine.totalStock <= medicine.minStockThreshold
  val earliestBatch = medicine.sortedBatches.firstOrNull()
  val earliestDays = earliestBatch?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999

  // Expiry Warning Status: Red (< 90 days), Orange (< 180 days), Green (> 180 days)
  val isCriticalExpiry = earliestDays <= 90
  val isWarningExpiry = earliestDays in 91..180

  val supplierInfo = suppliers.find { it.name.equals(medicine.primarySupplier, ignoreCase = true) }
  val categoryColor = remember(medicine.category) { getMedicineCategoryColor(medicine.category) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
    ) {
      // Left Category Accent Color Stripe (Distinct visual demarcation)
      Box(
        modifier = Modifier
          .width(5.dp)
          .fillMaxHeight()
          .background(categoryColor)
      )

      Column(modifier = Modifier.weight(1f).padding(horizontal = 9.dp, vertical = 7.dp)) {
        // Row 1: Name, Category Pill Badge, Generic / Dosage, Stock Badge
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                medicine.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DarkGrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (medicine.isAllergyWarning) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  shape = RoundedCornerShape(3.dp),
                  color = StatusCriticalContainer
                ) {
                  Text(
                    "Allergy",
                    color = StatusCritical,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(3.dp),
                color = categoryColor.copy(alpha = 0.12f)
              ) {
                Text(
                  medicine.category.ifBlank { "General" },
                  color = categoryColor,
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
              Text(
                "${medicine.genericName} • ${medicine.dosageForm}",
                fontSize = 10.sp,
                color = SlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Stock and Expiry Badges
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
          if (earliestBatch != null && (isCriticalExpiry || isWarningExpiry)) {
            Surface(
              shape = RoundedCornerShape(3.dp),
              color = if (isCriticalExpiry) StatusCriticalContainer else StatusWarningContainer
            ) {
              Text(
                "Exp: ${earliestBatch.expiryDate}",
                color = if (isCriticalExpiry) StatusCritical else StatusWarning,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(3.dp),
            color = if (isLowStock) StatusCriticalContainer else SoftGreenContainer
          ) {
            Text(
              if (isLowStock) "Low: ${medicine.totalStock}" else "${medicine.totalStock} ${medicine.unitType}",
              color = if (isLowStock) StatusCritical else MedicalGreenDark,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Row 2: Compact Inline Pricing & Earliest Batch Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(BgLightGray)
          .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            "Cost: ${currencyFmt.format(medicine.latestCostPrice.toInt())}",
            fontSize = 9.5.sp,
            color = SlateDark
          )
          Text(
            "+${medicine.profitMarginPercent.toInt()}%",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreen
          )
          Text(
            "Sell: ${currencyFmt.format(medicine.calculatedSellingPrice.toInt())} Ks",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = MedicalGreenDark
          )
        }

        if (earliestBatch != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isExpanded = !isExpanded }
          ) {
            Text(
              "Batch #${earliestBatch.batchNumber}",
              fontSize = 9.sp,
              color = SlateMedium
            )
            if (medicine.sortedBatches.size > 1) {
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                if (isExpanded) "▲" else "+${medicine.sortedBatches.size - 1} ▼",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreen
              )
            }
          }
        }
      }

      // If expanded: show additional FEFO batches
      if (isExpanded && medicine.sortedBatches.size > 1) {
        Spacer(modifier = Modifier.height(3.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          medicine.sortedBatches.drop(1).forEach { batch ->
            val bDays = getDaysUntilExpiry(batch.expiryDate)
            val isBCrit = bDays <= 90
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(PureWhite)
                .border(0.5.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Batch #${batch.batchNumber} • Exp: ${batch.expiryDate}", fontSize = 9.sp, color = if (isBCrit) StatusCritical else DarkGrayText)
              Text("Qty: ${batch.quantity} ${medicine.unitType}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(3.dp))

      // Row 3: Supplier Info & Quick Dispense Action (Compact height 24.dp)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "Supplier: ${medicine.primarySupplier}${if (supplierInfo != null) " • ${supplierInfo.phone}" else ""}",
          fontSize = 9.sp,
          color = SlateMuted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        OutlinedButton(
          onClick = { onDispenseQuick(1) },
          enabled = medicine.totalStock > 0,
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.8.dp, MedicalGreen),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
          modifier = Modifier.height(24.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalGreen)
        ) {
          Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(11.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("Dispense 1", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }

        if (onDeleteMedicine != null) {
          Spacer(modifier = Modifier.width(3.dp))
          IconButton(
            onClick = onDeleteMedicine,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              Icons.Default.DeleteOutline,
              contentDescription = "Delete Medicine",
              tint = Color(0xFFDC2626),
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }
    }
  }
}
}

@Composable
fun SupplierManagementView(
  suppliers: List<Supplier>,
  onAddSupplierClick: () -> Unit,
  onEditSupplier: (Supplier) -> Unit,
  onDeleteSupplier: (Supplier) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Pharmaceutical Suppliers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkGrayText)
        Text("Registered medicine distributors & vendors", fontSize = 11.sp, color = SlateMuted)
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onAddSupplierClick,
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
      ) {
        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Supplier", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(suppliers, key = { it.id }) { sup ->
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
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = DarkGrayText)
                if (sup.contactPerson.isNotBlank()) {
                  Text("Contact: ${sup.contactPerson}", fontSize = 11.sp, color = SlateMuted)
                }
              }

              Surface(
                shape = RoundedCornerShape(4.dp),
                color = SoftGreenContainer
              ) {
                Text(
                  "Active Vendor",
                  color = MedicalGreenDark,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(sup.phone, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              if (sup.email.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Email, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(sup.email, fontSize = 11.sp, color = SlateMuted)
              }
            }

            if (sup.address.isNotBlank()) {
              Spacer(modifier = Modifier.height(3.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(sup.address, fontSize = 10.5.sp, color = SlateMuted)
              }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { onEditSupplier(sup) },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalGreenDark),
                border = BorderStroke(1.dp, MedicalGreenDark.copy(alpha = 0.5f))
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Edit", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.width(6.dp))
              OutlinedButton(
                onClick = { onDeleteSupplier(sup) },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f))
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Delete", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun EditSupplierDialog(
  supplier: Supplier,
  onDismiss: () -> Unit,
  onSave: (Supplier) -> Unit
) {
  var name by remember { mutableStateOf(supplier.name) }
  var contact by remember { mutableStateOf(supplier.contactPerson) }
  var phone by remember { mutableStateOf(supplier.phone) }
  var email by remember { mutableStateOf(supplier.email) }
  var address by remember { mutableStateOf(supplier.address) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Supplier (ဆေးကုမ္ပဏီ ပြင်ဆင်ရန်)", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Company / Vendor Name", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )
        OutlinedTextField(
          value = contact,
          onValueChange = { contact = it },
          label = { Text("Contact Person", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Office Address", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          minLines = 2,
          maxLines = 3
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(supplier.copy(
              name = name.trim(),
              contactPerson = contact.trim(),
              phone = phone.trim(),
              email = email.trim(),
              address = address.trim()
            ))
          }
        },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("Save Changes", fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
        Text("Cancel", fontSize = 11.sp)
      }
    }
  )
}
