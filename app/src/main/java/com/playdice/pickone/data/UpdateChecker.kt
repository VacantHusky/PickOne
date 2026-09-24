package com.playdice.pickone.data

import com.playdice.pickone.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val name: String,
    val description: String,
    val version: String,
    val date: String,
    val fileUrl: String,
)

object UpdateVersion {
    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    fun compare(left: String, right: String): Int {
        val leftParts = parts(left)
        val rightParts = parts(right)
        val count = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until count) {
            val difference = (leftParts.getOrElse(index) { 0 } - rightParts.getOrElse(index) { 0 })
            if (difference != 0) return difference.coerceIn(-1, 1)
        }
        return 0
    }

    private fun parts(version: String): List<Int> = version
        .trim()
        .removePrefix("v")
        .split('.', '-', '+')
        .map { it.toIntOrNull() ?: 0 }
}

class UpdateChecker {
    suspend fun check(): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
                useCaches = false
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val payload = JSONObject(json)
                val version = payload.optString("version").trim()
                val file = payload.optString("file").trim()
                if (version.isBlank() || file.isBlank() || !file.startsWith("https://")) return@runCatching null
                if (!UpdateVersion.isNewer(version, BuildConfig.VERSION_NAME)) return@runCatching null
                AppUpdate(
                    name = payload.optString("name", "PickOne"),
                    description = payload.optString("describe"),
                    version = version,
                    date = payload.optString("date"),
                    fileUrl = file,
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    companion object {
        const val UPDATE_URL = "https://app.wanghu.host/PickOne/last.json"
    }
}
