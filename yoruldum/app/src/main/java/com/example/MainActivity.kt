package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.ui.MainViewModel
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.SoyAgaciTheme

class MainActivity : FragmentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoyAgaciTheme {
                NavGraph(viewModel = mainViewModel)
            }
        }
    }
}

