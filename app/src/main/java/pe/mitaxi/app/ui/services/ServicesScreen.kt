package pe.mitaxi.app.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.time.ZoneId
import pe.mitaxi.app.utils.formatSoles
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.ui.home.DailySummaryCard
import pe.mitaxi.app.R
import pe.mitaxi.app.data.repository.ServiceRepository
import pe.mitaxi.app.domain.model.ServiceInput
import pe.mitaxi.app.domain.model.TaxiService

@Composable
fun ServicesRoute(repository: ServiceRepository, fareRepository: pe.mitaxi.app.data.repository.FareRepository, quickEntry: Boolean = false, dailyRepository: DailySummaryRepository? = null) {
    val model: ServicesViewModel = viewModel(factory = ServicesViewModel.factory(repository, fareRepository))
    val state by model.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.noticeVersion) {
        state.notice?.let { notice ->
            val registered = state.lastRegistered
            val version = state.noticeVersion
            val message = when (notice) {
                ServiceNotice.Added -> R.string.service_added
                ServiceNotice.Updated -> R.string.service_updated
                ServiceNotice.Deleted -> R.string.service_deleted
                ServiceNotice.DeleteFailed -> R.string.service_delete_error
                ServiceNotice.QuickFailed -> R.string.quick_service_error
                ServiceNotice.Undone -> R.string.registration_undone
            }
            val canUndo = notice == ServiceNotice.Added && registered != null
            val result = snackbar.showSnackbar(
                message = if (canUndo) resources.getString(R.string.quick_service_saved, formatSoles(BigDecimal.valueOf(checkNotNull(registered).amountCents, 2)))
                    else resources.getString(message),
                actionLabel = if (canUndo) resources.getString(R.string.undo) else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed && registered != null) model.undoRegistration(registered.id)
            model.clearNotice(version)
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(stringResource(if (quickEntry) R.string.quick_service_title else R.string.services), style = MaterialTheme.typography.headlineSmall)
                if (!quickEntry) Button(onClick = model::add, enabled = !state.busy) { Text(stringResource(R.string.add_service)) }
            }
            if (quickEntry) {
                Column(Modifier.padding(bottom = if (state.notice != null) 64.dp else 0.dp)
                    .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.quick_service_hint), style = MaterialTheme.typography.bodyLarge)
                    if (state.faresLoading) Text(stringResource(R.string.fares_loading))
                    if (state.faresError) {
                        Text(stringResource(R.string.fares_load_error))
                        TextButton(onClick = model::reloadFares) { Text(stringResource(R.string.retry)) }
                    }
                    if (!state.faresLoading && !state.faresError && state.fares.isEmpty()) Text(stringResource(R.string.fares_empty))
                    state.fares.chunked(2).forEach { fares ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            fares.forEach { fare ->
                                val cents = fare.amountCents
                                Button(
                                    onClick = { model.registerQuick(cents) }, enabled = !state.busy && !state.quickLocked,
                                    modifier = Modifier.weight(1f).heightIn(min = 80.dp).testTag("quickFare$cents"),
                                ) {
                                    Text(formatSoles(BigDecimal.valueOf(cents, 2)), style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                    Button(onClick = model::add, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                        Text(stringResource(R.string.other_amount), style = MaterialTheme.typography.titleLarge)
                    }
                    DailySummaryCard(checkNotNull(dailyRepository))
                }
            } else when {
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.loadError -> Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.services_load_error))
                    Button(onClick = model::reload) { Text(stringResource(R.string.retry)) }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.services_total), style = MaterialTheme.typography.labelLarge)
                                Text(formatSoles(state.total), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("servicesTotal"))
                                Text(stringResource(R.string.services_count, state.services.size))
                            }
                        }
                    }
                    if (state.services.isEmpty()) item {
                        Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.no_records), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.services_empty_description))
                        }
                    }
                    items(state.services, key = { it.id }) { service ->
                        ServiceCard(service, !state.busy, { model.edit(service) }, { model.requestDelete(service) })
                    }
                }
            }
        }
    }
    state.editor?.let { editor ->
        ServiceEditorDialog(editor, state.busy, model::changeAmount, model::changeNote,
            model::changeDate, model::changeTime, model::save, model::closeEditor)
    }
    state.pendingDelete?.let { service ->
        AlertDialog(
            onDismissRequest = model::cancelDelete,
            title = { Text(stringResource(R.string.delete_service_title)) },
            text = { Text(stringResource(R.string.delete_service_message, formatSoles(BigDecimal.valueOf(service.amountCents, 2)))) },
            confirmButton = {
                TextButton(onClick = model::confirmDelete, enabled = !state.busy, modifier = Modifier.testTag("confirmDelete")) {
                    Text(stringResource(if (state.busy) R.string.saving else R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = model::cancelDelete, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ServiceCard(service: TaxiService, enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val zone = ZoneId.systemDefault()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatSoles(BigDecimal.valueOf(service.amountCents, 2)), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.service_date_time, ServiceInput.date(service.occurredAt, zone), ServiceInput.time(service.occurredAt, zone)))
            service.note?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit, enabled = enabled) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = onDelete, enabled = enabled) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
private fun ServiceEditorDialog(
    editor: ServiceEditor,
    busy: Boolean,
    onAmount: (String) -> Unit,
    onNote: (String) -> Unit,
    onDate: (String) -> Unit,
    onTime: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val amountFocus = remember { FocusRequester() }
    var details by rememberSaveable(editor.id) { mutableStateOf(false) }
    LaunchedEffect(editor.id) { if (editor.id == null) amountFocus.requestFocus() }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (editor.id == null) R.string.new_service else R.string.edit_service)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.amount, onValueChange = onAmount,
                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus).testTag("serviceAmount"),
                    label = { Text(stringResource(R.string.amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!busy) onSave() }),
                    singleLine = true, enabled = !busy, isError = editor.amountError,
                    supportingText = if (editor.amountError) {{ Text(stringResource(R.string.invalid_amount)) }} else null,
                )
                if (editor.id != null) TextButton(onClick = { details = !details }) {
                    Text(stringResource(if (details) R.string.hide_details else R.string.more_details))
                }
                if (editor.id != null && details) OutlinedTextField(
                    value = editor.note, onValueChange = onNote,
                    modifier = Modifier.fillMaxWidth().testTag("serviceNote"),
                    label = { Text(stringResource(R.string.note_label)) },
                    minLines = 2, maxLines = 4, enabled = !busy,
                )
                if (editor.id == null) {
                    Text(stringResource(R.string.automatic_date_time), style = MaterialTheme.typography.bodySmall)
                } else if (details) {
                    OutlinedTextField(
                        value = editor.date, onValueChange = onDate,
                        modifier = Modifier.fillMaxWidth().testTag("serviceDate"),
                        label = { Text(stringResource(R.string.date_label)) },
                        singleLine = true, enabled = !busy, isError = editor.dateTimeError,
                    )
                    OutlinedTextField(
                        value = editor.time, onValueChange = onTime,
                        modifier = Modifier.fillMaxWidth().testTag("serviceTime"),
                        label = { Text(stringResource(R.string.time_label)) },
                        singleLine = true, enabled = !busy, isError = editor.dateTimeError,
                    )
                    if (editor.dateTimeError) Text(stringResource(R.string.invalid_date_time), color = MaterialTheme.colorScheme.error)
                }
                if (editor.saveError) Text(stringResource(R.string.service_save_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !busy, modifier = Modifier.testTag("saveService")) {
                Text(stringResource(if (busy) R.string.saving else if (editor.id == null) R.string.register else R.string.save_changes))
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !busy) { Text(stringResource(R.string.cancel)) } },
    )
}
