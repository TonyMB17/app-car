package pe.mitaxi.app.data.repository

import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.mitaxi.app.data.local.dao.ServiceDao
import pe.mitaxi.app.data.local.entity.ServiceEntity
import pe.mitaxi.app.domain.model.TaxiService

class ServiceRepository(private val dao: ServiceDao, private val clock: Clock = Clock.systemUTC()) {
    val services: Flow<List<TaxiService>> = dao.observeAll().map { rows ->
        rows.map { TaxiService(it.id, it.occurredAt, it.amountCents, it.note) }
    }

    suspend fun add(amountCents: Long, note: String): Long {
        require(amountCents > 0)
        val now = clock.millis()
        return dao.insert(ServiceEntity(
            occurredAt = now,
            amountCents = amountCents,
            note = note.trim().ifEmpty { null },
            createdAt = now,
            updatedAt = now,
        ))
    }

    suspend fun update(id: Long, amountCents: Long, note: String, occurredAt: Long) {
        require(amountCents > 0)
        check(dao.update(id, amountCents, note.trim().ifEmpty { null }, occurredAt, clock.millis()) == 1)
    }

    suspend fun delete(id: Long) {
        check(dao.delete(id) == 1)
    }
}
