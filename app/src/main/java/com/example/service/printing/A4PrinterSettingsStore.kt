package com.example.service.printing

import android.content.Context
import android.content.SharedPreferences

/**
 * A4PrinterSettingsStore
 *
 * Manages persistence of A4 Clinical Medical Record & Prescription print configurations
 * (Paper size, Margins, Letterhead branding, Physician SAMA stamp, Color/Grayscale mode).
 */
class A4PrinterSettingsStore(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  companion object {
    private const val PREF_NAME = "shwekhit_a4_printer_preferences"
    private const val KEY_PAPER_SIZE = "key_a4_paper_size"
    private const val KEY_ORIENTATION = "key_a4_orientation"
    private const val KEY_MARGIN_TYPE = "key_a4_margin_type" // 0: Narrow (10mm), 1: Normal (15mm), 2: Wide (20mm)
    private const val KEY_IS_GRAYSCALE = "key_a4_is_grayscale"
    private const val KEY_INC_LETTERHEAD = "key_a4_inc_letterhead"
    private const val KEY_INC_STAMP = "key_a4_inc_stamp"
    private const val KEY_INC_DEMOGRAPHICS = "key_a4_inc_demographics"
    private const val KEY_INC_ANATOMY = "key_a4_inc_anatomy"
    private const val KEY_RX_ONLY = "key_a4_rx_only"
    private const val KEY_DEFAULT_PRINTER_NAME = "key_a4_default_printer_name"

    const val DEFAULT_PAPER_SIZE = "A4"
    const val DEFAULT_ORIENTATION = "Portrait"
    const val DEFAULT_MARGIN_TYPE = 1 // Normal 15mm
  }

  fun getPaperSize(): String = prefs.getString(KEY_PAPER_SIZE, DEFAULT_PAPER_SIZE) ?: DEFAULT_PAPER_SIZE

  fun setPaperSize(size: String) {
    prefs.edit().putString(KEY_PAPER_SIZE, size).apply()
  }

  fun getOrientation(): String = prefs.getString(KEY_ORIENTATION, DEFAULT_ORIENTATION) ?: DEFAULT_ORIENTATION

  fun setOrientation(orientation: String) {
    prefs.edit().putString(KEY_ORIENTATION, orientation).apply()
  }

  fun getMarginType(): Int = prefs.getInt(KEY_MARGIN_TYPE, DEFAULT_MARGIN_TYPE)

  fun setMarginType(type: Int) {
    prefs.edit().putInt(KEY_MARGIN_TYPE, type).apply()
  }

  fun getMarginMm(): Float {
    return when (getMarginType()) {
      0 -> 10f // Narrow
      2 -> 20f // Wide
      else -> 15f // Normal
    }
  }

  fun isGrayscale(): Boolean = prefs.getBoolean(KEY_IS_GRAYSCALE, false)

  fun setGrayscale(value: Boolean) {
    prefs.edit().putBoolean(KEY_IS_GRAYSCALE, value).apply()
  }

  fun isIncludeLetterhead(): Boolean = prefs.getBoolean(KEY_INC_LETTERHEAD, true)

  fun setIncludeLetterhead(value: Boolean) {
    prefs.edit().putBoolean(KEY_INC_LETTERHEAD, value).apply()
  }

  fun isIncludeStamp(): Boolean = prefs.getBoolean(KEY_INC_STAMP, true)

  fun setIncludeStamp(value: Boolean) {
    prefs.edit().putBoolean(KEY_INC_STAMP, value).apply()
  }

  fun isIncludeDemographics(): Boolean = prefs.getBoolean(KEY_INC_DEMOGRAPHICS, true)

  fun setIncludeDemographics(value: Boolean) {
    prefs.edit().putBoolean(KEY_INC_DEMOGRAPHICS, value).apply()
  }

  fun isIncludeAnatomy(): Boolean = prefs.getBoolean(KEY_INC_ANATOMY, true)

  fun setIncludeAnatomy(value: Boolean) {
    prefs.edit().putBoolean(KEY_INC_ANATOMY, value).apply()
  }

  fun isRxOnly(): Boolean = prefs.getBoolean(KEY_RX_ONLY, false)

  fun setRxOnly(value: Boolean) {
    prefs.edit().putBoolean(KEY_RX_ONLY, value).apply()
  }

  fun getDefaultPrinterName(): String = prefs.getString(KEY_DEFAULT_PRINTER_NAME, "Android System Print Spooler (Wi-Fi / Network)") ?: "Android System Print Spooler (Wi-Fi / Network)"

  fun setDefaultPrinterName(name: String) {
    prefs.edit().putString(KEY_DEFAULT_PRINTER_NAME, name).apply()
  }
}
