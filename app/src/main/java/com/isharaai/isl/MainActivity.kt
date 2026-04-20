package com.isharaai.isl

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.isharaai.isl.core.navigation.IsharaAINavGraph
import com.isharaai.isl.core.theme.IsharaAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsharaAITheme {
                IsharaAINavGraph()
            }
        }
    }
}
