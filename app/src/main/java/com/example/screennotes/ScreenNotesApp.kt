package com.example.screennotes

import android.app.Application
import com.example.screennotes.data.NoteDatabase
import com.example.screennotes.data.NoteRepository

class ScreenNotesApp : Application() {
    private val database by lazy { NoteDatabase.create(this) }

    val repository: NoteRepository by lazy { NoteRepository(database.noteDao()) }
}
