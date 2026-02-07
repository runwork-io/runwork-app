package testbundle.bundle1

import testbundle.common.UpdateManager
import java.awt.BorderLayout
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
        println("[TestBundle] Starting Bundle v1 - Build #1")
        SwingUtilities.invokeLater {
            val frame = JFrame("Test Bundle")
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.preferredSize = Dimension(400, 300)

            val label = JLabel("Bundle v1 - Build #1", SwingConstants.CENTER)
            label.font = Font("SansSerif", Font.BOLD, 32)

            frame.contentPane.add(label, BorderLayout.CENTER)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
            val updateManager = UpdateManager(currentBuildNumber = 1L)
            updateManager.startBackgroundUpdate(scope) { status ->
                SwingUtilities.invokeLater {
                    frame.title = "Test Bundle - $status"
                }
            }
        }
    }
}
