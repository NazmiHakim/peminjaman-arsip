package com.bpkpad.peminjaman.peminjaman.domain.model.enums

enum class KondisiPengembalian(val displayName: String) {
    BAIK("Baik"),
    RUSAK("Rusak"),
    HILANG("Hilang");

    companion object {
        fun fromString(value: String?): KondisiPengembalian? = if (value == null) null else
            entries.firstOrNull { it.name.equals(value.uppercase(), ignoreCase = true) }
    }
}
