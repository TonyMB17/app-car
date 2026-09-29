package pe.mitaxi.app.domain.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DayWindowTest {
    @Test fun usesLocalDayRatherThanUtcDate() {
        val day = DayWindow.at(Instant.parse("2026-09-29T04:59:59Z"), ZoneId.of("America/Lima"))
        assertEquals("2026-09-28", day.date.toString())
        assertEquals(Instant.parse("2026-09-28T05:00:00Z").toEpochMilli(), day.start)
        assertEquals(Instant.parse("2026-09-29T05:00:00Z").toEpochMilli(), day.end)
    }

    @Test fun changesDayAtLocalMidnightAcrossYearBoundary() {
        val zone = ZoneId.of("America/Lima")
        val before = DayWindow.at(Instant.parse("2027-01-01T04:59:59.999Z"), zone)
        val after = DayWindow.at(Instant.parse("2027-01-01T05:00:00Z"), zone)
        assertEquals("2026-12-31", before.date.toString())
        assertEquals("2027-01-01", after.date.toString())
        assertEquals(before.end, after.start)
    }

    @Test fun handlesDaysThatAreNot24Hours() {
        val zone = ZoneId.of("America/New_York")
        val spring = DayWindow.at(Instant.parse("2026-03-08T12:00:00Z"), zone)
        val autumn = DayWindow.at(Instant.parse("2026-11-01T12:00:00Z"), zone)
        assertEquals(23 * 3_600_000L, spring.end - spring.start)
        assertEquals(25 * 3_600_000L, autumn.end - autumn.start)
    }
}
