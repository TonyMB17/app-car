package pe.mitaxi.app.data.repository

import androidx.room.withTransaction
import pe.mitaxi.app.data.local.database.TaxiDatabase
import pe.mitaxi.app.data.local.entity.FareEntity

class FareRepository(private val database: TaxiDatabase) {
    private val dao = database.fareDao()
    val fares = dao.observe()
    suspend fun register(id: Long, expectedAmount: Long): Long = database.withTransaction {
        val fare = checkNotNull(dao.find(id))
        check(fare.amountCents == expectedAmount)
        val now = java.time.Instant.now().toEpochMilli()
        database.serviceDao().insert(pe.mitaxi.app.data.local.entity.ServiceEntity(
            occurredAt = now, amountCents = fare.amountCents, fareId = fare.id, createdAt = now, updatedAt = now))
    }

    suspend fun save(id: Long?, amount: Long) = database.withTransaction {
        require(amount > 0)
        val rows = dao.all()
        require(rows.none { it.id != id && it.amountCents == amount })
        if (id == null) dao.insert(FareEntity(amountCents = amount, position = rows.size))
        else { check(dao.update(id, amount) == 1); id }
    }
    suspend fun delete(id: Long) = database.withTransaction {
        check(dao.delete(id) == 1)
        dao.all().forEachIndexed { index, fare -> dao.position(fare.id, index) }
    }
    suspend fun move(id: Long, direction: Int) = database.withTransaction {
        require(direction == -1 || direction == 1)
        val rows = dao.all().toMutableList()
        val index = rows.indexOfFirst { it.id == id }
        check(index >= 0)
        val target = index + direction
        if (target in rows.indices) {
            rows.add(target, rows.removeAt(index))
            rows.forEachIndexed { position, fare -> dao.position(fare.id, position) }
        }
    }
}
