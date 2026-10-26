package com.example.sync

import com.example.model.*
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
  val clinicSettings: ClinicSettings? = null,
  val deletedPatientIds: List<String> = emptyList(),
  val deletedConsultationIds: List<String> = emptyList(),
  val deletedInvoiceIds: List<String> = emptyList(),
  val deletedMedicineIds: List<String> = emptyList()
) {
  fun toJsonString(): String {
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
      co.put("patientInstructions", c.patientInstructions)

      val rxArr = JSONArray()
      c.prescriptions.forEach { rx ->
        val ro = JSONObject()
        ro.put("medicineId", rx.medicineId)
        ro.put("medicineName", rx.medicineName)
        ro.put("dosage", rx.dosage)
        ro.put("frequency", rx.frequency)
        ro.put("duration", rx.duration)
        ro.put("quantity", rx.quantity)
        ro.put("unitPrice", rx.unitPrice)
        ro.put("instructions", rx.instructions)
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
        atArr.put(ao)
      }
      co.put("attachments", atArr)

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
