package com.bhaai.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startDate AND timestamp <= :endDate")
    fun getTotalAmountInRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses WHERE timestamp >= :startDate AND timestamp <= :endDate GROUP BY category")
    fun getCategoryTotalsInRange(startDate: Long, endDate: Long): Flow<List<CategoryTotalEntity>>

    // --- New Ledger Methods ---
    @Query("SELECT * FROM financial_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE id = :id")
    suspend fun getEventById(id: String): FinancialEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: FinancialEventEntity)

    @Query("DELETE FROM financial_events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Query("SELECT * FROM ledger_entries")
    fun getAllLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE eventId = :eventId")
    fun getLedgerEntriesForEvent(eventId: String): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)

    @Query("DELETE FROM ledger_entries WHERE eventId = :eventId")
    suspend fun deleteLedgerEntriesForEvent(eventId: String)

    @Query("SELECT * FROM persons")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPerson(person: PersonEntity)

    @Query("SELECT * FROM obligations")
    fun getAllObligations(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations WHERE personId = :personId")
    fun getObligationsForPerson(personId: String): Flow<List<ObligationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligation(obligation: ObligationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligations(obligations: List<ObligationEntity>)

    @Update
    suspend fun updateObligation(obligation: ObligationEntity)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteObligation(id: String)
}
