package pe.mitaxi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "expenses", indices = [Index(value = ["category", "occurredAt"])])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Long,
    val category: String,
    val amountCents: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
