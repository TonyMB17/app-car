package pe.mitaxi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.mitaxi.app.data.local.entity.FareEntity

@Dao
interface FareDao {
    @Query("SELECT * FROM fares ORDER BY position, id") fun observe(): Flow<List<FareEntity>>
    @Query("SELECT * FROM fares ORDER BY position, id") suspend fun all(): List<FareEntity>
    @Query("SELECT * FROM fares WHERE id = :id") suspend fun find(id: Long): FareEntity?
    @Insert suspend fun insert(fare: FareEntity): Long
    @Query("UPDATE fares SET amountCents = :amount WHERE id = :id") suspend fun update(id: Long, amount: Long): Int
    @Query("UPDATE fares SET position = :position WHERE id = :id") suspend fun position(id: Long, position: Int)
    @Query("DELETE FROM fares WHERE id = :id") suspend fun delete(id: Long): Int
}
