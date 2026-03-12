package com.example.borrowbay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.borrowbay.core.supabase
import com.example.borrowbay.navigation.NavGraph
import com.example.borrowbay.ui.theme.BorrowBayTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle deep links from Supabase (Google/Email login)
        // Fixed: called directly on 'supabase' (SupabaseClient)
        supabase.handleDeeplinks(intent)
        
        enableEdgeToEdge()
        setContent {
            BorrowBayTheme {
                NavGraph()
            }
        }
    }
}
