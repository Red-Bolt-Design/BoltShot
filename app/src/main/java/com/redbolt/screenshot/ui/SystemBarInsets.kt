package com.redbolt.screenshot.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Context.systemBarHeightDp(resourceName: String, fallback: Dp): Dp {
    val resId = resources.getIdentifier(resourceName, "dimen", "android")
    if (resId <= 0) return fallback
    return (resources.getDimensionPixelSize(resId) / resources.displayMetrics.density).dp
}

fun Context.statusBarHeightDp(): Dp = systemBarHeightDp("status_bar_height", 28.dp)

fun Context.navigationBarHeightDp(): Dp = systemBarHeightDp("navigation_bar_height", 0.dp)

@Composable
fun rememberOverlaySystemBarInsets(): Pair<Dp, Dp> {
    val context = LocalContext.current
    return remember(context) {
        context.statusBarHeightDp() to context.navigationBarHeightDp()
    }
}
