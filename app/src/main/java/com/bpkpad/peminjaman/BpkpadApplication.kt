package com.bpkpad.peminjaman

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.bpkpad.peminjaman.core.notification.OverdueWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import java.util.Calendar
import javax.inject.Inject

@HiltAndroidApp
class BpkpadApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleOverdueWorker()
    }

    private fun scheduleOverdueWorker() {
        // Schedule daily at 08:00 WITA (UTC+8 = 00:00 UTC)
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = calendar.timeInMillis - now

        val overdueWorkRequest = PeriodicWorkRequestBuilder<OverdueWorker>(
            repeatInterval = 1, repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "overdue_check",
            ExistingPeriodicWorkPolicy.KEEP,
            overdueWorkRequest
        )
    }
}
