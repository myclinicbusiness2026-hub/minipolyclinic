import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_theme.dart';
import '../providers/auth_provider.dart';

/// Compact Login Screen
///
/// Refactored for Compact Clinical UI:
/// - M3 Compact Card featuring "ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း Header Branding.
/// - Username and Password / Quick PIN input fields with isDense: true and 38px height.
/// - Direct offline authentication against local Drift SQLite database.
/// - Quick Demo Account switcher chips for quick testing during clinical rounds.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController(text: 'admin');
  final _passwordController = TextEditingController(text: 'admin123');
  bool _obscurePassword = true;
  String? _localError;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _fillAccount(String username, String password) {
    setState(() {
      _usernameController.text = username;
      _passwordController.text = password;
      _localError = null;
    });
  }

  Future<void> _handleLogin() async {
    setState(() {
      _localError = null;
    });

    final username = _usernameController.text.trim();
    final password = _passwordController.text.trim();

    if (username.isEmpty || password.isEmpty) {
      setState(() {
        _localError = 'ကျေးဇူးပြု၍ အသုံးပြုသူအမည်နှင့် စကားဝှက် ထည့်သွင်းပါ';
      });
      return;
    }

    final success = await ref.read(authProvider.notifier).login(username, password);
    if (success && mounted) {
      context.go('/');
    } else if (mounted) {
      final authState = ref.read(authProvider);
      setState(() {
        _localError = authState.errorMessage ?? 'အကောင့်ဝင်ရောက်မှု မအောင်မြင်ပါ';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      backgroundColor: AppTheme.bgLightGray,
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Card(
              elevation: 1,
              shape: RoundedCornerShape(
                borderRadius: BorderRadius.circular(12),
                side: BorderSide(color: AppTheme.accentRedBrown.withOpacity(0.25), width: 1),
              ),
              color: AppTheme.cardPureWhite,
              child: Padding(
                padding: const EdgeInsets.all(22),
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Header Clinic Logo & Branding
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            width: 44,
                            height: 44,
                            decoration: BoxDecoration(
                              color: AppTheme.softGreenContainer,
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(
                                color: AppTheme.primaryGreen.withOpacity(0.35),
                                width: 1,
                              ),
                            ),
                            child: const Icon(
                              Icons.local_hospital_rounded,
                              color: AppTheme.primaryGreen,
                              size: 26,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: const [
                              Text(
                                AppTheme.clinicTitleMyanmar,
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w800,
                                  color: AppTheme.slateDark,
                                  letterSpacing: -0.2,
                                ),
                              ),
                              SizedBox(height: 2),
                              Text(
                                'Shwe Khit Polyclinic & General Practice',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w600,
                                  color: AppTheme.primaryGreen,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),

                      const SizedBox(height: 14),
                      Divider(color: AppTheme.accentRedBrown.withOpacity(0.15), height: 1),
                      const SizedBox(height: 14),

                      // Doctor & Facility Details Banner
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                        decoration: BoxDecoration(
                          color: AppTheme.softGreenContainer.withOpacity(0.6),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.2)),
                        ),
                        child: Row(
                          children: const [
                            Icon(Icons.verified_user_rounded, color: AppTheme.primaryGreen, size: 16),
                            SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Dr. Nyan Kaung Sett M.B.,B.S(MDY) • စမ-၄၇၄၁၄\nOffline SQLite Database Access Control',
                                style: TextStyle(
                                  fontSize: 10.5,
                                  color: AppTheme.primaryDarkGreen,
                                  fontWeight: FontWeight.w600,
                                  height: 1.25,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),

                      const SizedBox(height: 18),

                      const Text(
                        'အကောင့်ဝင်ရောက်ရန် (Sign In)',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: AppTheme.slateDark,
                        ),
                      ),
                      const SizedBox(height: 12),

                      // Error message banner
                      if (_localError != null) ...[
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                          decoration: BoxDecoration(
                            color: AppTheme.statusCritical.withOpacity(0.08),
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: AppTheme.statusCritical.withOpacity(0.3)),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.error_outline, color: AppTheme.statusCritical, size: 16),
                              const SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  _localError!,
                                  style: const TextStyle(
                                    fontSize: 11,
                                    color: AppTheme.statusCritical,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                      ],

                      // Username Field (Compact 38px)
                      const Text(
                        'အသုံးပြုသူအမည် (Username):',
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium),
                      ),
                      const SizedBox(height: 4),
                      SizedBox(
                        height: 38,
                        child: TextFormField(
                          controller: _usernameController,
                          style: const TextStyle(fontSize: 12, color: AppTheme.slateDark),
                          decoration: InputDecoration(
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                            prefixIcon: const Icon(Icons.person_outline, size: 16, color: AppTheme.slateMuted),
                            hintText: 'e.g. admin, drnyan, pharmacy, cashier',
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

                      const SizedBox(height: 12),

                      // Password Field (Compact 38px)
                      const Text(
                        'စကားဝှက် (Password / PIN):',
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.slateMedium),
                      ),
                      const SizedBox(height: 4),
                      SizedBox(
                        height: 38,
                        child: TextFormField(
                          controller: _passwordController,
                          obscureText: _obscurePassword,
                          style: const TextStyle(fontSize: 12, color: AppTheme.slateDark),
                          onFieldSubmitted: (_) => _handleLogin(),
                          decoration: InputDecoration(
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                            prefixIcon: const Icon(Icons.lock_outline, size: 16, color: AppTheme.slateMuted),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscurePassword ? Icons.visibility_off : Icons.visibility,
                                size: 16,
                                color: AppTheme.slateMuted,
                              ),
                              padding: EdgeInsets.zero,
                              constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                              onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                            ),
                            hintText: 'Enter password',
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

                      const SizedBox(height: 18),

                      // Login Button (Compact 38px, Primary Medical Green)
                      SizedBox(
                        height: 38,
                        child: ElevatedButton(
                          onPressed: authState.isLoading ? null : _handleLogin,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.primaryGreen,
                            foregroundColor: AppTheme.cardPureWhite,
                            elevation: 0,
                            shape: RoundedCornerShape(borderRadius: BorderRadius.circular(6)),
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                          ),
                          child: authState.isLoading
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                                  ),
                                )
                              : Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: const [
                                    Icon(Icons.login_rounded, size: 16),
                                    SizedBox(width: 6),
                                    Text(
                                      'အကောင့်ဝင်မည် (Sign In)',
                                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                                    ),
                                  ],
                                ),
                        ),
                      ),

                      const SizedBox(height: 18),
                      Divider(color: AppTheme.accentRedBrown.withOpacity(0.15), height: 1),
                      const SizedBox(height: 12),

                      // Quick Demo Account Selection Chips
                      const Text(
                        'Demo Quick Logins (ဆေးခန်းအမြန်ရွေးချယ်ရန်):',
                        style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppTheme.slateMuted),
                      ),
                      const SizedBox(height: 6),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: [
                          _buildQuickLoginChip('Admin', 'admin', 'admin123', AppTheme.primaryGreen),
                          _buildQuickLoginChip('Dr. Nyan', 'drnyan', 'doctor123', const Color(0xFF0284C7)),
                          _buildQuickLoginChip('Pharmacy', 'pharmacy', 'pharm123', AppTheme.badgePurple),
                          _buildQuickLoginChip('Cashier', 'cashier', 'cash123', const Color(0xFFD97706)),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildQuickLoginChip(String label, String u, String p, Color chipColor) {
    return InkWell(
      onTap: () => _fillAccount(u, p),
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
        decoration: BoxDecoration(
          color: chipColor.withOpacity(0.1),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: chipColor.withOpacity(0.3), width: 0.8),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.account_circle, size: 12, color: chipColor),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: chipColor),
            ),
          ],
        ),
      ),
    );
  }
}
