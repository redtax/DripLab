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
    val selectedMethod: BrewMethod = BrewMethod.POUR_OVER,
    val showEditDialog: Boolean = false,
    val editingNote: BrewNote? = null
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
            val method = _uiState.value.selectedMethod
            val notes = brewNoteRepository.getNotesByMethod(method)
            _uiState.value = _uiState.value.copy(notes = notes)
        }
    }

    fun selectMethod(method: BrewMethod) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
        loadNotes()
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            brewNoteRepository.deleteNote(noteId)
            loadNotes()
        }
    }

    fun showEditDialog(note: BrewNote) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingNote = note)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingNote = null)
    }

    fun saveEditedNote(note: BrewNote) {
        viewModelScope.launch {
            brewNoteRepository.updateNote(note)
            _uiState.value = _uiState.value.copy(showEditDialog = false, editingNote = null)
            loadNotes()
        }
    }
}