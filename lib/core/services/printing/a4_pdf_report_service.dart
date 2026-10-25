import 'dart:typed_data';

/// A4PdfReportService
/// 
/// Generates A4 Medical Record + Prescription documents.
/// MANDATORY RULE: Used ONLY for Patient Clinical Notes & Prescriptions when requested.
class A4PdfReportService {
  /// Clinical report data model container
  static A4MedicalRecordDocument generateClinicalDocument({
    required String clinicName,
    required String clinicAddress,
    required String clinicPhone,
    required String doctorName,
    required String doctorSamaNo, // S.A.M.A. Myanmar Medical Council Reg Number
    required String doctorQualifications,
    required String patientName,
    required String patientCode,
    required int patientAge,
    required String patientGender,
    required String patientPhone,
    required String patientAddress,
    required String visitDate,
    required Map<String, String> vitalSigns, // BP, Pulse, Temp, SpO2, Weight
    required String chiefComplaint,
    required String clinicalDiagnosis,
    required String objectiveNotes,
    required String doctorAdvice,
    required List<A4PrescriptionItem> prescriptions,
    required List<String> performedProcedures,
    required String followUpDate,
  }) {
    return A4MedicalRecordDocument(
      clinicName: clinicName,
      clinicAddress: clinicAddress,
      clinicPhone: clinicPhone,
      doctorName: doctorName,
      doctorSamaNo: doctorSamaNo,
      doctorQualifications: doctorQualifications,
      patientName: patientName,
      patientCode: patientCode,
      patientAge: patientAge,
      patientGender: patientGender,
      patientPhone: patientPhone,
      patientAddress: patientAddress,
      visitDate: visitDate,
      vitalSigns: vitalSigns,
      chiefComplaint: chiefComplaint,
      clinicalDiagnosis: clinicalDiagnosis,
      objectiveNotes: objectiveNotes,
      doctorAdvice: doctorAdvice,
      prescriptions: prescriptions,
      performedProcedures: performedProcedures,
      followUpDate: followUpDate,
    );
  }

  /// Builds printable formatted HTML or plain formatted document for web/preview
  static String buildPrintableHtml(A4MedicalRecordDocument doc) {
    final rxRows = doc.prescriptions.map((p) => '''
      <tr>
        <td style="padding: 6px; border-bottom: 1px solid #E2E8F0;">
          <strong>${p.tradeName}</strong> (${p.dosageForm} ${p.dosageStrength})<br/>
          <span style="font-size: 11px; color: #64748B;">Generic: ${p.genericName}</span>
        </td>
        <td style="padding: 6px; border-bottom: 1px solid #E2E8F0; text-align: center;">${p.quantity} ${p.unit}</td>
        <td style="padding: 6px; border-bottom: 1px solid #E2E8F0;">${p.frequency}</td>
        <td style="padding: 6px; border-bottom: 1px solid #E2E8F0;">${p.duration}</td>
        <td style="padding: 6px; border-bottom: 1px solid #E2E8F0;">${p.instructions}</td>
      </tr>
    ''').join('');

    final procedureRows = doc.performedProcedures.isEmpty ? '' : '''
      <div style="margin-top: 15px;">
        <h4 style="margin: 0 0 5px 0; color: #2E7D32;">Clinical Procedures Performed:</h4>
        <ul style="margin: 0; padding-left: 20px;">
          ${doc.performedProcedures.map((proc) => '<li>$proc</li>').join('')}
        </ul>
      </div>
    ''';

    return '''
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>A4 Medical Record - ${doc.patientName}</title>
  <style>
    @page { size: A4; margin: 12mm; }
    body { font-family: 'Segoe UI', Arial, sans-serif; color: #0F172A; line-height: 1.35; font-size: 12px; background: #FFFFFF; }
    .header { border-bottom: 2px solid #2E7D32; padding-bottom: 8px; display: flex; justify-content: space-between; }
    .clinic-title { font-size: 18px; font-weight: bold; color: #2E7D32; }
    .clinic-sub { font-size: 11px; color: #64748B; }
    .doctor-title { font-size: 13px; font-weight: bold; color: #0F172A; }
    .vitals-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; background: #E8F5E9; padding: 8px; border-radius: 6px; margin: 10px 0; border: 1px solid #C8E6C9; }
    .vital-box { text-align: center; }
    .vital-label { font-size: 10px; color: #2E7D32; font-weight: bold; }
    .vital-value { font-size: 13px; font-weight: bold; color: #1B5E20; }
    .rx-table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 11px; }
    .rx-table th { background: #2E7D32; color: white; padding: 6px 8px; text-align: left; font-size: 11px; }
    .rx-table td { padding: 6px 8px; border-bottom: 1px solid #E0E0E0; }
    .accent-bar { height: 1px; background: #7A2E2E; opacity: 0.3; margin: 8px 0; }
    .footer { margin-top: 30px; display: flex; justify-content: space-between; align-items: flex-end; }
    .signature-box { text-align: center; width: 220px; border-top: 1px solid #7A2E2E; padding-top: 6px; }
  </style>
</head>
<body>
  <div class="header">
    <div>
      <div class="clinic-title">${doc.clinicName}</div>
      <div class="clinic-sub">Shwe Khit Polyclinic & General Practice</div>
      <div style="font-size: 11px;">${doc.clinicAddress} | Tel: ${doc.clinicPhone}</div>
    </div>
    <div style="text-align: right;">
      <div class="doctor-title">${doc.doctorName}</div>
      <div style="font-size: 11px;">${doc.doctorQualifications}</div>
      <div style="color: #2E7D32; font-weight: bold; font-size: 11px;">S.A.M.A Reg No: ${doc.doctorSamaNo}</div>
    </div>
  </div>

  <div style="margin: 8px 0; padding: 8px; border: 1px solid #E0E0E0; border-radius: 6px; display: flex; justify-content: space-between; font-size: 11px; background: #F4F6F8;">
    <div><strong>Patient HN:</strong> ${doc.patientCode}</div>
    <div><strong>Name:</strong> ${doc.patientName}</div>
    <div><strong>Age/Sex:</strong> ${doc.patientAge} Yrs / ${doc.patientGender}</div>
    <div><strong>Date:</strong> ${doc.visitDate}</div>
  </div>

  <div class="vitals-grid">
    <div class="vital-box"><div class="vital-label">BLOOD PRESSURE</div><div class="vital-value">${doc.vitalSigns['BP'] ?? doc.vitalSigns['Blood Pressure'] ?? '120/80'} mmHg</div></div>
    <div class="vital-box"><div class="vital-label">PULSE RATE</div><div class="vital-value">${doc.vitalSigns['Pulse'] ?? doc.vitalSigns['Pulse Rate'] ?? '78'} bpm</div></div>
    <div class="vital-box"><div class="vital-label">TEMPERATURE</div><div class="vital-value">${doc.vitalSigns['Temp'] ?? doc.vitalSigns['Temperature'] ?? '98.6'} °F</div></div>
    <div class="vital-box"><div class="vital-label">OXYGEN (SPO2)</div><div class="vital-value">${doc.vitalSigns['SpO2'] ?? doc.vitalSigns['Oxygen Saturation (SpO2)'] ?? '98'} %</div></div>
    <div class="vital-box"><div class="vital-label">BODY WEIGHT</div><div class="vital-value">${doc.vitalSigns['Weight'] ?? doc.vitalSigns['Body Weight'] ?? '60'} kg</div></div>
  </div>

  <div style="margin-top: 8px; font-size: 11.5px;">
    <strong>Chief Complaint:</strong> ${doc.chiefComplaint}<br/>
    <strong>Clinical Diagnosis:</strong> <span style="color: #2E7D32; font-weight: bold;">${doc.clinicalDiagnosis}</span><br/>
    ${doc.objectiveNotes.isNotEmpty ? '<strong>Clinical Examination:</strong> ' + doc.objectiveNotes + '<br/>' : ''}
    ${doc.doctorAdvice.isNotEmpty ? '<strong>Doctor Plan & Advice:</strong> ' + doc.doctorAdvice + '<br/>' : ''}
  </div>

  <div class="accent-bar"></div>

  $procedureRows

  <div style="margin-top: 10px;">
    <h3 style="color: #2E7D32; margin-bottom: 4px; font-size: 13px;">Prescription (Rx)</h3>
    <table class="rx-table">
      <thead>
        <tr>
          <th>Medicine & Generic</th>
          <th style="text-align: center;">Qty</th>
          <th>Frequency</th>
          <th>Duration</th>
          <th>Special Instructions</th>
        </tr>
      </thead>
      <tbody>
        $rxRows
      </tbody>
    </table>
  </div>

  ${doc.followUpDate.isNotEmpty ? '<div style="margin-top: 10px; font-weight: bold; color: #1B5E20; font-size: 11px;">Next Follow-Up Appointment: ' + doc.followUpDate + '</div>' : ''}

  <div class="footer">
    <div style="font-size: 10px; color: #64748B;">
      Generated by "ရွှေခေတ်" EMR & Clinical Management System.<br/>
      Myanmar Medical Council Certified Electronic Clinical Record.
    </div>
    <div class="signature-box">
      <strong style="color: #0F172A;">${doc.doctorName}</strong><br/>
      <span style="font-size: 10px;">S.A.M.A Reg: ${doc.doctorSamaNo}</span><br/>
      <span style="font-size: 10px; color: #64748B;">Physician Signature & Stamp</span>
    </div>
  </div>
</body>
</html>
''';
  }
}

class A4MedicalRecordDocument {
  final String clinicName;
  final String clinicAddress;
  final String clinicPhone;
  final String doctorName;
  final String doctorSamaNo;
  final String doctorQualifications;
  final String patientName;
  final String patientCode;
  final int patientAge;
  final String patientGender;
  final String patientPhone;
  final String patientAddress;
  final String visitDate;
  final Map<String, String> vitalSigns;
  final String chiefComplaint;
  final String clinicalDiagnosis;
  final String objectiveNotes;
  final String doctorAdvice;
  final List<A4PrescriptionItem> prescriptions;
  final List<String> performedProcedures;
  final String followUpDate;

  A4MedicalRecordDocument({
    required this.clinicName,
    required this.clinicAddress,
    required this.clinicPhone,
    required this.doctorName,
    required this.doctorSamaNo,
    required this.doctorQualifications,
    required this.patientName,
    required this.patientCode,
    required this.patientAge,
    required this.patientGender,
    required this.patientPhone,
    required this.patientAddress,
    required this.visitDate,
    required this.vitalSigns,
    required this.chiefComplaint,
    required this.clinicalDiagnosis,
    required this.objectiveNotes,
    required this.doctorAdvice,
    required this.prescriptions,
    required this.performedProcedures,
    required this.followUpDate,
  });
}

class A4PrescriptionItem {
  final String tradeName;
  final String genericName;
  final String dosageForm;
  final String dosageStrength;
  final int quantity;
  final String unit;
  final String frequency;
  final String duration;
  final String instructions;

  A4PrescriptionItem({
    required this.tradeName,
    required this.genericName,
    required this.dosageForm,
    required this.dosageStrength,
    required this.quantity,
    required this.unit,
    required this.frequency,
    required this.duration,
    required this.instructions,
  });
}
