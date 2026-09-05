package com.example.data.supabase

import com.example.BuildConfig

object SupabaseConfig {
    val supabaseUrl: String by lazy {
        try {
            val field = BuildConfig::class.java.getField("SUPABASE_URL")
            val v = field.get(null) as? String
            if (!v.isNullOrBlank() && !v.startsWith("MY_")) v else fallbackUrl()
        } catch (_: Throwable) {
            fallbackUrl()
        }
    }

    val supabaseAnonKey: String by lazy {
        try {
            val field = BuildConfig::class.java.getField("SUPABASE_ANON_KEY")
            val v = field.get(null) as? String
            if (!v.isNullOrBlank() && !v.startsWith("MY_")) v else fallbackKey()
        } catch (_: Throwable) {
            fallbackKey()
        }
    }

    private fun fallbackUrl(): String {
        return try {
            val field = BuildConfig::class.java.getField("public_url")
            (field.get(null) as? String)?.takeIf { it.isNotBlank() && !it.startsWith("MY_") }
                ?: "https://yteasfddnnyjvsbitrem.supabase.co"
        } catch (_: Throwable) {
            "https://yteasfddnnyjvsbitrem.supabase.co"
        }
    }

    private fun fallbackKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("Anon_key")
            (field.get(null) as? String)?.takeIf { it.isNotBlank() && !it.startsWith("MY_") }
                ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}
