package com.odoo.odools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.*
import com.intellij.platform.lsp.api.customization.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.eclipse.lsp4j.ConfigurationItem

val osName = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()

val targetOs = when {
    osName.contains("win") && arch.contains("64") -> "win32-x64"
    osName.contains("win") && arch.contains("arm") -> "win32-arm64"
    osName.contains("linux") && arch.contains("64") -> "linux-x64"
    osName.contains("linux") && arch.contains("arm") -> "linux-arm64"
    osName.contains("darwin") && arch.contains("64") -> "darwin-x64"
    osName.contains("darwin") && arch.contains("arm") -> "darwin-arm64"
    else -> throw IllegalStateException("Unsupported OS: $osName $arch")
}

private val HAS_MANIFEST_KEY = Key.create<Boolean>("OdooLS.HasManifest")

internal class OdooLSLspServerSupportProvider : LspServerSupportProvider {

    fun findManifest(project: Project): Boolean {
        project.getUserData(HAS_MANIFEST_KEY)?.let { return it }
        val scope = GlobalSearchScope.projectScope(project)
        val found = FilenameIndex.getVirtualFilesByName("__manifest__.py", scope).isNotEmpty()

        project.putUserData(HAS_MANIFEST_KEY, found)
        return found
    }

    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        val isInstalled = project.getUserData<Boolean>(ODOO_LSP_INSTALLED)
        if (isInstalled != true) {
            return
        }
        // Only run if project is loaded
        if (project.isDisposed) return

        if (!findManifest(project)) return
        if (project.service<OdooProjectSettingsService>().state.selectedProfile == "disabled") return
        println("__manifest__.py file detected in project. Starting OdooLS")

        serverStarter.ensureServerStarted(OdooLsServerDescriptor(project))
    }
}

private class OdooLsServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "OdooLS") {
    override val lspServerListener: LspServerListener?
        get() = OdooLspServerListener(project)
    override val lspCustomization: LspCustomization
        get() = object : LspCustomization() {
            override val semanticTokensCustomizer = LspSemanticTokensDisabled
            override val codeActionsCustomizer = LspCodeActionsDisabled
            override val documentColorCustomizer = LspDocumentColorDisabled
            override val formattingCustomizer = LspFormattingDisabled
        }

    override fun createLsp4jClient(handler: LspServerNotificationsHandler): Lsp4jClient {
        return OdooCustomLsp4jClient(project, handler)
    }
    override fun isSupportedFile(file: VirtualFile) = true
    override fun createCommandLine(): GeneralCommandLine {
        val exeName = if (SystemInfo.isWindows) "odoo_ls_server.exe" else "odoo_ls_server"
        val pathToInstallation = OdooLSApplicationSettings.getInstance().state.dataPath;
        val resourcePath = "${pathToInstallation}/$exeName"
        println(pathToInstallation)

        return GeneralCommandLine(resourcePath)
    }

    override fun getWorkspaceConfiguration(item: ConfigurationItem): Any? {
        if (item.section == "Odoo") {
            val sp = project.service<OdooProjectSettingsService>().state.selectedProfile
            return object {
                @Suppress("unused")
                val selectedProfile: String = sp
            }
        }
        return super.getWorkspaceConfiguration(item)
    }
}
