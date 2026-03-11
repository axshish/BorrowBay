package com.example.borrowbay1

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://your-project-id.supabase.co",
    supabaseKey = "your-anon-key"
) {
    install(Auth)
    install(Postgrest)
}
