package it.numerone

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAY = 86_400_000L
private val periods = listOf("Settimana" to 7, "Mese" to 30, "3 mesi" to 91, "6 mesi" to 182, "Anno" to 365, "Tutto" to null)
private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun Long.localDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun Long.pickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate() // pickers return UTC midnight

@Composable
fun ResultsScreen(runs: List<Run>) {
    var period by rememberSaveable { mutableIntStateOf(1) }
    var metric by rememberSaveable { mutableIntStateOf(0) } // 0 time, 1 wrong digits
    var from by rememberSaveable { mutableStateOf<Long?>(null) }
    var to by rememberSaveable { mutableStateOf<Long?>(null) }
    var timeMode by rememberSaveable { mutableIntStateOf(0) } // 0 any, 1 under, 2 over
    var seconds by rememberSaveable { mutableStateOf("") }
    var outcome by rememberSaveable { mutableIntStateOf(0) } // 0 any, 1 correct, 2 wrong
    var picking by rememberSaveable { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val chartStart = periods[period].second?.let { now - it * DAY } ?: runs.firstOrNull()?.time ?: now
    val charted = runs.filter { it.time >= chartStart }

    val limit = seconds.replace(',', '.').toDoubleOrNull()?.times(1000)
    val shown = runs.asReversed().filter { r ->
        val d = r.time.localDate()
        (from == null || d >= from!!.pickerDate()) && (to == null || d <= to!!.pickerDate()) &&
            (limit == null || timeMode == 0 || (if (timeMode == 1) r.millis < limit else r.millis > limit)) &&
            (outcome == 0 || r.correct == (outcome == 1))
    }

    LazyColumn(contentPadding = PaddingValues(24.dp, 0.dp, 24.dp, 24.dp)) {
        item {
            Text("Risultati", Modifier.padding(top = 24.dp, bottom = 16.dp), fontFamily = Serif, fontSize = 40.sp)
            Chips(periods.map { it.first }, period) { period = it }
            Row(Modifier.padding(top = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf("Tempo", "Errori").forEachIndexed { i, label ->
                    Text(label, Modifier.clickable { metric = i }, color = if (i == metric) Ink else Muted, fontFamily = Serif, fontSize = 22.sp)
                }
            }
            Crossfade(metric, label = "metric") {
                if (it == 0) Chart(charted, chartStart, now, { r -> r.millis.toFloat() }) { v -> formatTime(v.toLong()) }
                else Chart(charted, chartStart, now, { r -> r.errors.toFloat() }) { v -> v.toInt().toString() }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Sand)
            Spacer(Modifier.height(16.dp))

            Label("Filtri")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    if (from == null) "Tutte le date"
                    else "${from!!.pickerDate().format(dayFormat)} – ${(to ?: from)!!.pickerDate().format(dayFormat)}",
                    from != null,
                ) { picking = true }
            }
            Chips(listOf("Tutti", "Corretti", "Errati"), outcome) { outcome = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chips(listOf("Ogni tempo", "Sotto", "Sopra"), timeMode) { timeMode = it }
                if (timeMode != 0) OutlinedTextField(
                    seconds, { seconds = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    Modifier.padding(start = 8.dp).width(96.dp),
                    singleLine = true, suffix = { Text("s") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(if (shown.size == 1) "1 risultato" else "${shown.size} risultati", color = Muted, fontSize = 13.sp)
        }

        items(shown, key = { it.time }) { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(if (r.correct) Good else Bad, CircleShape))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(formatTime(r.millis), fontFamily = Serif, fontSize = 36.sp, color = Ink)
                    Digits(r.number, maxSize = 16.sp) { if (r.ok(it)) Good else Bad }
                }
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val t = Instant.ofEpochMilli(r.time).atZone(ZoneId.systemDefault())
                    Text(t.format(dayFormat), fontSize = 14.sp)
                    Text(t.format(hourFormat), color = Muted, fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = Sand)
        }
    }

    if (picking) {
        val state = rememberDateRangePickerState(from, to)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton({
                    from = state.selectedStartDateMillis
                    to = state.selectedEndDateMillis ?: from
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ from = null; to = null; picking = false }) { Text("Azzera") } },
        ) { DateRangePicker(state, Modifier.weight(1f)) }
    }
}

@Composable
private fun Chips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) =
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, l -> Chip(l, i == selected) { onSelect(i) } }
    }

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) = FilterChip(
    selected, onClick, { Text(label) },
    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = Beige),
)

/** Values plotted against date between [start] and [end]; dots green for correct runs, red for wrong. */
@Composable
private fun Chart(runs: List<Run>, start: Long, end: Long, value: (Run) -> Float, format: (Float) -> String) = Column {
    val max = runs.maxOfOrNull(value)?.coerceAtLeast(1f) ?: 1f
    if (runs.isEmpty()) {
        Text("Nessun dato nel periodo", color = Muted, fontSize = 13.sp)
        return@Column
    }
    Text("max ${format(max)}", Modifier.align(Alignment.End), color = Muted, fontSize = 13.sp)
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val r = 4.dp.toPx()
        fun point(run: Run) = Offset(
            if (end > start) r + (size.width - 2 * r) * (run.time - start) / (end - start) else size.width / 2,
            size.height - r - (size.height - 2 * r) * value(run) / max,
        )
        drawLine(Sand, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        val points = runs.map(::point)
        drawPath(Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }, Ink, style = Stroke(1.5.dp.toPx()))
        runs.forEachIndexed { i, run -> drawCircle(if (run.correct) Good else Bad, r, points[i]) }
    }
}
