package com.github.tolgacanunal.intellijgltfviewerplugin

import com.github.tolgacanunal.intellijgltfviewerplugin.GltfHttpServer
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser

class GltfViewerToolWindowFactory : ToolWindowFactory {

    private fun getBaseUrl() = "http://localhost:${GltfHttpServer.port}"

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        GltfHttpServer.start()

        val browser = JBCefBrowser("${getBaseUrl()}/viewer/index.html")
        val content = ContentFactory
            .getInstance()
            .createContent(browser.component, "glTF Viewer", false)

        toolWindow.contentManager.addContent(content)

        val connection = project.messageBus.connect(toolWindow.disposable)
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    event.newFile
                        ?.takeIf { it.extension?.lowercase() in listOf("gltf", "glb") }
                        ?.also { loadModel(browser, it) }
                }
            }
        )
    }

    private fun loadModel(browser: JBCefBrowser, file: VirtualFile) {
        val path = file.path
        val url = "${getBaseUrl()}/files$path"
        browser.cefBrowser.executeJavaScript("loadGltf('$url')", file.url, 0)
    }
}
