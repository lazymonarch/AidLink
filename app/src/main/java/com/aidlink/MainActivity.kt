
package com.aidlink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aidlink.ui.AppNavigation // 1. Import your AppNavigation
import com.aidlink.ui.theme.AidLinkTheme // Make sure you have your theme file

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Note: Manual Firebase initialization is often not needed
        // if you have the google-services plugin configured correctly.
        // FirebaseApp.initializeApp(this)

        setContent {
            // 2. Set your app's theme
            AidLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Call your navigation graph. This is now the root of your UI.
                    AppNavigation()
                }
            }
        }
    }
}