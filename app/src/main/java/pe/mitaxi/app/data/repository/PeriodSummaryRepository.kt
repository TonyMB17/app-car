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
import kotlinx.coroutines.flow.map
import pe.mitaxi.app.data.local.dao.ExpenseDao
import pe.mitaxi.app.data.local.dao.ServiceDao
import pe.mitaxi.app.domain.model.PeriodWindow
import pe.mitaxi.app.domain.model.SummaryPeriod

data class PeriodSummary(val window: PeriodWindow?, val count: Int, val income: BigDecimal, val expenses: BigDecimal) {
    val profit: BigDecimal get() = income - expenses
}

class PeriodSummaryRepository(
    private val services: ServiceDao, private val expenses: ExpenseDao,
    private val clock: Clock = Clock.systemUTC(), private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private fun windows(period: SummaryPeriod): Flow<PeriodWindow?> = flow {
        while (true) {
            val window = PeriodWindow.at(period, clock.instant(), zone())
            emit(window)
            delay(window?.let { (it.end - clock.millis()).coerceIn(1, 60_000) } ?: 60_000)
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(period: SummaryPeriod): Flow<PeriodSummary> = windows(period).flatMapLatest { window ->
        val incomes = if (window == null) services.observeAll().map { rows -> rows.map { it.amountCents } }
            else services.observeAmounts(window.start, window.end)
        val costs = if (window == null) expenses.observeAll() else expenses.observeDay(window.start, window.end)
        combine(incomes, costs) { amounts, rows ->
            PeriodSummary(window, amounts.size, total(amounts), total(rows.map { it.amountCents }))
        }
    }

    private fun total(amounts: List<Long>) = amounts.fold(BigDecimal.ZERO) { sum, cents -> sum + BigDecimal.valueOf(cents, 2) }
}
