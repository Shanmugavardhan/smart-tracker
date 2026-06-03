package com.bhaai.expensetracker.presentation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, application = dagger.hilt.android.testing.HiltTestApplication::class)
class ExpenseAppUITest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addExpense_displaysInList() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Add Expense").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Amount").performTextInput("500")
        composeTestRule.onNodeWithText("Description").performTextInput("Groceries UI Test")

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Groceries UI Test").assertExists()
    }
}
