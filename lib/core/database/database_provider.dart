import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app_database.dart';

/// Global Riverpod Provider for the Drift SQLite Database singleton
final appDatabaseProvider = Provider<AppDatabase>((ref) {
  final db = AppDatabase();
  ref.onDispose(() => db.close());
  return db;
});

/// Data Access Object (DAO) providing core business operations for the clinic
class ClinicDatabaseDao {
  final AppDatabase db;

  ClinicDatabaseDao(this.db);

  // ===========================================================================
  // 1. PATIENT REGISTRATION & SEARCH
  // ===========================================================================

  /// Registers a new patient into the database
  Future<int> registerPatient(PatientsCompanion patient) {
    return db.into(db.patients).insert(patient);
  }

  /// Watches all registered patients ordered by registration date descending
  Stream<List<Patient>> watchAllPatients() {
    return (db.select(db.patients)
          ..orderBy([(t) => OrderingTerm.desc(t.registeredAt)]))
        .watch();
  }

  /// Searches patients by Full Name, Patient Code (HN), or Phone number
  Stream<List<Patient>> searchPatients(String query) {
    if (query.trim().isEmpty) return watchAllPatients();
    return (db.select(db.patients)
          ..where((t) =>
              t.fullName.contains(query) |
              t.patientCode.contains(query) |
              t.phone.contains(query))
          ..orderBy([(t) => OrderingTerm.desc(t.registeredAt)]))
        .watch();
  }

  /// Retrieves a specific patient by their primary UUID
  Future<Patient?> getPatientById(String id) {
    return (db.select(db.patients)..where((t) => t.id.equals(id)))
        .getSingleOrNull();
  }

  // ===========================================================================
  // 2. PRODUCTS & FEFO BATCH QUERIES
  // ===========================================================================

  /// Inserts a new catalog product
  Future<int> insertProduct(ProductsCompanion product) {
    return db.into(db.products).insert(product);
  }

  /// Inserts a new inventory batch with cost, margin %, and calculated selling price
  Future<int> insertProductBatch(ProductBatchesCompanion batch) {
    return db.into(db.productBatches).insert(batch);
  }

  /// Watches all products with their active batch count
  Stream<List<Product>> watchAllProducts() {
    return (db.select(db.products)
          ..orderBy([(t) => OrderingTerm.asc(t.tradeName)]))
        .watch();
  }

  /// FEFO Query: Fetches available batches for a product sorted strictly by earliest Expiry Date
  Stream<List<ProductBatch>> watchFefoBatches(String productId) {
    return (db.select(db.productBatches)
          ..where((t) =>
              t.productId.equals(productId) &
              t.quantityInStock.isBiggerThanValue(0))
          ..orderBy([(t) => OrderingTerm.asc(t.expiryDate)]))
        .watch();
  }

  /// FEFO Stock Deduction Engine: Automatically consumes earliest expiry batches
  Future<void> deductStockFefo({
    required String productId,
    required int requestedQuantity,
    required String referenceInvoiceNo,
  }) async {
    await db.transaction(() async {
      final availableBatches = await (db.select(db.productBatches)
            ..where((t) =>
                t.productId.equals(productId) &
                t.quantityInStock.isBiggerThanValue(0))
            ..orderBy([(t) => OrderingTerm.asc(t.expiryDate)]))
          .get();

      int remaining = requestedQuantity;

      for (final batch in availableBatches) {
        if (remaining <= 0) break;

        final int deductFromBatch =
            batch.quantityInStock >= remaining ? remaining : batch.quantityInStock;
        final int updatedStock = batch.quantityInStock - deductFromBatch;

        // 1. Update batch stock
        await (db.update(db.productBatches)..where((t) => t.id.equals(batch.id)))
            .write(ProductBatchesCompanion(quantityInStock: Value(updatedStock)));

        // 2. Audit in Inventory Ledger
        await db.into(db.inventoryLedger).insert(
              InventoryLedgerCompanion.insert(
                id: Value(DateTime.now().microsecondsSinceEpoch.toString()),
                batchId: batch.id,
                transactionType: 'DISPENSED_PRESCRIPTION',
                quantityChange: -deductFromBatch,
                referenceId: Value(referenceInvoiceNo),
                notes: Value('FEFO auto-deduction for Invoice #$referenceInvoiceNo'),
              ),
            );

        remaining -= deductFromBatch;
      }

      if (remaining > 0) {
        throw StateError(
            'Insufficient stock! Unable to fulfill $remaining units for product ID $productId');
      }
    });
  }

  // ===========================================================================
  // 3. CLINICAL VISITS & CONSULTATIONS (EMR)
  // ===========================================================================

  /// Records a new clinical visit / consultation entry
  Future<int> createVisit(VisitsCompanion visit) {
    return db.into(db.visits).insert(visit);
  }

  /// Watches all active patient visits in the clinic queue
  Stream<List<Visit>> watchActiveVisits() {
    return (db.select(db.visits)
          ..where((t) => t.status.isNotIn(['COMPLETED']))
          ..orderBy([(t) => OrderingTerm.asc(t.visitDate)]))
        .watch();
  }

  /// Updates queue status (WAITING -> IN_CONSULTATION -> AT_PHARMACY -> AT_CASHIER -> COMPLETED)
  Future<int> updateVisitStatus(String visitId, String status) {
    return (db.update(db.visits)..where((t) => t.id.equals(visitId))).write(
      VisitsCompanion(status: Value(status)),
    );
  }

  // ===========================================================================
  // 4. INVENTORY LEDGER AUDIT TRAIL
  // ===========================================================================

  /// Logs manual stock adjustments, supplier purchases, or spoilage returns
  Future<int> recordStockLedger({
    required String ledgerId,
    required String batchId,
    required String transactionType,
    required int quantityChange,
    String? referenceId,
    String? notes,
  }) {
    return db.into(db.inventoryLedger).insert(
          InventoryLedgerCompanion.insert(
            id: Value(ledgerId),
            batchId: batchId,
            transactionType: transactionType,
            quantityChange: quantityChange,
            referenceId: Value(referenceId),
            notes: Value(notes),
          ),
        );
  }

  // ===========================================================================
  // 5. USER MANAGEMENT & RBAC
  // ===========================================================================

  /// Watches all users
  Stream<List<User>> watchAllUsers() {
    return (db.select(db.users)..orderBy([(t) => OrderingTerm.asc(t.username)])).watch();
  }

  /// Get user by username
  Future<User?> getUserByUsername(String username) {
    return (db.select(db.users)..where((t) => t.username.equals(username))).getSingleOrNull();
  }

  /// Get user by ID
  Future<User?> getUserById(String id) {
    return (db.select(db.users)..where((t) => t.id.equals(id))).getSingleOrNull();
  }

  /// Count total users
  Future<int> countUsers() async {
    final countExp = db.users.id.count();
    final query = db.selectOnly(db.users)..addColumns([countExp]);
    final result = await query.map((row) => row.read(countExp)).getSingle();
    return result ?? 0;
  }

  /// Insert new user
  Future<int> insertUser(UsersCompanion user) {
    return db.into(db.users).insert(user);
  }

  /// Update user details (role, fullName, samaNumber)
  Future<bool> updateUser({
    required String id,
    required String fullName,
    required String role,
    String? samaNumber,
  }) async {
    final count = await (db.update(db.users)..where((t) => t.id.equals(id))).write(
      UsersCompanion(
        fullName: Value(fullName),
        role: Value(role),
        samaNumber: Value(samaNumber),
      ),
    );
    return count > 0;
  }

  /// Update user password hash
  Future<bool> updateUserPassword({
    required String id,
    required String newPasswordHash,
  }) async {
    final count = await (db.update(db.users)..where((t) => t.id.equals(id))).write(
      UsersCompanion(
        passwordHash: Value(newPasswordHash),
      ),
    );
    return count > 0;
  }

  /// Toggle user active status
  Future<bool> toggleUserStatus({
    required String id,
    required bool isActive,
  }) async {
    final count = await (db.update(db.users)..where((t) => t.id.equals(id))).write(
      UsersCompanion(
        isActive: Value(isActive),
      ),
    );
    return count > 0;
  }
}

/// Provider for accessing the ClinicDatabaseDao
final clinicDaoProvider = Provider<ClinicDatabaseDao>((ref) {
  final db = ref.watch(appDatabaseProvider);
  return ClinicDatabaseDao(db);
});

