package io.runwork.app.shell

import io.runwork.app.ui.AppWindow
import io.runwork.bundle.bootstrap.BundleValidationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.swing.Swing
import java.io.File
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class AppControllerIntegrationTest {
    private lateinit var window: AppWindow

    @BeforeTest
    fun setUp() {
        SwingUtilities.invokeAndWait {
            window = AppWindow()
            window.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        }
    }

    @AfterTest
    fun tearDown() {
        SwingUtilities.invokeAndWait {
            window.dispose()
        }
    }

    private fun flushEdt() {
        SwingUtilities.invokeAndWait {}
    }

    /**
     * Poll for the controller state to match a predicate, with a real-time timeout.
     * Used for tests that involve real I/O (not virtual-time tests).
     */
    private fun awaitState(
        controller: AppController,
        timeoutMs: Long = 30_000,
        predicate: (AppUiState) -> Boolean,
    ): AppUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val current = controller.state.value
            if (predicate(current)) return current
            Thread.sleep(50)
        }
        error("Timed out waiting for state. Current: ${controller.state.value}")
    }

    @Test
    fun `valid bundle transitions from Checking to Launching`() {
        var launchedValidation: BundleValidationResult.Valid? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

        try {
            val controller = AppController(
                window = window,
                config = testBundleConfig(),
                scope = scope,
                launchAction = { valid -> launchedValidation = valid },
            )

            controller.start()

            awaitState(controller) { it is AppUiState.Launching }
            flushEdt()

            val state = controller.state.value
            assertIs<AppUiState.Launching>(state)
            assertNotNull(launchedValidation, "launchAction should have been called with a Valid result")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `no bundle at path enters Error state`() {
        val emptyDir = createEmptyTempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

        try {
            val config = emptyDirConfig(emptyDir)

            val controller = AppController(
                window = window,
                config = config,
                scope = scope,
            )

            controller.start()

            awaitState(controller) { it is AppUiState.Error }
            flushEdt()

            val state = controller.state.value
            assertIs<AppUiState.Error>(state)
            assertNotNull(state.retryInSeconds)
        } finally {
            scope.cancel()
            emptyDir.deleteRecursively()
        }
    }

    @Test
    fun `error countdown decrements each second`() {
        val emptyDir = createEmptyTempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

        try {
            val config = emptyDirConfig(emptyDir, initialRetryDelayMs = 4_000L)

            val controller = AppController(
                window = window,
                config = config,
                scope = scope,
            )

            controller.start()

            awaitState(controller) { it is AppUiState.Error }

            val initialState = controller.state.value
            assertIs<AppUiState.Error>(initialState)
            assertEquals(4, initialState.retryInSeconds)

            // Wait for countdown to decrement (real time)
            awaitState(controller) { state ->
                state is AppUiState.Error && state.retryInSeconds == 3
            }

            awaitState(controller) { state ->
                state is AppUiState.Error && state.retryInSeconds == 2
            }
        } finally {
            scope.cancel()
            emptyDir.deleteRecursively()
        }
    }

    @Test
    fun `retry button resets delay and restarts download`() {
        val emptyDir = createEmptyTempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

        try {
            val config = emptyDirConfig(emptyDir)

            val controller = AppController(
                window = window,
                config = config,
                scope = scope,
            )

            controller.start()

            awaitState(controller) { it is AppUiState.Error }
            assertIs<AppUiState.Error>(controller.state.value)

            // Click retry button
            SwingUtilities.invokeAndWait {
                window.retryButton.doClick()
            }

            // After retry, should go through download attempt and end up in error again
            // Wait for state to leave Error (goes to Downloading), then return to Error
            awaitState(controller) { it !is AppUiState.Error }
            awaitState(controller) { it is AppUiState.Error }
            flushEdt()

            val stateAfterRetry = controller.state.value
            assertIs<AppUiState.Error>(stateAfterRetry)
            // After manual retry, delay resets to initial, so countdown = 2
            assertEquals(2, stateAfterRetry.retryInSeconds)
        } finally {
            scope.cancel()
            emptyDir.deleteRecursively()
        }
    }

    @Test
    fun `exponential backoff on repeated failures`() {
        val emptyDir = createEmptyTempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

        try {
            val config = emptyDirConfig(emptyDir)

            val controller = AppController(
                window = window,
                config = config,
                scope = scope,
            )

            controller.start()

            // First failure: retry in 2 seconds
            awaitState(controller) { it is AppUiState.Error }
            val firstError = controller.state.value
            assertIs<AppUiState.Error>(firstError)
            assertEquals(2, firstError.retryInSeconds)

            // Wait for auto-retry to trigger, then second failure (retry delay doubles to 4s)
            awaitState(controller) { state ->
                state is AppUiState.Error && state.retryInSeconds == 4
            }
            val secondError = controller.state.value
            assertIs<AppUiState.Error>(secondError)
            assertEquals(4, secondError.retryInSeconds)

            // Wait for auto-retry to trigger, then third failure (retry delay doubles to 8s)
            awaitState(controller) { state ->
                state is AppUiState.Error && state.retryInSeconds == 8
            }
            val thirdError = controller.state.value
            assertIs<AppUiState.Error>(thirdError)
            assertEquals(8, thirdError.retryInSeconds)
        } finally {
            scope.cancel()
            emptyDir.deleteRecursively()
        }
    }

    private fun createEmptyTempDir(): File {
        return File.createTempFile("empty-bundle", "").also {
            it.delete()
            it.mkdirs()
        }
    }

    private fun testBundleConfig(): AppConfig {
        val testBundleDir = File("../test-bundle").canonicalFile
        require(testBundleDir.exists()) { "test-bundle directory not found at ${testBundleDir.absolutePath}" }
        return AppConfig(
            baseUrl = "file://${testBundleDir.absolutePath}/",
            publicKey = TEST_PUBLIC_KEY,
            shellVersion = 1,
            mainClass = "demo.Main",
            appId = "io.runwork.app.desktop",
        )
    }

    private fun emptyDirConfig(
        dir: File,
        initialRetryDelayMs: Long = 2_000L,
    ) = AppConfig(
        baseUrl = "file://${dir.absolutePath}/",
        publicKey = TEST_PUBLIC_KEY,
        shellVersion = 1,
        mainClass = "demo.Main",
        appId = "io.runwork.app.desktop.test",
        initialRetryDelayMs = initialRetryDelayMs,
        maxRetryDelayMs = 60_000L,
    )

    companion object {
        private const val TEST_PUBLIC_KEY = "MCowBQYDK2VwAyEALfvWpE5MbxS87YZvixsKxuSS2QGGSoUJao7idEABK0Q="
    }
}
