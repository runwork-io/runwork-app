package testbundle.bundle1

import io.runwork.bundle.common.BundleLaunchConfig
import kotlinx.serialization.json.Json
import testbundle.common.UpdateManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
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
        println("[TestBundle] Starting Bundle v1 - Build #1")
        val config = Json.decodeFromString<BundleLaunchConfig>(args[0])

        // Point the update check at bundles/2/ so we discover the new version
        val updateConfig = config.copy(baseUrl = config.baseUrl.replace("/bundles/1/", "/bundles/2/"))

        SwingUtilities.invokeLater {
            val frame = JFrame("Test Bundle")
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    exitProcess(0)
                }
            })
            frame.preferredSize = Dimension(400, 300)

            val label = JLabel("Bundle v1 - Build #1", SwingConstants.CENTER)
            label.font = Font("SansSerif", Font.BOLD, 32)

            val restartButton = JButton("Restart to Update")
            restartButton.font = Font("SansSerif", Font.PLAIN, 14)
            restartButton.background = Color(0xBB, 0x86, 0xFC)
            restartButton.foreground = Color.BLACK
            restartButton.isVisible = false
            restartButton.addActionListener {
                println("[TestBundle] Restart clicked — spawning new process and exiting")
                val javaHome = System.getProperty("java.home")
                val javaBin = "$javaHome/bin/java"
                val classpath = System.getProperty("java.class.path")
                ProcessBuilder(javaBin, "-cp", classpath, "io.runwork.app.ManualUpdateTestKt", "--restart")
                    .inheritIO()
                    .start()
                exitProcess(0)
            }

            val bottomPanel = JPanel()
            bottomPanel.add(restartButton)

            frame.contentPane.add(label, BorderLayout.CENTER)
            frame.contentPane.add(bottomPanel, BorderLayout.SOUTH)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
            val updateManager = UpdateManager(updateConfig)
            updateManager.startBackgroundUpdate(scope) { status ->
                SwingUtilities.invokeLater {
                    frame.title = "Test Bundle - $status"
                    if (status.startsWith("Update ready")) {
                        restartButton.isVisible = true
                    }
                }
            }
        }

        Thread.currentThread().join()
    }
}
