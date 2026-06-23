package com.bpkpad.peminjaman.peminjaman.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveDocumentDto(
    val id: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("document_number") val documentNumber: String? = null,
    val title: String,
    val description: String? = null,
    val year: Int,
    val status: String,
    @SerialName("storage_locations") val storageLocation: StorageLocationDto? = null
)

@Serializable
data class StorageLocationDto(
    val room: String,
    val shelf: String,
    @SerialName("box_number") val boxNumber: String? = null
)
