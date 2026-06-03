package com.bhaai.expensetracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaai.expensetracker.domain.CategoryTotal
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal = _monthlyTotal.asStateFlow()

    private val _categoryTotals = MutableStateFlow<List<CategoryTotal>>(emptyList())
    val categoryTotals = _categoryTotals.asStateFlow()

    init {
        loadMonthlyData()
    }

    private fun loadMonthlyData() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        viewModelScope.launch {
            repository.getTotalAmountInRange(startDate, endDate)
                .catch { emit(0.0) }
                .collect { total ->
                    _monthlyTotal.value = total
                }
        }

        viewModelScope.launch {
            repository.getCategoryTotalsInRange(startDate, endDate)
                .catch { emit(emptyList()) }
                .collect { totals ->
                    _categoryTotals.value = totals
                }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}
