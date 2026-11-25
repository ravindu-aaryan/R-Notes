package com.example.screennotes.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.screennotes.data.Note
import com.example.screennotes.data.NoteExporter
import com.example.screennotes.overlay.FloatingBubbleService
import com.example.screennotes.util.isScreenTextAccessEnabled
import com.example.screennotes.util.openAccessibilitySettings
import com.example.screennotes.util.openOverlaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val NEW_NOTE_ID = 0L

enum class Route { List, Editor, Settings }

@Composable
fun ScreenNotesTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun AppRoot(vm: NotesViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val floatingOn by FloatingBubbleService.running.collectAsStateWithLifecycle()
    val resumeTick = rememberResumeTick()

    var route by rememberSaveable { mutableStateOf(Route.List) }
    var editingId by rememberSaveable { mutableStateOf(NEW_NOTE_ID) }

    BackHandler(enabled = route != Route.List) { route = Route.List }

    when (route) {
        Route.List -> NotesListScreen(
            notes = notes,
            query = query,
            onQueryChange = vm::setQuery,
            onOpen = { id ->
                editingId = id
                route = Route.Editor
            },
            onNew = {
                editingId = NEW_NOTE_ID
                route = Route.Editor
            },
            onTogglePin = vm::togglePin,
            onSettings = { route = Route.Settings },
        )

        Route.Editor -> NoteEditorRoute(
            vm = vm,
            id = editingId,
            onDone = { route = Route.List },
        )

        Route.Settings -> SettingsScreen(
            floatingOn = floatingOn,
            onFloatingChange = { on -> handleFloatingToggle(context, on) },
            accessibilityOn = remember(resumeTick) { isScreenTextAccessEnabled(context) },
            onOpenAccessibility = { openAccessibilitySettings(context) },
            onExport = {
                scope.launch { shareNotes(context, vm.exportAll()) }
            },
            onBack = { route = Route.List },
        )
    }
}

@Composable
private fun NoteEditorRoute(vm: NotesViewModel, id: Long, onDone: () -> Unit) {
    val note by produceState<Note?>(initialValue = null, id) {
        value = if (id == NEW_NOTE_ID) Note() else vm.load(id)
    }
    val current = note
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    NoteEditorScreen(
        note = current,
        onSave = { edited -> vm.save(edited) { onDone() } },
        onDelete = { vm.delete(current.id, onDone) },
        onBack = onDone,
    )
}

private fun handleFloatingToggle(context: Context, on: Boolean) {
    when {
        !on -> FloatingBubbleService.stop(context)
        !Settings.canDrawOverlays(context) -> openOverlaySettings(context)
        else -> FloatingBubbleService.start(context)
    }
}

private suspend fun shareNotes(context: Context, notes: List<Note>) {
    val uri = withContext(Dispatchers.IO) { NoteExporter.writeExport(context, notes) }
    context.startActivity(Intent.createChooser(NoteExporter.shareIntent(uri), "Share notes"))
}

/** Increments each time the screen returns to the foreground, so permission checks refresh. */
@Composable
private fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}
