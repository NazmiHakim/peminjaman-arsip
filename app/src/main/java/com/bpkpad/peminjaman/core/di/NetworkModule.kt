package com.bpkpad.peminjaman.core.di

import com.bpkpad.peminjaman.core.network.AuthInterceptor
import com.bpkpad.peminjaman.core.network.RetrofitClient
import com.bpkpad.peminjaman.core.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): AuthInterceptor {
        return AuthInterceptor(sessionManager)
    }

    @Provides
    @Singleton
    fun provideRetrofit(authInterceptor: AuthInterceptor): Retrofit {
        return RetrofitClient.create(authInterceptor)
    }
}
