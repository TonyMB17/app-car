package pe.mitaxi.app.domain.model

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceInputTest {
    @Test fun parsesExactCentsWithEitherDecimalSeparator() {
        assertEquals(500L, ServiceInput.amountCents("5"))
        assertEquals(710L, ServiceInput.amountCents(" 7,10 "))
        assertEquals(1001L, ServiceInput.amountCents("10.01"))
        assertEquals(1L, ServiceInput.amountCents("0.01"))
    }

    @Test fun rejectsInvalidAndAmbiguousAmounts() {
        listOf("", " ", "0", "0.00", "-5", "+5", "1.234", "1,234", "1,234.56", "1e2", "NaN", "1 000", "5.")
            .forEach { assertNull("Accepted invalid amount: $it", ServiceInput.amountCents(it)) }
    }

    @Test fun handlesLongBoundaryWithoutRoundingOrOverflow() {
        assertEquals(Long.MAX_VALUE, ServiceInput.amountCents("92233720368547758.07"))
        assertNull(ServiceInput.amountCents("92233720368547758.08"))
        assertNull(ServiceInput.amountCents("99999999999999999999999999999999"))
        assertEquals("92233720368547758.07", ServiceInput.editableAmount(Long.MAX_VALUE))
    }

    @Test fun convertsLocalTimeUsingDeviceZone() {
        val zone = ZoneId.of("America/Lima")
        val instant = Instant.parse("2026-09-29T12:32:00Z").toEpochMilli()
        assertEquals(instant, ServiceInput.occurredAt("29/09/2026", "07:32", zone))
        assertEquals("29/09/2026", ServiceInput.date(instant, zone))
        assertEquals("07:32", ServiceInput.time(instant, zone))
    }

    @Test fun rejectsImpossibleDatesAndTimes() {
        val zone = ZoneId.of("America/Lima")
        assertNull(ServiceInput.occurredAt("31/02/2026", "12:00", zone))
        assertNull(ServiceInput.occurredAt("29/02/2025", "12:00", zone))
        assertNull(ServiceInput.occurredAt("29/09/2026", "24:00", zone))
        assertNull(ServiceInput.occurredAt("29/09/2026", "12:60", zone))
        assertNull(ServiceInput.occurredAt("", "", zone))
    }

    @Test fun acceptsLeapDayAndRejectsDstGap() {
        assertEquals(Instant.parse("2024-02-29T17:00:00Z").toEpochMilli(),
            ServiceInput.occurredAt("29/02/2024", "12:00", ZoneId.of("America/Lima")))
        assertNull(ServiceInput.occurredAt("08/03/2026", "02:30", ZoneId.of("America/New_York")))
    }
}
