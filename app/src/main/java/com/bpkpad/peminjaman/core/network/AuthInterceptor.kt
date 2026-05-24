package com.bpkpad.peminjaman.core.network

import com.bpkpad.peminjaman.core.session.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = runBlocking { sessionManager.session.firstOrNull() }
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .apply {
                if (session != null) {
                    addHeader("X-User-Id", session.userId.toString())
                    addHeader("X-User-Role", session.role.name)
                }
            }
            .build()
        return chain.proceed(request)
    }
}
