package io.runwork.app.shell

import io.runwork.app.ui.AppWindow
import io.runwork.bundle.bootstrap.BundleBootstrap
import io.runwork.bundle.bootstrap.BundleBootstrapConfig
import io.runwork.bundle.bootstrap.BundleBootstrapProgress
import io.runwork.bundle.bootstrap.BundleValidationResult
import io.runwork.bundle.updater.BundleUpdater
import io.runwork.bundle.updater.BundleUpdaterConfig
import io.runwork.bundle.updater.download.DownloadProgress
import io.runwork.bundle.updater.result.DownloadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import javax.swing.SwingUtilities
import kotlin.time.Duration

class AppController(
    private val window: AppWindow,
    private val config: AppConfig,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing),
    private val launchAction: ((BundleValidationResult.Valid) -> Unit)? = null,
) {
    private val _state = MutableStateFlow<AppUiState>(AppUiState.Checking)
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private val bootstrapConfig = BundleBootstrapConfig(
        appId = config.appId,
        baseUrl = config.baseUrl,
        publicKey = config.publicKey,
        shellVersion = config.shellVersion,
        mainClass = config.mainClass,
    )

    private val updaterConfig = BundleUpdaterConfig(
        appId = config.appId,
        baseUrl = config.baseUrl,
        publicKey = config.publicKey,
        currentBuildNumber = 0L,
    )

    private val bootstrap = BundleBootstrap(bootstrapConfig)
    private val updater = BundleUpdater(updaterConfig)

    private var downloadJob: Job? = null
    private var retryJob: Job? = null
    private var currentRetryDelay = config.initialRetryDelay
    private var lastProgressTime = 0L

    init {
        window.retryButton.addActionListener {
            retryNow()
        }
    }

    fun start() {
        log("Starting AppController")
        log("  BASE_URL: ${config.baseUrl}")
        log("  APP_ID: ${config.appId}")
        log("  SHELL_VERSION: ${config.shellVersion}")
        log("  MAIN_CLASS: ${config.mainClass}")
        checkAndDownloadBundle()
    }

    private fun log(message: String) {
        println("[AppController] $message")
    }

    private fun updateUi(state: AppUiState) {
        _state.value = state
        SwingUtilities.invokeLater {
            window.updateState(state)
        }
    }

    private suspend fun validateWithTiming(context: String): BundleValidationResult {
        log("$context: Starting validation...")
        val validationStartTime = System.currentTimeMillis()
        var lastPhaseTime = validationStartTime
        var lastPhase: String? = null

        val result = bootstrap.validate { progress ->
            val now = System.currentTimeMillis()
            val phaseName = when (progress) {
                is BundleBootstrapProgress.LoadingManifest -> "LoadingManifest"
                is BundleBootstrapProgress.VerifyingSignature -> "VerifyingSignature"
                is BundleBootstrapProgress.VerifyingFiles -> "VerifyingFiles(${progress.filesVerified}/${progress.totalFiles})"
                is BundleBootstrapProgress.Complete -> "Complete"
            }

            if (lastPhase != null) {
                val phaseDuration = now - lastPhaseTime
                log("$context:   Phase '$lastPhase' took ${phaseDuration}ms")
            }

            log("$context:   Progress: $phaseName")
            lastPhase = phaseName
            lastPhaseTime = now
        }

        val totalValidationTime = System.currentTimeMillis() - validationStartTime
        log("$context: Validation completed in ${totalValidationTime}ms")
        return result
    }

    private fun checkAndDownloadBundle() {
        scope.launch {
            log("Checking for existing bundle...")
            updateUi(AppUiState.Checking)

            val result = validateWithTiming("Initial")
            log("Validation result: $result")

            when (result) {
                is BundleValidationResult.Valid -> {
                    log("Bundle is valid, launching...")
                    launchBundle(result)
                }
                is BundleValidationResult.NoBundleExists -> {
                    log("No bundle exists, starting download...")
                    startDownload()
                }
                is BundleValidationResult.Failed -> {
                    log("Bundle validation failed: ${result.reason}, starting download...")
                    startDownload()
                }
                is BundleValidationResult.NetworkError -> {
                    log("Network error during validation: ${result.message}")
                    handleDownloadError("Network error: ${result.message}")
                }
                is BundleValidationResult.ShellUpdateRequired -> {
                    log("Shell update required: requiredVersion=${result.requiredVersion}, currentVersion=${result.currentVersion}")
                    handleDownloadError("Shell update required. Please update the app.")
                }
            }
        }
    }

    private fun startDownload() {
        downloadJob?.cancel()
        retryJob?.cancel()

        downloadJob = scope.launch {
            log("Starting download...")
            updateUi(AppUiState.Downloading(0f, "0%"))

            try {
                log("Calling updater.downloadLatest() on IO dispatcher...")
                log("  Updater config: baseUrl=${config.baseUrl}, appId=${config.appId}")
                val result = withContext(Dispatchers.IO) {
                    log("  Inside IO dispatcher, calling downloadLatest...")
                    val res = updater.downloadLatest { progress: DownloadProgress ->
                        val percent = progress.percentCompleteInt
                        log("Download progress: $percent% (${progress.bytesDownloaded}/${progress.totalBytes} bytes, file: ${progress.currentFile})")
                        updateUi(AppUiState.Downloading(
                            progress = progress.percentComplete,
                            percentText = "$percent%"
                        ))
                    }
                    log("  downloadLatest returned: $res")
                    res
                }

                log("Download result: $result")

                when (result) {
                    is DownloadResult.Success -> {
                        log("Download successful, build number: ${result.buildNumber}")
                        currentRetryDelay = config.initialRetryDelay
                        // Re-validate to get the Valid result needed for launch
                        when (val validationResult = validateWithTiming("Post-download")) {
                            is BundleValidationResult.Valid -> {
                                log("Post-download validation successful")
                                launchBundle(validationResult)
                            }
                            else -> {
                                log("Post-download validation failed: $validationResult")
                                handleDownloadError("Downloaded bundle failed validation")
                            }
                        }
                    }
                    is DownloadResult.Failure -> {
                        log("Download failed: ${result.error}")
                        result.cause?.let { log("  Cause: ${it.stackTraceToString()}") }
                        handleDownloadError(result.error)
                    }
                    is DownloadResult.Cancelled -> {
                        log("Download was cancelled")
                        // Download was cancelled, do nothing
                    }
                    is DownloadResult.AlreadyUpToDate -> {
                        log("Already up to date, re-validating...")
                        // Already up to date, re-validate and launch
                        when (val validationResult = validateWithTiming("AlreadyUpToDate")) {
                            is BundleValidationResult.Valid -> {
                                log("Validation successful, launching...")
                                launchBundle(validationResult)
                            }
                            else -> {
                                log("Validation failed after AlreadyUpToDate: $validationResult")
                                handleDownloadError("Bundle validation failed after update check")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log("Exception during download: ${e.message}")
                log("  Stack trace: ${e.stackTraceToString()}")
                if (isActive) {
                    handleDownloadError(e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    private fun handleDownloadError(message: String) {
        log("Handling error: $message")
        val retrySeconds = currentRetryDelay.inWholeSeconds.toInt()
        log("Will retry in $retrySeconds seconds")
        updateUi(AppUiState.Error(message, retrySeconds))

        retryJob?.cancel()
        retryJob = scope.launch {
            var remaining = retrySeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                if (remaining > 0) {
                    updateUi(AppUiState.Error(message, remaining))
                }
            }

            if (isActive) {
                currentRetryDelay = minOf(currentRetryDelay * 2, config.maxRetryDelay)
                log("Retrying download (next delay will be $currentRetryDelay)...")
                startDownload()
            }
        }
    }

    private fun retryNow() {
        log("Manual retry requested")
        retryJob?.cancel()
        currentRetryDelay = config.initialRetryDelay
        startDownload()
    }

    private fun launchBundle(validation: BundleValidationResult.Valid) {
        log("Launching bundle...")
        log("  Manifest: ${validation.manifest}")
        log("  Version path: ${validation.versionPath}")
        updateUi(AppUiState.Launching)

        if (launchAction != null) {
            launchAction.invoke(validation)
            return
        }

        try {
            bootstrap.launch(validation)
            log("Launch successful")
            // Hide the shell window - the bundle is now running
            window.isVisible = false
        } catch (e: Exception) {
            log("Launch failed: ${e.message}")
            log("  Stack trace: ${e.stackTraceToString()}")
            handleDownloadError("Failed to launch: ${e.message}")
        }
    }

    fun shutdown() {
        log("Shutting down...")
        scope.cancel()
        updater.close()
    }
}
