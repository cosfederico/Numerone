package it.numerone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val languages = listOf("it" to "Italiano", "en" to "English")
private const val SITE = "https://vincenzopalmisano.net"

/** Settings, with the About page as a sub-page. Changes apply immediately. */
@Composable
fun SettingsScreen(settings: Settings, onChange: (Settings) -> Unit, onDeleteAll: () -> Unit, onClose: () -> Unit) {
    var about by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (about) about = false else onClose() }
    AnimatedContent(
        about,
        transitionSpec = {
            val dir = if (targetState) 1 else -1
            (slideInHorizontally { it * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir } + fadeOut())
        },
        label = "settings",
    ) { showAbout ->
        if (showAbout) About { about = false }
        else SettingsList(settings, onChange, onDeleteAll, onClose) { about = true }
    }
}

@Composable
private fun SettingsList(settings: Settings, onChange: (Settings) -> Unit, onDeleteAll: () -> Unit, onClose: () -> Unit, onAbout: () -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        BackTitle(S.settings, onClose)

        Label(S.name)
        // An emptied field keeps the stored name until something valid is typed.
        NameField(settings.name) { text -> validName(text)?.let { onChange(settings.copy(name = it)) } }

        Spacer(Modifier.height(24.dp))
        Label(S.language)
        LanguageChips(settings.lang) { onChange(settings.copy(lang = it)) }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Sand)
        Row(Modifier.fillMaxWidth().clickable(onClick = onAbout).padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(S.about, Modifier.weight(1f), fontSize = 17.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
        }
        HorizontalDivider(color = Sand)

        Spacer(Modifier.height(40.dp))
        OutlinedButton(
            { confirmDelete = true }, Modifier.fillMaxWidth().height(52.dp),
            border = BorderStroke(1.5.dp, Bad), colors = ButtonDefaults.outlinedButtonColors(contentColor = Bad),
        ) { Text(S.deleteAll, fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(S.deleteTitle, fontFamily = Serif, fontSize = 24.sp, color = Ink) },
        text = { Text(S.deleteText, color = Muted) },
        confirmButton = {
            TextButton({
                onDeleteAll()
                confirmDelete = false
            }) { Text(S.delete, color = Bad) }
        },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(S.cancel) } },
    )
}

/** Name input: every keystroke goes through [cleanName], so nothing but letters and a few separators can be typed or pasted. */
@Composable
fun NameField(initial: String, imeAction: ImeAction = ImeAction.Done, onText: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    OutlinedTextField(
        text,
        {
            text = cleanName(it)
            onText(text)
        },
        Modifier.fillMaxWidth().padding(top = 8.dp),
        singleLine = true,
        supportingText = { Text("${S.nameHint(MAX_NAME)} · ${text.length}/$MAX_NAME") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction),
    )
}

@Composable
fun LanguageChips(lang: String, onSelect: (String) -> Unit) =
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        languages.forEach { (code, label) ->
            FilterChip(
                lang == code, { onSelect(code) }, { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Ink, selectedLabelColor = Beige),
            )
        }
    }

@Composable
private fun BackTitle(title: String, onBack: () -> Unit) =
    Row(Modifier.padding(top = TITLE_TOP - 4.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack, Modifier.padding(end = 4.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, S.back) }
        Text(title, fontFamily = Serif, fontSize = 36.sp)
    }

@Composable
private fun About(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    // Scrolling content above, site link pinned to the bottom of the page.
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        BackTitle(S.about, onBack)
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(S.numerone, fontFamily = Serif, fontSize = 48.sp)
            Text(S.version(version), color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            Text("“${S.motto}”", color = Ink, fontWeight = FontWeight.Light, fontSize = 11.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Text(S.createdBy, fontSize = 15.sp)
            Text(S.builtWith, Modifier.padding(top = 8.dp), color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Sand)
            Text(S.disclaimer, Modifier.padding(vertical = 24.dp), color = Muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
        }
        }
        Text(
            buildAnnotatedString {
                append(S.moreInfo)
                withLink(LinkAnnotation.Url(SITE, TextLinkStyles(SpanStyle(color = Ink, textDecoration = TextDecoration.Underline)))) {
                    append(SITE.removePrefix("https://"))
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 20.dp), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
    }
}

/** First launch: pick a language and tell us your name. */
@Composable
fun WelcomeScreen(settings: Settings, onLang: (String) -> Unit, onDone: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        LanguageChips(settings.lang, onLang)
        Spacer(Modifier.height(32.dp))
        Text(S.welcome, fontFamily = Serif, fontSize = 48.sp)
        Text(S.askName, Modifier.padding(top = 8.dp, bottom = 16.dp), color = Muted, fontSize = 18.sp)
        NameField("", ImeAction.Go) { name = validName(it) }
        Spacer(Modifier.height(24.dp))
        Pill(S.letsGo, Ink, enabled = name != null) { name?.let(onDone) }
    }
}
