package com.bpkpad.peminjaman.auth.domain.usecase

import com.bpkpad.peminjaman.core.session.SessionManager
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() {
        sessionManager.clearSession()
    }
}
