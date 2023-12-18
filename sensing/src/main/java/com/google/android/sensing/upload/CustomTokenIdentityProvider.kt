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

import com.google.android.sensing.TokenAuthenticator
import io.minio.credentials.AssumeRoleBaseProvider
import io.minio.credentials.Credentials
import java.util.Objects
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

/**
 * Credential Provider using AssumeRoleWithCustomToken API. Extends and implements
 * AssumeRoleBaseProvider.
 */
class CustomTokenIdentityProvider(
  okHttpClient: OkHttpClient?,
  private val authenticator: TokenAuthenticator,
  private val stsEndpoint: String
) : AssumeRoleBaseProvider(okHttpClient) {

  init {
    Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty")
  }

  /** This API is called whenever new temporary credentials are needed. */
  override fun getRequest(): Request {
    val stsHttpUrl = Objects.requireNonNull(stsEndpoint.toHttpUrl(), "Invalid STS endpoint")
    val url =
      newUrlBuilder(
          stsHttpUrl,
          "AssumeRoleWithCustomToken",
          getValidDurationSeconds(authenticator.getDurationSeconds()),
          null,
          authenticator.getRoleArn(),
          null
        )
        .addQueryParameter("Token", authenticator.getToken())
        .build()

    val emptyRequestBody =
      object : RequestBody() {
        override fun contentType(): MediaType? {
          return "application/octet-stream".toMediaTypeOrNull()
        }

        override fun writeTo(sink: BufferedSink) {}
      }

    return Request.Builder().url(url).method("POST", emptyRequestBody).build()
  }

  override fun getResponseClass(): Class<out Response> {
    return CustomTokenIdentityResponse::class.java
  }

  /** Object representation of response XML of AssumeRoleWithCustomToken API. */
  @Root(name = "AssumeRoleWithLDAPIdentityResponse", strict = false)
  @Namespace(
    reference =
      "https://min.io/docs/minio/linux/developers/security-token-service/AssumeRoleWithCustomToken.html#id3"
  )
  class CustomTokenIdentityResponse : Response {
    @Path(value = "AssumeRoleWithCustomTokenResult")
    @Element(name = "Credentials")
    private val credentials: Credentials? = null
    override fun getCredentials(): Credentials {
      return credentials!!
    }
  }
}
