import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/admin/presentation/screens/admin_user_management_screen.dart';
import '../../features/auth/presentation/providers/auth_provider.dart';
import '../../features/auth/presentation/screens/login_screen.dart';
import '../../features/billing/presentation/screens/billing_screen.dart';
import '../../features/consultation/presentation/screens/consultation_screen.dart';
import '../../features/dashboard/presentation/screens/dashboard_shell.dart';
import '../../features/dashboard/presentation/screens/home_dashboard_screen.dart';
import '../../features/patients/presentation/screens/patients_screen.dart';
import '../../features/pharmacy/presentation/screens/pharmacy_screen.dart';

// Global navigation navigator keys
final GlobalKey<NavigatorState> rootNavigatorKey = GlobalKey<NavigatorState>(debugLabel: 'root');
final GlobalKey<NavigatorState> shellNavigatorKey = GlobalKey<NavigatorState>(debugLabel: 'shell');

/// AppRouter Provider equipped with Role Guards and Session Authentication
final appRouterProvider = Provider<GoRouter>((ref) {
  final authNotifier = ref.read(authProvider.notifier);

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: '/',
    refreshListenable: GoRouterRefreshStream(ref.watch(authProvider.notifier).stream),
    redirect: (BuildContext context, GoRouterState state) {
      final authState = ref.read(authProvider);
      final isLoggingIn = state.uri.path == '/login';

      // 1. If not authenticated, redirect to /login
      if (!authState.isAuthenticated) {
        return isLoggingIn ? null : '/login';
      }

      // 2. If authenticated and attempting to visit /login, redirect to home
      if (isLoggingIn) {
        return '/';
      }

      // 3. Enforce Role Guards:
      // Restrict /admin route exclusively to users with ADMIN role
      if (state.uri.path.startsWith('/admin') && !authState.isAdmin) {
        return '/';
      }

      // Restrict /consultation route exclusively to DOCTOR or ADMIN role
      if (state.uri.path.startsWith('/consultation') && !authState.isDoctor) {
        return '/';
      }

      // No redirection needed
      return null;
    },
    routes: [
      // Direct Login Route
      GoRoute(
        path: '/login',
        name: 'login',
        builder: (context, state) => const LoginScreen(),
      ),

      // Stateful Shell Route with Persistent Tabs
      StatefulShellRoute.indexedStack(
        builder: (BuildContext context, GoRouterState state, StatefulNavigationShell navigationShell) {
          return DashboardShell(navigationShell: navigationShell);
        },
        branches: [
          // Branch 0: Home / Patient Queue Dashboard
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/',
                name: 'dashboard',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: HomeDashboardScreen(),
                ),
              ),
            ],
          ),

          // Branch 1: Doctor Consultation Screen
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/consultation',
                name: 'consultation',
                pageBuilder: (context, state) {
                  final patientId = state.uri.queryParameters['patientId'];
                  final visitId = state.uri.queryParameters['visitId'];
                  return NoTransitionPage(
                    child: ConsultationScreen(
                      initialPatientId: patientId,
                      initialVisitId: visitId,
                    ),
                  );
                },
              ),
            ],
          ),

          // Branch 2: Pharmacy Inventory & Stock-In
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/pharmacy',
                name: 'pharmacy',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: PharmacyScreen(),
                ),
              ),
            ],
          ),

          // Branch 3: Cashier Billing Panel
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/billing',
                name: 'billing',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: BillingScreen(),
                ),
              ),
            ],
          ),

          // Branch 4: Patient Registration & Records
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/patients',
                name: 'patients',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: PatientsScreen(),
                ),
              ),
            ],
          ),

          // Branch 5: Admin User Accounts & RBAC Screen (Admin Only)
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/admin',
                name: 'admin',
                pageBuilder: (context, state) => const NoTransitionPage(
                  child: AdminUserManagementScreen(),
                ),
              ),
            ],
          ),
        ],
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text(
              'Route not found: ${state.uri}',
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => context.go('/'),
              child: const Text('Return to Home Dashboard'),
            ),
          ],
        ),
      ),
    ),
  );
});

/// Bridge helper for GoRouter refreshListenable using a Stream
class GoRouterRefreshStream extends ChangeNotifier {
  GoRouterRefreshStream(Stream<dynamic> stream) {
    notifyListeners();
    stream.asBroadcastStream().listen((_) => notifyListeners());
  }
}

/// Backward compatibility static instance for any legacy references
final GoRouter appRouter = GoRouter(
  navigatorKey: rootNavigatorKey,
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeDashboardScreen(),
    ),
  ],
);
