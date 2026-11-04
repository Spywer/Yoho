package com.spywer.yoho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.spywer.yoho.ui.main.MainShell
import com.spywer.yoho.ui.theme.YohoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YohoTheme(darkTheme = true) {
                MainShell()
            }
        }
    }
}
