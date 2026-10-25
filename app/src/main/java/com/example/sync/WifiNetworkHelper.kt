package com.example.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.*
import java.util.Enumeration

class WifiNetworkHelper(
  private val context: Context,
  private val onPeerDiscovered: (PeerDevice) -> Unit
) {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var multicastLock: WifiManager.MulticastLock? = null
  private var isListening = false
  private var listenSocket: DatagramSocket? = null

  companion object {
    const val BROADCAST_PORT = 8889
    const val HTTP_SYNC_PORT = 8888
    private const val TAG = "WifiNetworkHelper"
  }

  fun startDiscovery(myDeviceId: String, myDeviceName: String, myRole: String) {
    acquireMulticastLock()
    startUdpListener(myDeviceId)
    startUdpBroadcaster(myDeviceId, myDeviceName, myRole)
  }

  fun stopDiscovery() {
    isListening = false
    try {
      listenSocket?.close()
    } catch (_: Exception) {}
    scope.cancel()
    releaseMulticastLock()
  }

  private fun acquireMulticastLock() {
    try {
      val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      multicastLock = wifi?.createMulticastLock("ShweKhitClinicMulticastLock")?.apply {
        setReferenceCounted(true)
        acquire()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to acquire multicast lock: ${e.message}")
    }
  }

  private fun releaseMulticastLock() {
    try {
      if (multicastLock?.isHeld == true) {
        multicastLock?.release()
      }
    } catch (_: Exception) {}
  }

  fun getLocalIpAddress(): String {
    try {
      val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (iface.isLoopback || !iface.isUp) continue
        val addresses: Enumeration<InetAddress> = iface.inetAddresses
        while (addresses.hasMoreElements()) {
          val addr = addresses.nextElement()
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            val ip = addr.hostAddress ?: ""
            if (ip.isNotEmpty() && !ip.startsWith("127.")) {
              return ip
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting local IP: ${e.message}")
    }
    return "127.0.0.1"
  }

  private fun startUdpListener(myDeviceId: String) {
    if (isListening) return
    isListening = true

    scope.launch {
      try {
        val socket = DatagramSocket(null).apply {
          reuseAddress = true
          bind(InetSocketAddress(BROADCAST_PORT))
        }
        listenSocket = socket
        val buffer = ByteArray(2048)

        while (isActive && isListening) {
          val packet = DatagramPacket(buffer, buffer.size)
          try {
            socket.receive(packet)
            val jsonStr = String(packet.data, 0, packet.length)
            val json = JSONObject(jsonStr)

            val remoteId = json.optString("id")
            val remoteName = json.optString("name", "Clinic Device")
            val remotePort = json.optInt("port", HTTP_SYNC_PORT)
            val remoteRole = json.optString("role", "CLINIC_CLIENT")
            val remoteIp = packet.address.hostAddress ?: ""

            // Ignore beacon from self
            if (remoteId.isNotEmpty() && remoteId != myDeviceId && remoteIp.isNotEmpty()) {
              val peer = PeerDevice(
                id = remoteId,
                name = remoteName,
                ipAddress = remoteIp,
                port = remotePort,
                role = remoteRole,
                lastSeenMillis = System.currentTimeMillis(),
                isConnected = true
              )
              withContext(Dispatchers.Main) {
                onPeerDiscovered(peer)
              }
            }
          } catch (e: Exception) {
            if (!isListening) break
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "UDP Listener error: ${e.message}")
      }
    }
  }

  private fun startUdpBroadcaster(myDeviceId: String, myDeviceName: String, myRole: String) {
    scope.launch {
      val broadcastIp = getBroadcastAddress()
      while (isActive) {
        try {
          val myIp = getLocalIpAddress()
          if (myIp != "127.0.0.1") {
            val beaconJson = JSONObject().apply {
              put("id", myDeviceId)
              put("name", myDeviceName)
              put("port", HTTP_SYNC_PORT)
              put("role", myRole)
              put("ip", myIp)
              put("time", System.currentTimeMillis())
            }.toString()

            val bytes = beaconJson.toByteArray()
            val socket = DatagramSocket()
            socket.broadcast = true

            // Send to standard broadcast
            val packet = DatagramPacket(bytes, bytes.size, broadcastIp, BROADCAST_PORT)
            socket.send(packet)

            // Also send to 255.255.255.255 for subnet coverage
            val universalPacket = DatagramPacket(
              bytes,
              bytes.size,
              InetAddress.getByName("255.255.255.255"),
              BROADCAST_PORT
            )
            socket.send(universalPacket)

            socket.close()
          }
        } catch (e: Exception) {
          Log.e(TAG, "Broadcast error: ${e.message}")
        }
        delay(4000) // Broadcast every 4 seconds
      }
    }
  }

  private fun getBroadcastAddress(): InetAddress {
    try {
      val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (iface.isLoopback || !iface.isUp) continue
        for (interfaceAddress in iface.interfaceAddresses) {
          val broadcast = interfaceAddress.broadcast
          if (broadcast != null) return broadcast
        }
      }
    } catch (_: Exception) {}
    return InetAddress.getByName("255.255.255.255")
  }
}
