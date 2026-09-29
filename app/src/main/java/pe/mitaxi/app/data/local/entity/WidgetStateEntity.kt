package pe.mitaxi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_state")
data class WidgetStateEntity(@PrimaryKey val id: Int, val token: String, val page: Int = 0,
    val lastTap: Long = 0, val lastServiceId: Long? = null, val lastAmount: Long = 0)
