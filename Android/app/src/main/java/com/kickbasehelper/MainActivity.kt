package com.kickbasehelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import kickbase.core.ContentView
import kickbase.core.LigainsiderService
import skip.foundation.ProcessInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve and set the application context for Skip
        ProcessInfo.launch(application)
        
        setContent {
            val ligainsiderService = remember { LigainsiderService() }
            com.kickbasehelper.ui.theme.KickbaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    ContentView()
                        .environmentObject(ligainsiderService)
                        .Compose()
                }
            }
        }
    }
}
