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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ExpenseRepositoryImplMoreTest {

    private lateinit var dao: ExpenseDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = ExpenseRepositoryImpl(dao)
    }

    @Test
    fun `getExpenseById maps entity to domain`() = runTest {
        val entity = ExpenseEntity(
            id = 2L,
            amount = 200.0,
            description = "Test 2",
            category = Category.TRAVEL,
            subcategory = null,
            status = ExpenseStatus.CATEGORIZED,
            timestamp = 2000L
        )
        coEvery { dao.getExpenseById(2L) } returns entity

        val result = repository.getExpenseById(2L)

        assertEquals(2L, result?.id)
        assertEquals(200.0, result?.amount)
    }

    @Test
    fun `getExpenseById returns null when not found`() = runTest {
        coEvery { dao.getExpenseById(any()) } returns null
        assertNull(repository.getExpenseById(99L))
    }

    @Test
    fun `updateExpense maps domain to entity and calls dao`() = runTest {
        val expense = Expense(
            id = 1L,
            amount = 50.0,
            description = "Test",
            category = Category.OTHER,
            status = ExpenseStatus.CATEGORIZED
        )
        repository.updateExpense(expense)
        coVerify { dao.updateExpense(match { it.id == 1L && it.amount == 50.0 }) }
    }

    @Test
    fun `deleteExpense maps domain to entity and calls dao`() = runTest {
        val expense = Expense(id = 1L, amount = 50.0, description = "Test", category = Category.OTHER, status = ExpenseStatus.CATEGORIZED)
        repository.deleteExpense(expense)
        coVerify { dao.deleteExpense(match { it.id == 1L }) }
    }

    @Test
    fun `getTotalAmountInRange maps correctly`() = runTest {
        every { dao.getTotalAmountInRange(100L, 200L) } returns flowOf(500.0)
        assertEquals(500.0, repository.getTotalAmountInRange(100L, 200L).first(), 0.0)

        every { dao.getTotalAmountInRange(200L, 300L) } returns flowOf(null)
        assertEquals(0.0, repository.getTotalAmountInRange(200L, 300L).first(), 0.0)
    }

    @Test
    fun `getCategoryTotalsInRange maps entities to domain`() = runTest {
        val entity = CategoryTotalEntity(Category.FOOD, 1000.0)
        every { dao.getCategoryTotalsInRange(100L, 200L) } returns flowOf(listOf(entity))

        val result = repository.getCategoryTotalsInRange(100L, 200L).first()

        assertEquals(1, result.size)
        assertEquals(Category.FOOD, result[0].category)
        assertEquals(1000.0, result[0].totalAmount, 0.0)
    }
}
