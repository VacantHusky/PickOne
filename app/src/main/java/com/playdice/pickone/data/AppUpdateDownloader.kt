package com.playdice.pickone.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class AppUpdateDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun download(update: AppUpdate, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val target = File(directory, "PickOne-${update.version}.apk")
        val connection = (URL(update.fileUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            useCaches = false
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Download failed: HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.buffered().use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var count: Int
                    while (input.read(buffer).also { count = it } >= 0) {
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            check(target.length() > 0) { "Downloaded APK is empty" }
            onProgress(100)
            target
        } catch (error: Throwable) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }
}
