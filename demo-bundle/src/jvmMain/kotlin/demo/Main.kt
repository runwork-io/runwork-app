package demo

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            val frame = JFrame("Demo Bundle")
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.preferredSize = Dimension(400, 300)

            val label = JLabel("Hello World", SwingConstants.CENTER)
            label.font = Font("SansSerif", Font.BOLD, 32)

            frame.contentPane.add(label, BorderLayout.CENTER)
            frame.pack()
            frame.setLocationRelativeTo(null)
            frame.isVisible = true
        }
    }
}
