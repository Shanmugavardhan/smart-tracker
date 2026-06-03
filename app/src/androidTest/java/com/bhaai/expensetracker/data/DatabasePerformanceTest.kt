package com.bhaai.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.ExpenseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {

    private lateinit var database: ExpenseDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            "test_perf_db"
        ).allowMainThreadQueries().build()
        dao = database.expenseDao
    }

    @After
    fun teardown() {
        database.close()
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase("test_perf_db")
    }

    @Test
    fun insertAndReadPerformance_Under100ms() = runBlocking {
        val expense = ExpenseEntity(
            amount = 100.0,
            description = "Perf Test",
            category = Category.OTHER,
            subcategory = null,
            status = ExpenseStatus.CATEGORIZED,
            timestamp = System.currentTimeMillis()
        )

        // Warm up DB
        dao.insertExpense(expense.copy(id = 0L))

        // Measure Insert
        val insertTime = measureTimeMillis {
            dao.insertExpense(expense.copy(id = 0L))
        }

        // Measure Read
        val readTime = measureTimeMillis {
            dao.getAllExpenses().first()
        }

        println("Database Insert Time: \${insertTime}ms")
        println("Database Read Time: \${readTime}ms")

        assertTrue("Insert took longer than 100ms (\${insertTime}ms)", insertTime < 100)
        assertTrue("Read took longer than 100ms (\${readTime}ms)", readTime < 100)
    }
}
