package com.livo.works.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.livo.works.Room.data.CreateRoomRequestDto
import com.livo.works.Upload.ui.MediaStripRenderer
import com.livo.works.ViewModel.MediaUploadViewModel
import com.livo.works.ViewModel.RoomViewModel
import com.livo.works.databinding.ActivityManagerCreateRoomBinding
import com.livo.works.util.ImageCompressor
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerCreateRoom : AppCompatActivity() {

    private lateinit var binding: ActivityManagerCreateRoomBinding
    private val viewModel: RoomViewModel by viewModels()
    private val uploadViewModel: MediaUploadViewModel by viewModels()

    private var hotelId: Long = -1L
    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()
    private var isSubmitting = false
    private val photoStripRenderer by lazy { MediaStripRenderer(binding.layoutPhotoContainer) }

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ImageCompressor.MAX_IMAGES)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Compression, presign and upload all start here.
            uploadViewModel.addImages(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerCreateRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        hotelId = intent.getLongExtra("HOTEL_ID", -1L)
        if (hotelId == -1L) {
            Toast.makeText(this, "Invalid Hotel ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
        observeData()
        observeUploads()
        updateSubmitAvailability()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAddPhotos.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun renderPhotoStrip() {
        photoStripRenderer.render(
            uploads = uploadViewModel.items.value,
            onRemoveUpload = { uploadViewModel.remove(it.clientId) },
            onRetryUpload = { uploadViewModel.retry(it.clientId) }
        )
    }

    private fun validateAndSubmit() {
        val typeStr = binding.etRoomType.text.toString().trim()
        val priceStr = binding.etBasePrice.text.toString().trim()
        val countStr = binding.etTotalCount.text.toString().trim()
        val capacityStr = binding.etCapacity.text.toString().trim()
        val amenitiesStr = binding.etAmenities.text.toString().trim()

        if (typeStr.isEmpty() || priceStr.isEmpty() || countStr.isEmpty() || capacityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val basePrice = priceStr.toDoubleOrNull()
        val totalCount = countStr.toIntOrNull()
        val capacity = capacityStr.toIntOrNull()

        if (basePrice == null || totalCount == null || capacity == null) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show()
            return
        }

        val uploadStatus = uploadViewModel.status.value
        if (uploadStatus.total == 0) {
            Toast.makeText(this, "Please select at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        if (uploadStatus.isUploading) {
            Toast.makeText(this, "Please wait for the photos to finish uploading", Toast.LENGTH_SHORT).show()
            return
        }

        if (uploadStatus.hasFailed) {
            Toast.makeText(this, "Some photos failed to upload. Retry or remove them.", Toast.LENGTH_SHORT).show()
            return
        }

        // Only successfully uploaded images are ever submitted.
        val mediaTempPaths = uploadViewModel.uploadedTempPaths()
        if (mediaTempPaths.isEmpty()) {
            Toast.makeText(this, "Please select at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        val amenitiesList = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val request = CreateRoomRequestDto(
            type = typeStr,
            basePrice = basePrice,
            photos = mediaTempPaths,
            amenities = amenitiesList,
            totalCount = totalCount,
            capacity = capacity
        )

        isSubmitting = true
        viewModel.createRoom(hotelId, request)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.actionState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading(true)
                    is UiState.Success -> {
                        showLoading(false)
                        Toast.makeText(this@ManagerCreateRoom, "Room Created Successfully!", Toast.LENGTH_LONG).show()
                        viewModel.resetActionState()
                        finish()
                    }
                    is UiState.Error -> {
                        isSubmitting = false
                        showLoading(false)
                        Toast.makeText(this@ManagerCreateRoom, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeUploads() {
        lifecycleScope.launch {
            uploadViewModel.items.collect { renderPhotoStrip() }
        }

        // Collected separately rather than read inside the items collector:
        // status is its own derived flow, so reading status.value from an items
        // emission can see a stale value and leave submit disabled for good.
        lifecycleScope.launch {
            uploadViewModel.status.collect { updateSubmitAvailability() }
        }

        lifecycleScope.launch {
            uploadViewModel.messages.collect { message ->
                Toast.makeText(this@ManagerCreateRoom, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Submission is only possible once every picked photo finished uploading. */
    private fun updateSubmitAvailability() {
        binding.btnSubmit.isEnabled = !isSubmitting && uploadViewModel.status.value.isReadyToSubmit
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAddPhotos.isEnabled = !isLoading
        isSubmitting = isLoading
        updateSubmitAvailability()

        if (isLoading) startDotAnimation() else stopDotAnimation()
    }

    private fun startDotAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dotAnimators.clear()

        dots.forEachIndexed { index, dot ->
            val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f, 1f)
            val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f, 1f)
            val alpha = android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f, 1f)

            val animator = android.animation.ObjectAnimator.ofPropertyValuesHolder(dot, scaleX, scaleY, alpha).apply {
                duration = 800
                repeatCount = android.animation.ObjectAnimator.INFINITE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                startDelay = (index * 150).toLong()
            }
            animator.start()
            dotAnimators.add(animator)
        }
    }

    private fun stopDotAnimation() {
        dotAnimators.forEach { it.cancel() }
        dotAnimators.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDotAnimation()
    }
}