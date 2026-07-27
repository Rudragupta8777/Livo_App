package com.livo.works.util

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.google.android.material.button.MaterialButton
import com.livo.works.R

class GlobalNetworkMonitor(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var currentActivity: Activity? = null
    private var networkDialog: Dialog? = null
    private var isNetworkAvailable = true
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Initial check on launch
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable = caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        // Listen for live network changes
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                if (hasInternet && isValidated) {
                    if (!isNetworkAvailable) {
                        isNetworkAvailable = true
                        mainHandler.post { dismissDialog() }
                    }
                } else {
                    if (isNetworkAvailable) {
                        isNetworkAvailable = false
                        mainHandler.post { showDialog() }
                    }
                }
            }

            override fun onLost(network: Network) {
                isNetworkAvailable = false
                mainHandler.post { showDialog() }
            }
        })

        application.registerActivityLifecycleCallbacks(this)
    }

    private fun showDialog() {
        currentActivity?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed && networkDialog?.isShowing != true) {
                val dialog = Dialog(activity)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(false) // Blocks the user from dismissing it

                val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_no_internet, null)
                dialog.setContentView(layout)

                // Perfect 30dp margins setup
                val marginPx = (30 * activity.resources.displayMetrics.density).toInt()
                val insetDrawable = InsetDrawable(ColorDrawable(Color.TRANSPARENT), marginPx, 0, marginPx, 0)

                dialog.window?.setBackgroundDrawable(insetDrawable)
                dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                dialog.window?.setGravity(Gravity.CENTER)

                // Wire up the button to open Wi-Fi settings
                val btnCheckSettings = layout.findViewById<MaterialButton>(R.id.btnCheckSettings)
                btnCheckSettings.setOnClickListener {
                    activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }

                networkDialog = dialog
                dialog.show()
            }
        }
    }

    private fun dismissDialog() {
        networkDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        networkDialog = null
    }

    // --- Lifecycle Overrides ---
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // If network drops while activity was paused, show dialog immediately upon returning
        if (!isNetworkAvailable) {
            showDialog()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            dismissDialog() // Clean up to prevent WindowLeaked exceptions
            currentActivity = null
        }
    }

    // Required empty implementations
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}