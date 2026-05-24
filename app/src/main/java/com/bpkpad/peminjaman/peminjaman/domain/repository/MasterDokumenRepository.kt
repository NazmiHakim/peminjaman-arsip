package com.bpkpad.peminjaman.peminjaman.domain.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.DokumenStatus
import kotlinx.coroutines.flow.Flow

interface MasterDokumenRepository {
    fun getAll(): Flow<List<MasterDokumen>>
    fun getAvailable(): Flow<List<MasterDokumen>>
    suspend fun getById(id: Int): MasterDokumen?
    suspend fun create(dokumen: MasterDokumen): ResultState<MasterDokumen>
    suspend fun update(dokumen: MasterDokumen): ResultState<MasterDokumen>
    suspend fun updateStatus(id: Int, status: DokumenStatus): ResultState<Unit>
    fun search(query: String): Flow<List<MasterDokumen>>
}
