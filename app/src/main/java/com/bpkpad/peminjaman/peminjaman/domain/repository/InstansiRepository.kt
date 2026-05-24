package com.bpkpad.peminjaman.peminjaman.domain.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import kotlinx.coroutines.flow.Flow

interface InstansiRepository {
    fun getAll(): Flow<List<Instansi>>
    suspend fun getById(id: Int): Instansi?
    suspend fun create(instansi: Instansi): ResultState<Instansi>
    suspend fun update(instansi: Instansi): ResultState<Instansi>
    suspend fun delete(id: Int): ResultState<Unit>
    fun search(query: String): Flow<List<Instansi>>
}
