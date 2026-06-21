package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CvDao {
    @Query("SELECT * FROM cv_drafts ORDER BY lastModified DESC")
    fun getAllCvDrafts(): Flow<List<CvDraft>>

    @Query("SELECT * FROM cv_drafts WHERE id = :id LIMIT 1")
    fun getCvDraftById(id: Int): Flow<CvDraft?>

    @Query("SELECT * FROM cv_drafts WHERE id = :id LIMIT 1")
    suspend fun getCvDraftByIdDirect(id: Int): CvDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCvDraft(cvDraft: CvDraft): Long

    @Update
    suspend fun updateCvDraft(cvDraft: CvDraft)

    @Delete
    suspend fun deleteCvDraft(cvDraft: CvDraft)

    @Query("DELETE FROM cv_drafts WHERE id = :id")
    suspend fun deleteCvDraftById(id: Int)
}
