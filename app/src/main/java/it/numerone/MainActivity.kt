package it.numerone

import android.os.Bundle
import android.util.AtomicFile
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

val Beige = Color(0xFFEDE6D9)
val Sand = Color(0xFFDDD3C1)
val Ink = Color(0xFF2A2623)
val Muted = Color(0xFF8C8377)
val Good = Color(0xFF4C8A5A)
val Bad = Color(0xFFC0503F)

val Serif = FontFamily(Font(R.font.dm_serif_display))
val Sans = FontFamily(
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

class RunStore(dir: File) {
    private val file = AtomicFile(File(dir, "runs.json"))

    fun load(): List<Run> = try {
        val a = JSONArray(String(file.readFully()))
        List(a.length()) {
            a.getJSONObject(it).run { Run(getLong("time"), getLong("millis"), getString("number"), getString("input")) }
        }
    } catch (_: FileNotFoundException) {
        emptyList()
    }

    // ponytail: rewrites the whole file on each run, fine for thousands of runs; move to Room if it ever lags.
    fun save(runs: List<Run>) {
        val json = JSONArray(runs.map {
            JSONObject().put("time", it.time).put("millis", it.millis).put("number", it.number).put("input", it.input)
        }).toString()
        val out = file.startWrite()
        try {
            out.write(json.toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.light(Beige.toArgb(), Beige.toArgb()), SystemBarStyle.light(Beige.toArgb(), Beige.toArgb()))
        val store = RunStore(filesDir)
        setContent { NumeroneTheme { App(store) } }
    }
}

@Composable
fun NumeroneTheme(content: @Composable () -> Unit) {
    val t = Typography()
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink, onPrimary = Beige, primaryContainer = Sand, onPrimaryContainer = Ink,
            secondaryContainer = Sand, onSecondaryContainer = Ink,
            background = Beige, onBackground = Ink, surface = Beige, onSurface = Ink, onSurfaceVariant = Muted,
            surfaceContainerHigh = Color(0xFFF3EEE5), surfaceContainerHighest = Color(0xFFF3EEE5),
            outline = Muted, outlineVariant = Sand,
        ),
        typography = Typography(
            bodyLarge = t.bodyLarge.copy(fontFamily = Sans),
            bodyMedium = t.bodyMedium.copy(fontFamily = Sans),
            labelLarge = t.labelLarge.copy(fontFamily = Sans, fontWeight = FontWeight.Bold),
            titleMedium = t.titleMedium.copy(fontFamily = Sans),
        ),
        content = content,
    )
}

@Composable
fun App(store: RunStore) {
    var runs by remember { mutableStateOf(store.load()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = rememberSaveableStateHolder()
    Surface(Modifier.fillMaxSize(), color = Beige, contentColor = Ink) {
        Column(Modifier.safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf("Pratica", "Risultati").forEachIndexed { i, label ->
                    Text(label, Modifier.clickable { tab = i }, color = if (i == tab) Ink else Muted, fontFamily = Serif, fontSize = 26.sp)
                }
            }
            tabs.SaveableStateProvider(tab) {
                if (tab == 0) PracticeScreen(runs) { runs = runs + it; store.save(runs) }
                else ResultsScreen(runs)
            }
        }
    }
}

/** Digits on a single line, sized to fill the width (capped at [maxSize]), each colored by [color]. */
@Composable
fun Digits(text: String, modifier: Modifier = Modifier, maxSize: TextUnit = 40.sp, color: (Int) -> Color = { Ink }) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fit = with(LocalDensity.current) { (maxWidth / (LENGTH * 0.68f)).toSp() }
        Text(
            buildAnnotatedString { text.forEachIndexed { i, c -> withStyle(SpanStyle(color = color(i))) { append(c) } } },
            style = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            fontSize = if (fit.value < maxSize.value) fit else maxSize,
            maxLines = 1,
            softWrap = false,
        )
    }
}
