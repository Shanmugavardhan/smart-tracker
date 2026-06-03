package com.bhaai.expensetracker.presentation

sealed class Screen(val route: String) {
    object ExpenseListScreen: Screen("expense_list_screen")
    object AddEditExpenseScreen: Screen("add_edit_expense_screen?expenseId={expenseId}") {
        fun passId(expenseId: Long?): String {
            return "add_edit_expense_screen?expenseId=\${expenseId ?: -1}"
        }
    }
}
