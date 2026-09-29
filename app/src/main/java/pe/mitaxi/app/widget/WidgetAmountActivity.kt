package pe.mitaxi.app.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.mitaxi.app.R
import pe.mitaxi.app.TaxiApplication
import pe.mitaxi.app.data.repository.WidgetRepository
import pe.mitaxi.app.domain.model.ServiceInput
import pe.mitaxi.app.ui.theme.TaxiTheme

data class WidgetAmountState(val amount: String = "", val busy: Boolean = false, val invalid: Boolean = false,
    val error: Boolean = false, val done: Boolean = false)

class WidgetAmountModel(private val saved: SavedStateHandle,
    private val register: suspend (Long) -> Boolean) : ViewModel() {
    private val mutable = MutableStateFlow(WidgetAmountState(amount = saved["amount"] ?: "", done = saved["done"] ?: false))
    val state = mutable.asStateFlow()
    fun amount(value: String) {
        if (state.value.busy || state.value.done) return
        saved["amount"] = value
        mutable.update { it.copy(amount = value, invalid = false, error = false) }
    }
    fun save() {
        if (state.value.busy || state.value.done) return
        val cents = ServiceInput.amountCents(state.value.amount)
        if (cents == null) { mutable.update { it.copy(invalid = true) }; return }
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                check(register(cents))
                saved["done"] = true
                mutable.update { it.copy(done = true) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { android.util.Log.e("WidgetAmount", "Unable to save", error); mutable.update { it.copy(error = true) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}

class WidgetAmountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as TaxiApplication
            val entryId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val token = intent.getStringExtra("token").orEmpty()
            val fromNotification = entryId == pe.mitaxi.app.notification.QuickNotification.ENTRY_ID
            val model: WidgetAmountModel = viewModel(factory = viewModelFactory {
                initializer { WidgetAmountModel(createSavedStateHandle()) { amount ->
                    if (fromNotification) app.quickNotification.registerOther(token, amount)
                    else app.widgetRepository.registerOther(entryId, token, amount)
                } }
            })
            val state by model.state.collectAsState()
            LaunchedEffect(state.done) {
                if (state.done) {
                    if (fromNotification) app.sendBroadcast(android.content.Intent(app, pe.mitaxi.app.notification.QuickNotificationReceiver::class.java)
                        .setAction(pe.mitaxi.app.notification.QuickNotification.CONFIRM))
                    else app.sendBroadcast(android.content.Intent(app, TaxiWidget::class.java).setAction(TaxiWidget.CONFIRM)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, entryId))
                    WidgetFeedback.saved(app, checkNotNull(ServiceInput.amountCents(state.amount)))
                    finish()
                }
            }
            TaxiTheme {
                Scaffold { padding ->
                    val focus = remember { FocusRequester() }
                    Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.other_amount), style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(value = state.amount, onValueChange = model::amount,
                            modifier = Modifier.fillMaxWidth().focusRequester(focus), label = { Text(stringResource(R.string.amount_label)) },
                            singleLine = true, enabled = !state.busy, isError = state.invalid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { model.save() }))
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        if (state.invalid) Text(stringResource(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
                        if (state.error) Text(stringResource(R.string.quick_service_error), color = MaterialTheme.colorScheme.error)
                        Button(onClick = model::save, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(stringResource(if (state.busy) R.string.saving else R.string.register))
                        }
                        TextButton(onClick = { finish() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
        }
    }
}
