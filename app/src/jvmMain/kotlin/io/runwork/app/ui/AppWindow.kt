package io.runwork.app.ui

import io.runwork.app.shell.AppUiState
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingConstants
import javax.swing.WindowConstants

class AppWindow : JFrame("Moscow") {
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    // Checking panel
    private val checkingLabel = JLabel("Checking...")

    // Downloading panel
    internal val downloadProgressBar = JProgressBar(0, 100)
    internal val downloadPercentLabel = JLabel("0%")

    // Error panel
    private val errorTitleLabel = JLabel("Download Failed")
    internal val errorMessageLabel = JLabel()
    internal val retryCountdownLabel = JLabel()
    val retryButton = JButton("Retry Now")

    // Launching panel
    private val launchingLabel = JLabel("Launching...")

    private val backgroundColor = Color(0x12, 0x12, 0x12)
    private val foregroundColor = Color(0xE1, 0xE1, 0xE1)
    private val primaryColor = Color(0xBB, 0x86, 0xFC)
    private val errorColor = Color(0xCF, 0x66, 0x79)

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        preferredSize = Dimension(400, 250)
        background = backgroundColor

        contentPanel.background = backgroundColor

        contentPanel.add(createCheckingPanel(), "checking")
        contentPanel.add(createDownloadingPanel(), "downloading")
        contentPanel.add(createErrorPanel(), "error")
        contentPanel.add(createLaunchingPanel(), "launching")

        contentPane.add(contentPanel, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(null)
    }

    private fun createCheckingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = backgroundColor

        checkingLabel.font = Font("SansSerif", Font.PLAIN, 16)
        checkingLabel.foreground = foregroundColor
        checkingLabel.horizontalAlignment = SwingConstants.CENTER

        panel.add(checkingLabel)
        return panel
    }

    private fun createDownloadingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = backgroundColor

        val gbc = GridBagConstraints()
        gbc.insets = Insets(8, 32, 8, 32)
        gbc.fill = GridBagConstraints.HORIZONTAL

        val downloadingLabel = JLabel("Downloading...")
        downloadingLabel.font = Font("SansSerif", Font.PLAIN, 16)
        downloadingLabel.foreground = foregroundColor
        downloadingLabel.horizontalAlignment = SwingConstants.CENTER

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        panel.add(downloadingLabel, gbc)

        downloadProgressBar.isStringPainted = false
        downloadProgressBar.foreground = primaryColor
        downloadProgressBar.background = Color(0x30, 0x30, 0x30)
        downloadProgressBar.preferredSize = Dimension(300, 20)

        gbc.gridy = 1
        panel.add(downloadProgressBar, gbc)

        downloadPercentLabel.font = Font("SansSerif", Font.PLAIN, 14)
        downloadPercentLabel.foreground = foregroundColor
        downloadPercentLabel.horizontalAlignment = SwingConstants.CENTER

        gbc.gridy = 2
        gbc.fill = GridBagConstraints.NONE
        panel.add(downloadPercentLabel, gbc)

        return panel
    }

    private fun createErrorPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = backgroundColor

        val gbc = GridBagConstraints()
        gbc.insets = Insets(8, 32, 8, 32)
        gbc.gridx = 0
        gbc.weightx = 1.0

        errorTitleLabel.font = Font("SansSerif", Font.BOLD, 18)
        errorTitleLabel.foreground = errorColor
        errorTitleLabel.horizontalAlignment = SwingConstants.CENTER

        gbc.gridy = 0
        panel.add(errorTitleLabel, gbc)

        errorMessageLabel.font = Font("SansSerif", Font.PLAIN, 14)
        errorMessageLabel.foreground = foregroundColor
        errorMessageLabel.horizontalAlignment = SwingConstants.CENTER

        gbc.gridy = 1
        panel.add(errorMessageLabel, gbc)

        retryCountdownLabel.font = Font("SansSerif", Font.PLAIN, 12)
        retryCountdownLabel.foreground = Color(0x90, 0x90, 0x90)
        retryCountdownLabel.horizontalAlignment = SwingConstants.CENTER

        gbc.gridy = 2
        panel.add(retryCountdownLabel, gbc)

        retryButton.font = Font("SansSerif", Font.PLAIN, 14)
        retryButton.background = primaryColor
        retryButton.foreground = Color.BLACK
        retryButton.isFocusPainted = false
        retryButton.border = BorderFactory.createEmptyBorder(8, 16, 8, 16)

        gbc.gridy = 3
        gbc.fill = GridBagConstraints.NONE
        panel.add(retryButton, gbc)

        return panel
    }

    private fun createLaunchingPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = backgroundColor

        launchingLabel.font = Font("SansSerif", Font.PLAIN, 16)
        launchingLabel.foreground = foregroundColor
        launchingLabel.horizontalAlignment = SwingConstants.CENTER

        panel.add(launchingLabel)
        return panel
    }

    fun updateState(state: AppUiState) {
        when (state) {
            is AppUiState.Checking -> {
                cardLayout.show(contentPanel, "checking")
            }
            is AppUiState.Downloading -> {
                downloadProgressBar.value = (state.progress * 100).toInt()
                downloadPercentLabel.text = state.percentText
                cardLayout.show(contentPanel, "downloading")
            }
            is AppUiState.Error -> {
                errorMessageLabel.text = state.message
                retryCountdownLabel.text = if (state.retryInSeconds != null) {
                    "Retrying in ${state.retryInSeconds} seconds..."
                } else {
                    ""
                }
                cardLayout.show(contentPanel, "error")
            }
            is AppUiState.Launching -> {
                cardLayout.show(contentPanel, "launching")
            }
        }
    }
}
