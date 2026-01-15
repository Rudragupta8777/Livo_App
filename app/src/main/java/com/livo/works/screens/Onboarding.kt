package com.livo.works.screens

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.livo.works.R
import com.livo.works.adapter.OnboardingAdapter
import com.livo.works.items.OnboardingItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Onboarding : AppCompatActivity() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var viewPager: ViewPager2
    private lateinit var btnAction: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvSkip: TextView
    private lateinit var pageIndicator: LinearLayout

    private var currentPage = 0
    private val totalPages = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        enableEdgeToEdge()

        initializeViews()
        setupOnboarding()
        setupPageIndicator()
        setupClickListeners()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        btnAction = findViewById(R.id.btnAction)
        btnBack = findViewById(R.id.btnBack)
        tvSkip = findViewById(R.id.tvSkip)
        pageIndicator = findViewById(R.id.pageIndicator)
    }

    private fun setupOnboarding() {
        val items = listOf(
            OnboardingItem(
                title = "Welcome to Livo",
                description = "Your perfect hotel booking companion. Discover, book, and enjoy amazing stays effortlessly.",
                imageRes = R.drawable.onboard_img_1
            ),
            OnboardingItem(
                title = "Find Best Hotels",
                description = "Browse thousands of hotels worldwide. Filter by price, location, and amenities to find your perfect match.",
                imageRes = R.drawable.onboard_img_2
            ),
            OnboardingItem(
                title = "Easy Booking",
                description = "Book your dream stay in just a few taps. Secure payment and instant confirmation guaranteed.",
                imageRes = R.drawable.onboard_img_3
            )
        )

        viewPager.adapter = OnboardingAdapter(items)

        // Add page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                updateUI()
                animatePageIndicator(position)
            }
        })
    }

    private fun setupPageIndicator() {
        val dots = arrayOfNulls<View>(totalPages)

        // Create dots
        for (i in 0 until totalPages) {
            dots[i] = View(this)
            dots[i]?.layoutParams = LinearLayout.LayoutParams(
                24, 24
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            dots[i]?.background = ContextCompat.getDrawable(
                this,
                if (i == 0) R.drawable.dot_indicator else R.drawable.dot_indicator_inactive
            )
            pageIndicator.addView(dots[i])
        }
    }

    private fun animatePageIndicator(position: Int) {
        for (i in 0 until totalPages) {
            val dot = pageIndicator.getChildAt(i)

            // Animate active dot
            if (i == position) {
                dot.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(200)
                    .start()
                dot.background = ContextCompat.getDrawable(this, R.drawable.dot_indicator)
            } else {
                dot.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
                dot.background = ContextCompat.getDrawable(this, R.drawable.dot_indicator_inactive)
            }
        }
    }

    private fun updateUI() {
        // Update button text and functionality
        when (currentPage) {
            0 -> {
                btnAction.text = "Next"
                btnBack.visibility = View.INVISIBLE
                tvSkip.visibility = View.VISIBLE
            }
            totalPages - 1 -> {
                btnAction.text = "Get Started"
                btnBack.visibility = View.VISIBLE
                tvSkip.visibility = View.INVISIBLE

                // Add subtle animation to "Get Started" button
                animateButton()
            }
            else -> {
                btnAction.text = "Next"
                btnBack.visibility = View.VISIBLE
                tvSkip.visibility = View.VISIBLE
            }
        }
    }

    private fun animateButton() {
        // Pulse animation for "Get Started" button
        val colorFrom = ContextCompat.getColor(this, R.color.text_primary)
        val colorTo = ContextCompat.getColor(this, R.color.text_primary) // You can use a lighter shade

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo, colorFrom)
        colorAnimation.duration = 1500
        colorAnimation.repeatCount = ValueAnimator.INFINITE
        colorAnimation.addUpdateListener { animator ->
            btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(
                animator.animatedValue as Int
            )
        }
        colorAnimation.start()
    }

    private fun setupClickListeners() {
        // Next/Get Started button
        btnAction.setOnClickListener {
            if (currentPage < totalPages - 1) {
                // Go to next page with smooth scroll
                viewPager.setCurrentItem(currentPage + 1, true)
            } else {
                // Last page - navigate to Login
                completeOnboarding()
            }
        }

        // Back button
        btnBack.setOnClickListener {
            if (currentPage > 0) {
                viewPager.setCurrentItem(currentPage - 1, true)
            }
        }

        // Skip button
        tvSkip.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        // Mark onboarding as completed
        sharedPreferences.edit()
            .putBoolean("is_first_time", false)
            .apply()

        // Navigate to Login with animation
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()

        // Custom transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}