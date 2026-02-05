package com.livo.works.screens

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.livo.works.R
import com.livo.works.databinding.ActivityDashboardBinding
import com.livo.works.screens.fragments.BookingFragment
import com.livo.works.screens.fragments.HomeFragment
import com.livo.works.screens.fragments.ProfileFragment
import com.livo.works.screens.fragments.SearchFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setup Listener (Extracted to function so we can reuse it)
        setupBottomNavListener()

        // 2. Handle Navigation Logic
        if (savedInstanceState == null) {
            handleNavigationIntent()
        }
    }

    private fun setupBottomNavListener() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_booking -> BookingFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }
            fragment?.let {
                loadFragment(it)
                return@setOnItemSelectedListener true
            }
            false
        }
    }

    private fun handleNavigationIntent() {
        val openTab = intent.getStringExtra("OPEN_TAB")

        if (openTab == "BOOKINGS") {
            binding.bottomNav.selectedItemId = R.id.nav_booking
        } else {
            // Default
            loadFragment(HomeFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}