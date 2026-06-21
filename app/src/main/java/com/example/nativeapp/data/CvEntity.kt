package com.example.nativeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cv_drafts")
data class CvEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled CV",
    val lastUpdated: Long = System.currentTimeMillis(),
    val personalJson: String = "{}",
    val experiencesJson: String = "[]",
    val educationsJson: String = "[]",
    val skillsJson: String = "[]",
    val languagesJson: String = "[]",
    val certificationsJson: String = "[]",
    val organizationsJson: String = "[]",
    val hobbiesJson: String = "[]",
    val templateId: String = "modern",
    val accentColor: String = "#06b6d4"
)
