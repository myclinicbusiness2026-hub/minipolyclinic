package com.example.ui.components

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ConsultationRecord
import com.example.model.Patient

/**
 * Triggers authentic Android system printing for the A4 Medical Record.
 */
fun triggerPrintDocument(
  context: Context,
  consultation: ConsultationRecord,
  patient: Patient?,
  printSettings: A4PrintSettings = A4PrintSettings(),
  marginDp: Dp = 18.dp,
  pageDescriptors: List<A4PageDescriptor> = listOf(A4PageDescriptor.UnifiedSinglePage()),
  isGrayscale: Boolean = false
) {
  val marginMm = (marginDp.value / 2.8346f).coerceIn(5f, 30f)
  triggerRealAndroidPrint(
    context = context,
    consultation = consultation,
    patient = patient,
    printSettings = printSettings,
    pageDescriptors = pageDescriptors,
    marginMm = marginMm,
    isGrayscale = isGrayscale
  )
}
