package pe.mitaxi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fares")
data class FareEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val amountCents: Long, val position: Int)
