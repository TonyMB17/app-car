package pe.mitaxi.app.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import pe.mitaxi.app.TaxiApplication
import pe.mitaxi.app.notification.QuickNotification

class QuickNotificationTest {
    @Test fun lockedPhoneDoesNotRegister() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TaxiApplication>()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (android.os.Build.VERSION.SDK_INT >= 33) automation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val controller = app.quickNotification
        val initial = app.serviceRepository.services.first().size
        fun shell(command: String) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
        }
        try {
            controller.setEnabled(true)
            val fare = app.fareRepository.fares.first().first()
            val token = app.widgetRepository.state(QuickNotification.ENTRY_ID).token
            shell("input keyevent 223")
            withTimeout(8000) { while (!app.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked) delay(100) }
            controller.action(token, QuickNotification.REGISTER, "locked") { putExtra("fare", fare.id); putExtra("amount", fare.amountCents) }.send()
            delay(700)
            assertEquals(initial, app.serviceRepository.services.first().size)
            assertFalse(controller.registerOther(token, 850))
        } finally {
            shell("input keyevent 224"); shell("wm dismiss-keyguard")
            controller.setEnabled(false)
        }
    }

    @Test fun notificationRegistersRepeatsUndoesAndRejectsDisabledActions() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TaxiApplication>()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        if (android.os.Build.VERSION.SDK_INT >= 33) automation.grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val controller = app.quickNotification
        val manager = app.getSystemService(NotificationManager::class.java)
        val initial = app.serviceRepository.services.first().map { it.id }.toSet()
        suspend fun count() = app.serviceRepository.services.first().size
        suspend fun awaitCount(expected: Int) = withTimeout(5000) { while (count() != expected) delay(50) }
        fun notification() = manager.activeNotifications.single { it.id == QuickNotification.ID }.notification
        try {
            controller.setEnabled(true)
            assertTrue(controller.enabled.first())
            assertNotNull(notification().bigContentView)
            assertEquals(Notification.VISIBILITY_SECRET, notification().visibility)
            val fare = app.fareRepository.fares.first().first()
            suspend fun pending() = controller.action(app.widgetRepository.state(QuickNotification.ENTRY_ID).token,
                QuickNotification.REGISTER, "fare") { putExtra("fare", fare.id); putExtra("amount", fare.amountCents) }
            val first = pending()
            first.send(); first.send()
            awaitCount(initial.size + 1)
            delay(800)
            assertEquals(initial.size + 1, count())
            withTimeout(5000) { while (!notification().extras.getString(Notification.EXTRA_TEXT).orEmpty().contains("Guardado")) delay(50) }
            pending().send()
            awaitCount(initial.size + 2)
            val saved = app.widgetRepository.state(QuickNotification.ENTRY_ID)
            controller.action(saved.token, QuickNotification.UNDO, "undo").send()
            awaitCount(initial.size + 1)
            delay(700)
            val token = app.widgetRepository.state(QuickNotification.ENTRY_ID).token
            assertTrue(controller.registerOther(token, 850))
            controller.handle(Intent().setAction(QuickNotification.CONFIRM))
            assertTrue(notification().extras.getString(Notification.EXTRA_TEXT).orEmpty().contains("8.50"))
            val stale = pending()
            controller.setEnabled(false)
            assertFalse(controller.enabled.first())
            assertFalse(manager.activeNotifications.any { it.id == QuickNotification.ID })
            stale.send()
            delay(700)
            assertEquals(initial.size + 2, count())
            assertFalse(controller.registerOther(token, 850))
            controller.setEnabled(true)
            stale.send()
            delay(700)
            assertEquals(initial.size + 2, count())
            controller.handle(Intent().setAction(QuickNotification.DISMISS)
                .putExtra("token", app.widgetRepository.state(QuickNotification.ENTRY_ID).token))
            assertFalse(controller.enabled.first())
            assertFalse(manager.activeNotifications.any { it.id == QuickNotification.ID })
        } finally {
            controller.setEnabled(false)
            app.serviceRepository.services.first().filter { it.id !in initial }.forEach { app.serviceRepository.delete(it.id) }
        }
    }
}
