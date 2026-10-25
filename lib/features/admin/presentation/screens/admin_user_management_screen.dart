import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/theme/app_theme.dart';
import '../../auth/presentation/providers/auth_provider.dart';

/// Admin User Management & Role-Based Access Control Configuration Screen
///
/// Features:
/// 1. Admin Access Only Screen (/admin).
/// 2. High-Density User Accounts Table (38px row height):
///    - Columns: Username, Full Name, Role Badge (M3 Chip), SAMA Reg No, Status (Active/Disabled), Actions ([Reset Password], [Edit Role], [Toggle Status]).
/// 3. "Add New User Account" Compact Dialog:
///    - Inputs: Username, Full Name, Password, Role Selector (ADMIN, DOCTOR, PHARMACIST, CASHIER), Doctor SAMA No.
/// 4. Compact Edit User Role Dialog & Password Reset Dialog.
class AdminUserManagementScreen extends ConsumerStatefulWidget {
  const AdminUserManagementScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<AdminUserManagementScreen> createState() => _AdminUserManagementScreenState();
}

class _AdminUserManagementScreenState extends ConsumerState<AdminUserManagementScreen> {
  String _searchQuery = '';

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);
    final usersAsync = ref.watch(allUsersStreamProvider);

    // Safeguard: Verify user has ADMIN role
    if (!authState.isAdmin) {
      return Scaffold(
        backgroundColor: AppTheme.bgLightGray,
        body: Center(
          child: Container(
            padding: const EdgeInsets.all(24),
            constraints: const BoxConstraints(maxWidth: 400),
            decoration: BoxDecoration(
              color: AppTheme.cardPureWhite,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppTheme.statusCritical.withOpacity(0.3)),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.gpp_bad_rounded, size: 48, color: AppTheme.statusCritical),
                const SizedBox(height: 12),
                const Text(
                  'ခွင့်ပြုချက် မရှိပါ (Access Denied)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Only System Administrators (ADMIN) are authorized to access User Management & Security Settings.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 12, color: AppTheme.slateMedium),
                ),
                const SizedBox(height: 16),
                ElevatedButton.icon(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.arrow_back, size: 14),
                  label: const Text('Go Back'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryGreen,
                    foregroundColor: Colors.white,
                    shape: RoundedCornerShape(borderRadius: BorderRadius.circular(6)),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppTheme.bgLightGray,
      appBar: AppBar(
        backgroundColor: AppTheme.cardPureWhite,
        elevation: 0,
        titleSpacing: 16,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1),
          child: Container(color: AppTheme.subtleBorder, height: 1),
        ),
        title: Row(
          children: const [
            Icon(Icons.admin_panel_settings_rounded, color: AppTheme.primaryGreen, size: 20),
            SizedBox(width: 8),
            Text(
              'Admin User Management & RBAC',
              style: TextStyle(color: AppTheme.slateDark, fontWeight: FontWeight.bold, fontSize: 15),
            ),
            SizedBox(width: 8),
            Text(
              'အသုံးပြုသူအကောင့်နှင့် ခွင့်ပြုချက်များ',
              style: TextStyle(color: AppTheme.slateMuted, fontSize: 12, fontWeight: FontWeight.normal),
            ),
          ],
        ),
        actions: [
          // Add New User Button (38px height)
          Padding(
            padding: const EdgeInsets.only(right: 14),
            child: SizedBox(
              height: 34,
              child: ElevatedButton.icon(
                onPressed: () => _showAddUserDialog(context),
                icon: const Icon(Icons.person_add_alt_1_rounded, size: 15),
                label: const Text(
                  'အကောင့်အသစ်ဖွင့်ရန်',
                  style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryGreen,
                  foregroundColor: AppTheme.cardPureWhite,
                  elevation: 0,
                  shape: RoundedCornerShape(borderRadius: BorderRadius.circular(6)),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                ),
              ),
            ),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Top Bar with Filter & Stats
            _buildStatsAndFilterBar(usersAsync),
            const SizedBox(height: 10),

            // High-Density User Accounts Table
            Expanded(
              child: Card(
                elevation: 0,
                shape: RoundedCornerShape(
                  borderRadius: BorderRadius.circular(8),
                  side: BorderSide(color: AppTheme.subtleBorder, width: 1),
                ),
                color: AppTheme.cardPureWhite,
                child: usersAsync.when(
                  loading: () => const Center(
                    child: CircularProgressIndicator(color: AppTheme.primaryGreen),
                  ),
                  error: (err, stack) => Center(
                    child: Text('Error loading users: $err', style: const TextStyle(color: Colors.red)),
                  ),
                  data: (users) {
                    final filtered = users.where((u) {
                      if (_searchQuery.isEmpty) return true;
                      final q = _searchQuery.toLowerCase();
                      return u.username.toLowerCase().contains(q) ||
                          u.fullName.toLowerCase().contains(q) ||
                          u.role.toLowerCase().contains(q) ||
                          (u.samaNumber?.toLowerCase().contains(q) ?? false);
                    }).toList();

                    if (filtered.isEmpty) {
                      return Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: const [
                            Icon(Icons.person_off_outlined, size: 36, color: AppTheme.slateMuted),
                            SizedBox(height: 8),
                            Text(
                              'အသုံးပြုသူ မတွေ့ရှိပါ (No users found)',
                              style: TextStyle(color: AppTheme.slateMedium, fontSize: 13),
                            ),
                          ],
                        ),
                      );
                    }

                    return _buildUsersTable(filtered);
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Compact Filter & Stats Bar
  Widget _buildStatsAndFilterBar(AsyncValue<List<User>> usersAsync) {
    final count = usersAsync.valueOrNull?.length ?? 0;
    final activeCount = usersAsync.valueOrNull?.where((u) => u.isActive).length ?? 0;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: AppTheme.cardPureWhite,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppTheme.subtleBorder, width: 1),
      ),
      child: Row(
        children: [
          // Total Count Badges
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: AppTheme.softGreenContainer,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.3)),
                ),
                child: Text(
                  'စုစုပေါင်း: $count ဦး',
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.primaryGreen,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFFECFDF5),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.statusSuccess.withOpacity(0.3)),
                ),
                child: Text(
                  'အသုံးပြုနိုင်: $activeCount ဦး',
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF065F46),
                  ),
                ),
              ),
            ],
          ),

          const Spacer(),

          // Search Field (Compact 34px)
          SizedBox(
            width: 260,
            height: 34,
            child: TextField(
              style: const TextStyle(fontSize: 12),
              onChanged: (val) => setState(() => _searchQuery = val.trim()),
              decoration: InputDecoration(
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                prefixIcon: const Icon(Icons.search, size: 16, color: AppTheme.slateMuted),
                hintText: 'Search username, name, SAMA...',
                hintStyle: const TextStyle(fontSize: 11, color: AppTheme.slateMuted),
                filled: true,
                fillColor: AppTheme.bgLightGray,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(6),
                  borderSide: const BorderSide(color: AppTheme.borderDivider),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(6),
                  borderSide: const BorderSide(color: AppTheme.borderDivider),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(6),
                  borderSide: const BorderSide(color: AppTheme.primaryGreen, width: 1.5),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// High-Density User Accounts Table
  Widget _buildUsersTable(List<User> users) {
    return Column(
      children: [
        // Table Header
        Container(
          height: 38,
          padding: const EdgeInsets.symmetric(horizontal: 14),
          decoration: const BoxDecoration(
            color: Color(0xFFF1F5F9),
            borderRadius: BorderRadius.vertical(top: Radius.circular(8)),
            border: Border(bottom: BorderSide(color: AppTheme.borderDivider, width: 1)),
          ),
          child: Row(
            children: const [
              Expanded(
                flex: 3,
                child: Text(
                  'USERNAME & NAME',
                  style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: AppTheme.slateDark),
                ),
              ),
              Expanded(
                flex: 2,
                child: Text(
                  'ROLE / နေရာ',
                  style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: AppTheme.slateDark),
                ),
              ),
              Expanded(
                flex: 2,
                child: Text(
                  'SAMA NO (စမ)',
                  style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: AppTheme.slateDark),
                ),
              ),
              Expanded(
                flex: 2,
                child: Text(
                  'STATUS',
                  style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: AppTheme.slateDark),
                ),
              ),
              Expanded(
                flex: 3,
                child: Text(
                  'ACTIONS',
                  textAlign: TextAlign.end,
                  style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: AppTheme.slateDark),
                ),
              ),
            ],
          ),
        ),

        // Table Rows
        Expanded(
          child: ListView.separated(
            itemCount: users.length,
            separatorBuilder: (context, index) => const Divider(
              height: 1,
              thickness: 0.5,
              color: AppTheme.borderDivider,
            ),
            itemBuilder: (context, index) {
              final user = users[index];
              return _buildUserTableRow(user);
            },
          ),
        ),
      ],
    );
  }

  /// Single High-Density Table Row
  Widget _buildUserTableRow(User user) {
    return Container(
      height: 48,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      color: user.isActive ? AppTheme.cardPureWhite : const Color(0xFFFAFAFA),
      child: Row(
        children: [
          // Username & Full Name
          Expanded(
            flex: 3,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      user.username,
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: user.isActive ? AppTheme.slateDark : AppTheme.slateMuted,
                      ),
                    ),
                    if (user.username == 'admin') ...[
                      const SizedBox(width: 4),
                      const Icon(Icons.star, size: 12, color: Color(0xFFD97706)),
                    ],
                  ],
                ),
                Text(
                  user.fullName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 10.5,
                    color: user.isActive ? AppTheme.slateMedium : AppTheme.slateMuted,
                  ),
                ),
              ],
            ),
          ),

          // Role Badge (M3 Chip Style)
          Expanded(
            flex: 2,
            child: Align(
              alignment: Alignment.centerLeft,
              child: _buildRoleBadge(user.role),
            ),
          ),

          // SAMA Reg No
          Expanded(
            flex: 2,
            child: Text(
              user.samaNumber?.isNotEmpty == true ? 'စမ-${user.samaNumber}' : '—',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w600,
                color: user.samaNumber?.isNotEmpty == true ? AppTheme.primaryDarkGreen : AppTheme.slateMuted,
              ),
            ),
          ),

          // Status Badge
          Expanded(
            flex: 2,
            child: Align(
              alignment: Alignment.centerLeft,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: user.isActive ? const Color(0xFFECFDF5) : const Color(0xFFFEF2F2),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(
                    color: user.isActive
                        ? AppTheme.statusSuccess.withOpacity(0.3)
                        : AppTheme.statusCritical.withOpacity(0.3),
                    width: 0.8,
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 5,
                      height: 5,
                      decoration: BoxDecoration(
                        color: user.isActive ? AppTheme.statusSuccess : AppTheme.statusCritical,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 4),
                    Text(
                      user.isActive ? 'Active' : 'Disabled',
                      style: TextStyle(
                        fontSize: 9.5,
                        fontWeight: FontWeight.bold,
                        color: user.isActive ? const Color(0xFF065F46) : const Color(0xFF991B1B),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Actions
          Expanded(
            flex: 3,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                // Edit Role Button
                IconButton(
                  tooltip: 'Edit Role & Info',
                  icon: const Icon(Icons.edit_note_rounded, size: 18, color: AppTheme.slateMedium),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                  onPressed: () => _showEditUserDialog(context, user),
                ),

                // Reset Password Button
                IconButton(
                  tooltip: 'Reset Password',
                  icon: const Icon(Icons.lock_reset_rounded, size: 18, color: Color(0xFF0284C7)),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                  onPressed: () => _showResetPasswordDialog(context, user),
                ),

                // Toggle Status Button
                IconButton(
                  tooltip: user.isActive ? 'Disable User' : 'Enable User',
                  icon: Icon(
                    user.isActive ? Icons.toggle_on_rounded : Icons.toggle_off_rounded,
                    size: 22,
                    color: user.isActive ? AppTheme.statusSuccess : AppTheme.slateMuted,
                  ),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                  onPressed: () => _toggleUserStatus(user),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Compact Role Badge
  Widget _buildRoleBadge(String roleStr) {
    Color bg;
    Color border;
    Color text;

    switch (roleStr.toUpperCase()) {
      case 'ADMIN':
        bg = const Color(0xFFFEF3C7);
        border = const Color(0xFFF59E0B);
        text = const Color(0xFFB45309);
        break;
      case 'DOCTOR':
        bg = AppTheme.softGreenContainer;
        border = AppTheme.primaryGreen;
        text = AppTheme.primaryDarkGreen;
        break;
      case 'PHARMACIST':
        bg = const Color(0xFFF3E8FF);
        border = AppTheme.badgePurple;
        text = const Color(0xFF6B21A8);
        break;
      case 'CASHIER':
      default:
        bg = const Color(0xFFE0F2FE);
        border = const Color(0xFF0284C7);
        text = const Color(0xFF0369A1);
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: border.withOpacity(0.5), width: 0.8),
      ),
      child: Text(
        roleStr.toUpperCase(),
        style: TextStyle(
          fontSize: 9.5,
          fontWeight: FontWeight.w700,
          color: text,
          letterSpacing: 0.2,
        ),
      ),
    );
  }

  // ===========================================================================
  // DIALOGS & ACTIONS
  // ===========================================================================

  /// Add New User Dialog (Compact M3)
  void _showAddUserDialog(BuildContext context) {
    final usernameCtrl = TextEditingController();
    final fullNameCtrl = TextEditingController();
    final passwordCtrl = TextEditingController();
    final samaCtrl = TextEditingController();
    var selectedRole = UserRole.DOCTOR;
    String? dialogError;

    showDialog(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          shape: RoundedCornerShape(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(color: AppTheme.accentRedBrown.withOpacity(0.2), width: 1),
          ),
          backgroundColor: AppTheme.cardPureWhite,
          titlePadding: const EdgeInsets.fromLTRB(18, 16, 18, 8),
          contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
          actionsPadding: const EdgeInsets.fromLTRB(18, 8, 18, 14),
          title: Row(
            children: const [
              Icon(Icons.person_add_rounded, color: AppTheme.primaryGreen, size: 20),
              SizedBox(width: 8),
              Text(
                'အကောင့်အသစ် ဖွင့်ရန် (New User)',
                style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
              ),
            ],
          ),
          content: SingleChildScrollView(
            child: SizedBox(
              width: 380,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (dialogError != null) ...[
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                      decoration: BoxDecoration(
                        color: AppTheme.statusCritical.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        dialogError!,
                        style: const TextStyle(fontSize: 11, color: AppTheme.statusCritical, fontWeight: FontWeight.w600),
                      ),
                    ),
                    const SizedBox(height: 8),
                  ],

                  // Username
                  const Text('Username (အသုံးပြုသူအမည်):', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium)),
                  const SizedBox(height: 4),
                  SizedBox(
                    height: 38,
                    child: TextField(
                      controller: usernameCtrl,
                      style: const TextStyle(fontSize: 12),
                      decoration: _inputDecoration('e.g. nurse_aye, dr_thura'),
                    ),
                  ),
                  const SizedBox(height: 10),

                  // Full Name
                  const Text('Full Name (အမည်အပြည့်အစုံ):', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium)),
                  const SizedBox(height: 4),
                  SizedBox(
                    height: 38,
                    child: TextField(
                      controller: fullNameCtrl,
                      style: const TextStyle(fontSize: 12),
                      decoration: _inputDecoration('e.g. Daw Aye Aye Win'),
                    ),
                  ),
                  const SizedBox(height: 10),

                  // Initial Password
                  const Text('Initial Password (စကားဝှက်):', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium)),
                  const SizedBox(height: 4),
                  SizedBox(
                    height: 38,
                    child: TextField(
                      controller: passwordCtrl,
                      obscureText: true,
                      style: const TextStyle(fontSize: 12),
                      decoration: _inputDecoration('Min 6 characters'),
                    ),
                  ),
                  const SizedBox(height: 10),

                  // Role Selector
                  const Text('Role / တာဝန်:', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium)),
                  const SizedBox(height: 4),
                  Container(
                    height: 38,
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    decoration: BoxDecoration(
                      color: AppTheme.bgLightGray,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: AppTheme.borderDivider),
                    ),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<UserRole>(
                        value: selectedRole,
                        isDense: true,
                        isExpanded: true,
                        style: const TextStyle(fontSize: 12, color: AppTheme.slateDark),
                        items: UserRole.values.map((role) {
                          return DropdownMenuItem<UserRole>(
                            value: role,
                            child: Text(role.displayName),
                          );
                        }).toList(),
                        onChanged: (val) {
                          if (val != null) {
                            setDialogState(() => selectedRole = val);
                          }
                        },
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),

                  // SAMA Number (If Doctor)
                  if (selectedRole == UserRole.DOCTOR) ...[
                    const Text('SAMA Reg No (စမ-နံပါတ်):', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium)),
                    const SizedBox(height: 4),
                    SizedBox(
                      height: 38,
                      child: TextField(
                        controller: samaCtrl,
                        style: const TextStyle(fontSize: 12),
                        decoration: _inputDecoration('e.g. 47414'),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogCtx).pop(),
              child: const Text('Cancel', style: TextStyle(fontSize: 12, color: AppTheme.slateMedium)),
            ),
            ElevatedButton(
              onPressed: () async {
                final u = usernameCtrl.text.trim();
                final f = fullNameCtrl.text.trim();
                final p = passwordCtrl.text.trim();
                final s = samaCtrl.text.trim();

                if (u.isEmpty || f.isEmpty || p.isEmpty) {
                  setDialogState(() => dialogError = 'ကျေးဇူးပြု၍ အချက်အလက်များ ပြည့်စုံစွာ ဖြည့်သွင်းပါ');
                  return;
                }

                final success = await ref.read(authProvider.notifier).createUser(
                      username: u,
                      fullName: f,
                      password: p,
                      role: selectedRole.name,
                      samaNumber: s,
                    );

                if (success && mounted) {
                  Navigator.of(dialogCtx).pop();
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('အသုံးပြုသူအကောင့် "$u" အား အောင်မြင်စွာ ဖန်တီးပြီးပါပြီ'),
                      backgroundColor: AppTheme.primaryGreen,
                    ),
                  );
                } else {
                  final err = ref.read(authProvider).errorMessage;
                  setDialogState(() => dialogError = err ?? 'Failed to create user');
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryGreen,
                foregroundColor: AppTheme.cardPureWhite,
                shape: RoundedCornerShape(borderRadius: BorderRadius.circular(6)),
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              ),
              child: const Text('Create User', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  /// Edit User Role Dialog
  void _showEditUserDialog(BuildContext context, User user) {
    final fullNameCtrl = TextEditingController(text: user.fullName);
    final samaCtrl = TextEditingController(text: user.samaNumber ?? '');
    var selectedRole = UserRole.fromString(user.role);

    showDialog(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          shape: RoundedCornerShape(borderRadius: BorderRadius.circular(12)),
          backgroundColor: AppTheme.cardPureWhite,
          title: Text(
            'Edit User: ${user.username}',
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
          ),
          content: SizedBox(
            width: 360,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text('Full Name:', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                SizedBox(
                  height: 38,
                  child: TextField(
                    controller: fullNameCtrl,
                    style: const TextStyle(fontSize: 12),
                    decoration: _inputDecoration('Full Name'),
                  ),
                ),
                const SizedBox(height: 10),

                const Text('Role:', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                Container(
                  height: 38,
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  decoration: BoxDecoration(
                    color: AppTheme.bgLightGray,
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: AppTheme.borderDivider),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<UserRole>(
                      value: selectedRole,
                      isDense: true,
                      isExpanded: true,
                      items: UserRole.values.map((role) {
                        return DropdownMenuItem<UserRole>(
                          value: role,
                          child: Text(role.displayName, style: const TextStyle(fontSize: 12)),
                        );
                      }).toList(),
                      onChanged: (val) {
                        if (val != null) setDialogState(() => selectedRole = val);
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 10),

                if (selectedRole == UserRole.DOCTOR) ...[
                  const Text('SAMA No (စမ):', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 4),
                  SizedBox(
                    height: 38,
                    child: TextField(
                      controller: samaCtrl,
                      style: const TextStyle(fontSize: 12),
                      decoration: _inputDecoration('e.g. 47414'),
                    ),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogCtx).pop(),
              child: const Text('Cancel', style: TextStyle(fontSize: 12)),
            ),
            ElevatedButton(
              onPressed: () async {
                final success = await ref.read(authProvider.notifier).updateUserRole(
                      id: user.id,
                      fullName: fullNameCtrl.text.trim(),
                      role: selectedRole.name,
                      samaNumber: samaCtrl.text.trim(),
                    );
                if (success && mounted) {
                  Navigator.of(dialogCtx).pop();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('အသုံးပြုသူအချက်အလက် ပြင်ဆင်ပြီးပါပြီ'),
                      backgroundColor: AppTheme.primaryGreen,
                    ),
                  );
                }
              },
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryGreen),
              child: const Text('Save Changes', style: TextStyle(color: Colors.white, fontSize: 12)),
            ),
          ],
        ),
      ),
    );
  }

  /// Reset Password Dialog
  void _showResetPasswordDialog(BuildContext context, User user) {
    final passwordCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        shape: RoundedCornerShape(borderRadius: BorderRadius.circular(12)),
        title: Text(
          'Reset Password: ${user.username}',
          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
        ),
        content: SizedBox(
          width: 320,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Enter new password:', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
              const SizedBox(height: 6),
              SizedBox(
                height: 38,
                child: TextField(
                  controller: passwordCtrl,
                  obscureText: true,
                  style: const TextStyle(fontSize: 12),
                  decoration: _inputDecoration('New password'),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogCtx).pop(),
            child: const Text('Cancel', style: TextStyle(fontSize: 12)),
          ),
          ElevatedButton(
            onPressed: () async {
              final newPass = passwordCtrl.text.trim();
              if (newPass.isEmpty) return;

              final success = await ref.read(authProvider.notifier).resetUserPassword(
                    id: user.id,
                    newPassword: newPass,
                  );
              if (success && mounted) {
                Navigator.of(dialogCtx).pop();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('စကားဝှက် ပြောင်းလဲပြီးပါပြီ (${user.username})'),
                    backgroundColor: AppTheme.primaryGreen,
                  ),
                );
              }
            },
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF0284C7)),
            child: const Text('Reset Password', style: TextStyle(color: Colors.white, fontSize: 12)),
          ),
        ],
      ),
    );
  }

  Future<void> _toggleUserStatus(User user) async {
    final newStatus = !user.isActive;
    final success = await ref.read(authProvider.notifier).toggleUserStatus(
          id: user.id,
          isActive: newStatus,
        );
    if (!success && mounted) {
      final err = ref.read(authProvider).errorMessage;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(err ?? 'Failed to update status'), backgroundColor: AppTheme.statusCritical),
      );
    }
  }

  InputDecoration _inputDecoration(String hint) {
    return InputDecoration(
      isDense: true,
      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      hintText: hint,
      hintStyle: const TextStyle(fontSize: 11, color: AppTheme.slateMuted),
      filled: true,
      fillColor: AppTheme.bgLightGray,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(6),
        borderSide: const BorderSide(color: AppTheme.borderDivider),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(6),
        borderSide: const BorderSide(color: AppTheme.borderDivider),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(6),
        borderSide: const BorderSide(color: AppTheme.primaryGreen, width: 1.5),
      ),
    );
  }
}
