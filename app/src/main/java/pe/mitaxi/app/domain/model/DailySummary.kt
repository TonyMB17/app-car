package pe.mitaxi.app.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayWindow(val date: LocalDate, val start: Long, val end: Long) {
    companion object {
        fun at(instant: Instant, zone: ZoneId): DayWindow {
            val date = instant.atZone(zone).toLocalDate()
            return DayWindow(date, date.atStartOfDay(zone).toInstant().toEpochMilli(),
                date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        }
    }
}

data class DailySummary(val date: LocalDate, val serviceCount: Int, val income: BigDecimal, val fuel: BigDecimal,
    val expenses: BigDecimal) {
    val profit: BigDecimal get() = income - expenses
}
