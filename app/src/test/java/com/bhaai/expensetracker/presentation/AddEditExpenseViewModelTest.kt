package com.bhaai.expensetracker.presentation

import androidx.lifecycle.SavedStateHandle
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.ExpenseStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditExpenseViewModelTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: AddEditExpenseViewModel
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
    fun `load existing expense when initialized with id`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("expenseId" to 1L))
        val expense = Expense(
            id = 1L,
            amount = 150.0,
            description = "Test Edit",
            category = Category.TRAVEL,
            status = ExpenseStatus.CATEGORIZED
        )

        coEvery { repository.getExpenseById(1L) } returns expense

        viewModel = AddEditExpenseViewModel(repository, savedStateHandle)

        val state = viewModel.state.first()

        assertEquals("150.0", state.amount)
        assertEquals("Test Edit", state.description)
        assertEquals(Category.TRAVEL, state.category)
        assertTrue(state.isEditing)
    }

    @Test
    fun `save new expense inserts into repository`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("expenseId" to -1L))
        viewModel = AddEditExpenseViewModel(repository, savedStateHandle)

        viewModel.onEvent(AddEditExpenseEvent.EnteredAmount("500.0"))
        viewModel.onEvent(AddEditExpenseEvent.EnteredDescription("New item"))
        viewModel.onEvent(AddEditExpenseEvent.SelectedCategory(Category.SHOPPING))

        viewModel.onEvent(AddEditExpenseEvent.SaveExpense)

        coVerify { repository.insertExpense(match {
            it.amount == 500.0 &&
            it.description == "New item" &&
            it.category == Category.SHOPPING
        }) }
    }
}
