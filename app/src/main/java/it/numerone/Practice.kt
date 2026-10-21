package it.numerone

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private enum class Phase { Idle, Ready, Memorizing, Recall, Done }

@Composable
fun PracticeScreen(runs: List<Run>, onRun: (Run) -> Unit) {
    var phase by rememberSaveable { mutableStateOf(Phase.Idle) }
    var number by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableLongStateOf(0L) }
    var millis by rememberSaveable { mutableLongStateOf(0L) }
    var time by rememberSaveable { mutableLongStateOf(0L) }
    var confirm by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (phase) {
                Phase.Idle -> {
                    Text("Numerone", fontFamily = Serif, fontSize = 64.sp)
                    Text("$LENGTH cifre da memorizzare nel minor tempo possibile.", color = Muted, textAlign = TextAlign.Center)
                    Pill("Inizia", Ink) { number = generateNumerone(); input = ""; phase = Phase.Ready }
                }

                Phase.Ready, Phase.Memorizing -> {
                    val ready = phase == Phase.Ready
                    Text(
                        if (ready) "Il numerone è stato generato. Effettua il rilassamento e, quando sei pronto, premi Memorizza per iniziare a memorizzare."
                        else "Memorizza il numerone, poi premi Stop.",
                        color = if (ready) Ink else Muted, fontFamily = if (ready) Serif else Sans,
                        fontSize = if (ready) 24.sp else 16.sp, textAlign = TextAlign.Center,
                    )
                    // Placeholder digits under the closed curtain, so nothing leaks before the timer starts.
                    Curtain(if (ready) "0".repeat(LENGTH) else number, open = !ready)
                    if (ready) Pill("Memorizza", Good) { start = SystemClock.elapsedRealtime(); phase = Phase.Memorizing }
                    else Pill("Stop", Ink) { millis = SystemClock.elapsedRealtime() - start; phase = Phase.Recall }
                }

                Phase.Recall -> {
                    Text("Scrivi il numerone", fontFamily = Serif, fontSize = 32.sp)
                    DigitInput(input) { input = it }
                    Pill("Invia", Ink, enabled = input.length == LENGTH) { confirm = true }
                }

                Phase.Done -> Report(Run(time, millis, number, input), runs.filter { it.time < time }) { phase = Phase.Idle }
            }
        }

        if (phase == Phase.Done) {
            val prior = runs.filter { it.time < time && it.correct }
            if (input == number && prior.all { millis < it.millis }) Confetti()
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        text = { Text("Sei sicuro che è corretto?", fontFamily = Serif, fontSize = 24.sp, color = Ink) },
        confirmButton = {
            TextButton({
                confirm = false
                time = System.currentTimeMillis()
                onRun(Run(time, millis, number, input))
                phase = Phase.Done
            }) { Text("Sì") }
        },
        dismissButton = { TextButton({ confirm = false }) { Text("No") } },
    )
}

@Composable
private fun Report(run: Run, prior: List<Run>, onNext: () -> Unit) {
    val previous = prior.lastOrNull()
    val best = prior.filter { it.correct }.minOfOrNull { it.millis }
    val record = run.correct && (best == null || run.millis < best)

    Text(
        when {
            record -> "Nuovo record!"
            run.correct -> "Tutto corretto"
            run.errors == 1 -> "1 cifra errata"
            else -> "${run.errors} cifre errate"
        },
        color = if (run.correct) Good else Bad, fontWeight = FontWeight.Bold,
    )
    Text(formatTime(run.millis), fontFamily = Serif, fontSize = 80.sp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Numerone")
        Digits(run.number) { if (run.ok(it)) Good else Bad }
        if (!run.correct) {
            Spacer(Modifier.height(8.dp))
            Label("La tua risposta")
            Digits(run.input) { if (run.ok(it)) Muted else Bad }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Sand)
        Stat("Ultimo tentativo", previous?.let { "${formatTime(it.millis)} · ${it.errors} err." }, previous?.let { run.millis - it.millis })
        HorizontalDivider(color = Sand)
        Stat("Record", best?.let { formatTime(it) }, best?.let { run.millis - it })
        HorizontalDivider(color = Sand)
    }
    Pill("Nuovo esercizio", Ink, onClick = onNext)
}

@Composable
private fun Stat(label: String, value: String?, delta: Long?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label(label)
            Text(value ?: "—", fontFamily = Serif, fontSize = 24.sp)
        }
        if (delta != null) Text(formatDelta(delta), color = if (delta < 0) Good else Bad, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Label(text: String) = Text(text.uppercase(), color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp)

@Composable
fun Pill(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) = Button(
    onClick, Modifier.fillMaxWidth().height(56.dp), enabled = enabled,
    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Beige, disabledContainerColor = Sand, disabledContentColor = Muted),
) { Text(text, fontSize = 18.sp) }

/** Number hidden behind a curtain that slides off to the right when [open]. */
@Composable
private fun Curtain(number: String, open: Boolean) {
    val reveal by animateFloatAsState(if (open) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "curtain")
    Digits(number, Modifier.drawWithContent {
        drawContent()
        val x = size.width * reveal
        drawRect(Ink, Offset(x, 0f), Size(size.width - x, size.height))
    })
}

/** Invisible text field over the digit line: typed digits show in ink, empty slots as faint zeros. */
@Composable
private fun DigitInput(value: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    Box {
        Digits(value.padEnd(LENGTH, '0')) { if (it < value.length) Ink else Sand }
        BasicTextField(
            value, { onChange(it.filter(Char::isDigit).take(LENGTH)) },
            Modifier.matchParentSize().alpha(0f).focusRequester(focus),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
        )
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

private class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val color: Color)

@Composable
private fun Confetti() {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(3500, easing = LinearEasing)) }
    val colors = listOf(Good, Bad, Ink, Color(0xFFC9A646), Sand)
    val pieces = remember {
        List(120) { Piece(Random.nextFloat(), Random.nextFloat() * 160 - 80, Random.nextFloat() * 300 + 60, Random.nextFloat() * 720 - 360, colors.random()) }
    }
    if (t.value < 1f) Canvas(Modifier.fillMaxSize()) {
        val s = t.value * 3.5f
        pieces.forEach { p ->
            val pos = Offset(size.width * p.x + p.vx * s * density, (p.vy * s + 140 * s * s - 20) * density)
            rotate(p.spin * s, pos) { drawRect(p.color, pos - Offset(6 * density, 3 * density), Size(12 * density, 6 * density)) }
        }
    }
}
