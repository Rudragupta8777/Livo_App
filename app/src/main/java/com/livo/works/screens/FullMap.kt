package com.livo.works.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.livo.works.R
import com.livo.works.databinding.ActivityFullMapBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class FullMap : AppCompatActivity() {

    private lateinit var binding: ActivityFullMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)
        val name = intent.getStringExtra("NAME") ?: "Hotel Location"

        setupMap(lat, lng)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnGetDirections.setOnClickListener {
            // Open Google Maps for Navigation
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            // Try to open Google Maps, fallback to browser if not installed
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
                startActivity(browserIntent)
            }
        }
    }

    private fun setupMap(lat: Double, lng: Double) {
        val point = Point.fromLngLat(lng, lat)
        val mapboxMap = binding.mapView.getMapboxMap()

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val styleUri = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) Style.DARK else Style.MAPBOX_STREETS

        mapboxMap.loadStyleUri(styleUri) {
            mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
            addMarker(point)
        }
    }

    private fun addMarker(point: Point) {
        // Use the same tinting logic as HotelDetails
        bitmapFromDrawableRes(this, R.drawable.ic_map)?.let { bitmap ->
            val annotationApi = binding.mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(2.0) // Bigger marker for full screen
            pointAnnotationManager.create(pointAnnotationOptions)
        }
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int): Bitmap? {
        var drawable = AppCompatResources.getDrawable(context, resourceId) ?: return null
        drawable = DrawableCompat.wrap(drawable)
        // Tint with Primary Color
        DrawableCompat.setTint(drawable, context.getColor(R.color.text_primary))

        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}