import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/services/printing/a4_pdf_report_service.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../billing/presentation/providers/billing_provider.dart';
import '../../../dashboard/presentation/providers/dashboard_provider.dart';

/// Doctor Consultation & EMR Screen
///
/// Refactored for Compact Clinical UI with Medical Green Theme (#2E7D32)
/// and Official Clinic Branding ("ရွှေခေတ်" အထွေထွေရောဂါကုဆေးခန်း).
class ConsultationScreen extends ConsumerStatefulWidget {
  final String? initialPatientId;
  final String? initialVisitId;

  const ConsultationScreen({
    Key? key,
    this.initialPatientId,
    this.initialVisitId,
  }) : super(key: key);

  @override
  ConsumerState<ConsultationScreen> createState() => _ConsultationScreenState();
}

class _ConsultationScreenState extends ConsumerState<ConsultationScreen> {
  // SOAP Controllers
  final _subjectiveController = TextEditingController();
  final _objectiveController = TextEditingController();
  final _assessmentController = TextEditingController();
  final _planController = TextEditingController();
  final _adviceController = TextEditingController();

  // Prescriptions list
  final List<PrescribedMedicine> _prescriptions = [];

  // Procedures list
  final Set<String> _selectedProcedures = {};

  @override
  void initState() {
    super.initState();
    _initDefaultClinicalData();
  }

  void _initDefaultClinicalData() {
    _subjectiveController.text = 'Patient presents with high-grade fever (39°C), chills, and body aches for 2 days. Dry irritating cough.';
    _objectiveController.text = 'Pharyngeal erythema (+), tonsils enlarged grade 1. Chest: clear to auscultation bilaterally, no crepitations.';
    _assessmentController.text = 'Acute Upper Respiratory Tract Infection (Acute URI)';
    _planController.text = 'Prescribed broad-spectrum antibiotic, antipyretic, and antihistamine. Encourage hydration and rest.';
    _adviceController.text = 'Drink warm fluids, complete full 5-day antibiotic course, return if fever persists > 3 days.';

    // Default demo Rx
    _prescriptions.addAll([
      PrescribedMedicine(
        name: 'Augmentin 625mg',
        dosage: '625 mg',
        frequency: '1 Tab BD (မနက်/ည)',
        duration: '5 Days',
        quantity: 10,
        unitPrice: 1062.5,
        instructions: 'After meals (အစားစားပြီးသောက်ရန်)',
      ),
      PrescribedMedicine(
        name: 'Panadol Extra 500mg',
        dosage: '500 mg',
        frequency: '1-2 Tab TDS PRN (ကိုက်ခဲဖျားနာပါက)',
        duration: '3 Days',
        quantity: 10,
        unitPrice: 260.0,
        instructions: 'Take when fever or pain occurs',
      ),
      PrescribedMedicine(
        name: 'Nexium 40mg',
        dosage: '40 mg',
        frequency: '1 Cap OD Before Breakfast (မနက်စာမစားမီ)',
        duration: '5 Days',
        quantity: 5,
        unitPrice: 1440.0,
        instructions: '30 mins before breakfast',
      ),
    ]);
  }

  @override
  void dispose() {
    _subjectiveController.dispose();
    _objectiveController.dispose();
    _assessmentController.dispose();
    _planController.dispose();
    _adviceController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final dashboardState = ref.watch(dashboardProvider);
    final activeQueueItem = dashboardState.selectedQueueItem ??
        (dashboardState.allQueueItems.isNotEmpty ? dashboardState.allQueueItems.first : null);

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
            Icon(Icons.medical_services_rounded, color: AppTheme.primaryGreen, size: 20),
            SizedBox(width: 8),
            Text(
              'Doctor Consultation & EMR',
              style: TextStyle(color: AppTheme.slateDark, fontWeight: FontWeight.bold, fontSize: 15),
            ),
            SizedBox(width: 8),
            Text(
              'ဆရာဝန်ကုသဆောင်',
              style: TextStyle(color: AppTheme.slateMuted, fontSize: 11),
            ),
          ],
        ),
        actions: [
          // Queue Selection Dropdown
          if (dashboardState.allQueueItems.isNotEmpty)
            Container(
              margin: const EdgeInsets.symmetric(vertical: 8),
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: AppTheme.softGreenContainer,
                borderRadius: BorderRadius.circular(AppTheme.buttonRadius),
                border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.3)),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: activeQueueItem?.visit.id,
                  hint: const Text('Select Patient', style: TextStyle(fontSize: 11)),
                  icon: const Icon(Icons.arrow_drop_down, color: AppTheme.primaryGreen, size: 18),
                  items: dashboardState.allQueueItems.map((item) {
                    return DropdownMenuItem(
                      value: item.visit.id,
                      child: Text(
                        '${item.queueNumber} - ${item.patient.fullName} (${item.patient.patientCode})',
                        style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
                      ),
                    );
                  }).toList(),
                  onChanged: (visitId) {
                    final found = dashboardState.allQueueItems.firstWhere((i) => i.visit.id == visitId);
                    ref.read(dashboardProvider.notifier).selectQueueItem(found);
                  },
                ),
              ),
            ),
          const SizedBox(width: 12),
        ],
      ),
      body: activeQueueItem == null
          ? const Center(child: Text('No active patient in consultation'))
          : Padding(
              padding: const EdgeInsets.all(10.0),
              child: Column(
                children: [
                  // 1. Patient Demographics & Triage Banner
                  _buildPatientBanner(activeQueueItem),

                  const SizedBox(height: 8),

                  // 2. Main EMR Area: 2 Columns on Tablets (SOAP Notes Left, Rx & Procedures Right)
                  Expanded(
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final isTablet = constraints.maxWidth >= 800;

                        if (isTablet) {
                          return Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              // Left: SOAP Clinical Documentation (Flex 5)
                              Expanded(
                                flex: 5,
                                child: _buildSoapFormCard(),
                              ),
                              const SizedBox(width: 10),
                              // Right: Prescriptions & Procedures (Flex 5)
                              Expanded(
                                flex: 5,
                                child: _buildPrescriptionCard(context, activeQueueItem),
                              ),
                            ],
                          );
                        }

                        // Compact screens: Vertical scroll
                        return SingleChildScrollView(
                          child: Column(
                            children: [
                              _buildSoapFormCard(),
                              const SizedBox(height: 10),
                              _buildPrescriptionCard(context, activeQueueItem),
                            ],
                          ),
                        );
                      },
                    ),
                  ),

                  const SizedBox(height: 8),

                  // 3. Bottom Action Bar: Print A4 Clinical Record & Send to Cashier Billing
                  _buildBottomActionBar(context, activeQueueItem),
                ],
              ),
            ),
    );
  }

  /// Patient Demographics & Vitals Header Banner
  Widget _buildPatientBanner(PatientQueueItem item) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: AppTheme.cardPureWhite,
        borderRadius: BorderRadius.circular(AppTheme.cardRadius),
        border: Border.all(color: AppTheme.subtleBorder),
      ),
      child: Row(
        children: [
          // Queue Token
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: AppTheme.primaryGreen,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              item.queueNumber,
              style: const TextStyle(color: AppTheme.cardPureWhite, fontWeight: FontWeight.bold, fontSize: 12),
            ),
          ),
          const SizedBox(width: 10),

          // Name & Details
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      item.patient.fullName,
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      '(${item.patient.patientCode})',
                      style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMuted, fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      '${item.patient.age} Yrs • ${item.patient.gender}',
                      style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMedium),
                    ),
                    if (item.patient.allergies.isNotEmpty) ...[
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFEE2E2),
                          borderRadius: BorderRadius.circular(4),
                          border: Border.all(color: AppTheme.statusCritical.withOpacity(0.3)),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.warning_amber_rounded, color: AppTheme.statusCritical, size: 11),
                            const SizedBox(width: 3),
                            Text(
                              'Allergy: ${item.patient.allergies}',
                              style: const TextStyle(fontSize: 9.5, fontWeight: FontWeight.bold, color: AppTheme.statusCritical),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  'Chief Complaint: ${item.visit.chiefComplaint ?? "None"}',
                  style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMedium),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),

          // Vitals Summary Pills
          Wrap(
            spacing: 6,
            children: [
              _buildVitalTag('BP', item.bloodPressure),
              _buildVitalTag('Pulse', item.pulse),
              _buildVitalTag('Temp', item.temp),
              _buildVitalTag('SpO2', item.spo2),
              _buildVitalTag('Weight', item.weight),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildVitalTag(String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: AppTheme.softGreenContainer,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: AppTheme.primaryGreen.withOpacity(0.2)),
      ),
      child: Column(
        children: [
          Text(label, style: const TextStyle(fontSize: 8.5, color: AppTheme.slateMuted, fontWeight: FontWeight.bold)),
          Text(value, style: const TextStyle(fontSize: 10.5, color: AppTheme.slateDark, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  /// SOAP Notes Card
  Widget _buildSoapFormCard() {
    return Card(
      elevation: 0,
      color: AppTheme.cardPureWhite,
      shape: RoundedCornerShape(AppTheme.cardRadius),
      side: BorderSide(color: AppTheme.subtleBorder),
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: const [
                  Icon(Icons.notes_rounded, color: AppTheme.primaryGreen, size: 16),
                  SizedBox(width: 6),
                  Text(
                    'SOAP Clinical Notes (ဆေးမှတ်တမ်းအသေးစိတ်)',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppTheme.slateDark),
                  ),
                ],
              ),
              const SizedBox(height: 8),

              _buildSoapField(
                label: 'S - Subjective (History & Symptoms):',
                controller: _subjectiveController,
                lines: 2,
              ),
              const SizedBox(height: 6),

              _buildSoapField(
                label: 'O - Objective (Physical Findings & Vitals):',
                controller: _objectiveController,
                lines: 2,
              ),
              const SizedBox(height: 6),

              _buildSoapField(
                label: 'A - Assessment (Clinical Diagnosis & ICD-10):',
                controller: _assessmentController,
                lines: 1,
              ),
              const SizedBox(height: 6),

              _buildSoapField(
                label: 'P - Plan & Doctor Advice (ကုသမှုအစီအစဉ်နှင့် ညွှန်ကြားချက်):',
                controller: _planController,
                lines: 2,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSoapField({
    required String label,
    required TextEditingController controller,
    required int lines,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark)),
        const SizedBox(height: 3),
        TextField(
          controller: controller,
          maxLines: lines,
          style: const TextStyle(fontSize: 11, color: AppTheme.slateDark),
          decoration: InputDecoration(
            isDense: true,
            contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            filled: true,
            fillColor: const Color(0xFFF8FAFC),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: AppTheme.subtleBorder),
            ),
          ),
        ),
      ],
    );
  }

  /// Prescriptions & Procedures Card
  Widget _buildPrescriptionCard(BuildContext context, PatientQueueItem item) {
    return Card(
      elevation: 0,
      color: AppTheme.cardPureWhite,
      shape: RoundedCornerShape(AppTheme.cardRadius),
      side: BorderSide(color: AppTheme.subtleBorder),
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Prescriptions Header & [ + Add Medicine ]
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: const [
                    Icon(Icons.medication_rounded, color: AppTheme.primaryGreen, size: 16),
                    SizedBox(width: 6),
                    Text(
                      'Prescriptions (ဆေးညွှန်းစာရင်း)',
                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppTheme.slateDark),
                    ),
                  ],
                ),
                TextButton.icon(
                  onPressed: () => _openAddMedicineDialog(context),
                  icon: const Icon(Icons.add, size: 13, color: AppTheme.primaryGreen),
                  label: const Text('+ Add Medicine', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.primaryGreen)),
                  style: TextButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ],
            ),

            // Prescriptions List
            Expanded(
              child: _prescriptions.isEmpty
                  ? const Center(child: Text('No medicines prescribed yet', style: TextStyle(fontSize: 11, color: AppTheme.slateMuted)))
                  : ListView.separated(
                      itemCount: _prescriptions.length,
                      separatorBuilder: (_, __) => Divider(height: 1, color: AppTheme.subtleBorder),
                      itemBuilder: (ctx, idx) {
                        final rx = _prescriptions[idx];
                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 3),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      '${rx.name} (${rx.dosage})',
                                      style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
                                    ),
                                    Text(
                                      '${rx.frequency} • ${rx.duration} • Qty: ${rx.quantity}',
                                      style: const TextStyle(fontSize: 9.5, color: AppTheme.slateMedium),
                                    ),
                                    Text(
                                      rx.instructions,
                                      style: const TextStyle(fontSize: 9.5, color: AppTheme.primaryGreen),
                                    ),
                                  ],
                                ),
                              ),
                              IconButton(
                                icon: const Icon(Icons.delete_outline, size: 15, color: AppTheme.statusCritical),
                                padding: EdgeInsets.zero,
                                constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                                onPressed: () => setState(() => _prescriptions.removeAt(idx)),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
            ),

            Divider(height: 1, color: AppTheme.subtleBorder),
            const SizedBox(height: 6),

            // Minor Procedures Checkboxes
            const Text(
              'Minor Procedures (ကုသစောင့်ရှောက်မှုများ):',
              style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
            ),
            const SizedBox(height: 4),
            Wrap(
              spacing: 6,
              children: [
                _buildProcedureChip('Minor Wound Dressing (Ks 11,000)'),
                _buildProcedureChip('Nebulization Therapy (Ks 8,000)'),
                _buildProcedureChip('IV Cannulation & Infusion (Ks 7,000)'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProcedureChip(String name) {
    final isSelected = _selectedProcedures.contains(name);
    return FilterChip(
      selected: isSelected,
      label: Text(name, style: TextStyle(fontSize: 9.5, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
      selectedColor: AppTheme.softGreenContainer,
      checkmarkColor: AppTheme.primaryGreen,
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      onSelected: (val) {
        setState(() {
          if (val) {
            _selectedProcedures.add(name);
          } else {
            _selectedProcedures.remove(name);
          }
        });
      },
    );
  }

  /// Bottom Action Bar: Print A4 Clinical Record & Send to Cashier Billing
  Widget _buildBottomActionBar(BuildContext context, PatientQueueItem item) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppTheme.cardPureWhite,
        borderRadius: BorderRadius.circular(AppTheme.cardRadius),
        border: Border.all(color: AppTheme.subtleBorder),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // Doctor Signature Credential Pill
          Row(
            children: const [
              Icon(Icons.verified, color: AppTheme.primaryGreen, size: 15),
              SizedBox(width: 6),
              Text(
                'Attending: Dr. Nyan Kaung Sett M.B.,B.S(MDY)',
                style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
              ),
            ],
          ),

          Row(
            children: [
              // [ Print A4 Clinical Record & Rx ] (Medical Record ONLY)
              OutlinedButton.icon(
                onPressed: () => _printA4ClinicalRecord(context, item),
                icon: const Icon(Icons.print_rounded, size: 14),
                label: const Text('Print A4 Clinical Record & Rx', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold)),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppTheme.primaryGreen,
                  side: const BorderSide(color: AppTheme.primaryGreen, width: 1.2),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  shape: RoundedCornerShape(AppTheme.buttonRadius),
                ),
              ),
              const SizedBox(width: 8),

              // [ Send to Cashier Billing ]
              ElevatedButton.icon(
                onPressed: () => _sendToCashierBilling(context, item),
                icon: const Icon(Icons.point_of_sale, size: 14, color: AppTheme.cardPureWhite),
                label: const Text('Send to Cashier Billing', style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppTheme.cardPureWhite)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryGreen,
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  shape: RoundedCornerShape(AppTheme.buttonRadius),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Prints A4 Clinical Record via A4PdfReportService
  void _printA4ClinicalRecord(BuildContext context, PatientQueueItem item) {
    final a4Doc = A4PdfReportService.generateClinicalDocument(
      clinicName: '“ရွှေခေတ်” အထွေထွေရောဂါကုဆေးခန်း',
      clinicAddress: 'အမှတ် (၁၄၂)၊ ဗဟိုလမ်း၊ ကမာရွတ်မြို့နယ်၊ ရန်ကုန်မြို့။',
      clinicPhone: '09-421008761, 01-523891',
      doctorName: 'Dr. Nyan Kaung Sett',
      doctorSamaNo: 'M.B.,B.S(MDY)',
      doctorQualifications: 'General Practitioner & Family Physician',
      patientName: item.patient.fullName,
      patientCode: item.patient.patientCode,
      patientAge: item.patient.age,
      patientGender: item.patient.gender,
      patientPhone: item.patient.phone,
      patientAddress: item.patient.address ?? 'Yangon, Myanmar',
      visitDate: DateTime.now().toLocal().toString().split(' ')[0],
      vitalSigns: {
        'Blood Pressure': item.bloodPressure,
        'Pulse Rate': item.pulse,
        'Temperature': item.temp,
        'Oxygen Saturation (SpO2)': item.spo2,
        'Body Weight': item.weight,
      },
      chiefComplaint: _subjectiveController.text.trim(),
      clinicalDiagnosis: _assessmentController.text.trim(),
      objectiveNotes: _objectiveController.text.trim(),
      doctorAdvice: _adviceController.text.trim().isNotEmpty
          ? _adviceController.text.trim()
          : _planController.text.trim(),
      prescriptions: _prescriptions
          .map((p) => A4PrescriptionItem(
                drugName: p.name,
                dosage: p.dosage,
                frequency: p.frequency,
                duration: p.duration,
                quantity: p.quantity,
                instructions: p.instructions,
              ))
          .toList(),
      performedProcedures: _selectedProcedures.toList(),
      followUpDate: 'In 5 Days if symptoms persist',
    );

    // Show Preview Modal
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedCornerShape(AppTheme.cardRadius),
        title: Row(
          children: const [
            Icon(Icons.description, color: AppTheme.primaryGreen, size: 20),
            SizedBox(width: 8),
            Text('A4 Clinical Document & Rx (Medical Record ONLY)', style: TextStyle(fontSize: 14)),
          ],
        ),
        content: SizedBox(
          width: 500,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Doctor: ${a4Doc.doctorName} (${a4Doc.doctorSamaNo})', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
              Text('Patient: ${a4Doc.patientName} • HN: ${a4Doc.patientCode}', style: const TextStyle(fontSize: 11)),
              Text('Diagnosis: ${a4Doc.clinicalDiagnosis}', style: const TextStyle(fontSize: 11, color: AppTheme.primaryGreen, fontWeight: FontWeight.bold)),
              Divider(height: 12, color: AppTheme.subtleBorder),
              const Text('Prescriptions:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
              ...a4Doc.prescriptions.map((rx) => Text('• ${rx.drugName} - ${rx.dosage} (${rx.frequency}) x ${rx.quantity}', style: const TextStyle(fontSize: 10.5))),
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: const Color(0xFFF1F5F9), borderRadius: BorderRadius.circular(6)),
                child: const Text('Enforced Rule: A4 Printing is strictly for Clinical Medical Records & Rx Prescriptions.', style: TextStyle(fontSize: 9.5, color: AppTheme.slateMuted)),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Close', style: TextStyle(fontSize: 11.5)),
          ),
          ElevatedButton.icon(
            onPressed: () {
              Navigator.of(ctx).pop();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Sent A4 Medical Record to Printer Spooler.'), backgroundColor: AppTheme.statusSuccess),
              );
            },
            icon: const Icon(Icons.print, size: 14, color: AppTheme.cardPureWhite),
            label: const Text('Print A4 Record', style: TextStyle(color: AppTheme.cardPureWhite, fontSize: 11.5)),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primaryGreen,
              shape: RoundedCornerShape(AppTheme.buttonRadius),
            ),
          ),
        ],
      ),
    );
  }

  /// Transfers visit to Billing and navigates to Cashier Billing Panel
  void _sendToCashierBilling(BuildContext context, PatientQueueItem item) async {
    final dashboardNotifier = ref.read(dashboardProvider.notifier);
    final billingNotifier = ref.read(billingProvider.notifier);

    // 1. Update status to BILLING
    await dashboardNotifier.updateVisitStatus(item.visit.id, 'BILLING');

    // 2. Reload billing queue
    await billingNotifier.loadPendingBillingVisits();

    // 3. Navigate to Cashier Billing panel
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${item.patient.fullName} sent to Cashier Billing station.'),
          backgroundColor: AppTheme.primaryGreen,
        ),
      );
      context.go('/billing');
    }
  }

  void _openAddMedicineDialog(BuildContext context) {
    final nameCtrl = TextEditingController(text: 'Amoxicillin 500mg');
    final dosageCtrl = TextEditingController(text: '500 mg');
    final freqCtrl = TextEditingController(text: '1 Cap TDS (မနက်/နေ့/ည)');
    final durCtrl = TextEditingController(text: '5 Days');
    final qtyCtrl = TextEditingController(text: '15');
    final priceCtrl = TextEditingController(text: '350');
    final instCtrl = TextEditingController(text: 'After food (အစားစားပြီးသောက်ပါ)');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedCornerShape(AppTheme.cardRadius),
        title: const Text('Add Prescription Medicine', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                style: const TextStyle(fontSize: 12),
                decoration: const InputDecoration(labelText: 'Medicine Name', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(child: TextField(controller: dosageCtrl, style: const TextStyle(fontSize: 12), decoration: const InputDecoration(labelText: 'Dosage', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)))),
                  const SizedBox(width: 8),
                  Expanded(child: TextField(controller: durCtrl, style: const TextStyle(fontSize: 12), decoration: const InputDecoration(labelText: 'Duration', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)))),
                ],
              ),
              const SizedBox(height: 8),
              TextField(controller: freqCtrl, style: const TextStyle(fontSize: 12), decoration: const InputDecoration(labelText: 'Frequency', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10))),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(child: TextField(controller: qtyCtrl, keyboardType: TextInputType.number, style: const TextStyle(fontSize: 12), decoration: const InputDecoration(labelText: 'Quantity', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)))),
                  const SizedBox(width: 8),
                  Expanded(child: TextField(controller: priceCtrl, keyboardType: TextInputType.number, style: const TextStyle(fontSize: 12), decoration: const InputDecoration(labelText: 'Unit Price (Ks)', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10)))),
                ],
              ),
              const SizedBox(height: 8),
              TextField(controller: instCtrl, style: const TextStyle(fontSize: 12), decoration: const InputDecoration(labelText: 'Instructions', isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10))),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancel', style: TextStyle(fontSize: 11.5))),
          ElevatedButton(
            onPressed: () {
              setState(() {
                _prescriptions.add(PrescribedMedicine(
                  name: nameCtrl.text.trim(),
                  dosage: dosageCtrl.text.trim(),
                  frequency: freqCtrl.text.trim(),
                  duration: durCtrl.text.trim(),
                  quantity: int.tryParse(qtyCtrl.text.trim()) ?? 1,
                  unitPrice: double.tryParse(priceCtrl.text.trim()) ?? 0.0,
                  instructions: instCtrl.text.trim(),
                ));
              });
              Navigator.of(ctx).pop();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primaryGreen,
              shape: RoundedCornerShape(AppTheme.buttonRadius),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            ),
            child: const Text('Add to Rx', style: TextStyle(color: AppTheme.cardPureWhite, fontSize: 11.5)),
          ),
        ],
      ),
    );
  }
}

class PrescribedMedicine {
  final String name;
  final String dosage;
  final String frequency;
  final String duration;
  final int quantity;
  final double unitPrice;
  final String instructions;

  PrescribedMedicine({
    required this.name,
    required this.dosage,
    required this.frequency,
    required this.duration,
    required this.quantity,
    required this.unitPrice,
    required this.instructions,
  });
}
