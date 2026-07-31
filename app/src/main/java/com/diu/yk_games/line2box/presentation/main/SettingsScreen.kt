package com.diu.yk_games.line2box.presentation.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.model.Settings
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val SheetShape = RoundedCornerShape(15.dp)
private val ThemeShape = RoundedCornerShape(12.dp)
val SheetColor = Color(0xE62A053E)
val SheetBorderColor = Color(0xFFC55FD6)
private val UnselectedBorderColor = Color.White.copy(alpha = 0.24f)
private val ThemeRows = Settings.Theme.entries.chunked(THEMES_PER_ROW)

@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
    isShifted: Boolean = false
) {
    var isShifted by remember { mutableStateOf(isShifted) }

    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        isShifted = true
    }

    val progress by animateFloatAsState(
        targetValue = if (isShifted) .07f else 3f,
        animationSpec = spring(),
        label = "translationYAnimation"
    )
    Surface(
        shape = SheetShape,
        color = SheetColor,
        border = BorderStroke(3.dp, SheetBorderColor),
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationY = progress * size.height
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DragHandle()

            Text(
                text = stringResource(R.string.action_settings),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            SectionTitle(text = stringResource(R.string.general))

            SettingsToggleGroup(
                settings = settings,
                onSettingsChange = onSettingsChange,
            )

            SectionTitle(text = stringResource(R.string.background))

            ThemeGrid(
                selectedTheme = settings.theme,
                onThemeSelected = { theme ->
                    onSettingsChange(settings.copy(theme = theme))
                },
            )
        }
    }
}

@Composable
fun DragHandle(
    modifier: Modifier = Modifier,
    width: Dp = 32.0.dp,
    height: Dp = 4.0.dp,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        color = color,
        shape = shape,
        modifier = modifier.padding(top = 12.dp)
    ) {
        Box(Modifier.size(width = width, height = height))
    }
}

@Composable
private fun SettingsToggleGroup(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = ThemeShape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, UnselectedBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(ThemeShape)
    ) {
        Column {
            SettingToggleRow(
                title = stringResource(R.string.play_sounds),
                checked = !settings.isMuted,
                onCheckedChange = { isMuted ->
                    onSettingsChange(settings.copy(isMuted = !isMuted))
                },
            )
            SettingToggleRow(
                title = stringResource(R.string.show_hadith),
                checked = settings.showHadith,
                onCheckedChange = { showHadith ->
                    onSettingsChange(settings.copy(showHadith = showHadith))
                },
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SheetBorderColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                uncheckedBorderColor = UnselectedBorderColor,
            ),
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleSmall,
        color = Color.White.copy(alpha = 0.9f),
    )
}

@Composable
private fun ThemeGrid(
    selectedTheme: Settings.Theme,
    onThemeSelected: (Settings.Theme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeRows.forEach { themes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                themes.forEach { theme ->
                    ThemeThumbnail(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onClick = { onThemeSelected(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeThumbnail(
    theme: Settings.Theme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundDescription =
        "${stringResource(R.string.background)} ${theme.ordinal + 1}"

    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(ThemeShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) SheetBorderColor else UnselectedBorderColor,
                shape = ThemeShape,
            )
            .semantics {
                contentDescription = backgroundDescription
            },
    ) {
        Image(
            painter = painterResource(theme.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = SheetBorderColor,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

private const val THEMES_PER_ROW = 4

@Preview(showBackground = true, backgroundColor = 0xFF101010)
@Composable
private fun SettingsScreenPreview() {
    Line2BoxTheme {
        SettingsScreen(
            isShifted = true,
            settings = Settings(
                isMuted = true,
                isFirstRun = false,
                showHadith = true,
                theme = Settings.Theme.BG3,
            ),
            onSettingsChange = {},
        )
    }
}
