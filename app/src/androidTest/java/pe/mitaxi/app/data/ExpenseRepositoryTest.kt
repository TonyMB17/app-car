package pe.mitaxi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.data.repository.ExpenseRepository
import pe.mitaxi.app.domain.model.DayWindow
import pe.mitaxi.app.domain.model.ExpenseCategory

@RunWith(AndroidJUnit4::class)
class ExpenseRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = Instant.parse("2026-09-29T15:00:00Z")

    @Test fun allCategoriesPersistAndEditsPreserveCreationTime() = runBlocking {
        val name = "expense-test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).build()
        try {
            var repository = ExpenseRepository(db.expenseDao(), Clock.fixed(now, ZoneOffset.UTC))
            val ids = ExpenseCategory.entries.map { repository.add(it, 1050) }
            assertEquals(ids.reversed(), repository.expenses.first().map { it.id })
            assertEquals(ExpenseCategory.entries.toSet(), repository.expenses.first().map { it.category }.toSet())
            db.close()
            db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).build()
            repository = ExpenseRepository(db.expenseDao(), Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC))
            assertEquals(5, repository.expenses.first().size)
            repository.update(ids.first(), ExpenseCategory.WASH, 2250, now.minusSeconds(3600).toEpochMilli())
            val edited = db.expenseDao().observeAll().first().single { it.id == ids.first() }
            assertEquals(now.toEpochMilli(), edited.createdAt)
            assertEquals(now.plusSeconds(60).toEpochMilli(), edited.updatedAt)
            assertEquals(now.minusSeconds(3600).toEpochMilli(), edited.occurredAt)
            assertEquals(2250L, edited.amountCents)
            val summary = DailySummaryRepository(db.serviceDao(), db.expenseDao())
                .observe(flowOf(DayWindow.at(now, ZoneId.of("America/Lima")))).first()
            assertEquals(0, summary.fuel.compareTo(BigDecimal.ZERO))
            repository.delete(ids.last())
            db.close()
            db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).build()
            assertEquals(4, db.expenseDao().observeAll().first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun rejectsInvalidAmountsAndMissingRecordsWithoutChangingData() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaxiDatabase::class.java).build()
        try {
            val repository = ExpenseRepository(db.expenseDao())
            val id = repository.add(ExpenseCategory.FUEL, 500)
            assertTrue(runCatching { repository.add(ExpenseCategory.OTHER, 0) }.isFailure)
            assertTrue(runCatching { repository.update(id, ExpenseCategory.OTHER, -1, 0) }.isFailure)
            assertTrue(runCatching { repository.update(99999, ExpenseCategory.OTHER, 500, 0) }.isFailure)
            assertTrue(runCatching { repository.delete(99999) }.isFailure)
            assertEquals(500L, repository.expenses.first().single().amountCents)
            assertEquals(ExpenseCategory.FUEL, repository.expenses.first().single().category)
        } finally { db.close() }
    }
}
