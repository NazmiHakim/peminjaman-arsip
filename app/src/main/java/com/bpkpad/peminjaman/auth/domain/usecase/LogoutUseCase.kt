package com.bpkpad.peminjaman.auth.domain.usecase

import com.bpkpad.peminjaman.core.session.SessionManager
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val auditLogRepository: AuditLogRepository
) {
    suspend operator fun invoke() {
        val session = sessionManager.session.first()
        if (session != null) {
            auditLogRepository.log(
                transaksiId = null,
                userId = session.userId,
                aksi = AuditAction.LOGOUT,
                detail = "Logout: ${session.username} (${session.role.name})"
            )
        }
        userRepository.logout()
        sessionManager.clearSession()
    }
}
