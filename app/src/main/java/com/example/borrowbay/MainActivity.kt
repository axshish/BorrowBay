package com.example.borrowbay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.borrowbay.features.userregistration.ui.UserRegistrationScreen
import com.example.borrowbay.navigation.NavGraph
import com.example.borrowbay.ui.theme.BorrowBayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            BorrowBayTheme {
//                NavGraph()
                HomeScreen()
            }
        }
    }
}
