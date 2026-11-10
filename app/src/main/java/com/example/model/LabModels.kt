package com.example.model

import java.util.UUID

/**
 * Single measurable parameter in a lab test (e.g., Hemoglobin, Fasting Blood Sugar, Urea)
 */
data class LabTestParameter(
  val name: String,
  val result: String = "",
  val unit: String = "",
  val referenceRange: String = "",
  val flag: String = "" // "Normal", "Low", "High", "Critical", or custom text
) {
  val normalRange: String get() = referenceRange
}

/**
 * Pre-configured or user-defined dynamic test template
 */
data class LabTestTemplate(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val category: String = "Hematology", // Hematology, Biochemistry, Urine/Stool, Serology, Microbiology, Others
  val defaultPrice: Double = 0.0,
  val specimenType: String = "Blood", // Blood, Urine, Stool, Swab, Sputum, etc.
  val parameters: List<LabTestParameter> = emptyList(),
  val isAnalyzerSlipOnly: Boolean = false // If true, analyzer prints direct slip or needs image attachment
)

/**
 * An individual test item inside a patient's lab order
 */
data class LabOrderItem(
  val testId: String = UUID.randomUUID().toString(),
  val testName: String,
  val category: String = "General",
  val price: Double = 0.0,
  val specimenType: String = "Blood",
  val parameters: List<LabTestParameter> = emptyList(),
  val remarks: String = "",
  val attachedImageUri: String? = null,
  val isCompleted: Boolean = false
) {
  val templateName: String get() = testName
  val slipImageUri: String get() = attachedImageUri ?: ""
  val isAnalyzerSlipOnly: Boolean get() = parameters.isEmpty()
}

enum class LabOrderStatus {
  ORDERED,           // Doctor ordered or cashier booked
  SAMPLE_COLLECTED,  // Specimen taken from patient
  COMPLETED,         // Results entered and verified
  CANCELLED          // Order cancelled
}

/**
 * Complete patient lab requisition and result record
 */
data class LabOrder(
  val id: String = UUID.randomUUID().toString(),
  val orderNumber: String, // e.g. "LAB-20261110-001"
  val patientId: String,
  val patientName: String,
  val patientHn: String = "",
  val patientAge: Int = 0,
  val patientGender: Gender = Gender.MALE,
  val doctorName: String = "",
  val orderDate: String, // YYYY-MM-DD
  val orderTime: String = "",
  val items: List<LabOrderItem> = emptyList(),
  val status: LabOrderStatus = LabOrderStatus.ORDERED,
  val totalAmount: Double = 0.0,
  val paidAmount: Double = 0.0,
  val paymentStatus: String = "Unpaid", // "Paid", "Partial", "Unpaid"
  val invoiceNumber: String = "",
  val technicianName: String = "",
  val completedAt: String = "",
  val notes: String = ""
) {
  val isPaid: Boolean get() = paidAmount >= totalAmount && totalAmount > 0.0 || paymentStatus.equals("Paid", ignoreCase = true)
  val isFullyPaid: Boolean get() = isPaid
  val balanceDue: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
  val formattedItemNames: String get() = items.joinToString(", ") { it.testName }
  val referringDoctorName: String get() = doctorName
  val performedBy: String get() = technicianName
}

/**
 * Dedicated Lab Cashier Voucher / Receipt (Completely separate from Clinic OPD invoices)
 */
data class LabInvoice(
  val id: String = UUID.randomUUID().toString(),
  val invoiceNumber: String, // e.g. "L-INV-1001"
  val orderId: String,
  val patientId: String,
  val patientName: String,
  val patientHn: String = "",
  val items: List<LabOrderItem> = emptyList(),
  val totalAmount: Double = 0.0,
  val discount: Double = 0.0,
  val netTotal: Double = 0.0,
  val paidAmount: Double = 0.0,
  val changeAmount: Double = 0.0,
  val paymentMethod: PaymentMethod = PaymentMethod.CASH,
  val cashierName: String = "Cashier",
  val timestamp: String = ""
) {
  val subtotal: Double get() = totalAmount
  val date: String get() = timestamp
}
