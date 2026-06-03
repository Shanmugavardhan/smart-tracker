package com.bhaai.expensetracker.presentation

import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.ExpenseStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: ExpenseListViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads expenses and monthly total`() = runTest(testDispatcher) {
        val expense = Expense(
            id = 1L,
            amount = 100.0,
            description = "Food",
            category = Category.FOOD,
            status = ExpenseStatus.CATEGORIZED
        )

        every { repository.getAllExpenses() } returns flowOf(listOf(expense))
        every { repository.getTotalAmountInRange(any(), any()) } returns flowOf(100.0)

        viewModel = ExpenseListViewModel(repository)

        // We need a collector to trigger WhileSubscribed StateFlows
        val collectJob = launch {
            viewModel.expenses.collect {}
        }

        val expenses = viewModel.expenses.value
        val total = viewModel.monthlyTotal.value

        assertEquals(1, expenses.size)
        assertEquals(100.0, expenses[0].amount, 0.0)
        assertEquals(100.0, total, 0.0)

        collectJob.cancel()
    }

    @Test
    fun `deleteExpense calls repository delete`() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        every { repository.getTotalAmountInRange(any(), any()) } returns flowOf(0.0)

        viewModel = ExpenseListViewModel(repository)

        val expense = Expense(
            id = 1L,
            amount = 50.0,
            description = "Test",
            category = Category.OTHER,
            status = ExpenseStatus.CATEGORIZED
        )

        viewModel.deleteExpense(expense)

        coVerify { repository.deleteExpense(expense) }
    }
}
