package com.example.data

import com.example.model.*
import java.util.UUID

object ClinicInitialDataSeeder {

  fun getSeedMedicines(): List<Medicine> = emptyList()

  fun getSeedProcedures(): List<ProcedureItem> = emptyList()

  fun getSeedPatients(): List<Patient> = emptyList()

  fun getSeedUsers(): List<UserAccount> = listOf(
    UserAccount(
      id = "USR-001-ADMIN",
      username = "admin",
      fullName = "Clinic Administrator",
      role = UserRole.ADMIN,
      passwordHash = "123456",
      pinCode = "123456",
      permissions = UserRolePermissions.defaultFor(UserRole.ADMIN)
    )
  )

  fun getSeedSuppliers(): List<Supplier> = emptyList()

  fun getSeedConsultations(): List<ConsultationRecord> = emptyList()

  fun getSeedInvoices(): List<Invoice> = emptyList()

  fun getSeedExpenses(): List<ExpenseRecord> = emptyList()

  fun getSeedVaccines(): List<VaccineRecord> = emptyList()

  fun getSeedSyncLogs(): List<SyncLog> = listOf(
    SyncLog(UUID.randomUUID().toString(), "Initial Setup", "App Initialized", "SUCCESS", "Fresh clean database ready")
  )
}
