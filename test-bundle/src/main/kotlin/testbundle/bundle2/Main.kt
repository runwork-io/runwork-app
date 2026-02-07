package testbundle.bundle2

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        println("[TestBundle] Starting Bundle v2 - Build #2")
        SwingUtilities.invokeLater {
            val frame = JFrame("Test Bundle v2")
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.preferredSize = Dimension(400, 300)

            val label = JLabel("Bundle v2 - Build #2", SwingConstants.CENTER)
            label.font = Font("SansSerif", Font.BOLD, 32)
            label.foreground = Color(0, 153, 0)

            frame.contentPane.add(label, BorderLayout.CENTER)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
            val updateManager = UpdateManager(currentBuildNumber = 2L)
            updateManager.startBackgroundUpdate(scope) { status ->
                SwingUtilities.invokeLater {
                    frame.title = "Test Bundle v2 - $status"
                }
            }
        }
    }
}
