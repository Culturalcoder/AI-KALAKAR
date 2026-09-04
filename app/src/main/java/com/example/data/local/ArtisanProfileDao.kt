package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ArtisanProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtisanProfileDao {
    @Query("SELECT * FROM artisan_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<ArtisanProfileEntity?>

    @Query("SELECT * FROM artisan_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): ArtisanProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ArtisanProfileEntity)

    @Update
    suspend fun updateProfile(profile: ArtisanProfileEntity)

    @Query("UPDATE artisan_profile SET preferredLanguage = :lang WHERE id = 1")
    suspend fun updateLanguage(lang: String)

    @Query("UPDATE artisan_profile SET completedWalkthrough = :completed WHERE id = 1")
    suspend fun updateWalkthrough(completed: Boolean)
}
