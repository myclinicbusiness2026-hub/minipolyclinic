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
import androidx.compose.foundation.verticalScroll
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
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext

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
  viewModel: ClinicViewModel,
  isReadOnly: Boolean = false
) {
  val medicines by viewModel.medicines.collectAsState()
  val medicineCategories by viewModel.medicineCategories.collectAsState()
  val suppliers by viewModel.suppliers.collectAsState()
  val patients by viewModel.patients.collectAsState()
  val consultations by viewModel.consultations.collectAsState()

  var currentTab by remember { mutableStateOf(PharmacyViewTab.INVENTORY) }
  var searchQuery by remember { mutableStateOf("") }
  var activeFilter by remember { mutableStateOf(InventoryFilter.ALL) }
  var selectedTypeFilter by remember { mutableStateOf("All") }
  val typeFilterOptions = listOf(
    "All" to "All Types",
    "Oral" to "Oral (သောက်ဆေး)",
    "Injection" to "Injection (ထိုးဆေး/သွင်းဆေး)",
    "Topical" to "Topical (လိမ်းဆေး)",
    "Consumables" to "Consumables (ဆေးခန်းသုံး)"
  )
  var selectedCategoryFilter by remember { mutableStateOf<String?>("All") }
  val medicineUnits by viewModel.medicineUnits.collectAsState()
  var selectedMedicineForDetails by remember { mutableStateOf<Medicine?>(null) }
  var categoryToManage by remember { mutableStateOf<String?>(null) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }
  var medicineToEdit by remember { mutableStateOf<Medicine?>(null) }
  var medicineForWastage by remember { mutableStateOf<Medicine?>(null) }
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

  // Summary Counts for Status & Expiry Filter Badges
  val filterCounts = remember(medicines) {
    var lowStock = 0
    var exp6m = 0
    var exp3m = 0
    val nowMs = System.currentTimeMillis()
    val sdfLocal = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    medicines.forEach { med ->
      if (med.isLowStock) {
        lowStock++
      }
      val earliestBatch = med.sortedBatches.firstOrNull()
      if (earliestBatch != null) {
        try {
          val expDate = sdfLocal.parse(earliestBatch.expiryDate)
          if (expDate != null) {
            val days = (expDate.time - nowMs) / (1000 * 60 * 60 * 24)
            if (days <= 90) {
              exp3m++
              exp6m++
            } else if (days <= 180) {
              exp6m++
            }
          }
        } catch (_: Exception) {}
      }
    }
    Triple(lowStock, exp6m, exp3m)
  }

  // Filtered Medicines List (Memoized to prevent jank & unnecessary recompositions)
  val filteredMedicines = remember(medicines, searchQuery, selectedCategoryFilter, selectedTypeFilter, activeFilter) {
    val queryTrimmed = searchQuery.trim()
    medicines.filter { med ->
      val matchesSearch = queryTrimmed.isBlank() ||
          med.name.contains(queryTrimmed, ignoreCase = true) ||
          med.genericName.contains(queryTrimmed, ignoreCase = true) ||
          med.category.contains(queryTrimmed, ignoreCase = true) ||
          med.batches.any { it.batchNumber.contains(queryTrimmed, ignoreCase = true) }

      val matchesCategory = selectedCategoryFilter == null ||
          selectedCategoryFilter == "All" ||
          med.category.equals(selectedCategoryFilter, ignoreCase = true)

      val matchesType = selectedTypeFilter == "All" ||
          med.effectiveMedicineType.equals(selectedTypeFilter, ignoreCase = true)

      val earliestExpDays = med.sortedBatches.firstOrNull()?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999

      val matchesFilter = when (activeFilter) {
        InventoryFilter.ALL -> true
        InventoryFilter.LOW_STOCK -> med.isLowStock
        InventoryFilter.EXPIRING_SOON -> earliestExpDays <= 180 // < 6 months
        InventoryFilter.CRITICAL_EXPIRY -> earliestExpDays <= 90 // < 3 months
      }

      matchesSearch && matchesCategory && matchesType && matchesFilter
    }
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

          if (currentTab == PharmacyViewTab.INVENTORY && !isReadOnly) {
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
                    val filterLabel = when (filter) {
                      InventoryFilter.ALL -> "All (${medicines.size})"
                      InventoryFilter.LOW_STOCK -> "Low Stock Alert (${filterCounts.first})"
                      InventoryFilter.EXPIRING_SOON -> "Expiring Soon (<6 Mo) (${filterCounts.second})"
                      InventoryFilter.CRITICAL_EXPIRY -> "Critical (<3 Mo) (${filterCounts.third})"
                    }
                    FilterChip(
                      selected = activeFilter == filter,
                      onClick = { activeFilter = filter },
                      label = { Text(filterLabel, fontSize = 11.sp, fontWeight = if (activeFilter == filter) FontWeight.Bold else FontWeight.Normal) },
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

                // Primary 5-Tab Medicine Classification Filter Bar
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  typeFilterOptions.forEach { (typeKey, typeLabel) ->
                    val isTypeSelected = selectedTypeFilter == typeKey
                    FilterChip(
                      selected = isTypeSelected,
                      onClick = { selectedTypeFilter = typeKey },
                      label = { Text(typeLabel, fontSize = 10.5.sp, fontWeight = if (isTypeSelected) FontWeight.Bold else FontWeight.Normal) },
                      shape = RoundedCornerShape(5.dp),
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (typeKey) {
                          "Injection" -> Color(0xFF0F766E)
                          "Consumables" -> Color(0xFFD97706)
                          "Topical" -> Color(0xFF7C3AED)
                          "Oral" -> Color(0xFF0284C7)
                          else -> MedicalGreen
                        },
                        selectedLabelColor = PureWhite,
                        containerColor = PureWhite,
                        labelColor = SlateMedium
                      )
                    )
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

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

                  val defaultMedCats = remember { listOf("All", "Antibiotics", "Analgesic / Antipyretic", "Antacid / PPI", "Cardiovascular", "Antidiabetic", "Respiratory", "Consumables", "Others") }
                  medicineCategories.forEach { cat ->
                    val isCatSelected = selectedCategoryFilter.equals(cat, ignoreCase = true)
                    val isCustom = !defaultMedCats.any { it.equals(cat, ignoreCase = true) }
                    FilterChip(
                      selected = isCatSelected,
                      onClick = { selectedCategoryFilter = cat },
                      label = { Text(cat, fontSize = 10.5.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal) },
                      trailingIcon = if (!isReadOnly && isCustom) {
                        {
                          Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Manage Category",
                            modifier = Modifier
                              .size(13.dp)
                              .clickable { categoryToManage = cat },
                            tint = if (isCatSelected) Color(0xFF0369A1) else SlateMedium
                          )
                        }
                      } else null,
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
                onMedicineClick = { selectedMedicineForDetails = med },
                onEditMedicine = { if (!isReadOnly) medicineToEdit = med },
                onWastageClick = { if (!isReadOnly) medicineForWastage = med },
                onDeleteMedicine = { if (!isReadOnly) medicineToDelete = med }
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

    // Edit Medicine & Batches Dialog
    medicineToEdit?.let { med ->
      EditMedicineDialog(
        medicine = med,
        suppliers = suppliers,
        categories = medicineCategories,
        units = medicineUnits,
        onAddCategory = { viewModel.addMedicineCategory(it) },
        onAddUnit = { viewModel.addMedicineUnit(it) },
        onDismiss = { medicineToEdit = null },
        onSave = { updatedMed ->
          viewModel.updateMedicine(updatedMed)
          medicineToEdit = null
        }
      )
    }

    // Medicine Details & Batch Inspector Dialog
    selectedMedicineForDetails?.let { med ->
      MedicineDetailsDialog(
        medicine = med,
        suppliers = suppliers,
        currencyFmt = currencyFmt,
        getDaysUntilExpiry = ::getDaysUntilExpiry,
        onDismiss = { selectedMedicineForDetails = null },
        onEditClick = {
          selectedMedicineForDetails = null
          if (!isReadOnly) medicineToEdit = med
        },
        onStockInClick = {
          selectedMedicineForDetails = null
          currentTab = PharmacyViewTab.STOCK_IN
        },
        onWastageClick = {
          selectedMedicineForDetails = null
          if (!isReadOnly) medicineForWastage = med
        }
      )
    }

    // Manage Category Dialog (Rename / Delete)
    categoryToManage?.let { cat ->
      ManageMedicineCategoryDialog(
        category = cat,
        onRename = { newName ->
          viewModel.renameMedicineCategory(cat, newName)
          if (selectedCategoryFilter.equals(cat, ignoreCase = true)) {
            selectedCategoryFilter = newName
          }
          categoryToManage = null
        },
        onDelete = {
          viewModel.deleteMedicineCategory(cat)
          if (selectedCategoryFilter.equals(cat, ignoreCase = true)) {
            selectedCategoryFilter = "All"
          }
          categoryToManage = null
        },
        onDismiss = { categoryToManage = null }
      )
    }

    // Medicine Wastage / Clinic Use Write-Off Dialog
    medicineForWastage?.let { med ->
      MedicineWastageDialog(
        medicine = med,
        currencyFmt = currencyFmt,
        onDismiss = { medicineForWastage = null },
        onConfirm = { qty, reason ->
          viewModel.writeOffMedicineWastage(med.id, qty, reason)
          medicineForWastage = null
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
  onEditMedicine: (() -> Unit)? = null,
  onWastageClick: (() -> Unit)? = null,
  onDeleteMedicine: (() -> Unit)? = null,
  onMedicineClick: (() -> Unit)? = null
) {
  var isExpanded by remember { mutableStateOf(false) }
  val isLowStock = medicine.isLowStock
  val earliestBatch = medicine.sortedBatches.firstOrNull()
  val earliestDays = earliestBatch?.let { getDaysUntilExpiry(it.expiryDate) } ?: 999

  // Expiry Warning Status: Red (< 90 days), Orange (< 180 days), Green (> 180 days)
  val isCriticalExpiry = earliestDays <= 90
  val isWarningExpiry = earliestDays in 91..180

  val supplierInfo = suppliers.find { it.name.equals(medicine.primarySupplier, ignoreCase = true) }
  val categoryColor = remember(medicine.category) { getMedicineCategoryColor(medicine.category) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (onMedicineClick != null) Modifier.clickable { onMedicineClick() } else Modifier),
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
              val (typeColor, typeBg) = when (medicine.effectiveMedicineType) {
                "Injection" -> Color(0xFFE11D48) to Color(0xFFFFE4E6)
                "Consumables" -> Color(0xFFD97706) to Color(0xFFFEF3C7)
                "Topical" -> Color(0xFF7C3AED) to Color(0xFFF3E8FF)
                else -> Color(0xFF0284C7) to Color(0xFFE0F2FE)
              }
              Surface(
                shape = RoundedCornerShape(3.dp),
                color = typeBg,
                border = BorderStroke(0.6.dp, typeColor.copy(alpha = 0.5f))
              ) {
                Text(
                  medicine.effectiveMedicineType,
                  color = typeColor,
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
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
                medicine.genericName.ifBlank { medicine.name },
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0E7490),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (medicine.dosageForm.isNotBlank()) {
                Text(
                  "• ${medicine.dosageForm}",
                  fontSize = 9.sp,
                  color = SlateMuted,
                  maxLines = 1
                )
              }
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
              if (isLowStock) "Low: ${medicine.totalStockDisplay}" else "${medicine.totalStockDisplay} ${medicine.unitType}",
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
          val marginStr = if (medicine.profitMarginPercent % 1.0 == 0.0) "+${medicine.profitMarginPercent.toInt()}%" else String.format(Locale.US, "+%.2f%%", medicine.profitMarginPercent)
          Text(
            marginStr,
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

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onEditMedicine != null) {
            OutlinedButton(
              onClick = onEditMedicine,
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E)),
              border = BorderStroke(1.dp, Color(0xFF0F766E))
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Edit Stock", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (onWastageClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedButton(
              onClick = onWastageClick,
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
              border = BorderStroke(1.dp, Color(0xFFD97706))
            ) {
              Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("အသုံး/လေလွင့်", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (onDeleteMedicine != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
              onClick = onDeleteMedicine,
              modifier = Modifier.size(26.dp)
            ) {
              Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete Medicine",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(16.dp)
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

class EditableBatchState(
  batchNumber: String,
  expiryDate: String,
  quantityText: String,
  costPriceText: String
) {
  var batchNumber by mutableStateOf(batchNumber)
  var expiryDate by mutableStateOf(expiryDate)
  var quantityText by mutableStateOf(quantityText)
  var costPriceText by mutableStateOf(costPriceText)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicineDialog(
  medicine: Medicine,
  suppliers: List<Supplier>,
  categories: List<String>,
  units: List<String>,
  onAddCategory: (String) -> Unit = {},
  onAddUnit: (String) -> Unit = {},
  onDismiss: () -> Unit,
  onSave: (Medicine) -> Unit
) {
  val context = LocalContext.current
  val calendar = Calendar.getInstance()

  var tradeName by remember { mutableStateOf(medicine.name) }
  var genericName by remember { mutableStateOf(medicine.genericName) }
  var selectedCategory by remember { mutableStateOf(medicine.category) }
  var selectedMedicineType by remember { mutableStateOf(medicine.effectiveMedicineType) }
  var selectedUnitType by remember { mutableStateOf(medicine.unitType) }
  var selectedSupplier by remember { mutableStateOf(medicine.primarySupplier) }
  var profitMarginText by remember {
    mutableStateOf(
      if (medicine.profitMarginPercent % 1.0 == 0.0) medicine.profitMarginPercent.toInt().toString()
      else String.format(Locale.US, "%.2f", medicine.profitMarginPercent)
    )
  }
  var isUpdatingProgrammatically by remember { mutableStateOf(false) }
  var sellingPriceText by remember {
    val initialSelling = if (medicine.customSellingPrice != null && medicine.customSellingPrice > 0.0) {
      kotlin.math.round(medicine.customSellingPrice)
    } else {
      val initialCost = medicine.latestCostPrice
      val initialMargin = medicine.profitMarginPercent
      kotlin.math.round(initialCost + (initialCost * (initialMargin / 100.0)))
    }
    mutableStateOf(initialSelling.toInt().toString())
  }

  fun updateSellingFromMargin(cost: Double, margin: Double) {
    if (isUpdatingProgrammatically) return
    isUpdatingProgrammatically = true
    val calculatedSelling = kotlin.math.round(cost + (cost * (margin / 100.0)))
    sellingPriceText = calculatedSelling.toInt().toString()
    isUpdatingProgrammatically = false
  }

  fun updateMarginFromSelling(cost: Double, selling: Double) {
    if (isUpdatingProgrammatically) return
    if (cost <= 0.0) return
    isUpdatingProgrammatically = true
    val calculatedMargin = ((selling - cost) / cost) * 100.0
    profitMarginText = if (calculatedMargin % 1.0 == 0.0) {
      calculatedMargin.toInt().toString()
    } else {
      String.format(Locale.US, "%.2f", calculatedMargin)
    }
    isUpdatingProgrammatically = false
  }

  var minThresholdText by remember { mutableStateOf(medicine.minStockThreshold.toString()) }
  var isAllergyWarning by remember { mutableStateOf(medicine.isAllergyWarning) }
  var standardDosage by remember { mutableStateOf(medicine.standardDosage) }

  // Dropdown expansion states
  var categoryExpanded by remember { mutableStateOf(false) }
  var showAddCategoryModal by remember { mutableStateOf(false) }
  var newCategoryModalText by remember { mutableStateOf("") }

  var unitExpanded by remember { mutableStateOf(false) }
  var showAddUnitModal by remember { mutableStateOf(false) }
  var newUnitModalText by remember { mutableStateOf("") }

  var supplierExpanded by remember { mutableStateOf(false) }

  val batchListState = remember {
    mutableStateListOf<EditableBatchState>().apply {
      if (medicine.batches.isNotEmpty()) {
        addAll(medicine.batches.map {
          EditableBatchState(
            batchNumber = it.batchNumber,
            expiryDate = it.expiryDate,
            quantityText = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString(),
            costPriceText = if (it.costPricePerUnit % 1.0 == 0.0) it.costPricePerUnit.toInt().toString() else it.costPricePerUnit.toString()
          )
        })
      } else {
        add(EditableBatchState(batchNumber = "B1", expiryDate = "2027-12-31", quantityText = "0", costPriceText = "0"))
      }
    }
  }

  fun showBatchDatePicker(batchIndex: Int) {
    val currentExp = batchListState[batchIndex].expiryDate
    val parts = currentExp.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: (calendar.get(Calendar.YEAR) + 2)
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
    val d = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _, year, month, dayOfMonth ->
      batchListState[batchIndex].expiryDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
    }, y, m, d).show()
  }

  val typeOptions = listOf(
    "Oral" to "Oral (သောက်ဆေး)",
    "Injection" to "Injection (ထိုးဆေး/သွင်းဆေး)",
    "Topical" to "Topical (လိမ်းဆေး/ခရင်မ်)",
    "Consumables" to "Consumables (ဆေးခန်းသုံး)"
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text("Edit Medicine & Stock Details", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = DarkGrayText)
          Text("ဆေးအချက်အလက်၊ စတော့နှင့် ရောင်းဈေး ပြင်ဆင်ရန်", fontSize = 10.sp, color = SlateMuted)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Section 1: Classification & Basic Information
        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = BgLightGray),
          border = BorderStroke(0.6.dp, BorderGray)
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("၁။ အခြေခံ အချက်အလက် (Classification & Name)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              typeOptions.forEach { (typeKey, typeLabel) ->
                val isSel = selectedMedicineType == typeKey
                FilterChip(
                  selected = isSel,
                  onClick = {
                    selectedMedicineType = typeKey
                    when (typeKey) {
                      "Oral" -> if (selectedUnitType in listOf("Ampoule", "Vial", "Piece", "Set", "Tube")) selectedUnitType = "Tab"
                      "Injection" -> selectedUnitType = "Ampoule"
                      "Topical" -> selectedUnitType = "Tube"
                      "Consumables" -> selectedUnitType = "Piece"
                    }
                  },
                  label = { Text(typeLabel, fontSize = 9.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (typeKey) {
                      "Injection" -> Color(0xFFE11D48)
                      "Consumables" -> Color(0xFFD97706)
                      "Topical" -> Color(0xFF7C3AED)
                      else -> Color(0xFF0284C7)
                    },
                    selectedLabelColor = PureWhite
                  )
                )
              }
            }

            OutlinedTextField(
              value = tradeName,
              onValueChange = { tradeName = it },
              label = { Text("Trade / Brand Name (ဆေးအမည်) *", fontSize = 11.sp) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )

            OutlinedTextField(
              value = genericName,
              onValueChange = { genericName = it },
              label = { Text("Generic Formula (ပါဝင်ဆေးပစ္စည်း)", fontSize = 11.sp) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              // Category Dropdown
              ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.weight(1f)
              ) {
                OutlinedTextField(
                  value = selectedCategory,
                  onValueChange = { selectedCategory = it },
                  label = { Text("Category", fontSize = 11.sp) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                  modifier = Modifier.menuAnchor().fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  singleLine = true
                )
                ExposedDropdownMenu(
                  expanded = categoryExpanded,
                  onDismissRequest = { categoryExpanded = false }
                ) {
                  categories.filter { it != "All" }.forEach { cat ->
                    DropdownMenuItem(
                      text = { Text(cat, fontSize = 11.sp) },
                      onClick = {
                        selectedCategory = cat
                        categoryExpanded = false
                      }
                    )
                  }
                  HorizontalDivider()
                  DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp)) },
                    text = { Text("+ Category အသစ်...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark) },
                    onClick = {
                      categoryExpanded = false
                      newCategoryModalText = ""
                      showAddCategoryModal = true
                    }
                  )
                }
              }

              // Unit Dropdown
              ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = it },
                modifier = Modifier.weight(1f)
              ) {
                OutlinedTextField(
                  value = selectedUnitType,
                  onValueChange = { selectedUnitType = it },
                  label = { Text("Unit Type", fontSize = 11.sp) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                  modifier = Modifier.menuAnchor().fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  singleLine = true
                )
                ExposedDropdownMenu(
                  expanded = unitExpanded,
                  onDismissRequest = { unitExpanded = false }
                ) {
                  units.forEach { u ->
                    DropdownMenuItem(
                      text = { Text(u, fontSize = 11.sp) },
                      onClick = {
                        selectedUnitType = u
                        unitExpanded = false
                      }
                    )
                  }
                  HorizontalDivider()
                  DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(13.dp)) },
                    text = { Text("+ Unit အသစ်...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark) },
                    onClick = {
                      unitExpanded = false
                      newUnitModalText = ""
                      showAddUnitModal = true
                    }
                  )
                }
              }
            }
          }
        }

        // Section 2: Pricing & Inventory Control
        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = BgLightGray),
          border = BorderStroke(0.6.dp, BorderGray)
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("၂။ ဈေးနှုန်းနှင့် စတော့သတ်မှတ်ချက် (Pricing & Margin)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = profitMarginText,
                onValueChange = {
                  profitMarginText = it
                  val margin = it.toDoubleOrNull() ?: 0.0
                  val currentCost = batchListState.firstOrNull()?.costPriceText?.toDoubleOrNull() ?: medicine.latestCostPrice
                  updateSellingFromMargin(currentCost, margin)
                },
                label = { Text("Margin %", fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
              OutlinedTextField(
                value = sellingPriceText,
                onValueChange = {
                  sellingPriceText = it
                  val selling = it.toDoubleOrNull() ?: 0.0
                  val currentCost = batchListState.firstOrNull()?.costPriceText?.toDoubleOrNull() ?: medicine.latestCostPrice
                  updateMarginFromSelling(currentCost, selling)
                },
                label = { Text("Selling (ရောင်းဈေး အတိအကျ)", fontSize = 11.sp) },
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
              OutlinedTextField(
                value = minThresholdText,
                onValueChange = { minThresholdText = it },
                label = { Text("Min Alert", fontSize = 11.sp) },
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
            }

            // Allergy Warning
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth().clickable { isAllergyWarning = !isAllergyWarning }
            ) {
              Checkbox(checked = isAllergyWarning, onCheckedChange = { isAllergyWarning = it })
              Spacer(modifier = Modifier.width(4.dp))
              Text("Allergy Warning (ဓာတ်မတည့်မှု အထူးသတိပေးရန်)", fontSize = 10.5.sp, color = if (isAllergyWarning) StatusCritical else DarkGrayText, fontWeight = if (isAllergyWarning) FontWeight.Bold else FontWeight.Normal)
            }

            OutlinedTextField(
              value = standardDosage,
              onValueChange = { standardDosage = it },
              label = { Text("Standard Instructions / သောက်သုံးရန် ညွှန်ကြားချက်", fontSize = 11.sp) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              singleLine = true
            )
          }
        }

        // Section 3: Registered Supplier Dropdown
        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = BgLightGray),
          border = BorderStroke(0.6.dp, BorderGray)
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("၃။ ပစ္စည်းပေးသွင်းသူ (Primary Supplier)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)

            ExposedDropdownMenuBox(
              expanded = supplierExpanded,
              onExpandedChange = { supplierExpanded = it },
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = selectedSupplier,
                onValueChange = { selectedSupplier = it },
                label = { Text("Supplier (ကုမ္ပဏီအမည် ရွေးချယ်ပါ/ရိုက်ထည့်ပါ)", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true
              )
              ExposedDropdownMenu(
                expanded = supplierExpanded,
                onDismissRequest = { supplierExpanded = false }
              ) {
                if (suppliers.isEmpty()) {
                  DropdownMenuItem(
                    text = { Text("စာရင်းသွင်းထားသော Supplier မရှိသေးပါ", fontSize = 11.sp, color = SlateMedium) },
                    onClick = { supplierExpanded = false }
                  )
                } else {
                  suppliers.forEach { sup ->
                    DropdownMenuItem(
                      text = {
                        Column {
                          Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
                          if (sup.phone.isNotBlank()) Text(sup.phone, fontSize = 9.sp, color = SlateMuted)
                        }
                      },
                      onClick = {
                        selectedSupplier = sup.name
                        supplierExpanded = false
                      }
                    )
                  }
                }
              }
            }
          }
        }

        // Section 4: Batches Management
        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = BgLightGray),
          border = BorderStroke(0.6.dp, BorderGray)
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("၄။ စတော့အသုတ်များ (Stock Batches - ${batchListState.size} အသုတ်):", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = MedicalGreenDark)
              TextButton(
                onClick = {
                  val nextNum = ""
                  val defExp = String.format(Locale.US, "%04d-12-31", calendar.get(Calendar.YEAR) + 2)
                  batchListState.add(EditableBatchState(batchNumber = nextNum, expiryDate = defExp, quantityText = "50", costPriceText = "1000"))
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Add Batch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }

            batchListState.forEachIndexed { index, batchState ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(0.5.dp, BorderGray)
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Batch #${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                    if (batchListState.size > 1) {
                      IconButton(
                        onClick = { batchListState.removeAt(index) },
                        modifier = Modifier.size(20.dp)
                      ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Batch", tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                      }
                    }
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                      value = batchState.batchNumber,
                      onValueChange = { batchState.batchNumber = it },
                      label = { Text("Batch #", fontSize = 10.sp) },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(4.dp),
                      singleLine = true
                    )
                    OutlinedTextField(
                      value = batchState.expiryDate,
                      onValueChange = { batchState.expiryDate = it },
                      label = { Text("Exp (YYYY-MM-DD)", fontSize = 10.sp) },
                      trailingIcon = {
                        IconButton(onClick = { showBatchDatePicker(index) }, modifier = Modifier.size(22.dp)) {
                          Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = MedicalGreen, modifier = Modifier.size(15.dp))
                        }
                      },
                      modifier = Modifier.weight(1.3f),
                      shape = RoundedCornerShape(4.dp),
                      singleLine = true
                    )
                  }
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                      value = batchState.quantityText,
                      onValueChange = { batchState.quantityText = it },
                      label = { Text("Qty ($selectedUnitType)", fontSize = 10.sp) },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(4.dp),
                      singleLine = true
                    )
                    OutlinedTextField(
                      value = batchState.costPriceText,
                      onValueChange = { batchState.costPriceText = it },
                      label = { Text("Cost Price (Ks)", fontSize = 10.sp) },
                      modifier = Modifier.weight(1f),
                      shape = RoundedCornerShape(4.dp),
                      singleLine = true
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (tradeName.isBlank()) return@Button
          val updatedBatches = batchListState.mapNotNull { bs ->
            val qty = bs.quantityText.toDoubleOrNull() ?: return@mapNotNull null
            val cost = bs.costPriceText.toDoubleOrNull() ?: return@mapNotNull null
            val bNum = bs.batchNumber.trim().ifBlank { "B1" }
            val exp = bs.expiryDate.trim().ifBlank { "2027-12-31" }
            StockBatch(
              batchNumber = bNum,
              expiryDate = exp,
              quantity = qty,
              costPricePerUnit = cost,
              supplierName = selectedSupplier.trim(),
              lastUpdated = System.currentTimeMillis()
            )
          }
          val exactSelling = sellingPriceText.toDoubleOrNull()?.let { kotlin.math.round(it) }
          val updated = medicine.copy(
            name = tradeName.trim(),
            genericName = genericName.trim().ifBlank { tradeName.trim() },
            category = selectedCategory.trim().ifBlank { "General" },
            medicineType = selectedMedicineType,
            unitType = selectedUnitType.trim().ifBlank { "Tab" },
            primarySupplier = selectedSupplier.trim().ifBlank { "AA Medical Products" },
            profitMarginPercent = profitMarginText.toDoubleOrNull() ?: 25.0,
            minStockThreshold = minThresholdText.toIntOrNull() ?: 20,
            isAllergyWarning = isAllergyWarning,
            standardDosage = standardDosage.trim(),
            batches = updatedBatches,
            customSellingPrice = if (exactSelling != null && exactSelling > 0.0) exactSelling else null
          )
          onSave(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("Save Changes (သိမ်းဆည်းမည်)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
        Text("Cancel", fontSize = 11.5.sp)
      }
    }
  )

  // Sub-dialog: Add Category Modal
  if (showAddCategoryModal) {
    AlertDialog(
      onDismissRequest = { showAddCategoryModal = false },
      title = { Text("Add Category အသစ်", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
      text = {
        OutlinedTextField(
          value = newCategoryModalText,
          onValueChange = { newCategoryModalText = it },
          placeholder = { Text("e.g. Analgesic / Inhaler", fontSize = 11.5.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newCategoryModalText.trim()
            if (clean.isNotBlank()) {
              onAddCategory(clean)
              selectedCategory = clean
              showAddCategoryModal = false
            }
          },
          enabled = newCategoryModalText.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Add", fontSize = 11.sp)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddCategoryModal = false }, shape = RoundedCornerShape(6.dp)) {
          Text("Cancel", fontSize = 11.sp)
        }
      }
    )
  }

  // Sub-dialog: Add Unit Modal
  if (showAddUnitModal) {
    AlertDialog(
      onDismissRequest = { showAddUnitModal = false },
      title = { Text("Add Unit အသစ်", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
      text = {
        OutlinedTextField(
          value = newUnitModalText,
          onValueChange = { newUnitModalText = it },
          placeholder = { Text("e.g. Puff / Jar / Set", fontSize = 11.5.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newUnitModalText.trim()
            if (clean.isNotBlank()) {
              onAddUnit(clean)
              selectedUnitType = clean
              showAddUnitModal = false
            }
          },
          enabled = newUnitModalText.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Add", fontSize = 11.sp)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddUnitModal = false }, shape = RoundedCornerShape(6.dp)) {
          Text("Cancel", fontSize = 11.sp)
        }
      }
    )
  }
}

// -------------------------------------------------------------------------------------------------
// Medicine Details & Batch Inspector Dialog
// -------------------------------------------------------------------------------------------------
@Composable
fun MedicineDetailsDialog(
  medicine: Medicine,
  suppliers: List<Supplier>,
  currencyFmt: NumberFormat,
  getDaysUntilExpiry: (String) -> Long,
  onDismiss: () -> Unit,
  onEditClick: () -> Unit,
  onStockInClick: () -> Unit,
  onWastageClick: () -> Unit
) {
  val isLowStock = medicine.isLowStock
  val supplierInfo = suppliers.find { it.name.equals(medicine.primarySupplier, ignoreCase = true) }
  val totalValuationCost = medicine.batches.sumOf { it.quantity * it.costPricePerUnit }
  val totalValuationSelling = medicine.batches.sumOf { it.quantity * medicine.calculatedSellingPrice }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SoftGreenContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Medication, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(medicine.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkGrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(medicine.genericName.ifBlank { "Generic formula not specified" }, fontSize = 10.5.sp, color = SlateMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Badges Row
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          val (typeColor, typeBg) = when (medicine.effectiveMedicineType) {
            "Injection" -> Color(0xFFE11D48) to Color(0xFFFFE4E6)
            "Consumables" -> Color(0xFFD97706) to Color(0xFFFEF3C7)
            "Topical" -> Color(0xFF7C3AED) to Color(0xFFF3E8FF)
            else -> Color(0xFF0284C7) to Color(0xFFE0F2FE)
          }
          Surface(shape = RoundedCornerShape(4.dp), color = typeBg, border = BorderStroke(0.6.dp, typeColor.copy(alpha = 0.5f))) {
            Text(medicine.effectiveMedicineType, color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
          }
          Surface(shape = RoundedCornerShape(4.dp), color = PureWhite, border = BorderStroke(0.6.dp, BorderGray)) {
            Text("Category: ${medicine.category}", fontSize = 9.sp, color = SlateDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
          }
          Surface(shape = RoundedCornerShape(4.dp), color = PureWhite, border = BorderStroke(0.6.dp, BorderGray)) {
            Text("Unit: ${medicine.unitType}", fontSize = 9.sp, color = SlateDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
          }
          if (medicine.isAllergyWarning) {
            Surface(shape = RoundedCornerShape(4.dp), color = StatusCriticalContainer) {
              Text("⚠ Allergy Warning", color = StatusCritical, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }
        }

        // 4-Box Key Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Box 1: Total Stock
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = if (isLowStock) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)),
            border = BorderStroke(0.6.dp, if (isLowStock) Color(0xFFFECACA) else Color(0xFFBBF7D0))
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text("လက်ကျန် စုစုပေါင်း", fontSize = 9.sp, color = SlateMedium)
              Text(
                "${medicine.totalStockDisplay} ${medicine.unitType}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isLowStock) StatusCritical else MedicalGreenDark
              )
              if (isLowStock) {
                Text("Low Stock (Min: ${medicine.minStockThreshold})", fontSize = 8.sp, color = StatusCritical, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Box 2: Latest Cost Price
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = BgLightGray),
            border = BorderStroke(0.6.dp, BorderGray)
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text("ဝယ်ဈေး (Cost)", fontSize = 9.sp, color = SlateMedium)
              Text("${currencyFmt.format(medicine.latestCostPrice.toInt())} Ks", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
              Text("Per ${medicine.unitType}", fontSize = 8.sp, color = SlateMuted)
            }
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Box 3: Exact Selling Price
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = BgLightGray),
            border = BorderStroke(0.6.dp, BorderGray)
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text("ရောင်းဈေး (Selling)", fontSize = 9.sp, color = SlateMedium)
              Text("${currencyFmt.format(medicine.calculatedSellingPrice.toInt())} Ks", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MedicalGreenDark)
              Text("+${medicine.profitMarginPercent.toInt()}% Margin", fontSize = 8.sp, color = MedicalGreen, fontWeight = FontWeight.Bold)
            }
          }

          // Box 4: Total Valuation
          Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = BgLightGray),
            border = BorderStroke(0.6.dp, BorderGray)
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text("စတော့တန်ဖိုး စုစုပေါင်း", fontSize = 9.sp, color = SlateMedium)
              Text("${currencyFmt.format(totalValuationCost.toInt())} Ks", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkGrayText)
              Text("ရောင်းရငွေ: ${currencyFmt.format(totalValuationSelling.toInt())} Ks", fontSize = 8.sp, color = SlateMuted)
            }
          }
        }

        // Supplier Info Strip
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = PureWhite,
          border = BorderStroke(0.6.dp, BorderGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Business, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text("ပေးသွင်းသူ: ${medicine.primarySupplier}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              if (supplierInfo != null && supplierInfo.phone.isNotBlank()) {
                Text("ဖုန်း: ${supplierInfo.phone}${if (supplierInfo.contactPerson.isNotBlank()) " (${supplierInfo.contactPerson})" else ""}", fontSize = 9.5.sp, color = SlateMedium)
              }
            }
          }
        }

        if (medicine.standardDosage.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = PureWhite,
            border = BorderStroke(0.6.dp, BorderGray),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
              Text("ညွှန်ကြားချက်: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Text(medicine.standardDosage, fontSize = 10.sp, color = DarkGrayText)
            }
          }
        }

        // Batches Breakdown Table
        Text(
          "စတော့အသုတ်များ အသေးစိတ် (Stock Batches - ${medicine.batches.size} batches):",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MedicalGreenDark
        )

        Card(
          shape = RoundedCornerShape(6.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          border = BorderStroke(0.6.dp, BorderGray)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 6.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Batch #", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.weight(1f))
              Text("Exp Date", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.weight(1.3f))
              Text("Qty", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.weight(0.8f))
              Text("Cost (Ks)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateDark, modifier = Modifier.weight(0.9f))
            }
            HorizontalDivider(color = BorderGray, thickness = 0.5.dp)

            medicine.allBatches.forEachIndexed { idx, batch ->
              val daysLeft = getDaysUntilExpiry(batch.expiryDate)
              val (badgeColor, badgeBg, badgeText) = when {
                daysLeft <= 0 -> Triple(Color(0xFFDC2626), Color(0xFFFEE2E2), "Expired")
                daysLeft <= 90 -> Triple(Color(0xFFEA580C), Color(0xFFFFEDD5), "$daysLeft d")
                else -> Triple(Color(0xFF16A34A), Color(0xFFDCFCE7), "$daysLeft d")
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else PureWhite)
                  .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(batch.batchNumber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText, modifier = Modifier.weight(1f))

                Row(modifier = Modifier.weight(1.3f), verticalAlignment = Alignment.CenterVertically) {
                  Text(batch.expiryDate, fontSize = 9.sp, color = DarkGrayText)
                  Spacer(modifier = Modifier.width(3.dp))
                  Surface(shape = RoundedCornerShape(3.dp), color = badgeBg) {
                    Text(badgeText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                  }
                }

                val qStr = if (batch.quantity % 1.0 == 0.0) batch.quantity.toInt().toString() else batch.quantity.toString()
                Text("$qStr ${medicine.unitType}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark, modifier = Modifier.weight(0.8f))

                Text(currencyFmt.format(batch.costPricePerUnit.toInt()), fontSize = 9.5.sp, color = SlateDark, modifier = Modifier.weight(0.9f))
              }
              if (idx < medicine.allBatches.size - 1) {
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
          onClick = onStockInClick,
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Stock-In", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onEditClick,
          colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Edit", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = onWastageClick,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(30.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
          border = BorderStroke(1.dp, Color(0xFFD97706))
        ) {
          Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("အသုံး/လေလွင့်", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Text("Close", fontSize = 10.5.sp)
        }
      }
    }
  )
}

// -------------------------------------------------------------------------------------------------
// Manage Medicine Category Dialog (Rename / Delete)
// -------------------------------------------------------------------------------------------------
@Composable
fun ManageMedicineCategoryDialog(
  category: String,
  onRename: (String) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit
) {
  var isRenaming by remember { mutableStateOf(false) }
  var renameText by remember { mutableStateOf(category) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
      title = { Text("Delete Category (အမျိုးအစား ဖျက်မည်)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp) },
      text = {
        Text("'$category' category ကို ဖျက်ရန် သေချာပါသလား?\n\n(မှတ်ချက် - ဤ category အောက်ရှိ ဆေးဝါးများကို 'Others' သို့ အလိုအလျောက် ရွှေ့ပြောင်းပေးပါမည်။)", fontSize = 11.5.sp, color = DarkGrayText)
      },
      confirmButton = {
        Button(
          onClick = {
            onDelete()
            showDeleteConfirm = false
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Delete (ဖျက်မည်)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteConfirm = false }, shape = RoundedCornerShape(6.dp)) {
          Text("Cancel", fontSize = 11.sp)
        }
      }
    )
  } else {
    AlertDialog(
      onDismissRequest = onDismiss,
      icon = { Icon(Icons.Default.Category, contentDescription = null, tint = SkyBluePrimary) },
      title = { Text("Manage Category ($category)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (isRenaming) {
            Text("အမျိုးအစား အမည်သစ် ရိုက်ထည့်ပါ:", fontSize = 11.sp, color = SlateMedium)
            OutlinedTextField(
              value = renameText,
              onValueChange = { renameText = it },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(6.dp)
            )
          } else {
            Text("ဤ Category အတွက် လုပ်ဆောင်ချက် ရွေးချယ်ပါ:", fontSize = 11.5.sp, color = DarkGrayText)
          }
        }
      },
      confirmButton = {
        if (isRenaming) {
          Button(
            onClick = {
              val clean = renameText.trim()
              if (clean.isNotBlank()) {
                onRename(clean)
                onDismiss()
              }
            },
            enabled = renameText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Save Rename", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = { isRenaming = true },
            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
            shape = RoundedCornerShape(6.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Rename", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (!isRenaming) {
            OutlinedButton(
              onClick = { showDeleteConfirm = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
              shape = RoundedCornerShape(6.dp)
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Delete", fontSize = 11.sp)
            }
          }
          OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
            Text("Cancel", fontSize = 11.sp)
          }
        }
      }
    )
  }
}

@Composable
fun MedicineWastageDialog(
  medicine: Medicine,
  currencyFmt: NumberFormat,
  onDismiss: () -> Unit,
  onConfirm: (quantity: Double, reason: String) -> Unit
) {
  var quantityText by remember { mutableStateOf("0.5") }
  var reasonText by remember { mutableStateOf("ရက်ကြာ/အသုံးမကုန် လေလွင့်စွန့်ပစ်") }
  val quickQtyPresets = listOf("0.5", "1", "2", "3")
  val parsedQty = quantityText.toDoubleOrNull() ?: 0.0
  val estimatedCost = parsedQty * medicine.latestCostPrice

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.Default.DeleteSweep,
        contentDescription = null,
        tint = Color(0xFFD97706),
        modifier = Modifier.size(28.dp)
      )
    },
    title = {
      Text(
        "ဆေးဝါးလေလွင့် / ဆေးခန်းအသုံး စာရင်းနှုတ်ခြင်း",
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFFFFBEB),
          border = BorderStroke(0.8.dp, Color(0xFFFDE68A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text(
              medicine.name,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color(0xFF92400E)
            )
            Text(
              "${medicine.genericName} • လက်ကျန်စတော့: ${medicine.totalStockDisplay} ${medicine.unitType}",
              fontSize = 10.sp,
              color = Color(0xFFB45309)
            )
          }
        }

        Text("နှုတ်ပယ်မည့် အရေအတွက် (Deduction Quantity):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)

        // Quick Preset Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickQtyPresets.forEach { preset ->
            val isSel = quantityText == preset
            FilterChip(
              selected = isSel,
              onClick = { quantityText = preset },
              label = { Text("$preset ${medicine.unitType}", fontSize = 10.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFD97706),
                selectedLabelColor = PureWhite
              )
            )
          }
        }

        OutlinedTextField(
          value = quantityText,
          onValueChange = { quantityText = it },
          label = { Text("Quantity (${medicine.unitType}) *", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )

        // Estimated Cost Preview
        if (estimatedCost > 0.0) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = BgLightGray,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "ဝယ်ရင်းတန်ဖိုး: ${currencyFmt.format(estimatedCost.toInt())} Ks အား ဆေးခန်းအသုံးစရိတ် (Expense) အဖြစ် အလိုအလျောက် စာရင်းသွင်းပေးပါမည်။",
                fontSize = 9.5.sp,
                color = SlateDark
              )
            }
          }
        }

        OutlinedTextField(
          value = reasonText,
          onValueChange = { reasonText = it },
          label = { Text("နှုတ်ရသည့် အကြောင်းပြချက် (Reason)", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedQty > 0.0) {
            onConfirm(parsedQty, reasonText.trim())
          }
        },
        enabled = parsedQty > 0.0 && parsedQty <= medicine.totalStock,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
        shape = RoundedCornerShape(6.dp)
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Confirm Write-Off", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(6.dp)) {
        Text("Cancel", fontSize = 11.sp)
      }
    }
  )
}
