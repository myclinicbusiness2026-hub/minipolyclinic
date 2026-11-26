package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AuditModule(val displayName: String, val myanmarName: String) {
  PHARMACY("Pharmacy", "ဆေးဝါးထုတ်ပေးခြင်း & စတို"),
  INVENTORY("Inventory", "စတော့အချုပ်"),
  BILLING("Billing", "ငွေစာရင်း & ပြေစာ"),
  PATIENT("Patient", "လူနာမှတ်တမ်း"),
  TREATMENT("Treatment", "ဆေးထိုး & ကုသမှု"),
  GENERAL("General", "အထွေထွေ")
}

enum class AuditActionType(val displayName: String, val myanmarName: String) {
  WASTAGE_WRITE_OFF("Wastage Write-Off", "အသုံး/အလေအလွင့် စာရင်းနှုတ်ခြင်း"),
  STOCK_ADJUSTMENT("Stock Adjustment", "စတော့အတိုးအလျှော့ ပြင်ဆင်ခြင်း"),
  STOCK_IN("Stock-In", "စတော့အသစ်သွင်းခြင်း"),
  PRICE_CHANGE("Price Change", "ဆေးဈေးနှုန်း ပြင်ဆင်ခြင်း"),
  INVOICE_EDIT("Invoice Edit", "ပြေစာ/ဘောင်ချာ ပြင်ဆင်ခြင်း"),
  INVOICE_VOID("Invoice Void", "ပြေစာ ဖျက်သိမ်းခြင်း"),
  PATIENT_EDIT("Patient Edit", "လူနာအချက်အလက် ပြင်ဆင်ခြင်း"),
  RECORD_DELETION("Record Deletion", "စာရင်း ဖျက်ပယ်ခြင်း")
}

data class AuditLogEntry(
  val id: String = UUID.randomUUID().toString(),
  val timestamp: Long = System.currentTimeMillis(),
  val operatorName: String = "Staff",
  val operatorRole: String = "Staff",
  val module: String = AuditModule.PHARMACY.displayName,
  val actionType: String = AuditActionType.WASTAGE_WRITE_OFF.displayName,
  val targetName: String = "",
  val quantityChange: Double = 0.0,
  val unitType: String = "",
  val costAmount: Double = 0.0,
  val details: String = "",
  val reason: String = "",
  val metadata: Map<String, String> = emptyMap()
) {
  val formattedDate: String get() {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
  }
}
