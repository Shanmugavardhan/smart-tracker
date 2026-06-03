package com.bhaai.expensetracker.data

import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseRepositoryImplTest {

    private lateinit var dao: ExpenseDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = ExpenseRepositoryImpl(dao)
    }

    @Test
    fun `getAllExpenses maps entities to domain models`() = runTest {
        val entity = ExpenseEntity(
            id = 1L,
            amount = 100.0,
            description = "Test",
            category = Category.FOOD,
            subcategory = null,
            status = ExpenseStatus.CATEGORIZED,
            timestamp = 1000L
        )
        every { dao.getAllExpenses() } returns flowOf(listOf(entity))

        val result = repository.getAllExpenses().first()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(100.0, result[0].amount, 0.0)
    }

    @Test
    fun `insertExpense maps domain model to entity and calls dao`() = runTest {
        val expense = Expense(
            amount = 50.0,
            description = "Insert Test",
            category = Category.TRAVEL,
            status = ExpenseStatus.CATEGORIZED,
            timestamp = 2000L
        )
        coEvery { dao.insertExpense(any()) } returns 1L

        val result = repository.insertExpense(expense)

        assertEquals(1L, result)
        coVerify { dao.insertExpense(match {
            it.amount == 50.0 && it.description == "Insert Test" && it.category == Category.TRAVEL
        }) }
    }
}
