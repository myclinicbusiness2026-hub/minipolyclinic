package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.model.Medicine
import com.example.model.ProcedureConsumable
import com.example.model.ProcedureItem
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProcedureManagementCard(
  procedures: List<ProcedureItem>,
  selectedProcedures: MutableList<ProcedureItem>,
  medicines: List<Medicine>,
  onOpenCustomProcedure: () -> Unit,
  currencyFmt: NumberFormat
) {
  var addingConsumablesForIndex by remember { mutableStateOf<Int?>(null) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("1-Tap Procedure Presets & Minor Surgery", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SkyBluePrimary)
          Text("${selectedProcedures.size} selected", fontSize = 10.sp, color = SlateMuted)
        }

        OutlinedButton(
          onClick = onOpenCustomProcedure,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(12.dp), tint = SkyBluePrimary)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Custom Procedure", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 240.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(procedures) { proc ->
          val isSelected = selectedProcedures.any { it.name.equals(proc.name, ignoreCase = true) }
          Card(
            modifier = Modifier
              .clickable {
                if (isSelected) {
                  selectedProcedures.removeAll { it.name.equals(proc.name, ignoreCase = true) }
                } else {
                  val currentTimestamp = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
                  selectedProcedures.add(
                    proc.copy(
                      id = UUID.randomUUID().toString(),
                      code = proc.code,
                      dateTime = currentTimestamp,
                      consumableItems = proc.consumableItems.map { it.copy(id = UUID.randomUUID().toString()) }
                    )
                  )
                }
              },
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) SkyBlueLight else PureWhite
            ),
            border = if (isSelected) {
              BorderStroke(1.5.dp, SkyBluePrimary)
            } else {
              CardDefaults.outlinedCardBorder()
            }
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  proc.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  color = SlateDark,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(14.dp))
                }
              }
              Text(
                proc.myanmarName,
                fontSize = 9.sp,
                color = SlateMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  proc.category,
                  fontSize = 8.sp,
                  color = SkyBlueDark,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  "${currencyFmt.format(proc.standardFee.toInt())} Ks",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = SkyBluePrimary
                )
              }
            }
          }
        }
      }

      if (selectedProcedures.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.8.dp, color = BorderGray)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Notes,
              contentDescription = null,
              tint = MedicalGreen,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "Selected Procedures & Operative Notes (${selectedProcedures.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 11.5.sp,
              color = DarkGrayText
            )
          }
          Text(
            "Date/time, fees & pharmacy consumables",
            fontSize = 9.sp,
            color = SlateMedium
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          selectedProcedures.forEachIndexed { index, proc ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
              border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                // Header: Name, Category, Delete
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        "${index + 1}. ${proc.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SlateDark
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = SkyBlueLight
                      ) {
                        Text(
                          proc.category,
                          fontSize = 8.5.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = SkyBlueDark,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                    }
                    if (proc.myanmarName.isNotBlank() && proc.myanmarName != proc.name) {
                      Text(proc.myanmarName, fontSize = 9.5.sp, color = SlateMuted)
                    }
                  }

                  IconButton(
                    onClick = { selectedProcedures.removeAt(index) },
                    modifier = Modifier.size(26.dp)
                  ) {
                    Icon(
                      Icons.Default.Delete,
                      contentDescription = "Remove Procedure",
                      tint = StatusCritical,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date and Time of Procedure
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedTextField(
                    value = proc.dateTime,
                    onValueChange = { dt ->
                      selectedProcedures[index] = proc.copy(dateTime = dt)
                    },
                    label = { Text("Procedure Date & Time (နေ့ရက်/အချိန် - Essential)", fontSize = 10.sp) },
                    leadingIcon = {
                      Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = SkyBluePrimary)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = clinicTextFieldColors(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
                  )

                  OutlinedButton(
                    onClick = {
                      val currentDt = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
                      selectedProcedures[index] = proc.copy(dateTime = currentDt)
                    },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(36.dp)
                  ) {
                    Text("Now", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Procedure Fees: Prefill or Manual Add
                OutlinedTextField(
                  value = if (proc.standardFee % 1.0 == 0.0) proc.standardFee.toInt().toString() else proc.standardFee.toString(),
                  onValueChange = { feeStr ->
                    val parsedFee = feeStr.toDoubleOrNull() ?: 0.0
                    selectedProcedures[index] = proc.copy(standardFee = parsedFee)
                  },
                  label = { Text("Procedure Service Fee (Prefill / Manual Add) (ဝန်ဆောင်ခ - Ks)", fontSize = 10.sp) },
                  trailingIcon = {
                    Text("Ks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateMedium, modifier = Modifier.padding(end = 8.dp))
                  },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  shape = RoundedCornerShape(6.dp),
                  colors = clinicTextFieldColors(),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Consumables Section
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = CardDefaults.cardColors(containerColor = PureWhite),
                  border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          "Consumables from Pharmacy Store (${proc.consumableItems.size})",
                          fontWeight = FontWeight.Bold,
                          fontSize = 10.5.sp,
                          color = DarkGrayText
                        )
                      }

                      Button(
                        onClick = { addingConsumablesForIndex = index },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                        modifier = Modifier.height(26.dp)
                      ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add Consumable", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (proc.consumableItems.isEmpty()) {
                      Text(
                        "No pharmacy consumables added. Connect items such as blade, bandage, syringe, local anesthesia, etc. from pharmacy store not to miss any cost items.",
                        fontSize = 9.sp,
                        color = SlateMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(vertical = 4.dp)
                      )
                    } else {
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        proc.consumableItems.forEachIndexed { cIdx, cons ->
                          Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                          ) {
                            Row(
                              modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Column(modifier = Modifier.weight(1f)) {
                                Text(cons.name, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = DarkGrayText)
                                Text(
                                  "@ ${currencyFmt.format(cons.unitPrice.toInt())} Ks x ${cons.quantity} ${cons.unitType} = ${currencyFmt.format(cons.totalPrice.toInt())} Ks",
                                  fontSize = 8.5.sp,
                                  color = SlateMedium
                                )
                              }

                              Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                  onClick = {
                                    if (cons.quantity > 1) {
                                      val updated = proc.consumableItems.toMutableList()
                                      updated[cIdx] = cons.copy(quantity = cons.quantity - 1)
                                      selectedProcedures[index] = proc.copy(consumableItems = updated)
                                    } else {
                                      val updated = proc.consumableItems.toMutableList()
                                      updated.removeAt(cIdx)
                                      selectedProcedures[index] = proc.copy(consumableItems = updated)
                                    }
                                  },
                                  modifier = Modifier.size(22.dp)
                                ) {
                                  Text("-", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
                                }

                                Text(
                                  "${cons.quantity}",
                                  fontSize = 10.sp,
                                  fontWeight = FontWeight.Bold,
                                  modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                  onClick = {
                                    val updated = proc.consumableItems.toMutableList()
                                    updated[cIdx] = cons.copy(quantity = cons.quantity + 1)
                                    selectedProcedures[index] = proc.copy(consumableItems = updated)
                                  },
                                  modifier = Modifier.size(22.dp)
                                ) {
                                  Text("+", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                  onClick = {
                                    val updated = proc.consumableItems.toMutableList()
                                    updated.removeAt(cIdx)
                                    selectedProcedures[index] = proc.copy(consumableItems = updated)
                                  },
                                  modifier = Modifier.size(22.dp)
                                ) {
                                  Icon(Icons.Default.Close, contentDescription = "Remove", tint = StatusCritical, modifier = Modifier.size(13.dp))
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Procedure Fee + Consumables Summary Box
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFF0FDF4),
                  border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text("Procedure Fee: ${currencyFmt.format(proc.standardFee.toInt())} Ks", fontSize = 9.sp, color = DarkGrayText)
                      Text("Consumables: +${currencyFmt.format(proc.consumableCost.toInt())} Ks", fontSize = 9.sp, color = SlateMedium)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                      Text("Total Procedure Cost:", fontSize = 8.5.sp, color = SlateMedium)
                      Text(
                        "${currencyFmt.format(proc.totalProcedureFee.toInt())} Ks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MedicalGreenDark
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detail Notes Text Area
                OutlinedTextField(
                  value = proc.notes,
                  onValueChange = { newNotes ->
                    selectedProcedures[index] = proc.copy(notes = newNotes)
                  },
                  label = {
                    Text("Procedure Notes / Detail on what was done (ပြုလုပ်ချက် အသေးစိတ် မှတ်တမ်း)", fontSize = 10.sp)
                  },
                  placeholder = {
                    Text("Detail what was done (e.g. Aseptic prep, 2% Lignocaine 2ml, 3-0 Silk 3 interrupted sutures, sterile dressing done.)", fontSize = 10.sp)
                  },
                  modifier = Modifier.fillMaxWidth(),
                  minLines = 2,
                  maxLines = 5,
                  shape = RoundedCornerShape(6.dp),
                  colors = clinicTextFieldColors(),
                  textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Quick Note Insertion Chips
                val quickChips = listOf(
                  "Aseptic prep & drape",
                  "2% Lignocaine local anesthesia",
                  "Dry sterile dressing applied",
                  "Hemostasis secured",
                  "Pus drained & cleansed",
                  "Sutures to be removed in 7 days",
                  "Well tolerated, no complications"
                )

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  quickChips.forEach { chipText ->
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = PureWhite,
                      border = BorderStroke(0.7.dp, Color(0xFFCBD5E1)),
                      modifier = Modifier.clickable {
                        val currentNotes = proc.notes.trim()
                        val updatedNotes = if (currentNotes.isBlank()) {
                          chipText
                        } else if (!currentNotes.contains(chipText, ignoreCase = true)) {
                          "$currentNotes. $chipText"
                        } else {
                          currentNotes
                        }
                        selectedProcedures[index] = proc.copy(notes = updatedNotes)
                      }
                    ) {
                      Text(
                        "+ $chipText",
                        fontSize = 8.5.sp,
                        color = SlateDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
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
  }

  // Dialog to add consumables directly from Pharmacy Store
  addingConsumablesForIndex?.let { targetIdx ->
    if (targetIdx in selectedProcedures.indices) {
      val targetProc = selectedProcedures[targetIdx]
      AddProcedureConsumableDialog(
        procedureName = targetProc.name,
        medicines = medicines,
        currencyFmt = currencyFmt,
        onDismiss = { addingConsumablesForIndex = null },
        onAddConsumable = { consumable ->
          val updated = targetProc.consumableItems + consumable
          selectedProcedures[targetIdx] = targetProc.copy(consumableItems = updated)
          addingConsumablesForIndex = null
        }
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProcedureConsumableDialog(
  procedureName: String,
  medicines: List<Medicine>,
  currencyFmt: NumberFormat,
  onDismiss: () -> Unit,
  onAddConsumable: (ProcedureConsumable) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0: From Pharmacy Store, 1: Manual / Custom

  // Pharmacy search & filter states
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  val categoryFilters = listOf("All", "Consumables", "Anesthesia / Injection", "Minor Surgery", "Dressing", "Infusion")

  val filteredMedicines = remember(searchQuery, selectedCategory, medicines) {
    medicines.filter { med ->
      val matchesSearch = searchQuery.isBlank() ||
          med.name.contains(searchQuery, ignoreCase = true) ||
          med.genericName.contains(searchQuery, ignoreCase = true) ||
          med.category.contains(searchQuery, ignoreCase = true)

      val matchesCategory = when (selectedCategory) {
        "All" -> true
        "Consumables" -> med.category.contains("Consumable", ignoreCase = true)
        "Anesthesia / Injection" -> med.category.contains("Anesthesia", ignoreCase = true) || med.category.contains("Injection", ignoreCase = true)
        "Minor Surgery" -> med.category.contains("Minor", ignoreCase = true) || med.dosageForm.contains("Blade", ignoreCase = true) || med.dosageForm.contains("Suture", ignoreCase = true)
        "Dressing" -> med.category.contains("Dressing", ignoreCase = true) || med.dosageForm.contains("Bandage", ignoreCase = true) || med.dosageForm.contains("Gauze", ignoreCase = true)
        "Infusion" -> med.category.contains("Infusion", ignoreCase = true) || med.dosageForm.contains("Cannula", ignoreCase = true)
        else -> true
      }
      matchesSearch && matchesCategory
    }
  }

  var selectedMedicine by remember { mutableStateOf(filteredMedicines.firstOrNull()) }
  var pharmacyQty by remember { mutableIntStateOf(1) }

  // Manual consumable states
  var manualName by remember { mutableStateOf("") }
  var manualPriceText by remember { mutableStateOf("1000") }
  var manualQty by remember { mutableIntStateOf(1) }
  var manualUnitType by remember { mutableStateOf("Pcs") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .padding(vertical = 10.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE0F2FE)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Procedure Consumables Costing", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = DarkGrayText)
              Text("Target: $procedureName", fontSize = 10.sp, color = MedicalGreenDark, fontWeight = FontWeight.Medium)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dual Mode Tabs: Pharmacy Store vs Manual Add
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = SoftLightGray,
          contentColor = MedicalGreen,
          modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("ဆေးဝါးစတိုမှ (Pharmacy Store)", fontSize = 10.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("တိုက်ရိုက်ထည့်သွင်းရန် (Manual / Custom)", fontSize = 10.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search blade, bandage, syringe, anesthesia, sutures...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            colors = clinicSearchBarColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Category Filter Chips
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            categoryFilters.forEach { cat ->
              FilterChip(
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                label = { Text(cat, fontSize = 9.sp, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(24.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Pharmacy Items List
          Text("Select Pharmacy Item (ဆေးဝါးနှင့် ပစ္စည်းများ):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Spacer(modifier = Modifier.height(4.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            val selMedId = selectedMedicine?.id
            items(filteredMedicines, key = { it.id }) { med ->
              val isSelected = selMedId != null && selMedId == med.id
              val unitPrice = med.currentSellingPrice

              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedMedicine = med
                    pharmacyQty = 1
                  },
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) SkyBlueLight else BackgroundSurface,
                border = BorderStroke(
                  if (isSelected) 1.5.dp else 0.8.dp,
                  if (isSelected) SkyBluePrimary else BorderGray
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(med.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkGrayText)
                      Spacer(modifier = Modifier.width(4.dp))
                      Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFFF1F5F9)
                      ) {
                        Text(med.unitType, fontSize = 8.5.sp, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                      }
                    }
                    Text("${med.genericName} • Stock: ${med.totalStock}", fontSize = 9.sp, color = SlateMedium)
                  }

                  Text(
                    "${currencyFmt.format(unitPrice.toInt())} Ks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MedicalGreenDark
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Selected Item Quantity & Cost Calculation
          selectedMedicine?.let { med ->
            val unitPrice = med.currentSellingPrice
            val subtotal = unitPrice * pharmacyQty

            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
              border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "Quantity Needed (${med.unitType}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DarkGrayText
                  )

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                      onClick = { if (pharmacyQty > 1) pharmacyQty-- },
                      shape = RoundedCornerShape(4.dp),
                      contentPadding = PaddingValues(0.dp),
                      modifier = Modifier.size(26.dp)
                    ) {
                      Text("-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Text(
                      "$pharmacyQty ${med.unitType}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.5.sp,
                      modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    OutlinedButton(
                      onClick = { pharmacyQty++ },
                      shape = RoundedCornerShape(4.dp),
                      contentPadding = PaddingValues(0.dp),
                      modifier = Modifier.size(26.dp)
                    ) {
                      Text("+", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF86EFAC))
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "Unit: ${currencyFmt.format(unitPrice.toInt())} Ks x $pharmacyQty",
                    fontSize = 9.5.sp,
                    color = SlateMedium
                  )
                  Text(
                    "Subtotal: ${currencyFmt.format(subtotal.toInt())} Ks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MedicalGreenDark
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f).height(36.dp)
            ) {
              Text("Cancel", fontSize = 11.sp)
            }

            Button(
              onClick = {
                selectedMedicine?.let { med ->
                  val consumable = ProcedureConsumable(
                    medicineId = med.id,
                    name = med.name,
                    unitPrice = med.currentSellingPrice,
                    quantity = pharmacyQty,
                    unitType = med.unitType
                  )
                  onAddConsumable(consumable)
                }
              },
              enabled = selectedMedicine != null,
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1.5f).height(36.dp)
            ) {
              Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add to Procedure", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

        } else {
          // --- TAB 1: MANUAL / CUSTOM CONSUMABLE ITEM ---
          Text(
            "Quick Presets (လက်တွေ့သုံး ပစ္စည်းများ):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateDark
          )
          Spacer(modifier = Modifier.height(4.dp))

          val presets = listOf(
            Triple("Disposable Syringe 5ml (21G)", 300.0, "Pcs"),
            Triple("Disposable Syringe 3ml (23G)", 300.0, "Pcs"),
            Triple("Sterile Surgical Blade #11", 800.0, "Pcs"),
            Triple("Sterile Surgical Blade #15", 800.0, "Pcs"),
            Triple("Lignocaine 2% Injection (Local Anesthesia)", 1800.0, "Vial"),
            Triple("Crepe Bandage 4-inch", 1200.0, "Roll"),
            Triple("Sterile Gauze Swab Pack", 600.0, "Pack"),
            Triple("Silk Suture 3-0 with Needle", 1500.0, "Pcs"),
            Triple("Povidone Iodine 10% Solution", 1200.0, "Bottle")
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            presets.forEach { (pName, pPrice, pUnit) ->
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = PureWhite,
                border = BorderStroke(0.8.dp, MedicalGreen.copy(alpha = 0.5f)),
                modifier = Modifier.clickable {
                  manualName = pName
                  manualPriceText = pPrice.toInt().toString()
                  manualUnitType = pUnit
                }
              ) {
                Text(
                  "+ $pName",
                  fontSize = 8.5.sp,
                  color = MedicalGreenDark,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = manualName,
            onValueChange = { manualName = it },
            label = { Text("Consumable Item Name (ပစ္စည်းအမည်)", fontSize = 10.sp) },
            placeholder = { Text("e.g. Syringe 5cc, Blade #15, Crepe Bandage, Lignocaine", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = manualPriceText,
              onValueChange = { manualPriceText = it },
              label = { Text("Unit Price (Ks)", fontSize = 10.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(6.dp),
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )

            OutlinedTextField(
              value = manualUnitType,
              onValueChange = { manualUnitType = it },
              label = { Text("Unit Type (e.g. Pcs, Roll, Vial)", fontSize = 10.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(6.dp),
              colors = clinicTextFieldColors(),
              textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Quantity Needed:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkGrayText)

            Row(verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { if (manualQty > 1) manualQty-- },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(26.dp)
              ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              Text(
                "$manualQty $manualUnitType",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
              )

              OutlinedButton(
                onClick = { manualQty++ },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(26.dp)
              ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val parsedManualPrice = manualPriceText.toDoubleOrNull() ?: 0.0
          val manualTotal = parsedManualPrice * manualQty

          Surface(
            color = SoftIceBlue,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Total Consumable Cost:", fontSize = 10.sp, color = SlateDark)
              Text(
                "${currencyFmt.format(manualTotal.toInt())} Ks",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MedicalGreenDark
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f).height(36.dp)
            ) {
              Text("Cancel", fontSize = 11.sp)
            }

            Button(
              onClick = {
                if (manualName.isNotBlank()) {
                  val consumable = ProcedureConsumable(
                    medicineId = "MANUAL-${UUID.randomUUID()}",
                    name = manualName.trim(),
                    unitPrice = parsedManualPrice,
                    quantity = manualQty,
                    unitType = manualUnitType.ifBlank { "Pcs" }
                  )
                  onAddConsumable(consumable)
                }
              },
              enabled = manualName.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1.5f).height(36.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add to Procedure", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomProcedureModal(
  medicines: List<Medicine>,
  onDismiss: () -> Unit,
  onSaveProcedure: (String, String, String, Double, String, String, List<String>, List<ProcedureConsumable>) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var myanmarName by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Minor Surgery") }
  var feeText by remember { mutableStateOf("20000") }
  var notes by remember { mutableStateOf("") }
  var dateTime by remember {
    mutableStateOf(SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date()))
  }

  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
  var showAddConsumableDialog by remember { mutableStateOf(false) }
  val attachedPharmacyConsumables = remember { mutableStateListOf<ProcedureConsumable>() }

  val commonConsumables = listOf(
    "Sterile Gauze pack",
    "Blade #11",
    "Lignocaine 2%",
    "Silk Suture 3-0",
    "Nylon Suture 4-0",
    "Normal Saline 100ml",
    "Povidone Iodine",
    "IV Cannula 22G",
    "Crepe Bandage 4\"",
    "Syringe 5ml + 21G Needle"
  )

  val selectedConsumables = remember { mutableStateListOf<String>() }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .padding(vertical = 10.dp),
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Healing, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Dynamic Procedure Builder", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SlateDark)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Procedure Name (English)", fontSize = 11.sp) },
          placeholder = { Text("e.g. Lipoma Excision / Ear Syringing", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = myanmarName,
          onValueChange = { myanmarName = it },
          label = { Text("Secondary / Local Name", fontSize = 11.sp) },
          placeholder = { Text("e.g. အကျိတ်ခွဲထုတ်ခြင်း / ဒဏ်ရာချုပ်ခြင်း", fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true,
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Date & Time of Procedure
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = dateTime,
            onValueChange = { dateTime = it },
            label = { Text("Procedure Date & Time (Essential)", fontSize = 10.sp) },
            leadingIcon = {
              Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = SkyBluePrimary)
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
          )

          OutlinedButton(
            onClick = {
              dateTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
            },
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(36.dp)
          ) {
            Text("Now", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category", fontSize = 11.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
          )
          OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = it },
            label = { Text("Procedure Fee (Ks)", fontSize = 11.sp) },
            trailingIcon = { Text("Ks", fontSize = 10.sp, color = SlateMedium, modifier = Modifier.padding(end = 6.dp)) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            singleLine = true,
            colors = clinicTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Consumables from Pharmacy Store
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Consumable Items (from Pharmacy Store):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Button(
            onClick = { showAddConsumableDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("Add from Pharmacy", fontSize = 9.sp, fontWeight = FontWeight.Bold)
          }
        }

        if (attachedPharmacyConsumables.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            attachedPharmacyConsumables.forEachIndexed { cIdx, cons ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "• ${cons.name} x${cons.quantity} ${cons.unitType} (@ ${currencyFmt.format(cons.unitPrice.toInt())} Ks)",
                    fontSize = 9.5.sp,
                    color = DarkGrayText,
                    fontWeight = FontWeight.Medium
                  )
                  IconButton(
                    onClick = { attachedPharmacyConsumables.removeAt(cIdx) },
                    modifier = Modifier.size(20.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(12.dp))
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Quick-Pick Consumables Required:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          commonConsumables.forEach { item ->
            val isPicked = selectedConsumables.contains(item)
            FilterChip(
              selected = isPicked,
              onClick = {
                if (isPicked) selectedConsumables.remove(item) else selectedConsumables.add(item)
              },
              label = { Text(item, fontSize = 9.sp) },
              modifier = Modifier.height(26.dp),
              shape = RoundedCornerShape(4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Detail Procedure / Operative Notes (What was done)", fontSize = 11.sp) },
          placeholder = { Text("e.g. Incision made, 20ml purulent fluid drained, cleansed with normal saline, dressed with sterile gauze.", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(36.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Cancel", fontSize = 11.sp, maxLines = 1)
          }

          Button(
            onClick = {
              if (name.isNotBlank()) {
                val fee = feeText.toDoubleOrNull() ?: 20000.0
                onSaveProcedure(
                  name,
                  myanmarName,
                  category,
                  fee,
                  notes,
                  dateTime,
                  selectedConsumables.toList(),
                  attachedPharmacyConsumables.toList()
                )
              }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier
              .weight(1.3f)
              .height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Save Procedure", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }
      }
    }
  }

  if (showAddConsumableDialog) {
    AddProcedureConsumableDialog(
      procedureName = name.ifBlank { "Custom Procedure" },
      medicines = medicines,
      currencyFmt = currencyFmt,
      onDismiss = { showAddConsumableDialog = false },
      onAddConsumable = { consumable ->
        attachedPharmacyConsumables.add(consumable)
        showAddConsumableDialog = false
      }
    )
  }
}
