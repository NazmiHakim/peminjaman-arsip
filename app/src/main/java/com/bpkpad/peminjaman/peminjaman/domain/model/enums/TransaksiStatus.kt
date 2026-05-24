package com.bpkpad.peminjaman.peminjaman.domain.model.enums

enum class TransaksiStatus(val displayName: String) {
    MENUNGGU_PERSETUJUAN("Menunggu Persetujuan"),
    DISETUJUI("Disetujui"),
    DITOLAK("Ditolak"),
    DIPINJAM("Sedang Dipinjam"),
    DIKEMBALIKAN("Dikembalikan"),
    DIBATALKAN("Dibatalkan");

    companion object {
        fun fromString(value: String): TransaksiStatus = entries.firstOrNull {
            it.name.equals(value.uppercase(), ignoreCase = true)
        } ?: MENUNGGU_PERSETUJUAN
    }
}
