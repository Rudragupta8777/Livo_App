package com.livo.works.Upload.data

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File

/**
 * Streams a file to the network and reports 0..100 progress as bytes are written.
 * OkHttp may call [writeTo] again on an internal retry, so progress can restart
 * from 0 for the same call — the UI simply follows it.
 */
class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val onProgress: (Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = file.length()
        var uploaded = 0L
        var lastReported = -1

        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break

                sink.write(buffer, 0, read)
                uploaded += read

                val percent = if (total > 0) ((uploaded * 100) / total).toInt() else 0
                if (percent != lastReported) {
                    lastReported = percent
                    onProgress(percent.coerceIn(0, 100))
                }
            }
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
    }
}
