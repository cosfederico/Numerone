package it.numerone

import kotlin.random.Random

const val LENGTH = 20

data class Run(val time: Long, val millis: Long, val number: String, val input: String) {
    fun ok(i: Int) = number[i] == input.getOrNull(i)
    val errors get() = number.indices.count { !ok(it) }
    val correct get() = errors == 0
}

/** 20 random digits whose 10 pairs (positions 0-1, 2-3, ...) are all distinct. */
fun generateNumerone(random: Random = Random.Default): String {
    while (true) {
        val s = buildString { repeat(LENGTH) { append(random.nextInt(10)) } }
        if (s.chunked(2).toSet().size == LENGTH / 2) return s
    }
}

fun formatTime(ms: Long) = "%02d:%02d.%02d".format(ms / 60000, ms / 1000 % 60, ms / 10 % 100)

fun formatDelta(ms: Long) = (if (ms < 0) "−" else "+") + "%.1fs".format(java.util.Locale.ROOT, kotlin.math.abs(ms) / 1000.0)
