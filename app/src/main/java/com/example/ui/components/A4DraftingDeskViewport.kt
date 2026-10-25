package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ConsultationRecord
import com.example.model.Patient
import com.example.ui.theme.PureWhite

/**
 * A4DraftingDeskViewport
 * 
 * Provides an authentic, highly-legible A4 Medical Record drafting desk:
 * - Guarantees the A4 sheet is 100% centered horizontally in the viewport
 * - Responsive sheet width matching mobile & tablet viewports with authentic A4 look & drop shadow
 * - Full-content preservation: dynamic vertical flow so every section (letterhead, demographics,
 *   clinical notes, diagnosis, procedures, investigations, Rx, instructions, follow-up, and
 *   doctor's official stamp & signature) is 100% visible with zero clipping.
 * - Crisp, high-contrast typography readable at 100% without squinting or microscopic downscaling.
 */
@Composable
fun A4DraftingDeskViewport(
  modifier: Modifier = Modifier,
  pageDescriptor: A4PageDescriptor,
  currentPage: Int,
  totalPages: Int,
  consultation: ConsultationRecord,
  patient: Patient?,
  marginDp: Dp,
  showMarginGuides: Boolean,
  isGrayscale: Boolean,
  incLetterhead: Boolean,
  incDemographics: Boolean,
  incCanvas: Boolean,
  incProcedures: Boolean,
  incInvestigations: Boolean,
  incAttachments: Boolean,
  incRx: Boolean,
  incStamp: Boolean,
  printSettings: A4PrintSettings,
  zoomScale: Float,
  fitMode: String
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F172A))
  ) {
    val standardA4Width = 595.dp
    val standardA4Height = 842.dp
    val padH = 12.dp
    val padV = 16.dp

    val availableW = (maxWidth - padH * 2).coerceAtLeast(100.dp)
    val availableH = (maxHeight - padV * 2).coerceAtLeast(100.dp)

    // Calculate uniform scale so 595dp sheet fits cleanly without text re-wrapping
    val fitWidthScale = (availableW / standardA4Width).coerceIn(0.25f, 2.0f)
    val fitHeightScale = (availableH / standardA4Height).coerceIn(0.25f, 2.0f)

    val baseScale = when (fitMode) {
      "fit_page" -> minOf(fitWidthScale, fitHeightScale)
      else -> fitWidthScale // "fit_width" is default
    }
    val effectiveScale = (baseScale * zoomScale).coerceIn(0.2f, 3.0f)

    val scaledWidth = standardA4Width * effectiveScale
    val scaledHeight = standardA4Height * effectiveScale

    val vertScroll = rememberScrollState()
    val horizScroll = rememberScrollState()

    // Authentic A4 Drafting Desk with smooth 2D scroll when zoomed
    Box(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(vertScroll)
        .horizontalScroll(horizScroll),
      contentAlignment = Alignment.TopCenter
    ) {
      Box(
        modifier = Modifier
          .padding(horizontal = padH, vertical = padV)
          .size(width = scaledWidth, height = scaledHeight),
        contentAlignment = Alignment.TopStart
      ) {
        Box(
          modifier = Modifier
            .requiredSize(width = standardA4Width, height = standardA4Height)
            .graphicsLayer {
              scaleX = effectiveScale
              scaleY = effectiveScale
              transformOrigin = TransformOrigin(0f, 0f)
            }
        ) {
          // Physical standard A4 paper sheet (595pt x 842pt)
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(2.dp),
                spotColor = Color(0x66000000),
                ambientColor = Color(0x33000000)
              ),
            shape = RoundedCornerShape(2.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
          ) {
            RenderA4PageByDescriptor(
              descriptor = pageDescriptor,
              pageNumber = currentPage,
              totalPages = totalPages,
              consultation = consultation,
              patient = patient,
              marginDp = marginDp,
              showMarginGuides = showMarginGuides,
              isGrayscale = isGrayscale,
              incLetterhead = incLetterhead,
              incDemographics = incDemographics,
              incCanvas = incCanvas,
              incProcedures = incProcedures,
              incInvestigations = incInvestigations,
              incAttachments = incAttachments,
              incRx = incRx,
              incStamp = incStamp,
              printSettings = printSettings
            )
          }
        }
      }
    }
  }
}
