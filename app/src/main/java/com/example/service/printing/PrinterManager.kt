package com.example.service.printing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PrinterManager
 *
 * Engine to manage connection and printing to XP-T361 (80mm Bluetooth Thermal Printer)
 * using the ESCPOS-ThermalPrinter-Android library.
 */
class PrinterManager(private val context: Context) {

  private val settingsStore = PrinterSettingsStore(context)

  data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val connection: BluetoothConnection? = null
  )

  /**
   * Check whether Bluetooth permissions are granted.
   */
  fun hasBluetoothPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH_CONNECT
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  /**
   * Retrieve list of all paired Bluetooth devices.
   */
  @SuppressLint("MissingPermission")
  fun getPairedBluetoothPrinters(): List<BluetoothPrinterDevice> {
    val result = mutableListOf<BluetoothPrinterDevice>()
    if (!hasBluetoothPermission()) {
      return result
    }

    try {
      // 1. Try Dantsu BluetoothPrintersConnections list
      val connections = BluetoothPrintersConnections().list
      if (connections != null && connections.isNotEmpty()) {
        for (conn in connections) {
          val device = conn.device
          val name = device.name ?: "Bluetooth Printer"
          val address = device.address
          result.add(BluetoothPrinterDevice(name = name, address = address, connection = conn))
        }
      }
    } catch (_: Exception) {}

    // Fallback: system BluetoothAdapter bonded devices if list is empty
    if (result.isEmpty()) {
      try {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && adapter.isEnabled) {
          adapter.bondedDevices?.forEach { dev ->
            result.add(
              BluetoothPrinterDevice(
                name = dev.name ?: "POS Printer",
                address = dev.address,
                connection = BluetoothConnection(dev)
              )
            )
          }
        }
      } catch (_: Exception) {}
    }

    return result
  }

  /**
   * Find specific BluetoothConnection by MAC address.
   */
  @SuppressLint("MissingPermission")
  fun getConnectionByAddress(macAddress: String): BluetoothConnection? {
    if (!hasBluetoothPermission()) return null

    try {
      val connections = BluetoothPrintersConnections().list
      if (connections != null) {
        for (conn in connections) {
          if (conn.device.address.equals(macAddress, ignoreCase = true)) {
            return conn
          }
        }
      }
    } catch (_: Exception) {}

    // Direct initialization by BluetoothAdapter
    return try {
      val adapter = BluetoothAdapter.getDefaultAdapter()
      val device = adapter?.getRemoteDevice(macAddress)
      if (device != null) BluetoothConnection(device) else null
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Helper function: Converts text (including Myanmar Unicode script like Pyidaungsu)
   * into a crisp monochrome Bitmap so thermal printers render complex scripts correctly.
   *
   * @param text Myanmar or formatted text to render
   * @param textSizePx Font size in pixels (default 26px for 80mm paper)
   * @param widthPx Width in pixels (576px standard for 80mm 203dpi printer)
   * @param align Text alignment (Left, Center, Right)
   * @param isBold Whether to render bold typeface
   */
  fun textToBitmap(
    text: String,
    textSizePx: Float = 26f,
    widthPx: Int = 576,
    align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    isBold: Boolean = false
  ): Bitmap {
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.BLACK
      textSize = textSizePx
      typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    // Use StaticLayout for proper text wrapping and multi-line rendering
    val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      StaticLayout.Builder.obtain(text, 0, text.length, textPaint, widthPx)
        .setAlignment(align)
        .setLineSpacing(0f, 1.15f)
        .setIncludePad(true)
        .build()
    } else {
      @Suppress("DEPRECATION")
      StaticLayout(text, textPaint, widthPx, align, 1.15f, 0f, true)
    }

    val heightPx = (staticLayout.height + 12).coerceAtLeast(30)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Solid white background for thermal printer contrast
    canvas.drawColor(Color.WHITE)
    canvas.save()
    canvas.translate(0f, 6f)
    staticLayout.draw(canvas)
    canvas.restore()

    return bitmap
  }

  /**
   * Helper function to convert any arbitrary Bitmap into a hexadecimal string
   * formatted for ESC/POS `<img>` tag in ESCPOS-ThermalPrinter-Android.
   */
  fun bitmapToPrinterHex(printer: EscPosPrinter, bitmap: Bitmap): String {
    return PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap)
  }

  /**
   * Send ESC/POS print job asynchronously via Dispatchers.IO.
   *
   * @param targetMac Optional target MAC. If null, loads from PrinterSettingsStore.
   * @param formattedEscPosText Formatted ESC/POS text template with tags ([C], [L], <b>, <img>, etc.)
   * @return Result<Unit> Success or Failure with specific exception
   */
  suspend fun printAsync(
    targetMac: String? = null,
    formattedEscPosText: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val mac = targetMac ?: settingsStore.getPrinterMacAddress()
    if (mac.isNullOrBlank()) {
      return@withContext Result.failure(IllegalStateException("No printer selected. Please configure a Bluetooth printer first."))
    }

    val connection = getConnectionByAddress(mac)
      ?: return@withContext Result.failure(IllegalStateException("Printer device not found for address: $mac. Ensure Bluetooth is enabled and paired."))

    try {
      val dpi = settingsStore.getPrinterDpi()
      val paperWidth = settingsStore.getPaperWidthMm()
      val charsPerLine = settingsStore.getCharsPerLine()

      val printer = EscPosPrinter(connection, dpi, paperWidth, charsPerLine)
      printer.printFormattedTextAndCut(formattedEscPosText)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Execute a full test print on the XP-T361 printer with text, Myanmar Unicode bitmap,
   * itemized table, total, and barcode.
   */
  suspend fun printTestReceipt(targetMac: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
    val mac = targetMac ?: settingsStore.getPrinterMacAddress()
    if (mac.isNullOrBlank()) {
      return@withContext Result.failure(IllegalStateException("Please select and save a Bluetooth printer first."))
    }

    val connection = getConnectionByAddress(mac)
      ?: return@withContext Result.failure(IllegalStateException("Cannot connect to printer ($mac). Check printer power & pairing."))

    try {
      val dpi = settingsStore.getPrinterDpi()
      val paperWidth = settingsStore.getPaperWidthMm()
      val charsPerLine = settingsStore.getCharsPerLine()

      val printer = EscPosPrinter(connection, dpi, paperWidth, charsPerLine)

      // Render Myanmar script header as bitmap
      val myanmarHeaderBitmap = textToBitmap(
        text = "\"ရွှေခေတ်\" အထွေထွေရောဂါကုဆေးခန်း",
        textSizePx = 30f,
        widthPx = 576,
        align = Layout.Alignment.ALIGN_CENTER,
        isBold = true
      )
      val myanmarHeaderHex = bitmapToPrinterHex(printer, myanmarHeaderBitmap)

      // Render Myanmar custom line
      val myanmarTestLineBitmap = textToBitmap(
        text = "ကျန်းမာခြင်းသည် လာဘ်ကြီးတစ်ပါး • XP-T361 စမ်းသပ်ပုံနှိပ်ခြင်း အောင်မြင်ပါသည်",
        textSizePx = 24f,
        widthPx = 576,
        align = Layout.Alignment.ALIGN_CENTER,
        isBold = false
      )
      val myanmarLineHex = bitmapToPrinterHex(printer, myanmarTestLineBitmap)

      val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault())
      val currentDate = dateFormat.format(java.util.Date())

      // 80mm ESC/POS Formatted Receipt Template
      val receipt = StringBuilder()
        .append("[C]<img>$myanmarHeaderHex</img>\n")
        .append("[C]<b><font size='big'>SHWE KHIT CLINIC</font></b>\n")
        .append("[C]Dr. Nyan Kaung Sett M.B.,B.S (MDY)\n")
        .append("[C]ဆမ-၄၇၄၁၄ • Tel: 09-421001122\n")
        .append("[C]================================================\n")
        .append("[L]Receipt No: #TEST-20260929\n")
        .append("[L]Date: $currentDate\n")
        .append("[L]Cashier: Admin (POS Station 01)\n")
        .append("[C]------------------------------------------------\n")
        .append("[L]<b>Item Description</b>[R]<b>Qty   Price     Amount</b>\n")
        .append("[C]------------------------------------------------\n")
        .append("[L]Doctor Consultation Fee[R]1   8,000      8,000\n")
        .append("[L]Amlodipine 5mg (14 Tabs)[R]14    250      3,500\n")
        .append("[L]Paracetamol 500mg (10 T)[R]10    150      1,500\n")
        .append("[L]Minor Wound Dressing[R]1   5,000      5,000\n")
        .append("[C]------------------------------------------------\n")
        .append("[L]Subtotal:[R]18,000 MMK\n")
        .append("[L]Discount (0%):[R]0 MMK\n")
        .append("[L]<b><font size='medium'>TOTAL AMOUNT:</font></b>[R]<b><font size='medium'>18,000 MMK</font></b>\n")
        .append("[C]================================================\n")
        .append("[C]<img>$myanmarLineHex</img>\n")
        .append("[C]<b>Thank You! Please Come Again</b>\n")
        .append("[C]<barcode type='128' width='2' height='60'>TEST-2026-001</barcode>\n")
        .append("[C]<qrcode size='24'>https://shwekhitclinic.com</qrcode>\n")
        .append("[L]\n")
        .append("[L]\n")
        .toString()

      printer.printFormattedTextAndCut(receipt)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
