package pe.mitaxi.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import pe.mitaxi.app.R
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.utils.formatSoles

@Composable
fun DailySummaryCard(repository: DailySummaryRepository) {
    val model: DailySummaryViewModel = viewModel(factory = DailySummaryViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(model) {
        model.refresh()
        onPauseOrDispose { model.stop() }
    }
    Card(Modifier.fillMaxWidth().testTag("dailySummary")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.today_summary), style = MaterialTheme.typography.titleMedium)
            when {
                state.loading -> Text(stringResource(R.string.summary_loading))
                state.loadError -> {
                    Text(stringResource(R.string.summary_error))
                    TextButton(onClick = model::refresh) { Text(stringResource(R.string.retry)) }
                }
                else -> state.summary?.let { summary ->
                    Text(summary.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-PE"))),
                        style = MaterialTheme.typography.bodySmall)
                    ProfitCard(summary.profit, stringResource(R.string.today_profit), "todayProfit")
                    SummaryRow(stringResource(R.string.today_services), summary.serviceCount.toString(), "todayServiceCount")
                    SummaryRow(stringResource(R.string.today_income), formatSoles(summary.income), "todayIncome")
                    SummaryRow(stringResource(R.string.today_expenses), formatSoles(summary.expenses), "todayExpenses")
                    SummaryRow(stringResource(R.string.today_fuel), formatSoles(summary.fuel), "todayFuel")
                }
            }
            TextButton(onClick = model::openFuel, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("addFuel")) {
                Text(stringResource(R.string.add_fuel))
            }
            if (state.lastFuelId != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(if (state.undoError) R.string.fuel_undo_error else R.string.fuel_saved),
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = model::undoFuel, enabled = !state.busy, modifier = Modifier.testTag("undoFuel")) {
                        Text(stringResource(R.string.undo))
                    }
                }
            }
        }
    }
    if (state.fuelOpen) {
        val focus = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = model::closeFuel,
            title = { Text(stringResource(R.string.fuel_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.amount, onValueChange = model::changeAmount,
                        label = { Text(stringResource(R.string.amount_label)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("fuelAmount"),
                        singleLine = true, enabled = !state.busy, isError = state.invalidAmount,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { model.saveFuel() }),
                        supportingText = if (state.invalidAmount) {{ Text(stringResource(R.string.invalid_amount)) }} else null,
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    if (state.saveError) Text(stringResource(R.string.fuel_save_error), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = model::saveFuel, enabled = !state.busy, modifier = Modifier.testTag("saveFuel")) {
                    Text(stringResource(if (state.busy) R.string.saving else R.string.register))
                }
            },
            dismissButton = {
                TextButton(onClick = model::closeFuel, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, tag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.testTag(tag), style = MaterialTheme.typography.titleMedium)
    }
}
