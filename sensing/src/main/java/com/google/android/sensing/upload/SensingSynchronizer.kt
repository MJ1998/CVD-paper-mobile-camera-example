/*
 * Copyright 2023-2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.sensing.upload

import android.content.Context
import com.google.android.sensing.SensingSyncDbInteractor
import com.google.android.sensing.model.UploadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold

internal class SensingSynchronizer(
  private val uploader: Uploader,
  private val sensingSyncDbInteractor: SensingSyncDbInteractor
) {

  /**
   * Sync Upload API. It fetches [UploadRequest]s using [uploadRequestFetcher], invokes [uploader],
   * collects [UploadResult]s and processes them using [uploadResultProcessor]. At each stage it
   * emits [SyncUploadState]s.
   */
  suspend fun synchronize(): Flow<SyncUploadState> =
    flow {
        var uploadRequestList = sensingSyncDbInteractor.fetchAll()
        emit(SyncUploadState.Started(initialTotalRequests = uploadRequestList.size))
        var completedRequests = 0
        var totalRequests = 0

        while (uploadRequestList.isNotEmpty()) {
          totalRequests += uploadRequestList.size
          uploadRequestList.forEach { uploadRequest ->
            // Following is to bootstrap new state calculation based on previous "InProgress" states
            val initialSyncUploadState =
              SyncUploadState.InProgress(
                currentTotalRequests = totalRequests,
                completedRequests = completedRequests
              )

            // upload() is a cold flow with finite emitted values. Hence it ends automatically.
            uploader
              .upload(uploadRequest)
              .onEach {
                sensingSyncDbInteractor.processUploadResult(it)
                if (it is UploadResult.Completed) completedRequests++
              }
              .runningFold(initialSyncUploadState, ::calculateSyncUploadState)
              // initialSyncUploadState is dropped
              .drop(1)
              .collect(::emit)
          }
          uploadRequestList = sensingSyncDbInteractor.fetchNew()
        }
        emit(SyncUploadState.Completed(completedRequests))
      }
      .flowOn(Dispatchers.IO)

  private fun calculateSyncUploadState(
    lastSyncUploadState: SyncUploadState,
    uploadResult: UploadResult
  ): SyncUploadState {
    /**
     * The last state can be assumed as [SyncUploadState.InProgress] as that's the initial state we
     * use to bootstrap the state calculation process.
     */
    with(lastSyncUploadState as SyncUploadState.InProgress) {
      return when (uploadResult) {
        is UploadResult.Started -> {
          SyncUploadState.InProgress(
            currentTotalRequests = currentTotalRequests,
            completedRequests = completedRequests,
            currentRequestTotalBytes = uploadResult.uploadRequest.fileSize,
            currentRequestCompletedBytes = 0
          )
        }
        is UploadResult.Success ->
          SyncUploadState.InProgress(
            currentTotalRequests = currentTotalRequests,
            completedRequests = completedRequests,
            currentRequestTotalBytes = currentRequestTotalBytes,
            currentRequestCompletedBytes = currentRequestCompletedBytes + uploadResult.bytesUploaded
          )
        is UploadResult.Completed ->
          if (completedRequests + 1 == currentTotalRequests)
            SyncUploadState.Completed(currentTotalRequests)
          else
            SyncUploadState.InProgress(
              currentTotalRequests = currentTotalRequests,
              completedRequests = completedRequests + 1,
              currentRequestTotalBytes = currentRequestTotalBytes,
              currentRequestCompletedBytes = currentRequestCompletedBytes
            )
        is UploadResult.Failure ->
          SyncUploadState.Failed(
            resourceInfoId = uploadResult.uploadRequest.resourceInfoId,
            exceptionMessage = uploadResult.uploadError.message ?: "Unknown Error",
            stacktrace = uploadResult.uploadError.stackTraceToString()
          )
      }
    }
  }

  companion object {
    @Volatile private var instance: SensingSynchronizer? = null
    fun getInstance(context: Context) =
      instance
        ?: synchronized(this) {
          instance
            ?: run {
              Uploader.getInstance(context)?.let {
                SensingSynchronizer(
                    uploader = it,
                    sensingSyncDbInteractor = SensingSyncDbInteractor.getInstance(context)
                  )
                  .also { instance = it }
              }
            }
        }
  }
}
