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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AppControllerUpdateIntegrationTest {
    private lateinit var window: AppWindow
    private lateinit var tempServerDir: File
    private lateinit var tempStorageDir: File

    @BeforeTest
    fun setUp() {
        SwingUtilities.invokeAndWait {
            window = AppWindow()
            window.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        }
        tempServerDir = File.createTempFile("bundle-update-test-server", "").also {
            it.delete()
            it.mkdirs()
        }
        tempStorageDir = File.createTempFile("bundle-update-test-storage", "").also {
            it.delete()
            it.mkdirs()
        }
    }

    @AfterTest
    fun tearDown() {
        SwingUtilities.invokeAndWait {
            window.dispose()
        }
        tempServerDir.deleteRecursively()
        tempStorageDir.deleteRecursively()
    }

    private fun flushEdt() {
        SwingUtilities.invokeAndWait {}
    }

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

    private fun copyBundleToServerDir(bundleNumber: Int) {
        val bundleDir = File("../bundles/$bundleNumber").canonicalFile
        require(bundleDir.exists()) { "bundles/$bundleNumber directory not found at ${bundleDir.absolutePath}" }
        // Clear server dir contents first
        tempServerDir.listFiles()?.forEach { it.deleteRecursively() }
        bundleDir.copyRecursively(tempServerDir, overwrite = true)
    }

    @Test
    fun `full update lifecycle - install, detect update, restart, cleanup`() {
        val launchHistory = mutableListOf<BundleValidationResult.Valid>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

        try {
            // Phase 1: Install Bundle 1
            copyBundleToServerDir(1)

            val config = AppConfig(
                baseUrl = "file://${tempServerDir.absolutePath}/",
                publicKey = TEST_PUBLIC_KEY,
                shellVersion = 1,
                mainClass = "testbundle.bundle1.Main",
                appId = "io.runwork.app.desktop.updatetest",
                appDataDir = tempStorageDir.toPath(),
                updateCheckInterval = 500.milliseconds,
            )

            val controller = AppController(
                window = window,
                config = config,
                scope = scope,
                launchAction = { valid -> launchHistory.add(valid) },
            )

            controller.start()

            // Await first launch
            awaitState(controller) { it is AppUiState.Launching }
            flushEdt()

            assertIs<AppUiState.Launching>(controller.state.value)
            assertEquals(1, launchHistory.size, "launchAction should have been called once")
            assertEquals(1L, launchHistory[0].manifest.buildNumber)

            // Phase 2: Publish Bundle 2
            copyBundleToServerDir(2)

            // Phase 3: Detect update
            val updateState = awaitState(controller) { it is AppUiState.UpdateAvailable }
            assertIs<AppUiState.UpdateAvailable>(updateState)
            assertEquals(2L, updateState.newBuildNumber)

            // Phase 4: Restart
            SwingUtilities.invokeAndWait {
                window.restartButton.doClick()
            }

            // Wait for second launch
            awaitState(controller) { state ->
                state is AppUiState.Launching && launchHistory.size == 2
            }
            flushEdt()

            assertEquals(2, launchHistory.size, "launchAction should have been called twice")
            assertEquals(2L, launchHistory[1].manifest.buildNumber)

            // Phase 5: Cleanup verification
            val bundleDir = File(tempStorageDir, "bundle")

            // Wait for cleanup to complete - versions/1/ should be deleted
            val cleanupDeadline = System.currentTimeMillis() + 30_000
            while (System.currentTimeMillis() < cleanupDeadline) {
                val v1Dir = File(bundleDir, "versions/1")
                if (!v1Dir.exists()) break
                Thread.sleep(100)
            }

            val versionsDir = File(bundleDir, "versions")
            val casDir = File(bundleDir, "cas")

            // versions/1/ should be cleaned up
            assertFalse(
                File(versionsDir, "1").exists(),
                "versions/1/ should be deleted after cleanup"
            )
            // versions/2/ should still exist
            assertTrue(
                File(versionsDir, "2").exists(),
                "versions/2/ should still exist"
            )

            // Bundle 1's unique CAS file (bundle-version.txt "1") should be deleted
            val bundle1UniqueCas = File(casDir, "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b")
            assertFalse(
                bundle1UniqueCas.exists(),
                "Bundle 1's unique CAS file should be deleted"
            )

            // Shared CAS file (test-bundle.jar) should still exist
            val sharedCas = File(casDir, "e20f396c4d8519e60a74b8b57d8c3a030b1fcf61a18208e9db86e4508adee212")
            assertTrue(
                sharedCas.exists(),
                "Shared CAS file should still exist"
            )
        } finally {
            scope.cancel()
        }
    }

    companion object {
        private const val TEST_PUBLIC_KEY = "MCowBQYDK2VwAyEALfvWpE5MbxS87YZvixsKxuSS2QGGSoUJao7idEABK0Q="
    }
}
