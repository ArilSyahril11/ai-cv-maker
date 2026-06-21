package com.example.nativeapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CvDao {
    @Query("SELECT * FROM cv_drafts ORDER BY lastUpdated DESC")
    fun getAllDrafts(): Flow<List<CvEntity>>

    @Query("SELECT * FROM cv_drafts WHERE id = :id")
    suspend fun getDraftById(id: String): CvEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: CvEntity)

    @Update
    suspend fun updateDraft(draft: CvEntity)

    @Delete
    suspend fun deleteDraft(draft: CvEntity)
}
