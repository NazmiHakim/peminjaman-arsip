package com.bpkpad.peminjaman.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bpkpad.peminjaman.MainActivity
import com.bpkpad.peminjaman.R
import com.bpkpad.peminjaman.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk dokumen peminjaman yang terlambat dikembalikan"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOverdueNotification(overdueCount: Int) {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("${Constants.DEEP_LINK_SCHEME}://${Constants.DEEP_LINK_OVERDUE_HOST}"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Dokumen Terlambat Dikembalikan!")
            .setContentText("Ada $overdueCount dokumen yang melewati batas waktu pengembalian")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Ada $overdueCount dokumen yang melewati batas waktu pengembalian. Segera hubungi peminjam untuk mengembalikan dokumen.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted - silent fail
        }
    }

    fun showApprovalRequestNotification(transaksiId: Int, instansiName: String, picNama: String) {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("${Constants.DEEP_LINK_SCHEME}://${Constants.DEEP_LINK_TRANSAKSI_HOST}/$transaksiId"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, transaksiId, deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pengajuan Baru Menunggu Persetujuan")
            .setContentText("$instansiName - PIC: $picNama")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2000 + transaksiId, notification)
        } catch (e: SecurityException) {
            // Silent fail
        }
    }
}
