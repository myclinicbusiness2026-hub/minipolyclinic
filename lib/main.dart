import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/database/database_provider.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'features/dashboard/presentation/providers/dashboard_provider.dart';

/// "ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း Main Application Entry Point
///
/// Tech Stack:
/// - Flutter (Latest)
/// - Riverpod 2.0 (ProviderScope)
/// - GoRouter (StatefulShellRoute with Tablet NavigationRail & Shell)
/// - Drift SQLite (Offline-First Local DB)
/// - Compact Clinical UI Theme (Medical Green #2E7D32, Background #F4F6F8, Red-Brown #7A2E2E Accent)
void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(
    const ProviderScope(
      child: MiniPolyclinicApp(),
    ),
  );
}

class MiniPolyclinicApp extends ConsumerWidget {
  const MiniPolyclinicApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Eagerly trigger database initialization & demo seed data
    ref.watch(dashboardProvider);
    final router = ref.watch(appRouterProvider);

    return MaterialApp.router(
      title: AppTheme.clinicTitleMyanmar,
      debugShowCheckedModeBanner: false,
      routerConfig: router,
      theme: AppTheme.lightTheme,
    );
  }
}
