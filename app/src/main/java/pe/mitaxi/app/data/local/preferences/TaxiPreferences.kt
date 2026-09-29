package pe.mitaxi.app.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map

val Context.taxiPreferences by preferencesDataStore(name = "taxi_preferences")

class NotificationPreferences(private val context: Context) {
    private val key = booleanPreferencesKey("quick_notification_enabled")
    val enabled = context.taxiPreferences.data.map { it[key] ?: false }
    suspend fun setEnabled(value: Boolean) { context.taxiPreferences.edit { it[key] = value } }
}
