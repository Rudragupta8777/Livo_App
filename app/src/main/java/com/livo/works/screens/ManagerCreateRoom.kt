package com.livo.works.screens

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.livo.works.R
import com.livo.works.Room.data.CreateRoomRequestDto
import com.livo.works.ViewModel.RoomViewModel
import com.livo.works.databinding.ActivityManagerCreateRoomBinding
import com.livo.works.util.CloudinaryHelper
import com.livo.works.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerCreateRoom : AppCompatActivity() {

    private lateinit var binding: ActivityManagerCreateRoomBinding
    private val viewModel: RoomViewModel by viewModels()

    private var hotelId: Long = -1L
    private val selectedImageUris = mutableListOf<Uri>()
    private var dotAnimators = mutableListOf<android.animation.ObjectAnimator>()

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updatePhotoUI()
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

    private fun updatePhotoUI() {
        val childCount = binding.layoutPhotoContainer.childCount
        if (childCount > 1) {
            binding.layoutPhotoContainer.removeViews(1, childCount - 1)
        }

        val dpToPx = resources.displayMetrics.density

        selectedImageUris.toList().forEach { uri ->
            val frameLayout = android.widget.FrameLayout(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.photo_size),
                    resources.getDimensionPixelSize(R.dimen.photo_size)
                ).apply { marginEnd = 24 }
            }

            val imageCard = MaterialCardView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                radius = 40f
                strokeWidth = 0
            }

            val imageView = ImageView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(this).load(uri).into(imageView)
            imageCard.addView(imageView)
            frameLayout.addView(imageCard)

            // Delete Icon Overlay
            val deleteButtonCard = MaterialCardView(this).apply {
                val cardSize = (24 * dpToPx).toInt()
                layoutParams = android.widget.FrameLayout.LayoutParams(cardSize, cardSize).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    topMargin = (6 * dpToPx).toInt()
                    marginEnd = (6 * dpToPx).toInt()
                }
                radius = (12 * dpToPx)
                setCardBackgroundColor(android.graphics.Color.parseColor("#80000000"))
                strokeWidth = 0
                cardElevation = 0f

                val deleteIcon = ImageView(context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setPadding((6 * dpToPx).toInt(), (6 * dpToPx).toInt(), (6 * dpToPx).toInt(), (6 * dpToPx).toInt())
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                }
                addView(deleteIcon)

                setOnClickListener {
                    selectedImageUris.remove(uri)
                    updatePhotoUI()
                }
            }

            frameLayout.addView(deleteButtonCard)
            binding.layoutPhotoContainer.addView(frameLayout)
        }
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

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            showLoading(true)
            val uploadedUrls = mutableListOf<String>()

            for (uri in selectedImageUris) {
                val url = CloudinaryHelper.uploadImage(this@ManagerCreateRoom, uri)
                if (url != null) {
                    uploadedUrls.add(url)
                } else {
                    Toast.makeText(this@ManagerCreateRoom, "Failed to upload an image", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }
            }

            val amenitiesList = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val request = CreateRoomRequestDto(
                type = typeStr,
                basePrice = basePrice,
                photos = uploadedUrls,
                amenities = amenitiesList,
                totalCount = totalCount,
                capacity = capacity
            )

            viewModel.createRoom(hotelId, request)
        }
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
                        showLoading(false)
                        Toast.makeText(this@ManagerCreateRoom, state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnAddPhotos.isEnabled = !isLoading

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