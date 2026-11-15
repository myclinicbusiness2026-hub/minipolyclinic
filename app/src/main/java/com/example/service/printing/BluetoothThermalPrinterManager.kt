package com.example.service.printing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

/**
 * BluetoothThermalPrinterManager
 * 
 * Provides robust connection and ESC/POS data streaming to 58mm / 80mm
 * Bluetooth POS receipt printers (Rongta, Xprinter, Sunmi, Goojprt, POS-58, etc.).
 * Standard Serial Port Profile (SPP) UUID: 00001101-0000-1000-8000-00805F9B34FB.
 */
object BluetoothThermalPrinterManager {

  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

  data class PairedPrinter(
    val name: String,
    val address: String
  )

  /**
   * Check if necessary Bluetooth permissions are granted.
   */
  fun hasBluetoothPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
    }
  }

  /**
   * Retrieve list of bonded / paired Bluetooth devices on the Android device.
   */
  @SuppressLint("MissingPermission")
  fun getPairedPrinters(context: Context): List<PairedPrinter> {
    if (!hasBluetoothPermission(context)) return emptyList()
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
    if (!adapter.isEnabled) return emptyList()

    return try {
      adapter.bondedDevices.map { device ->
        PairedPrinter(
          name = device.name ?: "Unknown POS Printer",
          address = device.address
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  /**
   * Connects to the given Bluetooth MAC address and transmits the ESC/POS binary byte array.
   */
  @SuppressLint("MissingPermission")
  suspend fun printBytes(
    context: Context,
    deviceAddress: String,
    bytes: ByteArray
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!hasBluetoothPermission(context)) {
      return@withContext Result.failure(Exception("Bluetooth ခွင့်ပြုချက် မရှိသေးပါ။ Android Settings > Apps > Shwe Khit Clinic တွင် Nearby devices ခွင့်ပြုချက် ပေးပါ"))
    }

    val adapter = BluetoothAdapter.getDefaultAdapter()
      ?: return@withContext Result.failure(Exception("ဤစက်တွင် Bluetooth hardware မပါဝင်ပါ"))

    if (!adapter.isEnabled) {
      return@withContext Result.failure(Exception("Bluetooth ပိတ်ထားပါသည်။ ကျေးဇူးပြု၍ ဖုန်း/တက်ဘလက် Settings တွင် Bluetooth ဖွင့်ပေးပါ"))
    }

    val cleanAddress = deviceAddress.trim()
    if (cleanAddress.isBlank()) {
      return@withContext Result.failure(Exception("Bluetooth ပရင်တာ MAC Address မရှိပါ။ ကျေးဇူးပြု၍ Pair လုပ်ထားသော ပရင်တာကို ရွေးချယ်ပါ"))
    }

    var socket: BluetoothSocket? = null
    var outStream: OutputStream? = null

    try {
      val device: BluetoothDevice = adapter.getRemoteDevice(cleanAddress)
      try { adapter.cancelDiscovery() } catch (_: Exception) {}

      var connected = false
      var lastError: Exception? = null

      val socketFactories: List<Pair<String, () -> BluetoothSocket>> = listOf(
        "Secure SPP" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
        "Insecure SPP" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
        "Insecure Channel 1" to {
          val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
          m.invoke(device, 1) as BluetoothSocket
        },
        "Secure Channel 1" to {
          val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
          m.invoke(device, 1) as BluetoothSocket
        }
      )

      for ((_, factory) in socketFactories) {
        try {
          socket?.close()
        } catch (_: Exception) {}
        socket = null

        try {
          val testSocket = factory()
          testSocket.connect()
          socket = testSocket
          connected = true
          break
        } catch (e: Exception) {
          lastError = e
          try {
            socket?.close()
          } catch (_: Exception) {}
          socket = null
        }
      }

      if (!connected || socket == null) {
        val detail = lastError?.localizedMessage ?: "Connection refused"
        return@withContext Result.failure(
          Exception("ပရင်တာသို့ ချိတ်ဆက်မရပါ ($cleanAddress): $detail။ ပရင်တာပါဝါဖွင့်ထားခြင်း၊ Bluetooth တွဲချိတ် (Pair) ထားခြင်းနှင့် အခြားစက်တစ်ခုခုနှင့် ချိတ်ဆက်အသုံးပြုမနေခြင်း သေချာပါစေ။")
        )
      }

      outStream = socket.outputStream
      outStream.write(bytes)
      outStream.flush()

      // Small delay to ensure printer hardware buffer finishes printing
      Thread.sleep(300)

      val printerName = try { device.name ?: cleanAddress } catch (_: Exception) { cleanAddress }
      Result.success("ဘောက်ချာ ပရင့်ထုတ်ယူပြီးပါပြီ ($printerName)")
    } catch (e: Exception) {
      Result.failure(Exception("ပရင့်ထုတ်ရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.localizedMessage}"))
    } finally {
      try {
        outStream?.close()
      } catch (_: Exception) {}
      try {
        socket?.close()
      } catch (_: Exception) {}
    }
  }
}
