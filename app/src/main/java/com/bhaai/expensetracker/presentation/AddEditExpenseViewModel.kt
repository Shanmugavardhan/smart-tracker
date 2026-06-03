package com.bhaai.expensetracker.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.ExpenseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val expenseId: Long? = savedStateHandle.get<Long>("expenseId")

    private val _state = MutableStateFlow(AddEditExpenseState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentExpense: Expense? = null

    init {
        expenseId?.let { id ->
            if (id != -1L) {
                viewModelScope.launch {
                    repository.getExpenseById(id)?.let { expense ->
                        currentExpense = expense
                        _state.value = AddEditExpenseState(
                            amount = expense.amount.toString(),
                            description = expense.description,
                            category = expense.category,
                            isEditing = true
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditExpenseEvent) {
        when (event) {
            is AddEditExpenseEvent.EnteredAmount -> {
                _state.value = _state.value.copy(amount = event.value)
            }
            is AddEditExpenseEvent.EnteredDescription -> {
                _state.value = _state.value.copy(description = event.value)
            }
            is AddEditExpenseEvent.SelectedCategory -> {
                _state.value = _state.value.copy(category = event.category)
            }
            is AddEditExpenseEvent.SaveExpense -> {
                viewModelScope.launch {
                    try {
                        val amountDouble = _state.value.amount.toDoubleOrNull() ?: throw Exception("Invalid amount")
                        if (_state.value.description.isBlank()) throw Exception("Description cannot be empty")

                        // Default to OTHER if no category selected initially (for V1 local version)
                        val categoryToSave = _state.value.category ?: Category.OTHER

                        val expense = Expense(
                            id = currentExpense?.id ?: 0,
                            amount = amountDouble,
                            description = _state.value.description,
                            category = categoryToSave,
                            status = ExpenseStatus.CATEGORIZED,
                            timestamp = currentExpense?.timestamp ?: System.currentTimeMillis()
                        )

                        if (currentExpense != null) {
                            repository.updateExpense(expense)
                        } else {
                            repository.insertExpense(expense)
                        }
                        _eventFlow.emit(UiEvent.SaveExpense)
                    } catch (e: Exception) {
                        _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Couldn't save expense"))
                    }
                }
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String): UiEvent()
        object SaveExpense: UiEvent()
    }
}

data class AddEditExpenseState(
    val amount: String = "",
    val description: String = "",
    val category: Category? = null,
    val isEditing: Boolean = false
)

sealed class AddEditExpenseEvent {
    data class EnteredAmount(val value: String): AddEditExpenseEvent()
    data class EnteredDescription(val value: String): AddEditExpenseEvent()
    data class SelectedCategory(val category: Category): AddEditExpenseEvent()
    object SaveExpense: AddEditExpenseEvent()
}
