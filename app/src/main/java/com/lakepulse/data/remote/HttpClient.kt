package com.lakepulse.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HttpClient {
    fun get(url: String): String {
        return String(getBytes(url, accept = "application/json,text/plain,*/*"), Charsets.UTF_8)
    }

    fun getBytes(
        url: String,
        accept: String = "*/*",
        connectTimeoutMs: Int = 20_000,
        readTimeoutMs: Int = 45_000,
    ): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("User-Agent", "LakePulse/1.0 (Android)")
            setRequestProperty("Accept", accept)
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            val body = stream?.readBytes() ?: ByteArray(0)
            if (code !in 200..299) {
                throw IOException("HTTP $code for $url")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }
}
