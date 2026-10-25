package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * A4PageDescriptor classifies pages in the active EMR print layout.
 */
sealed class A4PageDescriptor {
  data class UnifiedSinglePage(val title: String = "Complete Clinical Chart & Rx") : A4PageDescriptor()
  data class ClinicalConsultationPage(
    val pageIndex: Int = 0,
    val isFirstPage: Boolean = true,
    val title: String = "Sheet 1: Clinical Notes & Examination"
  ) : A4PageDescriptor()
  data class OfficialPrescriptionPage(val title: String = "Sheet: Prescriptions & Doctor's Orders") : A4PageDescriptor()
  data class CanvasDrawingPage(val pageIndex: Int, val title: String) : A4PageDescriptor()
  data class MedicalLeaveCertificatePage(val title: String = "Medical Leave Certificate") : A4PageDescriptor()
  data class ReferralLetterPage(val title: String = "Official Clinical Referral Letter") : A4PageDescriptor()
}

@Composable
fun A4PreviewToolbar(
  currentPage: Int,
  totalPages: Int,
  currentDescriptor: A4PageDescriptor?,
  zoomScale: Float,
  fitMode: String = "fit_page",
  onFitModeChange: (String) -> Unit = {},
  showMarginGuides: Boolean,
  onToggleMarginGuides: () -> Unit,
  onPrevPage: () -> Unit,
  onNextPage: () -> Unit,
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit,
  onResetZoom: () -> Unit
) {
  Surface(
    color = Color(0xFF0F172A),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Page Controls & Active Sheet Indicator
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onPrevPage,
          enabled = currentPage > 1,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = if (currentPage > 1) PureWhite else SlateMedium)
        }

        Surface(
          color = Color(0xFF1E293B),
          shape = RoundedCornerShape(4.dp),
          modifier = Modifier.padding(horizontal = 2.dp)
        ) {
          Text(
            "Page $currentPage of $totalPages",
            color = PureWhite,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }

        IconButton(
          onClick = onNextPage,
          enabled = currentPage < totalPages,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = if (currentPage < totalPages) PureWhite else SlateMedium)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Sheet Title Pill
        val sheetTitle = when (currentDescriptor) {
          is A4PageDescriptor.UnifiedSinglePage -> currentDescriptor.title
          is A4PageDescriptor.ClinicalConsultationPage -> currentDescriptor.title
          is A4PageDescriptor.OfficialPrescriptionPage -> currentDescriptor.title
          is A4PageDescriptor.CanvasDrawingPage -> currentDescriptor.title
          is A4PageDescriptor.MedicalLeaveCertificatePage -> currentDescriptor.title
          is A4PageDescriptor.ReferralLetterPage -> currentDescriptor.title
          null -> "EMR Sheet"
        }

        Surface(
          color = when (currentDescriptor) {
            is A4PageDescriptor.OfficialPrescriptionPage -> Color(0xFF15803D).copy(alpha = 0.25f)
            is A4PageDescriptor.CanvasDrawingPage -> Color(0xFF0284C7).copy(alpha = 0.25f)
            is A4PageDescriptor.MedicalLeaveCertificatePage -> Color(0xFFB45309).copy(alpha = 0.25f)
            is A4PageDescriptor.ReferralLetterPage -> Color(0xFF1D4ED8).copy(alpha = 0.25f)
            else -> MedicalGreen.copy(alpha = 0.25f)
          },
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.5.dp, BorderGray)
        ) {
          Text(
            sheetTitle,
            color = PureWhite,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      // Scale Presets, Margin Guides & Zoom Controls
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Fit Page Preset Button
        Surface(
          color = if (fitMode == "fit_page") MedicalGreen else Color(0xFF1E293B),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.5.dp, if (fitMode == "fit_page") MedicalGreen else Color(0xFF475569)),
          modifier = Modifier.clickable { onFitModeChange("fit_page") }
        ) {
          Text(
            "Fit Sheet",
            color = PureWhite,
            fontSize = 9.sp,
            fontWeight = if (fitMode == "fit_page") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
          )
        }

        // Fit Width Preset Button
        Surface(
          color = if (fitMode == "fit_width") MedicalGreen else Color(0xFF1E293B),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(0.5.dp, if (fitMode == "fit_width") MedicalGreen else Color(0xFF475569)),
          modifier = Modifier.clickable { onFitModeChange("fit_width") }
        ) {
          Text(
            "Fit Width",
            color = PureWhite,
            fontSize = 9.sp,
            fontWeight = if (fitMode == "fit_width") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
          )
        }

        // Margin Guides Visual Toggle
        IconButton(
          onClick = onToggleMarginGuides,
          modifier = Modifier.size(26.dp)
        ) {
          Icon(
            if (showMarginGuides) Icons.Default.GridOn else Icons.Default.GridOff,
            contentDescription = "Toggle Margin Guides",
            tint = if (showMarginGuides) MedicalGreen else SlateMedium,
            modifier = Modifier.size(15.dp)
          )
        }

        IconButton(onClick = onZoomOut, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = PureWhite, modifier = Modifier.size(14.dp))
        }
        Text("${(zoomScale * 100).toInt()}%", color = PureWhite, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onZoomIn, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = PureWhite, modifier = Modifier.size(14.dp))
        }
        if (zoomScale != 1.0f) {
          TextButton(onClick = onResetZoom, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp), modifier = Modifier.height(22.dp)) {
            Text("100%", fontSize = 9.sp, color = MedicalGreen)
          }
        }
      }
    }
  }
}
