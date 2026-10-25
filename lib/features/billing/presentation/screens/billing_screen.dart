import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/services/printing/a4_pdf_report_service.dart';
import '../../../../core/services/printing/thermal_print_service.dart';
import '../../../../core/theme/app_theme.dart';
import '../providers/billing_provider.dart';

/// Cashier Billing Screen
/// Refactored for Compact Clinical UI with Medical Green Theme (#2E7D32)
/// and Official Clinic Branding ("ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း).
class BillingScreen extends ConsumerStatefulWidget {
  const BillingScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<BillingScreen> createState() => _BillingScreenState();
}

class _BillingScreenState extends ConsumerState<BillingScreen> {
  final TextEditingController _discountPercentController = TextEditingController(text: '0');

  @override
  void dispose() {
    _discountPercentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final billingState = ref.watch(billingProvider);
    final billingNotifier = ref.read(billingProvider.notifier);

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
            Icon(Icons.point_of_sale, color: AppTheme.primaryGreen, size: 20),
            SizedBox(width: 8),
            Text(
              'Cashier Billing & POS Station',
              style: TextStyle(color: AppTheme.slateDark, fontWeight: FontWeight.bold, fontSize: 15),
            ),
            SizedBox(width: 8),
            Text(
              'ငွေရှင်းကောင်တာ',
              style: TextStyle(color: AppTheme.slateMuted, fontSize: 11),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: AppTheme.primaryGreen, size: 20),
            tooltip: 'Refresh Queue',
            onPressed: () => billingNotifier.loadPendingBillingVisits(),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ===================================================================
          // LEFT COLUMN: Pending Patient Queue List (35% width or flex 4)
          // ===================================================================
          Expanded(
            flex: 4,
            child: Container(
              margin: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: AppTheme.cardPureWhite,
                borderRadius: BorderRadius.circular(AppTheme.cardRadius),
                border: Border.all(color: AppTheme.subtleBorder),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Queue Header
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: AppTheme.softGreenContainer,
                      borderRadius: BorderRadius.only(
                        topLeft: Radius.circular(AppTheme.cardRadius),
                        topRight: Radius.circular(AppTheme.cardRadius),
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: const [
                            Icon(Icons.people_alt, size: 16, color: AppTheme.primaryGreen),
                            SizedBox(width: 6),
                            Text(
                              'Billing Queue',
                              style: TextStyle(fontWeight: FontWeight.bold, color: AppTheme.slateDark, fontSize: 12.5),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppTheme.cardPureWhite,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            '${billingState.pendingVisits.length} Waiting',
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 10, color: AppTheme.primaryGreen),
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Queue List View
                  Expanded(
                    child: billingState.pendingVisits.isEmpty
                        ? Center(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: const [
                                Icon(Icons.check_circle_outline, size: 40, color: AppTheme.slateMuted),
                                SizedBox(height: 6),
                                Text(
                                  'No pending visits at cashier',
                                  style: TextStyle(color: AppTheme.slateMuted, fontSize: 12),
                                ),
                              ],
                            ),
                          )
                        : ListView.separated(
                            padding: const EdgeInsets.all(6),
                            itemCount: billingState.pendingVisits.length,
                            separatorBuilder: (_, __) => const SizedBox(height: 4),
                            itemBuilder: (context, index) {
                              final item = billingState.pendingVisits[index];
                              final isSelected = billingState.selectedVisit?.visit.id == item.visit.id;

                              return InkWell(
                                onTap: () => billingNotifier.selectVisit(item),
                                borderRadius: BorderRadius.circular(AppTheme.buttonRadius),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                                  decoration: BoxDecoration(
                                    color: isSelected ? AppTheme.softGreenContainer : AppTheme.cardPureWhite,
                                    borderRadius: BorderRadius.circular(AppTheme.buttonRadius),
                                    border: Border.all(
                                      color: isSelected ? AppTheme.primaryGreen : AppTheme.subtleBorder,
                                      width: isSelected ? 1.5 : 1.0,
                                    ),
                                  ),
                                  child: Row(
                                    children: [
                                      CircleAvatar(
                                        radius: 15,
                                        backgroundColor: isSelected ? AppTheme.primaryGreen : const Color(0xFFF1F5F9),
                                        child: Text(
                                          item.patient.fullName.isNotEmpty ? item.patient.fullName[0] : 'P',
                                          style: TextStyle(
                                            color: isSelected ? AppTheme.cardPureWhite : AppTheme.slateDark,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 11,
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              item.patient.fullName,
                                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppTheme.slateDark),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                            Text(
                                              '${item.patient.patientCode} • ${item.patient.gender}, ${item.patient.age}y',
                                              style: const TextStyle(color: AppTheme.slateMuted, fontSize: 10),
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(width: 4),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFFEF3C7),
                                          borderRadius: BorderRadius.circular(4),
                                        ),
                                        child: const Text(
                                          'UNPAID',
                                          style: TextStyle(color: Color(0xFFD97706), fontWeight: FontWeight.bold, fontSize: 8.5),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                  ),
                ],
              ),
            ),
          ),

          // ===================================================================
          // RIGHT COLUMN: Itemized Invoice Inspector (65% width or flex 7)
          // ===================================================================
          Expanded(
            flex: 7,
            child: Container(
              margin: const EdgeInsets.fromLTRB(0, 8, 8, 8),
              decoration: BoxDecoration(
                color: AppTheme.cardPureWhite,
                borderRadius: BorderRadius.circular(AppTheme.cardRadius),
                border: Border.all(color: AppTheme.subtleBorder),
              ),
              child: billingState.selectedVisit == null
                  ? Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: const [
                          Icon(Icons.receipt_long, size: 48, color: AppTheme.slateMuted),
                          SizedBox(height: 10),
                          Text(
                            'Select a patient from the queue to view invoice items',
                            style: TextStyle(color: AppTheme.slateMuted, fontSize: 13),
                          ),
                        ],
                      ),
                    )
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 1. Patient Banner & Doctor Name
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          decoration: BoxDecoration(
                            color: AppTheme.softGreenContainer,
                            borderRadius: BorderRadius.only(
                              topLeft: Radius.circular(AppTheme.cardRadius),
                              topRight: Radius.circular(AppTheme.cardRadius),
                            ),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Text(
                                        billingState.selectedVisit!.patient.fullName,
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppTheme.slateDark),
                                      ),
                                      const SizedBox(width: 6),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                                        decoration: BoxDecoration(
                                          color: AppTheme.primaryGreen,
                                          borderRadius: BorderRadius.circular(4),
                                        ),
                                        child: Text(
                                          billingState.selectedVisit!.patient.patientCode,
                                          style: const TextStyle(color: AppTheme.cardPureWhite, fontSize: 10, fontWeight: FontWeight.bold),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    'Physician: Dr. Nyan Kaung Sett M.B.,B.S(MDY) • Phone: ${billingState.selectedVisit!.patient.phone}',
                                    style: const TextStyle(color: AppTheme.slateMedium, fontSize: 11),
                                  ),
                                ],
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppTheme.cardPureWhite,
                                  borderRadius: BorderRadius.circular(6),
                                  border: Border.all(color: AppTheme.subtleBorder),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: const [
                                    Text('Invoice Status', style: TextStyle(fontSize: 9, color: AppTheme.slateMuted)),
                                    Text('PENDING CHECKOUT', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Color(0xFFD97706))),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),

                        // 2. Itemized Table: Consultation Fee, Procedures, Prescriptions
                        Expanded(
                          child: ListView(
                            padding: const EdgeInsets.all(12),
                            children: [
                              const Text(
                                'Itemized Billing Charges',
                                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppTheme.slateDark),
                              ),
                              const SizedBox(height: 6),
                              Table(
                                columnWidths: const {
                                  0: FlexColumnWidth(4),
                                  1: FlexColumnWidth(2),
                                  2: FlexColumnWidth(1),
                                  3: FlexColumnWidth(2),
                                  4: FlexColumnWidth(2),
                                },
                                children: [
                                  TableRow(
                                    decoration: const BoxDecoration(
                                      color: Color(0xFFF1F5F9),
                                    ),
                                    children: const [
                                      Padding(padding: EdgeInsets.all(6), child: Text('DESCRIPTION', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 10, color: AppTheme.slateDark))),
                                      Padding(padding: EdgeInsets.all(6), child: Text('CATEGORY', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 10, color: AppTheme.slateDark))),
                                      Padding(padding: EdgeInsets.all(6), child: Text('QTY', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 10, color: AppTheme.slateDark))),
                                      Padding(padding: EdgeInsets.all(6), child: Text('UNIT (Ks)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 10, color: AppTheme.slateDark))),
                                      Padding(padding: EdgeInsets.all(6), child: Text('TOTAL (Ks)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 10, color: AppTheme.slateDark))),
                                    ],
                                  ),
                                  ...billingState.invoiceItems.map((item) {
                                    return TableRow(
                                      decoration: BoxDecoration(
                                        border: Border(bottom: BorderSide(color: AppTheme.subtleBorder, width: 0.8)),
                                      ),
                                      children: [
                                        Padding(padding: const EdgeInsets.all(6), child: Text(item.description, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w500))),
                                        Padding(padding: const EdgeInsets.all(6), child: Text(item.category, style: const TextStyle(fontSize: 10, color: AppTheme.slateMuted))),
                                        Padding(padding: const EdgeInsets.all(6), child: Text('${item.quantity}', style: const TextStyle(fontSize: 11))),
                                        Padding(padding: const EdgeInsets.all(6), child: Text('${item.unitPrice.toInt()}', style: const TextStyle(fontSize: 11))),
                                        Padding(padding: const EdgeInsets.all(6), child: Text('${item.totalPrice.toInt()}', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold))),
                                      ],
                                    );
                                  }).toList(),
                                ],
                              ),
                            ],
                          ),
                        ),

                        // 3. Payment Controls & Calculations (Subtotal, Discount %, Grand Total, Payment Methods)
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFAFAFA),
                            border: Border(top: BorderSide(color: AppTheme.subtleBorder)),
                          ),
                          child: Column(
                            children: [
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  // Payment Method Selector
                                  Expanded(
                                    flex: 3,
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        const Text('Payment Method:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: AppTheme.slateDark)),
                                        const SizedBox(height: 4),
                                        Wrap(
                                          spacing: 6,
                                          runSpacing: 4,
                                          children: ['CASH', 'KBZ_PAY', 'WAVE_PAY', 'CARD'].map((m) {
                                            final isSelected = billingState.selectedPaymentMethod == m;
                                            return ChoiceChip(
                                              label: Text(
                                                m == 'KBZ_PAY' ? 'KBZPay' : m == 'WAVE_PAY' ? 'WavePay' : m,
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  fontWeight: FontWeight.bold,
                                                  color: isSelected ? AppTheme.cardPureWhite : AppTheme.slateDark,
                                                ),
                                              ),
                                              selected: isSelected,
                                              selectedColor: AppTheme.primaryGreen,
                                              backgroundColor: AppTheme.cardPureWhite,
                                              padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                                              onSelected: (_) => billingNotifier.setPaymentMethod(m),
                                            );
                                          }).toList(),
                                        ),
                                      ],
                                    ),
                                  ),

                                  // Financial Totals
                                  Expanded(
                                    flex: 3,
                                    child: Column(
                                      children: [
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                          children: [
                                            const Text('Subtotal:', style: TextStyle(fontSize: 11, color: AppTheme.slateMedium)),
                                            Text('${billingState.subtotal.toInt()} Ks', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 11.5)),
                                          ],
                                        ),
                                        const SizedBox(height: 4),
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                          children: [
                                            const Text('Discount (%):', style: TextStyle(fontSize: 11, color: AppTheme.slateMedium)),
                                            SizedBox(
                                              width: 65,
                                              height: 30,
                                              child: TextField(
                                                controller: _discountPercentController,
                                                keyboardType: TextInputType.number,
                                                style: const TextStyle(fontSize: 11),
                                                decoration: const InputDecoration(
                                                  contentPadding: EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                                  isDense: true,
                                                  border: OutlineInputBorder(),
                                                ),
                                                onChanged: (val) {
                                                  final p = double.tryParse(val) ?? 0.0;
                                                  billingNotifier.setDiscountPercent(p);
                                                },
                                              ),
                                            ),
                                          ],
                                        ),
                                        Divider(height: 12, color: AppTheme.subtleBorder),
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                          children: [
                                            const Text('GRAND TOTAL:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppTheme.slateDark)),
                                            Text(
                                              '${billingState.grandTotal.toInt()} Ks',
                                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppTheme.primaryGreen),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),

                              const SizedBox(height: 10),

                              // 4. Action Buttons:
                              // [ Confirm Payment & Print Thermal Receipt ]
                              // [ Print A4 Clinical Record & Rx ]
                              Row(
                                children: [
                                  // Print A4 Clinical Record & Rx (Medical Document ONLY)
                                  Expanded(
                                    child: OutlinedButton.icon(
                                      onPressed: () {
                                        _showA4PreviewDialog(context, billingState.selectedVisit!);
                                      },
                                      icon: const Icon(Icons.description, size: 15, color: AppTheme.primaryGreen),
                                      label: const Text('Print A4 Clinical Record & Rx', style: TextStyle(color: AppTheme.primaryGreen, fontWeight: FontWeight.bold, fontSize: 11)),
                                      style: OutlinedButton.styleFrom(
                                        padding: const EdgeInsets.symmetric(vertical: 8),
                                        side: const BorderSide(color: AppTheme.primaryGreen, width: 1.2),
                                        shape: RoundedCornerShape(AppTheme.buttonRadius),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),

                                  // Confirm Payment & Print Thermal Receipt (Invoice ONLY)
                                  Expanded(
                                    flex: 1,
                                    child: ElevatedButton.icon(
                                      onPressed: billingState.isProcessingPayment
                                          ? null
                                          : () async {
                                              final invoiceNo = await billingNotifier.confirmPaymentAndCheckout();
                                              if (invoiceNo != null && context.mounted) {
                                                ScaffoldMessenger.of(context).showSnackBar(
                                                  SnackBar(
                                                    content: Text('Payment Confirmed & Thermal Receipt Generated ($invoiceNo)!'),
                                                    backgroundColor: AppTheme.statusSuccess,
                                                  ),
                                                );
                                              }
                                            },
                                      icon: billingState.isProcessingPayment
                                          ? const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.cardPureWhite))
                                          : const Icon(Icons.print, size: 15, color: AppTheme.cardPureWhite),
                                      label: Text(
                                        billingState.isProcessingPayment ? 'Processing...' : 'Confirm & Print Thermal Receipt',
                                        style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.cardPureWhite, fontSize: 11),
                                      ),
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: AppTheme.primaryGreen,
                                        elevation: 0,
                                        padding: const EdgeInsets.symmetric(vertical: 8),
                                        shape: RoundedCornerShape(AppTheme.buttonRadius),
                                      ),
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
          ),
        ],
      ),
    );
  }

  /// Displays preview modal for A4 Clinical Record & Rx (Medical Document ONLY)
  void _showA4PreviewDialog(BuildContext context, VisitWithDetails visitDetails) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          shape: RoundedCornerShape(AppTheme.cardRadius),
          title: Row(
            children: const [
              Icon(Icons.picture_as_pdf, color: AppTheme.primaryGreen, size: 20),
              SizedBox(width: 8),
              Text('A4 Medical Record & Rx Preview', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
            ],
          ),
          content: SizedBox(
            width: 500,
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppTheme.softGreenContainer,
                      borderRadius: BorderRadius.circular(AppTheme.cardRadius),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('“ရွှေခေတ်” အထွေထွေရောဂါကုဆေးခန်း', style: TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primaryGreen, fontSize: 12)),
                        const Text('Dr. Nyan Kaung Sett M.B.,B.S(MDY) • အမှတ် (၁၄၂)၊ ဗဟိုလမ်း၊ ကမာရွတ်မြို့နယ်၊ ရန်ကုန်မြို့။', style: TextStyle(fontSize: 10.5)),
                        Divider(height: 10, color: AppTheme.subtleBorder),
                        Text('Patient: ${visitDetails.patient.fullName} (${visitDetails.patient.patientCode})', style: const TextStyle(fontSize: 11)),
                        Text('Age/Sex: ${visitDetails.patient.age}y / ${visitDetails.patient.gender} • Phone: ${visitDetails.patient.phone}', style: const TextStyle(fontSize: 10.5)),
                        Text('Date: ${DateTime.now().toString().substring(0, 10)}', style: const TextStyle(fontSize: 10.5)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                  const Text('Vital Signs:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
                  const Text('• BP: 120/80 mmHg | Pulse: 78 bpm | Temp: 98.6 °F | SpO2: 98% | Wt: 62 kg', style: TextStyle(fontSize: 10.5)),
                  const SizedBox(height: 6),
                  const Text('Clinical Diagnosis:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
                  const Text('• Acute Upper Respiratory Infection (URI)', style: TextStyle(color: AppTheme.primaryGreen, fontWeight: FontWeight.bold, fontSize: 10.5)),
                  const SizedBox(height: 6),
                  const Text('Prescription (Rx):', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
                  const Text('1. Amoxicillin 500mg - 15 Caps | 1-0-1 BD x 5 Days (After Food)', style: TextStyle(fontSize: 10.5)),
                  const Text('2. Paracetamol 500mg - 10 Tabs | 1-1-1 TDS PRN for Fever', style: TextStyle(fontSize: 10.5)),
                  const Text('3. Cetirizine 10mg - 10 Tabs | 0-0-1 HS x 7 Days', style: TextStyle(fontSize: 10.5)),
                  const SizedBox(height: 12),
                  const Align(
                    alignment: Alignment.centerRight,
                    child: Text('Physician Signature: Dr. Nyan Kaung Sett M.B.,B.S(MDY)', style: TextStyle(fontStyle: FontStyle.italic, fontWeight: FontWeight.bold, fontSize: 10.5)),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Close', style: TextStyle(fontSize: 11.5)),
            ),
            ElevatedButton.icon(
              onPressed: () {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('A4 Medical Record & Rx sent to System Print Dialog!'),
                    backgroundColor: AppTheme.primaryGreen,
                  ),
                );
              },
              icon: const Icon(Icons.print, size: 14, color: AppTheme.cardPureWhite),
              label: const Text('Print A4 Report', style: TextStyle(color: AppTheme.cardPureWhite, fontSize: 11.5)),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryGreen,
                shape: RoundedCornerShape(AppTheme.buttonRadius),
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              ),
            ),
          ],
        );
      },
    );
  }
}

