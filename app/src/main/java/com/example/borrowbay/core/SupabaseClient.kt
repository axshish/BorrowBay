package com.example.borrowbay.core

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

val supabase = createSupabaseClient(
    supabaseUrl = "https://qrgselhzvxfxmpqpzobv.supabase.co",
    supabaseKey = "sb_publishable_U7EkYoa7UOtruZvzUI5JrQ_Z8wdRWeF"
) {
    httpEngine = OkHttp.create()
    install(Auth)
    install(Postgrest)
    install(Storage)
}
