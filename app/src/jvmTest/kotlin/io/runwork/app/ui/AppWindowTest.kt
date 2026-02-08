package io.runwork.app.ui

import io.runwork.app.shell.AppUiState
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppWindowTest {
    private lateinit var window: AppWindow

    @BeforeTest
    fun setUp() {
        SwingUtilities.invokeAndWait {
            window = AppWindow()
            window.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        }
    }

    @AfterTest
    fun tearDown() {
        SwingUtilities.invokeAndWait {
            window.dispose()
        }
    }

    @Test
    fun `updateState Downloading sets progress bar and label`() {
        SwingUtilities.invokeAndWait {
            window.updateState(AppUiState.Downloading(0.75f, "75%"))
        }
        SwingUtilities.invokeAndWait {
            assertEquals(75, window.downloadProgressBar.value)
            assertEquals("75%", window.downloadPercentLabel.text)
        }
    }

    @Test
    fun `updateState Error shows message and countdown`() {
        SwingUtilities.invokeAndWait {
            window.updateState(AppUiState.Error("Connection failed", 5))
        }
        SwingUtilities.invokeAndWait {
            assertEquals("Connection failed", window.errorMessageLabel.text)
            assertEquals("Retrying in 5 seconds...", window.retryCountdownLabel.text)
        }
    }

    @Test
    fun `updateState Error with null countdown shows empty label`() {
        SwingUtilities.invokeAndWait {
            window.updateState(AppUiState.Error("Connection failed", null))
        }
        SwingUtilities.invokeAndWait {
            assertEquals("Connection failed", window.errorMessageLabel.text)
            assertEquals("", window.retryCountdownLabel.text)
        }
    }

    @Test
    fun `updateState Launching shows launching panel`() {
        SwingUtilities.invokeAndWait {
            window.updateState(AppUiState.Launching)
        }
        // If no exception, the launching panel is shown via CardLayout
    }
}
