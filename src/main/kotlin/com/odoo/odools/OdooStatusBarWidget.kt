package com.odoo.odools

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.platform.lsp.api.LspServerState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.testFramework.LightVirtualFile
import java.awt.Desktop
import java.nio.file.Paths

class OdooLspStatusWidget(
    project: Project
) : EditorBasedStatusBarPopup(project, false) {

    private var widgetState = WidgetState("Odoo", "Odoo (not started)", true)
    private var listProfiles: List<String> = listOf("default", "disabled")
    private var gotProfiles: Boolean = false
    private var config_html: Map<String, Any> = mapOf();
    private var currentStatus = "stop"

    private var popup: JBPopup? = null

    override fun ID(): String = "OdooLspStatusWidget"

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        // This controls what is displayed in the status bar
        return widgetState
    }

    fun updateListProfiles(profiles: List<String>) {
        listProfiles = profiles
        gotProfiles = true
    }

    fun updateConfigurations(html: Map<String, Any>) {
        this.config_html = html;
    }

    fun updateStatus(status: String? = null) {
        var full_name = "Odoo"
        val profile = project.service<OdooProjectSettingsService>().state.selectedProfile
        full_name += " ($profile)"
        if (status == "start") {
            currentStatus = "start"
        } else if (status == "stop") {
            currentStatus = "stop"
        }
        if (currentStatus == "start") {
            full_name += " - Loading"
        }
        widgetState = WidgetState("Odoo LS plugin", full_name, true)
        update()
        val popup = this.popup
        if (popup != null) {
            ApplicationManager.getApplication().invokeLater {
                popup.cancel()
            }
        }
    }

    inner class ProfileSelectorWithWheelAction(
        private val project: Project,
        private val text: String,
        private val option: String
    ) : ToggleAction(text) {

        override fun isSelected(e: AnActionEvent): Boolean {
            return option == project.service<OdooProjectSettingsService>().state.selectedProfile
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                project.service<OdooProjectSettingsService>().state.selectedProfile = option
                if (option == "disabled") {
                    LspServerManager.getInstance(project)
                        .stopServers(OdooLSLspServerSupportProvider::class.java)
                    updateStatus()
                } else {
                    LspServerManager.getInstance(project)
                        .stopAndRestartIfNeeded(OdooLSLspServerSupportProvider::class.java)
                    updateStatus()
                }
            }
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }
    }

    fun openHtmlInEditor(project: Project, html: String, title: String = "Preview.html") {
        val file: VirtualFile = LightVirtualFile(title, HtmlFileType.INSTANCE, html)
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    override fun createPopup(context: DataContext): ListPopup {

        val lspServer = LspServerManager.getInstance(project).getServersForProvider(OdooLSLspServerSupportProvider::class.java).firstOrNull();
        var isRunning = false;
        if (lspServer != null && lspServer.state == LspServerState.Running) {
            isRunning = true
        }

        val group = DefaultActionGroup().apply {
            if (gotProfiles) { //Do not show profiles if only "default" and "disabled" are availables
                add(Separator.create("Profiles:"))
                for (availableProfile in listProfiles) {
                    add(ProfileSelectorWithWheelAction(project, availableProfile, availableProfile))
                }
            }
            add(Separator.create("Options:"))
            if (isRunning) {
                add(object : AnAction("Show Configurations") {
                    override fun actionPerformed(e: AnActionEvent) {
                        openHtmlInEditor(project, config_html.get("__all__") as String, "All_configurations.preview.html")
                    }
                })
            }
            var textStart = "Start Server"
            if (isRunning) {
                textStart = "Restart Server"
            }
            add(object : AnAction(textStart) {
                override fun actionPerformed(e: AnActionEvent) {
                    LspServerManager.getInstance(project)
                        .stopAndRestartIfNeeded(OdooLSLspServerSupportProvider::class.java)
                }
            })
            add(object : AnAction("Open Logs") {
                override fun actionPerformed(e: AnActionEvent) {
                    val pathToInstallation = OdooLSApplicationSettings.getInstance().state.dataPath;
                    if (pathToInstallation != null) {
                        val file = Paths.get(pathToInstallation, "logs").toFile();
                        if (file.exists()) {
                            Desktop.getDesktop().open(file)
                        }
                    }
                }
            })
            add(object : AnAction("Go to Settings") {
                override fun actionPerformed(e: AnActionEvent) {
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, "OdooLS")
                }
            })
        }

        var title = "Odoo LS: Stopped"
        if (isRunning) {
            title = "Odoo LS: Running"
        }

        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            title,
            group,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
            null,
            -1,
            null,
            ActionPlaces.POPUP
        )
        this.popup = popup
        return popup
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation? = null

    override fun createInstance(project: Project): StatusBarWidget = OdooLspStatusWidget(project)
    override fun dispose() {}
}

class OdooLspStatusWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "OdooLspStatusWidget"
    override fun getDisplayName(): String = "Odoo LSP Status"
    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return OdooLspStatusWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {}
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}