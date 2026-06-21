package com.bpkpad.peminjaman.auth.domain.usecase

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.core.session.SessionObject
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val auditLogRepository: AuditLogRepository
) {
    suspend operator fun invoke(username: String, password: String): ResultState<User> {
        if (username.isBlank()) return ResultState.Error("Username tidak boleh kosong")
        if (password.isBlank()) return ResultState.Error("Password tidak boleh kosong")
        if (username.length > 254) return ResultState.Error("Username terlalu panjang")
        if (password.length > 128) return ResultState.Error("Password terlalu panjang")

        val result = userRepository.login(username.trim(), password)

        if (result is ResultState.Success) {
            val user = result.data
            if (!user.isActive) return ResultState.Error("Akun tidak aktif. Hubungi administrator.")

            sessionManager.saveSession(
                SessionObject(
                    userId = user.id,
                    username = user.username,
                    namaLengkap = user.namaLengkap,
                    role = user.role,
                    noHp = user.noHp
                )
            )
            auditLogRepository.log(
                transaksiId = null,
                userId = user.id,
                aksi = AuditAction.LOGIN,
                detail = "Login berhasil: ${user.username} (${user.role.name})"
            )
        }

        return result
    }
}
