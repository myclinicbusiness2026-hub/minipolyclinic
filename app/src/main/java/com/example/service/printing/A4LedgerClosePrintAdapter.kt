package com.example.service.printing

import android.content.Context
import android.graphics.*
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
import com.example.model.ClinicExpense
import com.example.model.ClinicSettings
import com.example.model.ConsultationRecord
import com.example.model.Invoice
import com.example.model.Patient
import com.example.model.PaymentMethod
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A4LedgerClosePrintAdapter
 * 
 * Generates an ISO A4 Daily / Monthly Financial Close Report with OPD Patient records,
 * diagnosis, consultation/pharmacy/procedure breakdown, expense vouchers,
 * and comprehensive cash-in-hand accounting summary.
 */
class A4LedgerClosePrintAdapter(
  private val context: Context,
  private val reportTitle: String,
  private val periodLabel: String,
  private val invoices: List<Invoice>,
  private val patients: List<Patient>,
  private val consultations: List<ConsultationRecord>,
  private val expenses: List<ClinicExpense>,
  private val clinicSettings: ClinicSettings?,
  private val preparedByName: String
) : PrintDocumentAdapter() {

  private var totalPages: Int = 1
  private val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

  // Pre-calculate pagination chunks
  // Page 1 has header (~110pt). Subsequent pages don't have main header, only mini-header (~30pt).
  // Summary box + Signatures take ~170pt on the final page.
  private val invoiceChunks = mutableListOf<List<Invoice>>()
  private var expensesChunkPage = 0 // page index where expenses start

  init {
    calculatePages()
  }

  private fun calculatePages() {
    invoiceChunks.clear()
    val availableHeightP1 = 842f - 72f - 110f // ~660pt
    val availableHeightSubsequent = 842f - 72f - 40f // ~730pt
    val rowHeight = 20f

    var currentInvoices = invoices.toList()
    var isP1 = true

    while (currentInvoices.isNotEmpty()) {
      val avail = if (isP1) availableHeightP1 else availableHeightSubsequent
      // If it's the last page, leave room for summary box (180pt) if needed
      val maxRows = (avail / rowHeight).toInt().coerceAtLeast(5)

      if (currentInvoices.size <= maxRows) {
        invoiceChunks.add(currentInvoices)
        currentInvoices = emptyList()
      } else {
        val chunk = currentInvoices.take(maxRows)
        invoiceChunks.add(chunk)
        currentInvoices = currentInvoices.drop(maxRows)
      }
      isP1 = false
    }

    if (invoiceChunks.isEmpty()) {
      invoiceChunks.add(emptyList())
    }

    // Check if summary box and expenses fit on the last page of invoiceChunks
    val lastChunkSize = invoiceChunks.last().size
    val lastAvail = if (invoiceChunks.size == 1) availableHeightP1 else availableHeightSubsequent
    val neededForSummary = 180f + (expenses.size * rowHeight) + 30f

    if ((lastChunkSize * rowHeight) + neededForSummary > lastAvail && (lastChunkSize > 0 || expenses.isNotEmpty())) {
      // Need an extra summary page
      totalPages = invoiceChunks.size + 1
    } else {
      totalPages = invoiceChunks.size
    }
    totalPages = totalPages.coerceAtLeast(1)
  }

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

    calculatePages()

    val info = PrintDocumentInfo.Builder("ShweKhit_LedgerClose.pdf")
      .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
      .setPageCount(totalPages)
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
      val a4W = 595
      val a4H = 842

      for (pageIdx in 0 until totalPages) {
        if (cancellationSignal?.isCanceled == true) {
          callback.onWriteCancelled()
          return
        }

        val pageInfo = PdfDocument.PageInfo.Builder(a4W, a4H, pageIdx + 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawReportPage(canvas, pageIdx + 1, totalPages, a4W.toFloat(), a4H.toFloat())

        pdfDoc.finishPage(page)
      }

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

  private fun drawReportPage(canvas: Canvas, pageNum: Int, totalPages: Int, w: Float, h: Float) {
    val mLeft = 32f
    val mRight = w - 32f
    val contentW = mRight - mLeft
    var curY = 32f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // --- HEADER (Page 1 Full Header, Subsequent Mini Header) ---
    if (pageNum == 1) {
      val clinicNameMm = clinicSettings?.clinicNameMm ?: "ရွှေခေတ်"
      val clinicSubtitle = clinicSettings?.clinicSubtitleMm ?: "အထွေထွေရောဂါကုဆေးခန်း"
      val doctorName = clinicSettings?.primaryDoctorName ?: "Dr. Nyan Kaung Sett"
      val doctorDegree = clinicSettings?.primaryDoctorDegree ?: "M.B.,B.S (MDY)"
      val doctorSama = clinicSettings?.primaryDoctorSama ?: "ဆမ - ၄၇၄၁၄"
      val address = clinicSettings?.clinicAddress ?: "အမှတ်(၁၂)၊ လမ်းမတော်လမ်း၊ မန္တလေးမြို့"
      val phone = clinicSettings?.primaryPhone ?: "09-401523447"

      // Clinic Brand
      paint.color = Color.rgb(15, 118, 110) // Medical Green Dark
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 14.5f
      canvas.drawText("$clinicNameMm $clinicSubtitle", mLeft, curY + 14f, paint)

      paint.color = Color.rgb(30, 41, 59)
      paint.textSize = 9.5f
      val drLine = "$doctorName $doctorDegree ($doctorSama)"
      val drW = paint.measureText(drLine)
      canvas.drawText(drLine, mRight - drW, curY + 12f, paint)

      curY += 20f

      paint.color = Color.rgb(100, 116, 139)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      paint.textSize = 8.5f
      canvas.drawText("လိပ်စာ: $address  •  ဖုန်း: $phone", mLeft, curY + 10f, paint)

      curY += 16f

      // Decorative divider
      paint.color = Color.rgb(15, 118, 110)
      paint.strokeWidth = 1.5f
      canvas.drawLine(mLeft, curY, mRight, curY, paint)
      paint.color = Color.rgb(203, 213, 225)
      paint.strokeWidth = 0.5f
      canvas.drawLine(mLeft, curY + 2.5f, mRight, curY + 2.5f, paint)

      curY += 8f

      // Title Banner
      paint.style = Paint.Style.FILL
      paint.color = Color.rgb(240, 253, 250) // Soft mint green
      val bannerRect = RectF(mLeft, curY, mRight, curY + 26f)
      canvas.drawRoundRect(bannerRect, 4f, 4f, paint)

      paint.style = Paint.Style.STROKE
      paint.color = Color.rgb(204, 251, 241)
      paint.strokeWidth = 1f
      canvas.drawRoundRect(bannerRect, 4f, 4f, paint)
      paint.style = Paint.Style.FILL

      paint.color = Color.rgb(13, 148, 136)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 10.5f
      canvas.drawText(reportTitle, mLeft + 8f, curY + 17f, paint)

      val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
      val rightMeta = "$periodLabel  |  Print: $timeStr"
      paint.textSize = 8.5f
      paint.color = Color.rgb(51, 65, 85)
      val metaW = paint.measureText(rightMeta)
      canvas.drawText(rightMeta, mRight - 8f - metaW, curY + 17f, paint)

      curY += 32f
    } else {
      // Mini Header for subsequent pages
      paint.color = Color.rgb(100, 116, 139)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 8.5f
      canvas.drawText("ရွှေခေတ်ဆေးခန်း • $reportTitle ($periodLabel)", mLeft, curY + 10f, paint)

      paint.color = Color.rgb(226, 232, 240)
      paint.strokeWidth = 0.8f
      canvas.drawLine(mLeft, curY + 14f, mRight, curY + 14f, paint)

      curY += 22f
    }

    // --- OPD INVOICES TABLE ---
    val pageChunk = invoiceChunks.getOrNull(pageNum - 1) ?: emptyList()
    val isSummaryOnlyPage = pageNum > invoiceChunks.size

    if (!isSummaryOnlyPage && pageChunk.isNotEmpty()) {
      // Table Header Row
      val colW_No = 22f
      val colW_Time = 42f
      val colW_Name = 115f
      val colW_Diag = 160f
      val colW_Amt = 65f
      val colW_Method = 55f
      val colW_Status = contentW - (colW_No + colW_Time + colW_Name + colW_Diag + colW_Amt + colW_Method)

      val headerH = 18f
      paint.color = Color.rgb(241, 245, 249) // Slate-100
      canvas.drawRect(mLeft, curY, mRight, curY + headerH, paint)

      paint.color = Color.rgb(203, 213, 225)
      paint.strokeWidth = 0.5f
      paint.style = Paint.Style.STROKE
      canvas.drawRect(mLeft, curY, mRight, curY + headerH, paint)
      paint.style = Paint.Style.FILL

      paint.color = Color.rgb(30, 41, 59)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 7.5f

      var cX = mLeft + 3f
      canvas.drawText("စဉ်", cX, curY + 12f, paint); cX += colW_No
      canvas.drawText("ချိန်/ဘောက်ချာ", cX, curY + 12f, paint); cX += colW_Time
      canvas.drawText("လူနာအမည် / HN", cX, curY + 12f, paint); cX += colW_Name
      canvas.drawText("ရောဂါရာဇဝင် / ကုသမှုအသုံးစာရင်း", cX, curY + 12f, paint); cX += colW_Diag
      canvas.drawText("ကျသင့်ငွေ (Ks)", cX, curY + 12f, paint); cX += colW_Amt
      canvas.drawText("ငွေချေပုံစံ", cX, curY + 12f, paint); cX += colW_Method
      canvas.drawText("အခြေအနေ", cX, curY + 12f, paint)

      curY += headerH

      // Calculate starting index for "No."
      val priorCount = invoiceChunks.take(pageNum - 1).sumOf { it.size }

      pageChunk.forEachIndexed { idx, inv ->
        val rowNo = priorCount + idx + 1
        val isEven = idx % 2 == 0
        val rowH = 19f

        if (isEven) {
          paint.color = Color.rgb(255, 255, 255)
        } else {
          paint.color = Color.rgb(248, 250, 252)
        }
        canvas.drawRect(mLeft, curY, mRight, curY + rowH, paint)

        // Bottom border line
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.5f
        canvas.drawLine(mLeft, curY + rowH, mRight, curY + rowH, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.color = Color.rgb(51, 65, 85)

        var rX = mLeft + 3f
        // 1. No.
        canvas.drawText("$rowNo", rX, curY + 13f, paint); rX += colW_No

        // 2. Time & Short Voucher
        val timeDisplay = if (inv.createdAt.length >= 16) inv.createdAt.substring(11, 16) else inv.invoiceNo.takeLast(6)
        canvas.drawText(timeDisplay, rX, curY + 13f, paint); rX += colW_Time

        // 3. Patient Name & HN
        val patient = patients.find { it.id == inv.patientId }
        val hnStr = patient?.hn?.takeLast(7) ?: ""
        val nameHn = "${inv.patientName.take(13)}${if (hnStr.isNotBlank()) " ($hnStr)" else ""}"
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(nameHn, rX, curY + 13f, paint); rX += colW_Name
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // 4. Diagnosis / Clinical items
        val consult = consultations.find { it.patientId == inv.patientId && it.timestamp.take(10) == inv.dateString }
        val diag = when {
          !consult?.diagnosis.isNullOrBlank() -> consult?.diagnosis!!
          !consult?.chiefComplaint.isNullOrBlank() -> consult?.chiefComplaint!!
          inv.items.isNotEmpty() -> inv.items.joinToString(", ") { it.name }.take(32)
          else -> "General OPD Consultation"
        }
        canvas.drawText(diag.take(30), rX, curY + 13f, paint); rX += colW_Diag

        // 5. Total Amount (Right aligned in its column)
        val amtStr = currencyFmt.format(inv.grandTotal.toInt())
        val amtW = paint.measureText(amtStr)
        canvas.drawText(amtStr, rX + colW_Amt - amtW - 6f, curY + 13f, paint); rX += colW_Amt

        // 6. Payment Method
        val payMethod = when (inv.paymentMethod) {
          PaymentMethod.CASH -> "Cash"
          PaymentMethod.KPAY -> "KPay"
          PaymentMethod.WAVEPAY -> "Wave"
          PaymentMethod.BANK_TRANSFER -> "Bank"
        }
        canvas.drawText(payMethod, rX, curY + 13f, paint); rX += colW_Method

        // 7. Status
        if (inv.isPaid) {
          paint.color = Color.rgb(22, 101, 52)
          canvas.drawText("PAID", rX, curY + 13f, paint)
        } else {
          paint.color = Color.rgb(220, 38, 38)
          canvas.drawText("UNPAID", rX, curY + 13f, paint)
        }

        curY += rowH
      }

      curY += 8f
    }

    // --- EXPENSES TABLE (If on last page or summary page and expenses exist) ---
    val isFinalPage = pageNum == totalPages
    if (isFinalPage && expenses.isNotEmpty()) {
      paint.color = Color.rgb(180, 83, 9) // Amber-700
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 8.5f
      canvas.drawText("ဆေးခန်း အသုံးစရိတ်များ (Clinic Expenses - ${expenses.size} ခု):", mLeft, curY + 10f, paint)
      curY += 14f

      // Expense rows
      expenses.take(10).forEachIndexed { expIdx, exp ->
        val expRowH = 15f
        paint.color = Color.rgb(254, 243, 199) // Light amber
        canvas.drawRect(mLeft, curY, mRight, curY + expRowH, paint)

        paint.color = Color.rgb(146, 64, 14)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f

        val expTitle = "${expIdx + 1}. [${exp.voucherNo}] ${exp.title}${if (exp.payee.isNotBlank()) " - ${exp.payee}" else ""}"
        canvas.drawText(expTitle.take(50), mLeft + 4f, curY + 11f, paint)

        val expAmtStr = "-${currencyFmt.format(exp.amount.toInt())} Ks (${exp.paymentMethod})"
        val expAmtW = paint.measureText(expAmtStr)
        canvas.drawText(expAmtStr, mRight - expAmtW - 6f, curY + 11f, paint)

        curY += expRowH + 2f
      }
      curY += 6f
    }

    // --- FINANCIAL SUMMARY BOX (On the Final Page) ---
    if (isFinalPage) {
      curY = (curY).coerceAtMost(h - 210f)

      // Summary Box Container
      val summaryH = 120f
      val sumRect = RectF(mLeft, curY, mRight, curY + summaryH)

      paint.style = Paint.Style.FILL
      paint.color = Color.rgb(248, 250, 252) // Slate-50
      canvas.drawRoundRect(sumRect, 6f, 6f, paint)

      paint.style = Paint.Style.STROKE
      paint.color = Color.rgb(148, 163, 184) // Slate-400
      paint.strokeWidth = 1f
      canvas.drawRoundRect(sumRect, 6f, 6f, paint)
      paint.style = Paint.Style.FILL

      // Summary Header Strip
      val sumHeadH = 18f
      paint.color = Color.rgb(15, 118, 110)
      val headRect = RectF(mLeft, curY, mRight, curY + sumHeadH)
      canvas.drawRoundRect(headRect, 6f, 6f, paint)
      canvas.drawRect(mLeft, curY + 10f, mRight, curY + sumHeadH, paint) // square bottom corners

      paint.color = Color.rgb(255, 255, 255)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 8.5f
      canvas.drawText("ငွေစာရင်းနှင့် နေ့ချုပ်/လချုပ် အနှစ်ချုပ် (FINANCIAL CLOSE ACCOUNTING SUMMARY)", mLeft + 8f, curY + 13f, paint)

      // Aggregates
      val paidInvoices = invoices.filter { it.isPaid }
      val totalGrossConsultation = paidInvoices.sumOf { it.consultationFee }
      val totalGrossPharmacy = paidInvoices.sumOf { it.medicineTotal }
      val totalGrossProcedure = paidInvoices.sumOf { it.procedureTotal }
      val totalDiscounts = paidInvoices.sumOf { it.discountAmount }
      val totalIncome = paidInvoices.sumOf { it.grandTotal }
      val totalExpensesAmt = expenses.sumOf { it.amount }
      val netBalance = totalIncome - totalExpensesAmt

      val cashIncome = paidInvoices.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.grandTotal }
      val digitalIncome = totalIncome - cashIncome

      var sY = curY + sumHeadH + 14f
      val midX = mLeft + (contentW / 2f)

      // Left Column: Breakdown
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      paint.textSize = 8f
      paint.color = Color.rgb(51, 65, 85)

      fun drawSummaryRow(label: String, value: String, x: Float, rightX: Float, y: Float, isBold: Boolean = false, valColor: Int = Color.rgb(30, 41, 59)) {
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        canvas.drawText(label, x, y, paint)
        paint.color = valColor
        val valW = paint.measureText(value)
        canvas.drawText(value, rightX - valW, y, paint)
      }

      drawSummaryRow("၁။ ဆရာဝန်ကြည့်ရှုခ (Consultation):", "${currencyFmt.format(totalGrossConsultation.toInt())} Ks", mLeft + 8f, midX - 12f, sY)
      drawSummaryRow("၂။ ဆေးဝါးရောင်းရငွေ (Pharmacy Sales):", "${currencyFmt.format(totalGrossPharmacy.toInt())} Ks", mLeft + 8f, midX - 12f, sY + 14f)
      drawSummaryRow("၃။ ဒဏ်ရာ/ကုသမှု (Procedures):", "${currencyFmt.format(totalGrossProcedure.toInt())} Ks", mLeft + 8f, midX - 12f, sY + 28f)
      drawSummaryRow("၄။ ပေးခဲ့သော လျှော့စျေး (Discounts):", "-${currencyFmt.format(totalDiscounts.toInt())} Ks", mLeft + 8f, midX - 12f, sY + 42f, valColor = Color.rgb(220, 38, 38))
      drawSummaryRow("စုစုပေါင်းလူနာ (Total Patients):", "${invoices.size} ဦး (Paid: ${paidInvoices.size})", mLeft + 8f, midX - 12f, sY + 58f, isBold = true)

      // Vertical separator
      paint.color = Color.rgb(203, 213, 225)
      paint.strokeWidth = 0.8f
      canvas.drawLine(midX, curY + sumHeadH + 6f, midX, curY + summaryH - 6f, paint)

      // Right Column: Cash & Net Balance
      drawSummaryRow("ငွေသားလက်ခံရရှိငွေ (Cash):", "${currencyFmt.format(cashIncome.toInt())} Ks", midX + 12f, mRight - 8f, sY)
      drawSummaryRow("ဒစ်ဂျစ်တယ်ငွေ (KPay/Wave):", "${currencyFmt.format(digitalIncome.toInt())} Ks", midX + 12f, mRight - 8f, sY + 14f)
      drawSummaryRow("ဆေးခန်း စုစုပေါင်းအသုံးစရိတ်:", "-${currencyFmt.format(totalExpensesAmt.toInt())} Ks", midX + 12f, mRight - 8f, sY + 28f, valColor = Color.rgb(180, 83, 9))

      // NET CASH BALANCE BANNER
      val netRect = RectF(midX + 8f, sY + 38f, mRight - 8f, sY + 68f)
      paint.color = if (netBalance >= 0) Color.rgb(220, 252, 231) else Color.rgb(254, 226, 226)
      canvas.drawRoundRect(netRect, 4f, 4f, paint)

      paint.color = if (netBalance >= 0) Color.rgb(22, 101, 52) else Color.rgb(153, 27, 27)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 8f
      canvas.drawText("အသားတင် လက်ကျန်ငွေ (NET CASH):", midX + 14f, sY + 54f, paint)

      paint.textSize = 10.5f
      val netStr = "${currencyFmt.format(netBalance.toInt())} Ks"
      val netW = paint.measureText(netStr)
      canvas.drawText(netStr, mRight - 14f - netW, sY + 55f, paint)

      curY += summaryH + 18f

      // Signatures
      val sigY = curY + 16f
      paint.color = Color.rgb(51, 65, 85)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      paint.textSize = 8f

      // Prepared By
      canvas.drawLine(mLeft + 10f, sigY, mLeft + 160f, sigY, paint)
      canvas.drawText("စာရင်းစစ်/ပြုစုသူ: $preparedByName", mLeft + 10f, sigY + 12f, paint)

      // Verified By Doctor
      canvas.drawLine(mRight - 160f, sigY, mRight - 10f, sigY, paint)
      val docSigTitle = "တာဝန်ခံ ဆရာဝန် (Dr. Nyan Kaung Sett)"
      val docSigW = paint.measureText(docSigTitle)
      canvas.drawText(docSigTitle, mRight - 10f - docSigW, sigY + 12f, paint)
    }

    // --- FOOTER (Page numbering) ---
    val footerY = h - 18f
    paint.color = Color.rgb(148, 163, 184)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 7.5f

    canvas.drawText("Shwe Khit Clinic System • Official Ledger Record", mLeft, footerY, paint)
    val pageStr = "စာမျက်နှာ $pageNum / $totalPages"
    val pageW = paint.measureText(pageStr)
    canvas.drawText(pageStr, mRight - pageW, footerY, paint)
  }
}

/**
 * Helper function to trigger Android's System Print Spooler for A4 Close Reports
 */
fun triggerLedgerCloseA4Print(
  context: Context,
  reportTitle: String,
  periodLabel: String,
  invoices: List<Invoice>,
  patients: List<Patient>,
  consultations: List<ConsultationRecord>,
  expenses: List<ClinicExpense>,
  clinicSettings: ClinicSettings?,
  preparedByName: String
) {
  try {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager != null) {
      val cleanPeriod = periodLabel.replace("[^a-zA-Z0-9]".toRegex(), "_")
      val jobName = "ShweKhit_LedgerClose_${cleanPeriod}_${SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())}"

      val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .setMinMargins(PrintAttributes.Margins(150, 150, 150, 150))
        .build()

      val adapter = A4LedgerClosePrintAdapter(
        context = context,
        reportTitle = reportTitle,
        periodLabel = periodLabel,
        invoices = invoices,
        patients = patients,
        consultations = consultations,
        expenses = expenses,
        clinicSettings = clinicSettings,
        preparedByName = preparedByName
      )

      printManager.print(jobName, adapter, printAttributes)
    } else {
      Toast.makeText(context, "System Print Service not available", Toast.LENGTH_SHORT).show()
    }
  } catch (e: Exception) {
    Toast.makeText(context, "Print error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
  }
}
