package com.example.screennotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.screennotes.ScreenNotesApp
import com.example.screennotes.data.Note
import com.example.screennotes.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val notes: StateFlow<List<Note>> = _query
        .flatMapLatest { repo.observeNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(text: String) {
        _query.value = text
    }

    suspend fun load(id: Long): Note? = repo.get(id)

    suspend fun exportAll(): List<Note> = repo.all()

    fun save(note: Note, onSaved: () -> Unit) {
        viewModelScope.launch {
            repo.save(note)
            onSaved()
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repo.save(note.copy(pinned = !note.pinned)) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ScreenNotesApp
                NotesViewModel(app.repository)
            }
        }
    }
}
