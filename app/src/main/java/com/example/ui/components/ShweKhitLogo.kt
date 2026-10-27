package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalGreenDark
import com.example.ui.theme.PureWhite

private val GoldPrimary = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFFFF8E1)
private val GoldAccent = Color(0xFFFFD700)

@Composable
fun ShweKhitLogo(
  modifier: Modifier = Modifier,
  size: Dp = 38.dp,
  showLabel: Boolean = false,
  customLogoUri: String? = null
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
  ) {
    val decodedModel: Any? = remember(customLogoUri) {
      if (customLogoUri.isNullOrBlank()) null
      else {
        try {
          when {
            customLogoUri.startsWith("/") -> {
              val f = java.io.File(customLogoUri)
              if (f.exists() && f.length() > 0) {
                android.graphics.BitmapFactory.decodeFile(f.absolutePath)
              } else null
            }
            customLogoUri.startsWith("file://") -> {
              val f = java.io.File(customLogoUri.removePrefix("file://"))
              if (f.exists() && f.length() > 0) {
                android.graphics.BitmapFactory.decodeFile(f.absolutePath)
              } else null
            }
            customLogoUri.startsWith("content://") -> android.net.Uri.parse(customLogoUri)
            customLogoUri.startsWith("http://") || customLogoUri.startsWith("https://") -> customLogoUri
            else -> {
              val clean = if (customLogoUri.contains(",")) customLogoUri.substringAfter(",") else customLogoUri
              val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
              android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
          }
        } catch (_: Exception) {
          null
        }
      }
    }

    // Elegant Circular Emblem (White/Neutral for custom logo, Gold & Emerald for default)
    Box(
      modifier = Modifier
        .size(size)
        .shadow(2.dp, CircleShape)
        .clip(CircleShape)
        .background(
          if (decodedModel != null) {
            Brush.radialGradient(listOf(PureWhite, Color(0xFFF8FAFC)))
          } else {
            Brush.radialGradient(listOf(GoldLight, GoldPrimary))
          }
        )
        .border(
          BorderStroke(
            1.5.dp,
            if (decodedModel != null) {
              Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
            } else {
              Brush.linearGradient(listOf(GoldAccent, GoldPrimary, Color(0xFF8C6D1F)))
            }
          ),
          CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      if (decodedModel != null) {
        AsyncImage(
          model = decodedModel,
          contentDescription = "Clinic Logo",
          modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape),
          contentScale = ContentScale.Crop
        )
      } else {
        Image(
          painter = painterResource(id = R.drawable.ic_shwekhit_logo),
          contentDescription = "ရွှေခေတ် Clinic Logo",
          modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
          contentScale = ContentScale.Fit
        )
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          "ShweKhit (ရွှေခေတ်)",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp,
          color = MedicalGreenDark
        )
        Text(
          "CLINIC & PHARMACY",
          fontWeight = FontWeight.SemiBold,
          fontSize = 8.5.sp,
          color = GoldPrimary,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
