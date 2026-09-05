package com.example.data.supabase

data class SupabaseProductResponse(
    val id: String,
    val artisanId: String?,
    val titleEn: String,
    val titleHi: String?,
    val descriptionEn: String?,
    val descriptionHi: String?,
    val category: String?,
    val material: String?,
    val selectedPrice: Double,
    val imageUrl: String?,
    val status: String,
    val createdAt: String?
)
