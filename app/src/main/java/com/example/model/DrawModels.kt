package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color


enum class AnatomyGuideTemplate(val title: String, val myanmarTitle: String) {
  RULED_NOTES("Ruled Medical Paper", "မျဉ်းသား ဆေးမှတ်တမ်းစာရွက်"),
  BLANK("Blank Clean Paper", "အလွတ် စာရွက်"),
  GRID_NOTES("Clinical Grid Paper", "ဂရစ်ကွက် ဆေးမှတ်တမ်းစာရွက်");

  companion object {
    val FULL_BODY get() = BLANK
    val TORSO get() = BLANK
    val HEAD_ENT get() = BLANK
  }
}
