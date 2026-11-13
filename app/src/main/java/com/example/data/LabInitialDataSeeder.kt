package com.example.data

import com.example.model.LabTestParameter
import com.example.model.LabTestTemplate

object LabInitialDataSeeder {

  fun getSeedTemplates(): List<LabTestTemplate> = getDefaultTemplates()

  fun getDefaultTemplates(): List<LabTestTemplate> = listOf(
    // 1. CBC (Complete Blood Count) - Analyzer Slip or Manual Input
    LabTestTemplate(
      id = "TMPL-CBC",
      name = "CBC (Complete Blood Count)",
      category = "Hematology",
      defaultPrice = 8000.0,
      specimenType = "Whole Blood (EDTA)",
      isAnalyzerSlipOnly = true, // Designed for blank letterhead feed or analyzer slip
      parameters = listOf(
        LabTestParameter("Hemoglobin (Hb)", "", "g/dL", "13.0 - 17.5 (M), 12.0 - 15.5 (F)"),
        LabTestParameter("Total WBC Count", "", "/uL", "4,000 - 11,000"),
        LabTestParameter("Neutrophils", "", "%", "40 - 75"),
        LabTestParameter("Lymphocytes", "", "%", "20 - 45"),
        LabTestParameter("Monocytes", "", "%", "2 - 10"),
        LabTestParameter("Eosinophils", "", "%", "1 - 6"),
        LabTestParameter("Basophils", "", "%", "0 - 1"),
        LabTestParameter("Platelet Count", "", "/uL", "150,000 - 450,000"),
        LabTestParameter("RBC Count", "", "x10^6/uL", "4.5 - 5.9 (M), 4.1 - 5.1 (F)"),
        LabTestParameter("Hematocrit (HCT/PCV)", "", "%", "40 - 52 (M), 36 - 48 (F)"),
        LabTestParameter("MCV", "", "fL", "80 - 100"),
        LabTestParameter("MCH", "", "pg", "27 - 33"),
        LabTestParameter("MCHC", "", "g/dL", "32 - 36")
      )
    ),

    // 2. Urine RE/ME (Routine & Microscopic Examination)
    LabTestTemplate(
      id = "TMPL-URE-ME",
      name = "Urine RE / ME",
      category = "Urine / Stool",
      defaultPrice = 4000.0,
      specimenType = "Urine (Fresh Midstream)",
      parameters = listOf(
        // Section: Routine Examination (Physical & Chemical / RE)
        LabTestParameter("Color", "Pale Yellow", "", "Pale Yellow / Amber", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Appearance", "Clear", "", "Clear", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Specific Gravity", "1.015", "", "1.005 - 1.030", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Reaction (pH)", "6.0", "", "4.6 - 8.0", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Protein (Albumin)", "Nil", "", "Nil / Negative", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Glucose (Sugar)", "Nil", "", "Nil / Negative", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Ketone Bodies", "Nil", "", "Negative", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Bilirubin", "Negative", "", "Negative", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Urobilinogen", "Normal", "", "Normal", "", "Routine Examination (Physical & Chemical / RE)"),
        LabTestParameter("Blood (Hb)", "Negative", "", "Negative", "", "Routine Examination (Physical & Chemical / RE)"),
        // Section: Microscopic Examination (ME)
        LabTestParameter("Pus Cells (WBC)", "0 - 2", "/HPF", "0 - 5 / HPF", "", "Microscopic Examination (ME)"),
        LabTestParameter("Red Blood Cells (RBC)", "0 - 1", "/HPF", "0 - 2 / HPF", "", "Microscopic Examination (ME)"),
        LabTestParameter("Epithelial Cells", "Few", "/HPF", "Few / HPF", "", "Microscopic Examination (ME)"),
        LabTestParameter("Casts", "Nil seen", "/LPF", "Nil / LPF", "", "Microscopic Examination (ME)"),
        LabTestParameter("Crystals", "Nil seen", "/HPF", "Nil seen", "", "Microscopic Examination (ME)"),
        LabTestParameter("Bacteria", "Nil seen", "", "Nil seen", "", "Microscopic Examination (ME)")
      )
    ),

    // 3. Stool RE/ME (Routine & Microscopic Examination)
    LabTestTemplate(
      id = "TMPL-STOOL-RE-ME",
      name = "Stool RE / ME",
      category = "Urine / Stool",
      defaultPrice = 4000.0,
      specimenType = "Stool (Fresh)",
      parameters = listOf(
        // Section: Macroscopic Examination (RE)
        LabTestParameter("Color", "Brown", "", "Brown / Yellowish", "", "Macroscopic Examination (RE)"),
        LabTestParameter("Consistency", "Formed", "", "Formed / Soft", "", "Macroscopic Examination (RE)"),
        LabTestParameter("Mucus", "Absent", "", "Absent", "", "Macroscopic Examination (RE)"),
        LabTestParameter("Blood", "Absent", "", "Absent", "", "Macroscopic Examination (RE)"),
        // Section: Microscopic Examination (ME)
        LabTestParameter("Pus Cells (WBC)", "Nil seen", "/HPF", "Nil / HPF", "", "Microscopic Examination (ME)"),
        LabTestParameter("Red Blood Cells (RBC)", "Nil seen", "/HPF", "Nil / HPF", "", "Microscopic Examination (ME)"),
        LabTestParameter("Cysts (E. histolytica / Giardia)", "Nil seen", "", "Nil seen", "", "Microscopic Examination (ME)"),
        LabTestParameter("Trophozoites", "Nil seen", "", "Nil seen", "", "Microscopic Examination (ME)"),
        LabTestParameter("Ova / Helminths", "Nil seen", "", "Nil seen", "", "Microscopic Examination (ME)"),
        LabTestParameter("Macrophages", "Nil seen", "", "Nil seen", "", "Microscopic Examination (ME)"),
        LabTestParameter("Fat Globules", "Nil seen", "", "Nil seen", "", "Microscopic Examination (ME)")
      )
    ),

    // 3. Renal Function: Blood Urea & Serum Creatinine
    LabTestTemplate(
      id = "TMPL-RFT",
      name = "Renal Function (Urea & Creatinine)",
      category = "Biochemistry",
      defaultPrice = 7000.0,
      specimenType = "Serum (Clot Blood)",
      parameters = listOf(
        LabTestParameter("Blood Urea", "", "mg/dL", "15 - 45"),
        LabTestParameter("Serum Creatinine", "", "mg/dL", "0.7 - 1.3 (M), 0.6 - 1.1 (F)"),
        LabTestParameter("Estimated GFR (eGFR)", "", "mL/min/1.73m²", "> 90 (Normal)")
      )
    ),

    // 4. Liver Function Test (LFT)
    LabTestTemplate(
      id = "TMPL-LFT",
      name = "Liver Function Test (LFT)",
      category = "Biochemistry",
      defaultPrice = 15000.0,
      specimenType = "Serum (Clot Blood)",
      parameters = listOf(
        LabTestParameter("Total Bilirubin", "", "mg/dL", "0.2 - 1.2"),
        LabTestParameter("Direct Bilirubin", "", "mg/dL", "0.0 - 0.3"),
        LabTestParameter("SGPT / ALT", "", "U/L", "Up to 45"),
        LabTestParameter("SGOT / AST", "", "U/L", "Up to 35"),
        LabTestParameter("Alkaline Phosphatase (ALP)", "", "U/L", "30 - 120"),
        LabTestParameter("Total Protein", "", "g/dL", "6.4 - 8.3"),
        LabTestParameter("Serum Albumin", "", "g/dL", "3.5 - 5.0")
      )
    ),

    // 5. Lipid Profile
    LabTestTemplate(
      id = "TMPL-LIPID",
      name = "Lipid Profile",
      category = "Biochemistry",
      defaultPrice = 12000.0,
      specimenType = "Serum (Fasting 10-12 hrs)",
      parameters = listOf(
        LabTestParameter("Total Cholesterol", "", "mg/dL", "< 200 (Desirable)"),
        LabTestParameter("Triglycerides", "", "mg/dL", "< 150 (Normal)"),
        LabTestParameter("HDL Cholesterol", "", "mg/dL", "> 40 (M), > 50 (F)"),
        LabTestParameter("LDL Cholesterol", "", "mg/dL", "< 100 (Optimal)")
      )
    ),

    // 6. Blood Glucose (RBS / FBS / HbA1c)
    LabTestTemplate(
      id = "TMPL-GLUCOSE",
      name = "Blood Glucose & HbA1c",
      category = "Biochemistry",
      defaultPrice = 4000.0,
      specimenType = "Fluoride / EDTA Blood",
      parameters = listOf(
        LabTestParameter("Random Blood Sugar (RBS)", "", "mg/dL", "70 - 140"),
        LabTestParameter("Fasting Blood Sugar (FBS)", "", "mg/dL", "70 - 100"),
        LabTestParameter("HbA1c (Glycated Hb)", "", "%", "< 5.7 (Normal), 5.7-6.4 (Pre-DM), >=6.5 (DM)")
      )
    ),

    // 7. Serum Electrolytes
    LabTestTemplate(
      id = "TMPL-ELECTROLYTES",
      name = "Serum Electrolytes",
      category = "Biochemistry",
      defaultPrice = 9000.0,
      specimenType = "Serum (Clot Blood)",
      parameters = listOf(
        LabTestParameter("Sodium (Na+)", "", "mmol/L", "135 - 145"),
        LabTestParameter("Potassium (K+)", "", "mmol/L", "3.5 - 5.0"),
        LabTestParameter("Chloride (Cl-)", "", "mmol/L", "96 - 106")
      )
    ),

    // 8. Infectious & Rapid Serology (Widal, Dengue, HBsAg, Malaria)
    LabTestTemplate(
      id = "TMPL-SEROLOGY",
      name = "Serology / Rapid Tests (Widal / Dengue / HBsAg)",
      category = "Serology",
      defaultPrice = 6000.0,
      specimenType = "Serum / Whole Blood",
      parameters = listOf(
        LabTestParameter("Widal TO", "Negative", "", "< 1:80 (Negative)"),
        LabTestParameter("Widal TH", "Negative", "", "< 1:80 (Negative)"),
        LabTestParameter("Dengue NS1 Antigen", "Negative", "", "Negative"),
        LabTestParameter("Dengue IgM Antibody", "Negative", "", "Negative"),
        LabTestParameter("Dengue IgG Antibody", "Negative", "", "Negative"),
        LabTestParameter("Hepatitis B sAg (HBsAg)", "Negative", "", "Negative"),
        LabTestParameter("Malaria RDT (Pf / Pv)", "Negative", "", "Negative")
      )
    )
  )
}
