package com.bpkpad.peminjaman.core.storage

import android.net.Uri
import com.bpkpad.peminjaman.core.common.ResultState

interface FileRepository {
    suspend fun uploadImage(localUri: Uri, remotePath: String): ResultState<String>
    suspend fun deleteFile(remotePath: String): ResultState<Unit>
}
