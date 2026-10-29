package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkGrayText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateMedium

/**
 * FormattedClinicalSectionView
 *
 * Elegantly formats clinical text (Subjective & Objective) by parsing subheadings,
 * lists, and details. Eliminates ugly [ ] brackets, rendering distinct bold headings
 * with Navy/Teal accents and crisp indentations.
 */
@Composable
fun FormattedClinicalSectionView(
  rawText: String,
  modifier: Modifier = Modifier
) {
  if (rawText.isBlank()) return

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

    lines.forEach { line ->
      val isBracketed = line.startsWith("[") && line.endsWith("]")
      val isMarkdownHeading = line.startsWith("###") || line.startsWith("##") || line.startsWith("# ")
      val isHeading = isBracketed || isMarkdownHeading

      if (isHeading) {
        val cleanTitle = line
          .removePrefix("[HEADING:")
          .removePrefix("[SECTION:")
          .removePrefix("[")
          .removeSuffix("]")
          .removePrefix("###")
          .removePrefix("##")
          .removePrefix("#")
          .trim()

        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFFEFF6FF), // Soft Blue container
          border = BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E3A8A))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = cleanTitle,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1E3A8A) // Bold Navy Blue
            )
          }
        }
      } else if (line.startsWith("→")) {
        // Indented arrow detail
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 1.dp),
          verticalAlignment = Alignment.Top
        ) {
          Text("→ ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
          Text(
            text = line.removePrefix("→").trim(),
            fontSize = 10.sp,
            color = SlateDark
          )
        }
      } else if (line.startsWith("#")) {
        // Numbered complaint item (e.g. #1. Epigastric burning pain...)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp),
          verticalAlignment = Alignment.Top
        ) {
          Text(
            text = line,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
          )
        }
      } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
        val clean = line.substring(1).trim()
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 1.dp),
          verticalAlignment = Alignment.Top
        ) {
          Text("• ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
          Text(
            text = clean,
            fontSize = 10.sp,
            color = DarkGrayText
          )
        }
      } else {
        Text(
          text = line,
          fontSize = 10.sp,
          color = SlateDark,
          modifier = Modifier.padding(start = 6.dp)
        )
      }
    }
  }
}
