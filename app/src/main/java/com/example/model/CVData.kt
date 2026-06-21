package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CVData(
    val id: String,
    val title: String,
    val lastModified: Long,
    val template: String,
    val accentColor: String? = "#06b6d4",
    val personal: PersonalData = PersonalData(),
    val experiences: List<ExperienceData> = emptyList(),
    val educations: List<EducationData> = emptyList(),
    val skills: List<SkillData> = emptyList(),
    val languages: List<LanguageData> = emptyList(),
    val certifications: List<CertificationData> = emptyList(),
    val organizations: List<OrganizationData> = emptyList(),
    val hobbies: List<HobbyData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PersonalData(
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
data class ExperienceData(
    val id: String,
    val title: String,
    val company: String,
    val startDate: String,
    val endDate: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class EducationData(
    val id: String,
    val institution: String,
    val major: String,
    val startDate: String,
    val endDate: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class SkillData(
    val name: String,
    val level: String
)

@JsonClass(generateAdapter = true)
data class LanguageData(
    val name: String,
    val proficiency: String
)

@JsonClass(generateAdapter = true)
data class CertificationData(
    val id: String,
    val name: String,
    val issuer: String,
    val year: String
)

@JsonClass(generateAdapter = true)
data class OrganizationData(
    val id: String,
    val name: String,
    val role: String,
    val period: String
)

@JsonClass(generateAdapter = true)
data class HobbyData(
    val name: String
)
