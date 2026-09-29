package pe.mitaxi.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pe.mitaxi.app.R

@Composable
fun HomeScreen(state: StorageState, onRetry: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.home_description), style = MaterialTheme.typography.bodyLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.home_services_available), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(when (state) {
                    StorageState.Loading -> R.string.loading
                    StorageState.Ready -> R.string.ready
                    StorageState.Error -> R.string.storage_error
                }))
                if (state == StorageState.Error) {
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        Text(stringResource(R.string.offline), style = MaterialTheme.typography.bodyMedium)
    }
}
