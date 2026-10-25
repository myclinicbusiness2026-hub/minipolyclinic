import 'package:drift/drift.dart' as drift;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/database/database_provider.dart';
import '../../../../core/theme/app_theme.dart';

/// Pharmacy Inventory & Stock-In Screen
///
/// Features:
/// 1. FEFO Batch Inventory (Sorted strictly by Expiry Date)
/// 2. Dynamic % Margin Calculator (Cost Price -> Dynamic Margin % -> Selling Price)
/// 3. Stock-In / Inward Purchase Batch Reception Modal
/// 4. Expiry Date & Low Stock Badges
class PharmacyScreen extends ConsumerStatefulWidget {
  const PharmacyScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<PharmacyScreen> createState() => _PharmacyScreenState();
}

class _PharmacyScreenState extends ConsumerState<PharmacyScreen> {
  String _searchFilter = '';

  @override
  Widget build(BuildContext context) {
    final db = ref.watch(appDatabaseProvider);

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
            Icon(Icons.medication_rounded, color: AppTheme.primaryGreen, size: 20),
            SizedBox(width: 8),
            Text(
              'Pharmacy Inventory & FEFO Stock',
              style: TextStyle(color: AppTheme.slateDark, fontWeight: FontWeight.bold, fontSize: 15),
            ),
            SizedBox(width: 8),
            Text(
              'ဆေးဝါးသိုလှောင်ရုံနှင့် စာရင်း',
              style: TextStyle(color: AppTheme.slateMuted, fontSize: 11),
            ),
          ],
        ),
        actions: [
          ElevatedButton.icon(
            onPressed: () => _openStockInModal(context, db),
            icon: const Icon(Icons.add_shopping_cart, size: 14, color: AppTheme.cardPureWhite),
            label: const Text('+ Inward Stock-In', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppTheme.cardPureWhite)),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primaryGreen,
              elevation: 0,
              minimumSize: const Size(0, 36),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppTheme.buttonRadius)),
            ),
          ),
          const SizedBox(width: 14),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(10.0),
        child: Column(
          children: [
            // Search & Filter Bar
            Card(
              elevation: 0,
              color: AppTheme.cardPureWhite,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppTheme.cardRadius),
                side: BorderSide(color: AppTheme.subtleBorder),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                child: Row(
                  children: [
                    const Icon(Icons.search, size: 16, color: AppTheme.slateMuted),
                    const SizedBox(width: 6),
                    Expanded(
                      child: TextField(
                        onChanged: (val) => setState(() => _searchFilter = val.trim().toLowerCase()),
                        decoration: const InputDecoration(
                          hintText: 'Search medicine name, batch number, or manufacturer...',
                          hintStyle: TextStyle(fontSize: 11.5, color: AppTheme.slateMuted),
                          border: InputBorder.none,
                          isDense: true,
                          contentPadding: EdgeInsets.symmetric(vertical: 6),
                        ),
                        style: const TextStyle(fontSize: 11.5, color: AppTheme.slateDark),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                      decoration: BoxDecoration(
                        color: AppTheme.softGreenContainer,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'FEFO Active (First-Expiry, First-Out)',
                        style: TextStyle(fontSize: 9.5, fontWeight: FontWeight.bold, color: AppTheme.primaryGreen),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 8),

            // FEFO Batches List
            Expanded(
              child: StreamBuilder<List<TypedResult>>(
                stream: (db.select(db.productBatches).join([
                  drift.innerJoin(db.products, db.products.id.equalsExp(db.productBatches.productId)),
                ])..orderBy([
                  drift.OrderingTerm.asc(db.productBatches.expiryDate), // STRICT FEFO SORT
                ])).watch(),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator(color: AppTheme.primaryGreen));
                  }

                  final rows = snapshot.data ?? [];
                  final filteredRows = rows.where((r) {
                    if (_searchFilter.isEmpty) return true;
                    final p = r.readTable(db.products);
                    final b = r.readTable(db.productBatches);
                    return p.tradeName.toLowerCase().contains(_searchFilter) ||
                        p.genericName.toLowerCase().contains(_searchFilter) ||
                        b.batchNumber.toLowerCase().contains(_searchFilter) ||
                        (b.manufacturer?.toLowerCase().contains(_searchFilter) ?? false);
                  }).toList();

                  if (filteredRows.isEmpty) {
                    return const Center(
                      child: Text('No batches found matching filter.', style: TextStyle(color: AppTheme.slateMuted, fontSize: 12)),
                    );
                  }

                  return ListView.separated(
                    itemCount: filteredRows.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 6),
                    itemBuilder: (context, idx) {
                      final row = filteredRows[idx];
                      final product = row.readTable(db.products);
                      final batch = row.readTable(db.productBatches);

                      final now = DateTime.now();
                      final daysUntilExpiry = batch.expiryDate.difference(now).inDays;
                      final isNearExpiry = daysUntilExpiry <= 60;
                      final isLowStock = batch.quantityInStock <= 15;

                      return Card(
                        elevation: 0,
                        color: AppTheme.cardPureWhite,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppTheme.cardRadius),
                          side: BorderSide(
                            color: isNearExpiry
                                ? AppTheme.statusCritical.withOpacity(0.5)
                                : (isLowStock ? AppTheme.statusWarning.withOpacity(0.5) : AppTheme.subtleBorder),
                          ),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                          child: Row(
                            children: [
                              // Priority Rank Icon
                              Container(
                                width: 32,
                                height: 32,
                                decoration: BoxDecoration(
                                  color: isNearExpiry
                                      ? const Color(0xFFFEE2E2)
                                      : (isLowStock ? const Color(0xFFFEF3C7) : AppTheme.softGreenContainer),
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                alignment: Alignment.Center,
                                child: Text(
                                  '#${idx + 1}',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 12,
                                    color: isNearExpiry ? AppTheme.statusCritical : AppTheme.primaryGreen,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),

                              // Drug info
                              Expanded(
                                flex: 3,
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Text(
                                          product.tradeName,
                                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12.5, color: AppTheme.slateDark),
                                        ),
                                        const SizedBox(width: 6),
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                                          decoration: BoxDecoration(color: const Color(0xFFF1F5F9), borderRadius: BorderRadius.circular(4)),
                                          child: Text(
                                            product.category,
                                            style: const TextStyle(fontSize: 9.5, color: AppTheme.slateMedium),
                                          ),
                                        ),
                                      ],
                                    ),
                                    Text(
                                      'Generic: ${product.genericName} • Mfr: ${batch.manufacturer ?? "Unknown"}',
                                      style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMuted),
                                    ),
                                    Text(
                                      'Batch No: ${batch.batchNumber}',
                                      style: const TextStyle(fontSize: 9.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
                                    ),
                                  ],
                                ),
                              ),

                              // Expiry & FEFO Status
                              Expanded(
                                flex: 2,
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Icon(
                                          Icons.calendar_today,
                                          size: 11,
                                          color: isNearExpiry ? AppTheme.statusCritical : AppTheme.slateMuted,
                                        ),
                                        const SizedBox(width: 4),
                                        Text(
                                          'Exp: ${batch.expiryDate.toLocal().toString().split(' ')[0]}',
                                          style: TextStyle(
                                            fontSize: 10.5,
                                            fontWeight: FontWeight.bold,
                                            color: isNearExpiry ? AppTheme.statusCritical : AppTheme.slateDark,
                                          ),
                                        ),
                                      ],
                                    ),
                                    Text(
                                      daysUntilExpiry <= 0
                                          ? 'EXPIRED'
                                          : (isNearExpiry ? '⚠️ Near Expiry ($daysUntilExpiry days left)' : '$daysUntilExpiry days remaining'),
                                      style: TextStyle(
                                        fontSize: 9.5,
                                        color: isNearExpiry ? AppTheme.statusCritical : AppTheme.statusSuccess,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ],
                                ),
                              ),

                              // Stock & Pricing (Cost, Dynamic Margin %, Selling)
                              Expanded(
                                flex: 3,
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  children: [
                                    Column(
                                      crossAxisAlignment: CrossAxisAlignment.end,
                                      children: [
                                        Text(
                                          'Cost: Ks ${batch.costPrice.toStringAsFixed(0)} (+${batch.profitMarginPercent.toStringAsFixed(0)}% Margin)',
                                          style: const TextStyle(fontSize: 9.5, color: AppTheme.slateMuted),
                                        ),
                                        Text(
                                          'Ks ${batch.sellingPrice.toStringAsFixed(0)}',
                                          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.primaryGreen),
                                        ),
                                        Text(
                                          'Stock: ${batch.quantityInStock} ${product.unitType}s',
                                          style: TextStyle(
                                            fontSize: 10.5,
                                            fontWeight: FontWeight.bold,
                                            color: isLowStock ? AppTheme.statusWarning : AppTheme.slateDark,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Stock-In Inward Purchase Modal with Dynamic % Margin Calculator
  void _openStockInModal(BuildContext context, AppDatabase db) {
    final batchNumCtrl = TextEditingController(text: 'BAT-2026-${DateTime.now().millisecondsSinceEpoch % 10000}');
    final mfrCtrl = TextEditingController(text: 'Mega Lifesciences');
    final costCtrl = TextEditingController(text: '500');
    final marginCtrl = TextEditingController(text: '25'); // 25% margin default
    final qtyCtrl = TextEditingController(text: '100');

    double sellingPrice = 625.0; // 500 * 1.25
    DateTime expiryDate = DateTime.now().add(const Duration(days: 365));

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) {
          void recalculateSellingPrice() {
            final cost = double.tryParse(costCtrl.text) ?? 0.0;
            final margin = double.tryParse(marginCtrl.text) ?? 0.0;
            setModalState(() {
              sellingPrice = cost * (1 + (margin / 100));
            });
          }

          return AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppTheme.cardRadius)),
            title: Row(
              children: const [
                Icon(Icons.inventory_2, color: AppTheme.primaryGreen, size: 20),
                SizedBox(width: 8),
                Text('Inward Purchase Batch (Stock-In)', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
              ],
            ),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Select Product: Augmentin 625mg (Antibiotics)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11.5)),
                  const SizedBox(height: 8),
                  TextField(
                    controller: batchNumCtrl,
                    style: const TextStyle(fontSize: 12),
                    decoration: const InputDecoration(labelText: 'Batch Number *', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: mfrCtrl,
                    style: const TextStyle(fontSize: 12),
                    decoration: const InputDecoration(labelText: 'Manufacturer / Supplier', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)),
                  ),
                  const SizedBox(height: 8),

                  // Expiry Picker
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Expiry Date: ${expiryDate.toLocal().toString().split(' ')[0]}', style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold)),
                      TextButton.icon(
                        onPressed: () async {
                          final picked = await showDatePicker(
                            context: context,
                            initialDate: expiryDate,
                            firstDate: DateTime.now(),
                            lastDate: DateTime.now().add(const Duration(days: 1825)),
                          );
                          if (picked != null) setModalState(() => expiryDate = picked);
                        },
                        icon: const Icon(Icons.date_range, size: 14, color: AppTheme.primaryGreen),
                        label: const Text('Change', style: TextStyle(fontSize: 11, color: AppTheme.primaryGreen)),
                      ),
                    ],
                  ),

                  Divider(height: 12, color: AppTheme.subtleBorder),

                  // Dynamic % Margin Calculator
                  const Text('Dynamic Profit Margin Calculator:', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark)),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: costCtrl,
                          keyboardType: TextInputType.number,
                          onChanged: (_) => recalculateSellingPrice(),
                          style: const TextStyle(fontSize: 12),
                          decoration: const InputDecoration(labelText: 'Cost Price (Ks)', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: marginCtrl,
                          keyboardType: TextInputType.number,
                          onChanged: (_) => recalculateSellingPrice(),
                          style: const TextStyle(fontSize: 12),
                          decoration: const InputDecoration(labelText: 'Margin (%)', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)),
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 8),

                  // Computed Selling Price Card
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(color: AppTheme.softGreenContainer, borderRadius: BorderRadius.circular(6)),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Calculated Selling Price:', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark)),
                        Text(
                          'Ks ${sellingPrice.toStringAsFixed(0)}',
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.primaryGreen),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 8),
                  TextField(
                    controller: qtyCtrl,
                    keyboardType: TextInputType.number,
                    style: const TextStyle(fontSize: 12),
                    decoration: const InputDecoration(labelText: 'Inward Quantity', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancel', style: TextStyle(fontSize: 11.5))),
              ElevatedButton(
                onPressed: () async {
                  final batchId = 'BATCH-${DateTime.now().millisecondsSinceEpoch}';
                  await db.into(db.productBatches).insert(
                        ProductBatchesCompanion.insert(
                          id: batchId,
                          productId: 'MED-001',
                          batchNumber: batchNumCtrl.text.trim(),
                          manufacturer: drift.Value(mfrCtrl.text.trim()),
                          expiryDate: expiryDate,
                          costPrice: double.tryParse(costCtrl.text.trim()) ?? 500.0,
                          profitMarginPercent: drift.Value(double.tryParse(marginCtrl.text.trim()) ?? 25.0),
                          sellingPrice: sellingPrice,
                          quantityInStock: drift.Value(int.tryParse(qtyCtrl.text.trim()) ?? 100),
                        ),
                      );

                  // Audit Trail Ledger
                  await db.into(db.inventoryLedger).insert(
                        InventoryLedgerCompanion.insert(
                          id: 'LEDGER-${DateTime.now().millisecondsSinceEpoch}',
                          batchId: batchId,
                          transactionType: 'INWARD_PURCHASE',
                          quantityChange: int.tryParse(qtyCtrl.text.trim()) ?? 100,
                          notes: drift.Value('Stock-In Batch ${batchNumCtrl.text.trim()}'),
                        ),
                      );

                  if (mounted) {
                    Navigator.of(ctx).pop();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Stock inward saved & FEFO inventory updated.'), backgroundColor: AppTheme.statusSuccess),
                    );
                  }
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryGreen,
                  minimumSize: const Size(0, 36),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppTheme.buttonRadius)),
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                ),
                child: const Text('Confirm Stock-In', style: TextStyle(color: AppTheme.cardPureWhite, fontSize: 11.5)),
              ),
            ],
          );
        },
      ),
    );
  }
}

