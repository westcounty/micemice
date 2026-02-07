package com.westcounty.micemice.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 1,
    val payload: String,
    val updatedAtMillis: Long,
)
