package com.driplab.app.data.repository

import com.driplab.app.core.database.dao.BrewNoteDao
import com.driplab.app.core.database.entity.BrewNoteEntity
import com.driplab.app.domain.model.BrewMethod
import com.driplab.app.domain.model.BrewNote
import com.driplab.app.domain.repository.BrewNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrewNoteRepositoryImpl @Inject constructor(
    private val brewNoteDao: BrewNoteDao
) : BrewNoteRepository {

    override suspend fun getNotesByMethod(method: BrewMethod?): List<BrewNote> {
        return brewNoteDao.getNotesByMethod(method?.name).map { it.toDomain() }
    }

    override suspend fun getNoteById(id: Long): BrewNote? {
        return brewNoteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun saveNote(note: BrewNote): Long {
        return brewNoteDao.insertNote(note.toEntity())
    }

    override suspend fun deleteNote(id: Long) {
        brewNoteDao.deleteNoteById(id)
    }

    override suspend fun getAllNotes(): List<BrewNote> {
        return emptyList()
    }

    fun getAllNotesFlow(): Flow<List<BrewNote>> {
        return brewNoteDao.getAllNotes().map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun BrewNoteEntity.toDomain() = BrewNote(
        id = id,
        recipeId = recipeId,
        method = try { BrewMethod.valueOf(method) } catch (_: Exception) { BrewMethod.POUR_OVER },
        brewDate = brewDate,
        totalTime = totalTime,
        acidity = acidity,
        sweetness = sweetness,
        bitterness = bitterness,
        body = body,
        notes = notes,
        photoPath = photoPath
    )

    private fun BrewNote.toEntity() = BrewNoteEntity(
        id = id,
        recipeId = recipeId,
        method = method.name,
        brewDate = brewDate,
        totalTime = totalTime,
        acidity = acidity,
        sweetness = sweetness,
        bitterness = bitterness,
        body = body,
        notes = notes,
        photoPath = photoPath
    )
}