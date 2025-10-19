package com.aidlink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aidlink.ui.AppNavigation
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AppNavViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appNavViewModel: AppNavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            AidLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val screen = it.getStringExtra("screen")
            if (screen == "review") {
                val requestId = it.getStringExtra("requestId")
                val revieweeId = it.getStringExtra("revieweeId")
                if (requestId != null && revieweeId != null) {
                    appNavViewModel.setReviewDeepLink(requestId, revieweeId)
                }
            } else {
                val chatId = it.getStringExtra("chatId")
                val userName = it.getStringExtra("userName")
                if (chatId != null && userName != null) {
                    appNavViewModel.setChatDeepLink(chatId, userName)
                }
            }
            it.removeExtra("screen")
            it.removeExtra("requestId")
            it.removeExtra("revieweeId")
            it.removeExtra("chatId")
            it.removeExtra("userName")
        }
    }
}