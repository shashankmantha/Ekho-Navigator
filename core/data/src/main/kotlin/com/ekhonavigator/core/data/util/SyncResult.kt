package com.ekhonavigator.core.data.util

sealed interface SyncResult {
    data class Success(val eventsUpdated: Int) : SyncResult
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult
}
