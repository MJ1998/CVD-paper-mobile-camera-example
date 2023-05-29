/*
 * Copyright 2022 Google LLC
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

package com.google.android.sensory.sensing_sdk.db.impl

import android.content.Context
import androidx.room.Room
import com.google.android.sensory.sensing_sdk.db.Database
import com.google.android.sensory.sensing_sdk.model.CaptureInfo
import com.google.android.sensory.sensing_sdk.model.RequestStatus
import com.google.android.sensory.sensing_sdk.model.ResourceInfo
import com.google.android.sensory.sensing_sdk.model.UploadRequest
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

internal class DatabaseImpl(context: Context, enableEncryption: Boolean) : Database {
  val db: ResourceDatabase =
    Room.databaseBuilder(context, ResourceDatabase::class.java, ENCRYPTED_DATABASE_NAME)
      .apply {
        if (enableEncryption) {
          openHelperFactory(SupportFactory(SQLiteDatabase.getBytes("PassPhrase".toCharArray())))
        }
      }
      .build()

  private val captureInfoDao = db.captureInfoDao()
  private val resourceInfoDao = db.resourceInfoDao()
  private val uploadRequestDao = db.uploadRequestDao()

  override suspend fun addCaptureInfo(captureInfo: CaptureInfo): String {
    return captureInfoDao.insertCaptureInfo(captureInfo)
  }

  override suspend fun addResourceInfo(resourceInfo: ResourceInfo): String {
    return resourceInfoDao.insertResourceInfo(resourceInfo)
  }

  override suspend fun addUploadRequest(uploadRequest: UploadRequest): String {
    return uploadRequestDao.insertUploadRequest(uploadRequest)
  }

  override suspend fun listResourceInfoForParticipant(participantId: String): List<ResourceInfo> {
    return resourceInfoDao.listResourceInfoForParticipant(participantId)
  }

  override suspend fun listResourceInfoInCapture(captureId: String): List<ResourceInfo> {
    return resourceInfoDao.listResourceInfoInCapture(captureId)
  }

  override suspend fun listUploadRequests(status: RequestStatus): List<UploadRequest> {
    return uploadRequestDao.listUploadRequests(status)
  }

  override suspend fun updateUploadRequest(uploadRequest: UploadRequest) {
    uploadRequestDao.updateUploadRequest(uploadRequest)
  }

  override suspend fun updateResourceInfo(resourceInfo: ResourceInfo) {
    resourceInfoDao.updateResourceInfo(resourceInfo)
  }

  override suspend fun getResourceInfo(resourceInfoId: String): ResourceInfo {
    return resourceInfoDao.getResourceInfo(resourceInfoId)
  }

  companion object {
    const val ENCRYPTED_DATABASE_NAME = "sensor_resources_encrypted.db"
  }
}
