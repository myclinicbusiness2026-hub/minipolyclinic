package com.example.ui.screens.consultation

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MedicalLeaveRecord
import com.example.model.Patient
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ---------------- MEDICAL LEAVE MODAL ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalLeaveModal(
  patient: Patient?,
  initialRecord: MedicalLeaveRecord?,
  diagnosisDefault: String,
  onDismiss: () -> Unit,
  onSave: (MedicalLeaveRecord) -> Unit,
  onRemove: () -> Unit
) {
  val defaultDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
  var issueDate by remember { mutableStateOf(initialRecord?.issueDate ?: defaultDateStr) }
  var startDateStr by remember { mutableStateOf(initialRecord?.startDate ?: defaultDateStr) }
  var days by remember { mutableStateOf(initialRecord?.leaveDays?.toString() ?: "3") }
  var diagnosis by remember { mutableStateOf(initialRecord?.diagnosis ?: diagnosisDefault.ifBlank { "Acute Condition" }) }
  var fitnessStatus by remember { mutableStateOf(initialRecord?.fitnessStatus ?: "Unfit for Duty / Complete Rest Required") }
  var remarks by remember { mutableStateOf(initialRecord?.remarks ?: "Patient is advised complete physical rest and medication adherence.") }

  val numDays = (days.toIntOrNull() ?: 3).coerceAtLeast(1)
  val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
  val endDateStr = remember(startDateStr, numDays) {
    try {
      val parsed = sdf.parse(startDateStr) ?: Date()
      sdf.format(Date(parsed.time + (numDays - 1).coerceAtLeast(0).toLong() * 24 * 3600 * 1000))
    } catch (e: Exception) {
      sdf.format(Date(System.currentTimeMillis() + (numDays - 1).coerceAtLeast(0).toLong() * 24 * 3600 * 1000))
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(16.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Badge, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Medical Leave Certificate", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = DarkGrayText)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "Official Medical Certificate for ${patient?.name ?: "Patient"} (${patient?.hn ?: ""})",
          fontSize = 11.sp,
          color = SlateMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Days Selector Chips
        Text("Leave Duration:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("1", "2", "3", "5", "7", "14").forEach { d ->
            FilterChip(
              selected = days == d,
              onClick = { days = d },
              label = { Text("$d Days", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MedicalGreen,
                selectedLabelColor = PureWhite
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = days,
          onValueChange = { days = it.filter { c -> c.isDigit() } },
          label = { Text("Custom Days", fontSize = 10.sp) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Modifiable Dates
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = issueDate,
            onValueChange = { issueDate = it },
            label = { Text("Issued Date", fontSize = 9.5.sp) },
            placeholder = { Text("dd/MM/yyyy", fontSize = 9.5.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
          OutlinedTextField(
            value = startDateStr,
            onValueChange = { startDateStr = it },
            label = { Text("Leave From", fontSize = 9.5.sp) },
            placeholder = { Text("dd/MM/yyyy", fontSize = 9.5.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = clinicTextFieldColors()
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = SoftGreenContainer,
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("From: $startDateStr", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MedicalGreenDark)
            Text("To: $endDateStr ($numDays Days)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedicalGreenDark)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Diagnosis / Clinical Reason:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = diagnosis,
          onValueChange = { diagnosis = it },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors()
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text("Fitness Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Spacer(modifier = Modifier.height(4.dp))
        listOf(
          "Unfit for Duty / Complete Rest Required",
          "Fit for Light Duty Only",
          "Fit to Resume Normal Duties"
        ).forEach { status ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { fitnessStatus = status }
              .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = fitnessStatus == status,
              onClick = { fitnessStatus = status },
              colors = RadioButtonDefaults.colors(selectedColor = MedicalGreen),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(status, fontSize = 10.sp, color = DarkGrayText)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Doctor Remarks / Advice:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2,
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (initialRecord != null) {
            OutlinedButton(
              onClick = {
                onRemove()
                onDismiss()
              },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
              border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
              modifier = Modifier.weight(1f).height(38.dp),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Remove", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
          }

          Button(
            onClick = {
              val newRec = MedicalLeaveRecord(
                id = initialRecord?.id ?: UUID.randomUUID().toString(),
                certificateNo = initialRecord?.certificateNo ?: "MC-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(1000..9999).random()}",
                patientName = patient?.name ?: "Patient",
                patientAge = patient?.age ?: 30,
                patientGender = patient?.gender?.name ?: "Male",
                patientHn = patient?.hn ?: "HN-2026-0081",
                diagnosis = diagnosis,
                leaveDays = numDays,
                startDate = startDateStr,
                endDate = endDateStr,
                fitnessStatus = fitnessStatus,
                remarks = remarks,
                issueDate = issueDate
              )
              onSave(newRec)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            modifier = Modifier.weight(1.2f).height(38.dp),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(if (initialRecord != null) "Update Certificate" else "Issue Certificate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
          }
        }
      }
    }
  }
}

// ---------------- PATIENT INSTRUCTIONS & ADVICE MULTI-PRESET SYSTEM ----------------

data class InstructionPresetItem(
  val id: String,
  val code: String,
  val name: String,
  val category: String,
  val text: String,
  val isCustom: Boolean = false
)

val DEFAULT_INSTRUCTION_PRESETS = listOf(
  InstructionPresetItem(
    id = "inst-1",
    code = "1",
    name = "ဖျားနာ/ပိုးဝင်ခြင်း",
    category = "General Fever & Infection",
    text = """၁။ ဆရာဝန်ညွှန်ကြားသည့်အတိုင်း ဆေးများကို အချိန်မှန်၊ ရက်ပြည့်အောင် သောက်သုံးပါ။ ရေများများသောက်ပါ။
၂။ ပဋိဇီဝဆေး (Antibiotics) များကို ရောဂါသက်သာသော်လည်း ရက်ပြည့်အောင် ဆက်သောက်ပါ။
၃။ အဖျားတက်ပါက ရေပတ်တိုက်ပေးပါ၊ အနားယူပါ။
၄။ ဆေးသောက်ပြီး ဓါတ်မတည့်ခြင်း (သို့) ထူးခြားသော လက္ခဏာများ ခံစားရပါက ဆေးခန်းသို့ ချက်ချင်း ပြန်လည်ပြသပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-2",
    code = "2",
    name = "သွေးတိုးရောဂါ",
    category = "Hypertension Advice",
    text = """၁။ သွေးတိုးကျဆေးကို နေ့စဉ် အချိန်မှန် ပုံမှန်သောက်သုံးပါ (ဆေးမပြတ်ပါစေနှင့်)။
၂။ အငန် (ဆား၊ ငံပြာရည်၊ ငါးပိ၊ အချိုမှုန့်) နှင့် အဆီအအမ့် လျှော့စားပါ။
၃။ သွေးပေါင်ချိန်ကို ပုံမှန်တိုင်းတာ မှတ်သားထားပါ။
၄။ ခေါင်းမူး၊ ခေါင်းကိုက်၊ မျက်စိပြာခြင်း၊ ရင်တုန်ခြင်း ဖြစ်ပါက ဆေးခန်းသို့ ချက်ချင်း ပြသပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-3",
    code = "3",
    name = "ဆီးချိုရောဂါ",
    category = "Diabetes Advice",
    text = """၁။ ဆီးချိုဆေးများကို ဆရာဝန်ညွှန်ကြားသည့် အချိန် (အစာမစားမီ/အစာစားပြီး) အတိအကျ သောက်သုံးပါ။
၂။ အချိုရည်၊ အချိုများသော အသီးအနှံနှင့် ကစီဓာတ်များသော အစားအစာများကို လျှော့စားပါ။
၃။ ခြေထောက်ဒဏ်ရာ မဖြစ်အောင် သတိပြုပါ၊ ခြေလက်သန့်ရှင်းရေး အထူးဂရုစိုက်ပါ။
၄။ သွေးတွင်းသကြားဓာတ် အလွန်ကျဆင်းခြင်း (ချွေးစေးပြန်၊ တုန်ရင်၊ သတိလစ်ချင်သလိုဖြစ်ခြင်း) ဖြစ်ပါက အချိုရည် ချက်ချင်းသောက်ပြီး ဆေးရုံ/ဆေးခန်းသို့ အမြန်ပြသပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-4",
    code = "4",
    name = "အစာအိမ် & GERD",
    category = "Gastritis & Acid Reflux",
    text = """၁။ အစာကို အချိန်မှန် စားသုံးပါ၊ အစာလွန်အောင် မနေပါနှင့်။
၂။ အစပ်၊ အချဉ်၊ အဆီအစိမ့်၊ ကော်ဖီ၊ လက်ဖက်ရည်နှင့် အရက်/ဆေးလိပ် ရှောင်ကြဉ်ပါ။
၃။ အစာစားပြီးပြီးချင်း ချက်ချင်း မလှဲပါနှင့် (အနည်းဆုံး ၂ နာရီခန့် ခြားပါ)။
၄။ အစာအိမ်ဆေးများကို ဆရာဝန်ညွှန်ကြားသည့်အတိုင်း အစာမစားမီ ကြိုတင်သောက်သုံးပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-5",
    code = "5",
    name = "ချောင်းဆိုး & ပန်းနာ",
    category = "URI & Respiratory Care",
    text = """၁။ ရေနွေးနွေး များများသောက်ပါ၊ ဆားရည်ဖြင့် ပလုတ်ကျင်းပေးပါ။
၂။ အအေးစာ၊ ရေခဲရေ၊ အကြော်အလှော်၊ ဖုန်မှုန့်နှင့် မီးခိုးငွေ့များ ရှောင်ကြဉ်ပါ။
၃။ ရှူဆေး (Inhaler) သုံးစွဲပုံကို စနစ်တကျ လိုက်နာပါ၊ ချောင်းဆိုးမသက်သာပါက ပြန်ပြပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-6",
    code = "6",
    name = "ဒဏ်ရာ & ချုပ်ရိုး",
    category = "Wound & Stitch Care",
    text = """၁။ ချုပ်ရိုး/ဒဏ်ရာကို ရေမထိအောင် ခြောက်သွေ့သန့်ရှင်းစွာ ထားရှိပါ။
၂။ ဆေးထည့်ရန် ရက်ချိန်းအတိုင်း ဆေးခန်းသို့ ပုံမှန် လာရောက်ပြသပါ။
၃။ ဒဏ်ရာ နီရဲရောင်ရမ်းခြင်း၊ ပြည်ယိုခြင်း၊ အဖျားတက်ခြင်း ဖြစ်ပေါ်ပါက ချက်ချင်း ဆေးခန်းပြပါ။
၄။ ချုပ်ရိုးဖြည်ရန် ရက်ချိန်း (၅-၇ ရက်) တွင် ပြန်လည်ပြသပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-7",
    code = "7",
    name = "ကလေးလူနာ",
    category = "Pediatric Care",
    text = """၁။ ကလေးဆေးရည်များကို ဆေးညွှန်းပါ ပမာဏအတိုင်း ဇွန်း/ပြွတ်ဖြင့် တိကျစွာ တိုက်ကျွေးပါ။
၂။ အဖျားတက်ပါက အဝတ်ပါးပါး ဝတ်ဆင်ပေးပြီး ရေပတ်တိုက်ပေးပါ။
၃။ မိခင်နို့နှင့် ရေ/အရည် များများတိုက်ကျွေးပါ၊ မှိန်းခြင်း၊ အန်ခြင်း ရှိပါက ချက်ချင်း ဆေးရုံ/ဆေးခန်း ပြသပါ။"""
  ),
  InstructionPresetItem(
    id = "inst-8",
    code = "8",
    name = "ကျန်းမာရေးနေထိုင်မှု",
    category = "Healthy Lifestyle",
    text = """၁။ တစ်နေ့လျှင် ရေ ၂ လီတာမှ ၃ လီတာခန့် လုံလောက်စွာ သောက်သုံးပါ။
၂။ တစ်နေ့လျှင် အနည်းဆုံး ၇-၈ နာရီ ပြည့်ဝစွာ အိပ်စက်အနားယူပါ။
၃။ လတ်ဆတ်သော အသီးအရွက်များ များများစားသုံးပြီး အသင့်စားအစားအစာများ လျှော့စားပါ။
၄။ တစ်ပတ်လျှင် အနည်းဆုံး ၃-၄ ရက် လမ်းလျှောက်ခြင်း (သို့) ကိုယ်လက်လှုပ်ရှားမှု ပြုလုပ်ပါ။"""
  )
)

val customInstructionPresets = mutableStateListOf<InstructionPresetItem>()

fun loadCustomInstructionPresets(context: Context): List<InstructionPresetItem> {
  return try {
    val sp = context.getSharedPreferences("shwekhit_instruction_presets", Context.MODE_PRIVATE)
    val raw = sp.getString("presets_json", null) ?: return emptyList()
    val arr = org.json.JSONArray(raw)
    val list = mutableListOf<InstructionPresetItem>()
    for (i in 0 until arr.length()) {
      val obj = arr.getJSONObject(i)
      list.add(
        InstructionPresetItem(
          id = obj.getString("id"),
          code = obj.optString("code", "${i + 1}"),
          name = obj.getString("name"),
          category = obj.optString("category", "Custom Advice"),
          text = obj.getString("text"),
          isCustom = true
        )
      )
    }
    list
  } catch (e: Exception) {
    emptyList()
  }
}

fun saveCustomInstructionPresets(context: Context, presets: List<InstructionPresetItem>) {
  try {
    val sp = context.getSharedPreferences("shwekhit_instruction_presets", Context.MODE_PRIVATE)
    val arr = org.json.JSONArray()
    presets.filter { it.isCustom }.forEach { p ->
      val obj = org.json.JSONObject()
      obj.put("id", p.id)
      obj.put("code", p.code)
      obj.put("name", p.name)
      obj.put("category", p.category)
      obj.put("text", p.text)
      arr.put(obj)
    }
    sp.edit().putString("presets_json", arr.toString()).apply()
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatientInstructionsModal(
  instructions: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit
) {
  val context = LocalContext.current
  var text by remember { mutableStateOf(instructions) }
  var selectedPresetCode by remember { mutableStateOf<String?>(null) }
  var selectedCategoryFilter by remember { mutableStateOf("All") }
  var appendMode by remember { mutableStateOf(false) }
  var showAddCustomDialog by remember { mutableStateOf(false) }

  var customTitle by remember { mutableStateOf("") }
  var customCategory by remember { mutableStateOf("") }
  var customBody by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    val saved = loadCustomInstructionPresets(context)
    if (saved.isNotEmpty() && customInstructionPresets.isEmpty()) {
      customInstructionPresets.addAll(saved)
    }
  }

  val allPresets = remember(customInstructionPresets.size) {
    DEFAULT_INSTRUCTION_PRESETS + customInstructionPresets
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PureWhite,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp)
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Modal Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE8F5E9)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Notes, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Patient Instructions & Advice", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkGrayText)
              Text("လူနာအမျိုးအစားအလိုက် ကြိုတင်ရေးထားသော ညွှန်ကြားချက်များ", fontSize = 10.sp, color = SlateMedium)
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMedium)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Instructions Selector Box
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF8FAFC),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "လူနာအမျိုးအစားအလိုက် ရွေးချယ်ရန် (Instruction 1, 2, 3, 4...):",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MedicalGreenDark
              )

              // Mode toggle: Replace vs Append
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (appendMode) "ဆက်လက်ထည့်မည် (+)" else "အစားထိုးမည်", fontSize = 9.5.sp, color = if (appendMode) Color(0xFF1565C0) else SlateDark, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                  checked = appendMode,
                  onCheckedChange = { appendMode = it },
                  modifier = Modifier.height(20.dp),
                  colors = SwitchDefaults.colors(checkedThumbColor = MedicalGreen, checkedTrackColor = SoftGreenContainer)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter / Selection Chips
            val categories = remember(allPresets.size) {
              listOf("All") + allPresets.map { it.category }.distinct()
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              categories.forEach { cat ->
                val isCatSelected = selectedCategoryFilter == cat
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (isCatSelected) MedicalGreen else Color(0xFFE2E8F0),
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selectedCategoryFilter = cat }
                ) {
                  Text(
                    cat,
                    fontSize = 9.sp,
                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCatSelected) PureWhite else DarkGrayText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset Buttons FlowRow
            val filteredPresets = if (selectedCategoryFilter == "All") allPresets else allPresets.filter { it.category == selectedCategoryFilter }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              filteredPresets.forEach { preset ->
                val isSelected = selectedPresetCode == preset.code
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    selectedPresetCode = preset.code
                    if (appendMode && text.isNotBlank()) {
                      text = text.trim() + "\n\n" + preset.text.trim()
                    } else {
                      text = preset.text.trim()
                    }
                  },
                  label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        "${preset.code}. ${preset.name}",
                        fontSize = 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                      )
                      if (preset.isCustom) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                          Icons.Default.Close,
                          contentDescription = "Delete custom template",
                          tint = Color(0xFFD32F2F),
                          modifier = Modifier
                            .size(11.dp)
                            .clickable {
                              customInstructionPresets.removeAll { it.id == preset.id }
                              saveCustomInstructionPresets(context, customInstructionPresets)
                              if (selectedPresetCode == preset.code) selectedPresetCode = null
                            }
                        )
                      }
                    }
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (preset.isCustom) Color(0xFF1565C0) else MedicalGreen,
                    selectedLabelColor = PureWhite,
                    containerColor = if (preset.isCustom) Color(0xFFE3F2FD) else Color(0xFFF1F5F9),
                    labelColor = if (preset.isCustom) Color(0xFF0D47A1) else DarkGrayText
                  ),
                  modifier = Modifier.height(28.dp)
                )
              }

              // Add New Custom Template Chip
              FilterChip(
                selected = false,
                onClick = { showAddCustomDialog = true },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ New Template", fontSize = 9.5.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                  }
                },
                modifier = Modifier.height(28.dp),
                border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = Color(0xFFE3F2FD),
                  labelColor = Color(0xFF1565C0)
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Editor Area
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "ညွှန်ကြားချက် အပြည့်အစုံ (A4 ဆေးမှတ်တမ်းတွင် ဖော်ပြပါမည်):",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkGrayText
          )
          if (text.isNotBlank()) {
            TextButton(
              onClick = { text = ""; selectedPresetCode = null },
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
              modifier = Modifier.height(22.dp)
            ) {
              Text("ရှင်းမည် (Clear)", fontSize = 9.sp, color = Color(0xFFD32F2F))
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 220.dp),
          shape = RoundedCornerShape(6.dp),
          colors = clinicTextFieldColors(),
          textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = DarkGrayText, lineHeight = 16.sp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
          placeholder = {
            Text("အထက်ပါ Instruction 1, 2, 3, 4 စသည်တို့မှ ရွေးချယ်ပါ သို့မဟုတ် စိတ်ကြိုက် ရိုက်ထည့်ပါ...", fontSize = 10.5.sp, color = SlateMuted)
          }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Action Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Text("Cancel", fontSize = 11.sp, color = SlateDark)
          }

          Button(
            onClick = {
              onSave(text.trim())
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Instructions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
          }
        }
      }
    }
  }

  // Dialog for Adding New Custom Preset
  if (showAddCustomDialog) {
    AlertDialog(
      onDismissRequest = { showAddCustomDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AddCircle, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Custom Instruction Template", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = customTitle,
            onValueChange = { customTitle = it },
            label = { Text("Template Name (ဥပမာ- အရေပြားယားယံခြင်း)", fontSize = 10.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = customCategory,
            onValueChange = { customCategory = it },
            label = { Text("Category / Patient Type (e.g. Dermatology)", fontSize = 10.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = customBody,
            onValueChange = { customBody = it },
            label = { Text("Instruction Advice Content (ညွှန်ကြားချက်များ)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customTitle.isNotBlank() && customBody.isNotBlank()) {
              val nextNumber = (allPresets.size + 1).toString()
              val newPreset = InstructionPresetItem(
                id = "custom-${System.currentTimeMillis()}",
                code = nextNumber,
                name = customTitle.trim(),
                category = customCategory.trim().ifBlank { "Custom Advice" },
                text = customBody.trim(),
                isCustom = true
              )
              customInstructionPresets.add(newPreset)
              saveCustomInstructionPresets(context, customInstructionPresets)
              if (appendMode && text.isNotBlank()) {
                text = text.trim() + "\n\n" + customBody.trim()
              } else {
                text = customBody.trim()
              }
              customTitle = ""
              customCategory = ""
              customBody = ""
              showAddCustomDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          enabled = customTitle.isNotBlank() && customBody.isNotBlank()
        ) {
          Text("Save Template", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCustomDialog = false }) {
          Text("Cancel", fontSize = 11.sp)
        }
      }
    )
  }
}
