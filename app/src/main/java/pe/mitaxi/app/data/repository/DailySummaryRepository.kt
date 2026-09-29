package pe.mitaxi.app.data.repository

import java.math.BigDecimal
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import pe.mitaxi.app.data.local.dao.ExpenseDao
import pe.mitaxi.app.data.local.dao.ServiceDao
import pe.mitaxi.app.domain.model.ExpenseCategory
import pe.mitaxi.app.domain.model.DailySummary
import pe.mitaxi.app.domain.model.DayWindow

class DailySummaryRepository(
    private val services: ServiceDao,
    private val expenses: ExpenseDao,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    // Wake at midnight; also recheck clock/timezone at most once per minute.
    private fun currentDays(): Flow<DayWindow> = flow {
        while (true) {
            val day = DayWindow.at(clock.instant(), zone())
            emit(day)
            delay((day.end - clock.millis()).coerceIn(1, 60_000))
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(days: Flow<DayWindow> = currentDays()): Flow<DailySummary> = days.flatMapLatest { day ->
        combine(services.observeAmounts(day.start, day.end), expenses.observeDay(day.start, day.end)) { income, costs ->
            DailySummary(day.date, income.size, total(income),
                total(costs.filter { it.category == ExpenseCategory.FUEL.name }.map { it.amountCents }),
                total(costs.map { it.amountCents }))
        }
    }

    suspend fun addFuel(amountCents: Long): Long {
        return ExpenseRepository(expenses, clock).add(ExpenseCategory.FUEL, amountCents)
    }

    suspend fun undoFuel(id: Long) { check(expenses.deleteFuel(id) == 1) }

    private fun total(amounts: List<Long>): BigDecimal = amounts.fold(BigDecimal.ZERO) { total, cents ->
        total + BigDecimal.valueOf(cents, 2)
    }
}
