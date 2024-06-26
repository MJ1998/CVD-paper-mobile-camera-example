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

package com.google.android.sensing.model

import com.google.android.sensing.capture.CaptureRequest
import java.util.Date

/** Data class equivalent to [CaptureInfoEntity] for usage outside database. */
data class CaptureInfo(
  var captureId: String,
  val externalIdentifier: String,
  val captureRequest: CaptureRequest,
  val captureFolder: String,
  var captureTime: Date? = null,
  val recapture: Boolean? = false,
  val resourceInfoList: List<ResourceInfo> = emptyList()
)
