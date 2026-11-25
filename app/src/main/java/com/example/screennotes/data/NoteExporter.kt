package com.example.screennotes.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Writes all notes to a JSON file in the app cache and builds a share intent for it. */
object NoteExporter {

    fun writeExport(context: Context, notes: List<Note>): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val file = File(dir, "screen-notes-$stamp.json")

        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("title", note.title)
                    .put("body", note.body)
                    .put("source", note.source)
                    .put("pinned", note.pinned)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt)
            )
        }
        val root = JSONObject()
            .put("format", "screen-notes")
            .put("version", 1)
            .put("exportedAt", Instant.now().toString())
            .put("count", notes.size)
            .put("notes", array)
        file.writeText(root.toString(2))

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
