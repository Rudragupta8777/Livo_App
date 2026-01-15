package com.livo.works.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import kotlinx.coroutines.flow.collectLatest // For collecting flow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.livo.works.ViewModel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Splash : AppCompatActivity() {

    // Hilt delivers the ViewModel here
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash screen on-screen until destination is decided
        splashScreen.setKeepOnScreenCondition { true }

        observeDestination()
    }

    private fun observeDestination() {
        lifecycleScope.launch {
            viewModel.destination.collect { target ->
                target?.let {
                    val intent = when (it) {
                        "ONBOARDING" -> Intent(this@Splash, Onboarding::class.java)
                        "LOGIN" -> Intent(this@Splash, Login::class.java)
                        "DASHBOARD" -> Intent(this@Splash, Dashboard::class.java)
                        else -> null
                    }
                    intent?.let { start ->
                        startActivity(start)
                        finish()
                    }
                }
            }
        }
    }
}