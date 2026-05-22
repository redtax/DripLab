package com.driplab.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.driplab.app.core.database.entity.BrewNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewNoteDao {
    @Query("SELECT * FROM brew_notes WHERE method = :method OR :method IS NULL ORDER BY brewDate DESC")
    suspend fun getNotesByMethod(method: String?): List<BrewNoteEntity>

    @Query("SELECT * FROM brew_notes ORDER BY brewDate DESC")
    fun getAllNotes(): Flow<List<BrewNoteEntity>>

    @Query("SELECT * FROM brew_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): BrewNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BrewNoteEntity): Long

    @androidx.room.Update
    suspend fun updateNote(note: BrewNoteEntity)

    @Query("DELETE FROM brew_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}