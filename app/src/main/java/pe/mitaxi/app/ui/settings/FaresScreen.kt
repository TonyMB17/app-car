package pe.mitaxi.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import pe.mitaxi.app.R
import pe.mitaxi.app.data.repository.FareRepository
import pe.mitaxi.app.utils.formatSoles

@Composable
fun FaresScreen(repository: FareRepository) {
    val model: FaresViewModel = viewModel(factory = FaresViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.fares_title), style = MaterialTheme.typography.headlineMedium) }
        item { Text(stringResource(R.string.fares_hint)) }
        item { NotificationSettings() }
        item {
            OutlinedButton(onClick = {
                val manager = android.appwidget.AppWidgetManager.getInstance(context)
                if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(
                    android.content.ComponentName(context, pe.mitaxi.app.widget.TaxiWidget::class.java), null, null)
                else android.widget.Toast.makeText(context, R.string.widget_add_help, android.widget.Toast.LENGTH_LONG).show()
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("pinWidget")) {
                Text(stringResource(R.string.widget_add))
            }
        }
        when {
            state.loading -> item { Text(stringResource(R.string.fares_loading)) }
            state.loadError -> item {
                Text(stringResource(R.string.fares_load_error))
                TextButton(onClick = model::reload) { Text(stringResource(R.string.retry)) }
            }
            else -> {
                item { Button(onClick = { model.edit() }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("addFare")) { Text(stringResource(R.string.add_fare)) } }
                if (state.fares.isEmpty()) item { Text(stringResource(R.string.fares_empty)) }
                itemsIndexed(state.fares, key = { _, fare -> fare.id }) { index, fare ->
                    Card(Modifier.fillMaxWidth().testTag("fare${fare.id}")) {
                        Column(Modifier.padding(12.dp)) {
                            Text(formatSoles(BigDecimal.valueOf(fare.amountCents, 2)), style = MaterialTheme.typography.headlineSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { model.move(fare.id, -1) }, enabled = !state.busy && index > 0,
                                    modifier = Modifier.testTag("upFare${fare.id}")) { Text(stringResource(R.string.fare_up)) }
                                TextButton(onClick = { model.move(fare.id, 1) }, enabled = !state.busy && index < state.fares.lastIndex,
                                    modifier = Modifier.testTag("downFare${fare.id}")) { Text(stringResource(R.string.fare_down)) }
                            }
                            Row {
                                TextButton(onClick = { model.edit(fare) }, enabled = !state.busy,
                                    modifier = Modifier.testTag("editFare${fare.id}")) { Text(stringResource(R.string.edit)) }
                                TextButton(onClick = { model.requestDelete(fare) }, enabled = !state.busy,
                                    modifier = Modifier.testTag("deleteFare${fare.id}")) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
        }
        if (state.error) item { Text(stringResource(R.string.fares_save_error), color = MaterialTheme.colorScheme.error) }
    }
    if (state.open) {
        val focus = remember { FocusRequester() }
        AlertDialog(onDismissRequest = model::close,
            title = { Text(stringResource(if (state.id == null) R.string.add_fare else R.string.edit_fare)) },
            text = { Column {
                OutlinedTextField(value = state.amount, onValueChange = model::amount, singleLine = true, enabled = !state.busy,
                    label = { Text(stringResource(R.string.amount_label)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("fareAmount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { model.save() }), isError = state.invalid || state.duplicate)
                LaunchedEffect(Unit) { focus.requestFocus() }
                if (state.invalid) Text(stringResource(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
                if (state.duplicate) Text(stringResource(R.string.fare_duplicate), color = MaterialTheme.colorScheme.error)
                if (state.error) Text(stringResource(R.string.fares_save_error), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = model::save, enabled = !state.busy, modifier = Modifier.testTag("saveFare")) { Text(stringResource(R.string.save_changes)) } },
            dismissButton = { TextButton(onClick = model::close, enabled = !state.busy) { Text(stringResource(R.string.cancel)) } })
    }
    state.pendingDelete?.let { fare ->
        AlertDialog(onDismissRequest = model::cancelDelete,
            title = { Text(stringResource(R.string.delete_fare)) },
            text = { Column {
                Text(stringResource(R.string.delete_fare_message, formatSoles(BigDecimal.valueOf(fare.amountCents, 2))))
                if (state.error) Text(stringResource(R.string.fares_save_error), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = model::delete, enabled = !state.busy, modifier = Modifier.testTag("confirmFareDelete")) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = model::cancelDelete, enabled = !state.busy) { Text(stringResource(R.string.cancel)) } })
    }
}
