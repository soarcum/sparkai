package com.soar.sparkai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.soar.sparkai.core.crash.GlobalExceptionHandler
import com.soar.sparkai.core.log.AppLogger
import com.soar.sparkai.core.theme.AppTheme
import com.soar.sparkai.feature.home.ui.HomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalExceptionHandler.register(this)
        AppLogger.init(this)
        AppLogger.i("MainActivity", "SparkAI application launched, system services initialized.")

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}
