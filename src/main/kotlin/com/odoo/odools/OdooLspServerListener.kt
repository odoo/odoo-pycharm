package com.odoo.odools

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.lsp.api.LspServerListener
import org.eclipse.lsp4j.InitializeResult


class OdooLspServerListener(val project: Project) : LspServerListener {

    override fun serverStopped(shutdownNormally: Boolean) {
        super.serverStopped(shutdownNormally)
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        val widget = statusBar?.getWidget("OdooLspStatusWidget") as? OdooLspStatusWidget
        widget?.updateStatus("stop") //Force stop, we know server is not running, so not loading
    }

    override fun serverInitialized(params: InitializeResult) {
        super.serverInitialized(params)
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        val widget = statusBar?.getWidget("OdooLspStatusWidget") as? OdooLspStatusWidget
        widget?.updateStatus()
    }
}