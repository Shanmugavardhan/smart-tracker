package com.bhaai.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryTotalTest {

    private lateinit var database: ExpenseDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.expenseDao
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getCategoryTotalsInRange() = runBlocking {
        val food1 = ExpenseEntity(amount = 100.0, description = "F1", category = Category.FOOD, subcategory = null, status = ExpenseStatus.CATEGORIZED, timestamp = 1000L)
        val food2 = ExpenseEntity(amount = 200.0, description = "F2", category = Category.FOOD, subcategory = null, status = ExpenseStatus.CATEGORIZED, timestamp = 2000L)
        val travel1 = ExpenseEntity(amount = 50.0, description = "T1", category = Category.TRAVEL, subcategory = null, status = ExpenseStatus.CATEGORIZED, timestamp = 1500L)

        dao.insertExpense(food1)
        dao.insertExpense(food2)
        dao.insertExpense(travel1)

        val totals = dao.getCategoryTotalsInRange(0L, 5000L).first()

        assertEquals(2, totals.size)

        val foodTotal = totals.find { it.category == Category.FOOD }?.totalAmount
        val travelTotal = totals.find { it.category == Category.TRAVEL }?.totalAmount

        assertEquals(300.0, foodTotal)
        assertEquals(50.0, travelTotal)
    }
}
