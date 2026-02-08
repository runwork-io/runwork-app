package testbundle.bundle2

import io.runwork.bundle.common.BundleLaunchConfig
import kotlinx.serialization.json.Json
import testbundle.common.UpdateManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import kotlin.system.exitProcess

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        println("[TestBundle] Starting Bundle v2 - Build #2")
        val config = Json.decodeFromString<BundleLaunchConfig>(args[0])

        SwingUtilities.invokeLater {
            val frame = JFrame("Test Bundle v2")
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    exitProcess(0)
                }
            })
            frame.preferredSize = Dimension(400, 300)

            val label = JLabel("Bundle v2 - Build #2", SwingConstants.CENTER)
            label.font = Font("SansSerif", Font.BOLD, 32)
            label.foreground = Color(0, 153, 0)

            frame.contentPane.add(label, BorderLayout.CENTER)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
            val updateManager = UpdateManager(config)
            updateManager.startBackgroundUpdate(scope) { status ->
                SwingUtilities.invokeLater {
                    frame.title = "Test Bundle v2 - $status"
                }
            }
        }

        Thread.currentThread().join()
    }
}
