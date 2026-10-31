package com.example.sync

import android.content.Context
import android.net.Uri
import java.io.File
import com.example.model.*
import com.example.ui.screens.treatment.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PeerDevice(
  val id: String,
  val name: String,
  val ipAddress: String,
  val port: Int = 8888,
  val role: String = "CLINIC_CLIENT",
  val lastSeenMillis: Long = System.currentTimeMillis(),
  val isConnected: Boolean = true
) {
  val baseUrl: String get() = "http://$ipAddress:$port"
}

enum class SyncStatus {
  IDLE,
  DISCOVERING,
  SYNCING,
  SUCCESS,
  ERROR
}

data class SyncAuditEntry(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: String,
  val peerName: String,
  val peerIp: String,
  val patientsMerged: Int,
  val consultationsMerged: Int,
  val invoicesMerged: Int,
  val medicinesMerged: Int,
  val status: String,
  val message: String
) {
  val action: String get() = status
  val entityType: String get() = peerName
  val recordId: String get() = if (message.isNotBlank()) message else peerIp
}

typealias SyncLogEntry = SyncAuditEntry

data class SyncDataPayload(
  val deviceId: String,
  val deviceName: String,
  val timestamp: Long,
  val patients: List<Patient>,
  val consultations: List<ConsultationRecord>,
  val invoices: List<Invoice>,
  val medicines: List<Medicine>,
  val users: List<UserAccount> = emptyList(),
  val treatmentCards: List<TreatmentCard> = emptyList(),
  val expenses: List<ExpenseRecord> = emptyList(),
  val procedures: List<ProcedureItem> = emptyList(),
  val suppliers: List<Supplier> = emptyList(),
  val vaccines: List<VaccineRecord> = emptyList(),
  val clinicSettings: ClinicSettings? = null,
  val deletedPatientIds: List<String> = emptyList(),
  val deletedConsultationIds: List<String> = emptyList(),
  val deletedInvoiceIds: List<String> = emptyList(),
  val deletedMedicineIds: List<String> = emptyList()
) {
  fun toJsonString(context: Context? = null): String {
    val root = JSONObject()
    root.put("deviceId", deviceId)
    root.put("deviceName", deviceName)
    root.put("timestamp", timestamp)

    // Patients
    val patArr = JSONArray()
    patients.forEach { p ->
      val po = JSONObject()
      po.put("id", p.id)
      po.put("tokenNumber", p.tokenNumber)
      po.put("hn", p.hn)
      po.put("queueNumber", p.queueNumber)
      po.put("name", p.name)
      po.put("age", p.age)
      po.put("gender", p.gender.name)
      po.put("phone", p.phone)
      po.put("address", p.address)
      po.put("status", p.status.name)
      po.put("registrationTime", p.registrationTime)
      po.put("notes", p.notes)
      po.put("chiefComplaint", p.chiefComplaint)
      po.put("followUpDueDate", p.followUpDueDate ?: "")
      po.put("isReminderSent", p.isReminderSent)
      po.put("isFollowUp", p.isFollowUp)
      po.put("followUpReason", p.followUpReason)
      po.put("bloodType", p.bloodType)
      po.put("knownAllergies", JSONArray(p.knownAllergies))
      po.put("chronicDiseases", JSONArray(p.chronicDiseases))
      val v = JSONObject().apply {
        put("bp", p.vitals.bloodPressure)
        put("pr", p.vitals.pulseRate)
        put("temp", p.vitals.temperatureCelsius)
        put("spo2", p.vitals.oxygenSaturation)
        put("weight", p.vitals.weightKg)
        put("rbs", p.vitals.rbs)
        put("height", p.vitals.heightCm)
      }
      po.put("vitals", v)
      patArr.put(po)
    }
    root.put("patients", patArr)

    // Consultations
    val consArr = JSONArray()
    consultations.forEach { c ->
      val co = JSONObject()
      co.put("id", c.id)
      co.put("patientId", c.patientId)
      co.put("patientName", c.patientName)
      co.put("doctorName", c.doctorName)
      co.put("doctorDegree", c.doctorDegree)
      co.put("doctorSama", c.doctorSama)
      co.put("subjective", c.subjective)
      co.put("objective", c.objective)
      co.put("assessment", c.assessment)
      co.put("plan", c.plan)
      co.put("diagnosisList", JSONArray(c.diagnosisList))
      co.put("suggestedInvestigations", JSONArray(c.suggestedInvestigations))
      co.put("consultationFee", c.consultationFee)
      co.put("timestamp", c.timestamp)
      co.put("followUpAppointment", c.followUpAppointment)
      co.put("followUpDueDate", c.followUpDueDate)
      co.put("nextVaccineSchedule", c.nextVaccineSchedule)
      co.put("patientInstructions", c.patientInstructions)

      val vObj = JSONObject().apply {
        put("bloodPressure", c.vitals.bloodPressure)
        put("pulseRate", c.vitals.pulseRate)
        put("temperatureCelsius", c.vitals.temperatureCelsius)
        put("oxygenSaturation", c.vitals.oxygenSaturation)
        put("weightKg", c.vitals.weightKg)
        c.vitals.rbs?.let { put("rbs", it) }
      }
      co.put("vitals", vObj)

      val rxArr = JSONArray()
      c.prescriptions.forEach { rx ->
        val ro = JSONObject()
        ro.put("id", rx.id)
        ro.put("name", rx.name)
        ro.put("medicineId", rx.medicineId)
        ro.put("medicineName", rx.medicineName)
        ro.put("dosage", rx.dosage)
        ro.put("frequency", rx.frequency)
        ro.put("duration", rx.duration)
        ro.put("quantity", rx.quantity)
        ro.put("unitPrice", rx.unitPrice)
        ro.put("instructions", rx.instructions)
        ro.put("isExternal", rx.isExternal)
        ro.put("medicineType", rx.medicineType)
        rxArr.put(ro)
      }
      co.put("prescriptions", rxArr)

      val prArr = JSONArray()
      c.selectedProcedures.forEach { proc ->
        val pro = JSONObject()
        pro.put("id", proc.id)
        pro.put("name", proc.name)
        pro.put("category", proc.category)
        pro.put("price", proc.price)
        pro.put("standardFee", proc.standardFee)
        pro.put("consumableCost", proc.consumableCost)
        pro.put("notes", proc.notes)
        pro.put("dateTime", proc.dateTime)
        prArr.put(pro)
      }
      co.put("procedures", prArr)

      val atArr = JSONArray()
      c.attachments.forEach { a ->
        val ao = JSONObject()
        ao.put("id", a.id)
        ao.put("title", a.title)
        ao.put("category", a.category)
        ao.put("date", a.date)
        ao.put("imageUri", a.imageUri)
        ao.put("filePath", a.filePath)
        ao.put("fileType", a.fileType)
        ao.put("fileName", a.fileName)
        ao.put("notes", a.notes)

        if (context != null) {
          try {
            val file = when {
              a.filePath.isNotBlank() && File(a.filePath).exists() -> File(a.filePath)
              a.imageUri.isNotBlank() && File(a.imageUri).exists() -> File(a.imageUri)
              a.imageUri.startsWith("file://") && File(Uri.parse(a.imageUri).path ?: "").exists() -> File(Uri.parse(a.imageUri).path!!)
              else -> null
            }
            val bytes = when {
              file != null && file.length() in 1..8_000_000 -> file.readBytes()
              a.imageUri.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(a.imageUri))?.use { it.readBytes() }
              }
              else -> null
            }
            if (bytes != null && bytes.isNotEmpty() && bytes.size <= 8_000_000) {
              val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
              ao.put("fileBase64", b64)
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
        atArr.put(ao)
      }
      co.put("attachments", atArr)

      val toArr = JSONArray()
      c.treatmentOrderDetails.forEach { toArr.put(it) }
      co.put("treatmentOrderDetails", toArr)

      consArr.put(co)
    }
    root.put("consultations", consArr)

    // Invoices
    val invArr = JSONArray()
    invoices.forEach { inv ->
      val io = JSONObject()
      io.put("id", inv.id)
      io.put("invoiceNumber", inv.invoiceNumber)
      io.put("consultationId", inv.consultationId)
      io.put("patientId", inv.patientId)
      io.put("patientName", inv.patientName)
      io.put("consultationFee", inv.consultationFee)
      io.put("serviceFee", inv.serviceFee)
      io.put("discount", inv.discount)
      io.put("totalAmount", inv.totalAmount)
      io.put("paymentMethod", inv.paymentMethod.name)
      io.put("paymentStatus", inv.paymentStatus)
      io.put("paidAmount", inv.paidAmount)
      io.put("changeAmount", inv.changeAmount)
      io.put("cashierName", inv.cashierName)
      io.put("timestamp", inv.timestamp)

      val itemArr = JSONArray()
      inv.items.forEach { item ->
        val ito = JSONObject()
        ito.put("name", item.name)
        ito.put("quantity", item.quantity)
        ito.put("unitPrice", item.unitPrice)
        ito.put("totalPrice", item.totalPrice)
        ito.put("isProcedure", item.isProcedure)
        itemArr.put(ito)
      }
      io.put("items", itemArr)
      invArr.put(io)
    }
    root.put("invoices", invArr)

    // Medicines
    val medArr = JSONArray()
    medicines.forEach { m ->
      val mo = JSONObject()
      mo.put("id", m.id)
      mo.put("name", m.name)
      mo.put("genericName", m.genericName)
      mo.put("dosageForm", m.dosageForm)
      mo.put("category", m.category)
      mo.put("unitType", m.unitType)
      mo.put("medicineType", m.effectiveMedicineType)
      mo.put("profitMarginPercent", m.profitMarginPercent)

      val bArr = JSONArray()
      m.batches.forEach { b ->
        val bo = JSONObject()
        bo.put("batchNumber", b.batchNumber)
        bo.put("expiryDate", b.expiryDate)
        bo.put("quantity", b.quantity)
        bo.put("costPricePerUnit", b.costPricePerUnit)
        bArr.put(bo)
      }
      mo.put("batches", bArr)
      medArr.put(mo)
    }
    root.put("medicines", medArr)

    val delPatArr = JSONArray()
    deletedPatientIds.forEach { delPatArr.put(it) }
    root.put("deletedPatientIds", delPatArr)

    val delConsArr = JSONArray()
    deletedConsultationIds.forEach { delConsArr.put(it) }
    root.put("deletedConsultationIds", delConsArr)

    val delInvArr = JSONArray()
    deletedInvoiceIds.forEach { delInvArr.put(it) }
    root.put("deletedInvoiceIds", delInvArr)

    val delMedArr = JSONArray()
    deletedMedicineIds.forEach { delMedArr.put(it) }
    root.put("deletedMedicineIds", delMedArr)

    // Users
    val userArr = JSONArray()
    users.forEach { u ->
      val uo = JSONObject().apply {
        put("id", u.id)
        put("username", u.username)
        put("fullName", u.fullName)
        put("role", u.role.name)
        put("samaNumber", u.samaNumber)
        put("degree", u.degree)
        put("pinCode", u.pinCode)
        put("passwordHash", u.passwordHash)
        put("phone", u.phone)
        put("department", u.department)
        put("isActive", u.isActive)
      }
      userArr.put(uo)
    }
    root.put("users", userArr)

    // Treatment Cards
    val tcArr = JSONArray()
    treatmentCards.forEach { card ->
      val tco = JSONObject().apply {
        put("id", card.id)
        put("patientId", card.patientId)
        put("patientName", card.patientName)
        put("patientHn", card.patientHn)
        put("doctorName", card.doctorName)
        put("prescribedDate", card.prescribedDate)
        put("medicationId", card.medicationId)
        put("medicationName", card.medicationName)
        put("dosage", card.dosage)
        put("route", card.route.name)
        put("frequency", card.frequency)
        put("totalDays", card.totalDays)
        put("totalSessions", card.totalSessions)
        put("instructions", card.instructions)
        put("sessionFee", card.sessionFee)
        put("medicationFee", card.medicationFee)
        put("isBilledPerSession", card.isBilledPerSession)
        put("isPayPerVisit", card.isPayPerVisit)
        put("isPrepaidFullCourse", card.isPrepaidFullCourse)
        put("cardStatus", card.cardStatus.name)
        put("discontinueReason", card.discontinueReason)
        put("discontinuedBy", card.discontinuedBy)
        put("discontinuedAt", card.discontinuedAt)
        put("notes", card.notes)
        put("isCocktail", card.isCocktail)
        put("isPatientSupplied", card.isPatientSupplied)

        val ckArr = JSONArray()
        card.cocktailItems.forEach { item ->
          val ckObj = JSONObject().apply {
            put("id", item.id)
            put("medicineId", item.medicineId)
            put("name", item.name)
            put("dosage", item.dosage)
            put("quantity", item.quantity)
            put("unitPrice", item.unitPrice)
          }
          ckArr.put(ckObj)
        }
        put("cocktailItems", ckArr)

        val sessArr = JSONArray()
        card.sessions.forEach { s ->
          val so = JSONObject().apply {
            put("id", s.id)
            put("sessionNumber", s.sessionNumber)
            put("scheduledDate", s.scheduledDate)
            put("scheduledTime", s.scheduledTime)
            put("administeredAt", s.administeredAt ?: "")
            put("administeredBy", s.administeredBy)
            put("status", s.status.name)
            put("isClinicMed", s.isClinicMed)
            put("isPrepaid", s.isPrepaid)
            put("medicationFee", s.medicationFee)
            put("sessionFee", s.sessionFee)
            put("invoiceId", s.invoiceId ?: "")
            put("notes", s.notes)

            val consArr = JSONArray()
            s.consumables.forEach { c ->
              consArr.put(JSONObject().apply {
                put("id", c.id)
                put("medicineId", c.medicineId)
                put("name", c.name)
                put("quantity", c.quantity)
                put("unitPrice", c.unitPrice)
              })
            }
            put("consumables", consArr)
          }
          sessArr.put(so)
        }
        put("sessions", sessArr)
      }
      tcArr.put(tco)
    }
    root.put("treatmentCards", tcArr)

    // Expenses
    val expArr = JSONArray()
    expenses.forEach { e ->
      val eo = JSONObject().apply {
        put("id", e.id)
        put("voucherNo", e.voucherNo)
        put("date", e.date)
        put("category", e.category)
        put("title", e.title)
        put("amount", e.amount)
        put("payee", e.payee)
        put("paymentMethod", e.paymentMethod.name)
        put("recordedBy", e.recordedBy)
        put("notes", e.notes)
      }
      expArr.put(eo)
    }
    root.put("expenses", expArr)

    // Procedures
    val procArr = JSONArray()
    procedures.forEach { p ->
      val po = JSONObject().apply {
        put("id", p.id)
        put("code", p.code)
        put("name", p.name)
        put("myanmarName", p.myanmarName)
        put("category", p.category)
        put("price", p.price)
        put("standardFee", p.standardFee)
        put("consumableCost", p.consumableCost)
        put("notes", p.notes)
        put("dateTime", p.dateTime)

        val conArr = JSONArray()
        p.consumableItems.forEach { c ->
          conArr.put(JSONObject().apply {
            put("id", c.id)
            put("medicineId", c.medicineId)
            put("name", c.name)
            put("quantity", c.quantity)
            put("unitPrice", c.unitPrice)
            put("unitType", c.unitType)
          })
        }
        put("consumableItems", conArr)
      }
      procArr.put(po)
    }
    root.put("procedures", procArr)

    // Suppliers
    val supArr = JSONArray()
    suppliers.forEach { s ->
      val so = JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        put("contactPerson", s.contactPerson)
        put("phone", s.phone)
        put("address", s.address)
        put("email", s.email)
      }
      supArr.put(so)
    }
    root.put("suppliers", supArr)

    // Vaccines
    val vacArr = JSONArray()
    vaccines.forEach { v ->
      val vo = JSONObject().apply {
        put("id", v.id)
        put("patientId", v.patientId)
        put("patientName", v.patientName)
        put("vaccineName", v.vaccineName)
        put("doseNumber", v.doseNumber)
        put("givenDate", v.givenDate)
        put("nextDueDate", v.nextDueDate ?: "")
        put("batchNumber", v.batchNumber)
        put("givenBy", v.givenBy)
        put("remarks", v.remarks)
        put("regimeName", v.regimeName)
      }
      vacArr.put(vo)
    }
    root.put("vaccines", vacArr)

    if (clinicSettings != null) {
      val so = JSONObject().apply {
        put("clinicNameEn", clinicSettings.clinicNameEn)
        put("clinicNameMm", clinicSettings.clinicNameMm)
        put("clinicSubtitleMm", clinicSettings.clinicSubtitleMm)
        put("clinicAddress", clinicSettings.clinicAddress)
        put("clinicPhone", clinicSettings.clinicPhone)
        put("doctorName", clinicSettings.doctorName)
        put("doctorDegree", clinicSettings.doctorDegree)
        put("doctorSama", clinicSettings.doctorSama)
        put("clinicLogoUri", clinicSettings.clinicLogoUri)
        put("clinicLogoBase64", clinicSettings.clinicLogoBase64)
      }
      root.put("clinicSettings", so)
    }

    return root.toString()
  }
}
