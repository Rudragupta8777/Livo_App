package com.livo.works.Upload.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.livo.works.R
import com.livo.works.Upload.data.MediaUploadItem
import com.livo.works.Upload.data.MediaUploadState

/**
 * Draws the horizontal photo strip used by CreateHotel / UpdateHotel /
 * ManagerCreateRoom.
 *
 * Each in-progress upload shows a round progress ring over a desaturated
 * (black & white) thumbnail; the moment it finishes, the ring fades out and
 * the photo animates from grayscale to full color.
 *
 * Views are kept alive and updated in place across progress ticks instead of
 * being rebuilt on every emission — a structural rebuild only happens when
 * the set of photos actually changes (add/remove), so the color-transition
 * animation isn't restarted or interrupted mid-flight by a redraw.
 *
 * One instance should be created once per screen and reused for its lifetime
 * (don't recreate it on every render call).
 */
class MediaStripRenderer(private val container: LinearLayout) {

    private class Entry(
        val frame: FrameLayout,
        val imageView: ImageView,
        val overlay: TextView,
        val progressIndicator: CircularProgressIndicator,
        var lastState: MediaUploadState?,
        var colorAnimator: ValueAnimator? = null
    )

    private val entries = LinkedHashMap<String, Entry>()
    private var currentKeys: List<String> = emptyList()

    fun render(
        uploads: List<MediaUploadItem>,
        existingUrls: List<String> = emptyList(),
        onRemoveExisting: (String) -> Unit = {},
        onRemoveUpload: (MediaUploadItem) -> Unit,
        onRetryUpload: (MediaUploadItem) -> Unit
    ) {
        val targetKeys = existingUrls.map { existingKey(it) } + uploads.map { uploadKey(it.clientId) }

        if (targetKeys != currentKeys) {
            rebuildStructure(uploads, existingUrls, onRemoveExisting, onRemoveUpload, onRetryUpload)
            currentKeys = targetKeys
        }

        // Same photo set as before: update progress/state in place so the
        // grayscale-to-color animation can run to completion undisturbed.
        uploads.forEach { item ->
            entries[uploadKey(item.clientId)]?.let { entry ->
                val stateChanged = entry.lastState != item.state
                applyState(entry, item, animateColor = stateChanged)
                entry.lastState = item.state
            }
        }
    }

    private fun existingKey(url: String) = "e:$url"
    private fun uploadKey(clientId: String) = "u:$clientId"

    private fun rebuildStructure(
        uploads: List<MediaUploadItem>,
        existingUrls: List<String>,
        onRemoveExisting: (String) -> Unit,
        onRemoveUpload: (MediaUploadItem) -> Unit,
        onRetryUpload: (MediaUploadItem) -> Unit
    ) {
        entries.values.forEach { it.colorAnimator?.cancel() }
        entries.clear()

        val childCount = container.childCount
        if (childCount > 1) {
            container.removeViews(1, childCount - 1)
        }

        val context = container.context
        val density = context.resources.displayMetrics.density

        existingUrls.forEach { url ->
            val entry = buildEntry(
                context = context,
                density = density,
                imageSource = url,
                startUploaded = true,
                onDelete = { onRemoveExisting(url) },
                onTap = null
            )
            entries[existingKey(url)] = entry
            container.addView(entry.frame)
        }

        uploads.forEach { item ->
            val entry = buildEntry(
                context = context,
                density = density,
                imageSource = item.uri,
                startUploaded = item.state == MediaUploadState.UPLOADED,
                onDelete = { onRemoveUpload(item) },
                onTap = { onRetryUpload(item) }
            )
            entry.lastState = item.state
            applyState(entry, item, animateColor = false)
            entries[uploadKey(item.clientId)] = entry
            container.addView(entry.frame)
        }
    }

    private fun applyState(entry: Entry, item: MediaUploadItem, animateColor: Boolean) {
        when (item.state) {
            MediaUploadState.PENDING -> {
                showOverlay(entry, text = null, background = SCRIM_DARK)
                entry.progressIndicator.visibility = View.VISIBLE
                entry.progressIndicator.alpha = 1f
                entry.progressIndicator.isIndeterminate = true
                setGrayscale(entry)
            }

            MediaUploadState.UPLOADING -> {
                showOverlay(entry, text = null, background = SCRIM_DARK)
                entry.progressIndicator.visibility = View.VISIBLE
                entry.progressIndicator.alpha = 1f
                if (entry.progressIndicator.isIndeterminate) {
                    entry.progressIndicator.isIndeterminate = false
                }
                entry.progressIndicator.setProgressCompat(item.progress, true)
                setGrayscale(entry)
            }

            MediaUploadState.FAILED -> {
                showOverlay(entry, text = "Retry", background = SCRIM_ERROR)
                entry.progressIndicator.visibility = View.GONE
                setGrayscale(entry)
            }

            MediaUploadState.UPLOADED -> {
                fadeOutOverlayAndProgress(entry)
                animateToColor(entry, animate = animateColor)
            }

            MediaUploadState.REMOVED -> Unit
        }
    }

    private fun showOverlay(entry: Entry, text: String?, background: Int) {
        entry.overlay.animate().cancel()
        entry.overlay.alpha = 1f
        entry.overlay.text = text.orEmpty()
        entry.overlay.setBackgroundColor(background)
        entry.overlay.visibility = View.VISIBLE
    }

    private fun fadeOutOverlayAndProgress(entry: Entry) {
        if (entry.overlay.visibility == View.VISIBLE) {
            entry.overlay.animate()
                .alpha(0f)
                .setDuration(FADE_DURATION_MS)
                .withEndAction { entry.overlay.visibility = View.GONE }
                .start()
        }
        if (entry.progressIndicator.visibility == View.VISIBLE) {
            entry.progressIndicator.animate()
                .alpha(0f)
                .setDuration(FADE_DURATION_MS)
                .withEndAction { entry.progressIndicator.visibility = View.GONE }
                .start()
        }
    }

    private fun setGrayscale(entry: Entry) {
        entry.colorAnimator?.cancel()
        entry.imageView.colorFilter = grayscaleFilter()
    }

    /** Animates the thumbnail from black & white to full color once, on completion. */
    private fun animateToColor(entry: Entry, animate: Boolean) {
        entry.colorAnimator?.cancel()

        if (!animate) {
            entry.imageView.clearColorFilter()
            return
        }

        val matrix = ColorMatrix()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COLOR_TRANSITION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                matrix.setSaturation(animation.animatedValue as Float)
                entry.imageView.colorFilter = ColorMatrixColorFilter(matrix)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    entry.imageView.clearColorFilter()
                }
            })
        }
        entry.colorAnimator = animator
        animator.start()
    }

    private fun grayscaleFilter(): ColorMatrixColorFilter =
        ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    private fun buildEntry(
        context: Context,
        density: Float,
        imageSource: Any,
        startUploaded: Boolean,
        onDelete: () -> Unit,
        onTap: (() -> Unit)?
    ): Entry {
        val frameLayout = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.photo_size),
                context.resources.getDimensionPixelSize(R.dimen.photo_size)
            ).apply { marginEnd = 24 }
        }

        val imageCard = MaterialCardView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            radius = 40f
            strokeWidth = 0
        }

        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (!startUploaded) colorFilter = grayscaleFilter()
        }
        Glide.with(context).load(imageSource).into(imageView)
        imageCard.addView(imageView)

        // Doubles as the dark upload scrim and the red failure scrim + label.
        val overlay = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            visibility = if (startUploaded) View.GONE else View.VISIBLE
            setBackgroundColor(SCRIM_DARK)
        }
        imageCard.addView(overlay)

        val progressSize = (36 * density).toInt()
        val progressIndicator = CircularProgressIndicator(context).apply {
            layoutParams = FrameLayout.LayoutParams(progressSize, progressSize, Gravity.CENTER)
            trackThickness = (3 * density).toInt()
            setIndicatorColor(Color.WHITE)
            trackColor = Color.parseColor("#66FFFFFF")
            isIndeterminate = true
            max = 100
            visibility = if (startUploaded) View.GONE else View.VISIBLE
        }
        imageCard.addView(progressIndicator)

        if (onTap != null) {
            imageCard.isClickable = true
            imageCard.isFocusable = true
            imageCard.setOnClickListener { onTap() }
        }

        frameLayout.addView(imageCard)
        frameLayout.addView(buildDeleteButton(context, density, onDelete))

        return Entry(
            frame = frameLayout,
            imageView = imageView,
            overlay = overlay,
            progressIndicator = progressIndicator,
            lastState = null
        )
    }

    private fun buildDeleteButton(
        context: Context,
        density: Float,
        onDelete: () -> Unit
    ): MaterialCardView = MaterialCardView(context).apply {
        val cardSize = (24 * density).toInt()
        layoutParams = FrameLayout.LayoutParams(cardSize, cardSize).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = (6 * density).toInt()
            marginEnd = (6 * density).toInt()
        }
        radius = 12 * density
        setCardBackgroundColor(Color.parseColor("#80000000"))
        strokeWidth = 0
        cardElevation = 0f

        val deleteIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val padding = (6 * density).toInt()
            setPadding(padding, padding, padding, padding)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
        addView(deleteIcon)

        setOnClickListener { onDelete() }
    }

    private companion object {
        const val FADE_DURATION_MS = 200L
        const val COLOR_TRANSITION_MS = 450L
        val SCRIM_DARK = Color.parseColor("#40000000")
        val SCRIM_ERROR = Color.parseColor("#99B00020")
    }
}
