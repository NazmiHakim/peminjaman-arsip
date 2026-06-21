package com.bpkpad.peminjaman.core.di

import com.bpkpad.peminjaman.core.network.AuthInterceptor
import com.bpkpad.peminjaman.core.network.RetrofitClient
import com.bpkpad.peminjaman.core.network.SupabaseConfig
import com.bpkpad.peminjaman.core.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
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

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        SupabaseConfig.requireConfigured()
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.url,
            supabaseKey = SupabaseConfig.publishableKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }
}
