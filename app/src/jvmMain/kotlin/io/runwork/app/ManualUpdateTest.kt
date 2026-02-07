package io.runwork.app

import io.runwork.app.shell.AppConfig
import io.runwork.app.shell.AppController
import io.runwork.app.ui.AppWindow
import java.io.File
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.seconds

/**
 * Manual test entry point for the bundle update flow.
 *
 * Points the shell at bundles/1 for the initial download. Bundle 1's own
 * UpdateManager discovers bundle 2, downloads it, and shows a restart button.
 * Clicking restart spawns a new process that re-enters here, finds bundle 2
 * already downloaded, and launches it.
 *
 * Run via: ./gradlew :app:runManualUpdateTest
 */
fun main(args: Array<String>) {
    val isRestart = args.contains("--restart")
    val bundle1Dir = File("../bundles/1").canonicalFile
    val storageDir = File("build/manual-test-storage").absoluteFile

    if (isRestart) {
        println("[ManualTest] Restart detected — reusing existing storage")
    } else {
        // First run: clean storage so the shell downloads fresh
        storageDir.deleteRecursively()
        storageDir.mkdirs()
    }

    println("Bundle base URL: file://${bundle1Dir.absolutePath}/")
    println("Bundle storage dir: $storageDir")

    SwingUtilities.invokeLater {
        val config = AppConfig(
            baseUrl = "file://${bundle1Dir.absolutePath}/",
            publicKey = "MCowBQYDK2VwAyEALfvWpE5MbxS87YZvixsKxuSS2QGGSoUJao7idEABK0Q=",
            shellVersion = 1,
            mainClass = "testbundle.bundle1.Main",
            appId = "io.runwork.app.desktop",
            appDataDir = storageDir.toPath(),
            updateCheckInterval = 5.seconds,
            enableBackgroundUpdate = false,
        )
        val window = AppWindow()
        val controller = AppController(
            window = window,
            config = config,
        )

        window.isVisible = true
        controller.start()
    }
}
