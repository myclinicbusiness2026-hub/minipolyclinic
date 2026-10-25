package com.example.ui.components

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
import androidx.compose.ui.window.DialogProperties
import com.example.model.InvoiceItem
import com.example.model.Medicine
import com.example.model.PaymentMethod
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkInSaleDialog(
  medicines: List<Medicine>,
  onDismiss: () -> Unit,
  onCompleteSale: (customerName: String, items: List<InvoiceItem>, paymentMethod: PaymentMethod, discountPercent: Double) -> Unit
) {
  var customerName by remember { mutableStateOf("Walk-in Customer / ပြင်ပဆေးဝယ်လူနာ") }
  var searchQuery by remember { mutableStateOf("") }
  var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var discountPercentText by remember { mutableStateOf("0") }
  var mobileActiveTab by remember { mutableStateOf(0) } // 0: Catalog, 1: Cart & Checkout

  // Cart State: map of medicineId to Pair(Medicine, Quantity)
  val cartItems = remember { mutableStateMapOf<String, Pair<Medicine, Int>>() }
  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  val filteredMedicines = remember(medicines, searchQuery) {
    if (searchQuery.isBlank()) medicines.take(15)
    else medicines.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
          it.genericName.contains(searchQuery, ignoreCase = true) ||
          it.category.contains(searchQuery, ignoreCase = true)
    }.take(25)
  }

  val subTotal = cartItems.values.sumOf { (med, qty) -> med.sellingPrice * qty }
  val discountPercent = discountPercentText.toDoubleOrNull() ?: 0.0
  val discountAmount = subTotal * (discountPercent / 100.0)
  val netPayable = (subTotal - discountAmount).coerceAtLeast(0.0)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .padding(6.dp),
      contentAlignment = Alignment.Center
    ) {
      val isWideScreen = maxWidth >= 680.dp

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        modifier = Modifier
          .fillMaxWidth(if (isWideScreen) 0.92f else 1f)
          .fillMaxHeight(if (isWideScreen) 0.92f else 0.98f)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
        ) {
          // --- HEADER BAR ---
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(OceanGreenLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = OceanGreenPrimary, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "ဆေးသီးသန့်အရောင်း (Walk-in OTC POS)",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateDark
                )
                Text(
                  "ဆရာဝန်ပြစရာမလိုဘဲ တိုက်ရိုက်ဆေးဝယ်ယူခြင်း",
                  fontSize = 10.sp,
                  color = SlateMuted
                )
              }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(20.dp))
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Customer Name Input
          OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("ဝယ်ယူသူအမည် (Customer Name)", fontSize = 10.5.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MedicalGreen,
              unfocusedBorderColor = BorderGray
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          if (isWideScreen) {
            // ==================== TABLET 2-COLUMN VIEW ====================
            Row(modifier = Modifier.weight(1f)) {
              // Left: Catalog
              Column(
                modifier = Modifier
                  .weight(1.1f)
                  .fillMaxHeight()
                  .padding(end = 8.dp)
              ) {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  placeholder = { Text("Search medicine name / generic...", fontSize = 11.sp) },
                  leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
                  trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                      IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                      }
                    }
                  },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("ဆေးစာရင်း (Tap to Add to Cart)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateMedium)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  items(filteredMedicines, key = { it.id }) { med ->
                    val inCart = cartItems.containsKey(med.id)
                    val currentQty = cartItems[med.id]?.second ?: 0
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (inCart) SoftGreenContainer else PureWhite,
                      border = BorderStroke(1.dp, if (inCart) MedicalGreen else Color(0xFFE2E8F0)),
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          cartItems[med.id] = Pair(med, currentQty + 1)
                        }
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(med.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SlateDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                          Text(
                            "${med.genericName} • Stock: ${med.totalStock} ${med.unitType}",
                            fontSize = 9.5.sp,
                            color = if (med.totalStock <= med.minStockThreshold) StatusCritical else SlateMuted
                          )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            "${currencyFmt.format(med.sellingPrice)} Ks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedicalGreenDark
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          if (inCart) {
                            Surface(
                              shape = RoundedCornerShape(4.dp),
                              color = MedicalGreen
                            ) {
                              Text("x$currentQty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PureWhite, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }

              VerticalDivider(color = BorderGray, modifier = Modifier.fillMaxHeight())

              // Right: Cart & Checkout
              Column(
                modifier = Modifier
                  .weight(0.9f)
                  .fillMaxHeight()
                  .padding(start = 8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("ရွေးချယ်ထားသော ဆေးများ (${cartItems.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { cartItems.clear() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                      Text("Clear All", fontSize = 10.sp, color = StatusCritical)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  items(cartItems.values.toList(), key = { it.first.id }) { (med, qty) ->
                    CartItemRow(med = med, qty = qty, currencyFmt = currencyFmt, onQtyChange = { newQ ->
                      if (newQ > 0) cartItems[med.id] = Pair(med, newQ)
                      else cartItems.remove(med.id)
                    })
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                CheckoutSummaryBlock(
                  subTotal = subTotal,
                  discountPercentText = discountPercentText,
                  onDiscountChange = { discountPercentText = it },
                  netPayable = netPayable,
                  currencyFmt = currencyFmt,
                  selectedPaymentMethod = selectedPaymentMethod,
                  onPaymentMethodSelect = { selectedPaymentMethod = it },
                  onComplete = {
                    val items = cartItems.values.map { (m, q) ->
                      InvoiceItem(name = m.name, quantity = q, unitPrice = m.sellingPrice, totalPrice = m.sellingPrice * q, isProcedure = false)
                    }
                    onCompleteSale(customerName, items, selectedPaymentMethod, discountPercent)
                    onDismiss()
                  },
                  isCartEmpty = cartItems.isEmpty()
                )
              }
            }
          } else {
            // ==================== MOBILE SINGLE-FLOW WITH TABS ====================
            TabRow(
              selectedTabIndex = mobileActiveTab,
              containerColor = SoftGreenContainer.copy(alpha = 0.5f),
              contentColor = MedicalGreenDark,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
            ) {
              Tab(
                selected = mobileActiveTab == 0,
                onClick = { mobileActiveTab = 0 },
                text = { Text("၁။ ဆေးရွေးချယ်ရန် (Catalog)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
              )
              Tab(
                selected = mobileActiveTab == 1,
                onClick = { mobileActiveTab = 1 },
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("၂။ စျေးခြင်း (${cartItems.values.sumOf { it.second }})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (cartItems.isNotEmpty()) {
                      Spacer(modifier = Modifier.width(4.dp))
                      Box(
                        modifier = Modifier
                          .size(6.dp)
                          .clip(RoundedCornerShape(3.dp))
                          .background(MedicalGreen)
                      )
                    }
                  }
                }
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (mobileActiveTab == 0) {
              // --- Mobile Tab 0: Catalog Search & Items List ---
              Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  placeholder = { Text("Search medicine name / generic...", fontSize = 11.sp) },
                  leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
                  trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                      IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                      }
                    }
                  },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  items(filteredMedicines, key = { it.id }) { med ->
                    val inCart = cartItems.containsKey(med.id)
                    val currentQty = cartItems[med.id]?.second ?: 0
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (inCart) SoftGreenContainer else PureWhite,
                      border = BorderStroke(1.dp, if (inCart) MedicalGreen else BorderGray),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(med.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                          Text("${med.genericName} • Stock: ${med.totalStock} ${med.unitType}", fontSize = 10.sp, color = SlateMuted)
                          Text("${currencyFmt.format(med.sellingPrice)} Ks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                        }

                        if (inCart) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                              onClick = {
                                if (currentQty > 1) cartItems[med.id] = Pair(med, currentQty - 1)
                                else cartItems.remove(med.id)
                              },
                              modifier = Modifier.size(26.dp)
                            ) {
                              Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(18.dp))
                            }
                            Text("$currentQty", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                            IconButton(
                              onClick = { cartItems[med.id] = Pair(med, currentQty + 1) },
                              modifier = Modifier.size(26.dp)
                            ) {
                              Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
                            }
                          }
                        } else {
                          Button(
                            onClick = { cartItems[med.id] = Pair(med, 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                          ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ ထည့်မည်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }

                // Sticky Footer Bar on Mobile Catalog
                if (cartItems.isNotEmpty()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MedicalGreenDark,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { mobileActiveTab = 1 }
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text("${cartItems.values.sumOf { it.second }} မျိုး ရွေးချယ်ပြီး", color = PureWhite, fontSize = 10.5.sp)
                        Text("${currencyFmt.format(netPayable)} Ks", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ငွေရှင်းရန် (Cart) သို့သွားမည်", color = PureWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                }
              }
            } else {
              // --- Mobile Tab 1: Cart Items & Final Checkout ---
              Column(
                modifier = Modifier
                  .weight(1f)
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("ရွေးချယ်ထားသော ဆေးများ (${cartItems.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  if (cartItems.isNotEmpty()) {
                    TextButton(
                      onClick = { cartItems.clear() },
                      contentPadding = PaddingValues(0.dp),
                      modifier = Modifier.height(24.dp)
                    ) {
                      Text("Clear All", fontSize = 10.5.sp, color = StatusCritical)
                    }
                  }
                }

                if (cartItems.isEmpty()) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(120.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text("စျေးခြင်းထဲတွင် ဆေးပစ္စည်း မရှိသေးပါ\n'ဆေးရွေးချယ်ရန်' Tab သို့သွား၍ ထည့်သွင်းပါ", fontSize = 11.sp, color = SlateMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                  }
                } else {
                  cartItems.values.forEach { (med, qty) ->
                    CartItemRow(med = med, qty = qty, currencyFmt = currencyFmt, onQtyChange = { newQ ->
                      if (newQ > 0) cartItems[med.id] = Pair(med, newQ)
                      else cartItems.remove(med.id)
                    })
                  }
                }

                CheckoutSummaryBlock(
                  subTotal = subTotal,
                  discountPercentText = discountPercentText,
                  onDiscountChange = { discountPercentText = it },
                  netPayable = netPayable,
                  currencyFmt = currencyFmt,
                  selectedPaymentMethod = selectedPaymentMethod,
                  onPaymentMethodSelect = { selectedPaymentMethod = it },
                  onComplete = {
                    val items = cartItems.values.map { (m, q) ->
                      InvoiceItem(name = m.name, quantity = q, unitPrice = m.sellingPrice, totalPrice = m.sellingPrice * q, isProcedure = false)
                    }
                    onCompleteSale(customerName, items, selectedPaymentMethod, discountPercent)
                    onDismiss()
                  },
                  isCartEmpty = cartItems.isEmpty()
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
private fun CartItemRow(
  med: Medicine,
  qty: Int,
  currencyFmt: NumberFormat,
  onQtyChange: (Int) -> Unit
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = BgLightGray,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(med.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SlateDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${currencyFmt.format(med.sellingPrice)} Ks x $qty = ${currencyFmt.format(med.sellingPrice * qty)} Ks", fontSize = 10.sp, color = MedicalGreenDark)
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onQtyChange(qty - 1) }, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(16.dp))
        }
        Text("$qty", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = { onQtyChange(qty + 1) }, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

@Composable
private fun CheckoutSummaryBlock(
  subTotal: Double,
  discountPercentText: String,
  onDiscountChange: (String) -> Unit,
  netPayable: Double,
  currencyFmt: NumberFormat,
  selectedPaymentMethod: PaymentMethod,
  onPaymentMethodSelect: (PaymentMethod) -> Unit,
  onComplete: () -> Unit,
  isCartEmpty: Boolean
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = SoftGreenContainer,
    border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("ကျသင့်ငွေပေါင်း (Subtotal):", fontSize = 11.sp, color = SlateMedium)
        Text("${currencyFmt.format(subTotal)} Ks", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
      }
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("လျှော့ငွေ % (Discount):", fontSize = 11.sp, color = SlateMedium)
        OutlinedTextField(
          value = discountPercentText,
          onValueChange = { onDiscountChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
          singleLine = true,
          modifier = Modifier.width(70.dp).height(40.dp),
          textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        )
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MedicalGreen.copy(alpha = 0.3f))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("ပေးချေရမည့်ငွေ (Net Total):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
        Text("${currencyFmt.format(netPayable)} Ks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
      }
    }
  }

  // Payment Method Chips (Horizontal Scrollable so never clipped)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    PaymentMethod.entries.forEach { pm ->
      val isSel = selectedPaymentMethod == pm
      FilterChip(
        selected = isSel,
        onClick = { onPaymentMethodSelect(pm) },
        label = { Text(pm.label, fontSize = 10.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MedicalGreen,
          selectedLabelColor = PureWhite
        ),
        modifier = Modifier.height(30.dp)
      )
    }
  }

  // Complete Checkout Button
  Button(
    onClick = onComplete,
    enabled = !isCartEmpty,
    colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
  ) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text("ငွေချေပြီး ပြေစာထုတ်မည် (${currencyFmt.format(netPayable)} Ks)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}
