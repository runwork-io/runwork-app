package io.runwork.app.shell

sealed class AppUiState {
    data object Checking : AppUiState()
    data class Downloading(val progress: Float, val percentText: String) : AppUiState()
    data class Error(val message: String, val retryInSeconds: Int?) : AppUiState()
    data object Launching : AppUiState()
    data class UpdateAvailable(val newBuildNumber: Long) : AppUiState()
}
