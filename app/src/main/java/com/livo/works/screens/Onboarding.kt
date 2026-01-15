package com.livo.works.screens

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.livo.works.R
import com.livo.works.adapter.OnboardingAdapter
import com.livo.works.items.OnboardingItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Onboarding : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnGetStarted: MaterialButton


    @Inject lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        enableEdgeToEdge()

        viewPager = findViewById(R.id.viewPager)
        btnGetStarted = findViewById(R.id.btnGetStarted)

        // ... inside onCreate
        val items = listOf(
            OnboardingItem(
                "Luxury Stays",
                "Discover handpicked hotels for your perfect vacation.",
                R.drawable.onboard_img_1
            ),
            OnboardingItem(
                "Secure Booking",
                "Your data is protected by industry-level security.",
                R.drawable.onboard_img_2
            ),
            OnboardingItem(
                "Easy Check-in",
                "Get your digital key and skip the queue.",
                R.drawable.onboard_img_3
            )
        )
        viewPager.adapter = OnboardingAdapter(items)

        // Show button only on last page
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == items.size - 1) {
                    btnGetStarted.visibility = View.VISIBLE
                    btnGetStarted.alpha = 0f
                    btnGetStarted.animate().alpha(1f).duration = 500
                } else {
                    btnGetStarted.visibility = View.GONE
                }
            }
        })

        sharedPrefs.edit().putBoolean("is_first_time", false).apply()
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}