package com.bpkpad.peminjaman.peminjaman.domain.model.enums

enum class MetodePersetujuan(val displayName: String) {
    ONLINE("Online"),
    BYPASS("Bypass Persetujuan");

    companion object {
        fun fromString(value: String?): MetodePersetujuan? = if (value == null) null else
            entries.firstOrNull { it.name.equals(value.uppercase(), ignoreCase = true) }
    }
}
