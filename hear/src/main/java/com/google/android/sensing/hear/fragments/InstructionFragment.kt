/*
 * Copyright 2024 Google LLC
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

package com.google.android.sensing.hear.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.sensing.hear.MainActivity
import com.google.android.sensing.hear.MainActivityViewModel
import com.google.android.sensing.hear.R
import com.google.android.sensing.upload.SyncUploadState
import kotlinx.coroutines.launch
import timber.log.Timber

/** Fragment for the component list. */
class InstructionFragment : Fragment(R.layout.fragment_instruction) {

  private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    view.findViewById<Button>(R.id.button_get_started).setOnClickListener {
      (requireActivity() as MainActivity).run {
        if (this.checkAllPermissions()) {
          findNavController().navigate(R.id.action_instructionFragment_to_recordingFragment)
        } else {
          lifecycleScope.launch {
            mainActivityViewModel.permissionsAvailable.collect {
              if (it) {
                findNavController().navigate(R.id.action_instructionFragment_to_recordingFragment)
              }
            }
          }
        }
      }
    }

    view.findViewById<Button>(R.id.button_sync).setOnClickListener {
      mainActivityViewModel.triggerOneTimeSync()
    }

    setupSyncUploadProgress()
  }

  private fun setupSyncUploadProgress() {
    lifecycleScope.launch {
      mainActivityViewModel.syncUploadState.collect {
        when (it) {
          is SyncUploadState.Started,
          is SyncUploadState.InProgress ->
            Toast.makeText(requireContext(), "Sync In Progress", Toast.LENGTH_LONG).show()
          is SyncUploadState.Completed ->
            Toast.makeText(requireContext(), "Sync Finished", Toast.LENGTH_LONG).show()
          is SyncUploadState.Failed -> {
            Timber.w(
              "DEBUG Sync Failed. \nMessage = ${it.exceptionMessage}\nStackTrace = ${it.stacktrace}"
            )
            Toast.makeText(requireContext(), "Sync Failed.", Toast.LENGTH_LONG).show()
          }
          is SyncUploadState.NoOp -> {
            Timber.i("No operation required. Sync is already in progress.")
          }
        }
      }
    }
  }
}
