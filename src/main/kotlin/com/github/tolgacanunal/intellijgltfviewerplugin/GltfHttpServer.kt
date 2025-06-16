package com.github.tolgacanunal.intellijgltfviewerplugin

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress

object GltfHttpServer {
    private val server: HttpServer by lazy { createServer() }

    val port
        get() = server.address.port

    private fun createServer(): HttpServer = HttpServer.create(InetSocketAddress(0), 0).apply {
        createContext("/viewer", ViewerHandler())
        createContext("/files", FilesHandler())
    }

    fun start() {
        server.start()
    }

    private class ViewerHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val resourcePath = exchange.requestURI.path.removePrefix("/")
            val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            if (stream != null) {
                val contentType = resourcePath.substringAfterLast('.', "").toContentType()
                exchange.sendResponse(stream.readBytes(), contentType)
            } else {
                exchange.sendEmpty(404)
            }
        }
    }

    private class FilesHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val filePath = exchange.requestURI.path.removePrefix("/files")
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                val contentType = file.extension.toContentType()
                FileInputStream(file).use { stream ->
                    exchange.sendResponse(stream.readBytes(), contentType, file.length())
                }
            } else {
                exchange.sendEmpty(404)
            }
        }
    }

    private fun HttpExchange.sendResponse(data: ByteArray, contentType: String, length: Long = 0L) {
        responseHeaders.apply {
            set("Content-Type", contentType)
            if ("/files" in requestURI.path) {
                set("Access-Control-Allow-Origin", "*")
            }
        }
        sendResponseHeaders(200, if (length > 0) length else data.size.toLong())
        responseBody.use { it.write(data) }
    }

    private fun HttpExchange.sendEmpty(code: Int) {
        sendResponseHeaders(code, -1)
        close()
    }

    private fun String.toContentType(): String = when (lowercase()) {
        "html" -> "text/html"
        "js" -> "application/javascript"
        "css" -> "text/css"
        "gltf" -> "model/gltf+json"
        "glb" -> "model/gltf-binary"
        else -> "application/octet-stream"
    }
}
