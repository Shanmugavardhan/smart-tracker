package com.bhaai.expensetracker.presentation

sealed class Screen(val route: String) {
    object ExpenseListScreen: Screen("expense_list_screen")
    object ReconciliationScreen: Screen("reconciliation_screen")
    object AddEditExpenseScreen: Screen("add_edit_expense_screen?expenseId={expenseId}&amount={amount}&description={description}&category={category}&confidence={confidence}&subcategory={subcategory}&splitCount={splitCount}&originalAmount={originalAmount}") {
        fun passId(expenseId: Long?): String {
            return "add_edit_expense_screen?expenseId=${expenseId ?: -1}"
        }

        fun passValues(
            expenseId: Long? = null,
            amount: String? = null,
            description: String? = null,
            category: String? = null,
            confidence: Double? = null,
            subcategory: String? = null,
            splitCount: Int? = null,
            originalAmount: Double? = null
        ): String {
            val builder = StringBuilder("add_edit_expense_screen?expenseId=${expenseId ?: -1}")
            if (amount != null) builder.append("&amount=$amount")
            if (description != null) builder.append("&description=$description")
            if (category != null) builder.append("&category=$category")
            if (confidence != null) builder.append("&confidence=$confidence")
            if (subcategory != null) builder.append("&subcategory=$subcategory")
            if (splitCount != null) builder.append("&splitCount=$splitCount")
            if (originalAmount != null) builder.append("&originalAmount=$originalAmount")
            return builder.toString()
        }
    }
}
