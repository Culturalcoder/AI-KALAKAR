package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artisan_profile")
data class ArtisanProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val artisanName: String = "कारीगर (Artisan)",
    val phone: String = "+91 98765 43210",
    val craftType: String = "हथकरघा एवं मिट्टी शिल्प (Handloom & Pottery)",
    val region: String = "सूरत एवं खुर्जा (Surat & Khurja)",
    val preferredLanguage: String = "hi", // "hi" or "en"
    val completedWalkthrough: Boolean = false,
    val totalEarnings: Long = 0L,
    val fairsAttended: String = "शिल्प समागम, सूरजकुंड मेला"
)
