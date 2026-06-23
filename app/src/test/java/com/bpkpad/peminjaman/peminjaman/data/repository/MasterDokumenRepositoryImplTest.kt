package com.bpkpad.peminjaman.peminjaman.data.repository

import com.bpkpad.peminjaman.core.database.dao.MasterDokumenDao
import com.bpkpad.peminjaman.core.database.entity.MasterDokumenEntity
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MasterDokumenRepositoryImplTest {

    private val dao: MasterDokumenDao = mockk(relaxed = true)

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `WB-REPO-03 fetchRemoteDocuments success`() = runTest {
        // Setup real SupabaseClient with MockEngine returning remote documents
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "dummy-key"
        ) {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                val path = request.url.encodedPath
                println("WB-REPO-03 Ktor Request Path: $path")
                when {
                    "archive_documents" in path -> {
                        respond(
                            content = """
                                [
                                    {
                                        "id": "remote-uuid-1",
                                        "document_type": "SP2D",
                                        "document_number": "SP2D-2023-001",
                                        "title": "SP2D Belanja Modal Dindik",
                                        "description": "desc",
                                        "year": 2023,
                                        "status": "available",
                                        "storage_locations": {
                                            "room": "Room A",
                                            "shelf": "Shelf 1",
                                            "box_number": "Box-01"
                                        }
                                    }
                                ]
                            """.trimIndent(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json")
                        )
                    }
                    else -> respond("[]", HttpStatusCode.NotFound)
                }
            }
        }

        val repository = MasterDokumenRepositoryImpl(dao, supabase)

        // Mock DAO flow
        val mockLocalList = listOf(
            MasterDokumenEntity(
                id = 1,
                nomorDokumen = "SP2D-2023-001",
                perihal = "SP2D Belanja Modal Dindik",
                nominal = 0.0,
                tahun = "2023",
                jenisDokumen = "SP2D",
                status = "tersedia",
                lokasiRak = "Room A - Shelf 1",
                lokasiBox = "Box-01",
                remoteId = "remote-uuid-1"
            )
        )
        every { dao.getAll() } returns flowOf(mockLocalList)

        // Call public method
        val flowResult = repository.getAll().first()

        // Assert local cache was updated and data emitted
        assertEquals(1, flowResult.size)
        assertEquals("SP2D-2023-001", flowResult[0].nomorDokumen)

        val expectedCapture = slot<List<MasterDokumenEntity>>()
        coVerify(exactly = 1) {
            dao.cacheRemoteDocuments(capture(expectedCapture))
        }
        val capturedList = expectedCapture.captured
        assertEquals(1, capturedList.size)
        assertEquals("SP2D-2023-001", capturedList[0].nomorDokumen)
        assertEquals("remote-uuid-1", capturedList[0].remoteId)
    }

    @Test
    fun `WB-REPO-04 fetchRemoteDocuments failed`() = runTest {
        // Setup real SupabaseClient with MockEngine returning network error
        val supabase = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "dummy-key"
        ) {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                respond("Network Error", HttpStatusCode.InternalServerError, headersOf())
            }
        }

        val repository = MasterDokumenRepositoryImpl(dao, supabase)

        // Mock DAO flow
        val mockLocalList = listOf(
            MasterDokumenEntity(
                id = 1,
                nomorDokumen = "SP2D-2023-001",
                perihal = "SP2D Belanja Modal Dindik",
                nominal = 0.0,
                tahun = "2023",
                jenisDokumen = "SP2D",
                status = "tersedia",
                lokasiRak = "Room A - Shelf 1",
                lokasiBox = "Box-01",
                remoteId = "remote-uuid-1"
            )
        )
        every { dao.getAll() } returns flowOf(mockLocalList)

        // Call public method
        val flowResult = repository.getAll().first()

        // Assert fallback to cache works
        assertEquals(1, flowResult.size)
        assertEquals("SP2D-2023-001", flowResult[0].nomorDokumen)

        // Verify cacheRemoteDocuments was NOT called
        coVerify(exactly = 0) {
            dao.cacheRemoteDocuments(any())
        }
    }
}
