package pe.mitaxi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.repository.FareRepository
import pe.mitaxi.app.data.repository.WidgetRepository

class WidgetRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private class MutableClock(var now: Long = 1_800_000_000_000L) : Clock() {
        override fun getZone() = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId) = this
        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }
    @Test fun replayAfterReopenDoubleTapAndUndoAreSafe() = runBlocking {
        val name = "widget-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, TaxiDatabase::class.java, name).addCallback(TaxiDatabase.INITIAL_FARES).build()
        var db = open()
        try {
            val clock = MutableClock()
            var repo = WidgetRepository(db, clock)
            val state = repo.state(42)
            val fare = db.fareDao().all().first()
            assertTrue(repo.register(42, state.token, fare.id, fare.amountCents))
            db.close(); db = open(); repo = WidgetRepository(db, clock)
            assertFalse(repo.register(42, state.token, fare.id, fare.amountCents))
            assertFalse(repo.register(42, repo.state(42).token, fare.id, fare.amountCents))
            assertEquals(1L, db.serviceDao().count())
            clock.now += 601
            assertTrue(repo.register(42, repo.state(42).token, fare.id, fare.amountCents))
            assertTrue(repo.undo(42, repo.state(42).token))
            assertEquals(1L, db.serviceDao().count())
            assertFalse(repo.undo(42, repo.state(42).token))
            clock.now += 601
            assertTrue(repo.registerOther(42, repo.state(42).token, 850))
            val other = db.serviceDao().find(repo.state(42).lastServiceId!!)!!
            assertEquals(850L, other.amountCents)
            assertNull(other.fareId)
            assertTrue(repo.undo(42, repo.state(42).token))
            assertEquals(1L, db.serviceDao().count())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun staleFaresAndEditedServicesAreNotAppliedAndPagesClamp() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaxiDatabase::class.java).addCallback(TaxiDatabase.INITIAL_FARES).build()
        try {
            val clock = MutableClock()
            val repo = WidgetRepository(db, clock)
            val fares = FareRepository(db)
            val state = repo.state(7)
            val first = db.fareDao().all().first()
            fares.save(first.id, 850)
            assertTrue(runCatching { repo.register(7, state.token, first.id, 500) }.isFailure)
            assertEquals(0L, db.serviceDao().count())
            assertTrue(repo.register(7, state.token, first.id, 850))
            val service = db.serviceDao().observeAll().first().single()
            db.serviceDao().update(service.id, 900, null, service.occurredAt, service.updatedAt + 1)
            assertTrue(runCatching { repo.undo(7, repo.state(7).token) }.isFailure)
            assertEquals(900L, db.serviceDao().find(service.id)!!.amountCents)
            fares.save(null, 1500)
            repo.page(7, repo.state(7).token, 1)
            assertEquals(1, repo.state(7).page)
            db.fareDao().all().drop(1).forEach { fares.delete(it.id) }
            repo.page(7, repo.state(7).token, -1)
            assertEquals(0, repo.state(7).page)
            fares.delete(first.id)
            clock.now += 601
            assertTrue(runCatching { repo.register(7, repo.state(7).token, first.id, 850) }.isFailure)
        } finally { db.close() }
    }
    @Test fun migration3PreservesCustomFaresAndHistory() = runBlocking {
        val name = "widget-migration-${UUID.randomUUID()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("${TaxiDatabase::class.java.name}/3.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO fares VALUES (12, 850, 0)")
            old.execSQL("INSERT INTO services VALUES (1, 123, 850, 12, 'Conservar', 123, 123)")
            old.execSQL("INSERT INTO expenses VALUES (1, 123, 'FUEL', 5000, 123, 123)")
            old.version = 3
        }
        val db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).addMigrations(TaxiDatabase.MIGRATION_3_4).build()
        try {
            assertEquals(850L, db.fareDao().all().single().amountCents)
            assertEquals(850L, db.serviceDao().observeAll().first().single().amountCents)
            assertEquals(5000L, db.expenseDao().observeAll().first().single().amountCents)
            assertNull(db.widgetStateDao().find(1))
            assertEquals(1, WidgetRepository(db).state(1).id)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
