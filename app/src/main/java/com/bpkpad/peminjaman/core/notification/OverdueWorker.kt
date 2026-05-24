package com.bpkpad.peminjaman.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bpkpad.peminjaman.core.database.dao.TransaksiDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * OverdueWorker - Checks for overdue loan transactions daily at 08:00 WITA.
 * Triggers local notification for Arsiparis when overdue items are found.
 */
@HiltWorker
class OverdueWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transaksiDao: TransaksiDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val overdueList = transaksiDao.getOverdueSync(today)

            if (overdueList.isNotEmpty()) {
                notificationHelper.showOverdueNotification(overdueList.size)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("OVERDUE_WORKER", "Failed: ${e.message}", e)
            Result.retry()
        }
    }
}
