package it.numerone

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val mottos = listOf(
    "Punta in alto!", "Datti un obiettivo cazzuto!", "Non accettare compromessi!",
    "Alza l'asticella!", "Niente scuse!", "Osa di più!", "Distruggi la tua bambagia!",
    "Il te del futuro ti ringrazierà.",
)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.ITALIAN)
private fun Long.formatDate(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(dateTimeFormat)

@Composable
fun GoalScreen(runs: List<Run>, goals: Goals, onTarget: (Long?) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val best = runs.filter { it.correct }.minByOrNull { it.millis }

    LazyColumn(contentPadding = PaddingValues(24.dp, 0.dp, 24.dp, 24.dp)) {
        item {
            Text("Obiettivo", Modifier.padding(top = 24.dp, bottom = 24.dp), fontFamily = Serif, fontSize = 40.sp)

            // Dashed gold frame and hollow numbers: an empty slot still to be filled. Solid gold is for reached goals.
            val stroke = with(LocalDensity.current) { 1.5.dp.toPx() }
            Column(
                Modifier.fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            Gold, cornerRadius = CornerRadius(24.dp.toPx()),
                            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                        )
                    }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", Modifier.padding(end = 6.dp), fontSize = 13.sp)
                    Text("OBIETTIVO", color = Gold, fontSize = 12.sp, letterSpacing = 1.5.sp)
                }
                val target = goals.target
                if (target == null) {
                    Text("Nessun obiettivo impostato.", Modifier.padding(top = 8.dp, bottom = 24.dp), color = Muted)
                    Glowing { Pill("Imposta obiettivo", Gold) { editing = true } }
                } else {
                    Text(formatTime(target), color = Gold, fontFamily = Serif, fontSize = 64.sp, style = TextStyle(drawStyle = Stroke(stroke)))
                    if (best != null) Text(
                        if (best.millis >= target) "Ti mancano ${formatDelta(best.millis - target).drop(1)} dal record."
                        else "Il tuo record è già sotto: rifai un tempo sotto l'obiettivo per raggiungerlo.",
                        color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                    )
                    Row(Modifier.padding(top = 8.dp)) {
                        TextButton({ editing = true }) { Text("Modifica", color = Ink) }
                        TextButton({ onTarget(null) }) { Text("Rimuovi", color = Muted) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val card = RoundedCornerShape(24.dp)
            Column(
                Modifier.fillMaxWidth().background(Paper, card).border(1.dp, Sand, card).padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏁", Modifier.padding(end = 6.dp), fontSize = 13.sp)
                    Label("Record")
                }
                Text(best?.let { formatTime(it.millis) } ?: "—", fontFamily = Serif, fontSize = 64.sp)
                if (best == null) {
                    Text(
                        "Il passo più importante di tutti ti aspetta, il prossimo", Modifier.padding(horizontal = 24.dp),
                        color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                    )
                } else {
                    Text(best.time.formatDate(), color = Muted, fontSize = 14.sp)
                    Text("Pensi di riuscire a batterlo?", Modifier.padding(top = 12.dp), color = Ink, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("Obiettivi raggiunti")
                Spacer(Modifier.weight(1f))
                Text("${goals.reached.size}", color = Muted, fontSize = 13.sp)
            }
            if (goals.reached.isEmpty()) Text("Ancora nessun obiettivo raggiunto.", Modifier.padding(vertical = 12.dp), color = Muted, fontSize = 14.sp)
        }

        items(goals.reached.asReversed(), key = { it.time }) { r ->
            val brush = goldBrush()
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, null, Modifier.size(28.dp).gold(brush))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(formatTime(r.target), style = TextStyle(brush = brush), fontFamily = Serif, fontSize = 36.sp)
                    Text("con ${formatTime(r.millis)}", color = Muted, fontSize = 13.sp)
                }
                Text(r.time.formatDate(), fontSize = 13.sp, color = Muted)
            }
            HorizontalDivider(color = GoldLight.copy(alpha = 0.6f))
        }
    }

    if (editing) TargetDialog(goals.target, best?.millis, onDismiss = { editing = false }) {
        onTarget(it)
        editing = false
    }
}

/** A goal slower than the [record] would already be beaten, so it can't be saved. */
@Composable
private fun TargetDialog(current: Long?, record: Long?, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var text by rememberSaveable { mutableStateOf(current?.let { "%.2f".format(Locale.ROOT, it / 1000.0) } ?: "") }
    val motto = rememberSaveable { mottos.random() }
    val millis = text.replace(',', '.').toDoubleOrNull()?.times(1000)?.toLong()?.takeIf { it > 0 }
    val beaten = millis != null && record != null && millis > record
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Obiettivo", fontFamily = Serif, fontSize = 28.sp, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(motto, color = Muted, fontSize = 14.sp)
                OutlinedTextField(
                    text, { text = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    singleLine = true, suffix = { Text("s") },
                    isError = beaten,
                    supportingText = {
                        if (beaten) Text("Il tuo record ${formatTime(record!!)} è già sotto: punta più in alto!")
                        else millis?.let { Text(formatTime(it)) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = { TextButton({ millis?.let(onSave) }, enabled = millis != null && !beaten) { Text("Salva") } },
        dismissButton = { TextButton(onDismiss) { Text("Annulla") } },
    )
}

/** Soft blurred gold glow breathing around the pill-shaped [content]. */
@Composable
private fun Glowing(content: @Composable () -> Unit) {
    val breath by rememberInfiniteTransition(label = "glow").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    Box(
        Modifier.drawBehind {
            val spread = (2 + 3 * breath).dp.toPx()
            val paint = Paint().apply { color = Gold.copy(alpha = 0.12f + 0.18f * breath) }
            // ponytail: BlurMaskFilter is drawn only on API 28+ (hardware canvas); older devices just skip the glow.
            paint.asFrameworkPaint().maskFilter = BlurMaskFilter((8 + 6 * breath).dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            val r = size.height / 2 + spread
            drawIntoCanvas { it.drawRoundRect(-spread, -spread, size.width + spread, size.height + spread, r, r, paint) }
        },
    ) { content() }
}
