package com.bpkpad.peminjaman.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bpkpad.peminjaman.core.common.Constants
import com.bpkpad.peminjaman.core.common.ResultState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [STUB] LocalFileRepository
 * TODO: Replace with Firebase Storage implementation when google-services.json is available.
 * Currently stores images locally in app internal storage.
 */
@Singleton
class LocalFileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {

    override suspend fun uploadImage(localUri: Uri, remotePath: String): ResultState<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(localUri)
                ?: return ResultState.Error("Cannot open image stream")

            val bitmap = BitmapFactory.decodeStream(inputStream)
            val compressed = compressBitmap(bitmap)

            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { out ->
                compressed.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_COMPRESSION_QUALITY, out)
            }

            // Return local file path as URL stub
            ResultState.Success("local://${file.absolutePath}")
        } catch (e: Exception) {
            ResultState.Error("Gagal upload gambar: ${e.message}", e)
        }
    }

    override suspend fun deleteFile(remotePath: String): ResultState<Unit> {
        return try {
            if (remotePath.startsWith("local://")) {
                val path = remotePath.removePrefix("local://")
                File(path).delete()
            }
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal hapus file: ${e.message}", e)
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.width > Constants.MAX_IMAGE_WIDTH || bitmap.height > Constants.MAX_IMAGE_HEIGHT) {
            val ratio = minOf(
                Constants.MAX_IMAGE_WIDTH.toFloat() / bitmap.width,
                Constants.MAX_IMAGE_HEIGHT.toFloat() / bitmap.height
            )
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else bitmap
    }
}
