package pe.mitaxi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "services", indices = [Index("occurredAt")])
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Long,
    val amountCents: Long,
    val fareId: Long? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
