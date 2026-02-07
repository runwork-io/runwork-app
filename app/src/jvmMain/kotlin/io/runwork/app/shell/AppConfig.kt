package io.runwork.app.shell

import java.io.File

data class AppConfig(
    val baseUrl: String,
    val publicKey: String,
    val shellVersion: Int,
    val mainClass: String,
    val appId: String,
    val downloadTimeoutMs: Long = 15_000L,
    val initialRetryDelayMs: Long = 2_000L,
    val maxRetryDelayMs: Long = 60_000L,
) {
    companion object {
        val DEFAULT = AppConfig(
            baseUrl = "file://${File(System.getProperty("user.home"), ".moscow-demo-bundle").absolutePath}/",
            publicKey = "MCowBQYDK2VwAyEALfvWpE5MbxS87YZvixsKxuSS2QGGSoUJao7idEABK0Q=",
            shellVersion = 1,
            mainClass = "demo.Main",
            appId = "io.runwork.moscow",
        )
    }
}
