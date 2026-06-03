package com.bhaai.expensetracker.data

import com.bhaai.expensetracker.domain.CategoryTotal
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return dao.getAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return dao.getExpenseById(id)?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense): Long {
        return dao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense.toEntity())
    }

    override fun getTotalAmountInRange(startDate: Long, endDate: Long): Flow<Double> {
        return dao.getTotalAmountInRange(startDate, endDate).map { it ?: 0.0 }
    }

    override fun getCategoryTotalsInRange(startDate: Long, endDate: Long): Flow<List<CategoryTotal>> {
        return dao.getCategoryTotalsInRange(startDate, endDate).map { entities ->
            entities.map { CategoryTotal(it.category, it.totalAmount) }
        }
    }
}
