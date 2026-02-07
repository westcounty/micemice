package com.westcounty.micemice.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MicemiceDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
}
