package com.bhaai.expensetracker.presentation

import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.CategoryTotal
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
    private lateinit var categorizationService: com.bhaai.expensetracker.domain.ExpenseCategorizationService
    private lateinit var viewModel: ExpenseListViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        categorizationService = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads expenses and monthly total and category breakdown`() = runTest(testDispatcher) {
        val expense = Expense(
            id = 1L,
            amount = 100.0,
            description = "Food",
            category = Category.FOOD,
            status = ExpenseStatus.CATEGORIZED
        )

        val categoryTotals = listOf(CategoryTotal(Category.FOOD, 300.0), CategoryTotal(Category.TRAVEL, 50.0))

        val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())
        every { repository.getAllExpenses() } returns expensesFlow
        every { repository.getTotalAmountInRange(any(), any()) } returns flowOf(350.0)
        every { repository.getCategoryTotalsInRange(any(), any()) } returns flowOf(categoryTotals)

        viewModel = ExpenseListViewModel(repository, categorizationService)

        val collectJob = launch {
            viewModel.expenses.collect {}
        }

        expensesFlow.value = listOf(expense)

        val expenses = viewModel.expenses.value
        val total = viewModel.monthlyTotal.value
        val cats = viewModel.categoryTotals.value

        assertEquals(1, expenses.size)
        assertEquals(350.0, total, 0.0)
        assertEquals(2, cats.size)
        assertEquals(Category.FOOD, cats[0].category)
        assertEquals(300.0, cats[0].totalAmount, 0.0)
        assertEquals(Category.TRAVEL, cats[1].category)
        assertEquals(50.0, cats[1].totalAmount, 0.0)

        collectJob.cancel()
    }

    @Test
    fun `deleteExpense calls repository delete`() = runTest(testDispatcher) {
        every { repository.getAllExpenses() } returns flowOf(emptyList())
        every { repository.getTotalAmountInRange(any(), any()) } returns flowOf(0.0)
        every { repository.getCategoryTotalsInRange(any(), any()) } returns flowOf(emptyList())

        viewModel = ExpenseListViewModel(repository, categorizationService)

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
