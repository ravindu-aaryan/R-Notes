package com.example.screennotes.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    /** Package name of the app the text came from, or "manual" for typed notes. */
    val source: String = "manual",
    val pinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query(
        "SELECT * FROM notes WHERE title LIKE '%' || :query || '%' " +
            "OR body LIKE '%' || :query || '%' ORDER BY pinned DESC, updatedAt DESC"
    )
    fun search(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Note>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): NoteDatabase =
            Room.databaseBuilder(context, NoteDatabase::class.java, "screen_notes.db").build()
    }
}
