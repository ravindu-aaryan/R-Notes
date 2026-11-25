package com.example.screennotes.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    fun observeNotes(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    suspend fun get(id: Long): Note? = dao.getById(id)

    suspend fun all(): List<Note> = dao.getAll()

    suspend fun save(note: Note): Long {
        val now = System.currentTimeMillis()
        return if (note.id == 0L) {
            dao.insert(note.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(note.copy(updatedAt = now))
            note.id
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** Saves text captured from the screen or from a text selection as a new note. */
    suspend fun saveCapture(text: String, source: String): Long {
        val clean = text.trim()
        val title = clean.lineSequence().firstOrNull()?.trim()?.take(60).orEmpty()
            .ifBlank { "Untitled" }
        return save(Note(title = title, body = clean, source = source))
    }
}
