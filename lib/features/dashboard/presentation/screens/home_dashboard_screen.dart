import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/theme/app_theme.dart';
import '../providers/dashboard_provider.dart';

/// Home Queue & Quick Stats Dashboard Screen
///
/// Compact Clinical UI with Medical Green Theme (#2E7D32)
class HomeDashboardScreen extends ConsumerStatefulWidget {
  const HomeDashboardScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<HomeDashboardScreen> createState() => _HomeDashboardScreenState();
}

class _HomeDashboardScreenState extends ConsumerState<HomeDashboardScreen> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(dashboardProvider);
    final notifier = ref.read(dashboardProvider.notifier);

    return Scaffold(
      backgroundColor: AppTheme.bgLightGray,
      body: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // =================================================================
            // 1. TOP SUMMARY CARDS ROW
            // =================================================================
            _buildSummaryCardsRow(state),

            const SizedBox(height: 10),

            // =================================================================
            // 2. PATIENT QUEUE TABLE & ACTIONS HEADER
            // =================================================================
            Expanded(
              child: Card(
                elevation: 0,
                color: AppTheme.cardPureWhite,
                shape: RoundedCornerShape(AppTheme.cardRadius),
                side: BorderSide(color: AppTheme.subtleBorder, width: 1),
                child: Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildQueueControlsHeader(context, state, notifier),
                      const SizedBox(height: 10),
                      Divider(height: 1, color: AppTheme.subtleBorder),
                      const SizedBox(height: 10),
                      // Queue Table / List View
                      Expanded(
                        child: _buildQueueList(context, state, notifier),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Top Summary Cards Row: Today's Patients, Waiting, Pending Billing, Low Stock Alert
  Widget _buildSummaryCardsRow(DashboardState state) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isSmall = constraints.maxWidth < 600;

        final cards = [
          _SummaryMetricCard(
            title: "Today's Patients",
            myanmarTitle: 'ယနေ့ လူနာစုစုပေါင်း',
            value: '${state.todayPatientsCount}',
            subtitle: 'Registered Visits',
            icon: Icons.people_alt_rounded,
            color: AppTheme.primaryGreen,
            backgroundColor: AppTheme.softGreenContainer,
          ),
          _SummaryMetricCard(
            title: 'Waiting in Queue',
            myanmarTitle: 'စောင့်ဆိုင်းဆရာဝန်ပြ',
            value: '${state.waitingCount}',
            subtitle: 'Ready for Doctor',
            icon: Icons.hourglass_top_rounded,
            color: AppTheme.statusWarning,
            backgroundColor: const Color(0xFFFEF3C7),
            isHighlighted: state.waitingCount > 0,
          ),
          _SummaryMetricCard(
            title: 'Pending Billing',
            myanmarTitle: 'ငွေရှင်းကောင်တာ',
            value: '${state.pendingBillingCount}',
            subtitle: 'At Cashier Station',
            icon: Icons.receipt_long_rounded,
            color: AppTheme.accentRedBrown,
            backgroundColor: AppTheme.accentRedBrown.withOpacity(0.1),
          ),
          _SummaryMetricCard(
            title: 'Low Stock Alert',
            myanmarTitle: 'ဆေးပစ္စည်းလက်ကျန်နည်း',
            value: '${state.lowStockAlertCount}',
            subtitle: 'Batches < Min Threshold',
            icon: Icons.warning_amber_rounded,
            color: state.lowStockAlertCount > 0 ? AppTheme.statusCritical : AppTheme.statusSuccess,
            backgroundColor: state.lowStockAlertCount > 0
                ? const Color(0xFFFEE2E2)
                : const Color(0xFFD1FAE5),
          ),
        ];

        if (isSmall) {
          return Column(
            children: [
              Row(children: [cards[0], const SizedBox(width: 8), cards[1]]),
              const SizedBox(height: 8),
              Row(children: [cards[2], const SizedBox(width: 8), cards[3]]),
            ],
          );
        }

        return Row(
          children: [
            Expanded(child: cards[0]),
            const SizedBox(width: 10),
            Expanded(child: cards[1]),
            const SizedBox(width: 10),
            Expanded(child: cards[2]),
            const SizedBox(width: 10),
            Expanded(child: cards[3]),
          ],
        );
      },
    );
  }

  /// Controls Header: Search, Filter Tabs, and [ + Register New Patient ] & [ Start Consultation ]
  Widget _buildQueueControlsHeader(
    BuildContext context,
    DashboardState state,
    DashboardNotifier notifier,
  ) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Row 1: Section Title & Quick Action Buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: const [
                    Icon(Icons.queue_rounded, color: AppTheme.primaryGreen, size: 20),
                    SizedBox(width: 6),
                    Text(
                      'Live Patient Queue & Triage',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: AppTheme.slateDark,
                      ),
                    ),
                    SizedBox(width: 6),
                    Text(
                      'လူနာစောင့်ဆိုင်းစာရင်း',
                      style: TextStyle(fontSize: 11, color: AppTheme.slateMuted),
                    ),
                  ],
                ),

                // Quick Action Buttons
                Row(
                  children: [
                    // [ + Register New Patient ]
                    ElevatedButton.icon(
                      onPressed: () => _openRegisterPatientSheet(context),
                      icon: const Icon(Icons.person_add_alt_1, size: 15, color: AppTheme.cardPureWhite),
                      label: const Text(
                        '+ Register Patient',
                        style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppTheme.cardPureWhite),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryGreen,
                        elevation: 0,
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        shape: RoundedCornerShape(AppTheme.buttonRadius),
                      ),
                    ),
                    const SizedBox(width: 8),

                    // [ Start Consultation ]
                    OutlinedButton.icon(
                      onPressed: state.selectedQueueItem == null
                          ? null
                          : () {
                              final item = state.selectedQueueItem!;
                              if (item.visit.status == 'WAITING') {
                                notifier.updateVisitStatus(item.visit.id, 'IN_CONSULTATION');
                              }
                              context.go(
                                '/consultation?patientId=${item.patient.id}&visitId=${item.visit.id}',
                              );
                            },
                      icon: const Icon(Icons.play_arrow_rounded, size: 16),
                      label: const Text(
                        'Start Consultation',
                        style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold),
                      ),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppTheme.primaryGreen,
                        side: const BorderSide(color: AppTheme.primaryGreen, width: 1.2),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        shape: RoundedCornerShape(AppTheme.buttonRadius),
                      ),
                    ),
                  ],
                ),
              ],
            ),

            const SizedBox(height: 10),

            // Row 2: Search Box & Status Filter Segment Chips
            Row(
              children: [
                // Search Input Field
                Expanded(
                  flex: 3,
                  child: SizedBox(
                    height: 36,
                    child: TextField(
                      controller: _searchController,
                      onChanged: (val) => notifier.setSearchQuery(val),
                      decoration: InputDecoration(
                        isDense: true,
                        hintText: 'Search patient name, HN, phone, or symptoms...',
                        hintStyle: const TextStyle(fontSize: 11, color: AppTheme.slateMuted),
                        prefixIcon: const Icon(Icons.search, size: 16, color: AppTheme.slateMuted),
                        suffixIcon: _searchController.text.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear, size: 14),
                                onPressed: () {
                                  _searchController.clear();
                                  notifier.setSearchQuery('');
                                },
                              )
                            : null,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                        filled: true,
                        fillColor: const Color(0xFFF1F5F9),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(6),
                          borderSide: BorderSide.none,
                        ),
                      ),
                      style: const TextStyle(fontSize: 11.5, color: AppTheme.slateDark),
                    ),
                  ),
                ),

                const SizedBox(width: 10),

                // Status Filter Chips: ALL, WAITING, IN_CONSULTATION, BILLING, COMPLETED
                Expanded(
                  flex: 4,
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _buildFilterChip(
                          label: 'All (${state.allQueueItems.length})',
                          filterKey: 'ALL',
                          currentFilter: state.statusFilter,
                          onTap: () => notifier.setStatusFilter('ALL'),
                        ),
                        const SizedBox(width: 6),
                        _buildFilterChip(
                          label: 'Waiting (${state.waitingCount})',
                          filterKey: 'WAITING',
                          currentFilter: state.statusFilter,
                          color: AppTheme.statusWarning,
                          onTap: () => notifier.setStatusFilter('WAITING'),
                        ),
                        const SizedBox(width: 6),
                        _buildFilterChip(
                          label: 'In Doctor (${state.inConsultationCount})',
                          filterKey: 'IN_CONSULTATION',
                          currentFilter: state.statusFilter,
                          color: AppTheme.primaryGreen,
                          onTap: () => notifier.setStatusFilter('IN_CONSULTATION'),
                        ),
                        const SizedBox(width: 6),
                        _buildFilterChip(
                          label: 'Billing (${state.pendingBillingCount})',
                          filterKey: 'BILLING',
                          currentFilter: state.statusFilter,
                          color: AppTheme.accentRedBrown,
                          onTap: () => notifier.setStatusFilter('BILLING'),
                        ),
                        const SizedBox(width: 6),
                        _buildFilterChip(
                          label: 'Completed (${state.completedCount})',
                          filterKey: 'COMPLETED',
                          currentFilter: state.statusFilter,
                          color: AppTheme.statusSuccess,
                          onTap: () => notifier.setStatusFilter('COMPLETED'),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  Widget _buildFilterChip({
    required String label,
    required String filterKey,
    required String currentFilter,
    Color? color,
    required VoidCallback onTap,
  }) {
    final isSelected = currentFilter == filterKey;
    final activeColor = color ?? AppTheme.primaryGreen;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
        decoration: BoxDecoration(
          color: isSelected ? activeColor.withOpacity(0.12) : const Color(0xFFF1F5F9),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected ? activeColor : Colors.transparent,
            width: 1.2,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 10.5,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
            color: isSelected ? activeColor : AppTheme.slateMedium,
          ),
        ),
      ),
    );
  }

  /// Builds the patient queue list / table
  Widget _buildQueueList(
    BuildContext context,
    DashboardState state,
    DashboardNotifier notifier,
  ) {
    if (state.isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: AppTheme.primaryGreen),
      );
    }

    final items = state.filteredQueueItems;

    if (items.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: const BoxDecoration(
                color: Color(0xFFF1F5F9),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.people_outline, color: AppTheme.slateMuted, size: 24),
            ),
            const SizedBox(height: 8),
            const Text(
              'No patients found in this queue category',
              style: TextStyle(color: AppTheme.slateDark, fontSize: 12, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 3),
            const Text(
              'Click "+ Register Patient" to enqueue a new patient visit.',
              style: TextStyle(color: AppTheme.slateMuted, fontSize: 10.5),
            ),
          ],
        ),
      );
    }

    return ListView.separated(
      itemCount: items.length,
      separatorBuilder: (_, __) => const SizedBox(height: 6),
      itemBuilder: (context, index) {
        final item = items[index];
        final isSelected = item.visit.id == state.selectedQueueItem?.visit.id;

        return _QueuePatientTile(
          item: item,
          isSelected: isSelected,
          onTap: () => notifier.selectQueueItem(item),
          onStartConsultation: () {
            notifier.selectQueueItem(item);
            if (item.visit.status == 'WAITING') {
              notifier.updateVisitStatus(item.visit.id, 'IN_CONSULTATION');
            }
            context.go(
              '/consultation?patientId=${item.patient.id}&visitId=${item.visit.id}',
            );
          },
          onSendToBilling: () {
            notifier.updateVisitStatus(item.visit.id, 'BILLING');
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('${item.patient.fullName} transferred to Billing Station.'),
                backgroundColor: AppTheme.primaryGreen,
                duration: const Duration(seconds: 2),
              ),
            );
          },
          onComplete: () {
            notifier.updateVisitStatus(item.visit.id, 'COMPLETED');
          },
        );
      },
    );
  }

  /// Opens Modal Bottom Sheet for registering a new patient & visit
  void _openRegisterPatientSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.cardPureWhite,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (ctx) => _RegisterPatientBottomSheet(
        onRegister: (data) async {
          final notifier = ref.read(dashboardProvider.notifier);
          final newItem = await notifier.registerPatientAndVisit(
            fullName: data['fullName'] as String,
            gender: data['gender'] as String,
            age: data['age'] as int,
            phone: data['phone'] as String,
            address: data['address'] as String?,
            allergies: data['allergies'] as String?,
            chiefComplaint: data['chiefComplaint'] as String,
            bp: data['bp'] as String?,
            pulse: data['pulse'] as String?,
            temp: data['temp'] as String?,
            spo2: data['spo2'] as String?,
            weight: data['weight'] as String?,
          );

          if (mounted) {
            Navigator.of(ctx).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(
                  'Patient ${newItem.patient.fullName} registered as ${newItem.queueNumber} (${newItem.patient.patientCode}) and added to Waiting Queue.',
                ),
                backgroundColor: AppTheme.statusSuccess,
              ),
            );
          }
        },
      ),
    );
  }
}

/// Queue Patient Tile
class _QueuePatientTile extends StatelessWidget {
  final PatientQueueItem item;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onStartConsultation;
  final VoidCallback onSendToBilling;
  final VoidCallback onComplete;

  const _QueuePatientTile({
    Key? key,
    required this.item,
    required this.isSelected,
    required this.onTap,
    required this.onStartConsultation,
    required this.onSendToBilling,
    required this.onComplete,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final status = item.visit.status;

    final (statusColor, statusBg, statusText) = switch (status) {
      'WAITING' => (AppTheme.statusWarning, const Color(0xFFFEF3C7), 'WAITING'),
      'IN_CONSULTATION' => (AppTheme.primaryGreen, AppTheme.softGreenContainer, 'IN DOCTOR'),
      'BILLING' || 'AT_CASHIER' => (AppTheme.accentRedBrown, AppTheme.accentRedBrown.withOpacity(0.1), 'BILLING'),
      'COMPLETED' => (AppTheme.statusSuccess, const Color(0xFFD1FAE5), 'COMPLETED'),
      _ => (AppTheme.slateMedium, const Color(0xFFF1F5F9), status),
    };

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppTheme.cardRadius),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? AppTheme.softGreenContainer.withOpacity(0.5) : AppTheme.cardPureWhite,
          borderRadius: BorderRadius.circular(AppTheme.cardRadius),
          border: Border.all(
            color: isSelected ? AppTheme.primaryGreen : AppTheme.subtleBorder,
            width: isSelected ? 1.4 : 1,
          ),
        ),
        child: Row(
          children: [
            // Queue Number Token Badge
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                color: isSelected ? AppTheme.primaryGreen : const Color(0xFFF1F5F9),
                borderRadius: BorderRadius.circular(6),
              ),
              alignment: Alignment.Center,
              child: Text(
                item.queueNumber,
                style: TextStyle(
                  color: isSelected ? AppTheme.cardPureWhite : AppTheme.slateDark,
                  fontSize: 12.5,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),

            const SizedBox(width: 10),

            // Patient Identity & Demographics
            Expanded(
              flex: 3,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        item.patient.fullName,
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.slateDark,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF1F5F9),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          item.patient.patientCode,
                          style: const TextStyle(
                            fontSize: 9.5,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.slateMedium,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 1),
                  Row(
                    children: [
                      Text(
                        '${item.patient.age} yrs • ${item.patient.gender} • ${item.patient.phone}',
                        style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMuted),
                      ),
                    ],
                  ),
                  // Allergies Warning Chip
                  if (item.patient.allergies.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        const Icon(Icons.warning_amber_rounded, size: 11, color: AppTheme.statusCritical),
                        const SizedBox(width: 3),
                        Text(
                          'Allergy: ${item.patient.allergies}',
                          style: const TextStyle(
                            fontSize: 9.5,
                            fontWeight: FontWeight.bold,
                            color: AppTheme.statusCritical,
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),

            // Chief Complaint & Vitals
            Expanded(
              flex: 4,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.visit.chiefComplaint ?? 'No symptoms recorded',
                    style: const TextStyle(
                      fontSize: 10.5,
                      fontWeight: FontWeight.w500,
                      color: AppTheme.slateDark,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 2),
                  Wrap(
                    spacing: 6,
                    children: [
                      _buildVitalMiniTag('BP', item.bloodPressure),
                      _buildVitalMiniTag('Pulse', item.pulse),
                      _buildVitalMiniTag('Temp', item.temp),
                      _buildVitalMiniTag('Wt', item.weight),
                    ],
                  ),
                ],
              ),
            ),

            // Status Badge
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: statusBg,
                borderRadius: BorderRadius.circular(4),
                border: Border.all(color: statusColor.withOpacity(0.3)),
              ),
              child: Text(
                statusText,
                style: TextStyle(
                  color: statusColor,
                  fontSize: 9.5,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),

            const SizedBox(width: 10),

            // Row Action Triggers
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (status == 'WAITING' || status == 'IN_CONSULTATION')
                  IconButton(
                    tooltip: 'Start Doctor Consultation',
                    icon: const Icon(Icons.medical_services_rounded, color: AppTheme.primaryGreen, size: 18),
                    onPressed: onStartConsultation,
                  ),
                if (status == 'IN_CONSULTATION')
                  IconButton(
                    tooltip: 'Send to Billing',
                    icon: const Icon(Icons.point_of_sale_rounded, color: AppTheme.accentRedBrown, size: 18),
                    onPressed: onSendToBilling,
                  ),
                if (status == 'BILLING')
                  IconButton(
                    tooltip: 'Go to Cashier Billing',
                    icon: const Icon(Icons.arrow_forward_rounded, color: AppTheme.primaryGreen, size: 18),
                    onPressed: () => context.go('/billing'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildVitalMiniTag(String label, String value) {
    return Text(
      '$label: $value',
      style: const TextStyle(fontSize: 9.5, color: AppTheme.slateMuted, fontWeight: FontWeight.w600),
    );
  }
}

/// Summary Metric Card
class _SummaryMetricCard extends StatelessWidget {
  final String title;
  final String myanmarTitle;
  final String value;
  final String subtitle;
  final IconData icon;
  final Color color;
  final Color backgroundColor;
  final bool isHighlighted;

  const _SummaryMetricCard({
    Key? key,
    required this.title,
    required this.myanmarTitle,
    required this.value,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.backgroundColor,
    this.isHighlighted = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: AppTheme.cardPureWhite,
        borderRadius: BorderRadius.circular(AppTheme.cardRadius),
        border: Border.all(
          color: isHighlighted ? color : AppTheme.subtleBorder,
          width: isHighlighted ? 1.4 : 1,
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: backgroundColor,
              borderRadius: BorderRadius.circular(6),
            ),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  value,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: color,
                    letterSpacing: -0.4,
                  ),
                ),
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.slateDark,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  myanmarTitle,
                  style: const TextStyle(fontSize: 9.5, color: AppTheme.slateMuted),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Register Patient Modal Bottom Sheet
class _RegisterPatientBottomSheet extends StatefulWidget {
  final Function(Map<String, dynamic>) onRegister;

  const _RegisterPatientBottomSheet({Key? key, required this.onRegister}) : super(key: key);

  @override
  State<_RegisterPatientBottomSheet> createState() => _RegisterPatientBottomSheetState();
}

class _RegisterPatientBottomSheetState extends State<_RegisterPatientBottomSheet> {
  final _formKey = GlobalKey<FormState>();

  final _nameController = TextEditingController();
  final _ageController = TextEditingController();
  final _phoneController = TextEditingController();
  final _addressController = TextEditingController();
  final _allergiesController = TextEditingController();
  final _complaintController = TextEditingController();

  // Vitals
  final _bpController = TextEditingController(text: '120/80');
  final _pulseController = TextEditingController(text: '76');
  final _tempController = TextEditingController(text: '98.6');
  final _spo2Controller = TextEditingController(text: '98');
  final _weightController = TextEditingController(text: '60');

  String _gender = 'MALE';

  @override
  void dispose() {
    _nameController.dispose();
    _ageController.dispose();
    _phoneController.dispose();
    _addressController.dispose();
    _allergiesController.dispose();
    _complaintController.dispose();
    _bpController.dispose();
    _pulseController.dispose();
    _tempController.dispose();
    _spo2Controller.dispose();
    _weightController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 16,
        bottom: MediaQuery.of(context).viewInsets.bottom + 16,
      ),
      child: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: const [
                      Icon(Icons.person_add_alt_1, color: AppTheme.primaryGreen, size: 20),
                      SizedBox(width: 8),
                      Text(
                        'Register New Patient & Enqueue Visit',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.slateDark,
                        ),
                      ),
                    ],
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, size: 18),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const Text(
                'လူနာသစ်မှတ်ပုံတင်ခြင်းနှင့် စောင့်ဆိုင်းစာရင်းသို့ ထည့်သွင်းခြင်း',
                style: TextStyle(fontSize: 10.5, color: AppTheme.slateMuted),
              ),
              const SizedBox(height: 12),

              // Full Name & Age & Gender
              Row(
                children: [
                  Expanded(
                    flex: 3,
                    child: TextFormField(
                      controller: _nameController,
                      style: const TextStyle(fontSize: 12),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Patient Full Name *',
                        hintText: 'e.g. U Thant Zin, Daw Aye Aye',
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                      validator: (val) => val == null || val.trim().isEmpty ? 'Name is required' : null,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    flex: 1,
                    child: TextFormField(
                      controller: _ageController,
                      keyboardType: TextInputType.number,
                      style: const TextStyle(fontSize: 12),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Age *',
                        hintText: '35',
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                      validator: (val) => val == null || int.tryParse(val) == null ? 'Valid age' : null,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    flex: 2,
                    child: DropdownButtonFormField<String>(
                      value: _gender,
                      style: const TextStyle(fontSize: 12, color: AppTheme.slateDark),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Gender',
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                      items: const [
                        DropdownMenuItem(value: 'MALE', child: Text('Male')),
                        DropdownMenuItem(value: 'FEMALE', child: Text('Female')),
                        DropdownMenuItem(value: 'OTHER', child: Text('Other')),
                      ],
                      onChanged: (val) {
                        if (val != null) setState(() => _gender = val);
                      },
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 10),

              // Phone & Allergies
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _phoneController,
                      keyboardType: TextInputType.phone,
                      style: const TextStyle(fontSize: 12),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Phone Number *',
                        hintText: '09-421008761',
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                      validator: (val) => val == null || val.trim().isEmpty ? 'Phone is required' : null,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextFormField(
                      controller: _allergiesController,
                      style: const TextStyle(fontSize: 12),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Known Drug Allergies',
                        hintText: 'e.g. Penicillin, Sulfa, NSAIDs',
                        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 10),

              // Chief Complaint
              TextFormField(
                controller: _complaintController,
                maxLines: 2,
                style: const TextStyle(fontSize: 12),
                decoration: const InputDecoration(
                  isDense: true,
                  labelText: 'Chief Complaint & Symptoms *',
                  hintText: 'e.g. High grade fever with chills, productive cough for 3 days',
                  contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                ),
                validator: (val) => val == null || val.trim().isEmpty ? 'Complaint required' : null,
              ),

              const SizedBox(height: 10),

              // Triage Vitals Row
              const Text(
                'Triage Vital Signs (အရေးကြီးအချက်အလက်များ):',
                style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
              ),
              const SizedBox(height: 6),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _bpController,
                      style: const TextStyle(fontSize: 11),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'BP',
                        hintText: '120/80',
                        contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: TextFormField(
                      controller: _pulseController,
                      style: const TextStyle(fontSize: 11),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Pulse',
                        hintText: '78',
                        contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: TextFormField(
                      controller: _tempController,
                      style: const TextStyle(fontSize: 11),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Temp (°F)',
                        hintText: '98.6',
                        contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: TextFormField(
                      controller: _spo2Controller,
                      style: const TextStyle(fontSize: 11),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'SpO2 (%)',
                        hintText: '98',
                        contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: TextFormField(
                      controller: _weightController,
                      style: const TextStyle(fontSize: 11),
                      decoration: const InputDecoration(
                        isDense: true,
                        labelText: 'Weight (kg)',
                        hintText: '62',
                        contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                      ),
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 14),

              // Submit Button
              SizedBox(
                width: double.infinity,
                height: 38,
                child: ElevatedButton.icon(
                  onPressed: () {
                    if (_formKey.currentState?.validate() == true) {
                      widget.onRegister({
                        'fullName': _nameController.text.trim(),
                        'age': int.parse(_ageController.text.trim()),
                        'gender': _gender,
                        'phone': _phoneController.text.trim(),
                        'address': _addressController.text.trim(),
                        'allergies': _allergiesController.text.trim(),
                        'chiefComplaint': _complaintController.text.trim(),
                        'bp': _bpController.text.trim(),
                        'pulse': '${_pulseController.text.trim()} bpm',
                        'temp': '${_tempController.text.trim()}°F',
                        'spo2': '${_spo2Controller.text.trim()}%',
                        'weight': '${_weightController.text.trim()} kg',
                      });
                    }
                  },
                  icon: const Icon(Icons.check_circle, color: Colors.white, size: 16),
                  label: const Text(
                    'Save Patient & Add to Waiting Queue',
                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryGreen,
                    shape: RoundedCornerShape(AppTheme.buttonRadius),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
