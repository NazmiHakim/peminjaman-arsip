package com.bpkpad.peminjaman.peminjaman.data.repository

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.InstansiDao
import com.bpkpad.peminjaman.core.database.entity.InstansiEntity
import com.bpkpad.peminjaman.peminjaman.domain.model.Instansi
import com.bpkpad.peminjaman.peminjaman.domain.repository.InstansiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstansiRepositoryImpl @Inject constructor(
    private val dao: InstansiDao
) : InstansiRepository {

    override fun getAll(): Flow<List<Instansi>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Int): Instansi? =
        dao.getById(id)?.toDomain()

    override suspend fun create(instansi: Instansi): ResultState<Instansi> {
        return try {
            val id = dao.insert(instansi.toEntity())
            ResultState.Success(instansi.copy(id = id.toInt()))
        } catch (e: Exception) {
            ResultState.Error("Gagal menyimpan instansi: ${e.message}", e)
        }
    }

    override suspend fun update(instansi: Instansi): ResultState<Instansi> {
        return try {
            dao.update(instansi.toEntity())
            ResultState.Success(instansi)
        } catch (e: Exception) {
            ResultState.Error("Gagal mengupdate instansi: ${e.message}", e)
        }
    }

    override suspend fun delete(id: Int): ResultState<Unit> {
        return try {
            val entity = dao.getById(id) ?: return ResultState.Error("Instansi tidak ditemukan")
            dao.delete(entity)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menghapus instansi: ${e.message}", e)
        }
    }

    override fun search(query: String): Flow<List<Instansi>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    private fun InstansiEntity.toDomain() = Instansi(
        id = id,
        namaInstansi = namaInstansi,
        alamat = alamat,
        kodeInstansi = kodeInstansi
    )

    private fun Instansi.toEntity() = InstansiEntity(
        id = id,
        namaInstansi = namaInstansi,
        alamat = alamat,
        kodeInstansi = kodeInstansi
    )
}
