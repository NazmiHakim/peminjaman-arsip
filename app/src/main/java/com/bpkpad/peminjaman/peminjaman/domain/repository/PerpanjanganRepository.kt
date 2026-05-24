package com.bpkpad.peminjaman.peminjaman.domain.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Perpanjangan
import kotlinx.coroutines.flow.Flow

interface PerpanjanganRepository {
    fun getPendingAll(): Flow<List<Perpanjangan>>
    fun getByTransaksiId(transaksiId: Int): Flow<List<Perpanjangan>>
    suspend fun create(perpanjangan: Perpanjangan): ResultState<Perpanjangan>
    suspend fun approve(perpanjanganId: Int, approverId: Int): ResultState<Unit>
    suspend fun reject(perpanjanganId: Int, approverId: Int, alasan: String): ResultState<Unit>
}
