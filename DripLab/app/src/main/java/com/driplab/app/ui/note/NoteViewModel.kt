package com.driplab.app.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.data.repository.BrewNoteRepositoryImpl
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteUiState(
    val notes: List<BrewNote> = emptyList(),
    val selectedMethod: BrewMethod? = null,
    val isLoading: Boolean = true,
    val showDetailDialog: Boolean = false,
    val selectedNote: BrewNote? = null
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val brewNoteRepository: BrewNoteRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            brewNoteRepository.getAllNotesFlow().collect { notes ->
                _uiState.value = _uiState.value.copy(
                    notes = notes,
                    isLoading = false
                )
            }
        }
    }

    fun filterByMethod(method: BrewMethod?) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
    }

    fun deleteNote(note: BrewNote) {
        viewModelScope.launch {
            brewNoteRepository.deleteNote(note.id)
        }
    }

    fun showDetail(note: BrewNote) {
        _uiState.value = _uiState.value.copy(
            showDetailDialog = true,
            selectedNote = note
        )
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(
            showDetailDialog = false,
            selectedNote = null
        )
    }

    fun getFilteredNotes(): List<BrewNote> {
        val state = _uiState.value
        return if (state.selectedMethod != null) {
            state.notes.filter { it.method == state.selectedMethod }
        } else {
            state.notes
        }
    }
}