package it.numerone

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private enum class Phase { Idle, Ready, Memorizing, Recall, Done }

private const val DIGIT_REVEAL_MS = 40

@Composable
fun PracticeScreen(runs: List<Run>, reached: List<Reached>, onRun: (Run) -> Unit, onGoal: () -> Unit) {
    var phase by rememberSaveable { mutableStateOf(Phase.Idle) }
    var number by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableLongStateOf(0L) }
    var millis by rememberSaveable { mutableLongStateOf(0L) }
    var time by rememberSaveable { mutableLongStateOf(0L) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var showTimer by rememberSaveable { mutableStateOf(true) }

    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(phase) { if (phase == Phase.Memorizing) while (true) withFrameMillis { now = SystemClock.elapsedRealtime() } }

    // Number of original digits revealed so far while checking; fractional part fades the next digit in.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == Phase.Done) reveal.animateTo(LENGTH.toFloat(), tween(LENGTH * DIGIT_REVEAL_MS, easing = LinearEasing))
        else reveal.snapTo(0f)
    }
    val checked = phase == Phase.Done && reveal.value >= LENGTH

    val run = Run(time, millis, number, input)
    val prior = runs.filter { it.time < time }
    val best = prior.filter { it.correct }.minOfOrNull { it.millis }
    val record = run.correct && (best == null || run.millis < best)

    fun next() {
        number = generateNumerone()
        input = ""
        phase = Phase.Ready
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Text(
                "“VIVI OGNI GIORNO COME SE FOSSE IL PRIMO”", Modifier.fillMaxWidth().padding(top = 20.dp),
                color = Muted, fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 11.sp, letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
            AnimatedContent(
                phase == Phase.Idle, Modifier.weight(1f),
                transitionSpec = { (fadeIn(tween(500, 150)) + slideInVertically(tween(500, 150)) { it / 12 }) togetherWith fadeOut(tween(200)) },
                label = "start",
            ) { idle ->
                if (idle) Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text("Numerone", fontFamily = Serif, fontSize = 64.sp)
                    Text("$LENGTH cifre da memorizzare nel minor tempo possibile.", color = Muted, textAlign = TextAlign.Center)
                }
                else Box(Modifier.fillMaxSize(), Alignment.Center) { Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val elapsed = when (phase) {
                        Phase.Ready -> 0L
                        Phase.Memorizing -> (now - start).coerceAtLeast(0)
                        else -> millis
                    }
                    Timer(elapsed, showTimer || phase == Phase.Done, if (phase == Phase.Done) null else ({ showTimer = !showTimer }))
                    Spacer(Modifier.height(48.dp))
                    AnimatedContent(
                        phase >= Phase.Recall,
                        transitionSpec = {
                            (slideInHorizontally(tween(450)) { it } + fadeIn(tween(450))) togetherWith
                                (slideOutHorizontally(tween(450)) { -it } + fadeOut(tween(450)))
                        },
                        label = "recall",
                    ) { recall ->
                        when {
                            !recall -> Memorize(number, phase == Phase.Ready)
                            phase == Phase.Recall -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Label("Scrivi il numerone")
                                DigitInput(input) { input = it }
                            }
                            else -> Check(run, reveal.value, checked, record, prior.lastOrNull(), best)
                        }
                    }
                } }
            }

            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp).height(56.dp)) {
                AnimatedContent(phase to checked, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "button") { (p, c) ->
                    when (p) {
                        Phase.Idle -> Pill("Inizia", Ink, onClick = ::next)
                        Phase.Ready -> Pill("Memorizza", Good) { start = SystemClock.elapsedRealtime(); phase = Phase.Memorizing }
                        Phase.Memorizing -> Pill("Stop", Ink) { millis = SystemClock.elapsedRealtime() - start; phase = Phase.Recall }
                        Phase.Recall -> Pill("Invia", Ink, enabled = input.length == LENGTH) { confirm = true }
                        Phase.Done -> if (c) {
                            if (run.correct) Pill("Vai oltre!", Good, onClick = ::next) else Pill("Riprova!", Ink, onClick = ::next)
                        }
                    }
                }
            }
        }

        val goal = reached.firstOrNull { it.time == time }
        if (checked && goal != null) GoalCelebration(goal.target) {
            phase = Phase.Idle
            onGoal()
        } else if (checked && record) Confetti(listOf(Good, Bad, Ink, Gold, Sand))
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

/** Big timer with an eye toggle; hiding fades and blurs it away. [onToggle] null hides the eye. */
@Composable
private fun Timer(ms: Long, visible: Boolean, onToggle: (() -> Unit)?) {
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "timer")
    Row(
        Modifier.clip(CircleShape).then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label("Timer")
        if (onToggle != null) Icon(
            if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
            if (visible) "Nascondi timer" else "Mostra timer",
            Modifier.padding(start = 8.dp).size(16.dp), tint = Muted,
        )
    }
    // Fixed-width cells so the proportional serif digits don't jitter while running.
    val cell = with(LocalDensity.current) { 64.sp.toDp() * 0.6f }
    Row(Modifier.alpha(shown).blur(16.dp * (1 - shown), BlurredEdgeTreatment.Unbounded)) {
        formatTime(ms).forEach {
            Text(it.toString(), Modifier.width(if (it.isDigit()) cell else cell * 0.45f), fontFamily = Serif, fontSize = 64.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Number blurred out until [ready] turns false, then it sharpens into view. */
@Composable
private fun Memorize(number: String, ready: Boolean) {
    val blur by animateDpAsState(if (ready) 14.dp else 0.dp, tween(700), label = "blur")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        // Placeholder digits while blurred, so nothing leaks before the timer starts (or where blur is unsupported).
        Digits(if (ready) "0".repeat(LENGTH) else number, Modifier.blur(blur, BlurredEdgeTreatment.Unbounded), align = Alignment.Center)
        Crossfade(ready, label = "hint") {
            Text(
                if (it) "Il numerone è stato generato. Effettua il rilassamento e, quando sei pronto, premi Memorizza per iniziare."
                else "Memorizza il numerone, poi premi Stop.",
                color = Muted, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

/** Original digits appear one by one above the answer, both colored as they are checked; then the verdict. */
@Composable
private fun Check(run: Run, reveal: Float, checked: Boolean, record: Boolean, previous: Run?, best: Long?) {
    fun progress(i: Int) = (reveal - i).coerceIn(0f, 1f)
    fun verdict(i: Int) = if (run.ok(i)) Good else Bad
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label("Numerone")
        Digits(run.number, align = Alignment.Center) { verdict(it).copy(alpha = progress(it)) }
        Spacer(Modifier.height(8.dp))
        Label("La tua risposta")
        Digits(run.input, align = Alignment.Center) { lerp(Ink, verdict(it), progress(it)) }

        AnimatedVisibility(checked, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 6 }) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (run.correct) "Bravo!" else "Peccato!", color = if (run.correct) Good else Bad, fontFamily = Serif, fontSize = 56.sp)
                Text(
                    when {
                        record -> "Nuovo record!"
                        run.correct -> "Tutte le cifre corrette"
                        run.errors == 1 -> "1 cifra errata"
                        else -> "${run.errors} cifre errate"
                    },
                    color = Muted,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Sand)
                Stat("Ultimo tentativo", previous?.millis, previous?.let { " · ${it.errors} err." }, previous?.let { run.millis - it.millis })
                HorizontalDivider(color = Sand)
                Stat("Record", best, null, best?.let { run.millis - it })
                HorizontalDivider(color = Sand)
            }
        }
    }
}

@Composable
private fun Stat(label: String, millis: Long?, extra: String?, delta: Long?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label(label)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = Serif, fontSize = 24.sp)) { append(millis?.let(::formatTime) ?: "—") }
                    extra?.let(::append)
                },
                fontSize = 16.sp,
            )
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

/** Invisible text field over the digit line: typed digits show in ink, empty slots as faint zeros. */
@Composable
private fun DigitInput(value: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    Box {
        Digits(value.padEnd(LENGTH, '0'), align = Alignment.Center) { if (it < value.length) Ink else Sand }
        // Transparent selection colors hide the cursor handle, which is drawn in a popup and ignores alpha.
        CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(Color.Transparent, Color.Transparent)) {
            BasicTextField(
                value, { onChange(it.filter(Char::isDigit).take(LENGTH)) },
                Modifier.matchParentSize().alpha(0f).focusRequester(focus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
            )
        }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
}

private class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val color: Color)

@Composable
private fun Confetti(colors: List<Color>, count: Int = 120) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(3500, easing = LinearEasing)) }
    val pieces = remember {
        List(count) { Piece(Random.nextFloat(), Random.nextFloat() * 160 - 80, Random.nextFloat() * 300 + 60, Random.nextFloat() * 720 - 360, colors.random()) }
    }
    if (t.value < 1f) Canvas(Modifier.fillMaxSize()) {
        val s = t.value * 3.5f
        pieces.forEach { p ->
            val pos = Offset(size.width * p.x + p.vx * s * density, (p.vy * s + 140 * s * s - 20) * density)
            rotate(p.spin * s, pos) { drawRect(p.color, pos - Offset(6 * density, 3 * density), Size(12 * density, 6 * density)) }
        }
    }
}

/** The biggest celebration: golden overlay, gold confetti and rising claps. Tap or wait to continue. */
@Composable
private fun GoalCelebration(target: Long, onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f)) }
    LaunchedEffect(Unit) {
        delay(6500)
        onDone()
    }
    val brush = goldBrush()
    Box(
        Modifier.fillMaxSize()
            .background(Beige.copy(alpha = 0.97f))
            .background(Brush.radialGradient(listOf(GoldLight.copy(alpha = 0.6f), Color.Transparent)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Claps()
        Column(
            Modifier.padding(24.dp).graphicsLayer {
                scaleX = 0.5f + 0.5f * appear.value
                scaleY = scaleX
                alpha = appear.value.coerceIn(0f, 1f)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.EmojiEvents, null, Modifier.size(88.dp).gold(brush))
            Text(
                "HAI RAGGIUNTO\nIL TUO OBIETTIVO!", style = TextStyle(brush = brush),
                fontFamily = Serif, fontSize = 40.sp, lineHeight = 46.sp, textAlign = TextAlign.Center,
            )
            Text(formatTime(target), style = TextStyle(brush = brush), fontFamily = Serif, fontSize = 34.sp)
            Text("Fatti un bel punto 7", Modifier.padding(top = 8.dp), color = Ink, fontSize = 20.sp)
        }
        Confetti(listOf(Gold, GoldLight, GoldDark, Color(0xFFFFF4C9)), count = 240)
    }
}

private class Clap(val x: Float, val delay: Float, val size: Int, val phase: Float)

@Composable
private fun Claps() {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(6500, easing = LinearEasing)) }
    val claps = remember { List(28) { Clap(Random.nextFloat() * 0.85f, Random.nextFloat() * 0.65f, 28 + Random.nextInt(32), Random.nextFloat() * 6.3f) } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        claps.forEach { c ->
            val p = (t.value - c.delay) / 0.35f // each clap rises over 35% of the show
            if (p in 0f..1f) Text(
                "👏",
                Modifier
                    .offset { IntOffset((c.x * w + sin(p * 7 + c.phase) * 24.dp.toPx()).toInt(), (h * (1.05f - 1.2f * p)).toInt()) }
                    .graphicsLayer { alpha = 1 - p * p; rotationZ = sin(p * 12 + c.phase) * 15 },
                fontSize = c.size.sp,
            )
        }
    }
}
