package com.bpkpad.peminjaman.qr

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.bpkpad.peminjaman.core.common.Constants
import java.io.File
import java.io.FileOutputStream

object QrShareHelper {

    private const val TAG = "QrShareHelper"
    private const val QR_SHARE_SIZE = 768

    fun sendQrToWhatsApp(
        context: Context,
        rawPhoneNumber: String,
        qrToken: String,
        message: String,
        fileName: String
    ): Boolean {
        val phoneNumber = normalizeIndonesianPhoneNumber(rawPhoneNumber)
        val qrFile = createQrFile(context, qrToken, fileName)
            ?: return openWhatsAppText(context, phoneNumber, message)
        val qrUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            qrFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage(Constants.WHATSAPP_PACKAGE)
            putExtra(Intent.EXTRA_STREAM, qrUri)
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra("jid", "$phoneNumber@s.whatsapp.net")
            clipData = ClipData.newRawUri("QR Code Pengembalian", qrUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "WhatsApp package unavailable, falling back to web URL.", e)
            openWhatsAppText(context, phoneNumber, message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share QR image to WhatsApp.", e)
            openWhatsAppText(context, phoneNumber, message)
        }
    }

    private fun createQrFile(context: Context, qrToken: String, fileName: String): File? {
        val bitmap = QrGenerator.generateQrBitmap(qrToken, QR_SHARE_SIZE) ?: return null
        return try {
            val qrDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            val safeFileName = fileName
                .ifBlank { "qr_pengembalian" }
                .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                .ifBlank { "qr_pengembalian" }
            val file = File(qrDir, "$safeFileName.png")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create QR share file.", e)
            null
        }
    }

    private fun openWhatsAppText(context: Context, phoneNumber: String, message: String): Boolean {
        val uri = Uri.parse(
            "${Constants.WHATSAPP_API_URL}?phone=$phoneNumber&text=${Uri.encode(message)}"
        )
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WhatsApp text URL.", e)
            false
        }
    }

    private fun normalizeIndonesianPhoneNumber(rawPhoneNumber: String): String {
        val digits = rawPhoneNumber.filter { it.isDigit() }
        return when {
            digits.startsWith("0") -> "62${digits.drop(1)}"
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }
}
