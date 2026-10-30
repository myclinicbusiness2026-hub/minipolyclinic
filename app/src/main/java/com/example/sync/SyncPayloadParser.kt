package com.example.sync

import com.example.model.*
import com.example.ui.screens.treatment.*
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
          rbs = if (vo.has("rbs") && !vo.isNull("rbs")) vo.getDouble("rbs") else null,
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

      val procArr = o.optJSONArray("selectedProcedures") ?: o.optJSONArray("procedures")
      val procedures = mutableListOf<ProcedureItem>()
      if (procArr != null) {
        for (k in 0 until procArr.length()) {
          val po = procArr.getJSONObject(k)
          procedures.add(
            ProcedureItem(
              id = po.optString("id", UUID.randomUUID().toString()),
              code = po.optString("code"),
              name = po.optString("name"),
              myanmarName = po.optString("myanmarName", po.optString("name")),
              category = po.optString("category", "Minor Surgery"),
              price = po.optDouble("price", 0.0),
              standardFee = po.optDouble("standardFee", po.optDouble("price", 0.0)),
              consumableCost = po.optDouble("consumableCost", 0.0),
              notes = po.optString("notes"),
              dateTime = po.optString("dateTime"),
              description = po.optString("description")
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
          prescriptions = rxs,
          selectedProcedures = procedures,
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

  fun parseTreatmentCards(arr: JSONArray?): List<TreatmentCard> {
    if (arr == null) return emptyList()
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
              sessionFee = so.optDouble("sessionFee", 0.0),
              invoiceId = so.optString("invoiceId").ifBlank { null },
              notes = so.optString("notes"),
              consumables = consumables
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
        TreatmentStatus.valueOf(o.optString("cardStatus", "PENDING"))
      } catch (_: Exception) {
        TreatmentStatus.PENDING
      }

      list.add(
        TreatmentCard(
          id = o.optString("id", UUID.randomUUID().toString()),
          patientId = o.optString("patientId"),
          patientName = o.optString("patientName"),
          patientHn = o.optString("patientHn"),
          doctorName = o.optString("doctorName"),
          prescribedDate = o.optString("prescribedDate"),
          medicationId = o.optString("medicationId"),
          medicationName = o.optString("medicationName"),
          dosage = o.optString("dosage"),
          route = route,
          frequency = o.optString("frequency"),
          totalDays = o.optInt("totalDays", 1),
          totalSessions = o.optInt("totalSessions", 1),
          instructions = o.optString("instructions"),
          sessionFee = o.optDouble("sessionFee", 0.0),
          medicationFee = o.optDouble("medicationFee", 0.0),
          isBilledPerSession = o.optBoolean("isBilledPerSession", true),
          cardStatus = cardStatus,
          discontinueReason = o.optString("discontinueReason"),
          discontinuedBy = o.optString("discontinuedBy"),
          discontinuedAt = o.optString("discontinuedAt"),
          notes = o.optString("notes"),
          sessions = sessions
        )
      )
    }
    return list
  }

  fun parseExpenses(arr: JSONArray?): List<ExpenseRecord> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ExpenseRecord>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val paymentMethod = try {
        PaymentMethod.valueOf(o.optString("paymentMethod", "CASH"))
      } catch (_: Exception) {
        PaymentMethod.CASH
      }
      list.add(
        ExpenseRecord(
          id = o.optString("id", UUID.randomUUID().toString()),
          voucherNo = o.optString("voucherNo"),
          date = o.optString("date"),
          category = o.optString("category", "General"),
          title = o.optString("title"),
          amount = o.optDouble("amount", 0.0),
          payee = o.optString("payee"),
          paymentMethod = paymentMethod,
          recordedBy = o.optString("recordedBy"),
          notes = o.optString("notes")
        )
      )
    }
    return list
  }

  fun parseProcedures(arr: JSONArray?): List<ProcedureItem> {
    if (arr == null) return emptyList()
    val list = mutableListOf<ProcedureItem>()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      val consList = mutableListOf<ProcedureConsumable>()
      val consArr = o.optJSONArray("consumableItems")
      if (consArr != null) {
        for (j in 0 until consArr.length()) {
          val co = consArr.getJSONObject(j)
          consList.add(
            ProcedureConsumable(
              id = co.optString("id", UUID.randomUUID().toString()),
              medicineId = co.optString("medicineId"),
              name = co.optString("name"),
              quantity = co.optInt("quantity", 1),
              unitPrice = co.optDouble("unitPrice", 0.0),
              unitType = co.optString("unitType", "Pcs")
            )
          )
        }
      }
      list.add(
        ProcedureItem(
          id = o.optString("id", UUID.randomUUID().toString()),
          code = o.optString("code"),
          name = o.optString("name"),
          myanmarName = o.optString("myanmarName"),
          category = o.optString("category", "General"),
          price = o.optDouble("price", 0.0),
          standardFee = o.optDouble("standardFee", 0.0),
          consumableCost = o.optDouble("consumableCost", 0.0),
          notes = o.optString("notes"),
          dateTime = o.optString("dateTime"),
          consumableItems = consList
        )
      )
    }
    return list
  }

  fun parseSuppliers(arr: JSONArray?): List<Supplier> {
    if (arr == null) return emptyList()
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
    return list
  }

  fun parseVaccines(arr: JSONArray?): List<VaccineRecord> {
    if (arr == null) return emptyList()
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
    return list
  }
}
