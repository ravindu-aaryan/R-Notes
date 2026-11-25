package com.example.screennotes.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextExtractorTest {

    @Test
    fun dropsBlankAndConsecutiveDuplicateFragments() {
        val result = TextExtractor.combine(listOf("  Hello  ", "", "Hello", "World\n", "  "))
        assertEquals("Hello\nWorld", result)
    }

    @Test
    fun collapsesInternalWhitespace() {
        assertEquals("a b c", TextExtractor.combine(listOf("a   \t b\n\nc")))
    }

    @Test
    fun capsTheTotalLength() {
        val big = List(20_000) { "line $it" }
        assertTrue(TextExtractor.combine(big).length <= TextExtractor.MAX_CHARS)
    }
}
