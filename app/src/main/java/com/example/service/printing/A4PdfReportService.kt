package com.example.service.printing

import com.example.model.ConsultationRecord
import com.example.model.Patient

/**
 * A4PdfReportService
 * 
 * Generates A4 Medical Record + Prescription Document representation for PDF printing.
 * MANDATORY RULE: Used ONLY for Patient Clinical Notes & Prescriptions when requested.
 */
object A4PdfReportService {

  data class A4ClinicalReportData(
    val clinicName: String = "Shwe Khit Clinic",
    val clinicAddress: String = "No. 12, Lanmadaw Street, Mandalay",
    val clinicPhone: String = "09-421001122 / 09-790001122",
    val doctorName: String = "Dr. Nyan Kaung Sett M.B.,B.S(MDY)",
    val doctorQualifications: String = "M.B.,B.S(MDY)",
    val doctorSamaNo: String = "SAMA: 47414",
    val patientHn: String,
    val patientName: String,
    val patientAge: Int,
    val patientGender: String,
    val patientPhone: String,
    val patientAddress: String = "",
    val allergies: List<String>,
    val visitDate: String,
    val bloodPressure: String,
    val pulseRate: Int,
    val temperatureCelsius: Double,
    val oxygenSaturation: Int,
    val weightKg: Double,
    val rbs: Double? = null,
    val chiefComplaint: String,
    val diagnosisList: List<String>,
    val assessment: String,
    val subjectiveNotes: String,
    val doctorAdvice: String,
    val prescriptions: List<A4PrescriptionLine>,
    val procedures: List<String>,
    val followUpDate: String = "After 5 Days (or if symptoms worsen)"
  )

  data class A4PrescriptionLine(
    val tradeName: String,
    val genericName: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val instructions: String,
    val quantity: Int
  )

  fun prepareReportData(
    consultation: ConsultationRecord,
    patient: Patient?
  ): A4ClinicalReportData {
    val vitals = patient?.vitals
    val docName = consultation.doctorName.ifBlank { "Dr. Nyan Kaung Sett" }
    val docDegree = consultation.doctorDegree.ifBlank { "M.B.,B.S(MDY)" }
    val docSama = if (consultation.doctorSama.isNotBlank()) "SAMA: ${consultation.doctorSama}" else "SAMA: 47414"
    return A4ClinicalReportData(
      doctorName = if (docName.contains("M.B", ignoreCase = true)) docName else "$docName $docDegree".trim(),
      doctorQualifications = docDegree,
      doctorSamaNo = docSama,
      patientHn = patient?.hn ?: "HN-2026-0081",
      patientName = consultation.patientName,
      patientAge = patient?.age ?: 34,
      patientGender = patient?.gender?.name ?: "MALE",
      patientPhone = patient?.phone ?: "09-790001122",
      patientAddress = patient?.address ?: "",
      allergies = patient?.allergies ?: emptyList(),
      visitDate = consultation.timestamp,
      bloodPressure = vitals?.bloodPressure ?: "120/80 mmHg",
      pulseRate = vitals?.pulseRate ?: 76,
      temperatureCelsius = vitals?.temperatureCelsius ?: 36.8,
      oxygenSaturation = vitals?.oxygenSaturation ?: 98,
      weightKg = vitals?.weightKg ?: 62.5,
      rbs = vitals?.rbs,
      chiefComplaint = patient?.chiefComplaint ?: "General Illness",
      diagnosisList = consultation.diagnosisList,
      assessment = consultation.assessment,
      subjectiveNotes = consultation.subjective,
      doctorAdvice = consultation.plan,
      prescriptions = consultation.prescriptions.map { drug ->
        val isCst = drug.isCst || drug.medicineId.startsWith("CST-") || drug.medicineId == "EXT-CST" || drug.name.startsWith("CST") || drug.name.contains("သောက်နေကြဆေး") || drug.instructions.contains("သောက်လက်စ")
        val isExt = drug.isExternal || drug.medicineId.startsWith("EXT-")
        val hasSpecificName = drug.name.isNotBlank() &&
          !drug.name.contains("သောက်နေကြဆေးများပုံမှန်သောက်ရန်") &&
          drug.name != "CST (Continue Same Treatment)" &&
          drug.name != "(သောက်နေကြဆေးများပုံမှန်သောက်ရန်)"

        val finalTitle = if (isCst) {
          if (hasSpecificName) "${drug.name} [CST ဆက်သောက်ရန်]" else "သောက်လက်စသောက်ဆေးများ ပုံမှန်အတိုင်း ဆက်သောက်ရန်"
        } else if (isExt) "${drug.name} [External - ပြင်ပဝယ်]" else drug.name
        val cleanFreq = if (isCst && !hasSpecificName) "-" else drug.frequency
        val cleanDuration = if (isCst) (if (drug.duration.isNotBlank() && !drug.duration.contains("Day")) drug.duration else "Continuous") else drug.duration
        A4PrescriptionLine(
          tradeName = finalTitle,
          genericName = if (isCst && !hasSpecificName) "" else (drug.name.split(" ").firstOrNull() ?: drug.name),
          dosage = if ((isCst && !hasSpecificName) || drug.dosage == "-") "" else drug.dosage,
          frequency = cleanFreq,
          duration = cleanDuration,
          instructions = if (isCst && !hasSpecificName) "" else drug.instructions,
          quantity = drug.quantity
        )
      },
      procedures = consultation.selectedProcedures.map { "${it.name} (${it.code})" },
      followUpDate = consultation.followUpDueDate ?: "After 5 Days (or if symptoms worsen)"
    )
  }
}
