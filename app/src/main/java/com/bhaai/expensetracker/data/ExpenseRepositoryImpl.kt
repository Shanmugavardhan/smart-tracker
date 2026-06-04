package com.bhaai.expensetracker.data

import com.bhaai.expensetracker.domain.CategoryTotal
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.FinancialEvent
import com.bhaai.expensetracker.domain.LedgerEntry
import com.bhaai.expensetracker.domain.Person
import com.bhaai.expensetracker.domain.Obligation
import com.bhaai.expensetracker.domain.TransactionType
import com.bhaai.expensetracker.domain.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao
) : ExpenseRepository {

    // --- New Ledger Methods ---

    override fun getAllEvents(): Flow<List<FinancialEvent>> {
        return dao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEventById(id: String): FinancialEvent? {
        return dao.getEventById(id)?.toDomain()
    }

    override suspend fun insertEvent(
        event: FinancialEvent,
        entries: List<LedgerEntry>,
        obligations: List<Obligation>
    ) {
        dao.insertEvent(FinancialEventEntity.fromDomain(event))
        dao.insertLedgerEntries(entries.map { LedgerEntryEntity.fromDomain(it) })
        if (obligations.isNotEmpty()) {
            dao.insertObligations(obligations.map { ObligationEntity.fromDomain(it) })
        }
    }

    override suspend fun updateEvent(
        event: FinancialEvent,
        entries: List<LedgerEntry>,
        obligations: List<Obligation>
    ) {
        // Delete old ledger entries and obligations to replace them cleanly
        dao.deleteLedgerEntriesForEvent(event.id)
        // Note: For obligations, since we don't have eventId in schema, we can map/delete obligations or overwrite them.
        // For updates, we delete obligations matching the id list, then re-insert.
        dao.insertEvent(FinancialEventEntity.fromDomain(event))
        dao.insertLedgerEntries(entries.map { LedgerEntryEntity.fromDomain(it) })
        if (obligations.isNotEmpty()) {
            dao.insertObligations(obligations.map { ObligationEntity.fromDomain(it) })
        }
    }

    override suspend fun deleteEvent(id: String) {
        dao.deleteEvent(id)
    }

    override fun getAllLedgerEntries(): Flow<List<LedgerEntry>> {
        return dao.getAllLedgerEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLedgerEntriesForEvent(eventId: String): Flow<List<LedgerEntry>> {
        return dao.getLedgerEntriesForEvent(eventId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllPersons(): Flow<List<Person>> {
        return dao.getAllPersons().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getObligationsForPerson(personId: String): Flow<List<Obligation>> {
        return dao.getObligationsForPerson(personId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllObligations(): Flow<List<Obligation>> {
        return dao.getAllObligations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertPerson(person: Person) {
        dao.insertPerson(PersonEntity.fromDomain(person))
    }

    override suspend fun insertObligation(obligation: Obligation) {
        dao.insertObligation(ObligationEntity.fromDomain(obligation))
    }

    override suspend fun updateObligation(obligation: Obligation) {
        dao.updateObligation(ObligationEntity.fromDomain(obligation))
    }

    // --- Legacy / Compatibility ---

    override fun getAllExpenses(): Flow<List<Expense>> {
        return dao.getAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return dao.getExpenseById(id)?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense): Long {
        val id = dao.insertExpense(expense.toEntity())
        val savedExpense = expense.copy(id = id)
        syncLegacyExpenseToLedger(savedExpense)
        return id
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
        syncLegacyExpenseToLedger(expense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense.toEntity())
        dao.deleteEvent("expense_${expense.id}")
    }

    override fun getTotalAmountInRange(startDate: Long, endDate: Long): Flow<Double> {
        return dao.getTotalAmountInRange(startDate, endDate).map { it ?: 0.0 }
    }

    override fun getCategoryTotalsInRange(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> {
        return dao.getCategoryTotalsInRange(startDate, endDate).map { entities ->
            entities.map { CategoryTotal(it.category, it.totalAmount) }
        }
    }

    private suspend fun syncLegacyExpenseToLedger(expense: Expense) {
        val eventId = "expense_${expense.id}"
        val splitCount = expense.splitCount
        val originalAmount = expense.originalAmount
        val isShared = splitCount != null && splitCount > 1

        val eventType = if (isShared) TransactionType.SHARED_EXPENSE else TransactionType.EXPENSE
        val event = FinancialEvent(
            id = eventId,
            type = eventType,
            description = expense.description,
            timestamp = expense.timestamp,
            category = expense.category
        )

        val entries = mutableListOf<LedgerEntry>()
        val obligations = mutableListOf<Obligation>()

        if (isShared) {
            val total = originalAmount ?: expense.amount
            val personal = expense.amount
            val receivable = total - personal

            entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, -total))
            entries.add(LedgerEntry("${eventId}_expense", eventId, AccountType.EXPENSE, personal))
            entries.add(LedgerEntry("${eventId}_receivable", eventId, AccountType.RECEIVABLE, receivable))

            if (splitCount != null && splitCount > 1) {
                val roommates = splitCount - 1
                val share = receivable / roommates
                for (i in 1..roommates) {
                    val roommateId = "roommate_$i"
                    dao.insertPerson(PersonEntity(roommateId, "Roommate $i"))
                    obligations.add(Obligation("${eventId}_ob_$i", roommateId, share, "Split for ${expense.description}"))
                }
            }
        } else {
            entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, -expense.amount))
            entries.add(LedgerEntry("${eventId}_expense", eventId, AccountType.EXPENSE, expense.amount))
        }

        // Overwrite the event & entries in the database
        dao.deleteLedgerEntriesForEvent(eventId)
        dao.insertEvent(FinancialEventEntity.fromDomain(event))
        dao.insertLedgerEntries(entries.map { LedgerEntryEntity.fromDomain(it) })
        if (obligations.isNotEmpty()) {
            dao.insertObligations(obligations.map { ObligationEntity.fromDomain(it) })
        }
    }
}
