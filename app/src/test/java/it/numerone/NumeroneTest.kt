package it.numerone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NumeroneTest {
    @Test
    fun generatedNumbersHaveUniquePairs() {
        val random = Random(42)
        repeat(1000) {
            val n = generateNumerone(random)
            assertEquals(LENGTH, n.length)
            assertTrue(n.all(Char::isDigit))
            assertEquals(LENGTH / 2, n.chunked(2).toSet().size)
        }
    }

    @Test
    fun errorsAreCountedPerDigit() {
        val n = "12345678901234567890"
        assertTrue(Run(0, 0, n, n).correct)
        val wrong = Run(0, 0, n, "12345678901234567809")
        assertEquals(2, wrong.errors)
        assertFalse(wrong.ok(18))
        assertEquals(20, Run(0, 0, n, "").errors)
    }

    @Test
    fun formatsTime() {
        assertEquals("01:02.34", formatTime(62_345))
        assertEquals("−1.5s", formatDelta(-1_500))
    }
}
