package com.example.screennotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.screennotes.data.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    notes: List<Note>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Notes") },
                actions = { TextButton(onClick = onSettings) { Text("Settings") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Text("+") },
                text = { Text("New note") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (notes.isEmpty()) {
                item {
                    Text(
                        if (query.isBlank()) {
                            "No notes yet. Use the floating button, or select text in any app and choose Save to Screen Notes."
                        } else {
                            "No matching notes."
                        }
                    )
                }
            }
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onOpen = { onOpen(note.id) },
                    onTogglePin = { onTogglePin(note) },
                )
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onOpen: () -> Unit, onTogglePin: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.pinned) {
                    Text("Pinned", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${timeFormat.format(Instant.ofEpochMilli(note.updatedAt))} · ${note.source}",
                style = MaterialTheme.typography.labelSmall,
            )
            TextButton(onClick = onTogglePin) {
                Text(if (note.pinned) "Unpin" else "Pin")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var body by rememberSaveable(note.id) { mutableStateOf(note.body) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note.id == 0L) "New note" else "Edit note") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onSave(note.copy(title = title.trim().ifBlank { "Untitled" }, body = body))
                }) {
                    Text("Save")
                }
                if (note.id != 0L) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    floatingOn: Boolean,
    onFloatingChange: (Boolean) -> Unit,
    accessibilityOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating button", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Shows a bubble over other apps. Tap it to save the screen as a note.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = floatingOn, onCheckedChange = onFloatingChange)
            }

            HorizontalDivider()

            Text(
                "Screen text access: ${if (accessibilityOn) "On" else "Off"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Needed to read text from other apps. Turn on Screen Notes in Accessibility settings.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenAccessibility) {
                Text("Open accessibility settings")
            }

            HorizontalDivider()

            Text("Save selected text", style = MaterialTheme.typography.titleMedium)
            Text(
                "Select text in any app, then choose Save to Screen Notes in the selection menu. The clipboard is not used.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text("Samsung Edge Panel", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add Screen Notes to your Edge panel from Settings > Display > Edge panels. Menu names can differ slightly by One UI version.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text("Export", style = MaterialTheme.typography.titleMedium)
            Text(
                "Save all notes to a JSON file and share it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onExport) { Text("Export notes as file") }
        }
    }
}
