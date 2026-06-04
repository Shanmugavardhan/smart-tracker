package com.bhaai.expensetracker.data.repository

import com.bhaai.expensetracker.data.NetworkMonitor
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ParsedExpense
import com.bhaai.expensetracker.domain.TransactionType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseCategorizationServiceImplTest {

    private lateinit var geminiService: GeminiExpenseCategorizationService
    private lateinit var localFallbackService: LocalFallbackExpenseCategorizationService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var service: ExpenseCategorizationServiceImpl

    @Before
    fun setUp() {
        geminiService = mockk()
        localFallbackService = mockk()
        networkMonitor = mockk()
        service = ExpenseCategorizationServiceImpl(geminiService, localFallbackService, networkMonitor)
    }

    @Test
    fun `when network is available gemini service is called`() = runTest {
        every { networkMonitor.isNetworkAvailable() } returns true
        val expected = ParsedExpense(
            transactionType = TransactionType.EXPENSE,
            description = "Biryani",
            category = Category.FOOD,
            totalAmount = 250.0,
            effectiveAmount = 250.0,
            originalAmount = 250.0,
            splitCount = null,
            person = null,
            people = emptyList(),
            confidence = 0.95
        )
        coEvery { geminiService.categorizeExpense(any()) } returns expected

        val result = service.categorizeExpense("Had biryani for 250")
        assertEquals(expected, result)
    }

    @Test
    fun `when network is available but gemini fails fallback is called`() = runTest {
        every { networkMonitor.isNetworkAvailable() } returns true
        coEvery { geminiService.categorizeExpense(any()) } throws Exception("API rate limit")
        val expected = ParsedExpense(
            transactionType = TransactionType.EXPENSE,
            description = "Biryani",
            category = Category.FOOD,
            totalAmount = 250.0,
            effectiveAmount = 250.0,
            originalAmount = 250.0,
            splitCount = null,
            person = null,
            people = emptyList(),
            confidence = 0.5
        )
        coEvery { localFallbackService.categorizeExpense(any()) } returns expected

        val result = service.categorizeExpense("Had biryani for 250")
        assertEquals(expected, result)
    }

    @Test
    fun `when network is unavailable fallback is called immediately`() = runTest {
        every { networkMonitor.isNetworkAvailable() } returns false
        val expected = ParsedExpense(
            transactionType = TransactionType.EXPENSE,
            description = "Biryani",
            category = Category.FOOD,
            totalAmount = 250.0,
            effectiveAmount = 250.0,
            originalAmount = 250.0,
            splitCount = null,
            person = null,
            people = emptyList(),
            confidence = 0.5
        )
        coEvery { localFallbackService.categorizeExpense(any()) } returns expected

        val result = service.categorizeExpense("Had biryani for 250")
        assertEquals(expected, result)
    }
}
