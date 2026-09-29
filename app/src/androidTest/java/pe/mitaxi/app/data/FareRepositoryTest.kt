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
import java.util.UUID
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.repository.FareRepository

class FareRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun persistsOrderAndEmptyListWithoutChangingHistoricalServices() = runBlocking {
        val name = "fares-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, TaxiDatabase::class.java, name).addCallback(TaxiDatabase.INITIAL_FARES).build()
        var db = open()
        try {
            var repo = FareRepository(db)
            assertEquals(listOf(500L, 600L, 700L, 1000L), repo.fares.first().map { it.amountCents })
            val id = repo.save(null, 850)
            repo.move(id, -1)
            assertEquals(id, repo.fares.first()[3].id)
            repo.register(id, 850)
            repo.save(id, 1250)
            assertTrue(runCatching { repo.register(id, 850) }.isFailure)
            assertTrue(runCatching { repo.save(null, 1250) }.isFailure)
            assertTrue(runCatching { repo.save(null, 0) }.isFailure)
            db.close(); db = open(); repo = FareRepository(db)
            assertEquals(1250L, repo.fares.first()[3].amountCents)
            repo.fares.first().forEach { repo.delete(it.id) }
            db.close(); db = open(); repo = FareRepository(db)
            assertTrue(repo.fares.first().isEmpty())
            val service = db.serviceDao().observeAll().first().single()
            assertEquals(850L, service.amountCents)
            assertEquals(id, service.fareId)
            assertTrue(runCatching { repo.register(id, 1250) }.isFailure)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migratesVersion2AndPreservesServicesAndExpenses() = runBlocking {
        val name = "fares-migration-${UUID.randomUUID()}.db"
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("${TaxiDatabase::class.java.name}/2.json").bufferedReader().use { it.readText() }).getJSONObject("database")
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
            old.execSQL("INSERT INTO services VALUES (1, 123, 850, NULL, 'Conservar', 123, 123)")
            old.execSQL("INSERT INTO expenses VALUES (1, 123, 'FUEL', 5000, 123, 123)")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, TaxiDatabase::class.java, name).addMigrations(TaxiDatabase.MIGRATION_2_3, TaxiDatabase.MIGRATION_3_4).build()
        try {
            assertEquals(listOf(500L, 600L, 700L, 1000L), db.fareDao().all().map { it.amountCents })
            assertEquals("Conservar", db.serviceDao().observeAll().first().single().note)
            assertEquals(850L, db.serviceDao().observeAll().first().single().amountCents)
            assertEquals(5000L, db.expenseDao().observeAll().first().single().amountCents)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
