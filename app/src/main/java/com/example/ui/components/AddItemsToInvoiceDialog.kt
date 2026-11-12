package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.Medicine
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private data class StagedAddOnItem(
  val medicineId: String = "",
  val name: String,
  var quantity: Int,
  var unitPrice: Double,
  val isCustom: Boolean = false
) {
  val total: Double get() = quantity * unitPrice
}

@Composable
fun AddItemsToInvoiceDialog(
  invoice: Invoice,
  medicines: List<Medicine>,
  onDismiss: () -> Unit,
  onConfirmAddItems: (List<InvoiceItem>) -> Unit
) {
  val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
  var searchQuery by remember { mutableStateOf("") }
  val stagedItems = remember { mutableStateListOf<StagedAddOnItem>() }

  var showCustomItemRow by remember { mutableStateOf(false) }
  var customName by remember { mutableStateOf("") }
  var customQtyText by remember { mutableStateOf("1") }
  var customPriceText by remember { mutableStateOf("") }

  val filteredMedicines = remember(medicines, searchQuery) {
    if (searchQuery.isBlank()) {
      medicines.take(15)
    } else {
      medicines.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.genericName.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
      }.take(25)
    }
  }

  val totalAddOnAmount = stagedItems.sumOf { it.total }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .padding(8.dp),
      contentAlignment = Alignment.Center
    ) {
      val isWideScreen = maxWidth >= 680.dp

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        modifier = Modifier
          .fillMaxWidth(if (isWideScreen) 0.85f else 1f)
          .fillMaxHeight(if (isWideScreen) 0.88f else 0.96f)
          .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
        ) {
          // --- 1. HEADER ---
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = SoftGreenContainer,
                  modifier = Modifier.padding(end = 8.dp)
                ) {
                  Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = MedicalGreenDark,
                    modifier = Modifier.padding(6.dp).size(20.dp)
                  )
                }
                Column {
                  Text(
                    "+ အသုံး/ပစ္စည်း ထပ်ပေါင်းထည့်ရန် (Add Items)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalGreenDark
                  )
                  Text(
                    "ပြေစာ: ${invoice.invoiceNumber} • လူနာ: ${invoice.patientName.ifBlank { "Walk-in" }}",
                    fontSize = 10.5.sp,
                    color = SlateMedium
                  )
                }
              }
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGray.copy(alpha = 0.6f))

          // --- 2. MAIN BODY (Catalog Search + Selected Items List) ---
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // LEFT COLUMN: Medicine/Consumable Selector
            Column(
              modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
            ) {
              // Search Field
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ဆေး/အသုံးပစ္စည်း ရှာဖွေပါ...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                  if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MedicalGreen,
                  unfocusedBorderColor = BorderGray
                )
              )

              Spacer(modifier = Modifier.height(6.dp))

              // Custom Item Toggle Button
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (showCustomItemRow) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (showCustomItemRow) Color(0xFFD97706) else BorderGray),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showCustomItemRow = !showCustomItemRow }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      if (showCustomItemRow) Icons.Default.CheckCircle else Icons.Default.EditNote,
                      contentDescription = null,
                      tint = if (showCustomItemRow) Color(0xFFB45309) else DarkGrayText,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      "စာရင်းပြင်ပ အသုံး/ပစ္စည်း ကိုယ်တိုင်ရိုက်ထည့်မည်",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (showCustomItemRow) Color(0xFFB45309) else DarkGrayText
                    )
                  }
                  Icon(
                    if (showCustomItemRow) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SlateMedium,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              if (showCustomItemRow) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFFFFBEB),
                  border = BorderStroke(0.8.dp, Color(0xFFFDE68A)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                      value = customName,
                      onValueChange = { customName = it },
                      placeholder = { Text("အသုံး/ပစ္စည်း အမည်", fontSize = 10.5.sp) },
                      singleLine = true,
                      modifier = Modifier.fillMaxWidth().height(42.dp),
                      shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      OutlinedTextField(
                        value = customQtyText,
                        onValueChange = { customQtyText = it },
                        label = { Text("Qty", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(4.dp)
                      )
                      OutlinedTextField(
                        value = customPriceText,
                        onValueChange = { customPriceText = it },
                        label = { Text("Unit Price (Ks)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        shape = RoundedCornerShape(4.dp)
                      )
                      Button(
                        onClick = {
                          val q = customQtyText.toIntOrNull() ?: 1
                          val p = customPriceText.toDoubleOrNull() ?: 0.0
                          if (customName.isNotBlank() && q > 0) {
                            stagedItems.add(
                              StagedAddOnItem(
                                medicineId = "",
                                name = customName.trim(),
                                quantity = q,
                                unitPrice = p,
                                isCustom = true
                              )
                            )
                            customName = ""
                            customQtyText = "1"
                            customPriceText = ""
                            showCustomItemRow = false
                          }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalGreenDark),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(44.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                      ) {
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Medicine Inventory Results List
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                items(filteredMedicines, key = { it.id }) { med ->
                  val unitPrice = med.calculatedSellingPrice
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.6.dp, BorderGray),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        val existingIndex = stagedItems.indexOfFirst { it.medicineId == med.id }
                        if (existingIndex >= 0) {
                          val old = stagedItems[existingIndex]
                          stagedItems[existingIndex] = old.copy(quantity = old.quantity + 1)
                        } else {
                          stagedItems.add(
                            StagedAddOnItem(
                              medicineId = med.id,
                              name = med.name,
                              quantity = 1,
                              unitPrice = unitPrice,
                              isCustom = false
                            )
                          )
                        }
                      }
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
                          med.name,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = DarkGrayText,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            "${med.category} • ${med.unitType}",
                            fontSize = 9.sp,
                            color = SlateMedium
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            "လက်ကျန်: ${med.totalStock}",
                            fontSize = 9.sp,
                            color = if (med.totalStock <= med.minStockThreshold) Color(0xFFDC2626) else MedicalGreenDark,
                            fontWeight = FontWeight.SemiBold
                          )
                        }
                      }
                      Column(horizontalAlignment = Alignment.End) {
                        Text(
                          "${currencyFmt.format(unitPrice.toInt())} Ks",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = MedicalGreenDark
                        )
                        Text(
                          "+ Add",
                          fontSize = 9.sp,
                          color = Color(0xFF1565C0),
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }
                  }
                }
              }
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = BorderGray, thickness = 0.8.dp)

            // RIGHT COLUMN: Staged Add-on Items Cart
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            ) {
              Text(
                "ထပ်တိုးမည့် စာရင်း (${stagedItems.size} မျိုး):",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGrayText
              )

              Spacer(modifier = Modifier.height(6.dp))

              if (stagedItems.isEmpty()) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      "ထပ်တိုးလိုသော အသုံးစရိတ်/ဆေးဝါးကို\nဘယ်ဘက်စာရင်းမှ ရွေးချယ်ပါ",
                      fontSize = 10.5.sp,
                      color = SlateMedium,
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                  }
                }
              } else {
                LazyColumn(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                  verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  items(stagedItems) { item ->
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = PureWhite,
                      border = BorderStroke(0.8.dp, BorderGray)
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                              shape = RoundedCornerShape(2.dp),
                              color = Color(0xFFFEF3C7),
                              border = BorderStroke(0.5.dp, Color(0xFFD97706)),
                              modifier = Modifier.padding(end = 4.dp)
                            ) {
                              Text(
                                "ထပ်တိုး",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                              )
                            }
                            Text(
                              item.name,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = DarkGrayText,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                          }
                          Text(
                            "${currencyFmt.format(item.unitPrice.toInt())} Ks x ${item.quantity} = ${currencyFmt.format(item.total.toInt())} Ks",
                            fontSize = 9.5.sp,
                            color = SlateMedium
                          )
                        }

                        // Stepper (- / +)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          IconButton(
                            onClick = {
                              val idx = stagedItems.indexOf(item)
                              if (idx >= 0) {
                                if (item.quantity > 1) {
                                  stagedItems[idx] = item.copy(quantity = item.quantity - 1)
                                } else {
                                  stagedItems.removeAt(idx)
                                }
                              }
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(13.dp), tint = SlateDark)
                          }

                          Text(
                            "${item.quantity}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGrayText,
                            modifier = Modifier.padding(horizontal = 4.dp)
                          )

                          IconButton(
                            onClick = {
                              val idx = stagedItems.indexOf(item)
                              if (idx >= 0) {
                                stagedItems[idx] = item.copy(quantity = item.quantity + 1)
                              }
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(13.dp), tint = MedicalGreenDark)
                          }

                          IconButton(
                            onClick = { stagedItems.remove(item) },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = StatusCritical, modifier = Modifier.size(14.dp))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGray.copy(alpha = 0.6f))

          // --- 3. FOOTER & CONFIRM BAR ---
          if (invoice.isPaid && stagedItems.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFFEF3C7),
              border = BorderStroke(0.8.dp, Color(0xFFD97706)),
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "ဤဘောင်ချာသည် ယခင်က ငွေရှင်းပြီးဖြစ်သဖြင့် ထပ်တိုးပြီးပါက ထပ်တိုးကျသင့်ငွေ ကျန်ရှိမှု (Balance Due) အဖြစ် ပြသပေးမည်ဖြစ်ပါသည်။",
                  fontSize = 9.5.sp,
                  color = Color(0xFF78350F),
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("ထပ်တိုး ကျသင့်ငွေ စုစုပေါင်း:", fontSize = 10.sp, color = DarkGrayText)
              Text(
                "+${currencyFmt.format(totalAddOnAmount.toInt())} Ks",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreenDark
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(38.dp)
              ) {
                Text("ပယ်ဖျက်မည် (Cancel)", fontSize = 11.sp)
              }

              Button(
                onClick = {
                  val newItems = stagedItems.map { staged ->
                    InvoiceItem(
                      name = staged.name,
                      quantity = staged.quantity,
                      unitPrice = staged.unitPrice,
                      totalPrice = staged.total,
                      isProcedure = false,
                      category = if (staged.isCustom) "Consumables / Custom" else "Pharmacy",
                      isAddOn = true,
                      medicineId = staged.medicineId
                    )
                  }
                  onConfirmAddItems(newItems)
                },
                enabled = stagedItems.isNotEmpty(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                modifier = Modifier.height(38.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("မူလဘောင်ချာထဲ ထပ်ပေါင်းထည့်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
