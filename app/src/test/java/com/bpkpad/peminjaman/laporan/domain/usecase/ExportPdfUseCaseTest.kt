package com.bpkpad.peminjaman.laporan.domain.usecase

import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import com.bpkpad.peminjaman.laporan.domain.repository.LaporanExportRepository
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExportPdfUseCaseTest {

    private val repository: LaporanExportRepository = mockk()
    private lateinit var useCase: ExportPdfUseCase

    @Before
    fun setUp() {
        useCase = ExportPdfUseCase(repository)
    }

    @Test
    fun `WB-UC-61 Empty list of transactions returns error`() = runTest {
        val result = useCase(transaksi = emptyList(), periodeMulai = null, periodeSampai = null)
        assertTrue(result is ResultState.Error)
        assertEquals("Tidak ada data transaksi untuk diekspor", (result as ResultState.Error).message)
        coVerify(exactly = 0) { repository.exportPdf(any(), any(), any()) }
    }

    @Test
    fun `WB-UC-62 Success path`() = runTest {
        val transactions = listOf(mockk<Transaksi>())
        val mockReport = ExportedReport(
            filePath = "/path/to/report.pdf",
            fileName = "report.pdf",
            mimeType = "application/pdf",
            totalRows = 1
        )
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 12, 31)
        coEvery { repository.exportPdf(transactions, start, end) } returns ResultState.Success(mockReport)

        val result = useCase(transactions, start, end)
        assertTrue(result is ResultState.Success)
        assertEquals(mockReport, (result as ResultState.Success).data)
    }
}
