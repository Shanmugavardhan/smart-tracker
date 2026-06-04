package com.bhaai.expensetracker.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bhaai.expensetracker.data.NetworkMonitor
import com.bhaai.expensetracker.data.api.GeminiApi
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AIParserIntegrationTest {

    private lateinit var context: Context
    private lateinit var moshi: Moshi
    private lateinit var geminiApi: GeminiApi
    private lateinit var geminiService: GeminiExpenseCategorizationService
    private lateinit var localFallbackService: LocalFallbackExpenseCategorizationService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var service: ExpenseCategorizationService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        geminiApi = retrofit.create(GeminiApi::class.java)
        
        geminiService = GeminiExpenseCategorizationService(
            api = geminiApi,
            moshi = moshi,
            apiKey = "" // Blank API key to ensure it falls back cleanly in offline test runs
        )
        localFallbackService = LocalFallbackExpenseCategorizationService()
        networkMonitor = NetworkMonitor(context)
        
        service = ExpenseCategorizationServiceImpl(
            geminiService = geminiService,
            localFallbackService = localFallbackService,
            networkMonitor = networkMonitor
        )
    }

    @Test
    fun testFoodCategory() = runTest {
        // "Had biriyani for 500"
        val res1 = service.categorizeExpense("Had biriyani for 500")
        assertEquals(500.0, res1.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.FOOD, res1.category)

        // "Ate dosa at A2B 120"
        val res2 = service.categorizeExpense("Ate dosa at A2B 120")
        assertEquals(120.0, res2.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.FOOD, res2.category)

        // "Tea and snacks 60"
        val res3 = service.categorizeExpense("Tea and snacks 60")
        assertEquals(60.0, res3.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.FOOD, res3.category)
    }

    @Test
    fun testTransportCategory() = runTest {
        // "Auto fare 180"
        val res1 = service.categorizeExpense("Auto fare 180")
        assertEquals(180.0, res1.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.TRAVEL, res1.category)

        // "Uber to office 250"
        val res2 = service.categorizeExpense("Uber to office 250")
        assertEquals(250.0, res2.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.TRAVEL, res2.category)
    }

    @Test
    fun testHousingCategory() = runTest {
        // "Room rent 12000"
        val res1 = service.categorizeExpense("Room rent 12000")
        assertEquals(12000.0, res1.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.HOUSING, res1.category)

        // "PG rent paid 6000"
        val res2 = service.categorizeExpense("PG rent paid 6000")
        assertEquals(6000.0, res2.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.HOUSING, res2.category)
    }

    @Test
    fun testShoppingCategory() = runTest {
        // "Bought fridge worth 15k"
        val res1 = service.categorizeExpense("Bought fridge worth 15k")
        assertEquals(15000.0, res1.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.SHOPPING, res1.category)

        // "Purchased induction stove 2200"
        val res2 = service.categorizeExpense("Purchased induction stove 2200")
        assertEquals(2200.0, res2.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.SHOPPING, res2.category)
    }

    @Test
    fun testSharedExpenses() = runTest {
        // "Bought fridge worth 15k split among 4"
        val res1 = service.categorizeExpense("Bought fridge worth 15k split among 4")
        assertEquals(3750.0, res1.effectiveAmount ?: 0.0, 0.0)
        assertEquals(15000.0, res1.originalAmount)
        assertEquals(4, res1.splitCount)
        assertEquals(Category.SHOPPING, res1.category)

        // "Groceries 2400 shared by 4"
        val res2 = service.categorizeExpense("Groceries 2400 shared by 4")
        assertEquals(600.0, res2.effectiveAmount ?: 0.0, 0.0)
        assertEquals(2400.0, res2.originalAmount)
        assertEquals(4, res2.splitCount)
        assertEquals(Category.FOOD, res2.category)

        // "Room rent 12000 split among 3"
        val res3 = service.categorizeExpense("Room rent 12000 split among 3")
        assertEquals(4000.0, res3.effectiveAmount ?: 0.0, 0.0)
        assertEquals(12000.0, res3.originalAmount)
        assertEquals(3, res3.splitCount)
        assertEquals(Category.HOUSING, res3.category)
    }

    @Test
    fun testIndianNumberFormats() = runTest {
        // "Bought fridge worth 15k"
        val res1 = service.categorizeExpense("Bought fridge worth 15k")
        assertEquals(15000.0, res1.effectiveAmount ?: 0.0, 0.0)

        // "Paid 2.5k for groceries"
        val res2 = service.categorizeExpense("Paid 2.5k for groceries")
        assertEquals(2500.0, res2.effectiveAmount ?: 0.0, 0.0)

        // "Bike repair 1 lakh"
        val res3 = service.categorizeExpense("Bike repair 1 lakh")
        assertEquals(100000.0, res3.effectiveAmount ?: 0.0, 0.0)
    }

    @Test
    fun testAmbiguousInput() = runTest {
        // "Spent 300"
        val res = service.categorizeExpense("Spent 300")
        assertEquals(300.0, res.effectiveAmount ?: 0.0, 0.0)
        assertEquals(Category.OTHER, res.category)
        assertTrue(res.confidence < 0.7)
    }

    @Test
    fun testFailureCases() = runTest {
        // "Bought fridge" -> amount must be null (no hallucinated amount allowed)
        val res = service.categorizeExpense("Bought fridge")
        assertNull(res.effectiveAmount)
        assertEquals(Category.SHOPPING, res.category)

        // "Hello world" -> parser rejection
        try {
            service.categorizeExpense("Hello world")
            fail("Expected IllegalArgumentException for 'Hello world'")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
