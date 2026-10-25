package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Categorized Master Clinical Diagnosis Presets
 * Covering common general outpatient / primary care consultations.
 */
data class DiagnosisItem(
  val name: String,
  val category: String,
  val code: String = ""
)

object ClinicalDiagnosisLibrary {
  val categories = listOf(
    "All",
    "Respiratory",
    "GI & Abdomen",
    "Cardiovascular",
    "Endocrine",
    "Musculoskeletal",
    "Dermatology",
    "Infectious",
    "Trauma & Minor",
    "Neurology & Other"
  )

  val library: List<DiagnosisItem> = listOf(
    // Respiratory
    DiagnosisItem("Viral URI (Common Cold)", "Respiratory", "J06.9"),
    DiagnosisItem("Acute Bronchitis", "Respiratory", "J20.9"),
    DiagnosisItem("Bronchial Asthma", "Respiratory", "J45.9"),
    DiagnosisItem("COPD Acute Exacerbation", "Respiratory", "J44.1"),
    DiagnosisItem("Acute Tonsillitis", "Respiratory", "J03.9"),
    DiagnosisItem("Acute Pharyngitis", "Respiratory", "J02.9"),
    DiagnosisItem("Acute Sinusitis", "Respiratory", "J01.9"),
    DiagnosisItem("Allergic Rhinitis", "Respiratory", "J30.4"),
    DiagnosisItem("Community Acquired Pneumonia", "Respiratory", "J18.9"),
    DiagnosisItem("Croup / Laryngotracheitis", "Respiratory", "J05.0"),

    // GI & Abdomen
    DiagnosisItem("Acute Gastroenteritis (AGE)", "GI & Abdomen", "A09"),
    DiagnosisItem("Gastritis / Dyspepsia", "GI & Abdomen", "K29.7"),
    DiagnosisItem("Gastroesophageal Reflux (GERD)", "GI & Abdomen", "K21.9"),
    DiagnosisItem("Peptic Ulcer Disease (PUD)", "GI & Abdomen", "K27.9"),
    DiagnosisItem("Irritable Bowel Syndrome (IBS)", "GI & Abdomen", "K58.9"),
    DiagnosisItem("Constipation", "GI & Abdomen", "K59.0"),
    DiagnosisItem("Hemorrhoids", "GI & Abdomen", "K64.9"),
    DiagnosisItem("Fatty Liver Disease (NAFLD)", "GI & Abdomen", "K76.0"),
    DiagnosisItem("Acute Viral Hepatitis", "GI & Abdomen", "B19.9"),
    DiagnosisItem("Aphthous Stomatitis", "GI & Abdomen", "K12.0"),
    DiagnosisItem("Cholelithiasis (Gallstones)", "GI & Abdomen", "K80.2"),

    // Cardiovascular
    DiagnosisItem("Essential Hypertension", "Cardiovascular", "I10"),
    DiagnosisItem("Hypertensive Heart Disease", "Cardiovascular", "I11.9"),
    DiagnosisItem("Ischemic Heart Disease (IHD)", "Cardiovascular", "I25.9"),
    DiagnosisItem("Palpitation / Tachycardia", "Cardiovascular", "R00.2"),
    DiagnosisItem("Congestive Heart Failure", "Cardiovascular", "I50.9"),
    DiagnosisItem("Dyslipidemia / Hypercholesterolemia", "Cardiovascular", "E78.5"),
    DiagnosisItem("Postural Hypotension", "Cardiovascular", "I95.1"),
    DiagnosisItem("Peripheral Edema", "Cardiovascular", "R60.0"),

    // Endocrine & Metabolic
    DiagnosisItem("Type 2 Diabetes Mellitus", "Endocrine", "E11.9"),
    DiagnosisItem("Type 1 Diabetes Mellitus", "Endocrine", "E10.9"),
    DiagnosisItem("Diabetic Neuropathy", "Endocrine", "E11.4"),
    DiagnosisItem("Hyperuricemia / Gout", "Endocrine", "M10.9"),
    DiagnosisItem("Hypothyroidism", "Endocrine", "E03.9"),
    DiagnosisItem("Hyperthyroidism / Thyrotoxicosis", "Endocrine", "E05.9"),
    DiagnosisItem("Metabolic Syndrome / Obesity", "Endocrine", "E66.9"),
    DiagnosisItem("Vitamin D Deficiency", "Endocrine", "E55.9"),

    // Musculoskeletal
    DiagnosisItem("Osteoarthritis (OA Knees)", "Musculoskeletal", "M17.9"),
    DiagnosisItem("Lumbar Spondylosis / Low Back Pain", "Musculoskeletal", "M54.5"),
    DiagnosisItem("Cervical Spondylosis / Neck Strain", "Musculoskeletal", "M47.8"),
    DiagnosisItem("Acute Muscle Sprain / Strain", "Musculoskeletal", "T14.3"),
    DiagnosisItem("Tension Myalgia", "Musculoskeletal", "M79.1"),
    DiagnosisItem("Plantar Fasciitis", "Musculoskeletal", "M72.2"),
    DiagnosisItem("Frozen Shoulder (Adhesive Capsulitis)", "Musculoskeletal", "M75.0"),
    DiagnosisItem("Carpal Tunnel Syndrome", "Musculoskeletal", "G56.0"),
    DiagnosisItem("Trigger Finger", "Musculoskeletal", "M65.3"),
    DiagnosisItem("Rheumatoid Arthritis", "Musculoskeletal", "M06.9"),

    // Dermatology
    DiagnosisItem("Acute Urticaria", "Dermatology", "L50.9"),
    DiagnosisItem("Contact Dermatitis (Allergic/Irritant)", "Dermatology", "L23.9"),
    DiagnosisItem("Atopic Eczema", "Dermatology", "L20.9"),
    DiagnosisItem("Tinea Corporis / Fungal Infection", "Dermatology", "B35.4"),
    DiagnosisItem("Bacterial Folliculitis / Furuncle", "Dermatology", "L73.9"),
    DiagnosisItem("Impetigo", "Dermatology", "L01.0"),
    DiagnosisItem("Cellulitis / Abscess", "Dermatology", "L03.9"),
    DiagnosisItem("Insect Bite Reaction", "Dermatology", "W57"),
    DiagnosisItem("Scabies", "Dermatology", "B86"),
    DiagnosisItem("Pityriasis Versicolor", "Dermatology", "B36.0"),
    DiagnosisItem("Acne Vulgaris", "Dermatology", "L70.0"),
    DiagnosisItem("Drug Rash / Exanthem", "Dermatology", "L27.0"),

    // Infectious & Tropical
    DiagnosisItem("Acute Viral Fever", "Infectious", "B34.9"),
    DiagnosisItem("Dengue Fever (DF)", "Infectious", "A90"),
    DiagnosisItem("Enteric Fever (Typhoid)", "Infectious", "A01.0"),
    DiagnosisItem("Urinary Tract Infection (UTI)", "Infectious", "N39.0"),
    DiagnosisItem("Acute Pyelonephritis", "Infectious", "N10"),
    DiagnosisItem("Malaria (P. vivax/falciparum)", "Infectious", "B54"),
    DiagnosisItem("Varicella (Chickenpox)", "Infectious", "B01.9"),
    DiagnosisItem("Herpes Zoster (Shingles)", "Infectious", "B02.9"),
    DiagnosisItem("Helminthiasis (Worm Infestation)", "Infectious", "B82.9"),

    // Trauma & Minor Surgical
    DiagnosisItem("Abrasion / Laceration Wound", "Trauma & Minor", "T14.1"),
    DiagnosisItem("Contusion / Soft Tissue Trauma", "Trauma & Minor", "T14.0"),
    DiagnosisItem("Burn Injury (1st/2nd Degree)", "Trauma & Minor", "T30.0"),
    DiagnosisItem("Animal / Dog Bite", "Trauma & Minor", "W54"),
    DiagnosisItem("Subungual Hematoma / Nail Trauma", "Trauma & Minor", "S60.0"),
    DiagnosisItem("Sprained Ankle", "Trauma & Minor", "S93.4"),

    // Neurology & Other
    DiagnosisItem("Tension-Type Headache", "Neurology & Other", "G44.2"),
    DiagnosisItem("Migraine with/without Aura", "Neurology & Other", "G43.9"),
    DiagnosisItem("Vertigo / BPPV", "Neurology & Other", "H81.1"),
    DiagnosisItem("Peripheral Facial Palsy (Bell's)", "Neurology & Other", "G51.0"),
    DiagnosisItem("Sciatica / Radiculopathy", "Neurology & Other", "M54.3"),
    DiagnosisItem("Generalized Anxiety Disorder", "Neurology & Other", "F41.1"),
    DiagnosisItem("Insomnia Disorder", "Neurology & Other", "G47.0"),
    DiagnosisItem("Acute Otitis Media (AOM)", "Neurology & Other", "H66.9"),
    DiagnosisItem("Iron Deficiency Anemia", "Neurology & Other", "D50.9")
  )
}

/**
 * ClinicalDiagnosisSection
 *
 * Allows doctor to:
 * 1. Type custom clinical diagnosis freely with immediate "+ Add" or Enter
 * 2. Search & filter through 80+ categorized common medical presets
 * 3. Review, manage, and tap-to-remove active diagnoses
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClinicalDiagnosisSection(
  selectedDiagnoses: SnapshotStateList<String>,
  modifier: Modifier = Modifier
) {
  var customDiagnosisInput by remember { mutableStateOf("") }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var isPresetBrowserExpanded by remember { mutableStateOf(false) }

  fun addCustomDiagnosis() {
    val trimmed = customDiagnosisInput.trim()
    if (trimmed.isNotBlank() && !selectedDiagnoses.contains(trimmed)) {
      selectedDiagnoses.add(trimmed)
      customDiagnosisInput = ""
    }
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Top Bar: Header & Counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.LocalHospital,
            contentDescription = null,
            tint = MedicalGreen,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Clinical Diagnosis",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = DarkGrayText
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (selectedDiagnoses.isNotEmpty()) SoftGreenContainer else Color(0xFFF1F5F9),
          border = BorderStroke(0.6.dp, if (selectedDiagnoses.isNotEmpty()) Color(0xFFA5D6A7) else BorderGray)
        ) {
          Text(
            text = if (selectedDiagnoses.isNotEmpty()) "${selectedDiagnoses.size} Diagnosed" else "No Diagnosis Set",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selectedDiagnoses.isNotEmpty()) MedicalGreenDark else SlateMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 1. Doctor's Custom Free-Text Typing Input Field
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = customDiagnosisInput,
          onValueChange = { customDiagnosisInput = it },
          placeholder = {
            Text("Doctor's typing: Type any diagnosis...", fontSize = 12.sp, color = SlateMuted)
          },
          singleLine = true,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(6.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { addCustomDiagnosis() }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MedicalGreen,
            unfocusedBorderColor = BorderGray
          )
        )

        Button(
          onClick = { addCustomDiagnosis() },
          enabled = customDiagnosisInput.isNotBlank(),
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.height(48.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      // 2. Active Selected Diagnoses Chips (Doctor can review & tap-to-remove)
      if (selectedDiagnoses.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          "Active Diagnoses (Tap ✕ to remove):",
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          color = SlateDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          selectedDiagnoses.forEach { diag ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFE8F5E9),
              border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
              modifier = Modifier.clickable { selectedDiagnoses.remove(diag) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  diag,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MedicalGreenDark
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Remove",
                  modifier = Modifier.size(13.dp),
                  tint = MedicalGreenDark
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 3. Wide-Range Library & Search Toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isPresetBrowserExpanded = !isPresetBrowserExpanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            if (isPresetBrowserExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = SkyBluePrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            if (isPresetBrowserExpanded) "Hide Presets & ICD Library" else "Browse Wide-Range Presets Library (80+ Conditions)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SkyBluePrimary
          )
        }

        if (!isPresetBrowserExpanded) {
          Text(
            "Tap to search presets",
            fontSize = 9.5.sp,
            color = SlateMuted
          )
        }
      }

      if (isPresetBrowserExpanded) {
        Spacer(modifier = Modifier.height(6.dp))

        // Search Bar for Presets
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Filter presets (e.g. asthma, gastritis, diabetes...)", fontSize = 11.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = SlateMedium) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = BorderGray,
            focusedBorderColor = SkyBluePrimary
          )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          ClinicalDiagnosisLibrary.categories.forEach { cat ->
            val isCatSelected = selectedCategory == cat
            FilterChip(
              selected = isCatSelected,
              onClick = { selectedCategory = cat },
              label = { Text(cat, fontSize = 9.5.sp) },
              modifier = Modifier.height(26.dp),
              shape = RoundedCornerShape(4.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SkyBlueLight,
                selectedLabelColor = SkyBlueDark
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filtered Preset Items
        val filteredPresets = remember(searchQuery, selectedCategory) {
          ClinicalDiagnosisLibrary.library.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.code.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
            .border(0.6.dp, BorderGray, RoundedCornerShape(6.dp))
            .padding(6.dp)
        ) {
          if (filteredPresets.isEmpty()) {
            Text(
              "No presets match \"$searchQuery\". You can type it above and press Add!",
              fontSize = 10.5.sp,
              color = SlateMuted,
              modifier = Modifier.align(Alignment.Center)
            )
          } else {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              filteredPresets.forEach { item ->
                val isSelected = selectedDiagnoses.contains(item.name)
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    if (isSelected) {
                      selectedDiagnoses.remove(item.name)
                    } else {
                      selectedDiagnoses.add(item.name)
                    }
                  },
                  label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(item.name, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                      if (item.code.isNotBlank()) {
                        Text(" [${item.code}]", fontSize = 8.5.sp, color = if (isSelected) SkyBlueDark else SlateMuted)
                      }
                    }
                  },
                  modifier = Modifier.height(28.dp),
                  shape = RoundedCornerShape(6.dp),
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SoftGreenContainer,
                    selectedLabelColor = MedicalGreenDark
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}
