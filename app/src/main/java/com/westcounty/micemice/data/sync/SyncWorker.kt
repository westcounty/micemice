package com.westcounty.micemice.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.westcounty.micemice.data.model.RepoResult
import com.westcounty.micemice.data.model.SyncStatus
import com.westcounty.micemice.data.remote.NetworkModule
import com.westcounty.micemice.data.remote.RemoteSyncDispatcher
import com.westcounty.micemice.di.RepositoryProvider
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        RepositoryProvider.init(applicationContext)
        val repo = RepositoryProvider.get()
        val pendingEvents = repo.snapshot.value.syncEvents.filter {
            it.status == SyncStatus.Pending || it.status == SyncStatus.Failed
        }
        if (pendingEvents.isNotEmpty()) {
            val dispatcher = RemoteSyncDispatcher(NetworkModule.apiService)
            val remoteOk = dispatcher.upload(pendingEvents)
            if (!remoteOk) return Result.retry()
        }
        return when (repo.syncPendingEvents("worker")) {
            RepoResult.Success -> Result.success()
            is RepoResult.Error -> Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "micemice_periodic_sync"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
