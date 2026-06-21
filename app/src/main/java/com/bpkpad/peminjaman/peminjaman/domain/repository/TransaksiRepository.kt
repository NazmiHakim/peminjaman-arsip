package com.bpkpad.peminjaman.peminjaman.domain.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import kotlinx.coroutines.flow.Flow

interface TransaksiRepository {
    fun getAll(): Flow<List<Transaksi>>
    fun getByStatus(status: TransaksiStatus): Flow<List<Transaksi>>
    fun getOverdue(): Flow<List<Transaksi>>
    suspend fun getById(id: Int): Transaksi?
    suspend fun findByQrToken(token: String): Transaksi?
    suspend fun create(transaksi: Transaksi, dokumenIds: List<Int>): ResultState<Transaksi>
    suspend fun update(transaksi: Transaksi): ResultState<Transaksi>
    suspend fun approve(transaksiId: Int, approverId: Int, qrToken: String): ResultState<Unit>
    suspend fun reject(transaksiId: Int, approverId: Int, alasan: String): ResultState<Unit>
    suspend fun bypass(
        transaksiId: Int,
        arsiparisId: Int,
        buktiPath: String,
        catatan: String,
        qrToken: String
    ): ResultState<Unit>
    suspend fun acknowledgeBypass(transaksiId: Int, kasubagId: Int): ResultState<Unit>
    suspend fun confirmHandover(transaksiId: Int, arsiparisId: Int): ResultState<Unit>
    suspend fun returnTransaksi(
        transaksiId: Int,
        arsiparisId: Int,
        kondisiMap: Map<Int, Pair<String, String?>>
    ): ResultState<Unit>
    suspend fun cancel(transaksiId: Int, userId: Int): ResultState<Unit>
    suspend fun syncPending()
    fun getDashboardStats(): Flow<Map<String, Int>>
}
