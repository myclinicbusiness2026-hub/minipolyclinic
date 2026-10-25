package com.example.service.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Discovered peer device on local Wi-Fi router LAN
 */
data class DiscoveredPeer(
  val ip: String,
  val port: Int = 8080,
  val deviceName: String = "Shwe Khit Station",
  val role: String = "Clinic Staff",
  val lastSeenMs: Long = System.currentTimeMillis()
)

/**
 * LocalWifiSyncService
 *
 * Enables 100% offline peer-to-peer synchronization across Android devices
 * (Doctor's tablet, Reception desk, Cashier/Pharmacy station) connected to the same Wi-Fi router.
 * Features:
 * 1. Zero-Config UDP Auto-Discovery (no manual IP typing needed)
 * 2. Bi-directional HTTP Micro-server on port 8080
 * 3. Smart record-level union (patients, consultations, invoices, users)
 * 4. Automatic periodic & event-driven background sync
 */
object LocalWifiSyncService {
  private const val TAG = "LocalWifiSync"
  private const val DISCOVERY_PORT = 8888
  private const val DEFAULT_HTTP_PORT = 8080

  private var serverSocket: ServerSocket? = null
  private var serverJob: Job? = null
  private var discoveryJob: Job? = null
  private var udpSocket: DatagramSocket? = null
  private var multicastLock: WifiManager.MulticastLock? = null
  private var isRunning = false

  private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
  val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

  // Callback to retrieve latest clinic JSON from ViewModel/Persistence
  var currentDataSupplier: (() -> String)? = null
  // Callback to merge incoming JSON data into ViewModel/Persistence
  var onDataReceivedToMerge: ((String) -> Pair<Boolean, String>)? = null

  // Device identity info for peer announcements
  var currentDeviceName: String = "Doctor Station"
  var currentDeviceRole: String = "Doctor"

  /**
   * Determine device's IPv4 address on the current local Wi-Fi / hotspot LAN.
   */
  fun getLocalIpAddress(): String {
    try {
      val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
      for (intf in interfaces) {
        val name = intf.name.lowercase()
        if (intf.isUp && (name.contains("wlan") || name.contains("ap") || name.contains("eth") || name.contains("wifi") || name.contains("rndis"))) {
          val addrs = Collections.list(intf.inetAddresses)
          for (addr in addrs) {
            if (!addr.isLoopbackAddress && addr is Inet4Address) {
              val ip = addr.hostAddress ?: ""
              if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                return ip
              }
            }
          }
        }
      }

      for (intf in interfaces) {
        if (!intf.isUp) continue
        val addrs = Collections.list(intf.inetAddresses)
        for (addr in addrs) {
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            val ip = addr.hostAddress ?: ""
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
              return ip
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error obtaining local IP: ${e.message}")
    }
    return "127.0.0.1"
  }

  fun isHubRunning(): Boolean = isRunning

  /**
   * Start the Wi-Fi Sync Hub (HTTP Micro-Server) on this device.
   */
  @Synchronized
  fun startHub(port: Int = DEFAULT_HTTP_PORT, onStarted: ((Boolean, String) -> Unit)? = null) {
    if (isRunning) {
      onStarted?.invoke(true, "Hub is already running on ${getLocalIpAddress()}:$port")
      return
    }

    try {
      serverSocket = ServerSocket(port).apply {
        reuseAddress = true
      }
      isRunning = true
      val localIp = getLocalIpAddress()

      serverJob = CoroutineScope(Dispatchers.IO).launch {
        Log.i(TAG, "Wi-Fi Sync Hub listening on $localIp:$port")
        while (isActive && isRunning) {
          try {
            val client = serverSocket?.accept() ?: break
            launch {
              handleClientConnection(client)
            }
          } catch (e: SocketException) {
            break
          } catch (e: Exception) {
            Log.e(TAG, "Error accepting client: ${e.message}")
          }
        }
      }
      onStarted?.invoke(true, "Wi-Fi Hub စတင်ပါပြီ: http://$localIp:$port")
    } catch (e: Exception) {
      isRunning = false
      Log.e(TAG, "Failed to start hub: ${e.message}")
      onStarted?.invoke(false, "Hub စတင်၍မရပါ (Port $port): ${e.message}")
    }
  }

  /**
   * Stop the Wi-Fi Sync Hub.
   */
  @Synchronized
  fun stopHub() {
    isRunning = false
    try {
      serverJob?.cancel()
      serverJob = null
      serverSocket?.close()
      serverSocket = null
      Log.i(TAG, "Wi-Fi Sync Hub stopped")
    } catch (e: Exception) {
      Log.e(TAG, "Error closing hub socket: ${e.message}")
    }
  }

  /**
   * Start Zero-Config UDP Peer Discovery.
   * Periodically broadcasts discovery beacon and listens for peers on the Wi-Fi LAN.
   */
  fun startDiscovery(context: Context) {
    stopDiscovery()

    try {
      val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      multicastLock = wifiManager?.createMulticastLock("ShweKhitSyncMulticastLock")?.apply {
        setReferenceCounted(true)
        acquire()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
    }

    discoveryJob = CoroutineScope(Dispatchers.IO).launch {
      // 1. Start UDP Listener
      launch {
        try {
          udpSocket = DatagramSocket(null).apply {
            reuseAddress = true
            bind(InetSocketAddress(DISCOVERY_PORT))
            broadcast = true
          }
          val buffer = ByteArray(1024)
          while (isActive) {
            try {
              val packet = DatagramPacket(buffer, buffer.size)
              udpSocket?.receive(packet)
              val message = String(packet.data, 0, packet.length).trim()
              val senderIp = packet.address.hostAddress ?: ""
              val localIp = getLocalIpAddress()

              // Ignore packets from self
              if (senderIp != localIp && senderIp != "127.0.0.1" && message.startsWith("SHWEKHIT_SYNC_DISCOVER|")) {
                val parts = message.split("|")
                val peerPort = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_HTTP_PORT
                val peerName = parts.getOrNull(2) ?: "Clinic Peer"
                val peerRole = parts.getOrNull(3) ?: "Staff"

                val updatedPeer = DiscoveredPeer(
                  ip = senderIp,
                  port = peerPort,
                  deviceName = peerName,
                  role = peerRole,
                  lastSeenMs = System.currentTimeMillis()
                )

                val current = _discoveredPeers.value.toMutableList()
                val idx = current.indexOfFirst { it.ip == senderIp }
                if (idx != -1) {
                  current[idx] = updatedPeer
                } else {
                  current.add(updatedPeer)
                }
                _discoveredPeers.value = current
              }
            } catch (_: SocketException) {
              break
            } catch (e: Exception) {
              Log.d(TAG, "Discovery receive error: ${e.message}")
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "UDP listener failed: ${e.message}")
        }
      }

      // 2. Periodic Broadcast Transmitter (every 4 seconds)
      launch {
        while (isActive) {
          try {
            val localIp = getLocalIpAddress()
            if (localIp != "127.0.0.1") {
              val beaconMsg = "SHWEKHIT_SYNC_DISCOVER|$DEFAULT_HTTP_PORT|$currentDeviceName|$currentDeviceRole|${System.currentTimeMillis()}"
              val data = beaconMsg.toByteArray(Charsets.UTF_8)
              
              val socket = DatagramSocket()
              socket.broadcast = true
              
              // Broadcast to universal broadcast address
              val packetAll = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT)
              socket.send(packetAll)

              // Also broadcast to subnet broadcast address (e.g. 192.168.1.255)
              val subnetParts = localIp.split(".")
              if (subnetParts.size == 4) {
                val subnetBroadcast = "${subnetParts[0]}.${subnetParts[1]}.${subnetParts[2]}.255"
                val packetSubnet = DatagramPacket(data, data.size, InetAddress.getByName(subnetBroadcast), DISCOVERY_PORT)
                socket.send(packetSubnet)
              }
              socket.close()
            }

            // Prune dead peers (not seen for > 20 seconds)
            val now = System.currentTimeMillis()
            val fresh = _discoveredPeers.value.filter { now - it.lastSeenMs < 20000 }
            if (fresh.size != _discoveredPeers.value.size) {
              _discoveredPeers.value = fresh
            }
          } catch (e: Exception) {
            Log.d(TAG, "Discovery broadcast error: ${e.message}")
          }
          delay(4000)
        }
      }
    }
  }

  fun stopDiscovery() {
    try {
      discoveryJob?.cancel()
      discoveryJob = null
      udpSocket?.close()
      udpSocket = null
      multicastLock?.let {
        if (it.isHeld) it.release()
      }
      multicastLock = null
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping discovery: ${e.message}")
    }
  }

  private var isScanningSubnet = false

  /**
   * Fast parallel TCP subnet sweep on port 8080.
   * Completely bypasses UDP broadcast router blocks/AP isolation.
   */
  suspend fun scanSubnetForPeers(timeoutMs: Int = 350): List<DiscoveredPeer> = withContext(Dispatchers.IO) {
    if (isScanningSubnet) return@withContext _discoveredPeers.value
    isScanningSubnet = true
    try {
      val localIp = getLocalIpAddress()
      if (localIp == "127.0.0.1") return@withContext emptyList()
      val parts = localIp.split(".")
      if (parts.size != 4) return@withContext emptyList()
      val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
      val myLastOctet = parts[3].toIntOrNull() ?: -1

      val found = mutableListOf<DiscoveredPeer>()
      val candidateOctets = (1..254).filter { it != myLastOctet }

      // Scan in parallel batches of 32 to avoid excessive socket concurrency
      candidateOctets.chunked(32).forEach { chunk ->
        val jobs = chunk.map { octet ->
          async {
            val targetIp = "$prefix$octet"
            var socket: Socket? = null
            try {
              socket = Socket()
              socket.connect(InetSocketAddress(targetIp, DEFAULT_HTTP_PORT), timeoutMs)
              socket.soTimeout = timeoutMs + 300
              val writer = OutputStreamWriter(socket.getOutputStream(), "UTF-8")
              writer.write("GET /api/status HTTP/1.1\r\nHost: $targetIp\r\nConnection: close\r\n\r\n")
              writer.flush()
              val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
              val statusLine = reader.readLine()
              if (statusLine != null && statusLine.contains("200")) {
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                  line = reader.readLine()
                }
                val body = reader.readText()
                val json = JSONObject(body)
                val deviceName = json.optString("device", "Clinic Peer")
                val role = json.optString("role", "Staff")
                DiscoveredPeer(
                  ip = targetIp,
                  port = DEFAULT_HTTP_PORT,
                  deviceName = deviceName,
                  role = role,
                  lastSeenMs = System.currentTimeMillis()
                )
              } else null
            } catch (_: Exception) {
              null
            } finally {
              try { socket?.close() } catch (_: Exception) {}
            }
          }
        }
        val batchResults = jobs.awaitAll().filterNotNull()
        found.addAll(batchResults)
      }

      if (found.isNotEmpty()) {
        val current = _discoveredPeers.value.toMutableList()
        found.forEach { peer ->
          val idx = current.indexOfFirst { it.ip == peer.ip }
          if (idx != -1) {
            current[idx] = peer
          } else {
            current.add(peer)
          }
        }
        _discoveredPeers.value = current
      }
      return@withContext _discoveredPeers.value
    } catch (e: Exception) {
      Log.e(TAG, "Subnet scan error: ${e.message}")
      return@withContext _discoveredPeers.value
    } finally {
      isScanningSubnet = false
    }
  }

  private fun handleClientConnection(socket: Socket) {
    try {
      socket.soTimeout = 12000
      val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
      val outputStream = OutputStreamWriter(socket.getOutputStream(), "UTF-8")

      val requestLine = inputStream.readLine() ?: return
      val parts = requestLine.split(" ")
      val method = if (parts.isNotEmpty()) parts[0] else "GET"
      val path = if (parts.size > 1) parts[1] else "/"

      var contentLength = 0
      var line: String? = inputStream.readLine()
      while (!line.isNullOrEmpty()) {
        if (line.lowercase().startsWith("content-length:")) {
          contentLength = line.substring(15).trim().toIntOrNull() ?: 0
        }
        line = inputStream.readLine()
      }

      var body = ""
      if (contentLength > 0) {
        val charArray = CharArray(contentLength)
        var totalRead = 0
        while (totalRead < contentLength) {
          val read = inputStream.read(charArray, totalRead, contentLength - totalRead)
          if (read == -1) break
          totalRead += read
        }
        body = String(charArray, 0, totalRead)
      }

      val responseJson = when {
        path.startsWith("/api/status") -> {
          JSONObject().apply {
            put("status", "ONLINE")
            put("device", currentDeviceName)
            put("role", currentDeviceRole)
            put("ip", getLocalIpAddress())
            put("time", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
          }.toString()
        }
        path.startsWith("/api/sync/pull") -> {
          currentDataSupplier?.invoke() ?: "{}"
        }
        path.startsWith("/api/sync/push") && method == "POST" -> {
          val mergeResult = onDataReceivedToMerge?.invoke(body) ?: Pair(false, "No merge handler")
          JSONObject().apply {
            put("success", mergeResult.first)
            put("message", mergeResult.second)
          }.toString()
        }
        path.startsWith("/api/sync/bidirectional") && method == "POST" -> {
          // 1. Merge incoming client data into local store using smart record union
          val mergeResult = onDataReceivedToMerge?.invoke(body) ?: Pair(false, "No merge handler")
          // 2. Supply updated local data back to client
          val updatedLocalData = currentDataSupplier?.invoke() ?: "{}"
          JSONObject().apply {
            put("success", mergeResult.first)
            put("mergeMessage", mergeResult.second)
            put("hubData", JSONObject(updatedLocalData))
          }.toString()
        }
        else -> {
          JSONObject().apply {
            put("status", "READY")
            put("message", "Shwe Khit Wi-Fi Peer Sync Endpoint Active")
          }.toString()
        }
      }

      val responseBytes = responseJson.toByteArray(Charsets.UTF_8)
      outputStream.write("HTTP/1.1 200 OK\r\n")
      outputStream.write("Content-Type: application/json; charset=UTF-8\r\n")
      outputStream.write("Content-Length: ${responseBytes.size}\r\n")
      outputStream.write("Connection: close\r\n")
      outputStream.write("\r\n")
      outputStream.flush()
      socket.getOutputStream().write(responseBytes)
      socket.getOutputStream().flush()
    } catch (e: Exception) {
      Log.e(TAG, "Error handling client request: ${e.message}")
    } finally {
      try { socket.close() } catch (_: Exception) {}
    }
  }

  // --- CLIENT ACTIONS ---

  /**
   * Test connection to a remote Wi-Fi Sync Hub.
   */
  suspend fun testHubConnection(hostIp: String, port: Int = DEFAULT_HTTP_PORT): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val raw = hostIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
    val parts = raw.split(":")
    val cleanHost = parts.getOrNull(0)?.trim() ?: ""
    val targetPort = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: port else port

    if (cleanHost.isBlank()) {
      return@withContext Pair(false, "Hub IP Address ထည့်သွင်းပါ (ဥပမာ: 192.168.1.50)")
    }

    try {
      val url = URL("http://$cleanHost:$targetPort/api/status")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 4000
        readTimeout = 4000
        requestMethod = "GET"
      }
      val code = conn.responseCode
      if (code == 200) {
        val resp = conn.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(resp)
        val dev = obj.optString("device", "Shwe Khit Hub")
        Pair(true, "ချိတ်ဆက်မှု အောင်မြင်ပါသည်: $dev ($cleanHost:$targetPort)")
      } else {
        Pair(false, "ချိတ်ဆက်၍ မရပါ (HTTP $code)")
      }
    } catch (e: Exception) {
      Pair(false, "ချိတ်ဆက်၍ မရပါ: ${e.message ?: "Connection timeout"}")
    }
  }

  /**
   * Perform bidirectional sync with a specific peer.
   * Sends local changes, peer merges them, and peer returns its unified clinic data.
   */
  suspend fun performBidirectionalSync(
    hostIp: String,
    port: Int = DEFAULT_HTTP_PORT,
    localDataJson: String
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val raw = hostIp.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
    val parts = raw.split(":")
    val cleanHost = parts.getOrNull(0)?.trim() ?: ""
    val targetPort = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: port else port

    if (cleanHost.isBlank()) {
      return@withContext Pair(false, "IP Address မမှန်ကန်ပါ")
    }

    try {
      val url = URL("http://$cleanHost:$targetPort/api/sync/bidirectional")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 6000
        readTimeout = 14000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
      }

      OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
        writer.write(localDataJson)
        writer.flush()
      }

      val code = conn.responseCode
      if (code == 200) {
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val responseObj = JSONObject(responseText)
        val hubDataObj = responseObj.optJSONObject("hubData")
        if (hubDataObj != null) {
          // Merge peer's data into local store
          onDataReceivedToMerge?.invoke(hubDataObj.toString())
          Pair(true, "Wi-Fi Router Sync အောင်မြင်ပါသည် ($cleanHost)")
        } else {
          Pair(true, "ဒေတာ ပေးပို့ပြီးပါပြီ ($cleanHost)")
        }
      } else {
        Pair(false, "Wi-Fi Sync မအောင်မြင်ပါ (HTTP $code)")
      }
    } catch (e: Exception) {
      Pair(false, "Wi-Fi Sync ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
    }
  }

  /**
   * Sync with all auto-discovered peers on the Wi-Fi LAN plus any configured manual host IP.
   */
  suspend fun syncWithAllPeers(
    manualHostIp: String? = null,
    localDataJson: String
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val targets = mutableListOf<String>()
    if (!manualHostIp.isNullOrBlank()) {
      targets.add(manualHostIp.trim())
    }
    _discoveredPeers.value.forEach { peer ->
      if (!targets.contains(peer.ip)) {
        targets.add(peer.ip)
      }
    }

    if (targets.isEmpty()) {
      // Auto-fallback: router might block UDP broadcasts, so perform quick subnet sweep
      val swept = scanSubnetForPeers(timeoutMs = 280)
      swept.forEach { p ->
        if (!targets.contains(p.ip)) targets.add(p.ip)
      }
    }

    if (targets.isEmpty()) {
      return@withContext Pair(false, "တူညီသော Wi-Fi တွင် အခြားစက် (Peer) မတွေ့ရှိသေးပါ")
    }

    var successCount = 0
    val messages = mutableListOf<String>()

    for (targetIp in targets) {
      val res = performBidirectionalSync(targetIp, DEFAULT_HTTP_PORT, localDataJson)
      if (res.first) {
        successCount++
        messages.add(res.second)
      }
    }

    if (successCount > 0) {
      Pair(true, "Wi-Fi အချင်းချင်း ချိတ်ဆက်မှု အောင်မြင်ပါသည် ($successCount စက်နှင့် Sync ပြီး)")
    } else {
      Pair(false, "စက်များနှင့် ချိတ်ဆက်၍ မရသေးပါ")
    }
  }
}
