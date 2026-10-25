package com.example.service.printing

import android.content.Context
import android.content.SharedPreferences

/**
 * PrinterSettingsStore
 *
 * Manages persistence of Bluetooth thermal printer configurations for XP-T361 (80mm)
 * via Android SharedPreferences.
 */
class PrinterSettingsStore(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  companion object {
    private const val PREF_NAME = "xp_t361_printer_preferences"
    private const val KEY_MAC_ADDRESS = "key_printer_mac_address"
    private const val KEY_DEVICE_NAME = "key_printer_device_name"
    private const val KEY_PAPER_WIDTH_MM = "key_printer_paper_width_mm"
    private const val KEY_PRINTER_DPI = "key_printer_dpi"
    private const val KEY_CHARS_PER_LINE = "key_printer_chars_per_line"

    // Defaults specifically tuned for XP-T361 80mm Bluetooth Thermal Printer
    const val DEFAULT_PAPER_WIDTH_MM = 80f
    const val DEFAULT_PRINTER_DPI = 203
    const val DEFAULT_CHARS_PER_LINE = 48
  }

  /**
   * Save selected printer device details.
   */
  fun savePrinterDevice(macAddress: String, deviceName: String = "") {
    prefs.edit()
      .putString(KEY_MAC_ADDRESS, macAddress)
      .putString(KEY_DEVICE_NAME, deviceName)
      .apply()
  }

  /**
   * Get configured printer MAC address.
   */
  fun getPrinterMacAddress(): String? {
    return prefs.getString(KEY_MAC_ADDRESS, null)
  }

  /**
   * Get configured printer name.
   */
  fun getPrinterDeviceName(): String? {
    return prefs.getString(KEY_DEVICE_NAME, null)
  }

  /**
   * Save printer hardware specifications.
   */
  fun savePrinterSpecs(paperWidthMm: Float, dpi: Int, charsPerLine: Int) {
    prefs.edit()
      .putFloat(KEY_PAPER_WIDTH_MM, paperWidthMm)
      .putInt(KEY_PRINTER_DPI, dpi)
      .putInt(KEY_CHARS_PER_LINE, charsPerLine)
      .apply()
  }

  fun getPaperWidthMm(): Float = prefs.getFloat(KEY_PAPER_WIDTH_MM, DEFAULT_PAPER_WIDTH_MM)

  fun getPrinterDpi(): Int = prefs.getInt(KEY_PRINTER_DPI, DEFAULT_PRINTER_DPI)

  fun getCharsPerLine(): Int = prefs.getInt(KEY_CHARS_PER_LINE, DEFAULT_CHARS_PER_LINE)

  fun clear() {
    prefs.edit().clear().apply()
  }
}
