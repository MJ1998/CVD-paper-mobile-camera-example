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

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit

/** Constant for the max number of retries in case of sync failure */
@PublishedApi internal const val MAX_RETRIES_ALLOWED = "max_retires"

val defaultRetryConfiguration =
  RetryConfiguration(BackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS), 3)

val defaultWorkManagerSyncConfiguration =
  WorkManagerSyncConfiguration(
    syncConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
    repeat = RepeatInterval(interval = 15, timeUnit = TimeUnit.MINUTES),
    retryConfiguration = defaultRetryConfiguration
  )

/** Configuration for period synchronisation */
class WorkManagerSyncConfiguration(
  /**
   * Constraints that specify the requirements needed before the synchronisation is triggered. E.g.
   * network type (Wifi, 3G etc), the device should be charging etc.
   */
  val syncConstraints: Constraints,

  /**
   * The interval at which the sync should be triggered in. It must be greater than or equal to
   * [androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]
   */
  val repeat: RepeatInterval,

  /** Configuration for synchronization retry */
  val retryConfiguration: RetryConfiguration? = null,
)

data class RepeatInterval(
  /** The interval at which the sync should be triggered in */
  val interval: Long,
  /** The time unit for the repeat interval */
  val timeUnit: TimeUnit,
)

/** Configuration for synchronization retry */
data class RetryConfiguration(
  /**
   * The criteria to retry failed synchronization work based on
   * [androidx.work.WorkRequest.Builder.setBackoffCriteria]
   */
  val backoffCriteria: BackoffCriteria,

  /** Maximum retries for a failing [SensorDataUploadWorker] */
  val maxRetries: Int,
)

/**
 * The criteria for [SensorDataUploadWorker] failure retry based on
 * [androidx.work.WorkRequest.Builder.setBackoffCriteria]
 */
data class BackoffCriteria(
  /** Backoff policy [androidx.work.BackoffPolicy] */
  val backoffPolicy: BackoffPolicy,

  /**
   * Backoff delay for each retry attempt. Check
   * [androidx.work.PeriodicWorkRequest.MIN_BACKOFF_MILLIS] and
   * [androidx.work.PeriodicWorkRequest.MAX_BACKOFF_MILLIS] for the min-max supported values
   */
  val backoffDelay: Long,

  /** The time unit for [backoffDelay] */
  val timeUnit: TimeUnit,
)
