package io.runwork.app.shell

import io.runwork.app.ui.AppWindow
import io.runwork.bundle.bootstrap.BundleBootstrap
import io.runwork.bundle.bootstrap.BundleBootstrapConfig
import io.runwork.bundle.bootstrap.BundleStartEvent
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
import javax.swing.SwingUtilities

class AppController(
    private val window: AppWindow,
    private val config: AppConfig,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing),
) {
    private val _state = MutableStateFlow<AppUiState?>(null)
    val state: StateFlow<AppUiState?> = _state.asStateFlow()

    private val bootstrapConfig = if (config.appDataDir != null) {
        BundleBootstrapConfig(
            appDataDir = config.appDataDir,
            baseUrl = config.baseUrl,
            publicKey = config.publicKey,
            shellVersion = config.shellVersion,
            mainClass = config.mainClass,
        )
    } else {
        BundleBootstrapConfig(
            appId = config.appId,
            baseUrl = config.baseUrl,
            publicKey = config.publicKey,
            shellVersion = config.shellVersion,
            mainClass = config.mainClass,
        )
    }

    private val bootstrap = BundleBootstrap(bootstrapConfig)
    private var startJob: Job? = null
    private var retryJob: Job? = null
    private var currentRetryDelay = config.initialRetryDelay

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

        startJob?.cancel()
        retryJob?.cancel()
        _state.value = null
        startJob = scope.launch {
            bootstrap.validateAndLaunch().collect { event ->
                when (event) {
                    is BundleStartEvent.Progress.ValidatingManifest -> {
                        log("Validating manifest...")
                    }
                    is BundleStartEvent.Progress.ValidatingFiles -> {
                        log("Validating files: ${event.percentCompleteInt}% (${event.filesVerified}/${event.totalFiles} files)")
                    }
                    is BundleStartEvent.Progress.Downloading -> {
                        val percent = event.progress.percentCompleteInt
                        log("Download progress: $percent%")
                        window.isVisible = true
                        updateUi(AppUiState.Downloading(
                            progress = event.progress.percentComplete,
                            percentText = "$percent%"
                        ))
                    }
                    is BundleStartEvent.Progress.Launching -> {
                        log("Launching bundle...")
                        if (window.isVisible) {
                            updateUi(AppUiState.Launching)
                        }
                    }
                    is BundleStartEvent.Failed -> {
                        log("Failed: ${event.reason}, retryable=${event.isRetryable}")
                        event.cause?.let { log("  Cause: ${it.stackTraceToString()}") }
                        window.isVisible = true
                        handleError(event.reason, event.isRetryable)
                    }
                    is BundleStartEvent.ShellUpdateRequired -> {
                        log("Shell update required: current=${event.currentVersion}, required=${event.requiredVersion}")
                        window.isVisible = true
                        updateUi(AppUiState.Error("Shell update required. Please update the app.", null))
                    }
                }
            }
        }
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

    private fun handleError(message: String, isRetryable: Boolean) {
        if (!isRetryable) {
            log("Non-retryable error: $message")
            updateUi(AppUiState.Error(message, null))
            return
        }

        log("Handling retryable error: $message")
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
                log("Retrying (next delay will be $currentRetryDelay)...")
                start()
            }
        }
    }

    private fun retryNow() {
        log("Manual retry requested")
        retryJob?.cancel()
        currentRetryDelay = config.initialRetryDelay
        start()
    }

    fun shutdown() {
        log("Shutting down...")
        startJob?.cancel()
        retryJob?.cancel()
        scope.cancel()
        bootstrap.close()
    }
}
