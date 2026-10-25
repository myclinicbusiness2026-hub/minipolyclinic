import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/database/app_database.dart';
import '../../../../core/database/database_provider.dart';
import '../../../../core/theme/app_theme.dart';

/// Patients Master Directory & Medical Records Screen
/// Refactored for Compact Clinical UI with Medical Green Theme (#2E7D32).
class PatientsScreen extends ConsumerStatefulWidget {
  const PatientsScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<PatientsScreen> createState() => _PatientsScreenState();
}

class _PatientsScreenState extends ConsumerState<PatientsScreen> {
  String _searchQuery = '';
  Patient? _selectedPatient;

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
            Icon(Icons.people_alt_rounded, color: AppTheme.primaryGreen, size: 20),
            SizedBox(width: 8),
            Text(
              'Patient Master Records & EMR',
              style: TextStyle(color: AppTheme.slateDark, fontWeight: FontWeight.bold, fontSize: 15),
            ),
            SizedBox(width: 8),
            Text(
              'လူနာမှတ်တမ်းချုပ်',
              style: TextStyle(color: AppTheme.slateMuted, fontSize: 11),
            ),
          ],
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(10.0),
        child: Column(
          children: [
            // Search Input
            Card(
              elevation: 0,
              color: AppTheme.cardPureWhite,
              shape: RoundedCornerShape(AppTheme.cardRadius),
              side: BorderSide(color: AppTheme.subtleBorder),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                child: Row(
                  children: [
                    const Icon(Icons.search, size: 16, color: AppTheme.slateMuted),
                    const SizedBox(width: 6),
                    Expanded(
                      child: TextField(
                        onChanged: (val) => setState(() => _searchQuery = val.trim().toLowerCase()),
                        decoration: const InputDecoration(
                          hintText: 'Search by Patient Name, HN Code, or Phone Number...',
                          hintStyle: TextStyle(fontSize: 11.5, color: AppTheme.slateMuted),
                          border: InputBorder.none,
                          isDense: true,
                          contentPadding: EdgeInsets.symmetric(vertical: 6),
                        ),
                        style: const TextStyle(fontSize: 11.5, color: AppTheme.slateDark),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 8),

            // Master Records List & Detail Pane
            Expanded(
              child: StreamBuilder<List<Patient>>(
                stream: db.select(db.patients).watch(),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator(color: AppTheme.primaryGreen));
                  }

                  final allPatients = snapshot.data ?? [];
                  final filtered = allPatients.where((p) {
                    if (_searchQuery.isEmpty) return true;
                    return p.fullName.toLowerCase().contains(_searchQuery) ||
                        p.patientCode.toLowerCase().contains(_searchQuery) ||
                        p.phone.contains(_searchQuery);
                  }).toList();

                  if (filtered.isEmpty) {
                    return const Center(
                      child: Text('No patient records found matching search.', style: TextStyle(color: AppTheme.slateMuted, fontSize: 12)),
                    );
                  }

                  _selectedPatient ??= filtered.first;

                  return LayoutBuilder(
                    builder: (context, constraints) {
                      final isWide = constraints.maxWidth >= 750;

                      if (isWide) {
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Left: Patients List
                            Expanded(
                              flex: 5,
                              child: _buildPatientsList(filtered),
                            ),
                            const SizedBox(width: 10),
                            // Right: Patient Profile & Visit History
                            Expanded(
                              flex: 5,
                              child: _buildPatientDetailPane(db, _selectedPatient!),
                            ),
                          ],
                        );
                      }

                      return _buildPatientsList(filtered);
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

  Widget _buildPatientsList(List<Patient> patients) {
    return ListView.separated(
      itemCount: patients.length,
      separatorBuilder: (_, __) => const SizedBox(height: 6),
      itemBuilder: (context, idx) {
        final p = patients[idx];
        final isSelected = p.id == _selectedPatient?.id;

        return Card(
          elevation: 0,
          color: AppTheme.cardPureWhite,
          shape: RoundedCornerShape(AppTheme.cardRadius),
          side: BorderSide(
            color: isSelected ? AppTheme.primaryGreen : AppTheme.subtleBorder,
            width: isSelected ? 1.5 : 1,
          ),
          child: ListTile(
            dense: true,
            contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            onTap: () => setState(() => _selectedPatient = p),
            leading: CircleAvatar(
              radius: 16,
              backgroundColor: isSelected ? AppTheme.primaryGreen : AppTheme.softGreenContainer,
              child: Text(
                p.fullName.isNotEmpty ? p.fullName[0].toUpperCase() : 'P',
                style: TextStyle(
                  color: isSelected ? AppTheme.cardPureWhite : AppTheme.primaryGreen,
                  fontWeight: FontWeight.bold,
                  fontSize: 11,
                ),
              ),
            ),
            title: Text(
              p.fullName,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12.5, color: AppTheme.slateDark),
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${p.patientCode} • ${p.age} Yrs • ${p.gender} • ${p.phone}',
                  style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMedium),
                ),
                if (p.allergies.isNotEmpty)
                  Text(
                    '⚠️ Allergy: ${p.allergies}',
                    style: const TextStyle(fontSize: 9.5, color: AppTheme.statusCritical, fontWeight: FontWeight.bold),
                  ),
              ],
            ),
            trailing: const Icon(Icons.chevron_right, color: AppTheme.slateMuted, size: 16),
          ),
        );
      },
    );
  }

  Widget _buildPatientDetailPane(AppDatabase db, Patient patient) {
    return Card(
      elevation: 0,
      color: AppTheme.cardPureWhite,
      shape: RoundedCornerShape(AppTheme.cardRadius),
      side: BorderSide(color: AppTheme.subtleBorder),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      patient.fullName,
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
                    ),
                    Text(
                      'Hospital No (HN): ${patient.patientCode}',
                      style: const TextStyle(fontSize: 11, color: AppTheme.primaryGreen, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                ElevatedButton.icon(
                  onPressed: () {
                    context.go('/consultation?patientId=${patient.id}');
                  },
                  icon: const Icon(Icons.play_arrow, size: 14, color: AppTheme.cardPureWhite),
                  label: const Text('Consult', style: TextStyle(color: AppTheme.cardPureWhite, fontSize: 11, fontWeight: FontWeight.bold)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryGreen,
                    elevation: 0,
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    shape: RoundedCornerShape(AppTheme.buttonRadius),
                  ),
                ),
              ],
            ),

            Divider(height: 16, color: AppTheme.subtleBorder),

            _buildDetailRow('Gender & Age', '${patient.gender}, ${patient.age} years old'),
            _buildDetailRow('Phone', patient.phone),
            _buildDetailRow('Address', patient.address ?? 'Yangon, Myanmar'),
            _buildDetailRow(
              'Known Allergies',
              patient.allergies.isNotEmpty ? patient.allergies : 'No known drug allergies',
              isWarning: patient.allergies.isNotEmpty,
            ),

            const SizedBox(height: 10),
            const Text(
              'Clinical Encounters & Visit History:',
              style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppTheme.slateDark),
            ),
            const SizedBox(height: 6),

            Expanded(
              child: FutureBuilder<List<Visit>>(
                future: (db.select(db.visits)..where((v) => v.patientId.equals(patient.id))).get(),
                builder: (context, snapshot) {
                  final visits = snapshot.data ?? [];
                  if (visits.isEmpty) {
                    return const Center(
                      child: Text('No past visit records for this patient.', style: TextStyle(fontSize: 11, color: AppTheme.slateMuted)),
                    );
                  }

                  return ListView.separated(
                    itemCount: visits.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 5),
                    itemBuilder: (ctx, idx) {
                      final v = visits[idx];
                      return Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF8FAFC),
                          borderRadius: BorderRadius.circular(AppTheme.buttonRadius),
                          border: Border.all(color: AppTheme.subtleBorder),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  'Visit Date: ${v.visitDate.toLocal().toString().split(' ')[0]}',
                                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 10.5, color: AppTheme.slateDark),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                                  decoration: BoxDecoration(color: AppTheme.softGreenContainer, borderRadius: BorderRadius.circular(4)),
                                  child: Text(
                                    v.status,
                                    style: const TextStyle(fontSize: 8.5, fontWeight: FontWeight.bold, color: AppTheme.primaryGreen),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Complaint: ${v.chiefComplaint ?? "General checkup"}',
                              style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMedium),
                            ),
                            if (v.diagnosis != null)
                              Text(
                                'Diagnosis: ${v.diagnosis}',
                                style: const TextStyle(fontSize: 9.5, fontWeight: FontWeight.bold, color: AppTheme.primaryGreen),
                              ),
                          ],
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

  Widget _buildDetailRow(String label, String value, {bool isWarning = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(label, style: const TextStyle(fontSize: 10.5, color: AppTheme.slateMuted, fontWeight: FontWeight.w600)),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 10.5,
                color: isWarning ? AppTheme.statusCritical : AppTheme.slateDark,
                fontWeight: isWarning ? FontWeight.bold : FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
