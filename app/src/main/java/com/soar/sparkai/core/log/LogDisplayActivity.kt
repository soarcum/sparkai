package com.soar.sparkai.core.log

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.soar.sparkai.core.theme.AppTheme

/**
 * 作用：作为展示日志控制台 Compose 视图的 Activity 载体。
 */
class LogDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LogDisplayScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
