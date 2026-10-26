package com.example.ui.components

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AnatomyGuideTemplate
import com.example.model.DrawPoint
import com.example.model.DrawStroke
import com.example.model.DrawingPage
import com.example.model.Patient
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * High-Precision Fullscreen A4 Clinical Notes & Stylus Writing Workspace.
 *
 * Features:
 * 1. Sub-pixel historical stylus digitizer sampling (120Hz/240Hz).
 * 2. Quadratic Bézier curve smoothing for natural, fluid handwriting.
 * 3. Dedicated pen tools (Fine Pen, Gel Pen, Bold Pen, Medical Highlighter, Eraser).
 * 4. Realistic Ruled Medical Paper, Blank Sheet, and Clinical Grid Paper styles.
 * 5. Palm rest guard and Stylus-only rejection.
 * 6. Dynamic multi-page notebook management (Page 1, 2, 3... + Add Sheet).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun A4AnatomyCanvasDialog(
  patient: Patient?,
  initialStrokes: List<DrawStroke> = emptyList(),
  initialPages: List<DrawingPage> = emptyList(),
  initialNotes: String = "",
  onDismiss: () -> Unit,
  onSave: (List<DrawStroke>, Int) -> Unit = { _, _ -> },
  onSavePages: (List<DrawingPage>, String) -> Unit = { _, _ -> }
) {
  // Multiple pages support
  val pages = remember {
    mutableStateListOf<androidx.compose.runtime.snapshots.SnapshotStateList<DrawStroke>>().apply {
      if (initialPages.isNotEmpty()) {
        initialPages.forEach { p ->
          add(androidx.compose.runtime.mutableStateListOf<DrawStroke>().apply { addAll(p.strokes) })
        }
      } else {
        add(androidx.compose.runtime.mutableStateListOf<DrawStroke>().apply { addAll(initialStrokes) })
      }
    }
  }

  val pageTemplates = remember {
    mutableStateListOf<AnatomyGuideTemplate>().apply {
      if (initialPages.isNotEmpty()) {
        initialPages.forEach { add(it.template) }
      } else {
        add(AnatomyGuideTemplate.RULED_NOTES)
      }
    }
  }

  val pageNotes = remember {
    mutableStateListOf<String>().apply {
      if (initialPages.isNotEmpty()) {
        initialPages.forEach { add(it.notes) }
      } else {
        add(initialNotes)
      }
    }
  }

  var currentPageIndex by remember { mutableStateOf(0) }
  var overallNotes by remember { mutableStateOf(initialNotes) }
  var canvasWidth by remember { mutableStateOf(800f) }
  var canvasHeight by remember { mutableStateOf(1131f) }

  // Pen tool presets
  var penMode by remember { mutableStateOf("gel") } // "fine", "gel", "bold", "highlighter", "eraser"
  var selectedColor by remember { mutableStateOf(Color(0xFF0D47A1)) } // Default: Doctor's Royal Blue
  var strokeWidth by remember { mutableStateOf(3.2f) }
  var isStylusOnlyMode by remember { mutableStateOf(false) }
  var showPalmGuard by remember { mutableStateOf(false) }
  var palmGuardHeightRatio by remember { mutableStateOf(0.35f) }

  var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
  val currentStrokes = pages.getOrElse(currentPageIndex) { remember { mutableStateListOf() } }
  val currentTemplate = pageTemplates.getOrElse(currentPageIndex) { AnatomyGuideTemplate.RULED_NOTES }

  val context = LocalContext.current
  val pageRedoStrokes = remember { mutableStateMapOf<Int, MutableList<DrawStroke>>() }
  var isSavedFeedback by remember { mutableStateOf(false) }
  var showTextNotesPanel by remember { mutableStateOf(false) }

  LaunchedEffect(isSavedFeedback) {
    if (isSavedFeedback) {
      delay(1800)
      isSavedFeedback = false
    }
  }

  fun performSave(silent: Boolean = false) {
    val savedPages = pages.mapIndexed { idx, strokeList ->
      DrawingPage(
        pageIndex = idx,
        strokes = strokeList.toMutableList(),
        template = pageTemplates.getOrElse(idx) { AnatomyGuideTemplate.RULED_NOTES },
        notes = pageNotes.getOrElse(idx) { "" },
        baseWidth = if (canvasWidth > 0f) canvasWidth else 800f,
        baseHeight = if (canvasHeight > 0f) canvasHeight else 1131f
      )
    }
    val allStrokes = pages.firstOrNull()?.toList() ?: emptyList()
    onSave(allStrokes, pages.size)
    onSavePages(savedPages, overallNotes)
    isSavedFeedback = true
    if (!silent) {
      Toast.makeText(context, "Clinical note saved ✓", Toast.LENGTH_SHORT).show()
    }
  }

  val colorPalette = listOf(
    Color(0xFF0D47A1) to "Royal Blue",
    DarkGrayText to "Pitch Black",
    Color(0xFFC62828) to "Clinical Red",
    Color(0xFF1B5E20) to "Forest Green"
  )

  Dialog(
    onDismissRequest = {
      performSave(silent = true)
      onDismiss()
    },
    properties = DialogProperties(
      usePlatformDefaultWidth = false
    )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      shape = RectangleShape,
      color = PureWhite
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // TOP HEADER BAR
        Surface(
          color = Color(0xFFF8FAFC),
          border = BorderStroke(0.8.dp, BorderGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  performSave(silent = true)
                  onDismiss()
                },
                modifier = Modifier.size(34.dp)
              ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkGrayText)
              }
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    "Doctor's Clinical Notes (A4)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkGrayText
                  )
                  if (patient != null) {
                    Text(
                      " • ${patient.name} (${patient.hn})",
                      fontSize = 12.sp,
                      color = MedicalGreenDark,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
                Text(
                  "Smooth stylus handwriting • Multipage clinical documentation",
                  fontSize = 10.sp,
                  color = SlateMuted
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              // Typed EMR Notes Panel Toggle
              OutlinedButton(
                onClick = { showTextNotesPanel = !showTextNotesPanel },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (showTextNotesPanel) SoftGreenContainer else PureWhite
                )
              ) {
                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(15.dp), tint = MedicalGreenDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Type Notes", fontSize = 11.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
              }

              // Save Button
              Button(
                onClick = { performSave(silent = false) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isSavedFeedback) Color(0xFF2E7D32) else MedicalGreen
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Icon(
                  if (isSavedFeedback) Icons.Default.Check else Icons.Default.Save,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  if (isSavedFeedback) "Saved ✓" else "Save Note",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              // Done & Close
              FilledTonalButton(
                onClick = {
                  performSave(silent = true)
                  onDismiss()
                },
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Text("Done", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // TOOLBAR ROW 1: Pen Presets, Colors, Undo/Redo, Palm Guard
        Surface(
          color = PureWhite,
          border = BorderStroke(0.5.dp, BorderGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Pen Preset Chips
            Text("Pen:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)

            FilterChip(
              selected = penMode == "fine",
              onClick = {
                penMode = "fine"
                strokeWidth = 2.0f
              },
              label = { Text("Fine (2mm)", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
            )

            FilterChip(
              selected = penMode == "gel",
              onClick = {
                penMode = "gel"
                strokeWidth = 3.2f
              },
              label = { Text("Gel Pen (3mm)", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
            )

            FilterChip(
              selected = penMode == "bold",
              onClick = {
                penMode = "bold"
                strokeWidth = 5.0f
              },
              label = { Text("Bold (5mm)", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
            )

            FilterChip(
              selected = penMode == "highlighter",
              onClick = {
                penMode = "highlighter"
                strokeWidth = 14.0f
              },
              label = { Text("Highlighter", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF9C4), selectedLabelColor = Color(0xFFF57F17))
            )

            FilterChip(
              selected = penMode == "eraser",
              onClick = {
                penMode = "eraser"
                strokeWidth = 22.0f
              },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Eraser", fontSize = 10.sp)
                }
              },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFEBEE), selectedLabelColor = Color(0xFFC62828))
            )

            Box(modifier = Modifier.width(1.dp).height(18.dp).background(BorderGray))

            // Ink Colors
            Text("Ink:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
            colorPalette.forEach { (c, name) ->
              val isSel = selectedColor == c && penMode != "eraser"
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(c)
                  .border(
                    if (isSel) 2.5.dp else 1.dp,
                    if (isSel) MedicalGreenDark else BorderGray,
                    CircleShape
                  )
                  .clickable {
                    selectedColor = c
                    if (penMode == "eraser") penMode = "gel"
                  }
              )
            }

            Box(modifier = Modifier.width(1.dp).height(18.dp).background(BorderGray))

            // Undo / Redo
            val hasUndo = currentStrokes.isNotEmpty()
            val redoList = pageRedoStrokes.getOrPut(currentPageIndex) { mutableListOf() }
            val hasRedo = redoList.isNotEmpty()

            IconButton(
              onClick = {
                if (currentStrokes.isNotEmpty()) {
                  val last = currentStrokes.removeAt(currentStrokes.size - 1)
                  redoList.add(last)
                }
              },
              enabled = hasUndo,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (hasUndo) DarkGrayText else SlateLight, modifier = Modifier.size(18.dp))
            }

            IconButton(
              onClick = {
                if (redoList.isNotEmpty()) {
                  val restored = redoList.removeAt(redoList.size - 1)
                  currentStrokes.add(restored)
                }
              },
              enabled = hasRedo,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (hasRedo) DarkGrayText else SlateLight, modifier = Modifier.size(18.dp))
            }

            Box(modifier = Modifier.width(1.dp).height(18.dp).background(BorderGray))

            // Stylus Only Mode Toggle
            FilterChip(
              selected = isStylusOnlyMode,
              onClick = { isStylusOnlyMode = !isStylusOnlyMode },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(if (isStylusOnlyMode) "Stylus Only: ON" else "Stylus Only", fontSize = 9.5.sp)
                }
              },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftGreenContainer, selectedLabelColor = MedicalGreenDark)
            )

            // Palm Rest Guard Toggle
            FilterChip(
              selected = showPalmGuard,
              onClick = { showPalmGuard = !showPalmGuard },
              label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.FrontHand, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(if (showPalmGuard) "Palm Rest: ON" else "Palm Rest", fontSize = 9.5.sp)
                }
              },
              modifier = Modifier.height(28.dp),
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFE0F2FE), selectedLabelColor = Color(0xFF0369A1))
            )

            // Clear Sheet
            OutlinedButton(
              onClick = {
                if (currentStrokes.isNotEmpty()) {
                  redoList.clear()
                  redoList.addAll(currentStrokes)
                  currentStrokes.clear()
                }
              },
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp),
              border = BorderStroke(0.8.dp, Color(0xFFEF9A9A))
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusCritical)
              Spacer(modifier = Modifier.width(2.dp))
              Text("Clear Sheet", fontSize = 10.sp, color = StatusCritical, fontWeight = FontWeight.Bold)
            }
          }
        }

        // TOOLBAR ROW 2: Page Navigation & Clean Paper Styles
        Surface(
          color = Color(0xFFF1F5F9),
          border = BorderStroke(0.5.dp, BorderGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("Sheet:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)

            pages.forEachIndexed { idx, _ ->
              val isSelected = idx == currentPageIndex
              FilterChip(
                selected = isSelected,
                onClick = { currentPageIndex = idx },
                label = { Text("Page ${idx + 1}", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SoftGreenContainer,
                  selectedLabelColor = MedicalGreenDark
                ),
                modifier = Modifier.height(26.dp)
              )
            }

            // "+ Add Note Sheet"
            OutlinedButton(
              onClick = {
                pages.add(androidx.compose.runtime.mutableStateListOf())
                pageTemplates.add(AnatomyGuideTemplate.RULED_NOTES)
                pageNotes.add("")
                currentPageIndex = pages.size - 1
              },
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp),
              border = BorderStroke(1.dp, MedicalGreen)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = MedicalGreen)
              Spacer(modifier = Modifier.width(2.dp))
              Text("Add Sheet", fontSize = 10.sp, color = MedicalGreen, fontWeight = FontWeight.Bold)
            }

            // Remove Sheet Button (Available when more than 1 sheet exists)
            if (pages.size > 1) {
              OutlinedButton(
                onClick = {
                  if (pages.size > 1 && currentPageIndex in pages.indices) {
                    val removeIdx = currentPageIndex
                    pages.removeAt(removeIdx)
                    if (removeIdx in pageTemplates.indices) pageTemplates.removeAt(removeIdx)
                    if (removeIdx in pageNotes.indices) pageNotes.removeAt(removeIdx)
                    pageRedoStrokes.remove(removeIdx)
                    currentPageIndex = (removeIdx - 1).coerceAtLeast(0)
                  }
                },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
              ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Sheet", modifier = Modifier.size(12.dp), tint = Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Remove Sheet", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
              }
            }

            Box(modifier = Modifier.width(1.dp).height(16.dp).background(BorderGray))

            // Clean Paper Styles (NO ANATOMICAL BODY OUTLINES)
            Text("Paper Style:", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = SlateDark)
            AnatomyGuideTemplate.entries.forEach { tpl ->
              val isSel = currentTemplate == tpl
              FilterChip(
                selected = isSel,
                onClick = {
                  if (currentPageIndex in pageTemplates.indices) {
                    pageTemplates[currentPageIndex] = tpl
                  }
                },
                label = { Text(tpl.title, fontSize = 9.5.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = SoftGreenContainer,
                  selectedLabelColor = MedicalGreenDark
                ),
                modifier = Modifier.height(26.dp)
              )
            }
          }
        }

        // OPTIONAL TYPED NOTES PANEL
        if (showTextNotesPanel) {
          Surface(
            color = Color(0xFFF8FAFC),
            border = BorderStroke(0.8.dp, BorderGray),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("Typed Clinical Notes for Page ${currentPageIndex + 1}:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DarkGrayText)
              Spacer(modifier = Modifier.height(4.dp))
              val currentNote = pageNotes.getOrElse(currentPageIndex) { "" }
              OutlinedTextField(
                value = currentNote,
                onValueChange = { newVal ->
                  if (currentPageIndex in pageNotes.indices) {
                    pageNotes[currentPageIndex] = newVal
                  }
                },
                placeholder = { Text("Type clinical findings, examination notes, or comments...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().height(65.dp),
                shape = RoundedCornerShape(4.dp)
              )
            }
          }
        }

        // MAIN A4 DRAWING / HANDWRITING CANVAS
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .background(Color(0xFFE2E8F0))
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          key(currentPageIndex) {
            // A4 Aspect Ratio Container (1 : 1.414)
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f / 1.4142f)
                .shadow(4.dp, RoundedCornerShape(2.dp))
                .background(PureWhite, RoundedCornerShape(2.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(2.dp))
                .pointerInput(currentPageIndex, isStylusOnlyMode, penMode, selectedColor, strokeWidth, showPalmGuard, palmGuardHeightRatio) {
                  awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Stylus-Only rejection check
                    if (isStylusOnlyMode && down.type != PointerType.Stylus) {
                      return@awaitEachGesture
                    }

                    // Palm Rest Guard check
                    if (showPalmGuard) {
                      val guardBoundary = size.height * (1f - palmGuardHeightRatio)
                      if (down.position.y > guardBoundary && down.type != PointerType.Stylus) {
                        return@awaitEachGesture
                      }
                    }

                    val activePageStrokes = pages.getOrNull(currentPageIndex) ?: return@awaitEachGesture

                    // Non-destructive stroke eraser: deletes drawn strokes on proximity without erasing ruled lines!
                    if (penMode == "eraser") {
                      fun eraseNearbyStrokes(pos: Offset, radius: Float = 28f) {
                        val rSq = radius * radius
                        val toRemove = activePageStrokes.filter { s ->
                          s.points.any { p ->
                            val dx = p.x - pos.x
                            val dy = p.y - pos.y
                            (dx * dx + dy * dy) <= rSq
                          }
                        }
                        if (toRemove.isNotEmpty()) {
                          activePageStrokes.removeAll(toRemove)
                          pageRedoStrokes[currentPageIndex]?.clear()
                        }
                      }

                      eraseNearbyStrokes(down.position)
                      down.consume()

                      while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                          eraseNearbyStrokes(change.position)
                          change.consume()
                        } else {
                          break
                        }
                      }
                      return@awaitEachGesture
                    }

                    // Drawing ink strokes (fine, gel, bold, highlighter)
                    val points = mutableListOf<Offset>()

                    // Sub-frame historical sampling for 120-240Hz stylus fidelity
                    for (hist in down.historical) {
                      points.add(hist.position)
                    }
                    points.add(down.position)
                    currentPoints = points.toList()

                    while (true) {
                      val event = awaitPointerEvent()
                      val change = event.changes.firstOrNull { it.id == down.id } ?: break

                      if (change.pressed) {
                        for (hist in change.historical) {
                          points.add(hist.position)
                        }
                        points.add(change.position)
                        currentPoints = points.toList()
                        change.consume()
                      } else {
                        break
                      }
                    }

                    // Finished stroke: commit with smoothing directly into active page's strokes
                    if (points.isNotEmpty()) {
                      val finalColor = when (penMode) {
                        "highlighter" -> Color(0xFFFACC15).copy(alpha = 0.35f)
                        else -> selectedColor
                      }
                      val finalWidth = when (penMode) {
                        "highlighter" -> 14.0f
                        else -> strokeWidth
                      }

                      activePageStrokes.add(
                        DrawStroke(
                          points = points.map { DrawPoint(it.x, it.y) },
                          colorHex = finalColor.value.toLong(),
                          strokeWidth = finalWidth
                        )
                      )
                      currentPoints = emptyList()
                      pageRedoStrokes[currentPageIndex]?.clear()
                    }
                  }
                }
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width > 0f) canvasWidth = size.width
                if (size.height > 0f) canvasHeight = size.height

                // 1. Clean Paper Styles (Ruled paper, grid, blank)
                when (currentTemplate) {
                  AnatomyGuideTemplate.RULED_NOTES -> {
                    // Ruled clinical lines with left vertical margin line
                    val lineSpacing = 28.dp.toPx()
                    val marginX = 36.dp.toPx()

                    // Left margin guide
                    drawLine(
                      color = Color(0xFFFFCDD2),
                      start = Offset(marginX, 20.dp.toPx()),
                      end = Offset(marginX, size.height - 20.dp.toPx()),
                      strokeWidth = 1f
                    )

                    var y = 50.dp.toPx()
                    while (y < size.height - 30.dp.toPx()) {
                      drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(16.dp.toPx(), y),
                        end = Offset(size.width - 16.dp.toPx(), y),
                        strokeWidth = 0.8f
                      )
                      y += lineSpacing
                    }
                  }
                  AnatomyGuideTemplate.GRID_NOTES -> {
                    val gridSpacing = 20.dp.toPx()
                    var x = gridSpacing
                    while (x < size.width) {
                      drawLine(color = Color(0xFFF1F5F9), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 0.6f)
                      x += gridSpacing
                    }
                    var y = gridSpacing
                    while (y < size.height) {
                      drawLine(color = Color(0xFFF1F5F9), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 0.6f)
                      y += gridSpacing
                    }
                  }
                  else -> {
                    // Blank clean paper
                  }
                }

                // 2. Helper for Quadratic Bézier Smoothing
                fun drawSmoothStroke(pts: List<Offset>, strokeColor: Color, width: Float) {
                  if (pts.isEmpty()) return
                  val path = Path()
                  path.moveTo(pts[0].x, pts[0].y)

                  if (pts.size == 1) {
                    path.lineTo(pts[0].x + 0.5f, pts[0].y + 0.5f)
                  } else {
                    for (i in 1 until pts.size) {
                      val p0 = pts[i - 1]
                      val p1 = pts[i]
                      val midX = (p0.x + p1.x) / 2f
                      val midY = (p0.y + p1.y) / 2f
                      path.quadraticBezierTo(p0.x, p0.y, midX, midY)
                    }
                    path.lineTo(pts.last().x, pts.last().y)
                  }

                  drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                      width = width,
                      cap = StrokeCap.Round,
                      join = StrokeJoin.Round
                    )
                  )
                }

                // 3. Draw Committed Strokes for active page
                val activeStrokes = pages.getOrElse(currentPageIndex) { currentStrokes }
                activeStrokes.forEach { stroke ->
                  drawSmoothStroke(stroke.offsets, stroke.color, stroke.strokeWidth)
                }

                // 4. Draw In-Progress Active Stroke (Only in pen / highlighter mode, never in eraser)
                if (currentPoints.isNotEmpty() && penMode != "eraser") {
                  val activeColor = when (penMode) {
                    "highlighter" -> Color(0xFFFACC15).copy(alpha = 0.35f)
                    else -> selectedColor
                  }
                  val activeWidth = when (penMode) {
                    "highlighter" -> 14.0f
                    else -> strokeWidth
                  }
                  drawSmoothStroke(currentPoints, activeColor, activeWidth)
                }
              }

              // Sheet Watermark Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  "CLINICAL CONSULTATION SHEET • ${patient?.name ?: "PATIENT"}",
                  fontSize = 9.sp,
                  color = SlateLight,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  "Sheet ${currentPageIndex + 1} / ${pages.size}",
                  fontSize = 9.sp,
                  color = SlateLight,
                  fontWeight = FontWeight.Bold
                )
              }

              if (currentStrokes.isEmpty() && currentPoints.isEmpty()) {
                Column(
                  modifier = Modifier.align(Alignment.Center),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(Icons.Default.EditNote, contentDescription = null, tint = SlateLight, modifier = Modifier.size(42.dp))
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    if (isStylusOnlyMode) "Stylus Mode Active (Palm Touch Ignored)" else "Write clinical consultation notes here",
                    color = if (isStylusOnlyMode) MedicalGreenDark else SlateMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    "High-frequency 120Hz stylus digitizer with smooth curves",
                    color = SlateLight,
                    fontSize = 10.sp
                  )
                }
              }

              // Visual Palm Rest Guard
              if (showPalmGuard) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(palmGuardHeightRatio)
                    .align(Alignment.BottomCenter)
                    .background(Color(0x220288D1))
                    .border(BorderStroke(1.dp, Color(0x660288D1)))
                ) {
                  Row(
                    modifier = Modifier
                      .align(Alignment.TopCenter)
                      .background(Color(0xCC0288D1), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                      .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.FrontHand, contentDescription = null, tint = PureWhite, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Palm Rest Zone (Rest hand safely here)", color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
