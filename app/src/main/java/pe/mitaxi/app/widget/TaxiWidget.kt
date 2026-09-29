package pe.mitaxi.app.widget

import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import pe.mitaxi.app.MainActivity
import pe.mitaxi.app.R
import pe.mitaxi.app.TaxiApplication
import pe.mitaxi.app.utils.formatSoles

class TaxiWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try { withTimeout(8_000) { WidgetUpdater.handle(context.applicationContext, intent) } }
            catch (error: Exception) { Log.e("TaxiWidget", "Widget operation failed", error) }
            finally { pending?.finish() }
        }
    }
    companion object {
        const val REGISTER = "pe.mitaxi.app.widget.REGISTER"
        const val PAGE = "pe.mitaxi.app.widget.PAGE"
        const val UNDO = "pe.mitaxi.app.widget.UNDO"
        const val CONFIRM = "pe.mitaxi.app.widget.CONFIRM"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

object WidgetUpdater {
    private val mutex = Mutex()
    fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, TaxiWidget::class.java))

    suspend fun refresh(context: Context) = mutex.withLock {
        for (id in ids(context)) render(context, id)
    }

    suspend fun handle(context: Context, intent: Intent) {
        var resetId: Int? = null
        mutex.withLock {
            val app = context as TaxiApplication
            if (intent.action == AppWidgetManager.ACTION_APPWIDGET_DELETED) {
                val deleted = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: intArrayOf(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
                deleted.filter { it >= 0 }.forEach { app.widgetRepository.remove(it) }
                return@withLock
            }
            val active = ids(context)
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            var message: String? = null
            if (id in active && intent.action in listOf(TaxiWidget.REGISTER, TaxiWidget.UNDO, TaxiWidget.PAGE)) {
                val token = intent.getStringExtra("token").orEmpty()
                val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
                try {
                    if (locked) message = context.getString(R.string.widget_locked)
                    else when (intent.action) {
                        TaxiWidget.REGISTER -> {
                            val amount = intent.getLongExtra("amount", -1)
                            if (app.widgetRepository.register(id, token, intent.getLongExtra("fare", -1), amount)) {
                                resetId = id
                                WidgetFeedback.saved(context, amount)
                            }
                        }
                        TaxiWidget.UNDO -> if (app.widgetRepository.undo(id, token)) message = context.getString(R.string.registration_undone)
                        TaxiWidget.PAGE -> app.widgetRepository.page(id, token, intent.getIntExtra("direction", 1))
                    }
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    Log.e("TaxiWidget", "Unable to apply widget action", error)
                    message = context.getString(R.string.widget_action_error)
                }
            }
            if (id in active && intent.action == TaxiWidget.CONFIRM) {
                resetId = id
            }
            for (widgetId in active) render(context, widgetId, if (widgetId == id) message else null)
        }
        resetId?.let { id ->
            // Wait outside the lock so another registration remains responsive.
            kotlinx.coroutines.delay(1800)
            mutex.withLock {
                if (id in ids(context)) render(context, id)
            }
        }
    }

    suspend fun render(context: Context, id: Int, message: String? = null) {
        val app = context.applicationContext as TaxiApplication
        val views = RemoteViews(context.packageName, R.layout.taxi_widget)
        val manager = AppWidgetManager.getInstance(context)
        views.setOnClickPendingIntent(R.id.widget_title, PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        try {
            val fares = app.fareRepository.fares.first()
            val state = app.widgetRepository.state(id)
            views.setOnClickPendingIntent(R.id.widget_other, PendingIntent.getActivity(context, 1,
                Intent(context, WidgetAmountActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(Uri.Builder().scheme("mitaxi-amount").authority(id.toString()).appendPath(state.token).build())
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("token", state.token),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            val pageCount = ((fares.size + 3) / 4).coerceAtLeast(1)
            val page = state.page.coerceIn(0, pageCount - 1)
            val buttons = listOf(R.id.widget_fare_1, R.id.widget_fare_2, R.id.widget_fare_3, R.id.widget_fare_4)
            buttons.forEachIndexed { index, button ->
                val fare = fares.getOrNull(page * 4 + index)
                views.setViewVisibility(button, if (fare == null) View.INVISIBLE else View.VISIBLE)
                if (fare != null) {
                    views.setTextViewText(button, formatSoles(BigDecimal.valueOf(fare.amountCents, 2)))
                    views.setOnClickPendingIntent(button, action(context, id, state.token, TaxiWidget.REGISTER, "fare${fare.id}") {
                        putExtra("fare", fare.id); putExtra("amount", fare.amountCents)
                    })
                }
            }
            views.setTextViewText(R.id.widget_page, context.getString(R.string.widget_page, page + 1, pageCount))
            views.setBoolean(R.id.widget_previous, "setEnabled", page > 0)
            views.setBoolean(R.id.widget_next, "setEnabled", page < pageCount - 1)
            views.setOnClickPendingIntent(R.id.widget_previous, action(context, id, state.token, TaxiWidget.PAGE, "previous") { putExtra("direction", -1) })
            views.setOnClickPendingIntent(R.id.widget_next, action(context, id, state.token, TaxiWidget.PAGE, "next") { putExtra("direction", 1) })
            views.setViewVisibility(R.id.widget_paging, if (pageCount > 1) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_undo, if (state.lastServiceId != null) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(R.id.widget_undo, action(context, id, state.token, TaxiWidget.UNDO, "undo"))
            val status = message ?: if (state.lastServiceId != null) context.getString(R.string.widget_saved,
                formatSoles(BigDecimal.valueOf(state.lastAmount, 2)),
                Instant.ofEpochMilli(state.lastTap).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")))
                else context.getString(if (fares.isEmpty()) R.string.widget_empty else R.string.quick_service_hint)
            views.setTextViewText(R.id.widget_status, status)
            val justSaved = message == null && state.lastServiceId != null &&
                System.currentTimeMillis() - state.lastTap in 0..1700
            views.setInt(R.id.widget_status, "setBackgroundColor", if (justSaved) 0xFFD8F3DC.toInt() else android.graphics.Color.TRANSPARENT)
            views.setTextColor(R.id.widget_status, if (justSaved) 0xFF14532D.toInt() else 0xFF332900.toInt())
            views.setTextViewTextSize(R.id.widget_status, android.util.TypedValue.COMPLEX_UNIT_SP, if (justSaved) 18f else 14f)
            if (justSaved) views.setTextViewText(R.id.widget_status,
                context.getString(R.string.widget_save_confirmation, formatSoles(BigDecimal.valueOf(state.lastAmount, 2))))
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            Log.e("TaxiWidget", "Unable to render fares", error)
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_load_error))
            listOf(R.id.widget_fare_1, R.id.widget_fare_2, R.id.widget_fare_3, R.id.widget_fare_4, R.id.widget_paging, R.id.widget_undo, R.id.widget_other)
                .forEach { views.setViewVisibility(it, View.GONE) }
        }
        manager.updateAppWidget(id, views)
    }

    fun action(context: Context, id: Int, token: String, action: String, key: String, extras: Intent.() -> Unit = {}): PendingIntent {
        val intent = Intent(context, TaxiWidget::class.java).setAction(action)
            .setData(Uri.Builder().scheme("mitaxi-widget").authority(id.toString()).appendPath(token).appendPath(key).build())
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("token", token).apply(extras)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
