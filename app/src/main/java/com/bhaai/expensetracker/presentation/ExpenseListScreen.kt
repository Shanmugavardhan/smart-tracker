package com.bhaai.expensetracker.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bhaai.expensetracker.domain.Category
import com.bhaai.expensetracker.domain.Expense
import com.bhaai.expensetracker.domain.ExpenseStatus
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    navController: NavController,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val text by viewModel.naturalLanguageText.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ExpenseListViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
                is ExpenseListViewModel.UiEvent.NavigateToAddEdit -> {
                    navController.navigate(
                        Screen.AddEditExpenseScreen.passValues(
                            amount = event.amount,
                            description = event.description,
                            category = event.category,
                            confidence = event.confidence,
                            subcategory = event.subcategory,
                            splitCount = event.splitCount,
                            originalAmount = event.originalAmount
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.AddEditExpenseScreen.passId(null))
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        val financialPosition by viewModel.financialPosition.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Financial Position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Net Position: ₹" + String.format(java.util.Locale.US, "%.2f", financialPosition.netPosition),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (financialPosition.netPosition >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Current Cash Outflow", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "₹" + String.format(java.util.Locale.US, "%.2f", financialPosition.cashOutflow), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Money Owed To Me", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "₹" + String.format(java.util.Locale.US, "%.2f", financialPosition.moneyOwedToMe), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• Pending Split Recoveries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₹" + String.format(java.util.Locale.US, "%.2f", financialPosition.pendingSplitRecoveries), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• Loans Outstanding", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "₹" + String.format(java.util.Locale.US, "%.2f", financialPosition.loansOutstanding), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Money I Owe Others", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "₹" + String.format(java.util.Locale.US, "%.2f", financialPosition.moneyIOweOthers), style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.ReconciliationScreen.route) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "Why Is My Balance Lower?")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✨ AI Quick Logger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { viewModel.onNaturalLanguageTextChange(it) },
                        placeholder = { Text("Had biryani at Paradise for 250...") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isParsing,
                        maxLines = 2,
                        trailingIcon = {
                            if (isParsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.onQuickAdd(context) },
                                    enabled = text.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Quick Add",
                                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Recent Expenses", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(8.dp))

            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses yet. Tap + or try AI Quick Logger.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(expenses) { expense ->
                        ExpenseItem(
                            expense = expense,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(Screen.AddEditExpenseScreen.passId(expense.id))
                                },
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(
    expense: Expense,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isPending = expense.status == ExpenseStatus.PENDING_CATEGORIZATION
            val icon = if (isPending) Icons.Default.Schedule else getCategoryIcon(expense.category)
            val iconColor = if (isPending) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            val bgColor = if (isPending) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer

            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = bgColor.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = expense.category?.name ?: "Pending",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = expense.description, style = MaterialTheme.typography.bodyLarge)
                    if (isPending) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "Pending AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (isPending) "Processing..." else (expense.category?.name ?: "Uncategorized"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹" + String.format(java.util.Locale.US, "%.2f", expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Expense",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun getCategoryIcon(category: Category?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        Category.FOOD -> Icons.Default.Restaurant
        Category.TRAVEL -> Icons.Default.DirectionsCar
        Category.HOUSING -> Icons.Default.Home
        Category.UTILITIES -> Icons.Default.Lightbulb
        Category.SHOPPING -> Icons.Default.ShoppingCart
        Category.HEALTHCARE -> Icons.Default.LocalHospital
        Category.EDUCATION -> Icons.Default.School
        Category.ENTERTAINMENT -> Icons.Default.Movie
        Category.INVESTMENT -> Icons.Default.TrendingUp
        else -> Icons.Default.Star
    }
}
