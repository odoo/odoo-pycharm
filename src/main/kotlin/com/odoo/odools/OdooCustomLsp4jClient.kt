package com.odoo.odools

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import com.intellij.util.concurrency.AppExecutorUtil
import com.odoo.odools.forms.CrashReportForm
import com.odoo.odools.params.DisplayCrashNotificationParams
import com.odoo.odools.params.SetConfigurationParams
import com.odoo.odools.params.SetPidParams
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.use

class OdooCustomLsp4jClient(val project: Project, handler: LspServerNotificationsHandler): Lsp4jClient(handler) {

    @Suppress("unused")
    @JsonNotification($$"$Odoo/setPid")
    fun setPid(pid: SetPidParams){
        println("Got PID to watch (not implemented feature on PyCharm: $pid")
    }

    @Suppress("unused")
    @JsonNotification($$"$Odoo/setConfiguration")
    fun setConfiguration(payload: SetConfigurationParams) {
        var html = payload.getHtml()
        var profiles = html.keys.filter { it != "__all__" }.toMutableList()
        profiles.add("disabled")
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        val widget = statusBar?.getWidget("OdooLspStatusWidget") as? OdooLspStatusWidget
        widget?.updateListProfiles(profiles)
        widget?.updateConfigurations(payload.getHtml())
    }

    @Suppress("unused")
    @JsonNotification($$"$Odoo/loadingStatusUpdate")
    fun loadingStatusUpdate(status: String){
        println("Got loadingStatusUpdate: $status")
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        val widget = statusBar?.getWidget("OdooLspStatusWidget") as? OdooLspStatusWidget
        widget?.updateStatus(status)
    }

    @Suppress("unused")
    @JsonNotification($$"$Odoo/restartNeeded")
    fun restartNeeded() {
        println("Got restartNeeded")
        LspServerManager.getInstance(project)
            .stopAndRestartIfNeeded(OdooLSLspServerSupportProvider::class.java)
    }

    @Suppress("unused")
    @JsonNotification("Odoo/displayCrashNotification")
    fun displayCrashNotification(params: DisplayCrashNotificationParams) {
        AppExecutorUtil.getAppScheduledExecutorService().schedule({
            val notification = Notification(
                "OdooLS Notifications",
                "Odoo LS crashed",
                "The Odoo language server has stopped unexpectedly.",
                NotificationType.ERROR
            )

            // Add an action to show the "Send crash report" dialog
            notification.addAction(object : AnAction("Send Crash Report") {
                override fun actionPerformed(e: AnActionEvent) {
                    val currentFile = getCurrentFile();
                    val logs = getCurrentLogs(params.getPid());
                    if (logs != null) {
                        CrashReportForm(project, currentFile, logs).show()
                    }
                }
            })

            Notifications.Bus.notify(notification, project)
        }, 1, TimeUnit.SECONDS)
    }



    private fun getCurrentFile(): VirtualFile? {
        return if (FileEditorManager.getInstance(project).selectedFiles.size > 0)
            FileEditorManager.getInstance(project).selectedFiles[0]
        else
            null
    }

    private fun getCurrentLogs(pid: Long): String? {
        val pathToInstallation = OdooLSApplicationSettings.getInstance().state.dataPath;
        if (pathToInstallation != null) {
            val logDir = Paths.get(pathToInstallation, "logs");
            val files: List<String> = Files.list(logDir).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) }
                    .map{ it.fileName.toString() }
                    .filter { s -> s.endsWith(".$pid.log") }
                    .toList()
            }
            if (files.isNotEmpty()) {
                return Paths.get(logDir.toString(), files[0]).toString()
            }
            return null;
        }
        return null;
    }
}