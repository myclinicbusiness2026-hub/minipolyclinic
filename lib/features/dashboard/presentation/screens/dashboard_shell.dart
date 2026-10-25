import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_theme.dart';
import '../../../auth/presentation/providers/auth_provider.dart';
import '../providers/dashboard_provider.dart';

/// Tablet & Mobile Navigation Shell with Role-Based Access Control (RBAC)
///
/// Compact Clinical Layout:
/// - NavigationRail on the left for wide/tablet displays (>= 720dp)
/// - BottomNavigationBar for compact phone displays (< 720dp)
/// - Top App Header:
///   * Clinic Title ("ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း)
///   * Current User Profile with Role & SAMA Reg No
///   * Logout / Switch Account action button
///   * Local DB Offline-First status indicator badge
/// - Dynamic Navigation Items filtered by Active User Role (RBAC):
///   * Admin: Queue, Consult, Pharmacy, Billing, Patients, Admin
///   * Doctor: Queue, Consult, Pharmacy, Billing, Patients
///   * Pharmacist: Queue, Pharmacy, Patients
///   * Cashier: Queue, Billing, Patients
class DashboardShell extends ConsumerWidget {
  final StatefulNavigationShell navigationShell;

  const DashboardShell({
    Key? key,
    required this.navigationShell,
  }) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dashboardState = ref.watch(dashboardProvider);
    final authState = ref.watch(authProvider);
    final waitingCount = dashboardState.waitingCount;
    final billingCount = dashboardState.pendingBillingCount;

    return LayoutBuilder(
      builder: (context, constraints) {
        final isWideScreen = constraints.maxWidth >= 720;

        return Scaffold(
          backgroundColor: AppTheme.bgLightGray,
          body: Column(
            children: [
              // ===============================================================
              // TOP APP HEADER
              // ===============================================================
              _buildTopAppHeader(context, ref, dashboardState, authState),

              // ===============================================================
              // MAIN CONTENT AREA: NavigationRail + Shell Branch OR BottomBar
              // ===============================================================
              Expanded(
                child: Row(
                  children: [
                    if (isWideScreen)
                      _buildNavigationRail(
                        context,
                        ref,
                        navigationShell.currentIndex,
                        waitingCount,
                        billingCount,
                        authState,
                      ),
                    if (isWideScreen)
                      VerticalDivider(thickness: 1, width: 1, color: AppTheme.subtleBorder),
                    Expanded(
                      child: navigationShell,
                    ),
                  ],
                ),
              ),
            ],
          ),
          bottomNavigationBar: isWideScreen
              ? null
              : _buildBottomNavigationBar(
                  context,
                  ref,
                  navigationShell.currentIndex,
                  waitingCount,
                  billingCount,
                  authState,
                ),
        );
      },
    );
  }

  /// Top App Header with Clinic Title, User Role & SQLite Offline-First Indicator
  Widget _buildTopAppHeader(
    BuildContext context,
    WidgetRef ref,
    DashboardState state,
    AuthState authState,
  ) {
    final user = authState.currentUser;
    final userInitials = (user?.fullName.isNotEmpty == true)
        ? user!.fullName.split(' ').map((s) => s.isNotEmpty ? s[0] : '').take(2).join()
        : 'U';

    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: AppTheme.cardPureWhite,
        border: Border(bottom: BorderSide(color: AppTheme.subtleBorder, width: 1)),
      ),
      child: Row(
        children: [
          // Clinic Icon & Brand Name
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: AppTheme.softGreenContainer,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.3)),
            ),
            child: const Icon(
              Icons.local_hospital_rounded,
              color: AppTheme.primaryGreen,
              size: 20,
            ),
          ),
          const SizedBox(width: 10),
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              Row(
                children: [
                  Text(
                    AppTheme.clinicTitleMyanmar,
                    style: TextStyle(
                      color: AppTheme.slateDark,
                      fontWeight: FontWeight.w800,
                      fontSize: 15,
                      letterSpacing: -0.2,
                    ),
                  ),
                  SizedBox(width: 6),
                  Text(
                    'Shwe Khit Polyclinic',
                    style: TextStyle(
                      color: AppTheme.primaryGreen,
                      fontWeight: FontWeight.w600,
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
              Text(
                'အထွေထွေရောဂါကု ဆေးခန်းနှင့် ဆေးဝါးသိုလှောင်ရုံ',
                style: TextStyle(color: AppTheme.slateMuted, fontSize: 10),
              ),
            ],
          ),

          const Spacer(),

          // Local DB Offline-First Synced Badge
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFFECFDF5),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.statusSuccess.withOpacity(0.3)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 7,
                  height: 7,
                  decoration: const BoxDecoration(
                    color: AppTheme.statusSuccess,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 5),
                const Text(
                  'SQLite Offline-First',
                  style: TextStyle(
                    color: Color(0xFF065F46),
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(width: 10),

          // User Profile & Role Card (Dynamic based on active auth)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: AppTheme.softGreenContainer,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.25)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                CircleAvatar(
                  radius: 12,
                  backgroundColor: AppTheme.primaryGreen,
                  child: Text(
                    userInitials,
                    style: const TextStyle(
                      color: AppTheme.cardPureWhite,
                      fontSize: 9,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user?.fullName ?? 'Dr. Nyan Kaung Sett',
                      style: const TextStyle(
                        color: AppTheme.slateDark,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Row(
                      children: [
                        Text(
                          user?.role ?? 'DOCTOR',
                          style: const TextStyle(
                            color: AppTheme.primaryDarkGreen,
                            fontSize: 9,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        if (user?.samaNumber != null && user!.samaNumber!.isNotEmpty) ...[
                          const SizedBox(width: 4),
                          Text(
                            '(စမ-${user.samaNumber})',
                            style: const TextStyle(color: AppTheme.slateMuted, fontSize: 8.5),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Admin & User Management Shortcut (if Admin)
          if (authState.isAdmin) ...[
            Container(
              height: 28,
              decoration: BoxDecoration(
                color: AppTheme.softGreenContainer,
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.4)),
              ),
              child: TextButton.icon(
                onPressed: () => context.go('/admin'),
                icon: const Icon(Icons.admin_panel_settings_rounded, size: 15, color: AppTheme.primaryGreen),
                label: const Text(
                  'User Accounts',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.primaryDarkGreen,
                  ),
                ),
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 0),
                  minimumSize: Size.zero,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              ),
            ),
            const SizedBox(width: 6),
          ],

          // Quick Refresh Action
          IconButton(
            tooltip: 'Refresh Queue & Stats',
            icon: const Icon(Icons.refresh, color: AppTheme.slateMedium, size: 18),
            onPressed: () => ref.read(dashboardProvider.notifier).refreshData(),
          ),

          // Logout Action Button
          IconButton(
            tooltip: 'အကောင့်ထွက်မည် (Sign Out)',
            icon: const Icon(Icons.logout_rounded, color: AppTheme.accentRedBrown, size: 18),
            onPressed: () => _confirmLogout(context, ref),
          ),
        ],
      ),
    );
  }

  /// Navigation Rail for Tablet / Desktop screens
  Widget _buildNavigationRail(
    BuildContext context,
    WidgetRef ref,
    int selectedIndex,
    int waitingCount,
    int billingCount,
    AuthState authState,
  ) {
    return NavigationRail(
      selectedIndex: selectedIndex,
      onDestinationSelected: (index) => _onTabSelected(context, index, authState),
      backgroundColor: AppTheme.cardPureWhite,
      minWidth: 78,
      groupAlignment: -0.85,
      indicatorColor: AppTheme.softGreenContainer,
      selectedIconTheme: const IconThemeData(color: AppTheme.primaryGreen, size: 22),
      unselectedIconTheme: const IconThemeData(color: AppTheme.slateMuted, size: 20),
      selectedLabelTextStyle: const TextStyle(
        color: AppTheme.primaryGreen,
        fontWeight: FontWeight.bold,
        fontSize: 10.5,
      ),
      unselectedLabelTextStyle: const TextStyle(
        color: AppTheme.slateMuted,
        fontWeight: FontWeight.w500,
        fontSize: 10.5,
      ),
      labelType: NavigationRailLabelType.all,
      destinations: [
        // 0: Dashboard Queue
        NavigationRailDestination(
          icon: Badge(
            isLabelVisible: waitingCount > 0,
            label: Text('$waitingCount'),
            backgroundColor: AppTheme.statusWarning,
            child: const Icon(Icons.dashboard_outlined),
          ),
          selectedIcon: Badge(
            isLabelVisible: waitingCount > 0,
            label: Text('$waitingCount'),
            backgroundColor: AppTheme.statusWarning,
            child: const Icon(Icons.dashboard_rounded),
          ),
          label: const Text('Queue'),
        ),

        // 1: Doctor Consultation (Doctor / Admin)
        NavigationRailDestination(
          icon: Icon(
            Icons.medical_services_outlined,
            color: authState.isDoctor ? null : AppTheme.slateMuted.withOpacity(0.4),
          ),
          selectedIcon: const Icon(Icons.medical_services_rounded),
          label: Text(
            'Consult',
            style: TextStyle(
              color: authState.isDoctor ? null : AppTheme.slateMuted.withOpacity(0.4),
            ),
          ),
        ),

        // 2: Pharmacy Inventory & Stock
        const NavigationRailDestination(
          icon: Icon(Icons.medication_outlined),
          selectedIcon: Icon(Icons.medication_rounded),
          label: Text('Pharmacy'),
        ),

        // 3: Cashier Billing Panel
        NavigationRailDestination(
          icon: Badge(
            isLabelVisible: billingCount > 0,
            label: Text('$billingCount'),
            backgroundColor: AppTheme.primaryGreen,
            child: const Icon(Icons.point_of_sale_outlined),
          ),
          selectedIcon: Badge(
            isLabelVisible: billingCount > 0,
            label: Text('$billingCount'),
            backgroundColor: AppTheme.primaryGreen,
            child: const Icon(Icons.point_of_sale_rounded),
          ),
          label: const Text('Billing'),
        ),

        // 4: Patient Records
        const NavigationRailDestination(
          icon: Icon(Icons.people_outline_rounded),
          selectedIcon: Icon(Icons.people_rounded),
          label: Text('Patients'),
        ),

        // 5: Admin User Management (Admin Only)
        if (authState.isAdmin)
          const NavigationRailDestination(
            icon: Icon(Icons.admin_panel_settings_outlined),
            selectedIcon: Icon(Icons.admin_panel_settings_rounded),
            label: Text('Admin'),
          ),
      ],
      trailing: Expanded(
        child: Align(
          alignment: Alignment.bottomCenter,
          child: Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: 'Settings & Cloud Sync',
                  icon: const Icon(Icons.settings_outlined, color: AppTheme.slateMuted, size: 18),
                  onPressed: () => _showSyncInfoDialog(context),
                ),
                const SizedBox(height: 2),
                const Text(
                  'v1.2.0-RBAC',
                  style: TextStyle(color: AppTheme.slateMuted, fontSize: 8.5),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Bottom Navigation Bar for Mobile / Compact Screens
  Widget _buildBottomNavigationBar(
    BuildContext context,
    WidgetRef ref,
    int selectedIndex,
    int waitingCount,
    int billingCount,
    AuthState authState,
  ) {
    // Clamp selected index within available bottom destinations length
    final effectiveIndex = selectedIndex >= 5 ? 0 : selectedIndex;

    return NavigationBar(
      selectedIndex: effectiveIndex,
      onDestinationSelected: (index) => _onTabSelected(context, index, authState),
      backgroundColor: AppTheme.cardPureWhite,
      indicatorColor: AppTheme.softGreenContainer,
      destinations: [
        NavigationDestination(
          icon: Badge(
            isLabelVisible: waitingCount > 0,
            label: Text('$waitingCount'),
            backgroundColor: AppTheme.statusWarning,
            child: const Icon(Icons.dashboard_outlined),
          ),
          selectedIcon: const Icon(Icons.dashboard, color: AppTheme.primaryGreen),
          label: 'Queue',
        ),
        NavigationDestination(
          icon: Icon(
            Icons.medical_services_outlined,
            color: authState.isDoctor ? null : AppTheme.slateMuted.withOpacity(0.4),
          ),
          selectedIcon: const Icon(Icons.medical_services, color: AppTheme.primaryGreen),
          label: 'Consult',
        ),
        const NavigationDestination(
          icon: Icon(Icons.medication_outlined),
          selectedIcon: Icon(Icons.medication, color: AppTheme.primaryGreen),
          label: 'Pharmacy',
        ),
        NavigationDestination(
          icon: Badge(
            isLabelVisible: billingCount > 0,
            label: Text('$billingCount'),
            backgroundColor: AppTheme.primaryGreen,
            child: const Icon(Icons.point_of_sale_outlined),
          ),
          selectedIcon: const Icon(Icons.point_of_sale, color: AppTheme.primaryGreen),
          label: 'Billing',
        ),
        const NavigationDestination(
          icon: Icon(Icons.people_outline),
          selectedIcon: Icon(Icons.people, color: AppTheme.primaryGreen),
          label: 'Patients',
        ),
      ],
    );
  }

  void _onTabSelected(BuildContext context, int index, AuthState authState) {
    // RBAC Guard: If trying to access Consultation (index 1) without Doctor or Admin role
    if (index == 1 && !authState.isDoctor) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('ဆရာဝန် (Doctor) သီးသန့် အသုံးပြုနိုင်သော နေရာဖြစ်ပါသည်'),
          backgroundColor: AppTheme.statusCritical,
          duration: Duration(seconds: 2),
        ),
      );
      return;
    }

    // RBAC Guard: If trying to access Admin (index 5) without Admin role
    if (index == 5 && !authState.isAdmin) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Admin သီးသန့် အသုံးပြုနိုင်သော နေရာဖြစ်ပါသည်'),
          backgroundColor: AppTheme.statusCritical,
          duration: Duration(seconds: 2),
        ),
      );
      return;
    }

    navigationShell.goBranch(
      index,
      initialLocation: index == navigationShell.currentIndex,
    );
  }

  void _confirmLogout(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedCornerShape(borderRadius: BorderRadius.circular(12)),
        title: Row(
          children: const [
            Icon(Icons.logout_rounded, color: AppTheme.accentRedBrown, size: 20),
            SizedBox(width: 8),
            Text('အကောင့်ထွက်ခွာမည် (Sign Out)', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
          ],
        ),
        content: const Text(
          'လက်ရှိအကောင့်မှ ထွက်ခွာပြီး Login မျက်နှာပြင်သို့ သွားရောက်လိုပါသလား?',
          style: TextStyle(fontSize: 12, color: AppTheme.slateDark),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancel', style: TextStyle(color: AppTheme.slateMedium)),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              ref.read(authProvider.notifier).logout();
              context.go('/login');
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentRedBrown,
              foregroundColor: Colors.white,
              shape: RoundedCornerShape(borderRadius: BorderRadius.circular(6)),
            ),
            child: const Text('Sign Out', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _showSyncInfoDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedCornerShape(12),
        title: Row(
          children: const [
            Icon(Icons.cloud_sync, color: AppTheme.primaryGreen),
            SizedBox(width: 8),
            Text('Offline-First Database Engine'),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text(
              '"ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း operates on an autonomous SQLite offline-first architecture via Drift.',
              style: TextStyle(fontSize: 13, color: AppTheme.slateDark),
            ),
            SizedBox(height: 10),
            Text(
              '• All patient records, consultations, and invoices are saved instantly to the local device.',
              style: TextStyle(fontSize: 12, color: AppTheme.slateMedium),
            ),
            SizedBox(height: 6),
            Text(
              '• Dual printing (Thermal 58/80mm & A4 PDF) functions completely offline without internet connectivity.',
              style: TextStyle(fontSize: 12, color: AppTheme.slateMedium),
            ),
            SizedBox(height: 6),
            Text(
              '• Role-Based Access Control (RBAC) securely protects clinical notes and admin user accounts.',
              style: TextStyle(fontSize: 12, color: AppTheme.slateMedium),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Close', style: TextStyle(color: AppTheme.primaryGreen, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
