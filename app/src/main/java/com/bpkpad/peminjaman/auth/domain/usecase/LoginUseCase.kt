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

        val passwordHash = hashPassword(password)
        val result = userRepository.login(username.trim(), passwordHash)

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
            // LOGIN audit — transaksiId = 0 is the sentinel for non-transaction auth actions
            auditLogRepository.log(
                transaksiId = 0,
                userId = user.id,
                aksi = AuditAction.LOGIN,
                detail = "Login berhasil: ${user.username} (${user.role.name})"
            )
        }

        return result
    }

    private fun hashPassword(password: String): String {
        // TODO: Replace with BCrypt when integrating real auth backend
        // Must match format used by DatabaseSeeder for dummy accounts
        return "\$2a\$10\$${password.hashCode()}dummyhash"
    }
}
