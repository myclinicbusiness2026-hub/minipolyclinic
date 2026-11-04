package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.TabAccessLevel
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.model.UserRolePermissions
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementDialog(
  viewModel: ClinicViewModel,
  onDismiss: () -> Unit
) {
  val users by viewModel.users.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  // Selected top tab: 0 = User List, 1 = Add/Edit Form, 2 = Role Matrix, 3 = Active Session
  var activeSubTab by remember { mutableStateOf(0) }

  // Feedback banner state
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var isError by remember { mutableStateOf(false) }

  // Search & Filter in list
  var searchQuery by remember { mutableStateOf("") }
  var filterRole by remember { mutableStateOf<UserRole?>(null) }

  // Edit User State (null = create new)
  var editingUser by remember { mutableStateOf<UserAccount?>(null) }

  // Password Reset Dialog State
  var resetPasswordUser by remember { mutableStateOf<UserAccount?>(null) }
  var resetNewPasswordText by remember { mutableStateOf("") }

  // Form input fields
  var inputUsername by remember { mutableStateOf("") }
  var inputFullName by remember { mutableStateOf("") }
  var inputPassword by remember { mutableStateOf("123456") }
  var inputRole by remember { mutableStateOf(UserRole.DOCTOR) }
  var inputSamaNumber by remember { mutableStateOf("") }
  var inputPhone by remember { mutableStateOf("09-") }
  var inputEmail by remember { mutableStateOf("") }
  var inputDepartment by remember { mutableStateOf("General OPD") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var showExportPairingDialog by remember { mutableStateOf(false) }

  // Custom Permissions Form State (3-Level Access: SEALED, VIEW_ONLY, ALLOW_EDIT)
  var permQueue by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permDoctorEmr by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permTreatment by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permPharmacy by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permBilling by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permProcedures by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permInventory by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permLedger by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permRecords by remember { mutableStateOf(TabAccessLevel.ALLOW_EDIT) }
  var permUsers by remember { mutableStateOf(TabAccessLevel.SEALED) }

  fun populateFormForNew() {
    editingUser = null
    inputUsername = ""
    inputFullName = ""
    inputPassword = "123456"
    inputRole = UserRole.DOCTOR
    inputSamaNumber = ""
    inputPhone = "09-"
    inputEmail = ""
    inputDepartment = "General OPD"
    val defaultPerm = UserRolePermissions.defaultFor(UserRole.DOCTOR)
    permQueue = defaultPerm.queueAccess
    permDoctorEmr = defaultPerm.doctorEmrAccess
    permTreatment = defaultPerm.treatmentAccess
    permPharmacy = defaultPerm.pharmacyAccess
    permBilling = defaultPerm.billingAccess
    permProcedures = defaultPerm.proceduresAccess
    permInventory = defaultPerm.inventoryAccess
    permLedger = defaultPerm.financialLedgerAccess
    permRecords = defaultPerm.medicalRecordsAccess
    permUsers = defaultPerm.userManagementAccess
  }

  fun populateFormForEdit(user: UserAccount) {
    editingUser = user
    inputUsername = user.username
    inputFullName = user.fullName
    inputPassword = user.passwordHash
    inputRole = user.role
    inputSamaNumber = user.samaNumber ?: ""
    inputPhone = user.phone
    inputEmail = user.email
    inputDepartment = user.department
    val currentPerm = user.effectivePermissions
    permQueue = currentPerm.queueAccess
    permDoctorEmr = currentPerm.doctorEmrAccess
    permTreatment = currentPerm.treatmentAccess
    permPharmacy = currentPerm.pharmacyAccess
    permBilling = currentPerm.billingAccess
    permProcedures = currentPerm.proceduresAccess
    permInventory = currentPerm.inventoryAccess
    permLedger = currentPerm.financialLedgerAccess
    permRecords = currentPerm.medicalRecordsAccess
    permUsers = currentPerm.userManagementAccess
  }

  fun getRoleColor(role: UserRole): Color = when (role) {
    UserRole.ADMIN -> AccentRedBrown
    UserRole.DOCTOR -> MedicalGreen
    UserRole.PHARMACIST -> OceanGreenPrimary
    UserRole.CASHIER -> Color(0xFFD97706)
    UserRole.NURSE -> Color(0xFF7C3AED)
    UserRole.RECEPTIONIST -> Color(0xFF0D9488)
  }

  fun getRoleBg(role: UserRole): Color = when (role) {
    UserRole.ADMIN -> StatusCriticalContainer
    UserRole.DOCTOR -> SoftGreenContainer
    UserRole.PHARMACIST -> OceanSurface
    UserRole.CASHIER -> StatusWarningContainer
    UserRole.NURSE -> Color(0xFFF3E8FF)
    UserRole.RECEPTIONIST -> Color(0xFFCCFBF1)
  }

  fun getRoleIcon(role: UserRole) = when (role) {
    UserRole.ADMIN -> Icons.Default.Security
    UserRole.DOCTOR -> Icons.Default.MedicalServices
    UserRole.PHARMACIST -> Icons.Default.Medication
    UserRole.CASHIER -> Icons.Default.PointOfSale
    UserRole.NURSE -> Icons.Default.LocalHospital
    UserRole.RECEPTIONIST -> Icons.Default.Badge
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .widthIn(max = 680.dp)
        .heightIn(max = 680.dp),
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
      border = BorderStroke(1.dp, BorderSubtleRedBrown)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp)
      ) {
        // --- HEADER ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SoftGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MedicalGreen,
                modifier = Modifier.size(19.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "အသုံးပြုသူအကောင့်များနှင့် ရာထူးစီမံခန့်ခွဲမှု (User Accounts & RBAC)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkGrayText,
                fontSize = 13.sp
              )
              Text(
                "ရွှေခေတ် ဆေးခန်းစနစ် • Local SQLite Offline-First Security",
                fontSize = 9.5.sp,
                color = SlateMuted
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateMuted, modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SUB TABS NAVIGATION ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(BackgroundSurface)
            .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val tabList = listOf(
            Triple(0, "အကောင့်များ (${users.size})", Icons.Default.People),
            Triple(1, if (editingUser != null) "ပြင်ဆင်ရန်" else "+ အကောင့်သစ်", Icons.Default.PersonAdd),
            Triple(2, "ရာထူး/ခွင့်ပြုချက်", Icons.Default.VerifiedUser),
            Triple(3, "လက်ရှိအကောင့်", Icons.Default.AccountCircle)
          )

          tabList.forEach { (index, title, icon) ->
            val isSelected = activeSubTab == index
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                  if (index == 1 && activeSubTab != 1 && editingUser == null) {
                    populateFormForNew()
                  }
                  activeSubTab = index
                },
              shape = RoundedCornerShape(4.dp),
              color = if (isSelected) PureWhite else Color.Transparent,
              border = if (isSelected) BorderStroke(1.dp, BorderGrey) else null
            ) {
              Row(
                modifier = Modifier
                  .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  icon,
                  contentDescription = null,
                  modifier = Modifier.size(13.dp),
                  tint = if (isSelected) MedicalGreen else SlateMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  title,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MedicalGreenDark else SlateMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Feedback Banner
        feedbackMessage?.let { msg ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp),
            shape = RoundedCornerShape(4.dp),
            color = if (isError) StatusCriticalContainer else StatusSuccessContainer,
            border = BorderStroke(1.dp, if (isError) StatusCritical else StatusSuccess)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) StatusCritical else StatusSuccess,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                msg,
                color = if (isError) StatusCritical else StatusSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.weight(1f))
              IconButton(
                onClick = { feedbackMessage = null },
                modifier = Modifier.size(16.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = SlateMedium, modifier = Modifier.size(10.dp))
              }
            }
          }
        }

        // --- SUB TAB CONTENTS ---
        when (activeSubTab) {
          0 -> {
            // ==========================================
            // TAB 0: USER LIST & MANAGEMENT
            // ==========================================
            Column(modifier = Modifier.fillMaxSize()) {
              OutlinedButton(
                onClick = { showExportPairingDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalGreen),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(34.dp)
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("🔗 အခြားစက်သို့ အကောင့်များကူးယူရန် (Pairing QR & Token)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Search and Filter Bar
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  placeholder = { Text("အမည်၊ Username၊ ဖုန်းဖြင့်ရှာရန်...", fontSize = 10.sp, color = SlateLight) },
                  leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = SlateMuted) },
                  trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                      IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp), tint = SlateMuted)
                      }
                    }
                  },
                  modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                  textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                  singleLine = true,
                  shape = RoundedCornerShape(4.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MedicalGreen,
                    unfocusedBorderColor = BorderGrey,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                  )
                )

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                  onClick = {
                    val count = viewModel.cleanupTestAccounts()
                    if (count > 0) {
                      feedbackMessage = "မလိုလားအပ်သော Test Account ($count) ခုကို ဖယ်ရှားပြီးပါပြီ"
                      isError = false
                    } else {
                      feedbackMessage = "ဖယ်ရှားရန် Test Account မရှိပါ (တရားဝင် အကောင့်များသာ ရှိပါသည်)"
                      isError = false
                    }
                  },
                  modifier = Modifier.height(36.dp),
                  shape = RoundedCornerShape(4.dp),
                  border = BorderStroke(0.8.dp, MahoganyAccent.copy(alpha = 0.7f)),
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                  Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp), tint = MahoganyAccent)
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Test Accounts ရှင်းမည်", fontSize = 9.sp, color = MahoganyAccent)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                  onClick = {
                    populateFormForNew()
                    activeSubTab = 1
                  },
                  modifier = Modifier.height(36.dp),
                  shape = RoundedCornerShape(4.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                  Icon(Icons.Default.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("အကောင့်သစ်", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Role Filter Chips
              LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                item {
                  FilterChip(
                    selected = filterRole == null,
                    onClick = { filterRole = null },
                    label = { Text("အားလုံး (${users.size})", fontSize = 9.sp) },
                    modifier = Modifier.height(26.dp),
                    shape = RoundedCornerShape(4.dp)
                  )
                }
                items(UserRole.values()) { role ->
                  val roleCount = users.count { it.role == role }
                  FilterChip(
                    selected = filterRole == role,
                    onClick = { filterRole = if (filterRole == role) null else role },
                    label = { Text("${role.displayName} ($roleCount)", fontSize = 9.sp) },
                    modifier = Modifier.height(26.dp),
                    shape = RoundedCornerShape(4.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Filtered User List
              val filteredUsers = users.filter { u ->
                val matchesSearch = searchQuery.isBlank() ||
                  u.fullName.contains(searchQuery, ignoreCase = true) ||
                  u.username.contains(searchQuery, ignoreCase = true) ||
                  u.phone.contains(searchQuery, ignoreCase = true) ||
                  u.department.contains(searchQuery, ignoreCase = true) ||
                  (u.samaNumber != null && u.samaNumber.contains(searchQuery, ignoreCase = true))

                val matchesRole = filterRole == null || u.role == filterRole
                matchesSearch && matchesRole
              }

              if (filteredUsers.isEmpty()) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = SlateLight, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ရှာဖွေမှုနှင့်ကိုက်ညီသော အကောင့်မရှိပါ", fontSize = 11.sp, color = SlateMuted)
                  }
                }
              } else {
                LazyColumn(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  val curUserId = currentUser?.id
                  items(filteredUsers) { user ->
                    val isCurrentUser = curUserId != null && curUserId == user.id
                    val roleColor = getRoleColor(user.role)
                    val roleBg = getRoleBg(user.role)

                    Card(
                      modifier = Modifier.fillMaxWidth(),
                      shape = RoundedCornerShape(5.dp),
                      colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentUser) SoftGreenContainer.copy(alpha = 0.45f) else PureWhite
                      ),
                      border = BorderStroke(
                        1.dp,
                        if (isCurrentUser) MedicalGreen else BorderGrey
                      )
                    ) {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 8.dp, vertical = 6.dp)
                      ) {
                        // Row 1: Role Icon, Full Name, Current Badge, Status, Quick Actions
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Box(
                            modifier = Modifier
                              .size(26.dp)
                              .clip(RoundedCornerShape(4.dp))
                              .background(roleBg),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(
                              getRoleIcon(user.role),
                              contentDescription = null,
                              tint = roleColor,
                              modifier = Modifier.size(15.dp)
                            )
                          }

                          Spacer(modifier = Modifier.width(6.dp))

                          Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Text(
                                user.fullName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGrayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                              )
                              if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                  shape = RoundedCornerShape(3.dp),
                                  color = MedicalGreen,
                                  contentColor = PureWhite
                                ) {
                                  Text(
                                    "လက်ရှိအသုံးပြုသူ",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                  )
                                }
                              }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Text(
                                "@${user.username}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateMuted
                              )
                              Spacer(modifier = Modifier.width(4.dp))
                              Text(
                                "• ${user.role.displayName} (${user.role.myanmarName})",
                                fontSize = 8.5.sp,
                                color = roleColor,
                                fontWeight = FontWeight.SemiBold
                              )
                              if (user.samaNumber != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                  "• ဆမ-${user.samaNumber}",
                                  fontSize = 8.5.sp,
                                  color = MedicalGreenDark,
                                  fontWeight = FontWeight.SemiBold
                                )
                              }
                            }
                          }

                          // Status Badge
                          Surface(
                            modifier = Modifier
                              .clip(RoundedCornerShape(3.dp))
                              .clickable {
                                viewModel.toggleUserStatus(user.id)
                                feedbackMessage = "@${user.username} ၏ အခြေအနေကို ပြောင်းလဲပြီးပါပြီ"
                                isError = false
                              },
                            shape = RoundedCornerShape(3.dp),
                            color = if (user.isActive) StatusSuccessContainer else StatusCriticalContainer,
                            border = BorderStroke(0.8.dp, if (user.isActive) StatusSuccess else StatusCritical)
                          ) {
                            Text(
                              if (user.isActive) "Active" else "Disabled",
                              fontSize = 8.sp,
                              fontWeight = FontWeight.Bold,
                              color = if (user.isActive) StatusSuccess else StatusCritical,
                              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                          }

                          Spacer(modifier = Modifier.width(4.dp))

                          // Switch Active User Button
                          if (!isCurrentUser) {
                            Button(
                              onClick = {
                                viewModel.switchActiveUser(user)
                                feedbackMessage = "@${user.username} (${user.role.displayName}) သို့ အကောင့်ပြောင်းသုံးပြီးပါပြီ"
                                isError = false
                              },
                              modifier = Modifier.height(24.dp),
                              shape = RoundedCornerShape(3.dp),
                              colors = ButtonDefaults.buttonColors(containerColor = SoftGreenContainer),
                              border = BorderStroke(0.8.dp, MedicalGreen),
                              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                              Text("ပြောင်းသုံး", fontSize = 8.5.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                          }

                          // Edit Button
                          IconButton(
                            onClick = {
                              populateFormForEdit(user)
                              activeSubTab = 1
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SlateMedium, modifier = Modifier.size(13.dp))
                          }

                          // Password Reset Button
                          IconButton(
                            onClick = {
                              resetPasswordUser = user
                              resetNewPasswordText = ""
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(Icons.Default.LockReset, contentDescription = "Password", tint = StatusWarning, modifier = Modifier.size(13.dp))
                          }

                          // Delete Button (disabled for self or last admin)
                          val canDelete = !isCurrentUser && !(user.role == UserRole.ADMIN && users.count { it.role == UserRole.ADMIN } <= 1)
                          if (canDelete) {
                            IconButton(
                              onClick = {
                                val success = viewModel.deleteUserAccount(user.id)
                                if (success) {
                                  feedbackMessage = "@${user.username} အကောင့်ကို ဖျက်ပြီးပါပြီ"
                                  isError = false
                                } else {
                                  feedbackMessage = "အကောင့်ကို ဖျက်၍မရပါ"
                                  isError = true
                                }
                              },
                              modifier = Modifier.size(24.dp)
                            ) {
                              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusCritical, modifier = Modifier.size(13.dp))
                            }
                          }
                        }

                        // Row 2: Department, Phone, Last Login, Permissions Summary
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text("ဌာန: ${user.department}", fontSize = 8.5.sp, color = SlateMuted)
                          if (user.phone.isNotBlank() && user.phone != "09-") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("• ဖုန်း: ${user.phone}", fontSize = 8.5.sp, color = SlateMuted)
                          }
                          Spacer(modifier = Modifier.weight(1f))
                          Text("နောက်ဆုံးဝင်: ${user.lastLoginAt}", fontSize = 8.sp, color = SlateLight)
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          1 -> {
            // ==========================================
            // TAB 1: CREATE / EDIT USER FORM
            // ==========================================
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    if (editingUser != null) "အကောင့်ပြင်ဆင်ရန်: @${editingUser?.username}" else "အကောင့်သစ်ဖွင့်လှစ်ရန်",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGrayText
                  )
                  if (editingUser != null) {
                    TextButton(
                      onClick = { populateFormForNew() },
                      contentPadding = PaddingValues(0.dp)
                    ) {
                      Text("+ အကောင့်သစ်ပြောင်းဖြည့်မည်", fontSize = 10.sp, color = MedicalGreen)
                    }
                  }
                }
              }

              // Username & Full Name
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedTextField(
                    value = inputUsername,
                    onValueChange = { inputUsername = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
                    enabled = true,
                    label = { Text("Username * (Admin ပြင်ဆင်နိုင်သည်)", fontSize = 9.sp) },
                    placeholder = { Text("e.g. drkyaw, nurse_moe", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MedicalGreen,
                      unfocusedBorderColor = BorderGrey
                    )
                  )

                  OutlinedTextField(
                    value = inputFullName,
                    onValueChange = { inputFullName = it },
                    label = { Text("ဝန်ထမ်းအမည်ရင်း *", fontSize = 9.sp) },
                    placeholder = { Text("e.g. ဒေါ်မိုးမိုးစန်း", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MedicalGreen,
                      unfocusedBorderColor = BorderGrey
                    )
                  )
                }
              }

              // Password & Phone
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = { Text("စကားဝှက် (Password)", fontSize = 9.sp) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                      IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                          if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                          contentDescription = null,
                          modifier = Modifier.size(15.dp),
                          tint = SlateMuted
                        )
                      }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MedicalGreen,
                      unfocusedBorderColor = BorderGrey
                    )
                  )

                  OutlinedTextField(
                    value = inputPhone,
                    onValueChange = { inputPhone = it },
                    label = { Text("ဆက်သွယ်ရန် ဖုန်းနံပါတ်", fontSize = 9.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MedicalGreen,
                      unfocusedBorderColor = BorderGrey
                    )
                  )
                }
              }

              // Department & Role Selection
              item {
                Text("တာဝန်ယူရမည့် ရာထူး (Clinic Role) *", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  items(UserRole.values()) { role ->
                    val isSelected = inputRole == role
                    Surface(
                      modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                          inputRole = role
                          // Auto update permissions to default for role
                          val d = UserRolePermissions.defaultFor(role)
                          permQueue = d.queueAccess
                          permDoctorEmr = d.doctorEmrAccess
                          permTreatment = d.treatmentAccess
                          permPharmacy = d.pharmacyAccess
                          permBilling = d.billingAccess
                          permProcedures = d.proceduresAccess
                          permInventory = d.inventoryAccess
                          permLedger = d.financialLedgerAccess
                          permRecords = d.medicalRecordsAccess
                          permUsers = d.userManagementAccess

                          // Auto select department
                          inputDepartment = when (role) {
                            UserRole.ADMIN -> "Administration & IT"
                            UserRole.DOCTOR -> "General OPD Clinic"
                            UserRole.PHARMACIST -> "Pharmacy Dispensary"
                            UserRole.CASHIER -> "Cashier & Billing Desk"
                            UserRole.NURSE -> "Triage & Nursing Station"
                            UserRole.RECEPTIONIST -> "Front Desk Registration"
                          }
                        },
                      shape = RoundedCornerShape(4.dp),
                      color = if (isSelected) getRoleBg(role) else BackgroundSurface,
                      border = BorderStroke(1.dp, if (isSelected) getRoleColor(role) else BorderGrey)
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(getRoleIcon(role), contentDescription = null, tint = getRoleColor(role), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          role.displayName,
                          fontSize = 9.5.sp,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                          color = if (isSelected) getRoleColor(role) else SlateDark
                        )
                      }
                    }
                  }
                }
              }

              // Doctor SAMA Number (Shown when Doctor is selected)
              if (inputRole == UserRole.DOCTOR) {
                item {
                  OutlinedTextField(
                    value = inputSamaNumber,
                    onValueChange = { inputSamaNumber = it.filter { c -> c.isDigit() } },
                    label = { Text("ဆရာဝန် ဆမ အမှတ် (SAMA Reg No.) *", fontSize = 9.sp) },
                    placeholder = { Text("e.g. 47414", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = MedicalGreen,
                      unfocusedBorderColor = BorderGrey
                    )
                  )
                }
              }

              // Department Field
              item {
                OutlinedTextField(
                  value = inputDepartment,
                  onValueChange = { inputDepartment = it },
                  label = { Text("ဌာန / တာဝန်ကျနေရာ (Department)", fontSize = 9.sp) },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  shape = RoundedCornerShape(4.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MedicalGreen,
                    unfocusedBorderColor = BorderGrey
                  )
                )
              }

              // Custom Permissions (3-Level Granular Control per Tab: Seal, View Only, Allow to Edit)
              item {
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = CardDefaults.cardColors(containerColor = BackgroundSurface),
                  border = BorderStroke(0.8.dp, BorderGrey)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text("Tab အလိုက် လုပ်ပိုင်ခွင့်များ သတ်မှတ်ရန် (Tab Permissions):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                        Text("Seal (မမြင်ရ) | View Only (ကြည့်ရုံ) | Allow to Edit (ပြင်ဆင်ခွင့်)", fontSize = 8.sp, color = SlateMuted)
                      }
                      TextButton(
                        onClick = {
                          val d = UserRolePermissions.defaultFor(inputRole)
                          permQueue = d.queueAccess
                          permDoctorEmr = d.doctorEmrAccess
                          permTreatment = d.treatmentAccess
                          permPharmacy = d.pharmacyAccess
                          permBilling = d.billingAccess
                          permProcedures = d.proceduresAccess
                          permInventory = d.inventoryAccess
                          permLedger = d.financialLedgerAccess
                          permRecords = d.medicalRecordsAccess
                          permUsers = d.userManagementAccess
                        },
                        contentPadding = PaddingValues(0.dp)
                      ) {
                        Text("မူရင်းအတိုင်း ပြန်ထားမည်", fontSize = 8.5.sp, color = MedicalGreen)
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(2.dp))

                    TabAccessSelectorRow("1. Patient Queue / Registration", "လူနာစာရင်း & မှတ်ပုံတင်", Icons.Default.People, permQueue) { permQueue = it }
                    TabAccessSelectorRow("2. Doctor Consultation & EMR", "ဆရာဝန်စမ်းသပ်ခန်း & ဆေးမှတ်တမ်း", Icons.Default.MedicalServices, permDoctorEmr) { permDoctorEmr = it }
                    TabAccessSelectorRow("3. Treatment & Injections", "ဆေးထိုး/အကြောပုလင်း ကုသဆောင်", Icons.Default.Healing, permTreatment) { permTreatment = it }
                    TabAccessSelectorRow("4. Pharmacy Dispense", "ဆေးဆိုင် & ဆေးထုတ်ပေးခြင်း", Icons.Default.Medication, permPharmacy) { permPharmacy = it }
                    TabAccessSelectorRow("5. Cashier & Invoices", "ငွေရှင်းကောင်တာ & ဘောင်ချာ", Icons.Default.PointOfSale, permBilling) { permBilling = it }
                    TabAccessSelectorRow("6. Clinical Procedures", "ဒဏ်ရာဆေးကြော & ကုသမှုများ", Icons.Default.Vaccines, permProcedures) { permProcedures = it }
                    TabAccessSelectorRow("7. Inventory Master", "စတော့အချုပ် & တန်ဖိုးသတ်မှတ်ချက်", Icons.Default.Inventory2, permInventory) { permInventory = it }
                    TabAccessSelectorRow("8. Clinic Ledger / Finance", "ဝင်ငွေ ထွက်ငွေ ဇယား (ငွေစာရင်း)", Icons.Default.AccountBalanceWallet, permLedger) { permLedger = it }
                    TabAccessSelectorRow("9. Medical Records Archive", "လူနာ ဆေးမှတ်တမ်းဟောင်းများ", Icons.Default.FolderShared, permRecords) { permRecords = it }
                    TabAccessSelectorRow("10. User Management", "ဝန်ထမ်းအကောင့်များ စီမံခန့်ခွဲခြင်း", Icons.Default.ManageAccounts, permUsers) { permUsers = it }
                  }
                }
              }

              // Action Buttons
              item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End
                ) {
                  OutlinedButton(
                    onClick = {
                      populateFormForNew()
                      activeSubTab = 0
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                  ) {
                    Text("မလုပ်တော့ပါ", fontSize = 10.sp, color = SlateMedium)
                  }

                  Spacer(modifier = Modifier.width(6.dp))

                  Button(
                    onClick = {
                      if (inputUsername.isBlank() || inputFullName.isBlank()) {
                        feedbackMessage = "Username နှင့် ဝန်ထမ်းအမည် ပြည့်စုံစွာ ဖြည့်ပါ"
                        isError = true
                        return@Button
                      }

                      val permissions = UserRolePermissions(
                        queueAccess = permQueue,
                        doctorEmrAccess = permDoctorEmr,
                        treatmentAccess = permTreatment,
                        pharmacyAccess = permPharmacy,
                        billingAccess = permBilling,
                        proceduresAccess = permProcedures,
                        inventoryAccess = permInventory,
                        financialLedgerAccess = permLedger,
                        medicalRecordsAccess = permRecords,
                        userManagementAccess = permUsers
                      )

                      if (editingUser != null) {
                        // Update existing user (Username and Password configurable by Admin)
                        val success = viewModel.updateUserAccount(
                          userId = editingUser!!.id,
                          username = inputUsername,
                          passwordHash = inputPassword.ifBlank { null },
                          fullName = inputFullName,
                          role = inputRole,
                          samaNumber = if (inputRole == UserRole.DOCTOR) inputSamaNumber.ifBlank { null } else null,
                          phone = inputPhone,
                          email = inputEmail,
                          department = inputDepartment,
                          customPermissions = permissions
                        )
                        if (success) {
                          feedbackMessage = "@$inputUsername ၏ Username၊ Password နှင့် အချက်အလက်များကို အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ"
                          isError = false
                          activeSubTab = 0
                          editingUser = null
                        } else {
                          feedbackMessage = "အကောင့်ပြင်ဆင်မှု မအောင်မြင်ပါ (Username ထပ်နေနိုင်ပါသည်)"
                          isError = true
                        }
                      } else {
                        // Create new user
                        val success = viewModel.createUserAccount(
                          username = inputUsername,
                          fullName = inputFullName,
                          passwordHash = inputPassword.ifBlank { "123456" },
                          role = inputRole,
                          samaNumber = if (inputRole == UserRole.DOCTOR) inputSamaNumber.ifBlank { null } else null,
                          phone = inputPhone,
                          email = inputEmail,
                          department = inputDepartment,
                          customPermissions = permissions
                        )
                        if (success) {
                          feedbackMessage = "အကောင့်သစ် (@$inputUsername) အောင်မြင်စွာ ဖန်တီးပြီးပါပြီ"
                          isError = false
                          activeSubTab = 0
                          populateFormForNew()
                        } else {
                          feedbackMessage = "ဤ Username သည် ရှိပြီးဖြစ်နေပါသည်"
                          isError = true
                        }
                      }
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                  ) {
                    Icon(
                      if (editingUser != null) Icons.Default.Save else Icons.Default.PersonAddAlt1,
                      contentDescription = null,
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      if (editingUser != null) "သိမ်းဆည်းမည်" else "အကောင့်ဖွင့်မည်",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }

          2 -> {
            // ==========================================
            // TAB 2: ROLE MATRIX & RBAC OVERVIEW
            // ==========================================
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              item {
                Text(
                  "ရာထူးဆိုင်ရာ လုပ်ပိုင်ခွင့်များ ဇယား (Role-Based Access Control Matrix)",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = DarkGrayText
                )
                Text(
                  "ဆေးခန်းရှိ တာဝန်ကျရာထူးအလိုက် မူရင်းခွင့်ပြုထားသော လုပ်ပိုင်ခွင့်အကျဉ်းချုပ်",
                  fontSize = 9.sp,
                  color = SlateMuted
                )
              }

              items(UserRole.values()) { role ->
                val perms = UserRolePermissions.defaultFor(role)
                val roleColor = getRoleColor(role)
                val roleBg = getRoleBg(role)
                val staffCount = users.count { it.role == role }

                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = CardDefaults.cardColors(containerColor = PureWhite),
                  border = BorderStroke(1.dp, BorderGrey)
                ) {
                  Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(26.dp)
                          .clip(RoundedCornerShape(4.dp))
                          .background(roleBg),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(getRoleIcon(role), contentDescription = null, tint = roleColor, modifier = Modifier.size(15.dp))
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          "${role.displayName} (${role.myanmarName})",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = DarkGrayText
                        )
                        Text(role.description, fontSize = 8.5.sp, color = SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                      Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = roleBg
                      ) {
                        Text(
                          "$staffCount ဦး",
                          fontSize = 8.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = roleColor,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = BorderGrey, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Permission Chips
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      PermBadge("Queue", perms.queueAccess)
                      PermBadge("EMR", perms.doctorEmrAccess)
                      PermBadge("Treatment", perms.treatmentAccess)
                      PermBadge("Pharmacy", perms.pharmacyAccess)
                      PermBadge("Billing", perms.billingAccess)
                      PermBadge("Procedures", perms.proceduresAccess)
                      PermBadge("Inventory", perms.inventoryAccess)
                      PermBadge("Ledger", perms.financialLedgerAccess)
                      PermBadge("Records", perms.medicalRecordsAccess)
                      PermBadge("Users", perms.userManagementAccess)
                    }
                  }
                }
              }
            }
          }

          3 -> {
            // ==========================================
            // TAB 3: ACTIVE SESSION & QUICK SWITCHER
            // ==========================================
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
            ) {
              Text("လက်ရှိအသုံးပြုနေသော အကောင့် (Active Station User):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
              Spacer(modifier = Modifier.height(6.dp))

              currentUser?.let { active ->
                val roleColor = getRoleColor(active.role)
                val roleBg = getRoleBg(active.role)

                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = CardDefaults.cardColors(containerColor = SoftGreenContainer.copy(alpha = 0.4f)),
                  border = BorderStroke(1.5.dp, MedicalGreen)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(roleBg),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(getRoleIcon(active.role), contentDescription = null, tint = roleColor, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(active.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                          shape = RoundedCornerShape(3.dp),
                          color = MedicalGreen,
                          contentColor = PureWhite
                        ) {
                          Text("Logged In", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                      }
                      Text(
                        "@${active.username} • ${active.role.displayName} (${active.role.myanmarName})",
                        fontSize = 10.sp,
                        color = roleColor,
                        fontWeight = FontWeight.SemiBold
                      )
                      if (active.samaNumber != null) {
                        Text("ဆမ အမှတ်: ${active.samaNumber}", fontSize = 9.sp, color = MedicalGreenDark)
                      }
                      Text("ဌာန: ${active.department} • နောက်ဆုံးဝင်ရောက်ချိန်: ${active.lastLoginAt}", fontSize = 8.5.sp, color = SlateMuted)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text("အခြားအကောင့်သို့ ချက်ချင်းပြောင်းသုံးရန် (Quick Switch Station):", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = SlateDark)
              Spacer(modifier = Modifier.height(4.dp))

              LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                items(users.filter { it.id != currentUser?.id }) { user ->
                  val roleColor = getRoleColor(user.role)
                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(4.dp))
                      .clickable {
                        viewModel.switchActiveUser(user)
                        feedbackMessage = "@${user.username} (${user.role.displayName}) သို့ အကောင့်ပြောင်းသုံးပြီးပါပြီ"
                        isError = false
                      },
                    shape = RoundedCornerShape(4.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, BorderGrey)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(24.dp)
                          .clip(CircleShape)
                          .background(getRoleBg(user.role)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(getRoleIcon(user.role), contentDescription = null, tint = roleColor, modifier = Modifier.size(14.dp))
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGrayText)
                        Text("@${user.username} • ${user.role.displayName}", fontSize = 8.5.sp, color = roleColor)
                      }
                      Button(
                        onClick = {
                          viewModel.switchActiveUser(user)
                          feedbackMessage = "@${user.username} သို့ ပြောင်းသုံးပြီးပါပြီ"
                          isError = false
                        },
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGreenContainer),
                        border = BorderStroke(0.8.dp, MedicalGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                      ) {
                        Text("ပြောင်းသုံးမည်", fontSize = 9.sp, color = MedicalGreenDark, fontWeight = FontWeight.Bold)
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
  }

  // --- PASSWORD RESET MODAL ---
  resetPasswordUser?.let { userToReset ->
    Dialog(onDismissRequest = { resetPasswordUser = null }) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("စကားဝှက် အသစ်သတ်မှတ်ရန်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkGrayText)
          Text("@${userToReset.username} (${userToReset.fullName})", fontSize = 10.sp, color = SlateMuted)
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = resetNewPasswordText,
            onValueChange = { resetNewPasswordText = it },
            label = { Text("စကားဝှက် အသစ် ရိုက်ထည့်ပါ", fontSize = 9.5.sp) },
            placeholder = { Text("e.g. 123456", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(4.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { resetPasswordUser = null }) {
              Text("မလုပ်တော့ပါ", fontSize = 10.sp, color = SlateMedium)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = {
                if (resetNewPasswordText.isNotBlank()) {
                  viewModel.resetUserPassword(userToReset.id, resetNewPasswordText.trim())
                  feedbackMessage = "@${userToReset.username} ၏ စကားဝှက်ကို အသစ်ပြောင်းပြီးပါပြီ"
                  isError = false
                  resetPasswordUser = null
                }
              },
              shape = RoundedCornerShape(4.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MedicalGreen)
            ) {
              Text("စကားဝှက်သိမ်းမည်", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  if (showExportPairingDialog) {
    ExportDevicePairingDialog(
      viewModel = viewModel,
      onDismiss = { showExportPairingDialog = false }
    )
  }
}

@Composable
private fun TabAccessSelectorRow(
  title: String,
  myanmarTitle: String,
  icon: ImageVector,
  currentLevel: TabAccessLevel,
  onLevelSelected: (TabAccessLevel) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MedicalGreenDark)
      Spacer(modifier = Modifier.width(6.dp))
      Column {
        Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText, maxLines = 1)
        Text(myanmarTitle, fontSize = 8.sp, color = SlateMuted, maxLines = 1)
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      TabAccessOptionChip(
        label = "Seal / မမြင်ရ",
        selected = currentLevel == TabAccessLevel.SEALED,
        selectedBg = StatusCriticalContainer,
        selectedColor = StatusCritical,
        onClick = { onLevelSelected(TabAccessLevel.SEALED) }
      )
      TabAccessOptionChip(
        label = "View Only / ကြည့်ရုံ",
        selected = currentLevel == TabAccessLevel.VIEW_ONLY,
        selectedBg = Color(0xFFFEF3C7),
        selectedColor = Color(0xFFB45309),
        onClick = { onLevelSelected(TabAccessLevel.VIEW_ONLY) }
      )
      TabAccessOptionChip(
        label = "Allow to Edit / ပြင်ဆင်",
        selected = currentLevel == TabAccessLevel.ALLOW_EDIT,
        selectedBg = StatusSuccessContainer,
        selectedColor = StatusSuccess,
        onClick = { onLevelSelected(TabAccessLevel.ALLOW_EDIT) }
      )
    }
  }
}

@Composable
private fun TabAccessOptionChip(
  label: String,
  selected: Boolean,
  selectedBg: Color,
  selectedColor: Color,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(3.dp),
    color = if (selected) selectedBg else Color(0xFFF3F4F6),
    border = BorderStroke(0.8.dp, if (selected) selectedColor else Color(0xFFD1D5DB)),
    modifier = Modifier
      .clip(RoundedCornerShape(3.dp))
      .clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      fontSize = 8.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      color = if (selected) selectedColor else Color(0xFF4B5563),
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
    )
  }
}

@Composable
private fun PermBadge(label: String, access: TabAccessLevel) {
  val (bg, border, tint, icon) = when (access) {
    TabAccessLevel.SEALED -> AccessBadgeStyle(StatusCriticalContainer, StatusCritical, StatusCritical, Icons.Default.Lock)
    TabAccessLevel.VIEW_ONLY -> AccessBadgeStyle(Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFFB45309), Icons.Default.Visibility)
    TabAccessLevel.ALLOW_EDIT -> AccessBadgeStyle(StatusSuccessContainer, StatusSuccess, StatusSuccess, Icons.Default.Edit)
  }
  Surface(
    shape = RoundedCornerShape(2.dp),
    color = bg,
    border = BorderStroke(0.6.dp, border)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(8.dp)
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        "$label: ${access.name.take(4)}",
        fontSize = 7.sp,
        fontWeight = if (access == TabAccessLevel.ALLOW_EDIT) FontWeight.Bold else FontWeight.Normal,
        color = tint
      )
    }
  }
}

private data class AccessBadgeStyle(val bg: Color, val border: Color, val tint: Color, val icon: ImageVector)
