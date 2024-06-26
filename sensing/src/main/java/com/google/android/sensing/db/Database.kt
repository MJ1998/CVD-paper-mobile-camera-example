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

package com.google.android.sensing.db

import android.content.Context
import com.google.android.sensing.DatabaseConfiguration
import com.google.android.sensing.db.impl.DatabaseImpl
import com.google.android.sensing.model.CaptureInfo
import com.google.android.sensing.model.RequestStatus
import com.google.android.sensing.model.ResourceInfo
import com.google.android.sensing.model.UploadRequest

/** The interface for the sensor resources database. */
internal interface Database {
  suspend fun addCaptureInfo(captureInfo: CaptureInfo): String
  suspend fun addResourceInfo(resourceInfo: ResourceInfo): String
  suspend fun addUploadRequest(uploadRequest: UploadRequest): String
  suspend fun listResourceInfoForExternalIdentifier(externalIdentifier: String): List<ResourceInfo>
  suspend fun listUploadRequests(status: RequestStatus): List<UploadRequest>
  suspend fun updateUploadRequest(uploadRequest: UploadRequest)
  suspend fun updateResourceInfo(resourceInfo: ResourceInfo)
  suspend fun getResourceInfo(resourceInfoId: String): ResourceInfo
  suspend fun getCaptureInfo(captureId: String): CaptureInfo
  suspend fun deleteRecordsInCapture(captureId: String): Boolean
  fun close()

  companion object {
    @Volatile private var instance: Database? = null
    fun getInstance(context: Context, databaseConfig: DatabaseConfiguration) =
      instance
        ?: synchronized(this) {
          instance ?: DatabaseImpl(context, databaseConfig).also { instance = it }
        }

    fun cleanup() {
      instance?.close()
      instance = null
    }
  }
}
