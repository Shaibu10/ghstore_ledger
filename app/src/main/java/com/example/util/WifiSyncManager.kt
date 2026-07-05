package com.example.util

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.Executors

object WifiSyncManager {

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var isRunning = false

    /**
     * Obtains the local Wi-Fi IP address formatted as standard IPv4.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) {
                            return sAddr
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Offline/No Connection"
    }

    /**
     * Starts the local master database sharing server context using standard ServerSocket.
     */
    fun startHostServer(
        port: Int,
        onGetSyncData: () -> String,
        onIncomingAction: (JSONObject) -> Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                // Synchronously close any existing host server socket to release resource
                stopHostServer()
                
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(java.net.InetSocketAddress(port))
                
                serverSocket = server
                isRunning = true
                onSuccess()

                while (isRunning) {
                    val clientSocket: Socket = try {
                        server.accept()
                    } catch (e: Exception) {
                        break
                    }
                    executor.execute {
                        handleClientConnection(clientSocket, onGetSyncData, onIncomingAction)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Could not start server. Port may be occupied.")
            }
        }
    }

    private fun handleClientConnection(
        socket: Socket,
        onGetSyncData: () -> String,
        onIncomingAction: (JSONObject) -> Boolean
    ) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            val os: OutputStream = socket.getOutputStream()

            // Read request header line
            val firstLine = reader.readLine() ?: return
            val parts = firstLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            // Read intermediate headers to parse content length
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                val lowerLine = line!!.lowercase()
                if (lowerLine.startsWith("content-length:")) {
                    contentLength = lowerLine.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            if (method == "GET" && path.startsWith("/api/sync")) {
                val payload = onGetSyncData()
                val bytes = payload.toByteArray(StandardCharsets.UTF_8)
                
                val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                os.write(header.toByteArray(StandardCharsets.UTF_8))
                os.write(bytes)
                os.flush()
            } else if (method == "POST" && path.startsWith("/api/action")) {
                // Read request body chars based on Content-Length header
                val bodyChars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                val bodyText = String(bodyChars)
                
                val rootJson = JSONObject(bodyText)
                val wasSuccessful = onIncomingAction(rootJson)

                val responseJson = JSONObject()
                responseJson.put("success", wasSuccessful)
                responseJson.put("message", if (wasSuccessful) "Action synchronized and committed on Host" else "Action processing failed")

                val bytes = responseJson.toString().toByteArray(StandardCharsets.UTF_8)
                val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                os.write(header.toByteArray(StandardCharsets.UTF_8))
                os.write(bytes)
                os.flush()
            } else {
                val header = "HTTP/1.1 405 Method Not Allowed\r\nConnection: close\r\n\r\n"
                os.write(header.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
            try { socket.close() } catch (ex: Exception) {}
        }
    }

    /**
     * Gracefully stops the active server socket listener.
     */
    fun stopHostServer() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes GET sync request from Client to Host.
     */
    fun fetchSyncDataFromHost(
        ip: String,
        port: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val urlObj = URL("http://$ip:$port/api/sync")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 8000

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))
                    val text = reader.readText()
                    reader.close()
                    onSuccess(text)
                } else {
                    onError("Server returned status code: $code")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Could not connect to Host. Check Wi-Fi and Host IP.")
            }
        }
    }

    /**
     * Executes POST action request from Client to Host.
     */
    fun postActionToHost(
        ip: String,
        port: Int,
        payload: JSONObject,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val urlObj = URL("http://$ip:$port/api/action")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

                val os: OutputStream = conn.outputStream
                os.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
                os.close()

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))
                    val text = reader.readText()
                    reader.close()

                    val responseJson = JSONObject(text)
                    if (responseJson.optBoolean("success", false)) {
                        onSuccess()
                    } else {
                        onError(responseJson.optString("message", "Host rejected action"))
                    }
                } else {
                    onError("Server returned status code: $code")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Network error. Fails to commit transaction to Host.")
            }
        }
    }
}
