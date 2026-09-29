package pe.mitaxi.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pe.mitaxi.app.data.local.dao.ExpenseDao
import pe.mitaxi.app.data.local.entity.ExpenseEntity
import pe.mitaxi.app.data.local.dao.ServiceDao
import pe.mitaxi.app.data.local.entity.ServiceEntity
import pe.mitaxi.app.data.local.entity.FareEntity
import pe.mitaxi.app.data.local.dao.FareDao
import pe.mitaxi.app.data.local.dao.WidgetStateDao
import pe.mitaxi.app.data.local.entity.WidgetStateEntity

@Database(entities = [ServiceEntity::class, ExpenseEntity::class, FareEntity::class, WidgetStateEntity::class], version = 4, exportSchema = true)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun fareDao(): FareDao
    abstract fun widgetStateDao(): WidgetStateDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS widget_state (id INTEGER NOT NULL PRIMARY KEY, token TEXT NOT NULL, page INTEGER NOT NULL, lastTap INTEGER NOT NULL, lastServiceId INTEGER, lastAmount INTEGER NOT NULL)")
            }
        }
        private fun seedFares(db: SupportSQLiteDatabase) {
            listOf(500L, 600L, 700L, 1000L).forEachIndexed { index, amount ->
                db.execSQL("INSERT INTO fares (amountCents, position) VALUES (?, ?)", arrayOf<Any>(amount, index))
            }
        }
        val INITIAL_FARES = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { seedFares(db) }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS fares (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amountCents INTEGER NOT NULL, position INTEGER NOT NULL)")
                seedFares(db)
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, occurredAt INTEGER NOT NULL, category TEXT NOT NULL, amountCents INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_category_occurredAt ON expenses (category, occurredAt)")
            }
        }
        fun create(context: Context): TaxiDatabase = Room.databaseBuilder(
            context.applicationContext,
            TaxiDatabase::class.java,
            "mi_taxi.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).addCallback(INITIAL_FARES).build()
    }
}
