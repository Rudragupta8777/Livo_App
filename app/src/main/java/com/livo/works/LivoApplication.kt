package com.livo.works

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.livo.works.security.TokenManager
import com.livo.works.screens.Login
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LivoApplication : Application(), Application.ActivityLifecycleCallbacks {

    @Inject
    lateinit var tokenManager: TokenManager
    private var currentActivity: Activity? = null
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)

        applicationScope.launch {
            tokenManager.logoutSignal.collect {
                handleSessionExpired()
            }
        }
    }

    private fun handleSessionExpired() {
        currentActivity?.let { activity ->
            if (activity !is Login) {
                val intent = Intent(activity, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
                activity.finishAffinity()
            }
        } ?: run {
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {
    }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}