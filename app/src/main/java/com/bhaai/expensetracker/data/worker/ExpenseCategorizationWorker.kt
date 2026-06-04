package com.bhaai.expensetracker.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.ExpenseStatus
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class ExpenseCategorizationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun expenseRepository(): ExpenseRepository
        fun expenseCategorizationService(): ExpenseCategorizationService
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )
        val repository = entryPoint.expenseRepository()
        val categorizationService = entryPoint.expenseCategorizationService()

        // Get the current snapshot of all expenses
        val pendingExpenses = repository.getAllExpenses().first().filter {
            it.status == ExpenseStatus.PENDING_CATEGORIZATION
        }

        if (pendingExpenses.isEmpty()) {
            return Result.success()
        }

        var hasFailure = false
        for (expense in pendingExpenses) {
            try {
                val result = categorizationService.categorizeExpense(expense.description)
                val updatedExpense = expense.copy(
                    amount = result.effectiveAmount ?: result.totalAmount ?: 0.0,
                    description = result.description,
                    category = result.category,
                    subcategory = null,
                    confidence = result.confidence,
                    status = ExpenseStatus.CATEGORIZED,
                    splitCount = result.splitCount,
                    originalAmount = result.originalAmount
                )
                repository.updateExpense(updatedExpense)
            } catch (e: Exception) {
                hasFailure = true
            }
        }

        return if (hasFailure) Result.retry() else Result.success()
    }
}
