package it.numerone

import android.os.Bundle
import android.util.AtomicFile
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Flag
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
val Paper = Color(0xFFF3EEE5)
val Ink = Color(0xFF2A2623)
val Muted = Color(0xFF8C8377)
val Good = Color(0xFF4C8A5A)
val Bad = Color(0xFFC0503F)
val Gold = Color(0xFFC9A646)
val GoldLight = Color(0xFFF1DE8F)
val GoldDark = Color(0xFF9A7621)

val Serif = FontFamily(Font(R.font.dm_serif_display))
val Sans = FontFamily(
    Font(R.font.inter, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

// ponytail: rewrites whole files on each change, fine for thousands of runs; move to Room if it ever lags.
class Store(dir: File) {
    private val runsFile = AtomicFile(File(dir, "runs.json"))
    private val goalsFile = AtomicFile(File(dir, "goals.json"))

    fun loadRuns(): List<Run> = runsFile.read()?.let { JSONArray(it) }?.objects()?.map {
        Run(it.getLong("time"), it.getLong("millis"), it.getString("number"), it.getString("input"))
    } ?: emptyList()

    fun saveRuns(runs: List<Run>) = runsFile.write(JSONArray(runs.map {
        JSONObject().put("time", it.time).put("millis", it.millis).put("number", it.number).put("input", it.input)
    }).toString())

    fun loadGoals(): Goals = goalsFile.read()?.let { JSONObject(it) }?.let { o ->
        Goals(
            if (o.has("target")) o.getLong("target") else null,
            o.getJSONArray("reached").objects().map { Reached(it.getLong("target"), it.getLong("time"), it.getLong("millis")) },
        )
    } ?: Goals()

    fun saveGoals(goals: Goals) = goalsFile.write(
        JSONObject().put("target", goals.target).put("reached", JSONArray(goals.reached.map {
            JSONObject().put("target", it.target).put("time", it.time).put("millis", it.millis)
        })).toString()
    )

    private fun JSONArray.objects() = List(length()) { getJSONObject(it) }

    private fun AtomicFile.read(): String? = try {
        String(readFully())
    } catch (_: FileNotFoundException) {
        null
    }

    private fun AtomicFile.write(text: String) {
        val out = startWrite()
        try {
            out.write(text.toByteArray())
            finishWrite(out)
        } catch (e: Exception) {
            failWrite(out)
            throw e
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.light(Beige.toArgb(), Beige.toArgb()), SystemBarStyle.light(Beige.toArgb(), Beige.toArgb()))
        val store = Store(filesDir)
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
            surfaceContainer = Paper, surfaceContainerHigh = Paper, surfaceContainerHighest = Paper,
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
fun App(store: Store) {
    var runs by remember { mutableStateOf(store.loadRuns()) }
    var goals by remember { mutableStateOf(store.loadGoals()) }
    fun updateGoals(g: Goals) {
        goals = g
        store.saveGoals(g)
    }
    var tab by rememberSaveable { mutableIntStateOf(1) }
    val tabs = rememberSaveableStateHolder()
    Surface(Modifier.fillMaxSize(), color = Beige, contentColor = Ink) {
        Column(Modifier.statusBarsPadding().imePadding()) {
            Crossfade(tab, Modifier.weight(1f), label = "tab") { t ->
                tabs.SaveableStateProvider(t) {
                    when (t) {
                        0 -> GoalScreen(runs, goals) { updateGoals(goals.copy(target = it)) }
                        1 -> PracticeScreen(
                            runs, goals.reached,
                            onRun = {
                                runs = runs + it
                                store.saveRuns(runs)
                                goals.after(it).let { g -> if (g != goals) updateGoals(g) }
                            },
                            onGoal = { tab = 0 },
                        )
                        else -> ResultsScreen(runs)
                    }
                }
            }
            // Hidden while typing so the keyboard sits right under the answer.
            if (!WindowInsets.isImeVisible) NavigationBar(containerColor = Beige) {
                listOf(
                    "Obiettivo" to Icons.Outlined.Flag,
                    "Pratica" to Icons.Outlined.Timer,
                    "Risultati" to Icons.AutoMirrored.Outlined.ShowChart,
                ).forEachIndexed { i, (label, icon) ->
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

/** Gold gradient with a slow moving shine, for text (TextStyle.brush) and icons (see [gold]). */
@Composable
fun goldBrush(): Brush {
    val shine by rememberInfiniteTransition(label = "gold").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "shine",
    )
    // Shifting along the gradient by exactly one mirrored period (2 × axis) makes the loop seamless.
    val axis = Offset(500f, 250f)
    val start = axis * (2 * shine)
    return Brush.linearGradient(listOf(GoldDark, Gold, GoldLight, Gold, GoldDark), start, start + axis, TileMode.Mirror)
}

/** Paints the content (e.g. an Icon) with [brush]. */
fun Modifier.gold(brush: Brush) = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(brush, blendMode = BlendMode.SrcIn)
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
