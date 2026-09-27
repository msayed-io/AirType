package com.livetype.remote.core.textdiff

/**
 * Computes the edit between two text states, in the exact shape the Windows bridge
 * can execute: N backspaces (which always erase at the END of its mirror) followed
 * by one insert. That means ops are END-ANCHORED: the common SUFFIX must be re-typed,
 * so the minimal correct op is (delete: old - commonPrefix, insert: new - commonPrefix).
 *
 * For dictation and normal typing (append at the end) this collapses to tiny inserts.
 * For mid-text edits it re-types the tail — slightly more keystrokes, always correct.
 */
object TextDiff {

    /** One stream operation. [deleteCount] backspaces, then [insertText] typed. */
    data class Op(val deleteCount: Int, val insertText: String) {
        val isEmpty: Boolean get() = deleteCount == 0 && insertText.isEmpty()
    }

    /** End-anchored diff that transforms [old] into [new]. */
    fun diff(old: String, new: String): Op {
        if (old == new) return Op(0, "")
        val minLen = minOf(old.length, new.length)
        var p = 0
        while (p < minLen && old[p] == new[p]) p++
        return Op(old.length - p, new.substring(p))
    }

    /** Replays [op] on [old] exactly as the Windows mirror would (backspaces then insert). */
    fun apply(old: String, op: Op): String {
        val keep = (old.length - op.deleteCount).coerceAtLeast(0)
        return old.substring(0, keep) + op.insertText
    }

    /** True when applying [op] to [old] yields [new] — used by tests as a safety property. */
    fun isConsistent(old: String, new: String, op: Op): Boolean = apply(old, op) == new
}
