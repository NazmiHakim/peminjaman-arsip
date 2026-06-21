package com.bpkpad.peminjaman.peminjaman.domain.usecase.transaksi

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.AuditAction
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.KondisiPengembalian
import com.bpkpad.peminjaman.peminjaman.domain.repository.AuditLogRepository
import com.bpkpad.peminjaman.peminjaman.domain.repository.TransaksiRepository
import javax.inject.Inject

class ReturnTransaksiUseCase @Inject constructor(
    private val transaksiRepo: TransaksiRepository,
    private val auditRepo: AuditLogRepository
) {
    /**
     * @param kondisiMap Map<detailId, Pair<kondisi, catatanKondisi>>
     *   kondisi must be one of: "baik", "rusak", "hilang" (case-insensitive)
     *   catatanKondisi is mandatory when kondisi is rusak or hilang
     */
    suspend operator fun invoke(
        transaksiId: Int,
        arsiparisId: Int,
        kondisiMap: Map<Int, Pair<String, String?>>
    ): ResultState<Unit> {
        val transaksi = transaksiRepo.getById(transaksiId)
            ?: return ResultState.Error("Transaksi tidak ditemukan")
        if (!transaksi.canBeReturned)
            return ResultState.Error("Hanya transaksi dipinjam yang dapat dikembalikan")

        // Validate: rusak/hilang must have catatan
        for ((_, pair) in kondisiMap) {
            val (kondisi, catatan) = pair
            val k = KondisiPengembalian.fromString(kondisi)
            if ((k == KondisiPengembalian.RUSAK || k == KondisiPengembalian.HILANG) && catatan.isNullOrBlank())
                return ResultState.Error("Catatan kondisi wajib diisi untuk dokumen rusak/hilang")
        }

        val result = transaksiRepo.returnTransaksi(transaksiId, arsiparisId, kondisiMap)
        if (result is ResultState.Success) {
            // Escalate audit action to worst condition found
            val hasHilang = kondisiMap.values.any { it.first.equals("hilang", ignoreCase = true) }
            val hasRusak = kondisiMap.values.any { it.first.equals("rusak", ignoreCase = true) }
            val action = when {
                hasHilang -> AuditAction.DIKEMBALIKAN_HILANG
                hasRusak -> AuditAction.DIKEMBALIKAN_RUSAK
                else -> AuditAction.DIKEMBALIKAN_BAIK
            }
            val conditionNotes = kondisiMap.values
                .mapNotNull { (condition, note) ->
                    note?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.let { "${condition.replaceFirstChar(Char::uppercase)}: $it" }
                }
                .joinToString(separator = "\n")
                .ifBlank { null }
            auditRepo.log(
                transaksiId = transaksiId,
                userId = arsiparisId,
                aksi = action,
                detail = "Dokumen dikembalikan. ${kondisiMap.size} dokumen diproses.",
                catatan = conditionNotes
            )
        }
        return result
    }
}
