package com.bhaai.expensetracker.data

import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromCategory returns string name or null`() {
        assertEquals("FOOD", converters.fromCategory(Category.FOOD))
        assertNull(converters.fromCategory(null))
    }

    @Test
    fun `toCategory returns enum or null`() {
        assertEquals(Category.TRAVEL, converters.toCategory("TRAVEL"))
        assertNull(converters.toCategory(null))
    }

    @Test
    fun `fromExpenseStatus returns string name`() {
        assertEquals("CATEGORIZED", converters.fromExpenseStatus(ExpenseStatus.CATEGORIZED))
    }

    @Test
    fun `toExpenseStatus returns enum`() {
        assertEquals(ExpenseStatus.PENDING_CATEGORIZATION, converters.toExpenseStatus("PENDING_CATEGORIZATION"))
    }
}
