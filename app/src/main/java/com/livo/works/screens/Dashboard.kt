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

        // 1. Correctly initialize ViewBinding
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Load Default Fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // 3. Handle Bottom Navigation Clicks
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

        if (intent.getStringExtra("OPEN_TAB") == "BOOKINGS") {
            binding.bottomNav.selectedItemId = R.id.nav_booking
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