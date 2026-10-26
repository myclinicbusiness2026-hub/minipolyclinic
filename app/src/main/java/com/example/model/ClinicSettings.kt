package com.example.model

/**
 * ClinicSettings
 * Centralized, dynamic clinic profile and operating parameters.
 * Eliminates all hardcoded clinic and physician branding throughout the app.
 */
data class ClinicSettings(
  val clinicNameEn: String = "Shwe Khit General Clinic",
  val clinicNameMm: String = "“ရွှေခေတ်”",
  val clinicSubtitleMm: String = "အထွေထွေရောဂါကုဆေးခန်း",
  val addressMm: String = "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့",
  val clinicAddress: String = "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့",
  val phonePrimary: String = "09-421001122",
  val phoneSecondary: String = "09-790112233",
  val clinicPhone: String = "09-421001122, 09-790112233",
  val clinicHours: String = "8:00 AM – 8:00 PM",
  val clinicRegNo: String = "MOHS-MDY-2024",
  val doctorName: String = "Dr. Nyan Kaung Sett",
  val doctorDegree: String = "M.B.,B.S (MDY)",
  val doctorSama: String = "47414",
  val defaultConsultationFee: Double = 8000.0,
  val defaultImInjectionFee: Double = 2000.0,
  val defaultIvPushFee: Double = 3000.0,
  val defaultIvInfusionFee: Double = 6000.0,
  val defaultNebulizerFee: Double = 3000.0,
  val defaultDepoFee: Double = 2500.0,
  val allowQuickLogin: Boolean = true,
  val thermalPaperWidthMm: Int = 80,
  val bluetoothPrinterMac: String = "",
  val preferredThermalPrinterAddress: String = "",
  val receiptGreetingMm: String = "ကျေးဇူးတင်ပါသည် (Thank you!)",
  val clinicLogoUri: String = "",
  val clinicLogoBase64: String = "",
  val supabaseUrl: String = "https://tjpayhhzbrcrkwyahecr.supabase.co",
  val supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRqcGF5aGh6YnJjcmt3eWFoZWNyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA2NTcxNTMsImV4cCI6MjEwNjIzMzE1M30.U7YlxEPl26SQt0bKjY2vw3nEWtYEOnAdxHt2L6_FEdU",
  val enableSupabaseSync: Boolean = true,
  val wifiHubHostIp: String = "",
  val wifiSyncPort: Int = 8080,
  val lastSupabaseSyncTimestamp: String = "",
  val lastWifiSyncTimestamp: String = "",
  val activeStationName: String = "Doctor Consultation Station"
) {
  val effectiveLogo: String get() = clinicLogoBase64.ifBlank { clinicLogoUri }
}
