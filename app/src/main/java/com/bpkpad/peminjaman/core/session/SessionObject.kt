package com.bpkpad.peminjaman.core.session

import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole

data class SessionObject(
    val userId: Int,
    val username: String,
    val namaLengkap: String,
    val role: UserRole,
    val noHp: String?
)
