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
import com.example.ui.screens.treatment.TreatmentConsumable
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
  onEditOrder: ((Int) -> Unit)? = null,
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
          Text("+ ကုသမှု/ဆေးထိုး အော်ဒါ", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
            "ဆေးထိုး/အကြောဆေး/ဒဏ်ရာဆေးထည့်/ကုသမှုအော်ဒါများ ပေးလိုပါက '+ ကုသမှု/ဆေးထိုး အော်ဒါ' ကို နှိပ်ပါ",
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
                      if (card.route == TreatmentRoute.DRESSING) "Dressing" else card.routeDisplay.split(" ").first(),
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

              Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEditOrder != null) {
                  IconButton(
                    onClick = { onEditOrder(index) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MedicalGreen, modifier = Modifier.size(15.dp))
                  }
                  Spacer(modifier = Modifier.width(4.dp))
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
}

/**
 * Mode selector for treatment order: Single drug, Cocktail mix, or Wound dressing procedure
 */
enum class TreatmentOrderMode {
  SINGLE_DRUG,
  COCKTAIL,
  WOUND_DRESSING
}

private data class DressingQuickPreset(
  val label: String,
  val fullName: String,
  val technique: String,
  val fee: Double,
  val frequency: String,
  val days: Int,
  val instructions: String
)

private val DRESSING_PRESETS = listOf(
  DressingQuickPreset(
    label = "Clean Dressing (5,000 Ks)",
    fullName = "Regular Clean Dressing (ဒဏ်ရာဆေးထည့်)",
    technique = "Aseptic Clean Dressing",
    fee = 5000.0,
    frequency = "Daily",
    days = 3,
    instructions = "Clean with Normal Saline & Povidone Iodine. Apply sterile gauze & micropore."
  ),
  DressingQuickPreset(
    label = "Infected / Large Wound (7,000 Ks)",
    fullName = "Infected / Large Wound Dressing (ပြည်တည်/ဒဏ်ရာကြီး)",
    technique = "Antiseptic Pack & Dressing",
    fee = 7000.0,
    frequency = "Daily",
    days = 5,
    instructions = "Debridement if needed, clean with H2O2 / NS. Betadine pack & sterile bandage."
  ),
  DressingQuickPreset(
    label = "Burn Dressing (7,000 Ks)",
    fullName = "Burn Dressing (မီးလောင်ဒဏ်ရာ)",
    technique = "Silver Sulphadiazine Dressing",
    fee = 7000.0,
    frequency = "Alternate Day",
    days = 5,
    instructions = "Clean with normal saline. Apply SSD cream and sterile dressing."
  ),
  DressingQuickPreset(
    label = "Suture Removal / Stitch Off (5,000 Ks)",
    fullName = "Suture Removal / Stitch Off (ချုပ်ရိုးဖြည်)",
    technique = "Stitch Off & Dressing",
    fee = 5000.0,
    frequency = "Stat",
    days = 1,
    instructions = "Clean wound edges. Remove sutures aseptically and apply sterile strip/dressing."
  ),
  DressingQuickPreset(
    label = "Minor Dressing (3,000 Ks)",
    fullName = "Simple / Minor Dressing (ဒဏ်ရာအသေး)",
    technique = "Minor Clean Dressing",
    fee = 3000.0,
    frequency = "Daily",
    days = 2,
    instructions = "Clean with antiseptic and apply sterile adhesive dressing."
  )
)

/**
 * Modal dialog for doctor to configure and add a Day-Care Treatment Order (Injections, Drips, Wound Dressings) during consultation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTreatmentOrderModal(
  patient: Patient?,
  medicines: List<Medicine>,
  clinicSettings: ClinicSettings,
  doctorName: String,
  initialCard: TreatmentCard? = null,
  onDismiss: () -> Unit,
  onConfirm: (TreatmentCard) -> Unit
) {
  var orderMode by remember(initialCard) {
    mutableStateOf(
      when {
        initialCard?.route == TreatmentRoute.DRESSING -> TreatmentOrderMode.WOUND_DRESSING
        initialCard?.isCocktail == true -> TreatmentOrderMode.COCKTAIL
        else -> TreatmentOrderMode.SINGLE_DRUG
      }
    )
  }
  var isCocktail by remember(initialCard) { mutableStateOf(initialCard?.isCocktail ?: false) }
  var isPatientSupplied by remember(initialCard) { mutableStateOf(initialCard?.isPatientSupplied ?: false) }

  var medicationName by remember(initialCard) {
    mutableStateOf(
      if (initialCard?.route == TreatmentRoute.DRESSING && (initialCard.medicationName.isBlank() || initialCard.medicationName == "Injection / Treatment"))
        "Regular Clean Dressing (ဒဏ်ရာဆေးထည့်)"
      else initialCard?.medicationName ?: ""
    )
  }
  var selectedMedId by remember(initialCard) { mutableStateOf(initialCard?.medicationId ?: "") }
  var cocktailTitle by remember(initialCard) { mutableStateOf(if (initialCard?.isCocktail == true) initialCard.medicationName else "") }
  val cocktailItems = remember(initialCard) {
    mutableStateListOf<CocktailDrugItem>().apply {
      if (initialCard?.isCocktail == true && initialCard.cocktailItems.isNotEmpty()) {
        addAll(initialCard.cocktailItems)
      }
    }
  }

  val consumablesList = remember(initialCard) {
    mutableStateListOf<TreatmentConsumable>().apply {
      if (initialCard != null && initialCard.consumables.isNotEmpty()) {
        addAll(initialCard.consumables)
      }
    }
  }
  var consumableSearchQuery by remember { mutableStateOf("") }

  var dosage by remember(initialCard) {
    mutableStateOf(
      if (initialCard?.route == TreatmentRoute.DRESSING && initialCard.dosage.isBlank())
        "Aseptic Clean Dressing"
      else initialCard?.dosage ?: ""
    )
  }
  var selectedRoute by remember(initialCard) {
    mutableStateOf(initialCard?.route ?: if (initialCard?.route == TreatmentRoute.DRESSING) TreatmentRoute.DRESSING else TreatmentRoute.IV_PUSH)
  }
  var customRouteText by remember(initialCard) { mutableStateOf(initialCard?.customRoute ?: "") }
  var frequency by remember(initialCard) {
    mutableStateOf(
      if (initialCard?.route == TreatmentRoute.DRESSING && (initialCard.frequency.isBlank() || initialCard.frequency == "Stat"))
        "Daily"
      else initialCard?.frequency ?: "Stat"
    )
  }
  var totalDaysText by remember(initialCard) {
    mutableStateOf((if (initialCard?.route == TreatmentRoute.DRESSING && (initialCard.totalDays <= 1)) 3 else (initialCard?.totalDays ?: 1)).toString())
  }
  var medFeeText by remember(initialCard) { mutableStateOf((initialCard?.medicationFee?.toInt() ?: 0).toString()) }
  var sessionFeeText by remember(initialCard, selectedRoute) {
    mutableStateOf(
      initialCard?.sessionFee?.toInt()?.toString()
        ?: (if (initialCard?.route == TreatmentRoute.DRESSING) "5000" else selectedRoute.getDefaultFee(clinicSettings).toInt().toString())
    )
  }
  var instructions by remember(initialCard) {
    mutableStateOf(
      if (initialCard?.route == TreatmentRoute.DRESSING && initialCard.instructions.isBlank())
        "Clean with Normal Saline & Povidone Iodine. Apply sterile gauze & micropore."
      else initialCard?.instructions ?: ""
    )
  }
  var searchQuery by remember { mutableStateOf("") }
  var splitVialMode by remember { mutableStateOf(false) }
  var isPayPerVisit by remember(initialCard) { mutableStateOf(initialCard?.isPayPerVisit ?: true) }

  val filteredMedicines = remember(medicines, searchQuery) {
    if (searchQuery.isBlank()) emptyList()
    else medicines.filter {
      (it.effectiveMedicineType in listOf("Injection", "Consumables")) &&
      (it.name.contains(searchQuery, ignoreCase = true) ||
       it.genericName.contains(searchQuery, ignoreCase = true))
    }.take(8)
  }

  val filteredConsumables = remember(medicines, consumableSearchQuery) {
    if (consumableSearchQuery.isBlank()) emptyList()
    else medicines.filter {
      (it.effectiveMedicineType == "Consumables" ||
       it.category.contains("Consumable", ignoreCase = true) ||
       it.name.contains("Syringe", ignoreCase = true) ||
       it.name.contains("Needle", ignoreCase = true) ||
       it.name.contains("Cannula", ignoreCase = true) ||
       it.name.contains("Set", ignoreCase = true) ||
       it.name.contains("Swab", ignoreCase = true) ||
       it.name.contains("Plaster", ignoreCase = true) ||
       it.name.contains("Gauze", ignoreCase = true) ||
       it.name.contains("Bandage", ignoreCase = true) ||
       it.name.contains("Micropore", ignoreCase = true) ||
       it.name.contains("Cotton", ignoreCase = true) ||
       it.name.contains("Povidone", ignoreCase = true) ||
       it.name.contains("Betadine", ignoreCase = true) ||
       it.name.contains("Water", ignoreCase = true) ||
       it.name.contains(consumableSearchQuery, ignoreCase = true)) &&
      (it.name.contains(consumableSearchQuery, ignoreCase = true) ||
       it.genericName.contains(consumableSearchQuery, ignoreCase = true))
    }.take(8)
  }

  val days = totalDaysText.toIntOrNull() ?: 1
  val medFee = if (isPatientSupplied && orderMode != TreatmentOrderMode.WOUND_DRESSING) 0.0 else (medFeeText.toDoubleOrNull() ?: 0.0)
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
        .fillMaxHeight(0.92f)
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
                .background(if (orderMode == TreatmentOrderMode.WOUND_DRESSING) Color(0xFFFEF3C7) else SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                if (orderMode == TreatmentOrderMode.WOUND_DRESSING) Icons.Default.MedicalServices else Icons.Default.Vaccines,
                contentDescription = null,
                tint = if (orderMode == TreatmentOrderMode.WOUND_DRESSING) Color(0xFFD97706) else MedicalGreenDark,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                if (initialCard != null) {
                  if (orderMode == TreatmentOrderMode.WOUND_DRESSING) "ဒဏ်ရာဆေးထည့်/ကုသမှု အော်ဒါ ပြင်ဆင်ရန် (Edit Dressing Order)"
                  else "ဆေးထိုး/အကြောဆေး အော်ဒါ ပြင်ဆင်ရန် (Edit Injection Order)"
                } else {
                  if (orderMode == TreatmentOrderMode.WOUND_DRESSING) "ဒဏ်ရာဆေးထည့်/ကုသမှု အော်ဒါ အသစ် (New Dressing Order)"
                  else "ဆေးထိုး/အကြောဆေး အော်ဒါ (New Injection Order)"
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
              )
              Text("လူနာ: ${patient?.name ?: ""} (${patient?.hn ?: ""})", fontSize = 10.5.sp, color = SlateMuted)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // 1. Order Type Mode Selector: Single Drug vs Cocktail Mix vs Wound Dressing
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(3.dp)) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (orderMode == TreatmentOrderMode.SINGLE_DRUG) MedicalGreen else Color.Transparent,
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    orderMode = TreatmentOrderMode.SINGLE_DRUG
                    isCocktail = false
                    if (selectedRoute == TreatmentRoute.DRESSING) selectedRoute = TreatmentRoute.IV_PUSH
                    sessionFeeText = selectedRoute.getDefaultFee(clinicSettings).toInt().toString()
                    if (medicationName.contains("Dressing", ignoreCase = true) || medicationName.contains("Stitch", ignoreCase = true)) {
                      medicationName = ""
                      dosage = ""
                    }
                    if (!isPatientSupplied && medFeeText == "0") medFeeText = "4500"
                  }
                  .padding(vertical = 6.dp),
                contentColor = if (orderMode == TreatmentOrderMode.SINGLE_DRUG) PureWhite else SlateMedium
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Single Drug", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (orderMode == TreatmentOrderMode.COCKTAIL) MedicalGreen else Color.Transparent,
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    orderMode = TreatmentOrderMode.COCKTAIL
                    isCocktail = true
                    if (selectedRoute == TreatmentRoute.IM || selectedRoute == TreatmentRoute.DRESSING) {
                      selectedRoute = TreatmentRoute.IV_INFUSION
                    }
                    sessionFeeText = selectedRoute.getDefaultFee(clinicSettings).toInt().toString()
                    if (!isPatientSupplied) {
                      medFeeText = cocktailItems.sumOf { it.totalPrice }.toInt().toString()
                    }
                  }
                  .padding(vertical = 6.dp),
                contentColor = if (orderMode == TreatmentOrderMode.COCKTAIL) PureWhite else SlateMedium
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Cocktail / Mix", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (orderMode == TreatmentOrderMode.WOUND_DRESSING) Color(0xFFD97706) else Color.Transparent,
                modifier = Modifier
                  .weight(1.1f)
                  .clickable {
                    orderMode = TreatmentOrderMode.WOUND_DRESSING
                    isCocktail = false
                    selectedRoute = TreatmentRoute.DRESSING
                    sessionFeeText = "5000"
                    if (medicationName.isBlank() || medicationName == "IV Cocktail Mix") {
                      medicationName = "Regular Clean Dressing (ဒဏ်ရာဆေးထည့်)"
                      dosage = "Aseptic Clean Dressing"
                      instructions = "Clean with Normal Saline & Povidone Iodine. Apply sterile gauze & micropore."
                    }
                    if (frequency == "Stat") frequency = "Daily"
                    if (totalDaysText == "1") totalDaysText = "3"
                    if (!isPatientSupplied) medFeeText = "0"
                  }
                  .padding(vertical = 6.dp),
                contentColor = if (orderMode == TreatmentOrderMode.WOUND_DRESSING) PureWhite else SlateMedium
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Wound Dressing", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
              }
            }
          }

          if (orderMode != TreatmentOrderMode.WOUND_DRESSING) {
            // 2. Patient-Supplied / External Medicine Toggle Banner
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isPatientSupplied) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
              border = BorderStroke(1.dp, if (isPatientSupplied) Color(0xFFF59E0B) else BorderGrey),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  isPatientSupplied = !isPatientSupplied
                  if (isPatientSupplied) {
                    medFeeText = "0"
                  } else {
                    medFeeText = if (isCocktail) cocktailItems.sumOf { it.totalPrice }.toInt().toString() else "4500"
                  }
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isPatientSupplied,
                  onCheckedChange = {
                    isPatientSupplied = it
                    if (it) {
                      medFeeText = "0"
                    } else {
                      medFeeText = if (isCocktail) cocktailItems.sumOf { c -> c.totalPrice }.toInt().toString() else "4500"
                    }
                  },
                  colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                  Text(
                    "လူနာပါ ဆေးဝါး (Patient-Supplied / External Medicine)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPatientSupplied) Color(0xFF92400E) else SlateDark
                  )
                  Text(
                    if (isPatientSupplied) "လူနာမှ ကိုယ်တိုင်ယူဆောင်လာသောဆေး (ဆေးဖိုး 0 Ks • Store မနှုတ်ပါ • ဝန်ဆောင်ခသာ ယူမည်)"
                    else "ဆေးခန်း Store ဆေးသုံးစွဲမည် (Stock နှုတ်ပြီး ဆေးဖိုးတွက်ချက်မည်)",
                    fontSize = 9.sp,
                    color = if (isPatientSupplied) Color(0xFFB45309) else SlateMuted
                  )
                }
              }
            }
          }

          when (orderMode) {
            TreatmentOrderMode.SINGLE_DRUG -> {
              // === Single Drug Mode ===
              // Search Store
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
                            val gen = med.genericName.trim()
                            medicationName = if (gen.isNotBlank()) gen else med.name
                            selectedMedId = med.id
                            if (!isPatientSupplied) {
                              medFeeText = med.sellingPrice.toInt().toString()
                            }
                            if (med.standardDosage.isNotBlank()) {
                              dosage = med.standardDosage
                            }
                            val lower = med.name.lowercase()
                            if (instructions.isBlank()) {
                              when {
                                lower.contains("rabies") || lower.contains("tetanus") || lower.contains("toxoid") ||
                                lower.contains("hep") || lower.contains("vaccine") || lower.contains("flu") -> {
                                  instructions = "Deltoid IM Stat (လက်မောင်း)"
                                  selectedRoute = TreatmentRoute.IM
                                }
                                lower.contains("depo") -> {
                                  instructions = "Deep IM Stat (Gluteal)"
                                  selectedRoute = TreatmentRoute.IM
                                }
                                lower.contains("ceftri") || lower.contains("roceph") -> {
                                  instructions = "Slow IV Push (over 3-5 mins)"
                                  selectedRoute = TreatmentRoute.IV_PUSH
                                }
                                lower.contains("todol") || lower.contains("tramadol") || lower.contains("ketorolac") -> {
                                  instructions = "Slow IV Push / IM"
                                  selectedRoute = TreatmentRoute.IV_PUSH
                                }
                              }
                            }
                            searchQuery = ""
                          }
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(med.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                          Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 9.5.sp, color = SlateMuted)
                        }
                        Text("Select", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                      }
                      HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }
                  }
                }
              }

              // Medication Name & Dosage
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
            }

            TreatmentOrderMode.COCKTAIL -> {
              // === Cocktail / IV Mix Mode ===
              OutlinedTextField(
                value = cocktailTitle,
                onValueChange = { cocktailTitle = it },
                label = { Text("Cocktail အမည် (Cocktail Title / Order Name)", fontSize = 11.sp) },
                placeholder = { Text("ဥပမာ - IV NS + Burplex + Vit-C သို့မဟုတ် IV Todol + NS 3cc", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              // Search to Add Drugs to Cocktail
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("+ Store ဆေးစာရင်းမှ ဆေးထည့်သွင်းရန် ရှာဖွေပါ (Add Drug)...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(16.dp)) },
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
                            val gen = med.genericName.trim()
                            val cleanName = if (gen.isNotBlank()) gen else med.name
                            cocktailItems.add(
                              CocktailDrugItem(
                                medicineId = med.id,
                                name = cleanName,
                                dosage = med.standardDosage.ifBlank { "1 amp" },
                                quantity = 1,
                                unitPrice = med.sellingPrice
                              )
                            )
                            if (!isPatientSupplied) {
                              medFeeText = cocktailItems.sumOf { it.totalPrice }.toInt().toString()
                            }
                            if (cocktailTitle.isBlank()) {
                              cocktailTitle = cocktailItems.joinToString(" + ") { it.name.replace(Regex("(?i)^inj\\.?\\s*"), "") }
                            }
                            searchQuery = ""
                          }
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(med.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                          Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 9.5.sp, color = SlateMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Default.Add, contentDescription = null, tint = MedicalGreenDark, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(2.dp))
                          Text("Add to Mix", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                        }
                      }
                      HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }
                  }
                }
              }

              // Constituent Drugs List
              Text(
                "တွဲစပ်ထားသော ဆေးများ (Constituent Drugs - ${cocktailItems.size} items):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
              )

              if (cocktailItems.isEmpty()) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Color(0xFFF8FAFC),
                  border = BorderStroke(1.dp, BorderGrey),
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                  Text(
                    "Cocktail တွင် ပါဝင်မည့် ဆေး ၂ မျိုး သို့မဟုတ် ၃ မျိုးကို အထက်ပါ Store ရှာဖွေမှုမှ ရွေးချယ်ထည့်သွင်းပါ",
                    fontSize = 10.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  cocktailItems.forEachIndexed { idx, item ->
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFFF0FDF4),
                      border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(item.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                          Text(
                            "Dose: ${item.dosage.ifBlank { "-" }} • Qty: ${item.quantity} • ${item.unitPrice.toInt()} Ks/unit",
                            fontSize = 9.5.sp,
                            color = SlateMedium
                          )
                        }

                        IconButton(
                          onClick = {
                            cocktailItems.removeAt(idx)
                            if (!isPatientSupplied) {
                              medFeeText = cocktailItems.sumOf { it.totalPrice }.toInt().toString()
                            }
                          },
                          modifier = Modifier.size(24.dp)
                        ) {
                          Icon(Icons.Default.Delete, contentDescription = "Remove", tint = StatusCritical, modifier = Modifier.size(15.dp))
                        }
                      }
                    }
                  }
                }
              }
            }

            TreatmentOrderMode.WOUND_DRESSING -> {
              // === Wound Dressing Presets & Configuration ===
              Text("အမြန်ရွေးချယ်နိုင်သော ဆေးထည့်ကုသမှု ပုံစံများ (Dressing Presets):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                DRESSING_PRESETS.forEach { preset ->
                  val isSelected = medicationName.contains(preset.technique, ignoreCase = true) || medicationName == preset.fullName
                  FilterChip(
                    selected = isSelected,
                    onClick = {
                      medicationName = preset.fullName
                      dosage = preset.technique
                      sessionFeeText = preset.fee.toInt().toString()
                      frequency = preset.frequency
                      totalDaysText = preset.days.toString()
                      instructions = preset.instructions
                      selectedRoute = TreatmentRoute.DRESSING
                      if (!isPatientSupplied) medFeeText = "0"
                    },
                    label = { Text(preset.label, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFD97706),
                      selectedLabelColor = PureWhite
                    )
                  )
                }
              }

              // Procedure / Dressing Name & Details
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = medicationName,
                  onValueChange = { medicationName = it },
                  label = { Text("ကုသမှု/ဒဏ်ရာဆေးထည့်အမည် (Procedure Name)", fontSize = 11.sp) },
                  singleLine = true,
                  modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                  value = dosage,
                  onValueChange = { dosage = it },
                  label = { Text("အမျိုးအစား/အသေးစိတ် (Technique / Details)", fontSize = 11.sp) },
                  singleLine = true,
                  modifier = Modifier.weight(0.9f)
                )
              }

              // Quick technique tags
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                listOf("Aseptic Clean", "Betadine Pack", "Normal Saline Only", "Silver Sulphadiazine", "Stitch Off & Dressing", "Pressure Bandage").forEach { t ->
                  AssistChip(
                    onClick = { dosage = t },
                    label = { Text(t, fontSize = 9.5.sp) }
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    "ဒဏ်ရာဆေးထည့်ခြင်းအတွက် ဆေးဖိုး (Med Fee) မရှိပါ (0 Ks)။ ဝန်ဆောင်ခ (Service Fee) နှင့် လိုအပ်ပါက အောက်တွင် Gauze/Plaster အသုံးစာရင်း ထည့်သွင်းနိုင်ပါသည်",
                    fontSize = 9.5.sp,
                    color = Color(0xFF92400E)
                  )
                }
              }
            }
          }

          // 2.5 Manual Consumables / Supplies Selection (Doctor picks manually - No auto-attach)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, BorderGrey),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "အသုံးစာရင်း / ဆေးထိုးပစ္စည်းများ (Consumables - ${consumablesList.size} items):",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateDark
                )
                if (consumablesList.isNotEmpty()) {
                  Text(
                    "Total: ${consumablesList.sumOf { it.totalPrice }.toInt()} Ks",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = consumableSearchQuery,
                onValueChange = { consumableSearchQuery = it },
                placeholder = { Text(if (orderMode == TreatmentOrderMode.WOUND_DRESSING) "+ Store မှ ဆေးထည့်ပစ္စည်း ရှာဖွေထည့်ရန် (Gauze, Plaster, Povidone)..." else "+ Store စာရင်းမှ ဆေးထိုးအပ်/အသုံးပစ္စည်း ရှာဖွေထည့်ရန် (e.g. Syringe, Needle, Cannula)...", fontSize = 10.5.sp) },
                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                  if (consumableSearchQuery.isNotBlank()) {
                    IconButton(onClick = { consumableSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                      Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                  }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              if (filteredConsumables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = PureWhite,
                  border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(4.dp)) {
                    filteredConsumables.forEach { med ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            val existingIdx = consumablesList.indexOfFirst { it.medicineId == med.id || it.name.equals(med.name, ignoreCase = true) }
                            if (existingIdx >= 0) {
                              val cur = consumablesList[existingIdx]
                              consumablesList[existingIdx] = cur.copy(quantity = cur.quantity + 1)
                            } else {
                              consumablesList.add(
                                TreatmentConsumable(
                                  medicineId = med.id,
                                  name = med.name,
                                  quantity = 1,
                                  unitPrice = med.sellingPrice
                                )
                              )
                            }
                            consumableSearchQuery = ""
                          }
                          .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(med.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                          Text("Stock: ${med.totalStock} ${med.unitType} • Price: ${med.sellingPrice.toInt()} Ks", fontSize = 9.sp, color = SlateMuted)
                        }
                        Text("+ Add", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                      }
                      HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                    }
                  }
                }
              }

              if (consumablesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  consumablesList.forEachIndexed { idx, cons ->
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = PureWhite,
                      border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(cons.name, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                          Text("${cons.unitPrice.toInt()} Ks x ${cons.quantity} = ${cons.totalPrice.toInt()} Ks", fontSize = 9.sp, color = SlateMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          IconButton(
                            onClick = {
                              if (cons.quantity > 1) {
                                consumablesList[idx] = cons.copy(quantity = cons.quantity - 1)
                              } else {
                                consumablesList.removeAt(idx)
                              }
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                          }
                          Text("${cons.quantity}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                          IconButton(
                            onClick = {
                              consumablesList[idx] = cons.copy(quantity = cons.quantity + 1)
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                          }
                          IconButton(
                            onClick = { consumablesList.removeAt(idx) },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                          }
                        }
                      }
                    }
                  }
                }
              } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text("(ဆေးထိုးအပ်/ပစ္စည်း ထည့်သွင်းလိုပါက အထက်တွင် ရှာဖွေရွေးချယ်ပါ - မလိုပါက ကျော်နိုင်ပါသည်)", fontSize = 9.sp, color = SlateMuted)
              }
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
                onClick = {
                  selectedRoute = r
                  sessionFeeText = r.getDefaultFee(clinicSettings).toInt().toString()
                },
                label = { Text(r.label, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MedicalGreen,
                  selectedLabelColor = PureWhite
                )
              )
            }
          }

          if (selectedRoute == TreatmentRoute.CUSTOM) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = customRouteText,
              onValueChange = { customRouteText = it },
              label = { Text("စိတ်ကြိုက် နည်းလမ်းအမည် (Custom Route)", fontSize = 11.sp) },
              placeholder = { Text("e.g. Intradermal (ID), Joint Injection, Eye Drop, Enema...") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
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
            listOf("Daily (OD)", "Alternate Day (တစ်ရက်ခြား)", "Stat (1 time)", "12 hrly", "BD (2 times/day)", "TDS (3 times/day)").forEach { f ->
              ElevatedAssistChip(
                onClick = {
                  frequency = when {
                    f.startsWith("Daily") -> "Daily"
                    f.startsWith("Alternate") -> "Alternate Day"
                    f.startsWith("Stat") -> {
                      totalDaysText = "1"
                      "Stat"
                    }
                    f.startsWith("BD") -> "BD"
                    f.startsWith("12 hrly") -> "12 hrly"
                    else -> f.split(" ").first()
                  }
                },
                label = { Text(f, fontSize = 9.5.sp) }
              )
            }
          }

          // Quick Duration Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf(1, 2, 3, 5, 7, 10).forEach { d ->
              AssistChip(
                onClick = { totalDaysText = d.toString() },
                label = { Text("$d ရက် ($d Days)", fontSize = 9.5.sp) }
              )
            }
          }

          if (orderMode != TreatmentOrderMode.WOUND_DRESSING) {
            // 5. Billing Calculation Mode
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
          }

          // 6. Payment Option
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

          // 7. Fee breakdown with clear Auto-Sum of Medication + Consumables
          val currencyFmt = remember { NumberFormat.getNumberInstance(Locale.US) }
          val totalConsumablesFee = consumablesList.sumOf { it.totalPrice }
          val effectiveBaseMedFee = if (isPatientSupplied && orderMode != TreatmentOrderMode.WOUND_DRESSING) 0.0 else (medFeeText.toDoubleOrNull() ?: 0.0)
          val combinedDrugAndConsumables = effectiveBaseMedFee + totalConsumablesFee

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(if (orderMode == TreatmentOrderMode.WOUND_DRESSING) "ဆေး/လိမ်းဆေးနှင့် အသုံးပစ္စည်းဖိုး စုစုပေါင်း:" else "ဆေးနှင့် အသုံးပစ္စည်းဖိုး စုစုပေါင်း:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
                Text("${currencyFmt.format(combinedDrugAndConsumables.toInt())} Ks", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
              }
              if (totalConsumablesFee > 0) {
                Text(
                  "ဆေးဖိုး: ${currencyFmt.format(effectiveBaseMedFee.toInt())} Ks + ပစ္စည်း (${consumablesList.size} မျိုး): ${currencyFmt.format(totalConsumablesFee.toInt())} Ks",
                  fontSize = 9.5.sp,
                  color = SlateMedium
                )
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = if (isPatientSupplied && orderMode != TreatmentOrderMode.WOUND_DRESSING) "0 (လူနာပါဆေး)" else medFeeText,
              onValueChange = { if (!isPatientSupplied || orderMode == TreatmentOrderMode.WOUND_DRESSING) medFeeText = it },
              enabled = !isPatientSupplied || orderMode == TreatmentOrderMode.WOUND_DRESSING,
              label = { Text(if (orderMode == TreatmentOrderMode.WOUND_DRESSING) "ဆေး/လိမ်းဆေးဖိုး (Med Fee Ks)" else "ဆေးဖိုး (Med Fee Ks)", fontSize = 10.5.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = sessionFeeText,
              onValueChange = { sessionFeeText = it },
              label = { Text(if (orderMode == TreatmentOrderMode.WOUND_DRESSING) "ဆေးထည့်ဝန်ဆောင်ခ (Dressing Fee Ks)" else "ဆေးထိုးဝန်ဆောင်ခ (Service Ks)", fontSize = 10.5.sp) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          // 8. Instructions
          OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = {
              Text(
                when (orderMode) {
                  TreatmentOrderMode.WOUND_DRESSING -> "ဆေးထည့်နည်း/ညွှန်ကြားချက် (Dressing Instructions & Dressing Pack Details)"
                  TreatmentOrderMode.COCKTAIL -> "ဆေးထိုးညွှန်ကြားချက် (Special Instructions / Drip Rate)"
                  else -> "အပိုဆောင်း ညွှန်ကြားချက် / မှတ်ချက် (Additional Remarks / Instructions)"
                },
                fontSize = 11.sp
              )
            },
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

          val canConfirm = when (orderMode) {
            TreatmentOrderMode.COCKTAIL -> cocktailTitle.isNotBlank() || cocktailItems.isNotEmpty()
            TreatmentOrderMode.WOUND_DRESSING -> medicationName.isNotBlank()
            TreatmentOrderMode.SINGLE_DRUG -> medicationName.isNotBlank()
          }

          Button(
            onClick = {
              val itemsSummary = if (isCocktail && cocktailItems.isNotEmpty()) {
                cocktailItems.joinToString(" + ") { "${it.name} ${it.dosage}".trim() }
              } else ""
              val finalMedName = when (orderMode) {
                TreatmentOrderMode.COCKTAIL -> cocktailTitle.ifBlank {
                  if (itemsSummary.isNotBlank()) itemsSummary
                  else "IV Cocktail Mix"
                }
                TreatmentOrderMode.WOUND_DRESSING -> medicationName.ifBlank { "Regular Clean Dressing (ဒဏ်ရာဆေးထည့်)" }
                TreatmentOrderMode.SINGLE_DRUG -> medicationName
              }
              val finalDosage = when (orderMode) {
                TreatmentOrderMode.COCKTAIL -> {
                  if (itemsSummary.isNotBlank()) "${cocktailItems.size} Drugs ($itemsSummary)"
                  else "${cocktailItems.size} Drugs Mixed"
                }
                TreatmentOrderMode.WOUND_DRESSING -> dosage.ifBlank { "Aseptic Clean Dressing" }
                TreatmentOrderMode.SINGLE_DRUG -> dosage
              }

              val sessions = TreatmentCardFactory.generateSessions(
                startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                frequency = frequency.ifBlank { "Stat" },
                days = days,
                medicationFee = medFee,
                sessionFee = sessionFee,
                splitVialMode = splitVialMode,
                consumables = consumablesList.toList()
              )
              val finalSessions = sessions.mapIndexed { idx, s ->
                val prev = initialCard?.sessions?.getOrNull(idx)
                if (prev != null) {
                  prev.copy(
                    medicationFee = medFee,
                    sessionFee = sessionFee,
                    consumables = consumablesList.toList(),
                    isPrepaid = !isPayPerVisit
                  )
                } else {
                  if (!isPayPerVisit) s.copy(isPrepaid = true) else s
                }
              }
              val card = (initialCard ?: TreatmentCard(patientId = patient?.id ?: "", patientName = patient?.name ?: "", medicationName = finalMedName)).copy(
                patientId = patient?.id ?: (initialCard?.patientId ?: ""),
                patientName = patient?.name ?: (initialCard?.patientName ?: ""),
                patientHn = patient?.hn ?: (initialCard?.patientHn ?: ""),
                doctorName = doctorName.ifBlank { initialCard?.doctorName ?: "" },
                medicationId = selectedMedId,
                medicationName = finalMedName,
                dosage = finalDosage,
                route = selectedRoute,
                customRoute = if (selectedRoute == TreatmentRoute.CUSTOM) customRouteText.trim() else "",
                frequency = frequency.ifBlank { "Stat" },
                totalDays = days,
                totalSessions = finalSessions.size,
                sessions = finalSessions,
                sessionFee = sessionFee,
                medicationFee = medFee,
                instructions = instructions,
                isPayPerVisit = isPayPerVisit,
                isPrepaidFullCourse = !isPayPerVisit,
                isCocktail = isCocktail,
                cocktailItems = if (isCocktail) cocktailItems.toList() else emptyList(),
                consumables = consumablesList.toList(),
                isPatientSupplied = isPatientSupplied
              )
              onConfirm(card)
              onDismiss()
            },
            enabled = canConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            modifier = Modifier
              .weight(1.3f)
              .height(40.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (initialCard != null) "ပြင်ဆင်ချက် သိမ်းမည် (Update Order)" else "အော်ဒါအတည်ပြုမည် (Confirm Order)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
