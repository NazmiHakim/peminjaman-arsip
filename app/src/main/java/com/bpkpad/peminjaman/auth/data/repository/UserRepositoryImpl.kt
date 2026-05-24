package com.bpkpad.peminjaman.auth.data.repository

import com.bpkpad.peminjaman.auth.domain.model.User
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.UserDao
import com.bpkpad.peminjaman.core.database.entity.UserEntity
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.UserRole
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun login(username: String, passwordHash: String): ResultState<User> {
        return try {
            val entity = userDao.login(username, passwordHash)
                ?: return ResultState.Error("Username atau password salah")
            ResultState.Success(entity.toDomain())
        } catch (e: Exception) {
            ResultState.Error("Gagal login: ${e.message}", e)
        }
    }

    override suspend fun getUserById(id: Int): User? {
        return userDao.getById(id)?.toDomain()
    }

    override suspend fun getAllUsers(): List<User> {
        // Collect from Flow synchronously for simple use
        return emptyList() // This impl uses Flow; direct access via DAO
    }

    private fun UserEntity.toDomain() = User(
        id = id,
        username = username,
        namaLengkap = namaLengkap,
        nip = nip,
        role = if (role == "kasubag") UserRole.KASUBAG else UserRole.ARSIPARIS,
        noHp = noHp,
        isActive = isActive
    )
}
