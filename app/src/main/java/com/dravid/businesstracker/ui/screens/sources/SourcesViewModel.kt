package com.dravid.businesstracker.ui.screens.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dravid.businesstracker.data.model.SourceEntity
import com.dravid.businesstracker.data.model.SourceWithAnalytics
import com.dravid.businesstracker.data.model.UnassignedSourceAnalytics
import com.dravid.businesstracker.data.repository.SourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourcesUiState(
    val sources: List<SourceWithAnalytics> = emptyList(),
    val unassignedAnalytics: UnassignedSourceAnalytics? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Add / Edit Dialog
    val isFormOpen: Boolean = false,
    val editingSourceId: Long? = null,
    val formName: String = "",
    val formNotes: String = "",
    val formValidationMessage: String? = null,
    // Delete Confirmation Dialog
    val sourceToDelete: SourceEntity? = null,
)

class SourcesViewModel(
    private val repository: SourceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState(isLoading = true))
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getSourcesWithAnalytics().collect { sourcesWithAnalytics ->
                _uiState.update { current ->
                    current.copy(
                        sources = sourcesWithAnalytics,
                        isLoading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.getUnassignedAnalytics().collect { unassigned ->
                _uiState.update { current ->
                    current.copy(unassignedAnalytics = unassigned)
                }
            }
        }
    }

    fun openAddForm() {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingSourceId = null,
                formName = "",
                formNotes = "",
                formValidationMessage = null,
            )
        }
    }

    fun openEditForm(source: SourceEntity) {
        _uiState.update {
            it.copy(
                isFormOpen = true,
                editingSourceId = source.id,
                formName = source.name,
                formNotes = source.notes ?: "",
                formValidationMessage = null,
            )
        }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                formValidationMessage = null,
            )
        }
    }

    fun updateFormName(name: String) {
        _uiState.update { it.copy(formName = name, formValidationMessage = null) }
    }

    fun updateFormNotes(notes: String) {
        _uiState.update { it.copy(formNotes = notes, formValidationMessage = null) }
    }

    fun saveSource() {
        val current = _uiState.value
        val trimmedName = current.formName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(formValidationMessage = "Source name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            val validation = repository.validateSource(trimmedName, current.editingSourceId)
            if (!validation.isSuccess) {
                _uiState.update { it.copy(formValidationMessage = validation.errorMessage) }
                return@launch
            }

            val result = if (current.editingSourceId == null) {
                repository.insertSource(
                    SourceEntity(
                        name = trimmedName,
                        notes = current.formNotes.trim().ifEmpty { null },
                    ),
                )
            } else {
                repository.updateSource(
                    SourceEntity(
                        id = current.editingSourceId,
                        name = trimmedName,
                        notes = current.formNotes.trim().ifEmpty { null },
                    ),
                )
            }

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isFormOpen = false,
                        formValidationMessage = null,
                        successMessage = if (current.editingSourceId == null) {
                            "Source '${trimmedName}' created successfully."
                        } else {
                            "Source '${trimmedName}' updated successfully."
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        formValidationMessage = result.exceptionOrNull()?.message
                            ?: "Failed to save source.",
                    )
                }
            }
        }
    }

    fun requestDelete(source: SourceEntity) {
        _uiState.update { it.copy(sourceToDelete = source) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(sourceToDelete = null) }
    }

    fun confirmDelete() {
        val source = _uiState.value.sourceToDelete ?: return
        viewModelScope.launch {
            val result = repository.deleteSource(source.id)
            _uiState.update { current ->
                current.copy(
                    sourceToDelete = null,
                    successMessage = if (result.isSuccess) "Source '${source.name}' deleted." else null,
                    errorMessage = if (result.isFailure) "Failed to delete source." else null,
                )
            }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
