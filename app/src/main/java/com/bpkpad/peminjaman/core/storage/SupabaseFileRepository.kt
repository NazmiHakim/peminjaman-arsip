package com.bpkpad.peminjaman.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bpkpad.peminjaman.core.common.Constants
import com.bpkpad.peminjaman.core.common.ResultState
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) : FileRepository {

    override suspend fun uploadImage(localUri: Uri, remotePath: String): ResultState<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(localUri)
                ?: return ResultState.Error("Tidak dapat membuka file gambar.")

            val bitmap = BitmapFactory.decodeStream(inputStream)
            val compressedBitmap = compressBitmap(bitmap)

            val bos = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_COMPRESSION_QUALITY, bos)
            val byteArray = bos.toByteArray()

            // File limit check (2MB)
            val maxLimit = 2 * 1024 * 1024 // 2MB
            if (byteArray.size > maxLimit) {
                return ResultState.Error("File hasil kompresi terlalu besar (${byteArray.size / 1024} KB). Batas maksimal adalah 2MB.")
            }

            // Upload to Supabase Storage
            val bucket = supabase.storage.from("loan-documents")
            bucket.upload(remotePath, byteArray) {
                upsert = true
            }

            // Construct and return the public URL
            val publicUrl = bucket.publicUrl(remotePath)
            ResultState.Success(publicUrl)
        } catch (e: Exception) {
            ResultState.Error("Gagal upload ke Supabase Storage: ${e.message}", e)
        }
    }

    override suspend fun deleteFile(remotePath: String): ResultState<Unit> {
        return try {
            // Extract relative path if publicUrl is provided
            val relativePath = if (remotePath.startsWith("http")) {
                // E.g., https://.../storage/v1/object/public/loan-documents/remotePath
                remotePath.substringAfter("/loan-documents/")
            } else {
                remotePath
            }
            supabase.storage.from("loan-documents").delete(relativePath)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error("Gagal menghapus file dari Supabase Storage: ${e.message}", e)
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
