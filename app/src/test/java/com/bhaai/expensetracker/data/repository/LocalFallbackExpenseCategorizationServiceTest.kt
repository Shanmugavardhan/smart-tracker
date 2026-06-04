package com.bhaai.expensetracker.data.repository

import com.bhaai.expensetracker.domain.Category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalFallbackExpenseCategorizationServiceTest {

    private val service = LocalFallbackExpenseCategorizationService()

    @Test
    fun testAmountExtraction() = runTest {
        val result1 = service.categorizeExpense("Spent 250 on food")
        assertEquals(250.0, result1.effectiveAmount ?: 0.0, 0.0)
        assertEquals("On food", result1.description)
        assertEquals(Category.FOOD, result1.category)

        val result2 = service.categorizeExpense("Had biryani for Rs. 500")
        assertEquals(500.0, result2.effectiveAmount ?: 0.0, 0.0)
        assertEquals("Had biryani", result2.description)
        assertEquals(Category.FOOD, result2.category)

        val result3 = service.categorizeExpense("uber ride Rs. 350.50")
        assertEquals(350.50, result3.effectiveAmount ?: 0.0, 0.0)
        assertEquals("Uber ride", result3.description)
        assertEquals(Category.TRAVEL, result3.category)
    }

    @Test
    fun testCategoryDetection() = runTest {
        val foodResult = service.categorizeExpense("had coffee 80")
        assertEquals(Category.FOOD, foodResult.category)

        val travelResult = service.categorizeExpense("petrol recharge 1000")
        assertEquals(Category.TRAVEL, travelResult.category)

        val housingResult = service.categorizeExpense("room rent 12000")
        assertEquals(Category.HOUSING, housingResult.category)

        val utilitiesResult = service.categorizeExpense("wifi recharge 799")
        assertEquals(Category.UTILITIES, utilitiesResult.category)

        val shoppingResult = service.categorizeExpense("bought clothes from amazon 1500")
        assertEquals(Category.SHOPPING, shoppingResult.category)

        val schoolResult = service.categorizeExpense("college fees 50000")
        assertEquals(Category.EDUCATION, schoolResult.category)
    }

    @Test
    fun testNewExtractionRequirements() = runTest {
        val biriyaniResult = service.categorizeExpense("Had a biriyani last night of 400 rupees")
        assertEquals(400.0, biriyaniResult.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.FOOD, biriyaniResult.category)

        val splitResult = service.categorizeExpense("Bought a fridge of 15k, split it for 4 people")
        assertEquals(3750.0, splitResult.effectiveAmount ?: 0.0, 0.0)
        assertEquals(15000.0, splitResult.originalAmount)
        assertEquals(4, splitResult.splitCount)
        assertEquals(Category.SHOPPING, splitResult.category)
    }
}
