package com.westcounty.micemice.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1")
    suspend fun getState(): AppStateEntity?

    @Upsert
    suspend fun upsertState(entity: AppStateEntity)
}
