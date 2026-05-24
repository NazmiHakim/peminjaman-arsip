package com.bpkpad.peminjaman.auth.domain.model

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole

data class User(
    val id: Int,
    val username: String,
    val namaLengkap: String,
    val nip: String?,
    val role: UserRole,
    val noHp: String?,
    val isActive: Boolean
)
