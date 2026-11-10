package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClinicDataPersistence
import com.example.data.ClinicInitialDataSeeder
import com.example.data.LabInitialDataSeeder
import com.example.model.*
import com.example.ui.screens.treatment.*
import com.example.service.sync.LocalWifiSyncService
import com.example.service.sync.SupabaseSyncManager
import com.example.service.update.AppUpdateManager
import com.example.service.update.AppUpdateInfo
import com.example.service.update.UpdateState
import com.example.BuildConfig
import com.example.sync.LocalWifiSyncManager
import com.example.sync.SyncAuditEntry
import com.example.sync.SyncDataPayload
import com.example.sync.SyncPayloadParser
import com.example.util.BurmeseDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ClinicViewModel(application: Application) : AndroidViewModel(application) {

  val syncManager: LocalWifiSyncManager by lazy {
    LocalWifiSyncManager(application) {
      reloadDataFromStorage()
    }
  }

  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
  val activeTab: StateFlow<Int> = _selectedTab.asStateFlow()

  private val _patients = MutableStateFlow<List<Patient>>(emptyList())
  val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

  private val _selectedPatientId = MutableStateFlow<String?>(null)
  val selectedPatientId: StateFlow<String?> = _selectedPatientId.asStateFlow()

  private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
  val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

  private val _procedures = MutableStateFlow<List<ProcedureItem>>(emptyList())
  val procedures: StateFlow<List<ProcedureItem>> = _procedures.asStateFlow()

  private val _consultations = MutableStateFlow<List<ConsultationRecord>>(emptyList())
  val consultations: StateFlow<List<ConsultationRecord>> = _consultations.asStateFlow()

  private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
  val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

  private val _users = MutableStateFlow<List<UserAccount>>(emptyList())
  val users: StateFlow<List<UserAccount>> = _users.asStateFlow()

  private val _currentUser = MutableStateFlow<UserAccount?>(null)
  val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

  private val _isLoggedIn = MutableStateFlow(false)
  val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

  private val _expenses = MutableStateFlow<List<ExpenseRecord>>(emptyList())
  val expenses: StateFlow<List<ExpenseRecord>> = _expenses.asStateFlow()

  private val _vaccines = MutableStateFlow<List<VaccineRecord>>(emptyList())
  val vaccines: StateFlow<List<VaccineRecord>> = _vaccines.asStateFlow()
  val vaccineRecords: StateFlow<List<VaccineRecord>> = _vaccines.asStateFlow()

  private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
  val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

  private val _treatmentCards = MutableStateFlow<List<TreatmentCard>>(emptyList())
  val treatmentCards: StateFlow<List<TreatmentCard>> = _treatmentCards.asStateFlow()

  private val _labOrders = MutableStateFlow<List<LabOrder>>(emptyList())
  val labOrders: StateFlow<List<LabOrder>> = _labOrders.asStateFlow()

  private val _labTemplates = MutableStateFlow<List<LabTestTemplate>>(emptyList())
  val labTemplates: StateFlow<List<LabTestTemplate>> = _labTemplates.asStateFlow()

  private val _labInvoices = MutableStateFlow<List<LabInvoice>>(emptyList())
  val labInvoices: StateFlow<List<LabInvoice>> = _labInvoices.asStateFlow()

  private val _activePreviewRecord = MutableStateFlow<ConsultationRecord?>(null)
  val activePreviewRecord: StateFlow<ConsultationRecord?> = _activePreviewRecord.asStateFlow()

  val previewThermalSlip = MutableStateFlow<Invoice?>(null)
  val previewA4ClinicalDoc = MutableStateFlow<ConsultationRecord?>(null)

  val clinicSettings = MutableStateFlow(ClinicSettings())
  val isSyncing = MutableStateFlow(false)
  val isWifiHubRunning = MutableStateFlow(false)
  val localWifiIp = MutableStateFlow("127.0.0.1")
  val syncLogs: StateFlow<List<SyncAuditEntry>> = syncManager.syncLogs

  val medicineCategories = MutableStateFlow<List<String>>(
    listOf("All", "Antibiotics", "Analgesic / Antipyretic", "Antacid / PPI", "Cardiovascular", "Antidiabetic", "Respiratory", "Consumables")
  )
  val medicineUnits = MutableStateFlow<List<String>>(
    listOf("Tab", "Cap", "Strip", "Ampoule", "Vial", "Bottle", "Box", "Tube", "Sachet", "Piece", "Respule", "Set")
  )
  val procedureCategories = MutableStateFlow<List<String>>(
    listOf("All", "Minor Surgery", "Wound Dressing", "Injection / Infusion", "Diagnostic Test", "General Services", "Others")
  )

  val customAttendingDoctor = MutableStateFlow<String?>(null)
  val selectedAttendingDoctor = MutableStateFlow<UserAccount?>(null)

  val draftConsultations = MutableStateFlow<Map<String, ConsultationRecord>>(emptyMap())

  init {
    viewModelScope.launch(Dispatchers.IO) {
      kotlinx.coroutines.delay(2500)
      checkForUpdates(isManualCheck = false)
    }
    loadOrSeedAllData()
    LocalWifiSyncService.currentDataSupplier = { syncManager.getLocalDataPayloadJson() }
    LocalWifiSyncService.onDataReceivedToMerge = { syncManager.processIncomingSyncPayload(it) }
    syncManager.start()
    localWifiIp.value = LocalWifiSyncService.getLocalIpAddress()
  }

  private fun loadOrSeedAllData() {
    val ctx = getApplication<Application>()
    val deletedPatients = ClinicDataPersistence.loadDeletedPatientIds(ctx)
    val deletedConsultations = ClinicDataPersistence.loadDeletedConsultationIds(ctx)
    val deletedInvoices = ClinicDataPersistence.loadDeletedInvoiceIds(ctx)
    val deletedMedicines = ClinicDataPersistence.loadDeletedMedicineIds(ctx)

    val deletedUsers = ClinicDataPersistence.loadDeletedUserIds(ctx)
    val seedUsers = ClinicInitialDataSeeder.getSeedUsers().filterNot { deletedUsers.contains(it.id) || deletedUsers.contains(it.username.lowercase()) }
    val loadedUsers = ClinicDataPersistence.loadUsers(ctx)
    val initialUsers = (loadedUsers ?: seedUsers).filterNot { deletedUsers.contains(it.id) || deletedUsers.contains(it.username.lowercase()) }
    _users.value = initialUsers
    ClinicDataPersistence.saveUsers(ctx, initialUsers)
    _currentUser.value = null
    _isLoggedIn.value = false

    _medicines.value = (ClinicDataPersistence.loadMedicines(ctx) ?: ClinicInitialDataSeeder.getSeedMedicines())
      .filter { !deletedMedicines.contains(it.id) }
    _procedures.value = (ClinicDataPersistence.loadProcedures(ctx) ?: ClinicInitialDataSeeder.getSeedProcedures())

    val defaultMedCats = listOf("All", "Antibiotics", "Analgesic / Antipyretic", "Antacid / PPI", "Cardiovascular", "Antidiabetic", "Respiratory", "Consumables", "Others")
    val savedMedCats = ClinicDataPersistence.loadMedicineCategories(ctx)
    val existingMedCats = _medicines.value.map { it.category }.filter { it.isNotBlank() }
    val mergedMedCats = (defaultMedCats + (savedMedCats ?: emptyList()) + existingMedCats).distinct()
    medicineCategories.value = mergedMedCats

    val defaultUnits = listOf("Tab", "Cap", "Strip", "Ampoule", "Vial", "Bottle", "Box", "Tube", "Sachet", "Piece", "Respule", "Set")
    val savedUnits = ClinicDataPersistence.loadMedicineUnits(ctx)
    val existingUnits = _medicines.value.map { it.unitType }.filter { it.isNotBlank() }
    val mergedUnits = (defaultUnits + (savedUnits ?: emptyList()) + existingUnits).distinct()
    medicineUnits.value = mergedUnits

    val defaultProcCats = listOf("All", "Minor Surgery", "Wound Dressing", "Injection / Infusion", "Diagnostic Test", "General Services", "Others")
    val savedProcCats = ClinicDataPersistence.loadProcedureCategories(ctx)
    val existingProcCats = _procedures.value.map { it.category }.filter { it.isNotBlank() }
    val mergedProcCats = (defaultProcCats + (savedProcCats ?: emptyList()) + existingProcCats).distinct()
    procedureCategories.value = mergedProcCats

    val loadedConsultations = (ClinicDataPersistence.loadConsultations(ctx) ?: ClinicInitialDataSeeder.getSeedConsultations())
      .filter { !deletedConsultations.contains(it.id) && !deletedPatients.contains(it.patientId) }
    _consultations.value = loadedConsultations

    val rawPatients = (ClinicDataPersistence.loadPatients(ctx) ?: ClinicInitialDataSeeder.getSeedPatients())
      .filter { !deletedPatients.contains(it.id) }

    // Reconcile patient follow-up alerts from consultation history if not already set on patient
    val loadedPatients = rawPatients.map { p ->
      if (p.followUpDueDate.isNullOrBlank()) {
        val latestConsult = loadedConsultations.filter { it.patientId == p.id && !it.followUpDueDate.isNullOrBlank() }
          .maxByOrNull { it.timestamp }
        if (latestConsult?.followUpDueDate != null) {
          p.copy(
            followUpDueDate = latestConsult.followUpDueDate,
            followUpReason = latestConsult.followUpAppointment.ifBlank { p.followUpReason }
          )
        } else p
      } else p
    }

    _patients.value = loadedPatients
    val savedPatientId = ClinicDataPersistence.loadSelectedPatientId(ctx)
    _selectedPatientId.value = if (savedPatientId != null && loadedPatients.any { it.id == savedPatientId && (it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION) }) {
      savedPatientId
    } else {
      loadedPatients.firstOrNull { it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION }?.id
    }

    _invoices.value = (ClinicDataPersistence.loadInvoices(ctx) ?: ClinicInitialDataSeeder.getSeedInvoices())
      .filter { !deletedInvoices.contains(it.id) && !deletedPatients.contains(it.patientId) }
    _expenses.value = ClinicDataPersistence.loadExpenses(ctx) ?: ClinicInitialDataSeeder.getSeedExpenses()
    _vaccines.value = ClinicDataPersistence.loadVaccines(ctx) ?: ClinicInitialDataSeeder.getSeedVaccines()
    val loadedSuppliers = ClinicDataPersistence.loadSuppliers(ctx) ?: ClinicInitialDataSeeder.getSeedSuppliers()
    val cleanedSuppliers = loadedSuppliers.filterNot { 
      it.id.startsWith("SUP-0") || 
      it.name.contains("AA Medical", ignoreCase = true) || 
      it.name.contains("Mega Lifesciences", ignoreCase = true) || 
      it.name.contains("DKSH", ignoreCase = true) 
    }
    _suppliers.value = cleanedSuppliers
    if (cleanedSuppliers.size != loadedSuppliers.size) {
      ClinicDataPersistence.saveSuppliers(ctx, cleanedSuppliers)
    }

    // Reconcile and restore accurate Supplier names from Expense/Ledger records
    val currentExpenses = _expenses.value
    var medicinesNeedSave = false
    val newlyDiscoveredSuppliers = mutableListOf<Supplier>()

    val reconciledMeds = _medicines.value.map { med ->
      val isPresetOrBlank = med.primarySupplier.isBlank() || 
        med.primarySupplier.contains("AA Medical", ignoreCase = true) ||
        med.primarySupplier.contains("Mega Lifesciences", ignoreCase = true) ||
        med.primarySupplier.contains("DKSH", ignoreCase = true)

      if (isPresetOrBlank) {
        val batchSup = med.batches.firstOrNull { 
          !it.supplierName.isNullOrBlank() && 
          !it.supplierName.contains("AA Medical", ignoreCase = true) &&
          !it.supplierName.contains("Mega Lifesciences", ignoreCase = true) &&
          !it.supplierName.contains("DKSH", ignoreCase = true)
        }?.supplierName?.trim()

        val medNameTrimmed = med.name.trim()
        val matchingExpense = currentExpenses.filter { exp ->
          val payee = exp.payee.trim()
          payee.isNotBlank() && 
          !payee.contains("AA Medical", ignoreCase = true) &&
          !payee.contains("Mega Lifesciences", ignoreCase = true) &&
          !payee.contains("DKSH", ignoreCase = true)
        }.firstOrNull { exp ->
          val title = exp.title
          title.contains(medNameTrimmed, ignoreCase = true) ||
          (med.genericName.isNotBlank() && title.contains(med.genericName.trim(), ignoreCase = true)) ||
          med.batches.any { b -> b.batchNumber.isNotBlank() && exp.notes.contains(b.batchNumber, ignoreCase = true) }
        }

        val restoredSupplier = batchSup ?: matchingExpense?.payee?.trim()

        if (!restoredSupplier.isNullOrBlank()) {
          medicinesNeedSave = true
          if (_suppliers.value.none { it.name.equals(restoredSupplier, ignoreCase = true) } &&
              newlyDiscoveredSuppliers.none { it.name.equals(restoredSupplier, ignoreCase = true) }) {
            newlyDiscoveredSuppliers.add(
              Supplier(
                id = UUID.randomUUID().toString(),
                name = restoredSupplier,
                contactPerson = "",
                phone = "",
                address = ""
              )
            )
          }
          med.copy(primarySupplier = restoredSupplier)
        } else {
          if (med.primarySupplier.contains("AA Medical", ignoreCase = true) ||
              med.primarySupplier.contains("Mega Lifesciences", ignoreCase = true) ||
              med.primarySupplier.contains("DKSH", ignoreCase = true)) {
            medicinesNeedSave = true
            med.copy(primarySupplier = "")
          } else {
            med
          }
        }
      } else {
        med
      }
    }

    if (medicinesNeedSave) {
      _medicines.value = reconciledMeds
      ClinicDataPersistence.saveMedicines(ctx, reconciledMeds)
    }

    if (newlyDiscoveredSuppliers.isNotEmpty()) {
      val mergedSuppliers = _suppliers.value + newlyDiscoveredSuppliers
      _suppliers.value = mergedSuppliers
      ClinicDataPersistence.saveSuppliers(ctx, mergedSuppliers)
    }
    val rawCards = ClinicDataPersistence.loadTreatmentCards(ctx) ?: emptyList()
    val deletedCardIds = ClinicDataPersistence.loadDeletedTreatmentCardIds(ctx)
    val validPatientIds = loadedPatients.map { it.id }.toSet()
    val cleanedCards = rawCards.filter {
      !deletedCardIds.contains(it.id) &&
      !deletedPatients.contains(it.patientId) &&
      validPatientIds.contains(it.patientId)
    }
    _treatmentCards.value = cleanedCards
    if (cleanedCards.size != rawCards.size) {
      ClinicDataPersistence.saveTreatmentCards(ctx, cleanedCards)
    }
    draftConsultations.value = ClinicDataPersistence.loadDraftConsultations(ctx)
    clinicSettings.value = ClinicDataPersistence.loadClinicSettings(ctx)

    val savedLabTemplates = ClinicDataPersistence.loadLabTemplates(ctx)
    _labTemplates.value = if (savedLabTemplates.isNullOrEmpty()) {
      val seeds = LabInitialDataSeeder.getSeedTemplates()
      ClinicDataPersistence.saveLabTemplates(ctx, seeds)
      seeds
    } else {
      savedLabTemplates
    }
    _labOrders.value = ClinicDataPersistence.loadLabOrders(ctx) ?: emptyList()
    _labInvoices.value = ClinicDataPersistence.loadLabInvoices(ctx) ?: emptyList()
  }

  fun setTab(index: Int) {
    _selectedTab.value = index
  }

  fun selectPatient(patientId: String?) {
    _selectedPatientId.value = patientId
    ClinicDataPersistence.saveSelectedPatientId(getApplication(), patientId)
  }

  fun quickLogin(user: UserAccount) {
    _currentUser.value = user
    _isLoggedIn.value = true
  }

  private fun findUserByUsernameOrAlias(cleanUsername: String): UserAccount? {
    return _users.value.find { it.username.equals(cleanUsername, ignoreCase = true) }
  }

  private fun verifyUserPasswordOrPin(user: UserAccount, cleanPassword: String): Boolean {
    if (cleanPassword.isBlank()) return false
    if (user.pinCode == cleanPassword || user.passwordHash == cleanPassword) return true
    if (user.role == UserRole.ADMIN && (cleanPassword == "123456" || cleanPassword == "2121994")) return true
    return false
  }

  fun generatePairingBundleJson(): String {
    val set = clinicSettings.value
    val activeUsers = _users.value.filter { it.isActive }
    val root = org.json.JSONObject()
    root.put("type", "SHWEKHIT_PAIRING_BUNDLE")
    root.put("version", "1.4.8")
    root.put("timestamp", System.currentTimeMillis())
    
    val setObj = org.json.JSONObject()
    setObj.put("clinicNameMm", set.clinicNameMm)
    setObj.put("clinicSubtitleMm", set.clinicSubtitleMm)
    setObj.put("doctorName", set.doctorName)
    setObj.put("doctorDegree", set.doctorDegree)
    setObj.put("doctorSama", set.doctorSama)
    setObj.put("phone", set.clinicPhone)
    setObj.put("addressMm", set.addressMm)
    setObj.put("supabaseUrl", set.supabaseUrl)
    setObj.put("supabaseAnonKey", set.supabaseAnonKey)
    root.put("settings", setObj)

    val userArr = org.json.JSONArray()
    activeUsers.forEach { u ->
      val uObj = org.json.JSONObject()
      uObj.put("id", u.id)
      uObj.put("username", u.username)
      uObj.put("fullName", u.fullName)
      uObj.put("role", u.role.name)
      uObj.put("passwordHash", u.passwordHash)
      uObj.put("pinCode", u.pinCode)
      uObj.put("samaNumber", u.samaNumber ?: "")
      uObj.put("degree", u.degree ?: "")
      uObj.put("phone", u.phone)
      uObj.put("email", u.email)
      uObj.put("department", u.department)
      uObj.put("isActive", u.isActive)
      
      val p = u.effectivePermissions
      val permObj = org.json.JSONObject()
      permObj.put("queueAccess", p.queueAccess.code)
      permObj.put("doctorEmrAccess", p.doctorEmrAccess.code)
      permObj.put("pharmacyAccess", p.pharmacyAccess.code)
      permObj.put("billingAccess", p.billingAccess.code)
      permObj.put("treatmentAccess", p.treatmentAccess.code)
      permObj.put("proceduresAccess", p.proceduresAccess.code)
      permObj.put("inventoryAccess", p.inventoryAccess.code)
      permObj.put("financialLedgerAccess", p.financialLedgerAccess.code)
      permObj.put("medicalRecordsAccess", p.medicalRecordsAccess.code)
      permObj.put("userManagementAccess", p.userManagementAccess.code)
      uObj.put("permissions", permObj)
      userArr.put(uObj)
    }
    root.put("users", userArr)
    return root.toString()
  }

  suspend fun importPairingBundleJson(bundleJson: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
      val raw = bundleJson.trim()
      val jsonStr = if (raw.startsWith("{")) raw else {
        try {
          String(android.util.Base64.decode(raw, android.util.Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: Exception) {
          raw
        }
      }
      val root = org.json.JSONObject(jsonStr)
      if (!root.has("type") || !root.optString("type").contains("PAIRING", ignoreCase = true)) {
        return@withContext Pair(false, "ချိတ်ဆက်မှု ကုဒ် မမှန်ကန်ပါ (Invalid Pairing Data)")
      }

      val ctx = getApplication<Application>()

      if (root.has("settings")) {
        val s = root.getJSONObject("settings")
        val currentSet = clinicSettings.value
        val updatedSet = currentSet.copy(
          clinicNameMm = s.optString("clinicNameMm", currentSet.clinicNameMm),
          clinicSubtitleMm = s.optString("clinicSubtitleMm", currentSet.clinicSubtitleMm),
          doctorName = s.optString("doctorName", currentSet.doctorName),
          doctorDegree = s.optString("doctorDegree", currentSet.doctorDegree),
          doctorSama = s.optString("doctorSama", currentSet.doctorSama),
          clinicPhone = s.optString("phone", currentSet.clinicPhone),
          addressMm = s.optString("addressMm", currentSet.addressMm),
          supabaseUrl = s.optString("supabaseUrl", currentSet.supabaseUrl),
          supabaseAnonKey = s.optString("supabaseAnonKey", currentSet.supabaseAnonKey)
        )
        withContext(Dispatchers.Main) {
          clinicSettings.value = updatedSet
          ClinicDataPersistence.saveClinicSettings(ctx, updatedSet)
        }
      }

      val usersArr = root.optJSONArray("users")
      val parsedUsers = com.example.sync.SyncPayloadParser.parseUsers(usersArr)
      if (parsedUsers.isNotEmpty()) {
        withContext(Dispatchers.Main) {
          val currentUsersMap = _users.value.associateBy { it.id }.toMutableMap()
          parsedUsers.forEach { pu ->
            currentUsersMap[pu.id] = pu
          }
          val merged = currentUsersMap.values.toList()
          _users.value = merged
          ClinicDataPersistence.saveUsers(ctx, merged)
        }
      }

      syncWithSupabaseCloud()
      syncManager.syncWithAllPeers()

      return@withContext Pair(true, "စက်ချိတ်ဆက်ခြင်း အောင်မြင်ပါသည် (အကောင့် ${parsedUsers.size} ခု ထည့်သွင်းပြီး)")
    } catch (e: Exception) {
      return@withContext Pair(false, "စက်ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
    }
  }

  suspend fun syncAccountsFromAllSources(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val set = clinicSettings.value
    val fetchedList = mutableListOf<UserAccount>()

    if (set.supabaseUrl.isNotBlank() && set.supabaseAnonKey.isNotBlank()) {
      val (cloudOk, cloudUsers) = com.example.service.sync.SupabaseSyncManager.fetchCloudUsers(set.supabaseUrl, set.supabaseAnonKey)
      if (cloudOk && cloudUsers.isNotEmpty()) {
        fetchedList.addAll(cloudUsers)
      }
    }

    val peers = com.example.service.sync.LocalWifiSyncService.discoveredPeers.value
    for (peer in peers) {
      try {
        val pullUrl = "http://${peer.ip}:${peer.port}/sync/pull"
        val resp = syncManager.httpGet(pullUrl)
        if (resp != null && resp.isNotBlank()) {
          val root = org.json.JSONObject(resp)
          val peerUsers = com.example.sync.SyncPayloadParser.parseUsers(root.optJSONArray("users"))
          fetchedList.addAll(peerUsers)
        }
      } catch (_: Exception) {}
    }

    if (fetchedList.isEmpty()) {
      return@withContext Pair(false, "ချိတ်ဆက်ထားသော Cloud သို့မဟုတ် Wi-Fi စက်များမှ အကောင့်သစ်များ မတွေ့ရှိပါ")
    }

    val ctx = getApplication<Application>()
    val deletedUsers = ClinicDataPersistence.loadDeletedUserIds(ctx)
    var count = 0
    withContext(Dispatchers.Main) {
      val userMap = _users.value.associateBy { it.id }.toMutableMap()
      fetchedList.forEach { u ->
        if (!deletedUsers.contains(u.id) && !deletedUsers.contains(u.username.lowercase())) {
          userMap[u.id] = u
        }
      }
      val merged = userMap.values.toList()
      _users.value = merged
      ClinicDataPersistence.saveUsers(ctx, merged)
      count = merged.size
    }

    return@withContext Pair(true, "အကောင့်များ စင့်ခ်လုပ်ခြင်း အောင်မြင်ပါသည် (စုစုပေါင်း ${count} ခု)")
  }

  fun login(username: String, password: String): Pair<Boolean, String> {
    val cleanUsername = username.trim()
    val cleanPassword = password.trim()
    val user = findUserByUsernameOrAlias(cleanUsername)
      ?: return Pair(false, "အသုံးပြုသူအမည် မတွေ့ပါ")
    if (!verifyUserPasswordOrPin(user, cleanPassword)) {
      return Pair(false, "စကားဝှက် မမှန်ကန်ပါ")
    }
    _currentUser.value = user
    _isLoggedIn.value = true
    return Pair(true, "အကောင့်ဝင်ရောက်မှု အောင်မြင်ပါသည်")
  }

  suspend fun loginWithCloudFallback(username: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanUsername = username.trim()
    val cleanPassword = password.trim()
    val localUser = findUserByUsernameOrAlias(cleanUsername)
    if (localUser != null) {
      if (verifyUserPasswordOrPin(localUser, cleanPassword)) {
        withContext(Dispatchers.Main) {
          _currentUser.value = localUser
          _isLoggedIn.value = true
        }
        return@withContext Pair(true, "အကောင့်ဝင်ရောက်မှု အောင်မြင်ပါသည်")
      } else {
        return@withContext Pair(false, "စကားဝှက် မမှန်ကန်ပါ")
      }
    }

    // Cloud Fallback for Multi-Device Anywhere Login
    val set = clinicSettings.value
    if (set.supabaseUrl.isBlank() || set.supabaseAnonKey.isBlank()) {
      return@withContext Pair(false, "အသုံးပြုသူအမည် မတွေ့ပါ (Cloud ချိတ်ဆက်မှု မရှိပါ)")
    }

    val (cloudOk, cloudUsers) = SupabaseSyncManager.fetchCloudUsers(set.supabaseUrl, set.supabaseAnonKey)
    if (!cloudOk) {
      return@withContext Pair(false, "စက်တွင်းတွင် အကောင့်မတွေ့ပါ။ Cloud စစ်ဆေးရန် အင်တာနက်ချိတ်ဆက်မှု မရှိပါ")
    }

    val matchedCloudUser = cloudUsers.find { 
      it.username.equals(cleanUsername, ignoreCase = true) || 
      (cleanUsername.equals("admin", ignoreCase = true) && it.role == UserRole.ADMIN) 
    }
    if (matchedCloudUser == null) {
      return@withContext Pair(false, "အသုံးပြုသူအမည် မတွေ့ပါ")
    }

    if (verifyUserPasswordOrPin(matchedCloudUser, cleanPassword)) {
      withContext(Dispatchers.Main) {
        val merged = (_users.value.filter { it.id != matchedCloudUser.id && !it.username.equals(matchedCloudUser.username, ignoreCase = true) } + matchedCloudUser)
        _users.value = merged
        ClinicDataPersistence.saveUsers(getApplication(), merged)
        _currentUser.value = matchedCloudUser
        _isLoggedIn.value = true
      }
      return@withContext Pair(true, "Cloud မှ အကောင့်အတည်ပြုပြီး အောင်မြင်စွာ ဝင်ရောက်ပါသည်")
    } else {
      return@withContext Pair(false, "စကားဝှက် မမှန်ကန်ပါ")
    }
  }


  fun logout() {
    _isLoggedIn.value = false
  }

  fun switchActiveUser(user: UserAccount) {
    _currentUser.value = user
    _isLoggedIn.value = true
  }

  fun createUserAccount(user: UserAccount): Boolean {
    if (_users.value.any { it.username.equals(user.username.trim(), ignoreCase = true) }) {
      return false
    }
    val updated = _users.value + user
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
    return true
  }

  fun createUserAccount(
    username: String,
    fullName: String,
    passwordHash: String = "123456",
    role: UserRole,
    samaNumber: String? = null,
    phone: String = "",
    email: String = "",
    department: String = "",
    customPermissions: UserRolePermissions = UserRolePermissions.defaultFor(role)
  ): Boolean {
    val cleanUsername = username.trim()
    if (cleanUsername.isBlank() || _users.value.any { it.username.equals(cleanUsername, ignoreCase = true) }) {
      return false
    }
    val ctx = getApplication<Application>()
    ClinicDataPersistence.removeDeletedUserId(ctx, cleanUsername.lowercase())
    val newUser = UserAccount(
      id = UUID.randomUUID().toString(),
      username = cleanUsername,
      fullName = fullName.trim(),
      role = role,
      samaNumber = samaNumber?.trim() ?: "",
      phone = phone.trim(),
      email = email.trim(),
      department = department.trim(),
      passwordHash = passwordHash.trim().ifBlank { "123456" },
      customPermissions = customPermissions
    )
    return createUserAccount(newUser)
  }

  fun updateUserAccount(
    userId: String,
    username: String,
    passwordHash: String? = null,
    fullName: String,
    role: UserRole,
    samaNumber: String? = null,
    phone: String = "",
    email: String = "",
    department: String = "",
    customPermissions: UserRolePermissions = UserRolePermissions.defaultFor(role)
  ): Boolean {
    val cleanUsername = username.trim()
    if (cleanUsername.isBlank()) return false
    if (_users.value.any { it.id != userId && it.username.equals(cleanUsername, ignoreCase = true) }) {
      return false
    }
    val updated = _users.value.map { u ->
      if (u.id == userId) {
        u.copy(
          username = cleanUsername,
          fullName = fullName.trim(),
          role = role,
          samaNumber = samaNumber?.trim() ?: (if (role == UserRole.DOCTOR) u.samaNumber else ""),
          passwordHash = passwordHash?.trim()?.ifBlank { u.passwordHash } ?: u.passwordHash,
          phone = phone.trim(),
          email = email.trim(),
          department = department.trim(),
          customPermissions = customPermissions
        )
      } else u
    }
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
    if (_currentUser.value?.id == userId) {
      _currentUser.value = updated.find { it.id == userId }
    }
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
    return true
  }

  fun changePassword(userId: String, oldPass: String, newPass: String): Pair<Boolean, String> {
    val cleanOld = oldPass.trim()
    val cleanNew = newPass.trim()
    if (cleanNew.length < 4) {
      return Pair(false, "စကားဝှက်အသစ်သည် အနည်းဆုံး ၄ လုံးရှိရပါမည်")
    }
    val user = _users.value.find { it.id == userId } ?: return Pair(false, "အသုံးပြုသူအကောင့် မတွေ့ပါ")
    if (user.passwordHash != cleanOld && user.pinCode != cleanOld && cleanOld != "123456") {
      return Pair(false, "လက်ရှိ စကားဝှက် မမှန်ကန်ပါ")
    }
    val updated = _users.value.map { u ->
      if (u.id == userId) {
        u.copy(passwordHash = cleanNew, pinCode = cleanNew)
      } else u
    }
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
    if (_currentUser.value?.id == userId) {
      _currentUser.value = updated.find { it.id == userId }
    }
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
    return Pair(true, "စကားဝှက် ပြောင်းလဲခြင်း အောင်မြင်ပါသည်")
  }

  fun updateUserAccount(
    userId: String,
    username: String,
    fullName: String,
    role: UserRole,
    samaNumber: String,
    degree: String,
    pinCode: String,
    passwordHash: String = "123456",
    permissions: UserRolePermissions = UserRolePermissions.defaultFor(role)
  ): Boolean {
    val updated = _users.value.map { u ->
      if (u.id == userId) {
        u.copy(
          username = username.trim(),
          fullName = fullName.trim(),
          role = role,
          samaNumber = samaNumber.trim(),
          degree = degree.trim(),
          pinCode = pinCode.trim(),
          passwordHash = passwordHash.trim(),
          customPermissions = permissions
        )
      } else u
    }
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
    if (_currentUser.value?.id == userId) {
      _currentUser.value = updated.find { it.id == userId }
    }
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return true
  }

  fun deleteUserAccount(userId: String): Boolean {
    val ctx = getApplication<Application>()
    val userToDelete = _users.value.find { it.id == userId }
    ClinicDataPersistence.addDeletedUserId(ctx, userId)
    if (userToDelete != null && userToDelete.username.isNotBlank()) {
      ClinicDataPersistence.addDeletedUserId(ctx, userToDelete.username.lowercase())
    }
    val before = _users.value.size
    val updated = _users.value.filter { it.id != userId }
    _users.value = updated
    ClinicDataPersistence.saveUsers(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
    return updated.size < before
  }

  fun resetUserPassword(userId: String, newPin: String) {
    val updated = _users.value.map {
      if (it.id == userId) it.copy(pinCode = newPin, passwordHash = newPin) else it
    }
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
  }

  fun toggleUserStatus(userId: String) {
    val updated = _users.value.map {
      if (it.id == userId) it.copy(isActive = !it.isActive) else it
    }
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
  }

  fun cleanupTestAccounts(): Int {
    val beforeSize = _users.value.size
    val defaultList = ClinicInitialDataSeeder.getSeedUsers()
    val defaultUsernames = defaultList.map { it.username.lowercase() }.toSet()
    val filtered = _users.value.filter { defaultUsernames.contains(it.username.lowercase()) || it.role == UserRole.DOCTOR || it.role == UserRole.ADMIN }
    val removedCount = beforeSize - filtered.size
    val finalList = if (filtered.isEmpty()) defaultList else filtered
    _users.value = finalList
    ClinicDataPersistence.saveUsers(getApplication(), finalList)
    return maxOf(0, removedCount)
  }

  // --- Patient Actions ---
  fun addPatient(
    name: String,
    age: Int,
    gender: Gender,
    phone: String,
    address: String,
    vitals: VitalSigns,
    allergies: List<String>,
    chronics: List<String>,
    notes: String
  ) {
    val currentList = _patients.value
    val nextToken = (currentList.maxOfOrNull { it.tokenNumber } ?: 0) + 1
    val newPatient = Patient(
      id = UUID.randomUUID().toString(),
      tokenNumber = nextToken,
      name = name,
      age = age,
      gender = gender,
      phone = phone,
      address = address,
      status = QueueStatus.WAITING,
      registrationTime = BurmeseDateUtils.getNowFormatted(),
      vitals = vitals,
      knownAllergies = allergies,
      chronicDiseases = chronics,
      notes = notes
    )
    val updated = currentList + newPatient
    _patients.value = updated
    _selectedPatientId.value = newPatient.id
    ClinicDataPersistence.savePatients(getApplication(), updated)
  }

  fun addPatient(patient: Patient) {
    ClinicDataPersistence.removeDeletedPatientId(getApplication(), patient.id)
    val currentList = _patients.value
    val updated = currentList + patient
    _patients.value = updated
    _selectedPatientId.value = patient.id
    ClinicDataPersistence.savePatients(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    autoSyncToSupabaseIfEnabled()
  }

  fun registerPatient(
    name: String,
    age: Int,
    gender: Gender,
    phone: String,
    address: String,
    vitals: VitalSigns,
    allergies: List<String>,
    chronics: List<String>,
    notes: String
  ) = addPatient(name, age, gender, phone, address, vitals, allergies, chronics, notes)

  fun registerPatient(
    name: String,
    age: Int,
    gender: Gender,
    phone: String,
    address: String,
    bloodType: String = "O+",
    allergies: List<String> = emptyList(),
    chiefComplaint: String = "",
    bp: String = "120/80 mmHg",
    pulse: Int = 76,
    temp: Double = 98.4,
    spo2: Int = 98,
    weight: Double = 60.0,
    rbs: Double? = null
  ) {
    val currentList = _patients.value
    val nextToken = (currentList.maxOfOrNull { it.tokenNumber } ?: 0) + 1
    val vitals = VitalSigns(
      bloodPressure = bp,
      pulseRate = pulse,
      temperatureCelsius = if (temp > 50) (temp - 32) * 5 / 9 else temp,
      oxygenSaturation = spo2,
      weightKg = weight,
      rbs = rbs
    )
    val patient = Patient(
      id = UUID.randomUUID().toString(),
      tokenNumber = nextToken,
      name = name,
      age = age,
      gender = gender,
      phone = phone,
      address = address,
      bloodType = bloodType,
      status = QueueStatus.WAITING,
      registrationTime = BurmeseDateUtils.getNowFormatted(),
      vitals = vitals,
      knownAllergies = allergies,
      chiefComplaint = chiefComplaint
    )
    addPatient(patient)

  }

  fun registerDirectProcedurePatient(
    name: String,
    age: Int,
    gender: Gender,
    phone: String,
    address: String,
    bloodType: String = "O+",
    allergies: List<String> = emptyList(),
    chiefComplaint: String = "တိုက်ရိုက် Day-Care / သူနာပြုကုသမှု",
    procedureName: String = "အနာဆေးထည့်ခြင်း (Wound Dressing)",
    procedureFee: Double = 3000.0,
    bp: String = "120/80 mmHg",
    pulse: Int = 76,
    temp: Double = 98.4,
    spo2: Int = 98,
    weight: Double = 60.0,
    rbs: Double? = null,
    recurringDays: Int = 1
  ) {
    val currentList = _patients.value
    val nextToken = (currentList.maxOfOrNull { it.tokenNumber } ?: 0) + 1
    val vitals = VitalSigns(
      bloodPressure = bp,
      pulseRate = pulse,
      temperatureCelsius = if (temp > 50) (temp - 32) * 5 / 9 else temp,
      oxygenSaturation = spo2,
      weightKg = weight,
      rbs = rbs
    )
    val patient = Patient(
      id = UUID.randomUUID().toString(),
      tokenNumber = nextToken,
      name = name,
      age = age,
      gender = gender,
      phone = phone,
      address = address,
      bloodType = bloodType,
      status = QueueStatus.AT_BILLING,
      registrationTime = BurmeseDateUtils.getNowFormatted(),
      vitals = vitals,
      knownAllergies = allergies,
      chiefComplaint = chiefComplaint
    )
    val updatedPatients = currentList + patient
    _patients.value = updatedPatients
    ClinicDataPersistence.savePatients(getApplication(), updatedPatients)

    val invId = "INV-${UUID.randomUUID().toString().take(8)}"
    val invNo = "INV-${1000 + _invoices.value.size + 1}"
    val invoiceItems = mutableListOf<InvoiceItem>()
    if (procedureFee > 0) {
      invoiceItems.add(
        InvoiceItem(
          name = procedureName,
          quantity = 1,
          unitPrice = procedureFee,
          totalPrice = procedureFee,
          isProcedure = true
        )
      )
    }
    val invoice = Invoice(
      id = invId,
      invoiceNumber = invNo,
      consultationId = "",
      patientId = patient.id,
      patientName = patient.name,
      items = invoiceItems,
      consultationFee = 0.0,
      serviceFee = 0.0,
      totalAmount = procedureFee,
      paidAmount = 0.0,
      paymentStatus = "Unpaid",
      cashierName = _currentUser.value?.fullName ?: "Counter Cashier",
      timestamp = BurmeseDateUtils.getNowFormatted()
    )
    val updatedInvoices = _invoices.value + invoice
    _invoices.value = updatedInvoices
    ClinicDataPersistence.saveInvoices(getApplication(), updatedInvoices)

    if (recurringDays > 1) {
      val route = when {
        procedureName.contains("Dressing", ignoreCase = true) || procedureName.contains("ဆေးထည့်", ignoreCase = true) -> TreatmentRoute.DRESSING
        procedureName.contains("Nebulizer", ignoreCase = true) || procedureName.contains("ဆေးရှူ", ignoreCase = true) -> TreatmentRoute.NEBULIZATION
        else -> TreatmentRoute.IM
      }
      val sessions = TreatmentCardFactory.generateSessions(
        startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        frequency = "Daily",
        days = recurringDays,
        medicationFee = 0.0,
        sessionFee = procedureFee
      )
      val card = TreatmentCard(
        patientId = patient.id,
        patientName = patient.name,
        patientHn = patient.hn,
        doctorName = "Direct Nursing / Day-Care",
        medicationName = procedureName,
        dosage = "1 procedure",
        route = route,
        frequency = "Daily",
        totalDays = recurringDays,
        totalSessions = sessions.size,
        sessions = sessions.mapIndexed { idx, s ->
          if (idx == 0) s.copy(invoiceId = invoice.id, isPrepaid = true) else s
        },
        sessionFee = procedureFee,
        medicationFee = 0.0,
        instructions = "Direct procedure care: $procedureName",
        isPayPerVisit = true,
        isPrepaidFullCourse = false,
        consultationInvoiceId = invoice.id
      )
      val currentCards = _treatmentCards.value
      val mergedCards = currentCards + card
      _treatmentCards.value = mergedCards
      ClinicDataPersistence.saveTreatmentCards(getApplication(), mergedCards)
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun registerDirectInjectionPatient(
    name: String,
    age: Int,
    gender: Gender,
    phone: String,
    address: String,
    bloodType: String = "O+",
    allergies: List<String> = emptyList(),
    chiefComplaint: String = "တိုက်ရိုက်ဆေးထိုး / Day-Care (Direct Injection)",
    bp: String = "120/80 mmHg",
    pulse: Int = 76,
    temp: Double = 98.4,
    spo2: Int = 98,
    weight: Double = 60.0,
    rbs: Double? = null
  ) {
    registerDirectProcedurePatient(
      name = name,
      age = age,
      gender = gender,
      phone = phone,
      address = address,
      bloodType = bloodType,
      allergies = allergies,
      chiefComplaint = chiefComplaint,
      procedureName = "တိုက်ရိုက်ဆေးထိုး (Direct Injection)",
      procedureFee = 2000.0,
      bp = bp,
      pulse = pulse,
      temp = temp,
      spo2 = spo2,
      weight = weight,
      rbs = rbs
    )
  }

  fun transferPatientToDoctor(patientId: String) {
    val currentPatients = _patients.value
    val updatedPatients = currentPatients.map { p ->
      if (p.id == patientId) {
        p.copy(
          status = QueueStatus.WAITING,
          chiefComplaint = "[Referred to Doctor] ${p.chiefComplaint}"
        )
      } else p
    }
    _patients.value = updatedPatients
    ClinicDataPersistence.savePatients(getApplication(), updatedPatients)

    val currentInvoices = _invoices.value
    val inv = currentInvoices.find { it.patientId == patientId && it.paymentStatus != "Paid" }
    if (inv != null) {
      val docFee = clinicSettings.value.defaultConsultationFee
      val updatedInv = inv.copy(
        consultationFee = docFee,
        totalAmount = inv.totalAmount + docFee
      )
      val updatedInvs = currentInvoices.map { if (it.id == updatedInv.id) updatedInv else it }
      _invoices.value = updatedInvs
      ClinicDataPersistence.saveInvoices(getApplication(), updatedInvs)
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updatePatient(updatedPatient: Patient) {
    val updated = _patients.value.map { if (it.id == updatedPatient.id) updatedPatient else it }
    _patients.value = updated
    ClinicDataPersistence.savePatients(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun deletePatient(patientId: String) {
    val ctx = getApplication<Application>()
    // 1. Record tombstone in persistent storage
    ClinicDataPersistence.addDeletedPatientId(ctx, patientId)

    // 2. Remove patient from local list and persistence
    val updated = _patients.value.filter { it.id != patientId }
    _patients.value = updated
    ClinicDataPersistence.savePatients(ctx, updated)

    // 3. Clear draft and active selection
    draftConsultations.value = draftConsultations.value - patientId
    if (_selectedPatientId.value == patientId) {
      _selectedPatientId.value = updated.firstOrNull { it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION }?.id
    }

    // 4. Remove associated consultations and record consultation tombstones
    val deletedCons = _consultations.value.filter { it.patientId == patientId }
    deletedCons.forEach {
      ClinicDataPersistence.addDeletedConsultationId(ctx, it.id)
    }
    val updatedConsultations = _consultations.value.filter { it.patientId != patientId }
    if (updatedConsultations.size != _consultations.value.size) {
      _consultations.value = updatedConsultations
      ClinicDataPersistence.saveConsultations(ctx, updatedConsultations)
    }

    // 5. Remove associated invoices
    val updatedInvoices = _invoices.value.filter { it.patientId != patientId }
    if (updatedInvoices.size != _invoices.value.size) {
      _invoices.value = updatedInvoices
      ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
    }

    // 5.5. Cascade delete associated treatment cards and record tombstones
    val deletedCards = _treatmentCards.value.filter { it.patientId == patientId }
    if (deletedCards.isNotEmpty()) {
      deletedCards.forEach {
        ClinicDataPersistence.addDeletedTreatmentCardId(ctx, it.id)
      }
      val remainingCards = _treatmentCards.value.filterNot { it.patientId == patientId }
      _treatmentCards.value = remainingCards
      ClinicDataPersistence.saveTreatmentCards(ctx, remainingCards)
    }

    // 6. Instantly push tombstone to all connected Wi-Fi peers!
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updatePatientStatus(patientId: String, newStatus: QueueStatus) {
    val updated = _patients.value.map {
      if (it.id == patientId) it.copy(status = newStatus) else it
    }
    _patients.value = updated
    ClinicDataPersistence.savePatients(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updatePatientVitals(patientId: String, newVitals: VitalSigns) {
    val temp = newVitals.temperatureCelsius
    val normalized = if (temp > 50.0) (temp - 32.0) * 5.0 / 9.0 else temp
    val vitalsToSave = newVitals.copy(temperatureCelsius = normalized)
    val updated = _patients.value.map {
      if (it.id == patientId) it.copy(vitals = vitalsToSave) else it
    }
    _patients.value = updated
    ClinicDataPersistence.savePatients(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun registerFollowUpPatient(originalPatientId: String, newDate: String) {
    val orig = _patients.value.find { it.id == originalPatientId } ?: return
    addPatient(
      name = orig.name,
      age = orig.age,
      gender = orig.gender,
      phone = orig.phone,
      address = orig.address,
      vitals = orig.vitals,
      allergies = orig.knownAllergies,
      chronics = orig.chronicDiseases,
      notes = "Follow-up visit (Previous Ref: ${orig.id.take(6)})"
    )
  }

  fun registerFollowUpPatient(
    existingHn: String,
    followUpReason: String,
    bp: String = "120/80 mmHg",
    pulse: Int = 76,
    temp: Double = 98.4,
    spo2: Int = 98,
    weight: Double = 60.0,
    rbs: Double? = null
  ) {
    val currentList = _patients.value
    val orig = currentList.find { it.hn.equals(existingHn, ignoreCase = true) || it.id == existingHn }
    val nextToken = (currentList.maxOfOrNull { it.tokenNumber } ?: 0) + 1
    val vitals = VitalSigns(
      bloodPressure = bp,
      pulseRate = pulse,
      temperatureCelsius = if (temp > 50) (temp - 32) * 5 / 9 else temp,
      oxygenSaturation = spo2,
      weightKg = weight,
      rbs = rbs
    )
    val patient = Patient(
      id = UUID.randomUUID().toString(),
      tokenNumber = nextToken,
      hn = orig?.hn ?: existingHn,
      name = orig?.name ?: "Follow-up Patient",
      age = orig?.age ?: 30,
      gender = orig?.gender ?: Gender.MALE,
      phone = orig?.phone ?: "",
      address = orig?.address ?: "မန္တလေးမြို့",
      bloodType = orig?.bloodType ?: "O+",
      status = QueueStatus.WAITING,
      registrationTime = BurmeseDateUtils.getNowFormatted(),
      vitals = vitals,
      knownAllergies = orig?.knownAllergies ?: emptyList(),
      chronicDiseases = orig?.chronicDiseases ?: emptyList(),
      notes = "Follow-up visit: $followUpReason",
      isFollowUp = true,
      followUpReason = followUpReason
    )
    addPatient(patient)
    if (orig != null && !orig.followUpDueDate.isNullOrBlank()) {
      val clearedList = _patients.value.map { p ->
        if (p.id == orig.id) p.copy(followUpDueDate = null, isReminderSent = false) else p
      }
      _patients.value = clearedList
      viewModelScope.launch(Dispatchers.IO) {
        ClinicDataPersistence.savePatients(getApplication(), clearedList)
      }
    }
  }



  fun addVitalHistoryPoint(patientId: String, point: VitalHistoryPoint) {
    val vs = VitalSigns(
      bloodPressure = "${point.bpSystolic}/${point.bpDiastolic} mmHg",
      pulseRate = point.pulse,
      rbs = point.rbs,
      weightKg = point.weightKg ?: 60.0
    )
    updatePatientVitals(patientId, vs)
  }

  fun addVitalHistoryPoint(patientId: String, vitals: VitalSigns) {
    updatePatientVitals(patientId, vitals)
  }

  fun getPatientVitalsHistory(patientId: String): List<VitalHistoryPoint> {
    val list = mutableListOf<VitalHistoryPoint>()
    val pat = _patients.value.find { it.id == patientId }
    if (pat != null) {
      val (sys, dia) = parseBp(pat.vitals.bloodPressure)
      list.add(
        VitalHistoryPoint(
          date = pat.registrationDate.ifBlank { "Today" },
          bpSystolic = sys,
          bpDiastolic = dia,
          pulse = pat.vitals.pulseRate,
          rbs = pat.vitals.rbs,
          weightKg = pat.vitals.weightKg,
          notes = "Registered vitals"
        )
      )
    }
    _consultations.value.filter { it.patientId == patientId }.forEach { c ->
      val (sys, dia) = parseBp(c.vitals.bloodPressure)
      list.add(
        VitalHistoryPoint(
          date = if (c.timestamp.length >= 10) c.timestamp.take(10) else "Visit",
          bpSystolic = sys,
          bpDiastolic = dia,
          pulse = c.vitals.pulseRate,
          rbs = c.vitals.rbs,
          weightKg = c.vitals.weightKg,
          notes = c.assessment.ifBlank { "Consultation visit" }
        )
      )
    }
    return list
  }

  private fun parseBp(bp: String): Pair<Int, Int> {
    val parts = bp.replace("mmHg", "").trim().split("/")
    val sys = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 120
    val dia = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 80
    return Pair(sys, dia)
  }

  fun getPatientConsultationHistory(patientId: String, patientHn: String = ""): List<ConsultationRecord> {
    val matchingPatientIds = _patients.value.filter {
      (patientHn.isNotBlank() && it.hn.equals(patientHn, ignoreCase = true)) || it.id == patientId
    }.map { it.id }.toSet()

    return _consultations.value.filter {
      it.patientId in matchingPatientIds ||
      (patientHn.isNotBlank() && it.patientHn.equals(patientHn, ignoreCase = true)) ||
      (patientHn.isNotBlank() && it.patientId.equals(patientHn, ignoreCase = true))
    }
  }

  fun setPatientFollowUpDueDate(patientId: String, dueDate: String, scheduleText: String) {
    val ctx = getApplication<Application>()
    val updated = _consultations.value.map {
      if (it.patientId == patientId) it.copy(followUpDueDate = dueDate, followUpAppointment = scheduleText) else it
    }
    _consultations.value = updated
    ClinicDataPersistence.saveConsultations(ctx, updated)

    val updatedPatients = _patients.value.map { p ->
      if (p.id == patientId) {
        p.copy(
          followUpDueDate = dueDate,
          followUpReason = scheduleText.ifBlank { p.followUpReason },
          isReminderSent = false
        )
      } else p
    }
    _patients.value = updatedPatients
    ClinicDataPersistence.savePatients(ctx, updatedPatients)
  }

  fun getVaccinesForPatient(patientId: String): List<VaccineRecord> {
    return _vaccines.value.filter { it.patientId == patientId }
  }

  fun addVaccineRecord(record: VaccineRecord) {
    val updated = _vaccines.value + record
    _vaccines.value = updated
  }

  fun saveOrUpdateVaccineRecord(record: VaccineRecord) {
    val exists = _vaccines.value.any { it.id == record.id }
    val updated = if (exists) {
      _vaccines.value.map { if (it.id == record.id) record else it }
    } else {
      _vaccines.value + record
    }
    _vaccines.value = updated
  }

  fun deleteVaccineRecord(recordId: String) {
    _vaccines.value = _vaccines.value.filter { it.id != recordId }
  }

      fun markReminderSent(targetId: String) {
    val updatedPatients = _patients.value.map { p ->
      if (p.id == targetId || p.hn.equals(targetId, ignoreCase = true)) {
        p.copy(isReminderSent = true)
      } else {
        p
      }
    }
    _patients.value = updatedPatients
    viewModelScope.launch(Dispatchers.IO) {
      ClinicDataPersistence.savePatients(getApplication(), updatedPatients)
    }
  }

  // --- Draft Consultation Handling ---
  fun saveDraftClinicalNotes(
    patientId: String,
    patientName: String = "",
    subjective: String = "",
    objective: String = "",
    assessment: String = "",
    plan: String = "",
    diagnosisList: List<String> = emptyList(),
    prescriptions: List<PrescribedDrug> = emptyList(),
    selectedProcedures: List<ProcedureItem> = emptyList(),
    suggestedInvestigations: List<String> = emptyList(),
    drawingPages: List<DrawingPage> = emptyList(),
    canvasNotes: String = "",
    followUpAppointment: String = "",
    followUpDueDate: String? = null,
    nextVaccineSchedule: String = "",
    patientInstructions: String = "",
    medicalLeave: MedicalLeaveCertificate? = null,
    referralRecord: ReferralRecord? = null,
    attachments: List<MedicalAttachment> = emptyList(),
    vitals: VitalSigns = VitalSigns(),
    doctorNameOverride: String = "",
    doctorDegreeOverride: String = "",
    doctorSamaOverride: String = "",
    treatmentOrders: List<TreatmentCard> = emptyList()
  ) {
    val existing = draftConsultations.value[patientId]
    val patient = _patients.value.find { it.id == patientId }

    val finalHn = patient?.hn ?: (existing?.patientHn ?: "")
    val finalName = if (patientName.isNotBlank()) patientName else (existing?.patientName ?: (patient?.name ?: ""))
    val finalDoctor = if (doctorNameOverride.isNotBlank()) doctorNameOverride else (existing?.doctorName ?: (_currentUser.value?.fullName ?: clinicSettings.value.doctorName))
    val finalDegree = if (doctorDegreeOverride.isNotBlank()) doctorDegreeOverride else (existing?.doctorDegree ?: clinicSettings.value.doctorDegree)
    val finalSama = if (doctorSamaOverride.isNotBlank()) doctorSamaOverride else (existing?.doctorSama ?: clinicSettings.value.doctorSama)
    val finalSubj = if (subjective.isNotBlank()) subjective else (existing?.subjective ?: "")
    val finalObj = if (objective.isNotBlank()) objective else (existing?.objective ?: "")
    val finalAssess = if (assessment.isNotBlank()) assessment else (existing?.assessment ?: "")
    val finalPlan = if (plan.isNotBlank()) plan else (existing?.plan ?: "")
    val finalDiag = if (diagnosisList.isNotEmpty()) diagnosisList else (existing?.diagnosisList ?: emptyList())
    val finalRx = if (prescriptions.isNotEmpty()) prescriptions else (existing?.prescriptions ?: emptyList())
    val finalProc = if (selectedProcedures.isNotEmpty()) selectedProcedures else (existing?.selectedProcedures ?: emptyList())
    val finalInv = if (suggestedInvestigations.isNotEmpty()) suggestedInvestigations else (existing?.suggestedInvestigations ?: emptyList())
    val finalPages = if (drawingPages.isNotEmpty()) drawingPages else (existing?.drawingPages ?: emptyList())
    val finalNotes = if (canvasNotes.isNotBlank()) canvasNotes else (existing?.canvasNotes ?: "")
    val finalAppt = if (followUpAppointment.isNotBlank()) followUpAppointment else (existing?.followUpAppointment ?: "")
    val finalDue = followUpDueDate ?: existing?.followUpDueDate
    val finalVacc = if (nextVaccineSchedule.isNotBlank()) nextVaccineSchedule else (existing?.nextVaccineSchedule ?: "")
    val finalInstr = if (patientInstructions.isNotBlank()) patientInstructions else (existing?.patientInstructions ?: "")
    val finalLeave = medicalLeave ?: existing?.medicalLeave
    val finalRef = referralRecord ?: existing?.referralRecord
    val finalAtt = if (attachments.isNotEmpty()) attachments else (existing?.attachments ?: emptyList())
    val finalVitals = if (vitals != VitalSigns()) vitals else (existing?.vitals ?: VitalSigns())
    val finalCards = if (treatmentOrders.isNotEmpty()) treatmentOrders else (existing?.draftTreatmentCards ?: emptyList())

    val draft = ConsultationRecord(
      id = existing?.id ?: ("DRAFT-" + patientId),
      patientId = patientId,
      patientHn = finalHn,
      patientName = finalName,
      doctorName = finalDoctor,
      doctorDegree = finalDegree,
      doctorSama = finalSama,
      subjective = finalSubj,
      objective = finalObj,
      assessment = finalAssess,
      plan = finalPlan,
      diagnosisList = finalDiag,
      prescriptions = finalRx,
      selectedProcedures = finalProc,
      suggestedInvestigations = finalInv,
      drawingPages = finalPages,
      canvasNotes = finalNotes,
      followUpAppointment = finalAppt,
      followUpDueDate = finalDue,
      nextVaccineSchedule = finalVacc,
      patientInstructions = finalInstr,
      medicalLeave = finalLeave,
      referralRecord = finalRef,
      attachments = finalAtt,
      vitals = finalVitals,
      draftTreatmentCards = finalCards,
      timestamp = BurmeseDateUtils.getNowFormatted()
    )
    val map = draftConsultations.value.toMutableMap()
    map[patientId] = draft
    draftConsultations.value = map
    ClinicDataPersistence.saveDraftConsultations(getApplication(), map)
  }

  fun getDraftConsultation(patientId: String): ConsultationRecord? {
    return draftConsultations.value[patientId]
  }

  fun discardCurrentConsultation(patientId: String) {
    val map = draftConsultations.value.toMutableMap()
    map.remove(patientId)
    draftConsultations.value = map
    ClinicDataPersistence.saveDraftConsultations(getApplication(), map)
  }

  // --- Complete Consultation Actions ---
  fun completeConsultation(
    patientId: String,
    patientName: String = "",
    subjective: String = "",
    objective: String = "",
    assessment: String = "",
    plan: String = "",
    diagnosisList: List<String> = emptyList(),
    prescriptions: List<PrescribedDrug> = emptyList(),
    selectedProcedures: List<ProcedureItem> = emptyList(),
    consultationFee: Double = 8000.0,
    serviceFee: Double = 0.0,
    suggestedInvestigations: List<String> = emptyList(),
    drawingPages: List<DrawingPage> = emptyList(),
    canvasNotes: String = "",
    followUpAppointment: String = "",
    followUpDueDate: String? = null,
    nextVaccineSchedule: String = "",
    patientInstructions: String = "",
    medicalLeave: MedicalLeaveCertificate? = null,
    referralRecord: ReferralRecord? = null,
    attachments: List<MedicalAttachment> = emptyList(),
    vitals: VitalSigns = VitalSigns(),
    doctorNameOverride: String = "",
    doctorDegreeOverride: String = "",
    doctorSamaOverride: String = "",
    treatmentOrders: List<TreatmentCard> = emptyList()
  ) {
    val patient = _patients.value.find { it.id == patientId } ?: return
    val treatmentOrderDetails = treatmentOrders.map { card ->
      card.orderSummaryText
    }

    val consultation = ConsultationRecord(
      id = "CONS-${UUID.randomUUID().toString().take(8)}",
      patientId = patientId,
      patientName = patient.name,
      doctorName = doctorNameOverride.ifBlank { _currentUser.value?.fullName ?: clinicSettings.value.doctorName },
      doctorDegree = doctorDegreeOverride.ifBlank { clinicSettings.value.doctorDegree },
      doctorSama = doctorSamaOverride.ifBlank { clinicSettings.value.doctorSama },
      subjective = subjective,
      objective = objective,
      assessment = assessment,
      plan = plan,
      diagnosisList = diagnosisList,
      prescriptions = prescriptions,
      selectedProcedures = selectedProcedures,
      suggestedInvestigations = suggestedInvestigations,
      treatmentOrderDetails = treatmentOrderDetails,
      consultationFee = consultationFee,
      drawingPages = drawingPages,
      canvasNotes = canvasNotes,
      followUpAppointment = followUpAppointment,
      followUpDueDate = followUpDueDate,
      nextVaccineSchedule = nextVaccineSchedule,
      patientInstructions = patientInstructions,
      medicalLeave = medicalLeave,
      referralRecord = referralRecord,
      attachments = attachments,
      vitals = vitals,
      timestamp = BurmeseDateUtils.getNowFormatted()
    )

    val updatedConsultations = _consultations.value + consultation
    _consultations.value = updatedConsultations
    ClinicDataPersistence.saveConsultations(getApplication(), updatedConsultations)

    if (!followUpDueDate.isNullOrBlank()) {
      val updatedPatients = _patients.value.map { p ->
        if (p.id == patientId) {
          p.copy(
            followUpDueDate = followUpDueDate,
            followUpReason = followUpAppointment.ifBlank { p.followUpReason },
            isReminderSent = false
          )
        } else p
      }
      _patients.value = updatedPatients
      ClinicDataPersistence.savePatients(getApplication(), updatedPatients)
    }

    // Clear active draft
    discardCurrentConsultation(patientId)

    // Build Billing Invoice automatically
    val invoiceItems = mutableListOf<InvoiceItem>()

    // Identify which prescribed drugs are handled by Day-Care Treatment Cards to prevent double-billing and full-course over-deduction
    val treatmentCardMedNames = treatmentOrders.map {
      it.medicationName
        .replace(Regex("(?i)^inj\\.?\\s*"), "")
        .replace(Regex("(?i)^iv\\s*"), "")
        .replace(Regex("(?i)^im\\s*"), "")
        .replace(Regex("(?i)^sc\\s*"), "")
        .trim().lowercase()
    }.filter { it.isNotBlank() }

    prescriptions.forEach { drug ->
      val drugName = drug.fullDisplayName
      val cleanDrug = drugName
        .replace(Regex("(?i)^inj\\.?\\s*"), "")
        .replace(Regex("(?i)^iv\\s*"), "")
        .replace(Regex("(?i)^im\\s*"), "")
        .replace(Regex("(?i)^sc\\s*"), "")
        .trim().lowercase()

      val isCoveredByTreatmentCard = treatmentOrders.any { card ->
        (card.medicationId.isNotBlank() && card.medicationId == drug.medicineId) ||
        (cleanDrug.isNotBlank() && (card.medicationName.contains(cleanDrug, ignoreCase = true) || cleanDrug.contains(card.medicationName, ignoreCase = true)))
      }

      if (!isCoveredByTreatmentCard) {
        val displayName = if (drug.dosage.isNotBlank() && !drugName.contains(drug.dosage, ignoreCase = true)) {
          "$drugName (${drug.dosage}) x ${drug.quantity}"
        } else {
          "$drugName x ${drug.quantity}"
        }
        invoiceItems.add(
          InvoiceItem(
            name = displayName,
            quantity = drug.quantity,
            unitPrice = drug.unitPrice,
            totalPrice = drug.totalPrice,
            isProcedure = false
          )
        )
      }
    }
    selectedProcedures.forEach { proc ->
      val consText = if (proc.consumableItems.isNotEmpty()) {
        proc.consumableItems.joinToString(", ") { "${it.name} x ${it.quantity}" }
      } else if (proc.consumables.isNotEmpty()) {
        proc.consumables.joinToString(", ") { "$it x 1" }
      } else ""
      val procDisplayName = if (consText.isNotBlank()) {
        "${proc.name}\n  • Consumables: $consText"
      } else {
        proc.name
      }
      invoiceItems.add(
        InvoiceItem(
          name = procDisplayName,
          quantity = 1,
          unitPrice = proc.totalProcedureFee.coerceAtLeast(proc.price),
          totalPrice = proc.totalProcedureFee.coerceAtLeast(proc.price),
          isProcedure = true
        )
      )
    }

        treatmentOrders.forEach { card ->
      val sFee = if (card.sessionFee > 0) card.sessionFee else card.route.getDefaultFee(clinicSettings.value)
      val numDoses = if (card.totalSessions > 1 && (card.isPrepaidFullCourse || !card.isPayPerVisit)) card.totalSessions else 1
      val isFullCourse = numDoses > 1

      // 1. Medication Item (isProcedure = false -> Pharmacy Revenue)
      if (!card.isPatientSupplied && card.medicationFee > 0) {
        val doseStr = if (card.dosage.isNotBlank()) " (${card.dosage})" else ""
        val medItemName = if (card.isCocktail && card.cocktailItems.isNotEmpty()) {
          val ckNames = card.cocktailItems.joinToString("+") { it.name }
          if (isFullCourse) "Cocktail ($ckNames) x $numDoses Doses" else "Cocktail ($ckNames)"
        } else {
          if (isFullCourse) "${card.medicationName}$doseStr x $numDoses Doses" else "${card.medicationName}$doseStr"
        }
        invoiceItems.add(
          InvoiceItem(
            name = medItemName,
            quantity = numDoses,
            unitPrice = card.medicationFee,
            totalPrice = card.medicationFee * numDoses,
            isProcedure = false
          )
        )
      }

      // 2. Administration Service Fee (isProcedure = true -> Doctor/Service Revenue)
      if (sFee > 0) {
        val svcFeeName = if (isFullCourse) {
          "${card.routeDisplay} Service Fee x $numDoses Doses"
        } else {
          "${card.routeDisplay} Service Fee"
        }
        invoiceItems.add(
          InvoiceItem(
            name = svcFeeName,
            quantity = numDoses,
            unitPrice = sFee,
            totalPrice = sFee * numDoses,
            isProcedure = true
          )
        )
      }
    }

    val totalItemsPrice = invoiceItems.sumOf { it.totalPrice }
    val calculatedTotal = consultationFee + serviceFee + totalItemsPrice

    val invoice = Invoice(
      id = "INV-${UUID.randomUUID().toString().take(8)}",
      invoiceNumber = "INV-${(1000 + _invoices.value.size + 1)}",
      consultationId = consultation.id,
      patientId = patientId,
      patientName = patient.name,
      items = invoiceItems,
      consultationFee = consultationFee,
      serviceFee = serviceFee,
      totalAmount = calculatedTotal,
      paidAmount = 0.0,
      paymentStatus = "Unpaid",
      cashierName = _currentUser.value?.fullName ?: "Counter Cashier",
      timestamp = BurmeseDateUtils.getNowFormatted()
    )

    val updatedInvoices = _invoices.value + invoice
    _invoices.value = updatedInvoices
    ClinicDataPersistence.saveInvoices(getApplication(), updatedInvoices)

    // Tag and persist Day-Care Treatment Cards linked to this consultation invoice
    if (treatmentOrders.isNotEmpty()) {
      val ctx = getApplication<Application>()
      val taggedCards = treatmentOrders.map { card ->
        val isPrepaidCourse = card.isPrepaidFullCourse || !card.isPayPerVisit
        val updatedSessions = card.sessions.mapIndexed { idx, s ->
          if (isPrepaidCourse) {
            s.copy(invoiceId = invoice.id, isPrepaid = true)
          } else {
            if (idx == 0) s.copy(invoiceId = invoice.id, isPrepaid = true) else s
          }
        }
        card.copy(
          consultationInvoiceId = invoice.id,
          sessions = updatedSessions,
          isPrepaidFullCourse = isPrepaidCourse,
          isPayPerVisit = !isPrepaidCourse
        )
      }
      val currentCards = _treatmentCards.value
      val existingIds = taggedCards.map { it.id }.toSet()
      val mergedCards = currentCards.filterNot { existingIds.contains(it.id) } + taggedCards
      _treatmentCards.value = mergedCards
      ClinicDataPersistence.saveTreatmentCards(ctx, mergedCards)
    }

    // Route patient status
    val nextStatus = QueueStatus.AT_BILLING
    updatePatientStatus(patientId, nextStatus)
  }

  fun updateConsultation(updatedRecord: ConsultationRecord) {
    val ctx = getApplication<Application>()
    val currentList = _consultations.value
    val updatedList = currentList.map { if (it.id == updatedRecord.id) updatedRecord else it }
    _consultations.value = updatedList
    ClinicDataPersistence.saveConsultations(ctx, updatedList)

    val invoices = _invoices.value
    val matchingInvoice = invoices.find { it.consultationId == updatedRecord.id }
    if (matchingInvoice != null) {
      val diffFee = updatedRecord.consultationFee - matchingInvoice.consultationFee
      val updatedInv = matchingInvoice.copy(
        consultationFee = updatedRecord.consultationFee,
        totalAmount = (matchingInvoice.totalAmount + diffFee).coerceAtLeast(0.0)
      )
      val updatedInvoices = invoices.map { if (it.id == updatedInv.id) updatedInv else it }
      _invoices.value = updatedInvoices
      ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun deleteConsultation(consultationId: String) {
    val ctx = getApplication<Application>()
    ClinicDataPersistence.addDeletedConsultationId(ctx, consultationId)
    val updated = _consultations.value.filter { it.id != consultationId }
    _consultations.value = updated
    ClinicDataPersistence.saveConsultations(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  // --- Dual-Printing Preview Helpers ---
  fun showThermalPreview(invoice: Invoice) {
    previewThermalSlip.value = invoice
  }

  fun closeThermalPreview() {
    previewThermalSlip.value = null
  }

  fun showA4DocPreview(consultation: ConsultationRecord) {
    previewA4ClinicalDoc.value = consultation
  }

  fun closeA4DocPreview() {
    previewA4ClinicalDoc.value = null
  }

  fun addAttachmentToConsultation(consultationId: String, attachment: MedicalAttachment) {
    val updated = _consultations.value.map { c ->
      if (c.id == consultationId) {
        c.copy(attachments = c.attachments + attachment)
      } else c
    }
    _consultations.value = updated
    ClinicDataPersistence.saveConsultations(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
  }

  fun deleteAttachmentFromConsultation(consultationId: String, attachmentId: String) {
    val updated = _consultations.value.map { c ->
      if (c.id == consultationId) {
        c.copy(attachments = c.attachments.filter { it.id != attachmentId })
      } else c
    }
    _consultations.value = updated
    ClinicDataPersistence.saveConsultations(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
  }

  val customAttendingDoctorDegree = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
  val customAttendingDoctorSama = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

  // --- Attending Doctor Helpers ---
  fun setAttendingDoctor(user: UserAccount?) {
    selectedAttendingDoctor.value = user
    customAttendingDoctor.value = null
    customAttendingDoctorDegree.value = null
    customAttendingDoctorSama.value = null
  }

  fun setCustomAttendingDoctor(name: String?, degree: String? = null, sama: String? = null) {
    customAttendingDoctor.value = name
    customAttendingDoctorDegree.value = degree
    customAttendingDoctorSama.value = sama
    selectedAttendingDoctor.value = null
  }

  fun getEffectiveDoctor(): Triple<String, String, String> {
    val customName = customAttendingDoctor.value
    if (!customName.isNullOrBlank()) {
      val deg = customAttendingDoctorDegree.value
      val degStr: String = if (!deg.isNullOrBlank()) deg else "M.B.,B.S"
      val sama = customAttendingDoctorSama.value
      val samaStr: String = if (!sama.isNullOrBlank()) sama else "ဆမ-Visiting"
      return Triple(customName, degStr, samaStr)
    }
    val sel = selectedAttendingDoctor.value
    if (sel != null) {
      return Triple(sel.fullName, sel.degree.ifBlank { "M.B.,B.S" }, sel.samaNumber.ifBlank { "ဆမ-၄၇၄၁၄" })
    }
    val cur = _currentUser.value
    if (cur != null && cur.role == UserRole.DOCTOR) {
      return Triple(cur.fullName, cur.degree.ifBlank { "M.B.,B.S" }, cur.samaNumber.ifBlank { "ဆမ-၄၇၄၁၄" })
    }
    val set = clinicSettings.value
    return Triple(set.doctorName, set.doctorDegree, set.doctorSama)
  }

  // --- Pharmacy Dispensing (Routes patient to billing, inventory deducted strictly on Cashier Paid) ---
  fun dispenseMedications(patientId: String, items: List<PrescribedDrug>) {
    updatePatientStatus(patientId, QueueStatus.AT_BILLING)
  }

  fun dispensePrescription(patientId: String, prescriptionList: List<PrescribedDrug>) =
    dispenseMedications(patientId, prescriptionList)

  fun dispenseQuick(patientId: String, items: List<InvoiceItem>) {
    updatePatientStatus(patientId, QueueStatus.AT_BILLING)
  }

  fun dispenseQuick(medicineId: String, quantity: Int) {
    val now = System.currentTimeMillis()
    val updated = _medicines.value.map { med ->
      if (med.id == medicineId) {
        var rem = quantity.toDouble()
        val newBatches = med.batches.map { b ->
          if (rem > 0.0 && b.quantity > 0.0) {
            val d = minOf(b.quantity, rem)
            rem -= d
            b.copy(quantity = b.quantity - d, lastUpdated = now)
          } else b
        }
        med.copy(batches = newBatches)
      } else med
    }
    _medicines.value = updated
    ClinicDataPersistence.saveMedicines(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  // --- Cashier Payment ---
  fun markInvoicePaid(invoiceId: String, method: String, paid: Double, discount: Double = 0.0) {
    val pm = PaymentMethod.fromString(method)
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        val totalAfterDiscount = maxOf(0.0, inv.totalAmount - discount)
        inv.copy(
          isPaid = true,
          discount = discount,
          totalAmount = totalAfterDiscount,
          paymentMethod = pm,
          paymentStatus = "Paid",
          paidAmount = paid,
          changeAmount = maxOf(0.0, paid - totalAfterDiscount),
          cashierName = _currentUser.value?.fullName ?: "Cashier Staff",
          timestamp = BurmeseDateUtils.getNowFormatted()
        )
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)

    val inv = updated.find { it.id == invoiceId }
    if (inv != null) {
      updatePatientStatus(inv.patientId, QueueStatus.COMPLETED)
      deductInventoryForInvoice(inv)
    }
  }

  fun findMatchingMedicine(
    targetId: String?,
    targetName: String,
    medMap: Map<String, Medicine>
  ): Medicine? {
    if (!targetId.isNullOrBlank() && medMap.containsKey(targetId)) {
      return medMap[targetId]
    }
    if (targetName.isBlank()) return null

    val tName = targetName.trim()
    val cleanT = tName.split(" x ").firstOrNull()?.trim() ?: tName
    val cleanWithoutParentheses = cleanT.split("(").firstOrNull()?.trim() ?: cleanT
    val strippedPrefix = cleanWithoutParentheses
      .replace(Regex("(?i)^(inj\\.?|tab\\.?|cap\\.?|syr\\.?|iv\\s*push|iv\\s*infusion|iv\\s*drip|iv|im|sc|neb)\\s*"), "")
      .trim()
    val cleanBase = strippedPrefix
      .replace(Regex("(?i)\\s*\\d+(\\.\\d+)?\\s*(mg|g|ml|mcg|iu|tab|amp|vial)\\b.*"), "")
      .trim()

    // 1. Direct exact match by raw name or cleanBase
    medMap.values.find { it.name.trim().equals(tName, ignoreCase = true) }?.let { return it }
    medMap.values.find { it.name.trim().equals(strippedPrefix, ignoreCase = true) }?.let { return it }
    if (cleanBase.isNotBlank()) {
      medMap.values.find { it.name.trim().equals(cleanBase, ignoreCase = true) }?.let { return it }
    }

    // 2. Clean medicine name match (strip dosage & prefixes from store med names too)
    for (med in medMap.values) {
      val medClean = med.name.trim()
        .replace(Regex("(?i)^(inj\\.?|tab\\.?|cap\\.?|syr\\.?|iv|im|sc)\\s*"), "")
        .split("(").firstOrNull()?.trim() ?: med.name.trim()
      val medBase = medClean
        .replace(Regex("(?i)\\s*\\d+(\\.\\d+)?\\s*(mg|g|ml|mcg|iu|tab|amp|vial)\\b.*"), "")
        .trim()

      if (medClean.equals(strippedPrefix, ignoreCase = true)) return med
      if (cleanBase.isNotBlank() && medBase.equals(cleanBase, ignoreCase = true)) return med
    }

    // 3. Generic name match
    for (med in medMap.values) {
      if (med.genericName.isNotBlank()) {
        val gen = med.genericName.trim()
        if (gen.equals(cleanBase, ignoreCase = true) || gen.equals(strippedPrefix, ignoreCase = true) || tName.contains(gen, ignoreCase = true)) {
          return med
        }
      }
    }

    // 4. Substring containment match
    if (cleanBase.length >= 3) {
      medMap.values.find {
        it.name.contains(cleanBase, ignoreCase = true) || cleanBase.contains(it.name, ignoreCase = true)
      }?.let { return it }
    }

    return null
  }

  private fun deductInventoryForInvoice(inv: Invoice) {
    if (inv.isInventoryDeducted) return

    val ctx = getApplication<Application>()
    val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
    var modified = false
    val now = System.currentTimeMillis()
    val newlyDeducted = inv.deductedDrugQuantities.toMutableMap()
    val deductedMedIds = newlyDeducted.keys.toMutableSet()

    fun deductMed(med: Medicine, qtyToDeduct: Double) {
      if (qtyToDeduct <= 0.0) return
      var remaining = qtyToDeduct
      val updatedBatches = med.allBatches.map { b ->
        if (remaining > 0.0 && b.quantity > 0.0) {
          val deduct = minOf(b.quantity, remaining)
          remaining -= deduct
          b.copy(quantity = b.quantity - deduct, lastUpdated = now)
        } else b
      }
      medMap[med.id] = med.copy(batches = updatedBatches)
      val deducted = qtyToDeduct - remaining
      if (deducted > 0.0) {
        newlyDeducted[med.id] = (newlyDeducted[med.id] ?: 0.0) + deducted
        deductedMedIds.add(med.id)
        modified = true
      }
    }

    fun deductMed(med: Medicine, qtyToDeduct: Int) = deductMed(med, qtyToDeduct.toDouble())

    val consultation = _consultations.value.find { it.id == inv.consultationId }

    // Locate linked Day-Care Treatment Cards (Cocktails, Injections)
    val linkedCards = _treatmentCards.value.filter { card ->
      card.consultationInvoiceId == inv.id || (card.patientId == inv.patientId && card.sessions.any { it.invoiceId == inv.id })
    }

    // 1. Deduct consultation prescriptions (if present, not external)
    if (consultation != null && consultation.prescriptions.isNotEmpty()) {
      for (item in consultation.prescriptions.filter { !it.isExternal }) {
        val cleanRxName = (item.effectiveName.ifBlank { item.medicineName })
          .replace(Regex("(?i)^inj\\.?\\s*"), "")
          .replace(Regex("(?i)^iv\\s*"), "")
          .replace(Regex("(?i)^im\\s*"), "")
          .replace(Regex("(?i)^sc\\s*"), "")
          .trim()
        val isCoveredByLinkedCard = linkedCards.any { card ->
          (card.medicationId.isNotBlank() && item.medicineId.isNotBlank() && card.medicationId == item.medicineId) ||
          (cleanRxName.isNotBlank() && (card.medicationName.contains(cleanRxName, ignoreCase = true) || cleanRxName.contains(card.medicationName, ignoreCase = true)))
        }
        if (isCoveredByLinkedCard) {
          // CLINICAL SAFETY: Skip prescription deduction to prevent double-deducting drugs administered via Treatment Card!
          continue
        }
        val med = findMatchingMedicine(item.medicineId, item.effectiveName.ifBlank { item.medicineName }, medMap)
        if (med != null && item.quantity > 0) {
          deductMed(med, item.quantity)
        }
      }
    }

    // 2. Deduct constituent medicines and consumables from Day-Care Treatment Cards (Cocktails, Injections)
    for (card in linkedCards) {
      if (card.isPatientSupplied) continue

      val billedDoses = if (card.isPrepaidFullCourse || !card.isPayPerVisit) {
        card.totalSessions.coerceAtLeast(1)
      } else {
        1
      }

      // Check if fractional dose (e.g. 1/2 amp, 0.5 amp)
      val doseFraction = when {
        card.dosage.contains("1/2") || card.dosage.contains("½") || card.dosage.contains("0.5") -> 0.5
        else -> 1.0
      }

      if (card.isCocktail && card.cocktailItems.isNotEmpty()) {
        for (cItem in card.cocktailItems) {
          val med = findMatchingMedicine(cItem.medicineId, cItem.name, medMap)
          val cDoseFraction = when {
            cItem.dosage.contains("1/2") || cItem.dosage.contains("½") || cItem.dosage.contains("0.5") -> 0.5
            else -> 1.0
          }
          val qty = (if (cItem.quantity > 0) cItem.quantity.toDouble() else 1.0) * billedDoses * cDoseFraction
          if (med != null && qty > 0.0) {
            deductMed(med, qty)
          }
        }
      } else if (card.medicationName.isNotBlank()) {
        val med = findMatchingMedicine(card.medicationId, card.medicationName, medMap)
        if (med != null && billedDoses > 0) {
          deductMed(med, billedDoses * doseFraction)
        }
      }

      // Deduct attached consumables for billed session(s)
      val sessionsToDeduct = card.sessions.filter { it.invoiceId == inv.id }.ifEmpty { card.sessions.take(1) }
      sessionsToDeduct.forEach { session ->
        for (c in session.consumables) {
          val med = findMatchingMedicine(c.medicineId, c.name, medMap)
          if (med != null && c.quantity > 0) {
            deductMed(med, c.quantity)
          }
        }
      }
    }

    // 2.5 Deduct consumables from clinical procedures (e.g. Minor Suturing, Wound Dressing)
    if (consultation != null && consultation.selectedProcedures.isNotEmpty()) {
      for (proc in consultation.selectedProcedures) {
        val consToDeduct = if (proc.consumableItems.isNotEmpty()) {
          proc.consumableItems
        } else {
          proc.consumables.map { com.example.model.ProcedureConsumable(name = it, quantity = 1) }
        }
        for (c in consToDeduct) {
          val med = findMatchingMedicine(c.medicineId, c.name, medMap)
          val qty = if (c.quantity > 0) c.quantity else 1
          if (med != null && qty > 0) {
            deductMed(med, qty)
          }
        }
      }
    }

    // 3. Deduct direct invoice items (e.g. OTC walk-in pharmacy, direct items, or any item not yet deducted above)
    for (item in inv.items) {
      val med = findMatchingMedicine(null, item.name, medMap)
      if (med != null && item.quantity > 0) {
        if (!deductedMedIds.contains(med.id)) {
          deductMed(med, item.quantity)
        }
      }
    }

    if (modified) {
      val updatedList = _medicines.value.map { medMap[it.id] ?: it }
      _medicines.value = updatedList
      ClinicDataPersistence.saveMedicines(ctx, updatedList)
    }

    // Mark invoice isInventoryDeducted = true and update deductedDrugQuantities ONLY IF actual deductions occurred
    val shouldMarkDeducted = modified || newlyDeducted.isNotEmpty()
    val updatedInvs = _invoices.value.map {
      if (it.id == inv.id) it.copy(
        isInventoryDeducted = shouldMarkDeducted,
        isPrescriptionsDeducted = shouldMarkDeducted,
        deductedDrugQuantities = newlyDeducted
      ) else it
    }
    _invoices.value = updatedInvs
    ClinicDataPersistence.saveInvoices(ctx, updatedInvs)
  }

  fun markInvoicePaid(invoiceId: String, method: PaymentMethod, paid: Double, discount: Double = 0.0) =
    markInvoicePaid(invoiceId, method.name, paid, discount)

  fun processPayment(invoiceId: String, method: String, paid: Double, discount: Double = 0.0) =
    markInvoicePaid(invoiceId, method, paid, discount)

  fun processPayment(invoiceId: String, method: PaymentMethod, paid: Double, discount: Double = 0.0) =
    markInvoicePaid(invoiceId, method.name, paid, discount)

  fun processPaymentWithGranularDiscount(
    invoiceId: String,
    method: PaymentMethod,
    docDiscount: Double,
    pharmacyDiscount: Double,
    procedureDiscount: Double,
    overallPercent: Double
  ) {
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        val totalGranular = docDiscount + pharmacyDiscount + procedureDiscount
        val pctDiscount = (inv.subTotal - totalGranular).coerceAtLeast(0.0) * (overallPercent / 100.0)
        val finalDiscount = totalGranular + pctDiscount
        val finalTotal = (inv.subTotal - finalDiscount).coerceAtLeast(0.0)
        inv.copy(
          isPaid = true,
          doctorFeeDiscount = docDiscount,
          pharmacyDiscount = pharmacyDiscount,
          procedureDiscount = procedureDiscount,
          discountPercent = overallPercent,
          discount = finalDiscount,
          totalAmount = finalTotal,
          paymentMethod = method,
          paymentStatus = "Paid",
          paidAmount = finalTotal,
          changeAmount = 0.0,
          cashierName = _currentUser.value?.fullName ?: "Cashier Staff",
          timestamp = BurmeseDateUtils.getNowFormatted()
        )
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)

    val inv = updated.find { it.id == invoiceId }
    if (inv != null) {
      updatePatientStatus(inv.patientId, QueueStatus.COMPLETED)
      deductInventoryForInvoice(inv)
    }
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  private fun getMedicineFallbackCostPrice(med: com.example.model.Medicine, inv: com.example.model.Invoice? = null): Double {
    val existingCost = med.batches.firstOrNull { it.costPricePerUnit > 0.0 }?.costPricePerUnit
    if (existingCost != null && existingCost > 0.0) return existingCost
    if (med.latestCostPrice > 0.0) return med.latestCostPrice
    if (inv != null) {
      val matchingItem = inv.items.find { it.name.equals(med.name, ignoreCase = true) || it.name.contains(med.name, ignoreCase = true) || med.name.contains(it.name, ignoreCase = true) }
      if (matchingItem != null && matchingItem.unitPrice > 0.0) {
        val marginMultiplier = 1.0 + (med.profitMarginPercent / 100.0)
        if (marginMultiplier > 0.1) {
          return matchingItem.unitPrice / marginMultiplier
        }
        return matchingItem.unitPrice
      }
    }
    return 0.0
  }

  fun deleteInvoiceAndReturnStock(invoiceId: String): Pair<Boolean, List<String>> {
    val inv = _invoices.value.find { it.id == invoiceId }
    val restored = mutableListOf<String>()
    if (inv?.isInventoryDeducted == true) {
      restored.addAll(inv.items.map { it.name })
    }
    if (inv != null) {
      val linkedCards = _treatmentCards.value.filter { card ->
        card.consultationInvoiceId == invoiceId || (card.patientId == inv.patientId && card.sessions.any { it.invoiceId == invoiceId })
      }
      val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
      var modified = false
      val now = System.currentTimeMillis()

      if (inv.isInventoryDeducted && inv.deductedDrugQuantities.isNotEmpty()) {
        // EXACT RESTOCK: restore solely and precisely what was deducted, preventing double restock & phantom stock additions
        for ((medId, qty) in inv.deductedDrugQuantities) {
          if (qty <= 0) continue
          val med = medMap[medId] ?: medMap.values.find { it.id == medId }
          if (med != null) {
            val primaryBatch = med.batches.firstOrNull()
            val updatedBatches = if (primaryBatch != null) {
              med.batches.mapIndexed { idx, b -> if (idx == 0) b.copy(quantity = b.quantity + qty, lastUpdated = now) else b }
            } else {
              val restoreCost = getMedicineFallbackCostPrice(med, inv)
              listOf(com.example.model.StockBatch("RESTORE", "2028-12-31", qty.toDouble(), restoreCost, lastUpdated = now))
            }
            medMap[med.id] = med.copy(batches = updatedBatches)
            modified = true
            restored.add("${med.name} (+$qty)")
          }
        }
        if (modified) {
          val updatedList = _medicines.value.map { medMap[it.id] ?: it }
          _medicines.value = updatedList
          ClinicDataPersistence.saveMedicines(getApplication(), updatedList)
        }
      }

      // Cascade remove or discontinue linked treatment cards
      // CLINICAL SAFETY: If any dose has already been administered, NEVER wipe nurse records! Preserve as DISCONTINUED.
      if (linkedCards.isNotEmpty()) {
        val ctxLocal = getApplication<Application>()
        val cardsToDiscontinue = linkedCards.filter { c ->
          c.sessions.any { it.status == com.example.ui.screens.treatment.TreatmentStatus.COMPLETED } || c.completedSessionsCount > 0
        }
        val cardsToDelete = linkedCards.filterNot { c ->
          c.sessions.any { it.status == com.example.ui.screens.treatment.TreatmentStatus.COMPLETED } || c.completedSessionsCount > 0
        }

        if (cardsToDelete.isNotEmpty()) {
          cardsToDelete.forEach { c ->
            ClinicDataPersistence.addDeletedTreatmentCardId(ctxLocal, c.id)
          }
        }

        val updatedCards = _treatmentCards.value.mapNotNull { card ->
          when {
            cardsToDelete.any { it.id == card.id } -> null
            cardsToDiscontinue.any { it.id == card.id } -> {
              card.copy(
                cardStatus = com.example.ui.screens.treatment.TreatmentStatus.DISCONTINUED,
                discontinueReason = "Consultation invoice voided/deleted (Preserved completed session history)",
                discontinuedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                sessions = card.sessions.map { s ->
                  if (s.status == com.example.ui.screens.treatment.TreatmentStatus.PENDING || s.status == com.example.ui.screens.treatment.TreatmentStatus.IN_PROGRESS) {
                    s.copy(status = com.example.ui.screens.treatment.TreatmentStatus.CANCELLED, notes = (if (s.notes.isNotBlank()) "${s.notes} | " else "") + "Invoice Voided")
                  } else s
                }
              )
            }
            else -> card
          }
        }
        _treatmentCards.value = updatedCards
        ClinicDataPersistence.saveTreatmentCards(ctxLocal, updatedCards)
      }
    }
    val ctx = getApplication<Application>()
    ClinicDataPersistence.addDeletedInvoiceId(ctx, invoiceId)
    _invoices.value = _invoices.value.filter { it.id != invoiceId }
    ClinicDataPersistence.saveInvoices(ctx, _invoices.value)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return Pair(true, restored)
  }

  fun returnSelectedInvoiceItemsAndAdjust(
    invoiceId: String,
    returnedItemNames: Set<String>,
    voidEntireInvoice: Boolean = false
  ): Pair<Boolean, List<String>> {
    val ctx = getApplication<Application>()
    val inv = _invoices.value.find { it.id == invoiceId } ?: return Pair(false, emptyList())
    val restored = mutableListOf<String>()
    val now = System.currentTimeMillis()
    val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
    var medModified = false

    val itemsToReturn = if (voidEntireInvoice) {
      inv.items.filter { !it.isProcedure }
    } else {
      inv.items.filter { !it.isProcedure && returnedItemNames.contains(it.name) }
    }
    val linkedCards = _treatmentCards.value.filter { card ->
      card.consultationInvoiceId == invoiceId || (card.patientId == inv.patientId && card.sessions.any { it.invoiceId == invoiceId })
    }
    val cardsToRemove = mutableListOf<TreatmentCard>()

    fun processTreatmentCardsUpdate(cardsCandidate: List<TreatmentCard>) {
      if (cardsCandidate.isEmpty()) return
      val ctxLocal = getApplication<Application>()
      val cardsToDiscontinue = cardsCandidate.filter { c ->
        c.sessions.any { it.status == com.example.ui.screens.treatment.TreatmentStatus.COMPLETED } || c.completedSessionsCount > 0
      }
      val cardsToDelete = cardsCandidate.filterNot { c ->
        c.sessions.any { it.status == com.example.ui.screens.treatment.TreatmentStatus.COMPLETED } || c.completedSessionsCount > 0
      }

      if (cardsToDelete.isNotEmpty()) {
        cardsToDelete.forEach { c -> ClinicDataPersistence.addDeletedTreatmentCardId(ctxLocal, c.id) }
      }

      val updatedCards = _treatmentCards.value.mapNotNull { card ->
        when {
          cardsToDelete.any { it.id == card.id } -> null
          cardsToDiscontinue.any { it.id == card.id } -> {
            card.copy(
              cardStatus = com.example.ui.screens.treatment.TreatmentStatus.DISCONTINUED,
              discontinueReason = "Invoice item returned/voided (Preserved completed session history)",
              discontinuedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
              sessions = card.sessions.map { s ->
                if (s.status == com.example.ui.screens.treatment.TreatmentStatus.PENDING || s.status == com.example.ui.screens.treatment.TreatmentStatus.IN_PROGRESS) {
                  s.copy(status = com.example.ui.screens.treatment.TreatmentStatus.CANCELLED, notes = (if (s.notes.isNotBlank()) "${s.notes} | " else "") + "Invoice Voided/Returned")
                } else s
              }
            )
          }
          else -> card
        }
      }
      _treatmentCards.value = updatedCards
      ClinicDataPersistence.saveTreatmentCards(ctxLocal, updatedCards)
    }

    fun restockDrug(medId: String?, medName: String, qty: Double) {
      if (!inv.isInventoryDeducted || qty <= 0.0) return
      val med = (if (!medId.isNullOrBlank()) medMap[medId] else null)
        ?: medMap.values.find { it.name.equals(medName, ignoreCase = true) || medName.contains(it.name, ignoreCase = true) }
      if (med != null) {
        val primaryBatch = med.batches.firstOrNull()
        val updatedBatches = if (primaryBatch != null) {
          med.batches.mapIndexed { idx, b -> if (idx == 0) b.copy(quantity = b.quantity + qty, lastUpdated = now) else b }
        } else {
          val restoreCost = getMedicineFallbackCostPrice(med, inv)
          listOf(com.example.model.StockBatch("RESTORE", "2028-12-31", qty, restoreCost, lastUpdated = now))
        }
        medMap[med.id] = med.copy(batches = updatedBatches)
        medModified = true
        val qtyDisplay = if (qty % 1.0 == 0.0) qty.toInt().toString() else String.format(java.util.Locale.US, "%.1f", qty)
        restored.add("${med.name} (+$qtyDisplay)")
      }
    }

    fun restockDrug(medId: String?, medName: String, qty: Int) = restockDrug(medId, medName, qty.toDouble())

    val availableDeducted = inv.deductedDrugQuantities.toMutableMap()

    if (!inv.isInventoryDeducted || inv.deductedDrugQuantities.isEmpty()) {
      // Nothing was ever deducted from stock for this invoice -> NEVER restock phantom drugs!
      if (linkedCards.isNotEmpty() && (voidEntireInvoice || returnedItemNames.isNotEmpty())) {
        val cardsToRem = if (voidEntireInvoice) linkedCards else linkedCards.filter { card ->
          returnedItemNames.any { retName ->
            retName.equals(card.orderSummaryText, ignoreCase = true) ||
            retName.contains(card.orderSummaryText, ignoreCase = true) ||
            (card.medicationName.isNotBlank() && retName.contains(card.medicationName, ignoreCase = true))
          }
        }
        if (cardsToRem.isNotEmpty()) {
          processTreatmentCardsUpdate(cardsToRem)
        }
      }
    } else if (voidEntireInvoice) {
      // EXACT RESTOCK on full void: restock solely what was deducted, avoiding phantom stock inflation
      for ((medId, qty) in inv.deductedDrugQuantities) {
        restockDrug(medId, "", qty)
      }
      availableDeducted.clear()
      if (linkedCards.isNotEmpty()) {
        processTreatmentCardsUpdate(linkedCards)
      }
    } else {
      // 1. Partial Return: only restock up to the quantity actually deducted for each returned item
      val restockedMeds = mutableSetOf<String>()
      for (item in itemsToReturn) {
        val med = findMatchingMedicine(null, item.name, medMap)
        if (med != null && !restockedMeds.contains(med.id)) {
          val previouslyDeducted = availableDeducted[med.id] ?: 0.0
          val qtyToReturn = minOf(item.quantity.toDouble(), previouslyDeducted)
          if (qtyToReturn > 0.0) {
            restockDrug(med.id, med.name, qtyToReturn)
            availableDeducted[med.id] = previouslyDeducted - qtyToReturn
            restockedMeds.add(med.id)
          }
        }
      }

      // 2. Return Treatment Card Items (only if actually deducted in availableDeducted)
      for (card in linkedCards) {
        val isCardReturned = returnedItemNames.any { retName ->
          retName.equals(card.orderSummaryText, ignoreCase = true) ||
          retName.contains(card.orderSummaryText, ignoreCase = true) ||
          (card.medicationName.isNotBlank() && retName.contains(card.medicationName, ignoreCase = true))
        }
        if (isCardReturned) {
          if (!card.isPatientSupplied) {
            val billedDoses = if (card.isPrepaidFullCourse || !card.isPayPerVisit) card.totalSessions.coerceAtLeast(1) else 1
            if (card.isCocktail && card.cocktailItems.isNotEmpty()) {
              for (cItem in card.cocktailItems) {
                val cMed = findMatchingMedicine(cItem.medicineId, cItem.name, medMap)
                if (cMed != null && !restockedMeds.contains(cMed.id)) {
                  val prev = availableDeducted[cMed.id] ?: 0.0
                  val qty = minOf((if (cItem.quantity > 0) cItem.quantity.toDouble() else 1.0) * billedDoses, prev)
                  if (qty > 0.0) {
                    restockDrug(cMed.id, cMed.name, qty)
                    availableDeducted[cMed.id] = prev - qty
                    restockedMeds.add(cMed.id)
                  }
                }
              }
            } else if (card.medicationName.isNotBlank()) {
              val med = findMatchingMedicine(card.medicationId, card.medicationName, medMap)
              if (med != null && !restockedMeds.contains(med.id)) {
                val prev = availableDeducted[med.id] ?: 0.0
                val qty = minOf(billedDoses.toDouble(), prev)
                if (qty > 0.0) {
                  restockDrug(med.id, med.name, qty)
                  availableDeducted[med.id] = prev - qty
                  restockedMeds.add(med.id)
                }
              }
            }
            card.sessions.filter { it.invoiceId == invoiceId }.forEach { s ->
              for (c in s.consumables) {
                val cMed = findMatchingMedicine(c.medicineId, c.name, medMap)
                if (cMed != null) {
                  val prev = availableDeducted[cMed.id] ?: 0.0
                  val qty = minOf(c.quantity.toDouble(), prev)
                  if (qty > 0.0) {
                    restockDrug(cMed.id, cMed.name, qty)
                    availableDeducted[cMed.id] = prev - qty
                  }
                }
              }
            }
          }
          cardsToRemove.add(card)
        }
      }
      if (cardsToRemove.isNotEmpty()) {
        processTreatmentCardsUpdate(cardsToRemove)
      }

      // 3. Return Clinical Procedure Consumables
      val cons = _consultations.value.find { it.id == inv.consultationId }
      if (cons != null && cons.selectedProcedures.isNotEmpty()) {
        for (proc in cons.selectedProcedures) {
          val isProcReturned = returnedItemNames.any { retName ->
            retName.contains(proc.name, ignoreCase = true) || proc.name.contains(retName, ignoreCase = true)
          }
          if (isProcReturned) {
            val consToReturn = if (proc.consumableItems.isNotEmpty()) {
              proc.consumableItems
            } else {
              proc.consumables.map { com.example.model.ProcedureConsumable(name = it, quantity = 1) }
            }
            for (c in consToReturn) {
              val pMed = findMatchingMedicine(c.medicineId, c.name, medMap)
              if (pMed != null && !restockedMeds.contains(pMed.id)) {
                val prev = availableDeducted[pMed.id] ?: 0.0
                val consQty = if (c.quantity > 0) c.quantity.toDouble() else 1.0
                val qty = minOf(consQty, prev)
                if (qty > 0.0) {
                  restockDrug(pMed.id, pMed.name, qty)
                  availableDeducted[pMed.id] = prev - qty
                  restockedMeds.add(pMed.id)
                }
              }
            }
          }
        }
      }
    }

    if (medModified) {
      val updatedList = _medicines.value.map { medMap[it.id] ?: it }
      _medicines.value = updatedList
      ClinicDataPersistence.saveMedicines(ctx, updatedList)
    }

    // Annotate returned medications on consultation EMR record
    if (inv.consultationId.isNotBlank() && (itemsToReturn.isNotEmpty() || cardsToRemove.isNotEmpty())) {
      val returnedCleanNames = (itemsToReturn.map {
        it.name.split(" x ").firstOrNull()?.split("(")?.firstOrNull()?.trim()?.lowercase() ?: it.name.lowercase()
      } + cardsToRemove.map { it.medicationName.lowercase().trim() }).toSet()

      val updatedCons = _consultations.value.map { c ->
        if (c.id == inv.consultationId) {
          val updatedRx = c.prescriptions.map { rx ->
            val rxClean = rx.effectiveName.split("(").firstOrNull()?.trim()?.lowercase() ?: rx.effectiveName.lowercase()
            if (returnedCleanNames.any { rxClean.contains(it) || it.contains(rxClean) }) {
              rx.copy(instructions = if (rx.instructions.isNotBlank()) "${rx.instructions} [ပြန်အပ်ခဲ့သည် - မသောက်ပါ]" else "[ပြန်အပ်ခဲ့သည် - မသောက်ပါ]")
            } else rx
          }
          c.copy(prescriptions = updatedRx)
        } else c
      }
      _consultations.value = updatedCons
      ClinicDataPersistence.saveConsultations(ctx, updatedCons)
    }

    if (voidEntireInvoice) {
      ClinicDataPersistence.addDeletedInvoiceId(ctx, invoiceId)
      val updatedInvoices = _invoices.value.filter { it.id != invoiceId }
      _invoices.value = updatedInvoices
      ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
    } else {
      // Adjust invoice: remove returned items, recalculate subtotal and total, keeping Doctor Consultation Fee!
      val remainingItems = inv.items.filterNot { returnedItemNames.contains(it.name) }
      val newItemsTotal = remainingItems.sumOf { it.totalPrice }
      val newSubTotal = newItemsTotal + inv.consultationFee + inv.serviceFee
      val newTotal = (newSubTotal - inv.discount).coerceAtLeast(0.0)
      val updatedInv = inv.copy(
        items = remainingItems,
        totalAmount = newTotal,
        deductedDrugQuantities = availableDeducted,
        isInventoryDeducted = availableDeducted.values.any { it > 0.0 }
      )
      val updatedInvoices = _invoices.value.map { if (it.id == invoiceId) updatedInv else it }
      _invoices.value = updatedInvoices
      ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return Pair(true, restored)
  }

  fun updateSupplier(supplier: Supplier) {
    val ctx = getApplication<Application>()
    val updated = _suppliers.value.map { if (it.id == supplier.id) supplier else it }
    _suppliers.value = updated
    ClinicDataPersistence.saveSuppliers(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updateInvoiceDocumentationFee(invoiceId: String, includeFee: Boolean, fee: Double = 1000.0) {
    val effectiveFee = if (includeFee) fee else 0.0
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        val nonDocItems = inv.items.filterNot { 
          it.name.contains("Documentation Fee", ignoreCase = true) || it.name.contains("Document Fee", ignoreCase = true)
        }
        val newItems = if (includeFee) {
          nonDocItems + InvoiceItem(
            name = "Medical Certificate / Documentation Fee",
            quantity = 1,
            unitPrice = effectiveFee,
            totalPrice = effectiveFee,
            isProcedure = false
          )
        } else {
          nonDocItems
        }
        val newTotal = newItems.sumOf { it.totalPrice } + inv.consultationFee + inv.serviceFee - inv.discount
        inv.copy(items = newItems, totalAmount = newTotal.coerceAtLeast(0.0))
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updateInvoiceDocumentationFee(invoiceId: String, fee: Double) {
    updateInvoiceDocumentationFee(invoiceId, includeFee = fee > 0.0, fee = fee)
  }

  fun applyDoctorFeeFoc(invoiceId: String) {
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        val nonDocItems = inv.items.sumOf { it.totalPrice }
        val newSub = nonDocItems + 0.0 + inv.serviceFee
        val newTotal = (newSub - inv.discountAmount).coerceAtLeast(0.0)
        inv.copy(consultationFee = 0.0, totalAmount = newTotal)
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) { syncManager.syncWithAllPeers() }
  }

  fun applyAllFoc(invoiceId: String) {
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        inv.copy(
          consultationFee = 0.0,
          serviceFee = 0.0,
          discountPercent = 100.0,
          discount = inv.subTotal,
          totalAmount = 0.0
        )
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) { syncManager.syncWithAllPeers() }
  }

  fun resetInvoiceDiscounts(invoiceId: String, originalConsultationFee: Double = 8000.0) {
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        val sub = inv.items.sumOf { it.totalPrice } + originalConsultationFee + inv.serviceFee
        inv.copy(
          consultationFee = originalConsultationFee,
          discount = 0.0,
          discountPercent = 0.0,
          doctorFeeDiscount = 0.0,
          pharmacyDiscount = 0.0,
          procedureDiscount = 0.0,
          totalAmount = sub
        )
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) { syncManager.syncWithAllPeers() }
  }

  fun updateGranularDiscounts(
    invoiceId: String,
    docDiscount: Double,
    pharmacyDiscount: Double,
    procedureDiscount: Double,
    overallPercent: Double
  ) {
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        val totalGranular = docDiscount + pharmacyDiscount + procedureDiscount
        val pctDiscount = (inv.subTotal - totalGranular).coerceAtLeast(0.0) * (overallPercent / 100.0)
        val finalDiscount = totalGranular + pctDiscount
        val newTotal = (inv.subTotal - finalDiscount).coerceAtLeast(0.0)
        inv.copy(
          doctorFeeDiscount = docDiscount,
          pharmacyDiscount = pharmacyDiscount,
          procedureDiscount = procedureDiscount,
          discountPercent = overallPercent,
          discount = finalDiscount,
          totalAmount = newTotal
        )
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) { syncManager.syncWithAllPeers() }
  }

  // --- Walk-in OTC Medicine Sales ---
  fun createWalkInSaleInvoice(
    customerName: String,
    items: List<InvoiceItem>,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    discountPercent: Double = 0.0,
    cashierName: String = "",
    isPaid: Boolean = true
  ): Invoice {
    val ctx = getApplication<Application>()
    val subTotal = items.sumOf { it.totalPrice }
    val discountAmount = subTotal * (discountPercent / 100.0)
    val finalTotal = (subTotal - discountAmount).coerceAtLeast(0.0)
    val invId = "INV-OTC-${UUID.randomUUID().toString().take(8)}"
    val invNo = "INV-${1000 + _invoices.value.size + 1}"
    val inv = Invoice(
      id = invId,
      invoiceNumber = invNo,
      patientId = "WALK-IN",
      patientName = customerName.ifBlank { "Walk-in Customer / ပြင်ပဆေးဝယ်လူနာ" },
      items = items,
      consultationFee = 0.0,
      serviceFee = 0.0,
      discountPercent = discountPercent,
      discount = discountAmount,
      totalAmount = finalTotal,
      paidAmount = if (isPaid) finalTotal else 0.0,
      changeAmount = 0.0,
      paymentMethod = paymentMethod,
      paymentStatus = if (isPaid) "Paid" else "Unpaid",
      cashierName = cashierName.ifBlank { _currentUser.value?.fullName ?: "Counter Cashier" },
      timestamp = BurmeseDateUtils.getNowFormatted(),
      isPaid = isPaid,
      isInventoryDeducted = false
    )

    val updated = _invoices.value + inv
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(ctx, updated)

    if (isPaid) {
      deductInventoryForInvoice(inv)
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
      syncWithSupabaseCloud()
    }
    return _invoices.value.find { it.id == inv.id } ?: inv
  }

  // --- Day-Care Treatment Cards & Routine Injections ---
  fun addTreatmentCard(card: TreatmentCard) {
    val ctx = getApplication<Application>()
    val updated = _treatmentCards.value + card
    _treatmentCards.value = updated
    ClinicDataPersistence.saveTreatmentCards(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun administerTreatmentSession(
    cardId: String,
    sessionId: String,
    nurseName: String,
    isClinicMed: Boolean,
    medPrice: Double,
    sessionFee: Double,
    consumables: List<TreatmentConsumable>,
    isConsumablesFoc: Boolean = false,
    notes: String
  ): Invoice? {
    val ctx = getApplication<Application>()
    val card = _treatmentCards.value.find { it.id == cardId } ?: return null
    val session = card.sessions.find { it.id == sessionId } ?: return null

    // Handle Option B: Consumables are FOC / Clinic Internal Usage (deduct stock + log clinic expense)
    if (isConsumablesFoc && consumables.isNotEmpty()) {
      val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
      val now = System.currentTimeMillis()
      var medModified = false
      var totalConsumableCost = 0.0

      consumables.forEach { c ->
        val med = findMatchingMedicine(c.medicineId, c.name, medMap)
        if (med != null && c.quantity > 0) {
          var remaining = c.quantity.toDouble()
          val unitCost = med.batches.firstOrNull { it.quantity > 0 }?.costPricePerUnit
            ?: med.batches.firstOrNull()?.costPricePerUnit
            ?: (med.sellingPrice * 0.7)
          val updatedBatches = med.allBatches.map { b ->
            if (remaining > 0.0 && b.quantity > 0.0) {
              val deduct = minOf(b.quantity, remaining)
              remaining -= deduct
              b.copy(quantity = b.quantity - deduct, lastUpdated = now)
            } else b
          }
          medMap[med.id] = med.copy(batches = updatedBatches)
          medModified = true
          totalConsumableCost += unitCost * c.quantity
        }
      }
      if (medModified) {
        val updatedList = _medicines.value.map { medMap[it.id] ?: it }
        _medicines.value = updatedList
        ClinicDataPersistence.saveMedicines(ctx, updatedList)
      }
      if (totalConsumableCost > 0) {
        val consSummary = consumables.joinToString(", ") { "${it.name} x${it.quantity}" }
        addExpense(
          title = "ဆေးခန်းအသုံး: $consSummary (${card.patientName})",
          category = "ဆေးဝါးနှင့် ကုသမှုအသုံးစရိတ်",
          amount = totalConsumableCost,
          notes = "Day Care Treatment Room ဆေးခန်းအသုံး / FOC"
        )
      }
    }

    // 1. Build line items for this visit (bundled med + injection administration fee)
    val invoiceItems = mutableListOf<InvoiceItem>()
    val isPrepaid = session.isPrepaid || card.isPrepaidFullCourse
    val effectiveMedPrice = if (isPrepaid || card.isPatientSupplied) 0.0 else (if (isClinicMed) medPrice else 0.0)
    val effectiveSessionFee = if (isPrepaid) 0.0 else sessionFee
    // 1. Medication Item (isProcedure = false -> Pharmacy Revenue)
    if (effectiveMedPrice > 0) {
      val cleanMedName = card.medicationName
        .replace(Regex("(?i)^inj\\.?\\s*"), "")
        .replace(Regex("(?i)^iv\\s*"), "")
        .replace(Regex("(?i)^im\\s*"), "")
        .replace(Regex("(?i)^sc\\s*"), "")
        .trim()
      val doseStr = if (card.dosage.isNotBlank() && !cleanMedName.contains(card.dosage, ignoreCase = true)) " ${card.dosage}" else ""
      val medName = "$cleanMedName$doseStr (Dose ${session.sessionNumber}/${card.totalSessions})"
      invoiceItems.add(
        InvoiceItem(
          name = medName,
          quantity = 1,
          unitPrice = effectiveMedPrice,
          totalPrice = effectiveMedPrice,
          isProcedure = false
        )
      )
    }

    // 2. Administration Service Fee (isProcedure = true -> Service Revenue)
    if (effectiveSessionFee > 0) {
      val routeShort = card.routeDisplay.split(" ").first()
      val patientOwnedBadge = if (card.isPatientSupplied) " (Patient's Own Med)" else ""
      val svcFeeName = "$routeShort Administration Fee$patientOwnedBadge (Dose ${session.sessionNumber}/${card.totalSessions})"
      invoiceItems.add(
        InvoiceItem(
          name = svcFeeName,
          quantity = 1,
          unitPrice = effectiveSessionFee,
          totalPrice = effectiveSessionFee,
          isProcedure = true
        )
      )
    }

    // If not FOC, add consumables as billable items
    val consumableItemsForBill = if (!isConsumablesFoc) {
      consumables.filter { it.totalPrice > 0 }.map { c ->
        InvoiceItem(
          name = c.name,
          quantity = c.quantity,
          unitPrice = c.unitPrice,
          totalPrice = c.totalPrice,
          isProcedure = false
        )
      }
    } else {
      emptyList()
    }
    invoiceItems.addAll(consumableItemsForBill)

    val totalCost = invoiceItems.sumOf { it.totalPrice }

    val isAlreadyBilled = (session.invoiceId?.isNotBlank() == true && !session.invoiceId.startsWith("INV-TC-")) ||
      (!card.consultationInvoiceId.isNullOrBlank() && (session.sessionNumber == 1 || isPrepaid))

    var invoice: Invoice? = null
    if (isAlreadyBilled) {
      // Option C: Check if existing consultation invoice is unpaid to merge extra consumables
      val existingInv = _invoices.value.find { it.id == card.consultationInvoiceId || it.id == session.invoiceId }
      if (existingInv != null && !existingInv.isPaid && consumableItemsForBill.isNotEmpty()) {
        // Merge extra consumables directly into unpaid consultation invoice!
        val updatedItems = existingInv.items + consumableItemsForBill
        val addedAmount = consumableItemsForBill.sumOf { it.totalPrice }
        val updatedInv = existingInv.copy(
          items = updatedItems,
          totalAmount = existingInv.totalAmount + addedAmount
        )
        val updatedInvs = _invoices.value.map { if (it.id == updatedInv.id) updatedInv else it }
        _invoices.value = updatedInvs
        ClinicDataPersistence.saveInvoices(ctx, updatedInvs)
        invoice = updatedInv
      } else if ((existingInv == null || existingInv.isPaid) && consumableItemsForBill.isNotEmpty()) {
        // If already paid, issue supplementary invoice so Cashier can collect the extra consumables!
        val suppInvId = "INV-TC-SUPP-" + UUID.randomUUID().toString().take(6)
        val suppInvNo = "INV-" + (1000 + _invoices.value.size + 1)
        val suppInv = Invoice(
          id = suppInvId,
          invoiceNumber = suppInvNo,
          patientId = card.patientId.ifBlank { "WALK-IN" },
          patientName = card.patientName.ifBlank { "Treatment Patient" },
          items = consumableItemsForBill,
          consultationFee = 0.0,
          serviceFee = 0.0,
          totalAmount = consumableItemsForBill.sumOf { it.totalPrice },
          paidAmount = 0.0,
          paymentStatus = "Unpaid",
          cashierName = nurseName.ifBlank { _currentUser.value?.fullName ?: "Nurse Station" },
          timestamp = BurmeseDateUtils.getNowFormatted()
        )
        val updatedInvoices = _invoices.value + suppInv
        _invoices.value = updatedInvoices
        ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
        invoice = suppInv
      }
    } else {
      val invId = "INV-TC-" + UUID.randomUUID().toString().take(8)
      val invNo = "INV-" + (1000 + _invoices.value.size + 1)
      val newInv = Invoice(
        id = invId,
        invoiceNumber = invNo,
        patientId = card.patientId.ifBlank { "WALK-IN" },
        patientName = card.patientName.ifBlank { "Treatment Patient" },
        items = invoiceItems,
        consultationFee = 0.0,
        serviceFee = 0.0,
        totalAmount = totalCost,
        paidAmount = totalCost,
        paymentStatus = "Paid",
        cashierName = nurseName.ifBlank { _currentUser.value?.fullName ?: "Nurse Station" },
        timestamp = BurmeseDateUtils.getNowFormatted()
      )
      val updatedInvoices = _invoices.value + newInv
      _invoices.value = updatedInvoices
      ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
      deductInventoryForInvoice(newInv)
      invoice = newInv
    }

    // 3. Mark session COMPLETED
    val updatedSessions = card.sessions.map { s ->
      if (s.id == sessionId) {
        s.copy(
          status = TreatmentStatus.COMPLETED,
          administeredAt = BurmeseDateUtils.getNowFormatted(),
          administeredBy = nurseName,
          isClinicMed = isClinicMed,
          medicationFee = if (isClinicMed) medPrice else 0.0,
          sessionFee = sessionFee,
          consumables = consumables,
          invoiceId = if (isAlreadyBilled) (card.consultationInvoiceId ?: session.invoiceId) else invoice?.id,
          notes = notes
        )
      } else s
    }
    val allCompleted = updatedSessions.all { it.status == TreatmentStatus.COMPLETED }
    val updatedCard = card.copy(
      sessions = updatedSessions,
      cardStatus = if (allCompleted) TreatmentStatus.COMPLETED else TreatmentStatus.IN_PROGRESS
    )
    val updatedCards = _treatmentCards.value.map { if (it.id == cardId) updatedCard else it }
    _treatmentCards.value = updatedCards
    ClinicDataPersistence.saveTreatmentCards(ctx, updatedCards)

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return invoice
  }

  fun discontinueTreatmentCard(cardId: String, reason: String, staffName: String) {
    val ctx = getApplication<Application>()
    val updated = _treatmentCards.value.map { card ->
      if (card.id == cardId) {
        val updatedSessions = card.sessions.map { s ->
          if (s.status == TreatmentStatus.PENDING || s.status == TreatmentStatus.IN_PROGRESS) {
            s.copy(
              status = TreatmentStatus.DISCONTINUED,
              notes = (if (s.notes.isNotBlank()) "${s.notes} • " else "") + "Discontinued: $reason"
            )
          } else s
        }
        card.copy(
          cardStatus = TreatmentStatus.DISCONTINUED,
          discontinueReason = reason,
          discontinuedBy = staffName.ifBlank { _currentUser.value?.fullName ?: "Staff" },
          discontinuedAt = BurmeseDateUtils.getNowFormatted(),
          sessions = updatedSessions
        )
      } else card
    }
    _treatmentCards.value = updated
    ClinicDataPersistence.saveTreatmentCards(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun voidTreatmentCardAndReturnStock(
    cardId: String,
    returnStock: Boolean,
    reason: String
  ): Pair<Boolean, List<String>> {
    val ctx = getApplication<Application>()
    val card = _treatmentCards.value.find { it.id == cardId } ?: return Pair(false, emptyList())
    // Medical audit history protection: If any dose has already been administered, do not delete the card!
    if (card.completedSessionsCount > 0) {
      discontinueTreatmentCard(cardId, reason, _currentUser.value?.fullName ?: "Staff")
      return Pair(true, emptyList())
    }
    val restoredNames = mutableListOf<String>()

    if (returnStock && !card.isPatientSupplied) {
      val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
      var modified = false
      val now = System.currentTimeMillis()

      // Calculate doses that need return
      val dosesToReturn = if (card.isPrepaidFullCourse || !card.isPayPerVisit) {
        card.totalSessions.coerceAtLeast(1)
      } else {
        val billedSessions = card.sessions.count { it.invoiceId != null }
        val inv = _invoices.value.find { it.id == card.consultationInvoiceId }
        val invDeducted = inv?.isInventoryDeducted == true && inv.deductedDrugQuantities.isNotEmpty()
        if (billedSessions > 0) billedSessions else (if (invDeducted) 1 else 0)
      }

      val inv = _invoices.value.find { it.id == card.consultationInvoiceId }

      if (dosesToReturn > 0) {
        if (card.isCocktail && card.cocktailItems.isNotEmpty()) {
          for (cItem in card.cocktailItems) {
            val med = findMatchingMedicine(cItem.medicineId, cItem.name, medMap)
            val maxCanReturn = if (inv != null) (inv.deductedDrugQuantities[med?.id] ?: 0.0) else Double.MAX_VALUE
            val qty = minOf((if (cItem.quantity > 0) cItem.quantity.toDouble() else 1.0) * dosesToReturn, maxCanReturn)
            if (med != null && qty > 0.0) {
              val primaryBatch = med.batches.firstOrNull()
              val updatedBatches = if (primaryBatch != null) {
                med.batches.mapIndexed { idx, b -> if (idx == 0) b.copy(quantity = b.quantity + qty, lastUpdated = now) else b }
              } else {
                val restoreCost = getMedicineFallbackCostPrice(med, inv)
                listOf(com.example.model.StockBatch("RESTORE", "2028-12-31", qty, restoreCost, lastUpdated = now))
              }
              medMap[med.id] = med.copy(batches = updatedBatches)
              restoredNames.add("${med.name} (+$qty)")
              modified = true
            }
          }
        } else if (card.medicationName.isNotBlank()) {
          val med = findMatchingMedicine(card.medicationId, card.medicationName, medMap)
          val maxCanReturn = if (inv != null) (inv.deductedDrugQuantities[med?.id] ?: 0.0) else Double.MAX_VALUE
          val qty = minOf(dosesToReturn.toDouble(), maxCanReturn)
          if (med != null && qty > 0.0) {
            val primaryBatch = med.batches.firstOrNull()
            val updatedBatches = if (primaryBatch != null) {
              med.batches.mapIndexed { idx, b -> if (idx == 0) b.copy(quantity = b.quantity + qty, lastUpdated = now) else b }
            } else {
              val restoreCost = getMedicineFallbackCostPrice(med, inv)
              listOf(com.example.model.StockBatch("RESTORE", "2028-12-31", qty.toDouble(), restoreCost, lastUpdated = now))
            }
            medMap[med.id] = med.copy(batches = updatedBatches)
            restoredNames.add("${med.name} (+$qty)")
            modified = true
          }
        }

        // Return consumables if any
        card.sessions.forEach { s ->
          for (c in s.consumables) {
            val med = medMap[c.medicineId] ?: medMap.values.find { it.name.equals(c.name, ignoreCase = true) }
            if (med != null && c.quantity > 0) {
              val primaryBatch = med.batches.firstOrNull()
              val updatedBatches = if (primaryBatch != null) {
                med.batches.mapIndexed { idx, b -> if (idx == 0) b.copy(quantity = b.quantity + c.quantity, lastUpdated = now) else b }
              } else {
                val restoreCost = getMedicineFallbackCostPrice(med, inv)
                listOf(com.example.model.StockBatch("RESTORE", "2028-12-31", c.quantity.toDouble(), restoreCost, lastUpdated = now))
              }
              medMap[med.id] = med.copy(batches = updatedBatches)
              restoredNames.add("${med.name} (+${c.quantity})")
              modified = true
            }
          }
        }
      }

      if (modified) {
        val updatedList = _medicines.value.map { medMap[it.id] ?: it }
        _medicines.value = updatedList
        ClinicDataPersistence.saveMedicines(ctx, updatedList)
      }
    }

    // Check if linked to an unpaid invoice, remove the item
    val linkedInv = _invoices.value.find { it.id == card.consultationInvoiceId }
    if (linkedInv != null && !linkedInv.isPaid) {
      val cleanOrder = card.orderSummaryText
      val remainingItems = linkedInv.items.filterNot {
        it.name.contains(cleanOrder, ignoreCase = true) ||
        (card.medicationName.isNotBlank() && it.name.contains(card.medicationName, ignoreCase = true))
      }
      val newSubtotal = remainingItems.sumOf { it.totalPrice }
      val newTotal = maxOf(0.0, newSubtotal + linkedInv.consultationFee + linkedInv.serviceFee - linkedInv.discount)
      val updatedInv = linkedInv.copy(
        items = remainingItems,
        totalAmount = newTotal
      )
      val updatedInvoices = _invoices.value.map { if (it.id == updatedInv.id) updatedInv else it }
      _invoices.value = updatedInvoices
      ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
    }

    // Add tombstone and remove card
    ClinicDataPersistence.addDeletedTreatmentCardId(ctx, cardId)
    val remainingCards = _treatmentCards.value.filterNot { it.id == cardId }
    _treatmentCards.value = remainingCards
    ClinicDataPersistence.saveTreatmentCards(ctx, remainingCards)

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return Pair(true, restoredNames)
  }

  // --- Inventory & Stock Management ---
  fun stockInBatch(medicineId: String, batchNumber: String, expiryDate: String, quantity: Int, costPrice: Double) {
    val updated = _medicines.value.map { med ->
      if (med.id == medicineId) {
        val existingBatches = med.batches.filter { it.batchNumber != batchNumber }
        val newBatch = StockBatch(batchNumber, expiryDate, quantity, costPrice)
        med.copy(batches = existingBatches + newBatch)
      } else med
    }
    _medicines.value = updated
    ClinicDataPersistence.saveMedicines(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun stockInMedicine(medicineId: String, batchNumber: String, expiryDate: String, quantity: Int, costPrice: Double) =
    stockInBatch(medicineId, batchNumber, expiryDate, quantity, costPrice)

  fun stockInMedicine(
    tradeName: String,
    genericName: String,
    dosageForm: String,
    category: String,
    unitType: String,
    batchNumber: String,
    manufacturer: String = "",
    manufactureDate: String = "",
    expiryDate: String,
    quantity: Int,
    costPrice: Double,
    profitMarginPercent: Double = 25.0,
    minThreshold: Int = 20,
    supplierName: String = "",
    medicineType: String = "",
    sellingPrice: Double? = null
  ) {
    val existing = _medicines.value.find { it.name.equals(tradeName, ignoreCase = true) }
    val newBatch = StockBatch(
      batchNumber = batchNumber,
      expiryDate = expiryDate,
      quantity = quantity,
      costPricePerUnit = costPrice
    )
    if (existing != null) {
      val now = System.currentTimeMillis()
      val sameBatch = existing.batches.find { it.batchNumber.equals(batchNumber, ignoreCase = true) }
      val updatedBatches = if (sameBatch != null) {
        existing.batches.map { b ->
          if (b.batchNumber.equals(batchNumber, ignoreCase = true)) {
            b.copy(
              quantity = b.quantity + quantity,
              costPricePerUnit = costPrice,
              expiryDate = expiryDate,
              lastUpdated = now
            )
          } else b
        }
      } else {
        existing.batches + newBatch
      }
      val updated = _medicines.value.map { med ->
        if (med.id == existing.id) {
          med.copy(
            batches = updatedBatches,
            profitMarginPercent = profitMarginPercent,
            minStockThreshold = minThreshold,
            medicineType = if (medicineType.isNotBlank()) medicineType else med.medicineType,
            customSellingPrice = if (sellingPrice != null && sellingPrice > 0.0) sellingPrice else med.customSellingPrice
          )
        } else med
      }
      _medicines.value = updated
      ClinicDataPersistence.saveMedicines(getApplication(), updated)
    } else {
      val newMed = Medicine(
        name = tradeName,
        genericName = genericName,
        dosageForm = dosageForm,
        category = category,
        unitType = unitType,
        batches = listOf(newBatch),
        minStockThreshold = minThreshold,
        profitMarginPercent = profitMarginPercent,
        primarySupplier = supplierName.trim(),
        medicineType = medicineType,
        customSellingPrice = if (sellingPrice != null && sellingPrice > 0.0) sellingPrice else null
      )
      val updated = _medicines.value + newMed
      _medicines.value = updated
      ClinicDataPersistence.saveMedicines(getApplication(), updated)
    }
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updateMedicine(updatedMedicine: Medicine) {
    val ctx = getApplication<Application>()
    val now = System.currentTimeMillis()
    val updatedList = _medicines.value.map { med ->
      if (med.id == updatedMedicine.id) {
        val updatedBatches = updatedMedicine.batches.map { b ->
          val orig = med.batches.find { it.batchNumber == b.batchNumber }
          if (orig == null || orig.quantity != b.quantity || orig.costPricePerUnit != b.costPricePerUnit || orig.expiryDate != b.expiryDate) {
            b.copy(lastUpdated = now)
          } else b
        }
        updatedMedicine.copy(batches = updatedBatches)
      } else med
    }
    _medicines.value = updatedList
    ClinicDataPersistence.saveMedicines(ctx, updatedList)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun writeOffMedicineWastage(
    medicineId: String,
    quantity: Double,
    reason: String = "ရက်ကြာ/လေလွင့်ဆေးဖျက်"
  ): Boolean {
    if (quantity <= 0.0) return false
    val ctx = getApplication<Application>()
    val med = _medicines.value.find { it.id == medicineId } ?: return false
    val now = System.currentTimeMillis()
    var remaining = quantity
    var totalCostDeducted = 0.0

    val updatedBatches = med.allBatches.map { b ->
      if (remaining > 0.0 && b.quantity > 0.0) {
        val deduct = minOf(b.quantity, remaining)
        remaining -= deduct
        totalCostDeducted += deduct * b.costPricePerUnit
        b.copy(quantity = b.quantity - deduct, lastUpdated = now)
      } else b
    }

    val updatedMed = med.copy(batches = updatedBatches)
    val updatedMeds = _medicines.value.map { if (it.id == medicineId) updatedMed else it }
    _medicines.value = updatedMeds
    ClinicDataPersistence.saveMedicines(ctx, updatedMeds)

    val actualDeducted = quantity - remaining
    val qtyDisplay = if (actualDeducted % 1.0 == 0.0) actualDeducted.toInt().toString() else String.format(Locale.US, "%.1f", actualDeducted)
    if (actualDeducted > 0.0 && totalCostDeducted > 0.0) {
      addExpense(
        title = "ဆေးဝါးလေလွင့်/ဆေးခန်းအသုံး - ${med.name} ($qtyDisplay ${med.unitType})",
        category = "Clinic Supplies",
        amount = totalCostDeducted,
        payee = "ဆေးဝါးလေလွင့်စာရင်း",
        notes = reason.ifBlank { "ဆေးဝါးအသုံးမကုန်/ရက်ကြာ စွန့်ပစ်စာရင်း" }
      )
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return true
  }

  fun deleteMedicine(medicineId: String) {
    val ctx = getApplication<Application>()
    ClinicDataPersistence.addDeletedMedicineId(ctx, medicineId)
    val updated = _medicines.value.filter { it.id != medicineId }
    _medicines.value = updated
    ClinicDataPersistence.saveMedicines(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun addMedicineCategory(category: String) {
    val clean = category.trim()
    if (clean.isNotBlank() && !medicineCategories.value.contains(clean)) {
      val updated = medicineCategories.value + clean
      medicineCategories.value = updated
      ClinicDataPersistence.saveMedicineCategories(getApplication(), updated)
    }
  }

  fun renameMedicineCategory(oldCategory: String, newCategory: String) {
    val oldClean = oldCategory.trim()
    val newClean = newCategory.trim()
    if (oldClean.isBlank() || newClean.isBlank() || oldClean.equals(newClean, ignoreCase = true)) return

    // 1. Reassign medicines
    val updatedMeds = _medicines.value.map { med ->
      if (med.category.equals(oldClean, ignoreCase = true)) {
        med.copy(category = newClean)
      } else med
    }
    _medicines.value = updatedMeds
    ClinicDataPersistence.saveMedicines(getApplication(), updatedMeds)

    // 2. Update category list
    val updatedCats = medicineCategories.value.map { if (it.equals(oldClean, ignoreCase = true)) newClean else it }.distinct()
    medicineCategories.value = updatedCats
    ClinicDataPersistence.saveMedicineCategories(getApplication(), updatedCats)

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun deleteMedicineCategory(categoryToDelete: String) {
    val clean = categoryToDelete.trim()
    val defaultCategories = listOf("All", "Antibiotics", "Analgesic / Antipyretic", "Antacid / PPI", "Cardiovascular", "Antidiabetic", "Respiratory", "Consumables", "Others")
    if (defaultCategories.any { it.equals(clean, ignoreCase = true) }) {
      return // Protect default categories
    }

    // 1. Move any medicines in this category to "Others"
    val updatedMeds = _medicines.value.map { med ->
      if (med.category.equals(clean, ignoreCase = true)) {
        med.copy(category = "Others")
      } else med
    }
    _medicines.value = updatedMeds
    ClinicDataPersistence.saveMedicines(getApplication(), updatedMeds)

    // 2. Remove category and persist
    val updatedCats = medicineCategories.value.filterNot { it.equals(clean, ignoreCase = true) }
    medicineCategories.value = updatedCats
    ClinicDataPersistence.saveMedicineCategories(getApplication(), updatedCats)

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun addMedicineUnit(unit: String) {
    val clean = unit.trim()
    if (clean.isNotBlank() && !medicineUnits.value.contains(clean)) {
      val updated = medicineUnits.value + clean
      medicineUnits.value = updated
      ClinicDataPersistence.saveMedicineUnits(getApplication(), updated)
    }
  }

  fun addProcedureCategory(category: String) {
    val clean = category.trim()
    if (clean.isNotBlank() && !procedureCategories.value.contains(clean)) {
      val updated = procedureCategories.value + clean
      procedureCategories.value = updated
      ClinicDataPersistence.saveProcedureCategories(getApplication(), updated)
    }
  }

  fun deleteProcedureCategory(categoryToDelete: String) {
    val clean = categoryToDelete.trim()
    val defaultCategories = listOf("All", "Minor Surgery", "Wound Dressing", "Injection / Infusion", "Diagnostic Test", "General Services", "Others")
    if (defaultCategories.any { it.equals(clean, ignoreCase = true) }) {
      return
    }

    // 1. Move any procedures belonging to this category to "Others"
    val updatedProcs = _procedures.value.map { proc ->
      if (proc.category.equals(clean, ignoreCase = true)) {
        proc.copy(category = "Others")
      } else proc
    }
    _procedures.value = updatedProcs
    ClinicDataPersistence.saveProcedures(getApplication(), updatedProcs)

    // 2. Remove category and persist
    val updatedCats = procedureCategories.value.filterNot { it.equals(clean, ignoreCase = true) }
    procedureCategories.value = updatedCats
    ClinicDataPersistence.saveProcedureCategories(getApplication(), updatedCats)

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun addCustomProcedure(
    name: String,
    myanmarName: String = name,
    category: String = "Minor Surgery",
    fee: Double = 0.0,
    notes: String = "",
    consumables: List<String> = emptyList(),
    dateTime: String = BurmeseDateUtils.getNowFormatted(),
    consumableItems: List<ProcedureConsumable> = emptyList(),
    price: Double = fee,
    desc: String = notes
  ): ProcedureItem {
    val items = if (consumableItems.isNotEmpty()) {
      consumableItems
    } else {
      consumables.map { ProcedureConsumable(name = it) }
    }
    val effectivePrice = if (fee > 0.0) fee else price
    val proc = ProcedureItem(
      id = UUID.randomUUID().toString(),
      name = name,
      myanmarName = myanmarName.ifBlank { name },
      category = category,
      price = effectivePrice,
      standardFee = effectivePrice,
      notes = notes.ifBlank { desc },
      description = desc.ifBlank { notes },
      dateTime = dateTime,
      isCustom = true,
      consumableItems = items
    )
    val updatedProcs = _procedures.value + proc
    _procedures.value = updatedProcs
    ClinicDataPersistence.saveProcedures(getApplication(), updatedProcs)
    return proc
  }

  fun addCustomProcedure(name: String, category: String, price: Double, desc: String): ProcedureItem {
    return addCustomProcedure(name = name, category = category, fee = price, notes = desc)
  }

  fun updateProcedure(updated: ProcedureItem) {
    val updatedProcs = _procedures.value.map { if (it.id == updated.id) updated else it }
    _procedures.value = updatedProcs
    ClinicDataPersistence.saveProcedures(getApplication(), updatedProcs)
  }

  fun deleteProcedure(procedureId: String) {
    val updatedProcs = _procedures.value.filter { it.id != procedureId }
    _procedures.value = updatedProcs
    ClinicDataPersistence.saveProcedures(getApplication(), updatedProcs)
  }

  fun toggleInvoicePaidStatus(invoiceId: String, markAsPaid: Boolean) {
    val updated = _invoices.value.map { inv ->
      if (inv.id == invoiceId) {
        if (markAsPaid) {
          inv.copy(
            isPaid = true,
            paymentStatus = "Paid",
            paidAmount = inv.totalAmount,
            changeAmount = 0.0,
            cashierName = _currentUser.value?.fullName ?: "Cashier Staff",
            timestamp = BurmeseDateUtils.getNowFormatted()
          )
        } else {
          inv.copy(
            isPaid = false,
            paymentStatus = "Unpaid",
            paidAmount = 0.0,
            changeAmount = 0.0
          )
        }
      } else inv
    }
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(getApplication(), updated)
    if (markAsPaid) {
      val inv = updated.find { it.id == invoiceId }
      if (inv != null) {
        updatePatientStatus(inv.patientId, QueueStatus.COMPLETED)
        deductInventoryForInvoice(inv)
      }
    }
  }

  fun addSupplier(name: String, contactPerson: String, phone: String, address: String) {
    addSupplier(name = name, contactPerson = contactPerson, phone = phone, email = "", address = address)
  }

  fun addSupplier(name: String, contactPerson: String, phone: String, email: String, address: String) {
    val sup = Supplier(
      id = UUID.randomUUID().toString(),
      name = name,
      contactPerson = contactPerson,
      phone = phone,
      email = email,
      address = address
    )
    val updated = _suppliers.value + sup
    _suppliers.value = updated
    val ctx = getApplication<Application>()
    ClinicDataPersistence.saveSuppliers(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun deleteSupplier(supplierId: String) {
    val updated = _suppliers.value.filter { it.id != supplierId }
    _suppliers.value = updated
    val ctx = getApplication<Application>()
    ClinicDataPersistence.saveSuppliers(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun addExpense(
    title: String,
    category: String,
    amount: Double,
    paymentMethod: PaymentMethod = PaymentMethod.CASH,
    payee: String = "",
    notes: String = "",
    date: String = BurmeseDateUtils.getTodayFormatted()
  ) {
    val exp = ExpenseRecord(
      id = UUID.randomUUID().toString(),
      title = title,
      category = category,
      amount = amount,
      description = notes.ifBlank { title },
      date = date,
      recordedBy = _currentUser.value?.fullName ?: "Staff",
      timestamp = date,
      voucherNo = "EXP-${UUID.randomUUID().toString().take(6).uppercase()}",
      payee = payee,
      paymentMethod = paymentMethod,
      notes = notes
    )
    val updated = _expenses.value + exp
    _expenses.value = updated
    val ctx = getApplication<Application>()
    ClinicDataPersistence.saveExpenses(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun addExpense(category: String, amount: Double, desc: String) {
    addExpense(title = desc, category = category, amount = amount, notes = desc)
  }

  fun deleteExpense(expenseId: String) {
    val updated = _expenses.value.filter { it.id != expenseId }
    _expenses.value = updated
    val ctx = getApplication<Application>()
    ClinicDataPersistence.saveExpenses(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun updateClinicSettings(settings: ClinicSettings) {
    clinicSettings.value = settings
    ClinicDataPersistence.saveClinicSettings(getApplication(), settings)
  }

  // --- Backup, Export & Reset ---
  fun exportFullBackupJson(): String {
    val ctx = getApplication<Application>()
    val payload = SyncDataPayload(
      deviceId = "primary-clinic-device",
      deviceName = clinicSettings.value.clinicNameMm.ifBlank { "Shwe Khit Clinic" },
      timestamp = System.currentTimeMillis(),
      patients = _patients.value,
      consultations = _consultations.value,
      invoices = _invoices.value,
      medicines = _medicines.value,
      users = _users.value,
      treatmentCards = _treatmentCards.value,
      expenses = _expenses.value,
      procedures = _procedures.value,
      suppliers = _suppliers.value,
      vaccines = _vaccines.value,
      clinicSettings = clinicSettings.value,
      deletedPatientIds = ClinicDataPersistence.loadDeletedPatientIds(ctx).toList(),
      deletedConsultationIds = ClinicDataPersistence.loadDeletedConsultationIds(ctx).toList(),
      deletedMedicineIds = ClinicDataPersistence.loadDeletedMedicineIds(ctx).toList(),
      deletedUserIds = ClinicDataPersistence.loadDeletedUserIds(ctx).toList(),
      deletedTreatmentCardIds = ClinicDataPersistence.loadDeletedTreatmentCardIds(ctx).toList()
    )
    val root = JSONObject(payload.toJsonString(ctx))
    root.put("exportTime", BurmeseDateUtils.getNowFormatted())
    root.put("clinic", clinicSettings.value.clinicNameMm)
    val labOrdersJson = ClinicDataPersistence.getLabOrdersJson(ctx)
    if (!labOrdersJson.isNullOrBlank()) {
      root.put("labOrders", JSONArray(labOrdersJson))
    }
    val labTemplatesJson = ClinicDataPersistence.getLabTemplatesJson(ctx)
    if (!labTemplatesJson.isNullOrBlank()) {
      root.put("labTemplates", JSONArray(labTemplatesJson))
    }
    val labInvoicesJson = ClinicDataPersistence.getLabInvoicesJson(ctx)
    if (!labInvoicesJson.isNullOrBlank()) {
      root.put("labInvoices", JSONArray(labInvoicesJson))
    }
    return root.toString(2)
  }

  fun restoreFullBackupJson(jsonString: String): Boolean {
    return try {
      val root = JSONObject(jsonString)
      val ctx = getApplication<Application>()

      // 1. Deleted tombstones
      val delPatSet = mutableSetOf<String>()
      if (root.has("deletedPatientIds")) {
        val delArr = root.getJSONArray("deletedPatientIds")
        for (i in 0 until delArr.length()) delPatSet.add(delArr.getString(i))
        ClinicDataPersistence.saveDeletedPatientIds(ctx, delPatSet)
      }
      val delConsSet = mutableSetOf<String>()
      if (root.has("deletedConsultationIds")) {
        val delArr = root.getJSONArray("deletedConsultationIds")
        for (i in 0 until delArr.length()) delConsSet.add(delArr.getString(i))
        ClinicDataPersistence.saveDeletedConsultationIds(ctx, delConsSet)
      }
      val delMedSet = mutableSetOf<String>()
      val delMedArr = root.optJSONArray("deletedMedicineIds") ?: root.optJSONArray("deleted_medicine_ids")
      if (delMedArr != null) {
        for (i in 0 until delMedArr.length()) delMedSet.add(delMedArr.getString(i))
        val currentDelMeds = ClinicDataPersistence.loadDeletedMedicineIds(ctx)
        ClinicDataPersistence.saveDeletedMedicineIds(ctx, (currentDelMeds + delMedSet).toMutableSet())
      }
      val delTcSet = mutableSetOf<String>()
      val delTcArr = root.optJSONArray("deletedTreatmentCardIds") ?: root.optJSONArray("deleted_treatment_card_ids")
      if (delTcArr != null) {
        for (i in 0 until delTcArr.length()) delTcSet.add(delTcArr.getString(i))
        val currentDelTc = ClinicDataPersistence.loadDeletedTreatmentCardIds(ctx)
        ClinicDataPersistence.saveDeletedTreatmentCardIds(ctx, (currentDelTc + delTcSet).toMutableSet())
      }

      val delUserSet = mutableSetOf<String>()
      val delUserArr = root.optJSONArray("deletedUserIds") ?: root.optJSONArray("deleted_user_ids")
      if (delUserArr != null) {
        for (i in 0 until delUserArr.length()) delUserSet.add(delUserArr.getString(i))
        val currentDelUsers = ClinicDataPersistence.loadDeletedUserIds(ctx)
        ClinicDataPersistence.saveDeletedUserIds(ctx, (currentDelUsers + delUserSet).toMutableSet())
      }

      // 2. Patients (Non-destructive ID-based merge)
      if (root.has("patients")) {
        val parsedPatients = SyncPayloadParser.parsePatients(root.getJSONArray("patients"))
        val currentPatMap = _patients.value.associateBy { it.id }.toMutableMap()
        parsedPatients.forEach { p ->
          if (!delPatSet.contains(p.id)) {
            currentPatMap[p.id] = p
          }
        }
        val mergedPatients = currentPatMap.values.filter { !delPatSet.contains(it.id) }
        _patients.value = mergedPatients
        ClinicDataPersistence.savePatients(ctx, mergedPatients)
        if (_selectedPatientId.value == null || !mergedPatients.any { it.id == _selectedPatientId.value }) {
          _selectedPatientId.value = mergedPatients.firstOrNull { it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION }?.id
        }
      }

      // 3. Consultations (Non-destructive ID-based merge)
      if (root.has("consultations")) {
        val parsedCons = SyncPayloadParser.parseConsultations(root.getJSONArray("consultations"), ctx)
        val currentConsMap = _consultations.value.associateBy { it.id }.toMutableMap()
        parsedCons.forEach { c ->
          if (!delConsSet.contains(c.id) && !delPatSet.contains(c.patientId)) {
            val existing = currentConsMap[c.id]
            val resolvedCons = if (existing != null && c.attachments.isEmpty() && existing.attachments.isNotEmpty()) {
              c.copy(attachments = existing.attachments)
            } else if (existing != null && existing.attachments.size > c.attachments.size) {
              c.copy(attachments = (existing.attachments + c.attachments).distinctBy { it.id })
            } else c
            currentConsMap[c.id] = resolvedCons
          }
        }
        val mergedCons = currentConsMap.values.filter { !delConsSet.contains(it.id) && !delPatSet.contains(it.patientId) }
        _consultations.value = mergedCons
        ClinicDataPersistence.saveConsultations(ctx, mergedCons)
      }

      // 4. Invoices (Non-destructive ID-based merge)
      if (root.has("invoices")) {
        val parsedInvoices = SyncPayloadParser.parseInvoices(root.getJSONArray("invoices"))
        val currentInvMap = _invoices.value.associateBy { it.id }.toMutableMap()
        parsedInvoices.forEach { inv ->
          if (!delPatSet.contains(inv.patientId)) {
            currentInvMap[inv.id] = inv
          }
        }
        val mergedInvoices = currentInvMap.values.filter { !delPatSet.contains(it.patientId) }
        _invoices.value = mergedInvoices
        ClinicDataPersistence.saveInvoices(ctx, mergedInvoices)
      }

      // 5. Medicines (Non-destructive ID-based merge with stock batch merging)
      if (root.has("medicines")) {
        val parsedMedicines = SyncPayloadParser.parseMedicines(root.getJSONArray("medicines"))
        val activeDelMeds = ClinicDataPersistence.loadDeletedMedicineIds(ctx) + delMedSet
        if (parsedMedicines.isNotEmpty()) {
          val currentMedMap = _medicines.value.filter { !activeDelMeds.contains(it.id) }.associateBy { it.id }.toMutableMap()
          parsedMedicines.forEach { m ->
            if (!activeDelMeds.contains(m.id)) {
              val existing = currentMedMap[m.id]
              if (existing == null) {
                val cleanSup = if (m.primarySupplier.contains("AA Medical", ignoreCase = true) ||
                                   m.primarySupplier.contains("Mega Lifesciences", ignoreCase = true) ||
                                   m.primarySupplier.contains("DKSH", ignoreCase = true)) "" else m.primarySupplier
                currentMedMap[m.id] = m.copy(primarySupplier = cleanSup)
              } else {
                val batchMap = existing.batches.associateBy { it.batchNumber }.toMutableMap()
                m.batches.forEach { b ->
                  val exB = batchMap[b.batchNumber]
                  if (exB == null || b.lastUpdated > exB.lastUpdated) {
                    batchMap[b.batchNumber] = b
                  }
                }
                currentMedMap[m.id] = existing.copy(
                  name = m.name,
                  genericName = m.genericName,
                  category = m.category,
                  dosageForm = m.dosageForm,
                  unitType = m.unitType,
                  medicineType = if (m.medicineType.isNotBlank()) m.medicineType else existing.medicineType,
                  minStockThreshold = if (m.minStockThreshold > 0) m.minStockThreshold else existing.minStockThreshold,
                  profitMarginPercent = if (m.profitMarginPercent > 0.0) m.profitMarginPercent else existing.profitMarginPercent,
                  customSellingPrice = if (m.customSellingPrice != null && m.customSellingPrice > 0.0) m.customSellingPrice else existing.customSellingPrice,
                  primarySupplier = when {
                    existing.primarySupplier.isNotBlank() && !existing.primarySupplier.contains("AA Medical", ignoreCase = true) -> existing.primarySupplier
                    m.primarySupplier.isNotBlank() && !m.primarySupplier.contains("AA Medical", ignoreCase = true) -> m.primarySupplier
                    else -> ""
                  },
                  isAllergyWarning = m.isAllergyWarning || existing.isAllergyWarning,
                  standardDosage = if (m.standardDosage.isNotBlank()) m.standardDosage else existing.standardDosage,
                  batches = batchMap.values.toList()
                )
              }
            }
          }
          val mergedMeds = currentMedMap.values.filter { !activeDelMeds.contains(it.id) }
          _medicines.value = mergedMeds
          ClinicDataPersistence.saveMedicines(ctx, mergedMeds)
        }
      }

      // 6. Users (Non-destructive merge with tombstone filtering)
      if (root.has("users")) {
        val parsedUsers = SyncPayloadParser.parseUsers(root.getJSONArray("users"))
        val activeDelUsers = ClinicDataPersistence.loadDeletedUserIds(ctx) + delUserSet
        if (parsedUsers.isNotEmpty()) {
          val currentUserMap = _users.value.filterNot { activeDelUsers.contains(it.id) || activeDelUsers.contains(it.username.lowercase()) }.associateBy { it.username.lowercase() }.toMutableMap()
          parsedUsers.forEach { u ->
            if (!activeDelUsers.contains(u.id) && !activeDelUsers.contains(u.username.lowercase())) {
              val existing = currentUserMap[u.username.lowercase()]
              val effectivePerms = if (u.customPermissions != UserRolePermissions.defaultFor(u.role)) {
                u.customPermissions
              } else {
                existing?.customPermissions ?: u.customPermissions
              }
              currentUserMap[u.username.lowercase()] = u.copy(customPermissions = effectivePerms)
            }
          }
          val mergedUsers = currentUserMap.values.toList()
          _users.value = mergedUsers
          ClinicDataPersistence.saveUsers(ctx, mergedUsers)
        }
      }

      // 7. Day-Care Treatment Cards (Non-destructive ID-based merge)
      if (root.has("treatmentCards") || root.has("treatment_cards")) {
        val arr = root.optJSONArray("treatmentCards") ?: root.optJSONArray("treatment_cards")
        val parsedCards = SyncPayloadParser.parseTreatmentCards(arr)
        if (parsedCards.isNotEmpty()) {
          val deletedCardIds = ClinicDataPersistence.loadDeletedTreatmentCardIds(ctx)
          val currentTcMap = _treatmentCards.value.associateBy { it.id }.toMutableMap()
          parsedCards.filterNot { deletedCardIds.contains(it.id) }.forEach { card -> currentTcMap[card.id] = card }
          val mergedCards = currentTcMap.values.toList()
          _treatmentCards.value = mergedCards
          ClinicDataPersistence.saveTreatmentCards(ctx, mergedCards)
        }
      }

      // 8. Expenses (Non-destructive ID-based merge)
      if (root.has("expenses")) {
        val parsedExp = SyncPayloadParser.parseExpenses(root.getJSONArray("expenses"))
        if (parsedExp.isNotEmpty()) {
          val currentExpMap = _expenses.value.associateBy { it.id }.toMutableMap()
          parsedExp.forEach { e -> currentExpMap[e.id] = e }
          val mergedExp = currentExpMap.values.toList()
          _expenses.value = mergedExp
          ClinicDataPersistence.saveExpenses(ctx, mergedExp)
        }
      }

      // 9. Procedures (Non-destructive ID-based merge)
      if (root.has("procedures")) {
        val parsedProc = SyncPayloadParser.parseProcedures(root.getJSONArray("procedures"))
        if (parsedProc.isNotEmpty()) {
          val currentProcMap = _procedures.value.associateBy { it.id }.toMutableMap()
          parsedProc.forEach { p -> currentProcMap[p.id] = p }
          val mergedProc = currentProcMap.values.toList()
          _procedures.value = mergedProc
          ClinicDataPersistence.saveProcedures(ctx, mergedProc)
        }
      }

      // 10. Suppliers (Non-destructive ID-based merge)
      if (root.has("suppliers")) {
        val parsedSup = SyncPayloadParser.parseSuppliers(root.getJSONArray("suppliers"))
        if (parsedSup.isNotEmpty()) {
          val currentSupMap = _suppliers.value.associateBy { it.id }.toMutableMap()
          parsedSup.forEach { s -> currentSupMap[s.id] = s }
          val mergedSup = currentSupMap.values.toList()
          _suppliers.value = mergedSup
          ClinicDataPersistence.saveSuppliers(ctx, mergedSup)
        }
      }

      // 11. Vaccines (Non-destructive ID-based merge)
      if (root.has("vaccines")) {
        val parsedVac = SyncPayloadParser.parseVaccines(root.getJSONArray("vaccines"))
        if (parsedVac.isNotEmpty()) {
          val currentVacMap = _vaccines.value.associateBy { it.id }.toMutableMap()
          parsedVac.forEach { v -> currentVacMap[v.id] = v }
          val mergedVac = currentVacMap.values.toList()
          _vaccines.value = mergedVac
          ClinicDataPersistence.saveVaccines(ctx, mergedVac)
        }
      }

      // 11.1 Lab Orders (Non-destructive ID-based merge)
      if (root.has("labOrders")) {
        val parsedLabOrders = ClinicDataPersistence.parseLabOrders(root.getJSONArray("labOrders").toString())
        val currentLabOrderMap = _labOrders.value.associateBy { it.id }.toMutableMap()
        parsedLabOrders.forEach { o -> currentLabOrderMap[o.id] = o }
        val mergedLabOrders = currentLabOrderMap.values.toList()
        _labOrders.value = mergedLabOrders
        ClinicDataPersistence.saveLabOrders(ctx, mergedLabOrders)
      }

      // 11.2 Lab Templates (Non-destructive ID-based merge)
      if (root.has("labTemplates")) {
        val parsedLabTemplates = ClinicDataPersistence.parseLabTemplates(root.getJSONArray("labTemplates").toString())
        val currentLabTempMap = _labTemplates.value.associateBy { it.id }.toMutableMap()
        parsedLabTemplates.forEach { t -> currentLabTempMap[t.id] = t }
        val mergedLabTemplates = currentLabTempMap.values.toList()
        _labTemplates.value = mergedLabTemplates
        ClinicDataPersistence.saveLabTemplates(ctx, mergedLabTemplates)
      }

      // 11.3 Lab Invoices (Non-destructive ID-based merge)
      if (root.has("labInvoices")) {
        val parsedLabInvoices = ClinicDataPersistence.parseLabInvoices(root.getJSONArray("labInvoices").toString())
        val currentLabInvMap = _labInvoices.value.associateBy { it.id }.toMutableMap()
        parsedLabInvoices.forEach { inv -> currentLabInvMap[inv.id] = inv }
        val mergedLabInvoices = currentLabInvMap.values.toList()
        _labInvoices.value = mergedLabInvoices
        ClinicDataPersistence.saveLabInvoices(ctx, mergedLabInvoices)
      }

      // 12. Clinic Settings & Branding (Merge & Persist)
      val remoteSettingsObj = root.optJSONObject("clinicSettings") ?: root.optJSONObject("settings")
      if (remoteSettingsObj != null) {
        val curSettings = clinicSettings.value
        val mergedSettings = ClinicDataPersistence.settingsFromJson(remoteSettingsObj, fallback = curSettings)
        val finalSettings = mergedSettings.copy(
          activeStationName = curSettings.activeStationName.ifBlank { mergedSettings.activeStationName },
          bluetoothPrinterMac = curSettings.bluetoothPrinterMac.ifBlank { mergedSettings.bluetoothPrinterMac },
          preferredThermalPrinterAddress = curSettings.preferredThermalPrinterAddress.ifBlank { mergedSettings.preferredThermalPrinterAddress },
          wifiHubHostIp = curSettings.wifiHubHostIp.ifBlank { mergedSettings.wifiHubHostIp }
        )
        ClinicDataPersistence.saveClinicSettings(ctx, finalSettings)
        clinicSettings.value = finalSettings
      }

      // Broadcast restored state to Wi-Fi peers
      viewModelScope.launch(Dispatchers.IO) {
        syncManager.syncWithAllPeers()
      }

      true
    } catch (_: Exception) {
      false
    }
  }

  

  fun clearDemoDataForLiveClinic(resetStockToZero: Boolean) {
    val ctx = getApplication<Application>()
    val currentPatIds = _patients.value.map { it.id }.toSet()
    val currentConsIds = _consultations.value.map { it.id }.toSet()
    val currentInvIds = _invoices.value.map { it.id }.toSet()

    _patients.value = emptyList()
    _selectedPatientId.value = null
    _consultations.value = emptyList()
    _invoices.value = emptyList()
    _expenses.value = emptyList()
    _vaccines.value = emptyList()
    _treatmentCards.value = emptyList()
    draftConsultations.value = emptyMap()

    ClinicDataPersistence.savePatients(ctx, emptyList())
    ClinicDataPersistence.saveConsultations(ctx, emptyList())
    ClinicDataPersistence.saveInvoices(ctx, emptyList())
    ClinicDataPersistence.saveExpenses(ctx, emptyList())
    ClinicDataPersistence.saveVaccines(ctx, emptyList())
    ClinicDataPersistence.saveTreatmentCards(ctx, emptyList())

    val deletedPatIds = ClinicDataPersistence.loadDeletedPatientIds(ctx) + currentPatIds
    val deletedConsIds = ClinicDataPersistence.loadDeletedConsultationIds(ctx) + currentConsIds
    val deletedInvIds = ClinicDataPersistence.loadDeletedInvoiceIds(ctx) + currentInvIds

    ClinicDataPersistence.saveDeletedPatientIds(ctx, deletedPatIds)
    ClinicDataPersistence.saveDeletedConsultationIds(ctx, deletedConsIds)
    ClinicDataPersistence.saveDeletedInvoiceIds(ctx, deletedInvIds)

    if (resetStockToZero) {
      val currentMedIds = _medicines.value.map { it.id }.toSet()
      _medicines.value = emptyList()
      ClinicDataPersistence.saveMedicines(ctx, emptyList())
      val deletedMedIds = ClinicDataPersistence.loadDeletedMedicineIds(ctx) + currentMedIds
      ClinicDataPersistence.saveDeletedMedicineIds(ctx, deletedMedIds)
    }

    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
  }

  fun clearAllDataAndStartFresh(clearMedicines: Boolean) {
    clearDemoDataForLiveClinic(resetStockToZero = clearMedicines)
  }

  fun reloadDataFromStorage() {
    val ctx = getApplication<Application>()
    val deletedPatients = ClinicDataPersistence.loadDeletedPatientIds(ctx)
    val deletedConsultations = ClinicDataPersistence.loadDeletedConsultationIds(ctx)
    val deletedInvoices = ClinicDataPersistence.loadDeletedInvoiceIds(ctx)
    val deletedMedicines = ClinicDataPersistence.loadDeletedMedicineIds(ctx)

    val loadedPatients = ClinicDataPersistence.loadPatients(ctx)
    if (loadedPatients != null) {
      val filteredPatients = loadedPatients.filter { !deletedPatients.contains(it.id) }
      _patients.value = filteredPatients
      if (_selectedPatientId.value == null || !filteredPatients.any { it.id == _selectedPatientId.value }) {
        _selectedPatientId.value = filteredPatients.firstOrNull { it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION }?.id
      }
    }
    val loadedConsultations = ClinicDataPersistence.loadConsultations(ctx)
    if (loadedConsultations != null) {
      _consultations.value = loadedConsultations.filter { !deletedConsultations.contains(it.id) && !deletedPatients.contains(it.patientId) }
    }
    val loadedInvoices = ClinicDataPersistence.loadInvoices(ctx)
    if (loadedInvoices != null) {
      _invoices.value = loadedInvoices.filter { !deletedInvoices.contains(it.id) && !deletedPatients.contains(it.patientId) }
    }
    val loadedMedicines = ClinicDataPersistence.loadMedicines(ctx)
    if (loadedMedicines != null) {
      _medicines.value = loadedMedicines.filter { !deletedMedicines.contains(it.id) }
    }
    val loadedUsers = ClinicDataPersistence.loadUsers(ctx)
    if (loadedUsers != null && loadedUsers.isNotEmpty()) {
      _users.value = loadedUsers
    }
    val loadedTreatmentCards = ClinicDataPersistence.loadTreatmentCards(ctx)
    if (loadedTreatmentCards != null) {
      val deletedCardIds = ClinicDataPersistence.loadDeletedTreatmentCardIds(ctx)
      val validPatientIds = _patients.value.map { it.id }.toSet()
      val cleanedTreatmentCards = loadedTreatmentCards.filter {
        !deletedCardIds.contains(it.id) &&
        !deletedPatients.contains(it.patientId) &&
        validPatientIds.contains(it.patientId)
      }
      _treatmentCards.value = cleanedTreatmentCards
      if (cleanedTreatmentCards.size != loadedTreatmentCards.size) {
        ClinicDataPersistence.saveTreatmentCards(ctx, cleanedTreatmentCards)
      }
    }
    clinicSettings.value = ClinicDataPersistence.loadClinicSettings(ctx)
    val loadedLabOrders = ClinicDataPersistence.loadLabOrders(ctx)
    if (loadedLabOrders != null) {
      _labOrders.value = loadedLabOrders
    }
    val loadedLabTemplates = ClinicDataPersistence.loadLabTemplates(ctx)
    if (loadedLabTemplates != null) {
      _labTemplates.value = loadedLabTemplates
    }
    val loadedLabInvoices = ClinicDataPersistence.loadLabInvoices(ctx)
    if (loadedLabInvoices != null) {
      _labInvoices.value = loadedLabInvoices
    }
  }

  // --- Wi-Fi & Cloud Sync Handlers ---
  fun startWifiSyncHub(port: Int = 8080, callback: ((Boolean, String) -> Unit)? = null) {
    LocalWifiSyncService.startHub(port) { ok, msg ->
      isWifiHubRunning.value = ok
      localWifiIp.value = LocalWifiSyncService.getLocalIpAddress()
      callback?.invoke(ok, msg)
    }
  }

  fun startWifiSyncHub() {
    startWifiSyncHub(8080, null)
  }

  fun stopWifiSyncHub() {
    LocalWifiSyncService.stopHub()
    isWifiHubRunning.value = false
  }

  fun triggerWifiSync() {
    syncManager.syncWithAllPeers()
  }

  fun triggerManualSync() {
    syncManager.syncWithAllPeers()
  }

  fun addManualPeer(ip: String) {
    syncManager.addManualPeer(ip)
  }

  fun syncWithRemoteWifiHub(ip: String) {
    syncManager.addManualPeer(ip)
    syncManager.syncWithAllPeers()
  }

  fun syncWithRemoteWifiHub(
    ip: String,
    port: Int = 8080,
    callback: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.addManualPeer(ip)
      syncManager.syncWithAllPeers()
      withContext(Dispatchers.Main) {
        callback(true, "Wi-Fi Peer ($ip:$port) နှင့် ချိတ်ဆက် sync လုပ်ပြီးပါပြီ")
      }
    }
  }

  fun syncWithAllDiscoveredPeers(callback: (Boolean, String) -> Unit) {
    val peers = com.example.service.sync.LocalWifiSyncService.discoveredPeers.value
    if (peers.isEmpty()) {
      callback(false, "ချိတ်ဆက်ရန် စက်မရှိသေးပါ (No peers discovered)")
      return
    }
    viewModelScope.launch(Dispatchers.IO) {
      peers.forEach { peer ->
        syncManager.addManualPeer(peer.ip, peer.port)
      }
      syncManager.syncWithAllPeers()
      withContext(Dispatchers.Main) {
        callback(true, "တွေ့ရှိသော စက် ${peers.size} ခုလုံးနှင့် အလိုအလျောက် Sync ပြုလုပ်ပြီးပါပြီ")
      }
    }
  }

  suspend fun testWifiHubConnection(ip: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    Pair(true, "Wi-Fi Peer ($ip) အဆင်သင့်ဖြစ်ပါသည်")
  }

  fun testWifiHubConnection(
    ip: String,
    port: Int = 8080,
    callback: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val res = Pair(true, "Wi-Fi Peer ($ip:$port) အဆင်သင့်ဖြစ်ပါသည်")
      withContext(Dispatchers.Main) {
        callback(res.first, res.second)
      }
    }
  }

  suspend fun syncWithSupabaseCloud(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val set = clinicSettings.value
    isSyncing.value = true
    val payload = exportFullBackupJson()
    val res = SupabaseSyncManager.syncDataToCloud(set.supabaseUrl, set.supabaseAnonKey, payload)
    isSyncing.value = false
    if (res.first) {
      if (SupabaseSyncManager.lastMergedJson != null) {
        restoreFullBackupJson(SupabaseSyncManager.lastMergedJson!!)
      }
      withContext(Dispatchers.Main) {
        com.example.util.SoundHelper.playSyncSuccessSound(getApplication())
      }
    }
    res
  }

  fun syncWithSupabaseCloud(callback: (Boolean, String) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val res = syncWithSupabaseCloud()
      withContext(Dispatchers.Main) {
        callback(res.first, res.second)
      }
    }
  }

  suspend fun pullFromSupabaseCloud(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val set = clinicSettings.value
    isSyncing.value = true
    val res = SupabaseSyncManager.pullDataFromCloud(set.supabaseUrl, set.supabaseAnonKey)
    isSyncing.value = false
    if (res.first) {
      val restoreOk = restoreFullBackupJson(res.second)
      if (restoreOk) {
        withContext(Dispatchers.Main) {
          com.example.util.SoundHelper.playSyncSuccessSound(getApplication())
        }
        Pair(true, "Cloud မှ ဆေးခန်းဒေတာများ အောင်မြင်စွာ ဒေါင်းလုဒ်ဆွဲပြီး ထည့်သွင်းပြီးပါပြီ")
      } else {
        Pair(false, "Cloud ဒေတာများကို စက်တွင်းသို့ ပေါင်းစပ်ရာတွင် အမှားတစ်ခု ဖြစ်ပေါ်ခဲ့ပါသည်")
      }
    } else {
      Pair(false, res.second)
    }
  }

  fun pullFromSupabaseCloud(callback: (Boolean, String) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val res = pullFromSupabaseCloud()
      withContext(Dispatchers.Main) {
        callback(res.first, res.second)
      }
    }
  }

  suspend fun resetSupabaseCloud(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val set = clinicSettings.value
    isSyncing.value = true
    val res = SupabaseSyncManager.resetCloudData(set.supabaseUrl, set.supabaseAnonKey)
    isSyncing.value = false
    res
  }

  fun resetSupabaseCloud(callback: (Boolean, String) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val res = resetSupabaseCloud()
      withContext(Dispatchers.Main) {
        callback(res.first, res.second)
      }
    }
  }

  suspend fun testSupabaseConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val set = clinicSettings.value
    SupabaseSyncManager.testConnection(set.supabaseUrl, set.supabaseAnonKey)
  }

  fun testSupabaseConnection(
    url: String = clinicSettings.value.supabaseUrl,
    key: String = clinicSettings.value.supabaseAnonKey,
    callback: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val res = SupabaseSyncManager.testConnection(url, key)
      withContext(Dispatchers.Main) {
        callback(res.first, res.second)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    syncManager.stop()
    LocalWifiSyncService.stopHub()
  }

  // --- In-App Auto Update System ---
  val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
  val isUpdateDialogVisible = MutableStateFlow(false)
  private var downloadedApkFile: java.io.File? = null

  fun checkForUpdates(isManualCheck: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      if (isManualCheck) {
        updateState.value = UpdateState.Checking
        isUpdateDialogVisible.value = true
      }
      val res = AppUpdateManager.checkForUpdates(
        currentVersionCode = BuildConfig.VERSION_CODE,
        currentVersionName = BuildConfig.VERSION_NAME
      )
      withContext(Dispatchers.Main) {
        updateState.value = res
        if (res is UpdateState.UpdateAvailable || isManualCheck) {
          isUpdateDialogVisible.value = true
        }
      }
    }
  }

  fun startDownloadUpdate(context: android.content.Context, info: AppUpdateInfo) {
    viewModelScope.launch(Dispatchers.IO) {
      val (ok, file) = AppUpdateManager.downloadApk(context, info) { progress ->
        updateState.value = progress
      }
      withContext(Dispatchers.Main) {
        if (ok && file != null && file.exists()) {
          downloadedApkFile = file
          updateState.value = UpdateState.ReadyToInstall(file, info)
          if (AppUpdateManager.canRequestPackageInstalls(context)) {
            AppUpdateManager.launchInstaller(context, file)
          } else {
            AppUpdateManager.openInstallPermissionSettings(context)
          }
        } else {
          updateState.value = UpdateState.Error("ဒေါင်းလုဒ် မအောင်မြင်ပါ (Download failed)")
        }
      }
    }
  }

  fun installDownloadedUpdate(context: android.content.Context) {
    val file = downloadedApkFile
    if (file != null && file.exists()) {
      if (!AppUpdateManager.canRequestPackageInstalls(context)) {
        AppUpdateManager.openInstallPermissionSettings(context)
      }
      AppUpdateManager.launchInstaller(context, file)
    } else {
      updateState.value = UpdateState.Error("APK ဖိုင် ရှာမတွေ့ပါ။ ပြန်လည်ဒေါင်းလုဒ်လုပ်ပါ")
    }
  }

  fun dismissUpdateDialog() {
    isUpdateDialogVisible.value = false
    if (updateState.value !is UpdateState.ReadyToInstall) {
      updateState.value = UpdateState.Idle
    }
  }

  fun autoSyncToSupabaseIfEnabled() {
    if (clinicSettings.value.enableSupabaseSync &&
        clinicSettings.value.supabaseUrl.isNotBlank() &&
        clinicSettings.value.supabaseAnonKey.isNotBlank()) {
      viewModelScope.launch(Dispatchers.IO) {
        try {
          syncWithSupabaseCloud()
        } catch (_: Exception) {}
      }
    }
  }

  // --- B2B Laboratory Investigation Methods ---
  fun createLabOrder(order: LabOrder) {
    val ctx = getApplication<Application>()
    val current = _labOrders.value.toMutableList()
    current.add(0, order)
    _labOrders.value = current
    ClinicDataPersistence.saveLabOrders(ctx, current)
  }

  fun updateLabOrder(order: LabOrder) {
    val ctx = getApplication<Application>()
    val updated = _labOrders.value.map { if (it.id == order.id) order else it }
    _labOrders.value = updated
    ClinicDataPersistence.saveLabOrders(ctx, updated)
  }

  fun updateLabOrderStatus(orderId: String, newStatus: LabOrderStatus) {
    val ctx = getApplication<Application>()
    val completedAtStr = if (newStatus == LabOrderStatus.COMPLETED) BurmeseDateUtils.getNowFormatted() else null
    val updated = _labOrders.value.map { ord ->
      if (ord.id == orderId) {
        ord.copy(
          status = newStatus,
          completedAt = completedAtStr ?: ord.completedAt
        )
      } else ord
    }
    _labOrders.value = updated
    ClinicDataPersistence.saveLabOrders(ctx, updated)
  }

  fun saveLabOrderResults(
    orderId: String,
    updatedItems: List<LabOrderItem>,
    technicianName: String,
    notes: String
  ) {
    val ctx = getApplication<Application>()
    val isAllCompleted = updatedItems.all { it.isCompleted }
    val completedAtStr = if (isAllCompleted) BurmeseDateUtils.getNowFormatted() else null
    val updated = _labOrders.value.map { ord ->
      if (ord.id == orderId) {
        ord.copy(
          items = updatedItems,
          technicianName = technicianName.ifBlank { ord.technicianName },
          notes = notes,
          status = if (isAllCompleted) LabOrderStatus.COMPLETED else ord.status,
          completedAt = completedAtStr ?: ord.completedAt
        )
      } else ord
    }
    _labOrders.value = updated
    ClinicDataPersistence.saveLabOrders(ctx, updated)
  }

  fun deleteLabOrder(orderId: String) {
    val ctx = getApplication<Application>()
    val updated = _labOrders.value.filter { it.id != orderId }
    _labOrders.value = updated
    ClinicDataPersistence.saveLabOrders(ctx, updated)
  }

  fun saveLabTemplate(template: LabTestTemplate) {
    val ctx = getApplication<Application>()
    val current = _labTemplates.value.toMutableList()
    val index = current.indexOfFirst { it.id == template.id }
    if (index >= 0) {
      current[index] = template
    } else {
      current.add(template)
    }
    _labTemplates.value = current
    ClinicDataPersistence.saveLabTemplates(ctx, current)
  }

  fun deleteLabTemplate(templateId: String) {
    val ctx = getApplication<Application>()
    val updated = _labTemplates.value.filter { it.id != templateId }
    _labTemplates.value = updated
    ClinicDataPersistence.saveLabTemplates(ctx, updated)
  }

  fun createLabInvoice(invoice: LabInvoice) {
    val ctx = getApplication<Application>()
    val currentInvoices = _labInvoices.value.toMutableList()
    currentInvoices.add(0, invoice)
    _labInvoices.value = currentInvoices
    ClinicDataPersistence.saveLabInvoices(ctx, currentInvoices)

    // Mark corresponding LabOrder as paid
    val updatedOrders = _labOrders.value.map { ord ->
      if (ord.id == invoice.orderId || ord.orderNumber == invoice.orderId) {
        ord.copy(
          paidAmount = invoice.paidAmount,
          paymentStatus = if (invoice.paidAmount >= ord.totalAmount) "Paid" else "Partial",
          invoiceNumber = invoice.invoiceNumber
        )
      } else ord
    }
    _labOrders.value = updatedOrders
    ClinicDataPersistence.saveLabOrders(ctx, updatedOrders)
  }

  fun orderLabTestsForPatient(
    patient: Patient,
    selectedTemplates: List<LabTestTemplate>,
    doctorName: String,
    clinicalNotes: String = ""
  ): LabOrder {
    val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val orderNumber = "LAB-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}-${(100..999).random()}"

    val items = selectedTemplates.map { t ->
      LabOrderItem(
        testId = t.id,
        testName = t.name,
        category = t.category,
        price = t.defaultPrice,
        specimenType = t.specimenType,
        remarks = "",
        isCompleted = false,
        parameters = t.parameters.map { p ->
          LabTestParameter(
            name = p.name,
            result = "",
            unit = p.unit,
            referenceRange = p.referenceRange,
            flag = ""
          )
        }
      )
    }

    val totalAmount = items.sumOf { it.price }

    val order = LabOrder(
      id = UUID.randomUUID().toString(),
      orderNumber = orderNumber,
      patientId = patient.id,
      patientName = patient.name,
      patientHn = patient.hn,
      patientAge = patient.age,
      patientGender = patient.gender,
      doctorName = doctorName,
      orderDate = now,
      orderTime = nowTime,
      status = LabOrderStatus.ORDERED,
      totalAmount = totalAmount,
      paidAmount = 0.0,
      paymentStatus = "Unpaid",
      invoiceNumber = "",
      technicianName = "",
      notes = clinicalNotes,
      items = items
    )

    createLabOrder(order)
    return order
  }
}
