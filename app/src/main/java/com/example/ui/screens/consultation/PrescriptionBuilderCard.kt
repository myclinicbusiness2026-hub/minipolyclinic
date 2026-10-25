package com.example.ui.screens.consultation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrescribedDrug
import com.example.ui.theme.*
import java.text.NumberFormat

@Composable
fun PrescriptionBuilderCard(
  prescribedDrugs: List<PrescribedDrug>,
  onOpenAddDrug: () -> Unit,
  onRemoveDrug: (Int) -> Unit,
  currencyFmt: NumberFormat
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
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
        Column {
          Text("Prescriptions (Rx)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SkyBluePrimary)
          Text("${prescribedDrugs.size} items prescribed", fontSize = 10.sp, color = SlateMuted)
        }

        Button(
          onClick = onOpenAddDrug,
          colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Rx", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (prescribedDrugs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(BackgroundSurface)
            .padding(12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No medications selected yet. Tap '+ Add Rx' to pick from stock.", fontSize = 11.sp, color = SlateMuted)
        }
      } else {
        prescribedDrugs.forEachIndexed { index, drug ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(BackgroundSurface)
              .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
              .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "${index + 1}. ${drug.name} (${drug.dosage})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = SlateDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                "${drug.frequency} • ${drug.instructions} x ${drug.duration} • Total Qty: ${drug.quantity}",
                fontSize = 10.sp,
                color = SlateMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              val isExt = drug.isExternal || drug.medicineId.startsWith("EXT-")
              if (isExt) {
                Surface(
                  color = Color(0xFFFEF3C7),
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(0.8.dp, Color(0xFFF59E0B))
                ) {
                  Text(
                    "ပြင်ပဆေး (External)",
                    fontSize = 9.sp,
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              } else {
                Text(
                  "${currencyFmt.format(drug.totalPrice.toInt())} Ks",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = SkyBluePrimary
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                onClick = { onRemoveDrug(index) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }
  }
}
