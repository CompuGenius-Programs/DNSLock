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
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var wifiManager: WifiManager
    private lateinit var ssid: EditText
    private lateinit var password: EditText
    private lateinit var addButton: Button
    private lateinit var toggleWifiButton: Button
    private var loadingDialog: AlertDialog? = null

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
        toggleWifiButton = findViewById(R.id.toggle_wifi)

        ssid.setOnClickListener {
            showAvailableSSIDs()
        }

        ssid.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showAvailableSSIDs()
            }
        }

        addButton.setOnClickListener {
            val wifiConfig = WifiConfiguration().apply {
                SSID = "\"${ssid.text}\""
                preSharedKey = "\"${password.text}\""
            }
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.enableNetwork(netId, true)
        }

        // Toggle Wi-Fi on/off
        toggleWifiButton.setOnClickListener {
            val status = !wifiManager.isWifiEnabled
            wifiManager.isWifiEnabled = status
            val statusText = if (status) "enabled" else "disabled"
            Toast.makeText(this, "Wi-Fi is now $statusText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAvailableSSIDs() {
        wifiManager.setWifiEnabled(true)

        // Show loading dialog before starting the scan
        showLoadingDialog()

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    displaySSIDDialog()
                } else {
                    Log.d("MainActivity", "WiFi Scan failed")
                }
                // Dismiss the loading dialog once scan results are ready
                dismissLoadingDialog()
                unregisterReceiver(this)
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            Log.d("MainActivity", "WiFi Scan initiation failed")
            dismissLoadingDialog() // Dismiss loading dialog in case of failure
        }
    }

    @SuppressLint("MissingPermission")
    private fun displaySSIDDialog() {
        val scanResults = wifiManager.scanResults
        val ssidList = scanResults.filter { it.SSID.isNotEmpty() }.sortedByDescending { it.level }
            .map { it to getSecurityType(it) }
            .map { "${it.first.SSID} (${it.second})" }  // Display SSID and security type
            .toSet().toList()

        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select SSID")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ssidList)
        builder.setAdapter(adapter) { _, which ->
            val selectedSSID = ssidList[which].split(" (")[0]  // Extract the SSID
            ssid.setText(selectedSSID)
            dialog?.dismiss()
            password.requestFocus()
        }

        builder.setNegativeButton("Cancel", null)
        dialog = builder.show()
    }

    // Function to determine the security type of the Wi-Fi network
    private fun getSecurityType(scanResult: ScanResult): String {
        val capabilities = scanResult.capabilities
        return when {
            capabilities.contains("WEP") -> "WEP"
            capabilities.contains("WPA") -> "WPA/WPA2"
            capabilities.contains("SAE") -> "WPA3"
            else -> "Open"  // No security
        }
    }

    // Show a loading dialog with a progress bar
    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val loadingView = inflater.inflate(R.layout.loading_dialog, null)
            builder.setView(loadingView)
            builder.setCancelable(false)
            loadingDialog = builder.create()
        }
        loadingDialog?.show()
    }

    // Dismiss the loading dialog
    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
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
