package com.cgprograms.dnslock

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText

class MainActivity : Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var wifiManager: WifiManager
    private lateinit var ssid: EditText
    private lateinit var password: EditText
    private lateinit var addButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Additional text explaining why we need this permission"
        )
        startActivityForResult(intent, RESULT_ENABLE)

        ssid = findViewById(R.id.ssid)
        password = findViewById(R.id.password)
        addButton = findViewById(R.id.add)

        ssid.setOnClickListener {
            showAvailableSSIDs()
        }

        addButton.setOnClickListener {
            wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"${ssid.text}\""
                preSharedKey = "\"${password.text}\""
            }
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.enableNetwork(netId, true)
        }
    }

    private fun showAvailableSSIDs() {
        wifiManager.setWifiEnabled(true)

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    displaySSIDDialog()
                } else {
                    Log.d("MainActivity", "WiFi Scan failed")
                }
                unregisterReceiver(this)
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            Log.d("MainActivity", "WiFi Scan initiation failed")
        }
    }

    @SuppressLint("MissingPermission")
    private fun displaySSIDDialog() {
        val scanResults = wifiManager.scanResults
        val ssidList = scanResults
            .filter { it.SSID.isNotEmpty() }
            .sortedByDescending { it.level }
            .map { it.SSID }
            .toSet()
            .toList()

        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select SSID")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ssidList)
        builder.setAdapter(adapter) { _, which ->
            ssid.setText(ssidList[which])
            dialog?.dismiss()
            password.requestFocus()
        }

        builder.setNegativeButton("Cancel", null)
        dialog = builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_ENABLE) {
            if (resultCode == RESULT_OK) {
                Log.d("MainActivity", "Device Admin enabled")
            } else {
                Log.d("MainActivity", "Device Admin enable FAILED")
            }
        }
    }

    companion object {
        const val RESULT_ENABLE = 1
    }
}