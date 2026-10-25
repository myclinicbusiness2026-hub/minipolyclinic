package com.example.ui.screens.consultation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MedicalAttachment
import com.example.ui.theme.*

@Composable
fun MedicalAttachmentsCard(
  attachments: List<MedicalAttachment>,
  onOpenAttachments: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = null,
            tint = SkyBluePrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              "ဓာတ်မှန်နှင့် ဆေးစစ်ချက်များ (Attachments)",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = SkyBluePrimary
            )
            Text(
              "${attachments.size} files attached",
              fontSize = 10.sp,
              color = SlateMuted
            )
          }
        }

        Button(
          onClick = onOpenAttachments,
          colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }

      if (attachments.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        attachments.take(3).forEach { att ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(SoftLightGray)
              .clickable { onOpenAttachments() }
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.AttachFile,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = SlateMedium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              att.title.ifBlank { "Attached file" },
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = DarkGrayText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            Text(att.date, fontSize = 9.sp, color = SlateMuted)
          }
          Spacer(modifier = Modifier.height(4.dp))
        }
        if (attachments.size > 3) {
          Text(
            "+${attachments.size - 3} more attachments...",
            fontSize = 9.sp,
            color = SlateMuted,
            modifier = Modifier.padding(start = 4.dp)
          )
        }
      }
    }
  }
}
