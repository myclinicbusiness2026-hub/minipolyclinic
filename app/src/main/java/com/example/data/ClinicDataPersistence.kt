package com.example.data

import android.content.Context
import com.example.model.*
import com.example.ui.screens.treatment.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ClinicDataPersistence {
  private const val PREF_NAME = "shwe_khit_clinic_prefs"
  private const val KEY_PATIENTS = "patients"
  private const val KEY_MEDICINES = "medicines"
  private const val KEY_PROCEDURES = "procedures"
  private const val KEY_CONSULTATIONS = "consultations"
  private const val KEY_INVOICES = "invoices"
  private const val KEY_USERS = "users"
  private const val KEY_EXPENSES = "expenses"
  private const val KEY_VACCINES = "vaccines"
  private const val KEY_SUPPLIERS = "suppliers"
  private const val KEY_DELETED_PATIENT_IDS = "deleted_patient_ids"
  private const val KEY_DELETED_CONSULTATION_IDS = "deleted_consultation_ids"
  private const val KEY_DELETED_INVOICE_IDS = "deleted_invoice_ids"
  private const val KEY_DELETED_MEDICINE_IDS = "deleted_medicine_ids"
  private const val KEY_TREATMENT_CARDS = "treatment_cards"
  private const val KEY_CLINIC_SETTINGS = "clinic_settings"

  private fun getPrefs(context: Context) =
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  // 1.5 Deleted Records Tracking (Tombstones for Local Wi-Fi Sync)
  fun saveDeletedPatientIds(context: Context, ids: Set<String>) {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    getPrefs(context).edit().putString(KEY_DELETED_PATIENT_IDS, arr.toString()).apply()
  }

  fun loadDeletedPatientIds(context: Context): MutableSet<String> {
    val str = getPrefs(context).getString(KEY_DELETED_PATIENT_IDS, null) ?: return mutableSetOf()
    return try {
      val arr = JSONArray(str)
      val set = mutableSetOf<String>()
      for (i in 0 until arr.length()) {
        val id = arr.optString(i)
        if (id.isNotBlank()) set.add(id)
      }
      set
    } catch (_: Exception) {
      mutableSetOf()
    }
  }

  fun addDeletedPatientId(context: Context, id: String) {
    if (id.isBlank()) return
    val current = loadDeletedPatientIds(context)
    current.add(id)
    saveDeletedPatientIds(context, current)
  }

  fun removeDeletedPatientId(context: Context, id: String) {
    if (id.isBlank()) return
    val current = loadDeletedPatientIds(context)
    if (current.remove(id)) {
      saveDeletedPatientIds(context, current)
    }
  }

  fun saveDeletedConsultationIds(context: Context, ids: Set<String>) {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    getPrefs(context).edit().putString(KEY_DELETED_CONSULTATION_IDS, arr.toString()).apply()
  }

  fun loadDeletedConsultationIds(context: Context): MutableSet<String> {
    val str = getPrefs(context).getString(KEY_DELETED_CONSULTATION_IDS, null) ?: return mutableSetOf()
    return try {
      val arr = JSONArray(str)
      val set = mutableSetOf<String>()
      for (i in 0 until arr.length()) {
        val id = arr.optString(i)
        if (id.isNotBlank()) set.add(id)
      }
      set
    } catch (_: Exception) {
      mutableSetOf()
    }
  }

  fun addDeletedConsultationId(context: Context, id: String) {
    if (id.isBlank()) return
    val current = loadDeletedConsultationIds(context)
    current.add(id)
    saveDeletedConsultationIds(context, current)
  }

  fun removeDeletedConsultationId(context: Context, id: String) {
    if (id.isBlank()) return
    val current = loadDeletedConsultationIds(context)
    if (current.remove(id)) {
      saveDeletedConsultationIds(context, current)
    }
  }

  fun saveDeletedInvoiceIds(context: Context, ids: Set<String>) {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    getPrefs(context).edit().putString(KEY_DELETED_INVOICE_IDS, arr.toString()).apply()
  }

  fun loadDeletedInvoiceIds(context: Context): MutableSet<String> {
    val str = getPrefs(context).getString(KEY_DELETED_INVOICE_IDS, null) ?: return mutableSetOf()
    return try {
      val arr = JSONArray(str)
      val set = mutableSetOf<String>()
      for (i in 0 until arr.length()) {
        val id = arr.optString(i)
        if (id.isNotBlank()) set.add(id)
      }
      set
    } catch (_: Exception) {
      mutableSetOf()
    }
  }

  fun addDeletedInvoiceId(context: Context, id: String) {
    if (id.isBlank()) return
    val current = loadDeletedInvoiceIds(context)
    current.add(id)
    saveDeletedInvoiceIds(context, current)
  }

  fun saveDeletedMedicineIds(context: Context, ids: Set<String>) {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    getPrefs(context).edit().putString(KEY_DELETED_MEDICINE_IDS, arr.toString()).apply()
  }

  fun loadDeletedMedicineIds(context: Context): MutableSet<String> {
    val str = getPrefs(context).getString(KEY_DELETED_MEDICINE_IDS, null) ?: return mutableSetOf()
    return try {
      val arr = JSONArray(str)
      val set = mutableSetOf<String>()
      for (i in 0 until arr.length()) {
        val id = arr.optString(i)
        if (id.isNotBlank()) set.add(id)
      }
      set
    } catch (_: Exception) {
      mutableSetOf()
    }
  }

  fun addDeletedMedicineId(context: Context, id: String) {
    if (id.isBlank()) return
    val current = loadDeletedMedicineIds(context)
    current.add(id)
    saveDeletedMedicineIds(context, current)
  }

  // 1. Patients
  fun savePatients(context: Context, list: List<Patient>) {
    val arr = JSONArray()
    list.forEach { p ->
      val obj = JSONObject().apply {
        put("id", p.id)
        put("tokenNumber", p.tokenNumber)
        put("hn", p.hn)
        put("queueNumber", p.queueNumber)
        put("name", p.name)
        put("age", p.age)
        put("gender", p.gender.name)
        put("phone", p.phone)
        put("address", p.address)
        put("status", p.status.name)
        put("registrationTime", p.registrationTime)
        put("notes", p.notes)
        put("chiefComplaint", p.chiefComplaint)
        put("followUpDueDate", p.followUpDueDate ?: "")
        put("isReminderSent", p.isReminderSent)
        put("isFollowUp", p.isFollowUp)
        put("followUpReason", p.followUpReason)
        put("bloodType", p.bloodType)
        put("knownAllergies", JSONArray(p.knownAllergies))
        put("chronicDiseases", JSONArray(p.chronicDiseases))

        val v = JSONObject().apply {
          put("bp", p.vitals.bloodPressure)
          put("pr", p.vitals.pulseRate)
          put("temp", p.vitals.temperatureCelsius)
          put("spo2", p.vitals.oxygenSaturation)
          put("weight", p.vitals.weightKg)
          put("rbs", p.vitals.rbs)
          put("height", p.vitals.heightCm)
        }
        put("vitals", v)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_PATIENTS, arr.toString()).apply()
  }

  fun loadPatients(context: Context): List<Patient>? {
    val str = getPrefs(context).getString(KEY_PATIENTS, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<Patient>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val vObj = o.optJSONObject("vitals")
        val vitals = if (vObj != null) {
          VitalSigns(
            bloodPressure = vObj.optString("bp", "120/80 mmHg"),
            pulseRate = vObj.optInt("pr", 76),
            temperatureCelsius = vObj.optDouble("temp", 98.4),
            oxygenSaturation = vObj.optInt("spo2", 98),
            weightKg = vObj.optDouble("weight", 60.0),
            rbs = vObj.optDouble("rbs", 110.0),
            heightCm = vObj.optDouble("height", 165.0)
          )
        } else VitalSigns()

        val allergies = mutableListOf<String>()
        val aArr = o.optJSONArray("knownAllergies")
        if (aArr != null) {
          for (j in 0 until aArr.length()) allergies.add(aArr.getString(j))
        }

        val chronics = mutableListOf<String>()
        val cArr = o.optJSONArray("chronicDiseases")
        if (cArr != null) {
          for (j in 0 until cArr.length()) chronics.add(cArr.getString(j))
        }

        val status = try {
          QueueStatus.valueOf(o.optString("status", "WAITING"))
        } catch (_: Exception) {
          QueueStatus.WAITING
        }

        val gender = try {
          Gender.valueOf(o.optString("gender", "MALE"))
        } catch (_: Exception) {
          Gender.MALE
        }

        list.add(
          Patient(
            id = o.optString("id"),
            tokenNumber = o.optInt("tokenNumber", i + 1),
            hn = o.optString("hn", "HN-${i + 1000}"),
            queueNumber = o.optInt("queueNumber", o.optInt("tokenNumber", i + 1)),
            name = o.optString("name", "Unnamed"),
            age = o.optInt("age", 30),
            gender = gender,
            phone = o.optString("phone", ""),
            address = o.optString("address", ""),
            status = status,
            registrationTime = o.optString("registrationTime", ""),
            vitals = vitals,
            knownAllergies = allergies,
            chronicDiseases = chronics,
            notes = o.optString("notes", ""),
            chiefComplaint = o.optString("chiefComplaint", ""),
            followUpDueDate = o.optString("followUpDueDate").ifBlank { null },
            isReminderSent = o.optBoolean("isReminderSent", false),
            isFollowUp = o.optBoolean("isFollowUp", false),
            followUpReason = o.optString("followUpReason", ""),
            bloodType = o.optString("bloodType", "O+")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 2. Medicines
  fun saveMedicines(context: Context, list: List<Medicine>) {
    val arr = JSONArray()
    list.forEach { m ->
      val obj = JSONObject().apply {
        put("id", m.id)
        put("name", m.name)
        put("genericName", m.genericName)
        put("dosageForm", m.dosageForm)
        put("category", m.category)
        put("unitType", m.unitType)
        put("minStockThreshold", m.minStockThreshold)
        put("profitMarginPercent", m.profitMarginPercent)
        put("isAllergyWarning", m.isAllergyWarning)
        put("primarySupplier", m.primarySupplier)

        val bArr = JSONArray()
        m.batches.forEach { b ->
          val bObj = JSONObject().apply {
            put("batchNumber", b.batchNumber)
            put("expiryDate", b.expiryDate)
            put("quantity", b.quantity)
            put("costPricePerUnit", b.costPricePerUnit)
            put("supplierName", b.supplierName ?: "")
            put("lastUpdated", b.lastUpdated)
          }
          bArr.put(bObj)
        }
        put("batches", bArr)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_MEDICINES, arr.toString()).apply()
  }

  fun loadMedicines(context: Context): List<Medicine>? {
    val str = getPrefs(context).getString(KEY_MEDICINES, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<Medicine>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val bArr = o.optJSONArray("batches")
        val batches = mutableListOf<StockBatch>()
        if (bArr != null) {
          for (j in 0 until bArr.length()) {
            val bo = bArr.getJSONObject(j)
            batches.add(
              StockBatch(
                batchNumber = bo.optString("batchNumber"),
                expiryDate = bo.optString("expiryDate"),
                quantity = bo.optInt("quantity", 0),
                costPricePerUnit = bo.optDouble("costPricePerUnit", 0.0),
                supplierName = if (bo.has("supplierName") && !bo.isNull("supplierName")) bo.optString("supplierName") else null,
                lastUpdated = bo.optLong("lastUpdated", System.currentTimeMillis())
              )
            )
          }
        }
        list.add(
          Medicine(
            id = o.optString("id"),
            name = o.optString("name"),
            genericName = o.optString("genericName"),
            dosageForm = o.optString("dosageForm"),
            category = o.optString("category"),
            unitType = o.optString("unitType", "Strip"),
            batches = batches,
            minStockThreshold = o.optInt("minStockThreshold", 20),
            profitMarginPercent = o.optDouble("profitMarginPercent", 25.0),
            isAllergyWarning = o.optBoolean("isAllergyWarning", false),
            primarySupplier = o.optString("primarySupplier", "AA Medical")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 3. Consultations
  fun saveConsultations(context: Context, list: List<ConsultationRecord>) {
    val arr = JSONArray()
    list.forEach { c ->
      val obj = JSONObject().apply {
        put("id", c.id)
        put("patientId", c.patientId)
        put("patientName", c.patientName)
        put("doctorName", c.doctorName)
        put("doctorDegree", c.doctorDegree)
        put("doctorSama", c.doctorSama)
        put("subjective", c.subjective)
        put("objective", c.objective)
        put("assessment", c.assessment)
        put("plan", c.plan)
        put("diagnosisList", JSONArray(c.diagnosisList))
        put("consultationFee", c.consultationFee)
        put("canvasNotes", c.canvasNotes)
        put("followUpAppointment", c.followUpAppointment)
        put("followUpDueDate", c.followUpDueDate ?: "")
        put("nextVaccineSchedule", c.nextVaccineSchedule)
        put("patientInstructions", c.patientInstructions)
        put("timestamp", c.timestamp)

        val rxArr = JSONArray()
        c.prescriptions.forEach { rx ->
          val rObj = JSONObject().apply {
            put("medicineId", rx.medicineId)
            put("medicineName", rx.medicineName)
            put("dosage", rx.dosage)
            put("frequency", rx.frequency)
            put("duration", rx.duration)
            put("quantity", rx.quantity)
            put("unitPrice", rx.unitPrice)
            put("instructions", rx.instructions)
          }
          rxArr.put(rObj)
        }
        put("prescriptions", rxArr)

        val procArr = JSONArray()
        c.selectedProcedures.forEach { p ->
          val pObj = JSONObject().apply {
            put("id", p.id)
            put("code", p.code)
            put("name", p.name)
            put("category", p.category)
            put("price", p.price)
            put("description", p.description)
          }
          procArr.put(pObj)
        }
        put("selectedProcedures", procArr)

        val attArr = JSONArray()
        c.attachments.forEach { a ->
          val aObj = JSONObject().apply {
            put("id", a.id)
            put("title", a.title)
            put("category", a.category)
            put("date", a.date)
            put("imageUri", a.imageUri)
            put("filePath", a.filePath)
            put("fileType", a.fileType)
            put("fileName", a.fileName)
            put("notes", a.notes)
          }
          attArr.put(aObj)
        }
        put("attachments", attArr)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_CONSULTATIONS, arr.toString()).apply()
  }

  fun loadConsultations(context: Context): List<ConsultationRecord>? {
    val str = getPrefs(context).getString(KEY_CONSULTATIONS, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<ConsultationRecord>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val dArr = o.optJSONArray("diagnosisList")
        val diagnoses = mutableListOf<String>()
        if (dArr != null) {
          for (j in 0 until dArr.length()) diagnoses.add(dArr.getString(j))
        }

        val rxArr = o.optJSONArray("prescriptions")
        val prescriptions = mutableListOf<PrescribedDrug>()
        if (rxArr != null) {
          for (j in 0 until rxArr.length()) {
            val ro = rxArr.getJSONObject(j)
            prescriptions.add(
              PrescribedDrug(
                medicineId = ro.optString("medicineId"),
                medicineName = ro.optString("medicineName"),
                dosage = ro.optString("dosage"),
                frequency = ro.optString("frequency"),
                duration = ro.optString("duration"),
                quantity = ro.optInt("quantity", 1),
                unitPrice = ro.optDouble("unitPrice", 0.0),
                instructions = ro.optString("instructions")
              )
            )
          }
        }

        val procArr = o.optJSONArray("selectedProcedures")
        val procedures = mutableListOf<ProcedureItem>()
        if (procArr != null) {
          for (j in 0 until procArr.length()) {
            val po = procArr.getJSONObject(j)
            procedures.add(
              ProcedureItem(
                id = po.optString("id"),
                code = po.optString("code", po.optString("id").take(6).uppercase()),
                name = po.optString("name"),
                category = po.optString("category"),
                price = po.optDouble("price", 0.0),
                description = po.optString("description")
              )
            )
          }
        }

        val attArr = o.optJSONArray("attachments")
        val attachments = mutableListOf<MedicalAttachment>()
        if (attArr != null) {
          for (j in 0 until attArr.length()) {
            val ao = attArr.getJSONObject(j)
            attachments.add(
              MedicalAttachment(
                id = ao.optString("id", UUID.randomUUID().toString()),
                title = ao.optString("title"),
                category = ao.optString("category", "Lab Report"),
                date = ao.optString("date"),
                imageUri = ao.optString("imageUri"),
                filePath = ao.optString("filePath", ao.optString("imageUri")),
                fileType = ao.optString("fileType", "Image"),
                fileName = ao.optString("fileName", ao.optString("title")),
                notes = ao.optString("notes")
              )
            )
          }
        }

        list.add(
          ConsultationRecord(
            id = o.optString("id"),
            patientId = o.optString("patientId"),
            patientName = o.optString("patientName"),
            doctorName = o.optString("doctorName"),
            doctorDegree = o.optString("doctorDegree"),
            doctorSama = o.optString("doctorSama"),
            subjective = o.optString("subjective"),
            objective = o.optString("objective"),
            assessment = o.optString("assessment"),
            plan = o.optString("plan"),
            diagnosisList = diagnoses,
            prescriptions = prescriptions,
            selectedProcedures = procedures,
            consultationFee = o.optDouble("consultationFee", 8000.0),
            canvasNotes = o.optString("canvasNotes"),
            followUpAppointment = o.optString("followUpAppointment"),
            followUpDueDate = o.optString("followUpDueDate").ifBlank { null },
            nextVaccineSchedule = o.optString("nextVaccineSchedule"),
            patientInstructions = o.optString("patientInstructions"),
            attachments = attachments,
            timestamp = o.optString("timestamp")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 4. Invoices
  fun saveInvoices(context: Context, list: List<Invoice>) {
    val arr = JSONArray()
    list.forEach { inv ->
      val obj = JSONObject().apply {
        put("id", inv.id)
        put("invoiceNumber", inv.invoiceNumber)
        put("consultationId", inv.consultationId)
        put("patientId", inv.patientId)
        put("patientName", inv.patientName)
        put("consultationFee", inv.consultationFee)
        put("serviceFee", inv.serviceFee)
        put("discount", inv.discount)
        put("tax", inv.tax)
        put("totalAmount", inv.totalAmount)
        put("paymentMethod", inv.paymentMethod.name)
        put("paymentStatus", inv.paymentStatus)
        put("paidAmount", inv.paidAmount)
        put("changeAmount", inv.changeAmount)
        put("cashierName", inv.cashierName)
        put("timestamp", inv.timestamp)
        put("isInventoryDeducted", inv.isInventoryDeducted)

        val itemsArr = JSONArray()
        inv.items.forEach { item ->
          val iObj = JSONObject().apply {
            put("name", item.name)
            put("quantity", item.quantity)
            put("unitPrice", item.unitPrice)
            put("totalPrice", item.totalPrice)
            put("isProcedure", item.isProcedure)
          }
          itemsArr.put(iObj)
        }
        put("items", itemsArr)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_INVOICES, arr.toString()).apply()
  }

  fun loadInvoices(context: Context): List<Invoice>? {
    val str = getPrefs(context).getString(KEY_INVOICES, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<Invoice>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val itemsArr = o.optJSONArray("items")
        val items = mutableListOf<InvoiceItem>()
        if (itemsArr != null) {
          for (j in 0 until itemsArr.length()) {
            val io = itemsArr.getJSONObject(j)
            items.add(
              InvoiceItem(
                name = io.optString("name"),
                quantity = io.optInt("quantity", 1),
                unitPrice = io.optDouble("unitPrice", 0.0),
                totalPrice = io.optDouble("totalPrice", 0.0),
                isProcedure = io.optBoolean("isProcedure", false)
              )
            )
          }
        }
        list.add(
          Invoice(
            id = o.optString("id"),
            invoiceNumber = o.optString("invoiceNumber"),
            consultationId = o.optString("consultationId"),
            patientId = o.optString("patientId"),
            patientName = o.optString("patientName"),
            items = items,
            consultationFee = o.optDouble("consultationFee", 8000.0),
            serviceFee = o.optDouble("serviceFee", 0.0),
            discount = o.optDouble("discount", 0.0),
            tax = o.optDouble("tax", 0.0),
            totalAmount = o.optDouble("totalAmount", 0.0),
            paymentMethod = PaymentMethod.fromString(o.optString("paymentMethod", "Cash")),
            paymentStatus = o.optString("paymentStatus", "Paid"),
            paidAmount = o.optDouble("paidAmount", 0.0),
            changeAmount = o.optDouble("changeAmount", 0.0),
            cashierName = o.optString("cashierName", "Staff"),
            timestamp = o.optString("timestamp"),
            isInventoryDeducted = o.optBoolean("isInventoryDeducted", false)
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 5. Users
  fun saveUsers(context: Context, list: List<UserAccount>) {
    val arr = JSONArray()
    list.forEach { u ->
      val obj = JSONObject().apply {
        put("id", u.id)
        put("username", u.username)
        put("fullName", u.fullName)
        put("role", u.role.name)
        put("samaNumber", u.samaNumber)
        put("degree", u.degree)
        put("pinCode", u.pinCode)
        put("passwordHash", u.passwordHash)
        put("isActive", u.isActive)
        put("phone", u.phone)
        put("email", u.email)
        put("department", u.department)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_USERS, arr.toString()).apply()
  }

  fun loadUsers(context: Context): List<UserAccount>? {
    val str = getPrefs(context).getString(KEY_USERS, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<UserAccount>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val role = try {
          UserRole.valueOf(o.optString("role", "DOCTOR"))
        } catch (_: Exception) {
          UserRole.DOCTOR
        }
        val passHash = o.optString("passwordHash", o.optString("pinCode", "123456"))
        list.add(
          UserAccount(
            id = o.optString("id"),
            username = o.optString("username"),
            fullName = o.optString("fullName"),
            role = role,
            samaNumber = o.optString("samaNumber"),
            degree = o.optString("degree"),
            pinCode = o.optString("pinCode", "123456"),
            passwordHash = passHash,
            isActive = o.optBoolean("isActive", true),
            phone = o.optString("phone", ""),
            email = o.optString("email", ""),
            department = o.optString("department", "")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 6. Day-Care Treatment Cards
  fun saveTreatmentCards(context: Context, list: List<TreatmentCard>) {
    val arr = JSONArray()
    list.forEach { card ->
      val obj = JSONObject().apply {
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
        put("cardStatus", card.cardStatus.name)
        put("discontinueReason", card.discontinueReason)
        put("discontinuedBy", card.discontinuedBy)
        put("discontinuedAt", card.discontinuedAt)
        put("notes", card.notes)

        val sArr = JSONArray()
        card.sessions.forEach { s ->
          val sObj = JSONObject().apply {
            put("id", s.id)
            put("sessionNumber", s.sessionNumber)
            put("scheduledDate", s.scheduledDate)
            put("scheduledTime", s.scheduledTime)
            put("administeredAt", s.administeredAt ?: "")
            put("administeredBy", s.administeredBy)
            put("status", s.status.name)
            put("isClinicMed", s.isClinicMed)
            put("medicationFee", s.medicationFee)
            put("sessionFee", s.sessionFee)
            put("invoiceId", s.invoiceId ?: "")
            put("notes", s.notes)

            val cArr = JSONArray()
            s.consumables.forEach { c ->
              val cObj = JSONObject().apply {
                put("id", c.id)
                put("medicineId", c.medicineId)
                put("name", c.name)
                put("quantity", c.quantity)
                put("unitPrice", c.unitPrice)
              }
              cArr.put(cObj)
            }
            put("consumables", cArr)
          }
          sArr.put(sObj)
        }
        put("sessions", sArr)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_TREATMENT_CARDS, arr.toString()).apply()
  }

  fun loadTreatmentCards(context: Context): List<TreatmentCard>? {
    val str = getPrefs(context).getString(KEY_TREATMENT_CARDS, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<TreatmentCard>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val sArr = o.optJSONArray("sessions")
        val sessions = mutableListOf<TreatmentSession>()
        if (sArr != null) {
          for (j in 0 until sArr.length()) {
            val so = sArr.getJSONObject(j)
            val cArr = so.optJSONArray("consumables")
            val consumables = mutableListOf<TreatmentConsumable>()
            if (cArr != null) {
              for (k in 0 until cArr.length()) {
                val co = cArr.getJSONObject(k)
                consumables.add(
                  TreatmentConsumable(
                    id = co.optString("id", UUID.randomUUID().toString()),
                    medicineId = co.optString("medicineId"),
                    name = co.optString("name"),
                    quantity = co.optInt("quantity", 1),
                    unitPrice = co.optDouble("unitPrice", 0.0)
                  )
                )
              }
            }
            val status = try {
              TreatmentStatus.valueOf(so.optString("status", "PENDING"))
            } catch (_: Exception) {
              TreatmentStatus.PENDING
            }
            sessions.add(
              TreatmentSession(
                id = so.optString("id", UUID.randomUUID().toString()),
                sessionNumber = so.optInt("sessionNumber", j + 1),
                scheduledDate = so.optString("scheduledDate"),
                scheduledTime = so.optString("scheduledTime", "08:00 AM"),
                administeredAt = so.optString("administeredAt").ifBlank { null },
                administeredBy = so.optString("administeredBy"),
                status = status,
                isClinicMed = so.optBoolean("isClinicMed", true),
                medicationFee = so.optDouble("medicationFee", 0.0),
                sessionFee = so.optDouble("sessionFee", 3000.0),
                consumables = consumables,
                invoiceId = so.optString("invoiceId").ifBlank { null },
                notes = so.optString("notes")
              )
            )
          }
        }
        val route = try {
          TreatmentRoute.valueOf(o.optString("route", "IM"))
        } catch (_: Exception) {
          TreatmentRoute.IM
        }
        val cardStatus = try {
          TreatmentStatus.valueOf(o.optString("cardStatus", "IN_PROGRESS"))
        } catch (_: Exception) {
          TreatmentStatus.IN_PROGRESS
        }
        list.add(
          TreatmentCard(
            id = o.optString("id"),
            patientId = o.optString("patientId"),
            patientName = o.optString("patientName"),
            patientHn = o.optString("patientHn"),
            doctorName = o.optString("doctorName"),
            prescribedDate = o.optString("prescribedDate"),
            medicationId = o.optString("medicationId"),
            medicationName = o.optString("medicationName"),
            dosage = o.optString("dosage"),
            route = route,
            frequency = o.optString("frequency", "12 hrly"),
            totalDays = o.optInt("totalDays", 3),
            totalSessions = o.optInt("totalSessions", 6),
            sessions = sessions,
            instructions = o.optString("instructions"),
            sessionFee = o.optDouble("sessionFee", 3000.0),
            medicationFee = o.optDouble("medicationFee", 0.0),
            isBilledPerSession = o.optBoolean("isBilledPerSession", true),
            cardStatus = cardStatus,
            discontinueReason = o.optString("discontinueReason"),
            discontinuedBy = o.optString("discontinuedBy"),
            discontinuedAt = o.optString("discontinuedAt"),
            notes = o.optString("notes")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 7. Expenses
  fun saveExpenses(context: Context, list: List<ExpenseRecord>) {
    val arr = JSONArray()
    list.forEach { e ->
      val obj = JSONObject().apply {
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
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_EXPENSES, arr.toString()).apply()
  }

  fun loadExpenses(context: Context): List<ExpenseRecord>? {
    val str = getPrefs(context).getString(KEY_EXPENSES, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<ExpenseRecord>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
          ExpenseRecord(
            id = o.optString("id", UUID.randomUUID().toString()),
            voucherNo = o.optString("voucherNo"),
            date = o.optString("date"),
            category = o.optString("category", "General"),
            title = o.optString("title"),
            amount = o.optDouble("amount", 0.0),
            payee = o.optString("payee"),
            paymentMethod = try { PaymentMethod.valueOf(o.optString("paymentMethod", "CASH")) } catch (_: Exception) { PaymentMethod.CASH },
            recordedBy = o.optString("recordedBy"),
            notes = o.optString("notes")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 8. Vaccines
  fun saveVaccines(context: Context, list: List<VaccineRecord>) {
    val arr = JSONArray()
    list.forEach { v ->
      val obj = JSONObject().apply {
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
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_VACCINES, arr.toString()).apply()
  }

  fun loadVaccines(context: Context): List<VaccineRecord>? {
    val str = getPrefs(context).getString(KEY_VACCINES, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<VaccineRecord>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
          VaccineRecord(
            id = o.optString("id", UUID.randomUUID().toString()),
            patientId = o.optString("patientId"),
            patientName = o.optString("patientName", "Patient"),
            vaccineName = o.optString("vaccineName"),
            doseNumber = o.optString("doseNumber", "Dose 1"),
            givenDate = o.optString("givenDate"),
            nextDueDate = if (o.has("nextDueDate") && o.getString("nextDueDate").isNotBlank()) o.getString("nextDueDate") else null,
            batchNumber = o.optString("batchNumber"),
            givenBy = o.optString("givenBy", "Nurse"),
            remarks = o.optString("remarks"),
            regimeName = o.optString("regimeName")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 10.5 Suppliers
  fun saveSuppliers(context: Context, list: List<Supplier>) {
    val arr = JSONArray()
    list.forEach { s ->
      val obj = JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        put("contactPerson", s.contactPerson)
        put("phone", s.phone)
        put("address", s.address)
        put("email", s.email)
      }
      arr.put(obj)
    }
    getPrefs(context).edit().putString(KEY_SUPPLIERS, arr.toString()).apply()
  }

  fun loadSuppliers(context: Context): List<Supplier>? {
    val str = getPrefs(context).getString(KEY_SUPPLIERS, null) ?: return null
    return try {
      val arr = JSONArray(str)
      val list = mutableListOf<Supplier>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
          Supplier(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name"),
            contactPerson = o.optString("contactPerson"),
            phone = o.optString("phone"),
            address = o.optString("address", "Mandalay"),
            email = o.optString("email")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // 11. Clinic Settings & Default Fee Configuration
  fun saveClinicSettings(context: Context, s: ClinicSettings) {
    val obj = JSONObject().apply {
      put("clinicNameEn", s.clinicNameEn)
      put("clinicNameMm", s.clinicNameMm)
      put("clinicSubtitleMm", s.clinicSubtitleMm)
      put("addressMm", s.addressMm)
      put("phonePrimary", s.phonePrimary)
      put("phoneSecondary", s.phoneSecondary)
      put("clinicHours", s.clinicHours)
      put("clinicRegNo", s.clinicRegNo)
      put("doctorName", s.doctorName)
      put("doctorDegree", s.doctorDegree)
      put("doctorSama", s.doctorSama)
      put("defaultConsultationFee", s.defaultConsultationFee)
      put("defaultImInjectionFee", s.defaultImInjectionFee)
      put("defaultIvPushFee", s.defaultIvPushFee)
      put("defaultIvInfusionFee", s.defaultIvInfusionFee)
      put("defaultNebulizerFee", s.defaultNebulizerFee)
      put("defaultDepoFee", s.defaultDepoFee)
      put("defaultDocumentationFee", s.defaultDocumentationFee)
      put("allowQuickLogin", s.allowQuickLogin)
      put("thermalPaperWidthMm", s.thermalPaperWidthMm)
      put("receiptGreetingMm", s.receiptGreetingMm)
      put("clinicLogoUri", s.clinicLogoUri)
      put("clinicLogoBase64", s.clinicLogoBase64)
      put("bluetoothPrinterMac", s.bluetoothPrinterMac)
      put("wifiHubHostIp", s.wifiHubHostIp)
      put("wifiSyncPort", s.wifiSyncPort)
      put("supabaseUrl", s.supabaseUrl)
      put("supabaseAnonKey", s.supabaseAnonKey)
      put("enableSupabaseSync", s.enableSupabaseSync)
      put("activeStationName", s.activeStationName)
    }
    getPrefs(context).edit().putString(KEY_CLINIC_SETTINGS, obj.toString()).apply()
  }

  fun loadClinicSettings(context: Context): ClinicSettings {
    val str = getPrefs(context).getString(KEY_CLINIC_SETTINGS, null) ?: return ClinicSettings()
    return try {
      val o = JSONObject(str)
      ClinicSettings(
        clinicNameEn = o.optString("clinicNameEn", "Shwe Khit General Clinic"),
        clinicNameMm = o.optString("clinicNameMm", "“ရွှေခေတ်”"),
        clinicSubtitleMm = o.optString("clinicSubtitleMm", "အထွေထွေရောဂါကုဆေးခန်း"),
        addressMm = o.optString("addressMm", "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့"),
        clinicAddress = o.optString("addressMm", "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့"),
        phonePrimary = o.optString("phonePrimary", "09-421001122"),
        phoneSecondary = o.optString("phoneSecondary", "09-790112233"),
        clinicPhone = o.optString("phonePrimary", "09-421001122"),
        clinicHours = o.optString("clinicHours", "8:00 AM – 8:00 PM"),
        clinicRegNo = o.optString("clinicRegNo", "MOHS-MDY-2024"),
        doctorName = o.optString("doctorName", "Dr. Nyan Kaung Sett"),
        doctorDegree = o.optString("doctorDegree", "M.B.,B.S (MDY)"),
        doctorSama = o.optString("doctorSama", "47414"),
        defaultConsultationFee = o.optDouble("defaultConsultationFee", 8000.0),
        defaultImInjectionFee = o.optDouble("defaultImInjectionFee", 2000.0),
        defaultIvPushFee = o.optDouble("defaultIvPushFee", 3000.0),
        defaultIvInfusionFee = o.optDouble("defaultIvInfusionFee", 6000.0),
        defaultNebulizerFee = o.optDouble("defaultNebulizerFee", 3000.0),
        defaultDepoFee = o.optDouble("defaultDepoFee", 2500.0),
        defaultDocumentationFee = o.optDouble("defaultDocumentationFee", 1000.0),
        allowQuickLogin = o.optBoolean("allowQuickLogin", true),
        thermalPaperWidthMm = o.optInt("thermalPaperWidthMm", 80),
        receiptGreetingMm = o.optString("receiptGreetingMm", "ကျေးဇူးတင်ပါသည် (Thank you!)"),
        clinicLogoUri = o.optString("clinicLogoUri", ""),
        clinicLogoBase64 = o.optString("clinicLogoBase64", ""),
        bluetoothPrinterMac = o.optString("bluetoothPrinterMac", ""),
        wifiHubHostIp = o.optString("wifiHubHostIp", ""),
        wifiSyncPort = o.optInt("wifiSyncPort", 8080),
        supabaseUrl = o.optString("supabaseUrl", "https://tjpayhhzbrcrkwyahecr.supabase.co"),
        supabaseAnonKey = o.optString("supabaseAnonKey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRqcGF5aGh6YnJjcmt3eWFoZWNyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA2NTcxNTMsImV4cCI6MjEwNjIzMzE1M30.U7YlxEPl26SQt0bKjY2vw3nEWtYEOnAdxHt2L6_FEdU"),
        enableSupabaseSync = o.optBoolean("enableSupabaseSync", true),
        activeStationName = o.optString("activeStationName", "Doctor Consultation Station")
      )
    } catch (_: Exception) {
      ClinicSettings()
    }
  }

  fun generateExpensesCsv(expenses: List<ExpenseRecord>): String {
    val sb = StringBuilder()
    sb.append("VoucherNo,Date,Category,Title,Amount,Payee,PaymentMethod,RecordedBy,Notes\n")
    for (e in expenses) {
      val vNo = e.voucherNo.replace(",", " ")
      val d = e.date.replace(",", " ")
      val cat = e.category.replace(",", " ")
      val title = e.title.replace(",", " ")
      val amt = e.amount
      val payee = e.payee.replace(",", " ")
      val pm = e.paymentMethod.name
      val rec = e.recordedBy.replace(",", " ")
      val notes = e.notes.replace(",", " ")
      sb.append("$vNo,$d,$cat,$title,$amt,$payee,$pm,$rec,$notes\n")
    }
    return sb.toString()
  }

  fun generatePatientsCsv(patients: List<Patient>): String {
    val sb = StringBuilder()
    sb.append("TokenNo,HN,Name,Age,Gender,Phone,Address,Status,BP,Pulse,Temp,SPO2,Weight,RBS,Allergies,Notes\n")
    for (p in patients) {
      val name = p.name.replace(",", " ")
      val hn = p.hn.replace(",", " ")
      val phone = p.phone.replace(",", " ")
      val addr = p.address.replace(",", " ")
      val bp = p.vitals.bloodPressure.replace(",", " ")
      val allergies = p.knownAllergies.joinToString("; ").replace(",", " ")
      val notes = p.notes.replace(",", " ")
      sb.append("${p.tokenNumber},$hn,$name,${p.age},${p.gender.name},$phone,$addr,${p.status.name},$bp,${p.vitals.pulseRate},${p.vitals.temperatureCelsius},${p.vitals.oxygenSaturation},${p.vitals.weightKg},${p.vitals.rbs},$allergies,$notes\n")
    }
    return sb.toString()
  }

  fun generateInventoryCsv(medicines: List<Medicine>): String {
    val sb = StringBuilder()
    sb.append("Name,GenericName,DosageForm,Category,UnitType,TotalStock,MinThreshold,CostPrice,SellingPrice,Supplier\n")
    for (m in medicines) {
      val name = m.name.replace(",", " ")
      val generic = m.genericName.replace(",", " ")
      val form = m.dosageForm.replace(",", " ")
      val cat = m.category.replace(",", " ")
      val unit = m.unitType.replace(",", " ")
      val sup = m.primarySupplier.replace(",", " ")
      sb.append("$name,$generic,$form,$cat,$unit,${m.totalStock},${m.minStockThreshold},${m.effectiveCostPrice},${m.sellingPrice},$sup\n")
    }
    return sb.toString()
  }

  fun generateConsultationsCsv(consultations: List<ConsultationRecord>): String {
    val sb = StringBuilder()
    sb.append("ID,PatientName,DoctorName,Diagnoses,Subjective,Objective,Assessment,Plan,ConsultationFee,FollowUp\n")
    for (c in consultations) {
      val pat = c.patientName.replace(",", " ")
      val doc = c.doctorName.replace(",", " ")
      val diag = c.diagnosisList.joinToString("; ").replace(",", " ")
      val s = c.subjective.replace(",", " ")
      val o = c.objective.replace(",", " ")
      val a = c.assessment.replace(",", " ")
      val p = c.plan.replace(",", " ")
      val fu = c.followUpAppointment.replace(",", " ")
      sb.append("${c.id},$pat,$doc,$diag,$s,$o,$a,$p,${c.consultationFee},$fu\n")
    }
    return sb.toString()
  }

  fun generateInvoicesCsv(invoices: List<Invoice>): String {
    val sb = StringBuilder()
    sb.append("InvoiceNo,PatientName,TotalAmount,PaidAmount,ChangeAmount,PaymentMethod,PaymentStatus,Cashier,Date\n")
    for (inv in invoices) {
      val invNo = inv.invoiceNumber.replace(",", " ")
      val pat = inv.patientName.replace(",", " ")
      val cashier = inv.cashierName.replace(",", " ")
      val date = inv.timestamp.replace(",", " ")
      sb.append("$invNo,$pat,${inv.totalAmount},${inv.paidAmount},${inv.changeAmount},${inv.paymentMethod.name},${inv.paymentStatus},$cashier,$date\n")
    }
    return sb.toString()
  }

  fun generateFullBackupJson(
    context: Context,
    patients: List<Patient> = loadPatients(context) ?: emptyList(),
    medicines: List<Medicine> = loadMedicines(context) ?: emptyList(),
    consultations: List<ConsultationRecord> = loadConsultations(context) ?: emptyList(),
    invoices: List<Invoice> = loadInvoices(context) ?: emptyList(),
    vaccines: List<VaccineRecord> = emptyList()
  ): String {
    val root = JSONObject()
    root.put("exportTime", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
    root.put("patientCount", patients.size)
    root.put("medicineCount", medicines.size)
    root.put("consultationCount", consultations.size)
    root.put("invoiceCount", invoices.size)

    val patArr = JSONArray()
    patients.forEach { p ->
      val po = JSONObject()
      po.put("id", p.id)
      po.put("tokenNumber", p.tokenNumber)
      po.put("name", p.name)
      po.put("age", p.age)
      po.put("gender", p.gender.name)
      po.put("phone", p.phone)
      po.put("address", p.address)
      po.put("status", p.status.name)
      po.put("notes", p.notes)
      patArr.put(po)
    }
    root.put("patients", patArr)

    val medArr = JSONArray()
    medicines.forEach { m ->
      val mo = JSONObject()
      mo.put("id", m.id)
      mo.put("name", m.name)
      mo.put("genericName", m.genericName)
      mo.put("category", m.category)
      medArr.put(mo)
    }
    root.put("medicines", medArr)

    val invArr = JSONArray()
    invoices.forEach { inv ->
      val io = JSONObject()
      io.put("id", inv.id)
      io.put("invoiceNumber", inv.invoiceNumber)
      io.put("patientName", inv.patientName)
      io.put("totalAmount", inv.totalAmount)
      invArr.put(io)
    }
    root.put("invoices", invArr)

    return root.toString(2)
  }
}

