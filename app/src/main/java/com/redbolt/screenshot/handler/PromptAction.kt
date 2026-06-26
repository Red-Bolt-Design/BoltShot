package com.redbolt.screenshot.handler

import com.redbolt.screenshot.R

enum class PromptAction(val storageKey: String, val labelRes: Int) {
    COPY_DELETE("copy_delete", R.string.action_copy_delete),
    COPY_SAVE("copy_save", R.string.action_copy_save),
    SHARE_DELETE("share_delete", R.string.action_share_delete),
    SHARE_SAVE("share_save", R.string.action_share_save),
    ;

    companion object {
        val COPY_ROW = listOf(COPY_DELETE, COPY_SAVE)
        val SHARE_ROW = listOf(SHARE_DELETE, SHARE_SAVE)

        fun orderedActions(copyRowOnTop: Boolean): List<PromptAction> =
            if (copyRowOnTop) COPY_ROW + SHARE_ROW else SHARE_ROW + COPY_ROW
    }
}
