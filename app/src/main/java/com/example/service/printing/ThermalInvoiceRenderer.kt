package com.example.service.printing

import com.example.model.Invoice

object ThermalInvoiceRenderer {

  fun generateReceiptText(invoice: Invoice, clinicName: String = "\"ရွှေခေတ်\" အထွေထွေရောဂါကုဆေးခန်း"): String {
    val divider = "------------------------------------------"
    val sb = StringBuilder()

    sb.appendLine(clinicName)
    sb.appendLine("Dr. Nyan Kaung Sett M.B.,B.S(MDY)")
    sb.appendLine("လိပ်စာ: မန္တလေးမြို့ • ဖုန်း: 09-450012345")
    sb.appendLine(divider)
    sb.appendLine("ပြေစာအမှတ်: ${invoice.invoiceNumber}")
    sb.appendLine("ရက်စွဲ/အချိန်: ${invoice.timestamp}")
    sb.appendLine("လူနာအမည်: ${invoice.patientName}")
    sb.appendLine("ငွေကိုင်: ${invoice.cashierName}")
    sb.appendLine(divider)
    sb.appendLine(String.format("%-22s %4s %12s", "ဆေးအမည် / ဝန်ဆောင်မှု", "Qty", "သင့်ငွေ (Ks)"))
    sb.appendLine(divider)

    if (invoice.consultationFee > 0) {
      sb.appendLine(String.format("%-22s %4s %12.0f", "ဆရာဝန်ပြသခ (Doctor Fee)", "1", invoice.consultationFee))
    }
    if (invoice.serviceFee > 0) {
      sb.appendLine(String.format("%-22s %4s %12.0f", "ဆေးခန်းဝန်ဆောင်ခ", "1", invoice.serviceFee))
    }

    invoice.items.forEach { item ->
      val truncatedName = if (item.name.length > 22) item.name.take(20) + ".." else item.name
      val qtyStr = item.quantity.toString()
      sb.appendLine(String.format("%-22s %4s %12.0f", truncatedName, qtyStr, item.totalPrice))
    }

    sb.appendLine(divider)
    sb.appendLine(String.format("%-28s %12.0f", "မူလကျသင့်ငွေ (Subtotal):", invoice.subTotal))

    if (invoice.doctorFeeDiscount > 0) {
      sb.appendLine(String.format("%-28s -%11.0f", "ဆရာဝန်ပြသခ FOC:", invoice.doctorFeeDiscount))
    }
    if (invoice.pharmacyDiscount > 0) {
      sb.appendLine(String.format("%-28s -%11.0f", "ဆေးဝါး လျှော့ငွေ:", invoice.pharmacyDiscount))
    }
    if (invoice.procedureDiscount > 0) {
      sb.appendLine(String.format("%-28s -%11.0f", "ကုသမှု လျှော့ငွေ:", invoice.procedureDiscount))
    }
    if (invoice.discountPercent > 0) {
      val overallVal = (invoice.subTotal - invoice.totalGranularDiscount).coerceAtLeast(0.0) * (invoice.discountPercent / 100.0)
      sb.appendLine(String.format("%-28s -%11.0f", "အထွေထွေ လျှော့ဈေး (${invoice.discountPercent.toInt()}%):", overallVal))
    } else if (invoice.discount > 0 && invoice.totalGranularDiscount == 0.0) {
      sb.appendLine(String.format("%-28s -%11.0f", "လျှော့ငွေ (Discount):", invoice.discount))
    }

    sb.appendLine(divider)
    sb.appendLine(String.format("%-28s %12.0f", "တကယ်ကျသင့်ငွေ (Net Total):", invoice.totalAmount))

    sb.appendLine(String.format("%-28s %12s", "ပေးချေမှုပုံစံ (Method):", invoice.paymentMethod))
    sb.appendLine(String.format("%-28s %12.0f", "ပေးငွေ (Paid Amount):", invoice.paidAmount))
    if (invoice.changeAmount > 0) {
      sb.appendLine(String.format("%-28s %12.0f", "ပြန်အမ်းငွေ (Change):", invoice.changeAmount))
    }

    sb.appendLine(divider)
    sb.appendLine("ကျန်းမာချမ်းသာပါစေ")
    sb.appendLine("Thank you for visiting Shwe Khit Clinic")
    sb.appendLine("==========================================")

    return sb.toString()
  }
}
