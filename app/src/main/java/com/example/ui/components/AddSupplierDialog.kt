package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun AddSupplierDialog(
  onDismiss: () -> Unit,
  onAdd: (name: String, contactPerson: String, phone: String, email: String, address: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var contactPerson by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("09-") }
  var email by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 440.dp)
        .padding(vertical = 12.dp),
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      border = BorderStroke(1.dp, BorderSubtleRedBrown)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "Add Pharmaceutical Supplier",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = DarkGrayText
            )
            Text(
              "ဆေးဝါးတင်သွင်း ဖြန့်ချိရေးကုမ္ပဏီအသစ်ထည့်သွင်းရန်",
              fontSize = 10.5.sp,
              color = SlateMuted
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Company / Supplier Name *", fontSize = 12.sp) },
          placeholder = { Text("e.g. AA Medical Products / DKSH", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = contactPerson,
          onValueChange = { contactPerson = it },
          label = { Text("Contact Person / Sales Rep", fontSize = 12.sp) },
          placeholder = { Text("e.g. Ko Min Thein", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number *", fontSize = 12.sp) },
          placeholder = { Text("e.g. 09-421001122", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address", fontSize = 12.sp) },
          placeholder = { Text("orders@distributor.com.mm", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Office / Warehouse Address", fontSize = 12.sp) },
          placeholder = { Text("e.g. Bahan Township, Yangon", fontSize = 12.sp) },
          leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = SlateMuted, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp),
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(38.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, BorderGray),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("Cancel", color = SlateMedium, fontSize = 11.5.sp)
          }

          Button(
            onClick = {
              if (name.isNotBlank() && phone.isNotBlank()) {
                onAdd(name.trim(), contactPerson.trim(), phone.trim(), email.trim(), address.trim())
                onDismiss()
              }
            },
            enabled = name.isNotBlank() && phone.isNotBlank(),
            modifier = Modifier
              .weight(1.5f)
              .height(38.dp),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Supplier", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  }
}
