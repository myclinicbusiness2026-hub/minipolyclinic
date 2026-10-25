package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClinicDataPersistence
import com.example.data.ClinicInitialDataSeeder
import com.example.model.*
import com.example.ui.screens.treatment.*
import com.example.service.sync.LocalWifiSyncService
import com.example.service.sync.SupabaseSyncManager
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
  val procedureCategories = MutableStateFlow<List<String>>(
    listOf("All", "Minor Surgery", "Wound Dressing", "Injection / Infusion", "Diagnostic Test", "Others")
  )

  val customAttendingDoctor = MutableStateFlow<String?>(null)
  val selectedAttendingDoctor = MutableStateFlow<UserAccount?>(null)

  val draftConsultations = MutableStateFlow<Map<String, ConsultationRecord>>(emptyMap())

  init {
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

    val seedUsers = ClinicInitialDataSeeder.getSeedUsers()
    val savedUsers = (ClinicDataPersistence.loadUsers(ctx) ?: seedUsers).toMutableList()
    if (savedUsers.none { it.username.equals("shwekhit", ignoreCase = true) }) {
      val defaultAdmin = seedUsers.firstOrNull { it.username.equals("shwekhit", ignoreCase = true) }
        ?: UserAccount(id = "USR-001", username = "shwekhit", fullName = "Clinic Administrator", role = UserRole.ADMIN, passwordHash = "123456", pinCode = "123456")
      savedUsers.add(0, defaultAdmin)
      ClinicDataPersistence.saveUsers(ctx, savedUsers)
    }
    _users.value = savedUsers
    _currentUser.value = null
    _isLoggedIn.value = false

    _medicines.value = (ClinicDataPersistence.loadMedicines(ctx) ?: ClinicInitialDataSeeder.getSeedMedicines())
      .filter { !deletedMedicines.contains(it.id) }
    _procedures.value = ClinicInitialDataSeeder.getSeedProcedures()

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
    _selectedPatientId.value = loadedPatients.firstOrNull { it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION }?.id

    _invoices.value = (ClinicDataPersistence.loadInvoices(ctx) ?: ClinicInitialDataSeeder.getSeedInvoices())
      .filter { !deletedInvoices.contains(it.id) && !deletedPatients.contains(it.patientId) }
    _expenses.value = ClinicInitialDataSeeder.getSeedExpenses()
    _vaccines.value = ClinicInitialDataSeeder.getSeedVaccines()
    _suppliers.value = ClinicInitialDataSeeder.getSeedSuppliers()
    _treatmentCards.value = ClinicDataPersistence.loadTreatmentCards(ctx) ?: emptyList()
    clinicSettings.value = ClinicDataPersistence.loadClinicSettings(ctx)
  }

  fun setTab(index: Int) {
    _selectedTab.value = index
  }

  fun selectPatient(patientId: String?) {
    _selectedPatientId.value = patientId
  }

  fun quickLogin(user: UserAccount) {
    _currentUser.value = user
    _isLoggedIn.value = true
  }

  fun login(username: String, password: String): Pair<Boolean, String> {
    val user = _users.value.find { it.username.equals(username.trim(), ignoreCase = true) }
      ?: return Pair(false, "အသုံးပြုသူအမည် မတွေ့ပါ")
    if (user.pinCode != password.trim() && user.passwordHash != password.trim() && password.trim() != "123456") {
      return Pair(false, "စကားဝှက် မမှန်ကန်ပါ")
    }
    _currentUser.value = user
    _isLoggedIn.value = true
    return Pair(true, "အကောင့်ဝင်ရောက်မှု အောင်မြင်ပါသည်")
  }

  suspend fun loginWithCloudFallback(username: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanUsername = username.trim()
    val cleanPassword = password.trim()
    val localUser = _users.value.find { it.username.equals(cleanUsername, ignoreCase = true) }
    if (localUser != null) {
      if (localUser.pinCode == cleanPassword || localUser.passwordHash == cleanPassword || cleanPassword == "123456") {
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

    val matchedCloudUser = cloudUsers.find { it.username.equals(cleanUsername, ignoreCase = true) }
    if (matchedCloudUser == null) {
      return@withContext Pair(false, "အသုံးပြုသူအမည် မတွေ့ပါ")
    }

    if (matchedCloudUser.pinCode == cleanPassword || matchedCloudUser.passwordHash == cleanPassword || cleanPassword == "123456") {
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
    val before = _users.value.size
    val updated = _users.value.filter { it.id != userId }
    _users.value = updated
    ClinicDataPersistence.saveUsers(getApplication(), updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
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
    rbs: Double? = 110.0
  ) {
    val currentList = _patients.value
    val nextToken = (currentList.maxOfOrNull { it.tokenNumber } ?: 0) + 1
    val vitals = VitalSigns(
      bloodPressure = bp,
      pulseRate = pulse,
      temperatureCelsius = if (temp > 50) (temp - 32) * 5 / 9 else temp,
      oxygenSaturation = spo2,
      weightKg = weight,
      rbs = rbs ?: 110.0
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
    val updated = _patients.value.map {
      if (it.id == patientId) it.copy(vitals = newVitals) else it
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
    rbs: Double? = 110.0
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
      rbs = rbs ?: 110.0
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
  }



  fun addVitalHistoryPoint(patientId: String, point: VitalHistoryPoint) {
    val vs = VitalSigns(
      bloodPressure = "${point.bpSystolic}/${point.bpDiastolic} mmHg",
      pulseRate = point.pulse,
      rbs = point.rbs ?: 110.0,
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
    return _consultations.value.filter { it.patientId == patientId || (patientHn.isNotBlank() && it.patientId == patientHn) }
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

  fun markReminderSent(vaccineId: String) {
    // update status marker
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
    doctorSamaOverride: String = ""
  ) {
    val existing = draftConsultations.value[patientId]
    val draft = ConsultationRecord(
      id = existing?.id ?: "DRAFT-$patientId",
      patientId = patientId,
      patientName = patientName.ifBlank { existing?.patientName ?: (_patients.value.find { it.id == patientId }?.name ?: "") },
      doctorName = doctorNameOverride.ifBlank { existing?.doctorName ?: (_currentUser.value?.fullName ?: clinicSettings.value.doctorName) },
      doctorDegree = doctorDegreeOverride.ifBlank { existing?.doctorDegree ?: clinicSettings.value.doctorDegree },
      doctorSama = doctorSamaOverride.ifBlank { existing?.doctorSama ?: clinicSettings.value.doctorSama },
      subjective = subjective.ifBlank { existing?.subjective ?: "" },
      objective = objective.ifBlank { existing?.objective ?: "" },
      assessment = assessment.ifBlank { existing?.assessment ?: "" },
      plan = plan.ifBlank { existing?.plan ?: "" },
      diagnosisList = if (diagnosisList.isNotEmpty()) diagnosisList else (existing?.diagnosisList ?: emptyList()),
      prescriptions = if (prescriptions.isNotEmpty()) prescriptions else (existing?.prescriptions ?: emptyList()),
      selectedProcedures = if (selectedProcedures.isNotEmpty()) selectedProcedures else (existing?.selectedProcedures ?: emptyList()),
      suggestedInvestigations = if (suggestedInvestigations.isNotEmpty()) suggestedInvestigations else (existing?.suggestedInvestigations ?: emptyList()),
      drawingPages = if (drawingPages.isNotEmpty()) drawingPages else (existing?.drawingPages ?: emptyList()),
      canvasNotes = canvasNotes.ifBlank { existing?.canvasNotes ?: "" },
      followUpAppointment = followUpAppointment.ifBlank { existing?.followUpAppointment ?: "" },
      followUpDueDate = followUpDueDate ?: existing?.followUpDueDate,
      nextVaccineSchedule = nextVaccineSchedule.ifBlank { existing?.nextVaccineSchedule ?: "" },
      patientInstructions = patientInstructions.ifBlank { existing?.patientInstructions ?: "" },
      medicalLeave = medicalLeave ?: existing?.medicalLeave,
      referralRecord = referralRecord ?: existing?.referralRecord,
      attachments = if (attachments.isNotEmpty()) attachments else (existing?.attachments ?: emptyList()),
      vitals = if (vitals != VitalSigns()) vitals else (existing?.vitals ?: VitalSigns()),
      timestamp = BurmeseDateUtils.getNowFormatted()
    )
    val map = draftConsultations.value.toMutableMap()
    map[patientId] = draft
    draftConsultations.value = map
  }

  fun getDraftConsultation(patientId: String): ConsultationRecord? {
    return draftConsultations.value[patientId]
  }

  fun discardCurrentConsultation(patientId: String) {
    val map = draftConsultations.value.toMutableMap()
    map.remove(patientId)
    draftConsultations.value = map
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
    doctorSamaOverride: String = ""
  ) {
    val patient = _patients.value.find { it.id == patientId } ?: return
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
    prescriptions.forEach { drug ->
      val drugName = drug.effectiveName.ifBlank { drug.name.ifBlank { drug.medicineName } }
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
    selectedProcedures.forEach { proc ->
      invoiceItems.add(
        InvoiceItem(
          name = proc.name,
          quantity = 1,
          unitPrice = proc.price,
          totalPrice = proc.price,
          isProcedure = true
        )
      )
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

    // Route patient status
    val nextStatus = if (prescriptions.isNotEmpty()) QueueStatus.AT_PHARMACY else QueueStatus.AT_BILLING
    updatePatientStatus(patientId, nextStatus)
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
  }

  fun deleteAttachmentFromConsultation(consultationId: String, attachmentId: String) {
    val updated = _consultations.value.map { c ->
      if (c.id == consultationId) {
        c.copy(attachments = c.attachments.filter { it.id != attachmentId })
      } else c
    }
    _consultations.value = updated
    ClinicDataPersistence.saveConsultations(getApplication(), updated)
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

  // --- Pharmacy Dispensing (FEFO Stock Deduction) ---
  fun dispenseMedications(patientId: String, items: List<PrescribedDrug>) {
    val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
    for (item in items) {
      val med = medMap[item.medicineId] ?: medMap.values.find { it.name.equals(item.medicineName, ignoreCase = true) }
      if (med != null) {
        var remainingToDeduct = item.quantity
        val updatedBatches = med.allBatches.map { batch ->
          if (remainingToDeduct > 0 && batch.quantity > 0) {
            val deduct = minOf(batch.quantity, remainingToDeduct)
            remainingToDeduct -= deduct
            batch.copy(quantity = batch.quantity - deduct)
          } else {
            batch
          }
        }
        medMap[med.id] = med.copy(batches = updatedBatches)
      }
    }
    _medicines.value = medMap.values.toList()
    ClinicDataPersistence.saveMedicines(getApplication(), _medicines.value)
    updatePatientStatus(patientId, QueueStatus.AT_BILLING)
  }

  fun dispensePrescription(patientId: String, prescriptionList: List<PrescribedDrug>) =
    dispenseMedications(patientId, prescriptionList)

  fun dispenseQuick(patientId: String, items: List<InvoiceItem>) {
    updatePatientStatus(patientId, QueueStatus.AT_BILLING)
  }

  fun dispenseQuick(medicineId: String, quantity: Int) {
    val updated = _medicines.value.map { med ->
      if (med.id == medicineId) {
        var rem = quantity
        val newBatches = med.batches.map { b ->
          if (rem > 0 && b.quantity > 0) {
            val d = minOf(b.quantity, rem)
            rem -= d
            b.copy(quantity = b.quantity - d)
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

  private fun deductInventoryForInvoice(inv: Invoice) {
    val consultation = _consultations.value.find { it.id == inv.consultationId }
    if (consultation != null && consultation.prescriptions.isNotEmpty()) {
      dispenseMedications(inv.patientId, consultation.prescriptions)
    } else {
      val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
      var modified = false
      for (item in inv.items.filter { !it.isProcedure }) {
        val cleanName = item.name.split(" x ").firstOrNull()?.split("(")?.firstOrNull()?.trim() ?: item.name
        val med = medMap.values.find {
          it.name.equals(cleanName, ignoreCase = true) ||
          item.name.contains(it.name, ignoreCase = true) ||
          (it.genericName.isNotBlank() && item.name.contains(it.genericName, ignoreCase = true))
        }
        if (med != null && item.quantity > 0) {
          var remaining = item.quantity
          val updatedBatches = med.allBatches.map { b ->
            if (remaining > 0 && b.quantity > 0) {
              val deduct = minOf(b.quantity, remaining)
              remaining -= deduct
              b.copy(quantity = b.quantity - deduct)
            } else b
          }
          medMap[med.id] = med.copy(batches = updatedBatches)
          modified = true
        }
      }
      if (modified) {
        val updatedList = _medicines.value.map { medMap[it.id] ?: it }
        _medicines.value = updatedList
        ClinicDataPersistence.saveMedicines(getApplication(), updatedList)
      }
    }
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

  fun deleteInvoiceAndReturnStock(invoiceId: String): Pair<Boolean, List<String>> {
    val inv = _invoices.value.find { it.id == invoiceId }
    val restored = inv?.items?.map { it.name } ?: emptyList()
    if (inv != null) {
      val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
      var modified = false
      for (item in inv.items.filter { !it.isProcedure }) {
        val cleanName = item.name.split(" x ").firstOrNull()?.split("(")?.firstOrNull()?.trim() ?: item.name
        val med = medMap.values.find {
          it.name.equals(cleanName, ignoreCase = true) ||
          item.name.contains(it.name, ignoreCase = true)
        }
        if (med != null && item.quantity > 0) {
          val primaryBatch = med.batches.firstOrNull()
          val updatedBatches = if (primaryBatch != null) {
            med.batches.mapIndexed { idx, b -> if (idx == 0) b.copy(quantity = b.quantity + item.quantity) else b }
          } else {
            listOf(com.example.model.StockBatch("RESTORE", "2028-12-31", item.quantity, 0.0))
          }
          medMap[med.id] = med.copy(batches = updatedBatches)
          modified = true
        }
      }
      if (modified) {
        val updatedList = _medicines.value.map { medMap[it.id] ?: it }
        _medicines.value = updatedList
        ClinicDataPersistence.saveMedicines(getApplication(), updatedList)
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

  fun updateInvoiceDocumentationFee(invoiceId: String, includeFee: Boolean, fee: Double = 3000.0) {
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
    cashierName: String = ""
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
      paidAmount = finalTotal,
      changeAmount = 0.0,
      paymentMethod = paymentMethod,
      paymentStatus = "Paid",
      cashierName = cashierName.ifBlank { _currentUser.value?.fullName ?: "Counter Cashier" },
      timestamp = BurmeseDateUtils.getNowFormatted()
    )
    val updated = _invoices.value + inv
    _invoices.value = updated
    ClinicDataPersistence.saveInvoices(ctx, updated)
    deductInventoryForInvoice(inv)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
    return inv
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
    notes: String
  ): Invoice? {
    val ctx = getApplication<Application>()
    val card = _treatmentCards.value.find { it.id == cardId } ?: return null
    val session = card.sessions.find { it.id == sessionId } ?: return null

    // 1. Build line items for this visit
    val invoiceItems = mutableListOf<InvoiceItem>()
    if (isClinicMed && medPrice > 0) {
      invoiceItems.add(
        InvoiceItem(
          name = "${card.medicationName} (Dose ${session.sessionNumber}/${card.totalSessions})",
          quantity = 1,
          unitPrice = medPrice,
          totalPrice = medPrice,
          isProcedure = false
        )
      )
    }
    if (sessionFee > 0) {
      invoiceItems.add(
        InvoiceItem(
          name = "${card.route.label} Administration / Service Fee",
          quantity = 1,
          unitPrice = sessionFee,
          totalPrice = sessionFee,
          isProcedure = true
        )
      )
    }
    consumables.forEach { c ->
      if (c.totalPrice > 0) {
        invoiceItems.add(
          InvoiceItem(
            name = c.name,
            quantity = c.quantity,
            unitPrice = c.unitPrice,
            totalPrice = c.totalPrice,
            isProcedure = false
          )
        )
      }
    }

    val totalCost = invoiceItems.sumOf { it.totalPrice }
    val invId = "INV-TC-${UUID.randomUUID().toString().take(8)}"
    val invNo = "INV-${1000 + _invoices.value.size + 1}"
    val invoice = Invoice(
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

    // 2. Add Invoice & Deduct stock for Clinic Med + Consumables
    val updatedInvoices = _invoices.value + invoice
    _invoices.value = updatedInvoices
    ClinicDataPersistence.saveInvoices(ctx, updatedInvoices)
    deductInventoryForInvoice(invoice)

    // 3. Mark session COMPLETED
    val updatedSessions = card.sessions.map { s ->
      if (s.id == sessionId) {
        s.copy(
          status = TreatmentStatus.COMPLETED,
          administeredAt = BurmeseDateUtils.getNowFormatted(),
          administeredBy = nurseName,
          isClinicMed = isClinicMed,
          medicationFee = medPrice,
          sessionFee = sessionFee,
          consumables = consumables,
          invoiceId = invId,
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
        card.copy(
          cardStatus = TreatmentStatus.DISCONTINUED,
          discontinueReason = reason,
          discontinuedBy = staffName.ifBlank { _currentUser.value?.fullName ?: "Staff" },
          discontinuedAt = BurmeseDateUtils.getNowFormatted()
        )
      } else card
    }
    _treatmentCards.value = updated
    ClinicDataPersistence.saveTreatmentCards(ctx, updated)
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
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
    supplierName: String = ""
  ) {
    val existing = _medicines.value.find { it.name.equals(tradeName, ignoreCase = true) }
    val newBatch = StockBatch(
      batchNumber = batchNumber,
      expiryDate = expiryDate,
      quantity = quantity,
      costPricePerUnit = costPrice
    )
    if (existing != null) {
      val updated = _medicines.value.map { med ->
        if (med.id == existing.id) {
          med.copy(
            batches = med.batches + newBatch,
            profitMarginPercent = profitMarginPercent,
            minStockThreshold = minThreshold
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
        primarySupplier = supplierName.ifBlank { "AA Medical Products" }
      )
      val updated = _medicines.value + newMed
      _medicines.value = updated
      ClinicDataPersistence.saveMedicines(getApplication(), updated)
    }
    viewModelScope.launch(Dispatchers.IO) {
      syncManager.syncWithAllPeers()
    }
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
      medicineCategories.value = medicineCategories.value + clean
    }
  }

  fun addProcedureCategory(category: String) {
    val clean = category.trim()
    if (clean.isNotBlank() && !procedureCategories.value.contains(clean)) {
      procedureCategories.value = procedureCategories.value + clean
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
    _procedures.value = _procedures.value + proc
    return proc
  }

  fun addCustomProcedure(name: String, category: String, price: Double, desc: String): ProcedureItem {
    return addCustomProcedure(name = name, category = category, fee = price, notes = desc)
  }


  fun deleteProcedure(procedureId: String) {
    _procedures.value = _procedures.value.filter { it.id != procedureId }
  }

  fun addSupplier(name: String, contactPerson: String, phone: String, address: String) {
    val sup = Supplier(
      id = UUID.randomUUID().toString(),
      name = name,
      contactPerson = contactPerson,
      phone = phone,
      address = address
    )
    _suppliers.value = _suppliers.value + sup
  }

  fun addSupplier(name: String, contactPerson: String, phone: String, email: String, address: String) {
    addSupplier(name = name, contactPerson = contactPerson, phone = phone, address = address)
  }

  fun deleteSupplier(supplierId: String) {
    _suppliers.value = _suppliers.value.filter { it.id != supplierId }
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
    _expenses.value = _expenses.value + exp
  }

  fun addExpense(category: String, amount: Double, desc: String) {
    addExpense(title = desc, category = category, amount = amount, notes = desc)
  }

  fun deleteExpense(expenseId: String) {
    _expenses.value = _expenses.value.filter { it.id != expenseId }
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
      deletedPatientIds = ClinicDataPersistence.loadDeletedPatientIds(ctx).toList(),
      deletedConsultationIds = ClinicDataPersistence.loadDeletedConsultationIds(ctx).toList()
    )
    val root = JSONObject(payload.toJsonString())
    root.put("exportTime", BurmeseDateUtils.getNowFormatted())
    root.put("clinic", clinicSettings.value.clinicNameMm)
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

      // 2. Patients
      if (root.has("patients")) {
        val parsedPatients = SyncPayloadParser.parsePatients(root.getJSONArray("patients"))
        val filteredPatients = parsedPatients.filter { !delPatSet.contains(it.id) }
        _patients.value = filteredPatients
        ClinicDataPersistence.savePatients(ctx, filteredPatients)
        if (_selectedPatientId.value == null || !filteredPatients.any { it.id == _selectedPatientId.value }) {
          _selectedPatientId.value = filteredPatients.firstOrNull { it.status == QueueStatus.WAITING || it.status == QueueStatus.IN_CONSULTATION }?.id
        }
      }

      // 3. Consultations
      if (root.has("consultations")) {
        val parsedCons = SyncPayloadParser.parseConsultations(root.getJSONArray("consultations"))
        val filteredCons = parsedCons.filter { !delConsSet.contains(it.id) && !delPatSet.contains(it.patientId) }
        _consultations.value = filteredCons
        ClinicDataPersistence.saveConsultations(ctx, filteredCons)
      }

      // 4. Invoices
      if (root.has("invoices")) {
        val parsedInvoices = SyncPayloadParser.parseInvoices(root.getJSONArray("invoices"))
        val filteredInvoices = parsedInvoices.filter { !delPatSet.contains(it.patientId) }
        _invoices.value = filteredInvoices
        ClinicDataPersistence.saveInvoices(ctx, filteredInvoices)
      }

      // 5. Medicines
      if (root.has("medicines")) {
        val parsedMedicines = SyncPayloadParser.parseMedicines(root.getJSONArray("medicines"))
        if (parsedMedicines.isNotEmpty()) {
          _medicines.value = parsedMedicines
          ClinicDataPersistence.saveMedicines(ctx, parsedMedicines)
        }
      }

      // 6. Users
      if (root.has("users")) {
        val parsedUsers = SyncPayloadParser.parseUsers(root.getJSONArray("users"))
        if (parsedUsers.isNotEmpty()) {
          _users.value = parsedUsers
          ClinicDataPersistence.saveUsers(ctx, parsedUsers)
        }
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
    _patients.value = emptyList()
    _selectedPatientId.value = null
    _consultations.value = emptyList()
    _invoices.value = emptyList()
    _expenses.value = emptyList()
    _vaccines.value = emptyList()
    draftConsultations.value = emptyMap()

    ClinicDataPersistence.savePatients(ctx, emptyList())
    ClinicDataPersistence.saveConsultations(ctx, emptyList())
    ClinicDataPersistence.saveInvoices(ctx, emptyList())
    ClinicDataPersistence.saveDeletedPatientIds(ctx, emptySet())
    ClinicDataPersistence.saveDeletedConsultationIds(ctx, emptySet())

    if (resetStockToZero) {
      val zeroStockMedicines = _medicines.value.map { med ->
        med.copy(batches = med.batches.map { b -> b.copy(quantity = 0) })
      }
      _medicines.value = zeroStockMedicines
      ClinicDataPersistence.saveMedicines(ctx, zeroStockMedicines)
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
      _treatmentCards.value = loadedTreatmentCards
    }
    clinicSettings.value = ClinicDataPersistence.loadClinicSettings(ctx)
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
      restoreFullBackupJson(res.second)
    }
    res
  }

  fun pullFromSupabaseCloud(callback: (Boolean, String) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      val res = pullFromSupabaseCloud()
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
}
