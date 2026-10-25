import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/database/database_provider.dart';

/// Supported User Roles
enum UserRole {
  ADMIN,
  DOCTOR,
  PHARMACIST,
  CASHIER;

  String get displayName {
    switch (this) {
      case UserRole.ADMIN:
        return 'System Admin (စီမံခန့်ခွဲသူ)';
      case UserRole.DOCTOR:
        return 'Doctor (ဆရာဝန်)';
      case UserRole.PHARMACIST:
        return 'Pharmacist (ဆေးဝါးတာဝန်ခံ)';
      case UserRole.CASHIER:
        return 'Cashier (ငွေကိုင်တာဝန်ခံ)';
    }
  }

  String get shortLabel {
    switch (this) {
      case UserRole.ADMIN:
        return 'ADMIN';
      case UserRole.DOCTOR:
        return 'DOCTOR';
      case UserRole.PHARMACIST:
        return 'PHARMACY';
      case UserRole.CASHIER:
        return 'CASHIER';
    }
  }

  static UserRole fromString(String? roleStr) {
    switch (roleStr?.toUpperCase()) {
      case 'ADMIN':
        return UserRole.ADMIN;
      case 'DOCTOR':
        return UserRole.DOCTOR;
      case 'PHARMACIST':
      case 'PHARMACY':
        return UserRole.PHARMACIST;
      case 'CASHIER':
        return UserRole.CASHIER;
      default:
        return UserRole.CASHIER;
    }
  }
}

/// Password Hashing Utility using SHA-256 with a local static salt
class PasswordHashUtility {
  static const String _localSalt = 'ShweKhit_Clinic_Secure_Salt_2026_@#\$%';

  /// Generates SHA-256 hash with local salt
  static String hashPassword(String rawPassword) {
    final saltedBytes = utf8.encode('$rawPassword:$_localSalt');
    final digest = sha256.convert(saltedBytes);
    return digest.toString();
  }

  /// Verifies raw password matches stored hash
  static bool verifyPassword(String rawPassword, String storedHash) {
    final computed = hashPassword(rawPassword);
    return computed == storedHash;
  }
}

/// Active Authentication Session State
class AuthState {
  final User? currentUser;
  final bool isAuthenticated;
  final bool isLoading;
  final String? errorMessage;

  const AuthState({
    this.currentUser,
    this.isAuthenticated = false,
    this.isLoading = false,
    this.errorMessage,
  });

  AuthState copyWith({
    User? currentUser,
    bool? isAuthenticated,
    bool? isLoading,
    String? errorMessage,
    bool clearUser = false,
  }) {
    return AuthState(
      currentUser: clearUser ? null : (currentUser ?? this.currentUser),
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }

  UserRole get role => UserRole.fromString(currentUser?.role);

  bool get isAdmin => role == UserRole.ADMIN;
  bool get isDoctor => role == UserRole.DOCTOR || role == UserRole.ADMIN;
  bool get isPharmacist => role == UserRole.PHARMACIST || role == UserRole.ADMIN;
  bool get isCashier => role == UserRole.CASHIER || role == UserRole.ADMIN;
}

/// Authentication State Notifier
class AuthNotifier extends StateNotifier<AuthState> {
  final Ref _ref;

  AuthNotifier(this._ref) : super(const AuthState(isLoading: true)) {
    _initializeAuthAndSeedAdmin();
  }

  ClinicDatabaseDao get _dao => _ref.read(clinicDaoProvider);

  /// Seeds default admin and doctor on first startup if user table is empty
  Future<void> _initializeAuthAndSeedAdmin() async {
    try {
      final userCount = await _dao.countUsers();
      if (userCount == 0) {
        // 1. Default Admin Account (admin / admin123)
        final adminTimestamp = DateTime.now();
        await _dao.insertUser(
          UsersCompanion.insert(
            id: 'USR-ADMIN-01',
            username: 'admin',
            passwordHash: PasswordHashUtility.hashPassword('admin123'),
            fullName: 'System Administrator',
            role: 'ADMIN',
            samaNumber: const Value(null),
            isActive: const Value(true),
            createdAt: Value(adminTimestamp),
          ),
        );

        // 2. Default Doctor Account (Dr. Nyan Kaung Sett / doctor123)
        await _dao.insertUser(
          UsersCompanion.insert(
            id: 'USR-DOC-01',
            username: 'drnyan',
            passwordHash: PasswordHashUtility.hashPassword('doctor123'),
            fullName: 'Dr. Nyan Kaung Sett M.B.,B.S(MDY)',
            role: 'DOCTOR',
            samaNumber: const Value('47414'),
            isActive: const Value(true),
            createdAt: Value(adminTimestamp),
          ),
        );

        // 3. Default Pharmacist Account (pharmacy / pharm123)
        await _dao.insertUser(
          UsersCompanion.insert(
            id: 'USR-PHARM-01',
            username: 'pharmacy',
            passwordHash: PasswordHashUtility.hashPassword('pharm123'),
            fullName: 'Daw Khin Sandar (Chief Pharmacist)',
            role: 'PHARMACIST',
            samaNumber: const Value(null),
            isActive: const Value(true),
            createdAt: Value(adminTimestamp),
          ),
        );

        // 4. Default Cashier Account (cashier / cash123)
        await _dao.insertUser(
          UsersCompanion.insert(
            id: 'USR-CASH-01',
            username: 'cashier',
            passwordHash: PasswordHashUtility.hashPassword('cash123'),
            fullName: 'Ma Hnin Wai (Senior Cashier)',
            role: 'CASHIER',
            samaNumber: const Value(null),
            isActive: const Value(true),
            createdAt: Value(adminTimestamp),
          ),
        );
      }

      // Auto-login default admin for seamless immediate access
      final defaultAdmin = await _dao.getUserByUsername('admin');
      if (defaultAdmin != null && defaultAdmin.isActive) {
        state = AuthState(
          currentUser: defaultAdmin,
          isAuthenticated: true,
          isLoading: false,
        );
      } else {
        state = const AuthState(isLoading: false, isAuthenticated: false);
      }
    } catch (e) {
      state = AuthState(
        isLoading: false,
        isAuthenticated: false,
        errorMessage: 'Auth initialization error: $e',
      );
    }
  }

  /// Login with username and raw password
  Future<bool> login(String username, String password) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    final cleanUsername = username.trim().toLowerCase();
    final user = await _dao.getUserByUsername(cleanUsername);

    if (user == null) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'အသုံးပြုသူအမည် မမှန်ကန်ပါ (Username not found)',
      );
      return false;
    }

    if (!user.isActive) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'ဤအကောင့်အား ပိတ်ထားပါသည် (Account is disabled by Admin)',
      );
      return false;
    }

    final isValid = PasswordHashUtility.verifyPassword(password.trim(), user.passwordHash);
    if (!isValid) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'စကားဝှက် မမှန်ကန်ပါ (Incorrect password)',
      );
      return false;
    }

    state = AuthState(
      currentUser: user,
      isAuthenticated: true,
      isLoading: false,
    );
    return true;
  }

  /// Logout current user
  void logout() {
    state = const AuthState(
      currentUser: null,
      isAuthenticated: false,
      isLoading: false,
    );
  }

  /// Create a new user account
  Future<bool> createUser({
    required String username,
    required String password,
    required String fullName,
    required String role,
    String? samaNumber,
  }) async {
    try {
      final cleanUsername = username.trim().toLowerCase();
      final existing = await _dao.getUserByUsername(cleanUsername);
      if (existing != null) {
        state = state.copyWith(errorMessage: 'Username "$cleanUsername" is already taken.');
        return false;
      }

      final userId = 'USR-${DateTime.now().millisecondsSinceEpoch}';
      final passwordHash = PasswordHashUtility.hashPassword(password.trim());

      await _dao.insertUser(
        UsersCompanion.insert(
          id: userId,
          username: cleanUsername,
          passwordHash: passwordHash,
          fullName: fullName.trim(),
          role: role.toUpperCase(),
          samaNumber: Value(samaNumber?.trim().isEmpty == true ? null : samaNumber?.trim()),
          isActive: const Value(true),
          createdAt: Value(DateTime.now()),
        ),
      );
      return true;
    } catch (e) {
      state = state.copyWith(errorMessage: 'Failed to create user: $e');
      return false;
    }
  }

  /// Update user role and metadata
  Future<bool> updateUserRole({
    required String id,
    required String fullName,
    required String role,
    String? samaNumber,
  }) async {
    try {
      final success = await _dao.updateUser(
        id: id,
        fullName: fullName.trim(),
        role: role.toUpperCase(),
        samaNumber: samaNumber?.trim().isEmpty == true ? null : samaNumber?.trim(),
      );

      // If updating current logged in user, refresh current user state
      if (state.currentUser?.id == id) {
        final updated = await _dao.getUserById(id);
        if (updated != null) {
          state = state.copyWith(currentUser: updated);
        }
      }
      return success;
    } catch (e) {
      state = state.copyWith(errorMessage: 'Failed to update user: $e');
      return false;
    }
  }

  /// Reset user password
  Future<bool> resetUserPassword({
    required String id,
    required String newPassword,
  }) async {
    try {
      final newHash = PasswordHashUtility.hashPassword(newPassword.trim());
      final success = await _dao.updateUserPassword(id: id, newPasswordHash: newHash);
      return success;
    } catch (e) {
      state = state.copyWith(errorMessage: 'Failed to reset password: $e');
      return false;
    }
  }

  /// Toggle user active status
  Future<bool> toggleUserStatus({
    required String id,
    required bool isActive,
  }) async {
    try {
      // Prevent disabling the currently active user if it's the only admin
      if (state.currentUser?.id == id && !isActive) {
        state = state.copyWith(errorMessage: 'Cannot disable your own active logged-in account.');
        return false;
      }

      final success = await _dao.toggleUserStatus(id: id, isActive: isActive);
      return success;
    } catch (e) {
      state = state.copyWith(errorMessage: 'Failed to toggle status: $e');
      return false;
    }
  }
}

/// Global Auth State Provider
final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref);
});

/// Current User Provider shortcut
final currentUserProvider = Provider<User?>((ref) {
  return ref.watch(authProvider).currentUser;
});

/// All Users Stream Provider for Admin Configuration
final allUsersStreamProvider = StreamProvider<List<User>>((ref) {
  final dao = ref.watch(clinicDaoProvider);
  return dao.watchAllUsers();
});
