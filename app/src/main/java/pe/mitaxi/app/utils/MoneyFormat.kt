package pe.mitaxi.app.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun formatSoles(value: BigDecimal): String = "S/ " + NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(value)
