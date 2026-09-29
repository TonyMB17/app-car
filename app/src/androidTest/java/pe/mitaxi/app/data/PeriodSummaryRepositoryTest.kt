package pe.mitaxi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.local.entity.ServiceEntity
import pe.mitaxi.app.data.local.entity.ExpenseEntity
import pe.mitaxi.app.data.repository.PeriodSummaryRepository
import pe.mitaxi.app.domain.model.SummaryPeriod

class PeriodSummaryRepositoryTest {
    @Test fun filtersAllPeriodsWithInclusiveStartExclusiveEndAndExactProfit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), TaxiDatabase::class.java).build()
        try {
            val zone = ZoneId.of("America/Lima")
            val clock = Clock.fixed(Instant.parse("2026-09-29T15:00:00Z"), zone)
            for (date in listOf("2026-08-31", "2026-09-01", "2026-09-28", "2026-09-29", "2026-09-30", "2026-10-01", "2026-10-05")) {
                val time = java.time.LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
                db.serviceDao().insert(ServiceEntity(occurredAt = time, amountCents = 1000, createdAt = time, updatedAt = time))
                db.expenseDao().insert(ExpenseEntity(occurredAt = time, category = "WASH", amountCents = 250, createdAt = time, updatedAt = time))
            }
            val repo = PeriodSummaryRepository(db.serviceDao(), db.expenseDao(), clock) { zone }
            for ((period, count) in listOf(SummaryPeriod.TODAY to 1, SummaryPeriod.WEEK to 4, SummaryPeriod.MONTH to 4, SummaryPeriod.ALL to 7)) {
                val result = repo.observe(period).first()
                assertEquals(count, result.count)
                assertEquals(0, result.income.compareTo(BigDecimal.valueOf(count * 10L)))
                assertEquals(0, result.expenses.compareTo(BigDecimal("2.50") * BigDecimal(count)))
                assertEquals(0, result.profit.compareTo(BigDecimal("7.50") * BigDecimal(count)))
            }
        } finally { db.close() }
    }
}
