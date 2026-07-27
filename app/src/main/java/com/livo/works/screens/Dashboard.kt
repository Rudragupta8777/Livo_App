package com.livo.works.screens

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts // Added for Play Updates
import com.google.android.play.core.appupdate.AppUpdateManagerFactory // Added for Play Updates
import com.google.android.play.core.appupdate.AppUpdateOptions // Added for Play Updates
import com.google.android.play.core.install.model.AppUpdateType // Added for Play Updates
import com.google.android.play.core.install.model.UpdateAvailability // Added for Play Updates
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
    private lateinit var homeFragment: Fragment
    private lateinit var searchFragment: Fragment
    private lateinit var bookingFragment: Fragment
    private lateinit var profileFragment: Fragment

    private lateinit var activeFragment: Fragment

    // --- ADDED: Play Core Update Launcher ---
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // User cancelled or the update failed.
            // If you want to force them to update, uncomment the line below to close the app:
            // finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // First time creation
            homeFragment = HomeFragment()
            searchFragment = SearchFragment()
            bookingFragment = BookingFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, profileFragment, "4").hide(profileFragment)
                add(R.id.fragmentContainer, bookingFragment, "3").hide(bookingFragment)
                add(R.id.fragmentContainer, searchFragment, "2").hide(searchFragment)
                add(R.id.fragmentContainer, homeFragment, "1")
            }.commit()

            activeFragment = homeFragment
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag("1") ?: HomeFragment()
            searchFragment = supportFragmentManager.findFragmentByTag("2") ?: SearchFragment()
            bookingFragment = supportFragmentManager.findFragmentByTag("3") ?: BookingFragment()
            profileFragment = supportFragmentManager.findFragmentByTag("4") ?: ProfileFragment()

            // Restore active fragment based on bottom nav selection
            activeFragment = when (binding.bottomNav.selectedItemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_search -> searchFragment
                R.id.nav_booking -> bookingFragment
                R.id.nav_profile -> profileFragment
                else -> homeFragment
            }
        }

        setupBottomNavListener()
        handleNavigationIntent()

        // --- ADDED: Trigger update check ---
        checkForAppUpdates()
    }

    private fun setupBottomNavListener() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_search -> {
                    switchFragment(searchFragment)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_booking -> {
                    switchFragment(bookingFragment)
                    return@setOnItemSelectedListener true
                }
                R.id.nav_profile -> {
                    switchFragment(profileFragment)
                    return@setOnItemSelectedListener true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment == targetFragment) return

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .hide(activeFragment)
            .show(targetFragment)
            .commit()

        activeFragment = targetFragment
    }

    private fun handleNavigationIntent() {
        val openTab = intent.getStringExtra("OPEN_TAB")
        if (openTab == "BOOKINGS") {
            binding.bottomNav.selectedItemId = R.id.nav_booking
        }
    }

    // --- ADDED: Play Core Update Logic ---
    private fun checkForAppUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }
}