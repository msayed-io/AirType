package com.livetype.remote

import com.livetype.remote.core.textdiff.TextDiff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDiffTest {

    @Test
    fun `pure append is a tiny insert`() {
        val op = TextDiff.diff("مرحبا", "مرحبا بالعالم")
        assertEquals(0, op.deleteCount)
        assertEquals(" بالعالم", op.insertText)
    }

    @Test
    fun `pure deletion at end becomes backspaces`() {
        val op = TextDiff.diff("hello world", "hello")
        assertEquals(6, op.deleteCount)
        assertEquals("", op.insertText)
    }

    @Test
    fun `replace tail`() {
        val op = TextDiff.diff("abc XYZ", "abc QRS")
        assertEquals(3, op.deleteCount)
        assertEquals("QRS", op.insertText)
    }

    @Test
    fun `gboard style word replacement mid sentence`() {
        // Dictation often corrects an earlier word: "انا ذاهب الى" -> "انا ذاهب إلى"
        val op = TextDiff.diff("انا ذاهب الى المدرسة", "انا ذاهب إلى المدرسة")
        // Common prefix "انا ذاهب ا" keeps; tail from "لى المدرسة" is re-typed.
        assertTrue(op.deleteCount > 0)
        assertTrue(TextDiff.isConsistent("انا ذاهب الى المدرسة", "انا ذاهب إلى المدرسة", op))
    }

    @Test
    fun `identical text produces empty op`() {
        val op = TextDiff.diff("same", "same")
        assertTrue(op.isEmpty)
    }

    @Test
    fun `emoji and mixed Arabic-English survive`() {
        val old = "نص عربي Hello 🌍"
        val new = "نص عربي Hello 🌍 وأزيد كمان"
        val op = TextDiff.diff(old, new)
        assertEquals(0, op.deleteCount)
        assertEquals(" وأزيد كمان", op.insertText)
        assertTrue(TextDiff.isConsistent(old, new, op))
    }

    @Test
    fun `newline insertion`() {
        val op = TextDiff.diff("سطر أول", "سطر أول\nسطر ثانٍ")
        assertEquals(0, op.deleteCount)
        assertEquals("\nسطر ثانٍ", op.insertText)
    }

    @Test
    fun `clearing everything`() {
        val op = TextDiff.diff("كل النص هيتمسح", "")
        assertEquals("كل النص هيتمسح".length, op.deleteCount)
        assertEquals("", op.insertText)
    }

    @Test
    fun `apply is always the inverse window - randomized property`() {
        val samples = listOf(
            "" to "a",
            "a" to "",
            "abc" to "abc",
            "abc" to "abx",
            "مرحبا" to "مرحبتك",
            "line1\nline2" to "line1\nLINE2\nline3",
            "🌍🌍" to "🌍",
        )
        samples.forEach { (old, new) ->
            val op = TextDiff.diff(old, new)
            assertTrue("diff($old, $new) must transform correctly", TextDiff.isConsistent(old, new, op))
        }
    }

    @Test
    fun `deleteCount never exceeds old length`() {
        val op = TextDiff.diff("abc", "xyz")
        assertTrue(op.deleteCount <= 3)
        assertFalse(op.deleteCount < 0)
    }
}
