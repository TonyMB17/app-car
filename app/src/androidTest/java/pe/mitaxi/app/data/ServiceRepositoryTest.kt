package pe.mitaxi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.local.entity.ServiceEntity
import pe.mitaxi.app.data.repository.ServiceRepository

@RunWith(AndroidJUnit4::class)
class ServiceRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun persistsAcrossCloseAndReopenAndPreservesHistoricalFieldsOnEdit() = runBlocking {
        val name = "service-test-${UUID.randomUUID()}.db"
        var database = Room.databaseBuilder(context, TaxiDatabase::class.java, name).build()
        try {
            val firstTime = Instant.parse("2026-09-29T12:00:00Z")
            var repository = ServiceRepository(database.serviceDao(), Clock.fixed(firstTime, ZoneOffset.UTC))
            val id = repository.add(715, "  Aeropuerto  ")
            val secondId = repository.add(500, "  ")
            val rows = database.serviceDao().observeAll().first()
            assertEquals(listOf(secondId, id), rows.map { it.id })
            assertEquals(firstTime.toEpochMilli(), rows.last().occurredAt)
            assertEquals(firstTime.toEpochMilli(), rows.last().createdAt)
            assertNull(rows.first().note)
            database.close()

            database = Room.databaseBuilder(context, TaxiDatabase::class.java, name).build()
            val updatedTime = firstTime.plusSeconds(120)
            repository = ServiceRepository(database.serviceDao(), Clock.fixed(updatedTime, ZoneOffset.UTC))
            assertEquals(2, repository.services.first().size)
            assertEquals("Aeropuerto", repository.services.first().last().note)
            repository.update(id, 1050, "  Editado  ", firstTime.plusSeconds(60).toEpochMilli())
            val edited = database.serviceDao().observeAll().first().first()
            assertEquals(id, edited.id)
            assertEquals(1050L, edited.amountCents)
            assertEquals("Editado", edited.note)
            assertEquals(firstTime.toEpochMilli(), edited.createdAt)
            assertEquals(updatedTime.toEpochMilli(), edited.updatedAt)
            repository.delete(id)
            assertEquals(listOf(secondId), repository.services.first().map { it.id })
            database.close()
            database = Room.databaseBuilder(context, TaxiDatabase::class.java, name).build()
            assertEquals(1L, database.serviceDao().count())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun editingPreservesFareAndRejectsInvalidAmounts() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, TaxiDatabase::class.java).build()
        try {
            val dao = database.serviceDao()
            val repository = ServiceRepository(dao)
            val id = dao.insert(ServiceEntity(occurredAt = 123, amountCents = 500, fareId = 42,
                note = null, createdAt = 100, updatedAt = 100))
            repository.update(id, 600, "", 124)
            val row = dao.observeAll().first().single()
            assertEquals(42L, row.fareId)
            assertEquals(100L, row.createdAt)
            assertTrue(runCatching { repository.add(0, "") }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { repository.update(id, -1, "", 124) }.exceptionOrNull() is IllegalArgumentException)
            assertEquals(600L, dao.observeAll().first().single().amountCents)
            assertTrue(runCatching { repository.delete(99999) }.isFailure)
        } finally { database.close() }
    }
}
