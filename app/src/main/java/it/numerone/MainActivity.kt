package it.numerone

import android.os.Bundle
import android.util.AtomicFile
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
            labelMedium = t.labelMedium.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
        ),
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(store: RunStore) {
    var runs by remember { mutableStateOf(store.load()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = rememberSaveableStateHolder()
    Surface(Modifier.fillMaxSize(), color = Beige, contentColor = Ink) {
        Column(Modifier.statusBarsPadding().imePadding()) {
            Crossfade(tab, Modifier.weight(1f), label = "tab") { t ->
                tabs.SaveableStateProvider(t) {
                    if (t == 0) PracticeScreen(runs) { runs = runs + it; store.save(runs) }
                    else ResultsScreen(runs)
                }
            }
            // Hidden while typing so the keyboard sits right under the answer.
            if (!WindowInsets.isImeVisible) NavigationBar(containerColor = Beige) {
                listOf("Pratica" to Icons.Outlined.Timer, "Risultati" to Icons.AutoMirrored.Outlined.ShowChart).forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        i == tab, { tab = i }, { Icon(icon, null) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink, selectedTextColor = Ink, indicatorColor = Sand,
                            unselectedIconColor = Muted, unselectedTextColor = Muted,
                        ),
                    )
                }
            }
        }
    }
}

/** Digits on a single line, sized to fill the width (capped at [maxSize]), each colored by [color]. */
@Composable
fun Digits(
    text: String, modifier: Modifier = Modifier, maxSize: TextUnit = 40.sp, align: Alignment = Alignment.TopStart,
    color: (Int) -> Color = { Ink },
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = align) {
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
