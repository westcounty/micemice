package com.westcounty.micemice

import android.app.Application
import com.westcounty.micemice.data.sync.SyncWorker
import com.westcounty.micemice.di.RepositoryProvider

class MicemiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RepositoryProvider.init(this)
        SyncWorker.enqueue(this)
    }
}
