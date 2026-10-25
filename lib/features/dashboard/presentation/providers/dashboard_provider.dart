import 'dart:convert';
import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/database/database_provider.dart';

/// Queue item container pairing Patient and Visit
class PatientQueueItem {
  final Patient patient;
  final Visit visit;
  final Map<String, dynamic> vitals;

  PatientQueueItem({
    required this.patient,
    required this.visit,
    required this.vitals,
  });

  String get queueNumber {
    final idx = patient.patientCode.replaceAll(RegExp(r'[^0-9]'), '');
    final lastTwo = idx.length >= 2 ? idx.substring(idx.length - 2) : '01';
    return '#$lastTwo';
  }

  String get bloodPressure => vitals['bp']?.toString() ?? '120/80';
  String get pulse => vitals['pulse']?.toString() ?? '76 bpm';
  String get temp => vitals['temp']?.toString() ?? '98.6°F';
  String get spo2 => vitals['spo2']?.toString() ?? '98%';
  String get weight => vitals['weight']?.toString() ?? '62 kg';
}

/// Dashboard UI State
class DashboardState {
  final int todayPatientsCount;
  final int waitingCount;
  final int inConsultationCount;
  final int pendingBillingCount;
  final int completedCount;
  final int lowStockAlertCount;

  final List<PatientQueueItem> allQueueItems;
  final List<PatientQueueItem> filteredQueueItems;
  final PatientQueueItem? selectedQueueItem;

  final String statusFilter; // 'ALL', 'WAITING', 'IN_CONSULTATION', 'BILLING', 'COMPLETED'
  final String searchQuery;
  final bool isLoading;
  final String? errorMessage;

  const DashboardState({
    this.todayPatientsCount = 0,
    this.waitingCount = 0,
    this.inConsultationCount = 0,
    this.pendingBillingCount = 0,
    this.completedCount = 0,
    this.lowStockAlertCount = 0,
    this.allQueueItems = const [],
    this.filteredQueueItems = const [],
    this.selectedQueueItem,
    this.statusFilter = 'ALL',
    this.searchQuery = '',
    this.isLoading = false,
    this.errorMessage,
  });

  DashboardState copyWith({
    int? todayPatientsCount,
    int? waitingCount,
    int? inConsultationCount,
    int? pendingBillingCount,
    int? completedCount,
    int? lowStockAlertCount,
    List<PatientQueueItem>? allQueueItems,
    List<PatientQueueItem>? filteredQueueItems,
    PatientQueueItem? selectedQueueItem,
    bool clearSelected = false,
    String? statusFilter,
    String? searchQuery,
    bool? isLoading,
    String? errorMessage,
  }) {
    return DashboardState(
      todayPatientsCount: todayPatientsCount ?? this.todayPatientsCount,
      waitingCount: waitingCount ?? this.waitingCount,
      inConsultationCount: inConsultationCount ?? this.inConsultationCount,
      pendingBillingCount: pendingBillingCount ?? this.pendingBillingCount,
      completedCount: completedCount ?? this.completedCount,
      lowStockAlertCount: lowStockAlertCount ?? this.lowStockAlertCount,
      allQueueItems: allQueueItems ?? this.allQueueItems,
      filteredQueueItems: filteredQueueItems ?? this.filteredQueueItems,
      selectedQueueItem: clearSelected ? null : (selectedQueueItem ?? this.selectedQueueItem),
      statusFilter: statusFilter ?? this.statusFilter,
      searchQuery: searchQuery ?? this.searchQuery,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

/// DashboardNotifier
class DashboardNotifier extends StateNotifier<DashboardState> {
  final AppDatabase db;

  DashboardNotifier(this.db) : super(const DashboardState()) {
    initAndSeedData();
  }

  Future<void> initAndSeedData() async {
    state = state.copyWith(isLoading: true);
    try {
      await _seedDatabaseIfEmpty();
      await refreshData();
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  /// Refreshes counters, queue, and inventory alerts
  Future<void> refreshData() async {
    try {
      // 1. Fetch all visits joined with patients
      final query = db.select(db.visits).join([
        innerJoin(db.patients, db.patients.id.equalsExp(db.visits.patientId)),
      ])..orderBy([OrderingTerm.desc(db.visits.visitDate)]);

      final rows = await query.get();

      final List<PatientQueueItem> items = [];
      for (final row in rows) {
        final visit = row.readTable(db.visits);
        final patient = row.readTable(db.patients);

        Map<String, dynamic> vitals = {};
        if (visit.vitalsJson != null && visit.vitalsJson!.isNotEmpty) {
          try {
            vitals = jsonDecode(visit.vitalsJson!) as Map<String, dynamic>;
          } catch (_) {}
        }

        items.add(PatientQueueItem(
          patient: patient,
          visit: visit,
          vitals: vitals,
        ));
      }

      // 2. Count statuses
      final waiting = items.countByStatus('WAITING');
      final inConsultation = items.countByStatus('IN_CONSULTATION');
      final billing = items.countByStatus('BILLING', 'AT_CASHIER');
      final completed = items.countByStatus('COMPLETED');
      final todayCount = items.length;

      // 3. Count Low stock items from productBatches
      final batches = await db.select(db.productBatches).get();
      final lowStockCount = batches.where((b) => b.quantityInStock <= 15).length;

      final filtered = _applyFilters(items, state.statusFilter, state.searchQuery);

      state = state.copyWith(
        isLoading: false,
        todayPatientsCount: todayCount,
        waitingCount: waiting,
        inConsultationCount: inConsultation,
        pendingBillingCount: billing,
        completedCount: completed,
        lowStockAlertCount: lowStockCount,
        allQueueItems: items,
        filteredQueueItems: filtered,
        selectedQueueItem: state.selectedQueueItem ?? (filtered.isNotEmpty ? filtered.first : null),
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  void setStatusFilter(String filter) {
    final filtered = _applyFilters(state.allQueueItems, filter, state.searchQuery);
    state = state.copyWith(
      statusFilter: filter,
      filteredQueueItems: filtered,
      selectedQueueItem: filtered.isNotEmpty ? filtered.first : null,
    );
  }

  void setSearchQuery(String query) {
    final filtered = _applyFilters(state.allQueueItems, state.statusFilter, query);
    state = state.copyWith(
      searchQuery: query,
      filteredQueueItems: filtered,
      selectedQueueItem: filtered.isNotEmpty ? filtered.first : null,
    );
  }

  void selectQueueItem(PatientQueueItem? item) {
    state = state.copyWith(selectedQueueItem: item);
  }

  /// Updates queue status for a visit
  Future<void> updateVisitStatus(String visitId, String newStatus) async {
    await (db.update(db.visits)..where((t) => t.id.equals(visitId))).write(
      VisitsCompanion(status: Value(newStatus)),
    );
    await refreshData();
  }

  /// Registers a new patient and enqueues them into WAITING status
  Future<PatientQueueItem> registerPatientAndVisit({
    required String fullName,
    required String gender,
    required int age,
    required String phone,
    String? address,
    String? allergies,
    required String chiefComplaint,
    String? bp,
    String? pulse,
    String? temp,
    String? spo2,
    String? weight,
  }) async {
    final timestamp = DateTime.now();
    final patientId = 'PAT-${timestamp.millisecondsSinceEpoch}';
    final visitId = 'VISIT-${timestamp.millisecondsSinceEpoch}';
    final count = state.todayPatientsCount + 1;
    final patientCode = 'HN-2026-${count.toString().padLeft(4, '0')}';

    final vitalsMap = {
      'bp': bp?.isNotEmpty == true ? bp : '120/80',
      'pulse': pulse?.isNotEmpty == true ? pulse : '78 bpm',
      'temp': temp?.isNotEmpty == true ? temp : '98.6°F',
      'spo2': spo2?.isNotEmpty == true ? spo2 : '99%',
      'weight': weight?.isNotEmpty == true ? weight : '60 kg',
    };

    // Insert Patient
    final patientCompanion = PatientsCompanion(
      id: Value(patientId),
      patientCode: Value(patientCode),
      fullName: Value(fullName),
      gender: Value(gender),
      age: Value(age),
      phone: Value(phone),
      address: Value(address),
      allergies: Value(allergies ?? ''),
      registeredAt: Value(timestamp),
    );
    await db.into(db.patients).insert(patientCompanion);

    // Insert Visit
    final visitCompanion = VisitsCompanion(
      id: Value(visitId),
      patientId: Value(patientId),
      doctorId: const Value('DOC-01'),
      status: const Value('WAITING'),
      vitalsJson: Value(jsonEncode(vitalsMap)),
      chiefComplaint: Value(chiefComplaint),
      visitDate: Value(timestamp),
    );
    await db.into(db.visits).insert(visitCompanion);

    await refreshData();

    final createdPatient = await (db.select(db.patients)..where((t) => t.id.equals(patientId))).getSingle();
    final createdVisit = await (db.select(db.visits)..where((t) => t.id.equals(visitId))).getSingle();

    return PatientQueueItem(
      patient: createdPatient,
      visit: createdVisit,
      vitals: vitalsMap,
    );
  }

  List<PatientQueueItem> _applyFilters(
    List<PatientQueueItem> items,
    String filter,
    String query,
  ) {
    return items.where((item) {
      final matchesFilter = switch (filter) {
        'ALL' => true,
        'WAITING' => item.visit.status == 'WAITING',
        'IN_CONSULTATION' => item.visit.status == 'IN_CONSULTATION',
        'BILLING' => item.visit.status == 'BILLING' || item.visit.status == 'AT_CASHIER',
        'COMPLETED' => item.visit.status == 'COMPLETED',
        _ => true,
      };

      final q = query.trim().toLowerCase();
      final matchesQuery = q.isEmpty ||
          item.patient.fullName.toLowerCase().contains(q) ||
          item.patient.patientCode.toLowerCase().contains(q) ||
          item.patient.phone.contains(q) ||
          (item.visit.chiefComplaint?.toLowerCase().contains(q) ?? false);

      return matchesFilter && matchesQuery;
    }).toList();
  }

  /// Seeds demo data if database tables are unpopulated
  Future<void> _seedDatabaseIfEmpty() async {
    final existingPatients = await db.select(db.patients).get();
    if (existingPatients.isNotEmpty) return;

    final now = DateTime.now();

    // 1. Seed Products
    final p1 = 'MED-001';
    final p2 = 'MED-002';
    final p3 = 'MED-003';
    final p4 = 'MED-004';
    final p5 = 'MED-005';

    await db.into(db.products).insert(ProductsCompanion.insert(
          id: p1,
          tradeName: 'Augmentin 625mg',
          genericName: 'Amoxicillin + Clavulanic acid',
          category: 'Antibiotics',
          unitType: 'Tablet',
          minStockWarning: const Value(20),
        ));
    await db.into(db.products).insert(ProductsCompanion.insert(
          id: p2,
          tradeName: 'Panadol Extra 500mg',
          genericName: 'Paracetamol + Caffeine',
          category: 'Analgesics',
          unitType: 'Tablet',
          minStockWarning: const Value(30),
        ));
    await db.into(db.products).insert(ProductsCompanion.insert(
          id: p3,
          tradeName: 'Nexium 40mg',
          genericName: 'Esomeprazole',
          category: 'Gastrointestinal',
          unitType: 'Capsule',
          minStockWarning: const Value(15),
        ));
    await db.into(db.products).insert(ProductsCompanion.insert(
          id: p4,
          tradeName: 'Amlodipine 5mg',
          genericName: 'Amlodipine Besylate',
          category: 'Cardiovascular',
          unitType: 'Tablet',
          minStockWarning: const Value(20),
        ));
    await db.into(db.products).insert(ProductsCompanion.insert(
          id: p5,
          tradeName: 'Cetirizine 10mg',
          genericName: 'Cetirizine Hydrochloride',
          category: 'Antihistamine',
          unitType: 'Tablet',
          minStockWarning: const Value(15),
        ));

    // 2. Seed Product Batches (FEFO demonstration)
    await db.into(db.productBatches).insert(ProductBatchesCompanion.insert(
          id: 'BATCH-AUG-01',
          productId: p1,
          batchNumber: 'AUG2401',
          manufacturer: const Value('GSK Pharmaceuticals'),
          expiryDate: now.add(const Duration(days: 45)), // Early Expiry
          costPrice: 850.0,
          profitMarginPercent: const Value(25.0),
          sellingPrice: 1062.5,
          quantityInStock: const Value(8), // Low stock alert!
        ));
    await db.into(db.productBatches).insert(ProductBatchesCompanion.insert(
          id: 'BATCH-AUG-02',
          productId: p1,
          batchNumber: 'AUG2402',
          manufacturer: const Value('GSK Pharmaceuticals'),
          expiryDate: now.add(const Duration(days: 280)),
          costPrice: 850.0,
          profitMarginPercent: const Value(25.0),
          sellingPrice: 1062.5,
          quantityInStock: const Value(50),
        ));
    await db.into(db.productBatches).insert(ProductBatchesCompanion.insert(
          id: 'BATCH-PAN-01',
          productId: p2,
          batchNumber: 'PAN9921',
          manufacturer: const Value('Haleon UK'),
          expiryDate: now.add(const Duration(days: 120)),
          costPrice: 200.0,
          profitMarginPercent: const Value(30.0),
          sellingPrice: 260.0,
          quantityInStock: const Value(120),
        ));
    await db.into(db.productBatches).insert(ProductBatchesCompanion.insert(
          id: 'BATCH-NEX-01',
          productId: p3,
          batchNumber: 'NEX5532',
          manufacturer: const Value('AstraZeneca'),
          expiryDate: now.add(const Duration(days: 30)), // Near Expiry!
          costPrice: 1200.0,
          profitMarginPercent: const Value(20.0),
          sellingPrice: 1440.0,
          quantityInStock: const Value(10), // Low stock alert!
        ));

    // 3. Seed Procedures
    await db.into(db.procedures).insert(ProceduresCompanion.insert(
          id: 'PROC-01',
          name: 'Minor Surgical Wound Dressing & Suture Check',
          baseProcedureFee: const Value(8000.0),
          riskNursingFee: const Value(3000.0),
        ));
    await db.into(db.procedures).insert(ProceduresCompanion.insert(
          id: 'PROC-02',
          name: 'Ultrasonic Nebulization Therapy',
          baseProcedureFee: const Value(6000.0),
          riskNursingFee: const Value(2000.0),
        ));

    // 4. Seed Demo Patients
    final demoPatients = [
      {
        'id': 'PAT-01',
        'code': 'HN-2026-0001',
        'name': 'U Kyaw Myint',
        'gender': 'MALE',
        'age': 54,
        'phone': '09-421008761',
        'allergies': 'Penicillin, Sulfa drugs',
        'status': 'WAITING',
        'complaint': 'Acute high fever (39°C), chills, and body aches for 2 days.',
        'bp': '135/88',
        'pulse': '88 bpm',
        'temp': '101.4°F',
      },
      {
        'id': 'PAT-02',
        'code': 'HN-2026-0002',
        'name': 'Daw Hla Hla Win',
        'gender': 'FEMALE',
        'age': 42,
        'phone': '09-250123899',
        'allergies': '',
        'status': 'IN_CONSULTATION',
        'complaint': 'Productive cough with yellow sputum and chest tightness.',
        'bp': '125/82',
        'pulse': '76 bpm',
        'temp': '99.1°F',
      },
      {
        'id': 'PAT-03',
        'code': 'HN-2026-0003',
        'name': 'Ko Aung Ko Oo',
        'gender': 'MALE',
        'age': 29,
        'phone': '09-799823412',
        'allergies': 'Aspirin',
        'status': 'BILLING',
        'complaint': 'Superficial laceration on right forearm, minor wound suturing done.',
        'bp': '118/75',
        'pulse': '72 bpm',
        'temp': '98.4°F',
      },
      {
        'id': 'PAT-04',
        'code': 'HN-2026-0004',
        'name': 'Ma Su Myat Noe',
        'gender': 'FEMALE',
        'age': 23,
        'phone': '09-978124567',
        'allergies': '',
        'status': 'WAITING',
        'complaint': 'Throat discomfort, dysphagia, and nasal congestion.',
        'bp': '110/70',
        'pulse': '70 bpm',
        'temp': '98.8°F',
      },
      {
        'id': 'PAT-05',
        'code': 'HN-2026-0005',
        'name': 'U Than Tun',
        'gender': 'MALE',
        'age': 68,
        'phone': '09-5123984',
        'allergies': '',
        'status': 'COMPLETED',
        'complaint': 'Routine hypertensive chronic care follow-up.',
        'bp': '140/90',
        'pulse': '74 bpm',
        'temp': '98.2°F',
      },
    ];

    for (final p in demoPatients) {
      final pid = p['id'] as String;
      final vid = 'VISIT-$pid';

      await db.into(db.patients).insert(PatientsCompanion.insert(
            id: pid,
            patientCode: p['code'] as String,
            fullName: p['name'] as String,
            gender: p['gender'] as String,
            age: p['age'] as int,
            phone: p['phone'] as String,
            allergies: Value(p['allergies'] as String),
          ));

      final vitalsJson = jsonEncode({
        'bp': p['bp'],
        'pulse': p['pulse'],
        'temp': p['temp'],
        'spo2': '98%',
        'weight': '64 kg',
      });

      await db.into(db.visits).insert(VisitsCompanion.insert(
            id: vid,
            patientId: pid,
            status: Value(p['status'] as String),
            vitalsJson: Value(vitalsJson),
            chiefComplaint: Value(p['complaint'] as String),
            diagnosis: Value(p['status'] == 'BILLING' || p['status'] == 'COMPLETED' ? 'Acute Upper Respiratory Infection' : null),
          ));

      if (p['status'] == 'BILLING') {
        // Seed invoice for billing screen test
        final invId = 'INV-2026-0001';
        await db.into(db.invoices).insert(InvoicesCompanion.insert(
              id: invId,
              invoiceNumber: 'INV-2026-0001',
              patientId: pid,
              visitId: Value(vid),
              subtotal: const Value(19000.0),
              discount: const Value(0.0),
              grandTotal: const Value(19000.0),
              isPaid: const Value(false),
            ));
      }
    }
  }
}

extension on List<PatientQueueItem> {
  int countByStatus(String status1, [String? status2]) {
    return where((i) => i.visit.status == status1 || (status2 != null && i.visit.status == status2)).length;
  }
}

/// Global Riverpod Provider for Dashboard
final dashboardProvider = StateNotifierProvider<DashboardNotifier, DashboardState>((ref) {
  final db = ref.watch(appDatabaseProvider);
  return DashboardNotifier(db);
});
