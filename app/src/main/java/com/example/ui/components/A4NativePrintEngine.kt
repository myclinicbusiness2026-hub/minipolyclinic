package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.ConsultationRecord
import com.example.model.Patient
import com.example.service.printing.A4PdfNativeCanvasRenderer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A4NativePrintEngine
 * 
 * Implements real Android system printing and native PDF file export:
 * 1. A4MedicalRecordPrintAdapter: Interfaces with Android's Print Spooler service.
 * 2. triggerRealAndroidPrint: Spawns the authentic Android Print dialog (Wi-Fi, Bluetooth, Save as PDF).
 * 3. exportAndShareA4Pdf: Writes true ISO A4 PDF to local storage and invokes the system Share sheet.
 */
class A4MedicalRecordPrintAdapter(
  private val context: Context,
  private val consultation: ConsultationRecord,
  private val patient: Patient?,
  private val printSettings: A4PrintSettings,
  private val pageDescriptors: List<A4PageDescriptor>,
  private val marginMm: Float
) : PrintDocumentAdapter() {

  private var pdfDocument: PdfDocument? = null
  private var totalPages: Int = if (pageDescriptors.isNotEmpty()) pageDescriptors.size else A4PdfNativeCanvasRenderer.computePageCount(consultation, patient, printSettings)

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

    totalPages = if (pageDescriptors.isNotEmpty()) pageDescriptors.size else A4PdfNativeCanvasRenderer.computePageCount(consultation, patient, printSettings)

    val safeHn = (patient?.hn ?: "EMR").replace("/", "_")
    val info = PrintDocumentInfo.Builder("MedicalRecord_${safeHn}.pdf")
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
    try {
      pdfDocument = PdfDocument()

      // Standard ISO A4 dimensions in typographic points (72 DPI)
      val a4WidthPoints = 595
      val a4HeightPoints = 842

      for (i in 0 until totalPages) {
        if (cancellationSignal?.isCanceled == true) {
          callback.onWriteCancelled()
          return
        }

        val pageInfo = PdfDocument.PageInfo.Builder(a4WidthPoints, a4HeightPoints, i + 1).create()
        val page = pdfDocument!!.startPage(pageInfo)
        val canvas = page.canvas

        val descriptor = pageDescriptors.getOrNull(i) ?: A4PageDescriptor.UnifiedSinglePage()

        A4PdfNativeCanvasRenderer.drawPage(
          canvas = canvas,
          descriptor = descriptor,
          pageNumber = i + 1,
          totalPages = totalPages,
          consultation = consultation,
          patient = patient,
          printSettings = printSettings,
          width = a4WidthPoints.toFloat(),
          height = a4HeightPoints.toFloat(),
          marginMm = marginMm
        )

        pdfDocument!!.finishPage(page)
      }

      FileOutputStream(destination.fileDescriptor).use { output ->
        pdfDocument!!.writeTo(output)
      }

      callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    } catch (e: Exception) {
      callback.onWriteFailed(e.message)
    } finally {
      pdfDocument?.close()
      pdfDocument = null
    }
  }
}

/**
 * Initiates the authentic Android System Print Spooler.
 */
fun triggerRealAndroidPrint(
  context: Context,
  consultation: ConsultationRecord,
  patient: Patient?,
  printSettings: A4PrintSettings,
  pageDescriptors: List<A4PageDescriptor>,
  marginMm: Float = 15f,
  isGrayscale: Boolean = false
) {
  try {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager != null) {
      val safePatientName = consultation.patientName.replace("[^a-zA-Z0-9]".toRegex(), "_")
      val jobName = "ShweKhit_A4_${safePatientName}_${patient?.hn ?: "Record"}"

      val colorMode = if (isGrayscale) {
        PrintAttributes.COLOR_MODE_MONOCHROME
      } else {
        PrintAttributes.COLOR_MODE_COLOR
      }

      // Convert mm to mils (1mm = 39.37 mils)
      val marginMils = (marginMm * 39.37f).toInt().coerceIn(100, 1000)
      val margins = PrintAttributes.Margins(marginMils, marginMils, marginMils, marginMils)

      val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setColorMode(colorMode)
        .setMinMargins(margins)
        .build()

      val adapter = A4MedicalRecordPrintAdapter(
        context = context,
        consultation = consultation,
        patient = patient,
        printSettings = printSettings,
        pageDescriptors = pageDescriptors,
        marginMm = marginMm
      )

      printManager.print(jobName, adapter, printAttributes)
    } else {
      Toast.makeText(context, "System Print Service not available", Toast.LENGTH_SHORT).show()
    }
  } catch (e: Exception) {
    Toast.makeText(context, "Print spooler error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
  }
}

/**
 * Renders the real A4 PDF document to storage and triggers the Android share sheet.
 */
fun exportAndShareA4Pdf(
  context: Context,
  consultation: ConsultationRecord,
  patient: Patient?,
  printSettings: A4PrintSettings,
  pageDescriptors: List<A4PageDescriptor>,
  marginMm: Float = 15f,
  onComplete: (File?, Uri?) -> Unit
) {
  try {
    val pdfDocument = PdfDocument()
    val a4WidthPoints = 595
    val a4HeightPoints = 842
    val totalPages = if (pageDescriptors.isNotEmpty()) pageDescriptors.size else A4PdfNativeCanvasRenderer.computePageCount(consultation, patient, printSettings)

    for (i in 0 until totalPages) {
      val pageInfo = PdfDocument.PageInfo.Builder(a4WidthPoints, a4HeightPoints, i + 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas = page.canvas

      val descriptor = pageDescriptors.getOrNull(i) ?: A4PageDescriptor.UnifiedSinglePage()

      A4PdfNativeCanvasRenderer.drawPage(
        canvas = canvas,
        descriptor = descriptor,
        pageNumber = i + 1,
        totalPages = totalPages,
        consultation = consultation,
        patient = patient,
        printSettings = printSettings,
        width = a4WidthPoints.toFloat(),
        height = a4HeightPoints.toFloat(),
        marginMm = marginMm
      )

      pdfDocument.finishPage(page)
    }

    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
    if (!dir.exists()) dir.mkdirs()

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    val safeHn = (patient?.hn ?: "HN2026").replace("/", "_")
    val file = File(dir, "ShweKhit_EMR_${safeHn}_$timeStamp.pdf")

    FileOutputStream(file).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    val fileUri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )

    // Launch Android Share Chooser
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, fileUri)
      putExtra(Intent.EXTRA_SUBJECT, "A4 Medical Record - ${consultation.patientName} (${patient?.hn})")
      putExtra(Intent.EXTRA_TEXT, "A4 Clinical Record for ${consultation.patientName} from ${printSettings.clinicNameMm}.")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(shareIntent, "Share or Save A4 PDF Record")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)

    onComplete(file, fileUri)
  } catch (e: Exception) {
    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    onComplete(null, null)
  }
}
