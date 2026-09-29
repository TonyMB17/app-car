package pe.mitaxi.app.notification

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.mitaxi.app.MainActivity
import pe.mitaxi.app.R
import pe.mitaxi.app.TaxiApplication
import pe.mitaxi.app.utils.formatSoles
import pe.mitaxi.app.widget.WidgetAmountActivity
import pe.mitaxi.app.widget.WidgetFeedback

/** Uses a reserved, negative quick-entry ID; Android widget IDs are nonnegative. */
class QuickNotification(private val app: TaxiApplication) {
    private val mutex = Mutex()
    private val manager get() = app.getSystemService(NotificationManager::class.java)
    val enabled get() = app.notificationPreferences.enabled

    fun permitted(): Boolean = manager.areNotificationsEnabled() &&
        manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    suspend fun setEnabled(value: Boolean) = mutex.withLock {
        if (value) check(permitted())
        // Invalidate PendingIntents from a previous activation without touching services.
        app.widgetRepository.remove(ENTRY_ID)
        app.notificationPreferences.setEnabled(value)
        render()
    }

    suspend fun refresh() = mutex.withLock { render() }

    suspend fun registerOther(token: String, amount: Long): Boolean = mutex.withLock {
        if (!enabled.first() || !permitted() || locked()) return@withLock false
        app.widgetRepository.registerOther(ENTRY_ID, token, amount)
    }

    suspend fun handle(intent: Intent) {
        var saved = false
        mutex.withLock {
            if (!enabled.first()) return@withLock
            if (intent.action == DISMISS) {
                if (intent.getStringExtra("token") != app.widgetRepository.state(ENTRY_ID).token) return@withLock
                app.notificationPreferences.setEnabled(false)
                app.widgetRepository.remove(ENTRY_ID)
                manager.cancel(ID)
                return@withLock
            }
            if (!permitted()) return@withLock
            var message: String? = null
            try {
                if (locked()) message = app.getString(R.string.widget_locked)
                else when (intent.action) {
                    REGISTER -> {
                        val amount = intent.getLongExtra("amount", -1)
                        saved = app.widgetRepository.register(ENTRY_ID, intent.getStringExtra("token").orEmpty(),
                            intent.getLongExtra("fare", -1), amount)
                        if (saved) WidgetFeedback.saved(app, amount)
                    }
                    UNDO -> if (app.widgetRepository.undo(ENTRY_ID, intent.getStringExtra("token").orEmpty()))
                        message = app.getString(R.string.registration_undone)
                    PAGE -> app.widgetRepository.page(ENTRY_ID, intent.getStringExtra("token").orEmpty(), intent.getIntExtra("direction", 1))
                    CONFIRM -> saved = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.e("QuickNotification", "Unable to apply action", error)
                message = app.getString(R.string.widget_action_error)
            }
            render(message)
        }
        if (saved) {
            delay(1800)
            refresh()
        }
    }

    private fun locked() = app.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private suspend fun render(message: String? = null) {
        if (!enabled.first() || !permitted()) { manager.cancel(ID); return }
        manager.createNotificationChannel(NotificationChannel(CHANNEL, app.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW))
        val state = app.widgetRepository.state(ENTRY_ID)
        val fares = app.fareRepository.fares.first()
        val pages = ((fares.size + 3) / 4).coerceAtLeast(1)
        val page = state.page.coerceIn(0, pages - 1)
        val justSaved = message == null && state.lastServiceId != null && System.currentTimeMillis() - state.lastTap in 0..1700
        val status = message ?: if (state.lastServiceId != null) app.getString(R.string.notification_saved,
            formatSoles(BigDecimal.valueOf(state.lastAmount, 2)),
            Instant.ofEpochMilli(state.lastTap).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss")))
            else app.getString(if (fares.isEmpty()) R.string.widget_empty else R.string.quick_service_hint)
        val views = RemoteViews(app.packageName, R.layout.quick_notification)
        views.setTextViewText(R.id.notification_status, status)
        views.setInt(R.id.notification_status, "setBackgroundColor", if (justSaved) 0xFFD8F3DC.toInt() else 0xFFFFF8E1.toInt())
        views.setTextColor(R.id.notification_status, if (justSaved) 0xFF14532D.toInt() else 0xFF332900.toInt())
        listOf(R.id.notification_fare_1, R.id.notification_fare_2, R.id.notification_fare_3, R.id.notification_fare_4).forEachIndexed { index, view ->
            val fare = fares.getOrNull(page * 4 + index)
            views.setViewVisibility(view, if (fare == null) View.GONE else View.VISIBLE)
            if (fare != null) {
                views.setTextViewText(view, formatSoles(BigDecimal.valueOf(fare.amountCents, 2)))
                views.setOnClickPendingIntent(view, action(state.token, REGISTER, "fare${fare.id}") {
                    putExtra("fare", fare.id); putExtra("amount", fare.amountCents)
                })
            }
        }
        views.setViewVisibility(R.id.notification_paging, if (pages > 1) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.notification_page, app.getString(R.string.widget_page, page + 1, pages))
        views.setBoolean(R.id.notification_previous, "setEnabled", page > 0)
        views.setBoolean(R.id.notification_next, "setEnabled", page < pages - 1)
        views.setOnClickPendingIntent(R.id.notification_previous, action(state.token, PAGE, "previous") { putExtra("direction", -1) })
        views.setOnClickPendingIntent(R.id.notification_next, action(state.token, PAGE, "next") { putExtra("direction", 1) })
        val other = PendingIntent.getActivity(app, 2,
            Intent(app, WidgetAmountActivity::class.java)
                .setData(Uri.Builder().scheme("mitaxi-notification").authority("other").appendPath(state.token).build())
                .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, ENTRY_ID).putExtra("token", state.token),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_taxi)
            .setContentTitle(app.getString(R.string.notification_title)).setContentText(status)
            .setSubText(app.getString(R.string.notification_expand))
            .setStyle(Notification.DecoratedCustomViewStyle()).setCustomBigContentView(views)
            .setOngoing(true).setOnlyAlertOnce(true).setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_SECRET).setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(PendingIntent.getActivity(app, 3, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .setDeleteIntent(action(state.token, DISMISS, "dismiss"))
            .addAction(Notification.Action.Builder(null, app.getString(R.string.other_amount), other).build())
        if (state.lastServiceId != null) notification.addAction(
            Notification.Action.Builder(null, app.getString(R.string.undo), action(state.token, UNDO, "undo")).build())
        if (android.os.Build.VERSION.SDK_INT >= 29) notification.setAllowSystemGeneratedContextualActions(false)
        manager.notify(ID, notification.build())
    }

    fun action(token: String, action: String, key: String, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(app, 0, Intent(app, QuickNotificationReceiver::class.java).setAction(action)
            .setData(Uri.Builder().scheme("mitaxi-notification").authority("action").appendPath(token).appendPath(key).build()).putExtra("token", token).apply(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    companion object {
        const val ENTRY_ID = -2
        const val ID = 700
        const val CHANNEL = "quick_services"
        const val REGISTER = "pe.mitaxi.app.notification.REGISTER"
        const val UNDO = "pe.mitaxi.app.notification.UNDO"
        const val PAGE = "pe.mitaxi.app.notification.PAGE"
        const val CONFIRM = "pe.mitaxi.app.notification.CONFIRM"
        const val DISMISS = "pe.mitaxi.app.notification.DISMISS"
    }
}

class QuickNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                withTimeout(8000) {
                    val controller = (context.applicationContext as TaxiApplication).quickNotification
                    if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) controller.refresh()
                    else controller.handle(intent)
                }
            } catch (error: Exception) { android.util.Log.e("QuickNotification", "Unable to update notification", error) }
            finally { pending.finish() }
        }
    }
    companion object { private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}
