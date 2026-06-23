package com.bpkpad.peminjaman.core.network

import com.bpkpad.peminjaman.BuildConfig

/**
 * Public Supabase client configuration.
 *
 * The publishable key is intentionally treated as a public client identifier.
 * Data security must be enforced by Supabase Auth, RLS, and Storage policies.
 */
object SupabaseConfig {
    val url: String
        get() = BuildConfig.SUPABASE_URL.trim()

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = url.startsWith("https://") &&
            url.endsWith(".supabase.co") &&
            publishableKey.startsWith("sb_publishable_")

    fun requireConfigured() {
        check(isConfigured) {
            "Supabase belum dikonfigurasi. Isi SUPABASE_URL dan " +
                "SUPABASE_PUBLISHABLE_KEY di local.properties."
        }
    }
}
