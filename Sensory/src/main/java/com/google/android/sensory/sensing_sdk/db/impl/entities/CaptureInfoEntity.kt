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

package com.google.android.sensory.sensing_sdk.db.impl.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.sensory.sensing_sdk.model.CaptureType

@Entity(
  indices =
    [
      Index(value = ["captureId"], unique = true),
      Index(value = ["captureFolder"], unique = true),
    ]
)
internal data class CaptureInfoEntity(
  @PrimaryKey(autoGenerate = true) val id: Long,
  val participantId: String,
  val captureType: CaptureType,
  val captureFolder: String,
  val captureId: String,
)
