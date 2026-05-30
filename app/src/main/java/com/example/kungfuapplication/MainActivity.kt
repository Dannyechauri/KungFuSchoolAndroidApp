package com.example.kungfuapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kungfuapplication.ui.theme.KungFuApplicationTheme
import com.example.kungfuapplication.ui.KungFuSchoolApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KungFuApplicationTheme {
                KungFuSchoolApp()
            }
        }
    }
}
