package pe.mitaxi.app.domain.model

data class TaxiService(
    val id: Long,
    val occurredAt: Long,
    val amountCents: Long,
    val note: String?,
)
