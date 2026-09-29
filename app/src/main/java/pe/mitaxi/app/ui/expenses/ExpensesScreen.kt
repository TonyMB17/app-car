package pe.mitaxi.app.ui.expenses

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.time.ZoneId
import pe.mitaxi.app.R
import pe.mitaxi.app.data.repository.ExpenseRepository
import pe.mitaxi.app.domain.model.ExpenseCategory
import pe.mitaxi.app.domain.model.ServiceInput
import pe.mitaxi.app.utils.formatSoles

@StringRes
private fun ExpenseCategory.label(): Int = when (this) {
    ExpenseCategory.FUEL -> R.string.category_fuel
    ExpenseCategory.MAINTENANCE -> R.string.category_maintenance
    ExpenseCategory.REPAIR -> R.string.category_repair
    ExpenseCategory.WASH -> R.string.category_wash
    ExpenseCategory.OTHER -> R.string.category_other
}

@Composable
fun ExpensesRoute(repository: ExpenseRepository) {
    val model: ExpensesViewModel = viewModel(factory = ExpensesViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.noticeVersion) {
        state.notice?.let { notice ->
            val version = state.noticeVersion
            snackbar.showSnackbar(resources.getString(when (notice) {
                ExpenseNotice.Saved -> R.string.expense_saved
                ExpenseNotice.Deleted -> R.string.expense_deleted
                ExpenseNotice.DeleteFailed -> R.string.expense_delete_error
            }))
            model.clearNotice(version)
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.expenses), style = MaterialTheme.typography.headlineMedium) }
            item { Text(stringResource(R.string.expense_choose_category), style = MaterialTheme.typography.bodyLarge) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(ExpenseCategory.FUEL, ExpenseCategory.WASH).forEach { category ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(category.label()), style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val amounts = if (category == ExpenseCategory.FUEL) listOf(5000L, 10000L) else listOf(1000L)
                                    amounts.forEach { cents ->
                                        Button(onClick = { model.quickAdd(category, cents) }, enabled = !state.busy,
                                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("quickExpense${category.name}$cents")) {
                                            Text(stringResource(R.string.quick_expense_amount, cents / 100))
                                        }
                                    }
                                    Button(onClick = { model.add(category) }, enabled = !state.busy,
                                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("addExpense${category.name}")) {
                                        Text(stringResource(R.string.quick_expense_other))
                                    }
                                }
                            }
                        }
                    }
                    ExpenseCategory.entries.filter { it != ExpenseCategory.FUEL && it != ExpenseCategory.WASH }.chunked(2).forEach { categories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            categories.forEach { category ->
                                Button(onClick = { model.add(category) }, enabled = !state.busy,
                                    modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("addExpense${category.name}")) {
                                    Text(stringResource(category.label()))
                                }
                            }
                        }
                    }
                }
            }
            when {
                state.loading -> item { Text(stringResource(R.string.expenses_loading)) }
                state.loadError -> item {
                    Text(stringResource(R.string.expenses_load_error))
                    TextButton(onClick = model::reload) { Text(stringResource(R.string.retry)) }
                }
                else -> {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.expenses_total), style = MaterialTheme.typography.labelLarge)
                                Text(formatSoles(state.total), Modifier.testTag("expensesTotal"), style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                    if (state.expenses.isEmpty()) item { Text(stringResource(R.string.expenses_empty)) }
                    items(state.expenses, key = { it.id }) { expense ->
                        Card(Modifier.fillMaxWidth().testTag("expense${expense.id}")) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(stringResource(expense.category.label()), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    Text(formatSoles(BigDecimal.valueOf(expense.amountCents, 2)), style = MaterialTheme.typography.titleMedium)
                                }
                                val zone = ZoneId.systemDefault()
                                Text(stringResource(R.string.service_date_time, ServiceInput.date(expense.occurredAt, zone), ServiceInput.time(expense.occurredAt, zone)))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { model.edit(expense) }, enabled = !state.busy, modifier = Modifier.testTag("editExpense${expense.id}")) {
                                        Text(stringResource(R.string.edit))
                                    }
                                    TextButton(onClick = { model.requestDelete(expense) }, enabled = !state.busy, modifier = Modifier.testTag("deleteExpense${expense.id}")) {
                                        Text(stringResource(R.string.delete))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    state.editor?.let { editor -> if (!editor.quick || editor.saveError) ExpenseDialog(editor, state.busy, model) }
    state.pendingDelete?.let { expense ->
        AlertDialog(onDismissRequest = model::cancelDelete,
            title = { Text(stringResource(R.string.delete_expense_title)) },
            text = { Text(stringResource(R.string.delete_expense_message, stringResource(expense.category.label()), formatSoles(BigDecimal.valueOf(expense.amountCents, 2)))) },
            confirmButton = { TextButton(onClick = model::delete, enabled = !state.busy, modifier = Modifier.testTag("confirmExpenseDelete")) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = model::cancelDelete, enabled = !state.busy) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ExpenseDialog(editor: ExpenseEditor, busy: Boolean, model: ExpensesViewModel) {
    val focus = remember { FocusRequester() }
    var details by rememberSaveable(editor.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = model::close,
        title = { Text(stringResource(if (editor.id == null) R.string.new_expense_title else R.string.edit_expense_title, stringResource(editor.category.label()))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = editor.amount, onValueChange = model::amount,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("expenseAmount"),
                    label = { Text(stringResource(R.string.amount_label)) }, singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { model.save() }), isError = editor.invalidAmount,
                    supportingText = if (editor.invalidAmount) {{ Text(stringResource(R.string.invalid_amount)) }} else null,
                )
                LaunchedEffect(editor.id) { if (editor.id == null) focus.requestFocus() }
                if (editor.id != null) {
                    TextButton(onClick = { details = !details }) { Text(stringResource(if (details) R.string.hide_details else R.string.more_details)) }
                    if (details) {
                        Text(stringResource(R.string.expense_category_label))
                        ExpenseCategory.entries.forEach { category ->
                            FilterChip(selected = category == editor.category, enabled = !busy,
                                onClick = { model.category(category) }, label = { Text(stringResource(category.label())) },
                                modifier = Modifier.fillMaxWidth().testTag("expenseCategory${category.name}"))
                        }
                        OutlinedTextField(value = editor.date, onValueChange = model::date,
                            label = { Text(stringResource(R.string.date_label)) }, modifier = Modifier.fillMaxWidth().testTag("expenseDate"),
                            singleLine = true, enabled = !busy, isError = editor.invalidDate)
                        OutlinedTextField(value = editor.time, onValueChange = model::time,
                            label = { Text(stringResource(R.string.time_label)) }, modifier = Modifier.fillMaxWidth().testTag("expenseTime"),
                            singleLine = true, enabled = !busy, isError = editor.invalidDate)
                        if (editor.invalidDate) Text(stringResource(R.string.invalid_date_time), color = MaterialTheme.colorScheme.error)
                    }
                }
                if (editor.saveError) Text(stringResource(R.string.expense_save_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = model::save, enabled = !busy, modifier = Modifier.testTag("saveExpense")) {
            Text(stringResource(if (busy) R.string.saving else if (editor.id == null) R.string.register else R.string.save_changes))
        } },
        dismissButton = { TextButton(onClick = model::close, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
    )
}
