package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.QueueStatus
import com.example.model.UserRole
import com.example.model.UserRolePermissions
import com.example.ui.components.A4ClinicalDocDialog
import com.example.ui.components.AdminUserManagementDialog
import com.example.ui.components.ClinicBackupResetDialog
import com.example.ui.components.ClinicSettingsDialog
import com.example.ui.components.PosThermalSlipDialog
import com.example.ui.components.ShweKhitLogo
import com.example.ui.components.SyncStatusDialog
import com.example.ui.screens.*
import com.example.ui.screens.treatment.TreatmentScreen
import com.example.ui.screens.treatment.TreatmentStatus
import com.example.ui.theme.*
import com.example.viewmodel.ClinicViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MiniPolyclinicMainApp()
      }
    }
  }
}

data class NavDrawerItem(
  val label: String,
  val myanmarLabel: String,
  val icon: ImageVector,
  val index: Int,
  val category: String,
  val isPermitted: (UserRolePermissions) -> Boolean
)

val navDrawerItems = listOf(
  // 1. Clinical OPD Workflow (Dashboard -> Patient Queue -> Doctor Consultation -> Treatment & Injections -> Pharmacy Dispense -> Cashier & Invoices)
  NavDrawerItem("Dashboard", "ပင်မ ဒက်ရှ်ဘုတ်", Icons.Default.Dashboard, 0, "CLINICAL OPD WORKFLOW", { true }),
  NavDrawerItem("Patient Queue", "လူနာမှတ်ပုံတင် & စာရင်း", Icons.Default.People, 1, "CLINICAL OPD WORKFLOW", { it.canAccessPatientQueue }),
  NavDrawerItem("Doctor Consultation", "ဆရာဝန် ဆေးမှတ်တမ်း & Rx", Icons.Default.MedicalServices, 2, "CLINICAL OPD WORKFLOW", { it.canAccessDoctorEmr }),
  NavDrawerItem("Treatment & Injections", "ဆေးထိုး/အကြောပုလင်း ကုသမှု", Icons.Default.Healing, 9, "CLINICAL OPD WORKFLOW", { it.canAccessProcedures || it.canAccessDoctorEmr }),
  NavDrawerItem("Pharmacy Dispense", "ဆေးထုတ်ပေးခြင်း (FEFO)", Icons.Default.Medication, 3, "CLINICAL OPD WORKFLOW", { it.canAccessPharmacy }),
  NavDrawerItem("Cashier & Invoices", "ငွေကောက်ခံ & ပြေစာထုတ်ပေးခြင်း", Icons.Default.PointOfSale, 4, "CLINICAL OPD WORKFLOW", { it.canAccessBilling }),

  // 2. Records, Procedures & Inventory Management
  NavDrawerItem("Medical Records", "လူနာ ဆေးမှတ်တမ်းများ (Archive)", Icons.Default.FolderShared, 8, "RECORDS & OPERATIONS", { it.canAccessDoctorEmr || it.canAccessPatientQueue }),
  NavDrawerItem("Clinical Procedures", "ဒဏ်ရာဆေးကြော & ကုသမှုများ", Icons.Default.Vaccines, 5, "RECORDS & OPERATIONS", { it.canAccessProcedures }),
  NavDrawerItem("Income & Expenses", "ဝင်ငွေ ထွက်ငွေ ဇယား (Ledger)", Icons.Default.AccountBalanceWallet, 7, "RECORDS & OPERATIONS", { it.canAccessFinancialLedger }),
  NavDrawerItem("Inventory Master", "စတော့အချုပ် & တန်ဖိုးသတ်မှတ်ချက်", Icons.Default.Inventory2, 6, "RECORDS & OPERATIONS", { it.canAccessInventoryMaster })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPolyclinicMainApp(
  clinicViewModel: ClinicViewModel = viewModel()
) {
  val activeTab by clinicViewModel.activeTab.collectAsState()
  val patients by clinicViewModel.patients.collectAsState()
  val medicines by clinicViewModel.medicines.collectAsState()
  val currentUser by clinicViewModel.currentUser.collectAsState()
  val isLoggedIn by clinicViewModel.isLoggedIn.collectAsState()
  val syncLogs by clinicViewModel.syncLogs.collectAsState()
  val isSyncing by clinicViewModel.isSyncing.collectAsState()
  val previewThermalSlip by clinicViewModel.previewThermalSlip.collectAsState()
  val previewA4Doc by clinicViewModel.previewA4ClinicalDoc.collectAsState()
  val clinicSettings by clinicViewModel.clinicSettings.collectAsState()

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var showSyncDialog by remember { mutableStateOf(false) }
  var syncDialogInitialTab by remember { mutableStateOf(0) }
  var showUserManagementDialog by remember { mutableStateOf(false) }
  var showChangePasswordDialog by remember { mutableStateOf(false) }
  var showClinicSettingsDialog by remember { mutableStateOf(false) }
  var showBackupResetDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    try {
      com.example.service.sync.LocalWifiSyncService.startHub()
      com.example.service.sync.LocalWifiSyncService.startDiscovery(context)
    } catch (_: Exception) {}
  }

  if (!isLoggedIn) {
    LoginScreen(viewModel = clinicViewModel)
    return
  }

  val waitingCount = patients.count { it.status == QueueStatus.WAITING }
  val pharmacyCount = patients.count { it.status == QueueStatus.AT_PHARMACY }
  val lowStockCount = medicines.count { it.totalStock <= it.minStockThreshold }
  val treatmentCards by clinicViewModel.treatmentCards.collectAsState()
  val dueInjectionsCount = treatmentCards.filter { !it.isDiscontinued && !it.isFullyCompleted }
    .flatMap { it.sessions }
    .count { it.status == TreatmentStatus.PENDING }

  val userPermissions = currentUser?.effectivePermissions ?: UserRolePermissions.defaultFor(UserRole.DOCTOR)

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        modifier = Modifier
          .width(310.dp)
          .fillMaxHeight(),
        drawerContainerColor = PureWhite,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
          // 1. Clinic Branding Header
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(MedicalGreenDark)
              .padding(horizontal = 16.dp, vertical = 20.dp)
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                ShweKhitLogo(
                  size = 40.dp,
                  customLogoUri = clinicSettings.effectiveLogo.ifBlank { null }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    "\"${clinicSettings.clinicNameMm.ifBlank { "ရွှေခေတ်" }}\"",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PureWhite
                  )
                  Text(
                    clinicSettings.clinicSubtitleMm.ifBlank { "အထွေထွေရောဂါကုဆေးခန်း" },
                    fontSize = 11.sp,
                    color = PureWhite.copy(alpha = 0.9f)
                  )
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              Divider(color = PureWhite.copy(alpha = 0.25f), thickness = 0.8.dp)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "${clinicSettings.doctorName.ifBlank { "Dr. Nyan Kaung Sett" }} ${clinicSettings.doctorDegree.ifBlank { "M.B.,B.S(MDY)" }}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
              )
              Text(
                "ဆမ-${clinicSettings.doctorSama.ifBlank { "၄၇၄၁၄" }} • ${clinicSettings.clinicNameEn.ifBlank { "General OPD Clinic" }}",
                fontSize = 9.sp,
                color = PureWhite.copy(alpha = 0.8f)
              )
            }
          }

          // 2. Current User Account & Role Card
          currentUser?.let { user ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              shape = RoundedCornerShape(8.dp),
              color = SoftGreenContainer,
              border = BorderStroke(1.dp, MedicalGreen.copy(alpha = 0.3f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(MedicalGreen),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      when (user.role) {
                        UserRole.ADMIN -> Icons.Default.Security
                        UserRole.DOCTOR -> Icons.Default.MedicalServices
                        UserRole.PHARMACIST -> Icons.Default.Medication
                        UserRole.CASHIER -> Icons.Default.PointOfSale
                        UserRole.NURSE -> Icons.Default.LocalHospital
                        UserRole.RECEPTIONIST -> Icons.Default.Badge
                      },
                      contentDescription = null,
                      tint = PureWhite,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      user.fullName,
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = DarkGrayText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      "${user.role.displayName} (${user.role.myanmarName})",
                      fontSize = 9.sp,
                      color = MedicalGreenDark,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }

                IconButton(
                  onClick = {
                    coroutineScope.launch { drawerState.close() }
                    if (currentUser?.role == UserRole.ADMIN) {
                      showUserManagementDialog = true
                    } else {
                      showChangePasswordDialog = true
                    }
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.Default.ManageAccounts,
                    contentDescription = "Manage / Switch Accounts",
                    tint = MedicalGreenDark,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // 3. Navigation Items Grouped by Categories (RBAC Protected)
          val groupedItems = navDrawerItems.groupBy { it.category }
          groupedItems.forEach { (category, items) ->
            val permittedItems = items.filter { it.isPermitted(userPermissions) }
            if (permittedItems.isNotEmpty()) {
              Text(
                category,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SlateMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
              )

              permittedItems.forEach { item ->
                val isSelected = activeTab == item.index
                val badgeCount = when (item.index) {
                  1 -> waitingCount
                  3 -> pharmacyCount
                  6 -> lowStockCount
                  9 -> dueInjectionsCount
                  else -> 0
                }

                NavigationDrawerItem(
                  label = {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(
                          item.label,
                          fontSize = 11.sp,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                          item.myanmarLabel,
                          fontSize = 8.5.sp,
                          color = if (isSelected) MedicalGreenDark else SlateMuted
                        )
                      }
                      if (badgeCount > 0) {
                        Surface(
                          shape = RoundedCornerShape(10.dp),
                          color = when (item.index) {
                            6 -> if (badgeCount > 0) StatusCritical else StatusSuccess
                            else -> MedicalGreen
                          }
                        ) {
                          Text(
                            "$badgeCount",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                  },
                  icon = {
                    Icon(
                      item.icon,
                      contentDescription = item.label,
                      modifier = Modifier.size(20.dp)
                    )
                  },
                  selected = isSelected,
                  onClick = {
                    clinicViewModel.setTab(item.index)
                    coroutineScope.launch { drawerState.close() }
                  },
                  shape = RoundedCornerShape(8.dp),
                  colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = SoftGreenContainer,
                    selectedIconColor = MedicalGreenDark,
                    selectedTextColor = MedicalGreenDark,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = SlateMedium,
                    unselectedTextColor = DarkGrayText
                  ),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
              }

              Spacer(modifier = Modifier.height(4.dp))
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = BorderGrey, thickness = 0.8.dp)

          // 4. Grouped Clinic Settings
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            // Group 1: Clinic Branding & Database
            Text(
              "CLINIC & DATABASE (ဆေးခန်း & ဒေတာ)",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = SlateMuted,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
            )

            DrawerSettingsRow(
              icon = Icons.Default.Store,
              iconTint = MedicalGreenDark,
              title = "Clinic Branding & Setup",
              subTitle = "ဆေးခန်း အချက်အလက်နှင့် တံဆိပ်",
              onClick = {
                coroutineScope.launch { drawerState.close() }
                showClinicSettingsDialog = true
              }
            )

            DrawerSettingsRow(
              icon = Icons.Default.CloudSync,
              iconTint = SkyBlueDark,
              title = "Database Backup, Restore & Reset",
              subTitle = "ဒေတာဘေ့စ် သိမ်းဆည်း & ပြန်တင်ခြင်း",
              onClick = {
                coroutineScope.launch { drawerState.close() }
                showBackupResetDialog = true
              }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Group 2: User Role Setting
            Text(
              "USER ROLE SETTING (အကောင့် & လုပ်ပိုင်ခွင့်)",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = SlateMuted,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
            )

            if (currentUser?.role == UserRole.ADMIN) {
              DrawerSettingsRow(
                icon = Icons.Default.AdminPanelSettings,
                iconTint = AccentRedBrown,
                title = "Staff & Role Permissions",
                subTitle = "ဝန်ထမ်း စာရင်းနှင့် လုပ်ပိုင်ခွင့် (Admin Only)",
                onClick = {
                  coroutineScope.launch { drawerState.close() }
                  showUserManagementDialog = true
                }
              )
            }

            DrawerSettingsRow(
              icon = Icons.Default.Password,
              iconTint = MedicalGreen,
              title = "Change Password",
              subTitle = "စကားဝှက် ပြောင်းလဲမည်",
              onClick = {
                coroutineScope.launch { drawerState.close() }
                showChangePasswordDialog = true
              }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Group 3: Printer Setting
            Text(
              "PRINTER SETTING (ပရင်တာ ဆက်တင်များ)",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = SlateMuted,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
            )

            DrawerSettingsRow(
              icon = Icons.Default.Description,
              iconTint = MedicalGreenDark,
              title = "A4 Printer Setup",
              subTitle = "A4 ဆေးမှတ်တမ်း & စာရွက် ဆက်တင်",
              onClick = {
                coroutineScope.launch { drawerState.close() }
                context.startActivity(android.content.Intent(context, com.example.ui.screens.A4PrinterSettingsActivity::class.java))
              }
            )

            DrawerSettingsRow(
              icon = Icons.Default.Print,
              iconTint = MedicalGreen,
              title = "Thermal Printer Setup",
              subTitle = "XP-T361 (80mm Bluetooth / USB)",
              onClick = {
                coroutineScope.launch { drawerState.close() }
                context.startActivity(android.content.Intent(context, com.example.ui.screens.PrinterSettingsActivity::class.java))
              }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Group 4: Network Setting
            Text(
              "NETWORK SETTING (ကွန်ရက် & ဒေတာ စင့်ခ်)",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = SlateMuted,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
            )

            DrawerSettingsRow(
              icon = Icons.Default.Wifi,
              iconTint = OceanGreenPrimary,
              title = "Local Sync (Wi-Fi Peer)",
              subTitle = "ကြိုးမဲ့ Local Wi-Fi တိုက်ရိုက်ချိတ်ဆက်မှု",
              onClick = {
                syncDialogInitialTab = 0
                coroutineScope.launch { drawerState.close() }
                showSyncDialog = true
              }
            )

            DrawerSettingsRow(
              icon = Icons.Default.CloudDone,
              iconTint = SkyBlueDark,
              title = "Cloud Sync (Supabase)",
              subTitle = "အွန်လိုင်း Cloud Database စင့်ခ်လုပ်ရန်",
              onClick = {
                syncDialogInitialTab = 1
                coroutineScope.launch { drawerState.close() }
                showSyncDialog = true
              }
            )

            DrawerSettingsRow(
              icon = Icons.Default.Sync,
              iconTint = if (isSyncing) StatusWarning else StatusSuccess,
              title = "Sync Logs (ချိတ်ဆက်မှု မှတ်တမ်းများ)",
              subTitle = if (isSyncing) "Syncing in progress..." else "Offline SQLite DB (Healthy)",
              onClick = {
                syncDialogInitialTab = 2
                coroutineScope.launch { drawerState.close() }
                showSyncDialog = true
              }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Logout Button
            OutlinedButton(
              onClick = {
                coroutineScope.launch { drawerState.close() }
                clinicViewModel.logout()
              },
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, MahoganyAccent.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(34.dp),
              contentPadding = PaddingValues(0.dp)
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MahoganyAccent, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Lock Station / Logout (အကောင့်ထွက်မည်)", fontSize = 10.sp, color = MahoganyAccent, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  ) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .imePadding(),
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(
              onClick = { coroutineScope.launch { drawerState.open() } }
            ) {
              Icon(
                Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = MedicalGreenDark,
                modifier = Modifier.size(24.dp)
              )
            }
          },
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ShweKhitLogo(
                size = 32.dp,
                customLogoUri = clinicSettings.effectiveLogo.ifBlank { null }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  when (activeTab) {
                    0 -> "ရွှေခေတ် - Dashboard"
                    1 -> "ရွှေခေတ် - Patient Queue"
                    2 -> "ရွှေခေတ် - Doctor Consultation EMR"
                    3 -> "ရွှေခေတ် - Pharmacy Dispensing"
                    4 -> "ရွှေခေတ် - Cashier & Billing"
                    5 -> "ရွှေခေတ် - Clinical Procedures"
                    6 -> "ရွှေခေတ် - Inventory Master (စတော့အချုပ်)"
                    7 -> "ရွှေခေတ် - ဝင်ငွေ ထွက်ငွေ ဇယား (Ledger)"
                    8 -> "ရွှေခေတ် - လူနာ ဆေးမှတ်တမ်းများ (EMR Archive)"
                    else -> "\"ရွှေခေတ်\" အထွေထွေရောဂါကုဆေးခန်း"
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp,
                  color = MedicalGreenDark,
                  maxLines = 1
                )
                Text(
                  "${clinicSettings.doctorName.ifBlank { "Dr. Nyan Kaung Sett" }} ${clinicSettings.doctorDegree.ifBlank { "M.B.,B.S(MDY)" }} • ဆမ-${clinicSettings.doctorSama.ifBlank { "၄၇၄၁၄" }}",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Medium,
                  color = SlateMedium,
                  maxLines = 1
                )
              }
            }
          },
          actions = {
            // Minimal, clean Offline SQLite DB Status Pill (Account Manager is safely inside Navigation Drawer)
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSyncing) StatusWarningContainer else StatusSuccessContainer)
                .clickable { showSyncDialog = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (isSyncing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(11.dp),
                  color = StatusWarning,
                  strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "Syncing...",
                  color = StatusWarning,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              } else {
                Icon(
                  Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = StatusSuccess,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  "SQLite DB",
                  color = StatusSuccess,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
          .background(BackgroundSurface)
      ) {
        when (activeTab) {
          0 -> DashboardScreen(
            viewModel = clinicViewModel,
            onNavigateToTab = { clinicViewModel.setTab(it) },
            onOpenUserAccounts = { showUserManagementDialog = true }
          )
          1 -> PatientQueueScreen(
            viewModel = clinicViewModel,
            onNavigateToConsultation = { patientId ->
              clinicViewModel.selectPatient(patientId)
              clinicViewModel.setTab(2)
            }
          )
          2 -> DoctorConsultationScreen(
            viewModel = clinicViewModel,
            onConsultationCompleted = {
              clinicViewModel.setTab(3) // Route directly to pharmacy
            }
          )
          3 -> PharmacyInventoryScreen(
            viewModel = clinicViewModel
          )
          4 -> CashierBillingScreen(
            viewModel = clinicViewModel
          )
          5 -> ProceduresScreen(
            viewModel = clinicViewModel
          )
          6 -> InventoryMasterScreen(
            viewModel = clinicViewModel,
            onNavigateToTab = { clinicViewModel.setTab(it) },
            onOpenUserAccounts = { showUserManagementDialog = true }
          )
          7 -> ClinicLedgerScreen(
            viewModel = clinicViewModel
          )
          8 -> MedicalRecordsScreen(
            viewModel = clinicViewModel,
            onNavigateToQueue = { clinicViewModel.setTab(1) }
          )
          9 -> TreatmentScreen(
            viewModel = clinicViewModel
          )
        }
      }
    }
  }

  // Dual-Printing Modals & Offline Status Dialog
  previewThermalSlip?.let { invoice ->
    val patient = patients.find { it.id == invoice.patientId }
    PosThermalSlipDialog(
      invoice = invoice,
      patientHn = patient?.hn ?: "HN-2026-0081",
      cashierName = currentUser?.fullName ?: "Cashier",
      clinicSettings = clinicSettings,
      onDismiss = { clinicViewModel.closeThermalPreview() },
      onPrintSuccess = { clinicViewModel.closeThermalPreview() }
    )
  }

  LaunchedEffect(previewA4Doc) {
    val consultation = previewA4Doc
    if (consultation != null) {
      val patient = patients.find { it.id == consultation.patientId }
      val store = com.example.service.printing.A4PrinterSettingsStore(context)
      val a4Settings = com.example.ui.components.A4PrintSettings.fromClinicSettings(clinicSettings)

      val descriptors = mutableListOf<com.example.ui.components.A4PageDescriptor>()
      val emrPages = com.example.service.printing.A4PdfNativeCanvasRenderer.computeEmrPages(consultation, patient, a4Settings)
      if (emrPages == 1) {
        descriptors.add(com.example.ui.components.A4PageDescriptor.UnifiedSinglePage())
      } else {
        descriptors.add(com.example.ui.components.A4PageDescriptor.ClinicalConsultationPage(0, true, "Clinical Notes & Examination"))
        descriptors.add(com.example.ui.components.A4PageDescriptor.OfficialPrescriptionPage("Prescriptions & Doctor's Orders"))
      }
      if (consultation.medicalLeave != null) {
        descriptors.add(com.example.ui.components.A4PageDescriptor.MedicalLeaveCertificatePage("Medical Leave Certificate"))
      }
      if (consultation.referralRecord != null) {
        descriptors.add(com.example.ui.components.A4PageDescriptor.ReferralLetterPage("Official Clinical Referral Letter"))
      }

      com.example.ui.components.triggerRealAndroidPrint(
        context = context,
        consultation = consultation,
        patient = patient,
        printSettings = a4Settings,
        pageDescriptors = descriptors,
        marginMm = store.getMarginMm(),
        isGrayscale = store.isGrayscale()
      )
      clinicViewModel.closeA4DocPreview()
    }
  }

  if (showClinicSettingsDialog) {
    ClinicSettingsDialog(
      currentSettings = clinicSettings,
      onDismiss = { showClinicSettingsDialog = false },
      onSave = { updated ->
        clinicViewModel.updateClinicSettings(updated)
      }
    )
  }

  if (showBackupResetDialog) {
    ClinicBackupResetDialog(
      onDismiss = { showBackupResetDialog = false },
      onExportBackup = { clinicViewModel.exportFullBackupJson() },
      onRestoreBackup = { json -> clinicViewModel.restoreFullBackupJson(json) },
      onFactoryReset = { clearMedicines -> clinicViewModel.clearAllDataAndStartFresh(clearMedicines = clearMedicines) }
    )
  }

  if (showSyncDialog) {
    SyncStatusDialog(
      viewModel = clinicViewModel,
      initialTab = syncDialogInitialTab,
      onDismiss = { showSyncDialog = false }
    )
  }

  if (showUserManagementDialog) {
    AdminUserManagementDialog(
      viewModel = clinicViewModel,
      onDismiss = { showUserManagementDialog = false }
    )
  }

  if (showChangePasswordDialog) {
    com.example.ui.components.ChangePasswordDialog(
      currentUser = currentUser,
      onDismiss = { showChangePasswordDialog = false },
      onChangePassword = { oldPass, newPass, callback ->
        val res = clinicViewModel.changePassword(currentUser?.id ?: "", oldPass, newPass)
        callback(res.first, res.second)
      }
    )
  }
}

@Composable
private fun DrawerSettingsRow(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  subTitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(BackgroundSurface)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DarkGrayText, maxLines = 1)
        Text(subTitle, fontSize = 8.5.sp, color = SlateMedium, maxLines = 1)
      }
    }
    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SlateLight, modifier = Modifier.size(14.dp))
  }
}
