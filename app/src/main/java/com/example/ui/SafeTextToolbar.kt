package com.example.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

class SafeTextToolbar : TextToolbar {
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        // Suppresses android.view.ActionMode.TYPE_FLOATING
        // to permanently eliminate DecorView FloatingActionMode race condition logs.
        status = TextToolbarStatus.Hidden
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
    }
}
