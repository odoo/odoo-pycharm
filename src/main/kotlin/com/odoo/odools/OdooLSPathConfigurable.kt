package com.odoo.odools

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel

class OdooLSPathConfigurable : Configurable {

    private val pathField = TextFieldWithBrowseButton()
    private val fileChooserDescriptor = FileChooserDescriptor(false, true, false, false, false, false)

    override fun getDisplayName(): String = "OdooLS"

    override fun createComponent(): JComponent {
        val container = JPanel(GridBagLayout())
        var gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            weightx = 1.0
        }
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weighty = 0.0
        container.add(JBLabel("Path to OdooLS installation:"), gbc)
        gbc.gridy = 1
        fileChooserDescriptor.title = "OdooLS Path"
        fileChooserDescriptor.description = "Choose OdooLS path installation (must contain binary and typeshed directory)"
        pathField.addBrowseFolderListener(null, fileChooserDescriptor)
        container.add(pathField, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 1
        val description = JLabel(
            """
            <html>
                <body style='width: 500px; color: gray;'>
                    The plugin will use this path to install needed resources and logs.<br>
                </body>
            </html>
            """.trimIndent()
        )
        description.font = description.font.deriveFont(description.font.size2D - 1f)
        description.foreground = JBColor.GRAY
        container.add(description, gbc)

        //outerPanel to keep everything at top of the window
        val outerPanel = JPanel(BorderLayout())
        outerPanel.add(container, BorderLayout.NORTH)

        return outerPanel
    }

    override fun isModified(): Boolean {
        val settings = OdooLSApplicationSettings.getInstance().state
        return settings.dataPath != pathField.text
    }

    override fun apply() {
        val settings = OdooLSApplicationSettings.getInstance().state
        settings.dataPath = pathField.text
    }

    override fun reset() {
        val settings = OdooLSApplicationSettings.getInstance().state
        pathField.text = settings.dataPath ?: ""
    }
}