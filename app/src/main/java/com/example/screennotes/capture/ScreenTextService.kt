package com.example.screennotes.capture

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

sealed interface CaptureResult {
    data class Text(val text: String, val source: String) : CaptureResult
    data object Empty : CaptureResult
    data object NoWindow : CaptureResult
}

/**
 * Reads the visible text of the app in front of the user through the Accessibility API.
 * No screenshots are taken and nothing is written to the clipboard.
 */
class ScreenTextService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /** Collects text from the focused application window. Overlays are not included. */
    fun captureScreenText(): CaptureResult {
        val window = windows.orEmpty()
            .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive }
        val root = window?.root ?: rootInActiveWindow ?: return CaptureResult.NoWindow

        val fragments = ArrayList<String>()
        collectText(root, fragments, depth = 0)
        val text = TextExtractor.combine(fragments)
        return if (text.isBlank()) {
            CaptureResult.Empty
        } else {
            CaptureResult.Text(text, root.packageName?.toString().orEmpty())
        }
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > MAX_DEPTH || out.size >= MAX_FRAGMENTS) return
        // Skip hidden nodes and password fields, including everything beneath them.
        if (!node.isVisibleToUser || node.isPassword) return

        val text = node.text?.toString()
        val label = if (text.isNullOrBlank()) node.contentDescription?.toString() else text
        if (!label.isNullOrBlank()) out.add(label)

        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), out, depth + 1)
        }
    }

    companion object {
        private const val MAX_DEPTH = 60
        private const val MAX_FRAGMENTS = 2_000

        @Volatile
        var instance: ScreenTextService? = null
    }
}
