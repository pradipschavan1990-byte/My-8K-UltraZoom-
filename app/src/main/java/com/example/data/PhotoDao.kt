package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<CapturedPhotoEntity>>

    @Query("SELECT * FROM captured_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): CapturedPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: CapturedPhotoEntity): Long

    @Delete
    suspend fun deletePhoto(photo: CapturedPhotoEntity)

    @Query("DELETE FROM captured_photos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
