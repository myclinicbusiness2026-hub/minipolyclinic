package com.example.sync

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object SyncPayloadParser {

  fun parsePatients(arr: JSONArray?): List<Patient> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Patient>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val vo = o.optJSONObject("vitals")
      val vitals = if (vo != null) {
        VitalSigns(
          bloodPressure = vo.optString("bp", "120/80 mmHg"),
          pulseRate = vo.optInt("pr", 76),
          temperatureCelsius = vo.optDouble("temp", 98.4),
          oxygenSaturation = vo.optInt("spo2", 98),
          weightKg = vo.optDouble("weight", 60.0),
          rbs = vo.optDouble("rbs", 110.0),
          heightCm = vo.optDouble("height", 165.0)
        )
      } else VitalSigns()

      list.add(
        Patient(
          id = o.optString("id", UUID.randomUUID().toString()),
          tokenNumber = o.optInt("tokenNumber", 1),
          hn = o.optString("hn", "HN-1000"),
          queueNumber = o.optInt("queueNumber", o.optInt("tokenNumber", 1)),
          name = o.optString("name", "Unknown"),
          age = o.optInt("age", 25),
          gender = try { Gender.valueOf(o.optString("gender", "MALE")) } catch (_: Exception) { Gender.MALE },
          phone = o.optString("phone", ""),
          address = o.optString("address", "Mandalay"),
          status = try { QueueStatus.valueOf(o.optString("status", "WAITING")) } catch (_: Exception) { QueueStatus.WAITING },
          registrationTime = o.optString("registrationTime", ""),
          vitals = vitals,
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
    return list
  }

  fun parseConsultations(arr: JSONArray?): List<ConsultationRecord> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ConsultationRecord>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val rxArr = o.optJSONArray("prescriptions")
      val rxs = mutableListOf<PrescribedDrug>()
      if (rxArr != null) {
        for (j in 0 until rxArr.length()) {
          val ro = rxArr.getJSONObject(j)
          rxs.add(
            PrescribedDrug(
              medicineId = ro.optString("medicineId"),
              medicineName = ro.optString("medicineName"),
              dosage = ro.optString("dosage"),
              frequency = ro.optString("frequency"),
              duration = ro.optString("duration"),
              quantity = ro.optInt("quantity", 1),
              unitPrice = ro.optDouble("unitPrice", 0.0),
              instructions = ro.optString("instructions", "")
            )
          )
        }
      }

      val atArr = o.optJSONArray("attachments")
      val attachments = mutableListOf<MedicalAttachment>()
      if (atArr != null) {
        for (j in 0 until atArr.length()) {
          val ao = atArr.getJSONObject(j)
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

      val dArr = o.optJSONArray("diagnosisList")
      val diagnoses = mutableListOf<String>()
      if (dArr != null) {
        for (j in 0 until dArr.length()) diagnoses.add(dArr.getString(j))
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
          prescriptions = rxs,
          attachments = attachments,
          consultationFee = o.optDouble("consultationFee", 8000.0),
          timestamp = o.optString("timestamp")
        )
      )
    }
    return list
  }

  fun parseInvoices(arr: JSONArray?): List<Invoice> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Invoice>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val itemArr = o.optJSONArray("items")
      val items = mutableListOf<InvoiceItem>()
      if (itemArr != null) {
        for (j in 0 until itemArr.length()) {
          val io = itemArr.getJSONObject(j)
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
          totalAmount = o.optDouble("totalAmount", 0.0),
          paymentMethod = PaymentMethod.fromString(o.optString("paymentMethod", "Cash")),
          paymentStatus = o.optString("paymentStatus", "Paid"),
          paidAmount = o.optDouble("paidAmount", 0.0),
          changeAmount = o.optDouble("changeAmount", 0.0),
          cashierName = o.optString("cashierName", "Staff"),
          timestamp = o.optString("timestamp")
        )
      )
    }
    return list
  }

  fun parseStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    val list = mutableListOf<String>()
    for (i in 0 until arr.length()) {
      val s = arr.optString(i)
      if (s.isNotBlank()) list.add(s)
    }
    return list
  }

  fun parseMedicines(arr: JSONArray?): List<Medicine> {
    if (arr == null) return emptyList()
    val list = mutableListOf<Medicine>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val batchArr = o.optJSONArray("batches")
      val batches = mutableListOf<StockBatch>()
      if (batchArr != null) {
        for (j in 0 until batchArr.length()) {
          val bo = batchArr.getJSONObject(j)
          batches.add(
            StockBatch(
              batchNumber = bo.optString("batchNumber", "B1"),
              expiryDate = bo.optString("expiryDate", "2027-12-31"),
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
          id = o.optString("id", UUID.randomUUID().toString()),
          name = o.optString("name", "Unknown"),
          genericName = o.optString("genericName", ""),
          dosageForm = o.optString("dosageForm", "Tablet"),
          category = o.optString("category", "General"),
          unitType = o.optString("unitType", "Tab"),
          profitMarginPercent = o.optDouble("profitMarginPercent", 25.0),
          batches = batches,
          minStockThreshold = o.optInt("minStockThreshold", 10),
          primarySupplier = o.optString("primarySupplier", ""),
          isAllergyWarning = o.optBoolean("isAllergyWarning", false)
        )
      )
    }
    return list
  }

  fun parseUsers(arr: JSONArray?): List<UserAccount> {
    if (arr == null) return emptyList()
    val list = mutableListOf<UserAccount>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val role = try {
        UserRole.valueOf(o.optString("role", "DOCTOR"))
      } catch (_: Exception) {
        UserRole.DOCTOR
      }
      list.add(
        UserAccount(
          id = o.optString("id", UUID.randomUUID().toString()),
          username = o.optString("username", "user"),
          fullName = o.optString("fullName", "Staff"),
          role = role,
          samaNumber = o.optString("samaNumber", ""),
          degree = o.optString("degree", ""),
          pinCode = o.optString("pinCode", "1234"),
          passwordHash = o.optString("passwordHash", "123456"),
          phone = o.optString("phone", ""),
          department = o.optString("department", "General OPD"),
          isActive = o.optBoolean("isActive", true)
        )
      )
    }
    return list
  }
}
