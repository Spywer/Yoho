package com.spywer.yoho.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.spywer.yoho.ui.theme.YohoTheme

class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YohoTheme(darkTheme = true) {
                AccountScreen(
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, AccountActivity::class.java)
    }
}
