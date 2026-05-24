package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class FindTransaksiByQrTokenUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository
) {
    suspend operator fun invoke(token: String): ResultState<Transaksi> {
        if (token.isBlank())
            return ResultState.Error("Token QR tidak valid")
        val transaksi = transaksiRepo.findByQrToken(token)
            ?: return ResultState.Error("Tidak ditemukan transaksi dengan QR token ini")
        return ResultState.Success(transaksi)
    }
}
