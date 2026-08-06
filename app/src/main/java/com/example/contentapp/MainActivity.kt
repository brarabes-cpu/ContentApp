package com.example.contentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.contentapp.navigation.AppNavigation
import com.example.contentapp.ui.theme.ContentAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ВРЕМЕННО, для отладки текущего краша на старте — см. CrashLogger.kt.
        CrashLogger.install(applicationContext)

        super.onCreate(savedInstanceState)
        setContent {
            ContentAppTheme {
                AppNavigation()
            }
        }
    }
}
