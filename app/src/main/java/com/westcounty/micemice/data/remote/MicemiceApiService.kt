package com.westcounty.micemice.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface MicemiceApiService {
    @POST("api/v1/sync/events")
    suspend fun uploadSyncEvents(@Body body: SyncUploadRequest): ApiResponse
}
