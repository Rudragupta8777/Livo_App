package com.livo.works.ViewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livo.works.Upload.data.MediaUploadItem
import com.livo.works.Upload.data.MediaUploadState
import com.livo.works.Upload.data.MediaUploadStatus
import com.livo.works.Upload.data.PresignFileRequest
import com.livo.works.Upload.data.PresignedFileDto
import com.livo.works.Upload.repository.MediaUploadRepository
import com.livo.works.Upload.repository.PresignOutcome
import com.livo.works.Upload.repository.StorageUploadResult
import com.livo.works.util.CompressedImage
import com.livo.works.util.ImageCompressor
import com.livo.works.util.MediaException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Owns the full client-side upload experience for one screen:
 * select -> compress -> presign -> direct upload -> track state.
 *
 * Screens read [items] to draw the strip and [status] to gate their submit
 * button, then call [uploadedTempPaths] when submitting the entity.
 */
@HiltViewModel
class MediaUploadViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: MediaUploadRepository
) : ViewModel() {

    private companion object {
        const val TAG = "MediaUploadViewModel"
        const val MAX_PARALLEL_UPLOADS = 3
        const val MAX_PARALLEL_COMPRESSIONS = 2
        const val MAX_UPLOAD_ATTEMPTS = 3
        const val BACKOFF_BASE_MS = 1000L
    }

    /** Everything ever picked, including REMOVED tombstones. */
    private val allItems = MutableStateFlow<List<MediaUploadItem>>(emptyList())

    val items = allItems
        .map { list -> list.filter { it.state != MediaUploadState.REMOVED } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val status = items
        .map { list ->
            MediaUploadStatus(
                total = list.size,
                uploaded = list.count { it.state == MediaUploadState.UPLOADED },
                isUploading = list.any {
                    it.state == MediaUploadState.PENDING || it.state == MediaUploadState.UPLOADING
                },
                hasFailed = list.any { it.state == MediaUploadState.FAILED }
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MediaUploadStatus())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private val compressedFiles = ConcurrentHashMap<String, CompressedImage>()
    private val uploadJobs = ConcurrentHashMap<String, Job>()

    private val uploadGate = Semaphore(MAX_PARALLEL_UPLOADS)
    private val compressionGate = Semaphore(MAX_PARALLEL_COMPRESSIONS)

    init {
        // Clean up compressed copies orphaned by an earlier process death.
        viewModelScope.launch(Dispatchers.IO) {
            ImageCompressor.pruneStaleFiles(appContext)
        }
    }

    // ---------------------------------------------------------------- public API

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val remainingSlots = ImageCompressor.MAX_IMAGES - items.value.size
        if (remainingSlots <= 0) {
            notify("You can upload a maximum of ${ImageCompressor.MAX_IMAGES} images")
            return
        }

        val accepted = uris.take(remainingSlots)
        if (uris.size > remainingSlots) {
            notify("Only ${ImageCompressor.MAX_IMAGES} images allowed, extra photos were skipped")
        }

        // Reject unsupported / oversized files up front, before any decoding.
        val valid = mutableListOf<Uri>()
        accepted.forEach { uri ->
            try {
                ImageCompressor.validate(appContext, uri)
                valid.add(uri)
            } catch (e: MediaException) {
                notify(e.message ?: "This image could not be used")
            }
        }
        if (valid.isEmpty()) return

        val newItems = valid.map {
            MediaUploadItem(clientId = UUID.randomUUID().toString(), uri = it)
        }
        allItems.update { current -> current + newItems }

        startPipeline(newItems.map { it.clientId })
    }

    /**
     * Local-only removal. The backend cleanup job disposes of the abandoned
     * temp object, so no delete API is called here.
     */
    fun remove(clientId: String) {
        uploadJobs.remove(clientId)?.cancel()
        compressedFiles.remove(clientId)?.file?.delete()
        updateItem(clientId) {
            it.copy(state = MediaUploadState.REMOVED, progress = 0, tempPath = null)
        }
    }

    fun retry(clientId: String) {
        val item = allItems.value.firstOrNull { it.clientId == clientId } ?: return
        if (item.state != MediaUploadState.FAILED) return

        updateItem(clientId) {
            it.copy(state = MediaUploadState.PENDING, progress = 0, errorMessage = null)
        }
        startPipeline(listOf(clientId))
    }

    fun retryAllFailed() {
        items.value
            .filter { it.state == MediaUploadState.FAILED }
            .forEach { retry(it.clientId) }
    }

    /** Only successfully uploaded paths — this is what the entity request carries. */
    fun uploadedTempPaths(): List<String> =
        items.value
            .filter { it.state == MediaUploadState.UPLOADED }
            .mapNotNull { it.tempPath }

    fun clear() {
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
        compressedFiles.values.forEach { it.file.delete() }
        compressedFiles.clear()
        allItems.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        compressedFiles.values.forEach { it.file.delete() }
        compressedFiles.clear()
    }

    // ------------------------------------------------------------------ pipeline

    private fun startPipeline(clientIds: List<String>) {
        viewModelScope.launch {
            val compressed = clientIds.map { clientId ->
                async { compressOne(clientId) }
            }.awaitAll().filterNotNull()

            val stillWanted = compressed.filter { !isRemoved(it) }
            if (stillWanted.isEmpty()) return@launch

            presignAndUpload(stillWanted)
        }
    }

    /** Returns the clientId when compression succeeded, null when it failed. */
    private suspend fun compressOne(clientId: String): String? {
        compressedFiles[clientId]?.let { return clientId }

        val item = allItems.value.firstOrNull { it.clientId == clientId } ?: return null
        if (item.state == MediaUploadState.REMOVED) return null

        return try {
            compressionGate.withPermit {
                if (isRemoved(clientId)) return null
                compressedFiles[clientId] = ImageCompressor.compress(appContext, item.uri, clientId)
            }
            clientId
        } catch (e: MediaException) {
            Log.w(TAG, "compressOne: rejected $clientId - ${e.message}")
            markFailed(clientId, e.message ?: "This image could not be used")
            null
        } catch (e: Exception) {
            Log.e(TAG, "compressOne: failed to compress $clientId", e)
            markFailed(clientId, "Could not compress this image")
            null
        }
    }

    private suspend fun presignAndUpload(clientIds: List<String>) {
        // The backend has no per-file id - it returns results positionally, in
        // the same order and count as the request. Build (clientId, request)
        // pairs together so a missing compressed file can never desync the
        // two lists relative to each other.
        val entries = clientIds.mapNotNull { clientId ->
            compressedFiles[clientId]?.let { compressed ->
                clientId to PresignFileRequest(
                    fileName = "$clientId.jpg",
                    contentType = compressed.contentType,
                    contentLength = compressed.sizeBytes
                )
            }
        }
        if (entries.isEmpty()) return

        val orderedClientIds = entries.map { it.first }
        val requests = entries.map { it.second }

        when (val outcome = repository.requestPresignedUrls(requests)) {
            is PresignOutcome.SessionExpired -> {
                orderedClientIds.forEach { markFailed(it, "Session expired") }
                notify("Session expired. Please log in again.")
            }

            is PresignOutcome.Failure -> {
                Log.e(TAG, "presignAndUpload: presign request failed - ${outcome.message}")
                orderedClientIds.forEach { markFailed(it, outcome.message) }
            }

            is PresignOutcome.Success -> {
                // The batch is all-or-nothing server-side, but guard against a
                // malformed/mismatched response defensively instead of crashing.
                if (outcome.files.size != orderedClientIds.size) {
                    orderedClientIds.forEach { markFailed(it, "Could not prepare the upload") }
                    return
                }
                orderedClientIds.zip(outcome.files).forEach { (clientId, presigned) ->
                    if (presigned.presignedUrl.isBlank() || presigned.tempPath.isBlank()) {
                        markFailed(clientId, "Could not prepare the upload")
                    } else {
                        launchUpload(clientId, presigned)
                    }
                }
            }
        }
    }

    private fun launchUpload(clientId: String, presigned: PresignedFileDto) {
        uploadJobs.remove(clientId)?.cancel()
        val job = viewModelScope.launch {
            uploadGate.withPermit { runUpload(clientId, presigned) }
        }
        uploadJobs[clientId] = job
        job.invokeOnCompletion { uploadJobs.remove(clientId, job) }
    }

    private suspend fun runUpload(clientId: String, presigned: PresignedFileDto) {
        val compressed = compressedFiles[clientId]
            ?: return markFailed(clientId, "This image is no longer available")

        var target = presigned
        var attempt = 0
        var lastError = "Upload failed"

        while (attempt < MAX_UPLOAD_ATTEMPTS) {
            if (isRemoved(clientId)) return

            updateItem(clientId) {
                it.copy(
                    state = MediaUploadState.UPLOADING,
                    progress = 0,
                    errorMessage = null,
                    tempPath = target.tempPath
                )
            }

            val result = repository.uploadToStorage(
                uploadUrl = target.presignedUrl,
                file = compressed.file,
                contentType = compressed.contentType
            ) { percent ->
                updateItem(clientId) {
                    if (it.state == MediaUploadState.UPLOADING) it.copy(progress = percent) else it
                }
            }

            if (isRemoved(clientId)) return

            when (result) {
                is StorageUploadResult.Success -> {
                    updateItem(clientId) {
                        it.copy(
                            state = MediaUploadState.UPLOADED,
                            progress = 100,
                            tempPath = target.tempPath,
                            errorMessage = null
                        )
                    }
                    return
                }

                is StorageUploadResult.UrlExpired -> {
                    // Never retry an expired URL — ask for a fresh one first.
                    attempt++
                    lastError = "Upload link expired"
                    val refreshed = refreshPresignedUrl(clientId, compressed)
                        ?: return markFailed(clientId, lastError)
                    target = refreshed
                }

                is StorageUploadResult.Failure -> {
                    attempt++
                    lastError = result.message
                    if (attempt >= MAX_UPLOAD_ATTEMPTS) break
                    delay(BACKOFF_BASE_MS * (1L shl (attempt - 1)))
                }
            }
        }

        markFailed(clientId, lastError)
    }

    private suspend fun refreshPresignedUrl(
        clientId: String,
        compressed: CompressedImage
    ): PresignedFileDto? {
        val request = PresignFileRequest(
            fileName = "$clientId.jpg",
            contentType = compressed.contentType,
            contentLength = compressed.sizeBytes
        )

        // Single-file request - the one result in the response is ours.
        val outcome = repository.requestPresignedUrls(listOf(request))
        val refreshed = (outcome as? PresignOutcome.Success)?.files?.firstOrNull()

        return refreshed?.takeIf {
            it.presignedUrl.isNotBlank() && it.tempPath.isNotBlank()
        }
    }

    // --------------------------------------------------------------------- state

    private fun isRemoved(clientId: String): Boolean =
        allItems.value.firstOrNull { it.clientId == clientId }?.state == MediaUploadState.REMOVED

    private fun markFailed(clientId: String, message: String) {
        updateItem(clientId) {
            if (it.state == MediaUploadState.REMOVED) it
            else it.copy(state = MediaUploadState.FAILED, progress = 0, errorMessage = message)
        }
    }

    private fun updateItem(clientId: String, transform: (MediaUploadItem) -> MediaUploadItem) {
        allItems.update { current ->
            current.map { if (it.clientId == clientId) transform(it) else it }
        }
    }

    private fun notify(message: String) {
        _messages.tryEmit(message)
    }
}
