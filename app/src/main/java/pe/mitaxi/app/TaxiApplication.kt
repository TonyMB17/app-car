package pe.mitaxi.app

import android.app.Application
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.repository.LocalStorageRepository
import pe.mitaxi.app.data.repository.ServiceRepository
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.data.repository.ExpenseRepository

class TaxiApplication : Application() {
    private val widgetScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    val widgetRepository by lazy { pe.mitaxi.app.data.repository.WidgetRepository(database) }
    val notificationPreferences by lazy { pe.mitaxi.app.data.local.preferences.NotificationPreferences(this) }
    val quickNotification by lazy { pe.mitaxi.app.notification.QuickNotification(this) }
    override fun onCreate() {
        super.onCreate()
        widgetScope.launch {
            kotlinx.coroutines.flow.combine(fareRepository.fares, notificationPreferences.enabled) { _, _ -> Unit }
                .catch { error -> android.util.Log.e("QuickNotification", "Unable to observe settings", error) }
                .collect {
                    try { quickNotification.refresh() }
                    catch (error: Exception) { android.util.Log.e("QuickNotification", "Unable to refresh", error) }
                }
        }
        widgetScope.launch {
            try {
                fareRepository.fares.collect {
                    try { pe.mitaxi.app.widget.WidgetUpdater.refresh(this@TaxiApplication) }
                    catch (error: Exception) { android.util.Log.e("TaxiWidget", "Unable to refresh widgets", error) }
                }
            } catch (error: Exception) { android.util.Log.e("TaxiWidget", "Unable to observe fares", error) }
        }
    }
    private val database by lazy { TaxiDatabase.create(this) }
    val storageRepository by lazy { LocalStorageRepository(database) }
    val fareRepository by lazy { pe.mitaxi.app.data.repository.FareRepository(database) }
    val serviceRepository by lazy { ServiceRepository(database.serviceDao()) }
    val dailySummaryRepository by lazy { DailySummaryRepository(database.serviceDao(), database.expenseDao()) }
    val periodSummaryRepository by lazy { pe.mitaxi.app.data.repository.PeriodSummaryRepository(database.serviceDao(), database.expenseDao()) }
    val expenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
}
