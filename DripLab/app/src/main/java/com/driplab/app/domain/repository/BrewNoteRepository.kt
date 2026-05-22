package com.driplab.app.domain.repository

import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewNote

interface BrewNoteRepository {
    suspend fun getNotesByMethod(method: BrewMethod?): List<BrewNote>
    suspend fun getNoteById(id: Long): BrewNote?
    suspend fun saveNote(note: BrewNote): Long
    suspend fun updateNote(note: BrewNote)
    suspend fun deleteNote(id: Long)
    suspend fun getAllNotes(): List<BrewNote>
}