package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.ProcedureItem
import com.example.ui.components.AddProcedureDialog
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProceduresScreen(
  viewModel: ClinicViewModel
) {
  val procedures by viewModel.procedures.collectAsState()
  val procedureCategories by viewModel.procedureCategories.collectAsState()

  var showAddDialog by remember { mutableStateOf(false) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var procedureToDelete by remember { mutableStateOf<ProcedureItem?>(null) }
  var selectedCategory by remember { mutableStateOf<String?>("All") }

  val filteredProcedures = remember(procedures, selectedCategory) {
    if (selectedCategory == null || selectedCategory == "All") {
      procedures
    } else {
      procedures.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
  }

  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundSurface)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    // Compact Clean Header Strip: Title, Counter & Action (Zero wasted space)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(8.dp),
      color = PureWhite,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderGrey)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFFFFEDD5)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(17.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                "Minor Procedures & Dressing",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DarkGrayText
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftGreenContainer
              ) {
                Text(
                  "${filteredProcedures.size} items",
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp,
                  color = MedicalGreenDark,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              "ကုသမှုများနှင့် အသေးစားခွဲစိတ်မှု မှတ်တမ်း",
              fontSize = 10.sp,
              color = SlateMuted
            )
          }
        }

        Button(
          onClick = { showAddDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(4.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Category Filter Chips Strip + Add Category Button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip(
        selected = selectedCategory == "All" || selectedCategory == null,
        onClick = { selectedCategory = "All" },
        label = { Text("All (${procedures.size})", fontSize = 10.5.sp, fontWeight = if (selectedCategory == "All") FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(5.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = Color(0xFFFFEDD5),
          selectedLabelColor = Color(0xFFC2410C),
          containerColor = PureWhite,
          labelColor = SlateMedium
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = selectedCategory == "All",
          borderColor = BorderGray,
          selectedBorderColor = Color(0xFFEA580C),
          borderWidth = 1.dp
        )
      )

      procedureCategories.forEach { cat ->
        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
        val countInCat = procedures.count { it.category.equals(cat, ignoreCase = true) }
        FilterChip(
          selected = isSelected,
          onClick = { selectedCategory = cat },
          label = { Text("$cat ($countInCat)", fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
          shape = RoundedCornerShape(5.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFFEDD5),
            selectedLabelColor = Color(0xFFC2410C),
            containerColor = PureWhite,
            labelColor = SlateMedium
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = BorderGray,
            selectedBorderColor = Color(0xFFEA580C),
            borderWidth = 1.dp
          )
        )
      }

      SuggestionChip(
        onClick = { showAddCategoryDialog = true },
        label = { Text("+ Category", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C)) },
        icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(13.dp)) },
        shape = RoundedCornerShape(5.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFFF7ED)),
        border = BorderStroke(1.dp, Color(0xFFFDBA74))
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (filteredProcedures.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = CardDefaults.outlinedCardBorder()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.SearchOff, contentDescription = null, tint = SlateLight, modifier = Modifier.size(36.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("No procedures in category '${selectedCategory ?: "All"}'", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
              Text("Select another category or click '+ Add' to create one.", fontSize = 11.sp, color = SlateMuted)
            }
          }
        }
      }

      // Procedure Cards
      items(filteredProcedures, key = { it.id }) { proc ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFEDD5)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    proc.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SlateDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    "${proc.code} • ${proc.category} • ${proc.myanmarName}",
                    fontSize = 11.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Spacer(modifier = Modifier.width(6.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "${currencyFmt.format(proc.standardFee.toInt())} Ks",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = OceanGreenPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                  onClick = { procedureToDelete = proc },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete Procedure",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }

            if (proc.dateTime.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Performed: ${proc.dateTime}",
                  fontSize = 9.5.sp,
                  color = SlateMedium,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            if (proc.consumableItems.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pharmacy Store Consumables (${proc.consumableItems.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(3.dp),
                  color = Color(0xFFDCFCE7)
                ) {
                  Text(
                    "FEFO Auto Deduct",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalGreenDark,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                proc.consumableItems.forEach { cons ->
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(Color(0xFFF1F5F9))
                      .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      "${cons.name} x${cons.quantity} ${cons.unitType} (${currencyFmt.format(cons.totalPrice.toInt())} Ks)",
                      fontSize = 9.5.sp,
                      color = DarkGrayText,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF86EFAC))
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Service: ${currencyFmt.format(proc.standardFee.toInt())} Ks + Consumables: ${currencyFmt.format(proc.consumableCost.toInt())} Ks", fontSize = 9.sp, color = DarkGrayText)
                  Text("Total: ${currencyFmt.format(proc.totalProcedureFee.toInt())} Ks", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                }
              }
            } else if (proc.consumables.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text("Consumables:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SlateMuted)
              Spacer(modifier = Modifier.height(2.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                proc.consumables.forEach { item ->
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(BackgroundSurface)
                      .border(1.dp, BorderGrey, RoundedCornerShape(4.dp))
                      .padding(horizontal = 4.dp, vertical = 2.dp)
                  ) {
                    Text(item, fontSize = 10.sp, color = SlateMedium)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddProcedureDialog(
      categoryOptions = procedureCategories,
      onDismiss = { showAddDialog = false },
      onAdd = { name, mmName, cat, fee, notes, consumables ->
        viewModel.addCustomProcedure(name, mmName, cat, fee, notes, consumables)
        if (cat.isNotBlank()) {
          viewModel.addProcedureCategory(cat)
        }
      }
    )
  }

  // Delete Procedure Confirmation Modal
  procedureToDelete?.let { proc ->
    AlertDialog(
      onDismissRequest = { procedureToDelete = null },
      icon = {
        Icon(
          Icons.Default.DeleteOutline,
          contentDescription = null,
          tint = Color(0xFFDC2626),
          modifier = Modifier.size(26.dp)
        )
      },
      title = {
        Text("Delete Procedure Record?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            "${proc.name} (${proc.myanmarName}) အား ကုသမှုစာရင်းမှ ဖျက်သိမ်းပါမည်လား။",
            fontSize = 12.sp,
            color = DarkGrayText,
            fontWeight = FontWeight.Medium
          )
          Text(
            "Category: ${proc.category} • Standard Fee: ${currencyFmt.format(proc.standardFee.toInt())} Ks",
            fontSize = 11.sp,
            color = SlateMedium
          )
          if (proc.consumableItems.isNotEmpty()) {
            Text(
              "ချိတ်ဆက်ထားသော ဆေးပစ္စည်းများ: ${proc.consumableItems.size} မျိုး",
              fontSize = 10.5.sp,
              color = SlateMuted
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            "သတိပေးချက်: အတည်ပြုဖျက်သိမ်းပါက ဤကုသမှုအမျိုးအစားသည် စာရင်းထဲမှ ပယ်ဖျက်သွားမည် ဖြစ်ပါသည်။",
            fontSize = 10.5.sp,
            color = Color(0xFFDC2626)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteProcedure(proc.id)
            procedureToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Delete Procedure", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PureWhite)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { procedureToDelete = null },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("Cancel", fontSize = 11.5.sp)
        }
      }
    )
  }

  // Add Custom Procedure Category Dialog
  if (showAddCategoryDialog) {
    var newCatName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddCategoryDialog = false },
      icon = { Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFEA580C)) },
      title = { Text("Add Procedure Category", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("ကုသမှုအမျိုးအစား အသစ် သတ်မှတ်ထည့်သွင်းပါ", fontSize = 11.5.sp, color = SlateMedium)
          OutlinedTextField(
            value = newCatName,
            onValueChange = { newCatName = it },
            placeholder = { Text("e.g. Ophthalmology / Wound Care", fontSize = 11.5.sp) },
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
            val clean = newCatName.trim()
            if (clean.isNotBlank()) {
              viewModel.addProcedureCategory(clean)
              selectedCategory = clean
              showAddCategoryDialog = false
            }
          },
          enabled = newCatName.isNotBlank(),
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
}

