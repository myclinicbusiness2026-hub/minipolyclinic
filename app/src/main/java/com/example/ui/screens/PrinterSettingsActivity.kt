package com.example.ui.screens

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.service.printing.PrinterManager
import com.example.service.printing.PrinterSettingsStore
import kotlinx.coroutines.launch

/**
 * PrinterSettingsActivity
 *
 * Dedicated configuration UI to manage, pair, and test the XP-T361 (80mm Bluetooth Thermal Printer)
 * using the ESCPOS-ThermalPrinter-Android engine.
 */
class PrinterSettingsActivity : ComponentActivity() {

  private lateinit var printerSettingsStore: PrinterSettingsStore
  private lateinit var printerManager: PrinterManager

  private lateinit var spnPrinters: Spinner
  private lateinit var tvCurrentMacAddress: TextView
  private lateinit var tvPaperWidth: TextView
  private lateinit var tvDpi: TextView
  private lateinit var tvCharsPerLine: TextView
  private lateinit var btnRefreshDevices: Button
  private lateinit var btnSavePrinter: Button
  private lateinit var btnTestPrint: Button
  private lateinit var pbPrintProgress: ProgressBar
  private lateinit var tvPrintStatus: TextView

  private val pairedDevices = mutableListOf<PrinterManager.BluetoothPrinterDevice>()
  private lateinit var spinnerAdapter: ArrayAdapter<String>

  // Android 12+ Bluetooth runtime permission launcher
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val connectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
    } else {
      permissions[Manifest.permission.BLUETOOTH] == true
    }

    if (connectGranted) {
      loadPairedPrinters()
    } else {
      Toast.makeText(
        applicationContext,
        "Bluetooth permissions are required to detect and print to XP-T361.",
        Toast.LENGTH_LONG
      ).show()
      tvPrintStatus.text = "Permission denied: Bluetooth access is required"
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_printer_settings)

    printerSettingsStore = PrinterSettingsStore(this)
    printerManager = PrinterManager(this)

    initViews()
    setupListeners()
    displayCurrentConfig()

    checkAndRequestPermissions()
  }

  private fun initViews() {
    spnPrinters = findViewById(R.id.spnPrinters)
    tvCurrentMacAddress = findViewById(R.id.tvCurrentMacAddress)
    tvPaperWidth = findViewById(R.id.tvPaperWidth)
    tvDpi = findViewById(R.id.tvDpi)
    tvCharsPerLine = findViewById(R.id.tvCharsPerLine)
    btnRefreshDevices = findViewById(R.id.btnRefreshDevices)
    btnSavePrinter = findViewById(R.id.btnSavePrinter)
    btnTestPrint = findViewById(R.id.btnTestPrint)
    pbPrintProgress = findViewById(R.id.pbPrintProgress)
    tvPrintStatus = findViewById(R.id.tvPrintStatus)

    spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mutableListOf())
    spnPrinters.adapter = spinnerAdapter
  }

  private fun setupListeners() {
    btnRefreshDevices.setOnClickListener {
      checkAndRequestPermissions()
    }

    btnSavePrinter.setOnClickListener {
      saveSelectedPrinter()
    }

    btnTestPrint.setOnClickListener {
      executeTestPrint()
    }
  }

  private fun displayCurrentConfig() {
    val savedMac = printerSettingsStore.getPrinterMacAddress()
    val savedName = printerSettingsStore.getPrinterDeviceName()

    if (!savedMac.isNullOrBlank()) {
      tvCurrentMacAddress.text = if (!savedName.isNullOrBlank()) "$savedName ($savedMac)" else savedMac
    } else {
      tvCurrentMacAddress.text = "None configured (Select from dropdown)"
    }

    tvPaperWidth.text = "${printerSettingsStore.getPaperWidthMm().toInt()} mm"
    tvDpi.text = "${printerSettingsStore.getPrinterDpi()} DPI"
    tvCharsPerLine.text = "${printerSettingsStore.getCharsPerLine()} Chars"
  }

  /**
   * Check permissions and load paired Bluetooth devices.
   */
  private fun checkAndRequestPermissions() {
    val neededPermissions = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
      }
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
      }
    } else {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH)
      }
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
      }
    }

    if (neededPermissions.isEmpty()) {
      loadPairedPrinters()
    } else {
      requestPermissionLauncher.launch(neededPermissions.toTypedArray())
    }
  }

  /**
   * Populate paired devices into the Spinner dropdown.
   */
  private fun loadPairedPrinters() {
    pairedDevices.clear()
    val devices = printerManager.getPairedBluetoothPrinters()
    pairedDevices.addAll(devices)

    spinnerAdapter.clear()

    if (pairedDevices.isEmpty()) {
      spinnerAdapter.add("No paired Bluetooth devices found")
      tvPrintStatus.text = "No Bluetooth devices paired. Pair XP-T361 in Android settings."
    } else {
      pairedDevices.forEach { dev ->
        spinnerAdapter.add("${dev.name} [${dev.address}]")
      }
      tvPrintStatus.text = "${pairedDevices.size} paired Bluetooth device(s) detected."

      // Automatically select previously saved printer if present
      val savedMac = printerSettingsStore.getPrinterMacAddress()
      if (!savedMac.isNullOrBlank()) {
        val foundIdx = pairedDevices.indexOfFirst { it.address.equals(savedMac, ignoreCase = true) }
        if (foundIdx >= 0) {
          spnPrinters.setSelection(foundIdx)
        }
      }
    }
    spinnerAdapter.notifyDataSetChanged()
  }

  /**
   * Persist the selected printer MAC to SharedPreferences.
   */
  private fun saveSelectedPrinter() {
    if (pairedDevices.isEmpty() || spnPrinters.selectedItemPosition < 0) {
      Toast.makeText(applicationContext, "No valid Bluetooth device to save.", Toast.LENGTH_SHORT).show()
      return
    }

    val selectedDevice = pairedDevices.getOrNull(spnPrinters.selectedItemPosition)
    if (selectedDevice != null) {
      printerSettingsStore.savePrinterDevice(selectedDevice.address, selectedDevice.name)
      displayCurrentConfig()

      Toast.makeText(
        applicationContext,
        "Printer Saved: ${selectedDevice.name} (${selectedDevice.address})",
        Toast.LENGTH_SHORT
      ).show()

      tvPrintStatus.text = "Saved target: ${selectedDevice.name}"
    }
  }

  /**
   * Execute test print job asynchronously using Coroutines.
   */
  private fun executeTestPrint() {
    val selectedDevice = pairedDevices.getOrNull(spnPrinters.selectedItemPosition)
    val targetMac = selectedDevice?.address ?: printerSettingsStore.getPrinterMacAddress()

    if (targetMac.isNullOrBlank()) {
      showErrorDialog("Printer Not Selected", "Please select a paired XP-T361 printer from the dropdown first.")
      return
    }

    pbPrintProgress.visibility = View.VISIBLE
    btnTestPrint.isEnabled = false
    tvPrintStatus.text = "Connecting to XP-T361 ($targetMac)..."

    lifecycleScope.launch {
      val result = printerManager.printTestReceipt(targetMac)

      pbPrintProgress.visibility = View.GONE
      btnTestPrint.isEnabled = true

      result.onSuccess {
        tvPrintStatus.text = "Test receipt printed successfully!"
        Toast.makeText(
          applicationContext,
          "Print Job Sent Successfully to XP-T361!",
          Toast.LENGTH_LONG
        ).show()
      }.onFailure { error ->
        val errorMsg = error.message ?: "Unknown communication failure"
        tvPrintStatus.text = "Print Error: $errorMsg"
        showErrorDialog("Print Failed", "Could not complete printing to XP-T361:\n\n$errorMsg\n\nPlease ensure printer is turned ON and paired.")
      }
    }
  }

  private fun showErrorDialog(title: String, message: String) {
    AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("OK", null)
      .show()
  }
}
