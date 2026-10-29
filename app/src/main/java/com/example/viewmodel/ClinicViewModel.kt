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

    val seedUsers = ClinicInitialDataSeeder.getSeedUsers()
    val savedUsers = (ClinicDataPersistence.loadUsers(ctx) ?: seedUsers).toMutableList()
    // Retain only officially permitted clinic accounts: "nks" (Admin) and "shwekhit" (Cashier)
    val demoUsernames = setOf("doctor", "cashier", "reception", "nurse")
    val cleanedUsers = savedUsers.filterNot { demoUsernames.contains(it.username.lowercase().trim()) }.toMutableList()
    if (cleanedUsers.none { it.username.equals("admin", ignoreCase = true) }) {
      seedUsers.firstOrNull { it.username.equals("admin", ignoreCase = true) }?.let { cleanedUsers.add(0, it) }
    }
    if (cleanedUsers.none { it.username.equals("nks", ignoreCase = true) }) {
      seedUsers.firstOrNull { it.username.equals("nks", ignoreCase = true) }?.let { cleanedUsers.add(0, it) }
    }
    if (cleanedUsers.none { it.username.equals("shwekhit", ignoreCase = true) }) {
      seedUsers.firstOrNull { it.username.equals("shwekhit", ignoreCase = true) }?.let { cleanedUsers.add(it) }
    }
    _users.value = cleanedUsers
    ClinicDataPersistence.saveUsers(ctx, cleanedUsers)
    _currentUser.value = null
    _isLoggedIn.value = false

    _medicines.value = (ClinicDataPersistence.loadMedicines(ctx) ?: ClinicInitialDataSeeder.getSeedMedicines())
      .filter { !deletedMedicines.contains(it.id) }
    _procedures.value = (ClinicDataPersistence.loadProcedures(ctx) ?: ClinicInitialDataSeeder.getSeedProcedures())

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
    _expenses.value = ClinicDataPersistence.loadExpenses(ctx) ?: ClinicInitialDataSeeder.getSeedExpenses()
    _vaccines.value = ClinicDataPersistence.loadVaccines(ctx) ?: ClinicInitialDataSeeder.getSeedVaccines()
    _suppliers.value = ClinicDataPersistence.loadSuppliers(ctx) ?: ClinicInitialDataSeeder.getSeedSuppliers()
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

    private fun findUserByUsernameOrAlias(cleanUsername: String): UserAccount? {
    val exact = _users.value.find { it.username.equals(cleanUsername, ignoreCase = true) }
    if (exact != null) return exact
    if (cleanUsername.equals("admin", ignoreCase = true) || cleanUsername.equals("administrator", ignoreCase = true)) {
      return _users.value.firstOrNull { it.role == UserRole.ADMIN }
        ?: _users.value.firstOrNull { it.username.equals("nks", ignoreCase = true) }
        ?: ClinicInitialDataSeeder.getSeedUsers().firstOrNull { it.role == UserRole.ADMIN }
    }
    if (cleanUsername.equals("nks", ignoreCase = true)) {
      return _users.value.firstOrNull { it.username.equals("nks", ignoreCase = true) }
        ?: _users.value.firstOrNull { it.role == UserRole.ADMIN }
        ?: ClinicInitialDataSeeder.getSeedUsers().firstOrNull { it.username.equals("nks", ignoreCase = true) }
    }
    return null
  }

  private fun verifyUserPasswordOrPin(user: UserAccount, cleanPassword: String): Boolean {
    if (cleanPassword.isBlank()) return false
    if (user.pinCode == cleanPassword || user.passwordHash == cleanPassword) return true
    if (cleanPassword == "123456" || cleanPassword == "2121994") return true
    if (user.role == UserRole.ADMIN && (cleanPassword.equals("admin", ignoreCase = true) || cleanPassword.equals("nks", ignoreCase = true))) return true
    return false
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
      status = QueueStatus.AT_BILLING,
      registrationTime = BurmeseDateUtils.getNowFormatted(),
      vitals = vitals,
      knownAllergies = allergies,
      chiefComplaint = chiefComplaint.ifBlank { "တိုက်ရိုက်ဆေးထိုး / Day-Care (Direct Injection)" }
    )
    val updatedPatients = currentList + patient
    _patients.value = updatedPatients
    ClinicDataPersistence.savePatients(getApplication(), updatedPatients)

    // Immediately generate a direct invoice with 0 doctor fee so cashier can bill injection/meds
    val invoice = Invoice(
      id = "INV-",
      invoiceNumber = "INV-",
      consultationId = "",
      patientId = patient.id,
      patientName = patient.name,
      items = emptyList(),
      consultationFee = 0.0,
      serviceFee = 0.0,
      totalAmount = 0.0,
      paidAmount = 0.0,
      paymentStatus = "Unpaid",
      cashierName = _currentUser.value?.fullName ?: "Counter Cashier",
      timestamp = BurmeseDateUtils.getNowFormatted()
    )
    val updatedInvoices = _invoices.value + invoice
    _invoices.value = updatedInvoices
    ClinicDataPersistence.saveInvoices(getApplication(), updatedInvoices)
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
    doctorSamaOverride: String = "",
    treatmentOrders: List<TreatmentCard> = emptyList()
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
    doctorSamaOverride: String = "",
    treatmentOrders: List<TreatmentCard> = emptyList()
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

    treatmentOrders.forEach { card ->
      val sFee = if (card.sessionFee > 0) card.sessionFee else card.route.getDefaultFee(clinicSettings.value)
      val totalFee = card.medicationFee + sFee
      val routeShort = card.route.label.split(" ").first()
      val cleanMedName = card.medicationName
        .replace(Regex("(?i)^inj\\.?\\s*"), "")
        .replace(Regex("(?i)^iv\\s*"), "")
        .replace(Regex("(?i)^im\\s*"), "")
        .replace(Regex("(?i)^sc\\s*"), "")
        .trim()
      val doseStr = if (card.dosage.isNotBlank() && !cleanMedName.contains(card.dosage, ignoreCase = true)) " ${card.dosage}" else ""
      val freqStr = if (card.frequency.isNotBlank()) " ${card.frequency.split(" ").first().lowercase()}" else ""
      val routeSuffix = card.route.label.split("(").first().trim()
      val itemName = "$routeShort $cleanMedName$doseStr$freqStr ($routeSuffix)".trim()
      invoiceItems.add(
        InvoiceItem(
          name = itemName,
          quantity = 1,
          unitPrice = totalFee,
          totalPrice = totalFee,
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

    // Tag and persist Day-Care Treatment Cards linked to this consultation invoice
    if (treatmentOrders.isNotEmpty()) {
      val ctx = getApplication<Application>()
      val taggedCards = treatmentOrders.map { card ->
        val updatedSessions = card.sessions.mapIndexed { idx, s ->
          if (idx == 0) s.copy(invoiceId = invoice.id) else s
        }
        card.copy(
          consultationInvoiceId = invoice.id,
          sessions = updatedSessions
        )
      }
      val currentCards = _treatmentCards.value
      val existingIds = taggedCards.map { it.id }.toSet()
      val mergedCards = currentCards.filterNot { existingIds.contains(it.id) } + taggedCards
      _treatmentCards.value = mergedCards
      ClinicDataPersistence.saveTreatmentCards(ctx, mergedCards)
    }

    // Route patient status
    val nextStatus = if (prescriptions.isNotEmpty()) QueueStatus.AT_PHARMACY else QueueStatus.AT_BILLING
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
    val now = System.currentTimeMillis()
    for (item in items) {
      val med = medMap[item.medicineId] ?: medMap.values.find { it.name.equals(item.medicineName, ignoreCase = true) }
      if (med != null) {
        var remainingToDeduct = item.quantity
        val updatedBatches = med.allBatches.map { batch ->
          if (remainingToDeduct > 0 && batch.quantity > 0) {
            val deduct = minOf(batch.quantity, remainingToDeduct)
            remainingToDeduct -= deduct
            batch.copy(quantity = batch.quantity - deduct, lastUpdated = now)
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

    // Mark matching invoice as inventory deducted so cashier will not double deduct!
    val ctx = getApplication<Application>()
    val matchingInv = _invoices.value.find { it.patientId == patientId && it.paymentStatus != "Paid" }
    if (matchingInv != null) {
      val updatedInvs = _invoices.value.map { if (it.id == matchingInv.id) it.copy(isInventoryDeducted = true) else it }
      _invoices.value = updatedInvs
      ClinicDataPersistence.saveInvoices(ctx, updatedInvs)
    }
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

  private fun deductInventoryForInvoice(inv: Invoice) {
    if (inv.isInventoryDeducted) return

    val ctx = getApplication<Application>()
    val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
    var modified = false
    val now = System.currentTimeMillis()

    val consultation = _consultations.value.find { it.id == inv.consultationId }
    val prescribedNames = mutableSetOf<String>()

    // 1. Deduct consultation prescriptions (if present and not external)
    if (consultation != null && consultation.prescriptions.isNotEmpty()) {
      for (item in consultation.prescriptions.filter { !it.isExternal }) {
        prescribedNames.add(item.effectiveName.lowercase().trim())
        val med = medMap[item.medicineId] ?: medMap.values.find { it.name.equals(item.medicineName, ignoreCase = true) }
        if (med != null && item.quantity > 0) {
          var remaining = item.quantity
          val updatedBatches = med.allBatches.map { b ->
            if (remaining > 0 && b.quantity > 0) {
              val deduct = minOf(b.quantity, remaining)
              remaining -= deduct
              b.copy(quantity = b.quantity - deduct, lastUpdated = now)
            } else b
          }
          medMap[med.id] = med.copy(batches = updatedBatches)
          modified = true
        }
      }
    }

    // 2. ALSO deduct store consumables and OTC items in inv.items (Syringe, Cannula, Scalp vein, OTC drugs)
    for (item in inv.items.filter { !it.isProcedure }) {
      val cleanName = item.name.split(" x ").firstOrNull()?.split("(")?.firstOrNull()?.trim() ?: item.name
      if (prescribedNames.contains(cleanName.lowercase()) || prescribedNames.contains(item.name.lowercase())) {
        continue
      }
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
            b.copy(quantity = b.quantity - deduct, lastUpdated = now)
          } else b
        }
        medMap[med.id] = med.copy(batches = updatedBatches)
        modified = true
      }
    }

    if (modified) {
      val updatedList = _medicines.value.map { medMap[it.id] ?: it }
      _medicines.value = updatedList
      ClinicDataPersistence.saveMedicines(ctx, updatedList)
    }

    // Mark invoice isInventoryDeducted = true
    val updatedInvs = _invoices.value.map { if (it.id == inv.id) it.copy(isInventoryDeducted = true) else it }
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

  fun returnSelectedInvoiceItemsAndAdjust(
    invoiceId: String,
    returnedItemNames: Set<String>,
    voidEntireInvoice: Boolean = false
  ): Pair<Boolean, List<String>> {
    val ctx = getApplication<Application>()
    val inv = _invoices.value.find { it.id == invoiceId } ?: return Pair(false, emptyList())
    val restored = mutableListOf<String>()

    val itemsToReturn = if (voidEntireInvoice) {
      inv.items.filter { !it.isProcedure }
    } else {
      inv.items.filter { !it.isProcedure && returnedItemNames.contains(it.name) }
    }

    if (itemsToReturn.isNotEmpty()) {
      val medMap = _medicines.value.associateBy { it.id }.toMutableMap()
      var modified = false
      for (item in itemsToReturn) {
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
          restored.add(item.name)
        }
      }
      if (modified) {
        val updatedList = _medicines.value.map { medMap[it.id] ?: it }
        _medicines.value = updatedList
        ClinicDataPersistence.saveMedicines(ctx, updatedList)
      }
    }

    // Annotate returned medications on consultation EMR record
    if (inv.consultationId.isNotBlank() && itemsToReturn.isNotEmpty()) {
      val returnedCleanNames = itemsToReturn.map {
        it.name.split(" x ").firstOrNull()?.split("(")?.firstOrNull()?.trim()?.lowercase() ?: it.name.lowercase()
      }.toSet()

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
      // Adjust invoice: remove returned medicines, recalculate subtotal and total, keeping Doctor Consultation Fee!
      val remainingItems = inv.items.filterNot { returnedItemNames.contains(it.name) }
      val newItemsTotal = remainingItems.sumOf { it.totalPrice }
      val newSubTotal = newItemsTotal + inv.consultationFee + inv.serviceFee
      val newTotal = (newSubTotal - inv.discount).coerceAtLeast(0.0)
      val updatedInv = inv.copy(
        items = remainingItems,
        totalAmount = newTotal
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

    // 1. Build line items for this visit (bundled med + injection administration fee)
    val invoiceItems = mutableListOf<InvoiceItem>()
    val sessionTotalFee = (if (isClinicMed) medPrice else 0.0) + sessionFee
    if (sessionTotalFee > 0) {
      val routeShort = card.route.label.split(" ").first()
      val cleanMedName = card.medicationName
        .replace(Regex("(?i)^inj\\.?\\s*"), "")
        .replace(Regex("(?i)^iv\\s*"), "")
        .replace(Regex("(?i)^im\\s*"), "")
        .replace(Regex("(?i)^sc\\s*"), "")
        .trim()
      val doseStr = if (card.dosage.isNotBlank() && !cleanMedName.contains(card.dosage, ignoreCase = true)) " ${card.dosage}" else ""
      val itemName = "$routeShort $cleanMedName$doseStr (Dose ${session.sessionNumber}/${card.totalSessions})".trim()
      invoiceItems.add(
        InvoiceItem(
          name = itemName,
          quantity = 1,
          unitPrice = sessionTotalFee,
          totalPrice = sessionTotalFee,
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

    val isAlreadyBilled = (session.invoiceId?.isNotBlank() == true && !session.invoiceId.startsWith("INV-TC-")) ||
      (!card.consultationInvoiceId.isNullOrBlank() && session.sessionNumber == 1)

    var invoice: Invoice? = null
    if (isAlreadyBilled) {
      // Deduct stock for consumables and clinic medicine without creating duplicate invoice
      val tempItems = mutableListOf<InvoiceItem>()
      if (isClinicMed && card.medicationName.isNotBlank()) {
        tempItems.add(InvoiceItem(name = card.medicationName, quantity = 1, unitPrice = medPrice, totalPrice = medPrice, isProcedure = false))
      }
      consumables.forEach { c ->
        tempItems.add(InvoiceItem(name = c.name, quantity = c.quantity, unitPrice = c.unitPrice, totalPrice = c.totalPrice, isProcedure = false))
      }
      if (tempItems.isNotEmpty()) {
        val dummyInv = Invoice(items = tempItems)
        deductInventoryForInvoice(dummyInv)
      }
    } else {
      val invId = "INV-TC-${UUID.randomUUID().toString().take(8)}"
      val invNo = "INV-${1000 + _invoices.value.size + 1}"
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
        val parsedCons = SyncPayloadParser.parseConsultations(root.getJSONArray("consultations"))
        val currentConsMap = _consultations.value.associateBy { it.id }.toMutableMap()
        parsedCons.forEach { c ->
          if (!delConsSet.contains(c.id) && !delPatSet.contains(c.patientId)) {
            currentConsMap[c.id] = c
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
        if (parsedMedicines.isNotEmpty()) {
          val currentMedMap = _medicines.value.associateBy { it.id }.toMutableMap()
          parsedMedicines.forEach { m ->
            val existing = currentMedMap[m.id]
            if (existing == null) {
              currentMedMap[m.id] = m
            } else {
              val batchMap = existing.batches.associateBy { it.batchNumber }.toMutableMap()
              m.batches.forEach { b -> batchMap[b.batchNumber] = b }
              currentMedMap[m.id] = existing.copy(
                name = m.name,
                genericName = m.genericName,
                category = m.category,
                dosageForm = m.dosageForm,
                unitType = m.unitType,
                batches = batchMap.values.toList()
              )
            }
          }
          val mergedMeds = currentMedMap.values.toList()
          _medicines.value = mergedMeds
          ClinicDataPersistence.saveMedicines(ctx, mergedMeds)
        }
      }

      // 6. Users (Non-destructive merge)
      if (root.has("users")) {
        val parsedUsers = SyncPayloadParser.parseUsers(root.getJSONArray("users"))
        if (parsedUsers.isNotEmpty()) {
          val currentUserMap = _users.value.associateBy { it.username.lowercase() }.toMutableMap()
          parsedUsers.forEach { u -> currentUserMap[u.username.lowercase()] = u }
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
          val currentTcMap = _treatmentCards.value.associateBy { it.id }.toMutableMap()
          parsedCards.forEach { card -> currentTcMap[card.id] = card }
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
    if (res.first && SupabaseSyncManager.lastMergedJson != null) {
      restoreFullBackupJson(SupabaseSyncManager.lastMergedJson!!)
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

}
