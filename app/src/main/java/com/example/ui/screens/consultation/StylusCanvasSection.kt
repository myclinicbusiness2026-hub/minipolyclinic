package com.example.ui.screens.consultation

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingPage
import com.example.model.DrawStroke
import com.example.ui.components.A4HandwritingNotesCanvasView
import com.example.ui.theme.*

@Composable
fun StylusCanvasSection(
  isStylusExpanded: Boolean,
  onToggleExpand: () -> Unit,
  strokes: MutableList<DrawStroke>,
  onStrokesUpdated: () -> Unit,
  onOpenFullscreenCanvas: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggleExpand() },
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFFFF7ED),
    border = BorderStroke(1.dp, if (strokes.isNotEmpty()) Color(0xFFFFB74D) else Color(0xFFFFEDD5))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Draw, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text("3. A4 Stylus / Freehand Canvas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
          Text("Touch/S-Pen scribbles, anatomy diagrams & handwriting", fontSize = 9.sp, color = SlateMedium)
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (strokes.isNotEmpty()) {
          Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFEDD5)) {
            Text("${strokes.size} strokes", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
          }
          Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(if (isStylusExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
      }
    }
  }

  if (isStylusExpanded) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
      if (strokes.isNotEmpty()) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(0xFF81D4FA))
        ) {
          A4HandwritingNotesCanvasView(
            page = DrawingPage(pageIndex = 0, strokes = strokes.toMutableList()),
            modifier = Modifier.fillMaxSize()
          )
        }
      } else {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable {
              onStrokesUpdated()
              onOpenFullscreenCanvas()
            },
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF0F9FF),
          border = BorderStroke(1.dp, Color(0xFFBAE6FD))
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              Icons.Default.Draw,
              contentDescription = null,
              tint = Color(0xFF0288D1),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              "Tap here to open Stylus Drawing Canvas",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0369A1)
            )
            Text(
              "Supports stylus pen, ruled medical notes, anatomy diagrams & undo/redo",
              fontSize = 8.5.sp,
              color = SlateMedium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = {
            onStrokesUpdated()
            onOpenFullscreenCanvas()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
        ) {
          Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            if (strokes.isNotEmpty()) "Open & Edit Canvas (Fullscreen)" else "Open Stylus Canvas (Fullscreen)",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
          )
        }

        if (strokes.isNotEmpty()) {
          OutlinedButton(
            onClick = {
              strokes.clear()
              onStrokesUpdated()
            },
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp),
            border = BorderStroke(1.dp, Color(0xFFEF9A9A))
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Strokes", tint = Color(0xFFD32F2F), modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("Clear Drawing", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}
