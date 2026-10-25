import 'package:drift/drift.dart';

/// 1. Patients Table
class Patients extends Table {
  TextColumn get id => text()(); // UUID / Unique ID
  TextColumn get patientCode => text().unique().withLength(min: 3, max: 30)(); // e.g. HN-2026-0001
  TextColumn get fullName => text().withLength(min: 1, max: 150)();
  TextColumn get gender => text().withLength(max: 10)(); // MALE, FEMALE, OTHER
  IntColumn get age => integer()();
  DateTimeColumn get dob => dateTime().nullable()();
  TextColumn get phone => text().withLength(max: 30)();
  TextColumn get address => text().nullable()();
  TextColumn get allergies => text().withDefault(const Constant(''))(); // Comma-separated or JSON string
  DateTimeColumn get registeredAt => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 2. Suppliers Table
class Suppliers extends Table {
  TextColumn get id => text()();
  TextColumn get supplierName => text().withLength(min: 1, max: 150)();
  TextColumn get phoneNumber => text().withLength(max: 30)();
  TextColumn get companyAddress => text().nullable()();
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 3. Products Table (Master Drug & Consumable Catalog)
class Products extends Table {
  TextColumn get id => text()();
  TextColumn get tradeName => text().withLength(min: 1, max: 150)();
  TextColumn get genericName => text().withLength(min: 1, max: 150)();
  TextColumn get category => text().withLength(max: 50)(); // Antibiotic, Analgesic, Consumable, etc.
  TextColumn get unitType => text().withLength(max: 30)(); // Tablet, Capsule, Bottle, Vial, Strip
  IntColumn get minStockWarning => integer().withDefault(const Constant(10))();
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 4. ProductBatches Table (Supports FEFO - First Expiry, First Out)
class ProductBatches extends Table {
  TextColumn get id => text()();
  TextColumn get productId => text().references(Products, #id, onDelete: KeyAction.cascade)();
  TextColumn get supplierId => text().nullable().references(Suppliers, #id, onDelete: KeyAction.setNull)();
  TextColumn get batchNumber => text().withLength(min: 1, max: 50)();
  TextColumn get manufacturer => text().nullable()();
  DateTimeColumn get manufactureDate => dateTime().nullable()();
  DateTimeColumn get expiryDate => dateTime()(); // Key sorting column for FEFO
  RealColumn get costPrice => real()();
  RealColumn get profitMarginPercent => real().withDefault(const Constant(20.0))();
  RealColumn get sellingPrice => real()(); // Calculated: costPrice * (1 + margin/100)
  IntColumn get quantityInStock => integer().withDefault(const Constant(0))();
  DateTimeColumn get receivedAt => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 5. Procedures Table (Preset Minor Surgery & Nursing Interventions)
class Procedures extends Table {
  TextColumn get id => text()();
  TextColumn get name => text().withLength(min: 1, max: 150)();
  RealColumn get baseProcedureFee => real().withDefault(const Constant(0.0))();
  RealColumn get riskNursingFee => real().withDefault(const Constant(0.0))();
  TextColumn get consumablesJson => text().nullable()();

  @override
  Set<Column> get primaryKey => {id};
}

/// 6. Visits Table (EMR, Consultation, SOAP & Drawing Canvas)
class Visits extends Table {
  TextColumn get id => text()();
  TextColumn get patientId => text().references(Patients, #id, onDelete: KeyAction.cascade)();
  TextColumn get doctorId => text().withDefault(const Constant('DOC-01'))();
  TextColumn get status => text().withDefault(const Constant('WAITING'))(); // WAITING, IN_CONSULTATION, AT_PHARMACY, AT_CASHIER, COMPLETED
  TextColumn get vitalsJson => text().nullable()(); // BP, Pulse, Temp, SpO2, Weight, RespRate
  TextColumn get chiefComplaint => text().nullable()();
  TextColumn get objectiveExam => text().nullable()();
  TextColumn get diagnosis => text().nullable()();
  TextColumn get planAndAdvice => text().nullable()();
  TextColumn get drawingCanvasData => text().nullable()(); // Serialized vector points / SVG paths
  DateTimeColumn get visitDate => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 7. Invoices Table (Financials, Cashier POS & Billing)
class Invoices extends Table {
  TextColumn get id => text()();
  TextColumn get invoiceNumber => text().unique().withLength(min: 3, max: 50)(); // e.g. INV-2026-0001
  TextColumn get patientId => text().references(Patients, #id, onDelete: KeyAction.cascade)();
  TextColumn get visitId => text().nullable().references(Visits, #id, onDelete: KeyAction.setNull)();
  RealColumn get subtotal => real().withDefault(const Constant(0.0))();
  RealColumn get discount => real().withDefault(const Constant(0.0))();
  RealColumn get grandTotal => real().withDefault(const Constant(0.0))();
  RealColumn get paidAmount => real().withDefault(const Constant(0.0))();
  TextColumn get paymentMethod => text().withDefault(const Constant('CASH'))(); // CASH, KBZ_PAY, WAVE_PAY, CARD
  TextColumn get cashierId => text().withDefault(const Constant('CASHIER-01'))();
  BoolColumn get isPaid => boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 8. InvoiceItems Table (Line Items for Medicines, Procedures & Fees)
class InvoiceItems extends Table {
  TextColumn get id => text()();
  TextColumn get invoiceId => text().references(Invoices, #id, onDelete: KeyAction.cascade)();
  TextColumn get itemDescription => text().withLength(min: 1, max: 200)();
  TextColumn get batchId => text().nullable().references(ProductBatches, #id, onDelete: KeyAction.setNull)();
  IntColumn get quantity => integer().withDefault(const Constant(1))();
  RealColumn get unitPrice => real()();
  RealColumn get totalPrice => real()();

  @override
  Set<Column> get primaryKey => {id};
}

/// 9. InventoryLedger Table (Audit Trail for Real-Time FEFO Stock Adjustments)
class InventoryLedger extends Table {
  TextColumn get id => text()();
  TextColumn get batchId => text().references(ProductBatches, #id, onDelete: KeyAction.cascade)();
  TextColumn get transactionType => text()(); // INWARD_PURCHASE, DISPENSED_PRESCRIPTION, RETURN, ADJUSTMENT, SPOILAGE
  IntColumn get quantityChange => integer()(); // Positive or negative integer
  TextColumn get referenceId => text().nullable()(); // Invoice ID or Stock GRN
  TextColumn get notes => text().nullable()();
  DateTimeColumn get timestamp => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

/// 10. Users Table (Role-Based Access Control, Auth & Salted Password Hashing)
class Users extends Table {
  TextColumn get id => text()(); // UUID / Unique ID
  TextColumn get username => text().unique().withLength(min: 3, max: 50)();
  TextColumn get passwordHash => text()(); // Stored using SHA-256 with salt
  TextColumn get fullName => text().withLength(min: 1, max: 150)();
  TextColumn get role => text()(); // 'ADMIN', 'DOCTOR', 'PHARMACIST', 'CASHIER'
  TextColumn get samaNumber => text().nullable()(); // Myanmar Medical Council Reg No. e.g. 47414
  BoolColumn get isActive => boolean().withDefault(const Constant(true))();
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();

  @override
  Set<Column> get primaryKey => {id};
}

