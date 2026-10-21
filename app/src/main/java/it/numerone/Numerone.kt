package it.numerone

import kotlin.random.Random

const val LENGTH = 20

data class Run(val time: Long, val millis: Long, val number: String, val input: String) {
    fun ok(i: Int) = number[i] == input.getOrNull(i)
    val errors get() = number.indices.count { !ok(it) }
    val correct get() = errors == 0
}

data class Reached(val target: Long, val time: Long, val millis: Long)

data class Goals(val target: Long? = null, val reached: List<Reached> = emptyList()) {
    /** Crowns the target when [run] is correct and strictly faster; the target is then cleared. */
    fun after(run: Run) =
        if (target != null && run.correct && run.millis < target) Goals(null, reached + Reached(target, run.time, run.millis)) else this
}

const val MAX_NAME = 20

data class Settings(val name: String = "", val lang: String = "it")

/**
 * Sanitizes a typed name: only letters, spaces, apostrophes, hyphens and dots survive (no markup, quotes,
 * control or format characters), runs of spaces collapse, and the length is capped. Trailing space is kept
 * while typing; [validName] trims for storage.
 */
fun cleanName(raw: String) = raw.filter { it.isLetter() || it in " '’-." }.replace(Regex(" {2,}"), " ").trimStart().take(MAX_NAME)

/** The name to store, or null when nothing usable is left. */
fun validName(raw: String) = cleanName(raw).trim().takeIf { n -> n.any { it.isLetter() } }

/** 20 random digits whose 10 pairs (positions 0-1, 2-3, ...) are all distinct. */
fun generateNumerone(random: Random = Random.Default): String {
    while (true) {
        val s = buildString { repeat(LENGTH) { append(random.nextInt(10)) } }
        if (s.chunked(2).toSet().size == LENGTH / 2) return s
    }
}

fun formatTime(ms: Long) = "%02d:%02d.%02d".format(ms / 60000, ms / 1000 % 60, ms / 10 % 100)

fun formatDelta(ms: Long) = (if (ms < 0) "−" else "+") + "%.1fs".format(java.util.Locale.ROOT, kotlin.math.abs(ms) / 1000.0)
