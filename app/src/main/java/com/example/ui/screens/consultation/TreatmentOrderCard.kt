package com.example.ui.screens.consultation

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
import com.example.model.ClinicSettings
import com.example.model.Medicine
import com.example.model.Patient
import com.example.ui.screens.treatment.CocktailDrugItem
import com.example.ui.screens.treatment.TreatmentCard
import com.example.ui.screens.treatment.TreatmentCardFactory
import com.example.ui.screens.treatment.TreatmentRoute
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TreatmentOrderCard(
  treatmentOrders: List<TreatmentCard>,
  onOpenAddOrder: () -> Unit,
  onRemoveOrder: (Int) -> Unit,
  onTogglePaymentMode: ((Int) -> Unit)? = null,
  currencyFmt: NumberFormat
) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(SoftGreenContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Vaccines, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(16.dp))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("Day-Care Injections & Treatment Card", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MedicalGreenDark)
            Text(
              if (treatmentOrders.isEmpty()) "ဆေးထိုး/အကြောဆေး ညွှန်ကြားချက် (0 Orders)" else "${treatmentOrders.size} Course(s) Ordered",
              fontSize = 10.sp,
              color = SlateMuted
            )
          }
        }

        Button(
          onClick = onOpenAddOrder,
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ဆေးထိုးအော်ဒါ", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (treatmentOrders.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(BackgroundSurface)
            .padding(10.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            "ဆေးထိုး/အကြောဆေး/ပုလင်းချိတ် ကုသမှုအော်ဒါများ ပေးလိုပါက '+ ဆေးထိုးအော်ဒါ' ကို နှိပ်ပါ",
            fontSize = 10.5.sp,
            color = SlateMuted
          )
        }
      } else {
        treatmentOrders.forEachIndexed { index, card ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.5.dp),
            shape = RoundedCornerShape(6.dp),
            color = BackgroundSurface,
            border = BorderStroke(0.8.dp, BorderGray)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (card.route) {
                      TreatmentRoute.IV_INFUSION -> Color(0xFFEFF6FF)
                      TreatmentRoute.IV_PUSH -> Color(0xFFF0FDF4)
                      TreatmentRoute.NEBULIZATION -> Color(0xFFFDF4FF)
                      TreatmentRoute.DRESSING -> Color(0xFFFFFBEB)
                      else -> SoftGreenContainer
                    },
                    border = BorderStroke(0.6.dp, when (card.route) {
                      TreatmentRoute.IV_INFUSION -> SkyBluePrimary
                      TreatmentRoute.IV_PUSH -> MedicalGreen
                      TreatmentRoute.NEBULIZATION -> Color(0xFFC026D3)
                      TreatmentRoute.DRESSING -> Color(0xFFD97706)
                      else -> MedicalGreenDark
                    })
                  ) {
                    Text(
                      card.route.label.split(" ").first(),
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = when (card.route) {
                        TreatmentRoute.IV_INFUSION -> SkyBlueDark
                        TreatmentRoute.IV_PUSH -> MedicalGreenDark
                        TreatmentRoute.NEBULIZATION -> Color(0xFF86198F)
                        TreatmentRoute.DRESSING -> Color(0xFFB45309)
                        else -> MedicalGreenDark
                      },
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    card.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = SlateDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )

                  Spacer(modifier = Modifier.width(6.dp))
                  // Billing Mode Tag
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (card.isPrepaidFullCourse || !card.isPayPerVisit) Color(0xFFEDE9FE) else Color(0xFFE0F2FE),
                    border = BorderStroke(0.5.dp, if (card.isPrepaidFullCourse || !card.isPayPerVisit) Color(0xFF7C3AED) else Color(0xFF0284C7)),
                    modifier = Modifier.clickable { onTogglePaymentMode?.invoke(index) }
                  ) {
                    Text(
                      if (card.isPrepaidFullCourse || !card.isPayPerVisit) "Full Course (အကြေရှင်း)" else "Pay per Visit (တစ်ကြိမ်ချင်း)",
                      fontSize = 8.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (card.isPrepaidFullCourse || !card.isPayPerVisit) Color(0xFF6D28D9) else Color(0xFF0369A1),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val billingModeNote = if (card.isPrepaidFullCourse || !card.isPayPerVisit) {
                  "အကြေရှင်း: ${currencyFmt.format((card.medicationFee + card.sessionFee) * card.totalSessions)} Ks (စုစုပေါင်း ${card.totalSessions} ကြိမ်)"
                } else {
                  "၁ ကြိမ်လျှင်: ${currencyFmt.format(card.medicationFee + card.sessionFee)} Ks (ယနေ့ ၁ ကြိမ်သာ ရှင်းမည်)"
                }
                Text(
                  "${card.frequency} x ${card.totalDays} Days (${card.totalSessions} doses) • $billingModeNote",
                  fontSize = 10.sp,
                  color = SlateMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (card.instructions.isNotBlank()) {
                  Text(
                    "Instr: ${card.instructions}",
                    fontSize = 9.5.sp,
                    color = SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              IconButton(
                onClick = { onRemoveOrder(index) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Modal dialog for doctor to configure and add a Day-Care Injection Order during consultation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTreatmentOrderModal(
  patient: Patient?,
  medicines: List<Medicine>,
  clinicSettings: ClinicSettings,
  doctorName: String,
  onDismiss: () -> Unit,
  onConfirm: (TreatmentCard) -> Unit
) {
  var medicationName by remember { mutableStateOf("Inj. Ceftriaxone 1g") }
  var dosage by remember { mutableStateOf("1 g") }
  var selectedRoute by remember { mutableStateOf(TreatmentRoute.IV_PUSH) }
  var frequency by remember { mutableStateOf("12 hrly") }
  var totalDaysText by remember { mutableStateOf("3") }
  var medFeeText by remember { mutableStateOf("4500") }
  var sessionFeeText by remember(selectedRoute) { mutableStateOf(selectedRoute.getDefaultFee(clinicSettings).toInt().toString()) }
  var instructions by remember { mutableStateOf("Slow IV push over 3-5 mins.") }
  var searchQuery by remember { mutableStateOf("") }
  var splitVialMode by remember { mutableStateOf(false) }
  var isPayPerVisit by remember { mutableStateOf(true) }
  var isCocktail by remember { mutableStateOf(false) }
  val cocktailItems = remember { mutableStateListOf<CocktailDrugItem>() }

  val filteredMedicines = remember(medicines, searchQuery) {
    if (searchQuery.isBlank()) emptyList()
    else medicines.filter {
      (it.effectiveMedicineType in listOf("Injection", "Consumables")) &&
      (it.name.contains(searchQuery, ignoreCase = true) ||
       it.genericName.contains(searchQuery, ignoreCase = true))
    }.take(8)
  }

  val days = totalDaysText.toIntOrNull() ?: 3
  val medFee = medFeeText.toDoubleOrNull() ?: 0.0
  val sessionFee = sessionFeeText.toDoubleOrNull() ?: selectedRoute.getDefaultFee(clinicSettings)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.9f)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
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
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Vaccines, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("ဆေးထိုး/အကြောဆေး အော်ဒါ (New Injection Order)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Text("လူနာ: ${patient?.name ?: ""} (${patient?.hn ?: ""})", fontSize = 10.5.sp, color = SlateMuted)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // 1. Search Medicine from Inventory
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Store ဆေးစာရင်းမှ ရှာဖွေရွေးချယ်ရန် (Optional)...", fontSize = 11.sp) },
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

          if (filteredMedicines.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, BorderGrey),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(4.dp)) {
                filteredMedicines.forEach { med ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        medicationName = med.name
                        medFeeText = med.sellingPrice.toInt().toString()
                        if (med.standardDosage.isNotBlank()) {
                          dosage = med.standardDosage
                        }
                        searchQuery = ""
                      }
                      .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(med.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                      Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 10.sp, color = SlateMuted)
                    }
                    Text("Select", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                  }
                  HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                }
              }
            }
          }

          // Quick Presets
          Text("အသုံးများသော Presets (Quick Routine Orders):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ElevatedAssistChip(
              onClick = {
                medicationName = "Inj. Ceftriaxone 1g"
                dosage = "1 g"
                selectedRoute = TreatmentRoute.IV_PUSH
                frequency = "12 hrly"
                totalDaysText = "3"
                medFeeText = "4500"
                sessionFeeText = "3000"
                instructions = "Slow IV push over 3-5 mins."
                isPayPerVisit = true
              },
              label = { Text("Inj. Ceftriaxone 1g (IV Push)", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Daily Dressing (အနာဆေးထည့်ခြင်း)"
                dosage = "1 time"
                selectedRoute = TreatmentRoute.DRESSING
                frequency = "Daily"
                totalDaysText = "3"
                medFeeText = "0"
                sessionFeeText = "3000"
                instructions = "Clean and aseptic dressing with normal saline & betadine."
                isPayPerVisit = true
              },
              label = { Text("🩹 Wound Dressing", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Nebulizer (Salbutamol 2.5mg + NS)"
                dosage = "1 nebule"
                selectedRoute = TreatmentRoute.NEBULIZATION
                frequency = "BD"
                totalDaysText = "2"
                medFeeText = "1500"
                sessionFeeText = "2000"
                instructions = "Nebulize with 6 L/min Oxygen over 10-15 mins."
                isPayPerVisit = true
              },
              label = { Text("🫁 Nebulizer Therapy", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "IV Todol 30 mg + NS 3cc"
                dosage = "30 mg"
                selectedRoute = TreatmentRoute.IV_PUSH
                frequency = "Stat"
                totalDaysText = "1"
                medFeeText = "3500"
                sessionFeeText = "2000"
                instructions = "Slow IV push over 2-3 mins. Check for NSAID hypersensitivity."
                isPayPerVisit = true
                isCocktail = true
                cocktailItems.clear()
                cocktailItems.add(CocktailDrugItem(name = "Todol (Ketorolac)", dosage = "30mg", quantity = 1, unitPrice = 2500.0))
                cocktailItems.add(CocktailDrugItem(name = "Normal Saline (0.9%)", dosage = "3cc", quantity = 1, unitPrice = 1000.0))
              },
              label = { Text("💉 IV Todol + NS 3cc", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "IV 0.9% NS + Burplex + Vit-C"
                dosage = "500ml Drip"
                selectedRoute = TreatmentRoute.IV_INFUSION
                frequency = "Daily"
                totalDaysText = "1"
                medFeeText = "8500"
                sessionFeeText = "8000"
                instructions = "IV infusion 30 drops/min with sterile infusion set. Monitor vitals."
                isPayPerVisit = true
                isCocktail = true
                cocktailItems.clear()
                cocktailItems.add(CocktailDrugItem(name = "0.9% Normal Saline", dosage = "500ml", quantity = 1, unitPrice = 3000.0))
                cocktailItems.add(CocktailDrugItem(name = "Burplex (Vitamin B-Complex)", dosage = "1 amp", quantity = 1, unitPrice = 2500.0))
                cocktailItems.add(CocktailDrugItem(name = "Vitamin C Inj", dosage = "500mg", quantity = 1, unitPrice = 3000.0))
              },
              label = { Text("💧 IV NS + Burplex + Vit-C", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "5% Dextrose Saline + Neurobion & Vit C"
                dosage = "500ml Drip"
                selectedRoute = TreatmentRoute.IV_INFUSION
                frequency = "Daily"
                totalDaysText = "1"
                medFeeText = "12000"
                sessionFeeText = "8000"
                instructions = "IV Drip 30-40 drops/min with micro-filter set. Monitor vitals."
                isPayPerVisit = true
                isCocktail = true
                cocktailItems.clear()
                cocktailItems.add(CocktailDrugItem(name = "5% Dextrose Saline", dosage = "500ml", quantity = 1, unitPrice = 3500.0))
                cocktailItems.add(CocktailDrugItem(name = "Inj. Neurobion", dosage = "1 amp", quantity = 1, unitPrice = 4500.0))
                cocktailItems.add(CocktailDrugItem(name = "Inj. Vitamin C", dosage = "500mg", quantity = 1, unitPrice = 4000.0))
              },
              label = { Text("💧 5% DS + Vitamins", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Depo-Provera (Medroxyprogesterone 150mg)"
                dosage = "1 vial (1ml)"
                selectedRoute = TreatmentRoute.IM
                frequency = "Stat"
                totalDaysText = "1"
                medFeeText = "7000"
                sessionFeeText = "2500"
                instructions = "Deep IM into gluteal / deltoid muscle."
                isPayPerVisit = true
              },
              label = { Text("💉 Depo-Provera IM", fontSize = 10.sp) }
            )
            ElevatedAssistChip(
              onClick = {
                medicationName = "Suture Removal (ချုပ်ရိုးဖြည်ခြင်း)"
                dosage = "1 procedure"
                selectedRoute = TreatmentRoute.DRESSING
                frequency = "Stat"
                totalDaysText = "1"
                medFeeText = "0"
                sessionFeeText = "3000"
                instructions = "Aseptic stitch off and wound cleaning."
                isPayPerVisit = true
              },
              label = { Text("✂️ Suture Removal", fontSize = 10.sp) }
            )
          }

          // 2. Medication Name & Dosage
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = medicationName,
              onValueChange = { medicationName = it },
              label = { Text("ဆေးအမည် (Medication Name)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(1.4f)
            )
            OutlinedTextField(
              value = dosage,
              onValueChange = { dosage = it },
              label = { Text("ဆေးပမာဏ (Dosage)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(0.8f)
            )
          }

          // Dosage Quick Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("30 mg", "75 mg", "300 mg", "500 mg", "1 g", "1.2 g", "1 Amp", "1 Vial", "1 Bottle").forEach { d ->
              AssistChip(
                onClick = { dosage = d },
                label = { Text(d, fontSize = 9.5.sp) }
              )
            }
          }

          // 3. Route Selector
          Text("ထိုးမည့်နည်းလမ်း (Route of Administration):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TreatmentRoute.entries.forEach { r ->
              val isSel = selectedRoute == r
              FilterChip(
                selected = isSel,
                onClick = { selectedRoute = r },
                label = { Text(r.label, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MedicalGreen,
                  selectedLabelColor = PureWhite
                )
              )
            }
          }

          // 4. Frequency & Duration
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = frequency,
              onValueChange = { frequency = it },
              label = { Text("ကြိမ်နှုန်း (Frequency)", fontSize = 11.sp) },
              placeholder = { Text("12 hrly, BD, Stat") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = totalDaysText,
              onValueChange = { totalDaysText = it },
              label = { Text("ရက်ပေါင်း (Days)", fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.weight(0.8f)
            )
          }

          // Quick Frequency Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("12 hrly", "BD (2 times/day)", "TDS (3 times/day)", "Daily (OD)", "Stat (1 time)").forEach { f ->
              ElevatedAssistChip(
                onClick = {
                  frequency = f.split(" ").first()
                  if (f.startsWith("Stat")) totalDaysText = "1"
                },
                label = { Text(f, fontSize = 9.5.sp) }
              )
            }
          }

          // Split-Vial / Pediatric billing option (Vertical Column Layout for Mobile readability)
          Text("ဆေးပုလင်း တွက်ချက်မှုပုံစံ (Billing Calculation):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (!splitVialMode) SoftGreenContainer else PureWhite,
              border = BorderStroke(1.dp, if (!splitVialMode) MedicalGreen else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { splitVialMode = false }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = !splitVialMode,
                  onClick = { splitVialMode = false },
                  colors = RadioButtonDefaults.colors(selectedColor = MedicalGreen),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("Dose တိုင်း ဆေးဖိုးတွက်မည် (Standard Default)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("ထိုးသည့်အကြိမ်တိုင်းအတွက် ဆေး ၁ လုံးစီ သီးခြားတွက်ချက်မည်", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (splitVialMode) Color(0xFFF0F9FF) else PureWhite,
              border = BorderStroke(1.dp, if (splitVialMode) Color(0xFF0284C7) else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { splitVialMode = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = splitVialMode,
                  onClick = { splitVialMode = true },
                  colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7)),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("ကလေး/ခွဲထိုး ဆေးတွက်ချက်မှု (Split-Vial Mode)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("၁ ရက်လျှင် ဆေး ၁ ပုလင်းသာ အားလုံးခွဲဝေထိုးမည် (ဆေးပုလင်း အပိုမဖြုန်းတီးပါ)", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }
          }

          // Payment Option (Vertical Column Layout for Mobile readability)
          Text("ငွေပေးချေမှုပုံစံ (Payment Option):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
          Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isPayPerVisit) SoftGreenContainer else PureWhite,
              border = BorderStroke(1.dp, if (isPayPerVisit) MedicalGreen else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { isPayPerVisit = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isPayPerVisit,
                  onClick = { isPayPerVisit = true },
                  colors = RadioButtonDefaults.colors(selectedColor = MedicalGreen),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("တစ်ကြိမ်ချင်းရှင်းမည် (Pay per Visit - Default)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("လာထိုးသည့် နေ့စဉ်အကြိမ်တိုင်းအတွက်သာ အလှည့်ကျငွေရှင်းမည် (လူနာကြိုက်)", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (!isPayPerVisit) Color(0xFFFAF5FF) else PureWhite,
              border = BorderStroke(1.dp, if (!isPayPerVisit) Color(0xFF7C3AED) else BorderGrey),
              modifier = Modifier.fillMaxWidth().clickable { isPayPerVisit = false }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = !isPayPerVisit,
                  onClick = { isPayPerVisit = false },
                  colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C3AED)),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("တစ်ခေါက်တည်း အကြေရှင်းမည် (Full Course Upfront)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                  Text("ရက်ပြည့် course စာရင်းတစ်ခုလုံးကို ပထမဆုံး visit တွင် အကြေရှင်းမည်", fontSize = 9.5.sp, color = SlateMuted)
                }
              }
            }
          }

          // 5. Fee breakdown
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = medFeeText,
              onValueChange = { medFeeText = it },
              label = { Text("ဆေးဖိုး (Med Fee Ks)", fontSize = 10.5.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sessionFeeText,
              onValueChange = { sessionFeeText = it },
              label = { Text("ဆေးထိုးဝန်ဆောင်ခ (Service Ks)", fontSize = 10.5.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          // 6. Instructions
          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("ဆေးထိုးညွှန်ကြားချက် (Special Instructions)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(40.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("မလုပ်တော့ပါ", fontSize = 11.sp)
          }

          Button(
            onClick = {
              val sessions = TreatmentCardFactory.generateSessions(
                startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                frequency = frequency.ifBlank { "12 hrly" },
                days = days,
                medicationFee = medFee,
                sessionFee = sessionFee,
                splitVialMode = splitVialMode
              )
              val card = TreatmentCard(
                patientId = patient?.id ?: "",
                patientName = patient?.name ?: "",
                patientHn = patient?.hn ?: "",
                doctorName = doctorName,
                medicationName = medicationName,
                dosage = dosage,
                route = selectedRoute,
                frequency = frequency.ifBlank { "12 hrly" },
                totalDays = days,
                totalSessions = sessions.size,
                sessions = sessions.mapIndexed { idx, s ->
                  if (!isPayPerVisit) s.copy(isPrepaid = true) else s
                },
                sessionFee = sessionFee,
                medicationFee = medFee,
                instructions = instructions,
                isPayPerVisit = isPayPerVisit,
                isPrepaidFullCourse = !isPayPerVisit,
                isCocktail = isCocktail,
                cocktailItems = cocktailItems.toList()
              )
              onConfirm(card)
              onDismiss()
            },
            enabled = medicationName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            modifier = Modifier
              .weight(1.3f)
              .height(40.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("အော်ဒါအတည်ပြုမည် (Confirm Order)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
