package io.runwork.app.shell

import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class AppConfig(
    val baseUrl: String,
    val publicKey: String,
    val shellVersion: Int,
    val mainClass: String,
    val appId: String,
    val appDataDir: Path? = null,
    val downloadTimeout: Duration = 15.seconds,
    val initialRetryDelay: Duration = 2.seconds,
    val maxRetryDelay: Duration = 60.seconds,
    val updateCheckInterval: Duration = 6.hours,
)
