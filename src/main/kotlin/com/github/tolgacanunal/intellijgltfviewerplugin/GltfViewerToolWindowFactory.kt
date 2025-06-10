package com.github.tolgacanunal.intellijgltfviewerplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser

class GltfViewerToolWindowFactory : ToolWindowFactory {

    private var toolWindowRef: ToolWindow? = null
    private var projectRef: Project? = null

    private fun getBaseUrl() = "http://localhost:${GltfHttpServer.port}"

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        this.toolWindowRef = toolWindow
        this.projectRef = project
        GltfHttpServer.start()

        val browser = JBCefBrowser("${getBaseUrl()}/viewer/index.html")
        val content = ContentFactory
            .getInstance()
            .createContent(browser.component, "View", false)

        toolWindow.contentManager.addContent(content)

        val connection = project.messageBus.connect(toolWindow.disposable)
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    if (event.newFile?.extension?.lowercase() !in listOf("gltf", "glb")) {
                        hideToolWindow()
                        return
                    }
                    if (event.newFile == null) {
                        return
                    }
                    loadModel(browser, event.newFile!!)
                    activateToolWindow()
                }
            }
        )
    }

    private fun loadModel(browser: JBCefBrowser, file: VirtualFile) {
        val path = file.path
        val url = "${getBaseUrl()}/files$path"
        browser.cefBrowser.executeJavaScript("loadGltf('$url')", file.url, 0)
    }

    private fun activateToolWindow() {
        if (projectRef == null || toolWindowRef == null || toolWindowRef?.isVisible == true) {
            return
        }
        val editor =  FileEditorManager.getInstance(projectRef!!).selectedEditor
        val restoreFocus = Runnable {
            IdeFocusManager.getInstance(projectRef).doWhenFocusSettlesDown {
                ApplicationManager.getApplication().invokeLater { editor?.component?.requestFocus() }
            }
        }
        toolWindowRef?.activate(restoreFocus, false, false)
    }

    private fun hideToolWindow() {
        if (toolWindowRef == null || toolWindowRef?.isVisible == false) {
            return
        }
        toolWindowRef?.hide()
    }
}
