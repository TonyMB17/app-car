package pe.mitaxi.app.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

object ServiceInput {
    private val amountPattern = Regex("[0-9]+([.,][0-9]{1,2})?")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun amountCents(text: String): Long? {
        val normalized = text.trim()
        if (!amountPattern.matches(normalized)) return null
        return try {
            BigDecimal(normalized.replace(',', '.')).movePointRight(2).longValueExact()
                .takeIf { it > 0 }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun editableAmount(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()

    fun date(epochMillis: Long, zone: ZoneId): String = Instant.ofEpochMilli(epochMillis)
        .atZone(zone).format(DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT))

    fun time(epochMillis: Long, zone: ZoneId): String = Instant.ofEpochMilli(epochMillis)
        .atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))

    fun occurredAt(date: String, time: String, zone: ZoneId): Long? = try {
        val local = LocalDateTime.parse("${date.trim()} ${time.trim()}", dateTimeFormat)
        // Reject nonexistent local times during a daylight-saving transition.
        if (zone.rules.getValidOffsets(local).isEmpty()) null else local.atZone(zone).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
