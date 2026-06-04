package com.bhaai.expensetracker.data.repository

import com.bhaai.expensetracker.data.api.GeminiApi
import com.bhaai.expensetracker.data.api.GeminiResponse
import com.bhaai.expensetracker.data.api.Candidate
import com.bhaai.expensetracker.data.api.Content
import com.bhaai.expensetracker.data.api.Part
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.TransactionType
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GeminiExpenseCategorizationServiceTest {

    private lateinit var api: GeminiApi
    private val moshi = Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private lateinit var service: GeminiExpenseCategorizationService

    @Before
    fun setUp() {
        api = mockk()
        service = GeminiExpenseCategorizationService(api, moshi, "test-api-key")
    }

    @Test
    fun testSuccessfulCategorization() = runTest {
        val jsonResponse = """
            {
              "transactionType": "EXPENSE",
              "category": "FOOD",
              "description": "biryani at Paradise",
              "totalAmount": 250.0,
              "effectiveAmount": 250.0,
              "confidence": 0.95
            }
        """.trimIndent()

        val mockResponse = GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(Part(text = jsonResponse))
                    )
                )
            )
        )

        coEvery { api.generateContent(any(), any()) } returns mockResponse

        val result = service.categorizeExpense("Had biryani at Paradise for 250")
        assertEquals(TransactionType.EXPENSE, result.transactionType)
        assertEquals(250.0, result.effectiveAmount ?: 0.0, 0.0)
        assertEquals("biryani at Paradise", result.description)
        assertEquals(Category.FOOD, result.category)
        assertEquals(0.95, result.confidence, 0.0)
        assertEquals(null, result.splitCount)
        assertEquals(null, result.originalAmount)
    }
}
