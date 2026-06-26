package com.redbolt.screenshot.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.redbolt.screenshot.handler.ScreenshotActions
import com.redbolt.screenshot.handler.ScreenshotPreferences
import com.redbolt.screenshot.handler.ScreenshotPromptLauncher
import com.redbolt.screenshot.ui.prompt.ScreenshotPromptContent
import com.redbolt.screenshot.ui.theme.BoltScreenshotTheme

object ScreenshotOverlay {
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun isShowing(): Boolean = composeView != null

    fun show(context: Context, uri: Uri): Boolean {
        hide(context)
        val windowManager = context.getSystemService(WindowManager::class.java) ?: return false

        return runCatching {
            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner
            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    val prefs = ScreenshotPreferences(context)
                    BoltScreenshotTheme(themeId = prefs.themeId) {
                        ScreenshotPromptContent(
                            uri = uri,
                            copyRowOnTop = prefs.copyRowOnTop,
                            overlayMode = true,
                            position = prefs.promptPosition,
                            tapOutsideToDismiss = prefs.tapOutsideToDismiss,
                            onCopyDelete = {
                                if (uri != ScreenshotPromptLauncher.testUri) {
                                    ScreenshotActions.copyAndDelete(context, uri)
                                }
                                hide(context)
                            },
                            onCopySave = {
                                if (uri != ScreenshotPromptLauncher.testUri) {
                                    ScreenshotActions.copyAndSave(context, uri)
                                }
                                hide(context)
                            },
                            onShareAndDelete = {
                                if (uri != ScreenshotPromptLauncher.testUri) {
                                    ScreenshotActions.launchShareAndDelete(context, uri)
                                }
                                hide(context)
                            },
                            onShareAndSave = {
                                if (uri != ScreenshotPromptLauncher.testUri) {
                                    ScreenshotActions.launchShareAndSave(context, uri)
                                }
                                hide(context)
                            },
                            onDismiss = { hide(context) },
                        )
                    }
                }
            }
            composeView = view

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            windowManager.addView(view, params)
            true
        }.getOrElse {
            hide(context)
            false
        }
    }

    fun hide(context: Context) {
        val view = composeView
        composeView = null
        if (view != null) {
            runCatching {
                context.getSystemService(WindowManager::class.java)?.removeView(view)
            }
        }
        lifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner = null
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
