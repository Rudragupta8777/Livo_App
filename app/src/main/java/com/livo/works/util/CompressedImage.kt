package com.livo.works.util

import java.io.File

data class CompressedImage(
    val file: File,
    val sizeBytes: Long,
    val contentType: String
)
