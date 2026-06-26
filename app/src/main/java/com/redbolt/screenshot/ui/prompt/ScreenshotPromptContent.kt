package com.redbolt.screenshot.ui.prompt

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbolt.screenshot.R
import com.redbolt.screenshot.handler.PromptPosition
import com.redbolt.screenshot.ui.rememberOverlaySystemBarInsets
import com.redbolt.screenshot.ui.theme.BoltTheme
import com.redbolt.screenshot.ui.theme.DotoFont

@Composable
fun ScreenshotPromptContent(
    uri: Uri,
    onCopyDelete: () -> Unit,
    onCopySave: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    overlayMode: Boolean = false,
    position: PromptPosition = PromptPosition.CENTER,
    tapOutsideToDismiss: Boolean = true,
) {
    val colors = BoltTheme.colors
    val (statusBarInset, navBarInset) = rememberOverlaySystemBarInsets()
    val scrimAlpha = if (overlayMode) 0f else 0.55f
    val effectivePosition = if (overlayMode) position else PromptPosition.BOTTOM
    val alignment = when (effectivePosition) {
        PromptPosition.TOP -> Alignment.TopCenter
        PromptPosition.CENTER -> Alignment.Center
        PromptPosition.BOTTOM -> Alignment.BottomCenter
    }
    val cardShape = when (effectivePosition) {
        PromptPosition.TOP -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        PromptPosition.CENTER -> RoundedCornerShape(24.dp)
        PromptPosition.BOTTOM -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (overlayMode) {
                    Modifier.padding(top = statusBarInset, bottom = navBarInset)
                } else {
                    Modifier.statusBarsPadding()
                },
            )
            .then(
                if (tapOutsideToDismiss) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha)),
            )
        }

        Surface(
            modifier = Modifier
                .align(alignment)
                .fillMaxWidth()
                .then(
                    if (overlayMode) {
                        Modifier.padding(horizontal = 20.dp)
                    } else {
                        Modifier.navigationBarsPadding()
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            color = colors.background,
            shape = cardShape,
            shadowElevation = if (overlayMode) 8.dp else 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = if (overlayMode) 18.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "SHOT",
                    fontFamily = DotoFont,
                    color = colors.accent,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(if (overlayMode) 14.dp else 20.dp))
                PromptButton(
                    label = stringResource(R.string.action_copy_delete),
                    primary = true,
                    onClick = onCopyDelete,
                )
                Spacer(modifier = Modifier.height(10.dp))
                PromptButton(
                    label = stringResource(R.string.action_copy_save),
                    primary = false,
                    onClick = onCopySave,
                )
                Spacer(modifier = Modifier.height(10.dp))
                PromptButton(
                    label = stringResource(R.string.action_share),
                    primary = false,
                    onClick = onShare,
                )
                if (!overlayMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TAP OUTSIDE TO DISMISS",
                        fontFamily = DotoFont,
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(onClick = onDismiss),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val colors = BoltTheme.colors
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) colors.accent else colors.surface,
            contentColor = if (primary) onAccentContentColor(colors.accent) else colors.textPrimary,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            label,
            fontFamily = DotoFont,
            fontSize = 12.sp,
            color = if (primary) onAccentContentColor(colors.accent) else colors.textPrimary,
        )
    }
}

private fun onAccentContentColor(accent: Color): Color =
    if (accent.luminance() > 0.55f) Color.Black else Color.White
