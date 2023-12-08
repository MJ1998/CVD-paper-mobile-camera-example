/*
 * Copyright 2023 Google LLC
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
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.hasKeyWithValueOfType
import com.google.android.sensing.OffsetDateTimeTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import java.time.OffsetDateTime

object SensingUploadSync {

  val gson: Gson =
    GsonBuilder()
      .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeTypeAdapter().nullSafe())
      .create()

  private val uniqueWorkName = "Unique_" + SensorDataUploadWorker::class.java.name
  private val periodicWorkName = "Periodic_" + SensorDataUploadWorker::class.java.name

  fun getSyncUploadProgressFlow(context: Context): Flow<SyncUploadProgress> {
    val oneTimeSyncFlow = WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkLiveData(uniqueWorkName).asFlow().flatMapConcat { it.asFlow() }

    val periodicSyncFlow = WorkManager.getInstance(context)
      .getWorkInfosForUniqueWorkLiveData(periodicWorkName).asFlow().flatMapConcat { it.asFlow() }

    return flowOf(oneTimeSyncFlow, periodicSyncFlow).flattenMerge().mapNotNull { workInfo ->
      workInfo.progress
        .takeIf { it.keyValueMap.isNotEmpty() && it.hasKeyWithValueOfType<String>("ProgressType") }
        ?.let {
          val state = it.getString("ProgressType")!!
          val stateData = it.getString("Progress")
          gson.fromJson(stateData, Class.forName(state)) as SyncUploadProgress
        }
    }
  }

  fun oneTimeSyncUpload(
    context: Context,
    retryConfiguration: RetryConfiguration = defaultRetryConfiguration,
  ) {
    WorkManager.getInstance(context)
      .enqueueUniqueWork(
        uniqueWorkName,
        ExistingWorkPolicy.KEEP,
        createOneTimeWorkRequest(retryConfiguration)
      )
  }

  @PublishedApi
  internal fun createOneTimeWorkRequest(
    retryConfiguration: RetryConfiguration?
  ): OneTimeWorkRequest {
    val oneTimeWorkRequestBuilder = OneTimeWorkRequest.Builder(SensorDataUploadWorker::class.java)
    retryConfiguration?.let {
      oneTimeWorkRequestBuilder.setBackoffCriteria(
        it.backoffCriteria.backoffPolicy,
        it.backoffCriteria.backoffDelay,
        it.backoffCriteria.timeUnit
      )
      oneTimeWorkRequestBuilder.setInputData(
        Data.Builder().putInt(MAX_RETRIES_ALLOWED, it.maxRetries).build()
      )
    }
    return oneTimeWorkRequestBuilder.build()
  }

  fun periodicSyncUpload(
    context: Context,
    periodicSyncConfiguration: PeriodicSyncConfiguration = defaultPeriodicSyncConfiguration,
  ) {
    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        periodicWorkName,
        ExistingPeriodicWorkPolicy.KEEP,
        createPeriodicWorkRequest(periodicSyncConfiguration)
      )
  }

  @PublishedApi
  internal fun createPeriodicWorkRequest(
    periodicSyncConfiguration: PeriodicSyncConfiguration
  ): PeriodicWorkRequest {
    val periodicWorkRequestBuilder =
      PeriodicWorkRequest.Builder(
          SensorDataUploadWorker::class.java,
          periodicSyncConfiguration.repeat.interval,
          periodicSyncConfiguration.repeat.timeUnit
        )
        .setConstraints(periodicSyncConfiguration.syncConstraints)

    periodicSyncConfiguration.retryConfiguration?.let {
      periodicWorkRequestBuilder.setBackoffCriteria(
        it.backoffCriteria.backoffPolicy,
        it.backoffCriteria.backoffDelay,
        it.backoffCriteria.timeUnit
      )
      periodicWorkRequestBuilder.setInputData(
        Data.Builder().putInt(MAX_RETRIES_ALLOWED, it.maxRetries).build()
      )
    }
    return periodicWorkRequestBuilder.build()
  }
}
