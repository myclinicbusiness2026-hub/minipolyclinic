import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/database/database_provider.dart';
import '../../../../core/services/printing/thermal_print_service.dart';

/// Billing UI State Representation
class BillingState {
  final List<VisitWithDetails> pendingVisits;
  final VisitWithDetails? selectedVisit;
  final List<InvoiceItemData> invoiceItems;
  final double subtotal;
  final double discountPercent;
  final double discountAmount;
  final double grandTotal;
  final String selectedPaymentMethod; // CASH, KBZ_PAY, WAVE_PAY, CARD
  final bool isProcessingPayment;
  final String? lastGeneratedInvoiceNo;

  const BillingState({
    this.pendingVisits = const [],
    this.selectedVisit,
    this.invoiceItems = const [],
    this.subtotal = 0.0,
    this.discountPercent = 0.0,
    this.discountAmount = 0.0,
    this.grandTotal = 0.0,
    this.selectedPaymentMethod = 'CASH',
    this.isProcessingPayment = false,
    this.lastGeneratedInvoiceNo,
  });

  BillingState copyWith({
    List<VisitWithDetails>? pendingVisits,
    VisitWithDetails? selectedVisit,
    List<InvoiceItemData>? invoiceItems,
    double? subtotal,
    double? discountPercent,
    double? discountAmount,
    double? grandTotal,
    String? selectedPaymentMethod,
    bool? isProcessingPayment,
    String? lastGeneratedInvoiceNo,
  }) {
    return BillingState(
      pendingVisits: pendingVisits ?? this.pendingVisits,
      selectedVisit: selectedVisit ?? this.selectedVisit,
      invoiceItems: invoiceItems ?? this.invoiceItems,
      subtotal: subtotal ?? this.subtotal,
      discountPercent: discountPercent ?? this.discountPercent,
      discountAmount: discountAmount ?? this.discountAmount,
      grandTotal: grandTotal ?? this.grandTotal,
      selectedPaymentMethod: selectedPaymentMethod ?? this.selectedPaymentMethod,
      isProcessingPayment: isProcessingPayment ?? this.isProcessingPayment,
      lastGeneratedInvoiceNo: lastGeneratedInvoiceNo ?? this.lastGeneratedInvoiceNo,
    );
  }
}

class VisitWithDetails {
  final Visit visit;
  final Patient patient;
  final List<InvoiceItemData> items;

  VisitWithDetails({
    required this.visit,
    required this.patient,
    required this.items,
  });
}

class InvoiceItemData {
  final String id;
  final String description;
  final String category; // CONSULTATION, PROCEDURE, MEDICINE
  final String? productId;
  final int quantity;
  final double unitPrice;
  final double totalPrice;

  InvoiceItemData({
    required this.id,
    required this.description,
    required this.category,
    this.productId,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
  });
}

/// Billing Notifier controlling checkout, dynamic discount calculation, and FEFO deduction
class BillingNotifier extends StateNotifier<BillingState> {
  final AppDatabase db;

  BillingNotifier(this.db) : super(const BillingState()) {
    loadPendingBillingVisits();
  }

  /// Loads all visits currently queued at BILLING / AT_CASHIER
  Future<void> loadPendingBillingVisits() async {
    final query = db.select(db.visits).join([
      innerJoin(db.patients, db.patients.id.equalsExp(db.visits.patientId)),
    ])..where(db.visits.status.equals('BILLING') | db.visits.status.equals('AT_CASHIER'));

    final rows = await query.get();

    final List<VisitWithDetails> list = [];
    for (final row in rows) {
      final visit = row.readTable(db.visits);
      final patient = row.readTable(db.patients);

      // Construct itemized details (Consultation + Sample Prescriptions/Procedures)
      final items = <InvoiceItemData>[
        InvoiceItemData(
          id: 'CONS-${visit.id}',
          description: 'Doctor General Consultation Fee',
          category: 'CONSULTATION',
          quantity: 1,
          unitPrice: 5000.0,
          totalPrice: 5000.0,
        ),
      ];

      list.add(VisitWithDetails(
        visit: visit,
        patient: patient,
        items: items,
      ));
    }

    state = state.copyWith(
      pendingVisits: list,
      selectedVisit: state.selectedVisit ?? (list.isNotEmpty ? list.first : null),
    );

    if (state.selectedVisit != null) {
      selectVisit(state.selectedVisit!);
    }
  }

  /// Selects a visit to inspect its itemized charges
  void selectVisit(VisitWithDetails visitDetails) {
    double calculatedSubtotal = 0.0;
    for (final item in visitDetails.items) {
      calculatedSubtotal += item.totalPrice;
    }

    final discountAmt = calculatedSubtotal * (state.discountPercent / 100.0);
    final finalGrandTotal = calculatedSubtotal - discountAmt;

    state = state.copyWith(
      selectedVisit: visitDetails,
      invoiceItems: visitDetails.items,
      subtotal: calculatedSubtotal,
      discountAmount: discountAmt,
      grandTotal: finalGrandTotal > 0 ? finalGrandTotal : 0.0,
    );
  }

  /// Updates discount percentage dynamically
  void setDiscountPercent(double percent) {
    final discountAmt = state.subtotal * (percent / 100.0);
    final finalGrandTotal = state.subtotal - discountAmt;

    state = state.copyWith(
      discountPercent: percent,
      discountAmount: discountAmt,
      grandTotal: finalGrandTotal > 0 ? finalGrandTotal : 0.0,
    );
  }

  /// Updates discount fixed amount dynamically
  void setDiscountAmount(double amount) {
    final percent = state.subtotal > 0 ? (amount / state.subtotal) * 100.0 : 0.0;
    final finalGrandTotal = state.subtotal - amount;

    state = state.copyWith(
      discountPercent: percent,
      discountAmount: amount,
      grandTotal: finalGrandTotal > 0 ? finalGrandTotal : 0.0,
    );
  }

  /// Sets selected payment method
  void setPaymentMethod(String method) {
    state = state.copyWith(selectedPaymentMethod: method);
  }

  /// CHECKOUT LOGIC:
  /// 1. Updates Visit status to 'COMPLETED'
  /// 2. Deducts FEFO Stock Batches in Drift DB
  /// 3. Writes transaction entries to InventoryLedger
  /// 4. Generates Invoice record
  /// 5. Triggers Auto-Thermal Print for Invoice
  Future<String?> confirmPaymentAndCheckout() async {
    final currentVisit = state.selectedVisit;
    if (currentVisit == null) return null;

    state = state.copyWith(isProcessingPayment: true);

    try {
      final now = DateTime.now();
      final invoiceNo = 'INV-${now.year}${now.month.toString().padLeft(2, '0')}-${now.millisecondsSinceEpoch % 10000}';

      await db.transaction(() async {
        // 1. Create Invoice Record
        final invoiceId = 'inv-${now.millisecondsSinceEpoch}';
        await db.into(db.invoices).insert(InvoicesCompanion(
          id: Value(invoiceId),
          invoiceNumber: Value(invoiceNo),
          patientId: Value(currentVisit.patient.id),
          visitId: Value(currentVisit.visit.id),
          subtotal: Value(state.subtotal),
          discount: Value(state.discountAmount),
          grandTotal: Value(state.grandTotal),
          paidAmount: Value(state.grandTotal),
          paymentMethod: Value(state.selectedPaymentMethod),
          isPaid: const Value(true),
          createdAt: Value(now),
        ));

        // 2. Insert Line Items
        for (final item in state.invoiceItems) {
          await db.into(db.invoiceItems).insert(InvoiceItemsCompanion(
            id: Value('item-${DateTime.now().microsecondsSinceEpoch}'),
            invoiceId: Value(invoiceId),
            itemDescription: Value(item.description),
            quantity: Value(item.quantity),
            unitPrice: Value(item.unitPrice),
            totalPrice: Value(item.totalPrice),
          ));

          // 3. Deduct FEFO Stock Batches if item is medicine
          if (item.category == 'MEDICINE' && item.productId != null) {
            // Find active batches for product sorted by earliest expiry
            final availableBatches = await (db.select(db.productBatches)
              ..where((t) => t.productId.equals(item.productId!) & t.quantityInStock.isBiggerThanValue(0))
              ..orderBy([(t) => OrderingTerm.asc(t.expiryDate)])).get();

            int remainingToDeduct = item.quantity;

            for (final batch in availableBatches) {
              if (remainingToDeduct <= 0) break;

              final int deduction = batch.quantityInStock >= remainingToDeduct
                  ? remainingToDeduct
                  : batch.quantityInStock;

              // Deduct from batch
              await (db.update(db.productBatches)..where((t) => t.id.equals(batch.id)))
                  .write(ProductBatchesCompanion(
                quantityInStock: Value(batch.quantityInStock - deduction),
              ));

              // Write entry to InventoryLedger
              await db.into(db.inventoryLedger).insert(InventoryLedgerCompanion(
                id: Value('ldg-${DateTime.now().microsecondsSinceEpoch}'),
                batchId: Value(batch.id),
                transactionType: const Value('DISPENSED_PRESCRIPTION'),
                quantityChange: Value(-deduction),
                referenceId: Value(invoiceNo),
                notes: Value('FEFO auto-dispensed for patient ${currentVisit.patient.fullName}'),
                timestamp: Value(now),
              ));

              remainingToDeduct -= deduction;
            }
          }
        }

        // 4. Update Visit status to COMPLETED
        await (db.update(db.visits)..where((t) => t.id.equals(currentVisit.visit.id)))
            .write(const VisitsCompanion(
          status: Value('COMPLETED'),
        ));
      });

      // 5. Trigger Thermal Print Byte generation
      final receiptItems = state.invoiceItems.map((it) => ThermalReceiptItem(
        description: it.description,
        quantity: it.quantity,
        unitPrice: it.unitPrice,
        totalPrice: it.totalPrice,
      )).toList();

      ThermalPrintService.generateEscPosReceipt(
        clinicName: 'MINIPOLYCLINIC & GENERAL PRACTICE',
        clinicAddress: 'No. 124, Bogyoke Road, Yangon',
        clinicPhone: '09-790001122',
        invoiceNo: invoiceNo,
        dateString: now.toString().substring(0, 16),
        patientName: currentVisit.patient.fullName,
        patientCode: currentVisit.patient.patientCode,
        items: receiptItems,
        subtotal: state.subtotal,
        discount: state.discountAmount,
        grandTotal: state.grandTotal,
        paidAmount: state.grandTotal,
        paymentMethod: state.selectedPaymentMethod,
        cashierName: 'Cashier Daw Hnin Hnin',
      );

      // Refresh list
      await loadPendingBillingVisits();

      state = state.copyWith(
        isProcessingPayment: false,
        lastGeneratedInvoiceNo: invoiceNo,
      );

      return invoiceNo;
    } catch (e) {
      state = state.copyWith(isProcessingPayment: false);
      rethrow;
    }
  }
}

/// Global Riverpod Provider for Billing
final billingProvider = StateNotifierProvider<BillingNotifier, BillingState>((ref) {
  final db = ref.watch(appDatabaseProvider);
  return BillingNotifier(db);
});
