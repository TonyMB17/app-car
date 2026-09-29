package pe.mitaxi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pe.mitaxi.app.data.local.entity.ExpenseEntity

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE occurredAt >= :start AND occurredAt < :end")
    fun observeDay(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET category = :category, amountCents = :amountCents, occurredAt = :occurredAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, category: String, amountCents: Long, occurredAt: Long, updatedAt: Long): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Insert suspend fun insert(expense: ExpenseEntity): Long

    @Query("SELECT amountCents FROM expenses WHERE category = 'FUEL' AND occurredAt >= :start AND occurredAt < :end")
    fun observeFuelAmounts(start: Long, end: Long): Flow<List<Long>>

    @Query("DELETE FROM expenses WHERE id = :id AND category = 'FUEL'")
    suspend fun deleteFuel(id: Long): Int
}
