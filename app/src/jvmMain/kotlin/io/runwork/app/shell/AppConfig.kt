package io.runwork.app.shell

data class AppConfig(
    val baseUrl: String,
    val publicKey: String,
    val shellVersion: Int,
    val mainClass: String,
    val appId: String,
    val downloadTimeoutMs: Long = 15_000L,
    val initialRetryDelayMs: Long = 2_000L,
    val maxRetryDelayMs: Long = 60_000L,
)
