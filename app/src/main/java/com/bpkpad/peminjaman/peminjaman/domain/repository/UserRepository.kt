package com.bpkpad.peminjaman.peminjaman.domain.repository

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState

interface UserRepository {
    suspend fun login(username: String, passwordHash: String): ResultState<User>
    suspend fun getUserById(id: Int): User?
    suspend fun getAllUsers(): List<User>
}
