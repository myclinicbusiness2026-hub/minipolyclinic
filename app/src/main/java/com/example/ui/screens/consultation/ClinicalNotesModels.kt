package com.example.ui.screens.consultation

data class ComplaintItem(
  val id: String,
  val name: String,
  var duration: String = "2-3 days",
  var customDuration: String = "",
  var severity: String = "Moderate (++)",
  var character: String = "",
  var customCharacter: String = "",
  var onset: String = "Gradual",
  val relievingFactors: MutableList<String> = mutableListOf(),
  var customRelieving: String = "",
  val aggravatingFactors: MutableList<String> = mutableListOf(),
  var customAggravating: String = "",
  val associatedSymptoms: MutableList<String> = mutableListOf(),
  var customAssociated: String = "",
  var notes: String = "",
  val isCustom: Boolean = false
)

data class AnatomicalPin(
  val id: String,
  val nameEn: String,
  val xFactor: Float, // 0.0 .. 1.0
  val yFactor: Float, // 0.0 .. 1.0
  val commonPresets: List<String>
)

val CLINIC_COMMON_SYMPTOMS = listOf(
  "Fever",
  "Cough",
  "Sore Throat",
  "Flu / Runny Nose",
  "Headache",
  "Dyspnea (Shortness of Breath)",
  "Chest Pain",
  "Abdominal Pain",
  "Nausea / Vomiting",
  "Diarrhea",
  "Body Ache / Myalgia",
  "Fatigue / Generalized Weakness",
  "Loss of Appetite",
  "Dysuria (Painful Urination)",
  "Skin Rash / Pruritus",
  "Dizziness / Vertigo",
  "Joint Pain / Arthralgia",
  "Constipation",
  "Palpitations",
  "Insomnia",
  "Epistaxis",
  "Weight Loss"
)

val CLINIC_BODY_PINS = listOf(
  AnatomicalPin("head", "Head & ENT", 0.50f, 0.08f,
    listOf("Throat normal, no erythema", "Pharyngeal injection (+)", "Tonsils enlarged", "Pale conjunctiva", "Anicteric sclera", "External auditory canal clear")),
  AnatomicalPin("neck", "Neck & Lymph Nodes", 0.50f, 0.18f,
    listOf("Supple, no lymphadenopathy", "Thyroid normal size", "Neck stiffness negative", "Cervical lymphadenopathy (+)")),
  AnatomicalPin("chest", "Chest & Lungs", 0.38f, 0.30f,
    listOf("Bilateral breath sounds clear", "Crepitations (Creps)", "Rhonchi / Wheezing", "Decreased air entry", "Vesicular breath sounds")),
  AnatomicalPin("heart", "Heart / CVS", 0.62f, 0.33f,
    listOf("S1 S2 normal, no murmurs", "Regular rate & rhythm", "Systolic murmur heard", "Tachycardia", "Apex beat in 5th ICS")),
  AnatomicalPin("abdomen", "Abdomen", 0.50f, 0.46f,
    listOf("Soft, non-tender, no guarding", "Epigastric tenderness", "RIF tenderness", "Guarding / Rebound tenderness", "No organomegaly", "Bowel sounds normal")),
  AnatomicalPin("pelvis", "Pelvis & GU", 0.50f, 0.58f,
    listOf("No suprapubic tenderness", "Renal angle tenderness negative", "Bladder non-distended")),
  AnatomicalPin("r_arm", "Right Arm", 0.16f, 0.36f,
    listOf("Normal ROM, no deformity", "No joint swelling", "Radial pulse strong & regular", "Motor power 5/5")),
  AnatomicalPin("l_arm", "Left Arm", 0.84f, 0.36f,
    listOf("Normal ROM, no deformity", "No joint swelling", "Radial pulse strong & regular", "Motor power 5/5")),
  AnatomicalPin("spine", "Spine & Back", 0.32f, 0.52f,
    listOf("No spinal tenderness", "No paraspinal spasm", "Normal spinal mobility", "SLR negative")),
  AnatomicalPin("limbs", "Lower Limbs", 0.50f, 0.84f,
    listOf("No pedal edema", "Pitting edema (+)", "Pitting edema (++)", "Peripheral pulses palpable", "Calf tenderness negative"))
)
