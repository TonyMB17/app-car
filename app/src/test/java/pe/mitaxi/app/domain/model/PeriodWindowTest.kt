package pe.mitaxi.app.domain.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodWindowTest {
    private val lima = ZoneId.of("America/Lima")
    @Test fun weekStartsMondayAcrossYearBoundary() {
        val week = PeriodWindow.at(SummaryPeriod.WEEK, Instant.parse("2027-01-01T15:00:00Z"), lima)!!
        assertEquals("2026-12-28", week.first.toString())
        assertEquals("2027-01-04", week.until.toString())
    }
    @Test fun monthChangesAtLocalMidnightAndSupportsLeapYear() {
        val before = PeriodWindow.at(SummaryPeriod.MONTH, Instant.parse("2028-03-01T04:59:59Z"), lima)!!
        val after = PeriodWindow.at(SummaryPeriod.MONTH, Instant.parse("2028-03-01T05:00:00Z"), lima)!!
        assertEquals("2028-02-01", before.first.toString())
        assertEquals("2028-03-01", after.first.toString())
        assertEquals(29 * 86_400_000L, before.end - before.start)
        assertEquals(before.end, after.start)
    }
    @Test fun weekHandlesDaylightSavingAndAllHasNoBounds() {
        val instant = Instant.parse("2026-03-08T12:00:00Z")
        val week = PeriodWindow.at(SummaryPeriod.WEEK, instant, ZoneId.of("America/New_York"))!!
        assertEquals(167 * 3_600_000L, week.end - week.start)
        assertNull(PeriodWindow.at(SummaryPeriod.ALL, instant, lima))
    }
}
