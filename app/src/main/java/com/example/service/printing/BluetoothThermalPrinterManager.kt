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
      return@withContext Result.failure(Exception("Bluetooth permission not granted"))
    }

    val adapter = BluetoothAdapter.getDefaultAdapter()
      ?: return@withContext Result.failure(Exception("Bluetooth hardware not available on this device"))

    if (!adapter.isEnabled) {
      return@withContext Result.failure(Exception("Bluetooth is turned off. Please turn on Bluetooth in Android Settings."))
    }

    var socket: BluetoothSocket? = null
    var outStream: OutputStream? = null

    try {
      val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
      adapter.cancelDiscovery() // Cancel discovery to prevent slow connection

      // Standard RFCOMM socket
      socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      socket.connect()

      outStream = socket.outputStream
      outStream.write(bytes)
      outStream.flush()

      // Small delay to ensure printer buffer completes
      Thread.sleep(300)

      Result.success("Receipt printed successfully on ${device.name ?: deviceAddress}")
    } catch (e: Exception) {
      e.printStackTrace()
      // Fallback: try fallback reflection method for stubborn POS printers
      try {
        val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        val fallbackSocket = m.invoke(device, 1) as BluetoothSocket
        fallbackSocket.connect()
        val stream = fallbackSocket.outputStream
        stream.write(bytes)
        stream.flush()
        Thread.sleep(300)
        fallbackSocket.close()
        Result.success("Receipt printed successfully (Fallback channel)")
      } catch (fallbackError: Exception) {
        fallbackError.printStackTrace()
        Result.failure(Exception("Cannot connect to printer ($deviceAddress): ${e.localizedMessage}"))
      }
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
