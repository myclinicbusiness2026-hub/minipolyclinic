package com.example.sync

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class LocalSyncHttpServer(
  private val port: Int = 8888,
  private val getDeviceStatusJson: () -> String,
  private val getDataPayloadJson: () -> String,
  private val onDataReceived: (String) -> Pair<Boolean, String>
) {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var serverSocket: ServerSocket? = null
  private var isRunning = false

  companion object {
    private const val TAG = "LocalSyncHttpServer"
  }

  fun start() {
    if (isRunning) return
    isRunning = true

    scope.launch {
      try {
        val server = ServerSocket(port)
        serverSocket = server
        Log.i(TAG, "Local Sync Server listening on port $port")

        while (isActive && isRunning) {
          try {
            val clientSocket = server.accept()
            launch {
              handleClient(clientSocket)
            }
          } catch (e: Exception) {
            if (!isRunning) break
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Server socket error on port $port: ${e.message}")
      }
    }
  }

  fun stop() {
    isRunning = false
    try {
      serverSocket?.close()
    } catch (_: Exception) {}
    scope.cancel()
  }

  private fun handleClient(socket: Socket) {
    try {
      socket.soTimeout = 10000
      val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
      val outputStream = socket.getOutputStream()

      val requestLine = reader.readLine() ?: return
      val tokens = requestLine.split(" ")
      if (tokens.size < 2) return

      val method = tokens[0]
      val path = tokens[1]

      var contentLength = 0
      var line = reader.readLine()
      while (!line.isNullOrEmpty()) {
        if (line.lowercase().startsWith("content-length:")) {
          contentLength = line.substring(15).trim().toIntOrNull() ?: 0
        }
        line = reader.readLine()
      }

      when {
        method == "GET" && (path == "/ping" || path == "/status") -> {
          val responseJson = getDeviceStatusJson()
          sendJsonResponse(outputStream, 200, responseJson)
        }

        method == "GET" && path == "/sync/pull" -> {
          val responseJson = getDataPayloadJson()
          sendJsonResponse(outputStream, 200, responseJson)
        }

        method == "POST" && path == "/sync/push" -> {
          val bodyChars = CharArray(contentLength)
          var totalRead = 0
          while (totalRead < contentLength) {
            val count = reader.read(bodyChars, totalRead, contentLength - totalRead)
            if (count == -1) break
            totalRead += count
          }
          val bodyStr = String(bodyChars, 0, totalRead)

          val (success, message) = onDataReceived(bodyStr)
          val respObj = JSONObject().apply {
            put("success", success)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
          }
          val statusCode = if (success) 200 else 400
          sendJsonResponse(outputStream, statusCode, respObj.toString())
        }

        method == "OPTIONS" -> {
          sendCorsResponse(outputStream)
        }

        else -> {
          sendJsonResponse(outputStream, 404, "{\"error\":\"Not Found\"}")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Client handling exception: ${e.message}")
    } finally {
      try {
        socket.close()
      } catch (_: Exception) {}
    }
  }

  private fun sendJsonResponse(out: OutputStream, code: Int, json: String) {
    val bytes = json.toByteArray(Charsets.UTF_8)
    val statusText = when (code) {
      200 -> "OK"
      400 -> "Bad Request"
      404 -> "Not Found"
      else -> "Error"
    }
    val headers = "HTTP/1.1 $code $statusText\r\n" +
      "Content-Type: application/json; charset=utf-8\r\n" +
      "Content-Length: ${bytes.size}\r\n" +
      "Connection: close\r\n" +
      "Access-Control-Allow-Origin: *\r\n\r\n"

    out.write(headers.toByteArray(Charsets.UTF_8))
    out.write(bytes)
    out.flush()
  }

  private fun sendCorsResponse(out: OutputStream) {
    val headers = "HTTP/1.1 204 No Content\r\n" +
      "Access-Control-Allow-Origin: *\r\n" +
      "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
      "Access-Control-Allow-Headers: Content-Type\r\n" +
      "Connection: close\r\n\r\n"
    out.write(headers.toByteArray(Charsets.UTF_8))
    out.flush()
  }
}
