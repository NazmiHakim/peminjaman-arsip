package com.bpkpad.peminjaman.laporan.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bpkpad.peminjaman.core.common.ResultState
import com.bpkpad.peminjaman.core.common.toDisplayString
import com.bpkpad.peminjaman.laporan.domain.model.ExportedReport
import com.bpkpad.peminjaman.laporan.domain.repository.LaporanExportRepository
import com.bpkpad.peminjaman.peminjaman.domain.model.Transaksi
import com.bpkpad.peminjaman.peminjaman.domain.model.enums.TransaksiStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaporanExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LaporanExportRepository {

    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun exportPdf(
        transaksi: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?
    ): ResultState<ExportedReport> {
        return try {
            val file = createReportFile("laporan_peminjaman_${timestampFormatter.format(LocalDateTime.now())}.pdf")
            val document = PdfDocument()
            try {
                writePdfPages(document, transaksi, periodeMulai, periodeSampai)
                FileOutputStream(file).use { output -> document.writeTo(output) }
            } finally {
                document.close()
            }
            ResultState.Success(
                ExportedReport(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    mimeType = "application/pdf",
                    totalRows = transaksi.size
                )
            )
        } catch (e: Exception) {
            ResultState.Error("Gagal membuat laporan PDF: ${e.message}", e)
        }
    }

    override suspend fun exportExcel(
        transaksi: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?
    ): ResultState<ExportedReport> {
        return try {
            val file = createReportFile("laporan_peminjaman_${timestampFormatter.format(LocalDateTime.now())}.xlsx")
            XSSFWorkbook().use { workbook ->
                val sheet = workbook.createSheet("Laporan Peminjaman")
                val headerStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    alignment = HorizontalAlignment.CENTER
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                    setFont(workbook.createFont().apply {
                        color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
                        bold = true
                    })
                }
                val bodyStyle = workbook.createCellStyle().apply {
                    borderBottom = BorderStyle.THIN
                    borderTop = BorderStyle.THIN
                    borderLeft = BorderStyle.THIN
                    borderRight = BorderStyle.THIN
                    wrapText = true
                }
                val titleStyle = workbook.createCellStyle().apply {
                    setFont(workbook.createFont().apply {
                        bold = true
                        fontHeightInPoints = 14.toShort()
                    })
                }

                sheet.createRow(0).apply {
                    createCell(0).setCellValue("Laporan Peminjaman Dokumen BPKPAD Balangan")
                    getCell(0).cellStyle = titleStyle
                }
                sheet.createRow(1).createCell(0).setCellValue("Periode: ${formatPeriod(periodeMulai, periodeSampai)}")
                sheet.createRow(2).createCell(0).setCellValue("Dibuat: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale("id", "ID")))}")

                val headers = listOf(
                    "ID",
                    "Instansi",
                    "PIC",
                    "No HP",
                    "Nomor Surat",
                    "Tanggal Pinjam",
                    "Tenggat",
                    "Tanggal Kembali",
                    "Status",
                    "Jumlah Dokumen",
                    "Overdue"
                )
                sheet.createRow(4).apply {
                    headers.forEachIndexed { index, label ->
                        createCell(index).apply {
                            setCellValue(label)
                            cellStyle = headerStyle
                        }
                    }
                }

                transaksi.forEachIndexed { index, item ->
                    sheet.createRow(index + 5).apply {
                        val values = listOf(
                            item.id.toString(),
                            item.namaInstansi.ifBlank { "Instansi #${item.namaInstansi}" },
                            item.picNama,
                            item.picNoHp,
                            item.nomorSuratPengantar,
                            item.tanggalPinjam.format(dateFormatter),
                            item.tanggalKembaliRencana.format(dateFormatter),
                            item.tanggalKembaliAktual?.format(dateFormatter).orEmpty(),
                            item.status.name.lowercase().replace('_', ' '),
                            item.details.size.toString(),
                            if (item.isOverdue) "${item.daysOverdue} hari" else "-"
                        )
                        values.forEachIndexed { cellIndex, value ->
                            createCell(cellIndex).apply {
                                setCellValue(value)
                                cellStyle = bodyStyle
                            }
                        }
                    }
                }

                for (column in headers.indices) {
                    sheet.setColumnWidth(column, columnWidthFor(column))
                }
                FileOutputStream(file).use { output -> workbook.write(output) }
            }
            ResultState.Success(
                ExportedReport(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    totalRows = transaksi.size
                )
            )
        } catch (e: Exception) {
            ResultState.Error("Gagal membuat laporan Excel: ${e.message}", e)
        }
    }

    private fun writePdfPages(
        document: PdfDocument,
        transaksi: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?
    ) {
        val pageHeight = 595
        val margin = 36f
        val rowHeight = 28f
        val headerHeight = 152f
        val rowsPerPage = ((pageHeight - headerHeight - margin) / rowHeight).toInt().coerceAtLeast(1)
        val pages = transaksi.chunked(rowsPerPage)
        val pageCount = pages.size.coerceAtLeast(1)

        if (pages.isEmpty()) {
            writePdfPage(document, emptyList(), periodeMulai, periodeSampai, 1, 1, margin, rowHeight)
            return
        }

        pages.forEachIndexed { index, rows ->
            writePdfPage(document, rows, periodeMulai, periodeSampai, index + 1, pageCount, margin, rowHeight)
        }
    }

    private fun writePdfPage(
        document: PdfDocument,
        rows: List<Transaksi>,
        periodeMulai: LocalDate?,
        periodeSampai: LocalDate?,
        pageNumber: Int,
        pageCount: Int,
        margin: Float,
        rowHeight: Float
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(13, 71, 161)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 28, 30)
            textSize = 9f
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(90, 94, 102)
            textSize = 8f
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 101, 192)
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(222, 227, 235)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawText("Laporan Peminjaman Dokumen", margin, 42f, titlePaint)
        canvas.drawText("BPKPAD Kabupaten Balangan", margin, 60f, bodyPaint)
        canvas.drawText("Periode: ${formatPeriod(periodeMulai, periodeSampai)}", margin, 82f, smallPaint)
        canvas.drawText(
            "Dibuat: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale("id", "ID")))}",
            margin,
            96f,
            smallPaint
        )
        drawPdfSummary(canvas, rows, margin, 116f, bodyPaint, smallPaint)

        val tableTop = 152f
        val columns = listOf(
            ColumnSpec("ID", 34f),
            ColumnSpec("Instansi", 126f),
            ColumnSpec("PIC", 84f),
            ColumnSpec("Surat", 112f),
            ColumnSpec("Pinjam", 70f),
            ColumnSpec("Tenggat", 70f),
            ColumnSpec("Status", 82f),
            ColumnSpec("Dok", 36f),
            ColumnSpec("Overdue", 62f)
        )
        drawPdfHeader(canvas, columns, margin, tableTop, rowHeight, headerPaint, headerTextPaint)

        var y = tableTop + rowHeight
        rows.forEach { item ->
            val values = listOf(
                "#${item.id}",
                item.namaInstansi.ifBlank { "Instansi #${item.namaInstansi}" },
                item.picNama,
                item.nomorSuratPengantar,
                item.tanggalPinjam.toDisplayString(),
                item.tanggalKembaliRencana.toDisplayString(),
                item.status.name.lowercase().replace('_', ' '),
                item.details.size.toString(),
                if (item.isOverdue) "${item.daysOverdue} hari" else "-"
            )
            drawPdfRow(canvas, columns, values, margin, y, rowHeight, bodyPaint, linePaint)
            y += rowHeight
        }

        canvas.drawText("Halaman $pageNumber dari $pageCount", 742f, 570f, smallPaint)
        document.finishPage(page)
    }

    private fun drawPdfSummary(
        canvas: Canvas,
        rows: List<Transaksi>,
        x: Float,
        y: Float,
        bodyPaint: Paint,
        smallPaint: Paint
    ) {
        val total = rows.size
        val dipinjam = rows.count { it.status == TransaksiStatus.DIPINJAM }
        val overdue = rows.count { it.isOverdue }
        canvas.drawText("Ringkasan halaman: $total transaksi | Dipinjam: $dipinjam | Overdue: $overdue", x, y, bodyPaint)
        canvas.drawText("Nominal tidak ditampilkan karena laporan ini berfokus pada transaksi peminjaman arsip.", x, y + 14f, smallPaint)
    }

    private fun drawPdfHeader(
        canvas: Canvas,
        columns: List<ColumnSpec>,
        x: Float,
        y: Float,
        height: Float,
        backgroundPaint: Paint,
        textPaint: Paint
    ) {
        var currentX = x
        columns.forEach { column ->
            canvas.drawRect(currentX, y, currentX + column.width, y + height, backgroundPaint)
            canvas.drawText(column.label, currentX + 4f, y + 18f, textPaint)
            currentX += column.width
        }
    }

    private fun drawPdfRow(
        canvas: Canvas,
        columns: List<ColumnSpec>,
        values: List<String>,
        x: Float,
        y: Float,
        height: Float,
        textPaint: Paint,
        linePaint: Paint
    ) {
        var currentX = x
        columns.forEachIndexed { index, column ->
            canvas.drawRect(currentX, y, currentX + column.width, y + height, linePaint)
            canvas.drawText(values[index].ellipsizeForPdf(column.width), currentX + 4f, y + 18f, textPaint)
            currentX += column.width
        }
    }

    private fun createReportFile(fileName: String): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        return File(reportsDir, fileName)
    }

    private fun formatPeriod(periodeMulai: LocalDate?, periodeSampai: LocalDate?): String {
        return when {
            periodeMulai != null && periodeSampai != null -> "${periodeMulai.toDisplayString()} - ${periodeSampai.toDisplayString()}"
            periodeMulai != null -> "Mulai ${periodeMulai.toDisplayString()}"
            periodeSampai != null -> "Sampai ${periodeSampai.toDisplayString()}"
            else -> "Semua periode"
        }
    }

    private fun columnWidthFor(index: Int): Int {
        return when (index) {
            0 -> 8
            1 -> 28
            2 -> 22
            3 -> 18
            4 -> 28
            5, 6, 7 -> 16
            8 -> 20
            9 -> 14
            else -> 12
        } * 256
    }

    private fun String.ellipsizeForPdf(width: Float): String {
        val maxChars = (width / 5.2f).toInt().coerceAtLeast(4)
        return if (length <= maxChars) this else take(maxChars - 1) + "..."
    }

    private data class ColumnSpec(val label: String, val width: Float)
}
