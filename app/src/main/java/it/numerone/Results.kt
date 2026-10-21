package it.numerone

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.DayOfWeek
import java.time.Duration
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.util.Locale

// Calendar days back from today (inclusive); null = all data.
private val periods = listOf("Oggi" to 1, "3 giorni" to 3, "Settimana" to 7, "Mese" to 30, "3 mesi" to 91, "6 mesi" to 182, "Anno" to 365, "Tutto" to null)
private const val AUTO_PERIOD_CAP = 2 // Settimana
private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
private val shortDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun Long.localDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun Long.pickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate() // pickers return UTC midnight

@Composable
fun ResultsScreen(runs: List<Run>) {
    var picked by rememberSaveable { mutableStateOf<Int?>(null) } // null until the user picks a period
    var metric by rememberSaveable { mutableIntStateOf(0) } // 0 time, 1 wrong digits
    var from by rememberSaveable { mutableStateOf<Long?>(null) }
    var to by rememberSaveable { mutableStateOf<Long?>(null) }
    var timeMode by rememberSaveable { mutableIntStateOf(0) } // 0 any, 1 under, 2 over
    var seconds by rememberSaveable { mutableStateOf("") }
    var outcome by rememberSaveable { mutableIntStateOf(0) } // 0 any, 1 correct, 2 wrong
    var picking by rememberSaveable { mutableStateOf(false) }

    val today = LocalDate.now()
    // Default: smallest period covering all data, capped at a week.
    val span = runs.firstOrNull()?.let { ChronoUnit.DAYS.between(it.time.localDate(), today) + 1 } ?: 1
    val period = picked ?: periods.indexOfFirst { (it.second ?: Int.MAX_VALUE) >= span }.coerceAtMost(AUTO_PERIOD_CAP)
    val charted = periods[period].second?.let { days -> runs.filter { it.time.localDate() > today.minusDays(days.toLong()) } } ?: runs

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
            Chips(periods.map { it.first }, period) { picked = it }
            Row(Modifier.padding(top = 20.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf("Tempo", "Errori").forEachIndexed { i, label ->
                    Text(label, Modifier.clickable { metric = i }, color = if (i == metric) Ink else Muted, fontFamily = Serif, fontSize = 22.sp)
                }
            }
            Crossfade(metric, label = "metric") {
                val days = periods[period].second
                // Times of wrong runs don't count; the error chart is about mistakes, so it keeps every run.
                if (it == 0) Chart(charted.filter { r -> r.correct }, days, { r -> r.millis.toFloat() }) { v -> formatTime(v.toLong()) }
                else Chart(charted, days, { r -> r.errors.toFloat() }) { v -> v.toInt().toString() }
            }
            Row(Modifier.padding(top = 28.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("Filtri")
                Spacer(Modifier.weight(1f))
                Text(if (shown.size == 1) "1 risultato" else "${shown.size} risultati", color = Muted, fontSize = 13.sp)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                Chip(
                    if (from == null) "Date" else "${from!!.pickerDate().format(shortDayFormat)} – ${(to ?: from)!!.pickerDate().format(shortDayFormat)}",
                    from != null, menu = true,
                ) { picking = true }
                MenuChip(listOf("Esito", "Corretti", "Errati")[outcome], outcome != 0, listOf("Tutti", "Corretti", "Errati")) { outcome = it }
                MenuChip(listOf("Tempo", "Sotto", "Sopra")[timeMode], timeMode != 0, listOf("Ogni tempo", "Sotto", "Sopra")) { timeMode = it }
                if (timeMode != 0) SecondsField(seconds) { seconds = it }
            }
            Spacer(Modifier.height(8.dp))
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
private fun Chip(label: String, selected: Boolean, menu: Boolean = false, onClick: () -> Unit) = FilterChip(
    selected, onClick, { Text(label) },
    trailingIcon = if (menu) ({ Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp)) }) else null,
    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = Beige, selectedTrailingIconColor = Beige),
)

@Composable
private fun MenuChip(label: String, selected: Boolean, options: List<String>, onSelect: (Int) -> Unit) = Box {
    var open by remember { mutableStateOf(false) }
    Chip(label, selected, menu = true) { open = true }
    DropdownMenu(open, { open = false }) {
        options.forEachIndexed { i, o -> DropdownMenuItem({ Text(o) }, { onSelect(i); open = false }) }
    }
}

/** Chip-sized seconds input. */
@Composable
private fun SecondsField(value: String, onChange: (String) -> Unit) = BasicTextField(
    value, { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
    Modifier.width(72.dp).height(32.dp).border(1.dp, Muted, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
    singleLine = true,
    textStyle = TextStyle(fontFamily = Sans, fontSize = 14.sp, color = Ink),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    decorationBox = { inner ->
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("sec", color = Muted, fontSize = 14.sp)
                inner()
            }
            Text("s", color = Muted, fontSize = 14.sp)
        }
    },
)

/**
 * Values over time with min/max guides on the y axis. Few runs in a short period: a stick per run, each
 * labelled with its time, x spanning the runs. Otherwise: x spans the whole period with sparse calendar ticks.
 */
@Composable
private fun Chart(runs: List<Run>, days: Int?, value: (Run) -> Float, format: (Float) -> String) {
    if (runs.isEmpty()) {
        Text("Nessun dato nel periodo", Modifier.padding(vertical = 8.dp), color = Muted, fontSize = 13.sp)
        return
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Sans, fontSize = 11.sp, color = Muted)
    val zone = ZoneId.systemDefault()
    val values = runs.map(value)
    val top = values.max()
    val sticks = days != null && days <= 3 && runs.size <= 12
    val start = if (sticks) runs.first().time
        else days?.let { LocalDate.now().minusDays(it - 1L).atStartOfDay(zone).toInstant().toEpochMilli() } ?: runs.first().time
    val end = if (sticks) runs.last().time else System.currentTimeMillis()
    val ticks = if (sticks) {
        val f = DateTimeFormatter.ofPattern(if (days == 1) "HH:mm" else "EEE HH:mm", Locale.ITALIAN)
        runs.map { it.time to Instant.ofEpochMilli(it.time).atZone(zone).format(f) }
    } else calendarTicks(start, end).ifEmpty {
        runs.map { it.time to Instant.ofEpochMilli(it.time).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ITALIAN)) }
    }

    Canvas(Modifier.fillMaxWidth().height(170.dp).padding(top = 8.dp)) {
        val r = 4.dp.toPx()
        val hair = 1.dp.toPx()
        val yLabels = listOf(top, values.min()).distinct().map { it to measurer.measure(format(it), labelStyle) }
        val gutter = yLabels.maxOf { it.second.size.width } + 10.dp.toPx()
        val left = gutter + r
        val right = size.width - r
        val bottom = size.height - 22.dp.toPx()
        fun x(t: Long) = if (end > start) left + (right - left) * (t - start).toFloat() / (end - start) else (left + right) / 2
        fun y(v: Float) = bottom - (bottom - r) * v / top.coerceAtLeast(1f)

        drawLine(Sand, Offset(gutter, bottom), Offset(size.width, bottom), hair)
        var lastY = Float.NaN
        yLabels.forEach { (v, label) ->
            val yy = y(v)
            if (!lastY.isNaN() && lastY + label.size.height + 4.dp.toPx() > yy) return@forEach // too close to the max label
            lastY = yy
            if (v > 0) drawLine(Sand, Offset(gutter, yy), Offset(size.width, yy), hair, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * hair, 3 * hair)))
            drawText(label, topLeft = Offset(0f, yy - label.size.height / 2))
        }

        val points = runs.map { Offset(x(it.time), y(value(it))) }
        if (sticks) points.forEach { drawLine(Muted.copy(alpha = 0.35f), Offset(it.x, bottom), it, hair) }
        drawPath(Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }, Ink, style = Stroke(1.5.dp.toPx()))
        runs.forEachIndexed { i, run -> drawCircle(if (run.correct) Good else Bad, r, points[i]) }

        // X labels: centered under sticks, left-aligned on calendar ticks; skipped when they would overlap.
        var lastRight = Float.NEGATIVE_INFINITY
        ticks.forEach { (t, text) ->
            val label = measurer.measure(text, labelStyle)
            val tx = x(t)
            val lx = (if (sticks) tx - label.size.width / 2 else tx + 3 * hair).coerceIn(gutter, size.width - label.size.width)
            if (lx < lastRight + 8.dp.toPx()) return@forEach
            lastRight = lx + label.size.width
            if (!sticks) drawLine(Muted, Offset(tx, bottom), Offset(tx, bottom + 4.dp.toPx()), hair)
            drawText(label, topLeft = Offset(lx, bottom + 6.dp.toPx()))
        }
    }
}

/** Calendar boundaries between [start] and [end]: hours, days, Mondays or months depending on the span. */
private fun calendarTicks(start: Long, end: Long): List<Pair<Long, String>> {
    val zone = ZoneId.systemDefault()
    val a = Instant.ofEpochMilli(start).atZone(zone)
    val b = Instant.ofEpochMilli(end).atZone(zone)
    val spanDays = ChronoUnit.DAYS.between(a, b)
    val (first, step, pattern) = when {
        spanDays < 1 -> Triple(a.truncatedTo(ChronoUnit.HOURS), Duration.ofHours(maxOf(1, ChronoUnit.HOURS.between(a, b) / 4)), "HH:mm")
        spanDays <= 10 -> Triple(a.truncatedTo(ChronoUnit.DAYS), Period.ofDays(1), "EEE")
        spanDays <= 90 -> Triple(a.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(zone), Period.ofWeeks(1), "d MMM")
        else -> Triple(a.toLocalDate().withDayOfMonth(1).atStartOfDay(zone), Period.ofMonths(1), if (spanDays > 365) "MMM yy" else "MMM")
    }
    val f = DateTimeFormatter.ofPattern(pattern, Locale.ITALIAN)
    return generateSequence(first) { it.plus(step) }.takeWhile { !it.isAfter(b) }.filter { !it.isBefore(a) }
        .map { it.toInstant().toEpochMilli() to it.format(f) }.toList()
}
