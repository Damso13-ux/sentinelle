package com.sentinelle.app.network

import com.google.gson.annotations.SerializedName

data class ReportRequest(
    val phone: String,
    @SerializedName("is_good") val isGood: Boolean,
)

data class ErrorResponse(
    val message: String? = null,
    val error: String? = null,
    val detail: String? = null,
    val errors: List<ErrorDetail>? = null,
) {
    data class ErrorDetail(
        val status: Int? = null,
        val title: String? = null,
        val detail: String? = null,
    )
}

data class ListSummary(
    val id: Long,
    val name: String,
    val description: String? = null,
    val license: String? = null,
    @SerializedName("is_enabled_by_default") val isEnabledByDefault: Boolean? = null,
    val priority: Int? = null,
    val channel: String? = null,
    val type: String? = null,
    @SerializedName("download_url") val downloadUrl: String,
    val version: String,
)

data class ListPatternInfo(
    val name: String,
    val pattern: String,
)
