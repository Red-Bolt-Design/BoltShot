package com.redbolt.screenshot.ui.prompt

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redbolt.screenshot.handler.PromptAction
import com.redbolt.screenshot.handler.PromptPosition
import com.redbolt.screenshot.ui.rememberOverlaySystemBarInsets
import com.redbolt.screenshot.ui.theme.BoltTheme
import com.redbolt.screenshot.ui.theme.DotoFont

@Composable
fun ScreenshotPromptContent(
    uri: Uri,
    copyRowOnTop: Boolean,
    onCopyDelete: () -> Unit,
    onCopySave: () -> Unit,
    onShareAndDelete: () -> Unit,
    onShareAndSave: () -> Unit,
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
    val handlers = mapOf(
        PromptAction.COPY_DELETE to onCopyDelete,
        PromptAction.COPY_SAVE to onCopySave,
        PromptAction.SHARE_DELETE to onShareAndDelete,
        PromptAction.SHARE_SAVE to onShareAndSave,
    )
    val topRow = if (copyRowOnTop) PromptAction.COPY_ROW else PromptAction.SHARE_ROW
    val bottomRow = if (copyRowOnTop) PromptAction.SHARE_ROW else PromptAction.COPY_ROW

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
                ActionPairRow(actions = topRow, handlers = handlers)
                Spacer(modifier = Modifier.height(10.dp))
                ActionPairRow(actions = bottomRow, handlers = handlers)
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
private fun ActionPairRow(
    actions: List<PromptAction>,
    handlers: Map<PromptAction, () -> Unit>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            GridPromptButton(
                label = stringResource(action.labelRes),
                onClick = handlers.getValue(action),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GridPromptButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BoltTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = label,
            fontFamily = DotoFont,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = colors.textPrimary,
        )
    }
}
