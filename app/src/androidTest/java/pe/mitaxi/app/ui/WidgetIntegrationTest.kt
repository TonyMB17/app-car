package pe.mitaxi.app.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import pe.mitaxi.app.TaxiApplication
import pe.mitaxi.app.widget.TaxiWidget
import pe.mitaxi.app.widget.WidgetUpdater

class WidgetIntegrationTest {
    @Test fun realPendingIntentRegistersOnlyOnceAndUndoRemovesOnlyThatService() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TaxiApplication>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val host = AppWidgetHost(app, 606)
        val manager = AppWidgetManager.getInstance(app)
        val id = host.allocateAppWidgetId()
        try {
            instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            try { assertTrue(manager.bindAppWidgetIdIfAllowed(id, ComponentName(app, TaxiWidget::class.java))) }
            finally { instrumentation.uiAutomation.dropShellPermissionIdentity() }
            WidgetUpdater.refresh(app)
            val initialCount = app.serviceRepository.services.first().size
            val state = app.widgetRepository.state(id)
            val fare = app.fareRepository.fares.first().first()
            val pending = WidgetUpdater.action(app, id, state.token, TaxiWidget.REGISTER, "test") {
                putExtra("fare", fare.id); putExtra("amount", fare.amountCents)
            }
            pending.send(); pending.send()
            assertTrue("El registro no llegó al repositorio", kotlinx.coroutines.withTimeoutOrNull(5000) {
                while (app.serviceRepository.services.first().size != initialCount + 1) delay(50)
                true
            } ?: false)
            delay(700)
            assertEquals(initialCount + 1, app.serviceRepository.services.first().size)
            val current = app.widgetRepository.state(id)
            WidgetUpdater.action(app, id, current.token, TaxiWidget.UNDO, "undoTest").send()
            assertTrue("Deshacer no eliminó el registro", kotlinx.coroutines.withTimeoutOrNull(5000) {
                while (app.serviceRepository.services.first().size != initialCount) delay(50)
                true
            } ?: false)
            fun shell(command: String) {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
            }
            try {
                shell("input keyevent 223")
                val keyguard = app.getSystemService(android.app.KeyguardManager::class.java)
                withTimeout(8000) { while (!keyguard.isKeyguardLocked) delay(100) }
                WidgetUpdater.action(app, id, app.widgetRepository.state(id).token, TaxiWidget.REGISTER, "lockedTest") {
                    putExtra("fare", fare.id); putExtra("amount", fare.amountCents)
                }.send()
                delay(700)
                assertEquals(initialCount, app.serviceRepository.services.first().size)
            } finally { shell("input keyevent 224"); shell("wm dismiss-keyguard") }
        } finally { host.deleteAppWidgetId(id); app.widgetRepository.remove(id) }
    }
}
