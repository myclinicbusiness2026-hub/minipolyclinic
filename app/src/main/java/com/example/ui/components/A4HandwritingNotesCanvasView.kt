package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.DrawingPage
import com.example.model.DrawPoint
import com.example.model.DrawStroke

@Composable
fun A4HandwritingNotesCanvasView(
  page: DrawingPage,
  modifier: Modifier = Modifier,
  strokeColor: Color = Color.Black,
  strokeWidth: Float = 4.0f,
  isEraser: Boolean = false,
  isInteractive: Boolean = false,
  onStrokeFinished: () -> Unit = {}
) {
  var currentPoints by remember { mutableStateOf<List<DrawPoint>>(emptyList()) }

  Box(
    modifier = modifier
      .background(Color.White)
      .then(
        if (isInteractive) {
          Modifier.pointerInput(page.pageIndex, isEraser, strokeColor, strokeWidth) {
            detectDragGestures(
              onDragStart = { offset ->
                currentPoints = listOf(DrawPoint(offset.x, offset.y))
              },
              onDrag = { change, _ ->
                change.consume()
                currentPoints = currentPoints + DrawPoint(change.position.x, change.position.y)
              },
              onDragEnd = {
                if (currentPoints.isNotEmpty()) {
                  val newStroke = DrawStroke(
                    points = currentPoints,
                    colorHex = if (isEraser) 0xFFFFFFFF else strokeColor.value.toLong(),
                    strokeWidth = strokeWidth
                  )
                  page.strokes.add(newStroke)
                  currentPoints = emptyList()
                  onStrokeFinished()
                }
              },
              onDragCancel = {
                currentPoints = emptyList()
              }
            )
          }
        } else Modifier
      )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Draw Ruled Lines
      val lineSpacing = 32f
      var y = lineSpacing
      while (y < size.height) {
        drawLine(
          color = Color(0xFFE2E8F0),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
        y += lineSpacing
      }

      // Draw committed strokes
      page.strokes.forEach { stroke ->
        if (stroke.points.size > 1) {
          val path = Path().apply {
            moveTo(stroke.points[0].x, stroke.points[0].y)
            for (i in 1 until stroke.points.size) {
              lineTo(stroke.points[i].x, stroke.points[i].y)
            }
          }
          drawPath(
            path = path,
            color = Color(stroke.colorHex),
            style = Stroke(
              width = stroke.strokeWidth,
              cap = StrokeCap.Round,
              join = StrokeJoin.Round
            )
          )
        }
      }

      // Draw current stroke in progress
      if (currentPoints.size > 1) {
        val path = Path().apply {
          moveTo(currentPoints[0].x, currentPoints[0].y)
          for (i in 1 until currentPoints.size) {
            lineTo(currentPoints[i].x, currentPoints[i].y)
          }
        }
        drawPath(
          path = path,
          color = if (isEraser) Color.White else strokeColor,
          style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
          )
        )
      }
    }
  }
}
