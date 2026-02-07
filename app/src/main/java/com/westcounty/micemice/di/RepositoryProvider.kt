package com.westcounty.micemice.di

import android.content.Context
import androidx.room.Room
import com.westcounty.micemice.data.local.MicemiceDatabase
import com.westcounty.micemice.data.local.SnapshotStore
import com.westcounty.micemice.data.repository.InMemoryLabRepository
import com.westcounty.micemice.data.repository.LabRepository
import com.westcounty.micemice.data.repository.PersistentLabRepository
import kotlinx.coroutines.runBlocking

object RepositoryProvider {
    @Volatile
    private var repository: LabRepository? = null

    fun init(context: Context) {
        if (repository != null) return
        synchronized(this) {
            if (repository != null) return

            val appContext = context.applicationContext
            val db = Room.databaseBuilder(
                appContext,
                MicemiceDatabase::class.java,
                "micemice.db",
            ).fallbackToDestructiveMigration().build()

            val store = SnapshotStore(db.appStateDao())
            val persistedSnapshot = runBlocking { store.load() }
            val delegate = if (persistedSnapshot != null) {
                InMemoryLabRepository.fromSnapshot(persistedSnapshot)
            } else {
                InMemoryLabRepository.instance
            }

            val persistentRepo = PersistentLabRepository(delegate, store)
            runBlocking {
                store.save(delegate.snapshot.value)
            }
            repository = persistentRepo
        }
    }

    fun get(): LabRepository {
        return repository ?: error("RepositoryProvider not initialized")
    }
}
