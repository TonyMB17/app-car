package pe.mitaxi.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.mitaxi.app.R
import pe.mitaxi.app.TaxiApplication
import pe.mitaxi.app.notification.QuickNotification

data class NotificationSettingsState(val enabled: Boolean = false, val permitted: Boolean = true,
    val busy: Boolean = false, val error: Boolean = false)

class NotificationSettingsModel(private val controller: QuickNotification) : ViewModel() {
    private val mutable = MutableStateFlow(NotificationSettingsState(permitted = controller.permitted()))
    val state = mutable.asStateFlow()
    init { viewModelScope.launch {
        controller.enabled.catch { mutable.update { it.copy(error = true) } }
            .collect { value -> mutable.update { it.copy(enabled = value) } }
    } }
    fun resume() { mutable.update { it.copy(permitted = controller.permitted()) } }
    fun enable(value: Boolean) = change { controller.setEnabled(value) }
    fun show() = change { controller.refresh() }
    private fun change(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.e("NotificationSettings", "Unable to update setting", error)
                mutable.update { it.copy(error = true) }
            } finally { mutable.update { it.copy(busy = false, permitted = controller.permitted()) } }
        }
    }
}

@Composable
fun NotificationSettings() {
    val context = LocalContext.current
    val controller = (context.applicationContext as TaxiApplication).quickNotification
    val model: NotificationSettingsModel = viewModel(factory = viewModelFactory { initializer { NotificationSettingsModel(controller) } })
    val state by model.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.resume()
        if (granted) model.enable(true)
    }
    LifecycleResumeEffect(Unit) { model.resume(); onPauseOrDispose {} }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.notification_setting), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = state.enabled, enabled = !state.busy, modifier = Modifier.testTag("quickNotification"), onCheckedChange = { value ->
                    if (value && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else model.enable(value)
                })
            }
            Text(stringResource(R.string.notification_hint))
            if (!state.permitted) {
                Text(stringResource(R.string.notification_permission))
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) {
                    Text(stringResource(R.string.notification_open_settings))
                }
            } else if (state.enabled) TextButton(onClick = model::show, enabled = !state.busy) { Text(stringResource(R.string.notification_show)) }
            if (state.error) Text(stringResource(R.string.notification_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
