package com.westcounty.micemice.data.remote

data class SyncEventPayload(
    val id: String,
    val type: String,
    val payload: String,
    val createdAtMillis: Long,
)

data class SyncUploadRequest(
    val events: List<SyncEventPayload>,
)

data class ApiResponse(
    val code: Int,
    val message: String,
)
