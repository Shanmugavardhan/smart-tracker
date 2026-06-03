package com.bhaai.expensetracker.presentation

import androidx.lifecycle.SavedStateHandle
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditExpenseViewModelCoverageTest {

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
    fun `save expense fails with invalid amount emits ShowSnackbar`() = runTest(testDispatcher) {
        viewModel = AddEditExpenseViewModel(repository, SavedStateHandle(mapOf("expenseId" to -1L)))
        viewModel.onEvent(AddEditExpenseEvent.EnteredAmount("invalid_number"))
        viewModel.onEvent(AddEditExpenseEvent.EnteredDescription("Test"))
        viewModel.onEvent(AddEditExpenseEvent.SelectedCategory(Category.OTHER))

        var emittedEvent: AddEditExpenseViewModel.UiEvent? = null
        val job = launch {
            emittedEvent = viewModel.eventFlow.first()
        }

        viewModel.onEvent(AddEditExpenseEvent.SaveExpense)

        assert(emittedEvent is AddEditExpenseViewModel.UiEvent.ShowSnackbar)
        assertEquals("Invalid amount", (emittedEvent as AddEditExpenseViewModel.UiEvent.ShowSnackbar).message)
        job.cancel()
    }

    @Test
    fun `save expense fails with empty description emits ShowSnackbar`() = runTest(testDispatcher) {
        viewModel = AddEditExpenseViewModel(repository, SavedStateHandle(mapOf("expenseId" to -1L)))
        viewModel.onEvent(AddEditExpenseEvent.EnteredAmount("100.0"))
        viewModel.onEvent(AddEditExpenseEvent.EnteredDescription(""))
        viewModel.onEvent(AddEditExpenseEvent.SelectedCategory(Category.OTHER))

        var emittedEvent: AddEditExpenseViewModel.UiEvent? = null
        val job = launch {
            emittedEvent = viewModel.eventFlow.first()
        }

        viewModel.onEvent(AddEditExpenseEvent.SaveExpense)

        assert(emittedEvent is AddEditExpenseViewModel.UiEvent.ShowSnackbar)
        assertEquals("Description cannot be empty", (emittedEvent as AddEditExpenseViewModel.UiEvent.ShowSnackbar).message)
        job.cancel()
    }
}
