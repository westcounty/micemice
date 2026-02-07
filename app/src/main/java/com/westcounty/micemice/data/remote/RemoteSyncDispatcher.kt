package com.westcounty.micemice.data.remote

import com.westcounty.micemice.data.model.SyncEvent

object RemoteSyncConfig {
    const val ENABLED: Boolean = false
}

class RemoteSyncDispatcher(
    private val service: MicemiceApiService,
) {
    suspend fun upload(events: List<SyncEvent>): Boolean {
        if (!RemoteSyncConfig.ENABLED) return true
        return runCatching {
            val payload = SyncUploadRequest(
                events = events.map {
                    SyncEventPayload(
                        id = it.id,
                        type = it.eventType,
                        payload = it.payloadSummary,
                        createdAtMillis = it.createdAtMillis,
                    )
                }
            )
            val resp = service.uploadSyncEvents(payload)
            resp.code == 0
        }.getOrDefault(false)
    }
}
