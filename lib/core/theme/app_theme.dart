import 'package:flutter/material.dart';

/// Central Design System & Theme Configuration for "ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း
///
/// Refactored to Compact Clinical UI:
/// - Primary: Medical Green (#2E7D32)
/// - Background: Light Gray (#F4F6F8)
/// - Card Background: Pure White (#FFFFFF), 8px radius, 1px border (#7A2E2E with 0.2 opacity or #E0E0E0)
/// - Accent Lines / Dividers: Red-Brown (#7A2E2E) with 1px width
/// - Global Visual Density: VisualDensity.compact
/// - Clean InputDecoration: isDense: true, contentPadding: (h:12, v:10), OutlineInputBorder(r:6)
///   to prevent cursor and floating label collisions.
/// - Compact Button Themes: height <= 38px, padding (h:14, v:8).
class AppTheme {
  // Brand & Palette Constants
  static const Color primaryGreen = Color(0xFF2E7D32); // Medical Green
  static const Color primaryDarkGreen = Color(0xFF1B5E20);
  static const Color softGreenContainer = Color(0xFFE8F5E9); // Light Green Tint
  static const Color bgLightGray = Color(0xFFF4F6F8); // Clinical Background
  static const Color cardPureWhite = Color(0xFFFFFFFF);
  static const Color accentRedBrown = Color(0xFF7A2E2E); // Red-Brown Accent
  static const Color borderDivider = Color(0xFFE0E0E0);
  static final Color subtleBorder = const Color(0xFF7A2E2E).withOpacity(0.2);

  // Typography & Neutral Slate
  static const Color slateDark = Color(0xFF0F172A);
  static const Color slateMedium = Color(0xFF334155);
  static const Color slateMuted = Color(0xFF64748B);

  // Status Colors
  static const Color statusSuccess = Color(0xFF10B981);
  static const Color statusWarning = Color(0xFFF59E0B);
  static const Color statusCritical = Color(0xFFEF4444);
  static const Color badgePurple = Color(0xFF8B5CF6);

  // Clinic Brand Strings
  static const String clinicTitleMyanmar = '"ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း';
  static const String clinicTitleEnglish = 'Shwe Khit Polyclinic & General Practice';
  static const String doctorNameAndCredentials = 'Dr. Nyan Kaung Sett M.B.,B.S(MDY)';
  static const String clinicAddress = 'အမှတ် (၁၄၂)၊ ဗဟိုလမ်း၊ ကမာရွတ်မြို့နယ်၊ ရန်ကုန်မြို့။';
  static const String clinicPhone = '09-421008761, 09-790001122';

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      visualDensity: VisualDensity.compact,
      fontFamily: 'Roboto',
      scaffoldBackgroundColor: bgLightGray,
      colorScheme: ColorScheme.fromSeed(
        seedColor: primaryGreen,
        primary: primaryGreen,
        onPrimary: cardPureWhite,
        primaryContainer: softGreenContainer,
        onPrimaryContainer: primaryDarkGreen,
        secondary: accentRedBrown,
        onSecondary: cardPureWhite,
        surface: cardPureWhite,
        onSurface: slateDark,
        background: bgLightGray,
        onBackground: slateDark,
        outline: borderDivider,
        error: statusCritical,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: cardPureWhite,
        foregroundColor: slateDark,
        elevation: 0.5,
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: TextStyle(
          color: slateDark,
          fontWeight: FontWeight.bold,
          fontSize: 16,
        ),
      ),
      cardTheme: CardTheme(
        color: cardPureWhite,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(
            color: subtleBorder,
            width: 1,
          ),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: subtleBorder,
        thickness: 1,
        space: 1,
      ),
      inputDecorationTheme: InputDecorationTheme(
        isDense: true,
        filled: true,
        fillColor: cardPureWhite,
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: borderDivider, width: 1),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: borderDivider, width: 1),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: primaryGreen, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: statusCritical, width: 1),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(6),
          borderSide: const BorderSide(color: statusCritical, width: 1.5),
        ),
        labelStyle: const TextStyle(fontSize: 12, color: slateMuted),
        floatingLabelStyle: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: primaryGreen,
        ),
        hintStyle: const TextStyle(fontSize: 12, color: Color(0xFF94A3B8)),
        floatingLabelBehavior: FloatingLabelBehavior.auto,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primaryGreen,
          foregroundColor: cardPureWhite,
          elevation: 0,
          minimumSize: const Size(0, 38),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(6),
          ),
          textStyle: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 12,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: primaryGreen,
          side: const BorderSide(color: primaryGreen, width: 1.5),
          minimumSize: const Size(0, 38),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(6),
          ),
          textStyle: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 12,
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: primaryGreen,
          minimumSize: const Size(0, 32),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          textStyle: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 12,
          ),
        ),
      ),
      navigationRailTheme: const NavigationRailThemeData(
        backgroundColor: cardPureWhite,
        indicatorColor: softGreenContainer,
        selectedIconTheme: IconThemeData(color: primaryGreen, size: 24),
        unselectedIconTheme: IconThemeData(color: slateMuted, size: 22),
        selectedLabelTextStyle: TextStyle(
          color: primaryGreen,
          fontWeight: FontWeight.bold,
          fontSize: 11,
        ),
        unselectedLabelTextStyle: TextStyle(
          color: slateMuted,
          fontWeight: FontWeight.w500,
          fontSize: 11,
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: cardPureWhite,
        indicatorColor: softGreenContainer,
        iconTheme: MaterialStateProperty.resolveWith((states) {
          if (states.contains(MaterialState.selected)) {
            return const IconThemeData(color: primaryGreen);
          }
          return const IconThemeData(color: slateMuted);
        }),
      ),
    );
  }
}
