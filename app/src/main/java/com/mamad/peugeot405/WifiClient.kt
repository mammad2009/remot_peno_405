package com.mamad.peugeot405

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * ارتباط با ماکت از طریق وای‌فای: ماکت یک شبکه‌ی وای‌فای (SSID پیش‌فرض Peugeot405) می‌سازد،
 * گوشی به آن وصل می‌شود و این کلاس یک سوکت TCP به 192.168.4.1:8080 باز می‌کند.
 * اگر در کد ماکت WIFI_PORT را عوض کردی، PORT را اینجا هم عوض کن.
 */
class WifiClient(context: Context) {
    companion object {
        const val NAME = "Peugeot405"
        const val HOST = "192.168.4.1"
        const val PORT = 8080
    }

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var socket: Socket? = null

    // شبکه‌ی وای‌فایی که گوشی الان بهش وصله (حتی اگر اینترنت نداشته باشد)
    @Suppress("DEPRECATION")
    private fun wifiNetwork(): Network? =
        cm.allNetworks.firstOrNull { n ->
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

    val hasWifi: Boolean get() = wifiNetwork() != null

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        runCatching {
            val net = wifiNetwork() ?: throw IllegalStateException("NO_WIFI")
            val s = net.socketFactory.createSocket()      // سوکت حتما از مسیر وای‌فای (نه اینترنت موبایل)
            try {
                s.connect(InetSocketAddress(HOST, PORT), 4000)
            } catch (e: Throwable) {
                runCatching { s.close() }
                throw e
            }
            s.tcpNoDelay = true
            s.keepAlive = true
            s.soTimeout = 10000                            // اگر ۱۰ ثانیه هیچ‌چیز نیامد (حتی PONG) اتصال مرده حساب می‌شود
            socket = s
        }
    }

    suspend fun send(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = socket!!.getOutputStream()
            out.write(text.toByteArray())
            out.flush()
        }
    }

    // خطوطی که ماکت می‌فرستد (مثلا "BAT:87" یا "I1:..."). وقتی اتصال بسته یا قطع شود جریان تمام می‌شود
    fun lines(): Flow<String> = flow {
        val input = socket?.getInputStream()
        if (input != null) {
            val sb = StringBuilder()
            val buf = ByteArray(128)
            while (true) {
                val n = try { input.read(buf) } catch (e: Exception) { -1 }
                if (n < 0) break
                for (i in 0 until n) {
                    val ch = buf[i].toInt().toChar()
                    if (ch == '\n') {
                        emit(sb.toString().trim())
                        sb.setLength(0)
                    } else if (ch != '\r' && sb.length < 96) {
                        sb.append(ch)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun disconnect() {
        runCatching { socket?.close() }
        socket = null
    }
}
