package com.bhaai.expensetracker.presentation

import com.bhaai.expensetracker.domain.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class ReconciliationLogicTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reconciliation screen outputs correct cash flows`() = runTest(testDispatcher) {
        val repository = mockk<ExpenseRepository>(relaxed = true)
        val categorizationService = mockk<ExpenseCategorizationService>(relaxed = true)

        val event = FinancialEvent(
            id = "expense_1",
            type = TransactionType.SHARED_EXPENSE,
            description = "Bought a fridge worth 15k split among 4",
            timestamp = 1000L
        )

        val entries = listOf(
            LedgerEntry("cash_1", "expense_1", AccountType.CASH, -15000.0),
            LedgerEntry("expense_1", "expense_1", AccountType.EXPENSE, 3750.0),
            LedgerEntry("rec_1", "expense_1", AccountType.RECEIVABLE, 11250.0)
        )

        every { repository.getAllEvents() } returns flowOf(listOf(event))
        every { repository.getAllLedgerEntries() } returns flowOf(entries)

        val viewModel = ExpenseListViewModel(repository, categorizationService)

        val position = viewModel.financialPosition.first()

        assertEquals(15000.0, position.cashOutflow, 0.0)
        assertEquals(3750.0, position.actualPersonalSpending, 0.0)
        assertEquals(11250.0, position.moneyOwedToMe, 0.0)
    }
}
