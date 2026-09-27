package com.livetype.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.livetype.remote.ui.AppRoot
import com.livetype.remote.ui.theme.LiveTypeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveTypeTheme {
                AppRoot()
            }
        }
    }
}
