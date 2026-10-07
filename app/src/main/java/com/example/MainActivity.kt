package com.example

import android.os.Bundle
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalTextToolbar
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.SafeTextToolbar
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalTextToolbar provides remember { SafeTextToolbar() }) {
                MyApplicationTheme {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        if (type == ActionMode.TYPE_FLOATING) {
            // Returning null explicitly prevents the window DecorView from creating
            // an unexpected FloatingActionMode instance, permanently avoiding DecorView logs.
            return null
        }
        return super.onWindowStartingActionMode(callback, type)
    }
}
