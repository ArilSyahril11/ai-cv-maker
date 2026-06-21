package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class PersonalInfo(
    val fullName: String = "",
    val title: String = "",
    val profilePhotoUri: String? = null,
    val profilePhotoShape: String = "circle",
    val profilePhotoScale: Float = 1.0f,
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedin: String = "",
    val behance: String = "",
    val dribbble: String = "",
    val github: String = "",
    val tiktok: String = "",
    val instagram: String = "",
    val youtube: String = "",
    val summary: String = ""
)

@JsonClass(generateAdapter = true)
data class Experience(
    val id: String = UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val startDate: String = "",
    val endDate: String = "", // e.g. "2024" or "Present"
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class Education(
    val id: String = UUID.randomUUID().toString(),
    val institution: String = "",
    val major: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class CvSkill(
    val name: String = "",
    val level: String = "" // e.g. "Pemula", "Menengah", "Ahli"
)

@JsonClass(generateAdapter = true)
data class CvLanguage(
    val name: String = "",
    val proficiency: String = "" // e.g. "Native", "Aktif", "Pasif"
)

@JsonClass(generateAdapter = true)
data class CvCertification(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val issuer: String = "",
    val year: String = ""
)

@JsonClass(generateAdapter = true)
data class CvOrganization(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val role: String = "",
    val period: String = ""
)

@JsonClass(generateAdapter = true)
data class CvHobby(
    val name: String = ""
)

enum class TemplateType(val displayName: String) {
    MODERN_TECH("Modern Tech"),
    ELEGANT_MINIMAL("Elegant Minimal"),
    CREATIVE_COLOR("Creative Colorful"),
    EXECUTIVE_CLASSIC("Executive Classic"),
    PROFESSIONAL_CLEAN("Professional Clean"),
    SIMPLE_MINIMALIST("Simple Minimalist"),
    BOLD_TYPOGRAPHY("Bold Typography"),
    CORPORATE_STANDARD("Corporate Standard"),
    CREATIVE_PORTFOLIO("Creative Portfolio"),
    ACADEMIC_SCHOLAR("Academic Scholar")
}

@Entity(tableName = "cv_drafts")
@JsonClass(generateAdapter = true)
data class CvDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val draftTitle: String = "Draft CV Baru",
    val lastModified: Long = System.currentTimeMillis(),
    val personalInfo: PersonalInfo = PersonalInfo(),
    val experiences: List<Experience> = emptyList(),
    val educations: List<Education> = emptyList(),
    val skills: List<CvSkill> = emptyList(),
    val languages: List<CvLanguage> = emptyList(),
    val certifications: List<CvCertification> = emptyList(),
    val organizations: List<CvOrganization> = emptyList(),
    val hobbies: List<CvHobby> = emptyList(),
    val templateType: String = TemplateType.MODERN_TECH.name
)
