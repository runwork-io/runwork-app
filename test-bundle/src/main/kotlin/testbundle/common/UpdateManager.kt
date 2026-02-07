package testbundle.common

import io.runwork.bundle.updater.BundleUpdater
import io.runwork.bundle.updater.BundleUpdaterConfig
import io.runwork.bundle.updater.result.DownloadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateManager(
    private val currentBuildNumber: Long,
    private val baseUrl: String = "https://updates.example.com/",
    private val publicKey: String = "MCowBQYDK2VwAyEALfvWpE5MbxS87YZvixsKxuSS2QGGSoUJao7idEABK0Q=",
) {
    private val config = BundleUpdaterConfig(
        appId = "io.runwork.testbundle",
        baseUrl = baseUrl,
        publicKey = publicKey,
        currentBuildNumber = currentBuildNumber,
    )

    private val updater = BundleUpdater(config)

    fun startBackgroundUpdate(scope: CoroutineScope, onResult: (String) -> Unit) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    updater.downloadLatest { progress ->
                        println("[UpdateManager] Download progress: ${progress.percentCompleteInt}%")
                    }
                }

                when (result) {
                    is DownloadResult.Success -> onResult("Update ready: build #${result.buildNumber}")
                    is DownloadResult.AlreadyUpToDate -> onResult("Already up to date")
                    is DownloadResult.Failure -> onResult("Update failed: ${result.error}")
                    is DownloadResult.Cancelled -> onResult("Update cancelled")
                }
            } catch (e: Exception) {
                onResult("Update error: ${e.message}")
            }
        }
    }

    fun close() {
        updater.close()
    }
}
