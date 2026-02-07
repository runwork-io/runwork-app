package io.runwork.app

import io.runwork.app.shell.AppConfig
import io.runwork.app.shell.AppController
import io.runwork.app.ui.AppWindow
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val window = AppWindow()
        val controller = AppController(window, AppConfig.DEFAULT)
        window.isVisible = true
        controller.start()
    }
}
