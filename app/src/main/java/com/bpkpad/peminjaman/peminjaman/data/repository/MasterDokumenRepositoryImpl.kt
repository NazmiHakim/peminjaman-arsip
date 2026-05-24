package com.bpkpad.peminjaman.peminjaman.data.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.MasterDokumenDao
import com.bpkpad.peminjaman.core.database.entity.MasterDokumenEntity
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.DokumenStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.MasterDokumenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterDokumenRepositoryImpl @Inject constructor(
    private val dao: MasterDokumenDao
) : MasterDokumenRepository {

    override fun getAll(): Flow<List<MasterDokumen>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getAvailable(): Flow<List<MasterDokumen>> =
        dao.getAvailable().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Int): MasterDokumen? =
        dao.getById(id)?.toDomain()

    override suspend fun create(dokumen: MasterDokumen): ResultState<MasterDokumen> {
        return try {
            val id = dao.insert(dokumen.toEntity())
            ResultState.Success(dokumen.copy(id = id.toInt()))
        } catch (e: Exception) {
            ResultState.Error("Gagal menyimpan dokumen: ${e.message}", e)
        }
    }

    override suspend fun update(dokumen: MasterDokumen): ResultState<MasterDokumen> {
        return try {
            dao.update(dokumen.toEntity())
            ResultState.Success(dokumen)
        } catch (e: Exception) {
            ResultState.Error("Gagal mengupdate dokumen: ${e.message}", e)
        }
    }

    override suspend fun updateStatus(id: Int, status: DokumenStatus): ResultState<Unit> {
        return try {
            dao.updateStatus(id, status.name.lowercase())
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal mengupdate status dokumen: ${e.message}", e)
        }
    }

    override fun search(query: String): Flow<List<MasterDokumen>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    private fun MasterDokumenEntity.toDomain() = MasterDokumen(
        id = id,
        nomorDokumen = nomorDokumen,
        perihal = perihal,
        nominal = nominal,
        tahun = tahun,
        jenisDokumen = jenisDokumen,
        status = DokumenStatus.fromString(status),
        lokasiRak = lokasiRak,
        lokasiBox = lokasiBox
    )

    private fun MasterDokumen.toEntity() = MasterDokumenEntity(
        id = id,
        nomorDokumen = nomorDokumen,
        perihal = perihal,
        nominal = nominal,
        tahun = tahun,
        jenisDokumen = jenisDokumen,
        status = status.name.lowercase(),
        lokasiRak = lokasiRak,
        lokasiBox = lokasiBox
    )
}
