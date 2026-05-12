package com.beeping.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.beeping.sample.ui.theme.BeepingSampleTheme

/**
 * BEE-64 — Sample app entry point. Hosts a single [MainScreen] under
 * [BeepingSampleTheme] and feeds it the [SampleAppViewModel].
 */
class MainActivity : ComponentActivity() {
    private val viewModel: SampleAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeepingSampleTheme {
                MainScreen(viewModel)
            }
        }
    }
}
