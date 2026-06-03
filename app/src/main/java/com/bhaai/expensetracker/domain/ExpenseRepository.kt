package com.bhaai.expensetracker.domain

import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    suspend fun getExpenseById(id: Long): Expense?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    fun getTotalAmountInRange(startDate: Long, endDate: Long): Flow<Double>
    fun getCategoryTotalsInRange(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>
}
