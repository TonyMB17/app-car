package pe.mitaxi.app.data.repository

import androidx.room.withTransaction
import java.time.Clock
import java.util.UUID
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.local.entity.ServiceEntity
import pe.mitaxi.app.data.local.entity.WidgetStateEntity

class WidgetRepository(private val db: TaxiDatabase, private val clock: Clock = Clock.systemUTC()) {
    suspend fun state(id: Int): WidgetStateEntity = db.withTransaction {
        db.widgetStateDao().find(id) ?: WidgetStateEntity(id, UUID.randomUUID().toString()).also { db.widgetStateDao().save(it) }
    }
    suspend fun register(id: Int, token: String, fareId: Long, amount: Long): Boolean = registerValue(id, token, amount, fareId)
    suspend fun registerOther(id: Int, token: String, amount: Long): Boolean = registerValue(id, token, amount, null)
    private suspend fun registerValue(id: Int, token: String, amount: Long, fareId: Long?): Boolean = db.withTransaction {
        val state = db.widgetStateDao().find(id) ?: return@withTransaction false
        if (state.token != token) return@withTransaction false
        val now = clock.millis()
        if (state.lastTap != 0L && now - state.lastTap in 0..599) return@withTransaction false
        require(amount > 0)
        if (fareId != null) check(checkNotNull(db.fareDao().find(fareId)).amountCents == amount)
        val serviceId = db.serviceDao().insert(ServiceEntity(occurredAt = now, amountCents = amount,
            fareId = fareId, createdAt = now, updatedAt = now))
        db.widgetStateDao().save(state.copy(token = UUID.randomUUID().toString(), lastTap = now,
            lastServiceId = serviceId, lastAmount = amount))
        true
    }
    suspend fun page(id: Int, token: String, direction: Int) = db.withTransaction {
        require(direction == -1 || direction == 1)
        val state = db.widgetStateDao().find(id) ?: return@withTransaction
        if (state.token != token) return@withTransaction
        val maxPage = ((db.fareDao().all().size - 1) / 4).coerceAtLeast(0)
        db.widgetStateDao().save(state.copy(page = (state.page.coerceIn(0, maxPage) + direction).coerceIn(0, maxPage), token = UUID.randomUUID().toString()))
    }
    suspend fun undo(id: Int, token: String): Boolean = db.withTransaction {
        val state = db.widgetStateDao().find(id) ?: return@withTransaction false
        if (state.token != token) return@withTransaction false
        val serviceId = state.lastServiceId ?: return@withTransaction false
        val service = checkNotNull(db.serviceDao().find(serviceId))
        check(service.amountCents == state.lastAmount && service.updatedAt == state.lastTap && service.occurredAt == state.lastTap)
        check(db.serviceDao().delete(serviceId) == 1)
        db.widgetStateDao().save(state.copy(token = UUID.randomUUID().toString(), lastServiceId = null, lastAmount = 0))
        true
    }
    suspend fun remove(id: Int) = db.widgetStateDao().delete(id)
}
