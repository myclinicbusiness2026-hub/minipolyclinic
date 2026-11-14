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

  private fun convertImageToBase64DataUri(context: Context, uriStr: String): String {
    if (uriStr.isBlank() || uriStr.startsWith("data:")) return uriStr
    return try {
      val uri = android.net.Uri.parse(uriStr)
      val inputStream = if (uriStr.startsWith("content://")) {
        context.contentResolver.openInputStream(uri)
      } else {
        val cleanPath = uriStr.replace("file://", "")
        java.io.File(cleanPath).inputStream()
      }
      val bytes = inputStream?.use { it.readBytes() } ?: return uriStr
      val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
      "data:image/jpeg;base64,$b64"
    } catch (e: Exception) {
      uriStr
    }
  }


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
    clinicSettings: ClinicSettings
  ) = printLabReportA4(context, order, null, clinicSettings, false)

  fun printBlankLetterheadA4(
    context: Context,
    clinicSettings: ClinicSettings
  ) {
    val dummyOrder = LabOrder(
      id = "BLANK-LETTERHEAD",
      orderNumber = "",
      patientId = "",
      patientName = "",
      patientHn = "",
      patientAge = 0,
      patientGender = com.example.model.Gender.OTHER,
      doctorName = "",
      orderDate = "",
      orderTime = "",
      status = com.example.model.LabOrderStatus.COMPLETED,
      totalAmount = 0.0,
      items = emptyList()
    )
    printLabReportA4(context, dummyOrder, null, clinicSettings, isBlankLetterhead = true)
  }

  fun printLabReportA4(
    context: Context,
    order: LabOrder,
    item: LabOrderItem? = null,
    clinicSettings: ClinicSettings,
    isBlankLetterhead: Boolean = false
  ) {
    Handler(Looper.getMainLooper()).post {
      try {
        val html = generateLabReportHtml(context, order, item, clinicSettings, isBlankLetterhead)
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
   * Supports multi-page independent reports with per-page letterheads and pinned doctor seals.
   */
  private fun generateLabReportHtml(
    context: Context,
    order: LabOrder,
    item: LabOrderItem?,
    clinic: ClinicSettings,
    isBlankLetterhead: Boolean
  ): String {
    val itemsToPrint = if (item != null) listOf(item) else order.items
    val docTitle = if (isBlankLetterhead) "Clinic Pre-printed Letterhead" else "Laboratory Investigation Report"

    val pages: List<List<LabOrderItem>> = if (isBlankLetterhead) {
      listOf(emptyList())
    } else if (item != null) {
      listOf(listOf(item))
    } else {
      partitionItemsIntoPages(itemsToPrint)
    }

    val effectivePathologistName = if (isBlankLetterhead) {
      clinic.defaultPathologistName.ifBlank { "Consultant Pathologist" }
    } else {
      order.pathologistName.ifBlank {
        clinic.defaultPathologistName.ifBlank { "Consultant Pathologist" }
      }
    }
    val effectivePathologistDegree = if (isBlankLetterhead) {
      clinic.defaultPathologistDegree.ifBlank { "M.B.,B.S., M.Med.Sc (Pathology)" }
    } else {
      order.pathologistDegree.ifBlank {
        clinic.defaultPathologistDegree.ifBlank { "M.B.,B.S., M.Med.Sc (Pathology)" }
      }
    }
    val effectivePathologistSama = if (isBlankLetterhead) {
      clinic.defaultPathologistSama
    } else {
      order.pathologistSama.ifBlank { clinic.defaultPathologistSama }
    }
    val effectivePathologistTitle = if (isBlankLetterhead) {
      clinic.defaultPathologistTitle.ifBlank { "Consultant Pathologist" }
    } else {
      order.pathologistTitle.ifBlank { clinic.defaultPathologistTitle }.trim()
    }

    val sb = StringBuilder()
    sb.append("""
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>$docTitle</title>
        <style>
          @page {
            size: A4 portrait;
            margin: 10mm 14mm 10mm 14mm;
          }
          * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }
          html, body {
            margin: 0;
            padding: 0;
            background-color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Pyidaungsu", Helvetica, Arial, sans-serif;
            color: #1a1a1a;
            font-size: 12.5px;
            line-height: 1.4;
          }
          .page-sheet {
            page-break-after: always;
            break-after: page;
            min-height: 275mm;
            max-height: 275mm;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            overflow: hidden;
            box-sizing: border-box;
            background: #ffffff;
          }
          .page-sheet:last-child {
            page-break-after: avoid;
            break-after: avoid;
          }
          .header-table {
            width: 100%;
            border-bottom: 2.5px solid #0056b3;
            padding-bottom: 8px;
            margin-bottom: 8px;
            flex-shrink: 0;
          }
          .clinic-logo {
            max-height: 52px;
            max-width: 110px;
            object-fit: contain;
          }
          .clinic-name-mm {
            font-size: 17px;
            font-weight: bold;
            color: #1b5e20;
            margin: 0 0 2px 0;
          }
          .clinic-name-en {
            font-size: 13px;
            font-weight: bold;
            color: #2e7d32;
            margin: 0 0 2px 0;
          }
          .clinic-contact {
            font-size: 10px;
            color: #4b5563;
            margin: 1px 0;
          }
          .report-banner {
            text-align: center;
            background: #f8fafc;
            border-top: 1.5px solid #cbd5e1;
            border-bottom: 1.5px solid #cbd5e1;
            color: #1b5e20;
            padding: 4px;
            font-weight: bold;
            font-size: 12.5px;
            letter-spacing: 0.8px;
            border-radius: 2px;
            margin-bottom: 8px;
            flex-shrink: 0;
          }
          .patient-box {
            width: 100%;
            border: 1px solid #dcdcdc;
            border-radius: 4px;
            padding: 6px 10px;
            margin-bottom: 10px;
            background-color: #fafbfc;
            flex-shrink: 0;
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
            padding: 2px 6px;
            font-size: 11.5px;
          }
          .patient-cell strong {
            color: #222;
          }
          .page-body {
            flex: 1 1 auto;
            display: flex;
            flex-direction: column;
            overflow: hidden;
          }
          .param-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 6px;
            margin-bottom: 10px;
          }
          .param-table th {
            background-color: #f1f5f9;
            color: #1e293b;
            font-weight: bold;
            text-align: left;
            padding: 5px 8px;
            font-size: 11px;
            border-top: 1.5px solid #94a3b8;
            border-bottom: 1.5px solid #94a3b8;
          }
          .param-table td {
            padding: 5px 8px;
            border-bottom: 1px solid #e2e8f0;
            font-size: 11.5px;
          }
          .param-table tr:nth-child(even) {
            background-color: #f8fafc;
          }
          .panel-section-row td {
            background-color: #f1f5f9;
            color: #0b3954;
            font-weight: bold;
            font-size: 11.5px;
            padding: 5px 8px;
            border-top: 1.5px solid #cbd5e1;
            border-bottom: 1px solid #cbd5e1;
            letter-spacing: 0.3px;
          }
          .page-footer {
            flex-shrink: 0;
            margin-top: auto;
            padding-top: 4px;
            border-top: 1px solid #e2e8f0;
          }
          .sign-table {
            width: 100%;
            margin-top: 4px;
          }
          .sign-cell {
            width: 50%;
            text-align: center;
            vertical-align: bottom;
            font-size: 10px;
          }
          .sign-line {
            width: 60%;
            margin: 0 auto 3px auto;
            border-bottom: 1px solid #334155;
          }
          .remark-box {
            border: 1px solid #e2e8f0;
            background: #fafbfc;
            border-radius: 4px;
            padding: 6px 10px;
            margin-top: 8px;
            font-size: 11px;
          }
        </style>
      </head>
      <body>
    """.trimIndent())

    for (pageItems in pages) {
      sb.append("<div class=\"page-sheet\">")

      // 1. Clinic Letterhead
      sb.append("""
        <table class="header-table">
          <tr>
            <td style="width: 72%; vertical-align: top;">
              <div class="clinic-name-mm">“${clinic.clinicNameMm.replace("“", "").replace("”", "").ifBlank { "ရွှေခေတ်" }}” ${clinic.clinicSubtitleMm.ifBlank { "အထွေထွေရောဂါကုဆေးခန်း" }}</div>
              <div class="clinic-name-en">${clinic.clinicNameEn.ifBlank { "Shwe Khit General Clinic & Diagnostic Lab" }}</div>
              <div class="clinic-contact">${clinic.clinicAddress.ifBlank { "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့" }} • Ph: ${clinic.clinicPhone.ifBlank { "09-421001122, 09-790112233" }}</div>
            </td>
            <td style="width: 25%; text-align: right; vertical-align: middle;">
              ${if (clinic.clinicLogoBase64.isNotBlank()) "<img src='data:image/png;base64,${clinic.clinicLogoBase64}' class='clinic-logo' style='max-height: 48px; max-width: 60px; object-fit: contain;'/>" else ""}
            </td>
          </tr>
        </table>
      """.trimIndent())

      // 2. Banner: Only on actual result reports (Omit on blank letterhead to avoid cluttering analyzer print)
      if (!isBlankLetterhead) {
        sb.append("<div class=\"report-banner\">LABORATORY INVESTIGATION REPORT</div>")
      }

      // 3. Patient Demographics Box (Omit completely on blank letterhead)
      if (!isBlankLetterhead) {
        val refDocDisplay = if (order.referringDoctorName.isNotBlank()) {
          if (order.referringDoctorName.startsWith("Dr", ignoreCase = true)) order.referringDoctorName
          else "Dr. ${order.referringDoctorName}"
        } else {
          if (clinic.doctorName.startsWith("Dr", ignoreCase = true)) clinic.doctorName
          else "Dr. ${clinic.doctorName}"
        }
        sb.append("""
          <div class="patient-box">
            <div class="patient-grid">
              <div class="patient-row">
                <div class="patient-cell" style="width: 50%;"><strong>Patient Name:</strong> ${order.patientName}</div>
                <div class="patient-cell" style="width: 50%;"><strong>Lab Order No:</strong> ${order.orderNumber}</div>
              </div>
              <div class="patient-row">
                <div class="patient-cell"><strong>Patient ID / Reg No:</strong> ${order.patientHn}</div>
                <div class="patient-cell"><strong>Date & Time:</strong> ${order.orderDate} ${order.orderTime}</div>
              </div>
              <div class="patient-row">
                <div class="patient-cell"><strong>Age / Gender:</strong> ${order.patientAge} / ${order.patientGender}</div>
                <div class="patient-cell"><strong>Referring Doctor:</strong> $refDocDisplay</div>
              </div>
            </div>
          </div>
        """.trimIndent())
      }

      // 4. Page Body
      sb.append("<div class=\"page-body\">")

      if (isBlankLetterhead) {
        // Mode A: Clean pre-printed sheet for feeding into CBC Analyzer
        sb.append("<div style=\"flex: 1 1 auto; min-height: 175mm;\"></div>")
      } else {
        val hasSingleSlipItem = pageItems.size == 1 &&
          (pageItems[0].isAnalyzerSlipOnly || pageItems[0].preferSlipPrint || !pageItems[0].attachedImageUri.isNullOrBlank())

        if (hasSingleSlipItem && pageItems[0].slipImageUri.isNotBlank()) {
          // Mode B-1: Machine Slip / Scan Photo Centered in Plain Sheet
          val targetItem = pageItems[0]
          val imgSrc = convertImageToBase64DataUri(context, targetItem.slipImageUri)
          sb.append("""
            <div style="font-weight: bold; font-size: 12px; color: #0b3954; margin: 4px 0 6px 0; border-left: 3px solid #0056b3; padding-left: 6px;">
              🔬 ${targetItem.templateName} — Automated Analyzer Direct Slip
            </div>
            <div style="text-align: center; margin: 6px auto; flex: 1 1 auto; display: flex; align-items: center; justify-content: center;">
              <img src="$imgSrc" style="max-width: 95%; max-height: 165mm; object-fit: contain; border: 1px solid #cbd5e1; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.08);" />
            </div>
          """.trimIndent())

          if (targetItem.remarks.isNotBlank()) {
            sb.append("<div class=\"remark-box\"><strong>${targetItem.templateName} Remarks:</strong> ${targetItem.remarks}</div>")
          }
        } else {
          // Mode B-2: Unified High-Contrast Parameters Table
          sb.append("""
            <table class="param-table">
              <thead>
                <tr>
                  <th style="width: 44%;">Investigation / Test Parameter</th>
                  <th style="width: 22%;">Result</th>
                  <th style="width: 14%;">Unit</th>
                  <th style="width: 20%;">Reference Interval</th>
                </tr>
              </thead>
              <tbody>
          """.trimIndent())

          for (testItem in pageItems) {
            if (pageItems.size > 1 || testItem.parameters.size > 1) {
              sb.append("""
                <tr class="panel-section-row">
                  <td colspan="4"><strong>${testItem.templateName}</strong></td>
                </tr>
              """.trimIndent())
            }

            var currentSection = ""
            for (param in testItem.parameters) {
              if (param.section.isNotBlank() && param.section != currentSection) {
                currentSection = param.section
                sb.append("""
                  <tr style="background-color: #f8fafc; border-top: 1px solid #cbd5e1; border-bottom: 1px solid #cbd5e1;">
                    <td colspan="4" style="padding: 4px 10px; font-weight: bold; color: #1e3a8a; font-size: 11px;">
                      ▶ $currentSection
                    </td>
                  </tr>
                """.trimIndent())
              }

              val arrowBadge = when (param.flag.uppercase()) {
                "HIGH" -> "<span style='color: #dc2626; font-weight: bold; font-size: 11px; margin-left: 4px;'>▲</span>"
                "LOW" -> "<span style='color: #0284c7; font-weight: bold; font-size: 11px; margin-left: 4px;'>▼</span>"
                else -> ""
              }
              sb.append("""
                <tr>
                  <td style="padding-left: ${if (currentSection.isNotBlank()) "24px" else "14px"};">${param.name}</td>
                  <td><strong style="color: #0f172a;">${param.result.ifBlank { "-" }}</strong>$arrowBadge</td>
                  <td>${param.unit.ifBlank { "-" }}</td>
                  <td>${param.referenceRange.ifBlank { "-" }}</td>
                </tr>
              """.trimIndent())
            }
          }

          sb.append("</tbody></table>")

          // Remarks for items on this page
          for (testItem in pageItems) {
            if (testItem.remarks.isNotBlank()) {
              sb.append("""
                <div class="remark-box">
                  <strong>${testItem.templateName} Remarks:</strong> ${testItem.remarks}
                </div>
              """.trimIndent())
            }
          }

          if (order.notes.isNotBlank() && pageItems == pages.lastOrNull()) {
            sb.append("""
              <div class="remark-box" style="margin-top: 8px;">
                <strong>General Clinical Notes:</strong> ${order.notes}
              </div>
            """.trimIndent())
          }
        }
      }

      sb.append("</div>") // Close .page-body

      // 5. Pinned Page Footer (Signatures & Doctor Seal)
      sb.append("""
        <div class="page-footer">
          <table class="sign-table">
            <tr>
              ${if (!isBlankLetterhead && order.showTechnicianSign && order.technicianName.isNotBlank()) """
              <td class="sign-cell">
                <div class="sign-line"></div>
                <strong>${order.technicianName}</strong><br/>
                <span style="color: #64748b; font-size: 9px;">Medical Laboratory Technologist</span>
              </td>
              """ else """
              <td class="sign-cell" style="width: 50%;"></td>
              """}
              ${if (isBlankLetterhead || order.showDoctorSeal) """
              <td class="sign-cell" style="${if (!order.showTechnicianSign || isBlankLetterhead) "width: 50%; float: right;" else ""}">
                <div class="sign-line"></div>
                <strong style="color: #0f172a;">$effectivePathologistName</strong><br/>
                ${if (effectivePathologistDegree.isNotBlank()) "<span style='font-size: 9.5px; color: #334155;'>$effectivePathologistDegree</span><br/>" else ""}
                ${if (effectivePathologistSama.isNotBlank()) "<span style='color: #475569; font-size: 9px;'>(S.A.M.A: $effectivePathologistSama)</span><br/>" else ""}
                ${if (effectivePathologistTitle.isNotBlank()) "<span style='color: #0f172a; font-size: 9px; font-weight: 500;'>$effectivePathologistTitle</span>" else ""}
              </td>
              """ else """
              <td class="sign-cell" style="${if (!order.showTechnicianSign) "width: 50%; float: right;" else ""}">
                <div class="sign-line" style="border-bottom: 1px dashed #94a3b8;"></div>
                <span style="color: #94a3b8; font-size: 9.5px;">(Attending Doctor Signature & Seal)</span>
              </td>
              """}
            </tr>
          </table>
          <div style="text-align: center; margin-top: 5px; font-size: 8.5px; color: #94a3b8;">
            This investigation is processed by the Laboratory Services at ${clinic.clinicNameMm}.
          </div>
        </div>
      """.trimIndent())

      sb.append("</div>") // Close .page-sheet
    }

    sb.append("""
      </body>
      </html>
    """.trimIndent())

    return sb.toString()
  }

  /**
   * Partitions lab test items into pages.
   * CBC, Urine RE/ME, Stool RE/ME, and items with attached analyzer slips get their own dedicated page.
   * Smaller biochemistry panels (LFT, Lipid, RFT, etc.) are comfortably grouped together on a shared page.
   */
  private fun partitionItemsIntoPages(items: List<LabOrderItem>): List<List<LabOrderItem>> {
    val pages = mutableListOf<List<LabOrderItem>>()
    val currentGeneral = mutableListOf<LabOrderItem>()
    var currentParamCount = 0

    for (it in items) {
      val isStandalone = it.isAnalyzerSlipOnly ||
        it.preferSlipPrint ||
        !it.attachedImageUri.isNullOrBlank() ||
        it.testName.contains("CBC", ignoreCase = true) ||
        it.testName.contains("Complete Blood", ignoreCase = true) ||
        it.testName.contains("Urine RE", ignoreCase = true) ||
        it.testName.contains("Stool RE", ignoreCase = true) ||
        it.parameters.size >= 10

      if (isStandalone) {
        if (currentGeneral.isNotEmpty()) {
          pages.add(currentGeneral.toList())
          currentGeneral.clear()
          currentParamCount = 0
        }
        pages.add(listOf(it))
      } else {
        val count = it.parameters.size.coerceAtLeast(1)
        if (currentGeneral.isNotEmpty() && (currentParamCount + count) > 16) {
          pages.add(currentGeneral.toList())
          currentGeneral.clear()
          currentParamCount = 0
        }
        currentGeneral.add(it)
        currentParamCount += count
      }
    }

    if (currentGeneral.isNotEmpty()) {
      pages.add(currentGeneral.toList())
    }

    return if (pages.isEmpty() && items.isNotEmpty()) listOf(items) else pages
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

  /**
   * Print an attached analyzer slip, image, or document directly to Android System Print (Canon, HP, PDF).
   */
  fun printAttachedImage(
    context: Context,
    imageUri: String,
    jobTitle: String = "Lab_Analyzer_Slip"
  ) {
    if (imageUri.isBlank()) {
      Toast.makeText(context, "No slip file attached to print", Toast.LENGTH_SHORT).show()
      return
    }
    Handler(Looper.getMainLooper()).post {
      try {
        val imgSrc = if (imageUri.startsWith("http") || imageUri.startsWith("file://") || imageUri.startsWith("data:")) {
          imageUri
        } else {
          "file://$imageUri"
        }
        val html = """
          <!DOCTYPE html>
          <html>
          <head>
            <meta charset="utf-8">
            <style>
              @page { size: auto; margin: 8mm; }
              body { margin: 0; padding: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #fff; }
              .slip-header { font-family: sans-serif; font-size: 13px; font-weight: bold; color: #333; margin-bottom: 12px; text-align: center; }
              img { max-width: 95%; max-height: 90vh; object-fit: contain; border: 1px solid #ccc; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
            </style>
          </head>
          <body>
            <div class="slip-header">Automated Analyzer Result Slip / Attached Document</div>
            <img src="$imgSrc" />
          </body>
          </html>
        """.trimIndent()
        val webView = WebView(context)
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
              val printAdapter = webView.createPrintDocumentAdapter(jobTitle)
              val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
              printManager.print(jobTitle, printAdapter, printAttributes)
            } else {
              Toast.makeText(context, "Print service not available", Toast.LENGTH_SHORT).show()
            }
          }
        }
        webView.loadDataWithBaseURL("file:///", html, "text/html", "UTF-8", null)
      } catch (e: Exception) {
        Toast.makeText(context, "Slip print error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
      }
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
