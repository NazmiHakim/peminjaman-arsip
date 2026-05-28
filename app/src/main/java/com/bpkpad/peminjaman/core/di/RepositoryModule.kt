package com.bpkpad.peminjaman.core.di

import com.bpkpad.peminjaman.core.storage.FileRepository
import com.bpkpad.peminjaman.core.storage.LocalFileRepository
import com.bpkpad.peminjaman.laporan.data.repository.LaporanExportRepositoryImpl
import com.bpkpad.peminjaman.laporan.domain.repository.LaporanExportRepository
import com.bpkpad.peminjaman.peminjaman.data.repository.*
import com.bpkpad.peminjaman.peminjaman.domain.repository.*
import com.bpkpad.peminjaman.auth.data.repository.UserRepositoryImpl
import com.bpkpad.peminjaman.peminjaman.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFileRepository(impl: LocalFileRepository): FileRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindInstansiRepository(impl: InstansiRepositoryImpl): InstansiRepository

    @Binds @Singleton
    abstract fun bindMasterDokumenRepository(impl: MasterDokumenRepositoryImpl): MasterDokumenRepository

    @Binds @Singleton
    abstract fun bindTransaksiRepository(impl: TransaksiRepositoryImpl): TransaksiRepository

    @Binds @Singleton
    abstract fun bindPerpanjanganRepository(impl: PerpanjanganRepositoryImpl): PerpanjanganRepository

    @Binds @Singleton
    abstract fun bindAuditLogRepository(impl: AuditLogRepositoryImpl): AuditLogRepository

    @Binds @Singleton
    abstract fun bindLaporanExportRepository(impl: LaporanExportRepositoryImpl): LaporanExportRepository
}
