package com.melonet.app.core.network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ApiErrorDto?,
    @SerializedName("meta") val meta: MetaDto?
)

data class ApiErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String
)

data class MetaDto(
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("has_more") val hasMore: Boolean
)
