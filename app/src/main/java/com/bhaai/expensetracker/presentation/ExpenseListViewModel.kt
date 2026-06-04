package com.bhaai.expensetracker.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bhaai.expensetracker.data.NetworkMonitor
import com.bhaai.expensetracker.data.worker.ExpenseCategorizationWorker
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.CategoryTotal
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.ExpenseStatus
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.TransactionType
import com.bhaai.expensetracker.domain.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class FinancialPosition(
    val cashOutflow: Double = 0.0,
    val moneyOwedToMe: Double = 0.0,
    val pendingSplitRecoveries: Double = 0.0,
    val loansOutstanding: Double = 0.0,
    val moneyIOweOthers: Double = 0.0,
    val netPosition: Double = 0.0,
    val actualPersonalSpending: Double = 0.0
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val categorizationService: ExpenseCategorizationService
) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val financialPosition: StateFlow<FinancialPosition> = combine(
        repository.getAllLedgerEntries(),
        repository.getAllEvents()
    ) { entries, events ->
        val eventMap = events.associateBy { it.id }
        
        var cashSum = 0.0
        var cashOutflow = 0.0
        var receivableSum = 0.0
        var pendingSplitSum = 0.0
        var loansOutstandingSum = 0.0
        var payableSum = 0.0
        var actualExpenseSum = 0.0
        
        entries.forEach { entry ->
            val event = eventMap[entry.eventId]
            when (entry.accountType) {
                AccountType.CASH -> {
                    cashSum += entry.amount
                    if (entry.amount < 0) {
                        cashOutflow += -entry.amount
                    }
                }
                AccountType.RECEIVABLE -> {
                    receivableSum += entry.amount
                    if (event != null) {
                        if (event.type == TransactionType.SHARED_EXPENSE) {
                            pendingSplitSum += entry.amount
                        } else if (event.type == TransactionType.LOAN_GIVEN || event.type == TransactionType.LOAN_REPAYMENT) {
                            loansOutstandingSum += entry.amount
                        }
                    }
                }
                AccountType.PAYABLE -> {
                    payableSum += entry.amount
                }
                AccountType.EXPENSE -> {
                    actualExpenseSum += entry.amount
                }
                else -> {}
            }
        }
        
        FinancialPosition(
            cashOutflow = cashOutflow,
            moneyOwedToMe = receivableSum,
            pendingSplitRecoveries = pendingSplitSum,
            loansOutstanding = loansOutstandingSum,
            moneyIOweOthers = payableSum,
            netPosition = cashSum + receivableSum - payableSum,
            actualPersonalSpending = actualExpenseSum
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialPosition()
    )

    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal = _monthlyTotal.asStateFlow()

    private val _categoryTotals = MutableStateFlow<List<CategoryTotal>>(emptyList())
    val categoryTotals = _categoryTotals.asStateFlow()

    private val _naturalLanguageText = MutableStateFlow("")
    val naturalLanguageText = _naturalLanguageText.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadMonthlyData()
    }

    fun onNaturalLanguageTextChange(value: String) {
        _naturalLanguageText.value = value
    }

    fun onQuickAdd(context: Context) {
        val text = _naturalLanguageText.value.trim()
        if (text.isBlank()) return

        _isParsing.value = true
        viewModelScope.launch {
            val networkMonitor = NetworkMonitor(context)
            if (!networkMonitor.isNetworkAvailable()) {
                val expense = Expense(
                    amount = 0.0,
                    description = text,
                    category = null,
                    status = ExpenseStatus.PENDING_CATEGORIZATION,
                    confidence = 0.0,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertExpense(expense)
                _naturalLanguageText.value = ""
                _isParsing.value = false

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val workRequest = OneTimeWorkRequestBuilder<ExpenseCategorizationWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "expense_categorization_work",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                _uiEvent.emit(UiEvent.ShowSnackbar("Saved offline. Will categorize when online."))
            } else {
                try {
                    val parsed = categorizationService.categorizeExpense(text)
                    _naturalLanguageText.value = ""
                    _isParsing.value = false
                    _uiEvent.emit(
                        UiEvent.NavigateToAddEdit(
                            amount = parsed.effectiveAmount?.toString() ?: "",
                            description = parsed.description,
                            category = parsed.category.name,
                            confidence = parsed.confidence,
                            subcategory = "",
                            splitCount = parsed.splitCount,
                            originalAmount = parsed.originalAmount
                        )
                    )
                } catch (e: Exception) {
                    val expense = Expense(
                        amount = 0.0,
                        description = text,
                        category = null,
                        status = ExpenseStatus.PENDING_CATEGORIZATION,
                        confidence = 0.0,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertExpense(expense)
                    _naturalLanguageText.value = ""
                    _isParsing.value = false
                    _uiEvent.emit(UiEvent.ShowSnackbar("AI categorization failed. Saved as pending."))
                }
            }
        }
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

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class NavigateToAddEdit(
            val amount: String,
            val description: String,
            val category: String,
            val confidence: Double,
            val subcategory: String? = null,
            val splitCount: Int? = null,
            val originalAmount: Double? = null
        ) : UiEvent()
    }
}
