package com.bpkpad.peminjaman.peminjaman.domain.model.enums

enum class PerpanjanganStatus(val displayName: String) {
    PENDING("Menunggu Persetujuan"),
    APPROVED("Disetujui"),
    REJECTED("Ditolak");

    companion object {
        fun fromString(value: String): PerpanjanganStatus = entries.firstOrNull {
            it.name.equals(value.uppercase(), ignoreCase = true)
        } ?: PENDING
    }
}
