package com.bhaai.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class ExpenseDaoTest {

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
    fun insertAndGetExpense() = runTest {
        val expense = ExpenseEntity(
            amount = 100.0,
            description = "Test Food",
            category = Category.FOOD,
            subcategory = null,
            status = ExpenseStatus.CATEGORIZED,
            timestamp = 1000L
        )

        val id = dao.insertExpense(expense)
        val loaded = dao.getExpenseById(id)

        assertEquals(100.0, loaded?.amount)
        assertEquals("Test Food", loaded?.description)
        assertEquals(Category.FOOD, loaded?.category)
    }

    @Test
    fun deleteExpense() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amount = 50.0,
            description = "To Delete",
            category = Category.OTHER,
            subcategory = null,
            status = ExpenseStatus.CATEGORIZED,
            timestamp = 2000L
        )

        dao.insertExpense(expense)
        var list = dao.getAllExpenses().first()
        assertEquals(1, list.size)

        dao.deleteExpense(expense)
        list = dao.getAllExpenses().first()
        assertEquals(0, list.size)
    }
}
