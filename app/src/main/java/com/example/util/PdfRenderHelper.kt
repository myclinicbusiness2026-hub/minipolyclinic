package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfRenderHelper {

  /**
   * Renders the first page of a local PDF file into a Bitmap for preview.
   * Returns null if the file cannot be opened or parsed as a PDF.
   */
  suspend fun renderPdfFirstPage(context: Context, filePath: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val file = File(filePath)
      if (!file.exists() || !file.canRead()) return@withContext null

      val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
      val renderer = PdfRenderer(pfd)
      if (renderer.pageCount <= 0) {
        renderer.close()
        pfd.close()
        return@withContext null
      }

      val page = renderer.openPage(0)
      // Render at a clear resolution (width around 800-1000px proportional to aspect ratio)
      val targetWidth = 900
      val targetHeight = (targetWidth.toFloat() / page.width * page.height).toInt().coerceAtLeast(400)

      val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
      // Fill with white background before rendering
      val canvas = android.graphics.Canvas(bitmap)
      canvas.drawColor(android.graphics.Color.WHITE)

      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()
      renderer.close()
      pfd.close()
      bitmap
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
