package pe.mitaxi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.local.entity.ExpenseEntity
import pe.mitaxi.app.data.local.entity.ServiceEntity
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.domain.model.DailySummary
import pe.mitaxi.app.domain.model.DayWindow

@RunWith(AndroidJUnit4::class)
class DailySummaryRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instant = Instant.parse("2026-09-29T15:00:00Z")
    private val zone = ZoneId.of("America/Lima")

    @Test fun includesOnlyTodayAndFuelAndSwitchesDaysWhileObserved() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaxiDatabase::class.java).build()
        try {
            val day = DayWindow.at(instant, zone)
            for ((time, amount) in listOf(day.start - 1 to 9999L, day.start to 500L, day.end - 1 to 700L, day.end to 1500L)) {
                db.serviceDao().insert(ServiceEntity(occurredAt = time, amountCents = amount, createdAt = time, updatedAt = time))
            }
            for ((time, category, amount) in listOf(Triple(day.start - 1, "FUEL", 9999L), Triple(day.start, "FUEL", 3000L),
                Triple(day.end - 1, "FUEL", 500L), Triple(day.start, "WASH", 900L), Triple(day.end, "FUEL", 1000L))) {
                db.expenseDao().insert(ExpenseEntity(occurredAt = time, category = category, amountCents = amount, createdAt = time, updatedAt = time))
            }
            val repo = DailySummaryRepository(db.serviceDao(), db.expenseDao())
            val days = MutableStateFlow(day)
            val events = Channel<DailySummary>(Channel.UNLIMITED)
            val collection = launch { repo.observe(days).collect { events.send(it) } }
            try {
                val today = withTimeout(5000) { events.receive() }
                assertEquals(2, today.serviceCount)
                assertEquals(BigDecimal("12.00"), today.income)
                assertEquals(BigDecimal("35.00"), today.fuel)
                assertEquals(BigDecimal("44.00"), today.expenses)
                assertEquals(BigDecimal("-32.00"), today.profit)
                days.value = DayWindow.at(Instant.ofEpochMilli(day.end), zone)
                val tomorrow = withTimeout(5000) { events.receive() }
                assertEquals(days.value.date, tomorrow.date)
                assertEquals(1, tomorrow.serviceCount)
                assertEquals(BigDecimal("15.00"), tomorrow.income)
                assertEquals(BigDecimal("10.00"), tomorrow.fuel)
                assertEquals(BigDecimal("5.00"), tomorrow.profit)
            } finally { collection.cancelAndJoin(); events.close() }
        } finally { db.close() }
    }

    @Test fun exactTotalsDoNotOverflowAndFuelCanBeUndone() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaxiDatabase::class.java).build()
        try {
            val repo = DailySummaryRepository(db.serviceDao(), db.expenseDao(), Clock.fixed(instant, ZoneOffset.UTC))
            repeat(2) { db.serviceDao().insert(ServiceEntity(occurredAt = instant.toEpochMilli(), amountCents = Long.MAX_VALUE,
                createdAt = instant.toEpochMilli(), updatedAt = instant.toEpochMilli())) }
            val id = repo.addFuel(3050)
            val day = DayWindow.at(instant, zone)
            val summary = repo.observe(flowOf(day)).first()
            assertEquals(BigDecimal("184467440737095516.14"), summary.income)
            assertEquals(BigDecimal("30.50"), summary.fuel)
            repo.undoFuel(id)
            assertEquals(0, repo.observe(flowOf(day)).first().fuel.compareTo(BigDecimal.ZERO))
            org.junit.Assert.assertTrue(runCatching { repo.addFuel(0) }.isFailure)
        } finally { db.close() }
    }

    @Test fun migratesRealVersion1SchemaWithoutLosingServicesAndPersistsFuel() = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"
        val asset = "${TaxiDatabase::class.java.name}/1.json"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open(asset).bufferedReader().use { it.readText() })
            .getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO services (id, occurredAt, amountCents, fareId, note, createdAt, updatedAt) VALUES (1, ?, 700, NULL, 'Conservar', ?, ?)",
                arrayOf(instant.toEpochMilli(), instant.toEpochMilli(), instant.toEpochMilli()))
            old.version = 1
        }
        var db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).addMigrations(TaxiDatabase.MIGRATION_1_2, TaxiDatabase.MIGRATION_2_3, TaxiDatabase.MIGRATION_3_4).build()
        try {
            val previous = db.serviceDao().observeAll().first().single()
            assertEquals("Conservar", previous.note)
            assertEquals(700L, previous.amountCents)
            DailySummaryRepository(db.serviceDao(), db.expenseDao(), Clock.fixed(instant, ZoneOffset.UTC)).addFuel(2500)
            db.close()
            db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).addMigrations(TaxiDatabase.MIGRATION_1_2, TaxiDatabase.MIGRATION_2_3, TaxiDatabase.MIGRATION_3_4).build()
            val summary = DailySummaryRepository(db.serviceDao(), db.expenseDao()).observe(flowOf(DayWindow.at(instant, zone))).first()
            assertEquals(1, summary.serviceCount)
            assertEquals(BigDecimal("25.00"), summary.fuel)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
