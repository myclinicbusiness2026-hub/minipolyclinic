package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ClinicSettings
import com.example.model.ConsultationRecord
import com.example.model.Invoice
import com.example.model.Patient
import com.example.service.printing.BluetoothThermalPrinterManager
import com.example.service.printing.ThermalPrintService
import com.example.service.printing.triggerSystemPrintThermalSlip
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * PosThermalSlipDialog
 * 
 * Provides an authentic, high-fidelity Thermal POS Receipt Studio:
 * 1. Dual Print Engine:
 *    - Android System Print Spooler (opens native Print Settings with Wi-Fi, PDF, All Printers)
 *    - Direct Bluetooth POS (ESC/POS binary stream for portable thermal hardware)
 * 2. Authentic, clean paper rendering with zero broken dash-wraps.
 * 3. 58mm (Standard Roll) and 80mm (Wide Roll) instant toggle with accurate paper proportions.
 */
@Composable
fun PosThermalSlipDialog(
  invoice: Invoice,
  patientHn: String = "HN-0000",
  cashierName: String = "Cashier",
  clinicSettings: ClinicSettings = ClinicSettings(),
  onDismiss: () -> Unit,
  onPrintSuccess: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currencyFmt = NumberFormat.getNumberInstance(Locale.US)
  
  var is80mm by remember { mutableStateOf(clinicSettings.thermalPaperWidthMm == 80) }
  var showSettingsPanel by remember { mutableStateOf(false) }
  var printMethod by remember { mutableStateOf(0) } // 0: System Print Spooler, 1: Bluetooth Direct POS
  var isPrinting by remember { mutableStateOf(false) }
  var printStatusMessage by remember { mutableStateOf<String?>(null) }
  var isErrorStatus by remember { mutableStateOf(false) }

  // Paired Bluetooth Devices & Hardware Settings Store
  val printerSettingsStore = remember { com.example.service.printing.PrinterSettingsStore(context) }
  val savedMac = remember { printerSettingsStore.getPrinterMacAddress() ?: "" }
  val pairedPrinters = remember { BluetoothThermalPrinterManager.getPairedPrinters(context) }
  var selectedPrinterAddress by remember {
    mutableStateOf(
      if (savedMac.isNotBlank()) savedMac
      else if (clinicSettings.bluetoothPrinterMac.isNotBlank()) clinicSettings.bluetoothPrinterMac
      else pairedPrinters.firstOrNull()?.address ?: ""
    )
  }

  // Generate binary ESC/POS commands dynamically
  val escPosBytes = remember(invoice, is80mm, clinicSettings, patientHn, cashierName) {
    ThermalPrintService.generateEscPosInvoiceBytes(
      invoice = invoice,
      patientHn = patientHn,
      cashierName = cashierName,
      is80mm = is80mm,
      settings = clinicSettings
    )
  }

  // System Print Spooler Handler (Launches Android Print Settings Dialog)
  val handleSystemPrint: () -> Unit = {
    triggerSystemPrintThermalSlip(
      context = context,
      invoice = invoice,
      patientHn = patientHn,
      cashierName = cashierName,
      clinicSettings = clinicSettings,
      is80mm = is80mm
    )
    printStatusMessage = "Android စနစ် Print Settings ဖွင့်လှစ်ပြီးပါပြီ"
    isErrorStatus = false
    onPrintSuccess()
  }

  // Direct Bluetooth Thermal Handler
  val handleDirectBluetoothPrint: () -> Unit = {
    if (selectedPrinterAddress.isBlank()) {
      if (pairedPrinters.isEmpty()) {
        printStatusMessage = "ဘလူးတု ပရင်တာ ချိတ်ဆက်ထားခြင်း မရှိသေးပါ။ Android Settings > Bluetooth တွင် POS Printer နှင့် Pair အရင်လုပ်ပေးပါ။"
        isErrorStatus = true
      } else {
        printStatusMessage = "ကျေးဇူးပြု၍ ပရင်တာ ရွေးချယ်ပါ"
        isErrorStatus = true
      }
    } else {
      isPrinting = true
      printStatusMessage = "ပရင်တာသို့ ဒေတာ ပေးပို့နေပါသည်..."
      isErrorStatus = false

      coroutineScope.launch {
        val result = BluetoothThermalPrinterManager.printBytes(
          context = context,
          deviceAddress = selectedPrinterAddress,
          bytes = escPosBytes
        )
        isPrinting = false
        if (result.isSuccess) {
          printStatusMessage = "ဘောက်ချာ ထုတ်ယူပြီးပါပြီ (Print Successful)"
          isErrorStatus = false
          Toast.makeText(context, "Thermal Receipt Printed!", Toast.LENGTH_SHORT).show()
          onPrintSuccess()
        } else {
          val errorMsg = result.exceptionOrNull()?.message ?: "Unknown Bluetooth Error"
          printStatusMessage = "ပရင်တာ ချိတ်ဆက်မှု မအောင်မြင်ပါ: $errorMsg"
          isErrorStatus = true
          Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.94f)
        .padding(vertical = 8.dp),
      shape = RoundedCornerShape(12.dp),
      color = BgLightGray,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // Dialog Top Bar
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = PureWhite,
          shadowElevation = 2.dp
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(MedicalGreen),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  "ဘောက်ချာ ထုတ်ယူရန် နမူနာ (Receipt Preview)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = DarkGrayText
                )
                Text(
                  "Invoice: ${invoice.invoiceNo} • ${invoice.patientName}",
                  fontSize = 10.sp,
                  color = SlateMuted
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Button(
                onClick = handleSystemPrint,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
              ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp), tint = PureWhite)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Print ထုတ်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
              }
              Spacer(modifier = Modifier.width(6.dp))
              IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        // Secondary Toolbar: Width Selector (58mm vs 80mm) + Print Method Toggle
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = PureWhite,
          border = androidx.compose.foundation.BorderStroke(0.8.dp, BorderGray)
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Paper Roll Toggle
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("စက္ကူဆိုဒ်:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SlateMedium)
                FilterChip(
                  selected = !is80mm,
                  onClick = { is80mm = false },
                  label = { Text("58mm Standard", fontSize = 10.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SoftGreenContainer,
                    selectedLabelColor = MedicalGreenDark
                  ),
                  modifier = Modifier.height(28.dp)
                )
                FilterChip(
                  selected = is80mm,
                  onClick = { is80mm = true },
                  label = { Text("80mm Wide", fontSize = 10.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SoftGreenContainer,
                    selectedLabelColor = MedicalGreenDark
                  ),
                  modifier = Modifier.height(28.dp)
                )
              }

              // Print Settings Toggle Button
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                  onClick = {
                    context.startActivity(android.content.Intent(context, com.example.ui.screens.PrinterSettingsActivity::class.java))
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(4.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, MedicalGreen)
                ) {
                  Icon(Icons.Default.Settings, contentDescription = "XP-T361 Settings", tint = MedicalGreen, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("XP-T361 Setup", fontSize = 9.5.sp, color = MedicalGreen, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                  onClick = { showSettingsPanel = !showSettingsPanel },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(4.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, if (showSettingsPanel) MedicalGreen else BorderGray)
                ) {
                  Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (showSettingsPanel) MedicalGreen else SlateMedium)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (showSettingsPanel) "ဆက်တင် ပိတ်ရန်" else "စက္ကူနှင့် နည်းလမ်း", fontSize = 10.sp, color = if (showSettingsPanel) MedicalGreen else SlateMedium)
                }
              }
            }

            // Expandable Print Settings Panel
            if (showSettingsPanel) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                color = BackgroundSurface,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(8.dp)) {
                  Text("ပရင်တာ ဆက်တင်များနှင့် ထုတ်ယူမည့်နည်းလမ်း:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                      selected = printMethod == 0,
                      onClick = { printMethod = 0 },
                      label = { Text("Android System Spooler (Wi-Fi, PDF)", fontSize = 9.5.sp) },
                      colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
                    )
                    FilterChip(
                      selected = printMethod == 1,
                      onClick = { printMethod = 1 },
                      label = { Text("Direct Bluetooth POS Printer", fontSize = 9.5.sp) },
                      colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
                    )
                  }

                  if (printMethod == 1 && pairedPrinters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ရွေးချယ်ထားသော ဘလူးတု ပရင်တာ:", fontSize = 9.5.sp, color = SlateMedium)
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      pairedPrinters.forEach { p ->
                        val isSelected = selectedPrinterAddress == p.address
                        Surface(
                          shape = RoundedCornerShape(4.dp),
                          color = if (isSelected) SoftGreenContainer else PureWhite,
                          border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MedicalGreen else BorderGray),
                          modifier = Modifier.clickable {
                            selectedPrinterAddress = p.address
                            printerSettingsStore.savePrinterDevice(p.address, p.name)
                          }
                        ) {
                          Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (isSelected) MedicalGreenDark else SlateMedium, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(p.name, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MedicalGreenDark else DarkGrayText)
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

        if (printStatusMessage != null) {
          Surface(
            color = if (isErrorStatus) StatusCriticalContainer else SoftGreenContainer,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(4.dp)
          ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(
                if (isErrorStatus) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isErrorStatus) StatusCritical else MedicalGreenDark,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(printStatusMessage!!, fontSize = 10.sp, color = if (isErrorStatus) StatusCritical else MedicalGreenDark)
            }
          }
        }

        // Center Viewport: Realistic Thermal Receipt Paper Sheet
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.TopCenter
        ) {
          Surface(
            modifier = Modifier
              .widthIn(max = if (is80mm) 350.dp else 295.dp)
              .fillMaxWidth(if (is80mm) 0.95f else 0.88f)
              .shadow(elevation = 6.dp, shape = RoundedCornerShape(3.dp)),
            shape = RoundedCornerShape(3.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              // 1. Clinic Branding Header
              ShweKhitLogo(
                size = 36.dp,
                customLogoUri = clinicSettings.effectiveLogo.ifBlank { null }
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                clinicSettings.clinicNameMm,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = DarkGrayText,
                textAlign = TextAlign.Center
              )
              if (clinicSettings.clinicSubtitleMm.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  clinicSettings.clinicSubtitleMm,
                  fontSize = 11.sp,
                  color = SlateMedium,
                  textAlign = TextAlign.Center
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              val cleanDocName = clinicSettings.doctorName.replace("(?i)(,\\s*)?M\\.?B\\.?\\,?\\s*B\\.?S\\.?(.*)".toRegex(), "").trim()
              Text(
                "$cleanDocName  ${clinicSettings.doctorDegree}\nTel: ${clinicSettings.clinicPhone}",
                fontSize = 10.sp,
                color = SlateMedium,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
              )

              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color(0xFF94A3B8))

              // 2. Receipt Metadata
              Text(
                "OFFICIAL INVOICE RECEIPT",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = DarkGrayText,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bill No : ${invoice.invoiceNo}", fontSize = 10.5.sp, color = DarkGrayText, fontWeight = FontWeight.SemiBold)
                Text(invoice.createdAt, fontSize = 9.5.sp, color = SlateMedium)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Patient : ${invoice.patientName} (${patientHn})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cashier : $cashierName", fontSize = 10.sp, color = SlateMedium)
                Text(
                  if (invoice.isPaid) "PAID (${invoice.paymentMethod.label})" else "UNPAID",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (invoice.isPaid) StatusSuccess else StatusWarning
                )
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 1.dp, color = Color(0xFF94A3B8))

              // 3. Table Column Headers
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ITEM DESCRIPTION", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                Text("AMOUNT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.6.dp, color = Color(0xFFCBD5E1))

              // 4. Line Items (Modern 2-Line POS Format matching physical thermal slip)
              if (invoice.consultationFee > 0.0) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.5.dp)
                ) {
                  Text(
                    "Doctor Consultation Fee",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText,
                    lineHeight = 14.5.sp
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      "  1 x ${currencyFmt.format(invoice.consultationFee.toInt())} Ks",
                      fontSize = 10.sp,
                      color = SlateMedium
                    )
                    Text(
                      "${currencyFmt.format(invoice.consultationFee.toInt())} Ks",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = DarkGrayText
                    )
                  }
                }
              }

              if (invoice.serviceFee > 0.0) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.5.dp)
                ) {
                  Text(
                    "Service Fee",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText,
                    lineHeight = 14.5.sp
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      "  1 x ${currencyFmt.format(invoice.serviceFee.toInt())} Ks",
                      fontSize = 10.sp,
                      color = SlateMedium
                    )
                    Text(
                      "${currencyFmt.format(invoice.serviceFee.toInt())} Ks",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = DarkGrayText
                    )
                  }
                }
              }

              invoice.items.forEach { item ->
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.5.dp)
                ) {
                  Text(
                    item.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText,
                    lineHeight = 14.5.sp
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      "  ${item.quantity} x ${currencyFmt.format(item.unitPrice.toInt())} Ks",
                      fontSize = 10.sp,
                      color = SlateMedium
                    )
                    Text(
                      "${currencyFmt.format(item.total.toInt())} Ks",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = DarkGrayText
                    )
                  }
                }
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 1.dp, color = Color(0xFF94A3B8))

              // 5. Totals
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal:", fontSize = 11.sp, color = DarkGrayText)
                Text("${currencyFmt.format(invoice.subTotal.toInt())} Ks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText)
              }

              if (invoice.discountAmount > 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  val discLabel = if (invoice.discountPercent > 0) "Discount (${invoice.discountPercent.toInt()}%):" else "Discount:"
                  Text(discLabel, fontSize = 10.5.sp, color = StatusSuccess)
                  Text("-${currencyFmt.format(invoice.discountAmount.toInt())} Ks", fontSize = 10.5.sp, color = StatusSuccess)
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GRAND TOTAL:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                Text("${currencyFmt.format(invoice.grandTotal.toInt())} Ks", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color(0xFF94A3B8))

              // 6. Footer Greeting
              Text(
                clinicSettings.receiptGreetingMm.ifBlank { "ကျေးဇူးတင်ပါသည် (Thank you!)" },
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
                color = SlateDark,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                "* ${if (is80mm) "80mm Wide" else "58mm Standard"} POS Thermal Slip *",
                fontSize = 8.5.sp,
                color = SlateMedium
              )
            }
          }
        }

        // Prominent Bottom Action Bar
        Surface(
          color = PureWhite,
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier
                .weight(0.9f)
                .height(40.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("ပိတ်မည် (Close)", fontSize = 11.sp, maxLines = 1)
            }

            // Direct System Print Button (Invokes Android Print Spooler immediately)
            Button(
              onClick = handleSystemPrint,
              modifier = Modifier
                .weight(1.3f)
                .height(40.dp),
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Print Settings (စနစ်ဖြင့်ထုတ်မည်)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            // Bluetooth Direct Print Button
            Button(
              onClick = handleDirectBluetoothPrint,
              enabled = !isPrinting,
              modifier = Modifier
                .weight(1.1f)
                .height(40.dp),
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
              if (isPrinting) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PureWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Printing...", fontSize = 10.sp)
              } else {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bluetooth POS", fontSize = 10.5.sp, maxLines = 1)
              }
            }
          }
        }
      }
    }
  }
}

/**
 * A4ClinicalDocDialog
 * Generates A4 Medical Record + Prescription PDF Preview.
 * MANDATORY RULE: Used ONLY for Patient Clinical Notes & Prescriptions when requested.
 */
@Composable
fun A4ClinicalDocDialog(
  consultation: ConsultationRecord,
  patient: Patient? = null,
  clinicSettings: ClinicSettings = ClinicSettings(),
  onDismiss: () -> Unit
) {
  A4PrintPreviewDialog(
    consultation = consultation,
    patient = patient,
    clinicSettings = clinicSettings,
    onDismiss = onDismiss
  )
}
