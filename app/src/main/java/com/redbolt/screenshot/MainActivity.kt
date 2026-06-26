package com.redbolt.screenshot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.redbolt.screenshot.accessibility.SystemPreviewDismissAccessibilityService
import com.redbolt.screenshot.handler.BoltThemeId
import com.redbolt.screenshot.handler.PromptAction
import com.redbolt.screenshot.handler.PromptPosition
import com.redbolt.screenshot.handler.ScreenshotActions
import com.redbolt.screenshot.handler.ScreenshotPreferences
import com.redbolt.screenshot.handler.ScreenshotPromptLauncher
import com.redbolt.screenshot.service.ScreenshotMonitorService
import com.redbolt.screenshot.ui.theme.BoltScreenshotTheme
import com.redbolt.screenshot.ui.theme.BoltTheme
import com.redbolt.screenshot.ui.theme.BoltThemePresets
import com.redbolt.screenshot.ui.theme.BodyFont
import com.redbolt.screenshot.ui.theme.DotoFont
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val app by lazy { application as ScreenshotApplication }

    private var monitorEnabled by mutableStateOf(true)
    private var instantPrompt by mutableStateOf(true)
    private var promptPosition by mutableStateOf(PromptPosition.CENTER)
    private var vibrateOnPrompt by mutableStateOf(true)
    private var tapOutsideToDismiss by mutableStateOf(true)
    private var detectionDelayMs by mutableFloatStateOf(
        ScreenshotPreferences.DEFAULT_DETECTION_DELAY_MS.toFloat(),
    )
    private var selectedTheme by mutableStateOf(BoltThemeId.BOLT_RED)
    private var copyRowOnTop by mutableStateOf(true)
    private var dismissSystemPreview by mutableStateOf(false)
    private var systemPreviewDismissDelayMs by mutableFloatStateOf(
        ScreenshotPreferences.DEFAULT_SYSTEM_PREVIEW_DISMISS_DELAY_MS.toFloat(),
    )
    private var hasMediaPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)
    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAllFilesAccess by mutableStateOf(false)
    private var hasAccessibilityDismiss by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasMediaPermission = hasMediaAccess()
        hasNotificationPermission = hasNotificationAccess()
        syncMonitorState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPreferences()
        hasMediaPermission = hasMediaAccess()
        hasNotificationPermission = hasNotificationAccess()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasAllFilesAccess = ScreenshotActions.hasAllFilesAccess(this)
        hasAccessibilityDismiss = SystemPreviewDismissAccessibilityService.isEnabled(this)
        enableEdgeToEdge()

        if (!hasMediaPermission || !hasNotificationPermission) {
            requestNeededPermissions()
        } else if (monitorEnabled) {
            ScreenshotMonitorService.start(this)
        }

        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrDefault("1.0.0")

        setContent {
            BoltScreenshotTheme(themeId = selectedTheme) {
                val colors = BoltTheme.colors
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(
                        text = "BOLT",
                        fontFamily = DotoFont,
                        color = colors.accent,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = "SHOT",
                        fontFamily = DotoFont,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Copy or share — delete or save after each screenshot.",
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 10.sp,
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    SettingRow(
                        title = "MONITOR SCREENSHOTS",
                        subtitle = if (hasMediaPermission) "Running in background" else "Needs photo access",
                        checked = monitorEnabled && hasMediaPermission,
                        onCheckedChange = { enabled ->
                            if (!hasMediaPermission) {
                                requestNeededPermissions()
                                return@SettingRow
                            }
                            monitorEnabled = enabled
                            app.preferences.isMonitorEnabled = enabled
                            syncMonitorState()
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingRow(
                        title = "POP-UP PROMPT",
                        subtitle = "Show chooser right after each screenshot",
                        checked = instantPrompt,
                        onCheckedChange = {
                            instantPrompt = it
                            app.preferences.showInstantPrompt = it
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingRow(
                        title = getString(R.string.overlay_permission_title),
                        subtitle = if (hasOverlayPermission) {
                            getString(R.string.overlay_permission_subtitle_on)
                        } else {
                            getString(R.string.overlay_permission_subtitle_off)
                        },
                        checked = hasOverlayPermission,
                        onCheckedChange = {
                            openOverlaySettings()
                        },
                    )
                    if (!hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsButton(
                            label = getString(R.string.enable_overlay),
                            primary = true,
                            onClick = { openOverlaySettings() },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingRow(
                        title = getString(R.string.all_files_title),
                        subtitle = if (hasAllFilesAccess) {
                            getString(R.string.all_files_subtitle_on)
                        } else {
                            getString(R.string.all_files_subtitle_off)
                        },
                        checked = hasAllFilesAccess,
                        onCheckedChange = {
                            ScreenshotActions.openAllFilesAccessSettings(this@MainActivity)
                        },
                    )
                    if (!hasAllFilesAccess) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsButton(
                            label = getString(R.string.enable_all_files),
                            primary = false,
                            onClick = { ScreenshotActions.openAllFilesAccessSettings(this@MainActivity) },
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = getString(R.string.section_theme),
                        fontFamily = DotoFont,
                        color = colors.accent,
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getString(R.string.theme_subtitle),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BoltThemeId.entries.chunked(2).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowThemes.forEach { theme ->
                                ThemeOptionCard(
                                    themeId = theme,
                                    selected = selectedTheme == theme,
                                    onClick = {
                                        selectedTheme = theme
                                        app.preferences.themeId = theme
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowThemes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = getString(R.string.section_customization),
                        fontFamily = DotoFont,
                        color = colors.accent,
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = getString(R.string.action_order_title),
                        fontFamily = BodyFont,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = getString(R.string.action_order_subtitle),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PromptLayoutPreview(copyRowOnTop = copyRowOnTop)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PositionChip(
                            label = getString(R.string.copy_row_on_top),
                            selected = copyRowOnTop,
                            onClick = {
                                copyRowOnTop = true
                                app.preferences.copyRowOnTop = true
                            },
                            modifier = Modifier.weight(1f),
                        )
                        PositionChip(
                            label = getString(R.string.share_row_on_top),
                            selected = !copyRowOnTop,
                            onClick = {
                                copyRowOnTop = false
                                app.preferences.copyRowOnTop = false
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingRow(
                        title = getString(R.string.dismiss_preview_title),
                        subtitle = if (hasAccessibilityDismiss && dismissSystemPreview) {
                            getString(R.string.dismiss_preview_subtitle_on)
                        } else {
                            getString(R.string.dismiss_preview_subtitle_off)
                        },
                        checked = dismissSystemPreview,
                        onCheckedChange = { enabled ->
                            dismissSystemPreview = enabled
                            app.preferences.dismissSystemPreview = enabled
                            if (enabled && !hasAccessibilityDismiss) {
                                SystemPreviewDismissAccessibilityService.openSettings(this@MainActivity)
                            }
                        },
                    )
                    if (dismissSystemPreview && !hasAccessibilityDismiss) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsButton(
                            label = getString(R.string.enable_dismiss_preview),
                            primary = false,
                            onClick = { SystemPreviewDismissAccessibilityService.openSettings(this@MainActivity) },
                        )
                    }
                    if (dismissSystemPreview && hasAccessibilityDismiss) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = getString(R.string.dismiss_preview_delay_title),
                            fontFamily = BodyFont,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = getString(R.string.dismiss_preview_delay_subtitle),
                            fontFamily = BodyFont,
                            color = colors.textMuted,
                            fontSize = 9.sp,
                        )
                        Text(
                            text = getString(
                                R.string.detection_delay_value,
                                systemPreviewDismissDelayMs.roundToInt(),
                            ),
                            fontFamily = BodyFont,
                            color = colors.accent,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Slider(
                            value = systemPreviewDismissDelayMs,
                            onValueChange = { systemPreviewDismissDelayMs = it },
                            onValueChangeFinished = {
                                app.preferences.systemPreviewDismissDelayMs =
                                    systemPreviewDismissDelayMs.roundToInt().toLong()
                            },
                            valueRange = 500f..3_000f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.textPrimary,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.surface,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = getString(R.string.prompt_position_title),
                        fontFamily = BodyFont,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = getString(R.string.prompt_position_subtitle),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PositionChip(
                            label = getString(R.string.position_top),
                            selected = promptPosition == PromptPosition.TOP,
                            onClick = {
                                promptPosition = PromptPosition.TOP
                                app.preferences.promptPosition = PromptPosition.TOP
                            },
                            modifier = Modifier.weight(1f),
                        )
                        PositionChip(
                            label = getString(R.string.position_center),
                            selected = promptPosition == PromptPosition.CENTER,
                            onClick = {
                                promptPosition = PromptPosition.CENTER
                                app.preferences.promptPosition = PromptPosition.CENTER
                            },
                            modifier = Modifier.weight(1f),
                        )
                        PositionChip(
                            label = getString(R.string.position_bottom),
                            selected = promptPosition == PromptPosition.BOTTOM,
                            onClick = {
                                promptPosition = PromptPosition.BOTTOM
                                app.preferences.promptPosition = PromptPosition.BOTTOM
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingRow(
                        title = getString(R.string.vibrate_title),
                        subtitle = getString(R.string.vibrate_subtitle),
                        checked = vibrateOnPrompt,
                        onCheckedChange = {
                            vibrateOnPrompt = it
                            app.preferences.vibrateOnPrompt = it
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingRow(
                        title = getString(R.string.tap_outside_title),
                        subtitle = getString(R.string.tap_outside_subtitle),
                        checked = tapOutsideToDismiss,
                        onCheckedChange = {
                            tapOutsideToDismiss = it
                            app.preferences.tapOutsideToDismiss = it
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = getString(R.string.detection_delay_title),
                        fontFamily = BodyFont,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = getString(R.string.detection_delay_subtitle),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                    )
                    Text(
                        text = getString(
                            R.string.detection_delay_value,
                            detectionDelayMs.roundToInt(),
                        ),
                        fontFamily = BodyFont,
                        color = colors.accent,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Slider(
                        value = detectionDelayMs,
                        onValueChange = { detectionDelayMs = it },
                        onValueChangeFinished = {
                            app.preferences.detectionDelayMs = detectionDelayMs.roundToInt().toLong()
                        },
                        valueRange = 0f..1000f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.textPrimary,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surface,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsButton(
                        label = getString(R.string.test_prompt),
                        primary = true,
                        onClick = { ScreenshotPromptLauncher.showTest(this@MainActivity) },
                    )
                    Text(
                        text = getString(R.string.test_prompt_subtitle),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = getString(R.string.system_preview_note),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                    )
                    if (!hasNotificationPermission && !instantPrompt) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Enable notifications so the prompt can appear.",
                            fontFamily = BodyFont,
                            color = colors.accent,
                            fontSize = 9.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = getString(R.string.version_label, versionName),
                        fontFamily = BodyFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasMediaPermission = hasMediaAccess()
        hasNotificationPermission = hasNotificationAccess()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasAllFilesAccess = ScreenshotActions.hasAllFilesAccess(this)
        hasAccessibilityDismiss = SystemPreviewDismissAccessibilityService.isEnabled(this)
        if (monitorEnabled && hasMediaPermission) {
            ScreenshotMonitorService.start(this)
        }
        ScreenshotActions.completePendingShareDelete(this)
    }

    private fun loadPreferences() {
        monitorEnabled = app.preferences.isMonitorEnabled
        instantPrompt = app.preferences.showInstantPrompt
        promptPosition = app.preferences.promptPosition
        vibrateOnPrompt = app.preferences.vibrateOnPrompt
        tapOutsideToDismiss = app.preferences.tapOutsideToDismiss
        detectionDelayMs = app.preferences.detectionDelayMs.toFloat()
        selectedTheme = app.preferences.themeId
        copyRowOnTop = app.preferences.copyRowOnTop
        dismissSystemPreview = app.preferences.dismissSystemPreview
        systemPreviewDismissDelayMs = app.preferences.systemPreviewDismissDelayMs.toFloat()
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun syncMonitorState() {
        if (monitorEnabled && hasMediaPermission) {
            ScreenshotMonitorService.start(this)
        } else {
            ScreenshotMonitorService.stop(this)
        }
    }

    private fun hasMediaAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasNotificationAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNeededPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@androidx.compose.runtime.Composable
private fun PromptLayoutPreview(copyRowOnTop: Boolean) {
    val colors = BoltTheme.colors
    val topRow = if (copyRowOnTop) PromptAction.COPY_ROW else PromptAction.SHARE_ROW
    val bottomRow = if (copyRowOnTop) PromptAction.SHARE_ROW else PromptAction.COPY_ROW
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewActionRow(actions = topRow)
        PreviewActionRow(actions = bottomRow)
    }
}

@androidx.compose.runtime.Composable
private fun PreviewActionRow(actions: List<PromptAction>) {
    val colors = BoltTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.background)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(action.labelRes),
                    fontFamily = DotoFont,
                    color = colors.textMuted,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = BoltTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = BodyFont, color = colors.textPrimary, fontSize = 11.sp)
            Text(text = subtitle, fontFamily = BodyFont, color = colors.textMuted, fontSize = 9.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.textPrimary,
                checkedTrackColor = colors.accent,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun SettingsButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val colors = BoltTheme.colors
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) colors.accent else colors.surface,
            contentColor = if (primary) colors.background else colors.textPrimary,
        ),
    ) {
        Text(label, fontFamily = BodyFont, fontSize = 10.sp)
    }
}

@androidx.compose.runtime.Composable
private fun PositionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BoltTheme.colors
    val shape = RoundedCornerShape(10.dp)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accent else colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = BodyFont,
            color = if (selected) colors.background else colors.textMuted,
            fontSize = 10.sp,
        )
    }
}

@androidx.compose.runtime.Composable
private fun ThemeOptionCard(
    themeId: BoltThemeId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = BoltThemePresets.fromId(themeId)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) theme.surface else theme.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) theme.accent else theme.border,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(theme.accent),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = themeId.displayName,
                fontFamily = BodyFont,
                color = if (selected) theme.textPrimary else theme.textMuted,
                fontSize = 9.sp,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(theme.accent),
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(theme.surface),
            )
        }
    }
}
