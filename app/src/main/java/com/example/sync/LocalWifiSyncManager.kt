package com.example.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.example.data.ClinicDataPersistence
import com.example.model.*
import com.example.util.BurmeseDateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class LocalWifiSyncManager(
  private val context: Context,
  private val onDataUpdated: () -> Unit
) {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  val deviceId: String = "DEV-${Build.MODEL.replace(" ", "_")}-${UUID.randomUUID().toString().take(4)}"
  val deviceName: String = "Device (${Build.MODEL.ifEmpty { "Clinic Tablet" }})"
  var userRole: String = "DOCTOR"

  private val _localIp = MutableStateFlow("127.0.0.1")
  val localIp: StateFlow<String> = _localIp.asStateFlow()

  private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
  val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()

  private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
  val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

  private val _isAutoSyncEnabled = MutableStateFlow(true)
  val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

  private val _lastSyncTimestamp = MutableStateFlow("Never")
  val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

  private val _syncLogs = MutableStateFlow<List<SyncAuditEntry>>(emptyList())
  val syncLogs: StateFlow<List<SyncAuditEntry>> = _syncLogs.asStateFlow()

  private var nsdManager: NsdManager? = null
  private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
  private var nsdDiscoveryListener: NsdManager.DiscoveryListener? = null

  private val wifiHelper = WifiNetworkHelper(context) { peer ->
    registerDiscoveredPeer(peer)
  }

  private val httpServer = LocalSyncHttpServer(
    port = WifiNetworkHelper.HTTP_SYNC_PORT,
    getDeviceStatusJson = { getLocalDeviceStatusJson() },
    getDataPayloadJson = { getLocalDataPayloadJson() },
    onDataReceived = { json -> processIncomingSyncPayload(json) }
  )

  companion object {
    private const val TAG = "LocalWifiSyncManager"
    private const val SERVICE_TYPE = "_shwekhit_sync._tcp."
  }

  fun start() {
    _localIp.value = wifiHelper.getLocalIpAddress()
    httpServer.start()
    wifiHelper.startDiscovery(deviceId, deviceName, userRole)
    startNsd()
    startAutoSyncLoop()
  }

  fun stop() {
    stopNsd()
    wifiHelper.stopDiscovery()
    httpServer.stop()
    scope.cancel()
  }

  fun setAutoSyncEnabled(enabled: Boolean) {
    _isAutoSyncEnabled.value = enabled
  }

  fun addManualPeer(ip: String, port: Int = WifiNetworkHelper.HTTP_SYNC_PORT) {
    val cleanIp = ip.trim()
    if (cleanIp.isEmpty() || cleanIp == _localIp.value) return
    val peer = PeerDevice(
      id = "MANUAL-$cleanIp",
      name = "Clinic Peer ($cleanIp)",
      ipAddress = cleanIp,
      port = port,
      role = "PEER"
    )
    registerDiscoveredPeer(peer)
    scope.launch {
      syncWithPeer(peer)
    }
  }

  private fun registerDiscoveredPeer(peer: PeerDevice) {
    if (peer.ipAddress == _localIp.value) return
    val current = _discoveredPeers.value.toMutableList()
    val idx = current.indexOfFirst { it.ipAddress == peer.ipAddress || it.id == peer.id }
    if (idx >= 0) {
      current[idx] = peer.copy(lastSeenMillis = System.currentTimeMillis(), isConnected = true)
    } else {
      current.add(peer)
      addLog("Peer Discovered", "Found device '${peer.name}' at ${peer.ipAddress}", "ONLINE")
    }
    _discoveredPeers.value = current
  }

  private fun startNsd() {
    try {
      nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
      registerNsdService()
      discoverNsdServices()
    } catch (e: Exception) {
      Log.e(TAG, "NSD initialization failed: ${e.message}")
    }
  }

  private fun registerNsdService() {
    try {
      val serviceInfo = NsdServiceInfo().apply {
        serviceName = "ShweKhit_${deviceId.takeLast(4)}"
        serviceType = SERVICE_TYPE
        port = WifiNetworkHelper.HTTP_SYNC_PORT
      }
      nsdRegistrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(info: NsdServiceInfo?) {
          Log.i(TAG, "NSD Service registered: ${info?.serviceName}")
        }
        override fun onRegistrationFailed(info: NsdServiceInfo?, err: Int) {}
        override fun onServiceUnregistered(info: NsdServiceInfo?) {}
        override fun onUnregistrationFailed(info: NsdServiceInfo?, err: Int) {}
      }
      nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
    } catch (e: Exception) {
      Log.e(TAG, "NSD register error: ${e.message}")
    }
  }

  private fun discoverNsdServices() {
    try {
      nsdDiscoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
          if (serviceInfo.serviceType == SERVICE_TYPE && !serviceInfo.serviceName.contains(deviceId.takeLast(4))) {
            try {
              nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, err: Int) {}
                override fun onServiceResolved(info: NsdServiceInfo) {
                  val host = info.host?.hostAddress ?: return
                  val peer = PeerDevice(
                    id = info.serviceName,
                    name = info.serviceName,
                    ipAddress = host,
                    port = info.port,
                    role = "PEER"
                  )
                  scope.launch(Dispatchers.Main) {
                    registerDiscoveredPeer(peer)
                  }
                }
              })
            } catch (_: Exception) {}
          }
        }
        override fun onServiceLost(service: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
      }
      nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener)
    } catch (e: Exception) {
      Log.e(TAG, "NSD discover error: ${e.message}")
    }
  }

  private fun stopNsd() {
    try {
      nsdRegistrationListener?.let { nsdManager?.unregisterService(it) }
      nsdDiscoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
    } catch (_: Exception) {}
  }

  private fun startAutoSyncLoop() {
    scope.launch {
      while (isActive) {
        delay(8000)
        _localIp.value = wifiHelper.getLocalIpAddress()
        if (_isAutoSyncEnabled.value && _discoveredPeers.value.isNotEmpty()) {
          syncWithAllPeers()
        }
      }
    }
  }

  fun syncWithAllPeers() {
    scope.launch {
      val peers = _discoveredPeers.value.toList()
      for (peer in peers) {
        syncWithPeer(peer)
      }
    }
  }

  suspend fun syncWithPeer(peer: PeerDevice) {
    if (_syncStatus.value == SyncStatus.SYNCING) return
    _syncStatus.value = SyncStatus.SYNCING

    try {
      // 1. Pull data from peer
      val pullUrl = "${peer.baseUrl}/sync/pull"
      val remoteJson = httpGet(pullUrl)

      var mergeSuccess = false
      var mergeMsg = "No data"
      if (!remoteJson.isNullOrEmpty()) {
        val (ok, msg) = processIncomingSyncPayload(remoteJson)
        mergeSuccess = ok
        mergeMsg = msg
      }

      // 2. Push our updated local data to peer
      val pushUrl = "${peer.baseUrl}/sync/push"
      val myPayload = getLocalDataPayloadJson()
      val pushResp = httpPost(pushUrl, myPayload)

      _syncStatus.value = SyncStatus.SUCCESS
      _lastSyncTimestamp.value = BurmeseDateUtils.getNowFormatted()
      addLog(
        action = "Peer Sync Completed",
        details = "Merged with ${peer.name} (${peer.ipAddress}). $mergeMsg",
        status = if (mergeSuccess) "SYNCED" else "OK"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Sync error with peer ${peer.ipAddress}: ${e.message}")
      _syncStatus.value = SyncStatus.ERROR
      addLog("Sync Error", "Failed to connect to ${peer.ipAddress}: ${e.message}", "FAILED")
    } finally {
      delay(2000)
      if (_syncStatus.value == SyncStatus.SUCCESS || _syncStatus.value == SyncStatus.ERROR) {
        _syncStatus.value = SyncStatus.IDLE
      }
    }
  }

  private fun httpGet(urlStr: String): String? {
    return try {
      val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        connectTimeout = 4000
        readTimeout = 6000
        requestMethod = "GET"
      }
      if (conn.responseCode == 200) {
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          sb.append(line)
        }
        reader.close()
        conn.disconnect()
        sb.toString()
      } else {
        conn.disconnect()
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun httpPost(urlStr: String, bodyJson: String): Boolean {
    return try {
      val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        connectTimeout = 4000
        readTimeout = 6000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
      }
      val writer = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
      writer.write(bodyJson)
      writer.flush()
      writer.close()
      val code = conn.responseCode
      conn.disconnect()
      code == 200
    } catch (e: Exception) {
      false
    }
  }

  private fun getLocalDeviceStatusJson(): String {
    return JSONObject().apply {
      put("deviceId", deviceId)
      put("deviceName", deviceName)
      put("role", userRole)
      put("ip", _localIp.value)
      put("timestamp", System.currentTimeMillis())
      put("version", "1.0")
    }.toString()
  }

  fun getLocalDataPayloadJson(): String {
    val deletedPatients = ClinicDataPersistence.loadDeletedPatientIds(context).toList()
    val deletedConsultations = ClinicDataPersistence.loadDeletedConsultationIds(context).toList()
    val deletedInvoices = ClinicDataPersistence.loadDeletedInvoiceIds(context).toList()
    val deletedMedicines = ClinicDataPersistence.loadDeletedMedicineIds(context).toList()
    val patients = (ClinicDataPersistence.loadPatients(context) ?: emptyList())
      .filter { !deletedPatients.contains(it.id) }
    val consultations = (ClinicDataPersistence.loadConsultations(context) ?: emptyList())
      .filter { !deletedConsultations.contains(it.id) && !deletedPatients.contains(it.patientId) }
    val invoices = (ClinicDataPersistence.loadInvoices(context) ?: emptyList())
      .filter { !deletedInvoices.contains(it.id) && !deletedPatients.contains(it.patientId) }
    val medicines = (ClinicDataPersistence.loadMedicines(context) ?: emptyList())
      .filter { !deletedMedicines.contains(it.id) }
    val users = ClinicDataPersistence.loadUsers(context) ?: emptyList()

    val payload = SyncDataPayload(
      deviceId = deviceId,
      deviceName = deviceName,
      timestamp = System.currentTimeMillis(),
      patients = patients,
      consultations = consultations,
      invoices = invoices,
      medicines = medicines,
      users = users,
      deletedPatientIds = deletedPatients,
      deletedConsultationIds = deletedConsultations,
      deletedInvoiceIds = deletedInvoices,
      deletedMedicineIds = deletedMedicines
    )
    return payload.toJsonString()
  }

  fun processIncomingSyncPayload(jsonStr: String): Pair<Boolean, String> {
    return try {
      val root = JSONObject(jsonStr)
      val remoteDeletedPatientIds = SyncPayloadParser.parseStringList(root.optJSONArray("deletedPatientIds"))
      val remoteDeletedConsultationIds = SyncPayloadParser.parseStringList(root.optJSONArray("deletedConsultationIds"))
      val remoteDeletedInvoiceIds = SyncPayloadParser.parseStringList(root.optJSONArray("deletedInvoiceIds"))
      val remoteDeletedMedicineIds = SyncPayloadParser.parseStringList(root.optJSONArray("deletedMedicineIds"))

      // Update deleted tombstones locally
      val localDeletedPatientIds = ClinicDataPersistence.loadDeletedPatientIds(context)
      val allDeletedPatientIds = (localDeletedPatientIds + remoteDeletedPatientIds).toMutableSet()
      ClinicDataPersistence.saveDeletedPatientIds(context, allDeletedPatientIds)

      val localDeletedConsultationIds = ClinicDataPersistence.loadDeletedConsultationIds(context)
      val allDeletedConsultationIds = (localDeletedConsultationIds + remoteDeletedConsultationIds).toMutableSet()
      ClinicDataPersistence.saveDeletedConsultationIds(context, allDeletedConsultationIds)

      val localDeletedInvoiceIds = ClinicDataPersistence.loadDeletedInvoiceIds(context)
      val allDeletedInvoiceIds = (localDeletedInvoiceIds + remoteDeletedInvoiceIds).toMutableSet()
      ClinicDataPersistence.saveDeletedInvoiceIds(context, allDeletedInvoiceIds)

      val localDeletedMedicineIds = ClinicDataPersistence.loadDeletedMedicineIds(context)
      val allDeletedMedicineIds = (localDeletedMedicineIds + remoteDeletedMedicineIds).toMutableSet()
      ClinicDataPersistence.saveDeletedMedicineIds(context, allDeletedMedicineIds)

      val remotePatients = SyncPayloadParser.parsePatients(root.optJSONArray("patients"))
      val remoteConsultations = SyncPayloadParser.parseConsultations(root.optJSONArray("consultations"))
      val remoteInvoices = SyncPayloadParser.parseInvoices(root.optJSONArray("invoices"))
      val remoteMedicines = SyncPayloadParser.parseMedicines(root.optJSONArray("medicines"))
      val remoteUsers = SyncPayloadParser.parseUsers(root.optJSONArray("users"))

      // Merge Patients (UUID Upsert & status progression)
      val currentPatients = ClinicDataPersistence.loadPatients(context) ?: emptyList()
      // Filter out any locally existing patients that were deleted either locally or remotely!
      val patientMap = currentPatients
        .filter { !allDeletedPatientIds.contains(it.id) }
        .associateBy { it.id }
        .toMutableMap()

      var newPatCount = 0
      remotePatients.forEach { rp ->
        // CRITICAL: Skip any patient that was deleted!
        if (allDeletedPatientIds.contains(rp.id)) {
          return@forEach
        }
        val existing = patientMap[rp.id]
        if (existing == null) {
          patientMap[rp.id] = rp
          newPatCount++
        } else {
          // Status progression: COMPLETED > AT_BILLING > AT_PHARMACY > IN_CONSULTATION > WAITING
          val higherStatus = if (rp.status.ordinal >= existing.status.ordinal) rp.status else existing.status
          patientMap[rp.id] = existing.copy(
            status = higherStatus,
            vitals = if (rp.vitals != VitalSigns()) rp.vitals else existing.vitals,
            notes = if (rp.notes.isNotBlank()) rp.notes else existing.notes
          )
        }
      }
      val mergedPatients = patientMap.values.sortedBy { it.tokenNumber }
      ClinicDataPersistence.savePatients(context, mergedPatients)

      // Merge Consultations
      val currentConsultations = ClinicDataPersistence.loadConsultations(context) ?: emptyList()
      val consMap = currentConsultations
        .filter { !allDeletedConsultationIds.contains(it.id) && !allDeletedPatientIds.contains(it.patientId) }
        .associateBy { it.id }
        .toMutableMap()

      var newConsCount = 0
      remoteConsultations.forEach { rc ->
        if (!allDeletedConsultationIds.contains(rc.id) && !allDeletedPatientIds.contains(rc.patientId)) {
          if (!consMap.containsKey(rc.id)) {
            consMap[rc.id] = rc
            newConsCount++
          }
        }
      }
      val mergedConsultations = consMap.values.toList()
      ClinicDataPersistence.saveConsultations(context, mergedConsultations)

      // Merge Invoices
      val currentInvoices = ClinicDataPersistence.loadInvoices(context) ?: emptyList()
      val invMap = currentInvoices
        .filter { !allDeletedInvoiceIds.contains(it.id) && !allDeletedPatientIds.contains(it.patientId) }
        .associateBy { it.id }
        .toMutableMap()

      var newInvCount = 0
      remoteInvoices.forEach { ri ->
        if (!allDeletedInvoiceIds.contains(ri.id) && !allDeletedPatientIds.contains(ri.patientId)) {
          val ex = invMap[ri.id]
          if (ex == null) {
            invMap[ri.id] = ri
            newInvCount++
          } else if (ri.paymentStatus == "Paid" && ex.paymentStatus != "Paid") {
            invMap[ri.id] = ri
          }
        }
      }
      val mergedInvoices = invMap.values.toList()
      ClinicDataPersistence.saveInvoices(context, mergedInvoices)

      // Merge Medicines (Batch reconciliation & inventory catalog sync)
      val currentMedicines = ClinicDataPersistence.loadMedicines(context) ?: emptyList()
      val medMap = currentMedicines
        .filter { !allDeletedMedicineIds.contains(it.id) }
        .associateBy { it.id }
        .toMutableMap()
      var newMedCount = 0
      remoteMedicines.forEach { rm ->
        if (allDeletedMedicineIds.contains(rm.id)) {
          return@forEach
        }
        val existing = medMap[rm.id] ?: medMap.values.find { it.name.equals(rm.name, ignoreCase = true) }
        if (existing == null) {
          medMap[rm.id] = rm
          newMedCount++
        } else {
          val batchMap = existing.batches.associateBy { it.batchNumber }.toMutableMap()
          rm.batches.forEach { rb ->
            val exBatch = batchMap[rb.batchNumber]
            if (exBatch == null) {
              batchMap[rb.batchNumber] = rb
            } else {
              val reconciledQty = if (rb.quantity < exBatch.quantity) rb.quantity else exBatch.quantity
              batchMap[rb.batchNumber] = exBatch.copy(quantity = reconciledQty)
            }
          }
          medMap[existing.id] = existing.copy(batches = batchMap.values.toList())
        }
      }
      val mergedMedicines = medMap.values.toList()
      ClinicDataPersistence.saveMedicines(context, mergedMedicines)

      // Merge Users (Staff accounts created by Admin on any station)
      val currentUsers = ClinicDataPersistence.loadUsers(context) ?: emptyList()
      val userMap = currentUsers.associateBy { it.id }.toMutableMap()
      var newUserCount = 0
      remoteUsers.forEach { ru ->
        val exUser = userMap[ru.id] ?: userMap.values.find { it.username.equals(ru.username, ignoreCase = true) }
        if (exUser == null) {
          userMap[ru.id] = ru
          newUserCount++
        } else {
          userMap[exUser.id] = exUser.copy(
            fullName = ru.fullName.ifBlank { exUser.fullName },
            role = ru.role,
            pinCode = ru.pinCode,
            passwordHash = ru.passwordHash,
            samaNumber = ru.samaNumber.ifBlank { exUser.samaNumber },
            degree = ru.degree.ifBlank { exUser.degree },
            isActive = ru.isActive
          )
        }
      }
      val mergedUsers = userMap.values.toList()
      ClinicDataPersistence.saveUsers(context, mergedUsers)

      scope.launch(Dispatchers.Main) {
        onDataUpdated()
      }

      Pair(true, "Merged +$newPatCount pat, +$newConsCount cons, +$newInvCount inv, +$newMedCount med, +$newUserCount users (Purged: ${allDeletedPatientIds.size})")
    } catch (e: Exception) {
      Log.e(TAG, "Merge error: ${e.message}")
      Pair(false, "Merge error: ${e.message}")
    }
  }

  private fun addLog(action: String, details: String, status: String) {
    val entry = SyncAuditEntry(
      timestamp = BurmeseDateUtils.getNowFormatted(),
      peerName = "Wi-Fi Peer",
      peerIp = _localIp.value,
      patientsMerged = 0,
      consultationsMerged = 0,
      invoicesMerged = 0,
      medicinesMerged = 0,
      status = status,
      message = "$action: $details"
    )
    _syncLogs.value = listOf(entry) + _syncLogs.value.take(25)
  }
}
