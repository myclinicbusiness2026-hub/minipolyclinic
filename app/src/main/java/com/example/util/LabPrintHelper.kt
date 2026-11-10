package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.model.ClinicSettings
import com.example.model.LabInvoice
import com.example.model.LabOrder
import com.example.model.LabOrderItem
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * LabPrintHelper
 *
 * Dedicated printing utility for the B2B Lab Investigation System:
 * 1. Canon A4 Printer (Android System Print / Mopria / Save as PDF):
 *    - Mode A: Blank Letterhead (Clinic header + Patient demographics + blank body for CBC analyzer slip)
 *    - Mode B: Full Dynamic Parameter Report (Clinic header + Patient demographics + Parameter Table with Units,
 *              Normal Ranges, High/Low Badges + Remarks + Attached Slips + Signatures)
 * 2. Thermal POS Printer (80mm & 58mm):
 *    - Laboratory Cash Voucher / Receipt Slip completely separate from clinic OPD cash drawer.
 */
object LabPrintHelper {

  /**
   * Print Lab Report on Canon A4 / System Printer via WebView HTML engine.
   *
   * @param context Application / Activity Context
   * @param order The lab order
   * @param item The specific test item (or null for all tests in order)
   * @param clinicSettings Clinic branding settings
   * @param isBlankLetterhead If true, generates blank letterhead for analyzer slip insertion
   */
  fun printLabReportA4(
    context: Context,
    order: LabOrder,
    item: LabOrderItem? = null,
    clinicSettings: ClinicSettings,
    isBlankLetterhead: Boolean = false
  ) {
    Handler(Looper.getMainLooper()).post {
      try {
        val html = generateLabReportHtml(order, item, clinicSettings, isBlankLetterhead)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
              val jobName = if (isBlankLetterhead) {
                "Lab_Letterhead_${order.orderNumber}"
              } else {
                "Lab_Report_${order.orderNumber}_${item?.templateName ?: "All"}"
              }
              val printAdapter = webView.createPrintDocumentAdapter(jobName)
              val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
              printManager.print(jobName, printAdapter, printAttributes)
            } else {
              Toast.makeText(context, "Print service not available", Toast.LENGTH_SHORT).show()
            }
          }
        }
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
      } catch (e: Exception) {
        Toast.makeText(context, "Error initiating print: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
      }
    }
  }

  /**
   * Generates responsive, high-contrast HTML formatted specifically for ISO A4 Canon & Inkjet Printers.
   */
  private fun generateLabReportHtml(
    order: LabOrder,
    item: LabOrderItem?,
    clinic: ClinicSettings,
    isBlankLetterhead: Boolean
  ): String {
    val itemsToPrint = if (item != null) listOf(item) else order.items
    val title = if (isBlankLetterhead) "LABORATORY INVESTIGATION LETTERHEAD" else "LABORATORY INVESTIGATION REPORT"
    val testNames = itemsToPrint.joinToString(", ") { it.templateName }

    val sb = StringBuilder()
    sb.append("""
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>$title</title>
        <style>
          @page {
            size: A4 portrait;
            margin: 12mm 15mm 15mm 15mm;
          }
          * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }
          body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Pyidaungsu", Helvetica, Arial, sans-serif;
            color: #1a1a1a;
            margin: 0;
            padding: 0;
            font-size: 13px;
            line-height: 1.4;
            background-color: #ffffff;
          }
          .header-table {
            width: 100%;
            border-bottom: 2.5px solid #0056b3;
            padding-bottom: 10px;
            margin-bottom: 12px;
          }
          .clinic-logo {
            max-height: 65px;
            max-width: 120px;
            object-fit: contain;
          }
          .clinic-name-mm {
            font-size: 19px;
            font-weight: bold;
            color: #0b3954;
            margin: 0 0 2px 0;
          }
          .clinic-name-en {
            font-size: 14px;
            font-weight: 600;
            color: #333333;
            margin: 0 0 3px 0;
          }
          .clinic-contact {
            font-size: 11px;
            color: #555555;
            margin: 1px 0;
          }
          .report-banner {
            text-align: center;
            background: #e9f2fb;
            border: 1px solid #b8daff;
            color: #004085;
            padding: 5px;
            font-weight: bold;
            font-size: 14px;
            letter-spacing: 0.5px;
            border-radius: 4px;
            margin-bottom: 12px;
          }
          .patient-box {
            width: 100%;
            border: 1px solid #dcdcdc;
            border-radius: 4px;
            padding: 8px 12px;
            margin-bottom: 15px;
            background-color: #fafbfc;
          }
          .patient-grid {
            display: table;
            width: 100%;
          }
          .patient-row {
            display: table-row;
          }
          .patient-cell {
            display: table-cell;
            padding: 3px 6px;
            font-size: 12px;
          }
          .patient-cell strong {
            color: #222;
          }
          .param-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 10px;
            margin-bottom: 15px;
          }
          .param-table th {
            background-color: #0b3954;
            color: #ffffff;
            font-weight: 600;
            text-align: left;
            padding: 7px 10px;
            font-size: 11.5px;
            border: 1px solid #0b3954;
          }
          .param-table td {
            padding: 6px 10px;
            border-bottom: 1px solid #e1e4e8;
            font-size: 12px;
          }
          .param-table tr:nth-child(even) {
            background-color: #f8f9fa;
          }
          .badge-high {
            background-color: #ffeef0;
            color: #d73a49;
            font-weight: bold;
            padding: 2px 6px;
            border-radius: 3px;
            border: 1px solid #f97583;
            font-size: 10px;
          }
          .badge-low {
            background-color: #fffbdd;
            color: #b08800;
            font-weight: bold;
            padding: 2px 6px;
            border-radius: 3px;
            border: 1px solid #ffea7f;
            font-size: 10px;
          }
          .badge-normal {
            background-color: #dcffe4;
            color: #22863a;
            font-weight: 600;
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 10px;
          }
          .test-heading {
            background: #eef4f8;
            padding: 6px 10px;
            font-weight: bold;
            font-size: 13px;
            color: #0b3954;
            border-left: 4px solid #0056b3;
            margin-top: 15px;
            margin-bottom: 4px;
          }
          .blank-area {
            min-height: 480px;
            border: 1.5px dashed #cccccc;
            border-radius: 8px;
            margin: 20px 0;
            display: flex;
            align-items: center;
            justify-content: center;
            text-align: center;
            color: #888888;
            background-color: #ffffff;
            padding: 40px 20px;
          }
          .blank-notice {
            font-size: 13px;
            line-height: 1.6;
          }
          .footer-section {
            margin-top: 25px;
            width: 100%;
            page-break-inside: avoid;
          }
          .sign-table {
            width: 100%;
            margin-top: 40px;
          }
          .sign-cell {
            width: 50%;
            text-align: center;
            vertical-align: bottom;
            font-size: 11.5px;
          }
          .sign-line {
            width: 75%;
            margin: 0 auto 5px auto;
            border-bottom: 1px solid #444;
          }
          .remark-box {
            border: 1px solid #e1e4e8;
            background: #fafbfc;
            border-radius: 4px;
            padding: 8px 12px;
            margin-top: 10px;
            font-size: 11.5px;
          }
          .slip-attachment {
            margin-top: 15px;
            text-align: center;
          }
          .slip-image {
            max-width: 95%;
            max-height: 450px;
            border: 1px solid #ccc;
            border-radius: 4px;
          }
        </style>
      </head>
      <body>
        <!-- Header -->
        <table class="header-table">
          <tr>
            <td style="width: 70%; vertical-align: top;">
              <div class="clinic-name-mm">${clinic.clinicNameMm.ifBlank { "“ရွှေခေတ်” ဆေးခန်း" }}</div>
              <div class="clinic-name-en">${clinic.clinicNameEn.ifBlank { "Shwe Khit General Clinic" }}</div>
              <div class="clinic-contact">${clinic.clinicAddress}</div>
              <div class="clinic-contact">Tel: ${clinic.clinicPhone} | Reg: ${clinic.clinicRegNo}</div>
              <div class="clinic-contact">Doctor: ${clinic.doctorName} (${clinic.doctorDegree}) • S.A.M.A: ${clinic.doctorSama}</div>
            </td>
            <td style="width: 30%; text-align: right; vertical-align: middle;">
              ${if (clinic.clinicLogoBase64.isNotBlank()) "<img src='data:image/png;base64,${clinic.clinicLogoBase64}' class='clinic-logo'/>" else ""}
            </td>
          </tr>
        </table>

        <!-- Report Banner -->
        <div class="report-banner">$title</div>

        <!-- Patient Demographics -->
        <div class="patient-box">
          <div class="patient-grid">
            <div class="patient-row">
              <div class="patient-cell" style="width: 50%;"><strong>Patient Name:</strong> ${order.patientName}</div>
              <div class="patient-cell" style="width: 50%;"><strong>Lab Order No:</strong> ${order.orderNumber}</div>
            </div>
            <div class="patient-row">
              <div class="patient-cell"><strong>Hospital No (HN):</strong> ${order.patientHn}</div>
              <div class="patient-cell"><strong>Date & Time:</strong> ${order.orderDate}</div>
            </div>
            <div class="patient-row">
              <div class="patient-cell"><strong>Age / Gender:</strong> ${order.patientAge} / ${order.patientGender}</div>
              <div class="patient-cell"><strong>Referring Doctor:</strong> ${order.referringDoctorName.ifBlank { clinic.doctorName }}</div>
            </div>
            <div class="patient-row">
              <div class="patient-cell" colspan="2"><strong>Investigation(s) Ordered:</strong> $testNames</div>
            </div>
          </div>
        </div>
    """.trimIndent())

    if (isBlankLetterhead) {
      // Mode A: Blank Letterhead for Analyzer feed / attachment
      sb.append("""
        <div class="blank-area">
          <div class="blank-notice">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="1.5" style="margin-bottom: 8px;">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="12" y1="18" x2="12" y2="12"></line>
              <line x1="9" y1="15" x2="15" y2="15"></line>
            </svg><br/>
            <strong>စက်မှတိုက်ရိုက်ထွက်သောအဖြေ (Analyzer Slip) ထည့်သွင်း / ကပ်ရန်နေရာ</strong><br/>
            (Auto-Hematology Analyzer / Blood Chemistry Printout Attachment Area)<br/>
            <span style="font-size: 11px; color: #999;">ဤ A4 စာရွက်အား Analyzer စက်ထဲထည့်သွင်း၍ direct print ထုတ်နိုင်သလို၊ Thermal analyzer strip ကိုလည်း ကပ်နိုင်ပါသည်။</span>
          </div>
        </div>
      """.trimIndent())
    } else {
      // Mode B: Dynamic Parameters Table
      for (testItem in itemsToPrint) {
        sb.append("<div class='test-heading'>${testItem.templateName}</div>")

        if (testItem.isAnalyzerSlipOnly && testItem.parameters.isEmpty()) {
          sb.append("""
            <div style="padding: 12px; border: 1px dashed #bbb; border-radius: 4px; text-align: center; color: #666; margin-bottom: 10px;">
              <em>Automated Analyzer Direct Print Test</em><br/>
              (CBC Analyzer Slip Attached / Recorded)
            </div>
          """.trimIndent())
        } else {
          sb.append("""
            <table class="param-table">
              <thead>
                <tr>
                  <th style="width: 35%;">Investigation / Parameter</th>
                  <th style="width: 20%;">Result</th>
                  <th style="width: 15%;">Unit</th>
                  <th style="width: 20%;">Reference Interval</th>
                  <th style="width: 10%; text-align: center;">Flag</th>
                </tr>
              </thead>
              <tbody>
          """.trimIndent())

          for (param in testItem.parameters) {
            val flagBadge = when (param.flag.uppercase()) {
              "HIGH" -> "<span class='badge-high'>HIGH</span>"
              "LOW" -> "<span class='badge-low'>LOW</span>"
              "NORMAL" -> "<span class='badge-normal'>NORMAL</span>"
              else -> if (param.flag.isNotBlank()) "<span>${param.flag}</span>" else "-"
            }
            sb.append("""
              <tr>
                <td><strong>${param.name}</strong></td>
                <td><strong style="color: #0b3954;">${param.result.ifBlank { "-" }}</strong></td>
                <td>${param.unit.ifBlank { "-" }}</td>
                <td>${param.normalRange.ifBlank { "-" }}</td>
                <td style="text-align: center;">$flagBadge</td>
              </tr>
            """.trimIndent())
          }
          sb.append("</tbody></table>")
        }

        // Test remarks
        if (testItem.remarks.isNotBlank()) {
          sb.append("""
            <div class="remark-box">
              <strong>Technician Remarks / Interpretation:</strong> ${testItem.remarks}
            </div>
          """.trimIndent())
        }

        // Attached analyzer image if present
        if (testItem.slipImageUri.isNotBlank()) {
          sb.append("""
            <div class="slip-attachment">
              <div style="font-weight: bold; font-size: 11.5px; color: #555; margin-bottom: 6px;">Attached Analyzer Result Slip:</div>
              <img src="${testItem.slipImageUri}" class="slip-image" />
            </div>
          """.trimIndent())
        }
      }

      if (order.notes.isNotBlank()) {
        sb.append("""
          <div class="remark-box" style="margin-top: 15px;">
            <strong>General Clinical Notes:</strong> ${order.notes}
          </div>
        """.trimIndent())
      }
    }

    // Signatures footer
    sb.append("""
        <div class="footer-section">
          <table class="sign-table">
            <tr>
              <td class="sign-cell">
                <div class="sign-line"></div>
                <strong>${order.performedBy.ifBlank { "Medical Laboratory Technologist" }}</strong><br/>
                <span style="color: #666; font-size: 10.5px;">Performed & Verified By</span>
              </td>
              <td class="sign-cell">
                <div class="sign-line"></div>
                <strong>${clinic.doctorName}</strong><br/>
                <span style="color: #666; font-size: 10.5px;">Pathologist / Attending Physician (S.A.M.A: ${clinic.doctorSama})</span>
              </td>
            </tr>
          </table>
          <div style="text-align: center; margin-top: 20px; font-size: 10px; color: #888;">
            This investigation is processed by the Independent Laboratory Services at ${clinic.clinicNameMm}.
          </div>
        </div>
      </body>
      </html>
    """.trimIndent())

    return sb.toString()
  }

  /**
   * Print Lab Cash Voucher via Android System Print Spooler (Thermal POS compatible 80mm & 58mm).
   */
  fun printLabVoucherThermal(
    context: Context,
    invoice: LabInvoice,
    clinicSettings: ClinicSettings,
    is80mm: Boolean = true
  ) {
    try {
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
      if (printManager != null) {
        val jobName = "Lab_Voucher_${invoice.invoiceNumber}"
        val adapter = LabThermalReceiptPrintAdapter(invoice, clinicSettings, is80mm)
        val printAttributes = PrintAttributes.Builder()
          .setMediaSize(if (is80mm) PrintAttributes.MediaSize.ISO_A7 else PrintAttributes.MediaSize.ISO_A8)
          .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
          .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
          .build()
        printManager.print(jobName, adapter, printAttributes)
      } else {
        Toast.makeText(context, "System print service unavailable", Toast.LENGTH_SHORT).show()
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Thermal print spooler error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
  }
}

/**
 * Android PrintDocumentAdapter generating authentic ESC/POS compatible thermal canvas
 * for Lab Invoices.
 */
class LabThermalReceiptPrintAdapter(
  private val invoice: LabInvoice,
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
    val safeNo = invoice.invoiceNumber.replace("/", "_")
    val info = PrintDocumentInfo.Builder("LabReceipt_${safeNo}.pdf")
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
      val pageWidth = if (is80mm) 226 else 164
      val estimatedHeight = 320 + (invoice.items.size * 22) + 120
      val pageHeight = estimatedHeight.coerceAtLeast(380)

      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
      val page = pdfDoc.startPage(pageInfo)
      val canvas = page.canvas

      drawLabReceiptCanvas(canvas, pageWidth.toFloat(), pageHeight.toFloat())

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

  private fun drawLabReceiptCanvas(canvas: Canvas, width: Float, height: Float) {
    canvas.drawColor(Color.WHITE)
    val paint = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
    }

    val currencyFmt = NumberFormat.getNumberInstance(Locale.US)
    val margin = 10f
    val contentW = width - (margin * 2)
    var y = 20f

    // 1. Clinic Header
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = if (is80mm) 12f else 10f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(clinicSettings.clinicNameMm.ifBlank { "Shwe Khit Clinic" }, width / 2f, y, paint)
    y += 14f

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 8f
    canvas.drawText("LABORATORY SERVICES (B2B)", width / 2f, y, paint)
    y += 11f
    canvas.drawText("Tel: ${clinicSettings.clinicPhone}", width / 2f, y, paint)
    y += 12f

    // Divider
    paint.strokeWidth = 0.8f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 10f

    // 2. Receipt Meta
    paint.textAlign = Paint.Align.LEFT
    paint.textSize = 8f
    canvas.drawText("Voucher: ${invoice.invoiceNumber}", margin, y, paint)
    y += 11f
    canvas.drawText("Date: ${invoice.date}", margin, y, paint)
    y += 11f
    canvas.drawText("Patient: ${invoice.patientName}", margin, y, paint)
    y += 11f
    canvas.drawText("HN: ${invoice.patientHn}", margin, y, paint)
    y += 11f
    canvas.drawText("Cashier: ${invoice.cashierName.ifBlank { "Lab Cashier" }}", margin, y, paint)
    y += 13f

    // Table Header
    paint.strokeWidth = 0.5f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 10f

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 7.5f
    canvas.drawText("Test Name", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Amount", width - margin, y, paint)
    y += 10f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 12f

    // Table Items
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    for (item in invoice.items) {
      paint.textAlign = Paint.Align.LEFT
      val displayName = if (item.templateName.length > 22) item.templateName.take(20) + ".." else item.templateName
      canvas.drawText(displayName, margin, y, paint)

      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("${currencyFmt.format(item.price.toInt())} Ks", width - margin, y, paint)
      y += 13f
    }

    // Divider
    y += 4f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 12f

    // Summary Financials
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("Subtotal:", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.subtotal.toInt())} Ks", width - margin, y, paint)
    y += 12f

    if (invoice.discount > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Discount:", margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.discount.toInt())} Ks", width - margin, y, paint)
      y += 12f
    }

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 9f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("Net Total:", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.netTotal.toInt())} Ks", width - margin, y, paint)
    y += 13f

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 8f
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("Paid (${invoice.paymentMethod}):", margin, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.paidAmount.toInt())} Ks", width - margin, y, paint)
    y += 12f

    if (invoice.changeAmount > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Change:", margin, y, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("${currencyFmt.format(invoice.changeAmount.toInt())} Ks", width - margin, y, paint)
      y += 12f
    }

    // Footer
    y += 6f
    canvas.drawLine(margin, y, width - margin, y, paint)
    y += 12f
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 7.5f
    canvas.drawText("ဓာတ်ခွဲခန်း သီးသန့် ငွေရပြေစာဖြစ်ပါသည်", width / 2f, y, paint)
    y += 10f
    canvas.drawText("Lab tests are independent & non-refundable.", width / 2f, y, paint)
    y += 10f
    canvas.drawText("Thank You!", width / 2f, y, paint)
  }
}
