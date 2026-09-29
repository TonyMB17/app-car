package pe.mitaxi.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class SummaryPeriod { TODAY, WEEK, MONTH, ALL }

data class PeriodWindow(val first: LocalDate, val until: LocalDate, val start: Long, val end: Long) {
    companion object {
        fun at(period: SummaryPeriod, instant: Instant, zone: ZoneId): PeriodWindow? {
            val today = instant.atZone(zone).toLocalDate()
            val first = when (period) {
                SummaryPeriod.TODAY -> today
                SummaryPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                SummaryPeriod.MONTH -> today.withDayOfMonth(1)
                SummaryPeriod.ALL -> return null
            }
            val until = when (period) {
                SummaryPeriod.WEEK -> first.plusWeeks(1)
                SummaryPeriod.MONTH -> first.plusMonths(1)
                else -> first.plusDays(1)
            }
            return PeriodWindow(first, until, first.atStartOfDay(zone).toInstant().toEpochMilli(),
                until.atStartOfDay(zone).toInstant().toEpochMilli())
        }
    }
}
