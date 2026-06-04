package com.bhaai.expensetracker.domain

import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    // --- New Ledger Methods ---
    fun getAllEvents(): Flow<List<FinancialEvent>>
    suspend fun getEventById(id: String): FinancialEvent?
    suspend fun insertEvent(event: FinancialEvent, entries: List<LedgerEntry>, obligations: List<Obligation> = emptyList())
    suspend fun updateEvent(event: FinancialEvent, entries: List<LedgerEntry>, obligations: List<Obligation> = emptyList())
    suspend fun deleteEvent(id: String)

    fun getAllLedgerEntries(): Flow<List<LedgerEntry>>
    fun getLedgerEntriesForEvent(eventId: String): Flow<List<LedgerEntry>>

    fun getAllPersons(): Flow<List<Person>>
    fun getObligationsForPerson(personId: String): Flow<List<Obligation>>
    fun getAllObligations(): Flow<List<Obligation>>
    suspend fun insertPerson(person: Person)
    suspend fun insertObligation(obligation: Obligation)
    suspend fun updateObligation(obligation: Obligation)

    // --- Legacy / Compatibility ---
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun getExpenseById(id: Long): Expense?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    fun getTotalAmountInRange(startDate: Long, endDate: Long): Flow<Double>
    fun getCategoryTotalsInRange(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>
}
