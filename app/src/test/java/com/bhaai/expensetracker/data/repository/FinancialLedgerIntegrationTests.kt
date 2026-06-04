package com.bhaai.expensetracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bhaai.expensetracker.data.Converters
import com.bhaai.expensetracker.data.ExpenseDatabase
import com.bhaai.expensetracker.data.ExpenseRepositoryImpl
import com.bhaai.expensetracker.data.NetworkMonitor
import com.bhaai.expensetracker.data.api.GeminiApi
import com.bhaai.expensetracker.domain.AccountType
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.FinancialEvent
import com.bhaai.expensetracker.domain.LedgerEntry
import com.bhaai.expensetracker.domain.Person
import com.bhaai.expensetracker.domain.Obligation
import com.bhaai.expensetracker.domain.ParsedExpense
import com.bhaai.expensetracker.domain.TransactionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinancialLedgerIntegrationTests {

    private lateinit var db: ExpenseDatabase
    private lateinit var repository: ExpenseRepository
    private lateinit var fallbackService: LocalFallbackExpenseCategorizationService
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExpenseRepositoryImpl(db.expenseDao)
        fallbackService = LocalFallbackExpenseCategorizationService()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun processPhrase(text: String): ParsedExpense {
        val parsed = fallbackService.categorizeExpense(text)
        val eventId = "event_${System.nanoTime()}"
        val event = FinancialEvent(
            id = eventId,
            type = parsed.transactionType,
            description = parsed.description,
            timestamp = System.currentTimeMillis(),
            category = parsed.category
        )
        val entries = mutableListOf<LedgerEntry>()
        val obligations = mutableListOf<Obligation>()

        when (parsed.transactionType) {
            TransactionType.EXPENSE -> {
                val amt = parsed.effectiveAmount ?: 0.0
                entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, -amt))
                entries.add(LedgerEntry("${eventId}_expense", eventId, AccountType.EXPENSE, amt))
            }
            TransactionType.SHARED_EXPENSE -> {
                val total = parsed.totalAmount ?: 0.0
                val personal = parsed.effectiveAmount ?: 0.0
                val receivable = total - personal
                entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, -total))
                entries.add(LedgerEntry("${eventId}_expense", eventId, AccountType.EXPENSE, personal))
                entries.add(LedgerEntry("${eventId}_receivable", eventId, AccountType.RECEIVABLE, receivable))

                if (parsed.splitCount != null && parsed.splitCount > 1) {
                    val roommates = parsed.splitCount - 1
                    val share = receivable / roommates
                    parsed.people.forEachIndexed { index, personName ->
                        val personId = "person_${personName.lowercase(Locale.ROOT).replace(" ", "_")}"
                        repository.insertPerson(Person(personId, personName))
                        obligations.add(Obligation("${eventId}_ob_$index", personId, share, "Split for ${parsed.description}"))
                    }
                }
            }
            TransactionType.LOAN_GIVEN -> {
                val amt = parsed.totalAmount ?: 0.0
                entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, -amt))
                entries.add(LedgerEntry("${eventId}_receivable", eventId, AccountType.RECEIVABLE, amt))

                parsed.person?.let { personName ->
                    val personId = "person_${personName.lowercase(Locale.ROOT).replace(" ", "_")}"
                    repository.insertPerson(Person(personId, personName))
                    obligations.add(Obligation("${eventId}_loan", personId, amt, "Loan to $personName"))
                }
            }
            TransactionType.LOAN_REPAYMENT -> {
                val amt = parsed.totalAmount ?: 0.0
                entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, amt))
                entries.add(LedgerEntry("${eventId}_receivable", eventId, AccountType.RECEIVABLE, -amt))

                parsed.person?.let { personName ->
                    val personId = "person_${personName.lowercase(Locale.ROOT).replace(" ", "_")}"
                    repository.insertPerson(Person(personId, personName))
                    obligations.add(Obligation("${eventId}_repay", personId, -amt, "Loan repayment from $personName"))
                }
            }
            TransactionType.REIMBURSEMENT_RECEIVED -> {
                val amt = parsed.totalAmount ?: 0.0
                entries.add(LedgerEntry("${eventId}_cash", eventId, AccountType.CASH, amt))
                entries.add(LedgerEntry("${eventId}_receivable", eventId, AccountType.RECEIVABLE, -amt))

                val roommateId = "person_roommate_a"
                repository.insertPerson(Person(roommateId, "Roommate A"))
                obligations.add(Obligation("${eventId}_reimburse", roommateId, -amt, "Reimbursement received"))
            }
            else -> {}
        }

        repository.insertEvent(event, entries, obligations)
        return parsed
    }

    @Test
    fun testPivotedFinancialLedgerEngine() = runTest {
        // Phrase 1: Groceries 3369 split among 4
        val parsed1 = processPhrase("Groceries 3369 split among 4")
        assertEquals(TransactionType.SHARED_EXPENSE, parsed1.transactionType)
        assertEquals(842.25, parsed1.effectiveAmount)
        assertEquals(3369.0, parsed1.totalAmount)
        assertEquals(4, parsed1.splitCount)

        // Phrase 2: Utensils 2400 split among 4
        val parsed2 = processPhrase("Utensils 2400 split among 4")
        assertEquals(TransactionType.SHARED_EXPENSE, parsed2.transactionType)
        assertEquals(600.0, parsed2.effectiveAmount)
        assertEquals(4, parsed2.splitCount)

        // Phrase 3: Rice Cooker 1804 split among 4
        val parsed3 = processPhrase("Rice Cooker 1804 split among 4")
        assertEquals(TransactionType.SHARED_EXPENSE, parsed3.transactionType)
        assertEquals(451.0, parsed3.effectiveAmount)

        // Phrase 4: Rice Bag 1700 split among 4
        val parsed4 = processPhrase("Rice Bag 1700 split among 4")
        assertEquals(TransactionType.SHARED_EXPENSE, parsed4.transactionType)
        assertEquals(425.0, parsed4.effectiveAmount)

        // Phrase 5: Gave Arun 3500 loan
        val parsed5 = processPhrase("Gave Arun 3500 loan")
        assertEquals(TransactionType.LOAN_GIVEN, parsed5.transactionType)
        assertEquals(3500.0, parsed5.totalAmount)
        assertEquals("Arun", parsed5.person)

        // Phrase 6: Arun returned 1000
        val parsed6 = processPhrase("Arun returned 1000")
        assertEquals(TransactionType.LOAN_REPAYMENT, parsed6.transactionType)
        assertEquals(1000.0, parsed6.totalAmount)
        assertEquals("Arun", parsed6.person)

        // Phrase 7: Paid rent 10000 roommates paid 2500 each
        val parsed7 = processPhrase("Paid rent 10000 roommates paid 2500 each")
        assertEquals(TransactionType.SHARED_EXPENSE, parsed7.transactionType)
        assertEquals(2500.0, parsed7.effectiveAmount)
        assertEquals(10000.0, parsed7.totalAmount)

        // Phrase 8: Received 451 split from roommate
        val parsed8 = processPhrase("Received 451 split from roommate")
        assertEquals(TransactionType.REIMBURSEMENT_RECEIVED, parsed8.transactionType)
        assertEquals(451.0, parsed8.totalAmount)

        // --- Validate Final Position Math ---
        val entries = repository.getAllLedgerEntries().first()
        val events = repository.getAllEvents().first()

        var cashSum = 0.0
        var receivableSum = 0.0
        var payableSum = 0.0

        entries.forEach { entry ->
            when (entry.accountType) {
                AccountType.CASH -> cashSum += entry.amount
                AccountType.RECEIVABLE -> receivableSum += entry.amount
                AccountType.PAYABLE -> payableSum += entry.amount
                else -> {}
            }
        }

        // Expected totals:
        // Cash sum: -(3369 + 2400 + 1804 + 1700 + 3500 - 1000 + 10000 - 451) = -21322.0
        assertEquals(-21322.0, cashSum, 0.01)

        // Receivables sum: 2526.75 + 1800.0 + 1353.0 + 1275.0 + 3500.0 - 1000.0 + 7500.0 - 451.0 = 16503.75
        assertEquals(16503.75, receivableSum, 0.01)

        // Net position: Cash + Receivables - Payables = -21322.0 + 16503.75 - 0.0 = -4818.25
        val netPosition = cashSum + receivableSum - payableSum
        assertEquals(-4818.25, netPosition, 0.01)

        // Outstanding balance for Arun: 3500.0 - 1000.0 = 2500.0
        val arunId = "person_arun"
        val arunObligations = repository.getAllObligations().first().filter { it.personId == arunId }
        val arunOutstanding = arunObligations.sumOf { it.amountOutstanding }
        assertEquals(2500.0, arunOutstanding, 0.01)

        // Outstanding balance for Roommate A: 842.25 + 600.0 + 451.0 + 425.0 + 2500.0 - 451.0 = 4367.25
        val roommateAId = "person_roommate_a"
        val roommateAObligations = repository.getAllObligations().first().filter { it.personId == roommateAId }
        val roommateAOutstanding = roommateAObligations.sumOf { it.amountOutstanding }
        assertEquals(4367.25, roommateAOutstanding, 0.01)
    }
}
