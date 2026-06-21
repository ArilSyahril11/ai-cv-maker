package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CvViewModel(
    application: Application,
    private val repository: CvRepository
) : AndroidViewModel(application) {

    // List of all drafts to show on dashboard
    val allDrafts: StateFlow<List<CvDraft>> = repository.allCvDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentDraft = MutableStateFlow<CvDraft?>(null)
    val currentDraft: StateFlow<CvDraft?> = _currentDraft.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAiPolishing = MutableStateFlow(false)
    val isAiPolishing: StateFlow<Boolean> = _isAiPolishing.asStateFlow()

    private val _aiPolishedResult = MutableStateFlow<String?>(null)
    val aiPolishedResult: StateFlow<String?> = _aiPolishedResult.asStateFlow()

    private val _originalTextToPolish = MutableStateFlow<String>("")
    val originalTextToPolish: StateFlow<String> = _originalTextToPolish.asStateFlow()

    private val _polishFieldType = MutableStateFlow<String>("") // "summary", "experience", "education"
    val polishFieldType: StateFlow<String> = _polishFieldType.asStateFlow()

    private val _saveFinishedEvent = MutableStateFlow<Int?>(null) // holds last saved ID
    val saveFinishedEvent: StateFlow<Int?> = _saveFinishedEvent.asStateFlow()

    private val _selectedPagePreview = MutableStateFlow(0)
    val selectedPagePreview: StateFlow<Int> = _selectedPagePreview.asStateFlow()

    private val geminiHelper = GeminiHelper()

    init {
        viewModelScope.launch {
            repository.populateSampleIfEmpty()
        }
    }

    fun selectPage(index: Int) {
        _selectedPagePreview.value = index
    }

    fun loadDraft(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedPagePreview.value = 0
            if (id == 0) {
                // Create brand new draft
                _currentDraft.value = CvDraft(
                    draftTitle = "Draft CV ${System.currentTimeMillis() % 1000}"
                )
            } else {
                val draft = repository.getCvDraftByIdDirect(id)
                _currentDraft.value = draft
            }
            _isLoading.value = false
        }
    }

    fun saveDraft() {
        val draft = _currentDraft.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val savedId = repository.saveCvDraft(draft)
            _saveFinishedEvent.value = savedId
            // reload to keep id synced
            loadDraft(savedId)
            _isLoading.value = false
        }
    }

    fun clearSaveFinishedEvent() {
        _saveFinishedEvent.value = null
    }

    fun deleteDraft(id: Int) {
        viewModelScope.launch {
            repository.deleteCvDraft(id)
        }
    }

    fun duplicateDraft(draft: CvDraft) {
        viewModelScope.launch {
            val duplicated = draft.copy(
                id = 0,
                draftTitle = "${draft.draftTitle} (Salinan)",
                lastModified = System.currentTimeMillis()
            )
            repository.saveCvDraft(duplicated)
        }
    }

    // --- FORM EDITING HELPERS ---
    fun updateDraftTitle(newTitle: String) {
        _currentDraft.value = _currentDraft.value?.copy(draftTitle = newTitle)
    }

    fun updatePersonalInfo(info: PersonalInfo) {
        _currentDraft.value = _currentDraft.value?.copy(personalInfo = info)
    }

    fun updateTemplate(templateName: String) {
        _currentDraft.value = _currentDraft.value?.copy(templateType = templateName)
    }

    // Experiences
    fun addExperience(exp: Experience) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(experiences = draft.experiences + exp)
    }

    fun updateExperience(updatedExp: Experience) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(
            experiences = draft.experiences.map { if (it.id == updatedExp.id) updatedExp else it }
        )
    }

    fun removeExperience(expId: String) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(
            experiences = draft.experiences.filter { it.id != expId }
        )
    }

    // Educations
    fun addEducation(edu: Education) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(educations = draft.educations + edu)
    }

    fun updateEducation(updatedEdu: Education) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(
            educations = draft.educations.map { if (it.id == updatedEdu.id) updatedEdu else it }
        )
    }

    fun removeEducation(eduId: String) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(
            educations = draft.educations.filter { it.id != eduId }
        )
    }

    // Skills
    fun addSkill(skill: CvSkill) {
        val draft = _currentDraft.value ?: return
        if (draft.skills.any { it.name.trim().equals(skill.name.trim(), ignoreCase = true) }) return
        _currentDraft.value = draft.copy(skills = draft.skills + skill)
    }

    fun removeSkill(skillName: String) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(
            skills = draft.skills.filter { it.name != skillName }
        )
    }

    // Languages
    fun addLanguage(lang: CvLanguage) {
        val draft = _currentDraft.value ?: return
        if (draft.languages.any { it.name.trim().equals(lang.name.trim(), ignoreCase = true) }) return
        _currentDraft.value = draft.copy(languages = draft.languages + lang)
    }

    fun removeLanguage(langName: String) {
        val draft = _currentDraft.value ?: return
        _currentDraft.value = draft.copy(
            languages = draft.languages.filter { it.name != langName }
        )
    }

    // --- AI POLISH OPERATION ---
    fun triggerAiPolish(fieldType: String, text: String) {
        if (text.isBlank()) return
        _originalTextToPolish.value = text
        _polishFieldType.value = fieldType
        _isAiPolishing.value = true
        _aiPolishedResult.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val polished = geminiHelper.polishText(fieldType, text)
            withContext(Dispatchers.Main) {
                _aiPolishedResult.value = polished
                _isAiPolishing.value = false
            }
        }
    }

    fun clearAiPolish() {
        _aiPolishedResult.value = null
        _originalTextToPolish.value = ""
        _polishFieldType.value = ""
    }

    // Factory Class
    companion object {
        fun provideFactory(
            application: Application,
            repository: CvRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CvViewModel(application, repository) as T
            }
        }
    }
}
