package pe.mitaxi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow
import pe.mitaxi.app.data.local.entity.ServiceEntity

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun find(id: Long): ServiceEntity?
    @Query("SELECT COUNT(*) FROM services")
    suspend fun count(): Long

    @Query("SELECT * FROM services ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<ServiceEntity>>

    @Query("SELECT amountCents FROM services WHERE occurredAt >= :start AND occurredAt < :end")
    fun observeAmounts(start: Long, end: Long): Flow<List<Long>>

    @Insert
    suspend fun insert(service: ServiceEntity): Long

    @Query("UPDATE services SET amountCents = :amountCents, note = :note, occurredAt = :occurredAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, amountCents: Long, note: String?, occurredAt: Long, updatedAt: Long): Int

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun delete(id: Long): Int
}
