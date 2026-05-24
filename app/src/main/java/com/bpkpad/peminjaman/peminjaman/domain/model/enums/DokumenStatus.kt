package com.bpkpad.peminjaman.peminjaman.domain.model.enums

enum class DokumenStatus(val displayName: String) {
    TERSEDIA("Tersedia"),
    DIPINJAM("Sedang Dipinjam"),
    RUSAK("Rusak"),
    HILANG("Hilang");

    companion object {
        fun fromString(value: String): DokumenStatus = entries.firstOrNull {
            it.name.equals(value.uppercase(), ignoreCase = true)
        } ?: TERSEDIA
    }
}
