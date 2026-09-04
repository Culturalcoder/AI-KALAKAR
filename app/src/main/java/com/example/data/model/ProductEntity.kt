package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleEn: String,
    val titleHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val category: String,
    val priceMin: Int,
    val priceMax: Int,
    val selectedPrice: Int,
    val materialCost: Int,
    val laborHours: Float = 4.0f,
    val pricingReasoning: String,
    val rawImageUri: String,
    val enhancedImageUri: String,
    val tags: String,
    val status: String = "LIVE", // LIVE, DRAFT, SOLD
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "LOCAL" // LOCAL, SYNCED
)
