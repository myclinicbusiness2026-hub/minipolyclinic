package com.example.ui.components

import com.example.model.ClinicSettings

/**
 * A4PrintOptions
 * Granular toggles allowing clinicians to choose which sections appear on the printed document.
 */
data class A4PrintOptions(
  val showVitals: Boolean = true,
  val showClinicalNotes: Boolean = true,
  val showDiagnosis: Boolean = true,
  val showProcedures: Boolean = true,
  val showInvestigations: Boolean = true,
  val showPrescriptions: Boolean = true,
  val showAdvice: Boolean = true,
  val showDoctorSignature: Boolean = true
)

/**
 * Clinic branding, credentials, and print options for A4 clinical documents.
 */
data class A4PrintSettings(
  val clinicNameEn: String = "Shwe Khit General Clinic",
  val clinicNameMm: String = "“ရွှေခေတ်” အထွေထွေရောဂါကုဆေးခန်း",
  val clinicAddress: String = "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့",
  val clinicPhone: String = "09-421001122, 09-790112233",
  val clinicHours: String = "8:00 AM – 8:00 PM",
  val clinicRegNo: String = "MOHS-MDY-2024",
  val doctorName: String = "Dr. Nyan Kaung Sett",
  val doctorDegree: String = "M.B.,B.S (MDY)",
  val doctorSama: String = "47414",
  val clinicLogoUri: String = "",
  val options: A4PrintOptions = A4PrintOptions()
) {
  val doctorDegrees: String get() = doctorDegree
  val samaNumber: String get() = doctorSama
  val operatingHours: String get() = clinicHours
  val registrationNumber: String get() = clinicRegNo

  companion object {
    fun fromClinicSettings(s: ClinicSettings, options: A4PrintOptions = A4PrintOptions()): A4PrintSettings {
      return A4PrintSettings(
        clinicNameEn = s.clinicNameEn,
        clinicNameMm = if (s.clinicSubtitleMm.isNotBlank()) "\"${s.clinicNameMm}\" ${s.clinicSubtitleMm}" else s.clinicNameMm,
        clinicAddress = s.clinicAddress,
        clinicPhone = s.clinicPhone,
        clinicHours = s.clinicHours,
        clinicRegNo = s.clinicRegNo,
        doctorName = s.doctorName,
        doctorDegree = s.doctorDegree,
        doctorSama = s.doctorSama,
        clinicLogoUri = s.clinicLogoUri,
        options = options
      )
    }
  }
}
