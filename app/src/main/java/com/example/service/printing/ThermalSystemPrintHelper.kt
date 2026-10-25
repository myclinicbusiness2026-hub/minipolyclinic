package com.example.service.printing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.example.model.ClinicSettings
import com.example.model.Invoice
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * ThermalSystemPrintHelper
 * 
 * Bridges Thermal POS Receipts with Android's Native System Print Spooler (PrintManager).
 * Ensures that clicking Print immediately opens Android's system Print Settings dialog,
 * supporting Wi-Fi printers, USB POS printers, Network printers, and "Save as PDF".
 */
class ThermalReceiptPrintAdapter(
  private val invoice: Invoice,
  private val patientHn: String,
  private val cashierName: String,
  private val clinicSettings: ClinicSettings,
  private val is80mm: Boolean
) : PrintDocumentAdapter() {

  override fun onLayout(
    oldAttributes: PrintAttributes?,
    newAttributes: PrintAttributes,
    cancellationSignal: CancellationSignal?,
    callback: LayoutResultCallback,
    metadata: Bundle?
  ) {
    if (cancellationSignal?.isCanceled == true) {
      callback.onLayoutCancelled()
      return
    }

    val safeInvNo = invoice.invoiceNo.replace("/", "_")
    val info = PrintDocumentInfo.Builder("Receipt_${safeInvNo}.pdf")
      .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
      .setPageCount(1)
      .build()

    callback.onLayoutFinished(info, true)
  }

  override fun onWrite(
    pages: Array<out PageRange>?,
    destination: ParcelFileDescriptor,
    cancellationSignal: CancellationSignal?,
    callback: WriteResultCallback
  ) {
    var pdfDoc: PdfDocument? = null
    try {
      pdfDoc = PdfDocument()

      // Width in typographical points (72 DPI): 58mm = ~164pt, 80mm = ~226pt
      // For universal compatibility with Android printer drivers & Save as PDF,
      // we use 226pt (80mm) or 164pt (58mm) width with dynamic height.
      val pageWidth = if (is80mm) 226 else 164
      // Dynamic height calculation based on item count
      val extraFeeCount = (if (invoice.consultationFee > 0.0) 1 else 0) + (if (invoice.serviceFee > 0.0) 1 else 0)
      val estimatedHeight = 360 + ((invoice.items.size + extraFeeCount) * 22) + 120
      val pageHeight = estimatedHeight.coerceAtLeast(420)

      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
      val page = pdfDoc.startPage(pageInfo)
      val canvas = page.canvas

      drawReceiptCanvas(canvas, pageWidth.toFloat(), pageHeight.toFloat())

      pdfDoc.finishPage(page)

      FileOutputStream(destination.fileDescriptor).use { out ->
        pdfDoc.writeTo(out)
      }

      callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    } catch (e: Exception) {
      callback.onWriteFailed(e.message)
    } finally {
      pdfDoc?.close()
    }
  }

  private fun drawReceiptCanvas(canvas: Canvas, width: Float, height: Float) {
    canvas.drawColor(Color.WHITE)

    val paint = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
    }

    val currencyFmt = NumberFormat.getNumberInstance(Locale.US)
    val margin = 10f
    val contentW = width - (margin * 2)
    var y = 20f

    // 1. Clinic Branding Header
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = if (is80mm) 12f else 10f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(clinicSettings.clinicNameMm, width / 2f, y, paint)
    y += 14f

    if (clinicSettings.clinicSubtitleMm.isNotBlank()) {
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      paint.textSize = 8f
      canvas.drawText(clinicSettings.clinicSubtitleMm, width / 2f, y, paint)
      y += 12f
    }

    paint.textSize = 7.5f
    canvas.drawText("${clinicSettings.doctorName} ${clinicSettings.doctorDegree}", width / 2f, y, paint)
    y += 11f
    canvas.drawText("SAMA: ${clinicSettings.doctorSama} • Tel: ${clinicSettings.clinicPhone}", width / 2f, y, paint)
    y += 12f

    // Divider
    paint.strokeWidth = 0.8f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 10f

    // 2. Receipt Meta
    paint.textAlign = Paint.Align.LEFT
    paint.textSize = 7.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("OFFICIAL INVOICE RECEIPT", margin, y, paint)
    y += 11f

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Bill No : ${invoice.invoiceNo}", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(invoice.createdAt, width - margin, y, paint)
    y += 10f

    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("Patient : ${invoice.patientName} (${patientHn})", margin, y, paint)
    y += 10f

    canvas.drawText("Cashier : $cashierName", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(if (invoice.isPaid) "PAID (${invoice.paymentMethod.label})" else "UNPAID", width - margin, y, paint)
    y += 10f

    // Divider
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 10f

    // 3. Table Column Headers
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 7.5f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("ITEM", margin, y, paint)
    val qtyX = width - margin - 55f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("QTY", qtyX, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("AMOUNT", width - margin, y, paint)
    y += 8f

    paint.strokeWidth = 0.5f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 10f

    // 4. Line Items
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 7.5f

    if (invoice.consultationFee > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("ဆရာဝန်ပြသခ (Doctor Fee)", margin, y, paint)
      paint.textAlign = Paint.Align.CENTER
      canvas.drawText("1", qtyX, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("${currencyFmt.format(invoice.consultationFee.toInt())} Ks", width - margin, y, paint)
      y += 12f
    }

    if (invoice.serviceFee > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("ဆေးခန်းဝန်ဆောင်ခ", margin, y, paint)
      paint.textAlign = Paint.Align.CENTER
      canvas.drawText("1", qtyX, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("${currencyFmt.format(invoice.serviceFee.toInt())} Ks", width - margin, y, paint)
      y += 12f
    }

    invoice.items.forEach { item ->
      paint.textAlign = Paint.Align.LEFT
      val maxDescChars = if (is80mm) 24 else 16
      val desc = if (item.description.length > maxDescChars) item.description.take(maxDescChars - 2) + ".." else item.description
      canvas.drawText(desc, margin, y, paint)

      paint.textAlign = Paint.Align.CENTER
      canvas.drawText("${item.quantity}", qtyX, y, paint)

      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("${currencyFmt.format(item.total.toInt())} Ks", width - margin, y, paint)
      y += 12f
    }

    // Divider
    paint.strokeWidth = 0.8f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 11f

    // 5. Totals
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("Subtotal (မူလကျသင့်ငွေ):", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.subTotal.toInt())} Ks", width - margin, y, paint)
    y += 11f

    if (invoice.doctorFeeDiscount > 0.0) {
      val isDocFoc = invoice.doctorFeeDiscount >= invoice.consultationFee && invoice.consultationFee > 0.0
      val label = if (isDocFoc) "Doctor Fee (ဆရာဝန်ပြသခ FOC):" else "Dr Fee Disc:"
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText(label, margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.doctorFeeDiscount.toInt())} Ks", width - margin, y, paint)
      y += 10f
    }

    if (invoice.pharmacyDiscount > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Pharmacy Disc (ဆေးဝါးလျှော့):", margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.pharmacyDiscount.toInt())} Ks", width - margin, y, paint)
      y += 10f
    }

    if (invoice.procedureDiscount > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Procedure Disc (ကုသမှုလျှော့):", margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.procedureDiscount.toInt())} Ks", width - margin, y, paint)
      y += 10f
    }

    if (invoice.discountPercent > 0) {
      val isAllFoc = invoice.discountPercent >= 100.0
      val label = if (isAllFoc) "All FOC (အားလုံး အခမဲ့):" else "Overall Disc (${invoice.discountPercent.toInt()}%):"
      val overallVal = (invoice.subTotal - invoice.totalGranularDiscount).coerceAtLeast(0.0) * (invoice.discountPercent / 100.0)
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText(label, margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(overallVal.toInt())} Ks", width - margin, y, paint)
      y += 10f
    } else if (invoice.discountAmount > 0.0 && invoice.totalGranularDiscount == 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Discount (လျှော့ဈေး):", margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.discountAmount.toInt())} Ks", width - margin, y, paint)
      y += 10f
    }

    if (invoice.discountAmount > 0.0 && ((invoice.doctorFeeDiscount > 0.0 && invoice.pharmacyDiscount > 0.0) || invoice.discountPercent > 0.0)) {
      paint.textAlign = Paint.Align.LEFT
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Total Deductions (စုစုပေါင်း လျှော့ငွေ):", margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.discountAmount.toInt())} Ks", width - margin, y, paint)
      paint.typeface = Typeface.DEFAULT
      y += 11f
    }

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 8.5f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("GRAND TOTAL:", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.grandTotal.toInt())} Ks", width - margin, y, paint)
    y += 12f

    // Double Divider
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 2f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 12f

    // 6. Footer Greeting
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 7f
    paint.textAlign = Paint.Align.CENTER
    val greeting = clinicSettings.receiptGreetingMm.ifBlank { "ကျေးဇူးတင်ပါသည် (Thank you!)" }
    canvas.drawText(greeting, width / 2f, y, paint)
    y += 10f
    canvas.drawText("* 58/80mm POS Thermal Slip *", width / 2f, y, paint)
  }
}

/**
 * Triggers Android's Native System Print Spooler for Thermal POS Receipts.
 */
fun triggerSystemPrintThermalSlip(
  context: Context,
  invoice: Invoice,
  patientHn: String,
  cashierName: String,
  clinicSettings: ClinicSettings,
  is80mm: Boolean = false
) {
  try {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager != null) {
      val safeInv = invoice.invoiceNo.replace("[^a-zA-Z0-9]".toRegex(), "_")
      val jobName = "ShweKhit_Receipt_${safeInv}"

      val printAttributes = PrintAttributes.Builder()
        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

      val adapter = ThermalReceiptPrintAdapter(
        invoice = invoice,
        patientHn = patientHn,
        cashierName = cashierName,
        clinicSettings = clinicSettings,
        is80mm = is80mm
      )

      printManager.print(jobName, adapter, printAttributes)
    } else {
      Toast.makeText(context, "System Print Spooler not available", Toast.LENGTH_SHORT).show()
    }
  } catch (e: Exception) {
    Toast.makeText(context, "Print Spooler Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
  }
}
