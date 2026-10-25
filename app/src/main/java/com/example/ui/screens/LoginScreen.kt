package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.service.sync.LocalWifiSyncService
import com.example.ui.components.ShweKhitLogo
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  viewModel: ClinicViewModel,
  modifier: Modifier = Modifier
) {
  val clinicSettings by viewModel.clinicSettings.collectAsState()
  val localWifiIp by viewModel.localWifiIp.collectAsState()
  val discoveredPeers by LocalWifiSyncService.discoveredPeers.collectAsState()

  val coroutineScope = rememberCoroutineScope()
  val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

  // Default credentials as requested: username "shwekhit", password default "123456"
  var username by remember { mutableStateOf("shwekhit") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var syncMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  var isScanningSubnet by remember { mutableStateOf(false) }

  fun executeLogin() {
    val cleanUsername = username.trim()
    val cleanPassword = password.trim()
    if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
      errorMessage = "ကျေးဇူးပြု၍ အသုံးပြုသူအမည် (Username) နှင့် စကားဝှက် (Password) ရိုက်ထည့်ပါ"
      return
    }
    keyboardController?.hide()
    focusManager.clearFocus(force = true)
    errorMessage = null
    isSubmitting = true

    coroutineScope.launch {
      val result = viewModel.loginWithCloudFallback(cleanUsername, cleanPassword)
      isSubmitting = false
      if (!result.first) {
        errorMessage = result.second
      } else {
        val loggedInUser = viewModel.currentUser.value
        val tab = when (loggedInUser?.role) {
          UserRole.CASHIER -> 1
          else -> 0
        }
        viewModel.setTab(tab)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SoftLightGray)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .imePadding()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 440.dp)
        .fillMaxWidth()
        .testTag("login_card"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      border = BorderStroke(1.dp, BorderGray)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Clinic Branding Header
        ShweKhitLogo(
          size = 48.dp,
          customLogoUri = clinicSettings.clinicLogoUri.ifBlank { null }
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "\"${clinicSettings.clinicNameMm.ifBlank { "ရွှေခေတ်" }}\" ${clinicSettings.clinicSubtitleMm.ifBlank { "အထွေထွေရောဂါကုဆေးခန်း" }}",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = MedicalGreen,
          textAlign = TextAlign.Center
        )

        Text(
          text = "${clinicSettings.doctorName.ifBlank { "Dr. Nyan Kaung Sett" }} ${clinicSettings.doctorDegree.ifBlank { "M.B.,B.S (MDY)" }} • ဆမ-${clinicSettings.doctorSama.ifBlank { "၄၇၄၁၄" }}",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium,
          color = MahoganyAccent,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Clinical Station & Pharmacy Management System",
          fontSize = 10.sp,
          color = DarkGrayText.copy(alpha = 0.7f),
          textAlign = TextAlign.Center
        )

        HorizontalDivider(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
          thickness = 1.dp,
          color = MahoganyAccent.copy(alpha = 0.2f)
        )

        // Error message banner
        if (errorMessage != null) {
          Surface(
            color = Color(0xFFFEE2E2),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFFF87171)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                errorMessage ?: "",
                fontSize = 11.sp,
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Sync feedback banner
        if (syncMessage != null) {
          Surface(
            color = Color(0xFFE0F2FE),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                syncMessage ?: "",
                fontSize = 11.sp,
                color = Color(0xFF0369A1),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // ----------------------------------------------------
        // USERNAME & PASSWORD LOGIN FORM
        // ----------------------------------------------------
        Text(
          "စနစ်သို့ ဝင်ရောက်ရန် (Sign In)",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = SlateDark,
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          "သတ်မှတ်ထားသော Username နှင့် Password ဖြင့် လော့ဂ်အင်ဝင်ပါ",
          fontSize = 10.sp,
          color = SlateMedium,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Username Field
        OutlinedTextField(
          value = username,
          onValueChange = {
            username = it
            errorMessage = null
          },
          label = { Text("အသုံးပြုသူအမည် (Username)", fontSize = 11.5.sp) },
          placeholder = { Text("e.g. shwekhit", fontSize = 11.5.sp) },
          leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
          },
          trailingIcon = {
            if (username.isNotEmpty()) {
              IconButton(onClick = { username = "" }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("username_input"),
          shape = RoundedCornerShape(8.dp),
          colors = clinicTextFieldColors(),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Password Field
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            errorMessage = null
          },
          label = { Text("စကားဝှက် (Password)", fontSize = 11.5.sp) },
          placeholder = { Text("Default: 123456", fontSize = 11.5.sp) },
          leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MedicalGreen, modifier = Modifier.size(18.dp))
          },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                tint = DarkGrayText.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
              )
            }
          },
          singleLine = true,
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { executeLogin() }),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("password_input"),
          shape = RoundedCornerShape(8.dp),
          colors = clinicTextFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login Button
        Button(
          onClick = { executeLogin() },
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("login_button"),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
          enabled = !isSubmitting
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PureWhite, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("စစ်ဆေးနေပါသည်...", fontSize = 12.sp, color = PureWhite)
          } else {
            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "စနစ်သို့ ဝင်ရောက်မည် (Log In)",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------------------------------------------
        // LOCAL WI-FI ROUTER SYNC STATUS (INFO ONLY - NO BYPASS)
        // ----------------------------------------------------
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFF8FAFC),
          border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Router,
                  contentDescription = null,
                  tint = if (localWifiIp != "127.0.0.1") MedicalGreen else SlateMedium,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(
                    "Local Wi-Fi Network Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = SlateDark
                  )
                  Text(
                    "IP: $localWifiIp:8080 • ${discoveredPeers.size} peer(s)",
                    fontSize = 8.5.sp,
                    color = SlateMedium
                  )
                }
              }

              OutlinedButton(
                onClick = {
                  coroutineScope.launch {
                    isScanningSubnet = true
                    syncMessage = "Wi-Fi LAN စက်များကို ရှာဖွေနေပါသည်..."
                    val swept = LocalWifiSyncService.scanSubnetForPeers(350)
                    isScanningSubnet = false
                    syncMessage = if (swept.isNotEmpty()) {
                      "စက် ${swept.size} လုံး တွေ့ရှိပါသည်"
                    } else {
                      "အခြားစက် မတွေ့ရှိသေးပါ"
                    }
                  }
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp),
                shape = RoundedCornerShape(4.dp),
                enabled = !isScanningSubnet
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(if (isScanningSubnet) "Scanning..." else "Rescan", fontSize = 8.5.sp)
              }
            }
          }
        }
      }
    }
  }
}
