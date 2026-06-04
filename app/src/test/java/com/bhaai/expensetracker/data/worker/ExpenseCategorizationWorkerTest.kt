package com.bhaai.expensetracker.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseCategorizationService
import com.bhaai.expensetracker.domain.ExpenseRepository
import com.bhaai.expensetracker.domain.ExpenseStatus
import com.bhaai.expensetracker.domain.ParsedExpense
import com.bhaai.expensetracker.domain.TransactionType
import dagger.hilt.android.EntryPointAccessors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseCategorizationWorkerTest {

    private lateinit var context: Context
    private lateinit var params: WorkerParameters
    private lateinit var repository: ExpenseRepository
    private lateinit var service: ExpenseCategorizationService
    private lateinit var entryPoint: ExpenseCategorizationWorker.WorkerEntryPoint

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        params = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        service = mockk(relaxed = true)
        entryPoint = mockk()

        every { entryPoint.expenseRepository() } returns repository
        every { entryPoint.expenseCategorizationService() } returns service

        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(
                any(),
                ExpenseCategorizationWorker.WorkerEntryPoint::class.java
            )
        } returns entryPoint
    }

    @After
    fun tearDown() {
        unmockkStatic(EntryPointAccessors::class)
    }

    @Test
    fun `worker processes pending expenses and updates them`() = runTest {
        val pendingExpense = Expense(
            id = 1L,
            amount = 0.0,
            description = "Had biryani at Paradise for 250",
            category = null,
            status = ExpenseStatus.PENDING_CATEGORIZATION,
            confidence = 0.0
        )

        every { repository.getAllExpenses() } returns flowOf(listOf(pendingExpense))

        val parsed = ParsedExpense(
            transactionType = TransactionType.EXPENSE,
            description = "biryani at Paradise",
            category = Category.FOOD,
            totalAmount = 250.0,
            effectiveAmount = 250.0,
            originalAmount = null,
            splitCount = null,
            person = null,
            people = emptyList(),
            confidence = 0.95
        )
        coEvery { service.categorizeExpense("Had biryani at Paradise for 250") } returns parsed

        val worker = ExpenseCategorizationWorker(context, params)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        val expectedUpdatedExpense = pendingExpense.copy(
            amount = 250.0,
            description = "biryani at Paradise",
            category = Category.FOOD,
            subcategory = null,
            confidence = 0.95,
            status = ExpenseStatus.CATEGORIZED,
            splitCount = null,
            originalAmount = null
        )

        coVerify { repository.updateExpense(expectedUpdatedExpense) }
    }
}
