package com.cgprograms.dnslock

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class TorMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 60000

    private val checkTorRunnable = object : Runnable {
        override fun run() {
            checkTorUsage()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(checkTorRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkTorRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkTorUsage() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(currentTime - checkInterval, currentTime)

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val packageName = event.packageName
                if (packageName.contains("torproject") || packageName.contains("torbrowser")) {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

                    dpm.setUninstallBlocked(adminComponent, packageName, true)
                    dpm.setPackagesSuspended(
                        adminComponent, arrayOf<String>(packageName), true
                    )
                }
            }
        }
    }
}