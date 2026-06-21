package com.bpkpad.peminjaman.core.di

import android.content.Context
import androidx.room.Room
import com.bpkpad.peminjaman.core.common.Constants
import com.bpkpad.peminjaman.core.database.AppDatabase
import com.bpkpad.peminjaman.core.database.DatabaseMigrations
import com.bpkpad.peminjaman.core.database.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "peminjaman_arsip.db"
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabaseSeeder(db: AppDatabase): DatabaseSeeder {
        return DatabaseSeeder(db)
    }

    // Expose individual DAOs for injection
    @Provides fun provideUserDao(db: AppDatabase) = db.userDao()
    @Provides fun provideInstansiDao(db: AppDatabase) = db.instansiDao()
    @Provides fun provideMasterDokumenDao(db: AppDatabase) = db.masterDokumenDao()
    @Provides fun provideTransaksiDao(db: AppDatabase) = db.transaksiDao()
    @Provides fun provideDetailPeminjamanDao(db: AppDatabase) = db.detailPeminjamanDao()
    @Provides fun providePerpanjanganDao(db: AppDatabase) = db.perpanjanganDao()
    @Provides fun provideAuditLogDao(db: AppDatabase) = db.auditLogDao()
}
