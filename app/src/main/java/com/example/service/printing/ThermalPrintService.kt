package com.example.service.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.ClinicSettings
import com.example.model.Invoice
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * ThermalPrintService
 *
 * High-Resolution ESC/POS Thermal Receipt Generator for 58mm (384px) & 80mm (576px) POS Printers.
 *
 * Enhancements:
 * - Perfectly balanced symmetrical margins (20px left & right).
 * - Renders custom uploaded clinic logo from settings if available.
 * - S.A.M.A. registration number excluded from cashier vouchers.
 * - Modern 2-Line POS item layout (Title on Line 1; Qty x Rate & Total on Line 2).
 * - Comfortable typography (16-20px font, 1.25x line height) for crisp thermal readability.
 * - Chunked ESC/POS raster streaming (GS v 0) preventing buffer overflows.
 */
object ThermalPrintService {

  private const val ESC: Byte = 0x1B
  private const val GS: Byte = 0x1D
  private const val LF: Byte = 0x0A

  /**
   * Generates standard binary ESC/POS raster byte array ready to send to Bluetooth/USB thermal printers.
   */
  fun generateEscPosInvoiceBytes(
    context: Context? = null,
    invoice: Invoice,
    patientHn: String = "HN-0000",
    cashierName: String = "Cashier",
    is80mm: Boolean = true,
    settings: ClinicSettings? = null
  ): ByteArray {
    val bitmap = generateEscPosInvoiceBitmap(
      context = context,
      invoice = invoice,
      patientHn = patientHn,
      cashierName = cashierName,
      is80mm = is80mm,
      settings = settings
    )
    return bitmapToEscPosRasterBytes(bitmap)
  }

  /**
   * Overload for backwards compatibility without Context.
   */
  fun generateEscPosInvoiceBytes(
    invoice: Invoice,
    patientHn: String = "HN-0000",
    cashierName: String = "Cashier",
    is80mm: Boolean = true,
    settings: ClinicSettings? = null
  ): ByteArray {
    return generateEscPosInvoiceBytes(
      context = null,
      invoice = invoice,
      patientHn = patientHn,
      cashierName = cashierName,
      is80mm = is80mm,
      settings = settings
    )
  }

  /**
   * Converts a formatted Receipt Bitmap into standard ESC/POS Raster Bit Image commands (GS v 0).
   * Slices the bitmap into vertical chunks of max 200 rows.
   */
  fun bitmapToEscPosRasterBytes(bitmap: Bitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val widthBytes = (width + 7) / 8

    val output = ByteArrayOutputStream()

    // 1. Initialize printer
    output.write(byteArrayOf(ESC, 0x40)) // ESC @

    // 2. Set line spacing to 0 for seamless vertical raster rendering
    output.write(byteArrayOf(ESC, 0x33, 0x00)) // ESC 3 0

    val maxSliceHeight = 200
    var yOffset = 0

    while (yOffset < height) {
      val sliceHeight = minOf(maxSliceHeight, height - yOffset)
      val xL = (widthBytes and 0xFF).toByte()
      val xH = ((widthBytes shr 8) and 0xFF).toByte()
      val yL = (sliceHeight and 0xFF).toByte()
      val yH = ((sliceHeight shr 8) and 0xFF).toByte()

      // GS v 0 0 xL xH yL yH
      output.write(byteArrayOf(GS, 0x76, 0x30, 0x00, xL, xH, yL, yH))

      for (y in 0 until sliceHeight) {
        val currentY = yOffset + y
        for (xByte in 0 until widthBytes) {
          var b = 0
          for (bit in 0 until 8) {
            val pixelX = xByte * 8 + bit
            if (pixelX < width) {
              val pixel = bitmap.getPixel(pixelX, currentY)
              val a = (pixel shr 24) and 0xFF
              if (a > 30) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val bVal = pixel and 0xFF
                // High-contrast monochrome conversion: preserve colored logo elements like yellow star and green emblems
                val isYellowStar = (r > 150 && g > 130 && bVal < 140)
                val isColorSaturation = (maxOf(r, g, bVal) - minOf(r, g, bVal)) > 30
                val isNotPaperWhite = (r < 240 || g < 240 || bVal < 240)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * bVal).toInt()
                if (isYellowStar || (isColorSaturation && isNotPaperWhite) || luminance < 175) {
                  b = b or (1 shl (7 - bit))
                }
              }
            }
          }
          output.write(b)
        }
      }
      yOffset += sliceHeight
    }

    // 3. Reset line spacing to default (ESC 2)
    output.write(byteArrayOf(ESC, 0x32))

    // 4. Feed 4 lines and execute paper cut
    output.write(byteArrayOf(LF, LF, LF, LF))
    output.write(byteArrayOf(GS, 0x56, 0x42, 0x00)) // GS V 66 0 (Full Cut)

    return output.toByteArray()
  }

  /**
   * Renders the complete, authentic Clinic Receipt into an exact monochrome Bitmap.
   */
  fun generateEscPosInvoiceBitmap(
    context: Context? = null,
    invoice: Invoice,
    patientHn: String = "HN-0000",
    cashierName: String = "Cashier",
    is80mm: Boolean = true,
    settings: ClinicSettings? = null
  ): Bitmap {
    val widthPx = if (is80mm) 576 else 384
    val currencyFmt = NumberFormat.getNumberInstance(Locale.US)

    // Pre-calculate estimated height dynamically based on 2-line items
    val baseEstimatedHeight = if (is80mm) 850 else 760
    val extraFeeCount = (if (invoice.consultationFee > 0.0) 1 else 0) + (if (invoice.serviceFee > 0.0) 1 else 0)
    val itemEstimatedHeight = (invoice.items.size + extraFeeCount) * (if (is80mm) 48 else 44)
    val totalHeightPx = baseEstimatedHeight + itemEstimatedHeight

    val bitmap = Bitmap.createBitmap(widthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val leftMargin = if (is80mm) 36f else 22f
    val rightMargin = widthPx.toFloat() - leftMargin
    val contentWidth = rightMargin - leftMargin
    var curY = 16f
    val centerX = widthPx / 2f

    // 1. Clinic Logo (Custom Uploaded or Styled Brand Mark)
    var customLogo: Bitmap? = null
    val logoUriString = settings?.clinicLogoUri
    if (context != null && !logoUriString.isNullOrBlank()) {
      customLogo = loadBitmapFromUri(context, logoUriString)
    }
    if (customLogo == null && !logoUriString.isNullOrBlank()) {
      try {
        val file = File(logoUriString)
        if (file.exists()) {
          customLogo = BitmapFactory.decodeFile(file.absolutePath)
        }
      } catch (_: Exception) {}
    }
    if (customLogo == null && context != null) {
      val fallbackFile = File(context.filesDir, "clinic_logo.png")
      if (fallbackFile.exists()) {
        try {
          customLogo = BitmapFactory.decodeFile(fallbackFile.absolutePath)
        } catch (_: Exception) {}
      }
    }

    if (customLogo != null) {
      val maxLogoW = if (is80mm) 150f else 110f
      val maxLogoH = if (is80mm) 75f else 55f
      val scale = minOf(maxLogoW / customLogo.width, maxLogoH / customLogo.height)
      val targetW = customLogo.width * scale
      val targetH = customLogo.height * scale
      val logoRect = RectF(centerX - targetW / 2f, curY, centerX + targetW / 2f, curY + targetH)
      canvas.drawBitmap(customLogo, null, logoRect, paint)
      curY += targetH + 8f
    } else {
      // Elegant circular medical cross emblem
      paint.color = Color.BLACK
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 2.5f
      val emblemSize = if (is80mm) 36f else 28f
      val emblemCenterY = curY + emblemSize / 2f
      canvas.drawCircle(centerX, emblemCenterY, emblemSize / 2f, paint)

      paint.style = Paint.Style.FILL
      val crossBarW = emblemSize * 0.22f
      val crossBarL = emblemSize * 0.65f
      canvas.drawRect(
        centerX - crossBarW / 2f,
        emblemCenterY - crossBarL / 2f,
        centerX + crossBarW / 2f,
        emblemCenterY + crossBarL / 2f,
        paint
      )
      canvas.drawRect(
        centerX - crossBarL / 2f,
        emblemCenterY - crossBarW / 2f,
        centerX + crossBarL / 2f,
        emblemCenterY + crossBarW / 2f,
        paint
      )
      curY += emblemSize + 10f
    }

    // 2. Clinic Name (Myanmar Unicode - Bold & Prominent)
    val clinicTitleMm = if (!settings?.clinicNameMm.isNullOrBlank()) {
      if (!settings.clinicSubtitleMm.isNullOrBlank()) "\"${settings.clinicNameMm}\" ${settings.clinicSubtitleMm}"
      else settings.clinicNameMm
    } else {
      "\"ရွှေခေတ်\" အထွေထွေရောဂါကုဆေးခန်း"
    }

    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = if (is80mm) 28f else 22f
    canvas.drawText(clinicTitleMm, centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    // Clinic Subtitle / English Name
    val clinicTitleEn = settings?.clinicNameEn?.ifBlank { "SHWE KHIT GENERAL CLINIC" } ?: "SHWE KHIT GENERAL CLINIC"
    paint.textSize = if (is80mm) 17f else 13.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(clinicTitleEn, centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    // Attending Physician (Name & Degree ONLY - NO SAMA on receipt)
    val rawDocName = settings?.doctorName?.ifBlank { "Dr. Nyan Kaung Sett" } ?: "Dr. Nyan Kaung Sett"
    val cleanDocName = rawDocName.replace("(?i)(,\\s*)?M\\.?B\\.?\\,?\\s*B\\.?S\\.?(.*)".toRegex(), "").trim()
    val degree = settings?.doctorDegree?.ifBlank { "M.B.,B.S (MDY)" } ?: "M.B.,B.S (MDY)"
    paint.textSize = if (is80mm) 16f else 13f
    canvas.drawText("$cleanDocName  $degree", centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 5f

    // Clinic Phone
    val phone = settings?.clinicPhone?.ifBlank { "09-421001122, 09-790112233" } ?: "09-421001122"
    paint.textSize = if (is80mm) 15f else 12f
    canvas.drawText("Tel: $phone", centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 8f

    // Double Divider Line (Header Boundary)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawLine(leftMargin, curY, rightMargin, curY, paint)
    canvas.drawLine(leftMargin, curY + 3.5f, rightMargin, curY + 3.5f, paint)
    curY += 12f

    // 3. Receipt Metadata
    paint.style = Paint.Style.FILL
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = if (is80mm) 18f else 14f
    canvas.drawText("OFFICIAL INVOICE RECEIPT (ငွေရပြေစာ)", centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 8f

    paint.textSize = if (is80mm) 15.5f else 12.5f

    // Bill No & Date
    paint.textAlign = Paint.Align.LEFT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Bill No : ${invoice.invoiceNo}", leftMargin, curY + paint.textSize * 0.8f, paint)
    paint.textAlign = Paint.Align.RIGHT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(invoice.createdAt, rightMargin, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    // Patient Name & HN
    paint.textAlign = Paint.Align.LEFT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Patient : ${invoice.patientName} (${patientHn})", leftMargin, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    // Cashier & Status
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Cashier : $cashierName", leftMargin, curY + paint.textSize * 0.8f, paint)
    paint.textAlign = Paint.Align.RIGHT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val isPaid = invoice.isPaid || invoice.paymentStatus.equals("Paid", ignoreCase = true)
    val statusText = if (isPaid) "PAID (${invoice.paymentMethod.label})" else "UNPAID"
    canvas.drawText(statusText, rightMargin, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 10f

    // Single Divider Line before Items
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.2f
    canvas.drawLine(leftMargin, curY, rightMargin, curY, paint)
    curY += 8f

    // 4. Items Table Header
    paint.style = Paint.Style.FILL
    paint.textSize = if (is80mm) 15f else 12f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("ITEM DESCRIPTION", leftMargin, curY + paint.textSize * 0.8f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("AMOUNT", rightMargin, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    // Divider Line
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawLine(leftMargin, curY, rightMargin, curY, paint)
    curY += 8f

    // 5. Line Items (Modern 2-Line POS Format)
    val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = if (is80mm) 16f else 13f
      color = Color.BLACK
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.LEFT
    }

    val itemSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = if (is80mm) 14.5f else 11.5f
      color = Color.BLACK
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      textAlign = Paint.Align.LEFT
    }

    val itemAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = if (is80mm) 16f else 13f
      color = Color.BLACK
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.RIGHT
    }

    if (invoice.consultationFee > 0.0) {
      val docFeeLayout = createStaticLayout("Doctor Consultation Fee", itemTitlePaint, contentWidth.toInt())
      canvas.save()
      canvas.translate(leftMargin, curY)
      docFeeLayout.draw(canvas)
      canvas.restore()
      curY += docFeeLayout.height + 4f

      val rateText = "  1 x ${currencyFmt.format(invoice.consultationFee.toInt())} Ks"
      canvas.drawText(rateText, leftMargin, curY + itemSubPaint.textSize * 0.85f, itemSubPaint)

      val totalText = "${currencyFmt.format(invoice.consultationFee.toInt())} Ks"
      canvas.drawText(totalText, rightMargin, curY + itemAmountPaint.textSize * 0.85f, itemAmountPaint)

      curY += maxOf(itemSubPaint.textSize, itemAmountPaint.textSize) + 8f
    }

    if (invoice.serviceFee > 0.0) {
      val srvLayout = createStaticLayout("Service Fee", itemTitlePaint, contentWidth.toInt())
      canvas.save()
      canvas.translate(leftMargin, curY)
      srvLayout.draw(canvas)
      canvas.restore()
      curY += srvLayout.height + 4f

      val rateText = "  1 x ${currencyFmt.format(invoice.serviceFee.toInt())} Ks"
      canvas.drawText(rateText, leftMargin, curY + itemSubPaint.textSize * 0.85f, itemSubPaint)

      val totalText = "${currencyFmt.format(invoice.serviceFee.toInt())} Ks"
      canvas.drawText(totalText, rightMargin, curY + itemAmountPaint.textSize * 0.85f, itemAmountPaint)

      curY += maxOf(itemSubPaint.textSize, itemAmountPaint.textSize) + 8f
    }

    invoice.items.forEach { item ->
      // Line 1: Item description (wraps cleanly)
      val itemLayout = createStaticLayout(item.description, itemTitlePaint, contentWidth.toInt())
      canvas.save()
      canvas.translate(leftMargin, curY)
      itemLayout.draw(canvas)
      canvas.restore()
      curY += itemLayout.height + 4f

      // Line 2: Qty x UnitPrice (Left) and Total Amount (Right)
      val rateText = "  ${item.quantity} x ${currencyFmt.format(item.unitPrice.toInt())} Ks"
      canvas.drawText(rateText, leftMargin, curY + itemSubPaint.textSize * 0.85f, itemSubPaint)

      val totalText = "${currencyFmt.format(item.total.toInt())} Ks"
      canvas.drawText(totalText, rightMargin, curY + itemAmountPaint.textSize * 0.85f, itemAmountPaint)

      curY += maxOf(itemSubPaint.textSize, itemAmountPaint.textSize) + 8f
    }

    // Divider Line after Items
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawLine(leftMargin, curY, rightMargin, curY, paint)
    curY += 10f

    // 6. Subtotal & Discount
    paint.style = Paint.Style.FILL
    paint.textSize = if (is80mm) 15.5f else 12.5f

    // Subtotal (Actual Cost)
    paint.textAlign = Paint.Align.LEFT
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Subtotal (မူလကျသင့်ငွေ):", leftMargin, curY + paint.textSize * 0.8f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.subTotal.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    // Itemized Granular Discounts & FOC
    if (invoice.doctorFeeDiscount > 0.0) {
      val isDocFoc = invoice.doctorFeeDiscount >= invoice.consultationFee && invoice.consultationFee > 0.0
      val label = if (isDocFoc) "Doctor Fee (ဆရာဝန်ပြသခ FOC):" else "Doctor Fee Disc (ဆရာဝန်ခ လျှော့):"
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText(label, leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.doctorFeeDiscount.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      curY += paint.textSize + 5f
    }

    if (invoice.pharmacyDiscount > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Pharmacy Disc (ဆေးဝါးလျှော့ငွေ):", leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.pharmacyDiscount.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      curY += paint.textSize + 5f
    }

    if (invoice.procedureDiscount > 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Procedure Disc (ကုသမှုစရိတ် လျှော့):", leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.procedureDiscount.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      curY += paint.textSize + 5f
    }

    if (invoice.discountPercent > 0.0) {
      val isAllFoc = invoice.discountPercent >= 100.0
      val label = if (isAllFoc) "All FOC (အားလုံး အခမဲ့):" else "Overall Disc (${invoice.discountPercent.toInt()}%):"
      val overallVal = (invoice.subTotal - invoice.totalGranularDiscount).coerceAtLeast(0.0) * (invoice.discountPercent / 100.0)
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText(label, leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(overallVal.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      curY += paint.textSize + 5f
    } else if (invoice.discountAmount > 0.0 && invoice.totalGranularDiscount == 0.0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Discount (လျှော့ဈေး):", leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.discountAmount.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      curY += paint.textSize + 5f
    }

    // Total Discounts Row if multiple deductions were applied
    if (invoice.discountAmount > 0.0 && ((invoice.doctorFeeDiscount > 0.0 && invoice.pharmacyDiscount > 0.0) || invoice.discountPercent > 0.0)) {
      paint.textAlign = Paint.Align.LEFT
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Total Deductions (စုစုပေါင်း လျှော့ငွေ):", leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("-${currencyFmt.format(invoice.discountAmount.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      paint.typeface = Typeface.DEFAULT
      curY += paint.textSize + 6f
    }

    // Commercial Tax
    if (invoice.taxPercent > 0) {
      paint.textAlign = Paint.Align.LEFT
      canvas.drawText("Commercial Tax (${invoice.taxPercent.toInt()}%):", leftMargin, curY + paint.textSize * 0.8f, paint)
      paint.textAlign = Paint.Align.RIGHT
      canvas.drawText("+${currencyFmt.format(invoice.taxAmount.toInt())} Ks", rightMargin, curY + paint.textSize * 0.8f, paint)
      curY += paint.textSize + 6f
    }

    curY += 6f

    // 7. Grand Total Highlight Box
    val grandTotalBoxH = if (is80mm) 42f else 34f
    val grandTotalRect = RectF(leftMargin, curY, rightMargin, curY + grandTotalBoxH)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f
    canvas.drawRoundRect(grandTotalRect, 5f, 5f, paint)

    paint.style = Paint.Style.FILL
    paint.textSize = if (is80mm) 18f else 14.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    paint.textAlign = Paint.Align.LEFT
    canvas.drawText("GRAND TOTAL:", leftMargin + 10f, curY + grandTotalBoxH * 0.65f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${currencyFmt.format(invoice.grandTotal.toInt())} Ks", rightMargin - 10f, curY + grandTotalBoxH * 0.65f, paint)
    curY += grandTotalBoxH + 14f

    // Double Divider Line before Footer
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.8f
    canvas.drawLine(leftMargin, curY, rightMargin, curY, paint)
    canvas.drawLine(leftMargin, curY + 3.5f, rightMargin, curY + 3.5f, paint)
    curY += 12f

    // 8. Footer Blessing & Greeting
    paint.style = Paint.Style.FILL
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = if (is80mm) 17f else 13.5f
    val greeting = settings?.receiptGreetingMm?.ifBlank { "ကျေးဇူးတင်ပါသည် (Thank You!)" } ?: "ကျေးဇူးတင်ပါသည် (Thank You!)"
    canvas.drawText(greeting, centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = if (is80mm) 14f else 11.5f
    canvas.drawText("လူနာများ အမြန်ဆုံး သက်သာပျောက်ကင်းပါစေ", centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 6f

    paint.textSize = if (is80mm) 12f else 10f
    canvas.drawText("* POS Thermal Slip • Shwe Khit Clinic *", centerX, curY + paint.textSize * 0.8f, paint)
    curY += paint.textSize + 24f

    val finalHeight = curY.toInt().coerceAtLeast(300)
    return Bitmap.createBitmap(bitmap, 0, 0, widthPx, finalHeight)
  }

  private fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
      val uri = Uri.parse(uriString)
      val inputStream = context.contentResolver.openInputStream(uri)
      val bitmap = BitmapFactory.decodeStream(inputStream)
      inputStream?.close()
      bitmap
    } catch (_: Exception) {
      try {
        val file = File(uriString)
        if (file.exists()) {
          BitmapFactory.decodeFile(file.absolutePath)
        } else null
      } catch (_: Exception) {
        null
      }
    }
  }

  private fun createStaticLayout(
    text: CharSequence,
    textPaint: TextPaint,
    maxWidth: Int,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
  ): StaticLayout {
    val safeWidth = maxWidth.coerceAtLeast(20)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      StaticLayout.Builder.obtain(text, 0, text.length, textPaint, safeWidth)
        .setAlignment(alignment)
        .setLineSpacing(0f, 1.25f)
        .setIncludePad(false)
        .build()
    } else {
      @Suppress("DEPRECATION")
      StaticLayout(text, textPaint, safeWidth, alignment, 1.25f, 0f, false)
    }
  }
}
