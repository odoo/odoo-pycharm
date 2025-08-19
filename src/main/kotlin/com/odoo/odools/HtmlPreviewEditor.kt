package com.odoo.odools

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.concurrency.EdtExecutorService
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.io.File
import javax.swing.JComponent

class HtmlPreviewEditor(private val project: Project, private val html: String) : FileEditor, UserDataHolderBase() {
    private val browser = JBCefBrowser("about:blank")
    private val virtualFile = LightVirtualFile("preview.html", PlainTextFileType.INSTANCE, html)

    init {
        val cefClient = browser.jbCefClient

        // Intercept navigation requests
        cefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: org.cef.browser.CefBrowser?,
                frame: org.cef.browser.CefFrame?,
                request: CefRequest?,
                user_gesture: Boolean,
                is_redirect: Boolean
            ): Boolean {
                val url = request?.url ?: return false

                return handleNavigation(url)
            }
        }, browser.cefBrowser)

        // Handle popup windows (prevent blank window)
        cefClient.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame,
                target_url: String,
                target_frame_name: String
            ): Boolean {
                target_url?.let {
                    if (it.startsWith("http")) {
                        BrowserUtil.browse(it)
                        return true // block popup
                    }
                }
                return false
            }
        }, browser.cefBrowser)

        browser.loadHTML(html)
    }

    private fun handleNavigation(url: String?): Boolean {
        if (url == null) return false

        return if (url.startsWith("http")) {
            // External links → open in user browser
            BrowserUtil.browse(url)
            true
        } else if (url.startsWith("file://") && !url.contains("jbcefbrowser")) {
            // Local file → open in editor
            val filePath = url.removePrefix("file://")
            EdtExecutorService.getInstance().execute {
                openFileInEditor(filePath)
            }
            true
        } else {
            false
        }
    }

    private fun openFileInEditor(path: String) {
        val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(path))
        if (vFile != null) {
            FileEditorManager.getInstance(project).openFile(vFile, true)
        }
    }
    override fun getFile(): VirtualFile = virtualFile
    override fun getComponent(): JComponent = browser.component
    override fun getPreferredFocusedComponent(): JComponent? = browser.component
    override fun getName(): String = "HTML Preview"
    override fun setState(state: FileEditorState) {

    }

    override fun isModified() = false
    override fun isValid() = true

    override fun dispose() {
        browser.dispose()
    }

    // No-op for the rest of FileEditor’s methods
    override fun selectNotify() {}
    override fun deselectNotify() {}
    override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
}
class HtmlPreviewEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        // Decide which files should open in preview mode
        return file.fileType.name == "HTML" && file.name.endsWith(".preview.html")
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val content = file.inputStream.bufferedReader().use { it.readText() }
        return HtmlPreviewEditor(project, content)
    }

    override fun getEditorTypeId(): String = "odoo.html.preview"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}