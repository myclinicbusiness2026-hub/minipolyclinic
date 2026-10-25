package com.example.model

/**
 * Vaccination Regime definition with pre-configured WHO / CDC clinical protocols.
 * Supports auto-calculation of follow-up dates based on dose intervals (Days or Months).
 */
data class VaccineRegimeDose(
  val doseLabel: String,      // e.g. "Dose 1 (Day 0)", "Dose 2 (Day 3)"
  val dayOffsetFromStart: Int, // e.g. 0, 3, 7, 21, 28, 30, 180, 365, 1825
  val defaultDosageRoute: String = "Deltoid IM 0.5ml"
)

data class VaccineRegime(
  val id: String,
  val vaccineName: String,     // e.g. "Rabies Vaccine", "Hepatitis B Vaccine"
  val regimeName: String,      // e.g. "Rabies 4-Dose Zagreb (Day 0, 3, 7, 21)"
  val descriptionBurmese: String, // e.g. "ခွေးကိုက်/ကြောင်ကိုက် ကာကွယ်ဆေး (၄ ကြိမ်ထိုး စနစ်)"
  val doses: List<VaccineRegimeDose>
)

object VaccineRegimesData {
  val ALL_REGIMES = listOf(
    // 1. Rabies 4-Dose (Zagreb / Updated Thai Red Cross modified IM 4-dose)
    VaccineRegime(
      id = "rabies_4dose",
      vaccineName = "Rabies Vaccine (ခွေးရူးပြန် ကာကွယ်ဆေး)",
      regimeName = "Rabies 4-Dose Regime (Day 0, 3, 7, 21)",
      descriptionBurmese = "ခွေးကိုက်/ကြောင်ကိုက် - ၄ ကြိမ်ထိုး စနစ် (Day 0, 3, 7, 21)",
      doses = listOf(
        VaccineRegimeDose("Dose 1 (Day 0)", 0, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 2 (Day 3)", 3, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 3 (Day 7)", 7, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 4 (Day 21)", 21, "Deltoid IM 0.5ml / 1.0ml")
      )
    ),

    // 2. Rabies 5-Dose (Essen standard post-exposure prophylaxis)
    VaccineRegime(
      id = "rabies_5dose",
      vaccineName = "Rabies Vaccine (ခွေးရူးပြန် ကာကွယ်ဆေး)",
      regimeName = "Rabies 5-Dose Essen Regime (Day 0, 3, 7, 14, 28)",
      descriptionBurmese = "ခွေးကိုက်/ကြောင်ကိုက် - ၅ ကြိမ်ထိုး အပြည့်အဝ စနစ် (Day 0, 3, 7, 14, 28)",
      doses = listOf(
        VaccineRegimeDose("Dose 1 (Day 0)", 0, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 2 (Day 3)", 3, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 3 (Day 7)", 7, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 4 (Day 14)", 14, "Deltoid IM 0.5ml / 1.0ml"),
        VaccineRegimeDose("Dose 5 (Day 28)", 28, "Deltoid IM 0.5ml / 1.0ml")
      )
    ),

    // 3. Hepatitis B Adult Standard Course + Boosters
    VaccineRegime(
      id = "hepb_adult",
      vaccineName = "Hepatitis B Vaccine (အသည်းရောင် အသားဝါ ဘီ)",
      regimeName = "Hepatitis B Adult (Day 0, 1mo, 6mo, 1yr, 5yr Booster)",
      descriptionBurmese = "အသည်းရောင် အသားဝါ ဘီပိုး ကာကွယ်ဆေး (Day 0, 1 လ, 6 လ, 1 နှစ်, 5 နှစ် Booster)",
      doses = listOf(
        VaccineRegimeDose("Dose 1 (Day 0)", 0, "Deltoid IM 20mcg/1.0ml"),
        VaccineRegimeDose("Dose 2 (1 Month)", 30, "Deltoid IM 20mcg/1.0ml"),
        VaccineRegimeDose("Dose 3 (6 Months)", 180, "Deltoid IM 20mcg/1.0ml"),
        VaccineRegimeDose("Booster 1 (1 Year)", 365, "Deltoid IM 20mcg/1.0ml"),
        VaccineRegimeDose("Booster 2 (5 Years)", 1825, "Deltoid IM 20mcg/1.0ml")
      )
    ),

    // 4. Tetanus Toxoid (TT) Post-Trauma & Prophylaxis
    VaccineRegime(
      id = "tetanus_tt",
      vaccineName = "Tetanus Toxoid (မေးခိုင် ကာကွယ်ဆေး)",
      regimeName = "Tetanus 3-Dose + Booster (Day 0, 1mo, 6mo, 10yr)",
      descriptionBurmese = "မေးခိုင် ကာကွယ်ဆေး (ဒဏ်ရာရရှိသူ နှင့် ကာကွယ်ဆေးပြည့်ထိုးသူများ)",
      doses = listOf(
        VaccineRegimeDose("Dose 1 (Day 0 - Initial)", 0, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Dose 2 (1 Month)", 30, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Dose 3 (6 Months)", 180, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Booster (10 Years)", 3650, "Deltoid IM 0.5ml")
      )
    ),

    // 5. Influenza (Flu) Annual Vaccine
    VaccineRegime(
      id = "influenza_annual",
      vaccineName = "Influenza Vaccine (ရာသီတုပ်ကွေး ကာကွယ်ဆေး)",
      regimeName = "Influenza Annual (၁ နှစ် ၁ ကြိမ်)",
      descriptionBurmese = "ရာသီတုပ်ကွေး ၄ မျိုးစပ် ကာကွယ်ဆေး (နှစ်စဉ်ထိုးနှံရန်)",
      doses = listOf(
        VaccineRegimeDose("Annual Dose (Year 1)", 0, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Next Annual Dose (1 Year)", 365, "Deltoid IM 0.5ml")
      )
    ),

    // 6. HPV (Human Papillomavirus - Cervarix / Gardasil)
    VaccineRegime(
      id = "hpv_gardasil",
      vaccineName = "HPV Vaccine (သားအိမ်ခေါင်းကင်ဆာ ကာကွယ်ဆေး)",
      regimeName = "HPV 3-Dose Regime (Day 0, 1-2mo, 6mo)",
      descriptionBurmese = "သားအိမ်ခေါင်းကင်ဆာ ကာကွယ်ဆေး (Day 0, 2 လ, 6 လ)",
      doses = listOf(
        VaccineRegimeDose("Dose 1 (Day 0)", 0, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Dose 2 (2 Months)", 60, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Dose 3 (6 Months)", 180, "Deltoid IM 0.5ml")
      )
    ),

    // 7. Pneumococcal (PCV13 / PPSV23)
    VaccineRegime(
      id = "pneumococcal",
      vaccineName = "Pneumococcal Vaccine (အဆုတ်ရောင် နမိုးနီးယား)",
      regimeName = "Pneumococcal Adult (Day 0, 1 Year Booster)",
      descriptionBurmese = "အဆုတ်ရောင် နမိုးနီးယား ကာကွယ်ဆေး (Day 0, 1 နှစ်အကြာ Booster)",
      doses = listOf(
        VaccineRegimeDose("PCV Primary Dose (Day 0)", 0, "Deltoid IM 0.5ml"),
        VaccineRegimeDose("Booster Dose (1 Year)", 365, "Deltoid IM 0.5ml")
      )
    )
  )
}
