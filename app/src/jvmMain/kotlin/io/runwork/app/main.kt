package io.runwork.app

import io.runwork.app.shell.AppConfig
import io.runwork.app.shell.AppController
import io.runwork.app.ui.AppWindow
import java.io.File
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val config = AppConfig(
            baseUrl = "file://${File(System.getProperty("user.home"), ".moscow-test-bundle").absolutePath}/",
            publicKey = "MCowBQYDK2VwAyEALfvWpE5MbxS87YZvixsKxuSS2QGGSoUJao7idEABK0Q=",
            shellVersion = 1,
            mainClass = "testbundle.bundle1.Main",
            appId = "io.runwork.app.desktop",
        )
        val window = AppWindow()
        val controller = AppController(window, config)
        controller.start()
    }
}
