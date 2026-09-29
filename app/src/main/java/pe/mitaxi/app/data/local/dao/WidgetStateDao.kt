package pe.mitaxi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import pe.mitaxi.app.data.local.entity.WidgetStateEntity

@Dao
interface WidgetStateDao {
    @Query("SELECT * FROM widget_state WHERE id = :id") suspend fun find(id: Int): WidgetStateEntity?
    @Upsert suspend fun save(state: WidgetStateEntity)
    @Query("DELETE FROM widget_state WHERE id = :id") suspend fun delete(id: Int)
}
