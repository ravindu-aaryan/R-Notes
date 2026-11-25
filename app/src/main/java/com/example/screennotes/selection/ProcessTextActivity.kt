package com.example.screennotes.selection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.screennotes.ScreenNotesApp
import kotlinx.coroutines.launch

/**
 * Handles "Save to Screen Notes" in the text-selection menu of other apps.
 * The selected text arrives directly in the intent, so the clipboard is not used.
 */
class ProcessTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selected = intent
            .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            ?.trim()

        if (selected.isNullOrEmpty()) {
            Toast.makeText(this, "Nothing selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Keep the host app's selection unchanged.
        setResult(Activity.RESULT_CANCELED)
        val source = callingPackage ?: "selection"
        val repository = (application as ScreenNotesApp).repository

        lifecycleScope.launch {
            repository.saveCapture(selected, source)
            Toast.makeText(this@ProcessTextActivity, "Saved as note", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
