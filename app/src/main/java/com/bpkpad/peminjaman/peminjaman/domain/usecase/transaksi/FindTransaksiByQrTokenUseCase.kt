package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
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
        if (transaksi.status == TransaksiStatus.DIKEMBALIKAN)
            return ResultState.Error("QR token sudah tidak aktif karena transaksi sudah dikembalikan")
        if (transaksi.status == TransaksiStatus.DIBATALKAN || transaksi.status == TransaksiStatus.DITOLAK)
            return ResultState.Error("QR token tidak aktif untuk transaksi terminal")
        return ResultState.Success(transaksi)
    }
}
