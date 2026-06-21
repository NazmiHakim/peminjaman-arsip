package com.bpkpad.peminjaman.peminjaman.data.repository

import android.util.Log
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.database.dao.MasterDokumenDao
import com.bpkpad.peminjaman.core.database.entity.MasterDokumenEntity
import com.bpkpad.peminjaman.peminjaman.data.remote.ArchiveDocumentDto
import com.bpkpad.peminjaman.peminjaman.domain.model.MasterDokumen
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.DokumenStatus
import com.bpkpad.peminjaman.peminjaman.domain.repository.MasterDokumenRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterDokumenRepositoryImpl @Inject constructor(
    private val dao: MasterDokumenDao,
    private val supabase: SupabaseClient
) : MasterDokumenRepository {

    override fun getAll(): Flow<List<MasterDokumen>> = flow {
        refreshFromRemote()
        emitAll(dao.getAll().map { list -> list.map { it.toDomain() } })
    }

    override fun getAvailable(): Flow<List<MasterDokumen>> = flow {
        refreshFromRemote()
        emitAll(dao.getAvailable().map { list -> list.map { it.toDomain() } })
    }

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

    override fun search(query: String): Flow<List<MasterDokumen>> = flow {
        refreshFromRemote()
        emitAll(dao.search(query).map { list -> list.map { it.toDomain() } })
    }

    private suspend fun refreshFromRemote() {
        runCatching {
            val remoteDocuments = supabase.from("archive_documents")
                .select(
                    columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                        """
                        id,
                        document_type,
                        document_number,
                        title,
                        description,
                        year,
                        status
                        """.trimIndent()
                    )
                )
                .decodeList<ArchiveDocumentDto>()
                .mapNotNull { it.toCacheEntityOrNull() }

            dao.cacheRemoteDocuments(remoteDocuments)
        }.onFailure { error ->
            Log.w(
                TAG,
                "Gagal menyinkronkan archive_documents; memakai cache Room.",
                error
            )
        }
    }

    private fun ArchiveDocumentDto.toCacheEntityOrNull(): MasterDokumenEntity? {
        val number = documentNumber?.trim().orEmpty()
        if (number.isBlank()) return null

        return MasterDokumenEntity(
            nomorDokumen = number,
            perihal = title,
            nominal = 0.0,
            tahun = year.toString(),
            jenisDokumen = documentType,
            status = status.toLocalDocumentStatus(),
            lokasiRak = storageLocation?.let { "${it.room} - ${it.shelf}" },
            lokasiBox = storageLocation?.boxNumber,
            remoteId = id
        )
    }

    private fun String.toLocalDocumentStatus(): String = when (uppercase()) {
        "AVAILABLE", "TERSEDIA" -> "tersedia"
        "BORROWED", "DIPINJAM" -> "dipinjam"
        "DAMAGED", "RUSAK" -> "rusak"
        "LOST", "HILANG" -> "hilang"
        else -> "tersedia"
    }

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

    private companion object {
        const val TAG = "MasterDokumenRepo"
    }
}
