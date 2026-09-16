package com.safelive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.safelive.app.presentation.main.MainAppScreen
import com.safelive.app.navigation.Screen
import com.safelive.app.ui.theme.SafeLiveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val resetToken = intent?.data
            ?.takeIf { it.host.equals("safelive.in", ignoreCase = true) || it.host.equals("www.safelive.in", ignoreCase = true) }
            ?.getQueryParameter("token")
        setContent {
            SafeLiveTheme(dynamicColor = false) {
                MainAppScreen(initialRoute = resetToken?.takeIf { it.isNotBlank() }?.let(Screen.ResetPassword::createRoute))
            }
        }
    }
}
