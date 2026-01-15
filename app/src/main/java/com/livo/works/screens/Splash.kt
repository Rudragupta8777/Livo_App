package com.livo.works.screens

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.livo.works.R
import com.livo.works.ViewModel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Splash : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { false }
        runCinematicAnimation()
        observeDestination()
    }

    private fun runCinematicAnimation() {
        val logo = findViewById<ImageView>(R.id.ivLogo)
        val text = findViewById<TextView>(R.id.tvAppName)

        val logoMoveUp = ObjectAnimator.ofFloat(logo, "translationY", -100f).apply {
            duration = 1200
            interpolator = AnticipateOvershootInterpolator(1.5f)
        }

        val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 0.9f, 1.05f, 1f).apply {
            duration = 1200
        }
        val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 0.9f, 1.05f, 1f).apply {
            duration = 1200
        }

        val textFade = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).apply {
            duration = 800
        }

        val textMove = ObjectAnimator.ofFloat(text, "translationY", 50f, -150f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
        }

        val textExpand = ValueAnimator.ofFloat(-0.1f, 0.2f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                text.letterSpacing = animator.animatedValue as Float
            }
        }

        AnimatorSet().apply {
            play(logoMoveUp).with(logoScaleX).with(logoScaleY)
            play(textFade).with(textMove).with(textExpand).after(400)
            start()
        }
    }

    private fun observeDestination() {
        lifecycleScope.launch {
            viewModel.destination.collectLatest { target ->
                // Prevent multiple navigations
                if (hasNavigated) return@collectLatest

                target?.let { destination ->
                    hasNavigated = true

                    val intent = when (destination) {
                        SplashViewModel.DESTINATION_ONBOARDING ->
                            Intent(this@Splash, Onboarding::class.java)

                        SplashViewModel.DESTINATION_LOGIN ->
                            Intent(this@Splash, Login::class.java)

                        SplashViewModel.DESTINATION_DASHBOARD ->
                            Intent(this@Splash, Dashboard::class.java)

                        else -> null
                    }

                    intent?.let {
                        startActivity(it)
                        finish()
                        overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                    }
                }
            }
        }
    }
}